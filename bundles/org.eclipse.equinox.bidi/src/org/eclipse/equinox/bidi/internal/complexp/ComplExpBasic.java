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
package org.eclipse.equinox.bidi.internal.complexp;

import org.eclipse.equinox.bidi.complexp.ComplExpUtil;
import org.eclipse.equinox.bidi.complexp.IComplExpProcessor;

/**
 *  <code>ComplExpBasic</code> is a
 *  complex expression processor for simple types of complex expressions.
 *  Normally, a group of <q>operators</q> is specified when creating a new
 *  instance.
 *  <i>Lean</i> text which is submitted to this processor is divided into
 *  segments by the operators.  The processor adds directional formatting
 *  characters into the lean text to generate a <i>full</i> text where
 *  operators and text segments will be presented in left-to-right sequence.
 *
 *  @see IComplExpProcessor
 *
 *  @author Matitiahu Allouche
 */
public class ComplExpBasic implements IComplExpProcessor {

	final private static String EMPTY_STRING = ""; //$NON-NLS-1$

	/**
	 *  Flag specifying that a specific instance of complex expression should
	 *  assume that the GUI is mirrored (globally going from right to left).
	 *  This flag overrides the default flag <code>mirroredDefault</code>
	 *  for the parent complex expression instance.
	 *  @see #assumeMirrored
	 *  @see #isMirrored
	 *  @see ComplExpUtil#mirroredDefault
	 */
	protected boolean mirrored = ComplExpUtil.isMirroredDefault();

	/**
	 *  Orientation that should be assumed for the text component where the
	 *  complex expression will be displayed. It can be specified using
	 *  {@link #assumeOrientation}.
	 *  It must be one of the values
	 *  {@link IComplExpProcessor#ORIENT_LTR ORIENT_LTR},
	 *  {@link IComplExpProcessor#ORIENT_LTR ORIENT_RTL},
	 *  {@link IComplExpProcessor#ORIENT_CONTEXTUAL_LTR ORIENT_CONTEXTUAL_LTR} or
	 *  {@link IComplExpProcessor#ORIENT_CONTEXTUAL_RTL ORIENT_CONTEXTUAL_RTL}.
	 *  {@link IComplExpProcessor#ORIENT_UNKNOWN ORIENT_UNKNOWN}.
	 *  {@link IComplExpProcessor#ORIENT_IGNORE ORIENT_IGNORE}.
	 *  The default is <code>ORIENT_LTR</code>.
	 *  <p>
	 *  The orientation affects the addition of directional formatting
	 *  characters as prefix and/or suffix when generating the <i>full</i>
	 *  text of the complex expression.
	 *
	 *  @see #assumeOrientation
	 *  @see #recallOrientation
	 *  @see #leanToFullText
	 */
	protected int orientation;

	/**
	 *  Type of the complex expression processor specified when calling
	 *  {@link ComplExpUtil#create}
	 */
	protected String type;

	/**
	 *  Base direction of the complex expression. This is an array such that
	 *  <ul>
	 *    <li><code>direction[0][0]</code> represents the base direction
	 *        for Arabic when the GUI is not mirrored.
	 *    <li><code>direction[0][1]</code> represents the base direction
	 *        for Arabic when the GUI is mirrored.
	 *    <li><code>direction[1][0]</code> represents the base direction
	 *        for Hebrew when the GUI is not mirrored.
	 *    <li><code>direction[1][1]</code> represents the base direction
	 *        for Hebrew when the GUI is mirrored.
	 *  </ul>
	 *  Each of the elements in the array must be one of the values
	 *  {@link IComplExpProcessor#DIRECTION_LTR DIRECTION_LTR},
	 *  {@link IComplExpProcessor#DIRECTION_LTR DIRECTION_RTL}.
	 *  The default is <code>DIRECTION_LTR</code>.
	 *
	 *  @see #setDirection(int) setDirection
	 *  @see #getDirection getDirection
	 */
	protected int[][] direction = new int[2][2];

