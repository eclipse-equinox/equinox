/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.bidi;

import org.eclipse.equinox.bidi.advanced.STextEnvironment;

// TBD combine with STextUtil; remove duplicates of those two constants
public interface STextDirection {

	/**
	 *  Constant specifying that the base direction of a structured text is LTR.
	 *  The base direction may depend on whether the GUI is
	 *  {@link STextEnvironment#getMirrored mirrored} and may
	 *  may be different for Arabic and for Hebrew.
	 *  This constant can appear as value returned by the
	 *  {@link #getCurDirection getCurDirection} method.
	 */
	public int DIR_LTR = 0;

	/**
	 *  Constant specifying that the base direction of a structured text is RTL.
	 *  The base direction may depend on whether the GUI is
	 *  {@link STextEnvironment#getMirrored mirrored} and may
	 *  may be different for Arabic and for Hebrew.
	 *  This constant can appear as value returned by the
	 *  {@link #getCurDirection getCurDirection} method.
	 */
	public int DIR_RTL = 1;

}