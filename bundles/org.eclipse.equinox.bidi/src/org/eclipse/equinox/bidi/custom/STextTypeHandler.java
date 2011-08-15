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
package org.eclipse.equinox.bidi.custom;

import org.eclipse.equinox.bidi.STextDirection;
import org.eclipse.equinox.bidi.advanced.ISTextExpert;
import org.eclipse.equinox.bidi.advanced.STextEnvironment;
import org.eclipse.equinox.bidi.internal.STextImpl;

/**
 *  Generic handler to be used as superclass (base class)
 *  for specific structured text handlers.
 *  <p>
 *  Here are some guidelines about how to write structured text
 *  handlers.
 *  <ul>
 *    <li>Handler instances may be accessed simultaneously by
 *        several threads. They should have no instance variables.</li>
 *    <li>The common logic uses handler methods to query the
 *        characteristics of the specific handler:
 *        <ul>
 *          <li>the separators which separate the structured text into
 *              tokens. See {@link #getSeparators getSeparators}.</li>
 *          <li>the direction which governs the display of tokens
 *              one after the other. See {@link #getDirection getDirection}.</li>
 *          <li>the number of special cases which need to be handled by
 *              code specific to that handler.
 *              See {@link #getSpecialsCount getSpecialsCount}.</li>
 *        </ul></li>
 *    <li>Before starting deeper analysis of the submitted text, the common
 *        logic gives to the handler a chance to shorten the processus by
 *        invoking its {@link #skipProcessing skipProcessing} method.</li>
 *    <li>The common logic then analyzes the text to segment it into tokens
 *        according to the appearance of separators (as retrieved using
 *        {@link #getSeparators getSeparators}).</li>
 *    <li>If the handler indicated a positive number of special cases as
 *        return value from its {@link #getSpecialsCount getSpecialsCount}
 *        method, the common logic will repeatedly invoke the handler's
 *        {@link #indexOfSpecial indexOfSpecial} method to let it signal the
 *        presence of special strings which may further delimit the source text.</li>
 *    <li>When such a special case is signalled by the handler, the common
 *        logic will call the handler's {@link #processSpecial processSpecial}
 *        method to give it the opportunity to handle it as needed. Typical
 *        actions that the handler may perform are to add directional marks
 *        inconditionally (by calling {@link #insertMark insertMark} or
 *        conditionally (by calling {@link #processSeparator processSeparator}).</li>
 *  </ul>
 *
 * @author Matitiahu Allouche
 */
public class STextTypeHandler {

	final private String separators;

	/**
	 * Creates a new instance of the STextTypeHandler class.
	 */
	public STextTypeHandler() {
		separators = ""; //$NON-NLS-1$
	}

	/**
	 * Creates a new instance of the STextTypeHandler class.
	 * @param separators string consisting of characters that split the text into fragments
	 */
	public STextTypeHandler(String separators) {
		this.separators = separators;
	}

	/**
	 * Locate occurrences of special strings within a structured text
	 * and return their indexes one after the other in successive calls.
	 * <p>
	 * This method is called repeatedly if the number of special cases 
	 * returned by {@link #getSpecialsCount} is greater than zero.
	 * </p><p>
	 * A handler handling special cases must override this method.
	 * </p>
	 * @param  environment the current environment, which may affect the behavior of
	 *         the handler. This parameter may be specified as
	 *         <code>null</code>, in which case the
	 *         {@link STextEnvironment#DEFAULT DEFAULT}
	 *         environment should be assumed.
	 *
	 * @param  text is the structured text string before
	 *         addition of any directional formatting characters.
	 *
	 * @param  charTypes is a parameter received by <code>indexOfSpecial</code>
	 *         uniquely to be used as argument for calls to methods which
	 *         need it.
	 *
	 * @param  offsets is a parameter received by <code>indexOfSpecial</code>
	 *         uniquely to be used as argument for calls to methods which
	 *         need it.
	 *
	 * @param  caseNumber number of the special case to locate.
	 *         This number varies from 1 to the number of special cases
	 *         returned by {@link #getSpecialsCount getSpecialsCount}
	 *         for this handler.
	 *         The meaning of this number is internal to the class
	 *         implementing <code>indexOfSpecial</code>.
	 *
	 * @param  fromIndex the index within <code>text</code> to start
	 *         the search from.
	 *
	 * @return the position where the start of the special case
	 *         corresponding to <code>caseNumber</code> was located.
	 *         The method must return the first occurrence of whatever
	 *         identifies the start of the special case starting from
	 *         <code>fromIndex</code>. The method does not have to check if
	 *         this occurrence appears within the scope of another special
	 *         case (e.g. a comment starting delimiter within the scope of
	 *         a literal or vice-versa).
	 *         <br>If no occurrence is found, the method must return -1.
	 *
	 * @throws IllegalStateException If not overridden, this method throws an
	 * <code>IllegalStateException</code>. This is appropriate behavior
	 * (and does not need to be overridden) for handlers whose
	 * number of special cases is zero, which means that
	 * <code>indexOfSpecial</code> should never be called for them.
	 */
	public int indexOfSpecial(STextEnvironment environment, String text, STextCharTypes charTypes, STextOffsets offsets, int caseNumber, int fromIndex) {
		// This method must be overridden by all subclasses with special cases.
		throw new IllegalStateException("A handler with specialsCount > 0 must have an indexOfSpecial() method."); //$NON-NLS-1$
	}

