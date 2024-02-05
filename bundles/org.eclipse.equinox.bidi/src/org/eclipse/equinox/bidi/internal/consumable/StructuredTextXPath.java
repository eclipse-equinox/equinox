/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.bidi.internal.consumable;

import org.eclipse.equinox.bidi.advanced.IStructuredTextExpert;
import org.eclipse.equinox.bidi.internal.StructuredTextDelims;

/**
 * Handler adapted to processing XPath expressions.
 */
public class StructuredTextXPath extends StructuredTextDelims {

	public StructuredTextXPath() {
		super(" /[]<>=!:@.|()+-*"); //$NON-NLS-1$
	}

	/**
	 * @return 2 as the number of special cases handled by this handler.
	 */
	@Override
	public int getSpecialsCount(IStructuredTextExpert expert) {
		return 2;
	}

	/**
	 * @return apostrophe and quotation mark as delimiters.
	 */
	@Override
	protected String getDelimiters() {
		return "''\"\""; //$NON-NLS-1$
	}

}
