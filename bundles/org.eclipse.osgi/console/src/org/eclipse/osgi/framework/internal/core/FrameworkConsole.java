/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
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
import org.eclipse.osgi.framework.console.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This class starts OSGi with a console for development use.
 *
 * FrameworkConsole provides a printStackTrace method to print Exceptions and their
 * nested Exceptions.
 */
public class FrameworkConsole implements Runnable {
	/** The stream to receive commands on  */
	private final BufferedReader in;
	/** The stream to write command results to */
	private final PrintWriter out;
	/** The current bundle context */
	private final BundleContext context;
	/** A tracker containing the service object of all registered command providers */
	private final ServiceTracker<CommandProvider, CommandProvider> cptracker;
	private final ConsoleSession consoleSession;
	private final boolean isSystemInOut;
	/** Default code page which must be supported by all JVMs */
	static final String defaultEncoding = "iso8859-1"; //$NON-NLS-1$
	/** The current setting for code page */
	static final String encoding = FrameworkProperties.getProperty("osgi.console.encoding", FrameworkProperties.getProperty("file.encoding", defaultEncoding)); //$NON-NLS-1$ //$NON-NLS-2$
	private static final boolean blockOnready = FrameworkProperties.getProperty("osgi.dev") != null || FrameworkProperties.getProperty("osgi.console.blockOnReady") != null; //$NON-NLS-1$ //$NON-NLS-2$
	volatile boolean shutdown = false;

	public FrameworkConsole(BundleContext context, ConsoleSession consoleSession, boolean isSystemInOut, ServiceTracker<CommandProvider, CommandProvider> cptracker) {
		this.context = context;
		this.cptracker = cptracker;
		this.isSystemInOut = isSystemInOut;
		this.consoleSession = consoleSession;
		in = createBufferedReader(consoleSession.getInput());
		out = createPrintWriter(consoleSession.getOutput());
	}

	/**
	 * Return a BufferedReader from an InputStream.  Handle encoding.
	 *
	 * @param _in An InputStream to wrap with a BufferedReader
	 * @return a BufferedReader
	 */
	static BufferedReader createBufferedReader(InputStream _in) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(_in, encoding));
		} catch (UnsupportedEncodingException uee) {
			// if the encoding is not supported by the jvm, punt and use whatever encodiing there is
			reader = new BufferedReader(new InputStreamReader(_in));
		}
		return reader;
	}

	/**
	 * Return a PrintWriter from an OutputStream.  Handle encoding.
	 *
	 * @param _out An OutputStream to wrap with a PrintWriter
	 * @return a PrintWriter
	 */
	static PrintWriter createPrintWriter(OutputStream _out) {
		PrintWriter writer;
		try {
			writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(_out, encoding)), true);
		} catch (UnsupportedEncodingException uee) {
			// if the encoding is not supported by the jvm, punt and use whatever encoding there is
			writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(_out)), true);
		}
		return writer;
	}

	/**
	 *  Return the current output PrintWriter
	 * @return The currently active PrintWriter
	 */
	public PrintWriter getWriter() {
		return out;
	}

	/**
	 * Command Line Interface for OSGi. The method processes the initial commands
	 * and then reads and processes commands from the console InputStream.
	 * Command output is written to the console PrintStream. The method will
	 * loop reading commands from the console InputStream until end-of-file
	 * is reached. This method will then return.
	 */
	public void run() {
		try {
			runConsole();
		} finally {
			// ensure the console is shutdown before exiting the thread
			shutdown();
		}
	}

	private void runConsole() {
		// wait to receive commands from console and handle them
		//cache the console prompt String
		String consolePrompt = "\r\n" + ConsoleMsg.CONSOLE_PROMPT; //$NON-NLS-1$
		while (!shutdown) {
			out.print(consolePrompt);
			out.flush();

			String cmdline = null;
			try {
				if (blockOnready && isSystemInOut) {
					// bug 40066: avoid waiting on input stream - apparently generates contention with other native calls 
					try {
						while (!in.ready())
							Thread.sleep(300);
						cmdline = in.readLine();
					} catch (InterruptedException e) {
						// do nothing; probably got disconnected
					}
				} else
					cmdline = in.readLine();
			} catch (IOException ioe) {
				if (!shutdown)
					ioe.printStackTrace(out);
			}
			if (cmdline == null)
				// we assume the session is done and break out of the loop.
				break;
			if (!shutdown)
				docommand(cmdline);
		}
	}

	/**
	 *  Process the args on the command line.
	 *  This method invokes a CommandInterpreter to do the actual work.
	 *
	 *  @param cmdline a string containing the command line arguments
	 */
	protected void docommand(String cmdline) {
		if (cmdline != null && cmdline.length() > 0) {
			CommandInterpreter intcp = new FrameworkCommandInterpreter(cmdline, getServices(), this);
			String command = intcp.nextArgument();
			if (command != null) {
				intcp.execute(command);
			}
		}
	}

	/**
	 * Reads a string from standard input until user hits the Enter key.
	 *
	 * @return	The string read from the standard input without the newline character.
	 */
	public String getInput() {
		String input;
		try {
			/** The buffered input reader on standard in. */
			input = in.readLine();
			System.out.println("<" + input + ">"); //$NON-NLS-1$//$NON-NLS-2$
		} catch (IOException e) {
			input = ""; //$NON-NLS-1$
		}
		return input;
	}

	/**
	 * Return an array of service objects for all services
	 * being tracked by this <tt>ServiceTracker</tt> object.
	 *
	 * The array is sorted primarily by descending Service Ranking and
	 * secondarily by ascending Service ID.
	 *
	 * @return Array of service objects; if no service
	 * are being tracked then an empty array is returned
	 */
	public CommandProvider[] getServices() {
		ServiceReference<CommandProvider>[] serviceRefs = cptracker.getServiceReferences();
		if (serviceRefs == null)
			return new CommandProvider[0];
		Util.dsort(serviceRefs, 0, serviceRefs.length);

		CommandProvider[] serviceObjects = new CommandProvider[serviceRefs.length];
		for (int i = 0; i < serviceRefs.length; i++)
			serviceObjects[i] = FrameworkConsole.this.context.getService(serviceRefs[i]);
		return serviceObjects;
	}

	/**
	 * Stops the console so the thread can be GC'ed
	 */
	public synchronized void shutdown() {
		if (shutdown)
			return;
		shutdown = true;
		consoleSession.close();
	}

}
