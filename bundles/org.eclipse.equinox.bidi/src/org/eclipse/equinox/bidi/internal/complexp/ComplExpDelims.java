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
package org.eclipse.equinox.bidi.internal.complexp;

import org.eclipse.equinox.bidi.complexp.IComplExpProcessor;

/**
 *  <code>ComplExpDelims</code> is a processor for complex expressions
 *  composed of text segments separated by operators where the text segments
 *  may include delimited parts within which operators are treated like
 *  regular characters.
 *
 *  @see IComplExpProcessor
 *  @see ComplExpBasic
 *
 *  @author Matitiahu Allouche
 */
public class ComplExpDelims extends ComplExpBasic {
	char[] delims;

	/**
	 *  Constructor for a complex expressions processor with support for
	 *  operators and delimiters.
	 *
	 *  @param operators string grouping one-character operators which
	 *         separate the text of the complex expression into segments.
	 *
	 *  @param delims delimiters implemented in this class instance.
	 *         This parameter is a string which must include an even
	 *         number of characters. The first 2 characters of a string
	 *         constitute a pair, the next 2 characters are a second pair, etc...
	 *         In each pair, the first character is a start delimiter and
	 *         the second character is an end delimiter. In the <i>lean</i>
	 *         text, any part starting with a start delimiter and ending with
	 *         the corresponding end delimiter is a delimited part. Within a
	 *         delimited part, operators are treated like regular characters,
	 *         which means that they do not define new segments.
	 */
	public ComplExpDelims(String operators, String delims) {
		super(operators, delims.length() / 2);
		this.delims = delims.toCharArray();
	}

	/**
	 *  This method is not supposed to be invoked directly by users of this
	 *  class. It may  be overridden by subclasses of this class.
	 */
	protected int indexOfSpecial(int whichSpecial, String srcText, int fromIndex) {
		char delim = delims[whichSpecial * 2];

		return srcText.indexOf(delim, fromIndex);
	}

	/**
	 *  This method is not supposed to be invoked directly by users of this
	 *  class. It may  be overridden by subclasses of this class.
	 */
	protected int processSpecial(int whichSpecial, String srcText, int operLocation) {
		processOperator(operLocation);
		int loc = operLocation + 1;
		char delim = delims[(whichSpecial * 2) + 1];
		loc = srcText.indexOf(delim, loc);
		if (loc < 0)
			return srcText.length();
		return loc + 1;
	}
}
