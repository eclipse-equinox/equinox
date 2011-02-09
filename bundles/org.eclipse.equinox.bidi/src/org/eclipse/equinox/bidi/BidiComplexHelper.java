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

import org.eclipse.equinox.bidi.custom.BidiComplexStringProcessor;
import org.eclipse.equinox.bidi.custom.IBidiComplexProcessor;
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
 *  <h2>Code Sample</h2>
 *
 *  <p>The following code shows how to instantiate a BidiComplexHelper adapted for a
 *  certain type of complex expression (directory and file paths), and how
 *  to obtain the <i>full</i> text corresponding to the <i>lean</i> text
 *  of such an expression.
 *
 *  <pre>
 *
 *    BidiComplexHelper helper = new BidiComplexHelper(IBidiComplexExpressionTypes.FILE);
 *    String leanText = "D:\\\u05d0\u05d1\\\u05d2\\\u05d3.ext";
 *    String fullText = helper.leanToFullText(leanText);
 *    System.out.println("full text = " + fullText);
 *
 *  </pre>
 *  This class provides a user-oriented API but does not provides
 *  an actual implementation. The real work is done by the class
 *  {@link BidiComplexImpl}. Users of the API need not be concerned by, and
 *  should not depend upon, details of the implementation by
 *  <code>BidiComplexImpl</code>.
 *
 *  @author Matitiahu Allouche
 *
 */
public class BidiComplexHelper {
	/**
	 *  Constant to use as <code>initState</code> argument when calling
	 *  {@link #leanToFullText(java.lang.String, int) leanToFullText} or
	 *  {@link #fullToLeanText(java.lang.String, int) fullToLeanText}
	 *  to indicate that there is no context of previous lines which
	 *  should be initialized before performing the operation.
	 */
	public static final int STATE_NOTHING_GOING = -1;

	private static final int[] EMPTY_INT_ARRAY = new int[0];

	/**
	 *  Reference to the {@link BidiComplexImpl} instance which accomplishes
	 *  most of the work. This member should be accessed only by
	 *  complex expression processors (implementing {@link IBidiComplexProcessor}).
	 */
	public BidiComplexImpl impl;

	/**
	 *  Create a <code>BidiComplexHelper</code> instance which does nothing tangible
	 *  but does it quickly. We will call it a <i>no-op helper</i>.
	 *  The {@link #BidiComplexHelper() no-op helper} does not modify text submitted to it, and can be
	 *  used when it is known that no complex expression processing
	 *  is needed, for instance because no bidi text is expected. With this
	 *  helper, the cost of invoking the complex expression API is minimal.
	 */
	public BidiComplexHelper() {
		// let impl stay null
	}

	/**
	 *  Create a <code>BidiComplexHelper</code> instance to process expressions
	 *  of type <code>type</code> with a
	 *  {@link BidiComplexEnvironment#DEFAULT DEFAULT} environment.
	 *
	 *  @param type represents the type of the complex expression. It must
	 *          be one of the values in {@link IBidiComplexExpressionTypes}, or a type
	 *          added in a plug-in extension.
	 *
	 *  @throws IllegalArgumentException if the <code>type</code> is not
	 *          supported.
	 */
	public BidiComplexHelper(String type) {
		this(type, null);
	}

	/**
	 *  Create a <code>BidiComplexHelper</code> instance to process expressions
	 *  of type <code>type</code> operating under the environment
	 *  {@link BidiComplexEnvironment environment}.
	 *
	 *  @param type represents the type of the complex expression. It must
	 *          be one of the values in {@link IBidiComplexExpressionTypes}, or a type
	 *          added in a plug-in extension.
	 *
	 *  @param environment represents the environment where the complex expression
	 *          will be displayed. It is better to specify the environment
	 *          when instantiating a <code>BidiComplexHelper</code> than to specify
	 *          it later using {@link #setEnvironment setEnvironment}.<br>
	 *          <code>null</code> can be specified here for a
	 *          {@link BidiComplexEnvironment#DEFAULT DEFAULT} environment.
	 *
	 *  @throws IllegalArgumentException if the <code>type</code> is not
	 *          supported.
	 */
	public BidiComplexHelper(String type, BidiComplexEnvironment environment) {
		IBidiComplexProcessor processor = BidiComplexStringProcessor.getProcessor(type);
		if (processor == null) {
			throw new IllegalArgumentException("Invalid processor type!"); //$NON-NLS-1$
		}
		impl = new BidiComplexImpl(this, processor, environment);
	}