	/**
	 *  This field represents the final state achieved in a previous call to
	 *  {@link IComplExpProcessor#leanToFullText leanToFullText} or
	 *  {@link IComplExpProcessor#fullToLeanText fullToLeanText},
	 *  {@link IComplExpProcessor#leanBidiCharOffsets(java.lang.String text)} or
	 *  {@link IComplExpProcessor#leanBidiCharOffsets(java.lang.String text, int initState)}.
	 *  The <code>state</code> is an opaque value which makes sense only
	 *  within calls to a same complex expression processor.
	 *  The only externalized value is
	 *  {@link IComplExpProcessor#STATE_NOTHING_GOING} which means that
	 *  there is nothing to remember from the last call.
	 *  <p>
	 *  <code>state</code> should be used only for complex expressions
	 *  which span more than one line, when the user makes a separate call to
	 *  <code>leanToFullText</code>, <code>fullToLeanText</code> or
	 *  <code>leanBidiCharOffsets</code> for each
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
	 *  @see IComplExpProcessor#getFinalState
	 *  @see IComplExpProcessor#leanToFullText(String text, int initState)
	 *  @see IComplExpProcessor#fullToLeanText(String text, int initState)
	 *  @see IComplExpProcessor#leanBidiCharOffsets(String text, int initState)
	 */
	protected int state = STATE_NOTHING_GOING;

	private boolean ignoreArabic, ignoreHebrew;

	public static final byte B = Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR;
	public static final byte L = Character.DIRECTIONALITY_LEFT_TO_RIGHT;
	public static final byte R = Character.DIRECTIONALITY_RIGHT_TO_LEFT;
	public static final byte AL = Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
	public static final byte AN = Character.DIRECTIONALITY_ARABIC_NUMBER;
	public static final byte EN = Character.DIRECTIONALITY_EUROPEAN_NUMBER;
	public static final char LRM = 0x200E;
	public static final char RLM = 0x200F;
	public static final char LRE = 0x202A;
	public static final char RLE = 0x202B;
	public static final char PDF = 0x202C;
	public static final char[] MARKS = {LRM, RLM};
	public static final char[] EMBEDS = {LRE, RLE};
	public static final byte[] STRONGS = {L, R};
	public static final int PREFIX_LENGTH = 2;
	public static final int SUFFIX_LENGTH = 2;
	public static final int FIXES_LENGTH = PREFIX_LENGTH + SUFFIX_LENGTH;

	private byte curStrong = -1;
	private char curMark;
	private char curEmbed;
	private int prefixLength;
	char[] operators;
	int operCount;
	int[] locations;
	int specialsCount;
	int nextLocation;
	int len;
	private String leanText;
	private int idxLocation;
	private int[] offsets;
	private int count, countLimit;
	private int curPos;
	// For positions where it has been looked up, the entry will receive
	// the Character directionality + 2 (so that 0 indicates that the
	// the directionality has not been looked up yet.
	private byte[] dirProps;
	// current UI orientation (after resolution if contextual)
	private int curOrient = -1;
	// Current expression base direction (after resolution if depending on
	// script and/or on GUI mirroring (0=LTR, 1=RTL))
	private int curDirection = -1;

	/**
	 *  Constructor specifying operators. The processor will add directional
	 *  formatting characters to <i>lean</i> text to generate a <i>full</i>
	 *  text so that the operators and the segments of text between operators
	 *  are displayed from left to right, while each segment is displayed
	 *  according to the <a href="http://www.unicode.org/reports/tr9/">UBA</a>.
	 *
	 *  @param operators string grouping one-character operators which
	 *         separate the text of the complex expression into segments.
	 */
	public ComplExpBasic(String operators) {
		this.operators = operators.toCharArray();
		operCount = this.operators.length;
		locations = new int[operCount];
	}

	public void setOperators(String operators) {
		this.operators = operators.toCharArray();
		operCount = this.operators.length;
		locations = new int[operCount + specialsCount];
	}

	public String getOperators() {
		return new String(operators);
	}

