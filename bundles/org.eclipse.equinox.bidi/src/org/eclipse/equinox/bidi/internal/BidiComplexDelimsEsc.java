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

/**
 *  <code>BidiComplexDelims</code> is a processor for complex expressions
 *  composed of text segments separated by operators where the text segments
 *  may include delimited parts within which operators are treated like
 *  regular characters and the delimiters may be escaped.
 *  This is similar to {@link BidiComplexDelims} except
 *  that delimiters can be escaped using the backslash character.
 *  <ul>
 *    <li>Two consecutive backslashes in a delimited part are treated like
 *        one regular characters.</li>
 *    <li>An ending delimiter preceded by an odd number of backslashes is
 *        treated like a regular character of a delimited part.</li>
 *  </ul>
 *
 *  @author Matitiahu Allouche
 */
public abstract class BidiComplexDelimsEsc extends BidiComplexDelims {

	/**
	*  This method skips until after the matching end delimiter,
	*  ignoring possibly escaped end delimiters.
	*/
	public int processSpecial(BidiComplexHelper helper, int caseNumber, String srcText, int operLocation) {
		helper.processOperator(operLocation);
		int location = operLocation + 1;
		char delim = getDelimiters().charAt((caseNumber * 2) + 1);
		while (true) {
			location = srcText.indexOf(delim, location);
			if (location < 0)
				return srcText.length();
			int cnt = 0;
			for (int i = location - 1; srcText.charAt(i) == '\\'; i--) {
				cnt++;
			}
			location++;
			if ((cnt & 1) == 0)
				return location;
		}
	}

}
