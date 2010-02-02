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
package org.eclipse.equinox.bidi.complexp;

/**
 *  <code>ComplExpSql</code> is a processor for complex expressions
 *  composed of SQL statements. Such a complex expression may span
 *  multiple lines.
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
public class ComplExpSql extends ComplExpBasic {
	private static final byte WS = Character.DIRECTIONALITY_WHITESPACE;
	static final String operators = "\t!#%&()*+,-./:;<=>?|[]{}";
	static String lineSep;

	/**
	 *  Constructor for a complex expressions processor with support for
	 *  SQL statements.
	 */
	public ComplExpSql() {
		super(operators, 5);
		// TBD use bundle properties
		if (lineSep == null)
			lineSep = System.getProperty("line.separator", "\n");
	}

	/**
	 *  This method is not supposed to be invoked directly by users of this
	 *  class. It may  be overridden by subclasses of this class.
	 */
	protected int indexOfSpecial(int whichSpecial, String leanText, int fromIndex) {
		switch (whichSpecial) {
			case 0 : /* space */
				return leanText.indexOf(" ", fromIndex);
			case 1 : /* literal */
				return leanText.indexOf('\'', fromIndex);
			case 2 : /* delimited identifier */
				return leanText.indexOf('"', fromIndex);
			case 3 : /* slash-aster comment */
				return leanText.indexOf("/*", fromIndex);
			case 4 : /* hyphen-hyphen comment */
				return leanText.indexOf("--", fromIndex);
		}
		// we should never get here
		return -1;
	}

	/**
	 *  This method is not supposed to be invoked directly by users of this
	 *  class. It may  be overridden by subclasses of this class.
	 */
	protected int processSpecial(int whichSpecial, String leanText, int operLocation) {
		int loc;

		processOperator(operLocation);
		switch (whichSpecial) {
			case 0 : /* space */
				operLocation++;
				while (operLocation < leanText.length() && leanText.charAt(operLocation) == ' ') {
					setDirProp(operLocation, WS);
					operLocation++;
				}
				return operLocation;
			case 1 : /* literal */
				loc = operLocation + 1;
				while (true) {
					loc = leanText.indexOf('\'', loc);
					if (loc < 0) {
						state = whichSpecial;
						return leanText.length();
					}
					if ((loc + 1) < leanText.length() && leanText.charAt(loc + 1) == '\'') {
						loc += 2;
						continue;
					}
					return loc + 1;
				}
			case 2 : /* delimited identifier */
				loc = operLocation + 1;
				while (true) {
					loc = leanText.indexOf('"', loc);
					if (loc < 0)
						return leanText.length();

					if ((loc + 1) < leanText.length() && leanText.charAt(loc + 1) == '"') {
						loc += 2;
						continue;
					}
					return loc + 1;
				}
			case 3 : /* slash-aster comment */
				if (operLocation < 0)
					loc = 0; // initial state from previous line
				else
					loc = operLocation + 2; // skip the opening slash-aster
				loc = leanText.indexOf("*/", loc);
				if (loc < 0) {
					state = whichSpecial;
					return leanText.length();
				}
				// we need to call processOperator since text may follow the
				//  end of comment immediately without even a space
				processOperator(loc);
				return loc + 2;
			case 4 : /* hyphen-hyphen comment */
				loc = leanText.indexOf(lineSep, operLocation + 2);
				if (loc < 0)
					return leanText.length();
				return loc + lineSep.length();
		}
		// we should never get here
		return leanText.length();
	}
}
