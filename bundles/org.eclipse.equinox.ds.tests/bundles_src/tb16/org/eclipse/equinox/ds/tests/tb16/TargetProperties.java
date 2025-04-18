/*******************************************************************************
 * Copyright (c) 1997-2009 by ProSyst Software GmbH
 * http://www.prosyst.com
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.tests.tb16;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.equinox.ds.tests.tbc.PropertiesProvider;
import org.eclipse.equinox.ds.tests.tbc.ComponentContextProvider;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

public class TargetProperties implements PropertiesProvider, ComponentContextProvider {
	private Dictionary properties;
	private ComponentContext ctxt;
	private ServiceRegistration sr;

	protected void activate(ComponentContext ctxt) {
		this.ctxt = ctxt;
		properties = ctxt.getProperties();

		Object prop = properties.get("serial.num");
		if (prop != null) {
			Dictionary<String, Object> serviceProps = new Hashtable<>();
			serviceProps.put("serial.num", prop);
			sr = ctxt.getBundleContext().registerService(getClass().getName(), this, serviceProps);
		}
	}

	protected void deactivate(ComponentContext ctxt) {
		if (sr != null) {
			sr.unregister();
			sr = null;
		}
	}

	@Override
	public Dictionary getProperties() {
		return properties;
	}

	@Override
	public ComponentContext getComponentContext() {
		return ctxt;
	}
}
