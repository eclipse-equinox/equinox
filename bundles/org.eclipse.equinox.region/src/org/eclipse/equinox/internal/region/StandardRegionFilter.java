/*******************************************************************************
 * Copyright (c) 2011 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.equinox.internal.region;

import java.util.*;
import org.eclipse.equinox.region.RegionFilter;
import org.osgi.framework.*;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;

final class StandardRegionFilter implements RegionFilter {
	private static final String BUNDLE_ID_ATTR = "id"; //$NON-NLS-1$
	private final Map<String, Collection<Filter>> filters;

	StandardRegionFilter(Map<String, Collection<Filter>> filters) {
		if (filters == null) {
			throw new IllegalArgumentException("filters must not be null."); //$NON-NLS-1$
		}
		// must perform deep copy to avoid external changes
		this.filters = new HashMap<String, Collection<Filter>>((int) ((filters.size() / 0.75) + 1));
		for (Map.Entry<String, Collection<Filter>> namespace : filters.entrySet()) {
			Collection<Filter> namespaceFilters = new ArrayList<Filter>(namespace.getValue());
			this.filters.put(namespace.getKey(), namespaceFilters);
		}
	}

	/**
	 * Determines whether this filter allows the given bundle
	 * 
	 * @param bundle the bundle
	 * @return <code>true</code> if the bundle is allowed and <code>false</code>otherwise
	 */
	public boolean isAllowed(Bundle bundle) {
		HashMap<String, Object> attrs = new HashMap<String, Object>(3);
		String bsn = bundle.getSymbolicName();
		if (bsn != null) {
			// TODO the javadoc says to use the bundle-symbolic-name attribute; leaving this for behavior compatibility
			attrs.put(VISIBLE_BUNDLE_NAMESPACE, bsn);
			// This is the correct attribute to use according to the javadoc
			attrs.put(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, bsn);
		}
		attrs.put(org.osgi.framework.Constants.BUNDLE_VERSION_ATTRIBUTE, bundle.getVersion());
		attrs.put(BUNDLE_ID_ATTR, bundle.getBundleId());
		return isBundleAllowed(attrs);
	}

	/**
	 * Determines whether this filter allows the given bundle
	 * 
	 * @param bundle the bundle revision
	 * @return <code>true</code> if the bundle is allowed and <code>false</code>otherwise
	 */
	public boolean isAllowed(BundleRevision bundle) {
		HashMap<String, Object> attrs = new HashMap<String, Object>(3);
		String bsn = bundle.getSymbolicName();
		if (bsn != null)
			attrs.put(VISIBLE_BUNDLE_NAMESPACE, bsn);
		attrs.put(org.osgi.framework.Constants.BUNDLE_VERSION_ATTRIBUTE, bundle.getVersion());
		attrs.put(BUNDLE_ID_ATTR, getBundleId(bundle));
		return isBundleAllowed(attrs);
	}

	/**
	 * Determines whether this filter allows the bundle with the given attributes
	 * 
	 * @param bundleAttributes the bundle attributes
	 * @return <code>true</code> if the bundle is allowed and <code>false</code>otherwise
	 */
	private boolean isBundleAllowed(Map<String, ?> bundleAttributes) {
		if (match(filters.get(VISIBLE_BUNDLE_NAMESPACE), bundleAttributes))
			return true;
		return match(filters.get(VISIBLE_ALL_NAMESPACE), bundleAttributes);
	}

	private static boolean match(Collection<Filter> filters, Map<String, ?> attrs) {
		if (filters == null)
			return false;
		for (Filter filter : filters) {
			if (filter.matches(attrs))
				return true;
		}
		return false;
	}

	private static boolean match(Collection<Filter> filters, ServiceReference<?> service) {
		if (filters == null)
			return false;
		for (Filter filter : filters) {
			if (filter.match(service))
				return true;
		}
		return false;
	}

	/**
	 * Determines whether this filter allows the given service reference.
	 * 
	 * @param service the service reference of the service
	 * @return <code>true</code> if the service is allowed and <code>false</code>otherwise
	 */
	public boolean isAllowed(ServiceReference<?> service) {
		if (match(filters.get(VISIBLE_SERVICE_NAMESPACE), service))
			return true;
		return match(filters.get(VISIBLE_ALL_NAMESPACE), service);
	}

	/**
	 * Determines whether this filter allows the given capability.
	 * 
	 * @param capability the bundle capability
	 * @return <code>true</code> if the capability is allowed and <code>false</code>otherwise
	 */
	public boolean isAllowed(BundleCapability capability) {
		String namespace = capability.getNamespace();
		Map<String, ?> attrs = capability.getAttributes();
		if (match(filters.get(namespace), attrs))
			return true;
		return match(filters.get(VISIBLE_ALL_NAMESPACE), attrs);
	}

	public Map<String, Collection<String>> getSharingPolicy() {
		Map<String, Collection<String>> result = new HashMap<String, Collection<String>>((int) ((filters.size() / 0.75) + 1));
		for (Map.Entry<String, Collection<Filter>> namespace : filters.entrySet()) {
			result.put(namespace.getKey(), getFilters(namespace.getValue()));
		}
		return result;
	}

	private static Collection<String> getFilters(Collection<Filter> filters) {
		Collection<String> result = new ArrayList<String>(filters.size());
		for (Filter filter : filters) {
			result.add(filter.toString());
		}
		return result;
	}

	public String toString() {
		return getSharingPolicy().toString();
	}

	private Long getBundleId(BundleRevision bundleRevision) {
		return EquinoxStateHelper.getBundleId(bundleRevision);
	}
}
