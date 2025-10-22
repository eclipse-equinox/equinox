---
layout: post
title: Equinox Startup Issues
summary: Common startup issues and their solutions
---

* The generated Toc will be an ordered list
{:toc}

## Overview

This page documents common issues encountered when starting Eclipse or Equinox-based applications and provides solutions and workarounds.

## Common Startup Problems

### Java Virtual Machine Not Found

**Problem**: The launcher cannot find a suitable Java Virtual Machine.

**Error Messages**:
- "A Java Runtime Environment (JRE) or Java Development Kit (JDK) must be available..."
- "No Java virtual machine was found..."

**Solutions**:
1. Ensure a compatible JDK/JRE is installed on your system
2. Specify the JVM explicitly in `eclipse.ini`:
   ```
   -vm
   /path/to/java/bin
   ```
   Note: The `-vm` option must appear before `-vmargs`
3. Set the `JAVA_HOME` environment variable to point to your JDK installation

### Insufficient Memory

**Problem**: The application runs out of memory during startup or operation.

**Error Messages**:
- "An error has occurred. See the log file..."
- OutOfMemoryError in logs

**Solutions**:
1. Increase heap size in `eclipse.ini` or via command line:
   ```
   -vmargs
   -Xms512m
   -Xmx2048m
   ```
2. For large workspaces, consider increasing `-Xmx` to 4096m or higher
3. If encountering PermGen errors (Java 7 and earlier), add:
   ```
   -XX:MaxPermSize=256m
   ```

### Configuration/Workspace Corruption

**Problem**: Cached configuration or workspace metadata is corrupted.

**Symptoms**:
- Eclipse fails to start
- Bundles fail to resolve
- Application hangs during startup

**Solutions**:
1. Start with the `-clean` option to clear cached OSGi data:
   ```
   eclipse -clean
   ```
2. Delete the `.metadata/.plugins/org.eclipse.core.resources/.snap` file
3. Start with a new workspace
4. For severe corruption, delete the `configuration/org.eclipse.osgi` directory

### Library Loading Errors

**Problem**: Native libraries fail to load (SWT, launcher libraries).

**Error Messages**:
- "Cannot load SWT libraries"
- "UnsatisfiedLinkError"

**Solutions**:
1. Ensure you're using the correct platform-specific download
2. Check file permissions on native libraries
3. On Linux, ensure required system libraries are installed:
   ```bash
   sudo apt-get install libgtk-3-0
   ```
4. On macOS, verify Gatekeeper hasn't blocked the libraries:
   ```bash
   xattr -cr Eclipse.app
   ```

### Wrong Architecture

**Problem**: Attempting to run a 64-bit Eclipse with a 32-bit JVM (or vice versa).

**Error Messages**:
- "Failed to load the JNI shared library"
- Architecture mismatch errors

**Solutions**:
1. Ensure JVM architecture matches Eclipse architecture
2. Download the correct Eclipse version for your JVM
3. Specify the correct JVM in `eclipse.ini` with `-vm`

### Lock File Issues

**Problem**: Workspace is already in use or lock file cannot be created.

**Error Messages**:
- "Could not lock workspace"
- "The workspace is already in use"

**Solutions**:
1. Close other Eclipse instances using the same workspace
2. Delete the `.metadata/.lock` file (ensure no other instance is running)
3. Check workspace directory permissions
4. On network drives, ensure proper file locking support

### Plugin/Bundle Resolution Failures

**Problem**: Required plugins fail to resolve or load.

**Error Messages**:
- "Application error: An error occurred while loading the application"
- "!MESSAGE Bundle X was not resolved"

**Solutions**:
1. Start with `-clean` to rebuild bundle cache
2. Check OSGi console with `-console` for resolution errors:
   ```
   eclipse -console
   ```
3. Use `ss` (short status) command in console to check bundle states
4. Use `diag <bundle-id>` to diagnose specific bundle issues
5. Verify required bundles are present in the plugins directory

### Configuration Area Not Writable

**Problem**: The configuration area is read-only or cannot be written to.

**Solutions**:
1. Ensure proper permissions on the configuration directory
2. Specify a writable configuration location:
   ```
   eclipse -configuration /path/to/writable/location
   ```
3. On Unix systems, check that you're not running as root when you shouldn't be

### Splash Screen Hangs

**Problem**: Eclipse hangs showing the splash screen during startup.

**Solutions**:
1. Start with `-nosplash` to disable the splash screen:
   ```
   eclipse -nosplash
   ```
