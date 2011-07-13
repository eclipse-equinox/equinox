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
import org.eclipse.equinox.bidi.custom.STextStringProcessor;
import org.eclipse.equinox.bidi.internal.STextImpl;

/**
 *  This class acts as a mediator between applications and structured text
 *  processors.
 *  The purpose of structured text processors is to add directional
 *  formatting characters to ensure correct display.
 *  This class shields applications from the
 *  intricacies of structured text processors.
 *  <p>
 *  For a general introduction to structured text, see
 *  {@link <a href="package-summary.html">
 *  the package documentation</a>}.
 *
 *  <h2><a name="processor">How to Specify a Processor</a></h2>
 *
 *  <p>All the methods in this class have a first argument which
 *  designates a processor.
 *
 *  <p>It must be specified as an instance of {@link STextProcessor}.
 *  Pre-defined instances are included in <b>STextEngine</b> for all
 *  the types of structured text supported by this package.
 *
 *  <p>For processors supplied by other packages, a processor instance
 *  can be obtained using the
 *  {@link org.eclipse.equinox.bidi.custom.STextStringProcessor#getProcessor getProcessor}
 *  method for the registered processors, or by instantiating a private processor.
 *
 *  <p>Specifying <code>null</code> for the processor as first argument
 *  of a method causes this method to behave as a no-op.
 *
 *  <h2><a name="state">State</a></h2>
 *
 *  <p>Most of the methods in this class have a <code>text</code>
 *  argument which may be just a part of a larger body of text.
 *  When it is the case that the text is submitted in parts with
 *  repeated calls, there may be a need to pass information from
 *  one invocation to the next one. For instance, one invocation
 *  may detect that a comment or a literal has been started but
 *  has not been completed. In such cases, a <code>state</code>
 *  argument must be used.
 *
 *  <p>The <code>state</code> argument must be an array of integers
 *  with at least one element. Only the first element is used by
 *  the methods of this class.
 *
 *  <p>When submitting the initial part of the text, the first element
 *  of <code>state</code> must contain the value {@link #STATE_INITIAL}
 *  or any value <= 0.
 *
 *  <p>After calling a method with a non-null <code>state</code> argument,
 *  a value is returned in the first element of <code>state</code>. This
 *  value should be passed unmodified to the method when calling it again
 *  with the text which is the continuation of the text submitted in the
 *  last call.
 *
 *  <p>When the text submitted to a method is not a continuation and is not
 *  expected to have a continuation , e.g. it is processed all by itself,
 *  the <code>state</code> argument should be specified as <code>null</code>.
 *
 *  <h2>Code Samples</h2>
 *
 *  <p>The following code shows how to transform a certain type of structured text
 *  (directory and file paths) in order to obtain the <i>full</i>
 *  text corresponding to the given <i>lean</i> text.
 *
 *  <pre>
 *
 *    String leanText = "D:\\\u05d0\u05d1\\\u05d2\\\u05d3.ext";
 *    String fullText = STextEngine.leanToFullText(STextEngine.PROC_FILE, null, leanText, null);
 *    System.out.println("full text = " + fullText);
 *
 *  </pre>
 *
 *  <p>The following code shows how to transform successive lines of Java
 *  code in order to obtain the <i>full</i>
 *  text corresponding to the <i>lean</i> text of each line.
 *
 *  <pre>
 *
 *    int[] state = new int[1];
 *    state[0] = STextEngine.STATE_INITIAL;
 *    String leanText = "int i = 3; // first Java statement";
 *    String fullText = STextEngine.leanToFullText(STextEngine.PROC_JAVA, null, leanText, state);
 *    System.out.println("full text = " + fullText);
 *    leanText = "i += 4; // next Java statement";
 *    fullText = STextEngine.leanToFullText(STextEngine.PROC_JAVA, null, leanText, state);
 *    System.out.println("full text = " + fullText);
 *
 *  </pre>
 *
 *  <p>This class provides a user-oriented API but does not provide
 *  an actual implementation. The real work is done by the class
 *  {@link STextImpl}. Users of the API need not be concerned by, and
 *  should not depend upon, details of the implementation in
 *  <code>STextImpl</code>.
 *
 *  @author Matitiahu Allouche
 *
 */
