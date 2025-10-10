---
layout: post
title: Equinox Launcher Plan
summary: Development plans and roadmap for the Equinox Launcher
---

* The generated Toc will be an ordered list
{:toc}

## Overview

This page outlines the development plans, ongoing improvements, and future direction for the Equinox Launcher. The launcher is a critical component that provides the entry point for Eclipse and Equinox-based applications.

## Current Status

The Equinox Launcher is a mature, stable component that has been in production use for many years. It consists of:

- **Native executable**: Platform-specific binaries for Windows, Linux, and macOS
- **Java launcher code**: `org.eclipse.equinox.launcher` bundle
- **Native libraries**: Platform-specific JNI libraries in launcher fragments

## Recent Improvements

### Java Version Support

- Updated minimum Java version requirements to align with Eclipse platform requirements
- Enhanced JVM detection to handle newer Java versions
- Improved error messaging when incompatible Java versions are detected

### Platform Support

- Added support for Apple Silicon (ARM64) on macOS
- Added support for Linux ARM64 (aarch64)
- Added support for Linux RISC-V 64-bit
- Continued support for existing platforms (x86_64, Windows ARM64)

### Build Infrastructure

- Modernized native build scripts
- Improved cross-compilation support
- Updated compiler toolchains (Visual Studio 2022, modern GCC)
- Better integration with Maven/Tycho build system

### Security Enhancements

- macOS notarization for improved security and user experience
- Code signing improvements
- Better handling of security policies on various platforms

## Ongoing Work

### Performance Improvements

**Goals**:
- Reduce startup time
- Optimize native library loading
- Improve JVM detection performance
- Reduce memory footprint

**Approach**:
- Profile startup sequence to identify bottlenecks
- Optimize native code
- Cache JVM detection results where appropriate

### Enhanced Error Reporting

**Goals**:
- Provide clearer error messages
- Better diagnostics for common issues
- Improved logging and debugging capabilities

**Approach**:
- Categorize common error scenarios
- Provide actionable error messages with suggested solutions
- Add structured logging for easier troubleshooting

### Platform Modernization

**Goals**:
- Support latest operating system versions
- Adapt to platform changes (e.g., macOS security, Windows store)
- Remove deprecated platform dependencies

**Approach**:
- Regular updates for new OS versions
- Testing on preview/beta OS releases
- Proactive API migration for deprecated functionality

## Future Plans

### Launcher Architecture

#### Modularization

**Goal**: Make the launcher more modular and extensible.

**Proposed Changes**:
- Better separation between launcher and framework initialization
- Pluggable JVM selection strategies
- Extensible command-line argument processing

#### Native Code Reduction

**Goal**: Reduce platform-specific native code where possible.

**Approach**:
- Leverage Java features that replace native functionality
- Use JDK APIs instead of custom native implementations
- Maintain native code only where necessary for platform integration

### Command-Line Interface

#### Enhanced Argument Processing

**Goal**: Improve command-line argument handling.

**Plans**:
- Support for modern CLI patterns (e.g., `--long-options`)
- Better validation and error reporting
- Consistent behavior across platforms

#### Configuration File Improvements

**Goal**: Enhance `eclipse.ini` processing.

**Ideas**:
- Support for comments and documentation in eclipse.ini
- Include mechanism for shared configurations
- Better validation and error reporting

### Cross-Platform Consistency

**Goal**: Ensure consistent behavior across all supported platforms.

**Areas**:
- Argument parsing
- Path handling
- Environment variable processing
- Error messages

### Development Experience

#### Improved Build System

**Goals**:
- Simplify building native components
- Better IDE integration
- Streamlined release process

**Approach**:
- Further Maven/Tycho integration
- Automated testing of native builds
- Containerized build environments

#### Testing Infrastructure

**Goals**:
- Comprehensive automated testing
- Platform-specific test coverage
- Performance benchmarking

**Plans**:
- Expand `org.eclipse.equinox.launcher.tests`
- Add integration tests for native launcher
- Automated testing on all supported platforms
- Performance regression testing

## Technical Debt

### Areas for Cleanup

1. **Legacy Code**: Some code dates back to early Eclipse versions
   - Gradual refactoring while maintaining compatibility
   - Remove unused code paths

2. **Platform-Specific Code**: Consolidate where possible
   - Identify common patterns across platforms
   - Create shared abstractions

3. **Documentation**: Keep documentation up-to-date
   - Document internal architecture
   - Improve code comments
   - Update user-facing documentation

## Platform-Specific Plans

### Windows

- Continued support for Windows 10 and 11
- ARM64 Windows support improvements
- Better high-DPI support
- Windows Store compatibility (if applicable)

### Linux

- Support for emerging architectures (RISC-V, etc.)
- Better Wayland support
- Compatibility with newer GTK versions
- Snap/Flatpak packaging considerations

### macOS

- Continued Apple Silicon improvements
- Adapt to macOS security changes
- Support for latest Xcode and SDK versions
- App Store compatibility considerations

## Contribution Guidelines

The Equinox Launcher welcomes contributions:

### Areas for Contribution

1. **Platform Support**: Help test and improve support for specific platforms
2. **Bug Fixes**: Address issues in the bug tracker
3. **Documentation**: Improve user and developer documentation
4. **Testing**: Add test coverage
5. **Performance**: Identify and fix performance issues

### How to Contribute

1. Check the [issue tracker](https://github.com/eclipse-equinox/equinox/issues)
2. Discuss significant changes in GitHub Discussions or issues first
3. Follow the [contribution guidelines](https://github.com/eclipse-equinox/equinox/blob/master/CONTRIBUTING.md)
4. Submit pull requests with:
   - Clear description of changes
   - Test coverage where applicable
   - Documentation updates

## Deprecation Policy

The launcher maintains compatibility with older Eclipse versions while moving forward:

- **Native APIs**: Maintain binary compatibility where possible
- **Command-Line Arguments**: Deprecated arguments supported with warnings
- **Java Versions**: Follow Eclipse platform requirements
- **Platform Support**: Announce end-of-life well in advance

## Version History

The launcher version corresponds to the Eclipse release it ships with:

- **Eclipse 4.x**: Major redesign with native launcher
- **Eclipse 3.x**: Original launcher implementation
- **Future**: Continuous evolution with Eclipse platform

## References

- [Source Code](https://github.com/eclipse-equinox/equinox/tree/master/bundles/org.eclipse.equinox.launcher)
- [Native Build Scripts](https://github.com/eclipse-equinox/equinox/tree/master/features/org.eclipse.equinox.executable.feature)
- [Issue Tracker](https://github.com/eclipse-equinox/equinox/issues)
- [Eclipse Equinox Project](https://www.eclipse.org/equinox/)

## See Also

- [Equinox Launcher](equinox_launcher.html)
- [Startup Issues](startup_issues.html)
- [Launcher Issues](launcher_issues.html)
- [Building the Launcher](https://github.com/eclipse-equinox/equinox/tree/master/features/org.eclipse.equinox.executable.feature)
