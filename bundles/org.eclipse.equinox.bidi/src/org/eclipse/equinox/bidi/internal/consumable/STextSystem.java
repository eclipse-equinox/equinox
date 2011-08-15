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

import org.eclipse.equinox.bidi.internal.STextSingle;

/**
 *  Handler adapted to processing structured text with the following format:
 *  <pre>
 *    system(user)
 *  </pre>
 */
public class STextSystem extends STextSingle {

	public STextSystem() {
		super("("); //$NON-NLS-1$
	}

}
