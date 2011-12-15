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
package org.eclipse.equinox.bidi.advanced;

import org.eclipse.equinox.bidi.custom.STextTypeHandler;

/**
 * Provides advanced methods for processing bidirectional text with 
 * a specific structure to ensure proper presentation.
 * For a general introduction to structured text, see the 
 * {@link <a href="../package-summary.html">org.eclipse.equinox.bidi
 * package documentation</a>}.
 * For details about when the advanced methods are needed, see
 * {@link <a href="package-summary.html">this
 * package documentation</a>}.
 * <p>
 * Identifiers for several common handlers are included in 
 * {@link org.eclipse.equinox.bidi.STextTypeHandlerFactory}. For handlers 
 * supplied by other packages, a handler instance can be obtained using the
 * {@link org.eclipse.equinox.bidi.STextTypeHandlerFactory#getHandler}
 * method for the registered handlers, or by instantiating a private handler.
 * </p><p>
 * Most of the methods in this interface have a <code>text</code>
 * argument which may be just a part of a larger body of text.
 * When it is the case that the text is submitted in parts with
 * repeated calls, there may be a need to pass information from
 * one invocation to the next one. For instance, one invocation
 * may detect that a comment or a literal has been started but
 * has not been completed. In such cases, the state must be managed
 * by a <code>ISTextExpert</code> instance obtained with the 
 * {@link STextExpertFactory#getStatefulExpert} method.
 * </p><p>
 * The <code>state</code> returned after processing a string
 * can be retrieved, set and reset using the {@link #getState()},
 * {@link #setState(Object)} and {@link #clearState()} methods.
 * </p><p>
 * When submitting the initial part of a text, the state should be
 * reset if it is not the first processing call for this 
 * <code>ISTextExpert</code> instance.
 * </p><p>
 * Values returned by {@link #getState()} are opaque objects whose meaning
 * is internal to the relevant structured type handler. These values can only
 * be used in {@link #setState(Object)} calls to restore a state
 * previously obtained after processing a given part of a text before 
 * processing the next part of the text.
 * </p><p>
 * Note that if the user does not modify the state, the state returned by
 * a given processing call is automatically passed as initial state to the
 * next processing call, provided that the expert is a stateful one.
 * </p><p>
 * <b>Code Samples</b>
 * </p><p>
 * The following code shows how to transform a certain type of structured text
 * (directory and file paths) in order to obtain the <i>full</i>
 * text corresponding to the given <i>lean</i> text.
 * <pre>
 *   ISTextExpert expert = STextExpertFactory.getExpert(STextTypeHandlerFactory.FILE);
 *   String leanText = "D:\\\u05d0\u05d1\\\u05d2\\\u05d3.ext";
 *   String fullText = expert.leanToFullText(leanText);
 *   System.out.println("full text = " + fullText);
 * </pre>
 * </p><p>
 * The following code shows how to transform successive lines of Java
 * code in order to obtain the <i>full</i>
 * text corresponding to the <i>lean</i> text of each line.
 * <pre>
 *   ISTextExpert expert = STextExpertFactory.getStatefulExpert(STextTypeHandlerFactory.JAVA);
 *   String leanText = "int i = 3; // first Java statement";
 *   String fullText = expert.leanToFullText(leanText);
 *   System.out.println("full text = " + fullText);
 *   leanText = "i += 4; // next Java statement";
 *   fullText = expert.leanToFullText(leanText,);
 *   System.out.println("full text = " + fullText);
 * </pre>
 * </p>
 *  @author Matitiahu Allouche
 *
 */
public interface ISTextExpert {

	/**
	 *  Constant specifying that the base direction of a structured text is LTR.
	 *  The base direction may depend on whether the GUI is
	 *  {@link STextEnvironment#getMirrored mirrored} and
	 *  may be different for Arabic and for Hebrew.
	 *  This constant can appear as value returned by the
	 *  {@link #getTextDirection} method.
	 */
	public static final int DIR_LTR = 0;

	/**
	 *  Constant specifying that the base direction of a structured text is RTL.
	 *  The base direction may depend on whether the GUI is
	 *  {@link STextEnvironment#getMirrored mirrored} and may
	 *  may be different for Arabic and for Hebrew.
	 *  This constant can appear as value returned by the
	 *  {@link #getTextDirection} method.
	 */
	public static final int DIR_RTL = 1;

	/**
	 * Obtains the structured type handler associated with this 
	 * <code>ISTextExpert</code> instance.
	 * 
	 * @return the type handler instance.
	 */
	public STextTypeHandler getTypeHandler();

	/**
	 * Obtains the environment associated with this 
	 * <code>ISTextExpert</code> instance.
	 * @return the environment instance.
	 */
	public STextEnvironment getEnvironment();

	/** 
	 * Adds directional formatting characters to a structured text
	 * to ensure correct presentation.
	 * 
	 * @param text is the structured text string
	 *
	 * @return the structured text with directional formatting characters added to ensure 
	 * correct presentation.
	 */
	public String leanToFullText(String text);

	/**
	 * Given a <i>lean</i> string, computes the positions of each of its
	 * characters within the corresponding <i>full</i> string.
	 *
	 * @param text is the structured text string.
	 *
	 * @return an array of integers with one element for each of the
	 * characters in the <code>text</code> argument, equal to the offset
	 * of the corresponding character in the <i>full</i> string.
	 */
	public int[] leanToFullMap(String text);

