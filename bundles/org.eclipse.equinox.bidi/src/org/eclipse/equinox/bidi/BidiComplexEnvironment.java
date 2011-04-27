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

import java.util.Locale;
import org.eclipse.equinox.bidi.custom.IBidiComplexProcessor;
import org.eclipse.equinox.bidi.internal.BidiComplexActivator;

/**
 *  This class defines certain details of the environment within which
 *  complex expressions are processed.
 *  <p>
 *  All public fields in this class are <code>final</code>, i.e. cannot be
 *  changed after creating an instance.
 *  <p>
 *  All methods in {@link BidiComplexEngine} have a BidiComplexEnvironment
 *  argument. If this argument is specified as <code>null</code>, the
 *  {@link #DEFAULT} environment is used.
 *
 *  <h2>Code Samples</h2>
 *  <p>Example (set all environment parameters)
 *  <pre>
 *
 *    BidiComplexEnvironment myEnv = new BidiComplexEnvironment("he_IL", true, BidiComplexEnvironment.ORIENT_RTL);
 *
 *  </pre>
 *  <p>
 *  This class also provides a number of convenience methods related to the environment.
 *  <p>&nbsp;</p>
 *  @see IBidiComplexProcessor#getFeatures
 *
 *  @author Matitiahu Allouche
 */
public class BidiComplexEnvironment {

	/**
	 *  Constant specifying that the orientation of the GUI component
	 *  where a complex expression will be displayed is LTR.
	 *  It can appear as <code>orientation</code> argument for the
	 *  {@link BidiComplexEnvironment#BidiComplexEnvironment BidiComplexEnvironment constructor}
	 *  and as value returned by {@link #getOrientation}.
	 */
	public static final int ORIENT_LTR = 0;

	/**
	 *  Constant specifying that the orientation of the GUI component
	 *  where a complex expression will be displayed is RTL.
	 *  It can appear as <code>orientation</code> argument for the
	 *  {@link BidiComplexEnvironment#BidiComplexEnvironment BidiComplexEnvironment constructor}
	 *  and as value returned by {@link #getOrientation}.
	 */
	public static final int ORIENT_RTL = 1;

	/**
	 *  Constant specifying that the orientation of the GUI component
	 *  where a complex expression will be displayed is contextual with
	 *  a default of LTR (if no strong character appears in the text).
	 *  It can appear as <code>orientation</code> argument for the
	 *  {@link BidiComplexEnvironment#BidiComplexEnvironment BidiComplexEnvironment constructor}
	 *  and as value returned by {@link #getOrientation}.
	 */
	public static final int ORIENT_CONTEXTUAL_LTR = 2;

	/**
	 *  Constant specifying that the orientation of the GUI component
	 *  where a complex expression will be displayed is contextual with
	 *  a default of RTL (if no strong character appears in the text).
	 *  It can appear as <code>orientation</code> argument for the
	 *  {@link BidiComplexEnvironment#BidiComplexEnvironment BidiComplexEnvironment constructor}
	 *  and as value returned by {@link #getOrientation}.
	 */
	public static final int ORIENT_CONTEXTUAL_RTL = 3;

	/**
	 *  Constant specifying that the orientation of the GUI component
	 *  where a complex expression will be displayed is not known.
	 *  Directional formatting characters must be added as prefix and
	 *  suffix whenever a <i>full</i> text is generated using
	 *  {@link BidiComplexEngine#leanToFullText leanToFullText}.
	 *  It can appear as <code>orientation</code> argument for the
	 *  {@link BidiComplexEnvironment#BidiComplexEnvironment BidiComplexEnvironment constructor}
	 *  and as value returned by {@link #getOrientation}.
	 */
	public static final int ORIENT_UNKNOWN = 4;

	/**
	 *  Constant specifying that whatever the orientation of the
	 *  GUI component where a complex expression will be displayed, no
	 *  directional formatting characters must be added as prefix or
	 *  suffix when a <i>full</i> text is generated using
	 *  {@link BidiComplexEngine#leanToFullText leanToFullText}.
	 *  It can appear as <code>orientation</code> argument for the
	 *  {@link BidiComplexEnvironment#BidiComplexEnvironment BidiComplexEnvironment constructor}
	 *  and as value returned by {@link #getOrientation}.
	 */
	public static final int ORIENT_IGNORE = 5;

