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

import junit.framework.*;
import org.eclipse.core.runtime.internal.adaptor.EclipseEnvironmentInfo;
import org.eclipse.osgi.service.environment.Constants;

public class EnvironmentInfoTest extends TestCase {

	public static Test suite() {
		return new TestSuite(EnvironmentInfoTest.class);
	}

	public EnvironmentInfoTest(String name) {
		super(name);
	}

	public void testAIX() {
		assertEquals("1.0", Constants.OS_AIX, EclipseEnvironmentInfo.guessOS("AIX"));
		assertEquals("1.1", Constants.OS_AIX, EclipseEnvironmentInfo.guessOS("aix"));
		assertEquals("1.2", Constants.OS_UNKNOWN, EclipseEnvironmentInfo.guessOS("aix xyz"));
		assertEquals("2.0", Constants.WS_MOTIF, EclipseEnvironmentInfo.guessWS(Constants.OS_AIX));
	}

	public void testHPUX() {
		assertEquals("1.0", Constants.OS_HPUX, EclipseEnvironmentInfo.guessOS("HP-UX"));
		assertEquals("1.1", Constants.OS_HPUX, EclipseEnvironmentInfo.guessOS("hp-ux"));
		assertEquals("1.2", Constants.OS_UNKNOWN, EclipseEnvironmentInfo.guessOS("hp-ux xyz"));
		assertEquals("2.0", Constants.WS_MOTIF, EclipseEnvironmentInfo.guessWS(Constants.OS_HPUX));
	}

	public void testLinux() {
		assertEquals("1.0", Constants.OS_LINUX, EclipseEnvironmentInfo.guessOS("Linux"));
		assertEquals("1.1", Constants.OS_LINUX, EclipseEnvironmentInfo.guessOS("linux"));
		assertEquals("1.2", Constants.OS_UNKNOWN, EclipseEnvironmentInfo.guessOS("linux xyz"));
		assertEquals("2.0", Constants.WS_MOTIF, EclipseEnvironmentInfo.guessWS(Constants.OS_LINUX));
	}

	public void testMacOSX() {
		assertEquals("1.0", Constants.OS_MACOSX, EclipseEnvironmentInfo.guessOS("Mac OS"));
		assertEquals("1.1", Constants.OS_MACOSX, EclipseEnvironmentInfo.guessOS("Mac OS X"));
		assertEquals("1.2", Constants.OS_MACOSX, EclipseEnvironmentInfo.guessOS("mac os x"));
		assertEquals("2.0", Constants.WS_CARBON, EclipseEnvironmentInfo.guessWS(Constants.OS_MACOSX));
	}

	public void testQNX() {
		assertEquals("1.0", Constants.OS_QNX, EclipseEnvironmentInfo.guessOS("QNX"));
		assertEquals("1.1", Constants.OS_QNX, EclipseEnvironmentInfo.guessOS("qnx"));
		assertEquals("1.2", Constants.OS_UNKNOWN, EclipseEnvironmentInfo.guessOS("qnx xyz"));
		assertEquals("2.0", Constants.WS_PHOTON, EclipseEnvironmentInfo.guessWS(Constants.OS_QNX));
	}

	public void testSolaris() {
		assertEquals("1.0", Constants.OS_UNKNOWN, EclipseEnvironmentInfo.guessOS("Solaris"));
		assertEquals("1.1", Constants.OS_UNKNOWN, EclipseEnvironmentInfo.guessOS("solaris"));
		assertEquals("1.2", Constants.OS_SOLARIS, EclipseEnvironmentInfo.guessOS("SunOS"));
		assertEquals("1.3", Constants.OS_SOLARIS, EclipseEnvironmentInfo.guessOS("sunos"));
		assertEquals("1.4", Constants.OS_UNKNOWN, EclipseEnvironmentInfo.guessOS("solaris xyz"));
		assertEquals("1.4", Constants.OS_UNKNOWN, EclipseEnvironmentInfo.guessOS("sunos xyz"));
		assertEquals("2.0", Constants.WS_MOTIF, EclipseEnvironmentInfo.guessWS(Constants.OS_SOLARIS));
	}

	public void testWindows() {
		assertEquals("1.0", Constants.OS_WIN32, EclipseEnvironmentInfo.guessOS("Windows XP"));
		assertEquals("1.1", Constants.OS_WIN32, EclipseEnvironmentInfo.guessOS("Windows 98"));
		assertEquals("1.2", Constants.OS_WIN32, EclipseEnvironmentInfo.guessOS("Windows 2000"));
		assertEquals("1.3", Constants.OS_WIN32, EclipseEnvironmentInfo.guessOS("Windows NT"));
		assertEquals("1.4", Constants.OS_WIN32, EclipseEnvironmentInfo.guessOS("Windows 95"));
		assertEquals("2.0", Constants.WS_WIN32, EclipseEnvironmentInfo.guessWS(Constants.OS_WIN32));
	}
}
