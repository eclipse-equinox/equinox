/*******************************************************************************
 * Copyright (c) 2011, 2018 SAP AG and others.
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

package org.eclipse.equinox.console.ssh;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.eclipse.equinox.console.common.ConsoleOutputStream;
import org.junit.Assert;
import org.junit.Test;


public class SshInputHandlerTests {

	private static final long WAIT_TIME = 10000;

	@Test
	public void testHandler() throws Exception {
		ByteArrayInputStream input = new ByteArrayInputStream("abcde".getBytes(StandardCharsets.UTF_8));
		ConsoleInputStream in = new ConsoleInputStream();
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		ConsoleOutputStream out = new ConsoleOutputStream(byteOut);
		SshInputHandler handler = new SshInputHandler(input, in, out);
		handler.start();

		// wait for the accept thread to start execution
		try {
			Thread.sleep(WAIT_TIME);
		} catch (InterruptedException ie) {
			// do nothing
		}

		String res = byteOut.toString();
		Assert.assertTrue("Wrong input. Expected abcde, read " + res, res.equals("abcde"));
	}
}