public class STextEngine {
	/**
	 * Constant indicating a type of structured text processor adapted
	 * to processing property file statements. It expects the following
	 * format:
	 * <pre>
	 *  name=value
	 * </pre>
	 */
	public static final STextProcessor PROC_PROPERTY = STextStringProcessor.getProcessor("property"); //$NON-NLS-1$

	/**
	 * Constant indicating a type of structured text processor adapted
	 * to processing compound names.
	 * This type covers names made of one or more parts separated by underscores:
	 * <pre>
	 *  part1_part2_part3
	 * </pre>
	 */
	public static final STextProcessor PROC_UNDERSCORE = STextStringProcessor.getProcessor("underscore"); //$NON-NLS-1$

	/**
	 * Constant indicating a type of structured text processor adapted
	 * to processing comma-delimited lists, such as:
	 * <pre>
	 *  part1,part2,part3
	 * </pre>
	 */
	public static final STextProcessor PROC_COMMA_DELIMITED = STextStringProcessor.getProcessor("comma"); //$NON-NLS-1$

	/**
	 * Constant indicating a type of structured text processor adapted
	 * to processing strings with the following format:
	 * <pre>
	 *  system(user)
	 * </pre>
	 */
	public static final STextProcessor PROC_SYSTEM_USER = STextStringProcessor.getProcessor("system"); //$NON-NLS-1$

	/**
	 * Constant indicating a type of structured text processor adapted
	 * to processing directory and file paths.
	 */
	public static final STextProcessor PROC_FILE = STextStringProcessor.getProcessor("file"); //$NON-NLS-1$

	/**
	 *  Constant indicating a type of structured text processor adapted
	 *  to processing e-mail addresses.
	 */
	public static final STextProcessor PROC_EMAIL = STextStringProcessor.getProcessor("email"); //$NON-NLS-1$

	/**
	 *  Constant indicating a type of structured text processor adapted
	 *  to processing URLs.
	 */
	public static final STextProcessor PROC_URL = STextStringProcessor.getProcessor("url"); //$NON-NLS-1$

	/**
	 *  Constant indicating a type of structured text processor adapted
	 *  to processing regular expressions, possibly spanning more than one
	 *  line.
	 */
	public static final STextProcessor PROC_REGEXP = STextStringProcessor.getProcessor("regex"); //$NON-NLS-1$

	/**
	 *  Constant indicating a type of structured text processor adapted
	 *  to processing XPath expressions.
	 */
	public static final STextProcessor PROC_XPATH = STextStringProcessor.getProcessor("xpath"); //$NON-NLS-1$

	/**
	 *  Constant indicating a type of structured text processor adapted
	 *  to processing Java code, possibly spanning more than one line.
	 */
	public static final STextProcessor PROC_JAVA = STextStringProcessor.getProcessor("java"); //$NON-NLS-1$

	/**
	 *  Constant indicating a type of structured text processor adapted
	 *  to processing SQL statements, possibly spanning more than one line.
	 */
	public static final STextProcessor PROC_SQL = STextStringProcessor.getProcessor("sql"); //$NON-NLS-1$

	/**
	 *  Constant indicating a type of structured text processor adapted
	 *  to processing arithmetic expressions, possibly with a RTL base direction.
	 */
	public static final STextProcessor PROC_RTL_ARITHMETIC = STextStringProcessor.getProcessor("math"); //$NON-NLS-1$

	/**
	 *  Constant specifying that the base direction of a structured text is LTR.
	 *  The base direction may depend on whether the GUI is
	 *  {@link STextEnvironment#getMirrored mirrored} and may
	 *  may be different for Arabic and for Hebrew.
	 *  This constant can appear as value returned by the
	 *  {@link #getCurDirection getCurDirection} method.
	 */
	public static final int DIR_LTR = 0;

