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

/**
 * Bi-directional processors supplied in this bundle.
 * 
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IBiDiProcessor {

	/**
	 * Constant indicating a type of complex expression processor adapted
	 * to processing property file statements. It expects the following 
	 * string format:
	 * <pre>
	 *  name=value
	 * </pre>
	 */
	public String PROPERTY = "property"; //$NON-NLS-1$

	/**
	 * Constant indicating a type of complex expression processor adapted
	 * to processing compound names.
	 * This type covers names made of one or more parts separated by underscores:
	 * <pre>
	 *  part1_part2_part3
	 * </pre>
	 */
	public String UNDERSCORE = "underscore"; //$NON-NLS-1$

	/**
	 * Constant indicating a type of complex expression processor adapted
	 * to processing comma-delimited lists, such as:
	 * <pre>
	 *  part1,part2,part3
	 * </pre>
	 */
	public String COMMA_DELIMITED = "comma"; //$NON-NLS-1$

	/**
	 * Constant indicating a type of complex expression processor adapted
	 * to processing expressions with the following string format:
	 * <pre>
	 *  system(user)
	 * </pre>
	 */
	public String SYSTEM_USER = "system"; //$NON-NLS-1$

	/**
	 * Constant indicating a type of complex expression processor adapted
	 * to processing directory and file paths.
	 */
	public String FILE = "file"; //$NON-NLS-1$

	/**
	 *  Constant indicating a type of complex expression processor adapted
	 *  to processing e-mail addresses.
	 */
	public String EMAIL = "email"; //$NON-NLS-1$

	/**
	 *  Constant indicating a type of complex expression processor adapted
	 *  to processing URLs.
	 */
	public String URL = "url"; //$NON-NLS-1$

	/**
	 *  Constant indicating a type of complex expression processor adapted
	 *  to processing regular expressions, possibly spanning more than one
	 *  line.
	 */
	public String REGEXP = "regex"; //$NON-NLS-1$

	/**
	 *  Constant indicating a type of complex expression processor adapted
	 *  to processing XPath expressions.
	 */
	public String XPATH = "xpath"; //$NON-NLS-1$

	/**
	 *  Constant indicating a type of complex expression processor adapted
	 *  to processing Java code, possibly spanning more than one line.
	 */
	public String JAVA = "java"; //$NON-NLS-1$

	/**
	 *  Constant indicating a type of complex expression processor adapted
	 *  to processing SQL statements, possibly spanning more than one line.
	 */
	public String SQL = "sql"; //$NON-NLS-1$

	/**
	 *  Constant indicating a type of complex expression processor adapted
	 *  to processing arithmetic expressions with a RTL base direction.
	 */
	public String RTL_ARITHMETIC = "math"; //$NON-NLS-1$
}
