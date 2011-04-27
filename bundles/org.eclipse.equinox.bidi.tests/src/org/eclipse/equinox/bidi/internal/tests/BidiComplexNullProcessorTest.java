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
import org.eclipse.equinox.bidi.custom.BidiComplexFeatures;

/**
 * Tests RTL arithmetic
 */
public class BidiComplexNullProcessorTest extends BidiComplexTestBase {

	static final int[] EMPTY_INT_ARRAY = new int[0];

	public void testNullProcessor() {
		String full = BidiComplexEngine.leanToFullText(null, null, null, "abc", null);
		assertEquals("leanToFullText", "abc", full);
		int[] state = new int[1];
		state[0] = 3;
		full = BidiComplexEngine.leanToFullText(null, null, null, "abc", state);
		assertEquals("leanToFullText with state", "abc", full);
		int[] offsets = BidiComplexEngine.leanBidiCharOffsets(null, null, null, "abc", null);
		assertEquals("leanBidiCharOffsets", 0, offsets.length);
		offsets = BidiComplexEngine.fullBidiCharOffsets(null, null, null, "abc", null);
		assertEquals("fullBidiCharOffsets", 0, offsets.length);
		String lean = BidiComplexEngine.fullToLeanText(null, null, null, "abc", null);
		assertEquals("fullToLeanText", "abc", lean);
		lean = BidiComplexEngine.fullToLeanText(null, null, null, "abc", state);
		assertEquals("fullToLeanText with state", "abc", lean);
		int[] map = BidiComplexEngine.leanToFullMap(null, null, null, "abc", null);
		int[] model = {0, 1, 2};
		assertEquals("leanToFullMap", array_display(model), array_display(map));
		map = BidiComplexEngine.fullToLeanMap(null, null, null, "abc", null);
		assertEquals("fullToLeanMap", array_display(model), array_display(map));
		int direction = BidiComplexEngine.getCurDirection(null, null, null, "abc");
		assertEquals("getCurDirection", BidiComplexFeatures.DIR_LTR, direction);
	}
}
