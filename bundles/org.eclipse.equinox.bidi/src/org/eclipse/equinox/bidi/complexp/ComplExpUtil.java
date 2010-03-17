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

import java.util.Locale;
import org.eclipse.equinox.bidi.internal.complexp.ComplExpBasic;

/**
 *  This class provides a number of convenience functions facilitating the
 *  processing of complex expressions.
 *
 *  <p>&nbsp;</p>
 *
 *  <h2>Code Sample</h2>
 *
 *  <p>The following code shows how to instantiate a processor adapted for a
 *  certain type of complex expression (directory and file paths), and how
 *  to obtain the <i>full</i> text corresponding to the <i>lean</i> text
 *  of such an expression.
 *
 *  <pre>
 *
 *    IComplExpProcessor processor = ComplExpUtil.create(ComplExpUtil.PATH);
 *    String leanText = "D:\\\u05d0\u05d1\\\u05d2\\\u05d3.ext";
 *    String fullText = processor.leanToFullText(leanText);
 *    System.out.println("full text = " + fullText);
 *  </pre>
 *  <p>&nbsp;</p>
 *  
 *  @noextend This class is not intended to be subclassed by clients.
 *  @noinstantiate This class is not intended to be instantiated by clients.
 *
 *  @author Matitiahu Allouche
 */
final public class ComplExpUtil {

	/**
	 *  Flag specifying that all complex expressions should by default assume
	 *  that the GUI is mirrored (gobally going from right to left).
	 *  The default can be overridden for specific instances of complex
	 *  expressions.
	 *  @see #assumeMirroredDefault
	 *  @see #isMirroredDefault
	 *  @see ComplExpBasic#mirrored
	 */
	static boolean mirroredDefault;

	/**
	 *  prevents instantiation
	 */
	private ComplExpUtil() {
		// empty
	}

	/** Specify whether the GUI where the complex expression will be displayed
	 *  is mirrored (is laid out from right to left). The value specified in
	 *  this method sets a default for all complex expressions to be created
	 *  from now on. If no value has been specified ever, the GUI
	 *  is assumed not to be mirrored.
	 *
	 *  @param  mirrored must be specified as <code>false</code> if the GUI
	 *          is not mirrored, as <code>true</code> if it is.
	 *
	 *  @see #isMirroredDefault
	 *  @see IComplExpProcessor#assumeMirrored
	 */
	public static void assumeMirroredDefault(boolean mirrored) {
		mirroredDefault = mirrored;
	}

	/** Retrieve the value currently assumed as default for GUI mirroring.
	 *
	 *  @return the current value assumed by default for GUI mirroring.
	 *
	 *  @see #assumeMirroredDefault
	 */
	public static boolean isMirroredDefault() {
		return mirroredDefault;
	}

	/** This is a convenience method which can add directional marks in a given
	 *  text before the characters specified in the given array of offsets,
	 *  and can add a prefix and/or a suffix of directional formatting characters.
	 *  This can be used for instance after obtaining offsets by calling
	 *  {@link IComplExpProcessor#leanBidiCharOffsets() leanBidiCharOffsets} in order to
	 *  produce a <i>full</i> text corresponding to the source text.
	 *  The directional formatting characters that will be added at the given
	 *  offsets will be LRMs for expressions with LTR base direction and
	 *  RLMs for expressions with RTL base direction. Leading and
	 *  trailing LRE, RLE and PDF which might be needed as prefix or suffix
	 *  depending on the orientation of the GUI component used for display
	 *  may be added depending on argument <code>affix</code>.
	 *
	 *  @param  text is the text of the complex expression.
	 *
	 *  @param  offsets is an array of offsets to characters in <code>text</code>
	 *          before which an LRM or RLM will be inserted.
	 *          Members of the array must be non-negative numbers smaller
	 *          than the length of <code>text</code>.
	 *          The array must be sorted in ascending order without duplicates.
	 *          This argument may be null if there are no marks to add.
	 *
	 *  @param  direction specifies the base direction of the complex expression.
	 *          It must be one of the values {@link IComplExpProcessor#DIRECTION_LTR} or
	 *          {@link IComplExpProcessor#DIRECTION_RTL}.
	 *
	 *  @param  affix specifies if a prefix and a suffix should be added to
	 *          the result to make sure that the <code>direction</code>
	 *          specified as second argument is honored even if the expression
	 *          is displayed in a GUI component with a different orientation.
	 *
	 *  @return a string corresponding to the source <code>text</code> with
	 *          directional marks (LRMs or RLMs) added at the specified offsets,
	 *          and directional formatting characters (LRE, RLE, PDF) added
	 *          as prefix and suffix if so required.
	 *
	 */
	public static String insertMarks(String text, int[] offsets, int direction, boolean affix) {
		int textLen = text.length();
		if (textLen == 0)
			return ""; //$NON-NLS-1$

		String curPrefix, curSuffix, full;
		char curMark, c;
		char[] fullChars;
		if (direction == IComplExpProcessor.DIRECTION_LTR) {
			curMark = LRM;
			curPrefix = "\u202a\u200e"; /* LRE+LRM *///$NON-NLS-1$
			curSuffix = "\u200e\u202c"; /* LRM+PDF *///$NON-NLS-1$
		} else {
			curMark = RLM;
			curPrefix = "\u202b\u200f"; /* RLE+RLM *///$NON-NLS-1$
			curSuffix = "\u200f\u202c"; /* RLM+PDF *///$NON-NLS-1$
		}
		// add marks at offsets
		if ((offsets != null) && (offsets.length > 0)) {
			int offLen = offsets.length;
			fullChars = new char[textLen + offLen];
			int added = 0;
			for (int i = 0, j = 0; i < textLen; i++) {
				c = text.charAt(i);
				if ((j < offLen) && (i == offsets[j])) {
					fullChars[i + added] = curMark;
					added++;
					j++;
				}
				fullChars[i + added] = c;
			}
			full = new String(fullChars);
		} else {
			full = text;
		}
		if (affix)
			return curPrefix + full + curSuffix;
		return full;
	}

