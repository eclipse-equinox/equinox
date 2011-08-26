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

import org.eclipse.equinox.bidi.advanced.ISTextExpert;
import org.eclipse.equinox.bidi.advanced.STextEnvironment;
import org.eclipse.equinox.bidi.custom.*;

/**
 *  Handler for regular expressions.
 *  Such expressions may span multiple lines.
 *  <p>
 *  In applications like an editor where parts of the text might be modified
 *  while other parts are not, the user may want to call
 *  {@link ISTextExpert#leanToFullText}
 *  separately on each line and save the initial state of each line (this is
 *  the final state of the previous line which can be retrieved using
 *  {@link ISTextExpert#getState()}.
 *  If both the content
 *  of a line and its initial state have not changed, the user can be sure that
 *  the last <i>full</i> text computed for this line has not changed either.
 *
 *  @see ISTextExpert explanation of state
 *
 *  @author Matitiahu Allouche
 */
public class STextRegex extends STextTypeHandler {
	static final String[] startStrings = {"", /*  0 *//* dummy *///$NON-NLS-1$
			"(?#", /*  1 *//* comment (?#...) *///$NON-NLS-1$
			"(?<", /*  2 *//* named group (?<name> *///$NON-NLS-1$
			"(?'", /*  3 *//* named group (?'name' *///$NON-NLS-1$
			"(?(<", /*  4 *//* conditional named back reference (?(<name>) *///$NON-NLS-1$
			"(?('", /*  5 *//* conditional named back reference (?('name') *///$NON-NLS-1$
			"(?(", /*  6 *//* conditional named back reference (?(name) *///$NON-NLS-1$
			"(?&", /*  7 *//* named parentheses reference (?&name) *///$NON-NLS-1$
			"(?P<", /*  8 *//* named group (?P<name> *///$NON-NLS-1$
			"\\k<", /*  9 *//* named back reference \k<name> *///$NON-NLS-1$
			"\\k'", /* 10 *//* named back reference \k'name' *///$NON-NLS-1$
			"\\k{", /* 11 *//* named back reference \k{name} *///$NON-NLS-1$
			"(?P=", /* 12 *//* named back reference (?P=name) *///$NON-NLS-1$
			"\\g{", /* 13 *//* named back reference \g{name} *///$NON-NLS-1$
			"\\g<", /* 14 *//* subroutine call \g<name> *///$NON-NLS-1$
			"\\g'", /* 15 *//* subroutine call \g'name' *///$NON-NLS-1$
			"(?(R&", /* 16 *//* named back reference recursion (?(R&name) *///$NON-NLS-1$
			"\\Q" /* 17 *//* quoted sequence \Q...\E *///$NON-NLS-1$
	};
	static final char[] endChars = {
			// 0    1    2    3     4    5    6    7    8    9    10   11   12   13   14    15   16
			'.', ')', '>', '\'', ')', ')', ')', ')', '>', '>', '\'', '}', ')', '}', '>', '\'', ')'};
	static final int numberOfStrings = startStrings.length; /* 18 */
	static final int maxSpecial = numberOfStrings;
	static final byte L = Character.DIRECTIONALITY_LEFT_TO_RIGHT;
	static final byte R = Character.DIRECTIONALITY_RIGHT_TO_LEFT;
	static final byte AL = Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
	static final byte AN = Character.DIRECTIONALITY_ARABIC_NUMBER;
	static final byte EN = Character.DIRECTIONALITY_EUROPEAN_NUMBER;
	private static final Integer STATE_COMMENT = new Integer(1);
	private static final Integer STATE_QUOTED_SEQUENCE = new Integer(17);

	/**
	 *  Retrieves the number of special cases handled by this handler.
	 *  
	 *  @return the number of special cases for this handler.
	 */
	public int getSpecialsCount(ISTextExpert expert) {
		return maxSpecial;
	}

	/**
	 *  Locates occurrences of the syntactic strings and of
	 *  R, AL, EN, AN characters.
	 */
	public int indexOfSpecial(ISTextExpert expert, String text, STextCharTypes charTypes, STextOffsets offsets, int caseNumber, int fromIndex) {
		// In this method, L, R, AL, AN and EN represent bidi categories
		// as defined in the Unicode Bidirectional Algorithm
		// ( http://www.unicode.org/reports/tr9/ ).
		// L  represents the category Left to Right character.
		// R  represents the category Right to Left character.
		// AL represents the category Arabic Letter.
		// AN represents the category Arabic Number.
		// EN  represents the category European Number.
		byte charType;

		if (caseNumber < numberOfStrings) {
			/*  1 *//* comment (?#...) */
			/*  2 *//* named group (?<name> */
			/*  3 *//* named group (?'name' */
			/*  4 *//* conditional named back reference (?(name) */
			/*  5 *//* conditional named back reference (?(<name>) */
			/*  6 *//* conditional named back reference (?('name') */
			/*  7 *//* named parentheses reference (?&name) */
			/*  8 *//* named group (?P<name> */
			/*  9 *//* named back reference \k<name> */
			/* 10 *//* named back reference \k'name' */
			/* 11 *//* named back reference \k{name} */
			/* 12 *//* named back reference (?P=name) */
			/* 13 *//* named back reference \g{name} */
			/* 14 *//* subroutine call \g<name> */
			/* 15 *//* subroutine call \g'name' */
			/* 16 *//* named back reference recursion (?(R&name) */
			/* 17 *//* quoted sequence \Q...\E */
			return text.indexOf(startStrings[caseNumber], fromIndex);
		}
		// there never is a need for a mark before the first char
		if (fromIndex <= 0)
			fromIndex = 1;
		// look for R, AL, AN, EN which are potentially needing a mark
		for (; fromIndex < text.length(); fromIndex++) {
			charType = charTypes.getBidiTypeAt(fromIndex);
			// R and AL will always be examined using processSeparator()
			if (charType == R || charType == AL)
				return fromIndex;

			if (charType == EN || charType == AN) {
				// no need for a mark after the first digit in a number
				if (charTypes.getBidiTypeAt(fromIndex - 1) == charType)
					continue;

				for (int i = fromIndex - 1; i >= 0; i--) {
					charType = charTypes.getBidiTypeAt(i);
					// after a L char, no need for a mark
					if (charType == L)
						continue;

					// digit after R or AL or AN need a mark, except for EN
					//   following AN, but this is a contrived case, so we
					//   don't check for it (and calling processSeparator()
					//   for it will do no harm)
					if (charType == R || charType == AL || charType == AN)
						return fromIndex;
				}
				continue;
			}
		}
		return -1;
	}

