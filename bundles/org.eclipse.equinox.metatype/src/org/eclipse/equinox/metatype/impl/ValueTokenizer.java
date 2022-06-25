/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.metatype.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.metatype.AttributeDefinition;

public class ValueTokenizer {
	private static final char DELIMITER = ',';
	private static final char ESCAPE = '\\';

	private final LogTracker logger;
	private final List<String> values = new ArrayList<>();

	/*
	 * Constructor of class ValueTokenizer
	 */
	public ValueTokenizer(String values_str, LogTracker logger) {
		this.logger = logger;
		if (values_str == null)
			return;
		// The trick is to strip out unescaped whitespace characters before and
		// after the input string as well as before and after each 
		// individual token within the input string without losing any escaped 
		// whitespace characters. Whitespace between two non-whitespace
		// characters may or may not be escaped. Also, any character may be
		// escaped. The escape character is '\'. The delimiter is ','.
		StringBuilder buffer = new StringBuilder();
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
					}
					// If the ESCAPE character occurs as the last character
					// of the string, ignore it.
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
	 * Method to return values as collection.
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
		StringBuilder buffer = new StringBuilder(values.get(0));
		for (int i = 1; i < values.size(); i++) {
			buffer.append(',');
			buffer.append(values.get(i));
		}
		return buffer.toString();
	}

	private boolean validateString(AttributeDefinitionImpl ad, String value) {
		// Try validating the string length.
		try {
			// All or nothing. If either min or max is not null and cannot be
			// parsed as an integer, do the string comparison instead.
			Integer min = ad.getMinValue() == null ? null : Integer.valueOf((String) ad.getMinValue());
			Integer max = ad.getMaxValue() == null ? null : Integer.valueOf((String) ad.getMaxValue());
			if (min != null && value.length() < min)
				return true;
			if (max != null && value.length() > max)
				return true;
		} catch (NumberFormatException e) {
			// Either min or max was not an integer. Do a string comparison.
			if ((ad.getMinValue() != null && value.compareTo((String) ad.getMinValue()) < 0) || (ad.getMaxValue() != null && value.compareTo((String) ad.getMaxValue()) > 0))
				return true;
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	public String validate(AttributeDefinitionImpl ad) {
		// An empty list means the original value was null. Null is never valid.
		if (values.isEmpty()) {
			return MetaTypeMsg.NULL_IS_INVALID;
		}
		String s = null;
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
				s = i.next();
				// If options were declared and the value does not match one of them, the value is not valid.
				if (ad.containsInvalidValue(s)) {
					return NLS.bind(MetaTypeMsg.VALUE_OUT_OF_OPTION, s);
				}
				// Check the type. Also check the range if min or max were declared.
				boolean rangeError = false;
				switch (ad.getType()) {
					case AttributeDefinition.PASSWORD :
					case AttributeDefinition.STRING :
						rangeError = validateString(ad, s);
						break;
					case AttributeDefinition.INTEGER :
						Integer intVal = Integer.valueOf(s);
						if (ad.getMinValue() != null && intVal.compareTo((Integer) ad.getMinValue()) < 0) {
							rangeError = true;
						} else if (ad.getMaxValue() != null && intVal.compareTo((Integer) ad.getMaxValue()) > 0) {
							rangeError = true;
						}
						break;
					case AttributeDefinition.LONG :
						Long longVal = Long.valueOf(s);
						if (ad.getMinValue() != null && longVal.compareTo((Long) ad.getMinValue()) < 0) {
							rangeError = true;
						} else if (ad.getMaxValue() != null && longVal.compareTo((Long) ad.getMaxValue()) > 0) {
							rangeError = true;
						}
						break;
					case AttributeDefinition.DOUBLE :
						Double doubleVal = Double.valueOf(s);
						if (ad.getMinValue() != null && doubleVal.compareTo((Double) ad.getMinValue()) < 0) {
							rangeError = true;
						} else if (ad.getMaxValue() != null && doubleVal.compareTo((Double) ad.getMaxValue()) > 0) {
							rangeError = true;
						}
						break;
					case AttributeDefinition.BOOLEAN :
						// Any string can be converted into a boolean via Boolean.valueOf(String).
						// Seems unnecessary to impose any further restrictions.
						break;
					case AttributeDefinition.CHARACTER :
						Character charVal = Character.valueOf(s.charAt(0));
						if (ad.getMinValue() != null && charVal.compareTo((Character) ad.getMinValue()) < 0) {
							rangeError = true;
						} else if (ad.getMaxValue() != null && charVal.compareTo((Character) ad.getMaxValue()) > 0) {
							rangeError = true;
						}
						break;
					case AttributeDefinition.FLOAT :
						Float floatVal = Float.valueOf(s);
						if (ad.getMinValue() != null && floatVal.compareTo((Float) ad.getMinValue()) < 0) {
							rangeError = true;
						} else if (ad.getMaxValue() != null && floatVal.compareTo((Float) ad.getMaxValue()) > 0) {
							rangeError = true;
						}
						break;
					case AttributeDefinition.SHORT :
						Short shortVal = Short.valueOf(s);
						if (ad.getMinValue() != null && shortVal.compareTo((Short) ad.getMinValue()) < 0) {
							rangeError = true;
						} else if (ad.getMaxValue() != null && shortVal.compareTo((Short) ad.getMaxValue()) > 0) {
							rangeError = true;
						}
						break;
					case AttributeDefinition.BYTE :
						Byte byteVal = Byte.valueOf(s);
						if (ad.getMinValue() != null && byteVal.compareTo((Byte) ad.getMinValue()) < 0) {
							rangeError = true;
						} else if (ad.getMaxValue() != null && byteVal.compareTo((Byte) ad.getMaxValue()) > 0) {
							rangeError = true;
						}
						break;
					case AttributeDefinition.BIGDECIMAL :
						BigDecimal bigDecVal = new BigDecimal(s);
						if (ad.getMinValue() != null && bigDecVal.compareTo((BigDecimal) ad.getMinValue()) < 0) {
							rangeError = true;
						} else if (ad.getMaxValue() != null && bigDecVal.compareTo((BigDecimal) ad.getMaxValue()) > 0) {
							rangeError = true;
						}
						break;
					case AttributeDefinition.BIGINTEGER :
						BigInteger bigIntVal = new BigInteger(s);
						if (ad.getMinValue() != null && bigIntVal.compareTo((BigInteger) ad.getMinValue()) < 0) {
							rangeError = true;
						} else if (ad.getMaxValue() != null && bigIntVal.compareTo((BigInteger) ad.getMaxValue()) > 0) {
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
		} catch (NumberFormatException e) {
			return NLS.bind(MetaTypeMsg.VALUE_NOT_A_NUMBER, s);
		} catch (Exception e) {
			String message = NLS.bind(MetaTypeMsg.EXCEPTION_MESSAGE, e.getClass().getName(), e.getMessage());
			logger.log(LogTracker.LOG_DEBUG, message, e);
			return message;
		}
	}
}
