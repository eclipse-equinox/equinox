/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.security.action;

import java.security.PrivilegedAction;

/**
 * Get System Property PrivilegedAction class.
 */

public class GetProperty implements PrivilegedAction {
	private String property;
	private String def;

	public GetProperty(String property) {
		this.property = property;
	}

	public GetProperty(String property, String def) {
		this.property = property;
		this.def = def;
	}

	/** 
	    @exception java.lang.NullPointerException
	    @exception java.lang.IllegalArgumentException
	*/
	public Object run() {
		if (property == null)
			return System.getProperties();
		else
			return System.getProperty(property, def);
	}
}
