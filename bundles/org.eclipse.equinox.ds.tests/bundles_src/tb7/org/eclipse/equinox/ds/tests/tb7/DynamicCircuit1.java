/*******************************************************************************
 * Copyright (c) 1997-2009 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.tests.tb7;

import org.osgi.service.component.ComponentContext;

public class DynamicCircuit1 {
	boolean active = false;

	public boolean isActivated() {
		return active;
	}

	protected void activate(ComponentContext context) {
		active = true;
	}

	protected void bind(DynamicCircuit2 service) {

	}

}
