/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.hooks;

import java.util.*;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.container.Module.StartOptions;
import org.eclipse.osgi.container.Module.State;
import org.eclipse.osgi.container.namespaces.EquinoxModuleDataNamespace;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;
import org.eclipse.osgi.internal.location.EclipseAdaptorMsg;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;

public class EclipseLazyStarter extends ClassLoaderHook {
	private static final EnumSet<State> alreadyActive = EnumSet.of(State.ACTIVE, State.STOPPING, State.UNINSTALLED);
	// holds the current activation trigger class and the ClasspathManagers that need to be activated
	private final ThreadLocal<List<Object>> activationStack = new ThreadLocal<List<Object>>();
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
		ModuleRevision revision = manager.getGeneration().getRevision();
		Module module = revision.getRevisions().getModule();
		// If the bundle is active, uninstalled or stopping then the bundle has already
		// been initialized (though it may have been destroyed) so just return the class.
		if (alreadyActive.contains(module.getState()))
			return;
		// The bundle is not active and does not require activation, just return the class
		if (!shouldActivateFor(name, module, revision, manager))
			return;
		List<Object> stack = activationStack.get();
		if (stack == null) {
			stack = new ArrayList<Object>(6);
			activationStack.set(stack);
		}
		// the first element in the stack is the name of the trigger class, 
		// each element after the trigger class is a classpath manager 
		// that must be activated after the trigger class has been defined (see postFindLocalClass)
		int size = stack.size();
		if (size > 1) {
			for (int i = size - 1; i >= 1; i--)
				if (manager == stack.get(i))
					// the manager is already on the stack in which case we are already in the process of loading the trigger class
					return;
		}
		if (size == 0)
			stack.add(name);
		stack.add(manager);
	}

	@Override
	public void postFindLocalClass(String name, Class<?> clazz, ClasspathManager manager) throws ClassNotFoundException {
		List<Object> stack = activationStack.get();
		if (stack == null)
			return;
		int size = stack.size();
		if (size <= 1 || stack.get(0) != name)
			return;
		// if we have a stack we must clear it even if (clazz == null)
		ClasspathManager[] managers = null;
		managers = new ClasspathManager[size - 1];
		for (int i = 1; i < size; i++)
			managers[i - 1] = (ClasspathManager) stack.get(i);
		stack.clear();
		if (clazz == null)
			return;
		for (int i = managers.length - 1; i >= 0; i--) {
			if (errors.get(managers[i]) != null) {
				if (container.getConfiguration().throwErrorOnFailedStart)
					throw errors.get(managers[i]);
				continue;
			}

			// The bundle must be started.
			// Note that another thread may already be starting this bundle;
			// In this case we will timeout after a default of 5 seconds and record the BundleException
			long startTime = System.currentTimeMillis();
			try {
				// do not persist the start of this bundle
				managers[i].getGeneration().getRevision().getRevisions().getModule().start(StartOptions.LAZY_TRIGGER);
			} catch (BundleException e) {
				Bundle bundle = managers[i].getGeneration().getRevision().getBundle();
				if (e.getType() == BundleException.STATECHANGE_ERROR) {
					String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CLASSLOADER_CONCURRENT_STARTUP, new Object[] {Thread.currentThread(), name, null, bundle, new Long(System.currentTimeMillis() - startTime)});
					container.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.WARNING, message, e);
					continue;
				}
				String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CLASSLOADER_ACTIVATION, bundle.getSymbolicName(), Long.toString(bundle.getBundleId()));
				ClassNotFoundException error = new ClassNotFoundException(message, e);
				errors.put(managers[i], error);
				if (container.getConfiguration().throwErrorOnFailedStart) {
					container.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, message, e, null);
					throw error;
				}
				container.getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, bundle, new BundleException(message, e));
			}
		}
	}

	private boolean shouldActivateFor(String className, Module module, ModuleRevision revision, ClasspathManager manager) throws ClassNotFoundException {
		if (!State.LAZY_STARTING.equals(module.getState())) {
			// Don't activate non-starting bundles
			if (State.RESOLVED.equals(module.getState())) {
				// handle the resolved case where a previous error occurred
				if (container.getConfiguration().throwErrorOnFailedStart) {
					ClassNotFoundException error = errors.get(manager);
					if (error != null)
						throw error;
				}
				return module.isPersistentlyStarted() && module.isActivationPolicyUsed() && isLazyStartable(className, revision);
			}
			return false;
		}

		return isLazyStartable(className, revision);
	}

	private boolean isLazyStartable(String className, ModuleRevision revision) {
		List<ModuleCapability> moduleDatas = revision.getModuleCapabilities(EquinoxModuleDataNamespace.MODULE_DATA_NAMESPACE);
		if (moduleDatas.isEmpty()) {
			return false;
		}

		Map<String, Object> moduleDataAttrs = moduleDatas.get(0).getAttributes();
		String policy = (String) moduleDataAttrs.get(EquinoxModuleDataNamespace.CAPABILITY_ACTIVATION_POLICY);
		if (!EquinoxModuleDataNamespace.CAPABILITY_ACTIVATION_POLICY_LAZY.equals(policy)) {
			return false;
		}

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

}
