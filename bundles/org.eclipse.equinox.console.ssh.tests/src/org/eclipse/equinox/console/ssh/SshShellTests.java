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

package org.eclipse.equinox.console.ssh;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.channel.ChannelSession;
import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.eclipse.equinox.console.storage.DigestUtil;
import org.eclipse.equinox.console.storage.SecureUserStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class SshShellTests {

	private static final int TEST_CONTENT = 100;
	private static final String USER_STORE_FILE_NAME = "org.eclipse.equinox.console.jaas.file";
	private static final String DEFAULT_USER_STORAGE = "osgi.console.ssh.useDefaultSecureStorage";
	private static final String USER_STORE_NAME = SshShellTests.class.getName() + "_store";
	private static final String HOST = "localhost";
	private static final String GOGO_SHELL_COMMAND = "gosh --login --noshutdown";
	private static final String TERM_PROPERTY = "TERM";
	private static final String XTERM = "XTERM";
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final String TRUE = "true";

	@Before
	public void init() throws Exception {
		clean();
		initStore();
	}

	@Test
	public void testSshConnection() throws Exception {
		SshShell shell = null;

		try (ServerSocket servSocket = new ServerSocket(0);
				Socket socketClient = new Socket(HOST, servSocket.getLocalPort());
				Socket socketServer = servSocket.accept()) {
			try (CommandSession session = mock(CommandSession.class)) {
				when(session.put(any(String.class), any())).thenReturn(any());
				when(session.execute(GOGO_SHELL_COMMAND)).thenReturn(null);
				CommandProcessor processor = mock(CommandProcessor.class);
				when(processor.createSession(any(ConsoleInputStream.class), any(PrintStream.class),
						any(PrintStream.class))).thenReturn(session);

				BundleContext context = mock(BundleContext.class);
				when(context.getProperty(DEFAULT_USER_STORAGE)).thenReturn(TRUE);

				Map<String, String> environment = new HashMap<>();
				environment.put(TERM_PROPERTY, XTERM);
				Environment env = mock(Environment.class);
				when(env.getEnv()).thenReturn(environment);

				List<CommandProcessor> processors = new ArrayList<>();
				processors.add(processor);
				shell = new SshShell(processors, context);
				shell.setInputStream(socketServer.getInputStream());
				shell.setOutputStream(socketServer.getOutputStream());
				shell.start(new ChannelSession(), env);
			}

			try (OutputStream outClient = socketClient.getOutputStream()) {
				outClient.write(TEST_CONTENT);
				outClient.write('\n');
				outClient.flush();
				try (InputStream input = socketClient.getInputStream()) {
					int in = input.read();
					Assert.assertEquals(
							"Server received [" + in + "] instead of " + TEST_CONTENT + " from the ssh client.",
							TEST_CONTENT, in);
				}
			}
		}
	}

	@After
	public void cleanUp() {
		clean();
	}

	private void initStore() throws Exception {
		System.setProperty(USER_STORE_FILE_NAME, USER_STORE_NAME);
		SecureUserStore.initStorage();
		SecureUserStore.putUser(USERNAME, DigestUtil.encrypt(PASSWORD), null);
	}

	private void clean() {
		System.setProperty(USER_STORE_FILE_NAME, "");
		File file = new File(USER_STORE_NAME);
		if (file.exists()) {
			file.delete();
		}
	}
}
