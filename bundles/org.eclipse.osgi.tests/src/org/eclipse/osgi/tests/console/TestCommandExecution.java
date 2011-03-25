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
import org.eclipse.osgi.framework.console.ConsoleSession;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.BundleInstaller;
import org.osgi.framework.*;

/**
 * This tests execution of shell commands. One simple command is tested, and one command, 
 * which itself executes another command. The test tests the whole scenario - installing a bundle, which 
 * provides the commands, registering a ConsoleSession object, through which the commands
 * are sent for execution, and finally checking the output of the commands.
 * 
 * Reading the output of the commands is performed in a separate thread in order to ensure that if no
 * output is available the test will not block on the reading
 * 
 * The echo command just echos its argument
 * The cust_exec executes the command, passed as first argument to it, and attaches a string to its output
 */
public class TestCommandExecution extends CoreTest {
	public static Test suite() {
		return new TestSuite(TestCommandExecution.class);
	}

	private BundleContext context;
	private BundleInstaller installer;
	private ConsoleSession session;
	private InputStream in;
	private OutputStream out;
	private PipedInputStream input;

	private static String BUNDLES_ROOT = "bundle_tests";

	private static String COMMAND_BUNDLE_FILE_NAME = "console.test";
	private static String SIMPLE_COMMAND_STRING = "echo";
	static String SIMPLE_COMMAND_OUTPUT = "Hello!";

	private static String COMPOSITE_COMMAND_STRING = "cust_exec echo Hello!";
	static String COMPOSITE_COMMAND_OUTPUT = "Customized execution of command echo";

	static String SIMPLE = "simple";
	static String COMPOSITE = "composite";

	boolean isOutputOk = false;
	boolean isCompositeContained = false;
	boolean isSimpleContained = false;

	private static final Object waitObject = new Object();
	private static final int TIMEOUT = 1000;

	public void testSimpleCommandExecution() {
		Bundle bundle = prepare(SIMPLE_COMMAND_STRING + " " + SIMPLE_COMMAND_OUTPUT);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		Thread readThread = new Thread(new ReadThread(reader, SIMPLE));
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

		assertTrue("Output not as expected", isOutputOk);
		cleanUp(bundle);
	}

	public void testCompositeCommandExecution() {
		Bundle bundle = prepare(COMPOSITE_COMMAND_STRING);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		Thread readThread = new Thread(new ReadThread(reader, COMPOSITE));
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
		assertTrue("Output not as expected", isCompositeContained && isSimpleContained);
		cleanUp(bundle);
	}

	private Bundle prepare(String command) {
		context = OSGiTestsActivator.getContext();
		try {
			installer = new BundleInstaller(BUNDLES_ROOT, context);
			String location = installer.getBundleLocation(COMMAND_BUNDLE_FILE_NAME);
			Bundle cmdBundle = context.installBundle(location);
			cmdBundle.start();
			out = new PipedOutputStream();
			input = new PipedInputStream((PipedOutputStream) out);
			in = new StringBufferInputStream(command);
			session = new TestConsoleSession(in, out);
			context.registerService(ConsoleSession.class.getName(), session, null);
			return cmdBundle;
		} catch (Exception e) {
			fail("Unexpected failure", e);
		}

		return null;
	}

	private void cleanUp(Bundle bundle) {
		if (bundle != null) {
			try {
				bundle.uninstall();
			} catch (BundleException e) {
				// do nothing
			}
		}
	}

	private class ReadThread implements Runnable {
		BufferedReader reader;
		String type;

		ReadThread(BufferedReader reader, String type) {
			this.reader = reader;
			this.type = type;
		}

		public void run() {
			try {
				if (type.equals(SIMPLE)) {
					checkSimple();
				} else if (type.equals(COMPOSITE)) {
					checkComposite();
				}
			} catch (IOException e) {
				fail("Unexpected failure", e);
			}
		}

		private void checkSimple() throws IOException {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.indexOf(SIMPLE_COMMAND_OUTPUT) > -1) {
					isOutputOk = true;
					break;
				}
			}
		}

		private void checkComposite() throws IOException {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.indexOf(COMPOSITE_COMMAND_OUTPUT) > -1) {
					isCompositeContained = true;
					if ((line = reader.readLine()) != null) {
						if (line.indexOf(SIMPLE_COMMAND_OUTPUT) > -1) {
							isSimpleContained = true;
							break;
						}
					}
				}
			}
		}
	}

}
