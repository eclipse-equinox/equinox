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
package org.eclipse.equinox.bidi.internal.consumable;

import org.eclipse.equinox.bidi.STextEnvironment;
import org.eclipse.equinox.bidi.internal.STextDelims;

/**
 *  Processor adapted to processing XPath expressions.
 */
public class STextXPath extends STextDelims {
	/**
	 *  @return " /[]<>=!:@.|()+-*" as the separators specific to this processor.
	 */
	public String getSeparators(STextEnvironment environment, String text, byte[] dirProps) {
		return " /[]<>=!:@.|()+-*"; //$NON-NLS-1$
	}

	/**
	 *  @return 2 as the number of special cases handled by this processor.
	 */
	public int getSpecialsCount(STextEnvironment environment, String text, byte[] dirProps) {
		return 2;
	}

	/**
	 *  @return apostrophe and quotation mark as delimiters.
	 */
	protected String getDelimiters() {
		return "''\"\""; //$NON-NLS-1$
	}

}
