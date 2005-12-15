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

public class Tokenizer {
	private char value[];
	private int max;
	private int cursor;

	public Tokenizer(String value) {
		this.value = value.toCharArray();
		max = this.value.length;
		cursor = 0;
	}

	public char getChar() {
		int cur = cursor;
		if (cur < max) {
			cursor = cur + 1;
			return (value[cur]);
		}
		return ('\0'); /* end of value */
	}

	public String getString(String terminals) {
		skipWhiteSpace();
		char[] val = value;
		int cur = cursor;

		if (cur < max) {
			if (val[cur] == '\"') /* if a quoted string */
			{
				cur++; /* skip quote */
				char c = '\0';
				int begin = cur;
				for (; cur < max; cur++) {
					c = val[cur];
					if (c == '\"') {
						break;
					}
				}
				int count = cur - begin;
				if (c == '\"') {
					cur++;
				}
				cursor = cur;
				if (count > 0) {
					skipWhiteSpace();
					return (new String(val, begin, count));
				}
			} else /* not a quoted string; same as token */
			{
				int begin = cur;
				for (; cur < max; cur++) {
					char c = val[cur];
					if ((c == ' ') || (c == '\t') || (terminals.indexOf(c) != -1)) {
						break;
					}
				}
				cursor = cur;
				int count = cur - begin;
				if (count > 0) {
					skipWhiteSpace();
					return (new String(val, begin, count));
				}
			}
		}
		return (null);
	}

	public String getToken(String terminals) {
		skipWhiteSpace();
		char[] val = value;
		int cur = cursor;

		int begin = cur;
		for (; cur < max; cur++) {
			char c = val[cur];
			if ((c == ' ') || (c == '\t') || (terminals.indexOf(c) != -1)) {
				break;
			}
		}
		cursor = cur;
		int count = cur - begin;
		if (count > 0) {
			skipWhiteSpace();
			return (new String(val, begin, count));
		}
		return (null);
	}

	private void skipWhiteSpace() {
		char[] val = value;
		int cur = cursor;

		for (; cur < max; cur++) {
			char c = val[cur];
			if ((c == ' ') || (c == '\t')) {
				continue;
			}
			break;
		}
		cursor = cur;
	}
}
