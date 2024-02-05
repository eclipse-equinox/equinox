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

import org.eclipse.equinox.bidi.custom.StructuredTextTypeHandler;

/**
 * Handler adapted to processing comma-delimited lists, such as:
 * 
 * <pre>
 *    part1,part2,part3
 * </pre>
 */
public class StructuredTextComma extends StructuredTextTypeHandler {
	public StructuredTextComma() {
		super(","); //$NON-NLS-1$
	}
}
