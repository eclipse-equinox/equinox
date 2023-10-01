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
package org.eclipse.equinox.bidi.internal;

import org.eclipse.equinox.bidi.advanced.IStructuredTextExpert;
import org.eclipse.equinox.bidi.custom.*;

/**
 * A base handler for structured text composed of two parts separated by a
 * separator. The first occurrence of the separator delimits the end of the
 * first part and the start of the second part. Further occurrences of the
 * separator, if any, are treated like regular characters of the second text
 * part. The handler makes sure that the text be presented in the form (assuming
 * that the equal sign is the separator):
 * 
 * <pre>
 * part1 = part2
 * </pre>
 * 
 * The string returned by {@link StructuredTextTypeHandler#getSeparators
 * getSeparators} for this handler should contain exactly one character.
 * Additional characters will be ignored.
 */
public class StructuredTextSingle extends StructuredTextTypeHandler {

	public StructuredTextSingle(String separator) {
		super(separator);
	}

	/**
	 * Locates occurrences of the separator.
	 *
	 * @see #getSeparators getSeparators
	 */
	@Override
	public int indexOfSpecial(IStructuredTextExpert expert, String text, StructuredTextCharTypes charTypes,
			StructuredTextOffsets offsets, int caseNumber, int fromIndex) {
		return text.indexOf(this.getSeparators(expert).charAt(0), fromIndex);
	}

	/**
	 * Inserts a mark before the separator if needed and skips to the end of the
	 * source string.
	 *
	 * @return the length of <code>text</code>.
	 */
	@Override
	public int processSpecial(IStructuredTextExpert expert, String text, StructuredTextCharTypes charTypes,
			StructuredTextOffsets offsets, int caseNumber, int separLocation) {
		StructuredTextTypeHandler.processSeparator(text, charTypes, offsets, separLocation);
		return text.length();
	}

	/**
	 * Returns 1 as number of special cases handled by this handler.
	 *
	 * @return 1.
	 */
	@Override
	public int getSpecialsCount(IStructuredTextExpert expert) {
		return 1;
	}

}
