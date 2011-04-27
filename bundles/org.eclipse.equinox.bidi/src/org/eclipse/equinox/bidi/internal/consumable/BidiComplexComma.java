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

import org.eclipse.equinox.bidi.BidiComplexEnvironment;
import org.eclipse.equinox.bidi.custom.BidiComplexFeatures;
import org.eclipse.equinox.bidi.custom.BidiComplexProcessor;

/**
 *  Processor adapted to processing comma-delimited lists, such as:
 *  <pre>
 *    part1,part2,part3
 *  </pre>
 */
public class BidiComplexComma extends BidiComplexProcessor {
	static final BidiComplexFeatures FEATURES = new BidiComplexFeatures(",", 0, -1, -1, false, false); //$NON-NLS-1$

	/**
	 *  This method retrieves the features specific to this processor.
	 *
	 *  @return features with one operator (comma), no special cases,
	 *          LTR direction for Arabic and Hebrew, and support for both.
	 */
	public BidiComplexFeatures getFeatures(BidiComplexEnvironment env) {
		return FEATURES;
	}

}
