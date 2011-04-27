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

import org.eclipse.equinox.bidi.BidiComplexEnvironment;

/**
 *  This class defines features of a complex expression processor.
 *  <p>
 *  All public fields in this class are <code>final</code>, i.e. cannot be
 *  changed after creating an instance.
 *
 *  <h2>Code Sample</h2>
 *  <p>Example 1 (set all features)
 *  <pre>
 *
 *    BidiComplexFeatures f1 = new BidiComplexFeatures("+-=", 0, -1, -1, false, false);
 *
 *  </pre>
 *  <p>Example 2 (change only the operators)
 *  <pre>
 *
 *    BidiComplexFeatures f2 = new BidiComplexFeatures("[]|()", f1.getSpecialsCount(),
 *                                     f1.getDirArabic(), f1.getDirHebrew(),
 *                                     f1.getIgnoreArabic(), f1.getIgnoreHebrew());
 *
 *  </pre>
 *
 *  @see IBidiComplexProcessor#getFeatures
 *
 *  @author Matitiahu Allouche
 */
public class BidiComplexFeatures {

	/**
	 *  Constant specifying that the base direction of a complex expression is LTR.
	 *  The base direction may depend on whether the GUI is
	 *  {@link BidiComplexEnvironment#getMirrored mirrored} and may
	 *  may be different for Arabic and for Hebrew.
	 *  This constant can appear as <code>dirArabic</code>
	 *  or <code>dirHebrew</code> argument for the
	 *  {@link BidiComplexFeatures#BidiComplexFeatures BidiComplexFeatures constructor}
	 *  and as value returned by {@link #getDirArabic} or {@link #getDirHebrew}
	 *  methods.
	 */
	public static final int DIR_LTR = 0;

	/**
	 *  Constant specifying that the base direction of a complex expression is RTL.
	 *  The base direction may depend on whether the GUI is
	 *  {@link BidiComplexEnvironment#getMirrored mirrored} and may
	 *  may be different for Arabic and for Hebrew.
	 *  This constant can appear as <code>dirArabic</code>
	 *  or <code>dirHebrew</code> argument for the
	 *  {@link BidiComplexFeatures#BidiComplexFeatures BidiComplexFeatures constructor}
	 *  and as value returned by {@link #getDirArabic} or {@link #getDirHebrew}
	 *  methods.
	 */
	public static final int DIR_RTL = 1;

	/**
	 *  Pre-defined <code>BidiComplexFeatures</code> instance with values for no
	 *  operators, no special processing, all directions LTR
	 *  and support for neither Arabic nor Hebrew.<br>
	 *  Since there are no operators and no special processing, a complex
	 *  expression processor with such features would do nothing.<br>
	 *  It is more efficient to do nothing with a <code>null</code> processor.
	 */
	public static final BidiComplexFeatures DEFAULT = new BidiComplexFeatures(null, 0, -1, -1, true, true);

	/**
	 *  String grouping one-character operators which
	 *  separate the text of the complex expression into tokens.
	 */
	final String operators;

	/**
	 *  Number of special cases for the associated processor.
	 *  Special cases exist for some types of complex expression processors.
	 *  They are implemented by overriding methods
	 *  {@link IBidiComplexProcessor#indexOfSpecial indexOfSpecial} and
	 *  {@link IBidiComplexProcessor#processSpecial processSpecial}.
	 *  Examples of special cases are comments, literals, or anything which
	 *  is not identified by a one-character operator.
	 */
	final int specialsCount;

	/**
	 *  Base direction of the complex expression for Arabic.
	 *  If a complex expression contains both Arabic and
	 *  Hebrew words, the first Arabic or Hebrew letter in the
	 *  expression determines which is the governing script).<br>
	 *  The value of this field must be one of
	 *  {@link #DIR_LTR} or {@link #DIR_RTL}.
	 *
	 *  @see #dirHebrew
	 */
	final int dirArabic;

	/**
	 *  Base direction of the complex expression for Hebrew.
	 *  If a complex expression contains both Arabic and
	 *  Hebrew words, the first Arabic or Hebrew letter in the
	 *  expression determines which is the governing script).<br>
	 *  The value of this field must be one of
	 *  {@link #DIR_LTR} or {@link #DIR_RTL}.
	 *
	 *  @see #dirArabic
	 */
	final int dirHebrew;

	/**
	 *  Flag indicating that Arabic letters will not be considered for
	 *  processing complex expressions. If both this flag and
	 *  ignoreHebrew are set to <code>true</code>, the
	 *  processor will do nothing (but some overhead can be expected).
	 */
	final boolean ignoreArabic;

