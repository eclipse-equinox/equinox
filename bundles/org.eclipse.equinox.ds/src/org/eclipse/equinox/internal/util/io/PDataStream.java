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
package org.eclipse.equinox.internal.util.io;

import java.io.*;

/**
 * @author Pavlin Dobrev
 * @author Georgi Andreev
 * @version 1.0
 */
public final class PDataStream {

	public static void writeInt(int i, OutputStream os) throws IOException {
		os.write((i >>> 24) & 0xFF);
		os.write((i >>> 16) & 0xFF);
		os.write((i >>> 8) & 0xFF);
		os.write(i & 0xFF);
	}

	public static void writeLong(long l, OutputStream os) throws IOException {
		os.write((int) (l >>> 56) & 0xFF);
		os.write((int) (l >>> 48) & 0xFF);
		os.write((int) (l >>> 40) & 0xFF);
		os.write((int) (l >>> 32) & 0xFF);
		os.write((int) (l >>> 24) & 0xFF);
		os.write((int) (l >>> 16) & 0xFF);
		os.write((int) (l >>> 8) & 0xFF);
		os.write((int) (l & 0xFF));
	}

	public static void writeShort(short s, OutputStream os) throws IOException {
		os.write((s >>> 8) & 0xFF);
		os.write(s & 0xFF);
	}

	public static void writeChar(char ch, OutputStream os) throws IOException {
		os.write((ch >>> 8) & 0xFF);
		os.write(ch & 0xFF);
	}

	public static void writeBoolean(boolean b, OutputStream os) throws IOException {
		os.write(b ? 1 : 0);
	}

	public static void writeByte(byte b, OutputStream os) throws IOException {
		os.write(b);
	}

	public static void writeBytes(String str, OutputStream os) throws IOException {
		byte[] b = str.getBytes();
		os.write(b);
	}

	public static void writeString(String str, OutputStream os) throws IOException {
		if (str == null) {
			writeBoolean(false, os);
		} else {
			writeBoolean(true, os);
			writeUTF(str, os);
		}
	}

	public static void writeUTF(String str, OutputStream os) throws IOException {
		int strlen = str.length();
		int utflen = 0;
		for (int i = 0; i < strlen; i++) {
			int ch = str.charAt(i);
			if ((ch >= 0x0001) && (ch <= 0x007F)) {
				utflen++;
			} else if (ch > 0x07FF) {
				utflen += 3;
			} else {
				utflen += 2;
			}
		}
		if (utflen > 65535)
			throw new UTFDataFormatException();
		os.write((utflen >>> 8) & 0xFF);
		os.write(utflen & 0xFF);
		for (int i = 0; i < strlen; i++) {
			int ch = str.charAt(i);
			if ((ch >= 0x0001) && (ch <= 0x007F)) {
				os.write(ch);
			} else if (ch > 0x07FF) {
				os.write(0xE0 | ((ch >> 12) & 0x0F));
				os.write(0x80 | ((ch >> 6) & 0x3F));
				os.write(0x80 | (ch & 0x3F));
			} else {
				os.write(0xC0 | ((ch >> 6) & 0x1F));
				os.write(0x80 | (ch & 0x3F));
			}
		}
	}

	public static void writeChars(String str, OutputStream os) throws IOException {
		int len = str.length();
		for (int i = 0; i < len; i++) {
			int ch = str.charAt(i);
			os.write((ch >>> 8) & 0xFF);
			os.write(ch & 0xFF);
		}
	}

	public static void writeDouble(double d, OutputStream os) throws IOException {
		writeLong(Double.doubleToLongBits(d), os);
	}

	public static void writeFloat(float f, OutputStream os) throws IOException {
		writeInt(Float.floatToIntBits(f), os);
	}

	public static int readInt(InputStream is) throws IOException {
		int ch1 = is.read();
		int ch2 = is.read();
		int ch3 = is.read();
		int ch4 = is.read();
		if ((ch1 | ch2 | ch3 | ch4) < 0) {
			throw new IOException("Read Error");
		}
		return (ch1 << 24) | (ch2 << 16) | (ch3 << 8) | ch4;
	}

	public static char readChar(InputStream is) throws IOException {
		int ch1 = is.read();
		int ch2 = is.read();
		if ((ch1 | ch2) < 0)
			throw new IOException("Read Error");
		return (char) ((ch1 << 8) | ch2);
	}

	public static short readShort(InputStream is) throws IOException {
		int ch1 = is.read();
		int ch2 = is.read();
		if ((ch1 | ch2) < 0)
			throw new IOException("Read Error");
		return (short) ((ch1 << 8) | ch2);
	}

	public static long readLong(InputStream is) throws IOException {
		return ((long) (readInt(is)) << 32) | (readInt(is) & 0xFFFFFFFFL);
	}

	public static boolean readBoolean(InputStream is) throws IOException {
		int ch = is.read();
		if (ch < 0) {
			throw new EOFException();
		}
		return (ch != 0);
	}

	public static byte readByte(InputStream is) throws IOException {
		int ch = is.read();
		if (ch < 0)
			throw new EOFException();
		return (byte) (ch);
	}

	public static int readUnsignedByte(InputStream is) throws IOException {
		int ch = is.read();
		if (ch < 0)
			throw new EOFException();
		return ch;
	}

	public static double readDouble(InputStream is) throws IOException {
		return Double.longBitsToDouble(readLong(is));
	}

	public static float readFloat(InputStream is) throws IOException {
		return Float.intBitsToFloat(readInt(is));
	}

	public static String readString(InputStream is) throws IOException {
		if (readBoolean(is))
			return readUTF(is);
		return null;
	}

	public static String readUTF(InputStream is) throws IOException {
		int utflen = readShort(is);
		char str[] = new char[utflen];
		int cnt = 0;
		int strlen = 0;
		while (cnt < utflen) {
			int c = readUnsignedByte(is);
			int char2, char3;
			switch (c >> 4) {
				case 0 :
				case 1 :
				case 2 :
				case 3 :
				case 4 :
				case 5 :
				case 6 :
				case 7 :
					cnt++;
					str[strlen++] = (char) c;
					break;
				case 12 :
				case 13 :
					cnt += 2;
					if (cnt > utflen)
						throw new UTFDataFormatException();
					char2 = readUnsignedByte(is);
					if ((char2 & 0xC0) != 0x80)
						throw new UTFDataFormatException();
					str[strlen++] = (char) (((c & 0x1F) << 6) | (char2 & 0x3F));
					break;
				case 14 :
					cnt += 3;
					if (cnt > utflen)
						throw new UTFDataFormatException();
					char2 = readUnsignedByte(is);
					char3 = readUnsignedByte(is);
					if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
						throw new UTFDataFormatException();
					str[strlen++] = (char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | (char3 & 0x3F));
					break;
				default :
					throw new UTFDataFormatException();
			}
		}
		return new String(str, 0, strlen);
	}

}
