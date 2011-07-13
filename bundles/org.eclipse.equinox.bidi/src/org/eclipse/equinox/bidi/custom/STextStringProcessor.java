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

import org.eclipse.equinox.bidi.internal.STextTypesCollector;

/**
 * This class provides access to registered structured text processors.
 */
public class STextStringProcessor {
	/**
	 *  Retrieve all registered types of structured text processors.
	 *
	 *  @return an array of strings, each string identifying a type of
	 *  structured text processor.
	 */
	static public String[] getKnownTypes() {
		return STextTypesCollector.getInstance().getTypes();
	}

	/**
	 *  Get access to a structured text processor of a given type.
	 *
	 *  @param type string identifying a type of processor
	 *
	 *  @return a reference to an instance of a processor of the
	 *  required type. If the type is unknown, return <code>null</code>.
	 */
	static public STextProcessor getProcessor(String type) {
		return STextTypesCollector.getInstance().getProcessor(type);
	}
}
