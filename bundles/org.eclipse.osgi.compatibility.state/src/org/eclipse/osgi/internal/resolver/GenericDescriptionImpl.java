/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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

import java.util.*;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class GenericDescriptionImpl extends BaseDescriptionImpl implements GenericDescription {
	private Dictionary<String, Object> attributes;
	private volatile BundleDescription supplier;
	private volatile String type = GenericDescription.DEFAULT_TYPE;
	private Map<String, String> directives;
	private GenericDescription fragmentDeclaration;

	public GenericDescriptionImpl() {
		super();
	}

	public GenericDescriptionImpl(BundleDescription host, GenericDescription fragmentDeclaration) {
		setType(fragmentDeclaration.getType());
		Dictionary<String, Object> origAttrs = fragmentDeclaration.getAttributes();
		if (origAttrs != null) {
			Hashtable<String, Object> copyAttrs = new Hashtable<>();
			for (Enumeration<String> keys = origAttrs.keys(); keys.hasMoreElements();) {
				String key = keys.nextElement();
				copyAttrs.put(key, origAttrs.get(key));
			}
			setAttributes(copyAttrs);
		}
		Map<String, String> origDirectives = fragmentDeclaration.getDeclaredDirectives();
		Map<String, String> copyDirectives = new HashMap<>(origDirectives);
		setDirectives(copyDirectives);
		setSupplier(host);
		this.fragmentDeclaration = fragmentDeclaration;
	}

	public Dictionary<String, Object> getAttributes() {
		synchronized (this.monitor) {
			return attributes;
		}
	}

	public BundleDescription getSupplier() {
		return supplier;
	}

	void setAttributes(Dictionary<String, Object> attributes) {
		synchronized (this.monitor) {
			this.attributes = attributes;
		}
	}

	void setDirectives(Map<String, String> directives) {
		synchronized (this.monitor) {
			this.directives = directives;
		}
	}

	void setSupplier(BundleDescription supplier) {
		this.supplier = supplier;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Constants.PROVIDE_CAPABILITY).append(": ").append(getType()); //$NON-NLS-1$
		Map<String, Object> attrs = getDeclaredAttributes();
		sb.append(toString(attrs, false));
		return sb.toString();
	}

	/**
	 * @deprecated
	 */
	@Override
	public String getName() {
		synchronized (this.monitor) {
			Object name = attributes != null ? attributes.get(getType()) : null;
			return name instanceof String ? (String) name : null;
		}
	}

	public String getType() {
		return type;
	}

	void setType(String type) {
		if (type == null || type.equals(GenericDescription.DEFAULT_TYPE))
			this.type = GenericDescription.DEFAULT_TYPE;
		else
			this.type = type;
	}

	/**
	 * @deprecated
	 */
	@Override
	public Version getVersion() {
		Object version = attributes != null ? attributes.get(Constants.VERSION_ATTRIBUTE) : null;
		return version instanceof Version ? (Version) version : super.getVersion();
	}

	public Map<String, String> getDeclaredDirectives() {
		synchronized (this.monitor) {
			if (directives == null)
				return Collections.<String, String> emptyMap();
			return Collections.unmodifiableMap(directives);
		}
	}

	public Map<String, Object> getDeclaredAttributes() {
		synchronized (this.monitor) {
			Map<String, Object> result = new HashMap<>(5);
			if (attributes != null)
				for (Enumeration<String> keys = attributes.keys(); keys.hasMoreElements();) {
					String key = keys.nextElement();
					Object value = attributes.get(key);
					if (value instanceof List)
						value = Collections.unmodifiableList((List<?>) value);
					result.put(key, value);
				}
			return Collections.unmodifiableMap(result);
		}
	}

	@Override
	String getInternalNameSpace() {
		return getType();
	}

	@Override
	public BaseDescription getFragmentDeclaration() {
		return fragmentDeclaration;
	}

	void setFragmentDeclaration(GenericDescription fragmentDeclaration) {
		this.fragmentDeclaration = fragmentDeclaration;
	}
}
