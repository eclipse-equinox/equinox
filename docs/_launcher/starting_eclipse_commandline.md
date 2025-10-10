---
layout: post
title: Starting Eclipse from Command Line with Equinox Launcher
summary: How to start Eclipse and Equinox applications from the command line
---

* The generated Toc will be an ordered list
{:toc}

## Overview

This guide covers how to start Eclipse and Equinox-based applications from the command line. Understanding command-line options is essential for automation, scripting, headless operation, and troubleshooting.

## Basic Usage

### Starting Eclipse

The simplest way to start Eclipse:

```bash
# Linux/macOS
./eclipse

# Windows
eclipse.exe
```

### With Workspace

Specify a workspace location:

```bash
eclipse -data /path/to/workspace
```

### Common Options

```bash
eclipse -data ~/workspace -nosplash -consoleLog
```

## Command-Line Arguments

### Application Arguments

These arguments control Eclipse/Equinox behavior:

| Argument | Description | Example |
|----------|-------------|---------|
| `-application <id>` | Specifies the application to run | `-application org.eclipse.ui.ide.workbench` |
| `-configuration <location>` | Sets configuration directory | `-configuration /tmp/config` |
| `-console [port]` | Opens OSGi console | `-console 1234` |
| `-consoleLog` | Mirrors console output to log | `-consoleLog` |
| `-data <location>` | Sets workspace location | `-data ~/workspace` |
| `-debug [file]` | Enables debug mode | `-debug .options` |
| `-dev [classpath]` | Enables development mode | `-dev bin/` |
| `-clean` | Clears cached OSGi data | `-clean` |
| `-initialize` | Initializes configuration without starting | `-initialize` |

### Launcher Arguments

Control the launcher behavior:

| Argument | Description | Example |
|----------|-------------|---------|
| `-vm <path>` | Specifies JVM to use | `-vm /usr/lib/jvm/java-17/bin/java` |
| `-vmargs` | All following args passed to JVM | See below |
| `-nosplash` | No splash screen | `-nosplash` |
| `-showlocation` | Show workspace in title | `-showlocation` |
| `--launcher.defaultAction` | Default file action | `--launcher.defaultAction openFile` |
| `--launcher.library` | Launcher library path | `--launcher.library plugins/...` |

### VM Arguments

Arguments after `-vmargs` are passed to the Java Virtual Machine:

```bash
eclipse -vmargs -Xmx2048m -XX:+UseG1GC
```

Common JVM arguments:

| Argument | Description | Example |
|----------|-------------|---------|
| `-Xms<size>` | Initial heap size | `-Xms512m` |
| `-Xmx<size>` | Maximum heap size | `-Xmx2048m` |
| `-XX:+UseG1GC` | Use G1 garbage collector | `-XX:+UseG1GC` |
| `-Dkey=value` | Set system property | `-Dosgi.requiredJavaVersion=17` |

**Important**: `-vmargs` must be the last Eclipse argument. Everything after is passed to the JVM.

## Application Types

### IDE Workbench

Default Eclipse IDE:

```bash
eclipse -application org.eclipse.ui.ide.workbench
```

### Headless Applications

Run without UI:

```bash
eclipse -application org.eclipse.ant.core.antRunner -buildfile build.xml
```

### Custom Applications

Run your own application:

```bash
eclipse -application com.example.myapp -consoleLog
```

## Headless Execution

### Running Without Display

For true headless execution (no UI):

```bash
# Linux/macOS - disable display
export DISPLAY=
eclipse -application org.eclipse.ant.core.antRunner -buildfile build.xml

# Or use Xvfb for virtual display
xvfb-run eclipse -application org.eclipse.ant.core.antRunner -buildfile build.xml
```

### Headless Build Example

```bash
eclipse \
  -nosplash \
  -application org.eclipse.ant.core.antRunner \
  -buildfile /path/to/build.xml \
  -data /tmp/workspace \
  -configuration /tmp/config \
  -consoleLog
```

## Automation and Scripting

### Batch Processing

Example shell script for batch processing:

