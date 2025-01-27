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
package org.eclipse.equinox.ds.tests.tb21a;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import org.eclipse.equinox.ds.tests.tbc.ComponentContextProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

public class ModifyRegistrator implements ComponentContextProvider {
	private Dictionary properties;
	private ComponentContext ctxt;
	private static final int MODIFIED = 1 << 0;
	private static final int MOD = 1 << 1;
	private static final int MOD_CC = 1 << 2;
	private static final int MOD_BC = 1 << 3;
	private static final int MOD_MAP = 1 << 4;
	private static final int MOD_CC_BC_MAP = 1 << 5;
	private static final int ACTIVATE = 1 << 6;
	private static final int DEACTIVATE = 1 << 7;

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

	protected void modified() {
		setDataBits(MODIFIED);
	}

	protected void mod() {
		setDataBits(MOD);
	}

	protected void modCc(ComponentContext ctxt) {
		setDataBits(MOD_CC);
	}

	protected void modBc(BundleContext bc) {
		setDataBits(MOD_BC);
	}

	protected void modMap(Map props) {
		setDataBits(MOD_MAP);
	}

	protected void modCcBcMap(ComponentContext ctxt, BundleContext bc, Map props) {
		setDataBits(MOD_CC_BC_MAP);
	}

	protected void throwException(ComponentContext ctxt) {
		throw new RuntimeException("Test method throwException(ComponentContext) is called!");
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
