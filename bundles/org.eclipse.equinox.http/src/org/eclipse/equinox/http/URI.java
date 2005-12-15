/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http;

import java.io.UnsupportedEncodingException;

public class URI {
	private URI() {
	}

	/**
	 * Takes an encoded string and decodes it
	 * @param input encoded String
	 * @param charset The charset to use convert escaped characters
	 * @return Decoded String.
	 */
	public static String decode(String input, String charset) {
		if (input == null) {
			return null;
		}

		return decode(input, 0, input.length(), charset);
	}

	/**
	 * Takes an encoded string and decodes it
	 * @param input encoded String (must not be null)
	 * @param begin the beginning index, inclusive.
	 * @param end   the ending index, exclusive.
	 * @param charset The charset to use convert escaped characters
	 * @return Decoded String.
	 */
	public static String decode(String input, int begin, int end, String charset) {
		if (input == null) {
			return (null);
		}

		int index = input.indexOf('%', begin);
		if ((index == -1) || (index >= end)) {
			return input.substring(begin, end).replace('+', ' ');
		}

		int size = end - begin;
		StringBuffer result = new StringBuffer(size);
		byte[] bytes = new byte[size];
		int length = 0;

		for (int i = begin; i < end; i++) {
			char c = input.charAt(i);

			if (c == '%') {
				if (i + 2 >= end) {
					throw new IllegalArgumentException();
				}

				i++;
				int digit = Character.digit(input.charAt(i), 16);
				if (digit == -1) {
					throw new IllegalArgumentException();
				}
				int value = (digit << 4);

				i++;
				digit = Character.digit(input.charAt(i), 16);
				if (digit == -1) {
					throw new IllegalArgumentException();
				}
				value |= digit;

				bytes[length] = (byte) value;
				length++;
			} else {
				if (length > 0) {
					result.append(convert(bytes, 0, length, charset));

					length = 0;
				}

				if (c == '+') {
					c = ' ';
				}

				result.append(c);
			}
		}

		if (length > 0) {
			result.append(convert(bytes, 0, length, charset));

			length = 0;
		}

		return result.toString();
	}

	/**
	 * Convert bytes to a String using the supplied charset.
	 * @param input Array of bytes to convert.
	 * @param offset the beginning index, inclusive.
	 * @param length number of bytes to convert.
	 * @param charset The charset to use convert the bytes.
	 * @return String containing converted bytes.
	 */
	public static String convert(byte[] input, int offset, int length, String charset) {
		if (charset != null) {
			try {
				return new String(input, offset, length, charset);
			} catch (UnsupportedEncodingException e) {
				/* if the supplied charset is invalid,
				 * fall through to use 8859_1.
				 */
			}
		}

		try {
			return new String(input, offset, length, "8859_1"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			/* in the unlikely event 8859_1 is not present */
			return new String(input, offset, length);
		}
	}
}