	/**
	 *  Pre-defined <code>BidiComplexEnvironment</code> instance with values
	 *  for a non-mirrored GUI and a Left-to-Right presentation component.<br>
	 *  The language is set to <code>null</code>, which defaults to the language
	 *  of the current default locale.
	 */
	public static final BidiComplexEnvironment DEFAULT = new BidiComplexEnvironment(null, false, ORIENT_LTR);

	/**
	 *  This string is a 2-letters code representing a language as defined by
	 *  ISO-639. If left as <code>null</code>, it defaults to the language
	 *  of the current default locale.
	 */
	final String language;

	/**
	 *  Flag specifying that complex expressions processed under this environment
	 *  should assume that the GUI is mirrored (globally going from right to left).
	 */
	final boolean mirrored;

	/** Specify the orientation (a.k.a. base direction) of the GUI
	 *  component in which the <i>full</i> text of the complex expression will
	 *  be displayed.
	 */
	final int orientation;

	static Locale defaultLocale;
	static String defaultLanguage;
	static boolean defaultBidi;
	boolean bidiFlag;

	/**
	 *  Constructor
	 *
	 *  @param lang represents the language to be used in this environment.
	 *          It should be specified as a 2-letters code as defined by
	 *          ISO-639.<br>
	 *          If longer than 2 letters, the extra letters are ignored.<br>
	 *          If set to <code>null</code>, it defaults to the language
	 *          of the default locale.
	 *  @see #getLanguage
	 *
	 *  @param mirrored specifies if the GUI is mirrored.
	 *  @see #getMirrored
	 *
	 *  @param orientation specifies the orientation of the component
	 *          which is to display the complex expression. It must be
	 *          one of the values
	 *          {@link #ORIENT_LTR ORIENT_LTR},
	 *          {@link #ORIENT_LTR ORIENT_RTL},
	 *          {@link #ORIENT_CONTEXTUAL_LTR ORIENT_CONTEXTUAL_LTR},
	 *          {@link #ORIENT_CONTEXTUAL_RTL ORIENT_CONTEXTUAL_RTL},
	 *          {@link #ORIENT_UNKNOWN ORIENT_UNKNOWN} or
	 *          {@link #ORIENT_IGNORE ORIENT_IGNORE}.<br>
	 *          If different, it defaults to {@link #ORIENT_UNKNOWN ORIENT_UNKNOWN}.
	 *  @see #getOrientation
	 */
	public BidiComplexEnvironment(String lang, boolean mirrored, int orientation) {
		if (lang == null) {
			language = null;
		} else {
			if (lang.length() > 2)
				language = lang.substring(0, 2);
			else
				language = lang;
			bidiFlag = isBidiLanguage(language);
		}
		this.mirrored = mirrored;
		this.orientation = orientation >= ORIENT_LTR && orientation <= ORIENT_IGNORE ? orientation : ORIENT_UNKNOWN;
	}

	/**
	 *  Return a 2-letters code representing a language as defined by
	 *  ISO-639. If equal to <code>null</code>, it defaults to the language
	 *  of the current default locale.
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 *  Return a flag indicating that complex expressions processed
	 *  within this environment should assume that the GUI is mirrored
	 * (globally going from right to left).
	 */
	public boolean getMirrored() {
		return mirrored;
	}

