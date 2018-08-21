/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.io.util;

import java.io.*;
import javax.microedition.io.Datagram;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public abstract class AbstractDatagram implements Datagram {
	protected byte[] data;
	private int pos;
	private int count;

	public AbstractDatagram(int size) {
		this(new byte[size]);
	}

	public AbstractDatagram(byte[] data) {
		this.data = data;
		pos = 0;
		count = data.length;
	}

	public AbstractDatagram(byte[] data, int start, int length) {
		if (start > data.length || (start + length) > data.length) {
			throw new IllegalArgumentException("Start possition or length is greater than data length");
		}

		this.data = data;
		pos = start;
		count = length;
	}

	public synchronized byte[] getData() {
		return data;
	}

	public int getLength() {
		return count;
	}

	public int getOffset() {
		return pos;
	}

	public void setLength(int len) {
		if (len < 0 || len > data.length) {
			throw new IllegalArgumentException("Given length is negative or greater than buffer size");
		}

		count = len;
	}

	public synchronized void setData(byte[] buffer, int offset, int len) {
		if (offset > buffer.length || (offset + len) > buffer.length) {
			throw new IllegalArgumentException();
		}

		data = buffer;
		pos = offset;
		count = len;
	}

	public void reset() {
		pos = 0;
		count = 0;
	}

	public int read() {
		return (pos < count) ? (data[pos++] & 0xff) : -1;
	}

	public void readFully(byte b[]) throws IOException {
		readFully(b, 0, b.length);
	}

	public void readFully(byte b[], int off, int len) throws IOException {
		if (len < 0) {
			throw new IndexOutOfBoundsException();
		}

		int n = 0;

		while (n < len) {
			int ch = read();

			if (ch < 0) {
				throw new EOFException();
			}

			b[off + (n++)] = (byte) ch;
		}
	}

	public int skipBytes(int n) throws IOException {
		if (pos + n > count) {
			n = count - pos;
		}

		if (n < 0) {
			return 0;
		}

		pos += n;
		return n;
	}

	public boolean readBoolean() throws IOException {
		int ch = read();

		if (ch < 0) {
			throw new EOFException();
		}

		return (ch != 0);
	}

	public byte readByte() throws IOException {
		int ch = read();

		if (ch < 0) {
			throw new EOFException();
		}

		return (byte) (ch);
	}

	public int readUnsignedByte() throws IOException {
		int ch = read();

		if (ch < 0) {
			throw new EOFException();
		}

		return ch;
	}

	public short readShort() throws IOException {
		int ch1 = read();
		int ch2 = read();

		if ((ch1 | ch2) < 0) {
			throw new EOFException();
		}

		return (short) ((ch1 << 8) + (ch2 << 0));
	}

	public int readUnsignedShort() throws IOException {
		int ch1 = read();
		int ch2 = read();

		if ((ch1 | ch2) < 0) {
			throw new EOFException();
		}

		return (ch1 << 8) + (ch2 << 0);
	}

	public char readChar() throws IOException {
		int ch1 = read();
		int ch2 = read();

		if ((ch1 | ch2) < 0) {
			throw new EOFException();
		}

		return (char) ((ch1 << 8) + (ch2 << 0));
	}

	public int readInt() throws IOException {
		int ch1 = read();
		int ch2 = read();
		int ch3 = read();
		int ch4 = read();

		if ((ch1 | ch2 | ch3 | ch4) < 0) {
			throw new EOFException();
		}

		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}

	public long readLong() throws IOException {
		return ((long) (readInt()) << 32) + (readInt() & 0xFFFFFFFFL);
	}

	public float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}

	public double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}

	public String readLine() throws IOException {
		throw new RuntimeException("Function not supported");
	}

	public String readUTF() throws IOException {
		return readUTF(this);
	}

	public final static String readUTF(DataInput in) throws IOException {
		int utflen = in.readUnsignedShort();
		StringBuffer str = new StringBuffer(utflen);
		byte bytearr[] = new byte[utflen];
		int c, char2, char3;
		int count = 0;

		in.readFully(bytearr, 0, utflen);

		while (count < utflen) {
			c = bytearr[count] & 0xff;

			switch (c >> 4) {
				case 0 :
				case 1 :
				case 2 :
				case 3 :
				case 4 :
				case 5 :
				case 6 :
				case 7 :
					/* 0xxxxxxx */
					count++;
					str.append((char) c);
					break;

				case 12 :
				case 13 :
					/* 110x xxxx 10xx xxxx */
					count += 2;

					if (count > utflen) {
						throw new UTFDataFormatException();
					}

					char2 = bytearr[count - 1];

					if ((char2 & 0xC0) != 0x80) {
						throw new UTFDataFormatException();
					}

					str.append((char) (((c & 0x1F) << 6) | (char2 & 0x3F)));
					break;

				case 14 :
					/* 1110 xxxx 10xx xxxx 10xx xxxx */
					count += 3;

					if (count > utflen) {
						throw new UTFDataFormatException();
					}

					char2 = bytearr[count - 2];
					char3 = bytearr[count - 1];

					if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80)) {
						throw new UTFDataFormatException();
					}

					str.append((char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0)));
					break;

				default :
					/* 10xx xxxx, 1111 xxxx */
					throw new UTFDataFormatException();
			}
		}

		// The number of chars produced may be less than utflen
		return str.toString();
	}

	// --------------------------------------------------------
	public void write(int b) throws IOException {

		if (pos < data.length) {
			data[pos++] = (byte) b;
		} else {
			byte buf[] = new byte[data.length + 1];
			System.arraycopy(data, 0, buf, 0, count);
			data = buf;
			data[pos++] = (byte) b;
			count++;
		}
	}

	public void write(byte b[]) throws IOException {
		write(b, 0, b.length);
	}

	public void write(byte b[], int off, int len) throws IOException {
		if (b == null) {
			throw new NullPointerException();
		} else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return;
		}

		for (int i = 0; i < len; i++) {
			write(b[off + i]);
		}
	}

	public void writeBoolean(boolean v) throws IOException {
		write(v ? 1 : 0);
	}

	public void writeByte(int v) throws IOException {
		write(v);
	}

	public void writeShort(int v) throws IOException {
		write((v >>> 8) & 0xFF);
		write((v >>> 0) & 0xFF);
	}

	public void writeChar(int v) throws IOException {
		write((v >>> 8) & 0xFF);
		write((v >>> 0) & 0xFF);
	}

	public void writeInt(int v) throws IOException {
		write((v >>> 24) & 0xFF);
		write((v >>> 16) & 0xFF);
		write((v >>> 8) & 0xFF);
		write((v >>> 0) & 0xFF);
	}

	public void writeLong(long v) throws IOException {
		write((int) (v >>> 56) & 0xFF);
		write((int) (v >>> 48) & 0xFF);
		write((int) (v >>> 40) & 0xFF);
		write((int) (v >>> 32) & 0xFF);
		write((int) (v >>> 24) & 0xFF);
		write((int) (v >>> 16) & 0xFF);
		write((int) (v >>> 8) & 0xFF);
		write((int) (v >>> 0) & 0xFF);
	}

	public void writeFloat(float v) throws IOException {
		writeInt(Float.floatToIntBits(v));
	}

	public void writeDouble(double v) throws IOException {
		writeLong(Double.doubleToLongBits(v));
	}

	public void writeBytes(String s) throws IOException {
		int len = s.length();

		for (int i = 0; i < len; i++) {
			write((byte) s.charAt(i));
		}
	}

	public void writeChars(String s) throws IOException {
		int len = s.length();

		for (int i = 0; i < len; i++) {
			int v = s.charAt(i);
			write((v >>> 8) & 0xFF);
			write((v >>> 0) & 0xFF);
		}
	}

	public void writeUTF(String str) throws IOException {
		writeUTF(str, this);
	}

	static int writeUTF(String str, DataOutput out) throws IOException {
		int strlen = str.length();
		int utflen = 0;
		char[] charr = new char[strlen];
		int c, count = 0;

		str.getChars(0, strlen, charr, 0);

		for (int i = 0; i < strlen; i++) {
			c = charr[i];

			if ((c >= 0x0001) && (c <= 0x007F)) {
				utflen++;
			} else if (c > 0x07FF) {
				utflen += 3;
			} else {
				utflen += 2;
			}
		}

		if (utflen > 65535) {
			throw new UTFDataFormatException();
		}

		byte[] bytearr = new byte[utflen + 2];
		bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
		bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);

		for (int i = 0; i < strlen; i++) {
			c = charr[i];

			if ((c >= 0x0001) && (c <= 0x007F)) {
				bytearr[count++] = (byte) c;
			} else if (c > 0x07FF) {
				bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
				bytearr[count++] = (byte) (0x80 | ((c >> 6) & 0x3F));
				bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			} else {
				bytearr[count++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
				bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			}
		}

		out.write(bytearr);
		return utflen + 2;
	}

}
