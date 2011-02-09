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
package org.eclipse.equinox.bidi.custom;

import org.eclipse.equinox.bidi.internal.BidiComplexTypesCollector;

/**
 * This class provides access to registered complex expression processors.
 */
public class BidiComplexStringProcessor {
	/**
	 *  Retrieve all registered types of complex expression processors.
	 *
	 *  @return an array of strings, each string identifying a type of
	 *  complex expression processor.
	 */
	static public String[] getKnownTypes() {
		return BidiComplexTypesCollector.getInstance().getTypes();
	}

	/**
	 *  Get access to a complex expression processor of a given type.
	 *
	 *  @param type string identifying a type of processor
	 *
	 *  @return a reference to an instance of a processor of the
	 *  required type. If the type is unknown, return <code>null</code>.
	 */
	static public IBidiComplexProcessor getProcessor(String type) {
		return BidiComplexTypesCollector.getInstance().getProcessor(type);
	}
}