	/**
	 *  Constructor specifying operators and a number of special cases.
	 *  In <code>ComplExpBasic</code> the special cases are implemented as
	 *  doing nothing. Subclasses of <code>ComplExpBasic</code> may override
	 *  methods <code>indexOfSpecial</code> and <code>processSpecial</code>
	 *  to provide specific handling for the special cases.
	 *  Examples of special cases are comments, literals, or anything which
	 *  is not identified by a one-character operator.
	 *  <p>
	 *  Independently of the special cases, the processor will add directional
	 *  formatting characters to <i>lean</i> text to generate a <i>full</i>
	 *  text so that the operators and the segments of text between operators
	 *  are displayed from left to right, while each segment is displayed
	 *  according to the <a href="http://www.unicode.org/reports/tr9/">UBA</a>.
	 *
	 *  @param operators string grouping one-character operators which
	 *         separate the text of the complex expression into segments.
	 *
	 *  @param specialsCount number of special cases supported by this
	 *         class. Handling of the special cases is implemented with
	 *         methods <code>indexOfSpecial</code> and
	 *         <code>processSpecial</code>.
	 *
	 *  @see #indexOfSpecial indexOfSpecial
	 *  @see #processSpecial processSpecial
	 */
	public ComplExpBasic(String operators, int specialsCount) {
		this.operators = operators.toCharArray();
		operCount = this.operators.length;
		this.specialsCount = specialsCount;
		locations = new int[operCount + specialsCount];
	}

	public void selectBidiScript(boolean arabic, boolean hebrew) {
		ignoreArabic = !arabic;
		ignoreHebrew = !hebrew;
	}

	public boolean handlesArabicScript() {
		return !ignoreArabic;
	}

	public boolean handlesHebrewScript() {
		return !ignoreHebrew;
	}

	void computeNextLocation() {
		nextLocation = len;
		// Start with special sequences to give them precedence over simple
		// operators. This may apply to cases like slash+asterisk versus slash.
		for (int i = 0; i < specialsCount; i++) {
			int loc = locations[operCount + i];
			if (loc < curPos) {
				loc = indexOfSpecial(i, leanText, curPos);
				if (loc < 0)
					loc = len;
				locations[operCount + i] = loc;
			}
			if (loc < nextLocation) {
				nextLocation = loc;
				idxLocation = operCount + i;
			}
		}
		for (int i = 0; i < operCount; i++) {
			int loc = locations[i];
			if (loc < curPos) {
				loc = leanText.indexOf(operators[i], curPos);
				if (loc < 0)
					loc = len;
				locations[i] = loc;
			}
			if (loc < nextLocation) {
				nextLocation = loc;
				idxLocation = i;
			}
		}
	}

	/**
	 *  This method is called repeatedly by <code>leanToFullText</code> to
	 *  locate all special cases specified in the constructor
	 *  (see {@link #ComplExpBasic(String operators, int specialsCount)}),
	 *  varying <code>whichSpecial</code> from zero to
	 *  <code>specialsCount - 1</code>. It is meant to be overridden in
	 *  subclasses of <code>ComplExpBasic</code>.
	 *  Those subclasses may use methods
	 *  {@link #getDirProp getDirProp},
	 *  {@link #setDirProp setDirProp} and
	 *  {@link #insertMark insertMark} within <code>indexOfSpecial</code>.
	 *
	 *  @param  whichSpecial number of the special case to locate. The meaning
	 *          of this number is internal to the class implementing
	 *          <code>indexOfSpecial</code>.
	 *
	 *  @param  srcText text of the complex expression before addition of any
	 *          directional formatting characters.
	 *
	 *  @param  fromIndex the index within <code>srcText</code> to start
	 *          the search from.
	 *
	 *  @return the position where the start of the special case was located.
	 *          The method must return the first occurrence of whatever
	 *          identifies the start of the special case after
	 *          <code>fromIndex</code>. The method does not have to check if
	 *          this occurrence appears within the scope of another special
	 *          case (e.g. a comment starting delimiter within the scope of
	 *          a literal or vice-versa).
	 *          If no occurrence is found, the method must return -1.
	 *  <p>
	 *          In <code>ComplExpBasic</code> this method always returns -1.
	 *
	 */
	protected int indexOfSpecial(int whichSpecial, String srcText, int fromIndex) {
		// This method must be overridden by all subclasses with special cases.
		return -1;
	}

