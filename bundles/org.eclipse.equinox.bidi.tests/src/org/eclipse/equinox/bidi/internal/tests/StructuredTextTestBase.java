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

/**
 * Base functionality for the handler tests.
 */
public class StructuredTextTestBase {

	static final private char LRM = 0x200E;

	static final private char RLM = 0x200F;

	static final private char LRE = 0x202A;

	static final private char RLE = 0x202B;

	static final private char PDF = 0x202C;

	public static String toPseudo(String text) {
		char[] chars = text.toCharArray();
		int len = chars.length;

		for (int i = 0; i < len; i++) {
			char c = chars[i];
			if (c >= 'A' && c <= 'Z')
				chars[i] = (char) (c + 'a' - 'A');
			else if (c >= 0x05D0 && c < 0x05EA)
				chars[i] = (char) (c + 'A' - 0x05D0);
			else if (c == 0x05EA)
				chars[i] = '~';
			else if (c == 0x0644)
				chars[i] = '#';
			else if (c >= 0x0665 && c <= 0x0669)
				chars[i] = (char) (c + '5' - 0x0665);
			else if (c == LRM)
				chars[i] = '@';
			else if (c == RLM)
				chars[i] = '&';
			else if (c == LRE)
				chars[i] = '>';
			else if (c == RLE)
				chars[i] = '<';
			else if (c == PDF)
				chars[i] = '^';
			else if (c == '\n')
				chars[i] = '|';
			else if (c == '\r')
				chars[i] = '`';
		}
		return new String(chars);
	}

	public static String toUT16(String text) {
		char[] chars = text.toCharArray();
		int len = chars.length;

		for (int i = 0; i < len; i++) {
			char c = chars[i];
			if (c >= '5' && c <= '9')
				chars[i] = (char) (0x0665 + c - '5');
			else if (c >= 'A' && c <= 'Z')
				chars[i] = (char) (0x05D0 + c - 'A');
			else if (c == '~')
				chars[i] = (char) (0x05EA);
			else if (c == '#')
				chars[i] = (char) (0x0644);
			else if (c == '@')
				chars[i] = LRM;
			else if (c == '&')
				chars[i] = RLM;
			else if (c == '>')
				chars[i] = LRE;
			else if (c == '<')
				chars[i] = RLE;
			else if (c == '^')
				chars[i] = PDF;
			else if (c == '|')
				chars[i] = '\n';
			else if (c == '`')
				chars[i] = '\r';
		}
		return new String(chars);
	}

	static String array_display(int[] array) {
		if (array == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder(50);
		int len = array.length;
		for (int i = 0; i < len; i++) {
			sb.append(array[i]);
			sb.append(' ');
		}
		return sb.toString();
	}

}
