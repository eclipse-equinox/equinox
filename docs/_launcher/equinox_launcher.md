---
layout: post
title: Equinox Launcher
summary: Overview of the Equinox Launcher and how to use it
---

* The generated Toc will be an ordered list
{:toc}

## Overview

The Equinox Launcher is the native executable and Java code that is responsible for starting the Equinox OSGi framework and Eclipse applications. The launcher provides a platform-specific executable (such as `eclipse.exe` on Windows or `eclipse` on Linux/Mac) along with native libraries that bootstrap the Java Virtual Machine and start the framework.

## Components

The Equinox Launcher consists of two main components:

### Native Executable

The native executable is a platform-specific binary file:
- **Windows**: `eclipse.exe`
- **Linux**: `eclipse`
- **macOS**: `Eclipse.app/Contents/MacOS/eclipse`

This executable:
- Locates and starts the appropriate Java Virtual Machine (JVM)
- Passes command-line arguments to the framework
- Manages the splash screen (if configured)
- Provides native integration with the operating system

### Launcher Bundle

The launcher bundle (`org.eclipse.equinox.launcher`) contains the Java code that:
- Initializes the OSGi framework
- Processes configuration files
- Manages the application lifecycle
- Handles command-line argument processing

The launcher also includes platform-specific fragments that provide native libraries for JNI integration.

## Command Line Arguments

The launcher accepts various command-line arguments to control its behavior:

### Common Arguments

- `-application <id>`: Specifies the application to run
- `-configuration <location>`: Specifies the configuration area location
- `-console [port]`: Opens the OSGi console (optionally on a specific port)
- `-consoleLog`: Logs console output to a file
- `-data <location>`: Sets the workspace location
- `-debug [options file]`: Enables debug mode with optional debug options
- `-dev [entries]`: Enables development mode with optional classpath entries
- `-clean`: Clears cached data before starting
- `-initialize`: Initializes the configuration without starting the application

### VM Arguments

- `-vm <path>`: Specifies the Java VM to use
- `-vmargs`: All arguments after this are passed directly to the JVM

### Display Arguments

- `-nosplash`: Disables the splash screen
- `-showlocation`: Shows the workspace location in the window title

## Configuration Files

### eclipse.ini

The `eclipse.ini` file (or `Eclipse.ini` on macOS) is located next to the launcher executable and contains default command-line arguments. Each argument should be on a separate line:

```
-startup
plugins/org.eclipse.equinox.launcher_<version>.jar
--launcher.library
plugins/org.eclipse.equinox.launcher.<ws>.<os>.<arch>_<version>
-product
org.eclipse.platform.ide
-showsplash
org.eclipse.epp.package.common
--launcher.defaultAction
openFile
-vmargs
-Dosgi.requiredJavaVersion=17
-Xms256m
-Xmx2048m
```

### config.ini

The `config.ini` file in the configuration area defines:
- OSGi bundles to install and start
- Framework properties
- Initial start levels
- System properties

Example:
```
osgi.bundles=org.eclipse.equinox.common@2:start,org.eclipse.update.configurator@3:start
osgi.bundles.defaultStartLevel=4
eclipse.product=org.eclipse.platform.ide
```

## Locations

The launcher uses several important locations:

### Install Location
The directory containing the Eclipse executable. This location is read-only in typical installations.

### Configuration Location
Contains configuration data including:
- `config.ini`: Framework configuration
- `org.eclipse.equinox.app`: Application state
- `.settings`: Platform preferences

Default: `<install>/configuration` or `<user.home>/.eclipse/<product-id>_<version>/configuration`

### Instance Location (Workspace)
The user's workspace containing projects and workspace-scoped settings.

Default: Prompted on first launch or specified via `-data`

### User Location
Contains user-specific data shared across workspaces.

Default: `<user.home>/.eclipse`

## Building the Launcher

For information on building the native launcher executable and libraries from source, see the [README in the executable feature](https://github.com/eclipse-equinox/equinox/tree/master/features/org.eclipse.equinox.executable.feature).

## Related Documentation

- [Startup Issues](startup_issues.html)
- [Launcher Issues](launcher_issues.html)
- [Launcher Plan](launcher_plan.html)
- [Execution Environment Descriptions](execution_environment_descriptions.html)
- [Starting Eclipse from Command Line](starting_eclipse_commandline.html)

## See Also

- [Main.java](https://github.com/eclipse-equinox/equinox/tree/master/bundles/org.eclipse.equinox.launcher/src/org/eclipse/equinox/launcher/Main.java) - The main launcher entry point
- [Equinox Framework](https://www.eclipse.org/equinox/)
