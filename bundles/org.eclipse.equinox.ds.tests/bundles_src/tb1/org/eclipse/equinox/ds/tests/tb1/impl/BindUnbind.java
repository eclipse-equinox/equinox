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
import java.util.Vector;

import org.eclipse.equinox.ds.tests.tbc.BoundTester;
import org.osgi.framework.ServiceReference;

public class BindUnbind implements BoundTester {

	private final Vector boundObjects = new Vector();

	public void bindSAComp(ServiceReference sr) {
		if (boundObjects.contains(sr)) {
		} else {
			boundObjects.addElement(sr);
		}
	}

	public void unbindSAComp(ServiceReference sr) {
		if (boundObjects.contains(sr)) {
			boundObjects.removeElement(sr);
		}
	}

	@Override
	public Dictionary getProperties() {
		return null;
	}

	@Override
	public int getBoundObjectsCount() {
		return boundObjects.size();
	}

	@Override
	public ServiceReference getBoundServiceRef(int index) {
		return (ServiceReference) boundObjects.elementAt(index);
	}

	@Override
	public Object getBoundService(int index) {
		return null;
	}
}