	/**
	 *  This method is called by <code>leanToFullText</code>
	 *  when a special case occurrence is located by
	 *  {@link #indexOfSpecial indexOfSpecial}.
	 *  It is meant to be overridden in subclasses of <code>ComplExpBasic</code>.
	 *  Those subclasses may use methods
	 *  {@link #getDirProp getDirProp},
	 *  {@link #setDirProp setDirProp} and
	 *  {@link #insertMark insertMark} within <code>processSpecial</code>.
	 *  <p>
	 *  If a special processing cannot be completed within a current call to
	 *  <code>processSpecial</code> (for instance, a comment has been started
	 *  in the current line but its end appears in a following line),
	 *  <code>processSpecial</code> should put in the {@link #state}
	 *  member of the class instance a number which characterizes this
	 *  situation. On a later call to
	 *  {@link IComplExpProcessor#leanToFullText(String text, int initState)}
	 *  specifying that state value, <code>processSpecial</code> will be
	 *  called with that value for parameter <code>whichSpecial</code> and
	 *  <code>-1</code> for parameter <code>operLocation</code> and should
	 *  perform whatever initializations are required depending on the state.
	 *
	 *  @param  whichSpecial number of the special case to handle.
	 *
	 *  @param  srcText text of the complex expression.
	 *
	 *  @param  operLocation the position returned by <code>indexOfSpecial</code>.
	 *          In calls to <code>leanToFullText</code> or
	 *          <code>fullToLeanText</code> specifying an <code>initState</code>
	 *          parameter, <code>processSpecial</code> is called when initializing
	 *          the processing with the value of <code>whichSpecial</code>
	 *          equal to <code>initState</code> and the value of
	 *          <code>operLocation</code> equal to <code>-1</code>.
	 *
	 *  @return the position after the scope of the special case ends.
	 *          For instance, the position after the end of a comment,
	 *          the position after the end of a literal.
	 *  <p>
	 *          In <code>ComplExpBasic</code> this method always returns
	 *          <code>operLocation + 1</code> (but it should never be called).
	 *
	 */
	protected int processSpecial(int whichSpecial, String srcText, int operLocation) {
		// This method must be overridden by all subclasses with any special case.
		return operLocation + 1;
	}

	private int getCurOrient() {
		if (curOrient >= 0)
			return curOrient;

		if ((orientation & ORIENT_CONTEXTUAL_LTR) == 0) {
			// absolute orientation
			curOrient = orientation;
			return curOrient;
		}
		// contextual orientation
		byte dirProp;
		for (int i = 0; i < len; i++) {
			dirProp = dirProps[i];
			if (dirProp == 0) {
				dirProp = Character.getDirectionality(leanText.charAt(i));
				if (dirProp == B) // B char resolves to L or R depending on orientation
					continue;
				dirProps[i] = (byte) (dirProp + 2);
			} else {
				dirProp -= 2;
			}
			if (dirProp == L) {
				curOrient = ORIENT_LTR;
				return curOrient;
			}
			if (dirProp == R || dirProp == AL) {
				curOrient = ORIENT_RTL;
				return curOrient;
			}
		}
		curOrient = orientation & 1;
		return curOrient;
	}

	public int getCurDirection() {
		if (curDirection >= 0)
			return curDirection;

		curStrong = -1;
		int idx2 = mirrored ? 1 : 0;
		// same direction for Arabic and Hebrew?
		if (direction[0][idx2] == direction[1][idx2]) {
			curDirection = direction[0][idx2];
			return curDirection;
		}
		// check if Arabic or Hebrew letter comes first
		byte dirProp;
		for (int i = 0; i < len; i++) {
			dirProp = getDirProp(i);
			if (dirProp == AL) {
				curDirection = direction[0][idx2];
				return curDirection;
			}
			if (dirProp == R) {
				curDirection = direction[1][idx2];
				return curDirection;
			}
		}
		// found no Arabic or Hebrew character
		curDirection = DIRECTION_LTR;
		return curDirection;
	}

	private void setMarkAndFixes() {
		int dir = getCurDirection();
		if (curStrong == STRONGS[dir])
			return;
		curStrong = STRONGS[dir];
		curMark = MARKS[dir];
		curEmbed = EMBEDS[dir];
	}

