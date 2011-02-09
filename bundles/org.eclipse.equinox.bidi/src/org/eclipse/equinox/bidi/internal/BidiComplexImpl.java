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
package org.eclipse.equinox.bidi.internal;

import org.eclipse.equinox.bidi.*;
import org.eclipse.equinox.bidi.custom.IBidiComplexProcessor;

/**
 *  <code>BidiComplexImpl</code> provides the code which implements the API in
 *  {@link BidiComplexHelper}. All its public methods are shadows of similarly
 *  signed methods of <code>BidiComplexHelper</code>, and their documentation
 *  is by reference to the methods in <code>BidiComplexHelper</code>.
 *
 *  @author Matitiahu Allouche
 */
public class BidiComplexImpl {

	static final String EMPTY_STRING = ""; //$NON-NLS-1$
	static final byte B = Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR;
	static final byte L = Character.DIRECTIONALITY_LEFT_TO_RIGHT;
	static final byte R = Character.DIRECTIONALITY_RIGHT_TO_LEFT;
	static final byte AL = Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
	static final byte AN = Character.DIRECTIONALITY_ARABIC_NUMBER;
	static final byte EN = Character.DIRECTIONALITY_EUROPEAN_NUMBER;
	static final char LRM = 0x200E;
	static final char RLM = 0x200F;
	static final char LRE = 0x202A;
	static final char RLE = 0x202B;
	static final char PDF = 0x202C;
	static final char[] MARKS = {LRM, RLM};
	static final char[] EMBEDS = {LRE, RLE};
	static final byte[] STRONGS = {L, R};
	static final int PREFIX_LENGTH = 2;
	static final int SUFFIX_LENGTH = 2;
	static final int FIXES_LENGTH = PREFIX_LENGTH + SUFFIX_LENGTH;

	BidiComplexHelper helper;

	BidiComplexEnvironment environment;

	IBidiComplexProcessor processor;

	/**
	 *  This field is reserved for use of the complex expression
	 *  processor associated with this instance of <code>BidiComplexImpl</code>.
	 *  The processor can keep here a reference to an object that it
	 *  has sole control of.
	 */
	public Object processorData;

	/**
	 *  Features of the associated processor.
	 *
	 */
	public BidiComplexFeatures features;

	int state = BidiComplexHelper.STATE_NOTHING_GOING;

	// strong bidi class (L or R) for current expression direction
	byte curStrong = -1;
	// strong character (LRM or RLM) for current expression direction
	char curMark;
	// strong directional control (LRE or RLE) for current expression direction
	char curEmbed;
	int prefixLength;
	// index of next occurrence for each operator and each special case
	int[] locations;
	// number of special cases
	int specialsCount;
	String leanText;
	// positions where LRM/RLM must be added
	int[] offsets;
	// number of LRM/RLM to add
	int count;
	// For positions where it has been looked up, the entry will receive
	// the Character directionality + 2 (so that 0 indicates that the
	// the directionality has not been looked up yet.
	byte[] dirProps;
	// current UI orientation (after resolution if contextual)
	int curOrient = -1;
	// Current expression base direction (after resolution if depending on
	// script and/or on GUI mirroring (0=LTR, 1=RTL))
	int curDirection = -1;

	/**
	 *  @see BidiComplexHelper#BidiComplexHelper(IBidiComplexProcessor myProcessor, BidiComplexEnvironment environment)
	 */
	public BidiComplexImpl(BidiComplexHelper caller, IBidiComplexProcessor myProcessor, BidiComplexEnvironment environment) {
		helper = caller;
		if (environment == null) {
			this.environment = BidiComplexEnvironment.DEFAULT;
		} else {
			this.environment = environment;
		}
		processor = myProcessor;
		features = processor.init(helper, this.environment);
		// keep private copy of specialsCount to avoid later modification
		specialsCount = features.specialsCount;
		locations = new int[features.operators.length() + specialsCount];
	}

