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
package org.eclipse.equinox.ds.tests.tb4.impl;

import java.util.Dictionary;

import org.eclipse.equinox.ds.tests.tb4.NamedService;
import org.eclipse.equinox.ds.tests.tbc.ComponentContextProvider;
import org.osgi.service.component.ComponentContext;

public class NamedFactory implements NamedService, ComponentContextProvider {
	private String name = "name not init";
	private ComponentContext ctxt;

	public void activate(ComponentContext componentContext) {
		this.ctxt = componentContext;
		name = (String) componentContext.getProperties().get("name");
		if (name == null) {
			this.name = "name not set";
		}
	}

	// it is absolutely legal to have activate without having deactivate!
	// public void deactivate(ComponentContext cc) {}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public ComponentContext getComponentContext() {
		return ctxt;
	}

	@Override
	public Dictionary getProperties() {
		return ctxt.getProperties();
	}

}
