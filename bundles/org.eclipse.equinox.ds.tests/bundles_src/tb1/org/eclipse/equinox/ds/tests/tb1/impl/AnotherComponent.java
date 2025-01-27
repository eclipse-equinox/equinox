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
package org.eclipse.equinox.ds.tests.tb1.impl;

import java.util.Dictionary;

import org.eclipse.equinox.ds.tests.tbc.BundleContextProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

public class AnotherComponent implements BundleContextProvider {

	private ComponentContext ctxt;

	public void activate(ComponentContext ctxt) {
		this.ctxt = ctxt;
	}

	public void deactivate(ComponentContext ctxt) {
		this.ctxt = null;
	}

	public ComponentContext getContext() {
		return ctxt;
	}

	@Override
	public BundleContext getBundleContext() {
		return Activator.getContext();
	}

	@Override
	public Dictionary getProperties() {
		return null;
	}
}
