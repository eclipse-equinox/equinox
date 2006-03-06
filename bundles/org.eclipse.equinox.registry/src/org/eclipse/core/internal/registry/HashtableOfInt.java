/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

import java.io.*;

/**
 *	Hashtable for non-zero int keys.
 */

public final class HashtableOfInt {
	private int[] keyTable;
	private int[] valueTable;
	private static final float GROWTH_FACTOR = 1.33f;

	private int elementSize; // number of elements in the table
	private int threshold;

	public HashtableOfInt() {
		this(13);
	}

	public HashtableOfInt(int size) {
		this.elementSize = 0;
		this.threshold = size; // size represents the expected number of elements
		int extraRoom = (int) (size * 1.33f);
		if (this.threshold == extraRoom)
			extraRoom++;
		this.keyTable = new int[extraRoom];
		this.valueTable = new int[extraRoom];
	}

	public boolean containsKey(int key) {
		int index = key % valueTable.length;
		int currentKey;
		while ((currentKey = keyTable[index]) != 0) {
			if (currentKey == key)
				return true;
			index = (index + 1) % keyTable.length;
		}
		return false;
	}

	public int get(int key) {
		int index = key % valueTable.length;
		int currentKey;
		while ((currentKey = keyTable[index]) != 0) {
			if (currentKey == key)
				return valueTable[index];
			index = (index + 1) % keyTable.length;
		}
		return Integer.MIN_VALUE;
	}

	public int removeKey(int key) {
		int index = key % valueTable.length;
		int currentKey;
		while ((currentKey = keyTable[index]) != 0) {
			if (currentKey == key) {
				return valueTable[index];
			}
			index = (index + 1) % keyTable.length;
		}
		return Integer.MIN_VALUE;
	}

	public int put(int key, int value) {
		int index = key % valueTable.length;
		int currentKey;
		while ((currentKey = keyTable[index]) != 0) {
			if (currentKey == key)
				return valueTable[index] = value;
			index = (index + 1) % keyTable.length;
		}
		keyTable[index] = key;
		valueTable[index] = value;

		// assumes the threshold is never equal to the size of the table
		if (++elementSize > threshold)
			rehash();
		return value;
	}

	private void rehash() {
		HashtableOfInt newHashtable = new HashtableOfInt((int) (elementSize * GROWTH_FACTOR)); // double the number of expected elements
		int currentKey;
		for (int i = keyTable.length; --i >= 0;)
			if ((currentKey = keyTable[i]) != 0)
				newHashtable.put(currentKey, valueTable[i]);

		this.keyTable = newHashtable.keyTable;
		this.valueTable = newHashtable.valueTable;
		this.threshold = newHashtable.threshold;
	}

	public int size() {
		return elementSize;
	}

	public String toString() {
		String s = ""; //$NON-NLS-1$
		int object;
		for (int i = 0, length = valueTable.length; i < length; i++)
			if ((object = valueTable[i]) != Integer.MIN_VALUE)
				s += keyTable[i] + " -> " + object + "\n"; //$NON-NLS-2$ //$NON-NLS-1$
		return s;
	}

	public void save(DataOutputStream out) throws IOException {
		out.writeInt(elementSize);
		int tableSize = keyTable.length;
		out.writeInt(tableSize);
		out.writeInt(threshold);

		out.write(IntToByte(keyTable));
		out.write(IntToByte(valueTable));
	}

	public void load(DataInputStream in) throws IOException {
		elementSize = in.readInt();
		int tableSize = in.readInt();
		threshold = in.readInt();

		byte[] inBuffer = new byte[tableSize * 4];
		in.readFully(inBuffer);
		keyTable = ByteToInt(inBuffer);
		in.readFully(inBuffer);
		valueTable = ByteToInt(inBuffer);
	}

	private byte[] IntToByte(int[] intArray) {
		int intSize = intArray.length;
		byte[] byteArray = new byte[4 * intSize];
		int bytePos = 0;
		for (int i = 0; i < intSize; i++) {
			int intValue = intArray[i];
			byteArray[bytePos] = (byte) (intValue & 0xFF);
			byteArray[bytePos + 1] = (byte) ((intValue >>> 8) & 0xFF);
			byteArray[bytePos + 2] = (byte) ((intValue >>> 16) & 0xFF);
			byteArray[bytePos + 3] = (byte) ((intValue >>> 24) & 0xFF);
			bytePos += 4;
		}
		return byteArray;
	}

	private int[] ByteToInt(byte[] byteArray) {
		int intSize = byteArray.length / 4;
		int[] intArray = new int[intSize];
		int bytePos = 0;
		for (int intPos = 0; intPos < intSize; intPos++) {
			int result = (byteArray[bytePos] & 0xFF);
			result |= ((byteArray[bytePos + 1] & 0xFF) << 8);
			result |= ((byteArray[bytePos + 2] & 0xFF) << 16);
			result |= ((byteArray[bytePos + 3] & 0xFF) << 24);
			intArray[intPos] = result;
			bytePos += 4;
		}
		return intArray;
	}
}
