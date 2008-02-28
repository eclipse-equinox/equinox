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

import java.io.IOException;
import java.net.URL;
import java.security.Security;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.Configuration;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.security.auth.ext.loader.ExtCallbackHandlerLoader;

// TBD what happens for server-side implementations if configurations are shared across all processes on VM?

public class SecurePlatformInternal {

	private static final String PROVIDER_URL_BASE = "login.config.url.";//$NON-NLS-1$
	private static final int MAX_PROVIDER_URL_COUNT = 777; // arbitrary upper limit on the number of provider URLs
	private Configuration defaultConfiguration;
	private ExtCallbackHandlerLoader callbackHandlerLoader = new ExtCallbackHandlerLoader();

	private boolean running = false;
	private static final SecurePlatformInternal s_instance = new SecurePlatformInternal();

	private SecurePlatformInternal() {
		// hides default constructor
	}

	public static final SecurePlatformInternal getInstance() {
		return s_instance;
	}

	public CallbackHandler loadCallbackHandler(String configurationName) {
		return callbackHandlerLoader.loadCallbackHandler(configurationName);
	}

	/**
	 * Java docs specify that if multiple config files are passed in, they will be merged into one file.
	 * Hence, aside from implementation details, no priority information is specified by the order
	 * of config files. In this implementation we add customer's config file to the end of the list.
	 * 
	 * This method substitutes default login configuration:
	 * Configuration Inquiries -> ConfigurationFederator ->
	 * 		1) Extension Point supplied config providers;
	 * 		2) default Java config provider ("login.configuration.provider")
	 */
	public void start() {
		if (running)
			return;
		try {
			defaultConfiguration = Configuration.getConfiguration();
		} catch (SecurityException e) {
			// could be caused by missing configuration provider URL;
			// this might be OK if default config provider is ignored
			defaultConfiguration = null;
		}
		Configuration.setConfiguration(new ConfigurationFederator(defaultConfiguration));
		running = true;
	}

	public void stop() {
		if (!running)
			return;
		Configuration.setConfiguration(defaultConfiguration);
		defaultConfiguration = null;
		running = false;
	}

	public boolean addConfigURL(URL url) {
		if (url == null)
			return false;

		// stop on a first empty URL entry - we will use it to add our new element
		for (int i = 1; i <= MAX_PROVIDER_URL_COUNT; i++) {
			String tag = PROVIDER_URL_BASE + Integer.toString(i);
			String currentURL = Security.getProperty(tag);
			if (currentURL != null && currentURL.length() != 0)
				continue;
			String path;
			try {
				// in case URL is contained in a JARed bundle, this will extract it into a file system
				path = FileLocator.toFileURL(url).toExternalForm();
			} catch (IOException e) {
				path = url.toExternalForm();
			}
			Security.setProperty(tag, path);
			return true;
		}
		return false;
	}
}
