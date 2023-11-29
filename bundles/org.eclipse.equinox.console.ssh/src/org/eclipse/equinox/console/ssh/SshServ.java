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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.jaas.JaasPasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.eclipse.equinox.console.internal.ssh.AuthorizedKeysFileAuthenticator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * This class configures and start an ssh server
 */
public class SshServ extends Thread {

	private final BundleContext context;
	private final int port;
	private final String host;
	private SshServer sshServer = null;
	private SshShellFactory shellFactory = null;

	private static final String SSH_KEYSTORE_PROP = "ssh.server.keystore";
	private static final String SSH_KEYSTORE_PROP_DEFAULT = "hostkey.ser";
	private static final String SSH_AUTHORIZED_KEYS_FILE_PROP = "ssh.server.authorized_keys";
	private static final String SSH_CUSTOM_PUBLIC_KEY_AUTHENTICATION = "ssh.custom.publickeys.auth";
	private static final String EQUINOX_CONSOLE_DOMAIN = "equinox_console";

	public SshServ(List<CommandProcessor> processors, BundleContext context, String host, int port) {
		this.context = context;
		this.host = host;
		this.port = port;
		shellFactory = new SshShellFactory(processors, context);
	}

	@Override
	public void run() throws RuntimeException {
		sshServer = SshServer.setUpDefaultServer();
		if (host != null) {
			sshServer.setHost(host);
		}
		sshServer.setPort(port);
		sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(
				Paths.get(System.getProperty(SSH_KEYSTORE_PROP, SSH_KEYSTORE_PROP_DEFAULT))));
		sshServer.setShellFactory(shellFactory);
		sshServer.setPasswordAuthenticator(createJaasPasswordAuthenticator());
		sshServer.setPublickeyAuthenticator(createSimpleAuthorizedKeysAuthenticator());
		try {
			sshServer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized void stopSshServer() {
		try {
			sshServer.stop(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized void addCommandProcessor(CommandProcessor processor) {
		shellFactory.addCommandProcessor(processor);
	}

	public synchronized void removeCommandProcessor(CommandProcessor processor) {
		shellFactory.removeCommandProcessor(processor);
	}

	private PasswordAuthenticator createJaasPasswordAuthenticator() {
		JaasPasswordAuthenticator jaasPasswordAuthenticator = new JaasPasswordAuthenticator();
		jaasPasswordAuthenticator.setDomain(EQUINOX_CONSOLE_DOMAIN);
		return jaasPasswordAuthenticator;
	}

	private PublickeyAuthenticator createSimpleAuthorizedKeysAuthenticator() {
		// use authorized keys file if property is set
		final String authorizedKeysFile = System.getProperty(SSH_AUTHORIZED_KEYS_FILE_PROP);
		if (null != authorizedKeysFile) {
			AuthorizedKeysFileAuthenticator authenticator = new AuthorizedKeysFileAuthenticator();
			authenticator.setAuthorizedKeysFile(authorizedKeysFile);
			return authenticator;
		}

		final String customPublicKeysAuthentication = System.getProperty(SSH_CUSTOM_PUBLIC_KEY_AUTHENTICATION);

		// fall back to dynamic provider based on available OSGi services only if
		// explicitly specified
		if ("true".equals(customPublicKeysAuthentication)) {
			return (username, key, session) -> {
				// find available services
				try {
					for (ServiceReference<PublickeyAuthenticator> reference : context
							.getServiceReferences(PublickeyAuthenticator.class, null)) {
						PublickeyAuthenticator authenticator = null;
						try {
							authenticator = context.getService(reference);
							// first positive match wins; continue looking otherwise
							if (authenticator.authenticate(username, key, session))
								return true;
						} finally {
							if (null != authenticator)
								context.ungetService(reference);
						}
					}
				} catch (InvalidSyntaxException e) {
					// no filter is used
				}
				return false;
			};
		}

		return null;
	}
}
