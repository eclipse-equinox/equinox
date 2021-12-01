/*******************************************************************************
 * Copyright (c) 2011, 2021 SAP AG and others.
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

package org.eclipse.equinox.console.telnet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.eclipse.equinox.console.common.ConsoleOutputStream;
import org.junit.Test;

public class TelnetInputHandlerTests {

	private static final long WAIT_TIME = 10000;

	@Test
	public void testHandler() throws Exception {
		ByteArrayInputStream input = new ByteArrayInputStream("abcde".getBytes());
		ConsoleInputStream in = new ConsoleInputStream();
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		ConsoleOutputStream out = new ConsoleOutputStream(byteOut);
		Callback callback = mock(Callback.class);
		TelnetInputHandler handler = new TelnetInputHandler(input, in, out, callback);
		handler.start();

		// wait for the accept thread to start execution
		try {
			Thread.sleep(WAIT_TIME);
		} catch (InterruptedException ie) {
			// do nothing
		}

		String res = byteOut.toString();
		assertEquals("Wrong input. Expected abcde, read " + res, "abcde", res);
	}

}