	/**
	 * This method handles special cases specific to this handler.
	 * It is called when a special case occurrence 
	 * is located by {@link #indexOfSpecial}.
	 * <p>
	 * If a special processing cannot be completed within a current call to
	 * <code>processSpecial</code> (for instance, a comment has been started
	 * in the current line but its end appears in a following line),
	 * <code>processSpecial</code> should specify a final state by
	 * putting its value in the first element of the <code>state</code>
	 * parameter.
	 * The meaning of this state is internal to the handler.
	 * On a later call, <code>processSpecial</code> will be called with that value 
	 * for parameter <code>caseNumber</code> and <code>-1</code> for parameter 
	 * <code>separLocation</code> and should perform whatever initializations are required
	 * depending on the state.
	 * </p><p>
	 * A handler handling special cases (with a number of
	 * special cases greater than zero) must override this method.
	 * </p>
	 * @param  environment the current environment, which may affect the behavior of
	 *         the handler. This parameter may be specified as
	 *         <code>null</code>, in which case the
	 *         {@link STextEnvironment#DEFAULT DEFAULT}
	 *         environment should be assumed.
	 *
	 * @param  text is the structured text string before
	 *         addition of any directional formatting characters.
	 *
	 * @param  charTypes is a parameter received by <code>processSpecial</code>
	 *         uniquely to be used as argument for calls to methods which
	 *         need it.
	 *
	 * @param  offsets is a parameter received by <code>processSpecial</code>
	 *         uniquely to be used as argument for calls to methods which
	 *         need it.
	 *
	 * @param  state is an integer array with at least one element.
	 *         If the handler needs to signal the occurrence of a
	 *         special case which must be passed to the next call to
	 *         <code>leanToFullText</code> (for instance, a comment or a
	 *         literal started but not closed in the current
	 *         <code>text</code>), it must put a value in the first element
	 *         of the <code>state</code> parameter.
	 *         This number must be >= 1 and less or equal to the number of special
	 *         cases returned by {@link #getSpecialsCount getSpecialsCount}
	 *         by this handler.
	 *         This number is passed back to the caller
	 *         and should be specified as <code>state</code> argument
	 *         in the next call to <code>leanToFullText</code> together
	 *         with the continuation text.
	 *         The meaning of this number is internal to the handler.
	 *
	 * @param  caseNumber number of the special case to handle.
	 *
	 * @param  separLocation the position returned by
	 *         {@link #indexOfSpecial indexOfSpecial}. In calls to
	 *         {@link ISTextExpert#leanToFullText leanToFullText} and other
	 *         methods of {@link ISTextExpert} specifying a  non-null
	 *         <code>state</code> parameter, <code>processSpecial</code> is
	 *         called when initializing the processing with the value of
	 *         <code>caseNumber</code> equal to the value returned in the
	 *         first element of <code>state</code> and the value of
	 *         <code>separLocation</code> equal to <code>-1</code>.
	 *
	 * @return the position after the scope of the special case ends.
	 *         For instance, the position after the end of a comment,
	 *         the position after the end of a literal.
	 *         <br>A value greater or equal to the length of <code>text</code>
	 *         means that there is no further occurrence of this case in the
	 *         current structured text.
	 *
	 * @throws IllegalStateException If not overridden, this method throws an
	 * <code>IllegalStateException</code>. This is appropriate behavior
	 * (and does not need to be overridden) for handlers whose
	 * number of special cases is zero, which means that
	 * <code>processSpecial</code> should never be called for them.
	 */
	public int processSpecial(STextEnvironment environment, String text, STextCharTypes charTypes, STextOffsets offsets, int[] state, int caseNumber, int separLocation) {
		// This method must be overridden by all subclasses with any special case.
		throw new IllegalStateException("A handler with specialsCount > 0 must have a processSpecial() method."); //$NON-NLS-1$
	}

