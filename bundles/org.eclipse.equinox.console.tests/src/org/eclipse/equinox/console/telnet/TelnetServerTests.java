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

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.junit.Test;

public class TelnetServerTests {

	private static final String HOST = "localhost";
	private static final int PORT = 38888;
	private static final long WAIT_TIME = 5000;
	private static final int TEST_CONTENT = 100;

	@Test
	public void testTelnetServer() throws Exception {
		try (CommandSession session = mock(CommandSession.class)) {
			when(session.execute(anyString())).thenReturn(new Object());

			CommandProcessor processor = mock(CommandProcessor.class);
			when(processor.createSession(any(ConsoleInputStream.class), any(PrintStream.class), any(PrintStream.class)))
					.thenReturn(session);

			List<CommandProcessor> processors = new ArrayList<>();
			processors.add(processor);
			TelnetServer telnetServer = new TelnetServer(null, processors, HOST, PORT);
			telnetServer.start();

			try (Socket socketClient = new Socket("localhost", PORT);) {

				OutputStream outClient = socketClient.getOutputStream();
				outClient.write(TEST_CONTENT);
				outClient.write('\n');
				outClient.flush();
				// wait for the accept thread to finish execution
				try {
					Thread.sleep(WAIT_TIME);
				} catch (InterruptedException ie) {
					// do nothing
				}
			} catch (ConnectException e) {
				fail("Telnet port not open");
			} finally {
				telnetServer.stopTelnetServer();
			}
		}
	}

	@Test
	public void testTelnetServerWithoutHost() throws Exception {
		try (CommandSession session = mock(CommandSession.class)) {
			when(session.execute(anyString())).thenReturn(new Object());

			CommandProcessor processor = mock(CommandProcessor.class);
			when(processor.createSession(any(ConsoleInputStream.class), any(PrintStream.class), any(PrintStream.class)))
					.thenReturn(session);

			List<CommandProcessor> processors = new ArrayList<>();
			processors.add(processor);
			TelnetServer telnetServer = new TelnetServer(null, processors, null, PORT);
			telnetServer.start();

			try (Socket socketClient = new Socket("localhost", PORT);) {
				OutputStream outClient = socketClient.getOutputStream();
				outClient.write(TEST_CONTENT);
				outClient.write('\n');
				outClient.flush();

				// wait for the accept thread to finish execution
				try {
					Thread.sleep(WAIT_TIME);
				} catch (InterruptedException ie) {
					// do nothing
				}
			} catch (ConnectException e) {
				fail("Telnet port not open");
			} finally {
				telnetServer.stopTelnetServer();
			}
		}

	}

}
