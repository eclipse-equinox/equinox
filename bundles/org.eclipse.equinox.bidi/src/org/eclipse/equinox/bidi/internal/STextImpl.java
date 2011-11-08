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

import org.eclipse.equinox.bidi.advanced.*;
import org.eclipse.equinox.bidi.custom.*;

/**
 * Implementation for ISTextExpert.
 * 
 * @author Matitiahu Allouche
 *
 */
public class STextImpl implements ISTextExpert {

	static final String EMPTY_STRING = ""; //$NON-NLS-1$

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

	static final char LRM = 0x200E;
	static final char RLM = 0x200F;
	static final char LRE = 0x202A;
	static final char RLE = 0x202B;
	static final char PDF = 0x202C;
	static final char[] MARKS = {LRM, RLM};
	static final char[] EMBEDS = {LRE, RLE};
	static final int PREFIX_LENGTH = 2;
	static final int SUFFIX_LENGTH = 2;
	static final int FIXES_LENGTH = PREFIX_LENGTH + SUFFIX_LENGTH;
	static final int[] EMPTY_INT_ARRAY = new int[0];

	/**
	 * The structured text handler utilized by this expert.
	 */
	protected final STextTypeHandler handler;
	/**
	 * The environment associated with the expert.
	 */
	protected final STextEnvironment environment;
	/**
	 * Flag which is true if the expert is stateful.
	 */
	protected final boolean sharedExpert;
	/**
	 * Last state value set by {@link #setState} or {@link #clearState}.
	 */
	protected Object state;

	/**
	 * Constructor used in {@link STextExpertFactory}.
	 * 
	 * @param structuredTextHandler the structured text handler used by this expert.
	 * @param environment the environment associated with this expert.
	 * @param shared flag which is true if the expert is stateful.
	 */
	public STextImpl(STextTypeHandler structuredTextHandler, STextEnvironment environment, boolean shared) {
		this.handler = structuredTextHandler;
		this.environment = environment;
		sharedExpert = shared;
	}

	public STextTypeHandler getTypeHandler() {
		return handler;
	}

	public STextEnvironment getEnvironment() {
		return environment;
	}

	public int getTextDirection(String text) {
		return handler.getDirection(this, text);
	}

	public void clearState() {
		if (sharedExpert)
			state = null;
	}

	public void setState(Object newState) {
		if (sharedExpert)
			state = newState;
	}

	public Object getState() {
		return state;
	}

