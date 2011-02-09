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

import org.eclipse.equinox.bidi.custom.IBidiComplexProcessor;

/**
 *  This class defines features of a complex expression processor.
 *  <p>
 *  All public fields in this class are <code>final</code>, i.e. cannot be
 *  changed after creating an instance.
 *  <p>
 *  A <code>BidiComplexFeatures</code> instance can be associated with a
 *  <code>BidiComplexHelper</code> instance using
 *  the {@link BidiComplexHelper#setFeatures setFeatures} method.
 *
 *  <h2>Code Samples</h2>
 *  <p>Example 1 (set all features)
 *  <pre>
 *
 *    BidiComplexFeatures myFeatures = new BidiComplexFeatures("+-=", 0, -1, -1, false, false);
 *    BidiComplexHelper myHelper = new BidiComplexHelper(IBidiComplexExpressionTypes.FILE, myEnv);
 *    myHelper.setFeatures(myFeatures);
 *
 *  </pre>
 *  <p>Example 2 (change only the operators)
 *  <pre>
 *
 *    BidiComplexFeatures f1 = myHelper.getFeatures();
 *    BidiComplexFeatures f2 = new BidiComplexFeatures("[]|()", f1.specialsCount,
 *                                     f1.dirArabic, f1.dirHebrew,
 *                                     f1.ignoreArabic, f1.ignoreHebrew);
 *    myHelper.setFeatures(f2);
 *
 *  </pre>
 *
 *  @see BidiComplexHelper#getFeatures BidiComplexHelper.getFeatures
 *  @see BidiComplexHelper#setFeatures BidiComplexHelper.setFeatures
 *  @see IBidiComplexProcessor#init IBidiComplexProcessor.init
 *  @see IBidiComplexProcessor#updateEnvironment IBidiComplexProcessor.updateEnvironment
 *
 *  @author Matitiahu Allouche
 */
public class BidiComplexFeatures {

	/**
	 *  Constant specifying that the base direction of a complex expression is LTR.
	 *  The base direction may depend on whether the GUI is
	 *  {@link BidiComplexEnvironment#mirrored mirrored} and may
	 *  may be different for Arabic and for Hebrew.
	 *  This constant can appear as <code>dirArabic</code>
	 *  or <code>dirHebrew</code> argument for
	 *  {@link BidiComplexFeatures#BidiComplexFeatures BidiComplexFeatures constructor} and as value
	 *  for the {@link #dirArabic} or {@link #dirHebrew} members of
	 *  <code>BidiComplexFeatures</code>.
	 */
	public static final int DIR_LTR = 0;

	/**
	 *  Constant specifying that the base direction of a complex expression is RTL.
	 *  The base direction may depend on whether the GUI is
	 *  {@link BidiComplexEnvironment#mirrored mirrored} and may
	 *  may be different for Arabic and for Hebrew.
	 *  This constant can appear as <code>dirArabic</code>
	 *  or <code>dirHebrew</code> argument for
	 *  {@link BidiComplexFeatures#BidiComplexFeatures BidiComplexFeatures constructor} and as value
	 *  for the {@link #dirArabic} or {@link #dirHebrew} members of
	 *  <code>BidiComplexFeatures</code>.
	 */
	public static final int DIR_RTL = 1;

	/**
	 *  Pre-defined <code>BidiComplexFeatures</code> instance with values for no
	 *  operators, no special processing, all directions LTR
	 *  and support for neither Arabic nor Hebrew.<br>
	 *  Since there are no operators and no special processing, a complex
	 *  expression processor with such features would do nothing.<br>
	 *  It is more efficient to do nothing with a
	 *  {@link BidiComplexHelper#BidiComplexHelper() BidiComplexHelper}
	 *  instantiated with no arguments.
	 */
	public static final BidiComplexFeatures DEFAULT = new BidiComplexFeatures(null, 0, -1, -1, true, true);

	/**
	 *  String grouping one-character operators which
	 *  separate the text of the complex expression into tokens.
	 */
	public final String operators;

	/**
	 *  Number of special cases for the associated processor.
	 *  Special cases exist for some types of complex expression processors.
	 *  They are implemented by overriding methods
	 *  {@link IBidiComplexProcessor#indexOfSpecial indexOfSpecial} and
	 *  {@link IBidiComplexProcessor#processSpecial processSpecial}.
	 *  Examples of special cases are comments, literals, or anything which
	 *  is not identified by a one-character operator.
	 */
	public final int specialsCount;

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
	public final int dirArabic;

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
	public final int dirHebrew;

	/**
	 *  Flag indicating that Arabic letters will not be considered for
	 *  processing complex expressions. If both this flag and
	 *  {@link #ignoreHebrew} are set to <code>true</code>, the
	 *  processor will do nothing (but some overhead can be expected).
	 */
	public final boolean ignoreArabic;

	/**
	 *  Flag indicating that Hebrew letters will not be considered for
	 *  processing complex expressions. If both this flag and
	 *  {@link #ignoreArabic} are set to <code>true</code>, the
	 *  processor will do nothing (but some overhead can be expected).
	 */
	public final boolean ignoreHebrew;

	/**
	 *  Constructor
	 *
	 *  @param operators is a string where each character is a delimiter
	 *          which separates the complex expression into tokens.
	 *  @see #operators
	 *
	 *  @param specialsCount specifies the number of special cases handled
	 *          by the processor.
	 *  @see #specialsCount
	 *
	 *  @param dirArabic specifies the base direction of the complex expression
	 *          for Arabic. It must be {@link #DIR_LTR} or {@link #DIR_RTL}.
	 *          If it is not (for instance if it is a negative value), it
	 *          defaults to <code>DIR_LTR</code>.
	 *  @see #dirArabic
	 *
	 *  @param dirHebrew specifies the base direction of the complex expression
	 *          for Hebrew. It must be {@link #DIR_LTR} or {@link #DIR_RTL}.
	 *          If it is not (for instance if it is a negative value), it
	 *          defaults to <code>DIR_LTR</code>.
	 *  @see #dirHebrew
	 *
	 *  @param ignoreArabic indicates that Arabic letters will not be
	 *          considered for processing complex expressions.
	 *  @see #ignoreArabic
	 *
	 *  @param ignoreHebrew indicates that Hebrew letters will not be
	 *          considered for processing complex expressions.
	 *  @see #ignoreHebrew
	 */
	public BidiComplexFeatures(String operators, int specialsCount, int dirArabic, int dirHebrew, boolean ignoreArabic, boolean ignoreHebrew) {

		this.operators = operators == null ? "" : operators; //$NON-NLS-1$
		this.specialsCount = specialsCount;
		this.dirArabic = dirArabic == DIR_LTR || dirArabic == DIR_RTL ? dirArabic : DIR_LTR;
		this.dirHebrew = dirHebrew == DIR_LTR || dirHebrew == DIR_RTL ? dirHebrew : DIR_LTR;
		this.ignoreArabic = ignoreArabic;
		this.ignoreHebrew = ignoreHebrew;
	}

}
