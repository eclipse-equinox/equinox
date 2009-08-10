/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
 * This table stores file offsets for cached registry objects.
 * Entries are never added when this table resides in memory. Entries could be removed.
 */
public final class OffsetTable {

	private static final float GROWTH_FACTOR = 1.33f;

	private int[] valueTable;

	public OffsetTable(int size) {
		this.valueTable = new int[size];
	}

	public int get(int key) {
		if (key < valueTable.length)
			return valueTable[key];
		return Integer.MIN_VALUE; // should not happen; will be converted to exception higher in the call stack
	}

	public void removeKey(int key) {
		if (key < valueTable.length) // registry elements added in the running session will have IDs outside of the valid offset range
			valueTable[key] = Integer.MIN_VALUE;
	}

	public void put(int key, int value) {
		if (key >= valueTable.length) { // this should not happen in the expected use cases as we know the max size in advance
			int[] newTable = new int[(int) (key * GROWTH_FACTOR)];
			System.arraycopy(valueTable, 0, newTable, 0, valueTable.length);
			valueTable = newTable;
		}
		valueTable[key] = value;
	}

	public void save(DataOutputStream out) throws IOException {
		int tableSize = valueTable.length;
		out.writeInt(tableSize);
		for (int i = 0; i < tableSize; i++) {
			out.writeInt(valueTable[i]);
		}
	}

	static public OffsetTable load(DataInputStream in) throws IOException {
		int tableSize = in.readInt();
		OffsetTable result = new OffsetTable(tableSize);
		result.valueTable = new int[tableSize];
		for (int i = 0; i < tableSize; i++)
			result.valueTable[i] = in.readInt();
		return result;
	}

}
