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

import org.eclipse.equinox.bidi.STextEngine;
import org.eclipse.equinox.bidi.STextEnvironment;
import org.eclipse.equinox.bidi.custom.STextProcessor;
import org.eclipse.equinox.bidi.internal.STextDelimsEsc;

/**
 *  Processor adapted to processing e-mail addresses.
 */
public class STextEmail extends STextDelimsEsc {
	static final byte L = Character.DIRECTIONALITY_LEFT_TO_RIGHT;
	static final byte R = Character.DIRECTIONALITY_RIGHT_TO_LEFT;
	static final byte AL = Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;

	/**
	 *  @return separators "<>.:,;@".
	 */
	public String getSeparators(STextEnvironment environment, String text, byte[] dirProps) {
		return "<>.:,;@"; //$NON-NLS-1$
	}

	/**
	 *  @return {@link STextEngine#DIR_RTL DIR_RTL} if the following
	 *          conditions are satisfied:
	 *          <ul>
	 *            <li>The current locale (as expressed by the environment
	 *                language) is Arabic.</li>
	 *            <li>The domain part of the email address contains
	 *                at least one RTL character.</li>
	 *          </ul>
	 *          Otherwise, returns {@link STextEngine#DIR_LTR DIR_LTR}.
	 */
	public int getDirection(STextEnvironment environment, String text, byte[] dirProps) {
		String language = environment.getLanguage();
		if (!language.equals("ar")) //$NON-NLS-1$
			return STextEngine.DIR_LTR;
		int domainStart;
		domainStart = text.indexOf('@');
		if (domainStart < 0)
			domainStart = 0;
		for (int i = domainStart; i < text.length(); i++) {
			byte dirProp = STextProcessor.getDirProp(text, dirProps, i);
			if (dirProp == AL || dirProp == R)
				return STextEngine.DIR_RTL;
		}
		return STextEngine.DIR_LTR;
	}

	/**
	 *  @return 2 as number of special cases handled by this processor.
	 */
	public int getSpecialsCount(STextEnvironment environment, String text, byte[] dirProps) {
		return 2;
	}

	/**
	 *  @return parentheses and quotation marks as delimiters.
	 */
	protected String getDelimiters() {
		return "()\"\""; //$NON-NLS-1$
	}

}
