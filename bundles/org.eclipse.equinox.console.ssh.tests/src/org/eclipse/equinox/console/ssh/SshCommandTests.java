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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.future.DefaultConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.server.Environment;
import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.eclipse.equinox.console.storage.DigestUtil;
import org.eclipse.equinox.console.storage.SecureUserStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class SshCommandTests {
	private static final int TEST_CONTENT = 100;
	private static final String USER_STORE_FILE_NAME = "org.eclipse.equinox.console.jaas.file";
	private static final String JAAS_CONFIG_FILE_NAME = "jaas.config";
	private static final String JAAS_CONFIG_PROPERTY_NAME = "java.security.auth.login.config";
	private static final String DEFAULT_USER_STORAGE = "osgi.console.ssh.useDefaultSecureStorage";
	private static final String SSH_PORT_PROP_NAME = "osgi.console.ssh";
	private static final String USE_CONFIG_ADMIN_PROP = "osgi.console.useConfigAdmin";
	private static final String STORE_FILE_NAME = SshCommandTests.class.getName() + "_store";
	private static final String GOGO_SHELL_COMMAND = "gosh --login --noshutdown";
	private static final String TRUE = "true";
	private static final String FALSE = "false";
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final String START_COMMAND = "start";
	private static final String STOP_COMMAND = "stop";
	private static final String TERM_PROPERTY = "TERM";
	private static final String XTERM = "XTERM";
	private static final String HOST = "localhost";
	private static final int SSH_PORT = 2222;
	private static final long WAIT_TIME = 5000;

	@Before
	public void init() throws Exception {
		clean();
		initStore();
		initJaasConfigFile();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSshCommand() throws Exception {
		try (CommandSession session = mock(CommandSession.class)) {
			when(session.put(any(String.class), any())).thenReturn(new Object());
			when(session.execute(GOGO_SHELL_COMMAND)).thenReturn(null);

			CommandProcessor processor = mock(CommandProcessor.class);
			when(processor.createSession(any(ConsoleInputStream.class),
					any(PrintStream.class), any(PrintStream.class))).thenReturn(session);
			BundleContext context = mock(BundleContext.class);
			when(context.getProperty(USE_CONFIG_ADMIN_PROP)).thenReturn(FALSE);
			when(context.getProperty(DEFAULT_USER_STORAGE)).thenReturn(TRUE);
			when(context.getProperty(SSH_PORT_PROP_NAME)).thenReturn(Integer.toString(SSH_PORT));
			when(context.registerService(any(String.class), any(),
					any(Dictionary.class))).thenReturn(null);

			Map<String, String> environment = new HashMap<>();
			environment.put(TERM_PROPERTY, XTERM);
			Environment env = mock(Environment.class);
			when(env.getEnv()).thenReturn(environment);

			SshCommand command = new SshCommand(processor, context);
			command.ssh(new String[] { START_COMMAND });

			SshClient client = SshClient.setUpDefaultClient();
			client.start();
			try {
				ConnectFuture connectFuture = client.connect(USERNAME, HOST, SSH_PORT);
				DefaultConnectFuture defaultConnectFuture = (DefaultConnectFuture) connectFuture;

				try {
					Thread.sleep(WAIT_TIME);
				} catch (InterruptedException ie) {
					// do nothing
				}
				try (ClientSession sshSession = defaultConnectFuture.getSession()) {
					sshSession.addPasswordIdentity(PASSWORD);
					ClientChannel channel = sshSession.createChannel("shell");
					channel.setIn(new ByteArrayInputStream((TEST_CONTENT + "\n").getBytes(StandardCharsets.UTF_8)));
					ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
					channel.setOut(byteOut);
					channel.setErr(byteOut);
					channel.open();
					try {
						Thread.sleep(WAIT_TIME);
					} catch (InterruptedException ie) {
						// do nothing
					}
					byte[] output = byteOut.toByteArray();
					Assert.assertEquals("Output not as expected", Integer.toString(TEST_CONTENT),
							new String(output).trim());
				}
			} finally {
				client.stop();
			}

			command.ssh(new String[] { STOP_COMMAND });
		}
	}

	@After
	public void cleanUp() {
		clean();
	}

	private void clean() {
		System.setProperty(USER_STORE_FILE_NAME, "");
		File file = new File(STORE_FILE_NAME);
		if (file.exists()) {
			file.delete();
		}

		System.setProperty(JAAS_CONFIG_PROPERTY_NAME, "");
		File jaasConfFile = new File(JAAS_CONFIG_FILE_NAME);
		if (jaasConfFile.exists()) {
			jaasConfFile.delete();
		}
	}

	private void initStore() throws Exception {
		System.setProperty(USER_STORE_FILE_NAME, STORE_FILE_NAME);
		SecureUserStore.initStorage();
		SecureUserStore.putUser(USERNAME, DigestUtil.encrypt(PASSWORD), null);
	}

	private void initJaasConfigFile() throws Exception {
		System.setProperty(JAAS_CONFIG_PROPERTY_NAME, JAAS_CONFIG_FILE_NAME);
		File jaasConfFile = new File(JAAS_CONFIG_FILE_NAME);
		if (!jaasConfFile.exists()) {
			try (PrintWriter out = new PrintWriter(jaasConfFile);) {
				out.println("equinox_console {");
				out.println("	org.eclipse.equinox.console.jaas.SecureStorageLoginModule REQUIRED;");
				out.println("};");
			}
		}
	}
}