	/**
	 *  Create a <code>BidiComplexHelper</code> instance to process complex
	 *  expressions by means of a given processor <code>myProcessor</code>,
	 *  operating under the environment {@link BidiComplexEnvironment environment}.
	 *
	 *  @param myProcessor is a complex expression processor.
	 *
	 *  @param environment represents the environment where the complex expression
	 *          will be displayed. It is better to specify the environment
	 *          when instantiating a <code>BidiComplexHelper</code> than to specify
	 *          it later using {@link #setEnvironment setEnvironment}.<br>
	 *          <code>null</code> can be specified here for a
	 *          {@link BidiComplexEnvironment#DEFAULT DEFAULT} environment.
	 */
	public BidiComplexHelper(IBidiComplexProcessor myProcessor, BidiComplexEnvironment environment) {
		impl = new BidiComplexImpl(this, myProcessor, environment);
	}

	/** Add directional formatting characters to a complex expression
	 *  to ensure correct presentation.
	 *
	 *  @param text is the text of the complex expression.
	 *
	 *  @return the complex expression with directional formatting
	 *          characters added at proper locations to ensure correct
	 *          presentation.<br>
	 *          The {@link #BidiComplexHelper() no-op helper} returns <code>text</code>.
	 */
	public String leanToFullText(String text) {
		return leanToFullText(text, STATE_NOTHING_GOING);
	}

	/**
	 *  Add directional formatting characters to a complex expression
	 *  to ensure correct presentation.
	 *
	 *  @param  text is the text of the complex expression.
	 *
	 *  @param  initState specifies that the first parameter is the
	 *          continuation of text submitted in a previous call.
	 *          The <code>initState</code> of the present call must be
	 *          the final state obtained by calling
	 *          {@link #getFinalState() getFinalState}
	 *          after the previous call.
	 *          <br>If the present call is not a continuation of
	 *          text submitted in a previous call, the value
	 *          {@link #STATE_NOTHING_GOING} should be used as argument.
	 *
	 *  @return the complex expression with directional formatting
	 *          characters added at proper locations to ensure correct
	 *          presentation.<br>
	 *          The {@link #BidiComplexHelper() no-op helper} returns <code>text</code>.
	 *
	 *  @see #getFinalState getFinalState
	 */
	public String leanToFullText(String text, int initState) {
		if (impl == null)
			return text;
		return impl.leanToFullText(text, initState);
	}

	/**
	 *  Given a complex expression, get the offsets of characters before
	 *  which directional formatting characters must be added in order to
	 *  ensure correct presentation.
	 *  Only LRMs (for an expression with LTR base direction) and RLMs (for
	 *  an expression with RTL base direction) are considered. Leading and
	 *  trailing LRE, RLE and PDF which might be prefixed or suffixed
	 *  depending on the {@link BidiComplexEnvironment#orientation orientation} of the
	 *  GUI component used for display are not reflected in this method.
	 *  <p>
	 *  This method assumes that a successful call to
	 *  {@link #leanToFullText leanToFullText} has been performed, and it
	 *  returns the offsets relevant for the last text submitted.
	 *
	 *  @return an array of offsets to the characters in the last submitted
	 *          complex expression before which directional marks must be
	 *          added to ensure correct presentation.
	 *          The offsets are sorted in ascending order.<br>
	 *          The {@link #BidiComplexHelper() no-op helper} returns an array of 0 elements.
	 */
	public int[] leanBidiCharOffsets() {
		if (impl == null)
			return EMPTY_INT_ARRAY;
		return impl.leanBidiCharOffsets();
	}

	/**
	 *  Get the offsets of characters in the <i>full</i> text of a
	 *  complex expression corresponding to directional formatting
	 *  characters which have been added in order to ensure correct presentation.
	 *  LRMs (for an expression with LTR base direction), RLMs (for
	 *  an expression with RTL base direction) are considered as well as
	 *  leading and trailing LRE, RLE and PDF which might be prefixed or suffixed
	 *  depending on the {@link BidiComplexEnvironment#orientation orientation} of the
	 *  GUI component used for display.
	 *  <p>
	 *  This method assumes that a successful call to
	 *  {@link #leanToFullText leanToFullText} has been performed, and it
	 *  returns the offsets relevant for the last text submitted.
	 *
	 *  @return an array of offsets to the characters in the <i>full</i>
	 *          text of the last submitted complex expression which are
	 *          directional formatting characters added to ensure correct
	 *          presentation.
	 *          The offsets are sorted in ascending order.<br>
	 *          The {@link #BidiComplexHelper() no-op helper} returns an array of 0 elements.
	 */
	public int[] fullBidiCharOffsets() {
		if (impl == null)
			return EMPTY_INT_ARRAY;
		return impl.fullBidiCharOffsets();
	}

