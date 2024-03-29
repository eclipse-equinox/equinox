/*******************************************************************************
 * Copyright (c) 2023, 2024 Eclipse Foundation, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Umair Sair - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.launcher;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Dummy test application used for eclipse launcher testing. JUnit tests
 * launches the eclipse launcher with startup application pointing to the jar file
 * of this application. Test starts a server and set the port number via
 * 'eclipse_test_port' environment variable. This application connects to that
 * port and accepts following queries
 * - -args - returns the arguments passed to this application. Test verify if the
 * arguments are passed to application correctly by the launcher.
 *
 * And accepts following information
 * - -exitdata - The exit data this application should set before exiting
 * - -exitcode - The exit code with which this application should exit
 *
 * @author umairsair
 *
 */
public class TestLauncherApp {

	public static final String ARGS_PARAMETER = "-args"; //$NON-NLS-1$
	public static final String EXITDATA_PARAMETER = "-exitdata"; //$NON-NLS-1$
	public static final String EXITCODE_PARAMETER = "-exitcode"; //$NON-NLS-1$
	public static final String MULTILINE_ARG_VALUE_TERMINATOR = "---"; //$NON-NLS-1$

	public static final String PORT_ENV_KEY = "eclipse_test_port"; //$NON-NLS-1$

	private static JNIBridge bridge;
	private static String sharedId;
	private static String[] args;
	private static List<String> exitData = new ArrayList<>();
	private static int exitCode = 0;


	public int run(String[] args) {
		parseArgs(args);
		return exitCode;
	}

	public static void main(String[] args) {
		parseArgs(args);

		System.exit(exitCode);
	}

	private static void parseArgs(String[] args) {
		TestLauncherApp.args = args;
		for (int i = 0; i < args.length; i++) {
			if ("--launcher.library".equals(args[i])) {
				try {
					bridge = new JNIBridge(args[i + 1]);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if ("-exitdata".equals(args[i])) {
				sharedId = args[i + 1];
			}
		}

		try {
			communicateToServer();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!exitData.isEmpty()) {
			bridge.setExitData(sharedId, String.join("\n", exitData) + "\n");
		}
	}

	private static void communicateToServer() throws Exception {
		String port = System.getenv(PORT_ENV_KEY);
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress("localhost", Integer.parseInt(port)), 10000);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			String line;
			while ((line = in.readLine()) != null) {
				if (ARGS_PARAMETER.equals(line)) {
					out.writeBytes(String.join("\n", args) + "\n" + MULTILINE_ARG_VALUE_TERMINATOR + "\n");
					out.flush();
				} else if (EXITDATA_PARAMETER.equals(line)) {
					while ((line = in.readLine()) != null) {
						if (MULTILINE_ARG_VALUE_TERMINATOR.equals(line)) {
							break;
						}
						exitData.add(line);
					}
				} else if (EXITCODE_PARAMETER.equals(line)) {
					exitCode = Integer.parseInt(in.readLine());
					break;
				}
			}
		}
	}
}
