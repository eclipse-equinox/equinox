/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.internal.adaptor;

import java.util.Dictionary;
import java.util.Enumeration;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

/**
 * Internal class.
 */
public class CachedManifest extends Dictionary<String, String> {
	static final String SERVICE_COMPONENT = "Service-Component"; //$NON-NLS-1$
	static boolean DEBUG = false;
	private Dictionary<String, String> manifest = null;
	private EclipseStorageHook storageHook;

	public CachedManifest(EclipseStorageHook storageHook) {
		this.storageHook = storageHook;
	}

	public Dictionary<String, String> getManifest() {
		if (manifest == null)
			try {
				if (DEBUG)
					System.out.println("Reading manifest for: " + storageHook.getBaseData()); //$NON-NLS-1$
				manifest = storageHook.createCachedManifest(true);
			} catch (BundleException e) {
				final String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CACHEDMANIFEST_UNEXPECTED_EXCEPTION, storageHook.getBaseData().getLocation());
				FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, message, 0, e, null);
				storageHook.getAdaptor().getFrameworkLog().log(entry);
			}
		if (manifest == null) {
			Headers<String, String> empty = new Headers<String, String>(0);
			empty.setReadOnly();
			manifest = empty;
			return empty;
		}
		return manifest;
	}

	public int size() {
		return getManifest().size();
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public Enumeration<String> elements() {
		return getManifest().elements();
	}

	public Enumeration<String> keys() {
		return getManifest().keys();
	}

	@SuppressWarnings("deprecation")
	public String get(Object key) {
		if (manifest != null)
			return manifest.get(key);
		String keyString = (String) key;
		if (Constants.BUNDLE_VERSION.equalsIgnoreCase(keyString)) {
			Version result = storageHook.getBaseData().getVersion();
			return result == null ? null : result.toString();
		}
		if (Constants.PLUGIN_CLASS.equalsIgnoreCase(keyString))
			return storageHook.getPluginClass();
		if (Constants.BUNDLE_SYMBOLICNAME.equalsIgnoreCase(keyString)) {
			if ((storageHook.getBaseData().getType() & BundleData.TYPE_SINGLETON) == 0)
				return storageHook.getBaseData().getSymbolicName();
			return storageHook.getBaseData().getSymbolicName() + ';' + Constants.SINGLETON_DIRECTIVE + ":=true"; //$NON-NLS-1$
		}
		if (Constants.BUDDY_LOADER.equalsIgnoreCase(keyString))
			return storageHook.getBuddyList();
		if (Constants.REGISTERED_POLICY.equalsIgnoreCase(keyString))
			return storageHook.getRegisteredBuddyList();
		if (Constants.BUNDLE_ACTIVATOR.equalsIgnoreCase(keyString))
			return storageHook.getBaseData().getActivator();
		if (Constants.BUNDLE_ACTIVATIONPOLICY.equals(keyString)) {
			if (!storageHook.isAutoStartable())
				return null;
			String[] excludes = storageHook.getLazyStartExcludes();
			String[] includes = storageHook.getLazyStartIncludes();
			if (excludes == null && includes == null)
				return Constants.ACTIVATION_LAZY;
			StringBuffer result = new StringBuffer(Constants.ACTIVATION_LAZY);
			if (excludes != null) {
				result.append(';').append(Constants.EXCLUDE_DIRECTIVE).append(":=\""); //$NON-NLS-1$
				for (int i = 0; i < excludes.length; i++) {
					if (i > 0)
						result.append(',');
					result.append(excludes[i]);
				}
				result.append("\""); //$NON-NLS-1$
			}
			if (includes != null) {
				result.append(';').append(Constants.INCLUDE_DIRECTIVE).append(":=\""); //$NON-NLS-1$
				for (int i = 0; i < includes.length; i++) {
					if (i > 0)
						result.append(',');
					result.append(includes[i]);
				}
				result.append("\""); //$NON-NLS-1$
			}
		}
		if (Constants.ECLIPSE_LAZYSTART.equals(keyString) || Constants.ECLIPSE_AUTOSTART.equals(keyString)) {
			if (!storageHook.isAutoStartable())
				return null;
			if (storageHook.getLazyStartExcludes() == null)
				return Boolean.TRUE.toString();
			StringBuffer result = new StringBuffer(storageHook.isLazyStart() ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
			result.append(";").append(Constants.ECLIPSE_LAZYSTART_EXCEPTIONS).append("=\""); //$NON-NLS-1$ //$NON-NLS-2$
			String[] exceptions = storageHook.getLazyStartExcludes();
			for (int i = 0; i < exceptions.length; i++) {
				if (i > 0)
					result.append(","); //$NON-NLS-1$
				result.append(exceptions[i]);
			}
			result.append("\""); //$NON-NLS-1$
			return result.toString();
		}
		if (Constants.BUNDLE_MANIFESTVERSION.equals(keyString))
			return storageHook.getBundleManifestVersion() == 0 ? null : Integer.toString(storageHook.getBundleManifestVersion());
		if (SERVICE_COMPONENT.equals(keyString))
			return storageHook.getServiceComponent();
		Dictionary<String, String> result = getManifest();
		if (DEBUG)
			System.out.println("Manifest read because of header: " + key); //$NON-NLS-1$
		return result == null ? null : result.get(key);
	}

	public String remove(Object key) {
		return getManifest().remove(key);
	}

	public String put(String key, String value) {
		return getManifest().put(key, value);
	}

}
