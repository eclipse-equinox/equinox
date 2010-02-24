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

import org.eclipse.equinox.bidi.internal.complexp.ComplExpBasic;

/**
 *  This interface provides a generic mechanism to handle
 *  complex expressions, with as main purpose to ensure that they are
 *  correctly presented.
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
 *
 *  <p>&nbsp;</p>
 *
 *  @author Matitiahu Allouche
 *
 */
public interface IComplExpProcessor {
	/**
	 *  Constant to use as <code>type</code> argument when calling
	 *  {@link #leanToFullText(java.lang.String, int) leanToFullText} or
	 *  {@link #fullToLeanText(java.lang.String, int) fullToLeanText}
	 *  to indicate that there is no context of previous lines which
	 *  should be initialized before performing the operation.
	 *
	 *  @see ComplExpBasic#state
	 */
	public static final int STATE_NOTHING_GOING = -1;

	/**
	 *  Constant specifying that the orientation of the GUI component
	 *  where a complex expression will be displayed is LTR.
	 *  It can appear as argument for {@link #assumeOrientation assumeOrientation}
	 *  or return value for {@link #recallOrientation recallOrientation}.
	 */
	public static final int ORIENT_LTR = 0;

	/**
	 *  Constant specifying that the orientation of the GUI component
	 *  where a complex expression will be displayed is RTL.
	 *  It can appear as argument for {@link #assumeOrientation assumeOrientation}
	 *  or return value for {@link #recallOrientation recallOrientation}.
	 */
	public static final int ORIENT_RTL = 1;

	/**
	 *  Constant specifying that the orientation of the GUI component
	 *  where a complex expression will be displayed is contextual with
	 *  a default of LTR (if no strong character appears in the text).
	 *  It can appear as argument for {@link #assumeOrientation assumeOrientation}
	 *  or return value for {@link #recallOrientation recallOrientation}.
	 */
	public static final int ORIENT_CONTEXTUAL_LTR = 2;

	/**
	 *  Constant specifying that the orientation of the GUI component
	 *  where a complex expression will be displayed is contextual with
	 *  a default of RTL (if no strong character appears in the text).
	 *  It can appear as argument for {@link #assumeOrientation assumeOrientation}
	 *  or return value for {@link #recallOrientation recallOrientation}.
	 */
	public static final int ORIENT_CONTEXTUAL_RTL = 3;

	/**
	 *  Constant specifying that the orientation of the GUI component
	 *  where a complex expression will be displayed is not known.
	 *  Directional formatting characters must be added as prefix and
	 *  suffix whenever a <i>full</i> text is generated using
	 *  {@link #leanToFullText leanToFullText}.
	 *  This constant can appear as argument for
	 *  {@link #assumeOrientation assumeOrientation}
	 *  or return value for {@link #recallOrientation recallOrientation}.
	 */
	public static final int ORIENT_UNKNOWN = 4;

	/**
	 *  Constant specifying that whatever the orientation of the
	 *  GUI component where a complex expression will be displayed, no
	 *  directional formatting characters must be added as prefix or
	 *  suffix whenever a <i>full</i> text is generated using
	 *  {@link #leanToFullText leanToFullText}.
	 *  This constant can appear as argument for
	 *  {@link #assumeOrientation assumeOrientation}
	 *  or return value for {@link #recallOrientation recallOrientation}.
	 */
	public static final int ORIENT_IGNORE = 5;

	/**
	 *  Constant specifying that the base direction of a complex expression is LTR.
	 *  The base direction may depend on whether the GUI is mirrored and may
	 *  may be different for Arabic and for Hebrew.
	 *  This constant can appear as argument for {@link #setDirection(int) setDirection}
	 *  or return value for {@link #getDirection getDirection}.
	 *
	 *  @see ComplExpBasic#mirrored
	 */
	public static final int DIRECTION_LTR = 0;

