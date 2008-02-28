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
 * receive notifications of login process.
 */
public interface ILoginListener extends ISecurityListener {

	/**
	 * This method is called before login starts.
	 * @param subject the subject being authenticated, might be <code>null</code>
	 * if there is no subject associated the context at this time
	 */
	void onLoginStart(Subject subject);

	/**
	 * This method is called after login sequence is finished. If login
	 * exception is not null, the login failed.
	 * @param subject the subject being authenticated, might be <code>null</code>
	 * if there is no subject associated the context at this time
	 * @param loginException <code>null</code> if login succeeded, otherwise contains
	 * exception caused login to fail 
	 */
	void onLoginFinish(Subject subject, LoginException loginException);
}
