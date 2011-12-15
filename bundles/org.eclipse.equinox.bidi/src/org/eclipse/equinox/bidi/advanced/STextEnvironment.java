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

import org.eclipse.equinox.bidi.internal.STextActivator;

/**
 *  Describes the environment within which structured text strings are 
 *  processed. It includes:
 *  <ul>
 *  <li>locale,</li>
 *  <li>desired orientation,</li>
 *  <li>text mirroring attributes.</li>
 *  </ul>
 *  @author Matitiahu Allouche
 */
public class STextEnvironment {

	/**
	 * Specifies that a GUI component should display text Left-To-Right (value is 0).
	 */
	public static final int ORIENT_LTR = 0;

	/**
	 * Specifies that a GUI component should display text Right-To-Left (value is 1).
	 */
	public static final int ORIENT_RTL = 1;

	/**
	 * Specifies that a GUI component should display text depending on the context
	 * (value is 2).
	 */
	public static final int ORIENT_CONTEXTUAL = 1 << 1;

	/**
	 * Specifies that a GUI component should display text depending on the context
	 * with default orientation being Left-To-Right (value is 2).
	 */
	public static final int ORIENT_CONTEXTUAL_LTR = ORIENT_CONTEXTUAL | ORIENT_LTR;

	/**
	 * Specifies that a GUI component should display text depending on the context
	 * with default orientation being Right-To-Left (value is 3).
	 */
	public static final int ORIENT_CONTEXTUAL_RTL = ORIENT_CONTEXTUAL | ORIENT_RTL;

	/**
	 * Used when the orientation of a GUI component is not known (value is 4). 
	 */
	public static final int ORIENT_UNKNOWN = 1 << 2;

	/**
	 * Used to specify that no directional formatting characters 
	 * should be added as prefix or suffix (value is 8).
	 */
	public static final int ORIENT_IGNORE = 1 << 3;

	/**
	 * Pre-defined <code>STextEnvironment</code> instance which uses default locale,
	 * non-mirrored environment, and a Left-to-Right presentation component.
	 */
	public static final STextEnvironment DEFAULT = new STextEnvironment(null, false, ORIENT_LTR);

	/**
	 * This string is a 2-letters code representing a language as defined by
	 * ISO-639.
	 */
	final private String language;

	/**
	 * Flag specifying that structured text processed under this environment
	 * should assume that the GUI is mirrored (globally going from right to left).
	 */
	final private boolean mirrored;

	/**
	 * Specify the orientation (a.k.a. base direction) of the GUI
	 * component in which the <i>full</i> structured text will
	 * be displayed.
	 */
	final private int orientation;

	/**
	 * Cached value that determines if the Bidi processing is needed 
	 * in this environment.
	 */
	private Boolean processingNeeded;

	/**
	 * Creates an instance of a structured text environment.
	 *
	 * @param lang the language of the environment, encoded as specified
	 * in ISO-639. Might be <code>null</code>, in which case the default
	 * locale is used.
	 * @param mirrored specifies if the environment is mirrored.
	 * @param orientation the orientation of the GUI component, one of the values: 
	 *         {@link #ORIENT_LTR ORIENT_LTR},
	 *         {@link #ORIENT_LTR ORIENT_RTL},
	 *         {@link #ORIENT_CONTEXTUAL_LTR ORIENT_CONTEXTUAL_LTR},
	 *         {@link #ORIENT_CONTEXTUAL_RTL ORIENT_CONTEXTUAL_RTL},
	 *         {@link #ORIENT_UNKNOWN ORIENT_UNKNOWN}, or
	 *         {@link #ORIENT_IGNORE ORIENT_IGNORE}.
	 */
	public STextEnvironment(String lang, boolean mirrored, int orientation) {
		if (lang != null) {
			if (lang.length() > 2)
				language = lang.substring(0, 2);
			else
				language = lang;
		} else
			language = STextActivator.getInstance().getDefaultLocale().getLanguage();
		this.mirrored = mirrored;
		this.orientation = orientation >= ORIENT_LTR && orientation <= ORIENT_IGNORE ? orientation : ORIENT_UNKNOWN;
	}

	/**
	 * Returns a 2-letters code representing a language as defined by
	 * ISO-639.
	 * 
	 * @return language of the environment
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * Returns a flag indicating that structured text processed
	 * within this environment should assume that the GUI is mirrored
	 * (globally going from right to left).
	 * 
	 * @return <code>true</code> if environment is mirrored 
	 */
	public boolean getMirrored() {
		return mirrored;
	}

	/** 
	 * Returns the orientation (a.k.a. base direction) of the GUI
	 * component in which the <i>full</i> structured text
	 * will be displayed.
	 * <p>
	 * The orientation value is one of the following:
	 * <ul>
	 * <li>{@link #ORIENT_LTR ORIENT_LTR},</li>
	 * <li>{@link #ORIENT_LTR ORIENT_RTL},</li>
	 * <li>{@link #ORIENT_CONTEXTUAL_LTR ORIENT_CONTEXTUAL_LTR},</li>
	 * <li>{@link #ORIENT_CONTEXTUAL_RTL ORIENT_CONTEXTUAL_RTL},</li>
	 * <li>{@link #ORIENT_UNKNOWN ORIENT_UNKNOWN}, or</li>
	 * <li>{@link #ORIENT_IGNORE ORIENT_IGNORE}</li>.
	 * </ul>
	 * </p>
	 */
	public int getOrientation() {
		return orientation;
	}

	/**
	 * Checks if bidi processing is needed in this environment. The result 
	 * depends on the operating system (must be supported by this package)
	 * and on the language supplied when constructing the instance (it
	 * must be a language using a bidirectional script).
	 * 
	 * @return <code>true</code> if bidi processing is needed in this environment.
	 */
	public boolean isProcessingNeeded() {
		if (processingNeeded == null) {
			String osName = STextActivator.getInstance().getProperty("os.name"); //$NON-NLS-1$/
			if (osName != null)
				osName = osName.toLowerCase();
			boolean supportedOS = osName.startsWith("windows") || osName.startsWith("linux") || osName.startsWith("mac"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (supportedOS) {
				// Check whether the current language uses a bidi script (Arabic, Hebrew, Farsi or Urdu)
				boolean isBidi = "iw".equals(language) || //$NON-NLS-1$
						"he".equals(language) || //$NON-NLS-1$
						"ar".equals(language) || //$NON-NLS-1$
						"fa".equals(language) || //$NON-NLS-1$
						"ur".equals(language); //$NON-NLS-1$
				processingNeeded = new Boolean(isBidi);
			} else {
				processingNeeded = new Boolean(false);
			}
		}
		return processingNeeded.booleanValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * Computes the hashCode based on the values supplied when constructing 
	 * the instance and on the result of {@link #isProcessingNeeded()}.
	 * 
	 * @return the hash code.
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((language == null) ? 0 : language.hashCode());
		result = prime * result + (mirrored ? 1231 : 1237);
		result = prime * result + orientation;
		result = prime * result + ((processingNeeded == null) ? 0 : processingNeeded.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * Compare 2 environment instances and returns true if both instances
	 * were constructed with the same arguments.
	 * 
	 * @return true if the 2 instances can be used interchangeably.
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		STextEnvironment other = (STextEnvironment) obj;
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		if (mirrored != other.mirrored)
			return false;
		if (orientation != other.orientation)
			return false;
		if (processingNeeded == null) {
			if (other.processingNeeded != null)
				return false;
		} else if (!processingNeeded.equals(other.processingNeeded))
			return false;
		return true;
	}

}
