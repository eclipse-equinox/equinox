/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import org.eclipse.osgi.framework.internal.core.FilterImpl;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

public class GenericSpecificationImpl extends VersionConstraintImpl implements GenericSpecification {
	private Filter matchingFilter;
	private String type = GenericDescription.DEFAULT_TYPE;
	private int resolution = 0;
	private GenericDescription[] suppliers;

	public String getMatchingFilter() {
		synchronized (this.monitor) {
			return matchingFilter == null ? null : matchingFilter.toString();
		}
	}

	void setMatchingFilter(String matchingFilter) throws InvalidSyntaxException {
		synchronized (this.monitor) {
			this.matchingFilter = matchingFilter == null ? null : FilterImpl.newInstance(matchingFilter);
		}
	}

	public boolean isSatisfiedBy(BaseDescription supplier) {
		if (!(supplier instanceof GenericDescription))
			return false;
		GenericDescription candidate = (GenericDescription) supplier;
		if (getName() == null || !getName().equals(candidate.getName()))
			return false;
		if (getType() == null || !getType().equals(candidate.getType()))
			return false;
		// Note that versions are only matched by including them in the filter
		return matchingFilter == null || matchingFilter.match(candidate.getAttributes());
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(StateBuilder.GENERIC_REQUIRE).append(": ").append(getName()); //$NON-NLS-1$
		if (getType() != GenericDescription.DEFAULT_TYPE)
			sb.append(':').append(getType());
		if (matchingFilter != null)
			sb.append("; ").append(getMatchingFilter()); //$NON-NLS-1$
		return sb.toString();
	}

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

	public int getResolution() {
		synchronized (this.monitor) {
			return resolution;
		}
	}

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

	public BaseDescription getSupplier() {
		synchronized (this.monitor) {
			return suppliers == null || suppliers.length == 0 ? null : suppliers[0];
		}
	}

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
}
