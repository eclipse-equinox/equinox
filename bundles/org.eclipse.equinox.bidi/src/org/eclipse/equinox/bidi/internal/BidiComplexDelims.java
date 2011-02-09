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

import org.eclipse.equinox.bidi.BidiComplexHelper;
import org.eclipse.equinox.bidi.custom.BidiComplexProcessor;

/**
 *  <code>BidiComplexDelims</code> is a processor for complex expressions
 *  composed of text segments separated by operators where the text segments
 *  may include delimited parts within which operators are treated like
 *  regular characters.
 *
 *  @author Matitiahu Allouche
 */
public abstract class BidiComplexDelims extends BidiComplexProcessor {

	/**
	 *  This method locates occurrences of start delimiters.
	 */
	public int indexOfSpecial(BidiComplexHelper helper, int caseNumber, String srcText, int fromIndex) {
		char delim = getDelimiters().charAt(caseNumber * 2);
		return srcText.indexOf(delim, fromIndex);
	}

	/**
	 *  This method skips until after the matching end delimiter.
	 */
	public int processSpecial(BidiComplexHelper helper, int caseNumber, String srcText, int operLocation) {
		helper.processOperator(operLocation);
		int loc = operLocation + 1;
		char delim = getDelimiters().charAt((caseNumber * 2) + 1);
		loc = srcText.indexOf(delim, loc);
		if (loc < 0)
			return srcText.length();
		return loc + 1;
	}

	/**
	 *  @return a string containing the delimiters implemented in this class
	 *         instance. This string must include an even
	 *         number of characters. The first 2 characters of a string
	 *         constitute a pair, the next 2 characters are a second pair, etc...
	 *         In each pair, the first character is a start delimiter and
	 *         the second character is an end delimiter. In the <i>lean</i>
	 *         text, any part starting with a start delimiter and ending with
	 *         the corresponding end delimiter is a delimited part. Within a
	 *         delimited part, operators are treated like regular characters,
	 *         which means that they do not define new segments.
	 */
	protected abstract String getDelimiters();

}
