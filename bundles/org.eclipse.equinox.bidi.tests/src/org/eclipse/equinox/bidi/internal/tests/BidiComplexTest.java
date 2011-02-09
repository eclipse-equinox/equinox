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

import org.eclipse.equinox.bidi.*;
import org.eclipse.equinox.bidi.custom.IBidiComplexProcessor;

public class BidiComplexTest implements IBidiComplexProcessor {

	static final BidiComplexFeatures FEATURES = new BidiComplexFeatures("-=.:", 0, -1, -1, false, false);

	public BidiComplexFeatures init(BidiComplexHelper caller, BidiComplexEnvironment env) {
		return FEATURES;
	}

	public BidiComplexFeatures updateEnvironment(BidiComplexHelper caller, BidiComplexEnvironment env) {
		return FEATURES;
	}

	public int indexOfSpecial(BidiComplexHelper caller, int caseNumber, String srcText, int fromIndex) {
		throw new IllegalStateException();
	}

	public int processSpecial(BidiComplexHelper caller, int caseNumber, String srcText, int operLocation) {
		throw new IllegalStateException();
	}

}
