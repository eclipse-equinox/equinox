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
package org.eclipse.equinox.bidi.internal.tests;

import org.eclipse.equinox.bidi.internal.StructuredTextSingle;

/**
 * Handler adapted to processing property file statements. It expects the
 * following string format:
 * 
 * <pre>
 * name = value
 * </pre>
 */
public class StructuredTextProperty extends StructuredTextSingle {

	public StructuredTextProperty() {
		super("="); //$NON-NLS-1$
	}
}
