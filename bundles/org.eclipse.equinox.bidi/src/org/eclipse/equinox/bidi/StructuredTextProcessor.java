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

import org.eclipse.equinox.bidi.advanced.*;
import org.eclipse.equinox.bidi.custom.StructuredTextTypeHandler;

/**
 * Provides methods to process bidirectional text with a specific structure. The
 * methods in this class are the most straightforward way to add directional
 * formatting characters to the source text to ensure correct presentation, or
 * to remove those characters to restore the original text (for more
 * explanations, please see the {@link org.eclipse.equinox.bidi package
 * documentation}.
 * <p>
 * This class can be used without OSGi running (but only the structured text
 * types declared in {@link StructuredTextTypeHandlerFactory} are available in
 * that mode).
 * </p>
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class StructuredTextProcessor {

	/**
	 * The default set of separators used to segment a string: dot, colon, slash,
	 * backslash.
	 */
	private static final String defaultSeparators = ".:/\\"; //$NON-NLS-1$

	// left to right mark
	private static final char LRM = '\u200e';

	// left to right embedding
	private static final char LRE = '\u202a';

	// right to left embedding
	private static final char RLE = '\u202b';

	// pop directional format
	private static final char PDF = '\u202c';

	/**
	 * Prevents instantiation.
	 */
	private StructuredTextProcessor() {
		// empty
	}

	/**
	 * Processes the given (<i>lean</i>) text and returns a string with appropriate
	 * directional formatting characters (<i>full</i> text). This is equivalent to
	 * calling {@link #process(String str, String separators)} with the default set
	 * of separators.
	 * <p>
	 * The processing adds directional formatting characters so that presentation
	 * using the Unicode Bidirectional Algorithm will provide the expected result.
	 * The text is segmented according to the provided separators. Each segment has
	 * the Unicode Bidi Algorithm applied to it, but as a whole, the string is
	 * oriented left to right.
	 * </p>
	 * <p>
	 * For example, a file path such as <code>d:\myfolder\FOLDER\MYFILE.java</code>
	 * (where capital letters indicate RTL text) should render as
	 * <code>d:\myfolder\REDLOF\ELIFYM.java</code>.
	 * </p>
	 * 
	 * @param str the <i>lean</i> text to process
	 * 
	 * @return the processed string (<i>full</i> text)
	 * 
	 * @see #deprocess(String)
	 */
	public static String process(String str) {
		return process(str, defaultSeparators);
	}

	/**
	 * Processes a string that has a particular semantic meaning to render it
	 * correctly in bidi environments. For more details, see
	 * {@link #process(String)}.
	 * 
	 * @param str        the <i>lean</i> text to process
	 * @param separators characters by which the string will be segmented
	 * 
	 * @return the processed string (<i>full</i> text)
	 * 
	 * @see #deprocess(String)
	 */
	public static String process(String str, String separators) {
		if ((str == null) || (str.length() <= 1))
			return str;

		// do not process a string that has already been processed.
		if (str.charAt(0) == LRE && str.charAt(str.length() - 1) == PDF)
			return str;

		StructuredTextEnvironment env = new StructuredTextEnvironment(null, false,
				StructuredTextEnvironment.ORIENT_UNKNOWN);
		// do not process a string if all the following conditions are true:
		// a) it has no RTL characters
		// b) it starts with a LTR character
		// c) it ends with a LTR character or a digit
		boolean isStringBidi = false;
		int strLength = str.length();
		char c;
		for (int i = 0; i < strLength; i++) {
			c = str.charAt(i);
			if (((c >= 0x05d0) && (c <= 0x07b1)) || ((c >= 0xfb1d) && (c <= 0xfefc))) {
				isStringBidi = true;
				break;
			}
		}
		while (!isStringBidi) {
			if (!Character.isLetter(str.charAt(0)))
				break;
			c = str.charAt(strLength - 1);
			if (!Character.isDigit(c) && !Character.isLetter(c))
				break;
			return str;
		}

		if (separators == null)
			separators = defaultSeparators;

		// make sure that LRE/PDF are added around the string
		StructuredTextTypeHandler handler = new StructuredTextTypeHandler(separators);
		IStructuredTextExpert expert = StructuredTextExpertFactory.getStatefulExpert(handler, env);
		return expert.leanToFullText(str);
	}

	/**
	 * Processes a string that has a particular semantic meaning to render it
	 * correctly in bidi environments. For more details, see
	 * {@link #process(String)}.
	 * 
	 * @param str      the <i>lean</i> text to process.
	 * @param textType an identifier for the structured text handler appropriate for
	 *                 the type of the text submitted. It may be one of the
	 *                 identifiers defined in
	 *                 {@link StructuredTextTypeHandlerFactory} or a type handler
	 *                 identifier registered by a plug-in.
	 * 
	 * @return the processed string (<i>full</i> text).
	 * 
	 * @see #deprocessTyped
	 */
	public static String processTyped(String str, String textType) {
		if ((str == null) || (str.length() <= 1))
			return str;

		// do not process a string that has already been processed.
		char c = str.charAt(0);
		if (((c == LRE) || (c == RLE)) && str.charAt(str.length() - 1) == PDF)
			return str;

		// make sure that LRE/PDF are added around the string
		StructuredTextEnvironment env = new StructuredTextEnvironment(null, false,
				StructuredTextEnvironment.ORIENT_UNKNOWN);
		IStructuredTextExpert expert = StructuredTextExpertFactory.getExpert(textType, env);
		return expert.leanToFullText(str);
	}

	/**
	 * Removes directional formatting characters in the given string.
	 * 
	 * @param str string with directional characters to remove (<i>full</i> text).
	 * 
	 * @return string without directional formatting characters (<i>lean</i> text).
	 */
	public static String deprocess(String str) {
		if ((str == null) || (str.length() <= 1))
			return str;

		StringBuilder buf = new StringBuilder();
		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {
			char c = str.charAt(i);
			switch (c) {
			case LRM:
				continue;
			case LRE:
				continue;
			case PDF:
				continue;
			default:
				buf.append(c);
			}
		}
		return buf.toString();
	}

	/**
	 * Removes directional formatting characters in the given string.
	 * 
	 * @param str      string with directional characters to remove (<i>full</i>
	 *                 text).
	 * @param textType an identifier for the structured text handler appropriate for
	 *                 the type of the text submitted. It may be one of the
	 *                 identifiers defined in
	 *                 {@link StructuredTextTypeHandlerFactory} or a type handler
	 *                 identifier registered by a plug-in.
	 * 
	 * @return string without directional formatting characters (<i>lean</i> text).
	 * 
	 * @see #processTyped(String, String)
	 */
	public static String deprocessTyped(String str, String textType) {
		if ((str == null) || (str.length() <= 1))
			return str;

		// make sure that LRE/PDF are removed from the string
		StructuredTextEnvironment env = new StructuredTextEnvironment(null, false,
				StructuredTextEnvironment.ORIENT_UNKNOWN);
		IStructuredTextExpert expert = StructuredTextExpertFactory.getExpert(textType, env);
		return expert.fullToLeanText(str);
	}

	/**
	 * Returns a string containing all the default separator characters to be used
	 * to segment a given string.
	 * 
	 * @return string containing all separators.
	 */
	public static String getDefaultSeparators() {
		return defaultSeparators;
	}

}
