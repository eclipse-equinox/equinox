/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.region;

import static org.eclipse.equinox.region.RegionFilter.VISIBLE_OSGI_SERVICE_NAMESPACE;
import static org.eclipse.equinox.region.RegionFilter.VISIBLE_SERVICE_NAMESPACE;

import java.util.*;
import org.eclipse.equinox.region.RegionFilter;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.*;

public final class StandardRegionFilterBuilder implements RegionFilterBuilder {

	private final Object monitor = new Object();

	private final Map<String, Collection<Filter>> policy = new HashMap<String, Collection<Filter>>();

	@SuppressWarnings("deprecation")
	@Override
	public RegionFilterBuilder allow(String namespace, String filter) throws InvalidSyntaxException {
		if (namespace == null)
			throw new IllegalArgumentException("The namespace must not be null."); //$NON-NLS-1$
		if (filter == null)
			throw new IllegalArgumentException("The filter must not be null."); //$NON-NLS-1$
		synchronized (this.monitor) {
			Collection<Filter> namespaceFilters = getNamespaceFilters(namespace);
			namespaceFilters.add(createFilter(filter));
		}
		if (VISIBLE_SERVICE_NAMESPACE.equals(namespace)) {
			// alias the deprecated namespace to osgi.service
			allow(VISIBLE_OSGI_SERVICE_NAMESPACE, filter);
		}
		return this;
	}

	public Filter createFilter(String spec) throws InvalidSyntaxException {
		// TODO need to use BundleContext.createFilter here
		Filter filter = FrameworkUtil.createFilter(spec);
		return (StandardRegionFilter.ALL.equals(filter)) ? StandardRegionFilter.ALL : filter;
	}

	@SuppressWarnings("deprecation")
	@Override
	public RegionFilterBuilder allowAll(String namespace) {
		if (namespace == null)
			throw new IllegalArgumentException("The namespace must not be null."); //$NON-NLS-1$
		synchronized (this.monitor) {
			Collection<Filter> namespaceFilters = getNamespaceFilters(namespace);
			// remove any other filters since this will override them all.
			namespaceFilters.clear();
			namespaceFilters.add(StandardRegionFilter.ALL);
		}
		if (VISIBLE_SERVICE_NAMESPACE.equals(namespace)) {
			// alias the deprecated namespace to osgi.service
			allowAll(VISIBLE_OSGI_SERVICE_NAMESPACE);
		}
		return this;
	}

	private Collection<Filter> getNamespaceFilters(String namespace) {
		Collection<Filter> namespaceFilters = policy.get(namespace);
		if (namespaceFilters == null) {
				// use set to avoid duplicates
				namespaceFilters = new LinkedHashSet<Filter>();
			policy.put(namespace, namespaceFilters);
		}
		return namespaceFilters;
	}

	@Override
	public RegionFilter build() {
		synchronized (this.monitor) {
			return new StandardRegionFilter(policy);
		}
	}
}