	/**
	 * Given a <i>lean</i> string, computes the offsets of characters
	 * before which directional formatting characters must be added
	 * in order to ensure correct presentation.
	 * <p>
	 * Only LRMs (for a string with LTR base direction) and RLMs (for
	 * a string with RTL base direction) are considered. Leading and
	 * trailing LRE, RLE and PDF which might be prefixed or suffixed
	 * depending on the {@link STextEnvironment#getOrientation orientation} of the
	 * GUI component used for display are not reflected in this method.
	 * </p>
	 * @param text is the structured text string
	 *
	 * @return an array of offsets to the characters in the <code>text</code> argument 
	 * before which directional marks must be added to ensure correct presentation.
	 * The offsets are sorted in ascending order.
	 */
	public int[] leanBidiCharOffsets(String text);

	/**
	 * Removes directional formatting characters which were added to a
	 * structured text string to ensure correct presentation.
	 *
	 * @param text is the structured text string including directional formatting characters.
	 *
	 * @return the structured text string without directional formatting characters 
	 * which might have been added by processing it with {@link #leanToFullText}.
	 *
	 */
	public String fullToLeanText(String text);

	/**
	 * Given a <i>full</i> string, computes the positions of each of its
	 * characters within the corresponding <i>lean</i> string.
	 *
	 * @param  text is the structured text string including directional formatting characters.
	 *
	 * @return an array of integers with one element for each of the
	 * characters in the <code>text</code> argument, equal to the offset
	 * of the corresponding character in the <i>lean</i> string.
	 * If there is no corresponding character in the <i>lean</i> string 
	 * (because the specified character is a directional formatting character
	 * added when invoking {@link #leanToFullText}), 
	 * the value returned for this character is -1.
	 */
	public int[] fullToLeanMap(String text);

	/**
	 * Given a <i>full</i> string, returns the offsets of characters
	 * which are directional formatting characters that have been added
	 * in order to ensure correct presentation.
	 * <p>
	 * LRMs (for a string with LTR base direction), RLMs (for a string with RTL base direction) 
	 * are considered as well as leading and trailing LRE, RLE and PDF which might be prefixed 
	 * or suffixed depending on the {@link STextEnvironment#getOrientation orientation} 
	 * of the GUI component used for display.
	 * </p>
	 * @param  text is the structured text string including directional formatting characters
	 *
	 * @return an array of offsets to the characters in the <code>text</code> argument which 
	 * are directional formatting characters added to ensure correct presentation. The offsets 
	 * are sorted in ascending order.
	 */
	public int[] fullBidiCharOffsets(String text);

	/** 
	 * Adds directional marks to the given text before the characters 
	 * specified in the given array of offsets. It can be used to add a prefix and/or 
	 * a suffix of directional formatting characters.
	 * <p>
	 * The directional marks will be LRMs for structured text strings with LTR base 
	 * direction and RLMs for strings with RTL base direction.
	 * </p><p> 
	 * If necessary, leading and trailing directional formatting characters
	 * (LRE, RLE and PDF) can be added depending on the value of the 
	 * <code>affix</code> argument.</p>
	 * <ul>
	 *   <li>A value of 1 means that one LRM or RLM must be prefixed, depending  
	 *       on the direction. This is useful when the GUI component presenting
	 *       this text has a contextual orientation.</li>
	 *   <li>A value of 2 means that LRE+LRM or RLE+RLM must be prefixed, 
	 *       depending on the direction, and LRM+PDF or RLM+PDF must be
	 *       suffixed, depending on the direction.
	 *       This is useful if the GUI component presenting this text needs to
	 *       have the text orientation explicitly specified.</li>
	 *   <li>A value of 0 means that no prefix or suffix are needed.</li>
	 * </ul>
	 * @see ISTextExpert#leanBidiCharOffsets(String)
	 * 
	 * @param  text the structured text string
	 * @param  offsets an array of offsets to characters in <code>text</code>
	 *         before which an LRM or RLM will be inserted.
	 *         The array must be sorted in ascending order without duplicates.
	 *         This argument may be <code>null</code> if there are no marks to add.
	 * @param  direction the base direction of the structured text.
	 *         It must be one of the values {@link #DIR_LTR}, or
	 *         {@link #DIR_RTL}.
	 * @param  affixLength specifies the length of prefix and suffix 
	 *         which should be added to the result.<br>
	 *         0 means no prefix or suffix<br>
	 *         1 means one LRM or RLM as prefix and no suffix<br>
	 *         2 means 2 characters in both prefix and suffix.
	 *         
	 * @return a string corresponding to the source <code>text</code> with
	 *         directional marks (LRMs or RLMs) added at the specified offsets,
	 *         and directional formatting characters (LRE, RLE, PDF) added
	 *         as prefix and suffix if so required.
	 */
	public String insertMarks(String text, int[] offsets, int direction, int affixLength);

	/**
	 * Get the base direction of a structured text. This base direction may depend on
	 * whether the text contains Arabic or Hebrew words. If the text contains both, 
	 * the first Arabic or Hebrew letter in the text determines which is the governing script.
	 *
	 * @param  text is the structured text string.
	 *
	 * @return the base direction of the structured text, {@link #DIR_LTR} or {@link #DIR_RTL}
	 */
	public int getTextDirection(String text);

	//////////////////////////////////////////////////////////////////////
	// Expert's state handling - can be used only for non-shared experts
	//////////////////////////////////////////////////////////////////////
	/**
	 * Sets the state for the next text processing call.
	 * This method does nothing if the expert instance is not a stateful one.
	 * 
	 * @param state an object returned by a previous call to {@link #getState}.
	 */
	public void setState(Object state);

	/**
	 * Gets the state established by the last text processing call.
	 * This is <code>null</code> if the expert instance is not a stateful one,
	 * or if the last text processing call had nothing to pass to the next call.
	 * 
	 * @return the last established state.
	 */
	public Object getState();

	/**
	 * Resets the state to initial.
	 * This method does nothing if the expert instance is not a stateful one.
	 */
	public void clearState();
}
