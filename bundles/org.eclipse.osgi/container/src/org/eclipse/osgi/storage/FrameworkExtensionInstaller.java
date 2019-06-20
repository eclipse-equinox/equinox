/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
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
package org.eclipse.osgi.storage;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleCapability;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.ModuleWire;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.container.namespaces.EquinoxModuleDataNamespace;
import org.eclipse.osgi.framework.util.ArrayMap;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.hookregistry.ActivatorHookFactory;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;

public class FrameworkExtensionInstaller {
	private static final ClassLoader CL = FrameworkExtensionInstaller.class.getClassLoader();
	private static final Method ADD_FWK_URL_METHOD = findAddURLMethod(CL, "addURL"); //$NON-NLS-1$
	private final ArrayMap<BundleActivator, Bundle> hookActivators = new ArrayMap<>(5);

	private static Method findAddURLMethod(ClassLoader cl, String name) {
		if (cl == null)
			return null;
		return findMethod(cl.getClass(), name, new Class[] {URL.class});
	}

	// recursively searches a class and it's superclasses for a (potentially inaccessable) method
	private static Method findMethod(Class<?> clazz, String name, Class<?>[] args) {
		if (clazz == null)
			return null; // ends the recursion when getSuperClass returns null
		try {
			Method result = clazz.getDeclaredMethod(name, args);
			result.setAccessible(true);
			return result;
		} catch (SecurityException e) {
			// if we do not have the permissions then we will not find the method
		} catch (NoSuchMethodException | RuntimeException e) {
			// do nothing look in super class below
			// have to avoid blowing up <clinit>
		}  
		return findMethod(clazz.getSuperclass(), name, args);
	}

	private static void callAddURLMethod(URL arg) throws InvocationTargetException {
		try {
			ADD_FWK_URL_METHOD.invoke(CL, new Object[] {arg});
		} catch (Throwable t) {
			throw new InvocationTargetException(t);
		}
	}

	private final EquinoxConfiguration configuration;

	public FrameworkExtensionInstaller(EquinoxConfiguration configuraiton) {
		this.configuration = configuraiton;
	}