	/**
	 *  This method can be called from within {@link #indexOfSpecial indexOfSpecial}
	 *  or {@link #processSpecial processSpecial} in subclasses of
	 *  <code>ComplExpBasic</code> to retrieve the bidirectional class of
	 *  characters in <code>leanText</code>.
	 *
	 *  @param index position of the character in <code>leanText</code>.
	 *
	 *  @return the bidirectional class of the character. It is one of the
	 *          values which can be returned by
	 *          <code>java.lang.Character#getDirectionality</code>.
	 *          However, it is recommended to use <code>getDirProp</code>
	 *          rather than <code>java.lang.Character.getDirectionality</code>
	 *          since <code>getDirProp</code> manages a cache of character
	 *          properties and so can be more efficient than calling the
	 *          java.lang.Character method.
	 *
	 */
	protected byte getDirProp(int index) {
		byte dirProp = dirProps[index];
		if (dirProp == 0) {
			dirProp = Character.getDirectionality(leanText.charAt(index));
			if (dirProp == B)
				dirProp = getCurOrient() == ORIENT_RTL ? R : L;
			dirProps[index] = (byte) (dirProp + 2);
			return dirProp;
		}
		return (byte) (dirProp - 2);
	}

	/**
	 *  This method can be called from within {@link #indexOfSpecial indexOfSpecial}
	 *  or {@link #processSpecial processSpecial} in subclasses of
	 *  <code>ComplExpBasic</code> to set or override the bidirectional
	 *  class of characters in <code>leanText</code>.
	 *
	 *  @param index position of the character in <code>leanText</code>.
	 *
	 *  @param dirProp bidirectional class of the character. It is one of the
	 *         values which can be returned by
	 *         <code>java.lang.Character.getDirectionality</code>.
	 *
	 */
	protected void setDirProp(int index, byte dirProp) {
		dirProps[index] = (byte) (dirProp + 2);
	}

	/**
	 *  This method can be called from within
	 *  {@link #processSpecial processSpecial} in subclasses of
	 *  <code>ComplExpBasic</code> to add a directional mark before an
	 *  operator if needed for correct display, depending on the
	 *  base direction of the expression and on the class of the
	 *  characters in <code>leanText</code> preceding and following
	 *  the operator itself.
	 *
	 *  @param  operLocation offset of the operator in <code>leanText</code>.
	 *
	 */
	protected void processOperator(int operLocation) {
		boolean doneAN = false;

		if (getCurDirection() == DIRECTION_RTL) {
			// the expression base direction is RTL
			for (int i = operLocation - 1; i >= 0; i--) {
				byte dirProp = getDirProp(i);
				if (dirProp == R || dirProp == AL)
					return;

				if (dirProp == L) {
					for (int j = operLocation; j < len; j++) {
						dirProp = getDirProp(j);
						if (dirProp == R || dirProp == AL)
							return;
						if (dirProp == L || dirProp == EN) {
							insertMark(operLocation);
							return;
						}
					}
					return;
				}
			}
			return;
		}

		// the expression base direction is LTR
		if (ignoreArabic) {
			if (ignoreHebrew) /* process neither Arabic nor Hebrew */
				return;
			/* process Hebrew, not Arabic */
			for (int i = operLocation - 1; i >= 0; i--) {
				byte dirProp = getDirProp(i);
				if (dirProp == L)
					return;
				if (dirProp == R) {
					for (int j = operLocation; j < len; j++) {
						dirProp = getDirProp(j);
						if (dirProp == L)
							return;
						if (dirProp == R || dirProp == EN) {
							insertMark(operLocation);
							return;
						}
					}
					return;
				}
			}
		} else {
			if (ignoreHebrew) { /* process Arabic, not Hebrew */
				for (int i = operLocation - 1; i >= 0; i--) {
					byte dirProp = getDirProp(i);
					if (dirProp == L)
						return;
					if (dirProp == AL) {
						for (int j = operLocation; j < len; j++) {
							dirProp = getDirProp(j);
							if (dirProp == L)
								return;
							if (dirProp == EN || dirProp == AL || dirProp == AN) {
								insertMark(operLocation);
								return;
							}
						}
						return;
					}
					if (dirProp == AN && !doneAN) {
						for (int j = operLocation; j < len; j++) {
							dirProp = getDirProp(j);
							if (dirProp == L)
								return;
							if (dirProp == AL || dirProp == AN) {
								insertMark(operLocation);
								return;
							}
						}
						doneAN = true;
					}
				}
			} else { /* process Arabic and Hebrew */
				for (int i = operLocation - 1; i >= 0; i--) {
					byte dirProp = getDirProp(i);
					if (dirProp == L)
						return;
					if (dirProp == R || dirProp == AL) {
						for (int j = operLocation; j < len; j++) {
							dirProp = getDirProp(j);
							if (dirProp == L)
								return;
							if (dirProp == R || dirProp == EN || dirProp == AL || dirProp == AN) {
								insertMark(operLocation);
								return;
							}
						}
						return;
					}
					if (dirProp == AN && !doneAN) {
						for (int j = operLocation; j < len; j++) {
							dirProp = getDirProp(j);
							if (dirProp == L)
								return;
							if (dirProp == AL || dirProp == AN || dirProp == R) {
								insertMark(operLocation);
								return;
							}
						}
						doneAN = true;
					}
				}
			}
		}
	}

