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

import org.eclipse.equinox.bidi.advanced.STextEnvironment;

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

	private static final int CHARTYPES_ADD = 2;

	final protected STextProcessor processor;
	final protected STextEnvironment environment;
	final protected String text;

	// 1 byte for each char in text
	private byte[] types;

	// structured text direction. -1 means not yet computed; -2 means within processor.getDirection
	private int direction = -1;

	/**
	 *  Constructor
	 *  
	 *  @param  processor is the processor handling this occurrence of
	 *          structured text.
	 *          
	 *  @param  environment the current environment, which may affect the behavior of
	 *          the processor. This parameter may be specified as
	 *          <code>null</code>, in which case the
	 *          {@link STextEnvironment#DEFAULT DEFAULT}
	 *          environment should be assumed.
	 *  
	 *  @param text is the text whose characters are analyzed.
	 */
	public STextCharTypes(STextProcessor processor, STextEnvironment environment, String text) {
		this.processor = processor;
		this.environment = environment;
		this.text = text;
		types = new byte[text.length()];
	}

	public int getDirection() {
		if (direction < 0)
			direction = processor.getDirection(environment, text, this);
		return direction;
	}

	private byte getCachedTypeAt(int index) {
		return (byte) (types[index] - CHARTYPES_ADD);
	}

	private boolean hasCachedTypeAt(int i) {
		return (types[i] != 0); // "0" means "unknown"
	}

	/**
	 *  Returns directionality of the character in the original string at
	 *  the specified index.
	 *  
	 *  @param  index position of the character in the <i>lean</i> text
	 *  
	 *  @return the bidi type of the character. It is one of the
	 *          values which can be returned by 
	 *          {@link Character#getDirectionality(char)}.
	 */
	public byte getBidiTypeAt(int index) {
		if (hasCachedTypeAt(index))
			return getCachedTypeAt(index);
		byte charType = Character.getDirectionality(text.charAt(index));
		if (charType == B) {
			if (direction < 0) {
				if (direction < -1) // called by processor.getDirection
					return charType; // avoid infinite recursion
				direction = -2; // signal we go within processor.getDirection
				direction = processor.getDirection(environment, text, this);
			}
			charType = (direction == STextEnvironment.ORIENT_RTL) ? R : L;
		}
		setBidiTypeAt(index, charType);
		return charType;
	}

	/**
	 *  Force a bidi type on a character.
	 *  
	 *  @param  index is the index of the character whose bidi type is set.
	 *   
	 *  @param  charType bidirectional type of the character. It must be
	 *          one of the values which can be returned by
	 *          <code>java.lang.Character.getDirectionality</code>.
	 */
	public void setBidiTypeAt(int index, byte charType) {
		types[index] = (byte) (charType + CHARTYPES_ADD);
	}

	/**
	 *  Get the orientation of the component in which the text will
	 *  be displayed.
	 *  
	 *  @param  envir is the current environment, which may affect the behavior of
	 *          the processor. This parameter may be specified as
	 *          <code>null</code>, in which case the
	 *          {@link STextEnvironment#DEFAULT DEFAULT}
	 *          environment should be assumed.
	 *  
	 *  @return the orientation as either 
	 *          {@link STextEnvironment#ORIENT_LTR} or
	 *          {@link STextEnvironment#ORIENT_RTL}.
	 */
	public int resolveOrientation(STextEnvironment envir) {
		int orient = envir.getOrientation();
		if ((orient & STextEnvironment.ORIENT_CONTEXTUAL_LTR) == 0) { // absolute orientation
			return orient;
		}
		// contextual orientation:
		orient &= 1; // initiate to the default orientation minus contextual bit
		int len = text.length();
		byte charType;
		for (int i = 0; i < len; i++) {
			if (!hasCachedTypeAt(i)) {
				charType = Character.getDirectionality(text.charAt(i));
				if (charType == B) // B char resolves to L or R depending on orientation
					continue;
				setBidiTypeAt(i, charType);
			} else
				charType = getCachedTypeAt(i);
			if (charType == L)
				return STextEnvironment.ORIENT_LTR;
			if (charType == R || charType == AL)
				return STextEnvironment.ORIENT_RTL;
		}
		return orient;
	}
}
