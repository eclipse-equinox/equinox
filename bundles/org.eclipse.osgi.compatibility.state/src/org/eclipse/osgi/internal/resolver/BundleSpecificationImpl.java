/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
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

import java.util.*;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleRevision;

public class BundleSpecificationImpl extends VersionConstraintImpl implements BundleSpecification {
	private boolean exported;
	private boolean optional;
	private Map<String, Object> attributes;
	private Map<String, String> arbitraryDirectives;

	protected void setExported(boolean exported) {
		synchronized (this.monitor) {
			this.exported = exported;
		}
	}

	protected void setOptional(boolean optional) {
		synchronized (this.monitor) {
			this.optional = optional;
		}
	}

	public boolean isExported() {
		synchronized (this.monitor) {
			return exported;
		}
	}

	public boolean isOptional() {
		synchronized (this.monitor) {
			return optional;
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

	public boolean isSatisfiedBy(BaseDescription supplier) {
		if (!(supplier instanceof BundleDescriptionImpl))
			return false;
		BundleDescriptionImpl candidate = (BundleDescriptionImpl) supplier;
		if (candidate.getHost() != null)
			return false;
		Map<String, ?> requiredAttrs = getAttributes();
		if (requiredAttrs != null) {
			Map<String, ?> prividerAttrs = candidate.getAttributes();
			if (prividerAttrs == null)
				return false;
			for (String key : requiredAttrs.keySet()) {
				Object requiredValue = requiredAttrs.get(key);
				Object prividedValue = prividerAttrs.get(key);
				if (prividedValue == null || !requiredValue.equals(prividedValue))
					return false;
			}
		}
		String[] mandatory = (String[]) candidate.getDirective(Constants.MANDATORY_DIRECTIVE);
		if (!hasMandatoryAttributes(mandatory))
			return false;
		if (getName() != null && getName().equals(candidate.getSymbolicName()) && (getVersionRange() == null || getVersionRange().isIncluded(candidate.getVersion())))
			return true;
		return false;
	}

	@Override
	protected boolean hasMandatoryAttributes(String[] mandatory) {
		if (mandatory != null) {
			Map<String, ?> requiredAttrs = getAttributes();
			for (String key : mandatory) {
				if (Constants.BUNDLE_VERSION_ATTRIBUTE.equals(key))
					continue; // has a default value of 0.0.0
				if (requiredAttrs == null || requiredAttrs.get(key) == null)
					return false;
			}
		}
		return true;
	}

	public String toString() {
		return "Require-Bundle: " + getName() + "; bundle-version=\"" + getVersionRange() + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	protected Map<String, String> getInternalDirectives() {
		Map<String, String> result = new HashMap<String, String>(2);
		synchronized (this.monitor) {
			if (arbitraryDirectives != null)
				result.putAll(arbitraryDirectives);
			if (exported)
				result.put(Constants.VISIBILITY_DIRECTIVE, Constants.VISIBILITY_REEXPORT);
			if (optional)
				result.put(Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL);
			result.put(Constants.FILTER_DIRECTIVE, createFilterDirective());
			return result;
		}
	}

	private String createFilterDirective() {
		StringBuffer filter = new StringBuffer();
		filter.append("(&"); //$NON-NLS-1$
		synchronized (this.monitor) {
			addFilterAttribute(filter, BundleRevision.BUNDLE_NAMESPACE, getName());
			VersionRange range = getVersionRange();
			if (range != null && range != VersionRange.emptyRange)
				addFilterAttribute(filter, Constants.BUNDLE_VERSION_ATTRIBUTE, range);
			if (attributes != null)
				addFilterAttributes(filter, attributes);
		}
		filter.append(')');
		return filter.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Map<String, Object> getInteralAttributes() {
		return Collections.EMPTY_MAP;
	}

	@Override
	protected String getInternalNameSpace() {
		return BundleRevision.BUNDLE_NAMESPACE;
	}
}
