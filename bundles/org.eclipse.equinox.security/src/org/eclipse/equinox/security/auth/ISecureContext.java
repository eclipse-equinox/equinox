/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.security.auth;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.eclipse.equinox.security.auth.event.*;

/**
 * The ISecureContext is the central entry point for the authentication support.
 * Use it to perform login, logout, and retrieve information associated with the security
 * subject.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 */
public interface ISecureContext {

	/**
	 * Call this method to perform a login. 
	 * @see LoginContext#login()
	 * @throws LoginException
	 */
	public void login() throws LoginException;

	/**
	 * Call this method to perform a logout.
	 * @see LoginContext#logout()
	 * @throws LoginException
	 */
	public void logout() throws LoginException;

	/**
	 * Retrieves the current Subject. Calling this method will force a login to occur 
	 * if the user is not already logged in.
	 * @see LoginContext#getSubject()
	 * @return the Subject
	 */
	public Subject getSubject() throws LoginException;

	/**
	 * Adds listener to be notified on security-related events.
	 * @param listener the listener to be registered
	 * @see ILoginListener
	 * @see ILogoutListener
	 */
	public void registerListener(ISecurityListener listener);

	/**
	 * Removes listener previously registered to receive notifications
	 * on security-related events.
	 * @param listener the listener to be unregistered
	 * @see ILoginListener
	 * @see ILogoutListener
	 */
	public void unregisterListener(ISecurityListener listener);
}
