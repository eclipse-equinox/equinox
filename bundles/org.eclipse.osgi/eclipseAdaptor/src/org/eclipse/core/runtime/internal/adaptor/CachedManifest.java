/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
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
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;

/**
 * Internal class.
 */
public class CachedManifest extends Dictionary {

	private Dictionary manifest = null;
	private EclipseStorageHook storageHook;

	public CachedManifest(EclipseStorageHook storageHook) {
		this.storageHook = storageHook;
	}

	public Dictionary getManifest() {
		if (manifest == null)
			try {
				manifest = storageHook.createCachedManifest(true);
			} catch (BundleException e) {
				final String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CACHEDMANIFEST_UNEXPECTED_EXCEPTION, storageHook.getBaseData().getLocation());
				FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, message, 0, e, null);
				storageHook.getAdaptor().getFrameworkLog().log(entry);
				return null;
			}
		return manifest;
	}

	public int size() {
		//TODO: getManifest may return null
		return getManifest().size();
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public Enumeration elements() {
		//TODO: getManifest may return null		
		return getManifest().elements();
	}

	public Enumeration keys() {
		//TODO: getManifest may return null		
		return getManifest().keys();
	}

	public Object get(Object key) {
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
		Dictionary result = getManifest();
		return result == null ? null : result.get(key);
	}

	public Object remove(Object key) {
		//TODO: getManifest may return null		
		return getManifest().remove(key);
	}

	public Object put(Object key, Object value) {
		//TODO: getManifest may return null		
		return getManifest().put(key, value);
	}

}
