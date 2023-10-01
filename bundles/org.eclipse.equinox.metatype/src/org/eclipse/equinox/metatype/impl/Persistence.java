package org.eclipse.equinox.metatype.impl;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/

public class Persistence {
	private static final int PERSISTENCE_VERSION = 0;
	private static final byte NULL = 0;
	private static final byte OBJECT = 1;
	private static final byte INDEX = 2;
	private static final byte LONG_STRING = 3;

	private Persistence() {
	}

	public static class Reader implements Closeable {
		private final Map<Integer, String> readStringCache = new HashMap<>();
		private final DataInputStream in;

		public Reader(DataInputStream in) {
			this.in = in;
		}

		public boolean isValidPersistenceVersion() throws IOException {
			return in.readInt() == PERSISTENCE_VERSION;
		}

		public void readIndexedStrings() throws IOException {
			int num = in.readInt();
			for (int i = 0; i < num; i++) {
				readIndexedString();
			}
		}

		private String readIndexedString() throws IOException {
			String string = readString();
			readStringCache.put(Integer.valueOf(in.readInt()), string);
			return string;
		}

		public String readString() throws IOException {
			byte type = in.readByte();
			if (type == INDEX) {
				int index = in.readInt();
				return readStringCache.get(index);
			}
			if (type == NULL) {
				return null;
			}
			String string;
			if (type == LONG_STRING) {
				int length = in.readInt();
				byte[] data = new byte[length];
				in.readFully(data);
				string = new String(data, StandardCharsets.UTF_8);
			} else {
				string = in.readUTF();
			}

			return string;
		}

		public long readLong() throws IOException {
			return in.readLong();
		}

		public int readInt() throws IOException {
			return in.readInt();
		}

		@Override
		public void close() throws IOException {
			in.close();
		}

		public boolean readBoolean() throws IOException {
			return in.readBoolean();
		}

		public short readShort() throws IOException {
			return in.readShort();
		}

		public char readCharacter() throws IOException {
			return in.readChar();
		}

		public byte readByte() throws IOException {
			return in.readByte();
		}

		public double readDouble() throws IOException {
			return in.readDouble();
		}

		public float readFloat() throws IOException {
			return in.readFloat();
		}
	}

	public static class Writer implements Closeable {
		private final Map<String, Integer> writeStringCache = new HashMap<>();
		private final DataOutputStream out;

		public Writer(DataOutputStream out) {
			this.out = out;
		}

		public void writePersistenceVersion() throws IOException {
			out.writeInt(PERSISTENCE_VERSION);
		}

		public void writeIndexedStrings(Set<String> strings) throws IOException {
			strings.remove(null); // do not index null
			out.writeInt(strings.size());
			for (String string : strings) {
				writeIndexedString(string);
			}
		}

		private void writeIndexedString(String string) throws IOException {
			writeString(string);
			addToIndex(string);
		}

		public void writeString(String string) throws IOException {
			Integer index = string != null ? writeStringCache.get(string) : null;
			if (index != null) {
				out.writeByte(INDEX);
				out.writeInt(index);
				return;
			}

			if (string == null)
				out.writeByte(NULL);
			else {
				byte[] data = string.getBytes(StandardCharsets.UTF_8);

				if (data.length > 65535) {
					out.writeByte(LONG_STRING);
					out.writeInt(data.length);
					out.write(data);
				} else {
					out.writeByte(OBJECT);
					out.writeUTF(string);
				}
			}
		}

		private void addToIndex(String string) throws IOException {
			if (string == null) {
				throw new NullPointerException();
			}
			Integer cur = writeStringCache.get(string);
			if (cur != null)
				throw new IllegalStateException("String is already in the write table: " + string); //$NON-NLS-1$
			Integer index = writeStringCache.size();
			writeStringCache.put(string, index);
			out.writeInt(index.intValue());
		}

		@Override
		public void close() throws IOException {
			out.close();
		}

		public void writeInt(int v) throws IOException {
			out.writeInt(v);
		}

		public void writeLong(long v) throws IOException {
			out.writeLong(v);
		}

		public void writeBoolean(boolean v) throws IOException {
			out.writeBoolean(v);
		}

		public void writeShort(Short v) throws IOException {
			out.writeShort(v);
		}

		public void writeCharacter(Character v) throws IOException {
			out.writeChar(v);
		}

		public void writeByte(Byte v) throws IOException {
			out.writeByte(v);
		}

		public void writeDouble(Double v) throws IOException {
			out.writeDouble(v);
		}

		public void writeFloat(Float v) throws IOException {
			out.writeFloat(v);
		}
	}
}
