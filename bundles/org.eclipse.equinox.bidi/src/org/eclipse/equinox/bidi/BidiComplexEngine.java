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

import org.eclipse.equinox.bidi.custom.BidiComplexFeatures;
import org.eclipse.equinox.bidi.internal.BidiComplexImpl;

/**
 *  This class acts as a mediator between applications and complex
 *  expression processors.
 *  The purpose of complex expression processors is to add directional
 *  formatting characters to ensure correct display.
 *  This class shields applications from the
 *  intricacies of complex expression processors.
 *  <p>
 *  For a general introduction to complex expressions, see
 *  {@link <a href="package-summary.html">
 *  the package documentation</a>}.
 *
 *  <h2><a name="processor">How to Specify a Processor</a></h2>
 *
 *  <p>All the methods in this class have a first argument which
 *  designates a type of processor.
 *
 *  <p>It can be specified as a string (usually one of the
 *  literals to be found in {@link IBidiComplexExpressionTypes}
 *  or as an instance of {@link custom.IBidiComplexProcessor}.
 *
 *  <p>Such an instance can be obtained using the
 *  {@link custom.BidiComplexStringProcessor#getProcessor getProcessor}
 *  method for the registered processors, or by instantiating a private processor.
 *
 *  <p>When the same processor is used in multiple calls, it may be
 *  beneficial to obtain a reference to the processor and to use it
 *  in following calls, rather than to specify the processor by its
 *  type expressed as a string, which necessitates a registry search
 *  for each call.
 *
 *  <p>A processor reference is also the only way to examine the
 *  features of a processor by calling its
 *  {@link custom.IBidiComplexProcessor#getFeatures getFeatures} method.
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
 *  <p>The following code shows how to transform a certain type of complex
 *  expression (directory and file paths) in order to obtain the <i>full</i>
 *  text corresponding to the <i>lean</i> text of such an expression.
 *
 *  <pre>
 *
 *    String leanText = "D:\\\u05d0\u05d1\\\u05d2\\\u05d3.ext";
 *    String fullText = BidiComplexEngine.leanToFullText(IBidiComplexExpressionTypes.FILE, null, null, leanText, null);
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
 *    state[0] = BidiComplexEngine.STATE_INITIAL;
 *    String leanText = "int i = 3; // first Java statement";
 *    String fullText = BidiComplexEngine.leanToFullText(IBidiComplexExpressionTypes.JAVA, null, null, leanText, state);
 *    System.out.println("full text = " + fullText);
 *    leanText = "i += 4; // next Java statement";
 *    fullText = BidiComplexEngine.leanToFullText(IBidiComplexExpressionTypes.JAVA, null, null, leanText, state);
 *    System.out.println("full text = " + fullText);
 *
 *  </pre>
 *
 *  <p>This class provides a user-oriented API but does not provide
 *  an actual implementation. The real work is done by the class
 *  {@link BidiComplexImpl}. Users of the API need not be concerned by, and
 *  should not depend upon, details of the implementation in
 *  <code>BidiComplexImpl</code>.
 *
 *  @author Matitiahu Allouche
 *
 */
public class BidiComplexEngine {
	/**
	 *  Constant to use in the first element of the <code>state</code>
	 *  argument when calling most methods of this class
	 *  to indicate that there is no context of previous lines which
	 *  should be initialized before performing the operation.
	 */
	public static final int STATE_INITIAL = 0;

	private static final int[] EMPTY_INT_ARRAY = new int[0];

	/**
	 *  Prevent creation of a BidiComplexEngine instance
	 */
	private BidiComplexEngine() {
		// nothing to do
	}

	/** Add directional formatting characters to a complex expression
	 *  to ensure correct presentation.
	 *
	 *  @param  processor designates one of the registered processors.
	 *          It can be a string containing a keyword according to
	 *          the processor type, or a processor reference.
	 *          For more details, see above <a href="#processor">
	 *          How to Specify a Processor</a>.
	 *          <p>If this argument is <code>null</code>, this method
	 *          returns the <code>text</code> string.
	 *
	 *  @param  features specifies features that affect the processor's
	 *          behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will use its standard features
	 *          (as returned by the processor
	 *          {@link custom.IBidiComplexProcessor#getFeatures getFeatures}
	 *          method).
	 *
	 *  @param  environment specifies an environment whose characteristics
	 *          may affect the processor's behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will assume a standard
	 *          environment as specified in
	 *          {@link BidiComplexEnvironment#DEFAULT}.
	 *
	 *  @param text is the text of the complex expression.
	 *
	 *  @param  state can be used to specify that the <code>text</code>
	 *          argument is the continuation of text submitted in a
	 *          previous call and/or to receive information to pass to
	 *          continuation calls.
	 *          For more details, see <a href="#state">State</a> above.
	 *          <p>If all calls to this method are independent from one another,
	 *          this argument should be specified as <code>null</code>.
	 *
	 *  @return the complex expression with directional formatting
	 *          characters added at proper locations to ensure correct
	 *          presentation.
	 */
	public static String leanToFullText(Object processor, BidiComplexFeatures features, BidiComplexEnvironment environment, String text, int[] state) {
		if (processor == null)
			return text;
		return BidiComplexImpl.leanToFullText(processor, features, environment, text, state);
	}

