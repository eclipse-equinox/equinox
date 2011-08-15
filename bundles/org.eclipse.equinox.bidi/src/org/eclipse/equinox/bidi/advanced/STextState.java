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
package org.eclipse.equinox.bidi.advanced;

final public class STextState {

	private STextState() {
		// prevent instantiation
	}

	public static Object createState() {
		return new Object[1];
	}

	public static Object getValueAndReset(Object state) {
		if (state instanceof Object[]) {
			Object[] values = (Object[]) state;
			if (values.length > 0) {
				Object value = values[0];
				values[0] = null;
				return value;
			}
		}
		throw new IllegalArgumentException("Invalid state argument"); //$NON-NLS-1$
	}

	public static void setValue(Object state, Object value) {
		if (state == null)
			return;
		if (state instanceof Object[]) {
			Object[] values = (Object[]) state;
			if (values.length > 0)
				values[0] = value;
			return;
		}
		throw new IllegalArgumentException("Invalid state argument"); //$NON-NLS-1$
	}

}