	/**
	 *  Constant specifying that the base direction of a structured text is RTL.
	 *  The base direction may depend on whether the GUI is
	 *  {@link STextEnvironment#getMirrored mirrored} and may
	 *  may be different for Arabic and for Hebrew.
	 *  This constant can appear as value returned by the
	 *  {@link #getCurDirection getCurDirection} method.
	 */
	public static final int DIR_RTL = 1;

	/**
	 *  Constant to use in the first element of the <code>state</code>
	 *  argument when calling most methods of this class
	 *  to indicate that there is no context of previous lines which
	 *  should be initialized before performing the operation.
	 */
	public static final int STATE_INITIAL = 0;

	private static final int[] EMPTY_INT_ARRAY = new int[0];

	/**
	 *  Prevent creation of a STextEngine instance
	 */
	private STextEngine() {
		// nothing to do
	}

	/** Add directional formatting characters to a structured text
	 *  to ensure correct presentation.
	 *
	 *  @param  processor designates a processor instance.
	 *          For more details, see above <a href="#processor">
	 *          How to Specify a Processor</a>.
	 *          <p>If this argument is <code>null</code>, this method
	 *          returns the <code>text</code> string.
	 *
	 *  @param  environment specifies an environment whose characteristics
	 *          may affect the processor's behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will assume a standard
	 *          environment as specified in
	 *          {@link STextEnvironment#DEFAULT}.
	 *
	 *  @param text is the structured text string.
	 *
	 *  @param  state can be used to specify that the <code>text</code>
	 *          argument is the continuation of text submitted in a
	 *          previous call and/or to receive information to pass to
	 *          continuation calls.
	 *          For more details, see <a href="#state">State</a> above.
	 *          <p>If all calls to this method are independent from one another,
	 *          this argument should be specified as <code>null</code>.
	 *
	 *  @return the structured text with directional formatting
	 *          characters added at proper locations to ensure correct
	 *          presentation.
	 */
	public static String leanToFullText(STextProcessor processor, STextEnvironment environment, String text, int[] state) {
		if (processor == null)
			return text;
		return STextImpl.leanToFullText(processor, environment, text, state);
	}

	/**
	 *  Given a <i>lean</i> string, compute the positions of each of its
	 *  characters within the corresponding <i>full</i> string.
	 *
	 *  @param  processor designates a processor instance.
	 *          For more details, see above <a href="#processor">
	 *          How to Specify a Processor</a>.
	 *          <p>If this argument is <code>null</code>, this method
	 *          returns an identity map.
	 *
	 *  @param  environment specifies an environment whose characteristics
	 *          may affect the processor's behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will assume a standard
	 *          environment as specified in
	 *          {@link STextEnvironment#DEFAULT}.
	 *
	 *  @param text is the structured text string.
	 *
	 *  @param  state can be used to specify that the <code>text</code>
	 *          argument is the continuation of text submitted in a
	 *          previous call and/or to receive information to pass to
	 *          continuation calls.
	 *          For more details, see <a href="#state">State</a> above.
	 *          <p>If all calls to this method are independent from one another,
	 *          this argument should be specified as <code>null</code>.
	 *
	 *  @return an array of integers with one element for each of the characters
	 *          in the <code>text</code> argument, equal to the offset of the
	 *          corresponding character in the <i>full</i> string.
	 */
	public static int[] leanToFullMap(STextProcessor processor, STextEnvironment environment, String text, int[] state) {
		if (processor == null) {
			int[] map = new int[text.length()];
			for (int i = 0; i < map.length; i++)
				map[i] = i;
			return map;
		}
		return STextImpl.leanToFullMap(processor, environment, text, state);
	}