	/**
	 *  Given a <i>lean</i> string, compute the positions of each of its
	 *  characters within the corresponding <i>full</i> string.
	 *
	 *  @param  processor designates one of the registered processors.
	 *          It can be a string containing a keyword according to
	 *          the processor type, or a processor reference.
	 *          For more details, see above <a href="#processor">
	 *          How to Specify a Processor</a>.
	 *          <p>If this argument is <code>null</code>, this method
	 *          returns an identity map.
	 *
	 *  @param  features specifies features that affect the processor's
	 *          behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will use its standard features
	 *          (as returned by the processor
	 *          {@link custom.IBidiComplexProcessor#getFeatures getFeatures}
	 *          method).
	 *
	 *  @param  environment specifies an environment whose characteristics
	 *          may affect the processor's behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will assume a standard
	 *          environment as specified in
	 *          {@link BidiComplexEnvironment#DEFAULT}.
	 *
	 *  @param text is the text of the complex expression.
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
	public static int[] leanToFullMap(Object processor, BidiComplexFeatures features, BidiComplexEnvironment environment, String text, int[] state) {
		if (processor == null) {
			int[] map = new int[text.length()];
			for (int i = 0; i < map.length; i++)
				map[i] = i;
			return map;
		}
		return BidiComplexImpl.leanToFullMap(processor, features, environment, text, state);
	}

	/**
	 *  Given a <i>lean</i> string, compute the offsets of characters
	 *  before which directional formatting characters must be added
	 *  in order to ensure correct presentation.
	 *
	 *  <p>Only LRMs (for an expression with LTR base direction) and RLMs (for
	 *  an expression with RTL base direction) are considered. Leading and
	 *  trailing LRE, RLE and PDF which might be prefixed or suffixed
	 *  depending on the {@link BidiComplexEnvironment#getOrientation orientation} of the
	 *  GUI component used for display are not reflected in this method.
	 *
	 *  @param  processor designates one of the registered processors.
	 *          It can be a string containing a keyword according to
	 *          the processor type, or a processor reference.
	 *          For more details, see above <a href="#processor">
	 *          How to Specify a Processor</a>.
	 *          <p>If this argument is <code>null</code>, this method
	 *          returns an empty array.
	 *
	 *  @param  features specifies features that affect the processor's
	 *          behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will use its standard features
	 *          (as returned by the processor
	 *          {@link custom.IBidiComplexProcessor#getFeatures getFeatures}
	 *          method).
	 *
	 *  @param  environment specifies an environment whose characteristics
	 *          may affect the processor's behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will assume a standard
	 *          environment as specified in
	 *          {@link BidiComplexEnvironment#DEFAULT}.
	 *
	 *  @param text is the text of the complex expression.
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
	public static int[] leanBidiCharOffsets(Object processor, BidiComplexFeatures features, BidiComplexEnvironment environment, String text, int[] state) {
		if (processor == null)
			return EMPTY_INT_ARRAY;
		return BidiComplexImpl.leanBidiCharOffsets(processor, features, environment, text, state);
	}

	/**
	 *  Remove directional formatting characters which were added to a
	 *  complex expression to ensure correct presentation.
	 *
	 *  @param  processor designates one of the registered processors.
	 *          It can be a string containing a keyword according to
	 *          the processor type, or a processor reference.
	 *          For more details, see above <a href="#processor">
	 *          How to Specify a Processor</a>.
	 *          <p>If this argument is <code>null</code>, this method
	 *          returns the <code>text</code> string.
	 *
	 *  @param  features specifies features that affect the processor's
	 *          behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will use its standard features
	 *          (as returned by the processor
	 *          {@link custom.IBidiComplexProcessor#getFeatures getFeatures}
	 *          method).
	 *
	 *  @param  environment specifies an environment whose characteristics
	 *          may affect the processor's behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will assume a standard
	 *          environment as specified in
	 *          {@link BidiComplexEnvironment#DEFAULT}.
	 *
	 *  @param text is the text of the complex expression including
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
	 *  @return the complex expression without directional formatting
	 *          characters which might have been added by processing it
	 *          with {@link #leanToFullText leanToFullText}.
	 *
	 */
	public static String fullToLeanText(Object processor, BidiComplexFeatures features, BidiComplexEnvironment environment, String text, int[] state) {
		if (processor == null)
			return text;
		return BidiComplexImpl.fullToLeanText(processor, features, environment, text, state);
	}

