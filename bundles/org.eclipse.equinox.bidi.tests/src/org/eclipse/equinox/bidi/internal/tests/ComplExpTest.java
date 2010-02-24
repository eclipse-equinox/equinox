/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.bidi.internal.tests;

import org.eclipse.equinox.bidi.complexp.IComplExpProcessor;

public class ComplExpTest implements IComplExpProcessor {

	private static final int[] EMPTY_INT_ARRAY = new int[0];
	private static final int[][] ALL_LTR = new int[][] { {DIRECTION_LTR, DIRECTION_LTR}, {DIRECTION_LTR, DIRECTION_LTR}};

	public ComplExpTest() {
		return;
	}

	public void setOperators(String operators) {
        // empty
	}
	
	public String getOperators() {
	    return "";
	}
	
	public void selectBidiScript(boolean arabic, boolean hebrew) {
		// empty
	}

	public boolean handlesArabicScript() {
		return false;
	}

	public boolean handlesHebrewScript() {
		return false;
	}

	public String leanToFullText(String text) {
		return text;
	}

	public String leanToFullText(String text, int initState) {
		return text;
	}

	public int[] leanBidiCharOffsets(String text) {
		return EMPTY_INT_ARRAY;
	}

	public int[] leanBidiCharOffsets(String text, int initState) {
		return EMPTY_INT_ARRAY;
	}

	public int[] leanBidiCharOffsets() {
		return EMPTY_INT_ARRAY;
	}

	public int[] fullBidiCharOffsets() {
		return EMPTY_INT_ARRAY;
	}

	public String fullToLeanText(String text) {
		return text;
	}

	public String fullToLeanText(String text, int initState) {
		return text;
	}

	public int getFinalState() {
		return STATE_NOTHING_GOING;
	}

	public int leanToFullPos(int pos) {
		return pos;
	}

	public int fullToLeanPos(int pos) {
		return pos;
	}

	public void assumeMirrored(boolean mirrored) {
		// empty
	}

	public boolean isMirrored() {
		return false;
	}

	public void assumeOrientation(int orientation) {
		// empty
	}

	public int recallOrientation() {
		return ORIENT_LTR;
	}

	public void setArabicDirection(int not_mirrored, int mirrored) {
		// empty
	}

	public void setArabicDirection(int direction) {
		// empty
	}

	public void setHebrewDirection(int not_mirrored, int mirrored) {
		// empty
	}

	public void setHebrewDirection(int direction) {
		// empty
	}

	public void setDirection(int not_mirrored, int mirrored) {
		// empty
	}

	public void setDirection(int direction) {
		// empty
	}

	public int[][] getDirection() {
		return ALL_LTR;
	}

	public int getCurDirection() {
		return DIRECTION_LTR;
	}
}