	/**
	 *  Constant specifying that the base direction of a complex expression is RTL.
	 *  The base direction may depend on whether the GUI is mirrored and may
	 *  be different for Arabic and for Hebrew.
	 *  This constant can appear as argument for {@link #setDirection(int) setDirection}
	 *  or return value for {@link #getDirection getDirection}.
	 *
	 *  @see ComplExpBasic#mirrored
	 */
	public static final int DIRECTION_RTL = 1;

	/**
	 *  This method specifies whether Arabic letters, Hebrew letters or
	 *  both will be considered for processing complex expressions.
	 *  If neither is selected, the processor will do nothing (but
	 *  some overhead can be expected).
	 *  <p>
	 *  By default, both Arabic and Hebrew are selected for processing.
	 *
	 *  @param arabic a <code>true</code> value means that Arabic letters
	 *         will be processed.
	 *
	 *  @param hebrew a <code>true</code> value means that Hebrew letters
	 *         will be processed.
	 *
	 *  @see #handlesArabicScript
	 *  @see #handlesHebrewScript
	 */
	public void selectBidiScript(boolean arabic, boolean hebrew);

	/**
	 *  This method specifies whether Arabic letters
	 *  will be considered for processing complex expressions.
	 *
	 *  @return <code>true</code> if Arabic letters will be processed.
	 *
	 *  @see #selectBidiScript selectBidiScript
	 */
	public boolean handlesArabicScript();

	/**
	 *  This method specifies whether Hebrew letters
	 *  will be considered for processing complex expressions.
	 *
	 *  @return <code>true</code> if Hebrew letters will be processed.
	 *
	 *  @see #selectBidiScript selectBidiScript
	 */
	public boolean handlesHebrewScript();

	/** Add directional formatting characters to a complex expression
	 *  to ensure correct presentation.
	 *
	 *  @param text is the text of the complex expression.
	 *
	 *  @return the complex expression with directional formatting
	 *          characters added at proper locations to ensure correct
	 *          presentation.
	 *
	 */
	public String leanToFullText(String text);

	/** Add directional formatting characters to a complex expression
	 *  to ensure correct presentation.
	 *
	 *  @param text is the text of the complex expression.
	 *
	 *  @param initState specifies that the first parameter is the
	 *         continuation of text submitted in a previous call.
	 *         The <code>initState</code> of the present call must be
	 *         the final state obtained by calling
	 *         {@link #getFinalState() getFinalState}
	 *         after the previous call.
	 *
	 *  @return the complex expression with directional formatting
	 *          characters added at proper locations to ensure correct
	 *          presentation.
	 *
	 *  @see #getFinalState getFinalState
	 */
	public String leanToFullText(String text, int initState);

	/** Given a complex expression, get the offsets of characters before which
	 *  directional formatting characters must be added in order to
	 *  ensure correct presentation.
	 *  Only LRMs (for an expression with LTR base direction) and RLMs (for
	 *  an expression with RTL base direction) are considered. Leading and
	 *  trailing LRE, RLE and PDF which might be prefixed or suffixed
	 *  depending on the orientation of the GUI component used for display
	 *  are not reflected in this method.
	 *
	 *  @param text is the text of the complex expression.
	 *
	 *  @return an array of offsets to the characters in the <code>text</code>
	 *          before which directional marks must be added to ensure correct
	 *          presentation.
	 *          The offsets are sorted in ascending order.
	 *
	 */
	public int[] leanBidiCharOffsets(String text);

