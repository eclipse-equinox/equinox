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
import org.eclipse.equinox.bidi.custom.BidiComplexProcessor;

/**
 * Processor adapted to processing arithmetic expressions with right-to-left
 * base direction.
 */
public class BidiComplexMath extends BidiComplexProcessor {
	static final int RTL = BidiComplexFeatures.DIR_RTL;
	static final BidiComplexFeatures FEATURES = new BidiComplexFeatures("+-/*()=", 0, RTL, RTL, false, false); //$NON-NLS-1$

	/**
	 *  This method retrieves the features specific to this processor.
	 *
	 *  @return features with operators "+-/*()=", no special cases,
	 *          RTL direction for Arabic and Hebrew, and support for both.
	 */
	public BidiComplexFeatures init(BidiComplexHelper helper, BidiComplexEnvironment env) {
		return FEATURES;
	}

}
