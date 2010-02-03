/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.bidi.complexp;

import org.eclipse.equinox.bidi.internal.complexp.BiDiTypesCollector;

/**
 * The API part for the TextProcessor replacement.
 */
// TBD processors currently are not thread-safe (ComplExpBasic has class variables containing
// parsing state). This means that either:
// a) a new instance of the processor needs to be created for every call;
// b) processors have to be made thread-safe.
public class StringProcessor {

	static public String[] getKnownTypes() {
		return BiDiTypesCollector.getInstance().getTypes();
	}

	static public IComplExpProcessor getProcessor(String type) {
		return BiDiTypesCollector.getInstance().getProcessor(type);
	}
}
