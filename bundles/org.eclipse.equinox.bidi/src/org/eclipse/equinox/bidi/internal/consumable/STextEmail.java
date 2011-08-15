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

import org.eclipse.equinox.bidi.STextDirection;
import org.eclipse.equinox.bidi.advanced.STextEnvironment;
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

	public int getDirection(STextEnvironment environment, String text) {
		return getDirection(environment, text, new STextCharTypes(this, environment, text));
	}

	/**
	 *  @return {@link STextDirection#DIR_RTL DIR_RTL} if the following
	 *          conditions are satisfied:
	 *          <ul>
	 *            <li>The current locale (as expressed by the environment
	 *                language) is Arabic.</li>
	 *            <li>The domain part of the email address contains
	 *                at least one RTL character.</li>
	 *          </ul>
	 *          Otherwise, returns {@link STextDirection#DIR_LTR DIR_LTR}.
	 */
	public int getDirection(STextEnvironment environment, String text, STextCharTypes charTypes) {
		String language = environment.getLanguage();
		if (!language.equals("ar")) //$NON-NLS-1$
			return STextDirection.DIR_LTR;
		int domainStart;
		domainStart = text.indexOf('@');
		if (domainStart < 0)
			domainStart = 0;
		for (int i = domainStart; i < text.length(); i++) {
			byte charType = charTypes.getBidiTypeAt(i);
			if (charType == AL || charType == R)
				return STextDirection.DIR_RTL;
		}
		return STextDirection.DIR_LTR;
	}

	/**
	 *  @return 2 as number of special cases handled by this handler.
	 */
	public int getSpecialsCount(STextEnvironment environment) {
		return 2;
	}

	/**
	 *  @return parentheses and quotation marks as delimiters.
	 */
	protected String getDelimiters() {
		return "()\"\""; //$NON-NLS-1$
	}

}
