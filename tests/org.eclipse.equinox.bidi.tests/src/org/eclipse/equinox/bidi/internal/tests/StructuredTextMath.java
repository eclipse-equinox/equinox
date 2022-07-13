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
package org.eclipse.equinox.bidi.internal.tests;

import org.eclipse.equinox.bidi.advanced.IStructuredTextExpert;
import org.eclipse.equinox.bidi.custom.StructuredTextCharTypes;
import org.eclipse.equinox.bidi.custom.StructuredTextTypeHandler;

/**
 *  Handler adapted to processing arithmetic expressions with
 *  a possible right-to-left base direction.
 */
public class StructuredTextMath extends StructuredTextTypeHandler {
	static final byte L = Character.DIRECTIONALITY_LEFT_TO_RIGHT;
	static final byte R = Character.DIRECTIONALITY_RIGHT_TO_LEFT;
	static final byte AL = Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
	static final byte AN = Character.DIRECTIONALITY_ARABIC_NUMBER;

	public StructuredTextMath() {
		super("+-/*()="); //$NON-NLS-1$
	}

	public int getDirection(IStructuredTextExpert expert, String text) {
		return getDirection(expert, text, new StructuredTextCharTypes(expert, text));
	}

	/**
	 *  @return {@link IStructuredTextExpert#DIR_RTL DIR_RTL} if the following
	 *          conditions are satisfied:
	 *          <ul>
	 *            <li>The current locale (as expressed by the environment
	 *                language) is Arabic.</li>
	 *            <li>The first strong character is an Arabic letter.</li>
	 *            <li>If there is no strong character in the text, there is
	 *                at least one Arabic-Indic digit in the text.</li>
	 *          </ul>
	 *          Otherwise, returns {@link IStructuredTextExpert#DIR_LTR DIR_LTR}.
	 */
	public int getDirection(IStructuredTextExpert expert, String text, StructuredTextCharTypes charTypes) {
		String language = expert.getEnvironment().getLanguage();
		if (!language.equals("ar")) //$NON-NLS-1$
			return IStructuredTextExpert.DIR_LTR;
		boolean flagAN = false;
		for (int i = 0; i < text.length(); i++) {
			byte charType = charTypes.getBidiTypeAt(i);
			if (charType == AL)
				return IStructuredTextExpert.DIR_RTL;
			if (charType == L || charType == R)
				return IStructuredTextExpert.DIR_LTR;
			if (charType == AN)
				flagAN = true;
		}
		if (flagAN)
			return IStructuredTextExpert.DIR_RTL;
		return IStructuredTextExpert.DIR_LTR;
	}
}
