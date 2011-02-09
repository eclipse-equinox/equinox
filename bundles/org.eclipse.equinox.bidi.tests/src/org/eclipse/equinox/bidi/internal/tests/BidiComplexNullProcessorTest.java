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
public class BidiComplexNullProcessorTest extends BidiComplexTestBase {

	static final int[] EMPTY_INT_ARRAY = new int[0];

	private BidiComplexHelper helper;

	protected void setUp() throws Exception {
		super.setUp();
		helper = new BidiComplexHelper();
	}

	public void testNullProcessor() {
		String full = helper.leanToFullText("abc");
		assertEquals("leanToFullText", "abc", full);
		full = helper.leanToFullText("abc", 3);
		assertEquals("leanToFullText with state", "abc", full);
		int[] offsets = helper.leanBidiCharOffsets();
		assertTrue("leanBidiCharOffsets", arrays_equal(offsets, EMPTY_INT_ARRAY));
		offsets = helper.fullBidiCharOffsets();
		assertTrue("fullBidiCharOffsets", arrays_equal(offsets, EMPTY_INT_ARRAY));
		String lean = helper.fullToLeanText("abc");
		assertEquals("fullToLeanText", "abc", lean);
		lean = helper.fullToLeanText("abc", 3);
		assertEquals("fullToLeanText with state", "abc", lean);
		int state = helper.getFinalState();
		assertEquals("getFinalState", BidiComplexHelper.STATE_NOTHING_GOING, state);
		int pos = helper.leanToFullPos(13);
		assertEquals("leanToFullPos", 13, pos);
		pos = helper.fullToLeanPos(15);
		assertEquals("fullToLeanPos", 15, pos);
		assertEquals("getDirProp", Character.DIRECTIONALITY_UNDEFINED, helper.getDirProp(123));
		int direction = helper.getCurDirection();
		assertEquals("getCurDirection", BidiComplexFeatures.DIR_LTR, direction);
		BidiComplexEnvironment env = helper.getEnvironment();
		assertEquals("getEnvironment", BidiComplexEnvironment.DEFAULT, env);
		helper.setEnvironment(env);
		BidiComplexFeatures features = helper.getFeatures();
		assertEquals("getFeatures", BidiComplexFeatures.DEFAULT, features);
		helper.setFeatures(features);
	}
}