	long computeNextLocation(String text, STextCharTypes charTypes, STextOffsets offsets, int[] locations, int curPos) {
		String separators = handler.getSeparators(this);
		int separCount = separators.length();
		int specialsCount = handler.getSpecialsCount(this);
		int len = text.length();
		int nextLocation = len;
		int idxLocation = 0;
		// Start with special sequences to give them precedence over simple
		// separators. This may apply to cases like slash+asterisk versus slash.
		for (int i = 0; i < specialsCount; i++) {
			int location = locations[separCount + i];
			if (location < curPos) {
				location = handler.indexOfSpecial(this, text, charTypes, offsets, i + 1, curPos);
				if (location < 0)
					location = len;
				locations[separCount + i] = location;
			}
			if (location < nextLocation) {
				nextLocation = location;
				idxLocation = separCount + i;
			}
		}
		for (int i = 0; i < separCount; i++) {
			int location = locations[i];
			if (location < curPos) {
				location = text.indexOf(separators.charAt(i), curPos);
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

	/**
	 * @see STextTypeHandler#processSeparator STextTypeHandler.processSeparator
	 */
	static public void processSeparator(String text, STextCharTypes charTypes, STextOffsets offsets, int separLocation) {
		int len = text.length();
		int direction = charTypes.getDirection();
		if (direction == DIR_RTL) {
			// the structured text base direction is RTL
			for (int i = separLocation - 1; i >= 0; i--) {
				byte charType = charTypes.getBidiTypeAt(i);
				if (charType == R || charType == AL)
					return;
				if (charType == L) {
					for (int j = separLocation; j < len; j++) {
						charType = charTypes.getBidiTypeAt(j);
						if (charType == R || charType == AL)
							return;
						if (charType == L || charType == EN) {
							offsets.insertOffset(charTypes, separLocation);
							return;
						}
					}
					return;
				}
			}
			return;
		}

		// the structured text base direction is LTR
		boolean doneAN = false;
		for (int i = separLocation - 1; i >= 0; i--) {
			byte charType = charTypes.getBidiTypeAt(i);
			if (charType == L)
				return;
			if (charType == R || charType == AL) {
				for (int j = separLocation; j < len; j++) {
					charType = charTypes.getBidiTypeAt(j);
					if (charType == L)
						return;
					if (charType == R || charType == EN || charType == AL || charType == AN) {
						offsets.insertOffset(charTypes, separLocation);
						return;
					}
				}
				return;
			}
			if (charType == AN && !doneAN) {
				for (int j = separLocation; j < len; j++) {
					charType = charTypes.getBidiTypeAt(j);
					if (charType == L)
						return;
					if (charType == AL || charType == AN || charType == R) {
						offsets.insertOffset(charTypes, separLocation);
						return;
					}
				}
				doneAN = true;
			}
		}
	}

	/**
	 *  When the orientation is <code>ORIENT_LTR</code> and the
	 *  structured text has a RTL base direction,
	 *  {@link ISTextExpert#leanToFullText leanToFullText}
	 *  adds RLE+RLM at the head of the <i>full</i> text and RLM+PDF at its
	 *  end.
	 *  <p>
	 *  When the orientation is <code>ORIENT_RTL</code> and the
	 *  structured text has a LTR base direction,
	 *  {@link ISTextExpert#leanToFullText leanToFullText}
	 *  adds LRE+LRM at the head of the <i>full</i> text and LRM+PDF at its
	 *  end.
	 *  <p>
	 *  When the orientation is <code>ORIENT_CONTEXTUAL_LTR</code> or
	 *  <code>ORIENT_CONTEXTUAL_RTL</code> and the data content would resolve
	 *  to a RTL orientation while the structured text has a LTR base
	 *  direction, {@link ISTextExpert#leanToFullText leanToFullText}
	 *  adds LRM at the head of the <i>full</i> text.
	 *  <p>
	 *  When the orientation is <code>ORIENT_CONTEXTUAL_LTR</code> or
	 *  <code>ORIENT_CONTEXTUAL_RTL</code> and the data content would resolve
	 *  to a LTR orientation while the structured text has a RTL base
	 *  direction, {@link ISTextExpert#leanToFullText leanToFullText}
	 *  adds RLM at the head of the <i>full</i> text.
	 *  <p>
	 *  When the orientation is <code>ORIENT_UNKNOWN</code> and the
	 *  structured text has a LTR base direction,
	 *  {@link ISTextExpert#leanToFullText leanToFullText}
	 *  adds LRE+LRM at the head of the <i>full</i> text and LRM+PDF at its
	 *  end.
	 *  <p>
	 *  When the orientation is <code>ORIENT_UNKNOWN</code> and the
	 *  structured text has a RTL base direction,
	 *  {@link ISTextExpert#leanToFullText leanToFullText}
	 *  adds RLE+RLM at the head of the <i>full</i> text and RLM+PDF at its
	 *  end.
	 *  <p>
	 *  When the orientation is <code>ORIENT_IGNORE</code>,
	 *  {@link ISTextExpert#leanToFullText leanToFullText} does not add any directional
	 *  formatting characters as either prefix or suffix of the <i>full</i> text.
	 *  <p>
	 */
	public String leanToFullText(String text) {
		int len = text.length();
		if (len == 0)
			return text;
		STextCharTypes charTypes = new STextCharTypes(this, text);
		STextOffsets offsets = leanToFullCommon(text, charTypes);
		int prefixLength = offsets.getPrefixLength();
		int direction = charTypes.getDirection();
		return insertMarks(text, offsets.getOffsets(), direction, prefixLength);
	}

	public int[] leanToFullMap(String text) {
		int len = text.length();
		if (len == 0)
			return EMPTY_INT_ARRAY;
		STextCharTypes charTypes = new STextCharTypes(this, text);
		STextOffsets offsets = leanToFullCommon(text, charTypes);
		int prefixLength = offsets.getPrefixLength();
		int[] map = new int[len];
		int count = offsets.getCount(); // number of used entries
		int added = prefixLength;
		for (int pos = 0, i = 0; pos < len; pos++) {
			if (i < count && pos == offsets.getOffset(i)) {
				added++;
				i++;
			}
			map[pos] = pos + added;
		}
		return map;
	}

	public int[] leanBidiCharOffsets(String text) {
		int len = text.length();
		if (len == 0)
			return EMPTY_INT_ARRAY;
		STextCharTypes charTypes = new STextCharTypes(this, text);
		STextOffsets offsets = leanToFullCommon(text, charTypes);
		return offsets.getOffsets();
	}

	private STextOffsets leanToFullCommon(String text, STextCharTypes charTypes) {
		int len = text.length();
		int direction = handler.getDirection(this, text, charTypes);
		STextOffsets offsets = new STextOffsets();
		if (!handler.skipProcessing(this, text, charTypes)) {
			// initialize locations
			int separCount = handler.getSeparators(this).length();
			int[] locations = new int[separCount + handler.getSpecialsCount(this)];
			for (int i = 0, k = locations.length; i < k; i++) {
				locations[i] = -1;
			}
			// current position
			int curPos = 0;
			if (state != null) {
				curPos = handler.processSpecial(this, text, charTypes, offsets, 0, -1);
			}
			while (true) {
				// location of next token to handle
				int nextLocation;
				// index of next token to handle (if < separCount, this is a separator; otherwise a special case
				int idxLocation;
				long res = computeNextLocation(text, charTypes, offsets, locations, curPos);
				nextLocation = (int) (res & 0x00000000FFFFFFFF); /* low word */
				if (nextLocation >= len)
					break;
				idxLocation = (int) (res >> 32); /* high word */
				if (idxLocation < separCount) {
					processSeparator(text, charTypes, offsets, nextLocation);
					curPos = nextLocation + 1;
				} else {
					idxLocation -= (separCount - 1); // because caseNumber starts from 1
					curPos = handler.processSpecial(this, text, charTypes, offsets, idxLocation, nextLocation);
				}
				if (curPos >= len)
					break;
			} // end while
		} // end if (!handler.skipProcessing())
		int prefixLength;
		int orientation = environment.getOrientation();
		if (orientation == STextEnvironment.ORIENT_IGNORE)
			prefixLength = 0;
		else {
			int resolvedOrientation = charTypes.resolveOrientation();
			if (orientation != STextEnvironment.ORIENT_UNKNOWN && resolvedOrientation == direction)
				prefixLength = 0;
			else if ((orientation & STextEnvironment.ORIENT_CONTEXTUAL) != 0)
				prefixLength = 1;
			else
				prefixLength = 2;
		}
		offsets.setPrefixLength(prefixLength);
		return offsets;
	}

	public String fullToLeanText(String full) {
		if (full.length() == 0)
			return full;
		int dir = handler.getDirection(this, full);
		char curMark = MARKS[dir];
		char curEmbed = EMBEDS[dir];
		int i; // used as loop index
		// remove any prefix and leading mark
		int lenFull = full.length();
		for (i = 0; i < lenFull; i++) {
			char c = full.charAt(i);
			if (c != curEmbed && c != curMark)
				break;
		}
		if (i > 0) { // found at least one prefix or leading mark
			full = full.substring(i);
			lenFull = full.length();
		}
		// remove any suffix and trailing mark
		for (i = lenFull - 1; i >= 0; i--) {
			char c = full.charAt(i);
			if (c != PDF && c != curMark)
				break;
		}
		if (i < 0) // only suffix and trailing marks, no real data
			return EMPTY_STRING;
		if (i < (lenFull - 1)) { // found at least one suffix or trailing mark
			full = full.substring(0, i + 1);
			lenFull = full.length();
		}
		char[] chars = full.toCharArray();
		// remove marks from chars
		int cnt = 0;
		for (i = 0; i < lenFull; i++) {
			char c = chars[i];
			if (c == curMark)
				cnt++;
			else if (cnt > 0)
				chars[i - cnt] = c;
		}
		String lean = new String(chars, 0, lenFull - cnt);
		String full2 = leanToFullText(lean);
		// strip prefix and suffix
		int beginIndex = 0, endIndex = full2.length();
		if (full2.charAt(0) == curMark)
			beginIndex = 1;
		else {
			if (full2.charAt(0) == curEmbed) {
				beginIndex = 1;
				if (full2.charAt(0) == curMark)
					beginIndex = 2;
			}
			if (full2.charAt(endIndex - 1) == PDF) {
				endIndex--;
				if (full2.charAt(endIndex - 1) == curMark)
					endIndex--;
			}
		}
		if (beginIndex > 0 || endIndex < full2.length())
			full2 = full2.substring(beginIndex, endIndex);
		if (full2.equals(full))
			return lean;

		// There are some marks in full which are not in full2 and/or vice versa.
		// We need to add to lean any mark appearing in full and not in full2.
		// The completed lean can never be longer than full itself.
		char[] newChars = new char[lenFull];
		char cFull, cFull2;
		int idxFull, idxFull2, idxLean, newCharsPos;
		int lenFull2 = full2.length();
		idxFull = idxFull2 = idxLean = newCharsPos = 0;
		while (idxFull < lenFull && idxFull2 < lenFull2) {
			cFull2 = full2.charAt(idxFull2);
			cFull = full.charAt(idxFull);
			if (cFull2 == cFull) { /* chars are equal, proceed */
				if (cFull2 != curMark)
					newChars[newCharsPos++] = chars[idxLean++];
				idxFull++;
				idxFull2++;
				continue;
			}
			if (cFull2 == curMark) { /* extra Mark in full2 text */
				idxFull2++;
				continue;
			}
			if (cFull == curMark) { /* extra Mark in source full text */
				idxFull++;
				// idxFull-2 always >= 0 since leading Marks were removed from full
				if (full.charAt(idxFull - 2) == curMark)
					continue; // ignore successive Marks in full after the first one
				newChars[newCharsPos++] = curMark;
				continue;
			}
			// we should never get here (extra char which is not a Mark)
			throw new IllegalStateException("Internal error: extra character not a Mark."); //$NON-NLS-1$
		}
		if (idxFull < lenFull) /* full2 ended before full - this should never happen since
								              we removed all marks and PDFs at the end of full */
			throw new IllegalStateException("Internal error: unexpected EOL."); //$NON-NLS-1$

		lean = new String(newChars, 0, newCharsPos);
		return lean;
	}

	public int[] fullToLeanMap(String full) {
		int lenFull = full.length();
		if (lenFull == 0)
			return EMPTY_INT_ARRAY;
		String lean = fullToLeanText(full);
		int lenLean = lean.length();
		int dir = handler.getDirection(this, lean);
		char curMark = MARKS[dir];
		char curEmbed = EMBEDS[dir];
		int[] map = new int[lenFull];
		int idxFull, idxLean;
		// skip any prefix and leading mark
		for (idxFull = 0; idxFull < lenFull; idxFull++) {
			char c = full.charAt(idxFull);
			if (c != curEmbed && c != curMark)
				break;
			map[idxFull] = -1;
		}
		// lean must be a subset of Full, so we only check on iLean < leanLen
		for (idxLean = 0; idxLean < lenLean; idxFull++) {
			if (full.charAt(idxFull) == lean.charAt(idxLean)) {
				map[idxFull] = idxLean;
				idxLean++;
			} else
				map[idxFull] = -1;
		}
		for (; idxFull < lenFull; idxFull++)
			map[idxFull] = -1;
		return map;
	}

	public int[] fullBidiCharOffsets(String full) {
		int lenFull = full.length();
		if (lenFull == 0)
			return EMPTY_INT_ARRAY;
		String lean = fullToLeanText(full);
		STextOffsets offsets = new STextOffsets();
		int lenLean = lean.length();
		int idxLean, idxFull;
		// lean must be a subset of Full, so we only check on iLean < leanLen
		for (idxLean = idxFull = 0; idxLean < lenLean; idxFull++) {
			if (full.charAt(idxFull) == lean.charAt(idxLean))
				idxLean++;
			else
				offsets.insertOffset(null, idxFull);
		}
		for (; idxFull < lenFull; idxFull++)
			offsets.insertOffset(null, idxFull);
		return offsets.getOffsets();
	}

	public String insertMarks(String text, int[] offsets, int direction, int affixLength) {
		if (direction != DIR_LTR && direction != DIR_RTL)
			throw new IllegalArgumentException("Invalid direction"); //$NON-NLS-1$
		if (affixLength < 0 || affixLength > 2)
			throw new IllegalArgumentException("Invalid affix length"); //$NON-NLS-1$
		int count = offsets == null ? 0 : offsets.length;
		if (count == 0 && affixLength == 0)
			return text;
		int textLength = text.length();
		if (textLength == 0)
			return text;
		int newLen = textLength + count;
		if (affixLength == 1)
			newLen++; /* +1 for a mark char */
		else if (affixLength == 2)
			newLen += FIXES_LENGTH;
		char[] fullChars = new char[newLen];
		int added = affixLength;
		// add marks at offsets
		char curMark = MARKS[direction];
		for (int i = 0, j = 0; i < textLength; i++) {
			char c = text.charAt(i);
			if (j < count && i == offsets[j]) {
				fullChars[i + added] = curMark;
				added++;
				j++;
			}
			fullChars[i + added] = c;
		}
		if (affixLength > 0) { /* add prefix/suffix ? */
			if (affixLength == 1) { /* contextual orientation */
				fullChars[0] = curMark;
			} else {
				// When the orientation is RTL, we need to add EMBED at the
				// start of the text and PDF at its end.
				// However, because of a bug in Windows' handling of LRE/RLE/PDF,
				// we add LRM or RLM (according to the direction) after the 
				// LRE/RLE and again before the PDF.
				char curEmbed = EMBEDS[direction];
				fullChars[0] = curEmbed;
				fullChars[1] = curMark;
				fullChars[newLen - 1] = PDF;
				fullChars[newLen - 2] = curMark;
			}
		}
		return new String(fullChars);
	}

}