	long computeNextLocation(int curPos) {
		int operCount = features.operators.length();
		int len = leanText.length();
		int nextLocation = len;
		int idxLocation = 0;
		// Start with special sequences to give them precedence over simple
		// operators. This may apply to cases like slash+asterisk versus slash.
		for (int i = 0; i < specialsCount; i++) {
			int location = locations[operCount + i];
			if (location < curPos) {
				location = processor.indexOfSpecial(helper, i, leanText, curPos);
				if (location < 0)
					location = len;
				locations[operCount + i] = location;
			}
			if (location < nextLocation) {
				nextLocation = location;
				idxLocation = operCount + i;
			}
		}
		for (int i = 0; i < operCount; i++) {
			int location = locations[i];
			if (location < curPos) {
				location = leanText.indexOf(features.operators.charAt(i), curPos);
				if (location < 0)
					location = len;
				locations[i] = location;
			}
			if (location < nextLocation) {
				nextLocation = location;
				idxLocation = i;
			}
		}
		return nextLocation + (((long) idxLocation) << 32);
	}

	int getCurOrient() {
		if (curOrient >= 0)
			return curOrient;

		if ((environment.orientation & BidiComplexEnvironment.ORIENT_CONTEXTUAL_LTR) == 0) {
			// absolute orientation
			curOrient = environment.orientation;
			return curOrient;
		}
		// contextual orientation
		int len = leanText.length();
		byte dirProp;
		for (int i = 0; i < len; i++) {
			// In the following lines, B, L, R and AL represent bidi categories
			// as defined in the Unicode Bidirectional Algorithm
			// ( http://www.unicode.org/reports/tr9/ ).
			// B  represents the category Block Separator.
			// L  represents the category Left to Right character.
			// R  represents the category Right to Left character.
			// AL represents the category Arabic Letter.
			dirProp = dirProps[i];
			if (dirProp == 0) {
				dirProp = Character.getDirectionality(leanText.charAt(i));
				if (dirProp == B) // B char resolves to L or R depending on orientation
					continue;
				dirProps[i] = (byte) (dirProp + 2);
			} else {
				dirProp -= 2;
			}
			if (dirProp == L) {
				curOrient = BidiComplexEnvironment.ORIENT_LTR;
				return curOrient;
			}
			if (dirProp == R || dirProp == AL) {
				curOrient = BidiComplexEnvironment.ORIENT_RTL;
				return curOrient;
			}
		}
		curOrient = environment.orientation & 1;
		return curOrient;
	}

	/**
	 *  @see BidiComplexHelper#getCurDirection
	 */
	public int getCurDirection() {
		if (curDirection >= 0)
			return curDirection;

		curStrong = -1;
		// same direction for Arabic and Hebrew?
		if (features.dirArabic == features.dirHebrew) {
			curDirection = features.dirArabic;
			return curDirection;
		}
		// check if Arabic or Hebrew letter comes first
		int len = leanText.length();
		byte dirProp;
		for (int i = 0; i < len; i++) {
			// In the following lines, R and AL represent bidi categories
			// as defined in the Unicode Bidirectional Algorithm
			// ( http://www.unicode.org/reports/tr9/ ).
			// R  represents the category Right to Left character.
			// AL represents the category Arabic Letter.
			dirProp = getDirProp(i);
			if (dirProp == AL) {
				curDirection = features.dirArabic;
				return curDirection;
			}
			if (dirProp == R) {
				curDirection = features.dirHebrew;
				return curDirection;
			}
		}
		// found no Arabic or Hebrew character
		curDirection = BidiComplexFeatures.DIR_LTR;
		return curDirection;
	}

	void setMarkAndFixes() {
		int dir = getCurDirection();
		if (curStrong == STRONGS[dir])
			return;
		curStrong = STRONGS[dir];
		curMark = MARKS[dir];
		curEmbed = EMBEDS[dir];
	}

