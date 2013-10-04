/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.security.*;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.container.namespaces.EquinoxModuleDataNamespace;
import org.eclipse.osgi.framework.util.ArrayMap;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.hookregistry.ActivatorHookFactory;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.osgi.framework.*;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;

public class FrameworkExtensionInstaller {
	private static final ClassLoader CL = FrameworkExtensionInstaller.class.getClassLoader();
	private static final Method ADD_FWK_URL_METHOD = findAddURLMethod(CL, "addURL"); //$NON-NLS-1$
	private final ArrayMap<BundleActivator, Bundle> hookActivators = new ArrayMap<BundleActivator, Bundle>(5);

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
		} catch (NoSuchMethodException e) {
			// do nothing look in super class below
		} catch (SecurityException e) {
			// if we do not have the permissions then we will not find the method
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

	public void addExtensionContent(final ModuleRevision revision, final Module systemModule) throws BundleException {
		if (System.getSecurityManager() == null) {
			addExtensionContent0(revision, systemModule);
		} else {
			try {
				AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
					@Override
					public Void run() throws BundleException {
						addExtensionContent0(revision, systemModule);
						return null;
					}
				});
			} catch (PrivilegedActionException e) {
				throw (BundleException) e.getCause();
			}
		}
	}

	void addExtensionContent0(ModuleRevision revision, Module systemModule) throws BundleException {
		if (CL == null || ADD_FWK_URL_METHOD == null) {
			return;
		}

		File[] files = getExtensionFiles(revision);
		if (files == null) {
			return;
		}
		for (int i = 0; i < files.length; i++) {
			if (files[i] == null)
				continue;
			try {
				callAddURLMethod(StorageUtil.encodeFileURL(files[i]));
			} catch (InvocationTargetException e) {
				throw new BundleException("Error adding extension content.", e); //$NON-NLS-1$
			} catch (MalformedURLException e) {
				throw new BundleException("Error adding extension content.", e); //$NON-NLS-1$
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
			if (systemContext != null) {
				startExtensionActivator(revision, systemContext);
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
			paths = new ArrayList<String>(1);
			paths.add("."); //$NON-NLS-1$
		}
		if (configuration.inDevelopmentMode()) {
			String[] devPaths = configuration.getDevClassPath(revision.getSymbolicName());
			for (String devPath : devPaths) {
				paths.add(devPath);
			}
		}
		List<File> results = new ArrayList<File>(paths.size());
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
			startActivator(activator, context, null);
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
			current = new ArrayMap<BundleActivator, Bundle>(hookActivators.getKeys(), hookActivators.getValues());
			hookActivators.clear();
		}
		for (BundleActivator activator : current) {
			try {
				activator.stop(context);
			} catch (Exception e) {
				configuration.getHookRegistry().getContainer().getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, current.get(activator), e);
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
		try {
			Class<?> activatorClass = Class.forName(activatorName);
			BundleActivator activator = (BundleActivator) activatorClass.newInstance();
			startActivator(activator, context, extensionRevision.getBundle());
		} catch (Exception e) {
			configuration.getHookRegistry().getContainer().getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, extensionRevision.getBundle(), e);
		}
	}

	private void startActivator(BundleActivator activator, BundleContext context, Bundle b) {
		try {
			activator.start(context);
			synchronized (hookActivators) {
				hookActivators.put(activator, b);
			}
		} catch (Exception e) {
			configuration.getHookRegistry().getContainer().getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, b, e);
		}
	}

}
