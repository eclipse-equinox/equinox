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

import java.io.ByteArrayOutputStream;

public class ConsoleOutputStreamTests {

	private static final int DATA_LENGTH = 4;

	@Test
	public void testWrite() throws Exception {
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		try (ConsoleOutputStream out = new ConsoleOutputStream(byteOut)) {
			byte[] data = new byte[] { 'a', 'b', 'c', 'd' };
			for (byte b : data) {
				out.write(b);
			}
			out.flush();
			byte[] res = byteOut.toByteArray();

			Assert.assertNotNull("Bytes not written; result null", res);
		Assert.assertFalse("Bytes not written; result empty", res.length == 0);

		for (int i = 0; i < DATA_LENGTH; i++) {
			Assert.assertEquals("Wrong char read. Position " + i + ", expected " + data[i] + ", read " + res[i], data[i], res[i]);
		}
		}
	}
}
