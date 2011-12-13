/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.ssh;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import org.eclipse.equinox.console.internal.ssh.AuthorizedKeys;

import org.osgi.framework.BundleContext;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.jaas.JaasPasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;

/**
 *  This class configures and start an ssh server
 *
 */
public class SshServ extends Thread {
	private int port;
	private String host;
	private SshServer sshServer = null;
	private SshShellFactory shellFactory = null;

	private static final String SSH_KEYSTORE_PROP = "ssh.server.keystore";
	private static final String SSH_KEYSTORE_PROP_DEFAULT = "hostkey.ser";
	private static final String SSH_AUTHORIZED_KEYS_FILE_PROP = "ssh.server.authorized_keys";
	private static final String EQUINOX_CONSOLE_DOMAIN = "equinox_console";

    public SshServ(List<CommandProcessor> processors, BundleContext context, String host, int port) {
    	this.host = host;
    	this.port = port;
    	shellFactory = new SshShellFactory(processors, context);
    }

    public void run() throws RuntimeException {
    	sshServer = SshServer.setUpDefaultServer();
		if (host != null) {
			sshServer.setHost(host);
		}
    	sshServer.setPort(port);
    	sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(System.getProperty(SSH_KEYSTORE_PROP, SSH_KEYSTORE_PROP_DEFAULT)));
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
		} catch (InterruptedException e) {
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
		// check if property is set
		final String authorizedKeysFile = System.getProperty(SSH_AUTHORIZED_KEYS_FILE_PROP);
		if (null == authorizedKeysFile)
			return null;

		// dynamically read key file at each login attempt
		return new PublickeyAuthenticator() {
			public boolean authenticate(String username, PublicKey key, ServerSession session) {
				try {
					AuthorizedKeys keys = new AuthorizedKeys(authorizedKeysFile);
					for (PublicKey authorizedKey : keys.getKeys()) {
						if (isSameKey(authorizedKey, key)) {
							return true;
						}
					}
				} catch (FileNotFoundException e) {
					System.err.println("Configured authorized_keys file not found! " + e.getMessage());
				} catch (IOException e) {
					System.err.println("Please check authorized_keys file! " + e.getMessage());
				}
				return false;
			}

			private boolean isSameKey(PublicKey k1, PublicKey k2) throws IOException {
				if ((k1 instanceof DSAPublicKey) && (k2 instanceof DSAPublicKey)) {
					return isSameDSAKey((DSAPublicKey) k1, (DSAPublicKey) k2);
				} else if ((k1 instanceof RSAPublicKey) && (k2 instanceof RSAPublicKey)) {
					return isSameRSAKey((RSAPublicKey) k1, (RSAPublicKey) k2);
				} else {
					throw new IOException("Unsupported key types detected!");
				}
			}

			private boolean isSameRSAKey(RSAPublicKey k1, RSAPublicKey k2) {
				return k1.getPublicExponent().equals(k2.getPublicExponent()) && k1.getModulus().equals(k2.getModulus());
			}

			private boolean isSameDSAKey(DSAPublicKey k1, DSAPublicKey k2) {
				return k1.getY().equals(k2.getY()) && k1.getParams().getG().equals(k2.getParams().getG()) && k1.getParams().getP().equals(k2.getParams().getP()) && k1.getParams().getQ().equals(k2.getParams().getQ());
			}
		};
    }
}
