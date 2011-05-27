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

import org.eclipse.equinox.bidi.STextEngine;
import org.eclipse.equinox.bidi.STextEnvironment;

/**
 *  Interface for all structured text processors.
 *  For guidelines about implementation, see
 *  {@link STextProcessor}.
 *
 *  @author Matitiahu Allouche
 */
public interface ISTextProcessor {

	/**
	 *  return the
	 *  {@link STextFeatures} characterizing the processor.
	 *
	 *  @param  env the current environment, which may affect the behavior of
	 *          the processor. This parameter may be specified as
	 *          <code>null</code>, in which case the
	 *          {@link STextEnvironment#DEFAULT DEFAULT}
	 *          environment should be assumed.
	 *
	 *  @return the features in use for this processor.
	 */
	public abstract STextFeatures getFeatures(STextEnvironment env);

	/**
	 *  Locate occurrences of special strings within a structured text
	 *  and return their indexes one after the other in successive calls.
	 *  <p>
	 *  This method is called repeatedly from the code implementing
	 *  {@link STextEngine#leanToFullText leanToFullText} if the
	 *  number of special cases appearing in the associated <code>features</code>
	 *  parameter is greater than zero.
	 *  <p>
	 *  The code implementing this method may use the following methods
	 *  in {@link STextProcessor}:
	 *  <ul>
	 *    <li>{@link STextProcessor#getDirProp getDirProp}</li>
	 *    <li>{@link STextProcessor#setDirProp setDirProp}</li>
	 *    <li>{@link STextProcessor#insertMark insertMark}</li>
	 *    <li>{@link STextProcessor#processSeparator processSeparator}</li>
	 *  </ul>
	 *
	 *  @param  features is the {@link STextFeatures} instance
	 *          currently associated with this processor.
	 *
	 *  @param  text is the structured text string before
	 *          addition of any directional formatting characters.
	 *
	 *  @param  dirProps is a parameter received by <code>indexOfSpecial</code>
	 *          uniquely to be used as argument for calls to methods which
	 *          need it.
	 *
	 *  @param  offsets is a parameter received by <code>indexOfSpecial</code>
	 *          uniquely to be used as argument for calls to methods which
	 *          need it.
	 *
	 *  @param  caseNumber number of the special case to locate.
	 *          This number varies from 1 to the number of special cases
	 *          in the features associated with this processor.
	 *          The meaning of this number is internal to the class
	 *          implementing <code>indexOfSpecial</code>.
	 *
	 *  @param  fromIndex the index within <code>text</code> to start
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
	public int indexOfSpecial(STextFeatures features, String text, byte[] dirProps, int[] offsets, int caseNumber, int fromIndex);

	/**
	 *  This method handles special cases specific to this processor.
	 *  It is called by {@link STextEngine#leanToFullText leanToFullText}
	 *  when a special case occurrence is located by
	 *  {@link #indexOfSpecial indexOfSpecial}.
	 *  <p>
	 *  The code implementing this method may use the following methods
	 *  in {@link STextProcessor}:
	 *  <ul>
	 *    <li>{@link STextProcessor#getDirProp getDirProp}</li>
	 *    <li>{@link STextProcessor#setDirProp setDirProp}</li>
	 *    <li>{@link STextProcessor#insertMark insertMark}</li>
	 *    <li>{@link STextProcessor#processSeparator processSeparator}</li>
	 *  </ul>
	 *  <p>
	 *  If a special processing cannot be completed within a current call to
	 *  <code>processSpecial</code> (for instance, a comment has been started
	 *  in the current line but its end appears in a following line),
	 *  <code>processSpecial</code> should specify a final state by
	 *  putting its value in the first element of the <code>state</code>
	 *  parameter.
	 *  The meaning of this state is internal to the processor.
	 *  On a later call to
	 *  {@link STextEngine#leanToFullText leanToFullText}
	 *  specifying that state value, <code>processSpecial</code> will be
	 *  called with that value for parameter <code>caseNumber</code> and
	 *  <code>-1</code> for parameter <code>separLocation</code> and should
	 *  perform whatever initializations are required depending on the state.
	 *
	 *  @param  features is the {@link STextFeatures} instance
	 *          currently associated with this processor.
	 *
	 *  @param  text is the structured text string before
	 *          addition of any directional formatting characters.
	 *
	 *  @param  dirProps is a parameter received by <code>processSpecial</code>
	 *          uniquely to be used as argument for calls to methods which
	 *          need it.
	 *
	 *  @param  offsets is a parameter received by <code>processSpecial</code>
	 *          uniquely to be used as argument for calls to methods which
	 *          need it.
	 *
	 *  @param  state is an integer array with at least one element.
	 *          If the processor needs to signal the occurrence of a
	 *          special case which must be passed to the next call to
	 *          <code>leanToFullText</code> (for instance, a comment or a
	 *          literal started but not closed in the current
	 *          <code>text</code>), it must put a value in the first element
	 *          of the <code>state</code> parameter.
	 *          This value must be a number between 1 and the number of
	 *          special cases appearing in the features associated with
	 *          this processor. This number is passed back to the caller
	 *          and should be specified as <code>state</code> argument
	 *          in the next call to <code>leanToFullText</code> together
	 *          with the continuation text.
	 *          The meaning of this number is internal to the processor.
	 *
	 *  @param  caseNumber number of the special case to handle.
	 *
	 *  @param  separLocation the position returned by
	 *          {@link #indexOfSpecial indexOfSpecial}. In calls to
	 *          {@link STextEngine#leanToFullText leanToFullText} and other
	 *          methods of {@link STextEngine} specifying a  non-null
	 *          <code>state</code> parameter, <code>processSpecial</code> is
	 *          called when initializing the processing with the value of
	 *          <code>caseNumber</code> equal to the value returned in the
	 *          first element of <code>state</code> and the value of
	 *          <code>separLocation</code> equal to <code>-1</code>.
	 *
	 *  @return the position after the scope of the special case ends.
	 *          For instance, the position after the end of a comment,
	 *          the position after the end of a literal.
	 *          <br>A value greater or equal to the length of <code>text</code>
	 *          means that there is no further occurrence of this case in the
	 *          current structured text.
	 */
	public int processSpecial(STextFeatures features, String text, byte[] dirProps, int[] offsets, int[] state, int caseNumber, int separLocation);

}
