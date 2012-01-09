/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
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
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleRevision;

public class ExportPackageDescriptionImpl extends BaseDescriptionImpl implements ExportPackageDescription {
	public static final String EQUINOX_EE = "x-equinox-ee"; //$NON-NLS-1$
	private static final Integer EQUINOX_EE_DEFAULT = new Integer(-1);
	private String[] uses;
	private Map<String, Object> attributes;
	private Map<String, String> arbitraryDirectives;
	private volatile BundleDescription exporter;
	private String exclude;
	private String include;
	private String[] friends;
	private String[] mandatory;
	private Boolean internal = Boolean.FALSE;
	private int equinox_ee = -1;
	private ExportPackageDescription fragmentDeclaration = null;

	public ExportPackageDescriptionImpl() {
		super();
	}

	public ExportPackageDescriptionImpl(BundleDescription host, ExportPackageDescription fragmentDeclaration) {
		setName(fragmentDeclaration.getName());
		setVersion(fragmentDeclaration.getVersion());
		setDirectives(fragmentDeclaration.getDirectives());
		setArbitraryDirectives(((ExportPackageDescriptionImpl) fragmentDeclaration).getArbitraryDirectives());
		setAttributes(fragmentDeclaration.getAttributes());
		setExporter(host);
		this.fragmentDeclaration = fragmentDeclaration;
	}

	public Map<String, Object> getDirectives() {
		synchronized (this.monitor) {
			Map<String, Object> result = new HashMap<String, Object>(7);
			if (uses != null)
				result.put(Constants.USES_DIRECTIVE, uses);
			if (exclude != null)
				result.put(Constants.EXCLUDE_DIRECTIVE, exclude);
			if (include != null)
				result.put(Constants.INCLUDE_DIRECTIVE, include);
			if (mandatory != null)
				result.put(Constants.MANDATORY_DIRECTIVE, mandatory);
			if (friends != null)
				result.put(Constants.FRIENDS_DIRECTIVE, friends);
			result.put(Constants.INTERNAL_DIRECTIVE, internal);
			result.put(EQUINOX_EE, equinox_ee == -1 ? EQUINOX_EE_DEFAULT : new Integer(equinox_ee));
			return result;
		}
	}

	public Map<String, String> getDeclaredDirectives() {
		Map<String, String> result = new HashMap<String, String>(6);
		synchronized (this.monitor) {
			Map<String, String> arbitrary = getArbitraryDirectives();
			if (arbitrary != null)
				result.putAll(arbitrary);
			if (uses != null)
				result.put(Constants.USES_DIRECTIVE, toString(uses));
			if (exclude != null)
				result.put(Constants.EXCLUDE_DIRECTIVE, exclude);
			if (include != null)
				result.put(Constants.INCLUDE_DIRECTIVE, include);
			if (mandatory != null)
				result.put(Constants.MANDATORY_DIRECTIVE, toString(mandatory));
			if (friends != null)
				result.put(Constants.FRIENDS_DIRECTIVE, toString(friends));
			if (internal != null)
				result.put(Constants.INTERNAL_DIRECTIVE, internal.toString());
			return Collections.unmodifiableMap(result);
		}
	}

	public Map<String, Object> getDeclaredAttributes() {
		Map<String, Object> result = new HashMap<String, Object>(2);
		synchronized (this.monitor) {
			if (attributes != null)
				result.putAll(attributes);
			result.put(BundleRevision.PACKAGE_NAMESPACE, getName());
			result.put(Constants.VERSION_ATTRIBUTE, getVersion());
			Version bundleVersion = getSupplier().getVersion();
			if (bundleVersion != null)
				result.put(Constants.BUNDLE_VERSION_ATTRIBUTE, bundleVersion);
			String symbolicName = getSupplier().getSymbolicName();
			if (symbolicName != null) {
				if (symbolicName.equals(Constants.getInternalSymbolicName()))
					result.put(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, Arrays.asList(Constants.SYSTEM_BUNDLE_SYMBOLICNAME, symbolicName));
				else
					result.put(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, symbolicName);
			}
			return Collections.unmodifiableMap(result);
		}
	}

