---
layout: post
title: Equinox Launcher Issues
summary: Known issues and limitations of the Equinox Launcher
---

* The generated Toc will be an ordered list
{:toc}

## Overview

This page documents known issues, limitations, and platform-specific behaviors of the Equinox Launcher.

## Known Issues

### Command-Line Argument Parsing

**Issue**: Special characters in command-line arguments may not be parsed correctly on some platforms.

**Impact**: Arguments containing spaces, quotes, or special characters may be split or interpreted incorrectly.

**Workaround**:
- Use `eclipse.ini` for complex arguments instead of command line
- Quote arguments properly for your shell
- Avoid special characters when possible

### VM Detection Order

**Issue**: The launcher searches for a JVM in a specific order that may not match user expectations.

**Search Order**:
1. `-vm` specified in command line
2. `-vm` specified in `eclipse.ini` (must be before `-vmargs`)
3. JVM in `jre/` subdirectory of Eclipse installation
4. JVM specified by `JAVA_HOME` environment variable
5. JVM in system PATH

**Workaround**: Explicitly specify the JVM with `-vm` in `eclipse.ini` for predictable behavior.

### Memory Settings in eclipse.ini

**Issue**: `-vmargs` must be the last option in `eclipse.ini`. Any arguments after `-vmargs` are passed to the JVM, not to Eclipse.

**Impact**: Incorrectly placed arguments are ignored or cause errors.

**Example of correct usage**:
```
-vm
/path/to/java
--launcher.defaultAction
openFile
-vmargs
-Xms512m
-Xmx2048m
```

### Splash Screen on Multiple Monitors

**Issue**: On multi-monitor setups, the splash screen may appear on an unexpected monitor.

**Workaround**: 
- Use `-nosplash` to disable splash screen
- No direct control over splash screen positioning is available

### Native Library Loading

**Issue**: The launcher may fail to load native libraries if they're missing or incompatible.

**Common Causes**:
- Architecture mismatch (32-bit vs 64-bit)
- Missing system dependencies (e.g., GTK on Linux)
- File permission issues
- Antivirus/security software blocking libraries

**Workaround**: Ensure all required system libraries are installed and accessible.

## Platform-Specific Issues

### Windows

#### Console Output

**Issue**: When launching Eclipse from the command line on Windows, console output may not be visible.

**Reason**: `eclipse.exe` is built as a Windows GUI application, not a console application.

**Workaround**:
- Use `eclipsec.exe` (console version) if available
- Use `-consoleLog` to write output to a file
- Redirect output: `eclipse.exe > output.txt 2>&1`

#### Long Path Names

**Issue**: Windows path length limitations (260 characters) can cause issues with deeply nested workspaces.

**Workaround**:
- Keep Eclipse installation path short
- Use short workspace paths
- Enable long path support in Windows 10/11 (Group Policy or Registry)

#### Windows Defender

**Issue**: Windows Defender or other antivirus software may slow down or interfere with Eclipse startup.

**Workaround**: Add Eclipse directory to antivirus exclusions.

#### DPI Scaling

**Issue**: On high-DPI displays, Eclipse may appear blurry or have scaling issues.

**Workaround**: Add to `eclipse.ini`:
```
-vmargs
-Dswt.autoScale=200
```

### Linux

#### GTK Version

**Issue**: Eclipse requires GTK 3, which may not be installed on all systems.

**Error**: "Cannot load SWT libraries"

**Workaround**: Install GTK 3 development libraries:
```bash
# Ubuntu/Debian
sudo apt-get install libgtk-3-0

# Fedora/RHEL
sudo dnf install gtk3
```

#### File Handle Limits

**Issue**: Large workspaces may exceed file handle limits on Linux.

**Symptoms**: "Too many open files" errors

**Workaround**: Increase file handle limit:
```bash
ulimit -n 4096
```

Make permanent by editing `/etc/security/limits.conf`.

#### Wayland Display Server

**Issue**: Some features may not work correctly under Wayland.

**Workaround**: Force X11:
```bash
GDK_BACKEND=x11 ./eclipse
```

