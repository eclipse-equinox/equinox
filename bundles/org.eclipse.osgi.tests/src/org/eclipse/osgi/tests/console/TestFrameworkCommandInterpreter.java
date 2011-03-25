/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.console;

import java.io.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.framework.internal.core.*;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class TestFrameworkCommandInterpreter extends CoreTest {
	public static Test suite() {
		return new TestSuite(TestFrameworkCommandInterpreter.class);
	}

	FrameworkCommandInterpreter cmdInterpreter;
	OutputStream out;
	private static String CMD_LINE_NEXT_ARG = "token1 token2 token3";
	private static String CMD_LINE_EXECUTE = "sl 0";
	static String EXPECTED_CMD_OUTPUT = "Bundle 0 Start Level = 0";

	boolean isOutputNull = false;
	boolean isOutputAsExpected = false;

	private static final Object waitObject = new Object();
	private static final int TIMEOUT = 1000;

	/*
	 * Tests the FrameworkCommandInterpreter.nextArgument() method
	 */
	public void testNextArgument() {
		// these are mock implementations of streams, because they are required to create FrameworkCommandInterpreter,
		// but are not actually used
		InputStream inputStr = new InputStream() {

			public int read() throws IOException {
				return 0;
			}
		};

		OutputStream outputStr = new OutputStream() {

			public void write(int b) throws IOException {
				// do nothing
			}
		};

		FrameworkConsoleSession consoleSession = new FrameworkConsoleSession(inputStr, outputStr, null);
		FrameworkConsole console = new FrameworkConsole(null, consoleSession, false, null);
		FrameworkCommandInterpreter frwkCmdInterpreter = new FrameworkCommandInterpreter(CMD_LINE_NEXT_ARG, null, console);
		assertEquals("First token not as expected", "token1", frwkCmdInterpreter.nextArgument());
		assertEquals("Second token not as expected", "token2", frwkCmdInterpreter.nextArgument());
		assertEquals("Third token not as expected", "token3", frwkCmdInterpreter.nextArgument());
	}

	/*
	 * This method tests the execute() method of FrameworkCommandInterpreter class. It passes to the execute method
	 * the "sl" command with parameter the system bundle and expects a particular output.
	 */
	public void testExecute() {
		prepare();
		String command = cmdInterpreter.nextArgument();

		try {
			PipedInputStream input = new PipedInputStream((PipedOutputStream) out);
			cmdInterpreter.execute(command);
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));

			// The output is read in another thread in order to insure that if for some reason there is no output,
			// the test will not block on the reading
			Thread readThread = new Thread(new ReadThread(reader));
			readThread.start();

			try {
				synchronized (waitObject) {
					waitObject.wait(TIMEOUT);
				}
			} catch (InterruptedException ie) {
				// do nothing
			} finally {
				readThread.interrupt();
			}

			assertFalse("No output from the command", isOutputNull);
			assertTrue("Output of the command not as expected", isOutputAsExpected);
		} catch (IOException e) {
			fail("Unexpected failure", e);
		}

	}

	private void prepare() {
		InputStream in = new InputStream() {

			public int read() throws IOException {
				return 0;
			}
		};

		out = new PipedOutputStream();
		FrameworkConsoleSession session = new FrameworkConsoleSession(in, out, null);
		FrameworkConsole console = new FrameworkConsole(null, session, false, null);
		BundleContext context = OSGiTestsActivator.getContext();
		ServiceTracker cptracker = new ServiceTracker(context, CommandProvider.class.getName(), null);
		cptracker.open();
		ServiceReference[] refs = cptracker.getServiceReferences();
		CommandProvider[] commandProviders = new CommandProvider[refs.length];
		for (int i = 0; i < refs.length; i++) {
			commandProviders[i] = (CommandProvider) context.getService(refs[i]);
		}
		cmdInterpreter = new FrameworkCommandInterpreter(CMD_LINE_EXECUTE, commandProviders, console);
	}

	private class ReadThread implements Runnable {
		BufferedReader reader;

		ReadThread(BufferedReader reader) {
			this.reader = reader;
		}

		public void run() {
			try {
				String line;
				line = reader.readLine();
				if (line == null) {
					isOutputNull = true;
					return;
				}
				isOutputAsExpected = line.indexOf(EXPECTED_CMD_OUTPUT) > -1;
			} catch (IOException e) {
				fail("Unexpected failure", e);
			}
		}
	}
}