2. Check console output for hanging operations
3. Enable debug mode to see detailed startup progress:
   ```
   eclipse -debug -consoleLog
   ```

## Debugging Startup Issues

### Enable Debug Output

Start Eclipse with debug options to see detailed information:

```bash
eclipse -debug -consoleLog
```

For more specific debug output, create a `.options` file:

```
org.eclipse.osgi/debug=true
org.eclipse.osgi/resolver/debug=true
org.eclipse.osgi/resolver/wiring=true
```

Then start with:
```bash
eclipse -debug /path/to/.options -consoleLog
```

### Configure Debug Logging via JVM parameters

If you are using Equinox as the OSGi framework in your OSGi application (e.g. if you are using another way to lauch like the bnd launcher or OSGi Feature launcher) then you might want configure logging via command line properties. 

The following properties can be used:

- `osgi.debug=/path/to/.options` (see above)
- `eclipse.log.enabled=true/false`
- `eclipse.consoleLog=true/false`

If you prefer the built-in equinox logger to go to `System.out` also then you can set the framework/system property `eclipse.consoleLog=true`.

By default Equinox writes the framework ERROR to a log file also from [here](https://github.com/eclipse-equinox/equinox/blob/41591b51eec1a02f01daf7069db21f4f9258531f/bundles/org.eclipse.osgi/container/src/org/eclipse/osgi/internal/log/EquinoxLogWriter.java#L769) unless you disabled it with the property `eclipse.log.enabled=false` which gets read [here](https://github.com/eclipse-equinox/equinox/blob/41591b51eec1a02f01daf7069db21f4f9258531f/bundles/org.eclipse.osgi/container/src/org/eclipse/osgi/internal/log/EquinoxLogServices.java#L69)

**Example via a .bndrun***

```
# launch.bndrun
-runproperties.debug: \
	eclipse.consoleLog=true
```

This enables basic logging of errors.

To use an additional `.options` file for more fine grained logging output do:

```
# launch.bndrun
-runproperties.debug: \
   osgi.debug=${workspace}/my.bundle.foo/debug.options,\
   eclipse.consoleLog=true
```

This enables console output 

### Check Log Files

Always check the workspace log for detailed error information:
- Location: `<workspace>/.metadata/.log`
- Contains stack traces and detailed error messages
- Most recent errors are at the end of the file

### Use the OSGi Console

Start Eclipse with the console to inspect bundle states:

```bash
eclipse -console
```

Useful console commands:
- `ss` - Short status of all bundles
- `diag <bundle-id>` - Diagnose bundle issues
- `packages <bundle-id>` - Show package dependencies
- `services` - List registered services

### Check Java Version Compatibility

Verify your Java version meets the minimum requirements:

```bash
java -version
```

Different Eclipse versions have different requirements:
- Eclipse 2023-12 and later: Java 17 or higher
- Eclipse 2022-09 to 2023-09: Java 11 or higher
- Eclipse 4.6 to 2022-06: Java 8 or higher

## Platform-Specific Issues

### Windows

- **Long path issues**: Windows has path length limitations. Keep Eclipse in a directory with a short path.
- **Antivirus interference**: Some antivirus programs may interfere with Eclipse. Add exceptions if needed.
- **UAC**: Don't install Eclipse in `Program Files` to avoid User Account Control issues.

### Linux

- **GTK version**: Ensure GTK 3 libraries are installed
- **Display server**: Some issues may occur with Wayland; try X11 if problems persist
- **File watchers**: May need to increase file watch limit:
  ```bash
  echo fs.inotify.max_user_watches=524288 | sudo tee -a /etc/sysctl.conf
  sudo sysctl -p
  ```

### macOS

- **Gatekeeper**: First launch may require right-click > Open
- **Quarantine attribute**: Remove with `xattr -cr Eclipse.app`
- **Notarization**: Ensure you're using a properly notarized version
- **Rosetta**: Apple Silicon Macs can run x86_64 versions through Rosetta 2

## Getting Help

If none of these solutions work:

1. Check the [Eclipse Community Forums](https://www.eclipse.org/forums/)
2. Report issues on [GitHub](https://github.com/eclipse-equinox/equinox/issues)
3. Include:
   - Eclipse version and build ID
   - Java version (`java -version`)
   - Operating system and version
   - Complete error messages and stack traces
   - Contents of `.metadata/.log`

## See Also

- [Equinox Launcher](equinox_launcher.html)
- [Launcher Issues](launcher_issues.html)
- [Starting Eclipse from Command Line](starting_eclipse_commandline.html)
