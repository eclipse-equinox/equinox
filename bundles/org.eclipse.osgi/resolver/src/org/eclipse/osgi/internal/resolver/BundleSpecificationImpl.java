/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
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

import java.util.Map;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.osgi.framework.Constants;

public class BundleSpecificationImpl extends VersionConstraintImpl implements BundleSpecification {
	private boolean exported;
	private boolean optional;
	private Map<String, Object> attributes;

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
		if (mandatory != null) {
			for (String key : mandatory) {
				if (Constants.BUNDLE_VERSION_ATTRIBUTE.equals(key))
					continue; // has a default value of 0.0.0
				if (requiredAttrs == null || requiredAttrs.get(key) == null)
					return false;
			}
		}
		if (getName() != null && getName().equals(candidate.getSymbolicName()) && (getVersionRange() == null || getVersionRange().isIncluded(candidate.getVersion())))
			return true;
		return false;
	}

	public String toString() {
		return "Require-Bundle: " + getName() + "; bundle-version=\"" + getVersionRange() + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