```bash
#!/bin/bash

ECLIPSE_HOME="/path/to/eclipse"
WORKSPACE="/tmp/batch-workspace"

"${ECLIPSE_HOME}/eclipse" \
  -nosplash \
  -application org.eclipse.ant.core.antRunner \
  -buildfile "${1}" \
  -data "${WORKSPACE}" \
  -consoleLog
```

Usage:
```bash
./run-build.sh /path/to/build.xml
```

### Exit Codes

Eclipse returns exit codes:
- `0`: Success
- `13`: Restart required
- Other: Error occurred

Check exit code in scripts:

```bash
eclipse -application com.example.app
EXIT_CODE=$?
if [ $EXIT_CODE -eq 0 ]; then
  echo "Success"
elif [ $EXIT_CODE -eq 13 ]; then
  echo "Restart required"
else
  echo "Error: $EXIT_CODE"
fi
```

### Continuous Integration

Example for CI environments:

```bash
#!/bin/bash
set -e

ECLIPSE_HOME="/opt/eclipse"
WORKSPACE="${WORKSPACE:-$HOME/workspace}"

"${ECLIPSE_HOME}/eclipse" \
  -nosplash \
  -application org.eclipse.ant.core.antRunner \
  -buildfile build.xml \
  -data "${WORKSPACE}" \
  -configuration "${WORKSPACE}/.configuration" \
  -clean \
  -consoleLog
```

## OSGi Console

### Starting with Console

Enable the OSGi console for debugging:

```bash
# Console on stdin/stdout
eclipse -console

# Console on specific port
eclipse -console 1234
```

### Connecting to Console

If console is on a port, connect with telnet:

```bash
telnet localhost 1234
```

### Useful Console Commands

| Command | Description |
|---------|-------------|
| `ss` | Short status of all bundles |
| `diag <id>` | Diagnose bundle resolution issues |
| `start <id>` | Start a bundle |
| `stop <id>` | Stop a bundle |
| `install <url>` | Install a bundle |
| `uninstall <id>` | Uninstall a bundle |
| `packages` | List package wiring |
| `services` | List registered services |
| `help` | Show all commands |
| `close` | Shutdown framework |

## Debug Mode

### Enabling Debug

Start with debug options:

```bash
eclipse -debug
```

### Debug Options File

Create `.options` file to control debug output:

```properties
# Framework debugging
org.eclipse.osgi/debug=true
org.eclipse.osgi/resolver/debug=true
org.eclipse.osgi/resolver/wiring=true

# Launcher debugging
org.eclipse.equinox.launcher/debug=true

# Application debugging
org.eclipse.core.runtime/debug=true
```

Use with:
```bash
eclipse -debug /path/to/.options -consoleLog
```

### Debug Output

Direct debug output to console:

```bash
eclipse -debug -consoleLog
```

## Development Mode

### Enabling Development Mode

For plugin development:

```bash
eclipse -dev
```

With custom classpath entries:

```bash
eclipse -dev file:/path/to/dev.properties
```

### Development Properties

Create `dev.properties`:

```properties
com.example.plugin=bin/
org.example.another=target/classes/
```

This adds development classpath entries for specified bundles.

## Advanced Configuration

### Multiple Instances

Run multiple Eclipse instances with separate configurations:

```bash
# Instance 1
eclipse -data ~/workspace1 -configuration ~/config1

# Instance 2
eclipse -data ~/workspace2 -configuration ~/config2
```

### Custom Configuration

Use a completely isolated configuration:

```bash
eclipse \
  -configuration /tmp/custom-config \
  -data /tmp/custom-workspace \
  -clean
```

### System Properties

Set OSGi and application properties:

```bash
eclipse -vmargs \
  -Dosgi.requiredJavaVersion=17 \
  -Dosgi.framework.extensions=org.eclipse.fx.osgi \
  -Dorg.eclipse.swt.browser.DefaultType=webkit
```

## eclipse.ini Configuration

### Basic Structure

The `eclipse.ini` file contains default command-line arguments (one per line):

```
-startup
plugins/org.eclipse.equinox.launcher_1.6.500.v20230717-2134.jar
--launcher.library
plugins/org.eclipse.equinox.launcher.gtk.linux.x86_64_1.2.800.v20230717-2134
-product
org.eclipse.platform.ide
--launcher.defaultAction
openFile
-showsplash
org.eclipse.epp.package.common
--launcher.appendVmargs
-vmargs
-Dosgi.requiredJavaVersion=17
-Xms256m
-Xmx2048m
--add-modules=ALL-SYSTEM
```

