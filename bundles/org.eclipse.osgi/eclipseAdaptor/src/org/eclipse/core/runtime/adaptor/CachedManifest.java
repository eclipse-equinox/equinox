/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import java.util.Dictionary;
import java.util.Enumeration;
import org.eclipse.osgi.service.resolver.Version;
import org.osgi.framework.BundleException;

public class CachedManifest extends Dictionary {

	Dictionary manifest = null;
	EclipseBundleData bundledata;
	
	public CachedManifest(EclipseBundleData bundledata) {
		this.bundledata = bundledata;
	}
	
	protected Dictionary getManifest() {
		if (manifest == null)
			try {
				manifest = bundledata.loadManifest();
			} catch (BundleException e) {
				//TODO: this needs to be logged
				return null;
			}
		return manifest;
	}

	public int size() {
		//TODO: getManifest may return null
		return getManifest().size();
	}

	public boolean isEmpty() {
		return false;
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
		if (org.osgi.framework.Constants.BUNDLE_VERSION.equalsIgnoreCase(keyString)) {
			Version result = bundledata.getVersion();
			return result == null ? null : result.toString();
		}
		if (EclipseAdaptorConstants.ECLIPSE_AUTOSTART.equalsIgnoreCase(keyString))
			return bundledata.getAutoStart();
		if (EclipseAdaptorConstants.ECLIPSE_AUTOSTOP.equalsIgnoreCase(keyString))
			return bundledata.getAutoStop();		
		if (EclipseAdaptorConstants.PLUGIN_CLASS.equalsIgnoreCase(keyString))
			return bundledata.getPluginClass();
		if (EclipseAdaptorConstants.LEGACY.equalsIgnoreCase(keyString))
			return bundledata.isLegacy();
		if (org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME.equalsIgnoreCase(keyString))
			return bundledata.getSymbolicName();
		//TODO: getManifest may return null. 
		//TODO This can only happen if the converter is not around.
		return getManifest().get(key);
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
