/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.eventmgr;

/**
 * ListElement is a package private class. This class
 * represents a primary object (e.g. listener) and its companion object.
 * Objects of this type are placed in an ElementList.
 * Objects of this type are immutable.
 */

class ListElement {
	/**
	 * Primary object.
	 */
	final Object primary;

	/**
	 * Companion object.
	 */
	final Object companion;

	/**
	 * Constructor for ElementList element
	 * @param primary Primary object in element. Used to uniqueness.
	 * @param companion Companion object stored with primary object.
	 */
	ListElement(final Object primary, final Object companion) {
		this.primary = primary;
		this.companion = companion;
	}
}