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

import org.eclipse.equinox.bidi.*;
import org.eclipse.equinox.bidi.internal.BidiComplexDelimsEsc;

/**
 * Processor adapted to processing e-mail addresses.
 */
public class BidiComplexEmail extends BidiComplexDelimsEsc {
	static final int LTR = BidiComplexFeatures.DIR_LTR;
	static final int RTL = BidiComplexFeatures.DIR_RTL;
	static final BidiComplexFeatures MIRRORED = new BidiComplexFeatures("<>.:,;@", 2, RTL, LTR, false, false); //$NON-NLS-1$
	static final BidiComplexFeatures NOT_MIRRORED = new BidiComplexFeatures("<>.:,;@", 2, LTR, LTR, false, false); //$NON-NLS-1$

	/**
	 *  This method retrieves the features specific to this processor.
	 *
	 *  @return features with operators "<>.:,;@", 2 special cases,
	 *          LTR direction for Arabic when the GUI is not mirrored,
	 *          RTL direction for Arabic when the GUI is mirrored,
	 *          LTR direction for Hebrew in all cases,
	 *          and support for both Arabic and Hebrew.
	 */
	public BidiComplexFeatures init(BidiComplexHelper helper, BidiComplexEnvironment env) {
		return env.mirrored ? MIRRORED : NOT_MIRRORED;
	}

	/**
	 *  @return parentheses and quotation marks as delimiters.
	 */
	protected String getDelimiters() {
		return "()\"\""; //$NON-NLS-1$
	}

}
