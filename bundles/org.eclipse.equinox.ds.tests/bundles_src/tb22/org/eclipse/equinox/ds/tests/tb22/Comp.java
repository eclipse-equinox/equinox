/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.equinox.ds.tests.tb22;

import java.util.Dictionary;

import org.eclipse.equinox.ds.tests.tbc.PropertiesProvider;
import org.osgi.service.component.ComponentContext;

public class Comp implements PropertiesProvider {
	private ComponentContext ctxt;

	protected void activate(ComponentContext ctxt) {
		this.ctxt = ctxt;
	}

	protected void deactivate(ComponentContext ctxt) {

	}

	@Override
	public Dictionary getProperties() {
		if (ctxt == null) {
			return null;
		}

		return ctxt.getProperties();
	}
}
