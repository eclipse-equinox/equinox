/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.baseadaptor;

import java.io.File;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook;
import org.eclipse.osgi.baseadaptor.loader.*;
import org.eclipse.osgi.framework.adaptor.BundleProtectionDomain;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Constants;

public class BaseClassLoadingHook implements ClassLoadingHook {
	private static final String[] LIB_EXTENSIONS;
	private static final String[] EMPTY_STRINGS = new String[0];
	static {
		String[] libExtensions = ManifestElement.getArrayFromList(FrameworkProperties.getProperty("osgi.framework.library.extensions", FrameworkProperties.getProperty(Constants.FRAMEWORK_LIBRARY_EXTENSIONS, getOSLibraryExtDefaults())), ","); //$NON-NLS-1$ //$NON-NLS-2$
		for (int i = 0; i < libExtensions.length; i++)
			if (libExtensions[i].length() > 0 && libExtensions[i].charAt(0) != '.')
				libExtensions[i] = '.' + libExtensions[i];
		LIB_EXTENSIONS = libExtensions;
	}

	private static String getOSLibraryExtDefaults() {
		// Some OSes have multiple library extensions
		// We should provide defaults to the known ones
		// For example Mac OS X uses dylib and jnilib (bug 380350)
		String os = FrameworkProperties.getProperty("os.name"); //$NON-NLS-1$
		return os == null || !os.startsWith("Mac OS") ? null : "dylib,jnilib"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * Maps an already mapped library name to additional library file extensions.
	 * This is needed on platforms like AIX where .a and .so can be used as library file
	 * extensions, but System.mapLibraryName only returns a single string.
	 */
	public static String[] mapLibraryNames(String mappedLibName) {
		int extIndex = mappedLibName.lastIndexOf('.');
		if (LIB_EXTENSIONS.length == 0 || extIndex < 0)
			return EMPTY_STRINGS;
		String libNameBase = mappedLibName.substring(0, extIndex);
		String[] results = new String[LIB_EXTENSIONS.length];
		for (int i = 0; i < results.length; i++)
			results[i] = libNameBase + LIB_EXTENSIONS[i];
		return results;
	}

	public String findLibrary(BaseData data, String libName) {
		String mappedName = System.mapLibraryName(libName);
		String path = null;
		if (Debug.DEBUG_LOADER)
			Debug.println("  mapped library name: " + mappedName); //$NON-NLS-1$
		path = findNativePath(data, mappedName);
		if (path == null) {
			String[] mappedNames = mapLibraryNames(mappedName);
			for (int i = 0; i < mappedNames.length && path == null; i++)
				path = findNativePath(data, mappedNames[i]);
		}
		if (path == null) {
			if (Debug.DEBUG_LOADER)
				Debug.println("  library does not exist: " + mappedName); //$NON-NLS-1$
			path = findNativePath(data, libName);
		}
		if (Debug.DEBUG_LOADER)
			Debug.println("  returning library: " + path); //$NON-NLS-1$
		return path;
	}

	private String findNativePath(BaseData bundledata, String libname) {
		int slash = libname.lastIndexOf('/');
		if (slash >= 0)
			libname = libname.substring(slash + 1);
		String[] nativepaths = getNativePaths(bundledata);
		if (nativepaths == null)
			return null;
		for (int i = 0; i < nativepaths.length; i++) {
			slash = nativepaths[i].lastIndexOf('/');
			String path = slash < 0 ? nativepaths[i] : nativepaths[i].substring(slash + 1);
			if (path.equals(libname)) {
				if (nativepaths[i].startsWith(BaseStorageHook.EXTERNAL_LIB_PREFIX)) {
					// references an external library; do variable substitution
					String externalPath = BaseStorageHook.substituteVars(nativepaths[i].substring(BaseStorageHook.EXTERNAL_LIB_PREFIX.length()));
					File nativeFile = new File(externalPath);
					return nativeFile.getAbsolutePath();
				}
				// this is a normal library contained within the bundle
				File nativeFile = bundledata.getBundleFile().getFile(nativepaths[i], true);
				if (nativeFile != null)
					return nativeFile.getAbsolutePath();
			}
		}
		return null;
	}

	private String[] getNativePaths(BaseData bundledata) {
		BaseStorageHook storageHook = (BaseStorageHook) bundledata.getStorageHook(BaseStorageHook.KEY);
		return storageHook != null ? storageHook.getNativePaths() : null;
	}

	public boolean addClassPathEntry(ArrayList<ClasspathEntry> cpEntries, String cp, ClasspathManager hostmanager, BaseData sourcedata, ProtectionDomain sourcedomain) {
		// do nothing
		return false;
	}

	public ClassLoader getBundleClassLoaderParent() {
		// do nothing
		return null;
	}

	public byte[] processClass(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry, ClasspathManager manager) {
		// do nothing
		return null;
	}

	public BaseClassLoader createClassLoader(ClassLoader parent, ClassLoaderDelegate delegate, BundleProtectionDomain domain, BaseData data, String[] bundleclasspath) {
		// do nothing
		return null;
	}

	public void initializedClassLoader(BaseClassLoader baseClassLoader, BaseData data) {
		// do nothing
	}

}
