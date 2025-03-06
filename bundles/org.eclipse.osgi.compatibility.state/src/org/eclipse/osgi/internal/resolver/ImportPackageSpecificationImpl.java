/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
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
 *     Danail Nachev -  ProSyst - bug 218625
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleRevision;

public class ImportPackageSpecificationImpl extends VersionConstraintImpl implements ImportPackageSpecification {
	private String resolution = ImportPackageSpecification.RESOLUTION_STATIC; // the default is static
	private String symbolicName;
	private VersionRange bundleVersionRange;
	private Map<String, Object> attributes;
	private Map<String, String> arbitraryDirectives;

	@Override
	public Map<String, Object> getDirectives() {
		synchronized (this.monitor) {
			Map<String, Object> result = new HashMap<>(5);
			if (resolution != null) {
				result.put(Constants.RESOLUTION_DIRECTIVE, resolution);
			}
			return result;
		}
	}

	@Override
	public Object getDirective(String key) {
		synchronized (this.monitor) {
			if (key.equals(Constants.RESOLUTION_DIRECTIVE)) {
				return resolution;
			}
			return null;
		}
	}

	Object setDirective(String key, Object value) {
		synchronized (this.monitor) {
			if (key.equals(Constants.RESOLUTION_DIRECTIVE)) {
				return resolution = (String) value;
			}
			return null;
		}
	}

	void setDirectives(Map<String, ?> directives) {
		synchronized (this.monitor) {
			if (directives == null) {
				return;
			}
			resolution = (String) directives.get(Constants.RESOLUTION_DIRECTIVE);
		}
	}

	@SuppressWarnings("unchecked")
	void setArbitraryDirectives(Map<String, ?> directives) {
		synchronized (this.monitor) {
			this.arbitraryDirectives = (Map<String, String>) directives;
		}
	}

	Map<String, String> getArbitraryDirectives() {
		synchronized (this.monitor) {
			return arbitraryDirectives;
		}
	}

	@Override
	public String getBundleSymbolicName() {
		synchronized (this.monitor) {
			if (Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(symbolicName)) {
				StateImpl state = (StateImpl) getBundle().getContainingState();
				return state == null ? EquinoxContainer.NAME : state.getSystemBundle();
			}
			return symbolicName;
		}
	}

	@Override
	public VersionRange getBundleVersionRange() {
		synchronized (this.monitor) {
			if (bundleVersionRange == null) {
				return VersionRange.emptyRange;
			}
			return bundleVersionRange;
		}
	}

	@Override
	public Map<String, Object> getAttributes() {
		synchronized (this.monitor) {
			return attributes;
		}
	}

	@Override
	public boolean isSatisfiedBy(BaseDescription supplier) {
		return isSatisfiedBy(supplier, true);
	}

