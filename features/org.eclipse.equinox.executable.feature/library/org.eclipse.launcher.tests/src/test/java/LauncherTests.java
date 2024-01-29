/*******************************************************************************
 * Copyright (c) 2023 Eclipse Foundation, Inc. and others.
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
package test.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import main.TestLauncherConstants;

public class LauncherTests {
	private static final String ECLIPSE_INI_PATH_KEY = "ECLIPSE_INI_PATH";
	// eclipse ini file name is relative to eclipse binary. e.g., in mac, it is ../Eclipse/eclipse.ini
	// and on other hosts, it is present in same directory as eclipse binary
	private static final String ECLIPSE_INI_FILE_NAME = System.getProperty(ECLIPSE_INI_PATH_KEY,
			System.getenv(ECLIPSE_INI_PATH_KEY) == null ? "eclipse.ini" : System.getenv(ECLIPSE_INI_PATH_KEY));
	// @formatter:off
	private static final String DEFAULT_ECLIPSE_INI_CONTENT = "-startup\n"
															+ "../test.launcher.jar\n"
															+ "--launcher.library\n"
															+ "plugins/org.eclipse.equinox.launcher\n"
															+ "-vmargs\n"
															+ "-Xms40m\n"
															+ "";
	// @formatter:on
	public static final Integer EXIT_OK = Integer.valueOf(0);
	public static final Integer EXIT_RESTART = Integer.valueOf(23);
	public static final Integer EXIT_RELAUNCH = Integer.valueOf(24);

	private ServerSocket server;

	@BeforeEach
	void setUp() throws IOException {
		server = new ServerSocket(0, 1);
		server.setSoTimeout(10000);
	}

	@AfterEach
	void tearDown() throws IOException {
		if (server != null) {
			server.close();
			server = null;
		}
	}

	@Test
	void test_appTerminatesWithCodeZeroOnExit() throws IOException, InterruptedException {
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT);

		Process launcherProcess = startEclipseLauncher(Collections.emptyList());

		Socket socket = server.accept();

		List<String> appArgs = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs, null, EXIT_OK);

		// Make sure arguments contain default vmargs
		assertTrue(appArgs.containsAll(Arrays.asList("-vmargs", "-Xms40m")));
		// Make sure launcher exited with code zero
		launcherProcess.waitFor(5, TimeUnit.SECONDS);
		assertTrue(launcherProcess.exitValue() == 0);
	}

	@Test
	void test_eclipseIniChangesShouldBePickedOnRestart() throws IOException {
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT);

		startEclipseLauncher(Collections.emptyList());

		Socket socket = server.accept();

		// Before restarting, update eclipse.ini and check if extra arg is read
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT + "-Dtest");

		List<String> appArgs1 = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs1, null, EXIT_RESTART);

		socket = server.accept();

		List<String> appArgs2 = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs2, null, EXIT_OK);

		// Args after restart contains new argument in eclipse.ini
		assertTrue(appArgs2.contains("-Dtest"));

		// Other than exitdata arg, all other args should be same over restarts
		appArgs1.remove(appArgs1.indexOf(TestLauncherConstants.EXITDATA_PARAMETER) + 1);
		appArgs2.remove(appArgs2.indexOf(TestLauncherConstants.EXITDATA_PARAMETER) + 1);
		// After restart, only -Dtest arg should be extra, other than that args should
		// be same
		appArgs2.remove(appArgs2.indexOf("-Dtest"));

		// Convert backslashes to forward slashes before comparison so that all paths are consistent
		assertEquals(appArgs1.stream().map(s -> s.replace('\\', '/')).collect(Collectors.toList()),
				appArgs2.stream().map(s -> s.replace('\\', '/')).collect(Collectors.toList()));
	}

	@Test
	void test_eclipseIniChangesShouldBePickedOnRelaunch() throws IOException {
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT);

		startEclipseLauncher(Collections.emptyList());

		Socket socket = server.accept();

		// Before relaunching, update eclipse.ini and check if extra arg is read
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT + "-Dtest");

		List<String> appArgs1 = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs1, "-data\ndir1", EXIT_RELAUNCH);

		socket = server.accept();

		List<String> appArgs2 = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs2, null, EXIT_OK);

		// Args after relaunch contains new argument in eclipse.ini
		assertTrue(appArgs2.contains("-Dtest"));

		// Other than exitdata arg, all other args should be same over relaunches
		appArgs1.remove(appArgs1.indexOf(TestLauncherConstants.EXITDATA_PARAMETER) + 1);
		appArgs2.remove(appArgs2.indexOf(TestLauncherConstants.EXITDATA_PARAMETER) + 1);
		// After relaunch, -Dtest and -data args should be extra, other than that args should
		// be same
		appArgs2.remove(appArgs2.indexOf("-Dtest"));
		appArgs2.remove(appArgs2.indexOf("-data"));
		appArgs2.remove(appArgs2.indexOf("dir1"));

		// Convert backslashes to forward slashes before comparison so that all paths are consistent
		assertEquals(appArgs1.stream().map(s -> s.replace('\\', '/')).collect(Collectors.toList()),
				appArgs2.stream().map(s -> s.replace('\\', '/')).collect(Collectors.toList()));
	}

	@Test
	void test_newNonVMArgsForRelaunchShouldBeEffective() throws IOException {
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT);

		startEclipseLauncher(Collections.emptyList());

		Socket socket = server.accept();

		List<String> appArgs1 = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs1, "-data\ndir1", EXIT_RELAUNCH);

		socket = server.accept();

		List<String> appArgs2 = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs2, null, EXIT_OK);

		// Make sure on relaunch, new args is provided
		assertTrue(appArgs2.contains("-data"));
		assertTrue(appArgs2.contains("dir1"));
	}

	@Test
	void test_newNonVMArgsForRelaunchShouldOverrideOlderSameArg() throws IOException {
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT);

		// Start eclipse with arguments '-data dir1'
		startEclipseLauncher(List.of("-data", "dir1"));

		Socket socket = server.accept();

		List<String> appArgs1 = new ArrayList<>();
		// Relaunch should provide arguments '-data dir2'
		analyzeLaunchedTestApp(socket, appArgs1, "-data\ndir2", EXIT_RELAUNCH);

		socket = server.accept();

		List<String> appArgs2 = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs2, null, EXIT_OK);

		// Relaunched app should contain '-data dir1 -data dir2'
		assertEquals(Collections.frequency(appArgs2, "-data"), 2);
		assertTrue(appArgs2.contains("dir1"));
		assertTrue(appArgs2.contains("dir2"));
		// dir2 argument should appear later so that it gets effective
		assertTrue(appArgs2.indexOf("dir2") > appArgs2.indexOf("dir1"));
	}

	@Test
	void test_newNonVMArgsForRelaunchWithSkipOldUserArgs() throws IOException {
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT);

		// Start eclipse with arguments '-data dir1'
		startEclipseLauncher(List.of("-data", "dir1"));

		Socket socket = server.accept();

		List<String> appArgs1 = new ArrayList<>();
		// Relaunch should provide arguments '-data dir2' and
		// '--launcher.skipOldUserArgs' so that 'data dir1 is not
		// provided on relaunch
		analyzeLaunchedTestApp(socket, appArgs1, "-data\ndir2\n--launcher.skipOldUserArgs", EXIT_RELAUNCH);

		socket = server.accept();

		List<String> appArgs2 = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs2, null, EXIT_OK);

		assertTrue(appArgs1.contains("-data"));
		assertTrue(appArgs1.contains("dir1"));

		// -data argument should be once
		assertEquals(1, Collections.frequency(appArgs2, "-data"));
		// -data argument should exist with only value dir2
		assertTrue(appArgs2.contains("dir2"));
		assertFalse(appArgs2.contains("dir1"));
	}

	@Test
	void test_newVMArgsForRelaunchShouldBeEffective() throws IOException {
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT);

		// Start eclipse with arguments '-vmargs -Dtest=1'. Note that by default
		// --launcher.overrideVmargs is set
		startEclipseLauncher(List.of("-vmargs", "-Dtest=1"));

		Socket socket = server.accept();

		List<String> appArgs1 = new ArrayList<>();
		// Relaunch should provide arguments '-vmargs -Dtest=2'
		analyzeLaunchedTestApp(socket, appArgs1, "-vmargs\n-Dtest=2", EXIT_RELAUNCH);

		socket = server.accept();

		List<String> appArgs2 = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs2, null, EXIT_OK);

		// First launch of eclipse should not have vmargs provided by eclipse.ini i.e.,
		// -Xms40m
		// and have the argument mentioned on commandline i.e., -Dtest=1
		assertFalse(appArgs1.contains("-Xms40m"));
		assertTrue(appArgs1.contains("-Dtest=1"));

		// After relaunch, vmargs should also contain -Dtest=2 and it should appear
		// after -Dtest=1
		assertFalse(appArgs2.contains("-Xms40m"));
		assertTrue(appArgs2.contains("-Dtest=1"));
		assertTrue(appArgs2.contains("-Dtest=2"));
		assertTrue(appArgs2.indexOf("-Dtest=2") > appArgs2.indexOf("-Dtest=1"));
	}

	@Test
	void test_newVMArgsForRelaunchhWithSkipOldUserArgs() throws IOException {
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT);

		// Start eclipse with arguments '-vmargs -Dtest=1'. Note that by default
		// --launcher.overrideVmargs is set
		startEclipseLauncher(List.of("-vmargs", "-Dtest=1"));

		Socket socket = server.accept();

		List<String> appArgs1 = new ArrayList<>();
		// Relaunch should provide arguments '-vmargs -Dtest=2' and
		// '--launcher.skipOldUserArgs'
		analyzeLaunchedTestApp(socket, appArgs1, "--launcher.skipOldUserArgs\n-vmargs\n-Dtest=2", EXIT_RELAUNCH);

		socket = server.accept();

		List<String> appArgs2 = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs2, null, EXIT_OK);

		// First launch of eclipse should not have vmargs provided by eclipse.ini i.e.,
		// -Xms40m
		// and have the argument mentioned on commandline i.e., -Dtest=1
		assertFalse(appArgs1.contains("-Xms40m"));
		assertTrue(appArgs1.contains("-Dtest=1"));

		// After relaunch, vmargs should only contain -Dtest=2
		assertFalse(appArgs2.contains("-Xms40m"));
		assertFalse(appArgs2.contains("-Dtest=1"));
		assertTrue(appArgs2.contains("-Dtest=2"));
	}

	@Test
	void test_newVMArgsForRelaunchhWithAppendVMArgs() throws IOException {
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT + "-Dtest=0");

		// Start eclipse with arguments '-vmargs -Dtest=1'. Note that by default
		// --launcher.overrideVmargs is set
		startEclipseLauncher(List.of("-vmargs", "-Dtest=1"));

		Socket socket = server.accept();

		List<String> appArgs1 = new ArrayList<>();
		// Relaunch should provide arguments '-vmargs -Dtest=2' and
		// '--launcher.appendVmargs'
		analyzeLaunchedTestApp(socket, appArgs1, "--launcher.appendVmargs\n-vmargs\n-Dtest=2", EXIT_RELAUNCH);

		socket = server.accept();

		List<String> appArgs2 = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs2, null, EXIT_OK);

		// First launch of eclipse should not have vmargs provided by eclipse.ini i.e.,
		// -Xms40m and -Dtest=0
		// and have the argument mentioned on commandline i.e., -Dtest=1
		assertFalse(appArgs1.contains("-Xms40m"));
		assertFalse(appArgs1.contains("-Dtest=0"));
		assertTrue(appArgs1.contains("-Dtest=1"));

		// After relaunch, vmargs should contain all args; args from commandline,
		// eclipse.ini and from args provided by relaunch
		assertTrue(appArgs2.contains("-Xms40m"));
		assertTrue(appArgs2.contains("-Dtest=0"));
		assertTrue(appArgs2.contains("-Dtest=1"));
		assertTrue(appArgs2.contains("-Dtest=2"));
		// @formatter:off
		// -Dtest args should appear in order as
		// - from eclipse.ini
		// - then from commandline
		// - then from relaunch
		// Hence relaunch one will be effective
		// @formatter:on
		assertTrue(appArgs2.indexOf("-Dtest=2") > appArgs2.indexOf("-Dtest=1"));
		assertTrue(appArgs2.indexOf("-Dtest=1") > appArgs2.indexOf("-Dtest=0"));
	}

	@Test
	void test_newVMArgsForRelaunchhWithAppendVMArgsAndSkipOldUserArgs() throws IOException {
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT + "-Dtest=0");

		// Start eclipse with arguments '-vmargs -Dtest=1'. Note that by default
		// --launcher.overrideVmargs is set
		startEclipseLauncher(List.of("-vmargs", "-Dtest=1"));

		Socket socket = server.accept();

		List<String> appArgs1 = new ArrayList<>();
		// Relaunch should provide arguments '-vmargs -Dtest=2',
		// '--launcher.appendVmargs' and '--launcher.skipOldUserArgs'
		analyzeLaunchedTestApp(socket, appArgs1,
				"--launcher.appendVmargs\n--launcher.skipOldUserArgs\n-vmargs\n-Dtest=2", EXIT_RELAUNCH);

		socket = server.accept();

		List<String> appArgs2 = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs2, null, EXIT_OK);

		// First launch of eclipse should not have vmargs provided by eclipse.ini i.e.,
		// -Xms40m and -Dtest=0
		// and have the argument mentioned on commandline i.e., -Dtest=1
		assertFalse(appArgs1.contains("-Xms40m"));
		assertFalse(appArgs1.contains("-Dtest=0"));
		assertTrue(appArgs1.contains("-Dtest=1"));

		// After restart, user provided args should be ignored and only eclipse.ini and
		// relaunch provided args should exist
		assertTrue(appArgs2.contains("-Xms40m"));
		assertTrue(appArgs2.contains("-Dtest=0"));
		assertFalse(appArgs2.contains("-Dtest=1")); // provided from commandline and doesn't exist on restart
		assertTrue(appArgs2.contains("-Dtest=2"));
		assertTrue(appArgs2.indexOf("-Dtest=2") > appArgs2.indexOf("-Dtest=0"));
	}

	@Test
	void test_newNonVMAndVMArgsForRelaunchhWithAppendVMArgs() throws IOException {
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT + "-Dtest=0");

		// Start eclipse with arguments '-data dir1 -vmargs -Dtest=1'. Note that by default
		// --launcher.overrideVmargs is set
		startEclipseLauncher(List.of("-data", "dir1", "-vmargs", "-Dtest=1"));

		Socket socket = server.accept();

		List<String> appArgs1 = new ArrayList<>();
		// Relaunch should provide arguments '-data dir2 -vmargs -Dtest=2' and '--launcher.appendVmargs'
		analyzeLaunchedTestApp(socket, appArgs1,
				"--launcher.appendVmargs\n-data\ndir2\n-vmargs\n-Dtest=2", EXIT_RELAUNCH);

		socket = server.accept();

		List<String> appArgs2 = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs2, null, EXIT_OK);

		// First launch of eclipse should not have vmargs provided by eclipse.ini i.e.,
		// -Xms40m and -Dtest=0
		// and have the argument mentioned on commandline i.e., -Dtest=1
		assertFalse(appArgs1.contains("-Xms40m"));
		assertFalse(appArgs1.contains("-Dtest=0"));
		assertTrue(appArgs1.contains("-Dtest=1"));

		// After restart, all args should exist
		assertTrue(appArgs2.contains("-Xms40m"));
		assertTrue(appArgs2.contains("-Dtest=0"));
		assertTrue(appArgs2.contains("-Dtest=1"));
		assertTrue(appArgs2.contains("-Dtest=2"));
		assertTrue(appArgs2.indexOf("-Dtest=2") > appArgs2.indexOf("-Dtest=0"));

		// -data argument should be twice
		assertEquals(2, Collections.frequency(appArgs2, "-data"));
		assertTrue(appArgs2.contains("dir2"));
		assertTrue(appArgs2.contains("dir1"));
		// dir2 argument should appear later so that it gets effective
		assertTrue(appArgs2.indexOf("dir2") > appArgs2.indexOf("dir1"));
	}

	@Test
	void test_newNonVMAndVMArgsForRelaunchhWithAppendVMArgsAndSkipOldUserArgs() throws IOException {
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT + "-Dtest=0");

		// Start eclipse with arguments '-data dir1 -vmargs -Dtest=1'. Note that by default
		// --launcher.overrideVmargs is set
		startEclipseLauncher(List.of("-data", "dir1", "-vmargs", "-Dtest=1"));

		Socket socket = server.accept();

		List<String> appArgs1 = new ArrayList<>();
		// Relaunch should provide arguments '-data dir2 -vmargs -Dtest=2',
		// '--launcher.appendVmargs' and '--launcher.skipOldUserArgs'
		analyzeLaunchedTestApp(socket, appArgs1,
				"--launcher.appendVmargs\n--launcher.skipOldUserArgs\n-data\ndir2\n-vmargs\n-Dtest=2", EXIT_RELAUNCH);

		socket = server.accept();

		List<String> appArgs2 = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs2, null, EXIT_OK);

		// First launch of eclipse should not have vmargs provided by eclipse.ini i.e.,
		// -Xms40m and -Dtest=0
		// and have the argument mentioned on commandline i.e., -Dtest=1
		assertFalse(appArgs1.contains("-Xms40m"));
		assertFalse(appArgs1.contains("-Dtest=0"));
		assertTrue(appArgs1.contains("-Dtest=1"));

		// After restart, user provided args should be ignored and only eclipse.ini and
		// relaunch provided args should exist
		assertTrue(appArgs2.contains("-Xms40m"));
		assertTrue(appArgs2.contains("-Dtest=0"));
		assertFalse(appArgs2.contains("-Dtest=1")); // provided from commandline and doesn't exist on restart
		assertTrue(appArgs2.contains("-Dtest=2"));
		assertTrue(appArgs2.indexOf("-Dtest=2") > appArgs2.indexOf("-Dtest=0"));

		// -data argument should appear once
		assertEquals(1, Collections.frequency(appArgs2, "-data"));
		// -data argument should exist with only value dir2
		assertTrue(appArgs2.contains("dir2"));
		assertFalse(appArgs2.contains("dir1"));
	}

	@Test
	void test_ArgsRemainSameOverRestarts() throws IOException {
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT);

		startEclipseLauncher(Collections.emptyList());

		Socket socket = server.accept();

		List<String> appArgs1 = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs1, null, EXIT_RESTART);
		appArgs1.remove(appArgs1.indexOf(TestLauncherConstants.EXITDATA_PARAMETER) + 1);

		for (int i = 0; i < 10; i++) {
			socket = server.accept();

			List<String> appArgs2 = new ArrayList<>();
			analyzeLaunchedTestApp(socket, appArgs2, null, i == 9 ? EXIT_OK : EXIT_RESTART);
			// Other than exitdata arg, all other args should be same over restarts
			appArgs2.remove(appArgs2.indexOf(TestLauncherConstants.EXITDATA_PARAMETER) + 1);

			// Convert backslashes to forward slashes before comparison so that all paths are consistent
			assertEquals(appArgs1.stream().map(s -> s.replace('\\', '/')).collect(Collectors.toList()),
					appArgs2.stream().map(s -> s.replace('\\', '/')).collect(Collectors.toList()));
		}
	}

	@Test
	void test_ArgsRemainSameOverRelaunches() throws IOException {
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT);

		startEclipseLauncher(List.of("-data", "dir1"));

		Socket socket = server.accept();

		List<String> appArgs1 = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs1, "--launcher.skipOldUserArgs\n-data\ndir1", EXIT_RELAUNCH);
		appArgs1.remove(appArgs1.indexOf(TestLauncherConstants.EXITDATA_PARAMETER) + 1);

		for (int i = 0; i < 10; i++) {
			socket = server.accept();

			List<String> appArgs2 = new ArrayList<>();
			analyzeLaunchedTestApp(socket, appArgs2, "--launcher.skipOldUserArgs\n-data\ndir1", i == 9 ? EXIT_OK : EXIT_RELAUNCH);
			// Other than exitdata arg, all other args should be same over these relaunches
			appArgs2.remove(appArgs2.indexOf(TestLauncherConstants.EXITDATA_PARAMETER) + 1);

			// Convert backslashes to forward slashes before comparison so that all paths are consistent
			assertEquals(appArgs1.stream().map(s -> s.replace('\\', '/')).collect(Collectors.toList()),
					appArgs2.stream().map(s -> s.replace('\\', '/')).collect(Collectors.toList()));
		}
	}

	private void analyzeLaunchedTestApp(Socket socket, List<String> appArgs, String restartArgs, int appExitCode)
			throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		out.writeBytes(TestLauncherConstants.ARGS_PARAMETER + "\n");
		out.flush();
		String line = null;
		System.out.println("--- start ----");
		while ((line = in.readLine()) != null) {
			if (TestLauncherConstants.MULTILINE_ARG_VALUE_TERMINATOR.equals(line))
				break;
			System.out.println(line);
			appArgs.add(line);
		}
		System.out.println("--- end ----");
		{
			out.writeBytes(TestLauncherConstants.EXITDATA_PARAMETER + "\n");
			if (restartArgs != null && !restartArgs.isBlank())
				out.writeBytes(restartArgs + "\n");
			out.writeBytes(TestLauncherConstants.MULTILINE_ARG_VALUE_TERMINATOR + "\n");
			out.flush();

			out.writeBytes(TestLauncherConstants.EXITCODE_PARAMETER + "\n");
			out.writeBytes(appExitCode + "\n");
			out.flush();
		}
	}

	private Process startEclipseLauncher(List<String> args) throws IOException {
		String launcherPath = new File(
				"eclipse" + (System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : ""))
				.getAbsolutePath();
		List<String> allArgs = new ArrayList<>();
		allArgs.add(launcherPath);
		allArgs.addAll(args);
		ProcessBuilder pb = new ProcessBuilder(allArgs);
		pb.environment().put(TestLauncherConstants.PORT_ENV_KEY, Integer.toString(server.getLocalPort()));
		return pb.start();
	}

	private void writeEclipseIni(String content) throws IOException {
		File iniFile = new File(ECLIPSE_INI_FILE_NAME);
		iniFile.createNewFile();
		FileWriter myWriter = new FileWriter(ECLIPSE_INI_FILE_NAME);
		myWriter.write(content);
		myWriter.close();
	}

}
