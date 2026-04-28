
/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Kevin Cornell (Rational Software Corporation)
 *     Martin Oberhuber (Wind River) - [176805] Support building with gcc and debug
 *     Hannes Wellmann - Convert build shell scripts into plain Java script
 *******************************************************************************/

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/// Usage:
/// ```
/// java build.java [install] [<optional switches>] [clean]
/// ```
///
/// where the optional switches are:
/// - `-os <DEFAULT_OS>` - targeted operating system (`linux`, `macosx`, `win32`)
/// - `-ws <DEFAULT_WS>` - targeted windowing system (`gtk`, `cocoa`, `win32`)
/// - `-arch <DEFAULT_OS_ARCH>` - targeted CPU architecture (`aarch64`, `x86_64`,
///   ...)
/// - `-java <JAVA_HOME>` - location of a Java SDK for JNI headers and libraries
/// - `-output <PROGRAM_OUTPUT>` - executable filename (`eclipse` or `eclipse.exe`)
///
/// All other arguments are directly passed to the `make` program. This script
/// can also be invoked with the `clean` argument.
///
/// Examples:
/// ```
/// java build.java clean
/// java build.java install -java C:\dev\java25 OPTFLAG=-g PICFLAG=-fpic
/// ```
///
public class build {

	static final String OS = "-os";
	static final String WS = "-ws";
	static final String ARCH = "-arch";
	static final String JAVA_HOME = "-java";
	static final String OUTPUT = "-output";

	static final String TARGET_CLEAN = "clean";
	static final String TARGET_INSTALL = "install";

	public static void main(String[] args) throws Exception {
		Map<String, String> argValue = new HashMap<>();
		List<String> arguments = readArguments(args, argValue);

		OperatingSystem os = argValue.containsKey(OS) ? OperatingSystem.byIdentifier(argValue.get(OS)) : null;
		String ws = argValue.get(WS);
		if (os == null && ws == null) {
			os = OperatingSystem.autoDetect();
		}
		if (ws == null) {
			ws = os.defaultWindowingSystem;
		} else if (os == null) {
			os = OperatingSystem.ofWindowingSystem(ws);
		}
		String arch = argValue.get(ARCH);
		if (arch == null) {
			arch = autoDetectCPUArchitecture();
		}
		String programOutput = argValue.getOrDefault(OUTPUT, "eclipse" + (os == OperatingSystem.WINDOWS ? ".exe" : ""));
		String javaHome = argValue.getOrDefault(JAVA_HOME, System.getProperty("java.home"));
		boolean install = arguments.remove(TARGET_INSTALL);
		boolean clean = arguments.remove(TARGET_CLEAN);

		Map<String, String> environment = new HashMap<>();
		environment.put("PROGRAM_OUTPUT", programOutput);
		environment.put("DEFAULT_OS", os.identifier);
		environment.put("DEFAULT_WS", ws);
		environment.put("DEFAULT_OS_ARCH", arch);
		environment.put("JAVA_HOME", javaHome);

		String makeTool = os == OperatingSystem.WINDOWS ? "nmake" : "make";
		String makefile = makeFileName(ws);
		List<String> makeCommand = List.of(makeTool, "-f", makefile);

		Path workingDir = workingDirectory(ws);

		System.out.println("Building Equinox launcher binaries with " + String.join(" ", makeCommand));
		System.out.println("  at " + workingDir);
		System.out.println("  for OS: " + os.identifier + ", WS: " + ws + ", ARCH: " + arch);
		System.out.println("  using JDK headers at: " + javaHome);

		List<String> commands = new ArrayList<>(arguments);
		if (install || !clean) { // call 'all' also as default
			commands.addFirst("all");
		}
		commands.addAll(0, makeCommand);

		if (os == OperatingSystem.WINDOWS) {
			Path msvcHome = findVisualStudio();
			String buildArch = switch (arch) {
			case "x86_64" -> "amd64";
			case "aarch64" -> "arm64";
			default -> throw new IllegalArgumentException("Unsupported target architecture: " + arch);
			};
			String processorArchitecture = System.getenv("PROCESSOR_ARCHITECTURE");
			if (!processorArchitecture.equalsIgnoreCase(buildArch)) {
				buildArch = processorArchitecture + "_" + buildArch;
			}
			commands.addAll(0, List.of(msvcHome.resolve(VC_VARSALL_BAT).toString(), buildArch, "&&"));
		}

		if (clean) {
			System.out.println("Cleaning working directory: " + workingDir);
			clean(workingDir, programOutput);
		}

		ProcessBuilder makeProcess = new ProcessBuilder(commands).directory(workingDir.toFile()).inheritIO();
		makeProcess.environment().putAll(environment);
		Process compileProcess = makeProcess.start();
		compileProcess.waitFor(5, TimeUnit.MINUTES);
		int exitValue = compileProcess.exitValue();
		if (exitValue != 0) {
			throw new IllegalStateException("Compile process terminated with error code: " + exitValue);
		}
		if (install) {
			Path binariesDir = Path
					.of(getEnvOrDefault("BINARIES_DIR", workingDir + "/../../../../../equinox.binaries"));
			Path defaultExePath = Path.of("org.eclipse.equinox.executable", "bin", ws, os.identifier, arch);
			if (os == OperatingSystem.MACOS) {
				defaultExePath.resolve("Eclipse.app/Contents/MacOS");
			}
			Path exeOutputDir = Path.of(getEnvOrDefault("EXE_OUTPUT_DIR", binariesDir + "/" + defaultExePath))
					.normalize().toAbsolutePath();
			String nativeLauncher = "org.eclipse.equinox.launcher." + ws + "." + os.identifier + "." + arch;
			Path libOutputDir = Path.of(getEnvOrDefault("LIB_OUTPUT_DIR", binariesDir + "/" + nativeLauncher))
					.normalize().toAbsolutePath();

			System.out.println("Installing built binaries");
			System.out.println("  Executable(s): " + exeOutputDir);
			System.out.println("  Library      : " + libOutputDir);

			moveFileInto(workingDir.resolve(programOutput), exeOutputDir);
			if (os == OperatingSystem.WINDOWS) {
				moveFileInto(workingDir.resolve(programOutput.replace(".exe", "c.exe")), exeOutputDir);
			}
			Files.createDirectories(libOutputDir);
			forEachLibraryFile(libOutputDir, Files::delete);
			forEachLibraryFile(workingDir, f -> Files.move(f, libOutputDir.resolve(f.getFileName())));
			clean(workingDir, programOutput);
		}
	}