	static String toString(String[] list) {
		StringBuffer buffer = new StringBuffer();
		for (String string : list)
			buffer.append(string).append(',');
		if (buffer.length() > 0)
			buffer.setLength(buffer.length() - 1);
		return buffer.toString();
	}

	public Object getDirective(String key) {
		synchronized (this.monitor) {
			if (key.equals(Constants.USES_DIRECTIVE))
				return uses;
			if (key.equals(Constants.EXCLUDE_DIRECTIVE))
				return exclude;
			if (key.equals(Constants.INCLUDE_DIRECTIVE))
				return include;
			if (key.equals(Constants.MANDATORY_DIRECTIVE))
				return mandatory;
			if (key.equals(Constants.FRIENDS_DIRECTIVE))
				return friends;
			if (key.equals(Constants.INTERNAL_DIRECTIVE))
				return internal;
			if (key.equals(EQUINOX_EE))
				return equinox_ee == -1 ? EQUINOX_EE_DEFAULT : new Integer(equinox_ee);
			return null;
		}
	}

	public Object setDirective(String key, Object value) {
		synchronized (this.monitor) {
			if (key.equals(Constants.USES_DIRECTIVE))
				return uses = (String[]) value;
			if (key.equals(Constants.EXCLUDE_DIRECTIVE))
				return exclude = (String) value;
			if (key.equals(Constants.INCLUDE_DIRECTIVE))
				return include = (String) value;
			if (key.equals(Constants.MANDATORY_DIRECTIVE))
				return mandatory = (String[]) value;
			if (key.equals(Constants.FRIENDS_DIRECTIVE))
				return friends = (String[]) value;
			if (key.equals(Constants.INTERNAL_DIRECTIVE))
				return internal = (Boolean) value;
			if (key.equals(EQUINOX_EE)) {
				equinox_ee = ((Integer) value).intValue();
				return value;
			}
			return null;
		}
	}

	public void setDirectives(Map<String, ?> directives) {
		synchronized (this.monitor) {
			if (directives == null)
				return;
			uses = (String[]) directives.get(Constants.USES_DIRECTIVE);
			exclude = (String) directives.get(Constants.EXCLUDE_DIRECTIVE);
			include = (String) directives.get(Constants.INCLUDE_DIRECTIVE);
			mandatory = (String[]) directives.get(Constants.MANDATORY_DIRECTIVE);
			friends = (String[]) directives.get(Constants.FRIENDS_DIRECTIVE);
			internal = (Boolean) directives.get(Constants.INTERNAL_DIRECTIVE);
			equinox_ee = ((Integer) directives.get(EQUINOX_EE)).intValue();
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

	public Map<String, Object> getAttributes() {
		synchronized (this.monitor) {
			return attributes;
		}
	}

	public BundleDescription getSupplier() {
		return getExporter();
	}

	public BundleDescription getExporter() {
		return exporter;
	}

	/**
	 * @deprecated
	 */
	public boolean isRoot() {
		return true;
	}

	@SuppressWarnings("unchecked")
	protected void setAttributes(Map<String, ?> attributes) {
		synchronized (this.monitor) {
			this.attributes = (Map<String, Object>) attributes;
		}
	}

	protected void setExporter(BundleDescription exporter) {
		this.exporter = exporter;
	}

	public BaseDescription getFragmentDeclaration() {
		return fragmentDeclaration;
	}

	void setFragmentDeclaration(ExportPackageDescription fragmentDeclaration) {
		this.fragmentDeclaration = fragmentDeclaration;
	}

	public String toString() {
		return "Export-Package: " + getName() + "; version=\"" + getVersion() + "\""; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

	String getInternalNameSpace() {
		return BundleRevision.PACKAGE_NAMESPACE;
	}
}
