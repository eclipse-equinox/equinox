/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
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
package org.eclipse.equinox.ds.tests.tb23;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.equinox.ds.tests.tbc.ComponentContextProvider;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

public class BindRegistrator implements ComponentContextProvider {
	private Dictionary properties;
	private ComponentContext ctxt;
	private static final int BIND = 1 << 0;
	private static final int UNBIND = 1 << 1;
	private static final int ACTIVATE = 1 << 2;
	private static final int DEACTIVATE = 1 << 3;

	protected void activate(ComponentContext ctxt) {
		this.ctxt = ctxt;
		properties = getProperties(ctxt.getProperties());
		setDataBits(ACTIVATE);
	}

	Properties getProperties(Dictionary dict) {
		Properties result = new Properties();
		Enumeration keys = dict.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			result.put(key, dict.get(key));
		}
		return result;
	}

	protected void deactivate(ComponentContext ctxt) {
		setDataBits(DEACTIVATE);
	}

	protected void bind(ServiceReference sr) {
		setDataBits(BIND);
		throw new RuntimeException("Test method throwException(ComponentContext) is called!");
	}

	protected void bind_ex(ServiceReference sr) {
		throw new RuntimeException("Test method bind_ex(ServiceReference) is called!");
	}

	protected void unbind(ServiceReference sr) {
		setDataBits(UNBIND);
	}

	@Override
	public Dictionary getProperties() {
		return properties;
	}

	private void setDataBits(int value) {
		if (properties == null) {
			return;
		}
		Object prop = properties.get("config.base.data");
		int data = (prop instanceof Integer) ? ((Integer) prop).intValue() : 0;
		properties.put("config.base.data", Integer.valueOf(data | value));
	}

	@Override
	public ComponentContext getComponentContext() {
		return ctxt;
	}
}
