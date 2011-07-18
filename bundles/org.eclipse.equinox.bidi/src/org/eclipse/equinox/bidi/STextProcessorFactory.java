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

import org.eclipse.equinox.bidi.custom.STextProcessor;
import org.eclipse.equinox.bidi.internal.STextTypesCollector;

/**
 * This class provides access to registered structured text processors.
 * 
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
final public class STextProcessorFactory {

	/**
	 * Structured text processor for property file statements. It expects the following format:
	 * <pre>
	 *  name=value
	 * </pre>
	 */
	public static final STextProcessor PROC_PROPERTY = getProcessor("property"); //$NON-NLS-1$

	/**
	 * Structured text processor for compound names. It expects text to be made of one or more 
	 * parts separated by underscores:
	 * <pre>
	 *  part1_part2_part3
	 * </pre>
	 */
	public static final STextProcessor PROC_UNDERSCORE = getProcessor("underscore"); //$NON-NLS-1$

	/**
	 * Structured text processor for comma-delimited lists, such as:
	 * <pre>
	 *  part1,part2,part3
	 * </pre>
	 */
	public static final STextProcessor PROC_COMMA_DELIMITED = getProcessor("comma"); //$NON-NLS-1$

	/**
	 * Structured text processor for strings with the following format:
	 * <pre>
	 *  system(user)
	 * </pre>
	 */
	public static final STextProcessor PROC_SYSTEM_USER = getProcessor("system"); //$NON-NLS-1$

	/**
	 * Structured text processor for directory and file paths.
	 */
	public static final STextProcessor PROC_FILE = getProcessor("file"); //$NON-NLS-1$

	/**
	 * Structured text processor for e-mail addresses.
	 */
	public static final STextProcessor PROC_EMAIL = getProcessor("email"); //$NON-NLS-1$

	/**
	 * Structured text processor for URLs.
	 */
	public static final STextProcessor PROC_URL = getProcessor("url"); //$NON-NLS-1$

	/**
	 * Structured text processor for regular expressions, possibly spanning multiple lines.
	 */
	public static final STextProcessor PROC_REGEXP = getProcessor("regex"); //$NON-NLS-1$

	/**
	 * Structured text processor for XPath expressions.
	 */
	public static final STextProcessor PROC_XPATH = getProcessor("xpath"); //$NON-NLS-1$

	/**
	 * Structured text processor for Java code, possibly spanning multiple lines.
	 */
	public static final STextProcessor PROC_JAVA = getProcessor("java"); //$NON-NLS-1$

	/**
	 * Structured text processor for SQL statements, possibly spanning multiple lines.
	 */
	public static final STextProcessor PROC_SQL = getProcessor("sql"); //$NON-NLS-1$

	/**
	 *  Structured text processor for arithmetic expressions, possibly with a RTL base direction.
	 */
	public static final STextProcessor PROC_RTL_ARITHMETIC = getProcessor("math"); //$NON-NLS-1$

	/**
	 * Prevents instantiation
	 */
	private STextProcessorFactory() {
		// placeholder
	}

	/**
	 * Retrieve all IDs of registered structured text processors.
	 * @return an array of text processor IDs.
	 */
	static public String[] getAllProcessorIDs() {
		return STextTypesCollector.getInstance().getTypes();
	}

	/**
	 *  Obtain a structured text processor of a given type.
	 *  @param id string identifying processor
	 *  @return a processor of the required type, or <code>null</code> if the type is unknown
	 */
	static public STextProcessor getProcessor(String id) {
		return STextTypesCollector.getInstance().getProcessor(id);
	}
}
