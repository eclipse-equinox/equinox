/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.metatype.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.AttributeDefinition;

public class ValueTokenizer {
	private static final char DELIMITER = ',';
	private static final char ESCAPE = '\\';

	private final LogService logger;
	private final List<String> values = new ArrayList<String>();

	/*
	 * Constructor of class ValueTokenizer
	 */
	public ValueTokenizer(String values_str, LogService logger) {
		this.logger = logger;
		if (values_str == null)
			return;
		// The trick is to strip out unescaped whitespace characters before and
		// after the input string as well as before and after each 
		// individual token within the input string without losing any escaped 
		// whitespace characters. Whitespace between two non-whitespace
		// characters may or may not be escaped. Also, any character may be
		// escaped. The escape character is '\'. The delimiter is ','.
		StringBuffer buffer = new StringBuffer();
		// Loop over the characters within the input string and extract each
		// value token.
		for (int i = 0; i < values_str.length(); i++) {
			char c1 = values_str.charAt(i);
			switch (c1) {
				case DELIMITER :
					// When the delimiter is encountered, add the extracted 
					// token to the result and prepare the buffer to receive the
					// next token.
					values.add(buffer.toString());
					buffer.delete(0, buffer.length());
					break;
				case ESCAPE :
					// When the escape is encountered, add the immediately
					// following character to the token, unless the end of the
					// input has been reached. Note this will result in loop 
					// counter 'i' being incremented twice, once here and once 
					// at the end of the loop.
					if (i + 1 < values_str.length()) {
						buffer.append(values_str.charAt(++i));
					} else {
						// If the ESCAPE character occurs as the last character
						// of the string, log the error and ignore it.
						logger.log(LogService.LOG_ERROR, "ValueTokenizer.ValueTokenizer(String) " + MetaTypeMsg.TOKENIZER_GOT_INVALID_DATA); //$NON-NLS-1$
					}
					break;
				default :
					// For all other characters, add them to the current token
					// unless dealing with unescaped whitespace at the beginning
					// or end. We know the whitespace is unescaped because it
					// would have been handled in the ESCAPE case otherwise.
					if (Character.isWhitespace(c1)) {
						// Ignore unescaped whitespace at the beginning of the
						// token.
						if (buffer.length() == 0) {
							continue;
						}
						// If the whitespace is not at the beginning, look
						// forward, starting with the next character, to see if 
						// it's in the middle or at the end. Unescaped 
						// whitespace in the middle is okay.
						for (int j = i + 1; j < values_str.length(); j++) {
							// Keep looping until the end of the string is
							// reached or a non-whitespace character other than
							// the escape is seen.
							char c2 = values_str.charAt(j);
							if (!Character.isWhitespace(c2)) {
								// If the current character is not the DELIMITER, all whitespace 
								// characters are significant and should be added to the token.
								// Otherwise, they're at the end and should be ignored. But watch
								// out for an escape character at the end of the input. Ignore it
								// and any previous insignificant whitespace if it exists.
								if (c2 == ESCAPE && j + 1 >= values_str.length()) {
									continue;
								}
								if (c2 != DELIMITER) {
									buffer.append(values_str.substring(i, j));
								}
								// Let loop counter i catch up with the inner loop but keep in
								// mind it will still be incremented at the end of the outer loop.
								i = j - 1;
								break;
							}
						}
					} else {
						// For non-whitespace characters.
						buffer.append(c1);
					}
			}
		}
		// Don't forget to add the last token.
		values.add(buffer.toString());
	}

	/*
	 * Method to return values as Vector.
	 */
	public Collection<String> getValues() {
		return Collections.unmodifiableList(values);
	}

	/*
	 * Method to return values as String[] or null.
	 */
	public String[] getValuesAsArray() {
		if (values.isEmpty()) {
			return null;
		}
		return values.toArray(new String[values.size()]);
	}

	public String getValuesAsString() {
		if (values.isEmpty()) {
			return null;
		}
		if (values.size() == 1) {
			return values.get(0);
		}
		StringBuffer buffer = new StringBuffer(values.get(0));
		for (int i = 1; i < values.size(); i++) {
			buffer.append(',');
			buffer.append(values.get(i));
		}
		return buffer.toString();
	}

