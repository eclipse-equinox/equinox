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
package org.eclipse.equinox.bidi.internal.complexp;

import org.eclipse.equinox.bidi.complexp.IComplExpProcessor;

/**
 *  This class is a minimal processor which implements the
 *  <code>IComplExpProcessor</code> interface but does no real processing.
 *  Since it is optimized for minimal overhead, it can
 *  be used as a low-cost default processor when no complex expression is
 *  involved and no processing is needed, but a general framework of
 *  handling complex expressions must be preserved.
 *
 *  @author Matitiahu Allouche
 */
// TBD is this needed?
public class ComplExpDoNothing implements IComplExpProcessor {

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private static final int[] EMPTY_INT_ARRAY = new int[0];
	private static final int[][] ALL_LTR = new int[][] { {DIRECTION_LTR, DIRECTION_LTR}, {DIRECTION_LTR, DIRECTION_LTR}};

	/**
	 *  Allocate a <code>ComplExpDoNothing</code> processor instance.
	 *  Such a processor does not modify text submitted to it, and can be
	 *  used as a place holder when the text to process is not a known
	 *  complex expression.
	 */
	public ComplExpDoNothing() {
		return;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code>
	 *  this method does nothing.
	 */
	public void setOperators(String operators) {
		// empty
	}

	/**
	 *  For class <code>ComplExpDoNothing</code>
	 *  this method returns a zero-length string.
	 */
	public String getOperators() {
		return EMPTY_STRING;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code>
	 *  this method does nothing.
	 */
	public void selectBidiScript(boolean arabic, boolean hebrew) {
		// empty
	}

	/**
	 *  For class <code>ComplExpDoNothing</code>
	 *  this method always returns <code>false</code>.
	 */
	public boolean handlesArabicScript() {
		return false;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code>
	 *  this method always returns <code>false</code>.
	 */
	public boolean handlesHebrewScript() {
		return false;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  returns a string identical to the <code>text</code> parameter.
	 */
	public String leanToFullText(String text) {
		return text;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  returns a string identical to the <code>text</code> parameter.
	 */
	public String leanToFullText(String text, int initState) {
		return text;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  returns a zero length array.
	 */
	public int[] leanBidiCharOffsets(String text) {
		return EMPTY_INT_ARRAY;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  returns a zero length array.
	 */
	public int[] leanBidiCharOffsets(String text, int initState) {
		return EMPTY_INT_ARRAY;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  returns a zero length array.
	 */
	public int[] leanBidiCharOffsets() {
		return EMPTY_INT_ARRAY;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  returns a zero length array.
	 */
	public int[] fullBidiCharOffsets() {
		return EMPTY_INT_ARRAY;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  returns a string identical to the <code>text</code> parameter.
	 */
	public String fullToLeanText(String text) {
		return text;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  returns a string identical to the <code>text</code> parameter.
	 */
	public String fullToLeanText(String text, int initState) {
		return text;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  returns value {@link #STATE_NOTHING_GOING}.
	 */
	public int getFinalState() {
		return STATE_NOTHING_GOING;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  returns an index identical to the <code>pos</code> parameter.
	 */
	public int leanToFullPos(int pos) {
		return pos;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  returns an index identical to the <code>pos</code> parameter.
	 */
	public int fullToLeanPos(int pos) {
		return pos;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  does nothing.
	 */
	public void assumeMirrored(boolean mirrored) {
		// empty
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  always returns <code>false</code>.
	 */
	public boolean isMirrored() {
		return false;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  does nothing.
	 */
	public void assumeOrientation(int orientation) {
		// empty
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  always returns <code>ORIENT_LTR</code>.
	 */
	public int recallOrientation() {
		return ORIENT_LTR;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  does nothing.
	 */
	public void setArabicDirection(int not_mirrored, int mirrored) {
		// empty
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  does nothing.
	 */
	public void setArabicDirection(int direction) {
		// empty
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  does nothing.
	 */
	public void setHebrewDirection(int not_mirrored, int mirrored) {
		// empty
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  does nothing.
	 */
	public void setHebrewDirection(int direction) {
		// empty
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  does nothing.
	 */
	public void setDirection(int not_mirrored, int mirrored) {
		// empty
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  does nothing.
	 */
	public void setDirection(int direction) {
		// empty
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  returns all directions as <code>DIRECTION_LTR</code>.
	 */
	public int[][] getDirection() {
		return ALL_LTR;
	}

	/**
	 *  For class <code>ComplExpDoNothing</code> this method
	 *  returns <code>DIRECTION_LTR</code>.
	 */
	public int getCurDirection() {
		return DIRECTION_LTR;
	}
}