	static List<String> readArguments(String[] args, Map<String, String> argumentValue) {
		Set<String> processedArguments = Set.of(OS, WS, ARCH, JAVA_HOME, OUTPUT);
		List<String> arguments = new ArrayList<>(Arrays.asList(args));
		for (int i = 1; i < arguments.size(); i++) {
			String key = arguments.get(i - 1);
			String value = arguments.get(i);
			if (processedArguments.contains(key) && !value.isBlank()) {
				argumentValue.put(key, value);
				arguments.remove(--i);
				arguments.remove(i);
			}
		}
		return arguments; // passed through arguments
	}

	enum OperatingSystem {
		LINUX("linux", "gtk"), MACOS("macosx", "cocoa"), WINDOWS("win32", "win32");

		final String identifier;
		final String defaultWindowingSystem;

		OperatingSystem(String identifier, String defaultWindowingSystem) {
			this.identifier = Objects.requireNonNull(identifier);
			this.defaultWindowingSystem = Objects.requireNonNull(defaultWindowingSystem);
		}

		static OperatingSystem byIdentifier(String id) { // id may be null
			return Arrays.stream(OperatingSystem.values()).filter(os -> os.identifier.equals(id)).findFirst()
					.orElseThrow(() -> new NoSuchElementException("No OS with id: " + id));
		}

		static OperatingSystem ofWindowingSystem(String ws) {
			return Arrays.stream(OperatingSystem.values()).filter(o -> o.defaultWindowingSystem.equals(ws)).findFirst()
					.orElseThrow(() -> new NoSuchElementException("No default OS for WS: " + ws));
		}

		static OperatingSystem autoDetect() {
			String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
			if (osName.startsWith("linux")) {
				return LINUX;
			} else if (osName.startsWith("mac os")) {
				return MACOS;
			} else if (osName.startsWith("windows")) {
				return WINDOWS;
			} else {
				throw new IllegalStateException("Failed to determine OS from name <" + osName + ">. Specify " + OS
						+ " or " + WS + " explicity");
			}
		}
	}

	static String autoDetectCPUArchitecture() {
		String arch;
		String archProperty = System.getProperty("os.arch");
		// For possible values see:
		// https://github.com/apache/commons-lang/blob/0745c26dac9ac76086f10c302d252a71bf4a68c5/src/main/java/org/apache/commons/lang3/ArchUtils.java#L103-L137
		arch = switch (archProperty) {
		case "aarch64" -> "aarch64";
		case "amd64", "x86_64" -> "x86_64";
		case "ppc64", "ppc64le", "power64" -> "ppc64le";
		case "riscv64" -> "riscv64";
		default -> throw new IllegalArgumentException("Unsupported arch: " + archProperty);
		};
		return arch;
	}