	/** Given a complex expression, get the offsets of characters before which
	 *  directional formatting characters must be added in order to
	 *  ensure correct presentation.
	 *  Only LRMs (for an expression with LTR base direction) and RLMs (for
	 *  an expression with RTL base direction) are considered. Leading and
	 *  trailing LRE, RLE and PDF which might be prefixed or suffixed
	 *  depending on the orientation of the GUI component used for display
	 *  are not reflected in this method.
	 *
	 *  @param text is the text of the complex expression.
	 *
	 *  @param initState specifies that the first parameter is the
	 *         continuation of text submitted in a previous call.
	 *         The <code>initState</code> of the present call must be
	 *         the final state obtained by calling
	 *         {@link #getFinalState() getFinalState}
	 *         after the previous call.
	 *
	 *  @return an array of offsets to the characters in the <code>text</code>
	 *          before which directional marks must be added to ensure correct
	 *          presentation.
	 *          The offsets are sorted in ascending order.
	 *
	 *  @see #getFinalState getFinalState
	 */
	public int[] leanBidiCharOffsets(String text, int initState);

	/** Given a complex expression, get the offsets of characters before which
	 *  directional formatting characters must be added in order to
	 *  ensure correct presentation.
	 *  Only LRMs (for an expression with LTR base direction) and RLMs (for
	 *  an expression with RTL base direction) are considered. Leading and
	 *  trailing LRE, RLE and PDF which might be prefixed or suffixed
	 *  depending on the orientation of the GUI component used for display
	 *  are not reflected in this method.
	 *  <p>
	 *  This method assumes that a successful call to
	 *  {@link #leanToFullText leanToFullText} has been performed, and it
	 *  returns the offsets relevant for the last text submitted.
	 *
	 *  @return an array of offsets to the characters in the last submitted
	 *          complex expression before which directional marks must be
	 *          added to ensure correct presentation.
	 *          The offsets are sorted in ascending order.
	 *
	 */
	public int[] leanBidiCharOffsets();

	/** Get the offsets of characters in the <i>full</i> text of a
	 *  complex expression corresponding to directional formatting
	 *  characters which have been added in order to ensure correct presentation.
	 *  LRMs (for an expression with LTR base direction), RLMs (for
	 *  an expression with RTL base direction) are considered as well as
	 *  leading and trailing LRE, RLE and PDF which might be prefixed or suffixed
	 *  depending on the orientation of the GUI component used for display.
	 *  <p>
	 *  This method assumes that a successful call to
	 *  {@link #leanToFullText leanToFullText} has been performed, and it
	 *  returns the offsets relevant for the last text submitted.
	 *
	 *  @return an array of offsets to the characters in the <i>full</i>
	 *          text of the last submitted complex expression which are
	 *          directional formatting characters added to ensure correct
	 *          presentation.
	 *          The offsets are sorted in ascending order.
	 *
	 */
	public int[] fullBidiCharOffsets();

	/** Remove directional formatting characters which were added to a
	 *  complex expression to ensure correct presentation.
	 *
	 *  @param text is the text of the complex expression including
	 *         directional formatting characters.
	 *
	 *  @return the complex expression without directional formatting
	 *          characters which might have been added by processing it
	 *          with {@link #leanToFullText leanToFullText}.
	 *
	 */
	public String fullToLeanText(String text);

	/** Remove directional formatting characters which were added to a
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
	 *
	 *  @return the complex expression without directional formatting
	 *          characters which might have been added by processing it
	 *          with {@link #leanToFullText leanToFullText}.
	 *
	 *  @see #getFinalState
	 */
	public String fullToLeanText(String text, int initState);

	/** Retrieve the final state achieved in a previous call to
	 *  {@link #leanToFullText leanToFullText} or
	 *  {@link #fullToLeanText fullToLeanText},
	 *  {@link #leanBidiCharOffsets(java.lang.String text)} or
	 *  {@link #leanBidiCharOffsets(java.lang.String text, int initState)}.
	 *
	 *  @return the last final state.
	 *
	 */
	public int getFinalState();

	/** After transforming a <i>lean</i> string into a <i>full</i> string
	 *  using {@link #leanToFullText leanToFullText}, compute the index in the
	 *  <i>full</i> string of the character corresponding to the
	 *  character with the specified position in the <i>lean</i> string.
	 *
	 *  @param pos is the index of a character in the <i>lean</i> string.
	 *
	 *  @return the index of the corresponding character in the
	 *          <i>full</i> string.
	 *
	 */
	public int leanToFullPos(int pos);