	/** Return the orientation (a.k.a. base direction) of the GUI
	 *  component in which the <i>full</i> text of the complex expression
	 *  will be displayed.<br>
	 *  The orientation must have one of the values
	 *  {@link #ORIENT_LTR ORIENT_LTR},
	 *  {@link #ORIENT_LTR ORIENT_RTL},
	 *  {@link #ORIENT_CONTEXTUAL_LTR ORIENT_CONTEXTUAL_LTR},
	 *  {@link #ORIENT_CONTEXTUAL_RTL ORIENT_CONTEXTUAL_RTL},
	 *  {@link #ORIENT_UNKNOWN ORIENT_UNKNOWN} or
	 *  {@link #ORIENT_IGNORE ORIENT_IGNORE}.
	  *  <p>
	 *  When the orientation is <code>ORIENT_LTR</code> and the complex
	 *  expression has a RTL base direction,
	 *  {@link BidiComplexEngine#leanToFullText leanToFullText}
	 *  adds RLE+RLM at the head of the <i>full</i> text and RLM+PDF at its
	 *  end.
	 *  <p>
	 *  When the orientation is <code>ORIENT_RTL</code> and the complex
	 *  expression has a LTR base direction,
	 *  {@link BidiComplexEngine#leanToFullText leanToFullText}
	 *  adds LRE+LRM at the head of the <i>full</i> text and LRM+PDF at its
	 *  end.
	 *  <p>
	 *  When the orientation is <code>ORIENT_CONTEXTUAL_LTR</code> or
	 *  <code>ORIENT_CONTEXTUAL_RTL</code> and the data content would resolve
	 *  to a RTL orientation while the complex expression has a LTR base
	 *  direction, {@link BidiComplexEngine#leanToFullText leanToFullText}
	 *  adds LRM at the head of the <i>full</i> text.
	 *  <p>
	 *  When the orientation is <code>ORIENT_CONTEXTUAL_LTR</code> or
	 *  <code>ORIENT_CONTEXTUAL_RTL</code> and the data content would resolve
	 *  to a LTR orientation while the complex expression has a RTL base
	 *  direction, {@link BidiComplexEngine#leanToFullText leanToFullText}
	 *  adds RLM at the head of the <i>full</i> text.
	 *  <p>
	 *  When the orientation is <code>ORIENT_UNKNOWN</code> and the complex
	 *  expression has a LTR base direction,
	 *  {@link BidiComplexEngine#leanToFullText leanToFullText}
	 *  adds LRE+LRM at the head of the <i>full</i> text and LRM+PDF at its
	 *  end.
	 *  <p>
	 *  When the orientation is <code>ORIENT_UNKNOWN</code> and the complex
	 *  expression has a RTL base direction,
	 *  {@link BidiComplexEngine#leanToFullText leanToFullText}
	 *  adds RLE+RLM at the head of the <i>full</i> text and RLM+PDF at its
	 *  end.
	 *  <p>
	 *  When the orientation is <code>ORIENT_IGNORE</code>,
	 *  {@link BidiComplexEngine#leanToFullText leanToFullText} does not add any directional
	 *  formatting characters as either prefix or suffix of the <i>full</i> text.
	 *  <p>
	 */
	public int getOrientation() {
		return orientation;
	}

	/**
	 *  Check whether the current language uses a
	 *  bidi script (Arabic, Hebrew, Farsi or Urdu).
	 *
	 *  @return <code>true</code> if the current language uses a bidi script.
	 *          The language may have been set explicitly when creating the
	 *          <code>BidiComplexEnvironment</code> instance, or it may have
	 *          defaulted to the language of the current default locale.
	 *  @see #getLanguage
	 */
	public boolean isBidi() {
		if (defaultLanguage != null && defaultLocale.equals(getDefaultLocale()))
			return defaultBidi;

		if (language == null) {
			defaultLocale = getDefaultLocale();
			defaultLanguage = defaultLocale.getLanguage();
			defaultBidi = isBidiLanguage(defaultLanguage);
			return defaultBidi;
		}

		return bidiFlag;
	}

	static boolean isBidiLanguage(String lang) {
		return "iw".equals(lang) || "he".equals(lang) || "ar".equals(lang) || "fa".equals(lang) || "ur".equals(lang); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	static String lineSep;

	/**
	 *  Retrieve the string which represents a line separator in this environment.
	 *
	 *  @return the string which is used as line separator (e.g. CRLF).
	 */
	public static String getLineSep() {
		// use bundle properties
		if (lineSep == null) {
			//          lineSep = System.getProperty("line.separator", "\n"); //$NON-NLS-1$//$NON-NLS-2$
			lineSep = getProperty("line.separator"); //$NON-NLS-1$/
		}
		return lineSep;
	}

	static String osName;
	static boolean flagOS;

	private static String getProperty(String key) {
		// use bundle properties
		//      osName = System.getProperty("os.name").toLowerCase(); //$NON-NLS-1$
		BidiComplexActivator bidiComplexActivator = BidiComplexActivator.getInstance();
		return bidiComplexActivator.getProperty(key);
	}

	private Locale getDefaultLocale() {
		BidiComplexActivator bidiComplexActivator = BidiComplexActivator.getInstance();
		return bidiComplexActivator.getDefaultLocale();
	}

	/**
	 *  Check if the current OS is supported by the complex expression packages.
	 *
	 *  @return <code>true</code> if the current OS is supported.
	 */
	public static boolean isSupportedOS() {
		if (osName == null) {
			// use bundle properties
			// osName = System.getProperty("os.name").toLowerCase(); //$NON-NLS-1$
			osName = getProperty("os.name").toLowerCase(); //$NON-NLS-1$/
			flagOS = osName.startsWith("windows") || osName.startsWith("linux"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return flagOS;
	}

}