Or add to `eclipse.ini`:
```
-vmargs
-Dorg.eclipse.swt.internal.gtk.disableWayland=true
```

#### Launcher Permissions

**Issue**: Launcher executable may not have execute permissions after extraction.

**Workaround**:
```bash
chmod +x eclipse
```

### macOS

#### Gatekeeper

**Issue**: macOS Gatekeeper prevents Eclipse from launching (unsigned or unnotarized builds).

**Error**: "Eclipse.app cannot be opened because the developer cannot be verified"

**Workaround**:
- Right-click Eclipse.app and select "Open"
- Or remove quarantine attribute:
  ```bash
  xattr -cr Eclipse.app
  ```

#### Quarantine Attribute

**Issue**: Downloaded applications are marked with quarantine attribute, which can cause issues.

**Workaround**:
```bash
xattr -cr Eclipse.app
```

#### Touch Bar Support

**Issue**: Limited Touch Bar support on MacBooks with Touch Bar.

**Status**: Known limitation, ongoing improvements.

#### Notarization

**Issue**: Non-notarized builds may have issues on macOS Catalina and later.

**Workaround**: Use official Eclipse builds which are notarized.

#### Apple Silicon (M1/M2)

**Issue**: Native ARM64 builds may have platform-specific issues; x86_64 builds run through Rosetta 2.

**Recommendations**:
- Use native ARM64 builds when available
- Ensure Rosetta 2 is installed if using x86_64 builds:
  ```bash
  softwareupdate --install-rosetta
  ```

## Configuration Issues

### eclipse.ini Not Read

**Issue**: Changes to `eclipse.ini` appear to have no effect.

**Possible Causes**:
1. Editing the wrong `eclipse.ini` file
2. Syntax errors in `eclipse.ini`
3. Using wrong line endings (should be platform-specific)
4. eclipse.ini cached by launcher

**Verification**:
- Start with `-debug` to see which configuration is used
- Check for syntax errors (one argument per line)
- Ensure file is in the same directory as launcher executable
- On macOS: `Eclipse.app/Contents/Eclipse/eclipse.ini`

### Configuration Area Conflicts

**Issue**: Multiple Eclipse instances or versions conflict on configuration.

**Workaround**: Use separate configuration areas:
```bash
eclipse -configuration /path/to/config1
```

### Shared Configuration in Read-Only Location

**Issue**: Configuration in read-only location (e.g., system install) prevents updates.

**Workaround**: The launcher automatically creates a user-specific configuration in `~/.eclipse/` for configuration updates.

## Troubleshooting Steps

### 1. Start with Clean Configuration

```bash
eclipse -clean
```

This clears cached OSGi data and forces bundle re-resolution.

### 2. Enable Debug Output

```bash
eclipse -debug -consoleLog
```

### 3. Check Launcher Log

Look for `launcher.log` or output in console for launcher-specific errors.

### 4. Verify Java Compatibility

```bash
java -version
```

Ensure Java version meets Eclipse requirements.

### 5. Test with Minimal Configuration

Remove all third-party plugins and test with base Eclipse installation.

### 6. Check File Permissions

Ensure Eclipse has read/write permissions for:
- Installation directory (for writable installs)
- Configuration area
- Workspace
- Temp directory

## Reporting Issues

When reporting launcher issues, please include:

1. **Eclipse Version**: Help > About Eclipse IDE > Installation Details
2. **Java Version**: Output of `java -version`
3. **Operating System**: Version and architecture
4. **Launcher Command**: Exact command line used
5. **eclipse.ini Contents**: Complete file
6. **Console Output**: With `-consoleLog` enabled
7. **Error Messages**: Complete error text and stack traces
8. **Steps to Reproduce**: Minimal steps to recreate the issue

Submit issues to: [https://github.com/eclipse-equinox/equinox/issues](https://github.com/eclipse-equinox/equinox/issues)

## See Also

- [Equinox Launcher](equinox_launcher.html)
- [Startup Issues](startup_issues.html)
- [Starting Eclipse from Command Line](starting_eclipse_commandline.html)
- [Execution Environment Descriptions](execution_environment_descriptions.html)
