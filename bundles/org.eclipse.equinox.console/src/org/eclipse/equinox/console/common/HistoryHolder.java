/*******************************************************************************
 * Copyright (c) 2010, 2018 SAP AG
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation 
 *******************************************************************************/

package org.eclipse.equinox.console.common;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A helper class, which implements history.
 */
public class HistoryHolder {

	private static final int MAX = 100;
	private final byte[][] history;
	private int size;
	private int pos;

	public HistoryHolder() {
		history = new byte[MAX][];
	}

	public synchronized void reset() {
		size = 0;
		pos = 0;
		for (int i = 0; i < MAX; i++) {
			history[i] = null;
		}
	}

	public synchronized void add(byte[] data) {
		data = new String(data, StandardCharsets.US_ASCII).trim().getBytes(StandardCharsets.US_ASCII);
		if (data.length == 0) {
			pos = size;
			return;
		}
		for (int i = 0; i < size; i++) {
			if (Arrays.equals(history[i], data)) {
				System.arraycopy(history, i + 1, history, i, size - i - 1);
				history[size - 1] = data;
				pos = size;
				return;
			}
		}
		if (size >= MAX) {
			System.arraycopy(history, 1, history, 0, size - 1);
			size--;
		}
		history[size++] = data;
		pos = size;
	}

	public synchronized byte[] next() {
		if (pos >= size - 1) {
			return null;
		}
		return history[++pos];
	}

	public synchronized byte[] last() {
		if (size > 0) {
			pos = size - 1;
			return history[pos];
		} else {
			return null;
		}
	}

	public synchronized byte[] first() {
		if (size > 0) {
			pos = 0;
			return history[pos];
		} else {
			return null;
		}
	}

	public synchronized byte[] prev() {
		if (size == 0) {
			return null;
		}
		if (pos == 0) {
			return history[pos];
		} else {
			return history[--pos];
		}
	}
}
