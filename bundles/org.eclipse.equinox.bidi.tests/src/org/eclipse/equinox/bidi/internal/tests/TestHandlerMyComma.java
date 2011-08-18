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
package org.eclipse.equinox.bidi.internal.tests;

import org.eclipse.equinox.bidi.STextDirection;
import org.eclipse.equinox.bidi.advanced.ISTextExpert;
import org.eclipse.equinox.bidi.custom.STextCharTypes;
import org.eclipse.equinox.bidi.custom.STextTypeHandler;

public class TestHandlerMyComma extends STextTypeHandler {

	private final static byte AL = Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
	protected final static int LTR = STextDirection.DIR_LTR;
	protected final static int RTL = STextDirection.DIR_RTL;

	final int dirArabic;
	final int dirHebrew;

	public TestHandlerMyComma(int dirArabic, int dirHebrew) {
		this.dirArabic = dirArabic;
		this.dirHebrew = dirHebrew;
	}

	public String getSeparators(ISTextExpert expert) {
		return ","; //$NON-NLS-1$
	}

	public boolean skipProcessing(ISTextExpert expert, String text, STextCharTypes charTypes) {
		byte charType = charTypes.getBidiTypeAt(0);
		if (charType == AL)
			return true;
		return false;
	}

	public int getDirection(ISTextExpert expert, String text) {
		return getDirection(expert, text, new STextCharTypes(expert, text));
	}

	public int getDirection(ISTextExpert expert, String text, STextCharTypes charTypes) {
		for (int i = 0; i < text.length(); i++) {
			byte charType = charTypes.getBidiTypeAt(i);
			if (charType == AL)
				return dirArabic;
		}
		return dirHebrew;
	}
}