	/**
	 *  Given a <i>lean</i> string, compute the offsets of characters
	 *  before which directional formatting characters must be added
	 *  in order to ensure correct presentation.
	 *
	 *  <p>Only LRMs (for a string with LTR base direction) and RLMs (for
	 *  a string with RTL base direction) are considered. Leading and
	 *  trailing LRE, RLE and PDF which might be prefixed or suffixed
	 *  depending on the {@link STextEnvironment#getOrientation orientation} of the
	 *  GUI component used for display are not reflected in this method.
	 *
	 *  @param  processor designates a processor instance.
	 *          For more details, see above <a href="#processor">
	 *          How to Specify a Processor</a>.
	 *          <p>If this argument is <code>null</code>, this method
	 *          returns an empty array.
	 *
	 *  @param  environment specifies an environment whose characteristics
	 *          may affect the processor's behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will assume a standard
	 *          environment as specified in
	 *          {@link STextEnvironment#DEFAULT}.
	 *
	 *  @param text is the structured text string.
	 *
	 *  @param  state can be used to specify that the <code>text</code>
	 *          argument is the continuation of text submitted in a
	 *          previous call and/or to receive information to pass to
	 *          continuation calls.
	 *          For more details, see <a href="#state">State</a> above.
	 *          <p>If all calls to this method are independent from one another,
	 *          this argument should be specified as <code>null</code>.
	 *
	 *  @return an array of offsets to the characters in the <code>text</code>
	 *          argument before which directional marks must be
	 *          added to ensure correct presentation.
	 *          The offsets are sorted in ascending order.
	 */
	public static int[] leanBidiCharOffsets(STextProcessor processor, STextEnvironment environment, String text, int[] state) {
		if (processor == null)
			return EMPTY_INT_ARRAY;
		return STextImpl.leanBidiCharOffsets(processor, environment, text, state);
	}

	/**
	 *  Remove directional formatting characters which were added to a
	 *  structured text string to ensure correct presentation.
	 *
	 *  @param  processor designates a processor instance.
	 *          For more details, see above <a href="#processor">
	 *          How to Specify a Processor</a>.
	 *          <p>If this argument is <code>null</code>, this method
	 *          returns the <code>text</code> string.
	 *
	 *  @param  environment specifies an environment whose characteristics
	 *          may affect the processor's behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will assume a standard
	 *          environment as specified in
	 *          {@link STextEnvironment#DEFAULT}.
	 *
	 *  @param text is the structured text string including
	 *         directional formatting characters.
	 *
	 *  @param  state can be used to specify that the <code>text</code>
	 *          argument is the continuation of text submitted in a
	 *          previous call and/or to receive information to pass to
	 *          continuation calls.
	 *          For more details, see <a href="#state">State</a> above.
	 *          <p>If all calls to this method are independent from one another,
	 *          this argument should be specified as <code>null</code>.
	 *
	 *  @return the structured text string without directional formatting
	 *          characters which might have been added by processing it
	 *          with {@link #leanToFullText leanToFullText}.
	 *
	 */
	public static String fullToLeanText(STextProcessor processor, STextEnvironment environment, String text, int[] state) {
		if (processor == null)
			return text;
		return STextImpl.fullToLeanText(processor, environment, text, state);
	}

	/**
	 *  Given a <i>full</i> string, compute the positions of each of its
	 *  characters within the corresponding <i>lean</i> string.
	 *
	 *  @param  processor designates a processor instance.
	 *          For more details, see above <a href="#processor">
	 *          How to Specify a Processor</a>.
	 *          <p>If this argument is <code>null</code>, this method
	 *          returns an identity map.
	 *
	 *  @param  environment specifies an environment whose characteristics
	 *          may affect the processor's behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will assume a standard
	 *          environment as specified in
	 *          {@link STextEnvironment#DEFAULT}.
	 *
	 *  @param  text is the structured text string including
	 *          directional formatting characters.
	 *
	 *  @param  state can be used to specify that the <code>text</code>
	 *          argument is the continuation of text submitted in a
	 *          previous call and/or to receive information to pass to
	 *          continuation calls.
	 *          For more details, see <a href="#state">State</a> above.
	 *          <p>If all calls to this method are independent from one another,
	 *          this argument should be specified as <code>null</code>.
	 *
	 *  @return an array of integers with one element for each of the characters
	 *          in the <code>text</code> argument, equal to the offset of the
	 *          corresponding character in the <i>lean</i> string.
	 *          If there is no corresponding
	 *          character in the <i>lean</i> string (because the
	 *          specified character is a directional formatting character
	 *          added when invoking {@link #leanToFullText leanToFullText}),
	 *          the value returned for this character is -1.
	 */
	public static int[] fullToLeanMap(STextProcessor processor, STextEnvironment environment, String text, int[] state) {
		if (processor == null) {
			int[] map = new int[text.length()];
			for (int i = 0; i < map.length; i++)
				map[i] = i;
			return map;
		}
		return STextImpl.fullToLeanMap(processor, environment, text, state);
	}

