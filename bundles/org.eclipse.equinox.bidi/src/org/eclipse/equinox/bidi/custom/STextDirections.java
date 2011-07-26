/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.bidi.custom;

import org.eclipse.equinox.bidi.STextEnvironment;

/**
 * The class determines bidirectional types of characters in a string.
 */
public class STextDirections {

	// In the following lines, B, L, R and AL represent bidi categories
	// as defined in the Unicode Bidirectional Algorithm
	// ( http://www.unicode.org/reports/tr9/ ).
	// B  represents the category Block Separator.
	// L  represents the category Left to Right character.
	// R  represents the category Right to Left character.
	// AL represents the category Arabic Letter.
	// AN represents the category Arabic Number.
	// EN  represents the category European Number.
	static final byte B = Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR;
	static final byte L = Character.DIRECTIONALITY_LEFT_TO_RIGHT;
	static final byte R = Character.DIRECTIONALITY_RIGHT_TO_LEFT;
	static final byte AL = Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
	static final byte AN = Character.DIRECTIONALITY_ARABIC_NUMBER;
	static final byte EN = Character.DIRECTIONALITY_EUROPEAN_NUMBER;

	// TBD consider moving L, R, AL, AN, EN, B into this class from STextImpl
	// TBD add methods:
	// isRTL	(dirProp == R || dirProp == AL)
	// isLTR	(dirProp == L || dirProp == EN)
	// isStrong (isRTL() || isLTR() || dirProp == AN) <= excludes unknown, B, WS

	final protected String text;

	// 1 byte for each char in text
	private byte[] dirProps;

	// current orientation
	private byte baseOrientation = 0; // "0" means "unknown"

	public STextDirections(String text) {
		this.text = text;
		dirProps = new byte[text.length()];
	}

	public void setBaseOrientation(byte orientation) {
		baseOrientation = orientation;
	}

	public byte getBaseOrientation() {
		return baseOrientation;
	}

	private byte getCachedDirectionAt(int index) {
		return (byte) (dirProps[index] - 1);
	}

	private boolean hasCachedDirectionAt(int i) {
		return (dirProps[i] != 0); // "0" means "unknown"
	}

	/**
	 * @param  dirProp bidirectional class of the character. It must be
	 *         one of the values which can be returned by
	 *         <code>java.lang.Character.getDirectionality</code>.
	 */
	public void setOrientationAt(int i, byte dirProp) {
		dirProps[i] = (byte) (dirProp + 1);
	}

	public int getBaseOrientation(STextEnvironment environment) {
		int result;
		int orient = environment.getOrientation();
		if ((orient & STextEnvironment.ORIENT_CONTEXTUAL_LTR) == 0) { // absolute orientation
			result = orient;
		} else { // contextual orientation:
			result = orient & 1; // initiate to the default orientation minus contextual bit
			int len = text.length();
			byte dirProp;
			for (int i = 0; i < len; i++) {
				if (!hasCachedDirectionAt(i)) {
					dirProp = Character.getDirectionality(text.charAt(i));
					if (dirProp == B) // B char resolves to L or R depending on orientation
						continue;
					setOrientationAt(i, dirProp);
				} else {
					dirProp = getCachedDirectionAt(i);
				}
				if (dirProp == L) { // TBD || == EN ?
					result = STextEnvironment.ORIENT_LTR;
					break;
				}
				if (dirProp == R || dirProp == AL) {
					result = STextEnvironment.ORIENT_RTL;
					break;
				}
			}
			if (result == -1) // return the default orientation minus contextual bit
				result = orient & 1;
		}
		baseOrientation = (byte) result;
		return result;
	}

	/**
	 * Returns directionality of the character in the original string at
	 * the specified index.
	 * @param index position of the character in the <i>lean</i> text
	 * @return the bidirectional class of the character. It is one of the
	 * values which can be returned by {@link Character#getDirectionality(char)}
	 */
	public byte getOrientationAt(int index) {
		if (hasCachedDirectionAt(index))
			return getCachedDirectionAt(index);
		byte dirProp = Character.getDirectionality(text.charAt(index));
		if (dirProp == B) {
			byte orient = getBaseOrientation();
			dirProp = (orient == STextEnvironment.ORIENT_RTL) ? R : L;
		}
		setOrientationAt(index, dirProp);
		return dirProp;
	}

}
