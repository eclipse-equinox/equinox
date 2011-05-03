/*******************************************************************************
 * Copyright (c) 2010, 2011 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SpringSource, a division of VMware - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.equinox.region;

import java.util.Collection;
import java.util.Map;
import org.osgi.framework.*;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;

/**
 * A {@link RegionFilter} is associated with a connection from one region to another and determines the bundles,
 * packages, services and other capabilities which are visible across the connection. A region filter is constant; its
 * sharing policy cannot be changed after construction. Instances of region filters can be created with a
 * {@link RegionFilterBuilder}.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * 
 * Implementations must be thread safe.
 * 
 */
public interface RegionFilter {

	/**
	 * Name space for sharing package capabilities.
	 * 
	 * @see BundleRevision#PACKAGE_NAMESPACE
	 */
	public static final String VISIBLE_PACKAGE_NAMESPACE = BundleRevision.PACKAGE_NAMESPACE;

	/**
	 * Name space for sharing bundle capabilities for require bundle constraints.
	 * 
	 * @see BundleRevision#BUNDLE_NAMESPACE
	 */
	public static final String VISIBLE_REQUIRE_NAMESPACE = BundleRevision.BUNDLE_NAMESPACE;

	/**
	 * Name space for sharing host capabilities.
	 * 
	 * @see BundleRevision#HOST_NAMESPACE
	 */
	public static final String VISIBLE_HOST_NAMESPACE = BundleRevision.HOST_NAMESPACE;

	/**
	 * Name space for sharing services. The filters specified in this name space will be used to match
	 * {@link ServiceReference services}.
	 */
	public static final String VISIBLE_SERVICE_NAMESPACE = "org.eclipse.equinox.allow.service"; //$NON-NLS-1$

	/**
	 * Name space for sharing bundles. The filters specified in this name space will be use to match against a bundle's
	 * symbolic name and version. The attribute {@link Constants#BUNDLE_SYMBOLICNAME_ATTRIBUTE bundle-symbolic-name} is
	 * used for the symbolic name and the attribute {@link Constants#BUNDLE_VERSION_ATTRIBUTE bundle-version} is used
	 * for the bundle version.
	 */
	public static final String VISIBLE_BUNDLE_NAMESPACE = "org.eclipse.equinox.allow.bundle"; //$NON-NLS-1$

	/**
	 * Name space for matching against all capabilities. The filters specified in this name space will be used to match
	 * all capabilities.
	 */
	public static final String VISIBLE_ALL_NAMESPACE = "org.eclipse.equinox.allow.all"; //$NON-NLS-1$

	/**
	 * Determines whether this filter allows the given bundle
	 * 
	 * @param bundle the bundle
	 * @return <code>true</code> if the bundle is allowed and <code>false</code>otherwise
	 */
	public boolean isAllowed(Bundle bundle);

	/**
	 * Determines whether this filter allows the given bundle
	 * 
	 * @param bundle the bundle revision
	 * @return <code>true</code> if the bundle is allowed and <code>false</code>otherwise
	 */
	public boolean isAllowed(BundleRevision bundle);

	/**
	 * Determines whether this filter allows the given service reference.
	 * 
	 * @param service the service reference of the service
	 * @return <code>true</code> if the service is allowed and <code>false</code>otherwise
	 */
	public boolean isAllowed(ServiceReference<?> service);

	/**
	 * Determines whether this filter allows the given capability.
	 * 
	 * @param capability the bundle capability
	 * @return <code>true</code> if the capability is allowed and <code>false</code>otherwise
	 */
	public boolean isAllowed(BundleCapability capability);

	/**
	 * Returns a map of the filters used by each name space for this region filter. The may key is the name space and
	 * the value is a collection of filters for the name space. The returned map is a snapshot of the sharing policy.
	 * Changes made to the returned map have no affect on this region filter.
	 * 
	 * @return a map containing the sharing policy used by this region filter
	 */
	public Map<String, Collection<String>> getSharingPolicy();
}