	static String makeFileName(String ws) {
		return "make_" + switch (ws) {
		case "win32" -> "win64";
		case "cocoa" -> "cocoa";
		case "gtk" -> "linux";
		default -> throw new IllegalArgumentException("Unsupported ws: " + ws);
		} + ".mak";
	}

	static Path workingDirectory(String ws) throws URISyntaxException, IOException {
		Path workingDirectory = Path.of(".").toRealPath();
		Path searchDirectory = workingDirectory;
		for (int i = 0; i < 2; i++) {
			Optional<Path> makeFile = Files.walk(searchDirectory, 3).filter(f -> f.endsWith("make_version.mak"))
					.findFirst();
			if (makeFile.isPresent()) {
				return makeFile.get().getParent().resolve(ws);
			}
			searchDirectory = searchDirectory.getParent();
		}
		throw new IllegalStateException("Failed to determine source directory from " + workingDirectory);
	}

	static void clean(Path workingDir, String programOutput) throws IOException {
		try (Stream<Path> files = Files.list(workingDir)) {
			List<String> deletionExtensions = List.of(".o", ".obj", ".exp", ".res", ".lib", ".exe", ".so", ".dll");
			files.filter(file -> {
				String filename = file.getFileName().toString();
				return filename.equals(programOutput) || deletionExtensions.stream().anyMatch(filename::endsWith);
			}).filter(Files::isRegularFile).forEach(consume(Files::delete));
		}
	}

	static Path moveFileInto(Path file, Path targetDir) throws IOException {
		Files.createDirectories(targetDir);
		return Files.move(file, targetDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
	}

	static void forEachLibraryFile(Path workingDir, ThrowingConsumer<Path, IOException> consumer) throws IOException {
		try (Stream<Path> files = Files.list(workingDir)) {
			files.filter(file -> {
				String name = file.getFileName().toString();
				return name.startsWith("eclipse_") && (name.endsWith(".so") || name.endsWith(".dll"));
			}).filter(Files::isRegularFile).forEach(consume(consumer));
		}
	}

	interface ThrowingConsumer<T, E extends Exception> {
		void accept(T t) throws E;
	}

	static <T, E extends Exception> Consumer<T> consume(ThrowingConsumer<T, E> consumer) {
		return t -> {
			try {
				consumer.accept(t);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		};
	}

	static String getEnvOrDefault(String name, String defaultValue) {
		String value = System.getenv(name);
		return value != null ? value : defaultValue;
	}

	static final Path VC_VARSALL_BAT = Path.of("VC/Auxiliary/Build/vcvarsall.bat");

	static Path findVisualStudio() {
		String version = getEnvOrDefault("MSVC_VERSION", "auto");
		String edition = getEnvOrDefault("MSVC_EDITION", "auto");
		Path msvcHome = Optional.ofNullable(System.getenv("MSVC_HOME")).map(Path::of).orElse(null);
		if (msvcHome == null) {
			msvcHome = Stream.of(System.getenv("ProgramFiles"), System.getenv("ProgramFiles(x86)")).map(Path::of)
					.map(p -> p.resolve("Microsoft Visual Studio")) //
					.mapMulti(appendPath(version, "2022", "2019")) //
					.mapMulti(appendPath(edition, "Community", "Enterprise", "Professional")) //
					.filter(Files::isDirectory).filter(path -> {
						if (!Files.isRegularFile(path.resolve(VC_VARSALL_BAT))) {
							System.err.println("-- VisualStudio '" + path + "' is bad: 'vcvarsall.bat' not found");
							return false;
						}
						return true;
					}).findFirst().orElse(null);
		}
		if (msvcHome == null || !Files.isDirectory(msvcHome)) {
			System.err.println(
					"WARNING: Microsoft Visual Studio not found (for edition=" + edition + " version=" + version + ")");
		} else {
			System.out.println("-- VisualStudio '" + msvcHome + "' looks good, selected.");
		}
		return msvcHome;
	}

	static BiConsumer<Path, Consumer<Path>> appendPath(String value, String... autoPaths) {
		return (basePath, downStream) -> {
			if (value.equals("auto")) {
				Arrays.stream(autoPaths).map(basePath::resolve).forEach(downStream);
			} else {
				downStream.accept(basePath.resolve(value));
			}
		};
	}
}
