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
 *  <code>ComplExpSingle</code> is a processor for complex expressions
 *  composed of two parts separated by an operator.
 *  The first occurrence of the operator delimits the end of the first part
 *  and the start of the second part. Further occurrences of the operator,
 *  if any, are treated like regular characters of the second text part.
 *  The processor makes sure that the expression be presented in the form
 *  (assuming that the equal sign is the operator):
 *  <pre>
 *  part1=part2
 *  </pre>
 *
 *  @see IComplExpProcessor
 *  @see ComplExpBasic
 *
 *  @author Matitiahu Allouche
 */
public class ComplExpSingle extends ComplExpBasic {
	char separator;

	/**
	 *  Constructor for a complex expressions processor with support for one
	 *  operator.
	 *
	 *  @param operators string including at least one character. The
	 *         first character of the string is the operator which divides
	 *         the expression into 2 parts.
	 *
	 */
	public ComplExpSingle(String operators) {
		super(operators, 1);
		separator = operators.charAt(0);
	}

	/**
	 *  This method is not supposed to be invoked directly by users of this
	 *  class. It may  be overridden by subclasses of this class.
	 */
	protected int indexOfSpecial(int whichSpecial, String leanText, int fromIndex) {
		return leanText.indexOf(separator, fromIndex);
	}

	/**
	 *  This method is not supposed to be invoked directly by users of this
	 *  class. It may  be overridden by subclasses of this class.
	 */
	protected int processSpecial(int whichSpecial, String leanText, int operLocation) {
		processOperator(operLocation);
		return leanText.length();
	}
}
