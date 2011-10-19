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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.eclipse.equinox.console.common.ConsoleOutputStream;
import org.eclipse.equinox.console.common.KEYS;
import org.eclipse.equinox.console.common.terminal.TerminalTypeMappings;
import org.eclipse.equinox.console.storage.SecureUserStore;
import org.eclipse.equinox.console.common.ConsoleInputHandler;
import org.eclipse.equinox.console.common.ConsoleInputScanner;
import org.osgi.framework.BundleContext;

/**
 * This class manages a ssh connection. It is responsible for wrapping the original io streams
 * from the socket, and starting a CommandSession to execute commands from the ssh.
 *
 */
public class SshSession extends Thread implements Closeable {
	private CommandProcessor processor;
	private BundleContext context;
	private SshShell sshShell;
	private InputStream in;
	private OutputStream out;
	private TerminalTypeMappings currentMappings;
	private Map<String, KEYS> currentEscapesToKey;
	
	private static final String PROMPT = "prompt";
    private static final String OSGI_PROMPT = "osgi> ";
    private static final String SCOPE = "SCOPE";
    private static final String EQUINOX_SCOPE = "equinox:*";
    private static final String INPUT_SCANNER = "INPUT_SCANNER";
    private static final String SSH_INPUT_SCANNER = "SSH_INPUT_SCANNER";
    private static final String USER_STORAGE_PROPERTY_NAME = "osgi.console.ssh.useDefaultSecureStorage";
    private static final String DEFAULT_USER = "equinox";
    private static final String CLOSEABLE = "CLOSEABLE";
	private static final int ADD_USER_COUNTER_LIMIT = 2;
	
	public SshSession(CommandProcessor processor, BundleContext context, SshShell sshShell, InputStream in, OutputStream out, TerminalTypeMappings currentMappings, Map<String, KEYS> currentExcapesToKey) {
		this.processor = processor;
		this.context = context;
		this.sshShell = sshShell;
		this.in = in;
		this.out = out;
		this.currentMappings = currentMappings;
		this.currentEscapesToKey = currentExcapesToKey;
	}
	
	public void run() {
		ConsoleInputStream input = new ConsoleInputStream();
		ConsoleOutputStream outp = new ConsoleOutputStream(out);
		SshInputHandler inputHandler = new SshInputHandler(in, input, outp);
		inputHandler.getScanner().setBackspace(currentMappings.getBackspace());
		inputHandler.getScanner().setDel(currentMappings.getDel());
		inputHandler.getScanner().setCurrentEscapesToKey(currentEscapesToKey);
		inputHandler.getScanner().setEscapes(currentMappings.getEscapes());
		inputHandler.start();

		ConsoleInputStream inp = new ConsoleInputStream();
		ConsoleInputHandler consoleInputHandler = new ConsoleInputHandler(input, inp, outp);
		consoleInputHandler.getScanner().setBackspace(currentMappings.getBackspace());
		consoleInputHandler.getScanner().setDel(currentMappings.getDel());
		consoleInputHandler.getScanner().setCurrentEscapesToKey(currentEscapesToKey);
		consoleInputHandler.getScanner().setEscapes(currentMappings.getEscapes());
		((ConsoleInputScanner)consoleInputHandler.getScanner()).setContext(context);
		consoleInputHandler.start();

		final CommandSession session;
		final PrintStream output = new PrintStream(outp);

		session = processor.createSession(inp, output, output);
		session.put(SCOPE, EQUINOX_SCOPE);
		session.put(PROMPT, OSGI_PROMPT);
		session.put(INPUT_SCANNER, consoleInputHandler.getScanner());
		session.put(SSH_INPUT_SCANNER, inputHandler.getScanner());
		// Store this closeable object in the session, so that the disconnect command can close it
		session.put(CLOSEABLE, this);
		((ConsoleInputScanner)consoleInputHandler.getScanner()).setSession(session);

		try {
			if ("true".equals(context.getProperty(USER_STORAGE_PROPERTY_NAME))) {
				String[] names = SecureUserStore.getUserNames();
				for (String name : names) {
					// if the default user is the only user, request creation of a new user and delete the default
					if (DEFAULT_USER.equals(name)) {
						if (names.length == 1) {
							session.getConsole().println("Currently the default user is the only one; since it will be deleted after first login, create a new user:");
							boolean isUserAdded =false;
							int count = 0;
							while (!isUserAdded && count < ADD_USER_COUNTER_LIMIT ){
								isUserAdded = ((Boolean) session.execute("addUser")).booleanValue();
								count++;
							}
							if (!isUserAdded) {
								break;
							}
						}
						if (SecureUserStore.existsUser(name)) {
							SecureUserStore.deleteUser(name);
						}
						break;
					}
				}
			}
			session.execute("gosh --login --noshutdown");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			session.close();
		}

	}
	
	public void close() throws IOException {
		this.interrupt();
		sshShell.removeSession(this);
	}

}
