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
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.launch.EquinoxFactory;
import org.eclipse.osgi.tests.OSGiTest;
import org.osgi.framework.launch.Framework;

/**
 * This tests that when the equinox framework is started with the -console option (with or without port)
 * the console will be actually started. In both test cases a framework is started with the respective
 * option, and the console prompt is expected to be read from the output stream.
 * 
 * The reading from the output stream is performed in a separate thread to ensure that if output is not availbl
 * the test will not block on the reading.
 * 
 */
public class TestEquinoxStartWithConsole extends OSGiTest {
	public static Test suite() {
		return new TestSuite(TestEquinoxStartWithConsole.class);
	}

	private static String CONSOLE_PROMPT = "osgi>";
	boolean isConsolePromptAvailable = false;
	private static final Object waitObject = new Object();
	private static final int TIMEOUT = 1000;

	public void testEquinoxStartWithPort() {
		try {
			Map configuration = new HashMap();
			configuration.put("osgi.console", "55555");
			configuration.put("osgi.configuration.area", "inner");
			EquinoxFactory factory = new EquinoxFactory();
			Framework framework = factory.newFramework(configuration);
			framework.start();
			Socket s = new Socket("localhost", 55555);

			InputStream input = s.getInputStream();
			StringBuffer buffer = new StringBuffer();
			Thread readThread = new Thread(new ReadThread(input, buffer));
			readThread.start();

			try {
				synchronized (waitObject) {
					waitObject.wait(TIMEOUT);
				}
			} catch (InterruptedException ie) {
				// do nothing
			} finally {
				readThread.interrupt();
				input.close();
				s.close();
			}

			framework.stop();
			assertTrue("Console prompt not available", isConsolePromptAvailable);
		} catch (Exception e) {
			fail("Unexpected failure", e);
		}
	}

	public void testEquinoxStartWithoutPort() {
		try {
			Map configuration = new HashMap();
			configuration.put("osgi.console", "");
			configuration.put("osgi.configuration.area", "inner");
			EquinoxFactory factory = new EquinoxFactory();
			Framework framework = factory.newFramework(configuration);

			PrintStream systemOutOriginal = System.out;
			PipedOutputStream out = new PipedOutputStream();
			PrintStream print = new PrintStream(out);
			PipedInputStream input = new PipedInputStream(out);
			System.setOut(print);

			framework.start();

			StringBuffer buffer = new StringBuffer();
			Thread readThread = new Thread(new ReadThread(input, buffer));
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

			System.setOut(systemOutOriginal);

			framework.stop();
			assertTrue("Console prompt not available", isConsolePromptAvailable);
		} catch (Exception e) {
			fail("Unexpected failure", e);
		}
	}

	private class ReadThread implements Runnable {
		private InputStreamReader input;
		private StringBuffer string;

		ReadThread(InputStream stream, StringBuffer buffer) {
			input = new InputStreamReader(stream);
			string = buffer;
		}

		public void run() {
			int c;
			try {
				while ((c = input.read()) != -1) {
					string.append((char) c);
					if (string.toString().indexOf(CONSOLE_PROMPT) > -1) {
						isConsolePromptAvailable = true;
						break;
					}
				}

			} catch (IOException e) {
				//do nothing
			}
		}
	}
}