	public int getFinalState() {
		return state;
	}

	public String leanToFullText(String text) {
		return leanToFullText(text, STATE_NOTHING_GOING);
	}

	public String leanToFullText(String text, int initState) {
		if (text.length() == 0) {
			prefixLength = 0;
			count = 0;
			return text;
		}
		leanToFullTextNofix(text, initState);
		return addMarks(true);
	}

	void leanToFullTextNofix(String text, int initState) {
		leanText = text;
		len = leanText.length();
		offsets = new int[20];
		count = 0;
		countLimit = offsets.length - 1;
		dirProps = new byte[len];
		curOrient = -1;
		curDirection = -1;
		curPos = 0;
		// initialize locations
		int k = locations.length;
		for (int i = 0; i < k; i++) {
			locations[i] = -1;
		}
		state = STATE_NOTHING_GOING;
		nextLocation = -1;
		if (initState != STATE_NOTHING_GOING)
			curPos = processSpecial(initState, leanText, -1);

		while (true) {
			computeNextLocation();
			if (nextLocation >= len)
				break;
			if (idxLocation < operCount) {
				processOperator(nextLocation);
				curPos = nextLocation + 1;
			} else {
				curPos = processSpecial(idxLocation - operCount, leanText, nextLocation);
			}
		}
	}

	public int[] leanBidiCharOffsets(String text) {
		return leanBidiCharOffsets(text, STATE_NOTHING_GOING);
	}

	public int[] leanBidiCharOffsets(String text, int initState) {
		leanToFullTextNofix(text, initState);
		return leanBidiCharOffsets();
	}

	public int[] leanBidiCharOffsets() {
		int[] result = new int[count];
		System.arraycopy(offsets, 0, result, 0, count);
		return result;
	}

	public int[] fullBidiCharOffsets() {
		int lim = count;
		if (prefixLength > 0) {
			if (prefixLength == 1)
				lim++;
			else
				lim += FIXES_LENGTH;
		}
		int[] fullOffsets = new int[lim];
		for (int i = 0; i < prefixLength; i++) {
			fullOffsets[i] = i;
		}
		int added = prefixLength;
		for (int i = 0; i < count; i++) {
			fullOffsets[prefixLength + i] = offsets[i] + added;
			added++;
		}
		if (prefixLength > 1) {
			fullOffsets[lim - 2] = len + lim - 2;
			fullOffsets[lim - 1] = len + lim - 1;
		}
		return fullOffsets;
	}

	public String fullToLeanText(String text) {
		return fullToLeanText(text, STATE_NOTHING_GOING);
	}