	public boolean isSatisfiedBy(BaseDescription supplier, boolean checkEE) {
		if (!(supplier instanceof ExportPackageDescription)) {
			return false;
		}
		ExportPackageDescriptionImpl pkgDes = (ExportPackageDescriptionImpl) supplier;

		// If we are in strict mode, check to see if the export specifies friends.
		// If it does, are we one of the friends
		String[] friends = (String[]) pkgDes.getDirective(StateImpl.FRIENDS_DIRECTIVE);
		Boolean internal = (Boolean) pkgDes.getDirective(StateImpl.INTERNAL_DIRECTIVE);
		if (internal.booleanValue() || friends != null) {
			StateImpl state = (StateImpl) getBundle().getContainingState();
			boolean strict = state == null ? false : state.inStrictMode();
			if (strict) {
				if (internal.booleanValue()) {
					return false;
				}
				boolean found = false;
				if (friends != null && getBundle().getSymbolicName() != null) {
					for (String friend : friends) {
						if (getBundle().getSymbolicName().equals(friend)) {
							found = true;
						}
					}
				}
				if (!found) {
					return false;
				}
			}
		}
		String exporterSymbolicName = getBundleSymbolicName();
		if (exporterSymbolicName != null) {
			BundleDescription exporter = pkgDes.getExporter();
			if (!exporterSymbolicName.equals(exporter.getSymbolicName())) {
				return false;
			}
			if (getBundleVersionRange() != null && !getBundleVersionRange().isIncluded(exporter.getVersion())) {
				return false;
			}
		}

		String name = getName();
		// shortcut '*'
		// NOTE: wildcards are supported only in cases where this is a dynamic import
		if (!"*".equals(name) && !(name.endsWith(".*") && pkgDes.getName().startsWith(name.substring(0, name.length() - 1))) && !pkgDes.getName().equals(name)) { //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
		if (getVersionRange() != null && !getVersionRange().isIncluded(pkgDes.getVersion())) {
			return false;
		}

		Map<String, ?> importAttrs = getAttributes();
		if (importAttrs != null) {
			Map<String, ?> exportAttrs = pkgDes.getAttributes();
			if (exportAttrs == null) {
				return false;
			}
			for (String importKey : importAttrs.keySet()) {
				Object importValue = importAttrs.get(importKey);
				Object exportValue = exportAttrs.get(importKey);
				if (exportValue == null || !importValue.equals(exportValue)) {
					return false;
				}
			}
		}
		String[] mandatory = (String[]) pkgDes.getDirective(Constants.MANDATORY_DIRECTIVE);
		if (!hasMandatoryAttributes(mandatory)) {
			return false;
		}
		// finally check the ee index
		if (!checkEE) {
			return true;
		}
		if (((BundleDescriptionImpl) getBundle()).getEquinoxEE() < 0) {
			return true;
		}
		int eeIndex = ((Integer) pkgDes.getDirective(ExportPackageDescriptionImpl.EQUINOX_EE)).intValue();
		return eeIndex < 0 || eeIndex == ((BundleDescriptionImpl) getBundle()).getEquinoxEE();
	}

	@Override
	protected boolean hasMandatoryAttributes(String[] checkMandatory) {
		if (checkMandatory != null) {
			Map<String, ?> importAttrs = getAttributes();
			for (String mandatory : checkMandatory) {
				if (Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE.equals(mandatory)) {
					if (getBundleSymbolicName() == null) {
						return false;
					}
				} else if (Constants.BUNDLE_VERSION_ATTRIBUTE.equals(mandatory)) {
					if (bundleVersionRange == null) {
						return false;
					}
				} else if (Constants.PACKAGE_SPECIFICATION_VERSION.equals(mandatory) || Constants.VERSION_ATTRIBUTE.equals(mandatory)) {
					if (getVersionRange() == null) {
						return false;
					}
				} else { // arbitrary attribute
					if (importAttrs == null) {
						return false;
					}
					if (importAttrs.get(mandatory) == null) {
						return false;
					}
				}
			}
		}
		return true;
	}

	protected void setBundleSymbolicName(String symbolicName) {
		synchronized (this.monitor) {
			this.symbolicName = symbolicName;
		}
	}

	protected void setBundleVersionRange(VersionRange bundleVersionRange) {
		synchronized (this.monitor) {
			this.bundleVersionRange = bundleVersionRange;
		}
	}

	@SuppressWarnings("unchecked")
	protected void setAttributes(Map<String, ?> attributes) {
		synchronized (this.monitor) {
			this.attributes = (Map<String, Object>) attributes;
		}
	}

	@Override
	public String toString() {
		return "Import-Package: " + getName() + "; version=\"" + getVersionRange() + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	protected Map<String, String> getInternalDirectives() {
		synchronized (this.monitor) {
			Map<String, String> result = new HashMap<>(5);
			if (arbitraryDirectives != null) {
				result.putAll(arbitraryDirectives);
			}
			if (resolution != null) {
				if (ImportPackageSpecification.RESOLUTION_STATIC.equals(resolution)) {
					result.put(Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_MANDATORY);
				} else {
					result.put(Constants.RESOLUTION_DIRECTIVE, resolution);
				}
			}
			result.put(Constants.FILTER_DIRECTIVE, createFilterDirective());
			return result;
		}
	}

	private String createFilterDirective() {
		StringBuilder filter = new StringBuilder();
		filter.append("(&"); //$NON-NLS-1$
		synchronized (this.monitor) {
			addFilterAttribute(filter, BundleRevision.PACKAGE_NAMESPACE, getName(), false);
			VersionRange range = getVersionRange();
			if (range != null && range != VersionRange.emptyRange) {
				addFilterAttribute(filter, Constants.VERSION_ATTRIBUTE, range);
			}
			if (symbolicName != null) {
				addFilterAttribute(filter, Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, symbolicName);
			}
			if (bundleVersionRange != null) {
				addFilterAttribute(filter, Constants.BUNDLE_VERSION_ATTRIBUTE, bundleVersionRange);
			}
			if (attributes != null) {
				addFilterAttributes(filter, attributes);
			}
		}
		filter.append(')');
		return filter.toString();
	}

	@Override
	protected Map<String, Object> getInteralAttributes() {
		return Collections.emptyMap();
	}

	@Override
	protected String getInternalNameSpace() {
		return BundleRevision.PACKAGE_NAMESPACE;
	}
}
