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

package org.eclipse.equinox.console.supportability;

import org.eclipse.equinox.console.common.ConsoleInputHandler;
import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class ConsoleInputHandlerTests {

	private static final long WAIT_TIME = 10000;

	@Test
	public void testHandler() throws Exception {
		try (PipedInputStream input = new PipedInputStream(); PipedOutputStream output = new PipedOutputStream(input)) {
			ConsoleInputStream in = new ConsoleInputStream();
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			ConsoleInputHandler handler = new ConsoleInputHandler(input, in, byteOut);
			byte[] testInput = new byte[] { 'a', 'b', 'c', 'd', 'e', '\r', '\n' };
			byte[] expected = new byte[] { 'a', 'b', 'c', 'd', 'e', '\n' };
			output.write(testInput);
			output.flush();
			handler.start();

			try {
				Thread.sleep(WAIT_TIME);
			} catch (Exception e) {
				// do nothing
			}

			byte[] read = new byte[expected.length];
			in.read(read, 0, expected.length);
			for (int i = 0; i < expected.length; i++) {
				Assert.assertEquals(
						"Incorrect char read. Position " + i + ", expected " + expected[i] + ", read " + read[i],
						expected[i], read[i]);
			}

		}
	}

}
