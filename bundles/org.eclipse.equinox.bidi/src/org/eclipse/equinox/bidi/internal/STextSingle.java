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
 *  A base handler for structured text composed of two parts separated by a separator.
 *  The first occurrence of the separator delimits the end of the first part
 *  and the start of the second part. Further occurrences of the separator,
 *  if any, are treated like regular characters of the second text part.
 *  The handler makes sure that the text be presented in the form
 *  (assuming that the equal sign is the separator):
 *  <pre>
 *  part1=part2
 *  </pre>
 *  The string returned by {@link STextTypeHandler#getSeparators getSeparators}
 *  for this handler should contain exactly one character.
 *  Additional characters will be ignored.
 *
 *  @author Matitiahu Allouche
 */
public class STextSingle extends STextTypeHandler {

	public STextSingle(String separator) {
		super(separator);
	}

	/**
	 *  Locates occurrences of the separator.
	 *
	 *  @see #getSeparators getSeparators
	 */
	public int indexOfSpecial(ISTextExpert expert, String text, STextCharTypes charTypes, STextOffsets offsets, int caseNumber, int fromIndex) {
		return text.indexOf(this.getSeparators(expert).charAt(0), fromIndex);
	}

	/**
	 *  Inserts a mark before the separator if needed and
	 *  skips to the end of the source string.
	 *
	 *  @return the length of <code>text</code>.
	 */
	public int processSpecial(ISTextExpert expert, String text, STextCharTypes charTypes, STextOffsets offsets, int caseNumber, int separLocation) {
		STextTypeHandler.processSeparator(text, charTypes, offsets, separLocation);
		return text.length();
	}

	/**
	 *  Returns 1 as number of special cases handled by this handler.
	 *
	 *  @return 1.
	 */
	public int getSpecialsCount(ISTextExpert expert) {
		return 1;
	}

}
