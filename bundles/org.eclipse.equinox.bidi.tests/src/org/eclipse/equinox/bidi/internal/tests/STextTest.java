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

import org.eclipse.equinox.bidi.STextEnvironment;
import org.eclipse.equinox.bidi.custom.STextFeatures;
import org.eclipse.equinox.bidi.custom.ISTextProcessor;

public class STextTest implements ISTextProcessor {

	static final STextFeatures FEATURES = new STextFeatures("-=.:", 0, -1, -1, false, false);

	public STextFeatures getFeatures(STextEnvironment env) {
		return FEATURES;
	}

	public int indexOfSpecial(STextFeatures features, String text, byte[] dirProps, int[] offsets, int caseNumber, int fromIndex) {
		throw new IllegalStateException();
	}

	public int processSpecial(STextFeatures features, String text, byte[] dirProps, int[] offsets, int[] state, int caseNumber, int separLocation) {
		throw new IllegalStateException();
	}

}
