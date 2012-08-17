/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
			Hashtable<String, Object> copyAttrs = new Hashtable<String, Object>();
			for (Enumeration<String> keys = origAttrs.keys(); keys.hasMoreElements();) {
				String key = keys.nextElement();
				copyAttrs.put(key, origAttrs.get(key));
			}
			setAttributes(copyAttrs);
		}
		Map<String, String> origDirectives = fragmentDeclaration.getDeclaredDirectives();
		Map<String, String> copyDirectives = new HashMap<String, String>(origDirectives);
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

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(Constants.PROVIDE_CAPABILITY).append(": ").append(getType()); //$NON-NLS-1$
		Map<String, Object> attrs = getDeclaredAttributes();
		sb.append(toString(attrs, false));
		return sb.toString();
	}

	/**
	 * @deprecated
	 */
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
	public Version getVersion() {
		Object version = attributes != null ? attributes.get(Constants.VERSION_ATTRIBUTE) : null;
		return version instanceof Version ? (Version) version : super.getVersion();
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getDeclaredDirectives() {
		synchronized (this.monitor) {
			if (directives == null)
				return Collections.EMPTY_MAP;
			return Collections.unmodifiableMap(directives);
		}
	}

	public Map<String, Object> getDeclaredAttributes() {
		synchronized (this.monitor) {
			Map<String, Object> result = new HashMap<String, Object>(5);
			if (attributes != null)
				for (Enumeration<String> keys = attributes.keys(); keys.hasMoreElements();) {
					String key = keys.nextElement();
					Object value = attributes.get(key);
					if (value instanceof List)
						value = Collections.unmodifiableList((List<Object>) value);
					result.put(key, value);
				}
			return Collections.unmodifiableMap(result);
		}
	}

	String getInternalNameSpace() {
		return getType();
	}

	public BaseDescription getFragmentDeclaration() {
		return fragmentDeclaration;
	}

	void setFragmentDeclaration(GenericDescription fragmentDeclaration) {
		this.fragmentDeclaration = fragmentDeclaration;
	}
}
