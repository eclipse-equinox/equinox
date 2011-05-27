/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.bidi.internal.tests;

import org.eclipse.equinox.bidi.*;

/**
 * Tests RTL arithmetic
 */
public class STextMathTest extends STextTestBase {

	static final STextEnvironment envLTR = new STextEnvironment(null, false, STextEnvironment.ORIENT_LTR);
	static final STextEnvironment envRTL = new STextEnvironment(null, false, STextEnvironment.ORIENT_RTL);

	String type = ISTextTypes.RTL_ARITHMETIC;

	private void verifyOneLine(String msg, String data, String resLTR, String resRTL) {
		String lean = toUT16(data);
		String fullLTR = STextEngine.leanToFullText(type, null, envLTR, lean, null);
		assertEquals(msg + " LTR - ", resLTR, toPseudo(fullLTR));
		String fullRTL = STextEngine.leanToFullText(type, null, envRTL, lean, null);
		assertEquals(msg + " RTL - ", resRTL, toPseudo(fullRTL));
	}

	public void testRTLarithmetic() {
		verifyOneLine("Math #0", "", "", "");
		verifyOneLine("Math #1", "1+abc", "<&1+abc&^", "1+abc");
		verifyOneLine("Math #2", "2+abc-def", "<&2+abc&-def&^", "2+abc&-def");
		verifyOneLine("Math #3", "a+3*bc/def", "<&a&+3*bc&/def&^", "a&+3*bc&/def");
		verifyOneLine("Math #4", "4+abc/def", "<&4+abc&/def&^", "4+abc&/def");
		verifyOneLine("Math #5", "13ABC", "<&13ABC&^", "13ABC");
		verifyOneLine("Math #6", "14ABC-DE", "<&14ABC-DE&^", "14ABC-DE");
		verifyOneLine("Math #7", "15ABC+DE", "<&15ABC+DE&^", "15ABC+DE");
		verifyOneLine("Math #8", "16ABC*DE", "<&16ABC*DE&^", "16ABC*DE");
		verifyOneLine("Math #9", "17ABC/DE", "<&17ABC/DE&^", "17ABC/DE");
	}
}
