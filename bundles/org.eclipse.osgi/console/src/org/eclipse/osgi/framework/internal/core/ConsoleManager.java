/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.framework.console.ConsoleSession;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ConsoleManager implements ServiceTrackerCustomizer {
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
			t.setDaemon(true);
			t.start();
		}

		public void run() {
			// Print message containing port console actually bound to..
			System.out.println(NLS.bind(ConsoleMsg.CONSOLE_LISTENING_ON_PORT, Integer.toString(server.getLocalPort())));
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
	private final Framework framework;
	private final ServiceTracker cpTracker;
	private final ServiceTracker sessions;
	private final String consolePort;
	private FrameworkCommandProvider fwkCommands;
	private ServiceRegistration builtinSession;
	private ConsoleSocketGetter scsg;

	public ConsoleManager(Framework framework, String consolePort) {
		this.framework = framework;
		this.consolePort = consolePort != null ? consolePort.trim() : consolePort;
		this.cpTracker = new ServiceTracker(framework.getSystemBundleContext(), CommandProvider.class.getName(), null);
		this.sessions = new ServiceTracker(framework.getSystemBundleContext(), ConsoleSession.class.getName(), this);
	}

	public static ConsoleManager startConsole(Framework framework) {
		ConsoleManager consoleManager = new ConsoleManager(framework, FrameworkProperties.getProperty(PROP_CONSOLE));
		consoleManager.startConsole();
		return consoleManager;
	}

	private void startConsole() {
		if ("none".equals(consolePort)) //$NON-NLS-1$
			return; // disables all console sessions
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
				public void close() throws IOException {
					// We don't want to close System.in
				}
			};
			OutputStream out = new FilterOutputStream(System.out) {
				public void close() throws IOException {
					// We don't want to close System.out
				}

				public void write(byte[] var0, int var1, int var2) throws IOException {
					this.out.write(var0, var1, var2);
				}

			};
			FrameworkConsoleSession session = new FrameworkConsoleSession(in, out, null);
			Hashtable props = null;
			props = new Hashtable(1);
			props.put(PROP_SYSTEM_IN_OUT, Boolean.TRUE);
			builtinSession = framework.getSystemBundleContext().registerService(ConsoleSession.class.getName(), session, props);
		} else {
			try {
				scsg = new ConsoleManager.ConsoleSocketGetter(new ServerSocket(port));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 *  Stops the OSGi Command console
	 *
	 */
	public void stopConsole() {
		if (builtinSession != null)
			try {
				builtinSession.unregister();
			} catch (IllegalStateException e) {
				// ignore; this can happen if the session was closed manually (bug 314343)
			}
		sessions.close();
		cpTracker.close();
		if (scsg != null)
			scsg.shutdown();
		if (fwkCommands != null)
			fwkCommands.stop();
	}

	public Object addingService(ServiceReference reference) {
		FrameworkConsole console = null;

		Boolean isSystemInOut = (Boolean) reference.getProperty(PROP_SYSTEM_IN_OUT);
		if (isSystemInOut == null)
			isSystemInOut = Boolean.FALSE;

		ConsoleSession session = (ConsoleSession) framework.getSystemBundleContext().getService(reference);
		console = new FrameworkConsole(framework.getSystemBundleContext(), session, isSystemInOut.booleanValue(), cpTracker);

		Thread t = new Thread(console, CONSOLE_NAME);
		t.setDaemon(false);
		t.start();
		return console;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// nothing
	}

	public void removedService(ServiceReference reference, Object service) {
		((FrameworkConsole) service).shutdown();
	}
}
