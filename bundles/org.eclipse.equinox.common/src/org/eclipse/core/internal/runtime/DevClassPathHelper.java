/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
 *     Sergey Prigogin (Google) - use parameterized types (bug 442021)
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class DevClassPathHelper {

	// command line options
	public static final String PROP_DEV = "osgi.dev"; //$NON-NLS-1$

	static protected boolean inDevelopmentMode = false;
	static protected String[] devDefaultClasspath;
	static protected Properties devProperties = null;

	static {
		// Check the osgi.dev property to see if dev classpath entries have been
		// defined.
		String osgiDev = Activator.getContext() == null ? System.getProperty(PROP_DEV)
				: Activator.getContext().getProperty(PROP_DEV);
		if (osgiDev != null) {
			try {
				inDevelopmentMode = true;
				URL location = new URL(osgiDev);
				devProperties = load(location);
				if (devProperties != null)
					devDefaultClasspath = getArrayFromList(devProperties.getProperty("*")); //$NON-NLS-1$
			} catch (MalformedURLException e) {
				devDefaultClasspath = getArrayFromList(osgiDev);
			}
		}
	}

	public static String[] getDevClassPath(String id) {
		String[] result = null;
		if (id != null && devProperties != null) {
			String entry = devProperties.getProperty(id);
			if (entry != null)
				result = getArrayFromList(entry);
		}
		if (result == null)
			result = devDefaultClasspath;
		return result;
	}

	/**
	 * Returns the result of converting a list of comma-separated tokens into an
	 * array
	 * 
	 * @return the array of string tokens
	 * @param prop the initial comma-separated string
	 */
	public static String[] getArrayFromList(String prop) {
		if (prop == null || prop.trim().equals("")) //$NON-NLS-1$
			return new String[0];
		Vector<String> list = new Vector<>();
		StringTokenizer tokens = new StringTokenizer(prop, ","); //$NON-NLS-1$
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				list.addElement(token);
		}
		return list.isEmpty() ? new String[0] : (String[]) list.toArray(new String[list.size()]);
	}

	public static boolean inDevelopmentMode() {
		return inDevelopmentMode;
	}

	/*
	 * Load the given properties file
	 */
	private static Properties load(URL url) {
		Properties props = new Properties();
		try {
			try (InputStream is = url.openStream()) {
				props.load(is);
			}
		} catch (IOException e) {
			// TODO consider logging here
		}
		return props;
	}
}
