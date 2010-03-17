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
package org.eclipse.equinox.bidi.internal.complexp.consumable;

import org.eclipse.equinox.bidi.complexp.IComplExpProcessor;
import org.eclipse.equinox.bidi.internal.complexp.ComplExpBasic;

/**
 *  <code>ComplExpRegex</code> is a processor for regular expressions.
 *  Such expressions may span multiple lines.
 *  <p>
 *  In applications like an editor where parts of the text might be modified
 *  while other parts are not, the user may want to call
 *  {@link IComplExpProcessor#leanToFullText leanToFullText}
 *  separately on each line and save the initial state of each line (this is
 *  the final state of the previous line which can be retrieved using
 *  {@link IComplExpProcessor#getFinalState getFinalState}. If both the content
 *  of a line and its initial state have not changed, the user can be sure that
 *  the last <i>full</i> text computed for this line has not changed either.
 *
 *  @see IComplExpProcessor
 *  @see ComplExpBasic#state
 *
 *  @author Matitiahu Allouche
 */
public class ComplExpRegex extends ComplExpBasic {
	static final String operators = ""; //$NON-NLS-1$
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

	/**
	 *  Constructor for a complex expressions processor with support for
	 *  regular expressions.
	 */
	public ComplExpRegex() {
		super(operators, maxSpecial);
	}

	/**
	 *  This method is not supposed to be invoked directly by users of this
	 *  class. It may  be overridden by subclasses of this class.
	 */
	protected int indexOfSpecial(int whichSpecial, String srcText, int fromIndex) {
		byte dirProp;

		if (whichSpecial < numberOfStrings) {
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
			return srcText.indexOf(startStrings[whichSpecial], fromIndex);
		}
		// look for R, AL, AN, EN which are potentially needing a mark
		for (; fromIndex < srcText.length(); fromIndex++) {
			// there never is a need for a mark before the first char
			if (fromIndex <= 0)
				continue;

			dirProp = getDirProp(fromIndex);
			// R and AL will always be examined using processOperator()
			if (dirProp == R || dirProp == AL)
				return fromIndex;

			if (dirProp == EN || dirProp == AN) {
				// no need for a mark after the first digit in a number
				if (getDirProp(fromIndex - 1) == dirProp)
					continue;

				for (int i = fromIndex - 1; i >= 0; i--) {
					dirProp = getDirProp(i);
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
	 *  This method is not supposed to be invoked directly by users of this
	 *  class. It may  be overridden by subclasses of this class.
	 */
	protected int processSpecial(int whichSpecial, String srcText, int operLocation) {
		int loc;

		switch (whichSpecial) {
			case 0 : /* comment (?#...) */
				if (operLocation < 0) {
					// initial state from previous line
					loc = 0;
				} else {
					processOperator(operLocation);
					// skip the opening "(?#"
					loc = operLocation + 3;
				}
				loc = srcText.indexOf(')', loc);
				if (loc < 0) {
					state = whichSpecial;
					return srcText.length();
				}
				return loc + 1;
			case 1 : /* named group (?<name> */
			case 2 : /* named group (?'name' */
			case 3 : /* conditional named back reference (?(name) */
			case 4 : /* conditional named back reference (?(<name>) */
			case 5 : /* conditional named back reference (?('name') */
			case 6 : /* named parentheses reference (?&name) */
				processOperator(operLocation);
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
				loc = operLocation + startStrings[whichSpecial].length();
				// look for ending character
				loc = srcText.indexOf(endChars[whichSpecial], loc);
				if (loc < 0)
					return srcText.length();
				return loc + 1;
			case 16 : /* quoted sequence \Q...\E */
				if (operLocation < 0) {
					// initial state from previous line
					loc = 0;
				} else {
					processOperator(operLocation);
					// skip the opening "\Q"
					loc = operLocation + 2;
				}
				loc = srcText.indexOf("\\E", loc); //$NON-NLS-1$
				if (loc < 0) {
					state = whichSpecial;
					return srcText.length();
				}
				// set the dirProp for the "E"
				setDirProp(loc + 1, L);
				return loc + 2;
			case 17 : /* R, AL, AN, EN */
				processOperator(operLocation);
				return operLocation + 1;

		}
		// we should never get here
		return srcText.length();
	}
}