	/**
	 *  Given a <i>full</i> string, return the offsets of characters
	 *  which are directional formatting characters that have been added
	 *  in order to ensure correct presentation.
	 *
	 *  <p>LRMs (for a string with LTR base direction), RLMs (for
	 *  a string with RTL base direction) are considered as well as
	 *  leading and trailing LRE, RLE and PDF which might be prefixed or suffixed
	 *  depending on the {@link STextEnvironment#getOrientation orientation}
	 *  of the GUI component used for display.
	 *
	 *  @param  processor designates a processor instance.
	 *          For more details, see above <a href="#processor">
	 *          How to Specify a Processor</a>.
	 *          <p>If this argument is <code>null</code>, this method
	 *          returns an empty array.
	 *
	 *  @param  environment specifies an environment whose characteristics
	 *          may affect the processor's behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will assume a standard
	 *          environment as specified in
	 *          {@link STextEnvironment#DEFAULT}.
	 *
	 *  @param  text is the structured text string including
	 *          directional formatting characters.
	 *
	 *  @param  state can be used to specify that the <code>text</code>
	 *          argument is the continuation of text submitted in a
	 *          previous call and/or to receive information to pass to
	 *          continuation calls.
	 *          For more details, see <a href="#state">State</a> above.
	 *          <p>If all calls to this method are independent from one another,
	 *          this argument should be specified as <code>null</code>.
	 *
	 *  @return an array of offsets to the characters in the <code>text</code>
	 *          argument which are directional formatting characters
	 *          added to ensure correct presentation.
	 *          The offsets are sorted in ascending order.
	 */
	public static int[] fullBidiCharOffsets(STextProcessor processor, STextEnvironment environment, String text, int[] state) {
		if (processor == null)
			return EMPTY_INT_ARRAY;
		return STextImpl.fullBidiCharOffsets(processor, environment, text, state);
	}

	/**
	 *  Get the base direction of a structured text.
	 *  This base direction may depend on
	 *  whether the text contains Arabic or Hebrew words
	 *  (if it contains both, the first Arabic or Hebrew letter in the
	 *  text determines which is the governing script) and on
	 *  whether the GUI is {@link STextEnvironment#getMirrored mirrored}.
	 *
	 *  @param  processor designates a processor instance.
	 *          For more details, see above <a href="#processor">
	 *          How to Specify a Processor</a>.
	 *          <p>If this argument is <code>null</code>, this method
	 *          returns {@link #DIR_LTR}.
	 *
	 *  @param  environment specifies an environment whose characteristics
	 *          may affect the processor's behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will assume a standard
	 *          environment as specified in
	 *          {@link STextEnvironment#DEFAULT}.
	 *
	 *  @param  text is the structured text string.
	 *
	 *  @return the base direction of the structured text.
	 *          It is one of the values {@link #DIR_LTR}
	 *          or {@link #DIR_RTL}.
	 */
	public static int getCurDirection(STextProcessor processor, STextEnvironment environment, String text) {
		if (processor == null)
			return DIR_LTR;
		return STextImpl.getCurDirection(processor, environment, text, null);
	}

}
