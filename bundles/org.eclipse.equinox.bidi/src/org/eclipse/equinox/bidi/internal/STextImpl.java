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

import org.eclipse.equinox.bidi.STextEngine;
import org.eclipse.equinox.bidi.STextEnvironment;
import org.eclipse.equinox.bidi.custom.*;

/**
 *  <code>STextImpl</code> provides the code which implements the API in
 *  {@link STextEngine}. All its public methods are shadows of similarly
 *  signed methods of <code>STextEngine</code>, and their documentation
 *  is by reference to the methods in <code>STextEngine</code>.
 *
 *  @author Matitiahu Allouche
 */
public class STextImpl {

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
	static final STextEnvironment IGNORE_ENVIRONMENT = new STextEnvironment(null, false, STextEnvironment.ORIENT_IGNORE);

	/**
	 *  Prevent creation of a STextImpl instance
	 */
	private STextImpl() {
		// nothing to do
	}

	static long computeNextLocation(STextProcessor processor, STextEnvironment environment, String text, STextCharTypes charTypes, STextOffsets offsets, int[] locations, int curPos) {
		String separators = processor.getSeparators(environment);
		int separCount = separators.length();
		int specialsCount = processor.getSpecialsCount(environment);
		int len = text.length();
		int nextLocation = len;
		int idxLocation = 0;
		// Start with special sequences to give them precedence over simple
		// separators. This may apply to cases like slash+asterisk versus slash.
		for (int i = 0; i < specialsCount; i++) {
			int location = locations[separCount + i];
			if (location < curPos) {
				offsets.ensureRoom();
				location = processor.indexOfSpecial(environment, text, charTypes, offsets, i + 1, curPos);
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
	 *  @see STextProcessor#processSeparator STextProcessor.processSeparator
	 */
	public static void processSeparator(String text, STextCharTypes charTypes, STextOffsets offsets, int separLocation) {
		int len = text.length();
		int direction = charTypes.getDirection();
		if (direction == STextEngine.DIR_RTL) {
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
	 *  {@link STextEngine#leanToFullText leanToFullText}
	 *  adds RLE+RLM at the head of the <i>full</i> text and RLM+PDF at its
	 *  end.
	 *  <p>
	 *  When the orientation is <code>ORIENT_RTL</code> and the
	 *  structured text has a LTR base direction,
	 *  {@link STextEngine#leanToFullText leanToFullText}
	 *  adds LRE+LRM at the head of the <i>full</i> text and LRM+PDF at its
	 *  end.
	 *  <p>
	 *  When the orientation is <code>ORIENT_CONTEXTUAL_LTR</code> or
	 *  <code>ORIENT_CONTEXTUAL_RTL</code> and the data content would resolve
	 *  to a RTL orientation while the structured text has a LTR base
	 *  direction, {@link STextEngine#leanToFullText leanToFullText}
	 *  adds LRM at the head of the <i>full</i> text.
	 *  <p>
	 *  When the orientation is <code>ORIENT_CONTEXTUAL_LTR</code> or
	 *  <code>ORIENT_CONTEXTUAL_RTL</code> and the data content would resolve
	 *  to a LTR orientation while the structured text has a RTL base
	 *  direction, {@link STextEngine#leanToFullText leanToFullText}
	 *  adds RLM at the head of the <i>full</i> text.
	 *  <p>
	 *  When the orientation is <code>ORIENT_UNKNOWN</code> and the
	 *  structured text has a LTR base direction,
	 *  {@link STextEngine#leanToFullText leanToFullText}
	 *  adds LRE+LRM at the head of the <i>full</i> text and LRM+PDF at its
	 *  end.
	 *  <p>
	 *  When the orientation is <code>ORIENT_UNKNOWN</code> and the
	 *  structured text has a RTL base direction,
	 *  {@link STextEngine#leanToFullText leanToFullText}
	 *  adds RLE+RLM at the head of the <i>full</i> text and RLM+PDF at its
	 *  end.
	 *  <p>
	 *  When the orientation is <code>ORIENT_IGNORE</code>,
	 *  {@link STextEngine#leanToFullText leanToFullText} does not add any directional
	 *  formatting characters as either prefix or suffix of the <i>full</i> text.
	 *  <p>
	 *  @see STextEngine#leanToFullText STextEngine.leanToFullText
	 */
	public static String leanToFullText(STextProcessor processor, STextEnvironment environment, String text, int[] state) {
		int len = text.length();
		if (len == 0)
			return text;
		STextCharTypes charTypes = new STextCharTypes(processor, environment, text);
		STextOffsets offsets = leanToFullCommon(processor, environment, text, state, charTypes);
		int prefixLength = offsets.getPrefixLength();
		int count = offsets.getCount();
		if (count == 0 && prefixLength == 0)
			return text;
		int newLen = len + count;
		if (prefixLength == 1)
			newLen++; /* +1 for a mark char */
		else if (prefixLength == 2)
			newLen += FIXES_LENGTH;
		char[] fullChars = new char[newLen];
		int added = prefixLength;
		// add marks at offsets
		int direction = charTypes.getDirection();
		char curMark = MARKS[direction];
		for (int i = 0, j = 0; i < len; i++) {
			char c = text.charAt(i);
			if (j < count && i == offsets.getOffset(j)) {
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
				char curEmbed = EMBEDS[direction];
				fullChars[0] = curEmbed;
				fullChars[1] = curMark;
				fullChars[newLen - 1] = PDF;
				fullChars[newLen - 2] = curMark;
			}
		}
		return new String(fullChars);
	}

	/**
	 *  @see STextEngine#leanToFullMap STextEngine.leanToFullMap
	 */
	public static int[] leanToFullMap(STextProcessor processor, STextEnvironment environment, String text, int[] state) {
		int len = text.length();
		if (len == 0)
			return EMPTY_INT_ARRAY;
		STextCharTypes charTypes = new STextCharTypes(processor, environment, text);
		STextOffsets offsets = leanToFullCommon(processor, environment, text, state, charTypes);
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

	/**
	 *  @see STextEngine#leanBidiCharOffsets STextEngine.leanBidiCharOffsets
	 */
	public static int[] leanBidiCharOffsets(STextProcessor processor, STextEnvironment environment, String text, int[] state) {
		int len = text.length();
		if (len == 0)
			return EMPTY_INT_ARRAY;
		STextCharTypes charTypes = new STextCharTypes(processor, environment, text);
		STextOffsets offsets = leanToFullCommon(processor, environment, text, state, charTypes);
		return offsets.getArray();
	}

	static STextOffsets leanToFullCommon(STextProcessor processor, STextEnvironment environment, String text, int[] state, STextCharTypes charTypes) {
		if (environment == null)
			environment = STextEnvironment.DEFAULT;
		if (state == null) {
			state = new int[1];
			state[0] = STextEngine.STATE_INITIAL;
		}
		int len = text.length();
		int direction = processor.getDirection(environment, text, charTypes);
		STextOffsets offsets = new STextOffsets();
		if (!processor.skipProcessing(environment, text, charTypes)) {
			// initialize locations
			int separCount = processor.getSeparators(environment).length();
			int[] locations = new int[separCount + processor.getSpecialsCount(environment)];
			for (int i = 0, k = locations.length; i < k; i++) {
				locations[i] = -1;
			}
			// current position
			int curPos = 0;
			if (state[0] > STextEngine.STATE_INITIAL) {
				offsets.ensureRoom();
				int initState = state[0];
				state[0] = STextEngine.STATE_INITIAL;
				curPos = processor.processSpecial(environment, text, charTypes, offsets, state, initState, -1);
			}
			while (true) {
				// location of next token to handle
				int nextLocation;
				// index of next token to handle (if < separCount, this is a separator; otherwise a special case
				int idxLocation;
				long res = computeNextLocation(processor, environment, text, charTypes, offsets, locations, curPos);
				nextLocation = (int) (res & 0x00000000FFFFFFFF); /* low word */
				if (nextLocation >= len)
					break;
				offsets.ensureRoom();
				idxLocation = (int) (res >> 32); /* high word */
				if (idxLocation < separCount) {
					processSeparator(text, charTypes, offsets, nextLocation);
					curPos = nextLocation + 1;
				} else {
					idxLocation -= (separCount - 1); // because caseNumber starts from 1
					curPos = processor.processSpecial(environment, text, charTypes, offsets, state, idxLocation, nextLocation);
				}
				if (curPos >= len)
					break;
			} // end while
		} // end if (!processor.skipProcessing())
		int prefixLength;
		int orientation = environment.getOrientation();
		if (orientation == STextEnvironment.ORIENT_IGNORE)
			prefixLength = 0;
		else {
			int resolvedOrientation = charTypes.resolveOrientation(environment);
			if (orientation != STextEnvironment.ORIENT_UNKNOWN && resolvedOrientation == direction)
				prefixLength = 0;
			else if ((orientation & STextEnvironment.ORIENT_CONTEXTUAL_LTR) != 0)
				prefixLength = 1;
			else
				prefixLength = 2;
		}
		offsets.setPrefixLength(prefixLength);
		return offsets;
	}

	/**
	 *  @see STextEngine#fullToLeanText STextEngine.fullToLeanText
	 */
	public static String fullToLeanText(STextProcessor processor, STextEnvironment environment, String text, int[] state) {
		if (text.length() == 0)
			return text;
		if (environment == null)
			environment = STextEnvironment.DEFAULT;
		if (state == null) {
			state = new int[1];
			state[0] = STextEngine.STATE_INITIAL;
		}
		int dir = processor.getDirection(environment, text);
		char curMark = MARKS[dir];
		char curEmbed = EMBEDS[dir];
		int i; // used as loop index
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
		if (i < 0) // only suffix and trailing marks, no real data
			return EMPTY_STRING;
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
		String full = leanToFullText(processor, IGNORE_ENVIRONMENT, lean, state);
		if (full.equals(text))
			return lean;

		// There are some marks in full which are not in text and/or vice versa.
		// We need to add to lean any mark appearing in text and not in full.
		// The completed lean can never be longer than text itself.
		char[] newChars = new char[lenText];
		char cFull, cText;
		int idxFull, idxText, idxLean, newCharsPos;
		int lenFull = full.length();
		idxFull = idxText = idxLean = newCharsPos = 0;
		while (idxText < lenText && idxFull < lenFull) {
			cFull = full.charAt(idxFull);
			cText = text.charAt(idxText);
			if (cFull == cText) { /* chars are equal, proceed */
				if (cFull != curMark)
					newChars[newCharsPos++] = chars[idxLean++];
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
				newChars[newCharsPos++] = curMark;
				continue;
			}
			// we should never get here (extra char which is not a Mark)
			throw new IllegalStateException("Internal error: extra character not a Mark."); //$NON-NLS-1$
		}
		if (idxText < lenText) /* full ended before text - this should never happen since
								              we removed all marks and PDFs at the end of text */
			throw new IllegalStateException("Internal error: unexpected EOL."); //$NON-NLS-1$

		lean = new String(newChars, 0, newCharsPos);
		return lean;
	}

	/**
	 *  @see STextEngine#fullToLeanMap STextEngine.fullToLeanMap
	 */
	public static int[] fullToLeanMap(STextProcessor processor, STextEnvironment environment, String full, int[] state) {
		int lenFull = full.length();
		if (lenFull == 0)
			return EMPTY_INT_ARRAY;
		String lean = fullToLeanText(processor, environment, full, state);
		int lenLean = lean.length();
		int dir = processor.getDirection(environment, lean);
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

	/**
	 *  @see STextEngine#fullBidiCharOffsets STextEngine.fullBidiCharOffsets
	 */
	public static int[] fullBidiCharOffsets(STextProcessor processor, STextEnvironment environment, String full, int[] state) {
		int lenFull = full.length();
		if (lenFull == 0)
			return EMPTY_INT_ARRAY;
		String lean = fullToLeanText(processor, environment, full, state);
		STextOffsets offsets = new STextOffsets();
		int lenLean = lean.length();
		int idxLean, idxFull;
		// lean must be a subset of Full, so we only check on iLean < leanLen
		for (idxLean = idxFull = 0; idxLean < lenLean; idxFull++) {
			if (full.charAt(idxFull) == lean.charAt(idxLean))
				idxLean++;
			else {
				offsets.ensureRoom();
				offsets.insertOffset(null, idxFull);
			}
		}
		for (; idxFull < lenFull; idxFull++) {
			offsets.ensureRoom();
			offsets.insertOffset(null, idxFull);
		}
		return offsets.getArray();
	}
}
