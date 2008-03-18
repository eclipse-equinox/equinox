/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.auth;

import java.net.URL;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.eclipse.equinox.internal.security.auth.events.SecurityEventsManager;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.equinox.security.auth.ISecureContext;
import org.eclipse.equinox.security.auth.event.ISecurityListener;

public class SecureContext implements ISecureContext {

	private String configName;
	private LoginContext loginContext;
	private CallbackHandler handler;

	private SecurityEventsManager eventsManager = new SecurityEventsManager();
	private boolean loggedIn = false;

	public SecureContext(String configugationName) {
		this(configugationName, null, null);
	}

	public SecureContext(String configugationName, URL configFile, CallbackHandler handler) {
		configName = configugationName;
		SecurePlatformInternal platform = SecurePlatformInternal.getInstance();
		if (configFile != null)
			platform.addConfigURL(configFile); // this call MUST be done before start()		
		platform.start();
		this.handler = handler;
	}

	public void login() throws LoginException {
		LoginContext context = getLoginContext();
		LoginException loginException = null;
		eventsManager.notifyLoginBegin(context.getSubject());
		try {
			context.login();
		} catch (LoginException e) {
			loginException = e;
		}
		// subject might have changed if login() was triggered
		eventsManager.notifyLoginEnd(context.getSubject(), loginException);
		if (loginException != null) {
			LoginException rtvException = new LoginException(SecAuthMessages.loginFailure);
			rtvException.initCause(loginException);
			throw rtvException;
		}
		loggedIn = true;
	}

	public void logout() throws LoginException {
		LoginContext context = getLoginContext();
		Subject subject = getLoginContext().getSubject();
		eventsManager.notifyLogoutBegin(subject);

		LoginException loginException = null;
		try {
			context.logout();
		} catch (LoginException e) {
			loginException = e;
		}
		eventsManager.notifyLogoutEnd(subject, loginException);
		loggedIn = false;
	}

	public Subject getSubject() throws LoginException {
		if (!loggedIn)
			login();
		return getLoginContext().getSubject();
	}

	public LoginContext getLoginContext() throws LoginException {
		if (loginContext != null)
			return loginContext;

		CallbackHandler callbackHandler;
		if (handler == null)
			callbackHandler = SecurePlatformInternal.getInstance().loadCallbackHandler(configName);
		else
			callbackHandler = handler;

		if (callbackHandler == null)
			loginContext = new LoginContext(configName);
		else
			loginContext = new LoginContext(configName, callbackHandler);
		return loginContext;
	}

	public void registerListener(ISecurityListener listener) {
		eventsManager.addListener(listener);
	}

	public void unregisterListener(ISecurityListener listener) {
		eventsManager.removeListener(listener);
	}

}
