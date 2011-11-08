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
package org.eclipse.equinox.bidi;

import org.eclipse.equinox.bidi.advanced.STextEnvironment;
import org.eclipse.equinox.bidi.advanced.STextExpertFactory;
import org.eclipse.equinox.bidi.custom.STextTypeHandler;
import org.eclipse.equinox.bidi.internal.STextTypesCollector;

/**
 * Provides access to registered structured text handlers.
 * <p>
 * A structured text handler is a subclass of {@link STextTypeHandler}
 * adapted for a given type of structured text.
 * <p>
 * The constants in this class are identifiers for structured text
 * handlers which are defined and supported "out of the box" by this package.
 * Text handler identifiers can be used when invoking {@link STextProcessor#processTyped(String, String)},
 * or when invoking <code>getExpert</code> methods in {@link STextExpertFactory}.
 * <p>
 * The {@link #getHandler} method in this class can be used to get a 
 * structured text handler reference for one of the handlers defined in this
 * package or for additional structured text handlers registered by plug-ins.
 * Text handler references can be used when invoking 
 * {@link STextExpertFactory#getStatefulExpert(STextTypeHandler, STextEnvironment)}.
 *  
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
final public class STextTypeHandlerFactory {

	/**
	 * Structured text handler identifier for property file statements. It expects the following format:
	 * <pre>
	 *  name=value
	 * </pre>
	 */
	public static final String PROPERTY = "property"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for compound names. It expects text to be made of one or more 
	 * parts separated by underscores:
	 * <pre>
	 *  part1_part2_part3
	 * </pre>
	 */
	public static final String UNDERSCORE = "underscore"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for comma-delimited lists, such as:
	 * <pre>
	 *  part1,part2,part3
	 * </pre>
	 */
	public static final String COMMA_DELIMITED = "comma"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for strings with the following format:
	 * <pre>
	 *  system(user)
	 * </pre>
	 */
	public static final String SYSTEM_USER = "system"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for directory and file paths.
	 */
	public static final String FILE = "file"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for e-mail addresses.
	 */
	public static final String EMAIL = "email"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for URLs.
	 */
	public static final String URL = "url"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for regular expressions, 
	 * possibly spanning multiple lines.
	 */
	public static final String REGEXP = "regex"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for XPath expressions.
	 */
	public static final String XPATH = "xpath"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for Java code, 
	 * possibly spanning multiple lines.
	 */
	public static final String JAVA = "java"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for SQL statements, 
	 * possibly spanning multiple lines.
	 */
	public static final String SQL = "sql"; //$NON-NLS-1$

	/**
	 *  Structured text handler identifier for arithmetic expressions, 
	 *  possibly with a RTL base direction.
	 */
	public static final String RTL_ARITHMETIC = "math"; //$NON-NLS-1$

	/**
	 * Prevents instantiation
	 */
	private STextTypeHandlerFactory() {
		// placeholder
	}

	/**
	 *  Obtains a structured text handler of a given type.
	 *  
	 *  @param id the string identifying a structured text handler.
	 *  
	 *  @return a handler of the required type, or <code>null</code> 
	 *          if the type is unknown.
	 */
	static public STextTypeHandler getHandler(String id) {
		return STextTypesCollector.getInstance().getHandler(id);
	}

}