	/*************************************************************************/
	/*                                                                       */
	/*  The following code is provided for compatibility with TextProcessor  */
	/*                                                                       */
	/*************************************************************************/

	//  The default set of delimiters to use to segment a string.
	private static final String defaultDelimiters = ".:/\\"; //$NON-NLS-1$
	// left to right mark
	private static final char LRM = '\u200e';
	// left to right mark
	private static final char RLM = '\u200f';
	// left to right embedding
	private static final char LRE = '\u202a';
	// right to left embedding
	private static final char RLE = '\u202b';
	// pop directional format
	private static final char PDF = '\u202c';
	// TBD use bundle properties
	private static String osName = System.getProperty("os.name").toLowerCase(); //$NON-NLS-1$	private static String
	private static boolean flagOS = osName.startsWith("windows") || osName.startsWith("linux"); //$NON-NLS-1$ //$NON-NLS-2$
	private static String lastLanguage;
	private static boolean lastGoodLang;

	static boolean isProcessingNeeded() {
		if (!flagOS)
			return false;
		// TBD use OSGi service
		String lang = Locale.getDefault().getLanguage();
		if (lang.equals(lastLanguage))
			return lastGoodLang;
		lastLanguage = lang;
		lastGoodLang = "iw".equals(lang) || "he".equals(lang) || "ar".equals(lang) || "fa".equals(lang) || "ur".equals(lang); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		return lastGoodLang;
	}

	/**
	 *  Process the given text and return a string with appropriate
	 *  directional formatting characters if the locale is a Bidi locale.
	 *  This is equivalent to calling
	 *  {@link #process(String, String)} with the default set of
	 *  delimiters (dot, colon, slash, backslash).
	 *
	 *  @param  str the text to be processed
	 *
	 *  @return the processed string
	 *
	 */
	public static String process(String str) {
		return process(str, defaultDelimiters);
	}

