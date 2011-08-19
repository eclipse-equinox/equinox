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
import org.eclipse.equinox.bidi.internal.STextDelimsEsc;

/**
 *  Handler adapted to processing e-mail addresses.
 */
public class STextEmail extends STextDelimsEsc {
	static final byte L = Character.DIRECTIONALITY_LEFT_TO_RIGHT;
	static final byte R = Character.DIRECTIONALITY_RIGHT_TO_LEFT;
	static final byte AL = Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;

	public STextEmail() {
		super("<>.:,;@"); //$NON-NLS-1$
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
	 *            <li>The domain part of the email address contains
	 *                at least one RTL character.</li>
	 *          </ul>
	 *          Otherwise, returns {@link ISTextExpert#DIR_LTR DIR_LTR}.
	 */
	public int getDirection(ISTextExpert expert, String text, STextCharTypes charTypes) {
		String language = expert.getEnvironment().getLanguage();
		if (!language.equals("ar")) //$NON-NLS-1$
			return ISTextExpert.DIR_LTR;
		int domainStart;
		domainStart = text.indexOf('@');
		if (domainStart < 0)
			domainStart = 0;
		for (int i = domainStart; i < text.length(); i++) {
			byte charType = charTypes.getBidiTypeAt(i);
			if (charType == AL || charType == R)
				return ISTextExpert.DIR_RTL;
		}
		return ISTextExpert.DIR_LTR;
	}

	/**
	 *  @return 2 as number of special cases handled by this handler.
	 */
	public int getSpecialsCount(ISTextExpert expert) {
		return 2;
	}

	/**
	 *  @return parentheses and quotation marks as delimiters.
	 */
	protected String getDelimiters() {
		return "()\"\""; //$NON-NLS-1$
	}

}
