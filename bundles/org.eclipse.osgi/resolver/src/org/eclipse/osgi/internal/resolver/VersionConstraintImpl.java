/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Danail Nachev -  ProSyst - bug 218625
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.util.Collections;
import java.util.Map;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.internal.resolver.BaseDescriptionImpl.BaseCapability;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.*;

abstract class VersionConstraintImpl implements VersionConstraint {

	protected final Object monitor = new Object();

	private String name;
	private VersionRange versionRange;
	private BundleDescription bundle;
	private BaseDescription supplier;

	public String getName() {
		synchronized (this.monitor) {
			if (Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(name)) {
				StateImpl state = (StateImpl) getBundle().getContainingState();
				return state == null ? Constants.getInternalSymbolicName() : state.getSystemBundle();
			}
			return name;
		}
	}

	public VersionRange getVersionRange() {
		synchronized (this.monitor) {
			if (versionRange == null)
				return VersionRange.emptyRange;
			return versionRange;
		}
	}

	public BundleDescription getBundle() {
		synchronized (this.monitor) {
			return bundle;
		}
	}

	public boolean isResolved() {
		synchronized (this.monitor) {
			return supplier != null;
		}
	}

	public BaseDescription getSupplier() {
		synchronized (this.monitor) {
			return supplier;
		}
	}

	public boolean isSatisfiedBy(BaseDescription candidate) {
		synchronized (this.monitor) {
			return false;
		}
	}

	protected void setName(String name) {
		synchronized (this.monitor) {
			this.name = name;
		}
	}

	protected void setVersionRange(VersionRange versionRange) {
		synchronized (this.monitor) {
			this.versionRange = versionRange;
		}
	}

	protected void setBundle(BundleDescription bundle) {
		synchronized (this.monitor) {
			this.bundle = bundle;
		}
	}

	protected void setSupplier(BaseDescription supplier) {
		synchronized (this.monitor) {
			this.supplier = supplier;
		}
	}

	protected abstract String getInternalNameSpace();

	protected abstract Map<String, String> getInternalDirectives();

	protected abstract Map<String, Object> getInteralAttributes();

	public BundleRequirement getRequirement() {
		String namespace = getInternalNameSpace();
		if (namespace == null)
			return null;
		return new BundleRequirementImpl(namespace);
	}

	class BundleRequirementImpl implements BundleRequirement {
		private final String namespace;

		public BundleRequirementImpl(String namespace) {
			this.namespace = namespace;
		}

		public String getNamespace() {
			return namespace;
		}

		@SuppressWarnings("unchecked")
		public Map<String, String> getDirectives() {
			return Collections.unmodifiableMap(getInternalDirectives());
		}

		@SuppressWarnings("unchecked")
		public Map<String, Object> getAttributes() {
			return Collections.unmodifiableMap(getInteralAttributes());
		}

		public BundleRevision getRevision() {
			return getBundle();
		}

		public boolean matches(BundleCapability capability) {
			return isSatisfiedBy(((BaseCapability) capability).getBaseDescription());
		}

		public int hashCode() {
			return System.identityHashCode(VersionConstraintImpl.this);
		}

		private VersionConstraintImpl getVersionConstraint() {
			return VersionConstraintImpl.this;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof BundleRequirementImpl))
				return false;
			return ((BundleRequirementImpl) obj).getVersionConstraint() == VersionConstraintImpl.this;
		}

		public String toString() {
			return getNamespace() + BaseDescriptionImpl.toString(getAttributes(), false);
		}
	}

	static StringBuffer addFilterAttributes(StringBuffer filter, Map<String, ?> attributes) {
		for (Map.Entry<String, ?> entry : attributes.entrySet()) {
			addFilterAttribute(filter, entry.getKey(), entry.getValue());
		}
		return filter;
	}

	static StringBuffer addFilterAttribute(StringBuffer filter, String attr, Object value) {
		return addFilterAttribute(filter, attr, value, true);
	}

	static private final Version MAX_VERSION = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

	// TODO this is coupled to the implementation detail of version range for open range check
	// TODO we need to create a new method on VersionRange to get a filter string and likely should add a FilterBuilder.
	static StringBuffer addFilterAttribute(StringBuffer filter, String attr, Object value, boolean escapeWildCard) {
		if (value instanceof VersionRange) {
			VersionRange range = (VersionRange) value;
			if (range.getIncludeMinimum()) {
				filter.append('(').append(attr).append(">=").append(escapeValue(range.getMinimum(), escapeWildCard)).append(')'); //$NON-NLS-1$
			} else {
				filter.append("(!(").append(attr).append("<=").append(escapeValue(range.getMinimum(), escapeWildCard)).append("))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			// only include the maximum check if this is not an open range
			// this check is a bit hacky because we have no method on VersionRange to check if the range really is open
			if (!(MAX_VERSION.equals(range.getMaximum()) && range.getIncludeMaximum())) {
				if (range.getIncludeMaximum()) {
					filter.append('(').append(attr).append("<=").append(escapeValue(range.getMaximum(), escapeWildCard)).append(')'); //$NON-NLS-1$
				} else {
					filter.append("(!(").append(attr).append(">=").append(escapeValue(range.getMaximum(), escapeWildCard)).append("))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
		} else {
			filter.append('(').append(attr).append('=').append(escapeValue(value, escapeWildCard)).append(')');
		}
		return filter;
	}

	private static String escapeValue(Object o, boolean escapeWildCard) {
		String value = o.toString();
		boolean escaped = false;
		int inlen = value.length();
		int outlen = inlen << 1; /* inlen * 2 */

		char[] output = new char[outlen];
		value.getChars(0, inlen, output, inlen);

		int cursor = 0;
		for (int i = inlen; i < outlen; i++) {
			char c = output[i];
			switch (c) {
				case '*' :
					if (!escapeWildCard)
						break;
				case '\\' :
				case '(' :
				case ')' :
					output[cursor] = '\\';
					cursor++;
					escaped = true;
					break;
			}

			output[cursor] = c;
			cursor++;
		}

		return escaped ? new String(output, 0, cursor) : value;
	}
}