	/**
	 *  Flag indicating that Hebrew letters will not be considered for
	 *  processing complex expressions. If both this flag and
	 *  ignoreArabic are set to <code>true</code>, the
	 *  processor will do nothing (but some overhead can be expected).
	 */
	final boolean ignoreHebrew;

	/**
	 *  Constructor
	 *
	 *  @param operators is a string where each character is a delimiter
	 *          which separates the complex expression into tokens.
	 *  @see #getOperators
	 *
	 *  @param specialsCount specifies the number of special cases handled
	 *          by the processor. This value must be identical to the
	 *          number of special cases handled by the processor with which
	 *          this <code>BidiComplexFeatures</code> instance is associated.
	 *  @see #getSpecialsCount
	 *
	 *  @param dirArabic specifies the base direction of the complex expression
	 *          for Arabic. It must be {@link #DIR_LTR} or {@link #DIR_RTL}.
	 *          If it is not (for instance if it is a negative value), it
	 *          defaults to <code>DIR_LTR</code>.
	 *  @see #getDirArabic
	 *
	 *  @param dirHebrew specifies the base direction of the complex expression
	 *          for Hebrew. It must be {@link #DIR_LTR} or {@link #DIR_RTL}.
	 *          If it is not (for instance if it is a negative value), it
	 *          defaults to <code>DIR_LTR</code>.
	 *  @see #getDirHebrew
	 *
	 *  @param ignoreArabic indicates that Arabic letters will not be
	 *          considered for processing complex expressions.
	 *          If both this flag and <code>ignoreHebrew</code>
	 *          are set to <code>true</code>, the processor will do
	 *          nothing (but some overhead can be expected).
	 *  @see #getIgnoreArabic
	 *
	 *  @param ignoreHebrew indicates that Hebrew letters will not be
	 *          considered for processing complex expressions.
	 *          If both this flag and <code>ignoreArabic</code>
	 *          are set to <code>true</code>, the processor will do
	 *          nothing (but some overhead can be expected).
	 *  @see #getIgnoreHebrew
	 */
	public BidiComplexFeatures(String operators, int specialsCount, int dirArabic, int dirHebrew, boolean ignoreArabic, boolean ignoreHebrew) {

		this.operators = operators == null ? "" : operators; //$NON-NLS-1$
		this.specialsCount = specialsCount;
		this.dirArabic = dirArabic == DIR_LTR || dirArabic == DIR_RTL ? dirArabic : DIR_LTR;
		this.dirHebrew = dirHebrew == DIR_LTR || dirHebrew == DIR_RTL ? dirHebrew : DIR_LTR;
		this.ignoreArabic = ignoreArabic;
		this.ignoreHebrew = ignoreHebrew;
	}

	/**
	 *  @return a string grouping one-character operators which separate
	 *          the text of the complex expression into tokens.
	 */
	public String getOperators() {
		return operators;
	}

	/**
	 *  @return the number of special cases for the associated processor.
	 *          Special cases exist for some types of complex expression
	 *          processors. They are implemented by overriding methods
	 *          {@link IBidiComplexProcessor#indexOfSpecial indexOfSpecial} and
	 *          {@link IBidiComplexProcessor#processSpecial processSpecial}.
	 *          Examples of special cases are comments, literals, or
	 *          anything which is not identified by a one-character operator.
	 */
	public int getSpecialsCount() {
		return specialsCount;
	}

	/**
	 *  @return the base direction of the complex expression for Arabic.
	 *          If a complex expression contains both Arabic and
	 *          Hebrew words, the first Arabic or Hebrew letter in the
	 *          expression determines which is the governing script.<br>
	 *          The value of this field is one of
	 *          {@link #DIR_LTR} or {@link #DIR_RTL}.
	 *
	 *  @see #getDirHebrew
	 */
	public int getDirArabic() {
		return dirArabic;
	}

	/**
	 *  @return the base direction of the complex expression for Hebrew.
	 *          If a complex expression contains both Arabic and
	 *          Hebrew words, the first Arabic or Hebrew letter in the
	 *          expression determines which is the governing script.<br>
	 *          The value of this field is one of
	 *          {@link #DIR_LTR} or {@link #DIR_RTL}.
	 *
	 *  @see #getDirArabic
	 */
	public int getDirHebrew() {
		return dirHebrew;
	}

	/**
	 *  @return a flag indicating that Arabic letters will not be considered
	 *  for processing complex expressions.
	 */
	public boolean getIgnoreArabic() {
		return ignoreArabic;
	}

	/**
	 *  Flag indicating that Hebrew letters will not be considered for
	 *  processing complex expressions.
	 */
	public boolean getIgnoreHebrew() {
		return ignoreHebrew;
	}

}