	/**
	 *  Remove directional formatting characters which were added to a
	 *  complex expression to ensure correct presentation.
	 *
	 *  @param text is the text of the complex expression including
	 *         directional formatting characters.
	 *
	 *  @return the complex expression without directional formatting
	 *          characters which might have been added by processing it
	 *          with {@link #leanToFullText leanToFullText}.<br>
	 *          The {@link #BidiComplexHelper() no-op helper} returns <code>text</code>
	 *
	 */
	public String fullToLeanText(String text) {
		return fullToLeanText(text, STATE_NOTHING_GOING);
	}

	/**
	 *  Remove directional formatting characters which were added to a
	 *  complex expression to ensure correct presentation.
	 *
	 *  @param text is the text of the complex expression including
	 *         directional formatting characters.
	 *
	 *  @param initState specifies that the first parameter is the
	 *         continuation of text submitted in a previous call.
	 *         The <code>initState</code> of the present call must be
	 *         the final state obtained by calling
	 *         {@link #getFinalState getFinalState} after the previous call.
	 *          <br>If the present call is not a continuation of
	 *          text submitted in a previous call, the value
	 *          {@link #STATE_NOTHING_GOING} should be used as argument.
	 *
	 *  @return the complex expression without directional formatting
	 *          characters which might have been added by processing it
	 *          with {@link #leanToFullText leanToFullText}.<br>
	 *          The {@link #BidiComplexHelper() no-op helper} returns <code>text</code>
	 *
	 *  @see #getFinalState getFinalState
	 */
	public String fullToLeanText(String text, int initState) {
		if (impl == null)
			return text;
		return impl.fullToLeanText(text, initState);
	}

	/**
	 *  Retrieve the final state achieved in a previous call to
	 *  {@link #leanToFullText leanToFullText} or
	 *  {@link #fullToLeanText fullToLeanText}.
	 *  This state is an opaque value which is meaningful only
	 *  within calls to the same complex expression processor.
	 *  The only externalized value is
	 *  {@link #STATE_NOTHING_GOING} which means that
	 *  there is nothing to remember from the last call.
	 *  <p>
	 *  The state should be used only for complex expressions which come
	 *  in parts, like when spanning multiple lines. The user can make
	 *  a separate call to
	 *  <code>leanToFullText</code> or <code>fullToLeanText</code> for each
	 *  line in the expression. The final state value retrieved after the
	 *  call for one line should be used as the initial state in the call
	 *  which processes the next line.
	 *  <p>
	 *  If a line within a complex expression has already been processed by
	 *  <code>leanToFullText</code> and the <i>lean</i> version of that line has
	 *  not changed, and its initial state has not changed either, the user
	 *  can be sure that the <i>full</i> version of that line is also
	 *  identical to the result of the previous processing.
	 *
	 *  @see #leanToFullText(String text, int initState)
	 *  @see #fullToLeanText(String text, int initState)
	 *
	 *  @return the last final state.<br>
	 *          The {@link #BidiComplexHelper() no-op helper} returns {@link #STATE_NOTHING_GOING}.
	 */
	public int getFinalState() {
		if (impl == null)
			return STATE_NOTHING_GOING;
		return impl.getFinalState();
	}

	/**
	 *  After transforming a <i>lean</i> string into a <i>full</i> string
	 *  using {@link #leanToFullText leanToFullText}, compute the index in the
	 *  <i>full</i> string of the character corresponding to the
	 *  character with the specified position in the <i>lean</i> string.
	 *
	 *  @param position is the index of a character in the <i>lean</i> string.
	 *          It must be a non-negative number smaller than the length
	 *          of the <i>lean</i> text.
	 *
	 *  @return the index of the corresponding character in the
	 *          <i>full</i> string.<br>
	 *          The {@link #BidiComplexHelper() no-op helper} returns <code>position</code>.
	 */
	public int leanToFullPos(int position) {
		if (impl == null)
			return position;
		return impl.leanToFullPos(position);
	}

