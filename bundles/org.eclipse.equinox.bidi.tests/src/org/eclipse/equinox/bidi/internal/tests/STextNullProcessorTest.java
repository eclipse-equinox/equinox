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


/**
 * Tests null processor
 */
public class STextNullProcessorTest extends STextTestBase {

	static final int[] EMPTY_INT_ARRAY = new int[0];

	public void testNullProcessor() {
		/* not needed
		String full = STextEngine.leanToFullText(null, null, "abc", null);
		assertEquals("leanToFullText", "abc", full);
		int[] state = new int[1];
		state[0] = 3;
		full = STextEngine.leanToFullText(null, null, "abc", state);
		assertEquals("leanToFullText with state", "abc", full);
		int[] offsets = STextEngine.leanBidiCharOffsets(null, null, "abc", null);
		assertEquals("leanBidiCharOffsets", 0, offsets.length);
		offsets = STextEngine.fullBidiCharOffsets(null, null, "abc", null);
		assertEquals("fullBidiCharOffsets", 0, offsets.length);
		String lean = STextEngine.fullToLeanText(null, null, "abc", null);
		assertEquals("fullToLeanText", "abc", lean);
		lean = STextEngine.fullToLeanText(null, null, "abc", state);
		assertEquals("fullToLeanText with state", "abc", lean);
		int[] map = STextEngine.leanToFullMap(null, null, "abc", null);
		int[] model = {0, 1, 2};
		assertEquals("leanToFullMap", array_display(model), array_display(map));
		map = STextEngine.fullToLeanMap(null, null, "abc", null);
		assertEquals("fullToLeanMap", array_display(model), array_display(map));
		int direction = STextEngine.getCurDirection(null, null, "abc");
		assertEquals("getCurDirection", STextEngine.DIR_LTR, direction);
		*/
	}
}
