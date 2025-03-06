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
package org.eclipse.equinox.bidi.internal.consumable;

import org.eclipse.equinox.bidi.advanced.IStructuredTextExpert;
import org.eclipse.equinox.bidi.custom.StructuredTextCharTypes;
import org.eclipse.equinox.bidi.internal.StructuredTextDelimsEsc;

/**
 * Handler adapted to processing e-mail addresses.
 */
public class StructuredTextEmail extends StructuredTextDelimsEsc {
	static final byte L = Character.DIRECTIONALITY_LEFT_TO_RIGHT;
	static final byte R = Character.DIRECTIONALITY_RIGHT_TO_LEFT;
	static final byte AL = Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;

	public StructuredTextEmail() {
		super("<>.:,;@"); //$NON-NLS-1$
	}

	@Override
	public int getDirection(IStructuredTextExpert expert, String text) {
		return getDirection(expert, text, new StructuredTextCharTypes(expert, text));
	}

	/**
	 * @return {@link IStructuredTextExpert#DIR_RTL DIR_RTL} if the following
	 *         conditions are satisfied:
	 *         <ul>
	 *         <li>The current locale (as expressed by the environment language) is
	 *         Arabic.</li>
	 *         <li>The domain part of the email address contains at least one RTL
	 *         character.</li>
	 *         </ul>
	 *         Otherwise, returns {@link IStructuredTextExpert#DIR_LTR DIR_LTR}.
	 */
	@Override
	public int getDirection(IStructuredTextExpert expert, String text, StructuredTextCharTypes charTypes) {
		String language = expert.getEnvironment().getLanguage();
		if (!language.equals("ar")) { //$NON-NLS-1$
			return IStructuredTextExpert.DIR_LTR;
		}
		int domainStart;
		domainStart = text.indexOf('@');
		if (domainStart < 0) {
			domainStart = 0;
		}
		for (int i = domainStart; i < text.length(); i++) {
			byte charType = charTypes.getBidiTypeAt(i);
			if (charType == AL || charType == R) {
				return IStructuredTextExpert.DIR_RTL;
			}
		}
		return IStructuredTextExpert.DIR_LTR;
	}

	/**
	 * @return 2 as number of special cases handled by this handler.
	 */
	@Override
	public int getSpecialsCount(IStructuredTextExpert expert) {
		return 2;
	}

	/**
	 * @return parentheses and quotation marks as delimiters.
	 */
	@Override
	protected String getDelimiters() {
		return "()\"\""; //$NON-NLS-1$
	}

}
