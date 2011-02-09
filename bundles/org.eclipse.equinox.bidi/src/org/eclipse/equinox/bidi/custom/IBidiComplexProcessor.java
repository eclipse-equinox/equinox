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

import org.eclipse.equinox.bidi.*;
import org.eclipse.equinox.bidi.internal.BidiComplexImpl;

/**
 *  Interface for all complex expression processors.
 *  For guidelines about implementation, see
 *  {@link BidiComplexProcessor}.
 *
 *  @author Matitiahu Allouche
 */
public interface IBidiComplexProcessor {

	/**
	 *  Do whatever initializations are needed and return the
	 *  {@link BidiComplexFeatures} characterizing the processor.
	 *
	 *  @param env the current environment, which may affect the
	 *          initializations.
	 *
	 *  @param caller <code>BidiComplexHelper</code> instance which called this method.
	 *          This allows access to the field
	 *          {@link BidiComplexImpl#processorData caller.impl.processorData} where
	 *          the processor can keep whatever data it needs.
	 *
	 *  @return the features in use for this processor.
	 */
	public abstract BidiComplexFeatures init(BidiComplexHelper caller, BidiComplexEnvironment env);

	/**
	 *  Do whatever renewed initializations are needed after a
	 *  change in the environment and return the possibly updated
	 *  {@link BidiComplexFeatures} characterizing the processor.
	 *
	 *  @param env the updated environment, which may affect the
	 *          initializations.
	 *
	 *  @param caller <code>BidiComplexHelper</code> instance which called this method.
	 *          This allows access to the field
	 *          {@link BidiComplexImpl#processorData caller.impl.processorData} where
	 *          the processor can keep whatever data it needs.
	 *
	 *  @return the features to use for this processor.
	 */
	public BidiComplexFeatures updateEnvironment(BidiComplexHelper caller, BidiComplexEnvironment env);

	/**
	 *  Locate occurrences of special strings within a complex expression
	 *  and return their indexes one after the other in successive calls.
	 *  <p>
	 *  This method is called repeatedly from the code implementing
	 *  {@link BidiComplexHelper#leanToFullText leanToFullText} if the field
	 *  {@link BidiComplexFeatures#specialsCount specialsCount} returned when
	 *  {@link #init initializing} the processor is greater than zero.
	 *  <p>
	 *  The code implementing this method may use the following methods
	 *  in {@link BidiComplexHelper}:
	 *  <ul>
	 *    <li>{@link BidiComplexHelper#getDirProp getDirProp}</li>
	 *    <li>{@link BidiComplexHelper#setDirProp setDirProp}</li>
	 *    <li>{@link BidiComplexHelper#insertMark insertMark}</li>
	 *    <li>{@link BidiComplexHelper#processOperator processOperator}</li>
	 *    <li>{@link BidiComplexHelper#setFinalState setFinalState}</li>
	 *  </ul>
	 *
	 *  @param  caseNumber number of the special case to locate.
	 *          This number varies from zero to <code>specialsCount - 1</code>.
	 *          The meaning of this number is internal to the class
	 *          implementing <code>indexOfSpecial</code>.
	 *
	 *  @param  srcText text of the complex expression before addition of any
	 *          directional formatting characters.
	 *
	 *  @param  fromIndex the index within <code>srcText</code> to start
	 *          the search from.
	 *
	 *  @return the position where the start of the special case was located.
	 *          The method must return the first occurrence of whatever
	 *          identifies the start of the special case starting from
	 *          <code>fromIndex</code>. The method does not have to check if
	 *          this occurrence appears within the scope of another special
	 *          case (e.g. a comment starting delimiter within the scope of
	 *          a literal or vice-versa).
	 *          <br>If no occurrence is found, the method must return -1.
	 */
	public int indexOfSpecial(BidiComplexHelper caller, int caseNumber, String srcText, int fromIndex);

	/**
	 *  This method is called by {@link BidiComplexHelper#leanToFullText leanToFullText}
	 *  when a special case occurrence is located by
	 *  {@link #indexOfSpecial indexOfSpecial}.
	 *  <p>
	 *  The code implementing this method may use the following methods
	 *  in {@link BidiComplexHelper}:
	 *  <ul>
	 *    <li>{@link BidiComplexHelper#getDirProp getDirProp}</li>
	 *    <li>{@link BidiComplexHelper#setDirProp setDirProp}</li>
	 *    <li>{@link BidiComplexHelper#insertMark insertMark}</li>
	 *    <li>{@link BidiComplexHelper#processOperator processOperator}</li>
	 *    <li>{@link BidiComplexHelper#setFinalState setFinalState}</li>
	 *  </ul>
	 *  <p>
	 *  If a special processing cannot be completed within a current call to
	 *  <code>processSpecial</code> (for instance, a comment has been started
	 *  in the current line but its end appears in a following line),
	 *  <code>processSpecial</code> should specify a final state using
	 *  the method {@link BidiComplexHelper#setFinalState setFinalState}.
	 *  The meaning of this state is internal to the processor.
	 *  On a later call to
	 *  {@link BidiComplexHelper#leanToFullText(String text, int initState)}
	 *  specifying that state value, <code>processSpecial</code> will be
	 *  called with that value for parameter <code>caseNumber</code> and
	 *  <code>-1</code> for parameter <code>operLocation</code> and should
	 *  perform whatever initializations are required depending on the state.
	 *
	 *  @param  caseNumber number of the special case to handle.
	 *
	 *  @param  srcText text of the complex expression.
	 *
	 *  @param  operLocation the position returned by
	 *          {@link #indexOfSpecial indexOfSpecial}. In calls to
	 *          {@link BidiComplexHelper#leanToFullText(String text, int initState)} or
	 *          {@link BidiComplexHelper#fullToLeanText(String text, int initState)}
	 *          specifying an <code>initState</code>
	 *          parameter, <code>processSpecial</code> is called when initializing
	 *          the processing with the value of <code>caseNumber</code>
	 *          equal to <code>initState</code> and the value of
	 *          <code>operLocation</code> equal to <code>-1</code>.
	 *
	 *  @return the position after the scope of the special case ends.
	 *          For instance, the position after the end of a comment,
	 *          the position after the end of a literal.
	 *          <br>A value greater or equal to the length of <code>srcText</code>
	 *          means that there is no further occurrence of this case in the
	 *          current complex expression.
	 */
	public int processSpecial(BidiComplexHelper caller, int caseNumber, String srcText, int operLocation);

}
