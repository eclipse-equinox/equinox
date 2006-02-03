/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.util;

import java.util.Locale;
import java.util.StringTokenizer;

/**
 * This class is used to process strings that have special semantic meaning (such 
 * as file paths) in RTL-oriented locales so that they render in a way that does
 * not corrupt the semantic meaning of the string but also maintains compliance 
 * with the Unicode Bidi algorithm of rendering Bidirectional text. 
 * <p>
 * Processing of the string is done by breaking it down into segments that are 
 * specified by a set of user provided delimiters. Directional punctuation 
 * characters are injected into the string in order to ensure the string retains 
 * its semantic meaning and conforms with the Unicode Bidi algorithm within each 
 * segment.
 * </p>
 * @since 3.2
 */
public class TextProcessor {

	// commonly used delimiters
	/**
	 * Dot (.) delimiter.  Used most often in package names and file extensions.
	 */
	private static final String DOT = "."; //$NON-NLS-1$

	/**
	 * Colon (:) delimiter.  Used most often in file paths and URLs.
	 */
	private static final String COLON = ":"; //$NON-NLS-1$

	/**
	 * Forward slash (/) delimiter.  Used most often in file paths and URLs.
	 */
	private static final String FILE_SEP_FSLASH = "/"; //$NON-NLS-1$

	/**
	 * Backslash (\) delimiter.  Used most often in file paths.
	 */
	private static final String FILE_SEP_BSLASH = "\\"; //$NON-NLS-1$

	/**
	 * The default set of delimiters to use to segment a string.
	 */
	private static final String delimiterString = DOT + COLON + FILE_SEP_FSLASH + FILE_SEP_BSLASH;

	// left to right marker
	private static final char LRM = '\u200e';
	// left to right embedding
	private static final char LRE = '\u202a';
	// pop directional format	
	private static final char PDF = '\u202c';

	private static boolean isBidi = false;
	static {
		Locale locale = Locale.getDefault();
		String lang = locale.getLanguage();

		if ("iw".equals(lang) || "ar".equals(lang) || "fa".equals(lang) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				|| "ur".equals(lang)) //$NON-NLS-1$
			isBidi = true;
	}

	/**
	 * Process the given text and return a string with the appropriate
	 * substitution based on the locale. This is equivalent to calling
	 * <code>process(String, String)</code> with the default set
	 * of delimiters.
	 * 
	 * @param text the text to be processed
	 * @return the manipulated string
	 * @see #process(String, String)
	 * @see #getDefaultDelimiters()
	 */
	public static String process(String text) {
		return process(text, getDefaultDelimiters());
	}

	/**
	 * Process a string that has a particular semantic meaning to render on Bidi 
	 * locales in way that maintains the semantic meaning of the text, but differs
	 * from the Unicode Bidi algoritm.  
	 * The text is segmented according to the provided delimiters. Each segment 
	 * has the Unicode Bidi algorithm applied to it, but as a whole, the string 
	 * is oriented left to right.  
	 * <p>
	 * For example a file path such as 
	 *     <tt>d:\myFolder\FOLDER\MYFILE.java</tt>  
	 * (where capital letters indicate RTL text)
	 * should render as 
	 *     <tt>d:\myFolder\REDLOF\ELIFYM.java</tt>
	 * when using the Unicode Bidi algorithm and segmenting the string according
	 * to the specified delimiter set.
	 * </p><p>
	 * The following algorithm is used:
	 * <ol>
	 * <li>Scan the string to locate the separators and the tokens.</li>
	 * <li>While scanning, note the direction of the last strong character scanned.  
	 *    Strong characters are characters which have a BiDi classification of 
	 *    L, R or AL as defined in the Unicode standard.</li>
	 * <li>If the last strong character before a separator is of class R or AL, 
	 *    add a LRM before the separator.  Since LRM itself is a strong L character, 
	 *    following separators do not need an LRM until a strong R or AL character 
	 *    is found.</li>
	 * <li>If the component where the pattern is displayed has a RTL basic direction, 
	 *    add a LRE at the beginning of the pattern and a PDF at its end.</li>
	 * </ol>
	 * </p><p>
	 * NOTE: this method will change the shape of the original string passed 
	 *    in by inserting punctuation characters into the text in order to 
	 *    make it render to correctly reflect the semantic meaning of the text.
	 *    Methods like String.equals(String) and String.length() called on the 
	 *    resulting string will not return the same values as would be returned 
	 *     for the original string. 
	 * </p>
	 * @param str the text to process
	 * @param delimiter delimiters by which the string will be segmented
	 * @return the processed string
	 */
	public static String process(String str, String delimiter) {
		if (!isBidi)
			return str;

		delimiter = delimiter == null ? getDefaultDelimiters() : delimiter;
		// the last scanned token
		String lastToken = null;

		StringTokenizer tokenizer = new StringTokenizer(str, delimiter, true);
		// no delimiters found so revert to default behaviour
		if (tokenizer.countTokens() == 1)
			return str;
		StringBuffer buf = new StringBuffer(); // the string to build
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (delimiter.indexOf(token) != -1) {
				// token is a delimiter
				// see (#3) of algorithm
				if (lastToken == null)
					buf.append(LRM);
				buf.append(token);
				buf.append(LRM);
			} else {
				// (non-delimiter) text has basic RTL orientation
				// see (#4) of algorithm
				buf.append(LRE);
				buf.append(token);
				buf.append(PDF);
			}
			lastToken = token;
		}
		return buf.toString();
	}

	/**
	 * Return the string containing all the default delimiter characters to be 
	 * used to segment a given string.
	 * 
	 * @return delimiter string
	 */
	public static String getDefaultDelimiters() {
		return delimiterString;
	}

	/*
	 * Constructor for the class.
	 */
	private TextProcessor() {
		// prevent instantiation
	}
}
