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

public class StaticCircuit1 {
	private StaticCircuit2 mate = null;

	public void bind(StaticCircuit2 mate) {
		this.mate = mate;
	}

	public void unbind(StaticCircuit2 mate) {
		if (this.mate == mate) {
			this.mate = null;
		}
	}
}