### Important Rules

1. **One argument per line**: Each option and its value on separate lines
2. **-vm before -vmargs**: JVM location must be specified before JVM arguments
3. **-vmargs last**: Everything after `-vmargs` goes to JVM
4. **Line endings**: Use platform-appropriate line endings

### Example with JVM Specification

```
-vm
/usr/lib/jvm/java-17-openjdk/bin/java
--launcher.defaultAction
openFile
-vmargs
-Dosgi.requiredJavaVersion=17
-Xms512m
-Xmx4096m
-XX:+UseG1GC
```

## Platform-Specific Considerations

### Windows

```cmd
REM Basic launch
eclipse.exe

REM With arguments
eclipse.exe -data C:\workspace -clean

REM Specify Java
eclipse.exe -vm C:\Program Files\Java\jdk-17\bin\javaw.exe
```

Console output:
```cmd
REM Use eclipsec.exe for console output
eclipsec.exe -consoleLog

REM Or redirect
eclipse.exe > output.txt 2>&1
```

### Linux

```bash
# Basic launch
./eclipse

# With arguments
./eclipse -data ~/workspace -clean

# Specify Java
./eclipse -vm /usr/lib/jvm/java-17/bin/java

# Background execution
./eclipse &

# With nohup
nohup ./eclipse > eclipse.log 2>&1 &
```

### macOS

```bash
# Launch from terminal
/Applications/Eclipse.app/Contents/MacOS/eclipse

# With arguments
/Applications/Eclipse.app/Contents/MacOS/eclipse -data ~/workspace

# Or use open command
open -a Eclipse --args -data ~/workspace
```

## Troubleshooting

### Command Not Found

Ensure eclipse is in your PATH or use full path:

```bash
export PATH=$PATH:/path/to/eclipse
eclipse
```

Or:
```bash
/path/to/eclipse/eclipse
```

### Wrong Java Version

Specify Java explicitly:

```bash
eclipse -vm /path/to/java17/bin/java
```

### Workspace Locked

Another instance is using the workspace:

```bash
# Use different workspace
eclipse -data ~/workspace2

# Or remove lock file (ensure no other instance running)
rm ~/workspace/.metadata/.lock
```

### Bundle Resolution Failures

Start clean and with console:

```bash
eclipse -clean -console -consoleLog
```

Then diagnose in console:
```
ss
diag <bundle-id>
```

## Additional Resources

### Related Documentation

- [Headless JUnit Testing](headless_junit_testing.html)
- [p2 Admin UI](p2_admin_ui.html)
- [Equinox Launcher](equinox_launcher.html)
- [Startup Issues](startup_issues.html)

### External Links

- [Eclipse Runtime Options](https://help.eclipse.org/latest/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html)
- [OSGi Console Commands](https://help.eclipse.org/latest/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html)

## Quick Reference

### Essential Commands

```bash
# Basic start
eclipse

# Specify workspace
eclipse -data /path/to/workspace

# Clean start
eclipse -clean

# With console
eclipse -console -consoleLog

# Headless
eclipse -nosplash -application org.eclipse.ant.core.antRunner

# Debug mode
eclipse -debug -consoleLog

# Custom Java
eclipse -vm /path/to/java
```

### Common Issues

| Problem | Solution |
|---------|----------|
| Workspace locked | Close other instances or remove `.lock` file |
| Wrong Java | Use `-vm` to specify correct Java |
| Bundle won't resolve | Start with `-clean` and check with `-console` |
| Out of memory | Increase `-Xmx` in eclipse.ini or via `-vmargs` |
| No console output | Use `-consoleLog` or `eclipsec.exe` on Windows |

## See Also

- [Equinox Launcher](equinox_launcher.html)
- [Startup Issues](startup_issues.html)
- [Launcher Issues](launcher_issues.html)
- [Headless JUnit Testing](headless_junit_testing.html)
- [p2 Admin UI](p2_admin_ui.html)