	public void addExtensionContent(final Collection<ModuleRevision> revisions, final Module systemModule) throws BundleException {
		if (System.getSecurityManager() == null) {
			addExtensionContent0(revisions, systemModule);
		} else {
			try {
				AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
					@Override
					public Void run() throws BundleException {
						addExtensionContent0(revisions, systemModule);
						return null;
					}
				});
			} catch (PrivilegedActionException e) {
				throw (BundleException) e.getCause();
			}
		}
	}

	void addExtensionContent0(Collection<ModuleRevision> revisions, Module systemModule) throws BundleException {
		if (revisions.isEmpty()) {
			// NOTE: revisions could be empty when initializing the framework with no
			// framework extensions
			return;
		}
		if (CL == null || ADD_FWK_URL_METHOD == null) {
			// use the first revision as the blame
			ModuleRevision revision = revisions.iterator().next();
			throw new BundleException("Cannot support framework extension bundles without a public addURL(URL) method on the framework class loader: " + revision.getBundle()); //$NON-NLS-1$
		}

		for (ModuleRevision revision : revisions) {
			File[] files = getExtensionFiles(revision);
			if (files == null) {
				return;
			}
			for (int i = 0; i < files.length; i++) {
				if (files[i] == null)
					continue;
				try {
					callAddURLMethod(StorageUtil.encodeFileURL(files[i]));
				} catch (InvocationTargetException | MalformedURLException e) {
					throw new BundleException("Error adding extension content.", e); //$NON-NLS-1$
				} 
			}
		}

		try {
			// initialize the new urls
			CL.loadClass("thisIsNotAClass"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			// do nothing
		}
		if (systemModule != null) {
			BundleContext systemContext = systemModule.getBundle().getBundleContext();
			for (ModuleRevision revision : revisions) {
				if (systemContext != null) {
					startExtensionActivator(revision, systemContext);
				}
			}
		}
	}

	/**
	 * Returns a list of classpath files for an extension bundle
	 * @param revision revision for the extension bundle
	 * @return a list of classpath files for an extension bundle
	 */
	private File[] getExtensionFiles(ModuleRevision revision) {
		List<ModuleCapability> metaDatas = revision.getModuleCapabilities(EquinoxModuleDataNamespace.MODULE_DATA_NAMESPACE);
		@SuppressWarnings("unchecked")
		List<String> paths = metaDatas.isEmpty() ? null : (List<String>) metaDatas.get(0).getAttributes().get(EquinoxModuleDataNamespace.CAPABILITY_CLASSPATH);
		if (paths == null) {
			paths = new ArrayList<>(1);
			paths.add("."); //$NON-NLS-1$
		}
		if (configuration.inDevelopmentMode()) {
			// must create a copy because paths could be unmodifiable
			paths = new ArrayList<>(paths);
			String[] devPaths = configuration.getDevClassPath(revision.getSymbolicName());
			Collections.addAll(paths, devPaths);
		}
		List<File> results = new ArrayList<>(paths.size());
		for (String path : paths) {
			if (".".equals(path)) { //$NON-NLS-1$
				results.add(((Generation) revision.getRevisionInfo()).getBundleFile().getBaseFile());
			} else {
				File result = ((Generation) revision.getRevisionInfo()).getBundleFile().getFile(path, false);
				if (result != null)
					results.add(result);
			}
		}
		return results.toArray(new File[results.size()]);
	}

	public void startExtensionActivators(BundleContext context) {
		// First start the hook registry activators
		// TODO not sure we really need these anymore
		HookRegistry hookRegistry = configuration.getHookRegistry();
		List<ActivatorHookFactory> activatorHookFactories = hookRegistry.getActivatorHookFactories();
		for (ActivatorHookFactory activatorFactory : activatorHookFactories) {
			BundleActivator activator = activatorFactory.createActivator();
			try {
				startActivator(activator, context, null);
			} catch (Exception e) {
				configuration.getHookRegistry().getContainer().getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, null, e);
			}
		}
		// start the extension bundle activators.  In Equinox we let
		// framework extensions define Bundle-Activator headers.
		ModuleWiring systemWiring = (ModuleWiring) context.getBundle().adapt(BundleWiring.class);
		if (systemWiring != null) {
			List<ModuleWire> extensionWires = systemWiring.getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
			for (ModuleWire extensionWire : extensionWires) {
				ModuleRevision extensionRevision = extensionWire.getRequirer();
				startExtensionActivator(extensionRevision, context);
			}
		}
	}

	public void stopExtensionActivators(BundleContext context) {
		ArrayMap<BundleActivator, Bundle> current;
		synchronized (hookActivators) {
			current = new ArrayMap<>(hookActivators.getKeys(), hookActivators.getValues());
			hookActivators.clear();
		}
		for (BundleActivator activator : current) {
			try {
				activator.stop(context);
			} catch (Exception e) {
				Bundle b = current.get(activator);
				BundleException eventException = new BundleException(NLS.bind(Msg.BUNDLE_ACTIVATOR_EXCEPTION, new Object[] {activator.getClass(), "stop", b == null ? "" : b.getSymbolicName()}), BundleException.ACTIVATOR_ERROR, e); //$NON-NLS-1$ //$NON-NLS-2$
				configuration.getHookRegistry().getContainer().getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, b, eventException);
			}
		}
	}

	private void startExtensionActivator(ModuleRevision extensionRevision, BundleContext context) {
		List<Capability> metadata = extensionRevision.getCapabilities(EquinoxModuleDataNamespace.MODULE_DATA_NAMESPACE);
		if (metadata.isEmpty()) {
			return;
		}

		String activatorName = (String) metadata.get(0).getAttributes().get(EquinoxModuleDataNamespace.CAPABILITY_ACTIVATOR);
		if (activatorName == null) {
			return;
		}

		BundleActivator activator = null;
		try {
			Class<?> activatorClass = Class.forName(activatorName);
			activator = (BundleActivator) activatorClass.getConstructor().newInstance();
			startActivator(activator, context, extensionRevision.getBundle());
		} catch (Throwable e) {
			BundleException eventException;
			if (activator == null) {
				eventException = new BundleException(Msg.BundleContextImpl_LoadActivatorError, BundleException.ACTIVATOR_ERROR, e);
			} else {
				eventException = new BundleException(NLS.bind(Msg.BUNDLE_ACTIVATOR_EXCEPTION, new Object[] {activator.getClass(), "start", extensionRevision.getSymbolicName()}), BundleException.ACTIVATOR_ERROR, e); //$NON-NLS-1$
			}
			configuration.getHookRegistry().getContainer().getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, extensionRevision.getBundle(), eventException);
		}

	}

	private void startActivator(BundleActivator activator, BundleContext context, Bundle b) throws Exception {
		activator.start(context);
		synchronized (hookActivators) {
			hookActivators.put(activator, b);
		}
	}

}