	/**
	 *  After transforming a <i>lean</i> string into a <i>full</i> string
	 *  using {@link #leanToFullText leanToFullText}, compute the index in the
	 *  <i>lean</i> string of the character corresponding to the
	 *  character with the specified position in the <i>full</i> string.
	 *
	 *  @param position is the index of a character in the <i>full</i> string.
	 *          It must be a non-negative number smaller than the length
	 *          of the <i>full</i> text.
	 *
	 *  @return the index of the corresponding character in the
	 *          <i>lean</i> string. If there is no corresponding
	 *          character in the <i>lean</i> string (because the
	 *          specified character is a directional formatting character
	 *          added when invoking {@link #leanToFullText leanToFullText}),
	 *          the value returned will be that corresponding to the
	 *          next character which is not a directional formatting
	 *          character.<br>
	 *          If <code>position</code> corresponds to a directional formatting
	 *          character beyond all characters of the original
	 *          <i>lean</i> text, the value returned is the length of the
	 *          <i>lean</i> text.<br>
	 *          The {@link #BidiComplexHelper() no-op helper} returns <code>position</code>.
	 */
	public int fullToLeanPos(int position) {
		if (impl == null)
			return position;
		return impl.fullToLeanPos(position);
	}

	/**
	 *  Get the base direction of the complex expression last
	 *  submitted to {@link #leanToFullText leanToFullText}.
	 *  This base direction may depend on
	 *  whether the expression contains Arabic or Hebrew words
	 *  (if it contains both, the first Arabic or Hebrew letter in the
	 *  expression determines which is the governing script) and on
	 *  whether the GUI is {@link BidiComplexEnvironment#mirrored mirrored}.
	 *
	 *  @return the base direction of the last submitted complex
	 *          expression. It must be one of the values
	 *          {@link BidiComplexFeatures#DIR_LTR} or {@link BidiComplexFeatures#DIR_RTL}.<br>
	 *          The {@link #BidiComplexHelper() no-op helper} returns <code>DIR_LTR</code>.
	 */
	public int getCurDirection() {
		if (impl == null)
			return BidiComplexFeatures.DIR_LTR;
		return impl.getCurDirection();
	}

	/**
	 *  Get the current environment under which the <code>BidiComplexHelper</code>
	 *  operates.
	 *  This environment may have been specified in the constructor or
	 *  specified later using {@link #setEnvironment setEnvironment}.<br>
	 *
	 *  @return the current environment.<br>
	 *          The {@link #BidiComplexHelper() no-op helper} returns a {@link BidiComplexEnvironment#DEFAULT DEFAULT}
	 *          environment.
	 *
	 *  @see #setEnvironment setEnvironment
	 */
	public BidiComplexEnvironment getEnvironment() {
		if (impl == null)
			return BidiComplexEnvironment.DEFAULT;
		return impl.getEnvironment();
	}

	/**
	 *  Specify the environment under which the <code>BidiComplexHelper</code>
	 *  must operate.
	 *  <p>
	 *  <b>Note</b> that calling this method causes the processor
	 *  associated with this instance of <code>BidiComplexHelper</code>
	 *  to re-initialize its features. The effect of a previous call
	 *  to {@link #setFeatures(BidiComplexFeatures) setFeatures} is lost.<br>
	 *  The {@link #BidiComplexHelper() no-op helper} does nothing.
	 *
	 *  @see #getEnvironment getEnvironment
	 *  @see IBidiComplexProcessor#updateEnvironment IBidiComplexProcessor.updateEnvironment
	 */
	public void setEnvironment(BidiComplexEnvironment environment) {
		if (impl != null)
			impl.setEnvironment(environment);
	}

	/**
	 *  Get the current features of the processor associated with this
	 *  <code>BidiComplexHelper</code> instance.
	 *
	 *  @return the current features.<br>
	 *          The {@link #BidiComplexHelper() no-op helper} returns {@link BidiComplexFeatures#DEFAULT DEFAULT}
	 *          features.
	 *
	 *  @see #setFeatures setFeatures
	 */
	public BidiComplexFeatures getFeatures() {
		if (impl == null)
			return BidiComplexFeatures.DEFAULT;
		return impl.getFeatures();
	}

	/**
	 *  Specify the features to be applied to the processor associated with this
	 *  <code>BidiComplexHelper</code> instance.
	 *  Note that the value of {@link BidiComplexFeatures#specialsCount specialsCount}
	 *  cannot be changed (the new value will be ignored).<br>
	 *  The {@link #BidiComplexHelper() no-op helper} does nothing.
	 *
	 *  @see #getFeatures getFeatures
	 */
	public void setFeatures(BidiComplexFeatures features) {
		if (impl != null)
			impl.setFeatures(features);
	}

