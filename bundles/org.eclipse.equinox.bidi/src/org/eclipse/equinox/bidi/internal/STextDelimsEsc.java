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

import org.eclipse.equinox.bidi.advanced.ISTextExpert;
import org.eclipse.equinox.bidi.custom.*;

/**
 *  A base handler for structured text composed of text segments separated 
 *  by separators where the text segments may include delimited parts within 
 *  which separators are treated like regular characters and the delimiters 
 *  may be escaped.
 *  <p>
 *  This is similar to {@link STextDelims} except that delimiters can be escaped 
 *  using the backslash character.
 *  <ul>
 *    <li>Two consecutive backslashes in a delimited part are treated like
 *        one regular character.</li>
 *    <li>An ending delimiter preceded by an odd number of backslashes is
 *        treated like a regular character within the delimited part.</li>
 *  </ul>
 *  </p>
 *
 *  @author Matitiahu Allouche
 */
public abstract class STextDelimsEsc extends STextDelims {

	public STextDelimsEsc() {
		// placeholder
	}

	public STextDelimsEsc(String separator) {
		super(separator);
	}

	/**
	 *  Handles the text between start and end delimiters as a token.
	 *  This method inserts a directional mark if needed at position
	 *  <code>separLocation</code> which corresponds to a start delimiter,
	 *  and skips until after the matching end delimiter,
	 *  ignoring possibly escaped end delimiters.
	 */
	public int processSpecial(ISTextExpert expert, String text, STextCharTypes charTypes, STextOffsets offsets, int caseNumber, int separLocation) {
		STextTypeHandler.processSeparator(text, charTypes, offsets, separLocation);
		int location = separLocation + 1;
		char delim = getDelimiters().charAt((caseNumber * 2) - 1);
		while (true) {
			location = text.indexOf(delim, location);
			if (location < 0)
				return text.length();
			int cnt = 0;
			for (int i = location - 1; text.charAt(i) == '\\'; i--) {
				cnt++;
			}
			location++;
			if ((cnt & 1) == 0)
				return location;
		}
	}

}
