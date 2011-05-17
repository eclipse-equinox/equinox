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

import org.eclipse.equinox.bidi.BidiComplexEngine;
import org.eclipse.equinox.bidi.BidiComplexEnvironment;
import org.eclipse.equinox.bidi.custom.*;

/**
 *  <code>BidiComplexImpl</code> provides the code which implements the API in
 *  {@link BidiComplexEngine}. All its public methods are shadows of similarly
 *  signed methods of <code>BidiComplexEngine</code>, and their documentation
 *  is by reference to the methods in <code>BidiComplexEngine</code>.
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
	static final int DIRPROPS_ADD = 2;
	static final int OFFSETS_SHIFT = 3;
	static final int[] EMPTY_INT_ARRAY = new int[0];
	static final BidiComplexEnvironment IGNORE_ENVIRONMENT = new BidiComplexEnvironment(null, false, BidiComplexEnvironment.ORIENT_IGNORE);

	/**
	 *  Prevent creation of a BidiComplexEngine instance
	 */
	private BidiComplexImpl() {
		// nothing to do
	}

	/*
	        // keep private copy of specialsCount to avoid later modification
	        specialsCount = features.getSpecialsCount();
	        locations = new int[features.getSeparators().length() + specialsCount];
	    }
	*/
	static long computeNextLocation(IBidiComplexProcessor processor, BidiComplexFeatures features, String text, byte[] dirProps, int[] offsets, int[] locations, int[] state, int curPos) {
		String separators = features.getSeparators();
		int separCount = separators.length();
		int specialsCount = features.getSpecialsCount();
		int len = text.length();
		int nextLocation = len;
		int idxLocation = 0;
		// Start with special sequences to give them precedence over simple
		// separators. This may apply to cases like slash+asterisk versus slash.
		for (int i = 0; i < specialsCount; i++) {
			int location = locations[separCount + i];
			if (location < curPos) {
				offsets = ensureRoomInOffsets(offsets);
				location = processor.indexOfSpecial(features, text, dirProps, offsets, i + 1, curPos);
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

	static int getCurOrient(BidiComplexEnvironment environment, String text, byte[] dirProps) {
		int orient = environment.getOrientation();
		if ((orient & BidiComplexEnvironment.ORIENT_CONTEXTUAL_LTR) == 0) {
			// absolute orientation
			return orient;
		}
		// contextual orientation
		int len = text.length();
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
				dirProp = Character.getDirectionality(text.charAt(i));
				if (dirProp == B) // B char resolves to L or R depending on orientation
					continue;
				dirProps[i] = (byte) (dirProp + DIRPROPS_ADD);
			} else {
				dirProp -= DIRPROPS_ADD;
			}
			if (dirProp == L)
				return BidiComplexEnvironment.ORIENT_LTR;
			if (dirProp == R || dirProp == AL)
				return BidiComplexEnvironment.ORIENT_RTL;
		}
		// return the default orientation corresponding to the contextual orientation
		return orient & 1;
	}

	/**
	 *  @see BidiComplexEngine#getCurDirection BidiComplexEngine.getCurDirection
	 */
	public static int getCurDirection(Object _processor, BidiComplexFeatures features, BidiComplexEnvironment environment, String text, byte[] dirProps) {
		if (environment == null)
			environment = BidiComplexEnvironment.DEFAULT;
		if (features == null) {
			if (_processor == null)
				return BidiComplexFeatures.DIR_LTR;
			IBidiComplexProcessor processor;
			if (_processor instanceof java.lang.String) {
				processor = BidiComplexStringProcessor.getProcessor((String) _processor);
				if (processor == null)
					throw new IllegalArgumentException("Invalid processor type!"); //$NON-NLS-1$
			} else if (_processor instanceof IBidiComplexProcessor)
				processor = (IBidiComplexProcessor) _processor;
			else
				throw new IllegalArgumentException("Invalid processor argument!"); //$NON-NLS-1$
			features = processor.getFeatures(environment);
		}
		int dirArabic = features.getDirArabic();
		int dirHebrew = features.getDirHebrew();
		// same direction for Arabic and Hebrew?
		if (dirArabic == dirHebrew)
			return dirArabic;
		// check if Arabic or Hebrew letter comes first
		int len = text.length();
		if (dirProps == null)
			dirProps = new byte[len + 1];
		byte dirProp;
		for (int i = 0; i < len; i++) {
			// In the following lines, R and AL represent bidi categories
			// as defined in the Unicode Bidirectional Algorithm
			// ( http://www.unicode.org/reports/tr9/ ).
			// R  represents the category Right to Left character.
			// AL represents the category Arabic Letter.
			byte saveOrient = dirProps[len];
			dirProps[len] = -1; // make getDirProp return B
			dirProp = getDirProp(text, dirProps, i);
			dirProps[len] = saveOrient;
			if (dirProp == AL)
				return dirArabic;
			if (dirProp == R)
				return dirHebrew;
		}
		// found no Arabic or Hebrew character
		return BidiComplexFeatures.DIR_LTR;
	}

	/**
	 *  @see BidiComplexProcessor#getDirProp BidiComplexProcessor.getDirProp
	 */
	public static byte getDirProp(String text, byte[] dirProps, int index) {
		byte dirProp = dirProps[index];
		if (dirProp == 0) {
			// In the following lines, B, L and R represent bidi categories
			// as defined in the Unicode Bidirectional Algorithm
			// ( http://www.unicode.org/reports/tr9/ ).
			// B  represents the category Block Separator.
			// L  represents the category Left to Right character.
			// R  represents the category Right to Left character.
			dirProp = Character.getDirectionality(text.charAt(index));
			if (dirProp == B) {
				// the last entry of dirProps contains the current component orientation
				byte orient = dirProps[dirProps.length - 1];
				if (orient == -1)
					return B;
				dirProp = orient == BidiComplexEnvironment.ORIENT_RTL ? R : L;
			}
			dirProps[index] = (byte) (dirProp + DIRPROPS_ADD);
			return dirProp;
		}
		return (byte) (dirProp - DIRPROPS_ADD);
	}

	/**
	 *  @see BidiComplexProcessor#setDirProp BidiComplexProcessor.setDirProp
	 */
	public static void setDirProp(byte[] dirProps, int index, byte dirProp) {
		dirProps[index] = (byte) (dirProp + DIRPROPS_ADD);
	}

	/**
	 *  @see BidiComplexProcessor#processSeparator BidiComplexProcessor.processSeparator
	 */
	public static void processSeparator(BidiComplexFeatures features, String text, byte[] dirProps, int[] offsets, int separLocation) {
		// In this method, L, R, AL, AN and EN represent bidi categories
		// as defined in the Unicode Bidirectional Algorithm
		// ( http://www.unicode.org/reports/tr9/ ).
		// L  represents the category Left to Right character.
		// R  represents the category Right to Left character.
		// AL represents the category Arabic Letter.
		// AN represents the category Arabic Number.
		// EN  represents the category European Number.
		int len = text.length();
		// offsets[2] contains the complex expression direction
		if (offsets[2] == BidiComplexFeatures.DIR_RTL) {
			// the expression base direction is RTL
			for (int i = separLocation - 1; i >= 0; i--) {
				byte dirProp = getDirProp(text, dirProps, i);
				if (dirProp == R || dirProp == AL)
					return;
				if (dirProp == L) {
					for (int j = separLocation; j < len; j++) {
						dirProp = getDirProp(text, dirProps, j);
						if (dirProp == R || dirProp == AL)
							return;
						if (dirProp == L || dirProp == EN) {
							insertMark(text, dirProps, offsets, separLocation);
							return;
						}
					}
					return;
				}
			}
			return;
		}

		// the expression base direction is LTR
		boolean doneAN = false;
		boolean ignoreArabic = features.getIgnoreArabic();
		boolean ignoreHebrew = features.getIgnoreHebrew();
		if (ignoreArabic && ignoreHebrew)
			return;
		byte _R, _AL, _AN;
		if (ignoreArabic) {
			_AL = Byte.MIN_VALUE; // not a real value
			_AN = Byte.MIN_VALUE;
		} else {
			_AL = AL;
			_AN = AN;
		}
		if (ignoreHebrew)
			_R = Byte.MIN_VALUE;
		else
			_R = R;
		for (int i = separLocation - 1; i >= 0; i--) {
			byte dirProp = getDirProp(text, dirProps, i);
			if (dirProp == L)
				return;
			if (dirProp == _R || dirProp == _AL) {
				for (int j = separLocation; j < len; j++) {
					dirProp = getDirProp(text, dirProps, j);
					if (dirProp == L)
						return;
					if (dirProp == _R || dirProp == EN || dirProp == _AL || dirProp == _AN) {
						insertMark(text, dirProps, offsets, separLocation);
						return;
					}
				}
				return;
			}
			if (dirProp == _AN && !doneAN) {
				for (int j = separLocation; j < len; j++) {
					dirProp = getDirProp(text, dirProps, j);
					if (dirProp == L)
						return;
					if (dirProp == _AL || dirProp == _AN || dirProp == _R) {
						insertMark(text, dirProps, offsets, separLocation);
						return;
					}
				}
				doneAN = true;
			}
		}
	}

	/**
	 *  @see BidiComplexEngine#leanToFullText BidiComplexEngine.leanToFullText
	 */
	public static String leanToFullText(Object processor, BidiComplexFeatures features, BidiComplexEnvironment environment, String text, int[] state) {
		int len = text.length();
		if (len == 0)
			return text;
		byte[] dirProps = new byte[len + 1];
		int[] offsets = leanToFullCommon(processor, features, environment, text, state, dirProps);
		int prefixLength = offsets[1];
		int count = offsets[0] - OFFSETS_SHIFT;
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
		int direction = offsets[2];
		char curMark = MARKS[direction];
		for (int i = 0, j = OFFSETS_SHIFT; i < len; i++) {
			char c = text.charAt(i);
			// offsets[0] contains the number of used entries
			if (j < offsets[0] && i == offsets[j]) {
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
	 *  @see BidiComplexEngine#leanToFullMap BidiComplexEngine.leanToFullMap
	 */
	public static int[] leanToFullMap(Object processor, BidiComplexFeatures features, BidiComplexEnvironment environment, String text, int[] state) {
		int len = text.length();
		if (len == 0)
			return EMPTY_INT_ARRAY;
		byte[] dirProps = new byte[len + 1];
		int[] offsets = leanToFullCommon(processor, features, environment, text, state, dirProps);
		int prefixLength = offsets[1];
		int[] map = new int[len];
		int count = offsets[0]; // number of used entries
		int added = prefixLength;
		for (int pos = 0, i = OFFSETS_SHIFT; pos < len; pos++) {
			if (i < count && pos == offsets[i]) {
				added++;
				i++;
			}
			map[pos] = pos + added;
		}
		return map;
	}

	/**
	 *  @see BidiComplexEngine#leanBidiCharOffsets BidiComplexEngine.leanBidiCharOffsets
	 */
	public static int[] leanBidiCharOffsets(Object processor, BidiComplexFeatures features, BidiComplexEnvironment environment, String text, int[] state) {
		int len = text.length();
		if (len == 0)
			return EMPTY_INT_ARRAY;
		byte[] dirProps = new byte[len + 1];
		int[] offsets = leanToFullCommon(processor, features, environment, text, state, dirProps);
		// offsets[0] contains the number of used entries
		int count = offsets[0] - OFFSETS_SHIFT;
		int[] result = new int[count];
		System.arraycopy(offsets, OFFSETS_SHIFT, result, 0, count);
		return result;
	}

	static int[] leanToFullCommon(Object _processor, BidiComplexFeatures features, BidiComplexEnvironment environment, String text, int[] state, byte[] dirProps) {
		IBidiComplexProcessor processor;
		if (_processor instanceof java.lang.String) {
			processor = BidiComplexStringProcessor.getProcessor((String) _processor);
			if (processor == null)
				throw new IllegalArgumentException("Invalid processor type!"); //$NON-NLS-1$
		} else if (_processor instanceof IBidiComplexProcessor)
			processor = (IBidiComplexProcessor) _processor;
		else
			throw new IllegalArgumentException("Invalid processor argument!"); //$NON-NLS-1$
		if (environment == null)
			environment = BidiComplexEnvironment.DEFAULT;
		if (features == null)
			features = processor.getFeatures(environment);
		if (state == null) {
			state = new int[1];
			state[0] = BidiComplexEngine.STATE_INITIAL;
		}
		int len = text.length();
		// dirProps: 1 byte for each char in text, + 1 byte = current orientation
		int orient = getCurOrient(environment, text, dirProps);
		dirProps[len] = (byte) orient;
		int separCount = features.getSeparators().length();
		int direction = getCurDirection(processor, features, environment, text, dirProps);
		// current position
		int curPos = 0;
		// offsets of marks to add. Entry 0 is the number of used slots;
		//  entry 1 is reserved to pass prefixLength.
		//  entry 2 is reserved to pass direction..
		int[] offsets = new int[20];
		offsets[0] = OFFSETS_SHIFT;
		offsets[2] = direction;
		// initialize locations
		int[] locations = new int[separCount + features.getSpecialsCount()];
		for (int i = 0, k = locations.length; i < k; i++) {
			locations[i] = -1;
		}
		if (state[0] > BidiComplexEngine.STATE_INITIAL) {
			offsets = ensureRoomInOffsets(offsets);
			int initState = state[0];
			state[0] = BidiComplexEngine.STATE_INITIAL;
			curPos = processor.processSpecial(features, text, dirProps, offsets, state, initState, -1);
		}
		while (true) {
			// location of next token to handle
			int nextLocation;
			// index of next token to handle (if < separCount, this is a separator; otherwise a special case
			int idxLocation;
			long res = computeNextLocation(processor, features, text, dirProps, offsets, locations, state, curPos);
			nextLocation = (int) (res & 0x00000000FFFFFFFF); /* low word */
			if (nextLocation >= len)
				break;
			idxLocation = (int) (res >> 32); /* high word */
			if (idxLocation < separCount) {
				offsets = ensureRoomInOffsets(offsets);
				processSeparator(features, text, dirProps, offsets, nextLocation);
				curPos = nextLocation + 1;
			} else {
				offsets = ensureRoomInOffsets(offsets);
				idxLocation -= (separCount - 1); // because caseNumber starts from 1
				curPos = processor.processSpecial(features, text, dirProps, offsets, state, idxLocation, nextLocation);
			}
		}
		if (orient == BidiComplexEnvironment.ORIENT_IGNORE)
			offsets[1] = 0;
		else {
			// recompute orient since it may have changed if contextual
			orient = getCurOrient(environment, text, dirProps);
			dirProps[len] = (byte) orient;
			if (orient == direction && orient != BidiComplexEnvironment.ORIENT_UNKNOWN)
				offsets[1] = 0;
			else if ((environment.getOrientation() & BidiComplexEnvironment.ORIENT_CONTEXTUAL_LTR) != 0)
				offsets[1] = 1;
			else
				offsets[1] = 2;
		}
		return offsets;
	}

	/**
	 *  @see BidiComplexEngine#fullToLeanText BidiComplexEngine.fullToLeanText
	 */
	public static String fullToLeanText(Object _processor, BidiComplexFeatures features, BidiComplexEnvironment environment, String text, int[] state) {
		if (text.length() == 0)
			return text;
		IBidiComplexProcessor processor;
		if (_processor instanceof java.lang.String) {
			processor = BidiComplexStringProcessor.getProcessor((String) _processor);
			if (processor == null)
				throw new IllegalArgumentException("Invalid processor type!"); //$NON-NLS-1$
		} else if (_processor instanceof IBidiComplexProcessor)
			processor = (IBidiComplexProcessor) _processor;
		else
			throw new IllegalArgumentException("Invalid processor argument!"); //$NON-NLS-1$
		if (environment == null)
			environment = BidiComplexEnvironment.DEFAULT;
		if (features == null)
			features = processor.getFeatures(environment);
		if (state == null) {
			state = new int[1];
			state[0] = BidiComplexEngine.STATE_INITIAL;
		}
		int dir = getCurDirection(processor, features, environment, text, null);
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
		String full = leanToFullText(processor, features, IGNORE_ENVIRONMENT, lean, state);
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
	 *  @see BidiComplexEngine#fullToLeanMap BidiComplexEngine.fullToLeanMap
	 */
	public static int[] fullToLeanMap(Object processor, BidiComplexFeatures features, BidiComplexEnvironment environment, String full, int[] state) {
		int lenFull = full.length();
		if (lenFull == 0)
			return EMPTY_INT_ARRAY;
		String lean = fullToLeanText(processor, features, environment, full, state);
		int lenLean = lean.length();
		int dir = getCurDirection(processor, features, environment, lean, null);
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
	 *  @see BidiComplexEngine#fullBidiCharOffsets BidiComplexEngine.fullBidiCharOffsets
	 */
	public static int[] fullBidiCharOffsets(Object processor, BidiComplexFeatures features, BidiComplexEnvironment environment, String full, int[] state) {
		int lenFull = full.length();
		if (lenFull == 0)
			return EMPTY_INT_ARRAY;
		String lean = fullToLeanText(processor, features, environment, full, state);
		int[] offsets = new int[20];
		offsets[0] = OFFSETS_SHIFT;
		int lenLean = lean.length();
		int idxLean, idxFull;
		// lean must be a subset of Full, so we only check on iLean < leanLen
		for (idxLean = idxFull = 0; idxLean < lenLean; idxFull++) {
			if (full.charAt(idxFull) == lean.charAt(idxLean))
				idxLean++;
			else {
				offsets = ensureRoomInOffsets(offsets);
				insertMark(lean, null, offsets, idxFull);
			}
		}
		for (; idxFull < lenFull; idxFull++) {
			offsets = ensureRoomInOffsets(offsets);
			insertMark(lean, null, offsets, idxFull);
		}
		int[] result = new int[offsets[0] - OFFSETS_SHIFT];
		System.arraycopy(offsets, OFFSETS_SHIFT, result, 0, result.length);
		return result;
	}

	static int[] ensureRoomInOffsets(int[] offsets) {
		// make sure
		if ((offsets.length - offsets[0]) < 3) {
			int[] newOffsets = new int[offsets.length * 2];
			System.arraycopy(offsets, 0, newOffsets, 0, offsets[0]);
			return newOffsets;
		}
		return offsets;
	}

	/**
	 *  @see BidiComplexProcessor#insertMark BidiComplexProcessor.insertMark
	 */
	public static void insertMark(String text, byte[] dirProps, int[] offsets, int offset) {
		int count = offsets[0];// number of used entries
		int index = count - 1; // index of greatest member <= offset
		// look up after which member the new offset should be inserted
		while (index >= OFFSETS_SHIFT) {
			int wrkOffset = offsets[index];
			if (offset > wrkOffset)
				break;
			if (offset == wrkOffset)
				return; // avoid duplicates
			index--;
		}
		index++; // index now points at where to insert
		int length = count - index; // number of members to move up
		if (length > 0) // shift right all members greater than offset
			System.arraycopy(offsets, index, offsets, index + 1, length);
		offsets[index] = offset;
		offsets[0]++; // number of used entries
		// if the offset is 0, adding a mark does not change anything
		if (dirProps == null || offset < 1)
			return;

		byte dirProp = getDirProp(text, dirProps, offset);
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

		int dir = offsets[2]; // current expression direction
		setDirProp(dirProps, index, STRONGS[dir]);
		return;
	}

}
