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

import org.eclipse.equinox.bidi.advanced.*;
import org.eclipse.equinox.bidi.custom.*;

/**
 * Test edge conditions.
 */
public class STextSomeMoreTest extends STextTestBase {

	private class TestHandler1 extends STextTypeHandler {

		public TestHandler1() {
			//empty constructor
		}

		public int getSpecialsCount(ISTextExpert expert) {
			return 1;
		}

		public int indexOfSpecial(ISTextExpert expert, String text, STextCharTypes charTypes, STextOffsets offsets, int caseNumber, int fromIndex) {
			return fromIndex;
		}

		public int processSpecial(ISTextExpert expert, String text, STextCharTypes charTypes, STextOffsets offsets, int caseNumber, int separLocation) {
			int len = text.length();
			for (int i = len - 1; i >= 0; i--) {
				STextTypeHandler.insertMark(text, charTypes, offsets, i);
				STextTypeHandler.insertMark(text, charTypes, offsets, i);
			}
			return len;
		}
	}

	private class TestHandler2 extends STextTypeHandler {

		public TestHandler2() {
			//empty constructor
		}

		public int getSpecialsCount(ISTextExpert expert) {
			return 1;
		}
	}

	private class TestHandler3 extends STextTypeHandler {

		public TestHandler3() {
			//empty constructor
		}

		public int getSpecialsCount(ISTextExpert expert) {
			return 1;
		}

		public int indexOfSpecial(ISTextExpert expert, String text, STextCharTypes charTypes, STextOffsets offsets, int caseNumber, int fromIndex) {
			return fromIndex;
		}
	}

	final static STextEnvironment env1 = new STextEnvironment("en_US", false, STextEnvironment.ORIENT_LTR);
	final static STextEnvironment env2 = new STextEnvironment("he", false, STextEnvironment.ORIENT_LTR);

	public void testSomeMore() {
		assertFalse(env1.isProcessingNeeded());
		assertTrue(env2.isProcessingNeeded());

		STextTypeHandler handler1 = new TestHandler1();
		ISTextExpert expert1 = STextExpertFactory.getStatefulExpert(handler1, env1);
		String full = expert1.leanToFullText("abcd");
		assertEquals("@a@b@c@d", toPseudo(full));

		STextTypeHandler handler2 = new TestHandler2();
		ISTextExpert expert2 = STextExpertFactory.getStatefulExpert(handler2, env1);
		boolean catchFlag = false;
		try {
			full = expert2.leanToFullText("abcd");
		} catch (IllegalStateException e) {
			catchFlag = true;
		}
		assertTrue("Catch missing indexOfSpecial", catchFlag);

		STextTypeHandler handler3 = new TestHandler3();
		ISTextExpert expert3 = STextExpertFactory.getStatefulExpert(handler3, env1);
		catchFlag = false;
		try {
			full = expert3.leanToFullText("abcd");
		} catch (IllegalStateException e) {
			catchFlag = true;
		}
		assertTrue("Catch missing processSpecial", catchFlag);
	}

}
