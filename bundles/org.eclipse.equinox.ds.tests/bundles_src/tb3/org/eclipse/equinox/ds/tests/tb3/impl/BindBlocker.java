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
package org.eclipse.equinox.ds.tests.tb3.impl;

import org.osgi.framework.ServiceReference;

public class BindBlocker {

	// the time the bind method will block
	private final int timeout = 60000;

	public void setLogger(ServiceReference log) {
		try {
			Thread.sleep(timeout);
		} catch (InterruptedException ignore) {
		}
	}

	public void unsetLogger(ServiceReference log) {
	}
}
