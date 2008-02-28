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
package org.eclipse.equinox.internal.security.tests.storage;

import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests Windows module, if available.
 *
 */
public class WinPreferencesTest extends SecurePreferencesTest {

	/**
	 * Unique ID of the Windows module.
	 */
	static private final String WIN_MODULE_ID = "org.eclipse.equinox.security.WindowsPasswordProvider"; //$NON-NLS-1$

	protected String getModuleID() {
		return WIN_MODULE_ID;
	}

	public static Test suite() {
		return new TestSuite(WinPreferencesTest.class);
	}

	protected Map getOptions() {
		// Don't specify default password when testing specific password provider
		return getOptions(null);
	}
}
