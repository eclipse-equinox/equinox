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
package org.eclipse.equinox.bidi.internal.consumable;

import org.eclipse.equinox.bidi.advanced.ISTextExpert;
import org.eclipse.equinox.bidi.custom.STextCharTypes;
import org.eclipse.equinox.bidi.custom.STextTypeHandler;

/**
 *  Handler adapted to processing arithmetic expressions with
 *  a possible right-to-left base direction.
 */
public class STextMath extends STextTypeHandler {
	static final byte L = Character.DIRECTIONALITY_LEFT_TO_RIGHT;
	static final byte R = Character.DIRECTIONALITY_RIGHT_TO_LEFT;
	static final byte AL = Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
	static final byte AN = Character.DIRECTIONALITY_ARABIC_NUMBER;

	public STextMath() {
		super("+-/*()="); //$NON-NLS-1$
	}

	public int getDirection(ISTextExpert expert, String text) {
		return getDirection(expert, text, new STextCharTypes(expert, text));
	}

	/**
	 *  @return {@link ISTextExpert#DIR_RTL DIR_RTL} if the following
	 *          conditions are satisfied:
	 *          <ul>
	 *            <li>The current locale (as expressed by the environment
	 *                language) is Arabic.</li>
	 *            <li>The first strong character is an Arabic letter.</li>
	 *            <li>If there is no strong character in the text, there is
	 *                at least one Arabic-Indic digit in the text.</li>
	 *          </ul>
	 *          Otherwise, returns {@link ISTextExpert#DIR_LTR DIR_LTR}.
	 */
	public int getDirection(ISTextExpert expert, String text, STextCharTypes charTypes) {
		String language = expert.getEnvironment().getLanguage();
		if (!language.equals("ar")) //$NON-NLS-1$
			return ISTextExpert.DIR_LTR;
		boolean flagAN = false;
		for (int i = 0; i < text.length(); i++) {
			byte charType = charTypes.getBidiTypeAt(i);
			if (charType == AL)
				return ISTextExpert.DIR_RTL;
			if (charType == L || charType == R)
				return ISTextExpert.DIR_LTR;
			if (charType == AN)
				flagAN = true;
		}
		if (flagAN)
			return ISTextExpert.DIR_RTL;
		return ISTextExpert.DIR_LTR;
	}
}