	/**
	 *  @see BidiComplexHelper#getDirProp(int index)
	 */
	public byte getDirProp(int index) {
		byte dirProp = dirProps[index];
		if (dirProp == 0) {
			// In the following lines, B, L and R represent bidi categories
			// as defined in the Unicode Bidirectional Algorithm
			// ( http://www.unicode.org/reports/tr9/ ).
			// B  represents the category Block Separator.
			// L  represents the category Left to Right character.
			// R  represents the category Right to Left character.
			dirProp = Character.getDirectionality(leanText.charAt(index));
			if (dirProp == B)
				dirProp = getCurOrient() == BidiComplexEnvironment.ORIENT_RTL ? R : L;
			dirProps[index] = (byte) (dirProp + 2);
			return dirProp;
		}
		return (byte) (dirProp - 2);
	}

	/**
	 *  @see BidiComplexHelper#setDirProp(int index, byte dirProp)
	 */
	public void setDirProp(int index, byte dirProp) {
		dirProps[index] = (byte) (dirProp + 2);
	}

	/**
	 *  @see BidiComplexHelper#processOperator(int operLocation)
	 */
	public void processOperator(int operLocation) {
		// In this method, L, R, AL, AN and EN represent bidi categories
		// as defined in the Unicode Bidirectional Algorithm
		// ( http://www.unicode.org/reports/tr9/ ).
		// L  represents the category Left to Right character.
		// R  represents the category Right to Left character.
		// AL represents the category Arabic Letter.
		// AN represents the category Arabic Number.
		// EN  represents the category European Number.
		int len = leanText.length();
		boolean doneAN = false;

		if (getCurDirection() == BidiComplexFeatures.DIR_RTL) {
			// the expression base direction is RTL
			for (int i = operLocation - 1; i >= 0; i--) {
				byte dirProp = getDirProp(i);
				if (dirProp == R || dirProp == AL)
					return;

				if (dirProp == L) {
					for (int j = operLocation; j < len; j++) {
						dirProp = getDirProp(j);
						if (dirProp == R || dirProp == AL)
							return;
						if (dirProp == L || dirProp == EN) {
							insertMark(operLocation);
							return;
						}
					}
					return;
				}
			}
			return;
		}

		// the expression base direction is LTR
		if (features.ignoreArabic) {
			if (features.ignoreHebrew) /* process neither Arabic nor Hebrew */
				return;
			/* process Hebrew, not Arabic */
			for (int i = operLocation - 1; i >= 0; i--) {
				byte dirProp = getDirProp(i);
				if (dirProp == L)
					return;
				if (dirProp == R) {
					for (int j = operLocation; j < len; j++) {
						dirProp = getDirProp(j);
						if (dirProp == L)
							return;
						if (dirProp == R || dirProp == EN) {
							insertMark(operLocation);
							return;
						}
					}
					return;
				}
			}
		} else {
			if (features.ignoreHebrew) { /* process Arabic, not Hebrew */
				for (int i = operLocation - 1; i >= 0; i--) {
					byte dirProp = getDirProp(i);
					if (dirProp == L)
						return;
					if (dirProp == AL) {
						for (int j = operLocation; j < len; j++) {
							dirProp = getDirProp(j);
							if (dirProp == L)
								return;
							if (dirProp == EN || dirProp == AL || dirProp == AN) {
								insertMark(operLocation);
								return;
							}
						}
						return;
					}
					if (dirProp == AN && !doneAN) {
						for (int j = operLocation; j < len; j++) {
							dirProp = getDirProp(j);
							if (dirProp == L)
								return;
							if (dirProp == AL || dirProp == AN) {
								insertMark(operLocation);
								return;
							}
						}
						doneAN = true;
					}
				}
			} else { /* process Arabic and Hebrew */
				for (int i = operLocation - 1; i >= 0; i--) {
					byte dirProp = getDirProp(i);
					if (dirProp == L)
						return;
					if (dirProp == R || dirProp == AL) {
						for (int j = operLocation; j < len; j++) {
							dirProp = getDirProp(j);
							if (dirProp == L)
								return;
							if (dirProp == R || dirProp == EN || dirProp == AL || dirProp == AN) {
								insertMark(operLocation);
								return;
							}
						}
						return;
					}
					if (dirProp == AN && !doneAN) {
						for (int j = operLocation; j < len; j++) {
							dirProp = getDirProp(j);
							if (dirProp == L)
								return;
							if (dirProp == AL || dirProp == AN || dirProp == R) {
								insertMark(operLocation);
								return;
							}
						}
						doneAN = true;
					}
				}
			}
		}
	}

