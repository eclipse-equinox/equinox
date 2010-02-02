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
 *  <code>ComplExpDelims</code> is a processor for complex expressions
 *  composed of text segments separated by operators where the text segments
 *  may include delimited parts within which operators are treated like
 *  regular characters and the delimiters may be escaped.
 *  This is similar to {@link ComplExpDelims} except
 *  that delimiters can be escaped using the backslash character.
 *  <ul>
 *    <li>Two consecutive backslashes in a delimited part are treated like
 *        regular characters.</li>
 *    <li>An ending delimiter preceded by an odd number of backslashes is
 *        treated like a regular character of a delimited part.</li>
 *  </ul>
 *
 *  @see IComplExpProcessor
 *  @see ComplExpBasic
 *
 *  @author Matitiahu Allouche
 */
public class ComplExpDelimsEsc extends ComplExpDelims {
	/**
	 *  Constructor for a complex expressions processor with support for
	 *  operators and delimiters which can be escaped.
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
	 *         which means that they do not define new segments.<br>
	 *         &nbsp;<br>
	 *         Note however that an ending delimiter preceded by an odd
	 *         number of backslashes is considered as a regular character
	 *         and does not mark the termination of a delimited part.
	 */
	public ComplExpDelimsEsc(String operators, String delims) {
		super(operators, delims);
	}

	/**
	 *  This method is not supposed to be invoked directly by users of this
	 *  class. It may  be overridden by subclasses of this class.
	 */
	protected int processSpecial(int whichSpecial, String leanText, int operLocation) {
		processOperator(operLocation);
		int loc = operLocation + 1;
		char delim = delims[(whichSpecial * 2) + 1];
		while (true) {
			loc = leanText.indexOf(delim, loc);
			if (loc < 0)
				return leanText.length();
			int cnt = 0;
			for (int i = loc - 1; leanText.charAt(i) == '\\'; i--) {
				cnt++;
			}
			loc++;
			if ((cnt & 1) == 0)
				return loc;
		}
	}
}