	/**
	 *  Given a <i>full</i> string, compute the positions of each of its
	 *  characters within the corresponding <i>lean</i> string.
	 *
	 *  @param  processor designates one of the registered processors.
	 *          It can be a string containing a keyword according to
	 *          the processor type, or a processor reference.
	 *          For more details, see above <a href="#processor">
	 *          How to Specify a Processor</a>.
	 *          <p>If this argument is <code>null</code>, this method
	 *          returns an identity map.
	 *
	 *  @param  features specifies features that affect the processor's
	 *          behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will use its standard features
	 *          (as returned by the processor
	 *          {@link custom.IBidiComplexProcessor#getFeatures getFeatures}
	 *          method).
	 *
	 *  @param  environment specifies an environment whose characteristics
	 *          may affect the processor's behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will assume a standard
	 *          environment as specified in
	 *          {@link BidiComplexEnvironment#DEFAULT}.
	 *
	 *  @param  text is the text of the complex expression including
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
	public static int[] fullToLeanMap(Object processor, BidiComplexFeatures features, BidiComplexEnvironment environment, String text, int[] state) {
		if (processor == null) {
			int[] map = new int[text.length()];
			for (int i = 0; i < map.length; i++)
				map[i] = i;
			return map;
		}
		return BidiComplexImpl.fullToLeanMap(processor, features, environment, text, state);
	}

	/**
	 *  Given a <i>full</i> string, return the offsets of characters
	 *  which are directional formatting characters that have been added
	 *  in order to ensure correct presentation.
	 *
	 *  <p>LRMs (for an expression with LTR base direction), RLMs (for
	 *  an expression with RTL base direction) are considered as well as
	 *  leading and trailing LRE, RLE and PDF which might be prefixed or suffixed
	 *  depending on the {@link BidiComplexEnvironment#getOrientation orientation}
	 *  of the GUI component used for display.
	 *
	 *  @param  processor designates one of the registered processors.
	 *          It can be a string containing a keyword according to
	 *          the processor type, or a processor reference.
	 *          For more details, see above <a href="#processor">
	 *          How to Specify a Processor</a>.
	 *          <p>If this argument is <code>null</code>, this method
	 *          returns an empty array.
	 *
	 *  @param  features specifies features that affect the processor's
	 *          behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will use its standard features
	 *          (as returned by the processor
	 *          {@link custom.IBidiComplexProcessor#getFeatures getFeatures}
	 *          method).
	 *
	 *  @param  environment specifies an environment whose characteristics
	 *          may affect the processor's behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will assume a standard
	 *          environment as specified in
	 *          {@link BidiComplexEnvironment#DEFAULT}.
	 *
	 *  @param  text is the text of the complex expression including
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
	public static int[] fullBidiCharOffsets(Object processor, BidiComplexFeatures features, BidiComplexEnvironment environment, String text, int[] state) {
		if (processor == null)
			return EMPTY_INT_ARRAY;
		return BidiComplexImpl.fullBidiCharOffsets(processor, features, environment, text, state);
	}

	/**
	 *  Get the base direction of a complex expression.
	 *  This base direction may depend on
	 *  whether the expression contains Arabic or Hebrew words
	 *  (if it contains both, the first Arabic or Hebrew letter in the
	 *  expression determines which is the governing script) and on
	 *  whether the GUI is {@link BidiComplexEnvironment#getMirrored mirrored}.
	 *
	 *  @param  processor designates one of the registered processors.
	 *          It can be a string containing a keyword according to
	 *          the processor type, or a processor reference.
	 *          For more details, see above <a href="#processor">
	 *          How to Specify a Processor</a>.
	 *          <p>If this argument is <code>null</code>, this method
	 *          returns {@link BidiComplexFeatures#DIR_LTR}.
	 *
	 *  @param  features specifies features that affect the processor's
	 *          behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will use its standard features
	 *          (as returned by the processor
	 *          {@link custom.IBidiComplexProcessor#getFeatures getFeatures}
	 *          method).
	 *
	 *  @param  environment specifies an environment whose characteristics
	 *          may affect the processor's behavior.
	 *          <p>This argument may be specified as <code>null</code>,
	 *          in which case the processor will assume a standard
	 *          environment as specified in
	 *          {@link BidiComplexEnvironment#DEFAULT}.
	 *
	 *  @param  text is the text of the complex expression.
	 *
	 *  @return the base direction of the complex expression.
	 *          It is one of the values {@link BidiComplexFeatures#DIR_LTR}
	 *          or {@link BidiComplexFeatures#DIR_RTL}.
	 */
	public static int getCurDirection(Object processor, BidiComplexFeatures features, BidiComplexEnvironment environment, String text) {
		return BidiComplexImpl.getCurDirection(processor, features, environment, text, null);
	}

}
