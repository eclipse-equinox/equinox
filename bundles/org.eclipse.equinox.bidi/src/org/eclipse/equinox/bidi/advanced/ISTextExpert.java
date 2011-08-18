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
 * For a general introduction to structured text, see
 * {@link <a href="package-summary.html"> the package documentation</a>}.
 * <p>
 * Several common handlers are included in <b>STextEngine</b>. For handlers 
 * supplied by other packages, a handler instance can be obtained using the
 * {@link org.eclipse.equinox.bidi.STextTypeHandlerFactory#getHandler}
 * method for the registered handlers, or by instantiating a private handler.
 * </p><p>
 * Most of the methods in this class have a <code>text</code>
 * argument which may be just a part of a larger body of text.
 * When it is the case that the text is submitted in parts with
 * repeated calls, there may be a need to pass information from
 * one invocation to the next one. For instance, one invocation
 * may detect that a comment or a literal has been started but
 * has not been completed. In such cases, a <code>state</code>
 * argument must be used.
 * </p><p>
 * The <code>state</code> argument must be an array of integers
 * with at least one element. Only the first element is used by
 * the methods of this class.
 * </p><p>
 * When submitting the initial part of the text, the first element
 * of <code>state</code> must contain the value {@link #STATE_INITIAL}
 * or any value <= 0.
 * </p><p>
 * After calling a method with a non-null <code>state</code> argument,
 * a value is returned in the first element of <code>state</code>. This
 * value should be passed unmodified to the method when calling it again
 * with the text which is the continuation of the text submitted in the
 * last call.
 * </p><p>
 * When the text submitted to a method is not a continuation and is not
 * expected to have a continuation , e.g. it is processed all by itself,
 * the <code>state</code> argument should be specified as <code>null</code>.
 * </p><p>
 * <b>Code Samples</b>
 * </p><p>
 * The following code shows how to transform a certain type of structured text
 * (directory and file paths) in order to obtain the <i>full</i>
 * text corresponding to the given <i>lean</i> text.
 * <pre>
 *   String leanText = "D:\\\u05d0\u05d1\\\u05d2\\\u05d3.ext";
 *   String fullText = STextEngine.leanToFullText(STextEngine.PROC_FILE, null, leanText, null);
 *   System.out.println("full text = " + fullText);
 * </pre>
 * </p><p>
 * The following code shows how to transform successive lines of Java
 * code in order to obtain the <i>full</i>
 * text corresponding to the <i>lean</i> text of each line.
 * <pre>
 *   Object state = STextState.createState();
 *   String leanText = "int i = 3; // first Java statement";
 *   String fullText = STextEngine.leanToFullText(STextEngine.PROC_JAVA, null, leanText, state);
 *   System.out.println("full text = " + fullText);
 *   leanText = "i += 4; // next Java statement";
 *   fullText = STextEngine.leanToFullText(STextEngine.PROC_JAVA, null, leanText, state);
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

	public STextTypeHandler getTypeHandler();

	public STextEnvironment getEnvironment();

	/** 
	 * Add directional formatting characters to a structured text
	 * to ensure correct presentation.
	 * 
	 * @param  handler the handler applicable to the text. If <code>null</code>, 
	 * the method returns unmodified text.
	 * 
	 * @param  environment a bidi environment. If <code>null</code>, the default environment 
	 * is used.
	 *  
	 * @param text is the structured text string
	 *
	 * @param  state can be used to specify that the <code>text</code> argument is 
	 * the continuation of text submitted in a previous call and/or to receive information 
	 * to pass to continuation calls. If all calls to this method are independent from one another,
	 * this argument should be specified as <code>null</code>.
	 *
	 * @return the structured text with directional formatting characters added to ensure 
	 * correct presentation.
	 */
	public String leanToFullText(String text);

	/**
	 * Given a <i>lean</i> string, compute the positions of each of its
	 * characters within the corresponding <i>full</i> string.
	 *
	 * @param  handler designates a handler instance. If <code>null</code>, this 
	 * method returns an identity map.
	 *
	 * @param  environment specifies an environment whose characteristics may affect 
	 * the handler's behavior. If <code>null</code>, the default environment is used.
	 *
	 * @param text is the structured text string.
	 *
	 * @param  state can be used to specify that the <code>text</code> argument is 
	 * the continuation of text submitted in a previous call and/or to receive information 
	 * to pass to continuation calls. If all calls to this method are independent from one another,
	 * this argument should be specified as <code>null</code>.
	 *
	 * @return an array which specifies offsets of the <code>text</code> characters
	 * in the <i>full</i> string
	 */
	public int[] leanToFullMap(String text);

	/**
	 * Given a <i>lean</i> string, compute the offsets of characters
	 * before which directional formatting characters must be added
	 * in order to ensure correct presentation.
	 * <p>
	 * Only LRMs (for a string with LTR base direction) and RLMs (for
	 * a string with RTL base direction) are considered. Leading and
	 * trailing LRE, RLE and PDF which might be prefixed or suffixed
	 * depending on the {@link STextEnvironment#getOrientation orientation} of the
	 * GUI component used for display are not reflected in this method.
	 * </p>
	 * @param handler designates a handler instance
	 *
	 * @param  environment specifies an environment whose characteristics may affect 
	 * the handler's behavior. If <code>null</code>, the default environment is used.
	 *
	 * @param text is the structured text string
	 *
	 * @param  state can be used to specify that the <code>text</code> argument is 
	 * the continuation of text submitted in a previous call and/or to receive information 
	 * to pass to continuation calls. If all calls to this method are independent from one another,
	 * this argument should be specified as <code>null</code>.
	 *
	 * @return an array of offsets to the characters in the <code>text</code> argument 
	 * before which directional marks must be added to ensure correct presentation.
	 * The offsets are sorted in ascending order.
	 */
	public int[] leanBidiCharOffsets(String text);

	/**
	 * Remove directional formatting characters which were added to a
	 * structured text string to ensure correct presentation.
	 *
	 * @param  handler designates a handler instance
	 *
	 * @param  environment specifies an environment whose characteristics may affect 
	 * the handler's behavior. If <code>null</code>, the default environment is used.
	 *
	 * @param text is the structured text string including directional formatting characters.
	 *
	 * @param  state can be used to specify that the <code>text</code> argument is 
	 * the continuation of text submitted in a previous call and/or to receive information 
	 * to pass to continuation calls. If all calls to this method are independent from one another,
	 * this argument should be specified as <code>null</code>.
	 *
	 * @return the structured text string without directional formatting characters 
	 * which might have been added by processing it with {@link #leanToFullText}.
	 *
	 */
	public String fullToLeanText(String text);

	/**
	 * Given a <i>full</i> string, compute the positions of each of its
	 * characters within the corresponding <i>lean</i> string.
	 *
	 * @param  handler designates a handler instance
	 *
	 * @param  environment specifies an environment whose characteristics may affect 
	 * the handler's behavior. If <code>null</code>, the default environment is used.
	 *
	 * @param  text is the structured text string including directional formatting characters.
	 *
	 * @param  state can be used to specify that the <code>text</code> argument is 
	 * the continuation of text submitted in a previous call and/or to receive information 
	 * to pass to continuation calls. If all calls to this method are independent from one another,
	 * this argument should be specified as <code>null</code>.
	 *
	 * @return an array of integers with one element for each of the characters
	 * in the <code>text</code> argument, equal to the offset of the corresponding character 
	 * in the <i>lean</i> string. If there is no corresponding character in the <i>lean</i> string 
	 * (because the specified character is a directional formatting character added when invoking 
	 * {@link #leanToFullText}), the value returned for this character is -1.
	 */
	public int[] fullToLeanMap(String text);

	/**
	 * Given a <i>full</i> string, return the offsets of characters
	 * which are directional formatting characters that have been added
	 * in order to ensure correct presentation.
	 * <p>
	 * LRMs (for a string with LTR base direction), RLMs (for a string with RTL base direction) 
	 * are considered as well as leading and trailing LRE, RLE and PDF which might be prefixed 
	 * or suffixed depending on the {@link STextEnvironment#getOrientation orientation} 
	 * of the GUI component used for display.
	 * </p>
	 * @param  handler designates a handler instance
	 *
	 * @param  environment specifies an environment whose characteristics may affect 
	 * the handler's behavior. If <code>null</code>, the default environment is used.
	 *
	 * @param  text is the structured text string including directional formatting characters
	 *
	 * @param  state can be used to specify that the <code>text</code> argument is 
	 * the continuation of text submitted in a previous call and/or to receive information 
	 * to pass to continuation calls. If all calls to this method are independent from one another,
	 * this argument should be specified as <code>null</code>.
	 *
	 * @return an array of offsets to the characters in the <code>text</code> argument which 
	 * are directional formatting characters added to ensure correct presentation. The offsets 
	 * are sorted in ascending order.
	 */
	public int[] fullBidiCharOffsets(String text);

	/**
	 * Get the base direction of a structured text. This base direction may depend on
	 * whether the text contains Arabic or Hebrew words. If the text contains both, 
	 * the first Arabic or Hebrew letter in the text determines which is the governing script.
	 *
	 * @param  handler designates a handler instance
	 *
	 * @param  environment specifies an environment whose characteristics may affect 
	 * the handler's behavior. If <code>null</code>, the default environment is used.
	 *
	 * @param  text is the structured text string
	 *
	 * @return the base direction of the structured text, {@link #DIR_LTR} or {@link #DIR_RTL}
	 */
	public int getTextDirection(String text);

	///////////////////////////////////////////////////////////////////////////////////////////////
	// Expert's state handling - can be used only for non-shared experts

	// TBD is there a case where we get a shared expert and setting state is a "must"?
	// Javadoc note: shared experts will ignore this
	public void setState(Object state);

	// Javadoc note: may return null. Will return null for shared experts
	public Object getState();

	/**
	 * Resets state to initial.
	 */
	public void clearState();
}