	public String validate(AttributeDefinitionImpl ad) {
		// An empty list means the original value was null. Null is never valid.
		if (values.isEmpty()) {
			return MetaTypeMsg.NULL_IS_INVALID;
		}
		try {
			// A value must match the cardinality.
			int cardinality = Math.abs(ad.getCardinality());
			// If the cardinality is zero, the value must contain one and only one token.
			if (cardinality == 0) {
				if (values.size() != 1) {
					return NLS.bind(MetaTypeMsg.CARDINALITY_VIOLATION, new Object[] {getValuesAsString(), values.size(), 1, 1});
				}
			}
			// Otherwise, the number of tokens must be between 0 and cardinality, inclusive.
			else if (values.size() > cardinality) {
				return NLS.bind(MetaTypeMsg.CARDINALITY_VIOLATION, new Object[] {getValuesAsString(), values.size(), 0, cardinality});
			}
			// Now inspect each token.
			for (Iterator<String> i = values.iterator(); i.hasNext();) {
				String s = i.next();
				// If options were declared and the value does not match one of them, the value is not valid.
				if (!ad._values.isEmpty() && !ad._values.contains(s)) {
					return NLS.bind(MetaTypeMsg.VALUE_OUT_OF_OPTION, s);
				}
				// Check the type. Also check the range if min or max were declared.
				boolean rangeError = false;
				switch (ad._dataType) {
					case AttributeDefinition.PASSWORD :
					case AttributeDefinition.STRING :
						if (ad._minValue != null && s.length() < (Integer) ad._minValue) {
							rangeError = true;
						} else if (ad._maxValue != null && s.length() > (Integer) ad._maxValue) {
							rangeError = true;
						}
						break;
					case AttributeDefinition.INTEGER :
						Integer intVal = new Integer(s);
						if (ad._minValue != null && intVal.compareTo((Integer) ad._minValue) < 0) {
							rangeError = true;
						} else if (ad._maxValue != null && intVal.compareTo((Integer) ad._maxValue) > 0) {
							rangeError = true;
						}
						break;
					case AttributeDefinition.LONG :
						Long longVal = new Long(s);
						if (ad._minValue != null && longVal.compareTo((Long) ad._minValue) < 0) {
							rangeError = true;
						} else if (ad._maxValue != null && longVal.compareTo((Long) ad._maxValue) > 0) {
							rangeError = true;
						}
						break;
					case AttributeDefinition.DOUBLE :
						Double doubleVal = new Double(s);
						if (ad._minValue != null && doubleVal.compareTo((Double) ad._minValue) < 0) {
							rangeError = true;
						} else if (ad._maxValue != null && doubleVal.compareTo((Double) ad._maxValue) > 0) {
							rangeError = true;
						}
						break;
					case AttributeDefinition.BOOLEAN :
						// Any string can be converted into a boolean via Boolean.valueOf(String).
						// Seems unnecessary to impose any further restrictions.
						break;
					case AttributeDefinition.CHARACTER :
						Character charVal = new Character(s.charAt(0));
						if (ad._minValue != null && charVal.compareTo((Character) ad._minValue) < 0) {
							rangeError = true;
						} else if (ad._maxValue != null && charVal.compareTo((Character) ad._maxValue) > 0) {
							rangeError = true;
						}
						break;
					case AttributeDefinition.FLOAT :
						Float floatVal = new Float(s);
						if (ad._minValue != null && floatVal.compareTo((Float) ad._minValue) < 0) {
							rangeError = true;
						} else if (ad._maxValue != null && floatVal.compareTo((Float) ad._maxValue) > 0) {
							rangeError = true;
						}
						break;
					case AttributeDefinition.SHORT :
						Short shortVal = new Short(s);
						if (ad._minValue != null && shortVal.compareTo((Short) ad._minValue) < 0) {
							rangeError = true;
						} else if (ad._maxValue != null && shortVal.compareTo((Short) ad._maxValue) > 0) {
							rangeError = true;
						}
						break;
					case AttributeDefinition.BYTE :
						Byte byteVal = new Byte(s);
						if (ad._minValue != null && byteVal.compareTo((Byte) ad._minValue) < 0) {
							rangeError = true;
						} else if (ad._maxValue != null && byteVal.compareTo((Byte) ad._maxValue) > 0) {
							rangeError = true;
						}
						break;
					case AttributeDefinition.BIGDECIMAL :
						BigDecimal bigDecVal = new BigDecimal(s);
						if (ad._minValue != null && bigDecVal.compareTo((BigDecimal) ad._minValue) < 0) {
							rangeError = true;
						} else if (ad._maxValue != null && bigDecVal.compareTo((BigDecimal) ad._maxValue) > 0) {
							rangeError = true;
						}
						break;
					case AttributeDefinition.BIGINTEGER :
						BigInteger bigIntVal = new BigInteger(s);
						if (ad._minValue != null && bigIntVal.compareTo((BigInteger) ad._minValue) < 0) {
							rangeError = true;
						} else if (ad._maxValue != null && bigIntVal.compareTo((BigInteger) ad._maxValue) > 0) {
							rangeError = true;
						}
						break;
					default :
						throw new IllegalStateException();
				}
				if (rangeError) {
					return (NLS.bind(MetaTypeMsg.VALUE_OUT_OF_RANGE, s));
				}
			}
			// No problems detected
			return ""; //$NON-NLS-1$
		} catch (Throwable t) {
			String message = NLS.bind(MetaTypeMsg.EXCEPTION_MESSAGE, t.getClass().getName(), t.getMessage());
			logger.log(LogService.LOG_DEBUG, message, t);
			return message;
		}
	}
}
