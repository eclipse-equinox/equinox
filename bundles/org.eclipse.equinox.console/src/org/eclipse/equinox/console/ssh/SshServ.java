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

import java.io.IOException;
import java.util.List;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.jaas.JaasPasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.osgi.framework.BundleContext;

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
	private static final String EQUINOX_CONSOLE_DOMAIN = "equinox_console";
	
    public SshServ(List<CommandProcessor> processors, BundleContext context, String host, int port) {
    	this.host = host;
    	this.port = port;
    	shellFactory = new SshShellFactory(processors, context);
    }
    
    public void run() throws RuntimeException {
    	try {
			sshServer = SshServer.setUpDefaultServer();
		} catch (NoClassDefFoundError e1) {
			System.out.println("SSH bundles not available! If you want to use SSH, please install Apache sshd-core, Apache mina-core, slf4j-api and a slf4j logger implementation bundles");
			throw new RuntimeException("SSH bundles not available");
		}
		if (host != null) {
			sshServer.setHost(host);
		}
    	sshServer.setPort(port);
    	sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(System.getProperty(SSH_KEYSTORE_PROP, SSH_KEYSTORE_PROP_DEFAULT)));
    	sshServer.setShellFactory(shellFactory);
    	sshServer.setPasswordAuthenticator(createJaasPasswordAuthenticator());
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
}
