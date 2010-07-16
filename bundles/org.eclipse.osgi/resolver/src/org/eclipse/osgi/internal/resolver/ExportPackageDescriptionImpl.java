/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;

public class ExportPackageDescriptionImpl extends BaseDescriptionImpl implements ExportPackageDescription {
	public static final String EQUINOX_EE = "x-equinox-ee"; //$NON-NLS-1$
	private static final Integer EQUINOX_EE_DEFAULT = new Integer(-1);
	private String[] uses;
	private Map attributes;
	private volatile BundleDescription exporter;
	private String exclude;
	private String include;
	private String[] friends;
	private String[] mandatory;
	private Boolean internal = Boolean.FALSE;
	private int equinox_ee = -1;
	private volatile int tableIndex;

	public Map getDirectives() {
		synchronized (this.monitor) {
			Map result = new HashMap(5);
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

	public void setDirectives(Map directives) {
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

	public Map getAttributes() {
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

	protected void setAttributes(Map attributes) {
		synchronized (this.monitor) {
			this.attributes = attributes;
		}
	}

	protected void setExporter(BundleDescription exporter) {
		this.exporter = exporter;
	}

	public String toString() {
		return "Export-Package: " + getName() + "; version=\"" + getVersion() + "\""; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

	int getTableIndex() {
		return tableIndex;
	}

	void setTableIndex(int tableIndex) {
		this.tableIndex = tableIndex;
	}
}
