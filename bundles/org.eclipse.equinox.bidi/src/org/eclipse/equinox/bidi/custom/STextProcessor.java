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
import org.eclipse.equinox.bidi.internal.STextImpl;

/**
 *  Generic processor which can be used as superclass (base class)
 *  for specific structured text processors.
 *  <p>
 *  Here are some guidelines about how to write structured text
 *  processors.
 *  <ul>
 *    <li>Processor instances may be accessed simultaneously by
 *        several threads. They should have no instance variables.</li>
 *    <li>Each use of a processor is associated with a set of
 *        {@link STextFeatures features}.
 *        All processors must have a default set of features which may be
 *        queried with the {@link #getFeatures getFeatures} method.
 *        These default features may be overridden by specifying a
 *        <code>features</code> argument when calling a method.
 *        See for instance
 *        {@link STextEngine#leanToFullText leanToFullText}.
 *    <li>The behavior of a processor is governed by 3 factors, all included
 *        in associated {@link STextFeatures features} data.
 *        <ul>
 *          <li>The separators specified in its
 *              {@link STextFeatures features} determine how submitted
 *              structured text is split into tokens.</li>
 *          <li>The tokens are displayed one after the other according
 *              to the appropriate direction, which can be different for
 *              Arabic and for Hebrew.</li>
 *          <li>The number of special cases which need to be handled by
 *              code specific to that processor.</li>
 *        </ul></li>
 *  </ul>
 *
 *  @see STextFeatures#getSeparators
 *  @see STextFeatures#getDirArabic
 *  @see STextFeatures#getDirHebrew
 *  @see STextFeatures#getSpecialsCount
 *
 *  @author Matitiahu Allouche
 */
public class STextProcessor implements ISTextProcessor {

	/**
	 *  In <code>STextProcessor</code> this method returns a
	 *  {@link STextFeatures#DEFAULT DEFAULT} value which
	 *  directs the processor to do nothing.
	 *
	 *  <p>A processor which extends this class must override this method
	 *  and return a STextFeatures instance representing its specific
	 *  features.
	 *
	 *  @see ISTextProcessor#getFeatures the corresponding interface method
	 */
	public STextFeatures getFeatures(STextEnvironment environment) {
		throw new IllegalStateException("A processor must have a getFeatures() method."); //$NON-NLS-1$
	}

	/**
	 *  In <code>STextProcessor</code> this method throws an
	 *  <code>IllegalStateException</code>. This is appropriate behavior
	 *  (and does not need to be overridden) for processors whose
	 *  number of special cases is zero, which means that
	 *  <code>indexOfSpecial</code> should never be called for them.
	 *
	 *  <p>A processor handling special cases must override this method.
	 *
	 *  @see ISTextProcessor#indexOfSpecial the corresponding interface method
	 */
	public int indexOfSpecial(STextFeatures features, String text, byte[] dirProps, int[] offsets, int caseNumber, int fromIndex) {
		// This method must be overridden by all subclasses with special cases.
		throw new IllegalStateException("A processor must have an indexOfSpecial() method."); //$NON-NLS-1$
	}

	/**
	 *  In <code>STextProcessor</code> this method throws an
	 *  <code>IllegalStateException</code>. This is appropriate behavior
	 *  (and does not need to be overridden) for processors whose
	 *  number of special cases is zero, which means that
	 *  <code>processSpecial</code> should never be called for them.
	 *
	 *  <p>A processor handling special cases must override this method.
	 *
	 *  @see ISTextProcessor#processSpecial the corresponding interface method
	 */
	public int processSpecial(STextFeatures features, String text, byte[] dirProps, int[] offsets, int[] state, int caseNumber, int separLocation) {
		// This method must be overridden by all subclasses with any special case.
		throw new IllegalStateException("A processor must have a processSpecial() method."); //$NON-NLS-1$
	}

	/**
	 *  This method can be called from within
	 *  {@link ISTextProcessor#indexOfSpecial indexOfSpecial} or
	 *  {@link ISTextProcessor#processSpecial processSpecial} in
	 *  implementations of {@link ISTextProcessor} to retrieve the
	 *  bidirectional class of characters in the <i>lean</i> text.
	 *
	 *  @param  text is the structured text string received as
	 *          parameter to <code>indexOfSpecial</code> or
	 *          <code>processSpecial</code>.
	 *
	 *  @param  dirProps is a parameter received by <code>indexOfSpecial</code>
	 *          or <code>processSpecial</code>, uniquely to be used as argument
	 *          for calls to <code>getDirProp</code> and other methods used
	 *          by processors.
	 *
	 *  @param index position of the character in the <i>lean</i> text.
	 *          It must be a non-negative number smaller than the length
	 *          of the <i>lean</i> text.
	 *
	 *  @return the bidirectional class of the character. It is one of the
	 *          values which can be returned by
	 *          <code>java.lang.Character.getDirectionality</code>.
	 *          However, it is recommended to use <code>getDirProp</code>
	 *          rather than <code>java.lang.Character.getDirectionality</code>
	 *          since <code>getDirProp</code> manages a cache of character
	 *          properties and so can be more efficient than calling the
	 *          java.lang.Character method.
	 */
	public static byte getDirProp(String text, byte[] dirProps, int index) {
		return STextImpl.getDirProp(text, dirProps, index);
	}