	public String fullToLeanText(String text, int initState) {
		int i; // TBD this variable is used for multiple unrelated tasks
		setMarkAndFixes();
		// remove any prefix and leading mark
		int lenText = text.length();
		for (i = 0; i < lenText; i++) {
			char c = text.charAt(i);
			if (c != curEmbed && c != curMark)
				break;
		}
		if (i > 0) {
			text = text.substring(i);
			lenText = text.length();
		}
		// remove any suffix and trailing mark
		for (i = lenText - 1; i >= 0; i--) {
			char c = text.charAt(i);
			if (c != PDF && c != curMark)
				break;
		}
		if (i < 0) {
			leanText = EMPTY_STRING;
			len = 0;
			prefixLength = 0;
			count = 0;
			return leanText;
		}
		if (i < (lenText - 1)) {
			text = text.substring(0, i + 1);
			lenText = text.length();
		}
		char[] chars = text.toCharArray();
		// remove marks from chars
		int cnt = 0;
		for (i = 0; i < lenText; i++) {
			char c = chars[i];
			if (c == curMark)
				cnt++;
			else if (cnt > 0)
				chars[i - cnt] = c;
		}
		String lean = new String(chars, 0, lenText - cnt);
		leanToFullTextNofix(lean, initState);
		String full = addMarks(false); /* only marks, no prefix/suffix */
		if (full.equals(text))
			return lean;

		// There are some marks in full which are not in text and/or vice versa.
		// We need to add to lean any mark appearing in text and not in full.
		// The completed lean can never be longer than text itself.
		char[] newChars = new char[lenText];
		char cFull, cText;
		int idxFull, idxText, idxLean, markPos, newCharsPos;
		int lenFull = full.length();
		idxFull = idxText = idxLean = newCharsPos = 0;
		while (idxText < lenText && idxFull < lenFull) {
			cFull = full.charAt(idxFull);
			cText = text.charAt(idxText);
			if (cFull == cText) { /* chars are equal, proceed */
				idxText++;
				idxFull++;
				continue;
			}
			if (cFull == curMark) { /* extra Mark in full text */
				idxFull++;
				continue;
			}
			if (cText == curMark) { /* extra Mark in source full text */
				idxText++;
				// idxText-2 always >= 0 since leading Marks were removed from text
				if (text.charAt(idxText - 2) == curMark)
					continue; // ignore successive Marks in text after the first one
				markPos = fullToLeanPos(idxFull);
				// copy from chars (== lean) to newChars until here
				for (i = idxLean; i < markPos; i++) {
					newChars[newCharsPos++] = chars[i];
				}
				idxLean = markPos;
				newChars[newCharsPos++] = curMark;
				continue;
			}
			// we should never get here (extra char which is not a Mark)
			throw new IllegalStateException("Internal error: extra character not a Mark."); //$NON-NLS-1$
		}
		if (idxText < lenText) /* full ended before text - this should never happen */
			throw new IllegalStateException("Internal error: unexpected EOL."); //$NON-NLS-1$

		// copy the last part of chars to newChars
		for (i = idxLean; i < lean.length(); i++) {
			newChars[newCharsPos++] = chars[i];
		}
		lean = new String(newChars, 0, newCharsPos);
		leanText = lean;
		len = leanText.length();
		return lean;
	}

	public int leanToFullPos(int pos) {
		int added = prefixLength;
		for (int i = 0; i < count; i++) {
			if (offsets[i] <= pos)
				added++;
			else
				return pos + added;
		}
		return pos + added;
	}

	public int fullToLeanPos(int pos) {
		pos -= prefixLength;
		int added = 0;
		for (int i = 0; i < count; i++) {
			if ((offsets[i] + added) < pos)
				added++;
			else
				break;
		}
		pos -= added;
		if (pos < 0)
			pos = 0;
		else if (pos > len)
			pos = len;
		return pos;
	}

	/**
	 *  This method can be called from within {@link #indexOfSpecial} or
	 *  {@link #processSpecial} in subclasses of <code>ComplExpBasic</code>
	 *  to specify that a mark character must be added before the character
	 *  at the specified position of the <i>lean</i> text when generating the
	 *  <i>full</i> text. The mark character will be LRM for complex expressions
	 *  with a LTR base direction, and RLM for complex expressions with RTL
	 *  base direction. The mark character is not added physically by this
	 *  method, but its position is noted and will be used when generating
	 *  the <i>full</i> text.
	 *
	 *  @param  offset position of the character in <code>leanText</code>.
	 *          It must be a non-negative number smaller than the length
	 *          of the <i>lean</i> text.
	 *          For the benefit of efficiency, it is better to insert
	 *          multiple marks in ascending order of the offsets.
	 *
	 */
	protected void insertMark(int offset) {
		int index = count - 1; // index of greatest member <= offset
		// look up after which member the new offset should be inserted
		while (index >= 0) {
			int wrkOffset = offsets[index];
			if (offset > wrkOffset)
				break;
			if (offset == wrkOffset)
				return; // avoid duplicates
			index--;
		}
		index++; // index now points at where to insert
		// check if we have an available slot for new member
		if (count >= countLimit) {
			int[] newOffsets = new int[offsets.length * 2];
			System.arraycopy(offsets, 0, newOffsets, 0, count);
			offsets = newOffsets;
			countLimit = offsets.length - 1;
		}

		int length = count - index; // number of members to move up
		if (length > 0) // shift right all members greater than offset
			System.arraycopy(offsets, index, offsets, index + 1, length);

		offsets[index] = offset;
		count++;
		// if the offset is 0, adding a mark does not change anything
		if (offset < 1)
			return;

		byte dirProp = getDirProp(offset);
		// if the current char is a strong one or a digit, we change the
		//   dirProp of the previous char to account for the inserted mark
		if (dirProp == L || dirProp == R || dirProp == AL || dirProp == EN || dirProp == AN)
			index = offset - 1;
		else
			// if the current char is a neutral, we change its own dirProp
			index = offset;
		setMarkAndFixes();
		setDirProp(index, curStrong);
	}