	/** After transforming a <i>lean</i> string into a <i>full</i> string
	 *  using {@link #leanToFullText leanToFullText}, compute the index in the
	 *  <i>lean</i> string of the character corresponding to the
	 *  character with the specified position in the <i>full</i> string.
	 *
	 *  @param pos is the index of a character in the <i>full</i> string.
	 *
	 *  @return the index of the corresponding character in the
	 *          <i>lean</i> string. If there is no corresponding
	 *          character in the <i>lean</i> string (because the
	 *          specified character is a directional formatting character
	 *          added when invoking {@link #leanToFullText leanToFullText}),
	 *          the value returned will be that corresponding to the
	 *          next character which is not a directional formatting
	 *          character.
	 *
	 */
	public int fullToLeanPos(int pos);

	/** Set the operators which separate a complex expression into tokens.
	 *
	 *  @param  operators is a string where each character is an operator.
	 *  
	 *  @see #getOperators
	 */
	public void setOperators(String operators);

	/** Get the operators which separate a complex expression into tokens.
	 *
	 *  @return a string where each character is an operator.
	 *
	 *  @see #getOperators
	 */
	public String getOperators();

	/** Specify whether the GUI where the complex expression will be displayed
	 *  is mirrored (is laid out from right to left). The value specified in
	 *  this method overrides the default assumed for all complex expressions
	 *  as specified by
	 *  {@link ComplExpUtil#assumeMirroredDefault assumeMirroredDefault}.
	 *
	 *  @param  shouldBeMirrored must be specified as <code>false</code> if the GUI
	 *          is not mirrored, as <code>true</code> if it is.
	 *
	 *  @see #isMirrored
	 *  @see ComplExpUtil#assumeMirroredDefault assumeMirroredDefault
	 */
	public void assumeMirrored(boolean shouldBeMirrored);

	/** Retrieve the value assumed for GUI mirroring for the current
	 *  complex expression.
	 *
	 *  @return the current value assumed for GUI mirroring.
	 *
	 *  @see #assumeMirrored
	 */
	public boolean isMirrored();

	/** Specify the orientation (a.k.a. base direction) of the GUI
	 *  component in which the <i>full</i> text of the complex expression will
	 *  be displayed. If no orientation was specified, <code>ORIENT_LTR</code>
	 *  is assumed.
	 *  <p>
	 *  When the orientation is <code>ORIENT_RTL</code> and the complex
	 *  expression has a LTR base direction, {@link #leanToFullText leanToFullText}
	 *  adds LRE+LRM at the head of the <i>full</i> text and LRM+PDF at its
	 *  end.
	 *  <p>
	 *  When the orientation is <code>ORIENT_LTR</code> and the complex
	 *  expression has a RTL base direction, {@link #leanToFullText leanToFullText}
	 *  adds RLE+LRM at the head of the <i>full</i> text and RLM+PDF at its
	 *  end.
	 *  <p>
	 *  When the orientation is <code>ORIENT_CONTEXTUAL_LTR</code> or
	 *  <code>ORIENT_CONTEXTUAL_RTL</code> and the data content would resolve
	 *  to a RTL orientation while the complex expression has a LTR base
	 *  direction, {@link #leanToFullText leanToFullText}
	 *  adds LRM at the head of the <i>full</i> text.
	 *  <p>
	 *  When the orientation is <code>ORIENT_CONTEXTUAL_LTR</code> or
	 *  <code>ORIENT_CONTEXTUAL_RTL</code> and the data content would resolve
	 *  to a LTR orientation while the complex expression has a RTL base
	 *  direction, {@link #leanToFullText leanToFullText}
	 *  adds RLM at the head of the <i>full</i> text.
	 *  <p>
	 *  When the orientation is <code>ORIENT_UNKNOWN</code> and the complex
	 *  expression has a LTR base direction, {@link #leanToFullText leanToFullText}
	 *  adds LRE+LRM at the head of the <i>full</i> text and LRM+PDF at its
	 *  end.
	 *  <p>
	 *  When the orientation is <code>ORIENT_UNKNOWN</code> and the complex
	 *  expression has a RTL base direction, {@link #leanToFullText leanToFullText}
	 *  adds RLE+LRM at the head of the <i>full</i> text and RLM+PDF at its
	 *  end.
	 *  <p>
	 *  When the orientation is <code>ORIENT_IGNORE</code>,
	 *  {@link #leanToFullText leanToFullText} does not add any directional
	 *  formatting characters as either prefix or suffix of the <i>full</i> text.
	 *
	 *  @param componentOrientation orientation of the GUI component where the
	 *         complex expression will be displayed. It must be one of the
	 *         values {@link #ORIENT_LTR}, {@link #ORIENT_RTL},
	 *         {@link #ORIENT_CONTEXTUAL_LTR}, {@link #ORIENT_CONTEXTUAL_RTL},
	 *         {@link #ORIENT_UNKNOWN} or {@link #ORIENT_IGNORE}.
	 *
	 *  @see #recallOrientation
	 */
	public void assumeOrientation(int componentOrientation);