	/**
	 *  @see BidiComplexHelper#getFinalState()
	 */
	public int getFinalState() {
		return state;
	}

	/**
	 *  @see BidiComplexHelper#setFinalState(int newState)
	 */
	public void setFinalState(int newState) {
		state = newState;
	}

	/**
	 *  @see BidiComplexHelper#leanToFullText(String text, int initState)
	 */
	public String leanToFullText(String text, int initState) {
		if (text.length() == 0) {
			prefixLength = 0;
			count = 0;
			return text;
		}
		leanToFullTextNofix(text, initState);
		return addMarks(true);
	}

	void leanToFullTextNofix(String text, int initState) {
		int operCount = features.operators.length();
		// current position
		int curPos = 0;
		int len = text.length();
		// location of next token to handle
		int nextLocation;
		// index of next token to handle (if < operCount, this is an operator; otherwise a special case
		int idxLocation;
		leanText = text;
		offsets = new int[20];
		count = 0;
		dirProps = new byte[len];
		curOrient = -1;
		curDirection = -1;
		// initialize locations
		int k = locations.length;
		for (int i = 0; i < k; i++) {
			locations[i] = -1;
		}
		state = BidiComplexHelper.STATE_NOTHING_GOING;
		if (initState != BidiComplexHelper.STATE_NOTHING_GOING)
			curPos = processor.processSpecial(helper, initState, leanText, -1);

		while (true) {
			long res = computeNextLocation(curPos);
			nextLocation = (int) (res & 0x00000000FFFFFFFF); /* low word */
			if (nextLocation >= len)
				break;
			idxLocation = (int) (res >> 32); /* high word */
			if (idxLocation < operCount) {
				processOperator(nextLocation);
				curPos = nextLocation + 1;
			} else {
				curPos = processor.processSpecial(helper, idxLocation - operCount, leanText, nextLocation);
			}
		}
	}

	/**
	 *  @see BidiComplexHelper#leanBidiCharOffsets()
	 */
	public int[] leanBidiCharOffsets() {
		int[] result = new int[count];
		System.arraycopy(offsets, 0, result, 0, count);
		return result;
	}

	/**
	 *  @see BidiComplexHelper#fullBidiCharOffsets()
	 */
	public int[] fullBidiCharOffsets() {
		int lim = count;
		if (prefixLength > 0) {
			if (prefixLength == 1)
				lim++;
			else
				lim += FIXES_LENGTH;
		}
		int[] fullOffsets = new int[lim];
		for (int i = 0; i < prefixLength; i++) {
			fullOffsets[i] = i;
		}
		int added = prefixLength;
		for (int i = 0; i < count; i++) {
			fullOffsets[prefixLength + i] = offsets[i] + added;
			added++;
		}
		if (prefixLength > 1) {
			int len = leanText.length();
			fullOffsets[lim - 2] = len + lim - 2;
			fullOffsets[lim - 1] = len + lim - 1;
		}
		return fullOffsets;
	}