	private String addMarks(boolean addFixes) {
		// add prefix/suffix only if addFixes is true
		if ((count == 0) && (!addFixes || (getCurOrient() == getCurDirection()) || (curOrient == ORIENT_IGNORE))) {
			prefixLength = 0;
			return leanText;
		}
		int newLen = len + count;
		if (addFixes && ((getCurOrient() != getCurDirection()) || (curOrient == ORIENT_UNKNOWN))) {
			if ((orientation & ORIENT_CONTEXTUAL_LTR) == 0) {
				prefixLength = PREFIX_LENGTH;
				newLen += FIXES_LENGTH;
			} else { /* contextual orientation */
				prefixLength = 1;
				newLen++; /* +1 for a mark char */
			}
		} else {
			prefixLength = 0;
		}
		char[] fullChars = new char[newLen];
		// add a dummy offset as fence
		offsets[count] = len;
		int added = prefixLength;
		// add marks at offsets
		setMarkAndFixes();
		for (int i = 0, j = 0; i < len; i++) {
			char c = leanText.charAt(i);
			if (i == offsets[j]) {
				fullChars[i + added] = curMark;
				added++;
				j++;
			}
			fullChars[i + added] = c;
		}
		if (prefixLength > 0) { /* add prefix/suffix ? */
			if (prefixLength == 1) { /* contextual orientation */
				fullChars[0] = curMark;
			} else {
				// When the orientation is RTL, we need to add EMBED at the
				// start of the text and PDF at its end.
				// However, because of a bug in Windows' handling of LRE/PDF,
				// we add EMBED_PREFIX at the start and EMBED_SUFFIX at the end.
				fullChars[0] = curEmbed;
				fullChars[1] = curMark;
				fullChars[newLen - 1] = PDF;
				fullChars[newLen - 2] = curMark;
			}
		}
		return new String(fullChars);
	}

	public void assumeMirrored(boolean shouldBeMirrored) {
		mirrored = shouldBeMirrored;
		curDirection = -1;
	}

	public boolean isMirrored() {
		return mirrored;
	}

	public void assumeOrientation(int componentOrientation) {
		if (componentOrientation < ORIENT_LTR || componentOrientation > ORIENT_IGNORE)
			orientation = ORIENT_UNKNOWN; // TBD should throw new IllegalArgumentException()?
		else
			orientation = componentOrientation;
	}

	public int recallOrientation() {
		return orientation;
	}

	public void setArabicDirection(int not_mirrored, int mirrored) {
		direction[0][0] = not_mirrored & 1;
		direction[0][1] = mirrored & 1;
		curDirection = -1;
	}

	public void setArabicDirection(int direction) {
		setArabicDirection(direction, direction);
	}

	public void setHebrewDirection(int not_mirrored, int mirrored) {
		direction[1][0] = not_mirrored & 1;
		direction[1][1] = mirrored & 1;
		curDirection = -1;
	}

	public void setHebrewDirection(int direction) {
		setHebrewDirection(direction, direction);
	}

	public void setDirection(int not_mirrored, int mirrored) {
		setArabicDirection(not_mirrored, mirrored);
		setHebrewDirection(not_mirrored, mirrored);
	}

	public void setDirection(int direction) {
		setDirection(direction, direction);
	}

	public int[][] getDirection() {
		return direction;
	}
}