	/**
	 *  Process a string that has a particular semantic meaning to render
	 *  it correctly on Bidi locales. This is done by adding directional
	 *  formatting characters so that presentation using the Unicode Bidi
	 *  Algorithm will provide the expected result.
	 *  The text is segmented according to the provided delimiters.
	 *  Each segment has the Unicode Bidi Algorithm applied to it,
	 *  but as a whole, the string is oriented left to right.
	 *  <p>
	 *  For example, a file path such as <tt>d:\myfolder\FOLDER\MYFILE.java</tt>
	 *  (where capital letters indicate RTL text) should render as
	 *  <tt>d:\myfolder\REDLOF\ELIFYM.java</tt>.</p>
	 *  <p>
	 *  NOTE: this method inserts directional formatting characters into the
	 *  text. Methods like <code>String.equals(String)</code> and
	 *  <code>String.length()</code> called on the resulting string will not
	 *  return the same values as would be returned for the original string.</p>
	 *
	 *  @param  str the text to process. If <code>null</code>, return
	 *          the string itself
	 *
	 *  @param  delimiters delimiters by which the string will be segmented.
	 *          If <code>null</code>, the default delimiters are used
	 *          (dot, colon, slash, backslash).
	 *
	 *  @return the processed string
	 */
	public static String process(String str, String delimiters) {
		if ((str == null) || (str.length() <= 1) || !isProcessingNeeded())
			return str;

		// do not process a string that has already been processed.
		if (str.charAt(0) == LRE && str.charAt(str.length() - 1) == PDF)
			return str;

		// do not process a string if all the following conditions are true:
		//  a) it has no RTL characters
		//  b) it starts with a LTR character
		//  c) it ends with a LTR character or a digit
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

		if (delimiters == null)
			delimiters = defaultDelimiters;

		IComplExpProcessor processor = new ComplExpBasic(delimiters);
		// make sure that LRE/PDF are added around the string
		processor.assumeOrientation(IComplExpProcessor.ORIENT_UNKNOWN);
		return processor.leanToFullText(str);
	}

	/**
	 *  Process a string that has a particular semantic meaning to render
	 *  it correctly on Bidi locales. This is done by adding directional
	 *  formatting characters so that presentation using the Unicode Bidi
	 *  Algorithm will provide the expected result..
	 *  The text is segmented according to the syntax specified in the
	 *  <code>type</code> argument.
	 *  Each segment has the Unicode Bidi Algorithm applied to it, but the
	 *  order of the segments is governed by the type of the complex expression.
	 *  <p>
	 *  For example, a file path such as <tt>d:\myfolder\FOLDER\MYFILE.java</tt>
	 *  (where capital letters indicate RTL text) should render as
	 *  <tt>d:\myfolder\REDLOF\ELIFYM.java</tt>.</p>
	 *  <p>
	 *  NOTE: this method inserts directional formatting characters into the
	 *  text. Methods like <code>String.equals(String)</code> and
	 *  <code>String.length()</code> called on the resulting string will not
	 *  return the same values as would be returned for the original string.</p>
	 *
	 *  @param  str the text to process. If <code>null</code>, return
	 *          the string itself
	 *
	 *  @param  type specifies the type of the complex expression. It must
	 *          be one of the values allowed as argument for method
	 *          {@link #create}.
	 *
	 *  @return the processed string
	 */
	public static String processTyped(String str, String type) {
		if ((str == null) || (str.length() <= 1) || !isProcessingNeeded())
			return str;

		// do not process a string that has already been processed.
		char c = str.charAt(0);
		if (((c == LRE) || (c == RLE)) && str.charAt(str.length() - 1) == PDF)
			return str;

		IComplExpProcessor processor = ComplExpFactory.create(type);
		if (processor == null) // invalid type
			return str;

		// make sure that LRE/PDF are added around the string
		processor.assumeOrientation(IComplExpProcessor.ORIENT_UNKNOWN);
		return processor.leanToFullText(str);
	}

	/**
	 *  Removes directional marker characters in the given string that were inserted
	 *  by the {@link #process(String)} or {@link #process(String, String)}
	 *  methods.
	 *
	 *  @param  str string with directional markers to remove
	 *
	 *  @return string with no directional formatting characters
	 *
	 */
	public static String deprocess(String str) {
		if ((str == null) || (str.length() <= 1) || !isProcessingNeeded())
			return str;

		StringBuffer buf = new StringBuffer();
		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {
			char c = str.charAt(i);
			switch (c) {
				case LRM :
					continue;
				case LRE :
					continue;
				case PDF :
					continue;
				default :
					buf.append(c);
			}
		}
		return buf.toString();
	}

	/**
	 *  Removes directional marker characters in the given string that were inserted
	 *  by the {@link #process(String, int)} method.
	 *
	 *  @param  str string with directional markers to remove
	 *
	 *  @param  type type of the complex expression as specified when
	 *          calling <code>process(String str, int type)</code>
	 *
	 *  @return string with no directional formatting characters
	 *
	 */
	public static String deprocess(String str, String type) {
		if ((str == null) || (str.length() <= 1) || !isProcessingNeeded())
			return str;

		IComplExpProcessor processor = ComplExpFactory.create(type);
		if (processor == null) // invalid type
			return str;

		// make sure that LRE/PDF are added around the string
		processor.assumeOrientation(IComplExpProcessor.ORIENT_UNKNOWN);
		return processor.fullToLeanText(str);
	}

}
