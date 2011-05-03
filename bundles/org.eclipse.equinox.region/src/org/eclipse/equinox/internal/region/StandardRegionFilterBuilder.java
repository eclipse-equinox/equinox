/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.region;

import org.eclipse.equinox.region.RegionFilter;
import org.eclipse.equinox.region.RegionFilterBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

final class StandardRegionFilterBuilder implements RegionFilterBuilder {

	private final static String ALL_SPEC = "(|(!(all=*))(all=*))"; //$NON-NLS-1$

	private final static Filter ALL;
	static {
		try {
			ALL = FrameworkUtil.createFilter(ALL_SPEC);
		} catch (InvalidSyntaxException e) {
			// should never happen!
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private final Object monitor = new Object();

	private final Map<String, Collection<Filter>> policy = new HashMap<String, Collection<Filter>>();

	public RegionFilterBuilder allow(String namespace, String filter) throws InvalidSyntaxException {
		if (namespace == null)
			throw new IllegalArgumentException("The namespace must not be null."); //$NON-NLS-1$
		if (filter == null)
			throw new IllegalArgumentException("The filter must not be null."); //$NON-NLS-1$
		synchronized (this.monitor) {
			Collection<Filter> namespaceFilters = policy.get(namespace);
			if (namespaceFilters == null) {
				namespaceFilters = new ArrayList<Filter>();
				policy.put(namespace, namespaceFilters);
			}
			// TODO need to use BundleContext.createFilter here
			namespaceFilters.add(FrameworkUtil.createFilter(filter));
		}
		return this;
	}

	public RegionFilterBuilder allowAll(String namespace) {
		if (namespace == null)
			throw new IllegalArgumentException("The namespace must not be null."); //$NON-NLS-1$
		synchronized (this.monitor) {
			Collection<Filter> namespaceFilters = policy.get(namespace);
			if (namespaceFilters == null) {
				namespaceFilters = new ArrayList<Filter>();
				policy.put(namespace, namespaceFilters);
			}
			// remove any other filters since this will override them all.
			namespaceFilters.clear();
			namespaceFilters.add(ALL);
		}
		return this;
	}

	public RegionFilter build() {
		synchronized (this.monitor) {
			return new StandardRegionFilter(policy);
		}
	}
}