	/**
	 * This method can be called from within {@link #indexOfSpecial} or
	 * {@link #processSpecial} in extensions of <code>STextTypeHandler</code>
	 * to specify that a mark character must be added before the character
	 * at the specified position of the <i>lean</i> text when generating the
	 * <i>full</i> text. The mark character will be LRM for structured text
	 * with a LTR base direction, and RLM for structured text with RTL
	 * base direction. The mark character is not added physically by this
	 * method, but its position is noted and will be used when generating
	 * the <i>full</i> text.
	 *
	 * @param  text is the structured text string received as
	 *         parameter to <code>indexOfSpecial</code> or
	 *         <code>processSpecial</code>.
	 *
	 * @param  charTypes is a parameter received by <code>indexOfSpecial</code>
	 *         or <code>processSpecial</code>, uniquely to be used as argument
	 *         for calls to <code>insertMark</code> and other methods used
	 *         by handlers.
	 *
	 * @param  offsets is a parameter received by <code>indexOfSpecial</code>
	 *         or <code>processSpecial</code>, uniquely to be used as argument
	 *         for calls to <code>insertMark</code> and other methods used
	 *         by handlers.
	 *
	 * @param  offset position of the character in the <i>lean</i> text.
	 *         It must be a non-negative number smaller than the length
	 *         of the <i>lean</i> text.
	 *         For the benefit of efficiency, it is better to insert
	 *         multiple marks in ascending order of the offsets.
	 */
	public static final void insertMark(String text, STextCharTypes charTypes, STextOffsets offsets, int offset) {
		offsets.insertOffset(charTypes, offset);
	}

	/**
	 * This method can be called from within {@link #indexOfSpecial} or
	 * {@link #processSpecial} in extensions of <code>STextTypeHandler</code> to add 
	 * a directional mark before a separator if needed for correct display, 
	 * depending on the base direction of the text and on the class of the
	 * characters in the <i>lean</i> text preceding and following the separator itself.
	 * <p>
	 * The logic implemented in this method considers the text before
	 * <code>separLocation</code> and the text following it. If, and only if,
	 * a directional mark is needed to insure that the two parts of text
	 * will be laid out according to the base direction, a mark will be
	 * added when generating the <i>full</i> text.
	 * </p>
	 * @param  text is the structured text string received as
	 *         parameter to <code>indexOfSpecial</code> or
	 *         <code>processSpecial</code>.
	 *
	 * @param  charTypes is a parameter received by <code>indexOfSpecial</code>
	 *         or <code>processSpecial</code>, uniquely to be used as argument
	 *         for calls to <code>processSeparator</code> and other methods used
	 *         by handlers.
	 *
	 * @param  offsets is a parameter received by <code>indexOfSpecial</code>
	 *         or <code>processSpecial</code>, uniquely to be used as argument
	 *         for calls to <code>processSeparator</code> and other methods used
	 *         by handlers.
	 *
	 * @param  separLocation offset of the separator in the <i>lean</i> text.
	 *         It must be a non-negative number smaller than the length
	 *         of the <i>lean</i> text.
	 */
	public static final void processSeparator(String text, STextCharTypes charTypes, STextOffsets offsets, int separLocation) {
		STextImpl.processSeparator(text, charTypes, offsets, separLocation);
	}

	/**
	 * Indicate the separators to use for the current handler.
	 * This method is invoked before starting the processing.
	 * <p>
	 * If no separators are specified, this method returns an empty string.
	 * </p>
	 * @param  environment the current environment, which may affect the behavior of
	 *         the handler. This parameter may be specified as
	 *         <code>null</code>, in which case the
	 *         {@link STextEnvironment#DEFAULT DEFAULT}
	 *         environment should be assumed.
	 *
	 * @return a string grouping one-character separators which separate
	 *         the structured text into tokens.
	 */
	public String getSeparators(STextEnvironment environment) {
		return separators;
	}

