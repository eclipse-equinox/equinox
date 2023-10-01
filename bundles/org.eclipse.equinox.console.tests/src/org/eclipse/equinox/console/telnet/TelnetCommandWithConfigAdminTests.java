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
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.console.telnet;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

public class TelnetCommandWithConfigAdminTests {
	private static final int TEST_CONTENT = 100;
	private static final String STOP_COMMAND = "stop";
	private static final String HOST = "localhost";
	private static final String TELNET_PORT = "2223";
	private static final long WAIT_TIME = 5000;
	private static final String USE_CONFIG_ADMIN_PROP = "osgi.console.useConfigAdmin";
	private static final String TRUE = "true";
	private static final String FALSE = "false";
	private ManagedService configurator;

	@Test
	public void testTelnetCommandWithConfigAdminEnabledTelnet() throws Exception {
		CommandSession session = mock(CommandSession.class);
		when(session.execute(any(String.class))).thenReturn(new Object());

		CommandProcessor processor = mock(CommandProcessor.class);
		when(processor.createSession(any(ConsoleInputStream.class), any(PrintStream.class), any(PrintStream.class)))
				.thenReturn(session);

		final ServiceRegistration<?> registration = mock(ServiceRegistration.class);

		BundleContext context = mock(BundleContext.class);
		when(context.getProperty(USE_CONFIG_ADMIN_PROP)).thenReturn(TRUE);
		when(context.registerService(any(String.class), any(ManagedService.class), any(Dictionary.class)))
				.thenAnswer(invocation -> {
					configurator = (ManagedService) invocation.getArguments()[1];
					return registration;
				});
		when(context.registerService(any(String.class), any(TelnetCommand.class), any(Dictionary.class)))
				.thenReturn(null);

		TelnetCommand command = new TelnetCommand(processor, context);
		command.startService();
		Dictionary<String, String> props = new Hashtable<>();
		props.put("port", TELNET_PORT);
		props.put("host", HOST);
		props.put("enabled", TRUE);
		configurator.updated(props);

		try (Socket socketClient = new Socket(HOST, Integer.parseInt(TELNET_PORT));) {
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
			command.telnet(new String[] { STOP_COMMAND });
		}
	}

	@Test
	public void testTelnetCommandWithConfigAdminDisabledTelnet() throws Exception {
		disabledTelnet(false);
	}

	@Test
	public void testTelnetCommandWithConfigAdminDisabledTelnetByDefault() throws Exception {
		disabledTelnet(true);
	}

	private void disabledTelnet(boolean isDefault) throws Exception {
		CommandSession session = mock(CommandSession.class);
		when(session.execute(any(String.class))).thenReturn(new Object());

		CommandProcessor processor = mock(CommandProcessor.class);
		when(processor.createSession(any(ConsoleInputStream.class), any(PrintStream.class), any(PrintStream.class)))
				.thenReturn(session);

		final ServiceRegistration<?> registration = mock(ServiceRegistration.class);

		BundleContext context = mock(BundleContext.class);
		when(context.getProperty(USE_CONFIG_ADMIN_PROP)).thenReturn(TRUE);
		when(context.registerService(any(String.class), any(ManagedService.class), any(Dictionary.class)))
				.thenAnswer(invocation -> {
					configurator = (ManagedService) invocation.getArguments()[1];
					return registration;
				});
		when(context.registerService(any(String.class), any(TelnetCommand.class), any(Dictionary.class)))
				.thenReturn(null);

		TelnetCommand command = new TelnetCommand(processor, context);
		command.startService();
		Dictionary<String, String> props = new Hashtable<>();
		props.put("port", TELNET_PORT);
		props.put("host", HOST);
		if (isDefault == false) {
			props.put("enabled", FALSE);
		}
		configurator.updated(props);

		try (Socket socketClient = new Socket(HOST, Integer.parseInt(TELNET_PORT))) {

			fail("It should not be possible to open a socket to " + HOST + ":" + TELNET_PORT);
		} catch (IOException e) {
			// this is ok, there should be an exception
		} finally {
			try {
				command.telnet(new String[] { STOP_COMMAND });
			} catch (IllegalStateException e) {
				// this is expected
			}
		}
	}

}