	/**
	 *  This method can be called from within
	 *  {@link IBidiComplexProcessor#indexOfSpecial indexOfSpecial} or
	 *  {@link IBidiComplexProcessor#processSpecial processSpecial} in implementations
	 *  of {@link IBidiComplexProcessor} to retrieve the bidirectional class of
	 *  characters in the <i>lean</i> text.
	 *
	 *  @param index position of the character in the <i>lean</i> text.
	 *          It must be a non-negative number smaller than the length
	 *          of the <i>lean</i> text.
	 *
	 *  @return the bidirectional class of the character. It is one of the
	 *          values which can be returned by
	 *          <code>java.lang.Character#getDirectionality</code>.
	 *          However, it is recommended to use <code>getDirProp</code>
	 *          rather than <code>java.lang.Character.getDirectionality</code>
	 *          since <code>getDirProp</code> manages a cache of character
	 *          properties and so can be more efficient than calling the
	 *          java.lang.Character method.<br>
	 *          The {@link #BidiComplexHelper() no-op helper} returns
	 *          <code>Character.DIRECTIONALITY_UNDEFINED</code>.
	 */
	public byte getDirProp(int index) {
		if (impl == null)
			return Character.DIRECTIONALITY_UNDEFINED;
		return impl.getDirProp(index);
	}

	/**
	 *  This method can be called from within
	 *  {@link IBidiComplexProcessor#indexOfSpecial indexOfSpecial} or
	 *  {@link IBidiComplexProcessor#processSpecial processSpecial} in implementations
	 *  of {@link IBidiComplexProcessor} to set or override the bidirectional
	 *  class of characters in the <i>lean</i> text.
	 *
	 *  @param index position of the character in the <i>lean</i> text.
	 *          It must be a non-negative number smaller than the length
	 *          of the <i>lean</i> text.
	 *
	 *  @param  dirProp bidirectional class of the character. It is one of the
	 *          values which can be returned by
	 *          <code>java.lang.Character.getDirectionality</code>.<br>
	 *          The {@link #BidiComplexHelper() no-op helper} does nothing.
	 */
	public void setDirProp(int index, byte dirProp) {
		if (impl != null)
			impl.getDirProp(index);
	}

	/**
	 *  This method can be called from within
	 *  {@link IBidiComplexProcessor#indexOfSpecial indexOfSpecial} or
	 *  {@link IBidiComplexProcessor#processSpecial processSpecial} in implementations
	 *  of {@link IBidiComplexProcessor}
	 *  to specify that a mark character must be added before the character
	 *  at the specified position of the <i>lean</i> text when generating the
	 *  <i>full</i> text. The mark character will be LRM for complex expressions
	 *  with a LTR base direction, and RLM for complex expressions with RTL
	 *  base direction. The mark character is not added physically by this
	 *  method, but its position is noted and will be used when generating
	 *  the <i>full</i> text.
	 *
	 *  @param  offset position of the character in the <i>lean</i> text.
	 *          It must be a non-negative number smaller than the length
	 *          of the <i>lean</i> text.
	 *          For the benefit of efficiency, it is better to insert
	 *          multiple marks in ascending order of the offsets.<br>
	 *          The {@link #BidiComplexHelper() no-op helper} does nothing.
	 */
	public void insertMark(int offset) {
		if (impl != null)
			impl.insertMark(offset);
	}

	/**
	 *  This method can be called from within
	 *  {@link IBidiComplexProcessor#indexOfSpecial indexOfSpecial} or
	 *  {@link IBidiComplexProcessor#processSpecial processSpecial} in
	 *  implementations of {@link IBidiComplexProcessor} to add a
	 *  directional mark before an
	 *  operator if needed for correct display, depending on the
	 *  base direction of the expression and on the class of the
	 *  characters in the <i>lean</i> text preceding and following
	 *  the operator itself.<br>
	 *  The {@link #BidiComplexHelper() no-op helper} does nothing.
	 *
	 *  @param  operLocation offset of the operator in the <i>lean</i> text.
	 *          It must be a non-negative number smaller than the length
	 *          of the <i>lean</i> text.
	 */
	public void processOperator(int operLocation) {
		if (impl != null)
			impl.processOperator(operLocation);
	}

	/**
	 *  This method can be called from within
	 *  {@link IBidiComplexProcessor#indexOfSpecial indexOfSpecial} or
	 *  {@link IBidiComplexProcessor#processSpecial processSpecial} in
	 *  implementations of {@link IBidiComplexProcessor} to
	 *  set the final state which should be used for the next call to
	 *  {@link #leanToFullText(java.lang.String, int)}.<br>
	 *  The {@link #BidiComplexHelper() no-op helper} does nothing.
	 */
	public void setFinalState(int newState) {
		if (impl != null)
			impl.setFinalState(newState);
	}

}
