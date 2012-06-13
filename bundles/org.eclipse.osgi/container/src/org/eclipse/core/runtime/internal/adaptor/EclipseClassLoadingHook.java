/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.runtime.internal.adaptor;

import java.io.File;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.eclipse.osgi.baseadaptor.*;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook;
import org.eclipse.osgi.baseadaptor.loader.*;
import org.eclipse.osgi.framework.adaptor.BundleProtectionDomain;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.internal.baseadaptor.BaseClassLoadingHook;
import org.eclipse.osgi.internal.baseadaptor.BaseStorageHook;

public class EclipseClassLoadingHook implements ClassLoadingHook, HookConfigurator {
	private static String[] NL_JAR_VARIANTS = buildNLJarVariants(EclipseEnvironmentInfo.getDefault().getNL());
	private static boolean DEFINE_PACKAGES;
	private final static boolean DEFINE_PACKAGE_ATTRIBUTES = !"noattributes".equals(FrameworkProperties.getProperty("osgi.classloader.define.packages")); //$NON-NLS-1$ //$NON-NLS-2$
	private static String[] LIB_VARIANTS = buildLibraryVariants();
	private Object pkgLock = new Object();

	static {
		try {
			Class.forName("java.lang.Package"); //$NON-NLS-1$
			DEFINE_PACKAGES = true;
		} catch (ClassNotFoundException e) {
			DEFINE_PACKAGES = false;
		}
	}

	private static String[] buildLibraryVariants() {
		List<String> result = new ArrayList<String>();
		EclipseEnvironmentInfo info = EclipseEnvironmentInfo.getDefault();
		result.add("ws/" + info.getWS() + "/"); //$NON-NLS-1$ //$NON-NLS-2$
		result.add("os/" + info.getOS() + "/" + info.getOSArch() + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		result.add("os/" + info.getOS() + "/"); //$NON-NLS-1$ //$NON-NLS-2$
		String nl = info.getNL();
		nl = nl.replace('_', '/');
		while (nl.length() > 0) {
			result.add("nl/" + nl + "/"); //$NON-NLS-1$ //$NON-NLS-2$
			int i = nl.lastIndexOf('/');
			nl = (i < 0) ? "" : nl.substring(0, i); //$NON-NLS-1$
		}
		result.add(""); //$NON-NLS-1$
		return result.toArray(new String[result.size()]);
	}

	public byte[] processClass(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry, ClasspathManager manager) {
		if (!DEFINE_PACKAGES)
			return null;
		// Define the package if it is not the default package.
		int lastIndex = name.lastIndexOf('.');
		if (lastIndex < 0)
			return null;
		String packageName = name.substring(0, lastIndex);
		Object pkg;
		synchronized (pkgLock) {
			pkg = manager.getBaseClassLoader().publicGetPackage(packageName);
			if (pkg != null)
				return null;
		}

		// get info about the package from the classpath entry's manifest.
		String specTitle = null, specVersion = null, specVendor = null, implTitle = null, implVersion = null, implVendor = null;

		if (DEFINE_PACKAGE_ATTRIBUTES) {
			ClasspathManifest cpm = (ClasspathManifest) classpathEntry.getUserObject(ClasspathManifest.KEY);
			if (cpm == null) {
				cpm = new ClasspathManifest();
				classpathEntry.addUserObject(cpm);
			}
			Manifest mf = cpm.getManifest(classpathEntry, manager);
			if (mf != null) {
				Attributes mainAttributes = mf.getMainAttributes();
				String dirName = packageName.replace('.', '/') + '/';
				Attributes packageAttributes = mf.getAttributes(dirName);
				boolean noEntry = false;
				if (packageAttributes == null) {
					noEntry = true;
					packageAttributes = mainAttributes;
				}
				specTitle = packageAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
				if (specTitle == null && !noEntry)
					specTitle = mainAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
				specVersion = packageAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
				if (specVersion == null && !noEntry)
					specVersion = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
				specVendor = packageAttributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);
				if (specVendor == null && !noEntry)
					specVendor = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);
				implTitle = packageAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
				if (implTitle == null && !noEntry)
					implTitle = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
				implVersion = packageAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
				if (implVersion == null && !noEntry)
					implVersion = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
				implVendor = packageAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
				if (implVendor == null && !noEntry)
					implVendor = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
			}
		}

