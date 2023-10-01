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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.junit.Test;

public class TelnetConnectionTests {

	private static final String HOST = "localhost";
	private static final int TEST_CONTENT = 100;
	private static final int IAC = 255;

	@Test
	public void testTelneConnection() throws Exception {

		try (ServerSocket servSocket = new ServerSocket(0);
				Socket socketClient = new Socket(HOST, servSocket.getLocalPort());
				Socket socketServer = servSocket.accept();
				CommandSession session = mock(CommandSession.class)) {

			when(session.execute(any(String.class))).thenReturn(null);

			CommandProcessor processor = mock(CommandProcessor.class);
			when(processor.createSession(any(ConsoleInputStream.class), any(PrintStream.class),
					any(PrintStream.class)))
					.thenReturn(session);

			try (TelnetConnection connection = new TelnetConnection(socketServer, processor, null)) {
				connection.start();

				try (OutputStream outClient = socketClient.getOutputStream()) {
					outClient.write(TEST_CONTENT);
					outClient.write('\n');
					outClient.flush();

					InputStream input = socketServer.getInputStream();
					int in = input.read();
					assertEquals("Server received [" + in + "] instead of " + TEST_CONTENT + " from the telnet client.",
							TEST_CONTENT, in);

					input = socketClient.getInputStream();
					in = input.read();
					// here IAC is expected, since when the output stream in TelnetConsoleSession is
					// created, several telnet
					// commands are written to it, each of them starting with IAC
					assertEquals("Client receive telnet responses from the server unexpected value [" + in
							+ "] instead of " + IAC + ".", IAC, in);
					connection.telnetNegotiationFinished();
					Thread.sleep(5000);
				}
			}
		}
	}
}
