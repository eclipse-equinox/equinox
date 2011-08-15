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

import org.eclipse.equinox.bidi.advanced.STextEnvironment;
import org.eclipse.equinox.bidi.custom.*;

public class TestHandler1 extends STextTypeHandler {

	public int getSpecialsCount(STextEnvironment env) {
		return 1;
	}

	public int indexOfSpecial(STextEnvironment env, String text, STextCharTypes charTypes, STextOffsets offsets, int caseNumber, int fromIndex) {
		return fromIndex;
	}

	public int processSpecial(STextEnvironment env, String text, STextCharTypes charTypes, STextOffsets offsets, Object state, int caseNumber, int separLocation) {
		int len = text.length();
		for (int i = len - 1; i >= 0; i--) {
			STextTypeHandler.insertMark(text, charTypes, offsets, i);
			STextTypeHandler.insertMark(text, charTypes, offsets, i);
		}
		return len;
	}
}
