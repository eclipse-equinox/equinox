/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.equinox.bidi.internal.tests;

import static org.junit.Assert.assertEquals;

import org.eclipse.equinox.bidi.advanced.*;
import org.junit.Test;

/**
 * Tests RTL arithmetic
 */
public class StructuredTextMathTest extends StructuredTextTestBase {

	private final StructuredTextEnvironment envLTR = new StructuredTextEnvironment("ar", false,
			StructuredTextEnvironment.ORIENT_LTR);
	private final StructuredTextEnvironment envRTL = new StructuredTextEnvironment("ar", false,
			StructuredTextEnvironment.ORIENT_RTL);

	private final IStructuredTextExpert expertLTR = StructuredTextExpertFactory.getStatefulExpert(new StructuredTextMath(),
			envLTR);
	private final IStructuredTextExpert expertRTL = StructuredTextExpertFactory.getStatefulExpert(new StructuredTextMath(),
			envRTL);

	private void verifyOneLine(String msg, String data, String resLTR, String resRTL) {
		String lean = toUT16(data);
		String fullLTR = expertLTR.leanToFullText(lean);
		assertEquals(msg + " LTR - ", resLTR, toPseudo(fullLTR));

		String fullRTL = expertRTL.leanToFullText(lean);
		assertEquals(msg + " RTL - ", resRTL, toPseudo(fullRTL));
	}

	@Test
	public void testRTLarithmetic() {
		verifyOneLine("Math #0", "", "", "");
		verifyOneLine("Math #1", "1+ABC", "1+ABC", ">@1+ABC@^");
		verifyOneLine("Math #2", "2+ABC-DEF", "2+ABC@-DEF", ">@2+ABC@-DEF@^");
		verifyOneLine("Math #3", "A+3*BC/DEF", "A@+3*BC@/DEF", ">@A@+3*BC@/DEF@^");
		verifyOneLine("Math #4", "4+ABC/DEF", "4+ABC@/DEF", ">@4+ABC@/DEF@^");

		verifyOneLine("Math #5", "5#BC", "<&5#BC&^", "5#BC");
		verifyOneLine("Math #6", "6#BC-DE", "<&6#BC-DE&^", "6#BC-DE");
		verifyOneLine("Math #7", "7#BC+DE", "<&7#BC+DE&^", "7#BC+DE");
		verifyOneLine("Math #8", "8#BC*DE", "<&8#BC*DE&^", "8#BC*DE");
		verifyOneLine("Math #9", "9#BC/DE", "<&9#BC/DE&^", "9#BC/DE");
		verifyOneLine("Math #10", "10ab+cd-ef", "10ab+cd-ef", ">@10ab+cd-ef@^");
	}
}
