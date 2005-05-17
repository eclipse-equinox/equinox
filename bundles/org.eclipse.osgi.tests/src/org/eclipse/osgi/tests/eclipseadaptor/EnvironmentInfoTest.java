/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.eclipseadaptor;

import junit.framework.TestCase;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.tests.adaptor.testsupport.TestHelper;

public class EnvironmentInfoTest extends TestCase {

	public EnvironmentInfoTest(String name) {
		super(name);
	}

	public void testWindows() {
		assertEquals("1.0", Constants.OS_WIN32, TestHelper.guessOS("Windows XP"));
		assertEquals("1.1", Constants.OS_WIN32, TestHelper.guessOS("Windows 98"));
		assertEquals("1.2", Constants.OS_WIN32, TestHelper.guessOS("Windows 2000"));
		assertEquals("1.3", Constants.OS_WIN32, TestHelper.guessOS("Windows NT"));
		assertEquals("1.4", Constants.OS_WIN32, TestHelper.guessOS("Windows 95"));
		assertEquals("2.0", Constants.WS_WIN32, TestHelper.guessWS(Constants.OS_WIN32));
	}

	public void testLinux() {
		assertEquals("1.0", Constants.OS_LINUX, TestHelper.guessOS("Linux"));
		assertEquals("1.1", Constants.OS_LINUX, TestHelper.guessOS("linux"));
		assertEquals("1.2", Constants.OS_UNKNOWN, TestHelper.guessOS("linux xyz"));
		assertEquals("2.0", Constants.WS_MOTIF, TestHelper.guessWS(Constants.OS_LINUX));
	}

	public void testMacOSX() {
		assertEquals("1.0", Constants.OS_MACOSX, TestHelper.guessOS("Mac OS"));
		assertEquals("1.1", Constants.OS_MACOSX, TestHelper.guessOS("Mac OS X"));
		assertEquals("1.2", Constants.OS_MACOSX, TestHelper.guessOS("mac os x"));
		assertEquals("2.0", Constants.WS_CARBON, TestHelper.guessWS(Constants.OS_MACOSX));
	}

	public void testHPUX() {
		assertEquals("1.0", Constants.OS_HPUX, TestHelper.guessOS("HP-UX"));
		assertEquals("1.1", Constants.OS_HPUX, TestHelper.guessOS("hp-ux"));
		assertEquals("1.2", Constants.OS_UNKNOWN, TestHelper.guessOS("hp-ux xyz"));
		assertEquals("2.0", Constants.WS_MOTIF, TestHelper.guessWS(Constants.OS_HPUX));
	}

	public void testAIX() {
		assertEquals("1.0", Constants.OS_AIX, TestHelper.guessOS("AIX"));
		assertEquals("1.1", Constants.OS_AIX, TestHelper.guessOS("aix"));
		assertEquals("1.2", Constants.OS_UNKNOWN, TestHelper.guessOS("aix xyz"));
		assertEquals("2.0", Constants.WS_MOTIF, TestHelper.guessWS(Constants.OS_AIX));
	}

	public void testQNX() {
		assertEquals("1.0", Constants.OS_QNX, TestHelper.guessOS("QNX"));
		assertEquals("1.1", Constants.OS_QNX, TestHelper.guessOS("qnx"));
		assertEquals("1.2", Constants.OS_UNKNOWN, TestHelper.guessOS("qnx xyz"));
		assertEquals("2.0", Constants.WS_PHOTON, TestHelper.guessWS(Constants.OS_QNX));
	}

	public void testSolaris() {
		assertEquals("1.0", Constants.OS_UNKNOWN, TestHelper.guessOS("Solaris"));
		assertEquals("1.1", Constants.OS_UNKNOWN, TestHelper.guessOS("solaris"));
		assertEquals("1.2", Constants.OS_SOLARIS, TestHelper.guessOS("SunOS"));
		assertEquals("1.3", Constants.OS_SOLARIS, TestHelper.guessOS("sunos"));
		assertEquals("1.4", Constants.OS_UNKNOWN, TestHelper.guessOS("solaris xyz"));
		assertEquals("1.4", Constants.OS_UNKNOWN, TestHelper.guessOS("sunos xyz"));
		assertEquals("2.0", Constants.WS_MOTIF, TestHelper.guessWS(Constants.OS_SOLARIS));
	}
}
