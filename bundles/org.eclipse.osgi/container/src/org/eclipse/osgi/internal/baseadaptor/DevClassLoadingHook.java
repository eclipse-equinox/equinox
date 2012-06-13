/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
import org.eclipse.osgi.baseadaptor.*;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook;
import org.eclipse.osgi.baseadaptor.loader.*;
import org.eclipse.osgi.framework.adaptor.BundleProtectionDomain;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.eclipse.osgi.framework.util.KeyedElement;

public class DevClassLoadingHook implements ClassLoadingHook, HookConfigurator, KeyedElement {
	public static final String KEY = DevClassLoadingHook.class.getName();
	public static final int HASHCODE = KEY.hashCode();
	private static final String FRAGMENT = "@fragment@"; //$NON-NLS-1$

	public byte[] processClass(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry, ClasspathManager manager) {
		// Do nothing
		return null;
	}

	public boolean addClassPathEntry(ArrayList<ClasspathEntry> cpEntries, String cp, ClasspathManager hostmanager, BaseData sourcedata, ProtectionDomain sourcedomain) {
		// first check that we are in devmode for this sourcedata
		String[] devClassPath = !DevClassPathHelper.inDevelopmentMode() ? null : DevClassPathHelper.getDevClassPath(sourcedata.getSymbolicName());
		if (devClassPath == null || devClassPath.length == 0)
			return false; // not in dev mode return
		// check that dev classpath entries have not already been added; we mark this in the first entry below
		if (cpEntries.size() > 0 && cpEntries.get(0).getUserObject(KEY) != null)
			return false; // this source has already had its dev classpath entries added.
		boolean result = false;
		for (int i = 0; i < devClassPath.length; i++) {
			if (ClasspathManager.addClassPathEntry(cpEntries, devClassPath[i], hostmanager, sourcedata, sourcedomain))
				result = true;
			else {
				String devCP = devClassPath[i];
				boolean fromFragment = devCP.endsWith(FRAGMENT);
				if (!fromFragment && devCP.indexOf("..") >= 0) { //$NON-NLS-1$
					// if in dev mode, try using cp as a relative path from the base bundle file
					File base = sourcedata.getBundleFile().getBaseFile();
					if (base.isDirectory()) {
						// this is only supported for directory bundles
						ClasspathEntry entry = hostmanager.getExternalClassPath(new File(base, devCP).getAbsolutePath(), sourcedata, sourcedomain);
						if (entry != null) {
							cpEntries.add(entry);
							result = true;
						}
					}
				} else {
					// if in dev mode, try using the cp as an absolute path
					// we assume absolute entries come from fragments.  Find the source
					if (fromFragment)
						devCP = devCP.substring(0, devCP.length() - FRAGMENT.length());
					BaseData fragData = findFragmentSource(sourcedata, devCP, hostmanager, fromFragment);
					if (fragData != null) {
						ClasspathEntry entry = hostmanager.getExternalClassPath(devCP, fragData, sourcedomain);
						if (entry != null) {
							cpEntries.add(entry);
							result = true;
						}
					}
				}
			}
		}
		// mark the first entry of the list.  
		// This way we can quickly tell that dev classpath entries have been added to the list
		if (result && cpEntries.size() > 0)
			cpEntries.get(0).addUserObject(this);
		return result;
	}

	private BaseData findFragmentSource(BaseData hostData, String cp, ClasspathManager manager, boolean fromFragment) {
		if (hostData != manager.getBaseData())
			return hostData;

		File file = new File(cp);
		if (!file.isAbsolute())
			return hostData;
		FragmentClasspath[] fragCP = manager.getFragmentClasspaths();
		for (int i = 0; i < fragCP.length; i++) {
			BundleFile fragBase = fragCP[i].getBundleData().getBundleFile();
			File fragFile = fragBase.getBaseFile();
			if (fragFile != null && file.getPath().startsWith(fragFile.getPath()))
				return fragCP[i].getBundleData();
		}
		return fromFragment ? null : hostData;
	}

	public String findLibrary(BaseData data, String libName) {
		// Do nothing
		return null;
	}

	public ClassLoader getBundleClassLoaderParent() {
		// Do nothing
		return null;
	}

	public BaseClassLoader createClassLoader(ClassLoader parent, ClassLoaderDelegate delegate, BundleProtectionDomain domain, BaseData data, String[] bundleclasspath) {
		// do nothing
		return null;
	}

	public void initializedClassLoader(BaseClassLoader baseClassLoader, BaseData data) {
		// do nothing
	}

	public void addHooks(HookRegistry hookRegistry) {
		if (DevClassPathHelper.inDevelopmentMode())
			// only add dev classpath manager if in dev mode
			hookRegistry.addClassLoadingHook(new DevClassLoadingHook());

	}

	public boolean compare(KeyedElement other) {
		return other.getKey() == KEY;
	}

	public Object getKey() {
		return KEY;
	}

	public int getKeyHashCode() {
		return HASHCODE;
	}
}
