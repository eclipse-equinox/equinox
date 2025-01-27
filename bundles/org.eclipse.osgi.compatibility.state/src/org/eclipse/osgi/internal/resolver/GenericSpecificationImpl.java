/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
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
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.GenericDescription;
import org.eclipse.osgi.service.resolver.GenericSpecification;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.resource.Namespace;

public class GenericSpecificationImpl extends VersionConstraintImpl implements GenericSpecification {
	private Filter matchingFilter;
	private String type = GenericDescription.DEFAULT_TYPE;
	private int resolution = 0;
	private GenericDescription[] suppliers;
	private Map<String, Object> attributes;
	private Map<String, String> arbitraryDirectives;
	/*
	 * Indicates that a generic constraint was from converting the BREE header
	 */
	public static final int RESOLUTION_FROM_BREE = 0x04;

	@Override
	public String getMatchingFilter() {
		synchronized (this.monitor) {
			return matchingFilter == null ? null : matchingFilter.toString();
		}
	}

	void setMatchingFilter(String matchingFilter, boolean matchName) throws InvalidSyntaxException {
		synchronized (this.monitor) {
			String name = getName();
			if (matchName && name != null && !"*".equals(name)) { //$NON-NLS-1$
				String nameFilter = "(" + getType() + "=" + getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				matchingFilter = matchingFilter == null ? nameFilter : "(&" + nameFilter + matchingFilter + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			this.matchingFilter = matchingFilter == null ? null : FilterImpl.newInstance(matchingFilter);
		}
	}

	void setMatchingFilter(Filter matchingFilter) {
		synchronized (this.monitor) {
			this.matchingFilter = matchingFilter;
		}
	}

	@Override
	public boolean isSatisfiedBy(BaseDescription supplier) {
		if (!(supplier instanceof GenericDescription))
			return false;
		GenericDescription candidate = (GenericDescription) supplier;
		if (!getType().equals(candidate.getType()))
			return false;
		// Note that names and versions are only matched by including them in the filter
		return matchingFilter == null || matchingFilter.match(candidate.getAttributes());
	}

	@Override
	protected boolean hasMandatoryAttributes(String[] mandatory) {
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Constants.REQUIRE_CAPABILITY).append(": ").append(getType()); //$NON-NLS-1$
		if (matchingFilter != null)
			sb.append("; filter=\"").append(getMatchingFilter()).append('"'); //$NON-NLS-1$
		return sb.toString();
	}

	@Override
	public String getType() {
		synchronized (this.monitor) {
			return type;
		}
	}

	void setType(String type) {
		synchronized (this.monitor) {
			if (type == null || type.equals(GenericDescription.DEFAULT_TYPE))
				this.type = GenericDescription.DEFAULT_TYPE;
			else
				this.type = type;
		}
	}

	@Override
	public int getResolution() {
		synchronized (this.monitor) {
			return resolution;
		}
	}

	@Override
	public boolean isResolved() {
		synchronized (this.monitor) {
			return suppliers != null && suppliers.length > 0;
		}
	}

	void setResolution(int resolution) {
		synchronized (this.monitor) {
			this.resolution = resolution;
		}
	}

	@Override
	public BaseDescription getSupplier() {
		synchronized (this.monitor) {
			return suppliers == null || suppliers.length == 0 ? null : suppliers[0];
		}
	}

	@Override
	protected void setSupplier(BaseDescription supplier) {
		synchronized (this.monitor) {
			if (supplier == null) {
				suppliers = null;
				return;
			}
			int len = suppliers == null ? 0 : suppliers.length;
			GenericDescription[] temp = new GenericDescription[len + 1];
			if (suppliers != null)
				System.arraycopy(suppliers, 0, temp, 0, len);
			temp[len] = (GenericDescription) supplier;
			suppliers = temp;
		}
	}

	@Override
	public GenericDescription[] getSuppliers() {
		synchronized (this.monitor) {
			return suppliers;
		}
	}

	void setSupplers(GenericDescription[] suppliers) {
		synchronized (this.monitor) {
			this.suppliers = suppliers;
		}
	}

	Map<String, Object> getAttributes() {
		synchronized (this.monitor) {
			return attributes;
		}
	}

	@SuppressWarnings("unchecked")
	void setAttributes(Map<String, ?> attributes) {
		synchronized (this.monitor) {
			this.attributes = (Map<String, Object>) attributes;
		}
	}

	Map<String, String> getArbitraryDirectives() {
		synchronized (this.monitor) {
			return arbitraryDirectives;
		}
	}

	@SuppressWarnings("unchecked")
	void setArbitraryDirectives(Map<String, ?> directives) {
		synchronized (this.monitor) {
			this.arbitraryDirectives = (Map<String, String>) directives;
		}
	}

	@Override
	protected Map<String, String> getInternalDirectives() {
		Map<String, String> result = new HashMap<>(2);
		synchronized (this.monitor) {
			if (arbitraryDirectives != null)
				result.putAll(arbitraryDirectives);
			if ((resolution & GenericSpecification.RESOLUTION_OPTIONAL) != 0)
				result.put(Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL);
			if ((resolution & GenericSpecification.RESOLUTION_MULTIPLE) != 0)
				result.put(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE, Namespace.CARDINALITY_MULTIPLE);
			if (matchingFilter != null) {
				result.put(Constants.FILTER_DIRECTIVE, matchingFilter.toString());
			}
		}
		return result;
	}

	@Override
	protected Map<String, Object> getInteralAttributes() {
		synchronized (this.monitor) {
			return attributes == null ? Collections.emptyMap() : new HashMap<>(attributes);
		}
	}

	@Override
	protected String getInternalNameSpace() {
		return getType();
	}
}
