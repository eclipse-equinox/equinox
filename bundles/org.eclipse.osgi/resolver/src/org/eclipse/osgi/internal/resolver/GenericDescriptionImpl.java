/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.util.Dictionary;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.GenericDescription;
import org.osgi.framework.Constants;

public class GenericDescriptionImpl extends BaseDescriptionImpl implements GenericDescription {
	private Dictionary attributes;
	private BundleDescription supplier;
	private String type = GenericDescription.DEFAULT_TYPE;

	public Dictionary getAttributes() {
		return attributes;
	}

	public BundleDescription getSupplier() {
		return supplier;
	}

	void setAttributes(Dictionary attributes) {
		this.attributes = attributes;
		// always add/replace the version attribute with the actual Version object
		attributes.put(Constants.VERSION_ATTRIBUTE, getVersion());
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
}
