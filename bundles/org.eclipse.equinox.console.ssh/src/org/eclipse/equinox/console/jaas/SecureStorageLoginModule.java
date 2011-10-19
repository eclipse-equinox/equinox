/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.jaas;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.eclipse.equinox.console.storage.DigestUtil;
import org.eclipse.equinox.console.storage.SecureUserStore;

/**
 * This class implements a JAAS LoginModule, which performs username/password
 * based authentication. It reads the user data from the store. 
 *
 */
public class SecureStorageLoginModule implements LoginModule {
	
	private volatile Subject subject;
	private volatile CallbackHandler callbackHandler;
	private volatile UserPrincipal userPrincipal;
	private volatile boolean isSuccess;

	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {
		this.subject = subject;
		this.callbackHandler = callbackHandler;
	}

	public boolean login() throws LoginException {
		NameCallback nameCallback = new NameCallback("username: ");
		PasswordCallback passwordCallback = new PasswordCallback("password: ", false);
		try {
			callbackHandler.handle(new Callback[]{nameCallback, passwordCallback});
		} catch (IOException e) {
			throw new FailedLoginException("Cannot get username and password");
		} catch (UnsupportedCallbackException e) {
			throw new FailedLoginException("Cannot get username and password");
		}
		
		String username = nameCallback.getName();
		char[] password = passwordCallback.getPassword();
		
		userPrincipal = getUserInfo(username);
		
		try {
			isSuccess = userPrincipal.authenticate(DigestUtil.encrypt(new String(password)).toCharArray());
		} catch (Exception e) {
			throw new FailedLoginException("Wrong credentials");
		}
		
		if (isSuccess == true) {
			return isSuccess;
		} else {
			throw new FailedLoginException("Wrong credentials");
		}
	}

	public boolean commit() throws LoginException {
		if (isSuccess == true) {
			synchronized (this) {
				subject.getPrincipals().add(userPrincipal);
				subject.getPrincipals().addAll(userPrincipal.getRoles());
			}
			return true;
		} else {
			userPrincipal.destroy();
			userPrincipal = null;
			return false;
		}
	}

	public boolean abort() throws LoginException {
		userPrincipal.destroy();
		userPrincipal = null;
		return true;
	}

	public boolean logout() throws LoginException {
		synchronized (this) {
			subject.getPrincipals().remove(userPrincipal);
			subject.getPrincipals().removeAll(userPrincipal.getRoles());
		}
		subject = null;
		userPrincipal.destroy();
		userPrincipal = null;
		return true;
	}
	
	private UserPrincipal getUserInfo(String username) throws FailedLoginException {
		try {
			if (!SecureUserStore.existsUser(username)) {
				throw new FailedLoginException("Wrong credentials");
			}
			
			String password = SecureUserStore.getPassword(username);
			if (password == null) {
				throw new FailedLoginException("Corrupted user");
			}
			
			String roles = SecureUserStore.getRoles(username);
			if (roles == null) {
				roles = "";
			}
			
			UserPrincipal userPrincipal = new UserPrincipal(username, password);
			for (String role : roles.split(",")) {
				userPrincipal.addRole(new RolePrincipal(role));
			}
			
			return userPrincipal;
		} catch (Exception e) {
			throw new FailedLoginException(e.getMessage());
		}
	}
	
}
