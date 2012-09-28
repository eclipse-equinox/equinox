/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
import org.eclipse.osgi.container.ModuleCapability;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.namespaces.EquinoxModuleDataNamespace;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.osgi.framework.BundleException;

public class FrameworkExtensionInstaller {
	private static final ClassLoader CL = FrameworkExtensionInstaller.class.getClassLoader();
	private static final Method ADD_FWK_URL_METHOD = findAddURLMethod(CL, "addURL"); //$NON-NLS-1$

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

	public void addExtensionContent(final ModuleRevision revision) throws BundleException {
		if (System.getSecurityManager() == null) {
			addExtensionContent0(revision);
		} else {
			try {
				AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
					@Override
					public Object run() throws BundleException {
						addExtensionContent0(revision);
						return null;
					}
				});
			} catch (PrivilegedActionException e) {
				throw (BundleException) e.getCause();
			}
		}
	}

	void addExtensionContent0(ModuleRevision revision) throws BundleException {
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
				throw new BundleException("Error adding extension content.", e);
			} catch (MalformedURLException e) {
				throw new BundleException("Error adding extension content.", e);
			}
		}
		try {
			// initialize the new urls
			CL.loadClass("thisIsNotAClass"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			// do nothing
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
}
