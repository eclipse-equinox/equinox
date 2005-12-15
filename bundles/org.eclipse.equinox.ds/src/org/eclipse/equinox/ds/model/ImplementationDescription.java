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
 * This class models the implementation element.
 * The implementation element is required and defines the name of the component
 * implementation class.
 * 
 * @version $Revision: 1.2 $
 */
public class ImplementationDescription implements Serializable {
	/**
	 * Eclipse-generated <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -5348855154850323280L;

	private String classname;

	public ImplementationDescription() {
	}

	/**
	 * @return Returns the classname.
	 */
	public String getClassname() {
		return classname;
	}

	/**
	 * @param classname set this class name.
	 */
	public void setClassname(String classname) {
		this.classname = classname;
	}
}
