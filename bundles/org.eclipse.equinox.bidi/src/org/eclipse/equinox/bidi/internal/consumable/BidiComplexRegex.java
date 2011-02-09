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
package org.eclipse.equinox.bidi.internal.consumable;

import org.eclipse.equinox.bidi.*;
import org.eclipse.equinox.bidi.custom.BidiComplexProcessor;

/**
 *  <code>BidiComplexRegex</code> is a processor for regular expressions.
 *  Such expressions may span multiple lines.
 *  <p>
 *  In applications like an editor where parts of the text might be modified
 *  while other parts are not, the user may want to call
 *  {@link BidiComplexHelper#leanToFullText leanToFullText}
 *  separately on each line and save the initial state of each line (this is
 *  the final state of the previous line which can be retrieved using
 *  {@link BidiComplexHelper#getFinalState getFinalState}. If both the content
 *  of a line and its initial state have not changed, the user can be sure that
 *  the last <i>full</i> text computed for this line has not changed either.
 *
 *  @see BidiComplexHelper
 *
 *  @author Matitiahu Allouche
 */
public class BidiComplexRegex extends BidiComplexProcessor {
	static final String[] startStrings = {"(?#", /*  0 *//* comment (?#...) *///$NON-NLS-1$
			"(?<", /*  1 *//* named group (?<name> *///$NON-NLS-1$
			"(?'", /*  2 *//* named group (?'name' *///$NON-NLS-1$
			"(?(<", /*  3 *//* conditional named back reference (?(<name>) *///$NON-NLS-1$
			"(?('", /*  4 *//* conditional named back reference (?('name') *///$NON-NLS-1$
			"(?(", /*  5 *//* conditional named back reference (?(name) *///$NON-NLS-1$
			"(?&", /*  6 *//* named parentheses reference (?&name) *///$NON-NLS-1$
			"(?P<", /*  7 *//* named group (?P<name> *///$NON-NLS-1$
			"\\k<", /*  8 *//* named back reference \k<name> *///$NON-NLS-1$
			"\\k'", /*  9 *//* named back reference \k'name' *///$NON-NLS-1$
			"\\k{", /* 10 *//* named back reference \k{name} *///$NON-NLS-1$
			"(?P=", /* 11 *//* named back reference (?P=name) *///$NON-NLS-1$
			"\\g{", /* 12 *//* named back reference \g{name} *///$NON-NLS-1$
			"\\g<", /* 13 *//* subroutine call \g<name> *///$NON-NLS-1$
			"\\g'", /* 14 *//* subroutine call \g'name' *///$NON-NLS-1$
			"(?(R&", /* 15 *//* named back reference recursion (?(R&name) *///$NON-NLS-1$
			"\\Q" /* 16 *//* quoted sequence \Q...\E *///$NON-NLS-1$
	};
	static final char[] endChars = {
			// 0    1     2    3    4    5    6    7    8     9   10   11   12   13    14   15
			')', '>', '\'', ')', ')', ')', ')', '>', '>', '\'', '}', ')', '}', '>', '\'', ')'};
	static final int numberOfStrings = startStrings.length;
	static final int maxSpecial = numberOfStrings + 1;
	static final BidiComplexFeatures FEATURES = new BidiComplexFeatures(null, maxSpecial, -1, -1, false, false);
	static final byte L = Character.DIRECTIONALITY_LEFT_TO_RIGHT;
	static final byte R = Character.DIRECTIONALITY_RIGHT_TO_LEFT;
	static final byte AL = Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
	static final byte AN = Character.DIRECTIONALITY_ARABIC_NUMBER;
	static final byte EN = Character.DIRECTIONALITY_EUROPEAN_NUMBER;

	/**
	 *  This method retrieves the features specific to this processor.
	 *
	 *  @return features with no operators , special cases for each kind of
	 *          regular expression syntactic string,
	 *          LTR direction for Arabic and Hebrew, and support for both.
	 */
	public BidiComplexFeatures init(BidiComplexHelper helper, BidiComplexEnvironment env) {
		return FEATURES;
	}

