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
import org.eclipse.equinox.bidi.internal.STextSingle;

/**
 *  Processor adapted to processing property file statements.
 *  It expects the following string format:
 *  <pre>
 *    name=value
 *  </pre>
 */
public class STextProperty extends STextSingle {
	/**
	 *  @return one separator (equal sign).
	 */
	public String getSeparators(STextEnvironment environment, String text, byte[] dirProps) {
		return "="; //$NON-NLS-1$
	}

}
