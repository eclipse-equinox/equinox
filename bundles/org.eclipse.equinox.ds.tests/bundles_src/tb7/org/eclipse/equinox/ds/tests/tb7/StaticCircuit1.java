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