	/**
	 *  @see BidiComplexHelper#fullToLeanText(String text, int initState)
	 */
	public String fullToLeanText(String text, int initState) {
		int i; // used as loop index
		setMarkAndFixes();
		// remove any prefix and leading mark
		int lenText = text.length();
		for (i = 0; i < lenText; i++) {
			char c = text.charAt(i);
			if (c != curEmbed && c != curMark)
				break;
		}
		if (i > 0) { // found at least one prefix or leading mark
			text = text.substring(i);
			lenText = text.length();
		}
		// remove any suffix and trailing mark
		for (i = lenText - 1; i >= 0; i--) {
			char c = text.charAt(i);
			if (c != PDF && c != curMark)
				break;
		}
		if (i < 0) { // only suffix and trailing marks, no real data
			leanText = EMPTY_STRING;
			prefixLength = 0;
			count = 0;
			return leanText;
		}
		if (i < (lenText - 1)) { // found at least one suffix or trailing mark
			text = text.substring(0, i + 1);
			lenText = text.length();
		}
		char[] chars = text.toCharArray();
		// remove marks from chars
		int cnt = 0;
		for (i = 0; i < lenText; i++) {
			char c = chars[i];
			if (c == curMark)
				cnt++;
			else if (cnt > 0)
				chars[i - cnt] = c;
		}
		String lean = new String(chars, 0, lenText - cnt);
		leanToFullTextNofix(lean, initState);
		String full = addMarks(false); /* only marks, no prefix/suffix */
		if (full.equals(text))
			return lean;

		// There are some marks in full which are not in text and/or vice versa.
		// We need to add to lean any mark appearing in text and not in full.
		// The completed lean can never be longer than text itself.
		char[] newChars = new char[lenText];
		char cFull, cText;
		int idxFull, idxText, idxLean, markPos, newCharsPos;
		int lenFull = full.length();
		idxFull = idxText = idxLean = newCharsPos = 0;
		while (idxText < lenText && idxFull < lenFull) {
			cFull = full.charAt(idxFull);
			cText = text.charAt(idxText);
			if (cFull == cText) { /* chars are equal, proceed */
				idxText++;
				idxFull++;
				continue;
			}
			if (cFull == curMark) { /* extra Mark in full text */
				idxFull++;
				continue;
			}
			if (cText == curMark) { /* extra Mark in source full text */
				idxText++;
				// idxText-2 always >= 0 since leading Marks were removed from text
				if (text.charAt(idxText - 2) == curMark)
					continue; // ignore successive Marks in text after the first one
				markPos = fullToLeanPos(idxFull);
				// copy from chars (== lean) to newChars until here
				for (i = idxLean; i < markPos; i++) {
					newChars[newCharsPos++] = chars[i];
				}
				idxLean = markPos;
				newChars[newCharsPos++] = curMark;
				continue;
			}
			// we should never get here (extra char which is not a Mark)
			throw new IllegalStateException("Internal error: extra character not a Mark."); //$NON-NLS-1$
		}
		if (idxText < lenText) /* full ended before text - this should never happen */
			throw new IllegalStateException("Internal error: unexpected EOL."); //$NON-NLS-1$

		// copy the last part of chars to newChars
		for (i = idxLean; i < lean.length(); i++) {
			newChars[newCharsPos++] = chars[i];
		}
		lean = new String(newChars, 0, newCharsPos);
		leanText = lean;
		return lean;
	}

	/**
	 *  @see BidiComplexHelper#leanToFullPos(int pos)
	 */
	public int leanToFullPos(int pos) {
		int added = prefixLength;
		for (int i = 0; i < count; i++) {
			if (offsets[i] <= pos)
				added++;
			else
				return pos + added;
		}
		return pos + added;
	}

	/**
	 *  @see BidiComplexHelper#fullToLeanPos(int pos)
	 */
	public int fullToLeanPos(int pos) {
		int len = leanText.length();
		int added = 0;
		pos -= prefixLength;
		for (int i = 0; i < count; i++) {
			if ((offsets[i] + added) < pos)
				added++;
			else
				break;
		}
		pos -= added;
		if (pos < 0)
			pos = 0;
		else if (pos > len)
			pos = len;
		return pos;
	}