	/**
	 *  This method can be called from within
	 *  {@link ISTextProcessor#indexOfSpecial indexOfSpecial} or
	 *  {@link ISTextProcessor#processSpecial processSpecial} in
	 *  implementations of {@link ISTextProcessor} to set or
	 *  override the bidirectional class of characters in the <i>lean</i> text.
	 *
	 *  @param  dirProps is a parameter received by <code>indexOfSpecial</code>
	 *          or <code>processSpecial</code>, uniquely to be used as argument
	 *          for calls to <code>setDirProp</code> and other methods used
	 *          by processors.
	 *
	 *  @param index position of the character in the <i>lean</i> text.
	 *          It must be a non-negative number smaller than the length
	 *          of the <i>lean</i> text.
	 *
	 *  @param  dirProp bidirectional class of the character. It is one of the
	 *          values which can be returned by
	 *          <code>java.lang.Character.getDirectionality</code>.
	 */
	public static void setDirProp(byte[] dirProps, int index, byte dirProp) {
		STextImpl.setDirProp(dirProps, index, dirProp);
	}

	/**
	 *  This method can be called from within
	 *  {@link ISTextProcessor#indexOfSpecial indexOfSpecial} or
	 *  {@link ISTextProcessor#processSpecial processSpecial} in
	 *  implementations of {@link ISTextProcessor}
	 *  to specify that a mark character must be added before the character
	 *  at the specified position of the <i>lean</i> text when generating the
	 *  <i>full</i> text. The mark character will be LRM for structured text
	 *  with a LTR base direction, and RLM for structured text with RTL
	 *  base direction. The mark character is not added physically by this
	 *  method, but its position is noted and will be used when generating
	 *  the <i>full</i> text.
	 *
	 *  @param  text is the structured text string received as
	 *          parameter to <code>indexOfSpecial</code> or
	 *          <code>processSpecial</code>.
	 *
	 *  @param  dirProps is a parameter received by <code>indexOfSpecial</code>
	 *          or <code>processSpecial</code>, uniquely to be used as argument
	 *          for calls to <code>insertMark</code> and other methods used
	 *          by processors.
	 *
	 *  @param  offsets is a parameter received by <code>indexOfSpecial</code>
	 *          or <code>processSpecial</code>, uniquely to be used as argument
	 *          for calls to <code>insertMark</code> and other methods used
	 *          by processors.
	 *
	 *  @param  offset position of the character in the <i>lean</i> text.
	 *          It must be a non-negative number smaller than the length
	 *          of the <i>lean</i> text.
	 *          For the benefit of efficiency, it is better to insert
	 *          multiple marks in ascending order of the offsets.
	 */
	public static void insertMark(String text, byte[] dirProps, int[] offsets, int offset) {
		STextImpl.insertMark(text, dirProps, offsets, offset);
	}

	/**
	 *  This method can be called from within
	 *  {@link ISTextProcessor#indexOfSpecial indexOfSpecial} or
	 *  {@link ISTextProcessor#processSpecial processSpecial} in
	 *  implementations of {@link ISTextProcessor} to add a
	 *  directional mark before a
	 *  separator if needed for correct display, depending on the
	 *  base direction of the text and on the class of the
	 *  characters in the <i>lean</i> text preceding and following
	 *  the separator itself.
	 *
	 *  @param  features is the {@link STextFeatures} instance
	 *          received as parameter to <code>indexOfSpecial</code> or
	 *          <code>processSpecial</code>.
	 *
	 *  @param  text is the structured text string received as
	 *          parameter to <code>indexOfSpecial</code> or
	 *          <code>processSpecial</code>.
	 *
	 *  @param  dirProps is a parameter received by <code>indexOfSpecial</code>
	 *          or <code>processSpecial</code>, uniquely to be used as argument
	 *          for calls to <code>processSeparator</code> and other methods used
	 *          by processors.
	 *
	 *  @param  offsets is a parameter received by <code>indexOfSpecial</code>
	 *          or <code>processSpecial</code>, uniquely to be used as argument
	 *          for calls to <code>processSeparator</code> and other methods used
	 *          by processors.
	 *
	 *  @param  separLocation offset of the separator in the <i>lean</i> text.
	 *          It must be a non-negative number smaller than the length
	 *          of the <i>lean</i> text.
	 */
	public static void processSeparator(STextFeatures features, String text, byte[] dirProps, int[] offsets, int separLocation) {
		STextImpl.processSeparator(features, text, dirProps, offsets, separLocation);
	}

}