	/**
	 *  Processes the special cases.
	 */
	public int processSpecial(ISTextExpert expert, String text, STextCharTypes charTypes, STextOffsets offsets, int caseNumber, int separLocation) {
		int location;

		if (separLocation < 0) {
			caseNumber = ((Integer) expert.getState()).intValue(); // TBD guard against "undefined"
			expert.clearState();
		}
		switch (caseNumber) {
			case 1 : /* comment (?#...) */
				if (separLocation < 0) {
					// initial state from previous line
					location = 0;
				} else {
					STextTypeHandler.processSeparator(text, charTypes, offsets, separLocation);
					// skip the opening "(?#"
					location = separLocation + 3;
				}
				location = text.indexOf(')', location);
				if (location < 0) {
					expert.setState(STATE_COMMENT);
					return text.length();
				}
				return location + 1;
			case 2 : /* named group (?<name> */
			case 3 : /* named group (?'name' */
			case 4 : /* conditional named back reference (?(name) */
			case 5 : /* conditional named back reference (?(<name>) */
			case 6 : /* conditional named back reference (?('name') */
			case 7 : /* named parentheses reference (?&name) */
				STextTypeHandler.processSeparator(text, charTypes, offsets, separLocation);
				// no need for calling processSeparator() for the following cases
				//   since the starting string contains a L char
			case 8 : /* named group (?P<name> */
			case 9 : /* named back reference \k<name> */
			case 10 : /* named back reference \k'name' */
			case 11 : /* named back reference \k{name} */
			case 12 : /* named back reference (?P=name) */
			case 13 : /* named back reference \g{name} */
			case 14 : /* subroutine call \g<name> */
			case 15 : /* subroutine call \g'name' */
			case 16 : /* named back reference recursion (?(R&name) */
				// skip the opening string
				location = separLocation + startStrings[caseNumber].length();
				// look for ending character
				location = text.indexOf(endChars[caseNumber], location);
				if (location < 0)
					return text.length();
				return location + 1;
			case 17 : /* quoted sequence \Q...\E */
				if (separLocation < 0) {
					// initial state from previous line
					location = 0;
				} else {
					STextTypeHandler.processSeparator(text, charTypes, offsets, separLocation);
					// skip the opening "\Q"
					location = separLocation + 2;
				}
				location = text.indexOf("\\E", location); //$NON-NLS-1$
				if (location < 0) {
					expert.setState(STATE_QUOTED_SEQUENCE);
					return text.length();
				}
				// set the charType for the "E" to L (Left to Right character)
				charTypes.setBidiTypeAt(location + 1, L);
				return location + 2;
			case 18 : /* R, AL, AN, EN */
				STextTypeHandler.processSeparator(text, charTypes, offsets, separLocation);
				return separLocation + 1;

		}
		// we should never get here
		return text.length();
	}

	public int getDirection(ISTextExpert expert, String text) {
		return getDirection(expert, text, new STextCharTypes(expert, text));
	}

	/**
	 *  @return {@link ISTextExpert#DIR_RTL DIR_RTL} if the following
	 *          conditions are satisfied:
	 *          <ul>
	 *            <li>The current locale (as expressed by the environment
	 *                language) is Arabic.</li>
	 *            <li>The first strong character has an RTL direction.</li>
	 *            <li>If there is no strong character in the text, the
	 *                GUI is mirrored.
	 *          </ul>
	 *          Otherwise, returns {@link ISTextExpert#DIR_LTR DIR_LTR}.
	 */
	public int getDirection(ISTextExpert expert, String text, STextCharTypes charTypes) {
		STextEnvironment environment = expert.getEnvironment();
		String language = environment.getLanguage();
		if (!language.equals("ar")) //$NON-NLS-1$
			return ISTextExpert.DIR_LTR;
		for (int i = 0; i < text.length(); i++) {
			byte charType = charTypes.getBidiTypeAt(i);
			if (charType == AL || charType == R)
				return ISTextExpert.DIR_RTL;
			if (charType == L)
				return ISTextExpert.DIR_LTR;
		}
		if (environment.getMirrored())
			return ISTextExpert.DIR_RTL;
		return ISTextExpert.DIR_LTR;
	}

}
