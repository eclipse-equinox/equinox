/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.security.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * The AuthorizationManager is the facility by which the end user
 * is informed of the current state of the system with respect to 
 * bundle authorization based security. 
 */
public abstract class AuthorizationManager {

	public AuthorizationManager() {
		//no content
	}

	/**
	 * Query whether the authorization system is enabled for the system.
	 * 
	 * @return <code>true</code> if and only if authorization is enabled 
	 */
	abstract public boolean isEnabled();

	/**
	 * Returns true when the system is in need of attention from the end
	 * user. This means that some unauthorized content has been encountered, and the 
	 * user has not yet inspected the situation.
	 * 
	 * @return <code>true</code> if user attention is required
	 */
	abstract public boolean needsAttention();

	/**
	 * Return an Eclipse IStatus object representing the current state of the 
	 * authorization system.
	 *
	 * @return IStatus code representing the system status
	 */
	abstract public IStatus getStatus();

	/**
	 * Open the authorization manager user interface so that the end
	 * user can view and edit the system's authorization state.
	 * 
	 * @param workbenchWindow the workbench window
	 */
	abstract public void displayManager(IWorkbenchWindow workbenchWindow);

}
