/*******************************************************************************
 * Copyright (c) 2008, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Christian Georgi (SAP SE) - Bug 460430: environment variable for secure store
 *     Maxime Porhel (Obeo) - #652: handle @user.home in -eclipse.password file retrieval
 *******************************************************************************/
package org.eclipse.equinox.internal.security.storage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.crypto.spec.PBEKeySpec;
import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.provider.IProviderHints;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.util.NLS;

public class SecurePreferencesMapper {

	/**
	 * Command line argument specifying default location
	 */
	private static final String KEYRING_ARGUMENT = "-eclipse.keyring"; //$NON-NLS-1$

	/**
	 * Environment variable name for the location
	 */
	private static final String KEYRING_ENVIRONMENT = "ECLIPSE_KEYRING"; //$NON-NLS-1$

	/**
	 * Command line argument specifying default password
	 */
	private static final String PASSWORD_ARGUMENT = "-eclipse.password"; //$NON-NLS-1$

	private static ISecurePreferences defaultPreferences = null;

	private static Map<String, SecurePreferencesRoot> preferences = new HashMap<>(); // URL.toString() ->
																						// SecurePreferencesRoot

	public static final String USER_HOME = "user.home"; //$NON-NLS-1$

	public static ISecurePreferences getDefault() {
		if (defaultPreferences == null) {
			try {
				defaultPreferences = open(null, null);
			} catch (IOException e) {
				AuthPlugin.getDefault().logError(SecAuthMessages.keyringNotAvailable, e);
			}
		}
		return defaultPreferences;
	}

	public static void clearDefault() {
		if (defaultPreferences == null) {
			return;
		}
		try {
			defaultPreferences.flush();
		} catch (IOException e) {
			// ignore in this context
		}
		close((((SecurePreferencesWrapper) defaultPreferences).getContainer().getRootData()));
		defaultPreferences = null;
	}

	public static ISecurePreferences open(URL location, Map<Object, Object> options) throws IOException {
		// 1) find if there are any command line arguments that need to be added
		EnvironmentInfo infoService = AuthPlugin.getDefault().getEnvironmentInfoService();
		if (infoService != null) {
			String[] args = infoService.getNonFrameworkArgs();
			if (args != null && args.length != 0) {
				for (int i = 0; i < args.length - 1; i++) {
					if (args[i + 1].startsWith(("-"))) //$NON-NLS-1$
						continue;
					if (location == null && KEYRING_ARGUMENT.equalsIgnoreCase(args[i])) {
						location = getKeyringFile(args[i + 1]).toURL(); // don't use File.toURI().toURL()
						continue;
					}
					if (PASSWORD_ARGUMENT.equalsIgnoreCase(args[i])) {
						options = processPassword(options, args[i + 1]);
						continue;
					}
				}
			}
		}

		// 2) process location from environment
		String environmentKeyring = System.getenv(KEYRING_ENVIRONMENT);
		if (location == null && environmentKeyring != null) {
			location = getKeyringFile(environmentKeyring).toURL();
		}
		// 3) process default location
		if (location == null) {
			location = StorageUtils.getDefaultLocation();
		}
		if (!StorageUtils.isFile(location)) {
			// at this time we only accept file URLs; check URL type right away
			throw new IOException(NLS.bind(SecAuthMessages.loginFileURL, location.toString()));
		}
		// 3) see if there is already SecurePreferencesRoot at that location; if not
		// open a new one
		String key = location.toString();
		SecurePreferencesRoot root = preferences.get(key);
		if (root == null) {
			root = new SecurePreferencesRoot(location);
			preferences.put(key, root);
		}
		// 4) create container with the options passed in
		SecurePreferencesContainer container = new SecurePreferencesContainer(root, options);
		return container.getPreferences();
	}

	public static void stop() {
		synchronized (preferences) {
			for (SecurePreferencesRoot provider : preferences.values()) {
				try {
					provider.flush();
				} catch (IOException e) {
					// use FrameworkLog directly for shutdown messages - RuntimeLog
					// is empty by this time
					AuthPlugin.getDefault().frameworkLogError(SecAuthMessages.errorOnSave, FrameworkLogEntry.ERROR, e);
				}
			}
			preferences.clear();
		}
	}

	public static void clearPasswordCache() {
		synchronized (preferences) {
			for (SecurePreferencesRoot provider : preferences.values()) {
				provider.clearPasswordCache();
			}
		}
	}

	// Not exposed as API; mostly intended for testing
	public static void close(SecurePreferencesRoot root) {
		if (root != null) {
			synchronized (preferences) {
				preferences.values().remove(root);
			}
		}
	}

	// Replace any @user.home variables found in eclipse.keyring path arg
	private static File getKeyringFile(String path) {
		if (path.startsWith('@' + USER_HOME)) {
			return new File(System.getProperty(USER_HOME), path.substring(USER_HOME.length() + 1));
		}
		return new File(path);
	}

	private static Map<Object, Object> processPassword(Map<Object, Object> options, String arg) {
		if (arg == null || arg.isEmpty()) {
			return options;
		}
		
		String path = arg;
		if (path.startsWith('@' + USER_HOME)) {
			path = System.getProperty(USER_HOME, "") + path.substring(USER_HOME.length() + 1); //$NON-NLS-1$
		}
		
		Path file = Path.of(path);
		if (!Files.isReadable(file)) {
			String msg = NLS.bind(SecAuthMessages.unableToReadPswdFile, arg);
			AuthPlugin.getDefault().logError(msg, null);
			return options;
		}
		try (Stream<String> lines = Files.lines(file)) {
			StringBuilder buffer = new StringBuilder();
			// this eliminates new line characters but that's fine
			lines.forEach(buffer::append);
			if (buffer.isEmpty()) {
				return options;
			}
			if (options == null) {
				options = new HashMap<>(1);
			}
			if (!options.containsKey(IProviderHints.DEFAULT_PASSWORD)) {
				options.put(IProviderHints.DEFAULT_PASSWORD, new PBEKeySpec(buffer.toString().toCharArray()));
			}
		} catch (IOException e) {
			String msg = NLS.bind(SecAuthMessages.unableToReadPswdFile, arg);
			AuthPlugin.getDefault().logError(msg, e);
		}
		return options;
	}

}
