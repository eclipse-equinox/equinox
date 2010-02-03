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
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
// TBD do we really want to provide individual instances of the text processors?
final public class ComplExpFactory implements IBiDiProcessor {

	/**
	 * Factory method to create a new instance of the bi-directional text
	 * processor for the specified text type. This method may return <code>null</code> 
	 * if it is unable to locate processor for the specified text type. 
	 * 
	 * @see #PROPERTY
	 * @see #UNDERSCORE
	 * @see #COMMA_DELIMITED
	 * @see #SYSTEM_USER
	 * @see #FILE
	 * @see #EMAIL
	 * @see #URL
	 * @see #REGEXP
	 * @see #XPATH
	 * @see #JAVA
	 * @see #SQL
	 * @see #RTL_ARITHMETIC
	 * 
	 * @param type specifies the type of complex expression to process.
	 * @return a <code>IComplExpProcessor</code> instance capable of handling 
	 * the type of complex expression specified. May return <code>null</code> 
	 * if the processor not found. 
	 */
	public static IComplExpProcessor create(String type) {
		return BiDiTypesCollector.getInstance().makeProcessor(type);
	}

}
