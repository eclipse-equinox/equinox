/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.hooks;

import java.security.AccessController;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.Module.StartOptions;
import org.eclipse.osgi.container.Module.State;
import org.eclipse.osgi.container.ModuleCapability;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.namespaces.EquinoxModuleDataNamespace;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;

public class EclipseLazyStarter extends ClassLoaderHook {
	private static final EnumSet<State> alreadyActive = EnumSet.of(State.ACTIVE, State.STOPPING, State.UNINSTALLED);
	private static final SecureAction secureAction = AccessController.doPrivileged(SecureAction.createSecureAction());

	// holds the initiating class name
	private final ThreadLocal<String> initiatingClassName = new ThreadLocal<>();
	// holds the ClasspathManagers that need to be activated
	private final ThreadLocal<Deque<ClasspathManager>> activationStack = new ThreadLocal<>();
	// used to store exceptions that occurred while activating a bundle
	// keyed by ClasspathManager->Exception
	// WeakHashMap is used to prevent pinning the ClasspathManager objects.
	private final Map<ClasspathManager, ClassNotFoundException> errors = Collections.synchronizedMap(new WeakHashMap<ClasspathManager, ClassNotFoundException>());

	private final EquinoxContainer container;

	public EclipseLazyStarter(EquinoxContainer container) {
		this.container = container;
	}

	@Override
	public void preFindLocalClass(String name, ClasspathManager manager) throws ClassNotFoundException {
		if (initiatingClassName.get() == null) {
			initiatingClassName.set(name);
		}
		ModuleRevision revision = manager.getGeneration().getRevision();
		Module module = revision.getRevisions().getModule();
		// If the bundle is active, uninstalled or stopping then the bundle has already
		// been initialized (though it may have been destroyed) so just return the class.
		if (alreadyActive.contains(module.getState()))
			return;
		// The bundle is not active and does not require activation, just return the class
		if (!shouldActivateFor(name, module, revision, manager))
			return;
		Deque<ClasspathManager> stack = activationStack.get();
		if (stack == null) {
			stack = new ArrayDeque<>(6);
			activationStack.set(stack);
		}
		// each element is a classpath manager that must be activated after
		// the initiating class has been defined (see postFindLocalClass)
		if (!stack.contains(manager)) {
			// only add the manager if it has not been added already
			stack.addFirst(manager);
		}
	}

	@Override
	public void postFindLocalClass(String name, Class<?> clazz, ClasspathManager manager) throws ClassNotFoundException {
		if (initiatingClassName.get() != name)
			return;
		initiatingClassName.set(null);
		Deque<ClasspathManager> stack = activationStack.get();
		if (stack == null || stack.isEmpty())
			return;

		// if we have a stack we must clear it even if (clazz == null)
		List<ClasspathManager> managers = new ArrayList<>(stack);
		stack.clear();
		if (clazz == null)
			return;
		for (ClasspathManager managerElement : managers) {
			if (errors.get(managerElement) != null) {
				if (container.getConfiguration().throwErrorOnFailedStart)
					throw errors.get(managerElement);
				continue;
			}

			// The bundle must be started.
			// Note that another thread may already be starting this bundle;
			// In this case we will timeout after a default of 5 seconds and record the BundleException
			long startTime = System.currentTimeMillis();
			Module m = managerElement.getGeneration().getRevision().getRevisions().getModule();
			try {
				// do not persist the start of this bundle
				secureAction.start(m, StartOptions.LAZY_TRIGGER);
			} catch (BundleException e) {
				Bundle bundle = managerElement.getGeneration().getRevision().getBundle();
				if (e.getType() == BundleException.STATECHANGE_ERROR) {
					String message = NLS.bind(Msg.ECLIPSE_CLASSLOADER_CONCURRENT_STARTUP, new Object[] {Thread.currentThread(), name, m.getStateChangeOwner(), bundle, new Long(System.currentTimeMillis() - startTime)});
					container.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.WARNING, message, e);
					continue;
				}
				String message = NLS.bind(Msg.ECLIPSE_CLASSLOADER_ACTIVATION, bundle.getSymbolicName(), Long.toString(bundle.getBundleId()));
				ClassNotFoundException error = new ClassNotFoundException(message, e);
				errors.put(managerElement, error);
				if (container.getConfiguration().throwErrorOnFailedStart) {
					container.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, message, e, null);
					throw error;
				}
				container.getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, bundle, new BundleException(message, e));
			}
		}
	}

	private boolean shouldActivateFor(String className, Module module, ModuleRevision revision, ClasspathManager manager) throws ClassNotFoundException {
		State state = module.getState();
		if (!State.LAZY_STARTING.equals(state)) {
			if (State.STARTING.equals(state) && manager.getClassLoader().getBundleLoader().isTriggerSet()) {
				// the trigger has been set but we are waiting for the activation to complete
				return true;
			}
			// Don't activate non-starting bundles
			if (State.RESOLVED.equals(module.getState())) {
				// handle the resolved case where a previous error occurred
				if (container.getConfiguration().throwErrorOnFailedStart) {
					ClassNotFoundException error = errors.get(manager);
					if (error != null)
						throw error;
				}
				// The module is persistently started and has the lazy activation policy but has not entered the LAZY_STARTING state
				// There are 2 cases where this can happen
				// 1) The start-level thread has not gotten to transitioning the bundle to LAZY_STARTING yet
				// 2) The bundle is marked for eager activation and the start-level thread has not activated it yet
				// In both cases we need to fire the lazy start trigger to activate the bundle if the start-level is met
				return module.isPersistentlyStarted() && isLazyStartable(className, revision);
			}
			return false;
		}

		return isLazyStartable(className, revision);
	}

	private boolean isLazyStartable(String className, ModuleRevision revision) {
		if (!revision.hasLazyActivatePolicy()) {
			return false;
		}
		List<ModuleCapability> moduleDatas = revision.getModuleCapabilities(EquinoxModuleDataNamespace.MODULE_DATA_NAMESPACE);
		if (moduleDatas.isEmpty()) {
			return false;
		}

		Map<String, Object> moduleDataAttrs = moduleDatas.get(0).getAttributes();
		@SuppressWarnings("unchecked")
		List<String> excludes = (List<String>) moduleDataAttrs.get(EquinoxModuleDataNamespace.CAPABILITY_LAZY_EXCLUDE_ATTRIBUTE);
		@SuppressWarnings("unchecked")
		List<String> includes = (List<String>) moduleDataAttrs.get(EquinoxModuleDataNamespace.CAPABILITY_LAZY_INCLUDE_ATTRIBUTE);
		// no exceptions, it is easy to figure it out
		if (excludes == null && includes == null)
			return true;
		// otherwise, we need to check if the package is in the exceptions list
		int dotPosition = className.lastIndexOf('.');
		// the class has no package name... no exceptions apply
		if (dotPosition == -1)
			return true;
		String packageName = className.substring(0, dotPosition);
		return ((includes == null || includes.contains(packageName)) && (excludes == null || !excludes.contains(packageName)));
	}

	@Override
	public boolean isProcessClassRecursionSupported() {
		return true;
	}

}