	/**
	 *  This method locates occurrences of the syntactic strings and of
	 *  R, AL, EN, AN characters.
	 */
	public int indexOfSpecial(BidiComplexHelper helper, int caseNumber, String srcText, int fromIndex) {
		// In this method, L, R, AL, AN and EN represent bidi categories
		// as defined in the Unicode Bidirectional Algorithm
		// ( http://www.unicode.org/reports/tr9/ ).
		// L  represents the category Left to Right character.
		// R  represents the category Right to Left character.
		// AL represents the category Arabic Letter.
		// AN represents the category Arabic Number.
		// EN  represents the category European Number.
		byte dirProp;

		if (caseNumber < numberOfStrings) {
			/*  0 *//* comment (?#...) */
			/*  1 *//* named group (?<name> */
			/*  2 *//* named group (?'name' */
			/*  3 *//* conditional named back reference (?(name) */
			/*  4 *//* conditional named back reference (?(<name>) */
			/*  5 *//* conditional named back reference (?('name') */
			/*  6 *//* named parentheses reference (?&name) */
			/*  7 *//* named group (?P<name> */
			/*  8 *//* named back reference \k<name> */
			/*  9 *//* named back reference \k'name' */
			/* 10 *//* named back reference \k{name} */
			/* 11 *//* named back reference (?P=name) */
			/* 12 *//* named back reference \g{name} */
			/* 13 *//* subroutine call \g<name> */
			/* 14 *//* subroutine call \g'name' */
			/* 15 *//* named back reference recursion (?(R&name) */
			/* 16 *//* quoted sequence \Q...\E */
			return srcText.indexOf(startStrings[caseNumber], fromIndex);
		}
		// there never is a need for a mark before the first char
		if (fromIndex <= 0)
			fromIndex = 1;
		// look for R, AL, AN, EN which are potentially needing a mark
		for (; fromIndex < srcText.length(); fromIndex++) {
			dirProp = helper.getDirProp(fromIndex);
			// R and AL will always be examined using processOperator()
			if (dirProp == R || dirProp == AL)
				return fromIndex;

			if (dirProp == EN || dirProp == AN) {
				// no need for a mark after the first digit in a number
				if (helper.getDirProp(fromIndex - 1) == dirProp)
					continue;

				for (int i = fromIndex - 1; i >= 0; i--) {
					dirProp = helper.getDirProp(i);
					// after a L char, no need for a mark
					if (dirProp == L)
						continue;

					// digit after R or AL or AN need a mark, except for EN
					//   following AN, but this is a contrived case, so we
					//   don't check for it (and calling processOperator()
					//   for it will do no harm)
					if (dirProp == R || dirProp == AL || dirProp == AN)
						return fromIndex;
				}
				continue;
			}
		}
		return -1;
	}

	/**
	 *  This method process the special cases.
	 */
	public int processSpecial(BidiComplexHelper helper, int caseNumber, String srcText, int operLocation) {
		int location;

		switch (caseNumber) {
			case 0 : /* comment (?#...) */
				if (operLocation < 0) {
					// initial state from previous line
					location = 0;
				} else {
					helper.processOperator(operLocation);
					// skip the opening "(?#"
					location = operLocation + 3;
				}
				location = srcText.indexOf(')', location);
				if (location < 0) {
					helper.setFinalState(caseNumber);
					return srcText.length();
				}
				return location + 1;
			case 1 : /* named group (?<name> */
			case 2 : /* named group (?'name' */
			case 3 : /* conditional named back reference (?(name) */
			case 4 : /* conditional named back reference (?(<name>) */
			case 5 : /* conditional named back reference (?('name') */
			case 6 : /* named parentheses reference (?&name) */
				helper.processOperator(operLocation);
				// no need for calling processOperator() for the following cases
				//   since the starting string contains a L char
			case 7 : /* named group (?P<name> */
			case 8 : /* named back reference \k<name> */
			case 9 : /* named back reference \k'name' */
			case 10 : /* named back reference \k{name} */
			case 11 : /* named back reference (?P=name) */
			case 12 : /* named back reference \g{name} */
			case 13 : /* subroutine call \g<name> */
			case 14 : /* subroutine call \g'name' */
			case 15 : /* named back reference recursion (?(R&name) */
				// skip the opening string
				location = operLocation + startStrings[caseNumber].length();
				// look for ending character
				location = srcText.indexOf(endChars[caseNumber], location);
				if (location < 0)
					return srcText.length();
				return location + 1;
			case 16 : /* quoted sequence \Q...\E */
				if (operLocation < 0) {
					// initial state from previous line
					location = 0;
				} else {
					helper.processOperator(operLocation);
					// skip the opening "\Q"
					location = operLocation + 2;
				}
				location = srcText.indexOf("\\E", location); //$NON-NLS-1$
				if (location < 0) {
					helper.setFinalState(caseNumber);
					return srcText.length();
				}
				// set the dirProp for the "E" to L (Left to Right character)
				helper.setDirProp(location + 1, L);
				return location + 2;
			case 17 : /* R, AL, AN, EN */
				helper.processOperator(operLocation);
				return operLocation + 1;

		}
		// we should never get here
		return srcText.length();
	}
}
