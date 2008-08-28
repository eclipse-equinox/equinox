/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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

/**
 * This class is used to process strings that have special semantic meaning
 * (such as file paths) in RTL-oriented locales so that they render in a way
 * that does not corrupt the semantic meaning of the string but also maintains
 * compliance with the Unicode BiDi algorithm of rendering Bidirectional text.
 * <p>
 * Processing of the string is done by breaking it down into segments that are
 * specified by a set of user provided delimiters. Directional punctuation
 * characters are injected into the string in order to ensure the string retains
 * its semantic meaning and conforms with the Unicode BiDi algorithm within each
 * segment.
 * </p>
 * 
 * @since 3.2
 * @noextend This class is not intended to be subclassed by clients.
 */
public class TextProcessor {

	// commonly used delimiters
	/**
	 * Dot (.) delimiter. Used most often in package names and file extensions.
	 */
	private static final String DOT = "."; //$NON-NLS-1$

	/**
	 * Colon (:) delimiter. Used most often in file paths and URLs.
	 */
	private static final String COLON = ":"; //$NON-NLS-1$

	/**
	 * Forward slash (/) delimiter. Used most often in file paths and URLs.
	 */
	private static final String FILE_SEP_FSLASH = "/"; //$NON-NLS-1$

	/**
	 * Backslash (\) delimiter. Used most often in file paths.
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

	// whether or not processing is needed
	private static boolean IS_PROCESSING_NEEDED = false;

	// constant used to indicate an LRM need not precede a delimiter 
	private static final int INDEX_NOT_SET = 999999999;

	static {
		Locale locale = Locale.getDefault();
		String lang = locale.getLanguage();

		if ("iw".equals(lang) || "he".equals(lang) || "ar".equals(lang) || "fa".equals(lang) || "ur".equals(lang)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			String osName = System.getProperty("os.name").toLowerCase(); //$NON-NLS-1$
			if (osName.startsWith("windows") || osName.startsWith("linux") || osName.startsWith("mac")) { //$NON-NLS-1$	//$NON-NLS-2$ //$NON-NLS-3$
				IS_PROCESSING_NEEDED = true;
			}
		}
	}

	/**
	 * Process the given text and return a string with the appropriate
	 * substitution based on the locale. This is equivalent to calling
	 * <code>process(String, String)</code> with the default set of
	 * delimiters.
	 * 
	 * @param text
	 *            the text to be processed
	 * @return the manipulated string
	 * @see #process(String, String)
	 * @see #getDefaultDelimiters()
	 */
	public static String process(String text) {
		if (!IS_PROCESSING_NEEDED || text == null || text.length() <= 1)
			return text;
		return process(text, getDefaultDelimiters());
	}

