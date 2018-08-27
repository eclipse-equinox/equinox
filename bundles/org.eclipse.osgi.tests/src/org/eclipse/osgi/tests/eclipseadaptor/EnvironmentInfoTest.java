/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.osgi.tests.eclipseadaptor;

import org.eclipse.osgi.internal.framework.EquinoxConfiguration;

import junit.framework.*;
import org.eclipse.osgi.service.environment.Constants;

public class EnvironmentInfoTest extends TestCase {

	public static Test suite() {
		return new TestSuite(EnvironmentInfoTest.class);
	}

	public EnvironmentInfoTest(String name) {
		super(name);
	}

	public void testAIX() {
		assertEquals("1.0", Constants.OS_AIX, EquinoxConfiguration.guessOS("AIX"));
		assertEquals("1.1", Constants.OS_AIX, EquinoxConfiguration.guessOS("aix"));
		assertEquals("1.2", Constants.OS_UNKNOWN, EquinoxConfiguration.guessOS("aix xyz"));
		assertEquals("2.0", Constants.WS_MOTIF, EquinoxConfiguration.guessWS(Constants.OS_AIX));
	}

	public void testHPUX() {
		assertEquals("1.0", Constants.OS_HPUX, EquinoxConfiguration.guessOS("HP-UX"));
		assertEquals("1.1", Constants.OS_HPUX, EquinoxConfiguration.guessOS("hp-ux"));
		assertEquals("1.2", Constants.OS_UNKNOWN, EquinoxConfiguration.guessOS("hp-ux xyz"));
		assertEquals("2.0", Constants.WS_MOTIF, EquinoxConfiguration.guessWS(Constants.OS_HPUX));
	}

	public void testLinux() {
		assertEquals("1.0", Constants.OS_LINUX, EquinoxConfiguration.guessOS("Linux"));
		assertEquals("1.1", Constants.OS_LINUX, EquinoxConfiguration.guessOS("linux"));
		assertEquals("1.2", Constants.OS_UNKNOWN, EquinoxConfiguration.guessOS("linux xyz"));
		assertEquals("2.0", Constants.WS_GTK, EquinoxConfiguration.guessWS(Constants.OS_LINUX));
	}

	public void testMacOSX() {
		assertEquals("1.0", Constants.OS_MACOSX, EquinoxConfiguration.guessOS("Mac OS"));
		assertEquals("1.1", Constants.OS_MACOSX, EquinoxConfiguration.guessOS("Mac OS X"));
		assertEquals("1.2", Constants.OS_MACOSX, EquinoxConfiguration.guessOS("mac os x"));
		assertEquals("2.0", Constants.WS_COCOA, EquinoxConfiguration.guessWS(Constants.OS_MACOSX));
	}

	public void testQNX() {
		assertEquals("1.0", Constants.OS_QNX, EquinoxConfiguration.guessOS("QNX"));
		assertEquals("1.1", Constants.OS_QNX, EquinoxConfiguration.guessOS("qnx"));
		assertEquals("1.2", Constants.OS_UNKNOWN, EquinoxConfiguration.guessOS("qnx xyz"));
		assertEquals("2.0", Constants.WS_PHOTON, EquinoxConfiguration.guessWS(Constants.OS_QNX));
	}

	public void testSolaris() {
		assertEquals("1.0", Constants.OS_UNKNOWN, EquinoxConfiguration.guessOS("Solaris"));
		assertEquals("1.1", Constants.OS_UNKNOWN, EquinoxConfiguration.guessOS("solaris"));
		assertEquals("1.2", Constants.OS_SOLARIS, EquinoxConfiguration.guessOS("SunOS"));
		assertEquals("1.3", Constants.OS_SOLARIS, EquinoxConfiguration.guessOS("sunos"));
		assertEquals("1.4", Constants.OS_UNKNOWN, EquinoxConfiguration.guessOS("solaris xyz"));
		assertEquals("1.4", Constants.OS_UNKNOWN, EquinoxConfiguration.guessOS("sunos xyz"));
		assertEquals("2.0", Constants.WS_GTK, EquinoxConfiguration.guessWS(Constants.OS_SOLARIS));
	}

	public void testWindows() {
		assertEquals("1.0", Constants.OS_WIN32, EquinoxConfiguration.guessOS("Windows XP"));
		assertEquals("1.1", Constants.OS_WIN32, EquinoxConfiguration.guessOS("Windows 98"));
		assertEquals("1.2", Constants.OS_WIN32, EquinoxConfiguration.guessOS("Windows 2000"));
		assertEquals("1.3", Constants.OS_WIN32, EquinoxConfiguration.guessOS("Windows NT"));
		assertEquals("1.4", Constants.OS_WIN32, EquinoxConfiguration.guessOS("Windows 95"));
		assertEquals("2.0", Constants.WS_WIN32, EquinoxConfiguration.guessWS(Constants.OS_WIN32));
	}

	public void testISeries() {
		assertEquals("1.0", Constants.OS_OS400, EquinoxConfiguration.guessOS("OS/400"));
		assertEquals("1.1", Constants.OS_OS400, EquinoxConfiguration.guessOS("os/400"));
		assertEquals("1.2", Constants.OS_UNKNOWN, EquinoxConfiguration.guessOS("OS/400 xyz"));
		assertEquals("2.0", Constants.WS_UNKNOWN, EquinoxConfiguration.guessWS(Constants.OS_OS400));
	}

	public void testZSeries() {
		assertEquals("1.0", Constants.OS_OS390, EquinoxConfiguration.guessOS("OS/390"));
		assertEquals("1.1", Constants.OS_OS390, EquinoxConfiguration.guessOS("os/390"));
		assertEquals("1.1", Constants.OS_ZOS, EquinoxConfiguration.guessOS("z/os"));
		assertEquals("1.1", Constants.OS_ZOS, EquinoxConfiguration.guessOS("Z/OS"));
		assertEquals("1.2", Constants.OS_UNKNOWN, EquinoxConfiguration.guessOS("OS/400 xyz"));
		assertEquals("1.2", Constants.OS_UNKNOWN, EquinoxConfiguration.guessOS("z/os xyz"));
		assertEquals("2.0", Constants.WS_UNKNOWN, EquinoxConfiguration.guessWS(Constants.OS_OS390));
	}
}
