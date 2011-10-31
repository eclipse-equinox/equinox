/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.internal.core;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.framework.console.ConsoleSession;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ConsoleManager implements ServiceTrackerCustomizer<ConsoleSession, FrameworkConsole> {
	/**
	 * ConsoleSocketGetter - provides a Thread that listens on the port
	 * for FrameworkConsole.
	 */
	class ConsoleSocketGetter implements Runnable {

		/** The ServerSocket to accept connections from */
		private final ServerSocket server;
		private volatile boolean shutdown = false;

		/**
		 * Constructor - sets the server and starts the thread to
		 * listen for connections.
		 *
		 * @param server a ServerSocket to accept connections from
		 */
		ConsoleSocketGetter(ServerSocket server) {
			this.server = server;
			try {
				Method reuseAddress = server.getClass().getMethod("setReuseAddress", new Class[] {boolean.class}); //$NON-NLS-1$
				reuseAddress.invoke(server, new Object[] {Boolean.TRUE});
			} catch (Exception ex) {
				// try to set the socket re-use property, it isn't a problem if it can't be set
			}
			Thread t = new Thread(this, "ConsoleSocketGetter"); //$NON-NLS-1$
			t.setDaemon(false);
			t.start();
		}

		public void run() {
			// Print message containing port console actually bound to..
			System.out.println(NLS.bind(ConsoleMsg.CONSOLE_LISTENING_ON_PORT, server.getInetAddress().toString() + ':' + Integer.toString(server.getLocalPort())));
			while (!shutdown) {
				try {
					Socket socket = server.accept();
					if (socket == null)
						throw new IOException("No socket available.  Probably caused by a shutdown."); //$NON-NLS-1$
					FrameworkConsoleSession session = new FrameworkConsoleSession(socket.getInputStream(), socket.getOutputStream(), socket);
					framework.getSystemBundleContext().registerService(ConsoleSession.class.getName(), session, null);
				} catch (Exception e) {
					if (!shutdown)
						e.printStackTrace();
				}

			}
		}

		public void shutdown() {
			if (shutdown)
				return;
			shutdown = true;
			try {
				server.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static final String PROP_CONSOLE = "osgi.console"; //$NON-NLS-1$
	private static final String PROP_SYSTEM_IN_OUT = "console.systemInOut"; //$NON-NLS-1$
	private static final String CONSOLE_NAME = "OSGi Console"; //$NON-NLS-1$
	public static final String CONSOLE_BUNDLE = "org.eclipse.equinox.console"; //$NON-NLS-1$
	public static final String PROP_CONSOLE_ENABLED = "osgi.console.enable.builtin"; //$NON-NLS-1$
	final Framework framework;
	private final ServiceTracker<CommandProvider, CommandProvider> cpTracker;
	private final ServiceTracker<ConsoleSession, FrameworkConsole> sessions;
	private final String consolePort;
	// Allow for specifying the particular local host address on which the framework to listen for connections. Currently it listens on 
	// all network interfaces of the host and restricting this is desirable from security point of view. See bug 322917.
	private final String consoleHost;
	private FrameworkCommandProvider fwkCommands;
	private ServiceRegistration<?> builtinSession;
	private ConsoleSocketGetter socketGetter;
	private final boolean isEnabled;
	private final String consoleBundle;

	public ConsoleManager(Framework framework, String consolePropValue) {
		String port = null;
		String host = null;
		if (consolePropValue != null) {
			int index = consolePropValue.lastIndexOf(":"); //$NON-NLS-1$
			if (index > -1) {
				host = consolePropValue.substring(0, index);
			}
			port = consolePropValue.substring(index + 1);
		}
		this.framework = framework;
		this.consoleHost = host != null ? host.trim() : host;
		this.consolePort = port != null ? port.trim() : port;
		String enabled = FrameworkProperties.getProperty(PROP_CONSOLE_ENABLED, CONSOLE_BUNDLE);
		if (!"true".equals(enabled) || "none".equals(port)) { //$NON-NLS-1$ //$NON-NLS-2$
			isEnabled = false;
			this.cpTracker = null;
			this.sessions = null;
			this.consoleBundle = "false".equals(enabled) ? CONSOLE_BUNDLE : enabled; //$NON-NLS-1$
			if (consolePort == null || consolePort.length() > 0) {
				// no -console was specified or it has specified none or a port for telnet;
				// need to make sure the gogo shell does not create an interactive console on standard in/out
				FrameworkProperties.setProperty("gosh.args", "--nointeractive"); //$NON-NLS-1$//$NON-NLS-2$
			} else {
				// Need to make sure we don't shutdown the framework if no console is around (bug 362412)
				FrameworkProperties.setProperty("gosh.args", "--noshutdown"); //$NON-NLS-1$//$NON-NLS-2$
			}
			return;
		}
		this.isEnabled = true;
		this.cpTracker = new ServiceTracker<CommandProvider, CommandProvider>(framework.getSystemBundleContext(), CommandProvider.class.getName(), null);
		this.sessions = new ServiceTracker<ConsoleSession, FrameworkConsole>(framework.getSystemBundleContext(), ConsoleSession.class.getName(), this);
		this.consoleBundle = "unknown"; //$NON-NLS-1$
	}

	public static ConsoleManager startConsole(Framework framework) {
		ConsoleManager consoleManager = new ConsoleManager(framework, FrameworkProperties.getProperty(PROP_CONSOLE));
		consoleManager.startConsole();
		return consoleManager;
	}

	private void startConsole() {
		if (!isEnabled) {
			return;
		}

		this.cpTracker.open();
		this.sessions.open();
		fwkCommands = new FrameworkCommandProvider(framework);
		fwkCommands.start();
		if (consolePort == null)
			return;
		int port = -1;
		try {
			if (consolePort.length() > 0)
				port = Integer.parseInt(consolePort);
		} catch (NumberFormatException e) {
			// do nothing;
		}
		if (port < 0) {
			InputStream in = new FilterInputStream(System.in) {
				/**
				 * @throws IOException  
				 */
				public void close() throws IOException {
					// We don't want to close System.in
				}
			};
			OutputStream out = new FilterOutputStream(System.out) {
				/**
				 * @throws IOException  
				 */
				public void close() throws IOException {
					// We don't want to close System.out
				}

				public void write(byte[] var0, int var1, int var2) throws IOException {
					this.out.write(var0, var1, var2);
				}

			};
			FrameworkConsoleSession session = new FrameworkConsoleSession(in, out, null);
			Dictionary<String, Object> props = null;
			props = new Hashtable<String, Object>(1);
			props.put(PROP_SYSTEM_IN_OUT, Boolean.TRUE);
			builtinSession = framework.getSystemBundleContext().registerService(ConsoleSession.class.getName(), session, props);
		} else {
			try {
				if (consoleHost != null) {
					socketGetter = new ConsoleSocketGetter(new ServerSocket(port, 0, InetAddress.getByName(consoleHost)));
				} else {
					socketGetter = new ConsoleManager.ConsoleSocketGetter(new ServerSocket(port));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void checkForConsoleBundle() throws BundleException {
		if (isEnabled)
			return;
		if ("none".equals(consolePort)) //$NON-NLS-1$
			return;
		// otherwise we need to check for the equinox console bundle and start it
		Bundle[] consoles = framework.getBundleBySymbolicName(consoleBundle);
		if (consoles == null || consoles.length == 0) {
			if (consolePort != null)
				throw new BundleException("Could not find bundle: " + consoleBundle, BundleException.UNSUPPORTED_OPERATION); //$NON-NLS-1$
			return;
		}
		try {
			consoles[0].start(Bundle.START_TRANSIENT);
		} catch (BundleException e) {
			throw new BundleException("Could not start bundle: " + consoleBundle, BundleException.UNSUPPORTED_OPERATION, e); //$NON-NLS-1$
		}
	}

	/**
	 *  Stops the OSGi Command console
	 *
	 */
	public void stopConsole() {
		if (!isEnabled) {
			return;
		}
		if (builtinSession != null)
			try {
				builtinSession.unregister();
			} catch (IllegalStateException e) {
				// ignore; this can happen if the session was closed manually (bug 314343)
			}
		sessions.close();
		cpTracker.close();
		if (socketGetter != null)
			socketGetter.shutdown();
		if (fwkCommands != null)
			fwkCommands.stop();
	}

	public FrameworkConsole addingService(ServiceReference<ConsoleSession> reference) {
		FrameworkConsole console = null;

		Boolean isSystemInOut = (Boolean) reference.getProperty(PROP_SYSTEM_IN_OUT);
		if (isSystemInOut == null)
			isSystemInOut = Boolean.FALSE;

		ConsoleSession session = framework.getSystemBundleContext().getService(reference);
		console = new FrameworkConsole(framework.getSystemBundleContext(), session, isSystemInOut.booleanValue(), cpTracker);

		Thread t = new Thread(console, CONSOLE_NAME);
		t.setDaemon(false);
		t.start();
		return console;
	}

	public void modifiedService(ServiceReference<ConsoleSession> reference, FrameworkConsole service) {
		// nothing
	}

	public void removedService(ServiceReference<ConsoleSession> reference, FrameworkConsole service) {
		service.shutdown();
	}
}
