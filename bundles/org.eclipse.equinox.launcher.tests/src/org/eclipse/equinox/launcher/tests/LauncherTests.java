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
package org.eclipse.equinox.launcher.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.eclipse.equinox.launcher.TestLauncherApp;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class LauncherTests {

	private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
	private static final String ECLIPSE_EXE_NAME = (OS_NAME.contains("win") ? "eclipsec.exe" : "eclipse");
	private static final String ECLIPSE_INI_FILE_NAME = "eclipse.ini";

	private static final String DEFAULT_ECLIPSE_INI_CONTENT = """
			-startup
			test.launcher.jar
			--launcher.library
			plugins/org.eclipse.equinox.launcher
			-vmargs
			-Xms40m
			""";
	public static final Integer EXIT_OK = 0;
	public static final Integer EXIT_RESTART = 23;
	public static final Integer EXIT_RELAUNCH = 24;

	@TempDir
	static Path tempDir;
	private static Path eclipseInstallationMockLocation;

	@BeforeAll
	static void prepareLauncherSetup() throws Exception {
		eclipseInstallationMockLocation = Files.createDirectories(tempDir.resolve("eclipse"));

		Path codeSource = Path.of(LauncherTests.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		Path equinoxRepo;
		Path classesDirectory;
		if (Files.isDirectory(codeSource) && codeSource.endsWith(Path.of("org.eclipse.equinox.launcher.tests"))) {
			// Test executed from within the IDE
			equinoxRepo = codeSource.getParent().getParent();
			classesDirectory = Path.of("bin");
		} else if (Files.isDirectory(codeSource)
				&& codeSource.endsWith(Path.of("org.eclipse.equinox.launcher.tests/target/classes"))) {
			// Test executed by Maven+Tycho
			equinoxRepo = codeSource.getParent().getParent().getParent().getParent();
			classesDirectory = Path.of("target", "classes");
		} else {
			throw new IllegalStateException("Unkown state");
		}

		Path binariesRoot = findBinariesRoot(equinoxRepo);
		if (!Files.isDirectory(binariesRoot)) {
			throw new IllegalArgumentException("Supplied equinox binaries location does not exist: " + binariesRoot);
		}
		String os = System.getProperty("osgi.os");
		String ws = System.getProperty("osgi.ws");
		String arch = System.getProperty("osgi.arch");

		// Assemble a Eclipse installation mock in the test-temporary directory

		Path exePath = Path.of("org.eclipse.equinox.executable", "bin", ws, os, arch);
		if (os.contains("mac")) {
			exePath = exePath.resolve("Eclipse.app/Contents/MacOS/eclipse");
		} else {
			exePath = exePath.resolve(ECLIPSE_EXE_NAME);
		}
		Files.createDirectories(eclipseInstallationMockLocation.resolve(ECLIPSE_EXE_NAME).getParent());
		Files.copy(binariesRoot.resolve(exePath), eclipseInstallationMockLocation.resolve(ECLIPSE_EXE_NAME));

		Path launcherDir = binariesRoot.resolve("org.eclipse.equinox.launcher." + ws + "." + os + "." + arch);
		try (var files = Files.walk(launcherDir, 1).filter(Files::isRegularFile)) {
			List<Path> launcherLibs = files.filter(f -> {
				String filename = f.getFileName().toString();
				return filename.startsWith("eclipse_") && filename.endsWith((os.contains("win") ? ".dll" : ".so"));
			}).toList();
			assertEquals(1, launcherLibs.size(), () -> "Not exactly one launcher library file found: " + launcherLibs);
			Path launcherLibFile = launcherLibs.get(0);

			Path mockLauncherDir = eclipseInstallationMockLocation.resolve("plugins/org.eclipse.equinox.launcher");
			Files.copy(launcherLibFile,
					Files.createDirectories(mockLauncherDir).resolve(launcherLibFile.getFileName()));
		}

		Manifest manifest = new Manifest();
		Attributes mainAttributes = manifest.getMainAttributes();
		mainAttributes.put(Name.MANIFEST_VERSION, "1.0");
		mainAttributes.putValue("Bundle-ManifestVersion", "2");
		mainAttributes.putValue("Main-Class", "org.eclipse.equinox.launcher.TestLauncherApp");

		Path launcherJarPath = eclipseInstallationMockLocation.resolve("test.launcher.jar");
		try (var out = Files.newOutputStream(launcherJarPath);
				JarOutputStream jar = new JarOutputStream(out, manifest);) {
			addProjectsClassFiles(jar,
					equinoxRepo.resolve("bundles/org.eclipse.equinox.launcher.tests").resolve(classesDirectory));
			addProjectsClassFiles(jar,
					equinoxRepo.resolve("bundles/org.eclipse.equinox.launcher").resolve(classesDirectory));
		}
	}

	private static Path findBinariesRoot(Path equinoxRepositoryPath) {
		String binariesLocationName = "EQUINOX_BINARIES_LOC"; // dots in variable names are not permitted in POSIX
		Optional<Path> binariesRepo = Optional.ofNullable(System.getProperty(binariesLocationName))
				.or(() -> Optional.ofNullable(System.getenv(binariesLocationName))).map(Path::of);
		if (binariesRepo.isEmpty()) { // search for co-located repository with known names
			binariesRepo = Stream.of("equinox.binaries", "rt.equinox.binaries")
					.map(equinoxRepositoryPath::resolveSibling).filter(Files::isDirectory).findFirst();
		}
		return binariesRepo.orElseThrow(() -> new IllegalStateException(
				"Location of equinox binaries could not be auto-detected and was not provided via the System.property or environment.variable '"
						+ binariesLocationName + "'."));
	}

	private static void addProjectsClassFiles(JarOutputStream jar, Path classesFolder) throws IOException {
		try (Stream<Path> classFiles = Files.walk(classesFolder);) {
			for (Path classFile : (Iterable<Path>) classFiles::iterator) {
				if (Files.isRegularFile(classFile)) {
					String entryName = classesFolder.relativize(classFile).toString().replace('\\', '/');
					jar.putNextEntry(new ZipEntry(entryName));
					Files.copy(classFile, jar);
				}
			}
		}
	}

	@AfterAll
	static void cleanUp() throws Exception {
		// Some of the processes launched stay alive for a bit too long to be able to
		// delete the temp directory
		ProcessHandle.current().descendants().forEach(ProcessHandle::destroyForcibly);
		Thread.sleep(100);
	}

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
		assertEquals(0, launcherProcess.exitValue());
	}

	@Test
	void test_appTerminatesWithoutNoRestartWithEXIT_OK() throws IOException, InterruptedException {
		test_norestart(EXIT_OK, false);
	}

	@Test
	void test_appTerminatesWithNoRestartAndEXIT_OK() throws IOException, InterruptedException {
		test_norestart(EXIT_OK, true);
	}

	@Test
	void test_appTerminatesWithNoRestartAndEXIT_RESTART() throws IOException, InterruptedException {
		test_norestart(EXIT_RESTART, true);
	}

	@Test
	void test_appTerminatesWithNoRestartAndEXIT_RELAUNCH() throws IOException, InterruptedException {
		test_norestart(EXIT_RELAUNCH, true);
	}

	@Test
	void test_appTerminatesWithoutNoRestartWithExitCode100() throws IOException, InterruptedException {
		test_norestart(100, false);
	}

	@Test
	void test_appTerminatesWithNoRestartAndExitCode100() throws IOException, InterruptedException {
		test_norestart(100, true);
	}

	private void test_norestart(int exitCode, boolean addNoRestartArg) throws IOException, InterruptedException {
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT);

		List<String> launcherArgs = addNoRestartArg ? List.of("--launcher.noRestart", "--launcher.suppressErrors")
				: List.of("--launcher.suppressErrors");
		Process launcherProcess = startEclipseLauncher(launcherArgs);

		Socket socket = server.accept();

		List<String> appArgs = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs, null, exitCode);

		// Make sure --launcher.noRestart arg is picked
		assertEquals(addNoRestartArg, appArgs.contains("--launcher.noRestart"));
		// Make sure launcher exited with expected exit value
		launcherProcess.waitFor(5, TimeUnit.SECONDS);
		assertEquals(exitCode, launcherProcess.exitValue());
		try {
			server.accept();
			if (addNoRestartArg) {
				fail("New eclipse started even with --launcher.noRestart arg and exit code " + exitCode);
			} else {
				fail("New eclipse started even with exit code " + exitCode);
			}
		} catch (SocketTimeoutException e) {
			// No new instance launched
			return;
		}
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
		appArgs1.remove(appArgs1.indexOf(TestLauncherApp.EXITDATA_PARAMETER) + 1);
		appArgs2.remove(appArgs2.indexOf(TestLauncherApp.EXITDATA_PARAMETER) + 1);
		// After restart, only -Dtest arg should be extra, other than that args should
		// be same
		appArgs2.remove(appArgs2.indexOf("-Dtest"));

		// Convert backslashes to forward slashes before comparison so that all paths are consistent
		assertEquals(appArgs1.stream().map(s -> s.replace('\\', '/')).toList(),
				appArgs2.stream().map(s -> s.replace('\\', '/')).toList());
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
		appArgs1.remove(appArgs1.indexOf(TestLauncherApp.EXITDATA_PARAMETER) + 1);
		appArgs2.remove(appArgs2.indexOf(TestLauncherApp.EXITDATA_PARAMETER) + 1);
		// After relaunch, -Dtest and -data args should be extra, other than that args should
		// be same
		appArgs2.remove(appArgs2.indexOf("-Dtest"));
		appArgs2.remove(appArgs2.indexOf("-data"));
		appArgs2.remove(appArgs2.indexOf("dir1"));

		// Convert backslashes to forward slashes before comparison so that all paths are consistent
		assertEquals(appArgs1.stream().map(s -> s.replace('\\', '/')).toList(),
				appArgs2.stream().map(s -> s.replace('\\', '/')).toList());
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
		assertEquals(2, Collections.frequency(appArgs2, "-data"));
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
		appArgs1.remove(appArgs1.indexOf(TestLauncherApp.EXITDATA_PARAMETER) + 1);

		for (int i = 0; i < 10; i++) {
			socket = server.accept();

			List<String> appArgs2 = new ArrayList<>();
			analyzeLaunchedTestApp(socket, appArgs2, null, i == 9 ? EXIT_OK : EXIT_RESTART);
			// Other than exitdata arg, all other args should be same over restarts
			appArgs2.remove(appArgs2.indexOf(TestLauncherApp.EXITDATA_PARAMETER) + 1);

			// Convert backslashes to forward slashes before comparison so that all paths are consistent
			assertEquals(appArgs1.stream().map(s -> s.replace('\\', '/')).toList(),
					appArgs2.stream().map(s -> s.replace('\\', '/')).toList());
		}
	}

	@Test
	void test_ArgsRemainSameOverRelaunches() throws IOException {
		writeEclipseIni(DEFAULT_ECLIPSE_INI_CONTENT);

		startEclipseLauncher(List.of("-data", "dir1"));

		Socket socket = server.accept();

		List<String> appArgs1 = new ArrayList<>();
		analyzeLaunchedTestApp(socket, appArgs1, "--launcher.skipOldUserArgs\n-data\ndir1", EXIT_RELAUNCH);
		appArgs1.remove(appArgs1.indexOf(TestLauncherApp.EXITDATA_PARAMETER) + 1);

		for (int i = 0; i < 10; i++) {
			socket = server.accept();

			List<String> appArgs2 = new ArrayList<>();
			analyzeLaunchedTestApp(socket, appArgs2, "--launcher.skipOldUserArgs\n-data\ndir1", i == 9 ? EXIT_OK : EXIT_RELAUNCH);
			// Other than exitdata arg, all other args should be same over these relaunches
			appArgs2.remove(appArgs2.indexOf(TestLauncherApp.EXITDATA_PARAMETER) + 1);

			// Convert backslashes to forward slashes before comparison so that all paths are consistent
			assertEquals(appArgs1.stream().map(s -> s.replace('\\', '/')).toList(),
					appArgs2.stream().map(s -> s.replace('\\', '/')).toList());
		}
	}

	private void analyzeLaunchedTestApp(Socket socket, List<String> appArgs, String restartArgs, int appExitCode)
			throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		out.writeBytes(TestLauncherApp.ARGS_PARAMETER + "\n");
		out.flush();
		String line = null;
		System.out.println("--- start ----");
		while ((line = in.readLine()) != null) {
			if (TestLauncherApp.MULTILINE_ARG_VALUE_TERMINATOR.equals(line)) {
				break;
			}
			System.out.println(line);
			appArgs.add(line);
		}
		System.out.println("--- end ----");
		{
			out.writeBytes(TestLauncherApp.EXITDATA_PARAMETER + "\n");
			if (restartArgs != null && !restartArgs.isBlank()) {
				out.writeBytes(restartArgs + "\n");
			}
			out.writeBytes(TestLauncherApp.MULTILINE_ARG_VALUE_TERMINATOR + "\n");
			out.flush();

			out.writeBytes(TestLauncherApp.EXITCODE_PARAMETER + "\n");
			out.writeBytes(appExitCode + "\n");
			out.flush();
		}
	}

	private Process startEclipseLauncher(List<String> args) throws IOException {
		Path launcherPath = eclipseInstallationMockLocation.resolve(ECLIPSE_EXE_NAME);
		List<String> allArgs = new ArrayList<>();
		allArgs.add(launcherPath.toString());
		allArgs.addAll(args);
		ProcessBuilder pb = new ProcessBuilder(allArgs);
		pb.directory(eclipseInstallationMockLocation.toFile());
		pb.environment().put(TestLauncherApp.PORT_ENV_KEY, Integer.toString(server.getLocalPort()));
		return pb.start();
	}

	private static void writeEclipseIni(String content) throws IOException {
		Files.writeString(eclipseInstallationMockLocation.resolve(ECLIPSE_INI_FILE_NAME), content);
	}

}