		// The package is not defined yet define it before we define the class.
		// TODO still need to seal packages.
		synchronized (pkgLock) {
			pkg = manager.getBaseClassLoader().publicGetPackage(packageName);
			if (pkg != null)
				return null;
			manager.getBaseClassLoader().publicDefinePackage(packageName, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, null);
		}
		// not doing any byte processing
		return null;
	}

	public boolean addClassPathEntry(ArrayList<ClasspathEntry> cpEntries, String cp, ClasspathManager hostmanager, BaseData sourcedata, ProtectionDomain sourcedomain) {
		String var = hasPrefix(cp);
		if (var != null)
			// find internal library using eclipse predefined vars
			return addInternalClassPath(var, cpEntries, cp, hostmanager, sourcedata, sourcedomain);
		if (cp.startsWith(BaseStorageHook.EXTERNAL_LIB_PREFIX)) {
			cp = cp.substring(BaseStorageHook.EXTERNAL_LIB_PREFIX.length());
			// find external library using system property substitution
			ClasspathEntry cpEntry = hostmanager.getExternalClassPath(BaseStorageHook.substituteVars(cp), sourcedata, sourcedomain);
			if (cpEntry != null) {
				cpEntries.add(cpEntry);
				return true;
			}
		}
		return false;
	}

	private boolean addInternalClassPath(String var, ArrayList<ClasspathEntry> cpEntries, String cp, ClasspathManager hostloader, BaseData sourcedata, ProtectionDomain sourcedomain) {
		if (var.equals("ws")) //$NON-NLS-1$
			return ClasspathManager.addClassPathEntry(cpEntries, "ws/" + EclipseEnvironmentInfo.getDefault().getWS() + cp.substring(4), hostloader, sourcedata, sourcedomain); //$NON-NLS-1$
		if (var.equals("os")) //$NON-NLS-1$
			return ClasspathManager.addClassPathEntry(cpEntries, "os/" + EclipseEnvironmentInfo.getDefault().getOS() + cp.substring(4), hostloader, sourcedata, sourcedomain); //$NON-NLS-1$ 
		if (var.equals("nl")) { //$NON-NLS-1$
			cp = cp.substring(4);
			for (int i = 0; i < NL_JAR_VARIANTS.length; i++)
				if (ClasspathManager.addClassPathEntry(cpEntries, "nl/" + NL_JAR_VARIANTS[i] + cp, hostloader, sourcedata, sourcedomain)) //$NON-NLS-1$ 
					return true;
		}
		return false;
	}

	//return a String representing the string found between the $s
	private static String hasPrefix(String libPath) {
		if (libPath.startsWith("$ws$")) //$NON-NLS-1$
			return "ws"; //$NON-NLS-1$
		if (libPath.startsWith("$os$")) //$NON-NLS-1$
			return "os"; //$NON-NLS-1$
		if (libPath.startsWith("$nl$")) //$NON-NLS-1$
			return "nl"; //$NON-NLS-1$
		return null;
	}

	private static String[] buildNLJarVariants(String nl) {
		List<String> result = new ArrayList<String>();
		nl = nl.replace('_', '/');
		while (nl.length() > 0) {
			result.add("nl/" + nl + "/"); //$NON-NLS-1$ //$NON-NLS-2$
			int i = nl.lastIndexOf('/');
			nl = (i < 0) ? "" : nl.substring(0, i); //$NON-NLS-1$
		}
		result.add(""); //$NON-NLS-1$
		return result.toArray(new String[result.size()]);
	}

	public void recordClassDefine(String name, Class<?> clazz, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry, ClasspathManager manager) {
		// do nothing
	}

	public String findLibrary(BaseData data, String libName) {
		if (libName.length() == 0)
			return null;
		if (libName.charAt(0) == '/' || libName.charAt(0) == '\\')
			libName = libName.substring(1);
		String mappedLibName = System.mapLibraryName(libName);
		String result = searchVariants(data, mappedLibName);
		if (result != null)
			return result;
		String[] mappedLibNames = BaseClassLoadingHook.mapLibraryNames(mappedLibName);
		for (int i = 0; i < mappedLibNames.length && result == null; i++)
			result = searchVariants(data, mappedLibNames[i]);
		return result;
	}

	private String searchVariants(BaseData bundledata, String path) {
		for (int i = 0; i < LIB_VARIANTS.length; i++) {
			BundleFile baseBundleFile = bundledata.getBundleFile();
			BundleEntry libEntry = baseBundleFile.getEntry(LIB_VARIANTS[i] + path);
			if (libEntry != null) {
				File libFile = baseBundleFile.getFile(LIB_VARIANTS[i] + path, true);
				if (libFile == null)
					return null;
				// see bug 88697 - HP requires libraries to have executable permissions
				if (org.eclipse.osgi.service.environment.Constants.OS_HPUX.equals(EclipseEnvironmentInfo.getDefault().getOS())) {
					try {
						// use the string array method in case there is a space in the path
						Runtime.getRuntime().exec(new String[] {"chmod", "755", libFile.getAbsolutePath()}).waitFor(); //$NON-NLS-1$ //$NON-NLS-2$
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				return libFile.getAbsolutePath();
			}
		}
		return null;
	}

	public ClassLoader getBundleClassLoaderParent() {
		return null; // do nothing
	}

	public void addHooks(HookRegistry hookRegistry) {
		hookRegistry.addClassLoadingHook(this);
	}

	public BaseClassLoader createClassLoader(ClassLoader parent, ClassLoaderDelegate delegate, BundleProtectionDomain domain, BaseData data, String[] bundleclasspath) {
		// do nothing
		return null;
	}

	public void initializedClassLoader(BaseClassLoader baseClassLoader, BaseData data) {
		// do nothing
	}
}
