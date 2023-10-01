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
package org.eclipse.equinox.ds.tests.tb4;

import org.eclipse.equinox.ds.tests.tbc.TestHelper;
import org.osgi.service.component.ComponentContext;

public class ServiceProvider {

	public void activate(ComponentContext ctxt) {
		TestHelper.setActivatedServiceProvider(true);
	}

	public void deactivate(ComponentContext ctxt) {
		TestHelper.setActivatedServiceProvider(false);
	}

}
