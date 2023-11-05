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
 * A base handler for structured text composed of text segments separated by
 * separators where the text segments may include delimited parts within which
 * separators are treated like regular characters.
 * <p>
 * A delimited part is bounded by a start delimiter and an end delimiter.
 * </p>
 */
public abstract class StructuredTextDelims extends StructuredTextTypeHandler {

	public StructuredTextDelims() {
		// placeholder
	}

	public StructuredTextDelims(String separators) {
		super(separators);
	}

	/**
	 * Locates occurrences of start delimiters.
	 *
	 * @return the position starting from offset <code>fromIndex</code> in
	 *         <code>text</code> of the first occurrence of the start delimiter
	 *         corresponding to <code>caseNumber</code> (first start delimiter if
	 *         <code>caseNumber</code> equals 1, second delimiter if
	 *         <code>caseNumber</code> equals 2, etc...).
	 *
	 * @see #getDelimiters
	 */
	@Override
	public int indexOfSpecial(IStructuredTextExpert expert, String text, StructuredTextCharTypes charTypes,
			StructuredTextOffsets offsets, int caseNumber, int fromIndex) {
		char delim = getDelimiters().charAt((caseNumber - 1) * 2);
		return text.indexOf(delim, fromIndex);
	}

	/**
	 * Handles the text between start and end delimiters as a token. This method
	 * inserts a directional mark if needed at position <code>separLocation</code>
	 * which corresponds to a start delimiter, and skips until after the matching
	 * end delimiter.
	 *
	 * @return the position after the matching end delimiter, or the length of
	 *         <code>text</code> if no end delimiter is found.
	 */
	@Override
	public int processSpecial(IStructuredTextExpert expert, String text, StructuredTextCharTypes charTypes,
			StructuredTextOffsets offsets, int caseNumber, int separLocation) {
		StructuredTextTypeHandler.processSeparator(text, charTypes, offsets, separLocation);
		int loc = separLocation + 1;
		char delim = getDelimiters().charAt((caseNumber * 2) - 1);
		loc = text.indexOf(delim, loc);
		if (loc < 0)
			return text.length();
		return loc + 1;
	}

	/**
	 * @return a string containing the delimiters implemented in this class
	 *         instance. This string must include an even number of characters. The
	 *         first 2 characters of a string constitute a pair, the next 2
	 *         characters are a second pair, etc... In each pair, the first
	 *         character is a start delimiter and the second character is an end
	 *         delimiter. In the <i>lean</i> text, any part starting with a start
	 *         delimiter and ending with the corresponding end delimiter is a
	 *         delimited part. Within a delimited part, separators are treated like
	 *         regular characters, which means that they do not define new segments.
	 */
	protected abstract String getDelimiters();

}
