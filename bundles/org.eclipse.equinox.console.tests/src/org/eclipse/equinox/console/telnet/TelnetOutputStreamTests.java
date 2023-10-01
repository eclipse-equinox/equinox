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

package org.eclipse.equinox.console.telnet;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

public class TelnetOutputStreamTests {

	@Test
	public void testAutoSend() throws Exception {
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		try (TelnetOutputStream out = new TelnetOutputStream(byteOut)) {
			out.autoSend();
			out.flush();
			byte[] message = byteOut.toByteArray();

			Assert.assertNotNull("Auto message not sent", message);
			Assert.assertFalse("Auto message not sent", message.length == 0);
			Assert.assertTrue(
					"Error sending auto message. Expected length: " + TelnetOutputStream.autoMessage.length
							+ ", actual length: " + message.length,
					message.length == TelnetOutputStream.autoMessage.length);

			for (int i = 0; i < message.length; i++) {
				Assert.assertEquals(
						"Wrong char in auto message. Position: " + i + ", expected: "
								+ TelnetOutputStream.autoMessage[i] + ", read: " + message[i],
						TelnetOutputStream.autoMessage[i], message[i]);
			}
		}
	}
}
