/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.bidi.internal.consumable;

import org.eclipse.equinox.bidi.advanced.IStructuredTextExpert;
import org.eclipse.equinox.bidi.custom.*;
import org.eclipse.equinox.bidi.internal.StructuredTextActivator;

/**
 * Handler for structured text composed of SQL statements. Such a structured
 * text may span multiple lines.
 * <p>
 * In applications like an editor where parts of the text might be modified
 * while other parts are not, the user may want to call
 * {@link IStructuredTextExpert#leanToFullText} separately on each line and save
 * the initial state of each line (this is the final state of the previous line
 * which can be retrieved by calling {@link IStructuredTextExpert#getState()}.
 * If both the content of a line and its initial state have not changed, the
 * user can be sure that the last <i>full</i> text computed for this line has
 * not changed either.
 *
 * @see IStructuredTextExpert explanation of state
 */
public class StructuredTextSql extends StructuredTextTypeHandler {
	private static final byte WS = Character.DIRECTIONALITY_WHITESPACE;
	static final String lineSep = StructuredTextActivator.getProperty("line.separator"); //$NON-NLS-1$
	private static final Integer STATE_LITERAL = Integer.valueOf(2);
	private static final Integer STATE_SLASH_ASTER_COMMENT = Integer.valueOf(4);

	public StructuredTextSql() {
		super("\t!#%&()*+,-./:;<=>?|[]{}"); //$NON-NLS-1$
	}

	/**
	 * @return 5 as the number of special cases handled by this handler.
	 */
	@Override
	public int getSpecialsCount(IStructuredTextExpert expert) {
		return 5;
	}

	/**
	 * Locates occurrences of 5 special strings:
	 * <ol>
	 * <li>spaces</li>
	 * <li>literals starting with apostrophe</li>
	 * <li>identifiers starting with quotation mark</li>
	 * <li>comments starting with slash-asterisk</li>
	 * <li>comments starting with hyphen-hyphen</li>
	 * </ol>
	 */
	@Override
	public int indexOfSpecial(IStructuredTextExpert expert, String text, StructuredTextCharTypes charTypes,
			StructuredTextOffsets offsets, int caseNumber, int fromIndex) {
		switch (caseNumber) {
		case 1: /* space */
			return text.indexOf(" ", fromIndex); //$NON-NLS-1$
		case 2: /* literal */
			return text.indexOf('\'', fromIndex);
		case 3: /* delimited identifier */
			return text.indexOf('"', fromIndex);
		case 4: /* slash-aster comment */
			return text.indexOf("/*", fromIndex); //$NON-NLS-1$
		case 5: /* hyphen-hyphen comment */
			return text.indexOf("--", fromIndex); //$NON-NLS-1$
		}
		// we should never get here
		return -1;
	}

	/**
	 * Processes the 5 special cases as follows.
	 * <ol>
	 * <li>skip the run of spaces</li>
	 * <li>look for a matching apostrophe and skip until after it</li>
	 * <li>look for a matching quotation mark and skip until after it</li>
	 * <li>skip until after the closing asterisk-slash</li>
	 * <li>skip until after a line separator</li>
	 * </ol>
	 */
	@Override
	public int processSpecial(IStructuredTextExpert expert, String text, StructuredTextCharTypes charTypes,
			StructuredTextOffsets offsets, int caseNumber, int separLocation) {
		int location;

		StructuredTextTypeHandler.processSeparator(text, charTypes, offsets, separLocation);
		if (separLocation < 0) {
			caseNumber = ((Integer) expert.getState()).intValue(); // TBD guard against "undefined"
			expert.clearState();
		}
		switch (caseNumber) {
		case 1: /* space */
			separLocation++;
			while (separLocation < text.length() && text.charAt(separLocation) == ' ') {
				charTypes.setBidiTypeAt(separLocation, WS);
				separLocation++;
			}
			return separLocation;
		case 2: /* literal */
			location = separLocation + 1;
			while (true) {
				location = text.indexOf('\'', location);
				if (location < 0) {
					expert.setState(STATE_LITERAL);
					return text.length();
				}
				if ((location + 1) < text.length() && text.charAt(location + 1) == '\'') {
					location += 2;
					continue;
				}
				return location + 1;
			}
		case 3: /* delimited identifier */
			location = separLocation + 1;
			while (true) {
				location = text.indexOf('"', location);
				if (location < 0) {
					return text.length();
				}

				if ((location + 1) < text.length() && text.charAt(location + 1) == '"') {
					location += 2;
					continue;
				}
				return location + 1;
			}
		case 4: /* slash-aster comment */
			if (separLocation < 0) { // continuation line
				location = 0;
			} else { // continuation line
				location = separLocation + 2; // skip the opening slash-aster
			}
			location = text.indexOf("*/", location); //$NON-NLS-1$
			if (location < 0) {
				expert.setState(STATE_SLASH_ASTER_COMMENT);
				return text.length();
			}
			// we need to call processSeparator since text may follow the
			// end of comment immediately without even a space
			StructuredTextTypeHandler.processSeparator(text, charTypes, offsets, location);
			return location + 2;
		case 5: /* hyphen-hyphen comment */
			location = text.indexOf(lineSep, separLocation + 2);
			if (location < 0) {
				return text.length();
			}
			return location + lineSep.length();
		}
		// we should never get here
		return text.length();
	}

}
