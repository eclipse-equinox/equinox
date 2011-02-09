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
import org.eclipse.equinox.bidi.custom.BidiComplexProcessor;
import org.eclipse.equinox.bidi.custom.IBidiComplexProcessor;

/**
 * Tests some weird cases
 */

public class BidiComplexSomeMoreTest extends BidiComplexTestBase {

	final static BidiComplexEnvironment env1 = new BidiComplexEnvironment("en_US", false, BidiComplexEnvironment.ORIENT_LTR);

	final static BidiComplexEnvironment env2 = new BidiComplexEnvironment("he", false, BidiComplexEnvironment.ORIENT_LTR);

	final static BidiComplexFeatures features = new BidiComplexFeatures(null, 1, -1, -1, false, false);

	BidiComplexHelper helper;

	class Processor1 extends BidiComplexProcessor {

		public BidiComplexFeatures init(BidiComplexHelper caller, BidiComplexEnvironment env) {
			return features;
		}

		public int indexOfSpecial(BidiComplexHelper caller, int caseNumber, String srcText, int fromIndex) {
			return fromIndex;
		}

		public int processSpecial(BidiComplexHelper caller, int caseNumber, String srcText, int operLocation) {
			int len = srcText.length();
			for (int i = len - 1; i >= 0; i--) {
				caller.insertMark(i);
				caller.insertMark(i);
			}
			return len;
		}
	}

	class Processor2 extends BidiComplexProcessor {

		public BidiComplexFeatures init(BidiComplexHelper caller, BidiComplexEnvironment env) {
			return features;
		}
	}

	class Processor3 extends BidiComplexProcessor {

		public BidiComplexFeatures init(BidiComplexHelper caller, BidiComplexEnvironment env) {
			return features;
		}

		public int indexOfSpecial(BidiComplexHelper caller, int caseNumber, String srcText, int fromIndex) {
			return 0;
		}
	}

	public void testSomeMore() {
		assertFalse(env1.isBidi());
		assertTrue(env2.isBidi());

		IBidiComplexProcessor processor = new Processor1();
		helper = new BidiComplexHelper(processor, env1);
		String full = toPseudo(helper.leanToFullText("abcd"));
		assertEquals("@a@b@c@d", full);

		processor = new Processor2();
		helper = new BidiComplexHelper(processor, env1);
		boolean catchFlag = false;
		try {
			full = toPseudo(helper.leanToFullText("abcd"));
		} catch (IllegalStateException e) {
			catchFlag = true;
		}
		assertTrue("Catch missing indexOfSpecial", catchFlag);

		processor = new Processor3();
		helper = new BidiComplexHelper(processor, env1);
		catchFlag = false;
		try {
			full = toPseudo(helper.leanToFullText("abcd"));
		} catch (IllegalStateException e) {
			catchFlag = true;
		}
		assertTrue("Catch missing processSpecial", catchFlag);
	}

}
