/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.dependencies;

/**
 * Represents a change that happened to an element's resolution status.
 * Not to be implemented by clients.
 */
public interface IElementChange {
	/** State transitions. */ 
	public final static int ADDED = 0x01;
	public final static int REMOVED = 0x02;
	public final static int UPDATED = ADDED | REMOVED;
	public final static int RESOLVED = 0x04;
	public final static int UNRESOLVED = 0x08;
	public final static int LINKAGE_CHANGED = 0x10;
	
	/**
	 * Returns the affected element.
	 */
	public IElement getElement();	
	/**
	 * Returns the kind of the transition.
	 */
	public int getKind();
}
