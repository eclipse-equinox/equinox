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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.equinox.bidi.advanced.*;
import org.eclipse.equinox.bidi.custom.*;
import org.junit.Test;

/**
 * Test edge conditions.
 */
public class StructuredTextSomeMoreTest extends StructuredTextTestBase {

	private class TestHandler1 extends StructuredTextTypeHandler {

		public TestHandler1() {
			//empty constructor
		}

		public int getSpecialsCount(IStructuredTextExpert expert) {
			return 1;
		}

		public int indexOfSpecial(IStructuredTextExpert expert, String text, StructuredTextCharTypes charTypes, StructuredTextOffsets offsets, int caseNumber, int fromIndex) {
			return fromIndex;
		}

		public int processSpecial(IStructuredTextExpert expert, String text, StructuredTextCharTypes charTypes, StructuredTextOffsets offsets, int caseNumber, int separLocation) {
			int len = text.length();
			for (int i = len - 1; i >= 0; i--) {
				StructuredTextTypeHandler.insertMark(text, charTypes, offsets, i);
				StructuredTextTypeHandler.insertMark(text, charTypes, offsets, i);
			}
			return len;
		}
	}

	private class TestHandler2 extends StructuredTextTypeHandler {

		public TestHandler2() {
			//empty constructor
		}

		public int getSpecialsCount(IStructuredTextExpert expert) {
			return 1;
		}
	}

	private class TestHandler3 extends StructuredTextTypeHandler {

		public TestHandler3() {
			//empty constructor
		}

		public int getSpecialsCount(IStructuredTextExpert expert) {
			return 1;
		}

		public int indexOfSpecial(IStructuredTextExpert expert, String text, StructuredTextCharTypes charTypes, StructuredTextOffsets offsets, int caseNumber, int fromIndex) {
			return fromIndex;
		}
	}

	final static StructuredTextEnvironment env1 = new StructuredTextEnvironment("en_US", false, StructuredTextEnvironment.ORIENT_LTR);
	final static StructuredTextEnvironment env2 = new StructuredTextEnvironment("he", false, StructuredTextEnvironment.ORIENT_LTR);

	@Test
	public void testSomeMore() {
		assertFalse(env1.isProcessingNeeded());
		assertTrue(env2.isProcessingNeeded());

		StructuredTextTypeHandler handler1 = new TestHandler1();
		IStructuredTextExpert expert1 = StructuredTextExpertFactory.getStatefulExpert(handler1, env1);
		String full = expert1.leanToFullText("abcd");
		assertEquals("@a@b@c@d", toPseudo(full));

		StructuredTextTypeHandler handler2 = new TestHandler2();
		IStructuredTextExpert expert2 = StructuredTextExpertFactory.getStatefulExpert(handler2, env1);
		boolean catchFlag = false;
		try {
			full = expert2.leanToFullText("abcd");
		} catch (IllegalStateException e) {
			catchFlag = true;
		}
		assertTrue("Catch missing indexOfSpecial", catchFlag);

		StructuredTextTypeHandler handler3 = new TestHandler3();
		IStructuredTextExpert expert3 = StructuredTextExpertFactory.getStatefulExpert(handler3, env1);
		catchFlag = false;
		try {
			full = expert3.leanToFullText("abcd");
		} catch (IllegalStateException e) {
			catchFlag = true;
		}
		assertTrue("Catch missing processSpecial", catchFlag);
	}

}