	/** Retrieve the value currently assumed for the orientation.
	 *
	 *  @return the current value of the orientation. It will be one of the
	 *         values {@link #ORIENT_LTR}, {@link #ORIENT_RTL},
	 *         {@link #ORIENT_CONTEXTUAL_LTR}, {@link #ORIENT_CONTEXTUAL_RTL},
	 *         {@link #ORIENT_UNKNOWN} or {@link #ORIENT_IGNORE}.
	 *
	 *  @see #assumeOrientation
	 */
	public int recallOrientation();

	/** Set the base direction of a complex expression including Arabic words.
	 *  It is specified separately for when the GUI is not mirrored and for
	 *  when it is mirrored. Each argument must be one of the values
	 *  {@link #DIRECTION_LTR} or {@link #DIRECTION_RTL}.
	 *  If the values are different for Arabic and Hebrew, the values for
	 *  Arabic will be used if an Arabic letter precedes all Hebrew letters
	 *  in the expression.
	 *
	 *  @param  not_mirrored base direction when the GUI is not mirrored.
	 *
	 *  @param  mirrored base direction when the GUI is mirrored.
	 *
	 *  @see #setHebrewDirection(int, int)
	 *  @see #getDirection
	 */
	public void setArabicDirection(int not_mirrored, int mirrored);

	/** Set the base direction of a complex expression including Arabic words.
	 *  This direction applies both for when the GUI is not mirrored and for
	 *  when it is mirrored.
	 *  If the values are different for Arabic and Hebrew, the values for
	 *  Arabic will be used if an Arabic letter precedes all Hebrew letters
	 *  in the expression.
	 *
	 *  @param  direction base direction of the expression.
	 *          It must be one of the values
	 *          {@link #DIRECTION_LTR} or {@link #DIRECTION_RTL}.
	 *
	 *  @see #setHebrewDirection(int)
	 *  @see #getDirection
	 */
	public void setArabicDirection(int direction);

	/** Set the base direction of a complex expression including Hebrew words.
	 *  It is specified separately for when the GUI is not mirrored and for
	 *  when it is mirrored. Each argument must be one of the values
	 *  {@link #DIRECTION_LTR} or {@link #DIRECTION_RTL}.
	 *  If the values are different for Arabic and Hebrew, the values for
	 *  Hebrew will be used if a Hebrew letter precedes all Arabic letters
	 *  in the expression.
	 *
	 *  @param  not_mirrored base direction when the GUI is not mirrored.
	 *
	 *  @param  mirrored base direction when the GUI is mirrored.
	 *
	 *  @see #setArabicDirection(int, int)
	 *  @see #getDirection
	 */
	public void setHebrewDirection(int not_mirrored, int mirrored);

