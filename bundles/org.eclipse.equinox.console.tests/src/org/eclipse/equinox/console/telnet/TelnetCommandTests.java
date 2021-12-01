/*******************************************************************************
 * Copyright (c) 2011, 2021 SAP AG and others
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

package org.eclipse.equinox.console.telnet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Dictionary;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class TelnetCommandTests {
	
	private static final int TEST_CONTENT = 100;
	private static final String TELNET_PORT_PROP_NAME = "osgi.console";
	private static final String USE_CONFIG_ADMIN_PROP = "osgi.console.useConfigAdmin";
	private static final String STOP_COMMAND = "stop";
	private static final String HOST = "localhost";
	private static final String FALSE = "false";
	private static final int TELNET_PORT = 2223;
	private static final long WAIT_TIME = 5000;
	
	@Test
	public void testTelnetCommand() throws Exception {
		try (CommandSession session = mock(CommandSession.class)) {
		when(session.execute(any(String.class))).thenReturn(new Object());
		
		CommandProcessor processor = mock(CommandProcessor.class);
		when(processor.createSession(any(ConsoleInputStream.class), any(PrintStream.class), any(PrintStream.class))).thenReturn(session);
		
		BundleContext context = mock(BundleContext.class);
		when(context.getProperty(USE_CONFIG_ADMIN_PROP)).thenReturn(FALSE);
		when(context.getProperty(TELNET_PORT_PROP_NAME)).thenReturn(Integer.toString(TELNET_PORT));
		when(context.registerService(any(String.class), any(), any(Dictionary.class))).thenReturn(null);
		
		TelnetCommand command = new TelnetCommand(processor, context);
		command.startService();
		
		try (Socket socketClient = new Socket(HOST, TELNET_PORT);){
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
		} finally {
			command.telnet(new String[] {STOP_COMMAND});
		}
		}
	}
}