	/**
	 * Process a string that has a particular semantic meaning to render on BiDi
	 * locales in way that maintains the semantic meaning of the text, but
	 * differs from the Unicode BiDi algorithm. The text is segmented according
	 * to the provided delimiters. Each segment has the Unicode BiDi algorithm
	 * applied to it, but as a whole, the string is oriented left to right.
	 * <p>
	 * For example a file path such as <tt>d:\myFolder\FOLDER\MYFILE.java</tt>
	 * (where capital letters indicate RTL text) should render as
	 * <tt>d:\myFolder\REDLOF\ELIFYM.java</tt> when using the Unicode BiDi
	 * algorithm and segmenting the string according to the specified delimiter
	 * set.
	 * </p>
	 * <p>
	 * The following algorithm is used:
	 * <ol>
	 * <li>Scan the string to locate the delimiters.</li>
	 * <li>While scanning, note the direction of the last strong character
	 * scanned. Strong characters are characters which have a BiDi
	 * classification of L, R or AL as defined in the Unicode standard.</li>
	 * <li>If the last strong character before a separator is of class R or AL,
	 * add a LRM before the separator. Since LRM itself is a strong L character,
	 * following separators do not need an LRM until a strong R or AL character
	 * is found.</li>
	 * <li>If the component where the pattern is displayed has a RTL basic
	 * direction, add a LRE at the beginning of the pattern and a PDF at its
	 * end. The string is considered to have RTL direction if it contains RTL
	 * characters and the runtime locale is BiDi. There is no need to add
	 * LRE/PDF if the string begins with an LTR letter, contains no RTL letter,
	 * and ends with either a LTR letter or a digit.</li>
	 * </ol>
	 * </p>
	 * <p>
	 * NOTE: this method will change the shape of the original string passed in
	 * by inserting punctuation characters into the text in order to make it
	 * render to correctly reflect the semantic meaning of the text. Methods
	 * like <code>String.equals(String)</code> and
	 * <code>String.length()</code> called on the resulting string will not
	 * return the same values as would be returned for the original string.
	 * </p>
	 * 
	 * @param str
	 *            the text to process, if <code>null</code> return the string
	 *            as it was passed in
	 * @param delimiter
	 *            delimiters by which the string will be segmented, if
	 *            <code>null</code> the default delimiters are used
	 * @return the processed string
	 */
	public static String process(String str, String delimiter) {
		if (!IS_PROCESSING_NEEDED || str == null || str.length() <= 1)
			return str;

		// do not process a string that has already been processed.
		if (str.charAt(0) == LRE && str.charAt(str.length() - 1) == PDF) {
			return str;
		}

		// String contains RTL characters
		boolean isStringBidi = false;
		// Last strong character is RTL
		boolean isLastRTL = false;
		// Last candidate delimiter index
		int delimIndex = INDEX_NOT_SET;

		delimiter = delimiter == null ? getDefaultDelimiters() : delimiter;

		StringBuffer target = new StringBuffer();
		target.append(LRE);
		char ch;

		for (int i = 0, n = str.length(); i < n; i++) {
			ch = str.charAt(i);
			if (delimiter.indexOf(ch) != -1) {
				// character is a delimiter, note its index in the buffer
				if (isLastRTL) {
					delimIndex = target.length();
				}
			} else if (Character.isDigit(ch)) {
				if (delimIndex != INDEX_NOT_SET) {
					// consecutive neutral and weak directional characters
					// explicitly force direction to be LRM					
					target.insert(delimIndex, LRM);
					delimIndex = INDEX_NOT_SET;
					isLastRTL = false;
				}
			} else if (Character.isLetter(ch)) {
				if (isRTL(ch)) {
					isStringBidi = true;
					if (delimIndex != INDEX_NOT_SET) {
						// neutral character followed by strong right directional character
						// explicitly force direction to be LRM	
						target.insert(delimIndex, LRM);
						delimIndex = INDEX_NOT_SET;
					}
					isLastRTL = true;
				} else {
					// strong LTR character, no LRM will be required
					delimIndex = INDEX_NOT_SET;
					isLastRTL = false;
				}
			}
			target.append(ch);
		}
		/*
		 * TextProcessor is not aware of the orientation of the component owning
		 * the processed string. Enclose the string in LRE/PDF in either of 2
		 * cases: 
		 * (1) The string contains BiDi characters - implying that the
		 * string appearance depends on the basic orientation 
		 * (2) The runtime locale is BiDi AND either the string does not start with 
		 * an LTR character or it ends with LTR char or digit.
		 */
		if (isStringBidi || !Character.isLetter(str.charAt(0)) || isNeutral(str.charAt(str.length() - 1))) {
			target.append(PDF);
			return target.toString();
		}
		// Otherwise, return the original string
		return str;
	}

	/**
	 * Removes directional marker characters in the given string that were inserted by 
	 * utilizing the <code>process(String)</code> or <code>process(String, String)</code>
	 * methods.
	 * 
	 * @param str string with directional markers to remove
	 * @return string with no directional markers 
	 * @see #process(String)
	 * @see #process(String, String)
	 * @since 3.3
	 */
	public static String deprocess(String str) {
		if (!IS_PROCESSING_NEEDED || str == null || str.length() <= 1)
			return str;

		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			switch (c) {
				case LRE :
					continue;
				case PDF :
					continue;
				case LRM :
					continue;
				default :
					buf.append(c);
			}
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
	 * Return whether or not the character falls is right to left oriented.
	 */
	private static boolean isRTL(char c) {
		/*
		 * Cannot use Character.getDirectionality() since the OSGi library can
		 * be compiled with execution environments that pre-date that API.
		 * 
		 * The first range of characters is Unicode Hebrew and Arabic
		 * characters. The second range of characters is Unicode Hebrew and
		 * Arabic presentation forms.
		 * 
		 * NOTE: Farsi and Urdu fall within the Arabic scripts.
		 */
		return (((c >= 0x05d0) && (c <= 0x07b1)) || ((c >= 0xfb1d) && (c <= 0xfefc)));
	}

	/*
	 * Return whether or not the given character has a weak directional type
	 */
	private static boolean isNeutral(char c) {
		return !(Character.isDigit(c) || Character.isLetter(c));
	}

	/*
	 * Constructor for the class.
	 */
	private TextProcessor() {
		// prevent instantiation
	}
}