	/** Set the base direction of a complex expression including Hebrew words.
	 *  This direction applies both for when the GUI is not mirrored and for
	 *  when it is mirrored.
	 *  If the values are different for Arabic and Hebrew, the values for
	 *  Hebrew will be used if a Hebrew letter precedes all Arabic letters
	 *  in the expression.
	 *
	 *  @param  direction base direction of the expression.
	 *          It must be one of the values
	 *          {@link #DIRECTION_LTR} or {@link #DIRECTION_RTL}.
	 *
	 *  @see #setArabicDirection(int)
	 *  @see #getDirection
	 */
	public void setHebrewDirection(int direction);

	/** Set the base direction of a complex expression.
	 *  It is specified separately for when the GUI is not mirrored and for
	 *  when it is mirrored. Each argument must be one of the values
	 *  {@link #DIRECTION_LTR} or {@link #DIRECTION_RTL}.
	 *  The values apply whether the expression contains Arabic or Hebrew
	 *  letters, or both.
	 *
	 *  @param  not_mirrored base direction when the GUI is not mirrored.
	 *
	 *  @param  mirrored base direction when the GUI is mirrored.
	 *
	 *  @see #setArabicDirection(int, int)
	 *  @see #setHebrewDirection(int, int)
	 *  @see #getDirection
	 */
	public void setDirection(int not_mirrored, int mirrored);

	/** Set the base direction of a complex expression.
	 *  This direction applies both for when the GUI is not mirrored and for
	 *  when it is mirrored.
	 *  The direction applies whether the expression contains Arabic or Hebrew
	 *  letters, or both.
	 *
	 *  @param  direction base direction of the expression.
	 *          It must be one of the values
	 *          {@link #DIRECTION_LTR} or {@link #DIRECTION_RTL}.
	 *
	 *  @see #setArabicDirection(int)
	 *  @see #setHebrewDirection(int)
	 *  @see #getDirection
	 */
	public void setDirection(int direction);

	/** Get the base direction of a complex expression.
	 *  Different base directions may have been specified depending on
	 *  whether the expression contains Arabic or Hebrew words
	 *  (if it contains both, the first Arabic or Hebrew letter in the
	 *  expression determines which is the governing script).
	 *  Different base directions may also have been specified depending on
	 *  whether the GUI is or is not mirrored.
	 *  The values for all the possible cases are returned in an array.
	 *  Each element of the array must be one of the values
	 *  {@link #DIRECTION_LTR} or {@link #DIRECTION_RTL}.
	 *
	 *  @return a two dimensional array with 2 rows and 2 columns such that
	 *  <ul>
	 *    <li>element [0][0] represents the base direction for Arabic
	 *        when the GUI is not mirrored.
	 *    <li>element [0][1] represents the base direction for Arabic
	 *        when the GUI is mirrored.
	 *    <li>element [1][0] represents the base direction for Hebrew
	 *        when the GUI is not mirrored.
	 *    <li>element [1][1] represents the base direction for Hebrew
	 *        when the GUI is mirrored.
	 *  </ul>
	 *
	 *  @see #setDirection
	 */
	public int[][] getDirection();

	/** Get the base direction of the current complex expression.
	 *  This base direction may depend on whether the GUI is mirrored and on
	 *  whether the expression contains Arabic or Hebrew words
	 *  (if it contains both, the first Arabic or Hebrew letter in the
	 *  expression determines which is the governing script).
	 *
	 *  @return the base direction of the current instance of a complex
	 *          expression. It must be one of the values
	 *  {@link #DIRECTION_LTR} or {@link #DIRECTION_RTL}.
	 *
	 *  @see #setDirection
	 */
	public int getCurDirection();
}
