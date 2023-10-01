/*******************************************************************************
 * Copyright (c) 2010, 2013 VMware Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
 * A {@link RegionFilter} is associated with a connection from one region to
 * another and determines the bundles, packages, services and other capabilities
 * which are visible across the connection. A region filter is constant; its
 * sharing policy cannot be changed after construction. Instances of region
 * filters can be created with a {@link RegionFilterBuilder}.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * 
 * Implementations must be thread safe.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
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
	 * Name space for sharing services. The filters specified in this name space
	 * will be used to match {@link ServiceReference services}.
	 * 
	 * @deprecated use the {@link RegionFilter#VISIBLE_OSGI_SERVICE_NAMESPACE
	 *             osgi.service} namespace instead.
	 */
	public static final String VISIBLE_SERVICE_NAMESPACE = "org.eclipse.equinox.allow.service"; //$NON-NLS-1$

	/**
	 * Name space for sharing services. The filters specified in this name space
	 * will be used to match {@link ServiceReference services} as well as
	 * {@code osgi.service} capabilities.
	 */
	public static final String VISIBLE_OSGI_SERVICE_NAMESPACE = "osgi.service"; //$NON-NLS-1$

	/**
	 * Name space for sharing bundles. The filters specified in this name space will
	 * be use to match against a bundle's symbolic name and version. The attributes
	 * {@link Constants#BUNDLE_SYMBOLICNAME_ATTRIBUTE bundle-symbolic-name} and
	 * org.eclipse.equinox.allow.bundle are used for the symbolic name and the
	 * attribute {@link Constants#BUNDLE_VERSION_ATTRIBUTE bundle-version} is used
	 * for the bundle version. This name space will share the complete bundle and
	 * all of its capabilities with another region. This includes bundle events,
	 * services, and all other generic capabilities.
	 */
	public static final String VISIBLE_BUNDLE_NAMESPACE = "org.eclipse.equinox.allow.bundle"; //$NON-NLS-1$

	/**
	 * Name space for sharing bundle lifecycle operations. The filters specified in
	 * this name space will be use to match against a bundle's symbolic name and
	 * version. The attributes {@link Constants#BUNDLE_SYMBOLICNAME_ATTRIBUTE
	 * bundle-symbolic-name} and org.eclipse.equinox.allow.bundle are used for the
	 * symbolic name and the attribute {@link Constants#BUNDLE_VERSION_ATTRIBUTE
	 * bundle-version} is used for the bundle version. This name space is only used
	 * to control the lifecycle layer of the framework. For example, the visible
	 * bundles available from a bundle context, the access to bundle events etc. It
	 * will not share any other capabilities provided by the bundle.
	 */
	public static final String VISIBLE_BUNDLE_LIFECYCLE_NAMESPACE = "org.eclipse.equinox.allow.bundle.lifecycle"; //$NON-NLS-1$

	/**
	 * Name space for matching against all capabilities. The filters specified in
	 * this name space will be used to match all capabilities.
	 */
	public static final String VISIBLE_ALL_NAMESPACE = "org.eclipse.equinox.allow.all"; //$NON-NLS-1$

	/**
	 * An attribute used to hold the namespace for which the attributes belong to
	 * when using the {@link #VISIBLE_ALL_NAMESPACE} namespace to match all
	 * capabilities. This can be useful for excluding some namespaces from the all
	 * namespace. For example,
	 * <q>(!(org.eclipse.equinox.allow.all.namespace=osgi.wiring.package))</q> will
	 * allow everything except package capabilities.
	 */
	public static final String VISIBLE_ALL_NAMESPACE_ATTRIBUTE = "org.eclipse.equinox.allow.all.namespace"; //$NON-NLS-1$

	/**
	 * Determines whether this filter allows the given bundle. A Bundle is allowed
	 * if it successfully matches one or more filters specified using the
	 * {@link RegionFilter#VISIBLE_BUNDLE_NAMESPACE} name space.
	 * 
	 * @param bundle the bundle
	 * @return <code>true</code> if the bundle is allowed and
	 *         <code>false</code>otherwise
	 */
	public boolean isAllowed(Bundle bundle);

	/**
	 * Determines whether this filter allows the given bundle. A Bundle is allowed
	 * if it successfully matches one or more filters specified using the
	 * {@link RegionFilter#VISIBLE_BUNDLE_NAMESPACE} name space.
	 * 
	 * @param bundle the bundle revision
	 * @return <code>true</code> if the bundle is allowed and
	 *         <code>false</code>otherwise
	 */
	public boolean isAllowed(BundleRevision bundle);

	/**
	 * Determines whether this filter allows the given service reference. A service
	 * is allowed if its service properties successfully matches one or more filters
	 * specified using the {@link RegionFilter#VISIBLE_OSGI_SERVICE_NAMESPACE} name
	 * space.
	 * 
	 * @param service the service reference of the service
	 * @return <code>true</code> if the service is allowed and
	 *         <code>false</code>otherwise
	 */
	public boolean isAllowed(ServiceReference<?> service);

	/**
	 * Determines whether this filter allows the given capability. A capability is
	 * allowed if it successfully matches one or more filters specified using the
	 * {@link BundleCapability#getNamespace() name space} of the given capability.
	 * For example, the name spaces {@link RegionFilter#VISIBLE_PACKAGE_NAMESPACE
	 * osgi.wiring.package}, {@link RegionFilter#VISIBLE_HOST_NAMESPACE
	 * osgi.wiring.host}, and {@link RegionFilter#VISIBLE_REQUIRE_NAMESPACE
	 * osgi.wiring.bundle} may be used. Any other generic capability
	 * {@link RegionFilterBuilder#allow(String, String) name spaces} can also be
	 * allowed by the region filter.
	 * 
	 * @param capability the bundle capability
	 * @return <code>true</code> if the capability is allowed and
	 *         <code>false</code>otherwise
	 */
	public boolean isAllowed(BundleCapability capability);

	/**
	 * Determines whether this filter allows the given name space with the given
	 * attributes. The name space can be any generic name space including but not
	 * limited to the following: {@link #VISIBLE_BUNDLE_NAMESPACE},
	 * {@link #VISIBLE_PACKAGE_NAMESPACE}, {@link #VISIBLE_HOST_NAMESPACE},
	 * {@link #VISIBLE_REQUIRE_NAMESPACE} and
	 * {@link #VISIBLE_OSGI_SERVICE_NAMESPACE}. Any other generic capability
	 * {@link RegionFilterBuilder#allow(String, String) name spaces} can also be
	 * allowed by the region filter.
	 * 
	 * @param namespace  the name space
	 * @param attributes the attributes to check if they are allowed
	 * @return <code>true</code> if the name space and attributes are allowed and
	 *         <code>false</code> otherwise
	 */
	public boolean isAllowed(String namespace, Map<String, ?> attributes);

	/**
	 * Returns a map of the filters used by each name space for this region filter.
	 * The may key is the name space and the value is a collection of filters for
	 * the name space. The returned map is a snapshot of the sharing policy. Changes
	 * made to the returned map have no affect on this region filter.
	 * 
	 * @return a map containing the sharing policy used by this region filter
	 */
	public Map<String, Collection<String>> getSharingPolicy();
}
