/*******************************************************************************
 * Copyright (c) 2011, 2017 SAP AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Lazar Kirchev, SAP AG - initial contribution
 ******************************************************************************/

package org.eclipse.equinox.console.common;

import org.junit.Assert;
import org.junit.Test;

public class ConsoleInputStreamTests {

	private static final int DATA_LENGTH = 4;

	@Test
	public void addReadBufferTest() throws Exception {
		try (ConsoleInputStream in = new ConsoleInputStream()) {
			byte[] data = new byte[] { (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd' };
			in.add(data);
			byte[] read = new byte[DATA_LENGTH];
			for (int i = 0; i < DATA_LENGTH; i++) {
				in.read(read, i, 1);
				Assert.assertEquals(
						"Incorrect char read; position " + i + " expected: " + data[i] + ", actual: " + read[i],
						read[i], data[i]);
			}
		}
	}

	@Test
	public void addReadTest() throws Exception {
		try (ConsoleInputStream in = new ConsoleInputStream()) {
			byte[] data = new byte[] { (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd' };
			in.add(data);
			for (int i = 0; i < DATA_LENGTH; i++) {
				byte symbol = (byte) in.read();
				Assert.assertEquals(
						"Incorrect char read; position " + i + " expected: " + data[i] + ", actual: " + symbol, symbol,
						data[i]);
			}
		}
	}
}
