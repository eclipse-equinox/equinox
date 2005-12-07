/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.preferences;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.framework.*;

/**
 * <p>
 * Class used to manage OSGi Preferences Service.  Creates a new OSGiPreferencesServiceImpl
 * object for every bundle that gets the Preferences Service.  When a bundle ungets the 
 * Preference Service, it's preferences are flushed to disk.
 * </p>
 * <p>
 * Also deletes saved preferences for bundles which are uninstalled.
 * </p>
 * <p>
 * Preferences are saved in the Bundle Data area under the directory "OSGiPreferences".
 * </p>
 */
public class OSGiPreferencesServiceManager implements ServiceFactory, BundleListener {

	private static final String OSGI_PREFS_DIR = "OSGiPreferences"; //$NON-NLS-1$

	private File prefsDir;

	public OSGiPreferencesServiceManager(BundleContext context) {

		prefsDir = context.getDataFile(OSGI_PREFS_DIR);

		context.addBundleListener(this);

		//clean up prefs for bundles that have been uninstalled
		Bundle[] allBundles = context.getBundles();
		Set bundleDirNames = new TreeSet();
		for (int i = 0; i < allBundles.length; i++) {
			bundleDirNames.add(getBundleDirName(allBundles[i]));
		}
		File[] prefsNodeDirs = prefsDir.listFiles();
		prefsNodeDirs = prefsNodeDirs == null ? new File[0] : prefsNodeDirs;

		for (int i = 0; i < prefsNodeDirs.length; i++) {
			if (!bundleDirNames.contains(prefsNodeDirs[i].getName())) {
				rmdir(prefsNodeDirs[i]);
			}

		}
	}

	/**
	 * Recursively remove a file or a directory and all of it's children.
	 */
	private void rmdir(File file) {
		if (!file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			File[] children = file.listFiles();

			for (int i = 0; i < children.length; i++) {
				rmdir(children[i]);
			}
		}
		file.delete();
	}

	/**
	 * Bundle Preferences are saves in a directory with the same name as the bundle's 
	 * symbolic id.  For backwards compatibility, preferences for bundles that do not 
	 * have a symbolic id are saved in a directory named 
	 * 'org.eclipse.core.internal.preferences.OSGiPreferences.bundleid.&lt;bundle id&gt;'.
	 */
	private String getBundleDirName(Bundle bundle) {
		String bundleDirName = bundle.getSymbolicName();

		//backwards compatibility - if bundle does not have symbolic name
		if (bundleDirName == null) {
			bundleDirName = "org.eclipse.core.internal.preferences.OSGiPreferences.bundleid." + bundle.getBundleId(); //$NON-NLS-1$
		}
		return bundleDirName;
	}
	
	/**
	 * Creates a new OSGiPreferencesServiceImpl for each bundle.
	 */
	public Object getService(Bundle bundle, ServiceRegistration registration) {
		return new OSGiPreferencesServiceImpl(new File(prefsDir, getBundleDirName(bundle)));
	}

	/**
	 * Flush the bundle's preferences to disk.
	 */
	public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
		((OSGiPreferencesServiceImpl) service).destroy();
	}

	/**
	 * If a bundle is uninstalled, delete all of it's preferences from the disk.
	 */
	public void bundleChanged(BundleEvent event) {
		if (event.getType() == BundleEvent.UNINSTALLED) {
			File bundlePrefs = new File(prefsDir, getBundleDirName(event.getBundle()));
			rmdir(bundlePrefs);
		}

	}

}
