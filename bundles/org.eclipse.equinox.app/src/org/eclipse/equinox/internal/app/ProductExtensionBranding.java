/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.internal.app;

import java.util.HashMap;
import org.eclipse.core.runtime.IConfigurationElement;
import org.osgi.framework.Bundle;

public class ProductExtensionBranding implements IBranding {
	private static final String ATTR_DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String ATTR_NAME = "name"; //$NON-NLS-1$
	private static final String ATTR_APPLICATION = "application"; //$NON-NLS-1$
	private static final String ATTR_VALUE = "value"; //$NON-NLS-1$

	String application = null;
	String name = null;
	String id = null;
	String description = null;
	HashMap<String, String> properties;
	Bundle definingBundle = null;

	public ProductExtensionBranding(String id, IConfigurationElement element) {
		this.id = id;
		if (element == null) {
			return;
		}
		application = element.getAttribute(ATTR_APPLICATION);
		name = element.getAttribute(ATTR_NAME);
		description = element.getAttribute(ATTR_DESCRIPTION);
		loadProperties(element);
	}

	private void loadProperties(IConfigurationElement element) {
		IConfigurationElement[] children = element.getChildren();
		properties = new HashMap<>(children.length);
		for (IConfigurationElement child : children) {
			String key = child.getAttribute(ATTR_NAME);
			String value = child.getAttribute(ATTR_VALUE);
			if (key != null && value != null) {
				properties.put(key, value);
			}
		}
		definingBundle = Activator.getBundle(element.getContributor());
	}

	@Override
	public Bundle getDefiningBundle() {
		return definingBundle;
	}

	@Override
	public String getApplication() {
		return application;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getProperty(String key) {
		return properties.get(key);
	}

	@Override
	public Object getProduct() {
		return null;
	}
}
