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

import org.eclipse.equinox.bidi.BidiComplexEngine;
import org.eclipse.equinox.bidi.BidiComplexEnvironment;
import org.eclipse.equinox.bidi.custom.*;

/**
 * Tests some weird cases
 */

public class BidiComplexSomeMoreTest extends BidiComplexTestBase {

	final static BidiComplexEnvironment env1 = new BidiComplexEnvironment("en_US", false, BidiComplexEnvironment.ORIENT_LTR);
	final static BidiComplexEnvironment env2 = new BidiComplexEnvironment("he", false, BidiComplexEnvironment.ORIENT_LTR);
	final static BidiComplexFeatures myFeatures = new BidiComplexFeatures(null, 1, -1, -1, false, false);

	class Processor1 extends BidiComplexProcessor {

		public BidiComplexFeatures getFeatures(BidiComplexEnvironment env) {
			return myFeatures;
		}

		public int indexOfSpecial(BidiComplexFeatures features, String text, byte[] dirProps, int[] offsets, int caseNumber, int fromIndex) {
			return fromIndex;
		}

		public int processSpecial(BidiComplexFeatures features, String text, byte[] dirProps, int[] offsets, int[] state, int caseNumber, int separLocation) {
			int len = text.length();
			for (int i = len - 1; i >= 0; i--) {
				BidiComplexProcessor.insertMark(text, dirProps, offsets, i);
				BidiComplexProcessor.insertMark(text, dirProps, offsets, i);
			}
			return len;
		}
	}

	class Processor2 extends BidiComplexProcessor {

		public BidiComplexFeatures getFeatures(BidiComplexEnvironment env) {
			return myFeatures;
		}
	}

	class Processor3 extends BidiComplexProcessor {

		public BidiComplexFeatures getFeatures(BidiComplexEnvironment env) {
			return myFeatures;
		}

		public int indexOfSpecial(BidiComplexFeatures features, String text, byte[] dirProps, int[] offsets, int caseNumber, int fromIndex) {
			return 0;
		}
	}

	public void testSomeMore() {
		assertFalse(env1.isBidi());
		assertTrue(env2.isBidi());

		IBidiComplexProcessor processor = new Processor1();
		String full = BidiComplexEngine.leanToFullText(processor, null, env1, "abcd", null);
		assertEquals("@a@b@c@d", toPseudo(full));

		processor = new Processor2();
		boolean catchFlag = false;
		try {
			full = BidiComplexEngine.leanToFullText(processor, null, env1, "abcd", null);
		} catch (IllegalStateException e) {
			catchFlag = true;
		}
		assertTrue("Catch missing indexOfSpecial", catchFlag);

		processor = new Processor3();
		catchFlag = false;
		try {
			full = BidiComplexEngine.leanToFullText(processor, null, env1, "abcd", null);
		} catch (IllegalStateException e) {
			catchFlag = true;
		}
		assertTrue("Catch missing processSpecial", catchFlag);
	}

}
