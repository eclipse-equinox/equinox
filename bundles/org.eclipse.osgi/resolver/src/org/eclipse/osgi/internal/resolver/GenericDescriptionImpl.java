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

import java.util.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.GenericDescription;
import org.osgi.framework.Constants;

public class GenericDescriptionImpl extends BaseDescriptionImpl implements GenericDescription {
	private Dictionary attributes;
	private volatile BundleDescription supplier;
	private volatile String type = GenericDescription.DEFAULT_TYPE;

	public Dictionary getAttributes() {
		synchronized (this.monitor) {
			return attributes;
		}
	}

	public BundleDescription getSupplier() {
		return supplier;
	}

	void setAttributes(Dictionary attributes, boolean addImplied) {
		synchronized (this.monitor) {
			this.attributes = attributes;
			if (addImplied) {
				// always add/replace the version attribute with the actual Version object
				attributes.put(Constants.VERSION_ATTRIBUTE, getVersion());
				// always add the name
				if (getName() != null)
					attributes.put(getType(), getName());
			}
		}
	}

	void setSupplier(BundleDescription supplier) {
		this.supplier = supplier;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(StateBuilder.GENERIC_CAPABILITY).append(": ").append(getName()); //$NON-NLS-1$
		if (getType() != GenericDescription.DEFAULT_TYPE)
			sb.append(':').append(getType());
		return sb.toString();
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

	public Map<String, String> getDeclaredDirectives() {
		// TODO need to allow directives
		return Collections.EMPTY_MAP;
	}

	public Map<String, Object> getDeclaredAttributes() {
		synchronized (this.monitor) {
			Map<String, Object> result = new HashMap(5);
			if (attributes != null)
				for (Enumeration keys = attributes.keys(); keys.hasMoreElements();) {
					String key = (String) keys.nextElement();
					result.put(key, attributes.get(key));
				}
			String name = getName();
			String nameSpace = getType();
			if (result.get(nameSpace) == null && name != null)
				result.put(nameSpace, name);
			return Collections.unmodifiableMap(result);
		}
	}
}
