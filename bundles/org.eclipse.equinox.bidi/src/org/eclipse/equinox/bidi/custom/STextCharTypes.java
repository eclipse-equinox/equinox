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
public class STextCharTypes {

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

	private static final int DIRPROPS_ADD = 2;

	final protected String text;

	// 1 byte for each char in text
	private byte[] dirProps;

	// current orientation
	private int orientation = -1; // "-1" means "unknown"

	public STextCharTypes(String text) {
		this.text = text;
		dirProps = new byte[text.length()];
	}

	private byte getCachedDirectionAt(int index) {
		return (byte) (dirProps[index] - DIRPROPS_ADD);
	}

	private boolean hasCachedDirectionAt(int i) {
		return (dirProps[i] != 0); // "0" means "unknown"
	}

	/**
	 * @param  dirProp bidirectional class of the character. It must be
	 *         one of the values which can be returned by
	 *         <code>java.lang.Character.getDirectionality</code>.
	 */
	public void setBidiTypeAt(int i, byte dirProp) {
		dirProps[i] = (byte) (dirProp + DIRPROPS_ADD);
	}

	public int getOrientation(STextEnvironment environment) {
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
					setBidiTypeAt(i, dirProp);
				} else {
					dirProp = getCachedDirectionAt(i);
				}
				if (dirProp == L) {
					result = STextEnvironment.ORIENT_LTR;
					break;
				}
				if (dirProp == R || dirProp == AL) {
					result = STextEnvironment.ORIENT_RTL;
					break;
				}
			}
		}
		orientation = result;
		return result;
	}

	/**
	 * Returns directionality of the character in the original string at
	 * the specified index.
	 * @param index position of the character in the <i>lean</i> text
	 * @return the bidirectional class of the character. It is one of the
	 * values which can be returned by {@link Character#getDirectionality(char)}
	 */
	public byte getBidiTypeAt(int index) {
		if (hasCachedDirectionAt(index))
			return getCachedDirectionAt(index);
		byte dirProp = Character.getDirectionality(text.charAt(index));
		if (dirProp == B) {
			dirProp = (orientation == STextEnvironment.ORIENT_RTL) ? R : L;
		}
		setBidiTypeAt(index, dirProp);
		return dirProp;
	}

}
