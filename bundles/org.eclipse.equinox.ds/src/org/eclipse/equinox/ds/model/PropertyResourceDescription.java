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

/**
 * 
 * This class models the properties element.
 * The properties element reads a set of properties from a bundle entry.
 * 
 * @version $Revision: 1.2 $
 */
public class PropertyResourceDescription extends PropertyDescription {

	/**
	 * Eclipse-generated <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -6094597960839333734L;

	private String entry;

	/**
	 * @return Returns the entry.
	 */
	public String getEntry() {
		return entry;
	}

	/**
	 * @param entry The entry to set.
	 */
	public void setEntry(String entry) {
		this.entry = entry;
	}
}
