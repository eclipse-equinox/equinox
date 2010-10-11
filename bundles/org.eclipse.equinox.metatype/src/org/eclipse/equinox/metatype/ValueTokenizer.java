/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.metatype;

import java.util.Vector;
import org.osgi.service.log.LogService;

public class ValueTokenizer {

	private static final char SEPARATE = ',';
	private static final char CONTROL = '\\';

	Vector _value_vector = new Vector(7);

	/*
	 * Constructor of class ValueTokenizer
	 */
	public ValueTokenizer(String values_str, LogService logger) {
		if (values_str != null) {

			StringBuffer buffer = new StringBuffer(""); //$NON-NLS-1$
			for (int i = 0; i < values_str.length(); i++) {
				if (values_str.charAt(i) == CONTROL) {
					if (i + 1 < values_str.length()) {
						buffer.append(values_str.charAt(++i));
						continue;
					}
					// CONTROL char should not occur in last char.
					logger.log(LogService.LOG_ERROR, "ValueTokenizer.ValueTokenizer(String) " + MetaTypeMsg.TOKENIZER_GOT_INVALID_DATA); //$NON-NLS-1$
					// It's an invalid char, but since it's the last one,
					// just ignore it.
					continue;
				}
				if (values_str.charAt(i) == SEPARATE) {
					_value_vector.addElement(buffer.toString().trim());
					buffer = new StringBuffer(""); //$NON-NLS-1$
					continue;
				}
				buffer.append(values_str.charAt(i));
			}
			// Don't forget the final one.
			_value_vector.addElement(buffer.toString().trim());
		}
	}

	/*
	 * Method to return values as Vector.
	 */
	public Vector getValuesAsVector() {
		return _value_vector;
	}

	/*
	 * Method to return values as String[] or null.
	 */
	public String[] getValuesAsArray() {

		String[] value_array = null;
		if ((_value_vector != null) && (_value_vector.size() != 0)) {

			value_array = new String[_value_vector.size()];
			_value_vector.toArray(value_array);
		}

		return value_array;
	}
}