	/**
	 * Indicate the base text direction appropriate for an instance of structured text.
	 * This method is invoked before starting the processing.
	 * <p>
	 * If not overridden, this method returns <code>DIR_LTR</code>.
	 * </p>
	 * @param  environment the current environment, which may affect the behavior of
	 *         the handler. This parameter may be specified as
	 *         <code>null</code>, in which case the
	 *         {@link STextEnvironment#DEFAULT DEFAULT}
	 *         environment should be assumed.
	 *
	 * @param  text is the structured text string to process.
	 *
	 * @return the base direction of the structured text. This direction
	 *         may not be the same depending on the environment and on
	 *         whether the structured text contains Arabic or Hebrew
	 *         letters.<br>
	 *         The value returned is either
	 *         {@link STextDirection#DIR_LTR DIR_LTR} or {@link STextDirection#DIR_RTL DIR_RTL}.
	 */
	public int getDirection(STextEnvironment environment, String text) {
		return STextDirection.DIR_LTR;
	}

	/**
	 * Indicate the base text direction appropriate for an instance of structured text.
	 * This method is invoked before starting the processing.
	 * <p>
	 * If not overridden, this method returns <code>DIR_LTR</code>.
	 * </p>
	 * @param  environment the current environment, which may affect the behavior of
	 *         the handler. This parameter may be specified as
	 *         <code>null</code>, in which case the
	 *         {@link STextEnvironment#DEFAULT DEFAULT}
	 *         environment should be assumed.
	 *
	 * @param  text is the structured text string to process.
	 *
	 * @param  charTypes is a parameter received uniquely to be used as argument
	 *         for calls to <code>getCharType</code> and other methods used
	 *         by handlers.
	 *
	 * @return the base direction of the structured text. This direction
	 *         may not be the same depending on the environment and on
	 *         whether the structured text contains Arabic or Hebrew
	 *         letters.<br>
	 *         The value returned is either
	 *         {@link STextDirection#DIR_LTR DIR_LTR} or {@link STextDirection#DIR_RTL DIR_RTL}.
	 */
	public int getDirection(STextEnvironment environment, String text, STextCharTypes charTypes) {
		return STextDirection.DIR_LTR;
	}

	/**
	 * Indicate the number of special cases handled by the current handler.
	 * This method is invoked before starting the processing.
	 * If the number returned is zero, {@link #indexOfSpecial} and
	 * {@link #processSpecial} will not be invoked.
	 * <p>
	 * If not overridden, this method returns <code>zero</code>.
	 * </p>
	 * @param  environment the current environment, which may affect the behavior of
	 *         the handler. This parameter may be specified as
	 *         <code>null</code>, in which case the
	 *         {@link STextEnvironment#DEFAULT DEFAULT}
	 *         environment should be assumed.
	 *
	 * @return the number of special cases for the associated handler.
	 *         Special cases exist for some types of structured text
	 *         handlers. They are implemented by overriding methods
	 *         {@link STextTypeHandler#indexOfSpecial} and {@link STextTypeHandler#processSpecial}.
	 *         Examples of special cases are comments, literals, or
	 *         anything which is not identified by a one-character separator.
	 *
	 */
	public int getSpecialsCount(STextEnvironment environment) {
		return 0;
	}

	/**
	 * Checks if there is a need for processing structured text.
	 * This method is invoked before starting the processing. If the
	 * handler returns <code>true</code>, no directional formatting
	 * characters are added to the <i>lean</i> text and the processing
	 * is shortened.
	 * <p>
	 * If not overridden, this method returns <code>false</code>.
	 * </p>
	 * @param  environment the current environment, which may affect the behavior of
	 *         the handler. This parameter may be specified as
	 *         <code>null</code>, in which case the
	 *         {@link STextEnvironment#DEFAULT DEFAULT}
	 *         environment should be assumed.
	 *
	 * @param  text is the structured text string to process.
	 *
	 * @param  charTypes is a parameter received uniquely to be used as argument
	 *         for calls to <code>getCharType</code> and other methods used
	 *         by handlers.
	 *
	 * @return a flag indicating if there is no need to process the structured
	 *         text to add directional formatting characters.
	 *
	 */
	public boolean skipProcessing(STextEnvironment environment, String text, STextCharTypes charTypes) {
		return false;
	}

}
