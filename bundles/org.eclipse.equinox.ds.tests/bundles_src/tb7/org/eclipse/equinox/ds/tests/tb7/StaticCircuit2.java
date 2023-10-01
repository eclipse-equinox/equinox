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
package org.eclipse.equinox.ds.tests.tb7;

import java.util.Dictionary;

import org.eclipse.equinox.ds.tests.tbc.BoundTester;
import org.osgi.framework.ServiceReference;

public class StaticCircuit2 implements BoundTester {

	private StaticCircuit1 mate;

	public int getBoundObjectsCount() {
		return (mate != null ? 1 : 0);
	}

	public Object getBoundService(int index) {
		return mate;
	}

	public ServiceReference getBoundServiceRef(int index) {
		return null;
	}

	public Dictionary getProperties() {
		return null;
	}

	public void bind(StaticCircuit1 mate) {
		this.mate = mate;
	}

	public void unbind(StaticCircuit1 mate) {
		if (this.mate == mate) {
			this.mate = null;
		}
	}

}
