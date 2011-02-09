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
 *  <code>BidiComplexSql</code> is a processor for complex expressions
 *  composed of SQL statements. Such a complex expression may span
 *  multiple lines.
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
public class BidiComplexSql extends BidiComplexProcessor {
	private static final byte WS = Character.DIRECTIONALITY_WHITESPACE;
	static final String operators = "\t!#%&()*+,-./:;<=>?|[]{}"; //$NON-NLS-1$
	static final BidiComplexFeatures FEATURES = new BidiComplexFeatures(operators, 5, -1, -1, false, false);
	static final String lineSep = BidiComplexEnvironment.getLineSep();

	/**
	 *  This method retrieves the features specific to this processor.
	 *
	 *  @return features with operators "\t!#%&()*+,-./:;<=>?|[]{}", 5 special cases,
	 *          LTR direction for Arabic and Hebrew, and support for both.
	 */
	public BidiComplexFeatures init(BidiComplexHelper helper, BidiComplexEnvironment env) {
		return FEATURES;
	}

	/**
	  *  This method looks for occurrences of 5 special strings:
	  *  <ol>
	  *    <li>spaces</li>
	  *    <li>literals starting with apostrophe</li>
	  *    <li>identifiers starting with quotation mark</li>
	  *    <li>comments starting with slash-asterisk</li>
	  *    <li>comments starting with hyphen-hyphen</li>
	  *  </ol>
	  */
	public int indexOfSpecial(BidiComplexHelper helper, int caseNumber, String srcText, int fromIndex) {
		switch (caseNumber) {
			case 0 : /* space */
				return srcText.indexOf(" ", fromIndex); //$NON-NLS-1$
			case 1 : /* literal */
				return srcText.indexOf('\'', fromIndex);
			case 2 : /* delimited identifier */
				return srcText.indexOf('"', fromIndex);
			case 3 : /* slash-aster comment */
				return srcText.indexOf("/*", fromIndex); //$NON-NLS-1$
			case 4 : /* hyphen-hyphen comment */
				return srcText.indexOf("--", fromIndex); //$NON-NLS-1$
		}
		// we should never get here
		return -1;
	}

	/**
	 *  This method processes the 5 special cases as follows.
	     *  <ol>
	     *    <li>skip the run of spaces</li>
	     *    <li>look for a matching apostrophe and skip until after it</li>
	     *    <li>look for a matching quotation mark and skip until after it</li>
	     *    <li>skip until after the closing asterisk-slash</li>
	     *    <li>skip until after a line separator</li>
	     *  </ol>
	 */
	public int processSpecial(BidiComplexHelper helper, int caseNumber, String srcText, int operLocation) {
		int location;

		helper.processOperator(operLocation);
		switch (caseNumber) {
			case 0 : /* space */
				operLocation++;
				while (operLocation < srcText.length() && srcText.charAt(operLocation) == ' ') {
					helper.setDirProp(operLocation, WS);
					operLocation++;
				}
				return operLocation;
			case 1 : /* literal */
				location = operLocation + 1;
				while (true) {
					location = srcText.indexOf('\'', location);
					if (location < 0) {
						helper.setFinalState(caseNumber);
						return srcText.length();
					}
					if ((location + 1) < srcText.length() && srcText.charAt(location + 1) == '\'') {
						location += 2;
						continue;
					}
					return location + 1;
				}
			case 2 : /* delimited identifier */
				location = operLocation + 1;
				while (true) {
					location = srcText.indexOf('"', location);
					if (location < 0)
						return srcText.length();

					if ((location + 1) < srcText.length() && srcText.charAt(location + 1) == '"') {
						location += 2;
						continue;
					}
					return location + 1;
				}
			case 3 : /* slash-aster comment */
				if (operLocation < 0)
					location = 0; // initial state from previous line
				else
					location = operLocation + 2; // skip the opening slash-aster
				location = srcText.indexOf("*/", location); //$NON-NLS-1$
				if (location < 0) {
					helper.setFinalState(caseNumber);
					return srcText.length();
				}
				// we need to call processOperator since text may follow the
				//  end of comment immediately without even a space
				helper.processOperator(location);
				return location + 2;
			case 4 : /* hyphen-hyphen comment */
				location = srcText.indexOf(lineSep, operLocation + 2);
				if (location < 0)
					return srcText.length();
				return location + lineSep.length();
		}
		// we should never get here
		return srcText.length();
	}
}
