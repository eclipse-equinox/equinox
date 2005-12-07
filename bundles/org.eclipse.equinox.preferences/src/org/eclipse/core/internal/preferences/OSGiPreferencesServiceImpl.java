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
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.*;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;

/**
 * <p>
 * Implements OSGi PreferencesService using the Eclipse preference system.
 * </p>
 * 
 * <p>
 * Note: Eclipse preferences are not accessible through the OSGi Preferences API and vice
 *  versa.
 * </p>
 */
public class OSGiPreferencesServiceImpl implements PreferencesService {

	/**
	 * Adaptor that implements OSGi Preferences interface on top of EclipsePreferences.
	 *
	 */
	private static final class OSGiPreferences extends EclipsePreferences implements Preferences {

		private IPath location;
		private IEclipsePreferences loadLevel;
		private OSGiPreferencesServiceImpl prefsServiceImpl;

		private OSGiPreferences(File prefsDir, OSGiPreferencesServiceImpl prefsServiceImpl) {
			super(null, ""); //$NON-NLS-1$
			this.prefsServiceImpl = prefsServiceImpl;
			this.location = new Path(prefsDir.getPath());
			this.loadLevel = this;
		}

		private OSGiPreferences(EclipsePreferences nodeParent, String nodeName, OSGiPreferencesServiceImpl prefsServiceImpl) {
			super(nodeParent, nodeName);
			this.loadLevel = nodeParent.getLoadLevel();
			this.prefsServiceImpl = prefsServiceImpl;
		}

		protected EclipsePreferences internalCreate(EclipsePreferences nodeParent, String nodeName, Object context) {
			return new OSGiPreferences(nodeParent, nodeName, prefsServiceImpl);
		}

		protected IPath getLocation() {
			return location;
		}

		protected IEclipsePreferences getLoadLevel() {
			return loadLevel;
		}

		/**
		 * Override node(String pathName) to be more strict about forbidden names - 
		 * EclipsePreferences implementation does a best-effort instead of throwing 
		 * {@link IllegalArgumentException}.
		 */
		public Preferences node(String pathName) {
			if ((pathName.length() > 1 && pathName.endsWith("/")) //$NON-NLS-1$
					|| pathName.indexOf("//") != -1) { //$NON-NLS-1$				
				throw new IllegalArgumentException();
			}
			return super.node(pathName);
		}

		/**
		 * Override removeNode() to allow removal of root nodes.  EclipsePreferences ignores
		 * attempts to remove the root node, but in OSGi Preferences there are many root nodes
		 * and removal is permitted.
		 */
		public void removeNode() throws BackingStoreException {
			if (parent() == null) {
				flush();
				if (this == prefsServiceImpl.systemPreferences) {
					prefsServiceImpl.systemPreferences = null;
				} else {
					prefsServiceImpl.userPreferences.values().remove(this);
				}
			}

			super.removeNode();
			removed = true;
		}

		/**
		 * <p>
		 * Override getByteArray(String key, byte [] defaultValue) to be more strict when
		 * decoding byte values.  EclipsePreferences implementation pads bytes if they are not 4
		 * bytes long, but the OSGi TCK expects this function to return null if the length of 
		 * the byte array is not an even multiple of 4. 
		 * </p>
		 * <p>
		 * Also catches any decoding exceptions and returns the default value instead of 
		 * propagating the exception.
		 * </p>
		 */
		public byte[] getByteArray(String key, byte[] defaultValue) {
			String value = internalGet(key);
			byte[] byteArray = null;
			if (value != null) {
				byte[] encodedBytes = value.getBytes();
				if (encodedBytes.length % 4 == 0) {
					try {
						byteArray = Base64.decode(encodedBytes);
					} catch (Exception e) {
						//do not raise exception - return defaultValue
					}
				}
			}
			return byteArray == null ? defaultValue : byteArray;
		}

	}

	private File systemPrefsDir;
	private File userPrefsDir;

	Preferences systemPreferences;

	//Map of String user name -> Preferences 
	Map userPreferences;

	OSGiPreferencesServiceImpl(File prefsLocation) {
		systemPrefsDir = new File(prefsLocation, "system"); //$NON-NLS-1$
		userPrefsDir = new File(prefsLocation, "user"); //$NON-NLS-1$
		userPreferences = new TreeMap(); //use TreeMap since keys are strings
	}

	public Preferences getSystemPreferences() {
		if (systemPreferences == null) {
			systemPreferences = new OSGiPreferences(systemPrefsDir, this);
			try {
				systemPreferences.sync();
			} catch (BackingStoreException e) {
				//nothing
			}
		}
		return systemPreferences;
	}

	public Preferences getUserPreferences(String name) {
		Preferences userPref = (Preferences) userPreferences.get(name);
		if (userPref == null) {
			userPref = new OSGiPreferences(new File(userPrefsDir, name), this);
			try {
				userPref.sync();
			} catch (BackingStoreException e) {
				//nothing
			}
			userPreferences.put(name, userPref);
		}
		return userPref;
	}

	public String[] getUsers() {
		return userPrefsDir.list();
	}

	/**
	 * Called when Bundle ungets Preferences Service - flushes all preferences to disk.
	 */
	void destroy() {
		try {
			if (systemPreferences != null && systemPreferences.nodeExists("")) { //$NON-NLS-1$
				systemPreferences.flush();
			}
		} catch (BackingStoreException e) {
			//nothing
		}
		Iterator it = userPreferences.values().iterator();
		while (it.hasNext()) {
			Preferences userPreference = (Preferences) it.next();
			try {
				if (userPreference.nodeExists("")) { //$NON-NLS-1$
					userPreference.flush();
				}
			} catch (BackingStoreException e) {
				//nothing
			}
		}

	}
}