	/**
	 *  @see BidiComplexHelper#insertMark(int offset)
	 */
	public void insertMark(int offset) {
		int index = count - 1; // index of greatest member <= offset
		// look up after which member the new offset should be inserted
		while (index >= 0) {
			int wrkOffset = offsets[index];
			if (offset > wrkOffset)
				break;
			if (offset == wrkOffset)
				return; // avoid duplicates
			index--;
		}
		index++; // index now points at where to insert
		// check if we have an available slot for new member
		if (count >= (offsets.length - 1)) {
			int[] newOffsets = new int[offsets.length * 2];
			System.arraycopy(offsets, 0, newOffsets, 0, count);
			offsets = newOffsets;
		}

		int length = count - index; // number of members to move up
		if (length > 0) // shift right all members greater than offset
			System.arraycopy(offsets, index, offsets, index + 1, length);

		offsets[index] = offset;
		count++;
		// if the offset is 0, adding a mark does not change anything
		if (offset < 1)
			return;

		byte dirProp = getDirProp(offset);
		// if the current char is a strong one or a digit, we change the
		//   dirProp of the previous char to account for the inserted mark.
		// In the following lines, L, R, AL, AN and EN represent bidi categories
		// as defined in the Unicode Bidirectional Algorithm
		// ( http://www.unicode.org/reports/tr9/ ).
		// L  represents the category Left to Right character.
		// R  represents the category Right to Left character.
		// AL represents the category Arabic Letter.
		// AN represents the category Arabic Number.
		// EN  represents the category European Number.
		if (dirProp == L || dirProp == R || dirProp == AL || dirProp == EN || dirProp == AN)
			index = offset - 1;
		else
			// if the current char is a neutral, we change its own dirProp
			index = offset;
		setMarkAndFixes();
		setDirProp(index, curStrong);
	}

	String addMarks(boolean addFixes) {
		// add prefix/suffix only if addFixes is true
		if ((count == 0) && (!addFixes || (getCurOrient() == getCurDirection()) || (curOrient == BidiComplexEnvironment.ORIENT_IGNORE))) {
			prefixLength = 0;
			return leanText;
		}
		int len = leanText.length();
		int newLen = len + count;
		if (addFixes && ((getCurOrient() != getCurDirection()) || (curOrient == BidiComplexEnvironment.ORIENT_UNKNOWN))) {
			if ((environment.orientation & BidiComplexEnvironment.ORIENT_CONTEXTUAL_LTR) == 0) {
				prefixLength = PREFIX_LENGTH;
				newLen += FIXES_LENGTH;
			} else { /* contextual orientation */
				prefixLength = 1;
				newLen++; /* +1 for a mark char */
			}
		} else {
			prefixLength = 0;
		}
		char[] fullChars = new char[newLen];
		// add a dummy offset as fence
		offsets[count] = len;
		int added = prefixLength;
		// add marks at offsets
		setMarkAndFixes();
		for (int i = 0, j = 0; i < len; i++) {
			char c = leanText.charAt(i);
			if (i == offsets[j]) {
				fullChars[i + added] = curMark;
				added++;
				j++;
			}
			fullChars[i + added] = c;
		}
		if (prefixLength > 0) { /* add prefix/suffix ? */
			if (prefixLength == 1) { /* contextual orientation */
				fullChars[0] = curMark;
			} else {
				// When the orientation is RTL, we need to add EMBED at the
				// start of the text and PDF at its end.
				// However, because of a bug in Windows' handling of LRE/PDF,
				// we add EMBED_PREFIX at the start and EMBED_SUFFIX at the end.
				fullChars[0] = curEmbed;
				fullChars[1] = curMark;
				fullChars[newLen - 1] = PDF;
				fullChars[newLen - 2] = curMark;
			}
		}
		return new String(fullChars);
	}

	/**
	 *  @see BidiComplexHelper#getEnvironment()
	 */
	public BidiComplexEnvironment getEnvironment() {
		return environment;
	}

	/**
	 *  @see BidiComplexHelper#setEnvironment(BidiComplexEnvironment environment)
	 */
	public void setEnvironment(BidiComplexEnvironment environment) {
		this.environment = environment;
		features = processor.updateEnvironment(helper, environment);
		specialsCount = features.specialsCount;
		if ((features.operators.length() + specialsCount) > locations.length)
			locations = new int[features.operators.length() + specialsCount];

	}

	/**
	 *  @see BidiComplexHelper#getFeatures()
	 */
	public BidiComplexFeatures getFeatures() {
		return features;
	}

	/**
	 *  @see BidiComplexHelper#setFeatures(BidiComplexFeatures features)
	 */
	public void setFeatures(BidiComplexFeatures features) {
		if ((features.operators.length() + specialsCount) > locations.length)
			locations = new int[features.operators.length() + specialsCount];
		this.features = features;
	}

}
