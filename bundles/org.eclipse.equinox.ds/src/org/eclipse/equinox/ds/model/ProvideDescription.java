/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.model;

import java.io.Serializable;

/**
 * 
 * This class models the provide element.
 * The service element must have one or more provide elements that define
 * the service interfaces. The provide element has a single attribute:
 * interface 
 * 
 * @version $Revision: 1.2 $
 */
public class ProvideDescription implements Serializable {
	/**
	 * Eclipse-generated <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -8301435779729338880L;

	private String interfacename;

	/**
	 * @return Returns the interfacename.
	 */
	public String getInterfacename() {
		return interfacename;
	}

	/**
	 * @param interfacename The interfacename to set.
	 */
	public void setInterfacename(String interfacename) {
		this.interfacename = interfacename;
	}
}
