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

import org.eclipse.equinox.bidi.STextEnvironment;
import org.eclipse.equinox.bidi.custom.STextFeatures;
import org.eclipse.equinox.bidi.internal.STextDelimsEsc;

/**
 *  Processor adapted to processing e-mail addresses.
 */
public class STextEmail extends STextDelimsEsc {
	static final int LTR = STextFeatures.DIR_LTR;
	static final int RTL = STextFeatures.DIR_RTL;
	static final STextFeatures MIRRORED = new STextFeatures("<>.:,;@", 2, RTL, LTR, false, false); //$NON-NLS-1$
	static final STextFeatures NOT_MIRRORED = new STextFeatures("<>.:,;@", 2, LTR, LTR, false, false); //$NON-NLS-1$

	/**
	 *  This method retrieves the features specific to this processor.
	 *
	 *  @return features with separators "<>.:,;@", 2 special cases,
	 *          LTR direction for Arabic when the GUI is not mirrored,
	 *          RTL direction for Arabic when the GUI is mirrored,
	 *          LTR direction for Hebrew in all cases,
	 *          and support for both Arabic and Hebrew.
	 */
	public STextFeatures getFeatures(STextEnvironment env) {
		if (env == null)
			env = STextEnvironment.DEFAULT;
		return env.getMirrored() ? MIRRORED : NOT_MIRRORED;
	}

	/**
	 *  @return parentheses and quotation marks as delimiters.
	 */
	protected String getDelimiters() {
		return "()\"\""; //$NON-NLS-1$
	}

}
