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
package org.eclipse.equinox.ds.tests.tb8;

import org.eclipse.equinox.ds.tests.tbc.NamespaceProvider;
import org.osgi.service.component.ComponentContext;

public class NamespaceTester implements NamespaceProvider {
	private int nsid = -1;
	private final static String NSID_PROP = "component.nsid";

	protected void activate(ComponentContext ctxt) {
		Object prop = ctxt.getProperties().get(NSID_PROP);
		if (!(prop instanceof Integer)) {
			return;
		}
		nsid = ((Integer) prop).intValue();
	}

	protected void deactivate(ComponentContext ctxt) {

	}

	@Override
	public int getComponentNSID() {
		return nsid;
	}
}
