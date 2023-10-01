/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.bidi;

import org.eclipse.equinox.bidi.advanced.StructuredTextEnvironment;
import org.eclipse.equinox.bidi.advanced.StructuredTextExpertFactory;
import org.eclipse.equinox.bidi.custom.StructuredTextTypeHandler;
import org.eclipse.equinox.bidi.internal.StructuredTextTypesCollector;

/**
 * Provides access to registered structured text handlers.
 * <p>
 * A structured text handler is a subclass of {@link StructuredTextTypeHandler}
 * adapted for a given type of structured text.
 * <p>
 * The constants in this class are identifiers for structured text handlers
 * which are defined and supported "out of the box" by this package. Text
 * handler identifiers can be used when invoking
 * {@link StructuredTextProcessor#processTyped(String, String)}, or when
 * invoking <code>getExpert</code> methods in
 * {@link StructuredTextExpertFactory}.
 * <p>
 * The {@link #getHandler} method in this class can be used to get a structured
 * text handler reference for one of the handlers defined in this package or for
 * additional structured text handlers registered by plug-ins via the
 * <code>org.eclipse.equinox.bidi.bidiTypes</code> extension point. Text handler
 * references can be used when invoking
 * {@link StructuredTextExpertFactory#getStatefulExpert(StructuredTextTypeHandler, StructuredTextEnvironment)}.
 * <p>
 * This class can be used without OSGi running, but only the structured text
 * types declared as string constants in this class are available in that mode.
 * </p>
 * 
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class StructuredTextTypeHandlerFactory {

	/**
	 * Structured text handler identifier for comma-delimited lists, such as:
	 * 
	 * <pre>
	 *  part1,part2,part3
	 * </pre>
	 */
	public static final String COMMA_DELIMITED = "comma"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for e-mail addresses.
	 */
	public static final String EMAIL = "email"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for directory and file paths.
	 */
	public static final String FILE = "file"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for Java code, possibly spanning multiple
	 * lines.
	 */
	public static final String JAVA = "java"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for regular expressions, possibly spanning
	 * multiple lines.
	 */
	public static final String REGEX = "regex"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for SQL statements, possibly spanning
	 * multiple lines.
	 */
	public static final String SQL = "sql"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for compound names. It expects text to be
	 * made of one or more parts separated by underscores:
	 * 
	 * <pre>
	 * part1_part2_part3
	 * </pre>
	 */
	public static final String UNDERSCORE = "underscore"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for URLs.
	 */
	public static final String URL = "url"; //$NON-NLS-1$

	/**
	 * Structured text handler identifier for XPath expressions.
	 */
	public static final String XPATH = "xpath"; //$NON-NLS-1$

	// *** New types must be added to
	// StructuredTextTypesCollector#getDefaultTypeHandlers()!

	/**
	 * Prevents instantiation
	 */
	private StructuredTextTypeHandlerFactory() {
		// placeholder
	}

	/**
	 * Obtains a structured text handler of a given type.
	 * 
	 * Supported type ids are:
	 * <ul>
	 * <li>the <code>String</code> constants in
	 * {@link StructuredTextTypeHandlerFactory}</li>
	 * <li>if OSGi is running, the types that have been contributed to the
	 * <code>org.eclipse.equinox.bidi.bidiTypes</code> extension point.</li>
	 * </ul>
	 * 
	 * @param id the string identifying a structured text handler
	 * @return a handler of the required type, or <code>null</code> if the type is
	 *         unknown
	 */
	static public StructuredTextTypeHandler getHandler(String id) {
		return StructuredTextTypesCollector.getInstance().getHandler(id);
	}

}
