package org.eclipse.core.runtime.adaptor;

import java.util.Dictionary;
import java.util.Enumeration;
import org.eclipse.osgi.framework.adaptor.Version;
import org.eclipse.osgi.framework.internal.core.Constants;
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
				return null;
			}
		return manifest;
	}

	public int size() {
		return getManifest().size();
	}

	public boolean isEmpty() {
		return false;
	}

	public Enumeration elements() {
		return getManifest().elements();
	}

	public Enumeration keys() {
		return getManifest().keys();
	}

	public Object get(Object key) {
		if (Constants.BUNDLE_VERSION.equalsIgnoreCase((String)key)) {
			Version result = bundledata.getVersion();
			return result == null ? null : result.toString();
		}
		if ("plugin-class".equalsIgnoreCase((String)key))
			return bundledata.getPluginClass();
		if ("legacy".equalsIgnoreCase((String)key))
			return bundledata.isLegacy();
		return getManifest().get(key);
	}

	public Object remove(Object key) {
		return getManifest().remove(key);
	}

	public Object put(Object key, Object value) {
		return getManifest().put(key, value);
	}

}
