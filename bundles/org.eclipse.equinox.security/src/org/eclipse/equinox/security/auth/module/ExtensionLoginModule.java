/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.security.auth.module;

import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import org.eclipse.equinox.internal.security.auth.ext.loader.ExtLoginModuleLoader;

/**
 * This class allows login modules specified via <code>loginModule</code> extension point
 * to be included in the login configurations.
 * <p>
 * To include your login module in a login configuration, specify this class as a login module 
 * using its qualified Java name. Options specified for such entry should contain an option named 
 * <code>extensionId</code> set to the qualified ID of the extension describing your login module.
 * </p><p>
 * This class should not be extended or instantiated directly.
 * </p>
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
final public class ExtensionLoginModule implements LoginModule {

	/**
	 * The key for the option that specifies an extension describing the actual login module 
	 */
	static final public String OPTION_MODULE_POINT = "extensionId"; //$NON-NLS-1$

	private LoginModule target = null;

	/**
	 * Constructor
	 */
	public ExtensionLoginModule() {
		// place holder
	}

	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, java.util.Map, java.util.Map)
	 */
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options) {
		target = ExtLoginModuleLoader.load(options);
		target.initialize(subject, callbackHandler, sharedState, options);
	}

	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#login()
	 */
	public boolean login() throws LoginException {
		return target.login();
	}

	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#commit()
	 */
	public boolean commit() throws LoginException {
		return target.commit();
	}

	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#abort()
	 */
	public boolean abort() throws LoginException {
		return target.abort();
	}

	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#logout()
	 */
	public boolean logout() throws LoginException {
		return target.logout();
	}
}
