/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.util.string;

/**
 * Class which stores text in a <code>char</code> array buffer 
 * and allows inserting, deleting, removing, appending etc. 
 * actions with the stored text.
 * 
 * @author Pavlin Dobrev
 * @author Georgi Andreev
 * @version 1.0
 */
public class CharBuffer {

	private char value[];
	private int cnt;

	public CharBuffer() {
		this(16);
	}

	public CharBuffer(int len) {
		value = new char[len];
	}

	public int length() {
		return cnt;
	}

	public void ensureCapacity(int minCapacity) {
		if (minCapacity > value.length)
			expandCapacity(minCapacity);
	}

	private void expandCapacity(int minCapacity) {
		int capacity = (value.length + 1) * 2;
		if (minCapacity > capacity) {
			capacity = minCapacity;
		}
		char newVal[] = new char[capacity];
		System.arraycopy(value, 0, newVal, 0, cnt);
		value = newVal;
	}

	public void setLength(int newLen) {
		if (newLen > value.length)
			expandCapacity(newLen);
		if (cnt < newLen) {
			for (; cnt < newLen; cnt++) {
				value[cnt] = '\0';
			}
		} else {
			cnt = newLen;
		}
	}

	public char charAt(int index) {
		if ((index < 0) || (index >= cnt))
			throw new StringIndexOutOfBoundsException(index);
		return value[index];
	}

	public void setCharAt(int index, char ch) {
		if ((index < 0) || (index >= cnt))
			throw new StringIndexOutOfBoundsException(index);
		value[index] = ch;
	}

	public void append(String str) {
		if (str == null)
			str = String.valueOf(str);
		int len = str.length();
		int newcnt = cnt + len;
		if (newcnt > value.length)
			expandCapacity(newcnt);
		str.getChars(0, len, value, cnt);
		cnt = newcnt;
	}

	public void append(char str[], int offset, int len) {
		int newcnt = cnt + len;
		if (newcnt > value.length)
			expandCapacity(newcnt);
		System.arraycopy(str, offset, value, cnt, len);
		cnt = newcnt;
	}

	public void append(char str[]) {
		append(str, 0, str.length);
	}

	public void append(char c) {
		int newcount = cnt + 1;
		if (newcount > value.length)
			expandCapacity(newcount);
		value[cnt++] = c;
	}

	public String substring(int start, int end) {
		return new String(value, start, end - start);
	}

	public String trim() {
		int len = cnt;
		int st = 0;
		int off = 0;
		char[] newVal = value;

		while ((st < len) && (newVal[off + st] <= ' '))
			st++;
		while ((st < len) && (newVal[off + len - 1] <= ' '))
			len--;

		return ((st > 0) || (len < cnt)) ? substring(st, len) : toString();
	}

	public boolean equals(int offset, String with) {
		int len = with.length();
		if (offset >= cnt || offset + len != cnt)
			return false;

		for (int i = offset; i < cnt; i++) {
			if (value[i] != with.charAt(i - offset))
				return false;
		}

		return true;
	}

	public void getChars(int srcStart, int srcEnd, char dest[], int destStart) {
		if ((srcStart < 0) || (srcStart >= cnt))
			throw new StringIndexOutOfBoundsException(srcStart);
		if ((srcEnd < 0) || (srcEnd > cnt))
			throw new StringIndexOutOfBoundsException(srcEnd);

		if (srcStart < srcEnd)
			System.arraycopy(value, srcStart, dest, destStart, srcEnd - srcStart);
		else if (srcStart > srcEnd)
			throw new StringIndexOutOfBoundsException(srcEnd - srcStart);
	}

	public String toString() {
		return new String(value, 0, cnt);
	}

	public char[] getValue() {
		char[] retVal;
		retVal = new char[cnt];
		System.arraycopy(value, 0, retVal, 0, cnt);
		return retVal;
	}

}
