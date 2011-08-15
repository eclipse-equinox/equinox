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

/**
 * Test edge conditions.
 */
public class STextSomeMoreTest extends STextTestBase {

	final static STextEnvironment env1 = new STextEnvironment("en_US", false, STextEnvironment.ORIENT_LTR);
	final static STextEnvironment env2 = new STextEnvironment("he", false, STextEnvironment.ORIENT_LTR);

	public void testSomeMore() {
		assertFalse(env1.isProcessingNeeded());
		assertTrue(env2.isProcessingNeeded());

		ISTextExpert expert1 = STextExpertFactory.getExpert("test.Handler1", env1);
		String full = expert1.leanToFullText("abcd");
		assertEquals("@a@b@c@d", toPseudo(full));

		ISTextExpert expert2 = STextExpertFactory.getExpert("test.Handler2", env1);
		boolean catchFlag = false;
		try {
			full = expert2.leanToFullText("abcd");
		} catch (IllegalStateException e) {
			catchFlag = true;
		}
		assertTrue("Catch missing indexOfSpecial", catchFlag);

		catchFlag = false;
		ISTextExpert expert3 = STextExpertFactory.getExpert("test.Handler3", env1);
		try {
			full = expert3.leanToFullText("abcd");
		} catch (IllegalStateException e) {
			catchFlag = true;
		}
		assertTrue("Catch missing processSpecial", catchFlag);
	}

}
