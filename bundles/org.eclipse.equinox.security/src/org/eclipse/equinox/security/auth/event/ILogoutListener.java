/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.security.auth.event;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

/**
 * Implement this interface on security event listeners to 
 * receive notifications of logout process.
 */
public interface ILogoutListener extends ISecurityListener {

	/**
	 * This method is called before logout starts.
	 * @param subject the authenticated subject, might be <code>null</code>
	 * if there is no subject associated the context at this time
	 */
	void onLogoutStart(Subject subject);

	/**
	 * This method is called after logout sequence finishes. If logout
	 * exception is not null, the logout failed.
	 * @param subject the authenticated subject, might be <code>null</code>
	 * if there is no subject associated the context at this time
	 * @param logoutException <code>null</code> if logout succeeded, otherwise contains
	 * exception caused logout to fail 
	 */
	void onLogoutFinish(Subject subject, LoginException logoutException);
}
