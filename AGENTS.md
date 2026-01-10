# Eclipse Equinox - GitHub Copilot Agent Guide

This document provides guidance for GitHub Copilot coding agents working with the Eclipse Equinox repository.

## Repository Overview

Eclipse Equinox is an implementation of the OSGi R6+ core framework specification, providing:
- OSGi core framework implementation (reference implementation)
- Implementation of various OSGi services (Log, Configuration Admin, Metatype, Preferences, User Admin, Event Admin, Coordinator)
- Native launchers and executables for Eclipse-based applications
- Infrastructure for running OSGi-based systems

## Repository Structure

```
equinox/
├── bundles/              # OSGi bundles (66+ bundles)
│   ├── org.eclipse.osgi/           # Core OSGi framework implementation
│   ├── org.eclipse.equinox.*/      # Various Equinox service implementations
│   └── *.tests/                    # Test bundles
├── features/             # Eclipse feature definitions
│   └── org.eclipse.equinox.executable.feature/  # Native launcher feature
├── launcher-binary-parent/  # Parent POM for native binaries
├── releng/              # Release engineering artifacts
├── docs/                # Documentation (Jekyll-based site)
├── .github/workflows/   # GitHub Actions CI/CD
├── pom.xml              # Root Maven POM (Tycho-based build)
└── Jenkinsfile          # Jenkins CI configuration
```

### Key Bundles
- **org.eclipse.osgi**: Core OSGi framework implementation (most important)
- **org.eclipse.equinox.launcher**: Java launcher code
- **org.eclipse.equinox.cm**: Configuration Admin service
- **org.eclipse.equinox.event**: Event Admin service
- **org.eclipse.equinox.preferences**: Preferences service
- **org.eclipse.equinox.registry**: Extension registry
- **org.eclipse.equinox.common**: Common utilities

## Build System

### Technology Stack
- **Build Tool**: Maven 3.9.12+ with Tycho (Eclipse's Maven plugin for OSGi)
- **Java Version**: Java 21 (required for building), supports Java 8, 17, 21 runtime via toolchains
- **Parent POM**: Inherits from `org.eclipse:eclipse-platform-parent`
- **Module Structure**: Multi-module Maven build with profiles

### Important Build Profiles

1. **full-build** (default): Builds all bundles and features
2. **tck**: Runs OSGi Technology Compatibility Kit tests
3. **build-individual-bundles**: For building individual bundles
4. **javadoc**: Generate javadoc

### Environment Requirements

**Critical**: The build system requires:
- **Java 21** as JAVA_HOME (Maven extensions require Java 21)
- **Maven 3.9.11** or later
- **Git** with full history (for jgit timestamp provider)

For native launcher builds, additional requirements:
- **Linux**: `libgtk-3-dev` package
- **Windows**: Visual Studio 2022 with C++ compiler
- **macOS**: Xcode Command Line Tools

### Building the Project

#### Full Build (without tests)
```bash
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64  # or your Java 21 installation
mvn clean verify -DskipTests=true --batch-mode
```

#### Build with Tests
```bash
mvn clean verify --batch-mode
```

#### Build Individual Bundle
```bash
cd bundles/org.eclipse.equinox.cm
mvn clean verify -Pbuild-individual-bundles
```

#### Run OSGi TCK Tests for a Bundle
```bash
cd bundles/org.eclipse.equinox.cm
mvn clean verify -Pbuild-individual-bundles -Ptck
```

#### Run All TCK Tests
```bash
mvn clean verify --batch-mode -Pbuild-individual-bundles -Ptck -Dskip-default-modules=true -fn
```

### Build Dependencies

The build depends on a separate repository for native binaries:
- **Repository**: https://github.com/eclipse-equinox/equinox.binaries
- **Location**: Expected at `../equinox.binaries` relative to this repo
- **Environment Variable**: `EQUINOX_BINARIES_LOC` or `equinox.binaries.loc` property

## Testing

### Test Structure
- **Unit Tests**: Located in `*.tests` bundles (e.g., `org.eclipse.osgi.tests`)
- **TCK Tests**: OSGi Technology Compatibility Kit tests (run with `-Ptck` profile)
- **Test Framework**: JUnit 4 and JUnit 5 (vintage engine for legacy tests)

### Running Tests

**All Tests**:
```bash
mvn clean verify --batch-mode
```

**Specific Test Bundle**:
```bash
cd bundles/org.eclipse.equinox.preferences.tests
mvn clean verify -Pbuild-individual-bundles
```

**TCK Tests Only**:
```bash
mvn clean verify -Ptck -Pbuild-individual-bundles -Dskip-default-modules=true
```

### Test Reports
- Test results: `**/target/surefire-reports/*.xml`
- TCK results: `**/target/tck-results/TEST-*.xml`

## Development Environment

### Eclipse IDE Setup
The project includes Oomph setup for automatic IDE configuration:
- Use Eclipse Installer with the provided setup configuration
- See: https://www.eclipse.org/setups/installer/

### Maven Toolchains
The build uses Maven toolchains to support multiple Java versions:
- JavaSE-1.8 (Java 8)
- JavaSE-17 (Java 17)
- JavaSE-21 (Java 21)

Toolchains allow building with Java 21 while targeting older Java versions.

## Common Workflows

### Adding a New Bundle
1. Create bundle directory under `bundles/`
2. Add `pom.xml` with appropriate parent reference
3. Add bundle manifest in `META-INF/MANIFEST.MF` or use `bnd.bnd`
4. Add module to parent `pom.xml` in `full-build` profile if appropriate
5. Build and test the bundle individually first

### Modifying Existing Code
1. Locate the relevant bundle under `bundles/`
2. Make code changes
3. Build the specific bundle: `mvn clean verify -Pbuild-individual-bundles`
4. Run tests if available
5. Consider running TCK tests if modifying core framework

### Working with Native Launchers
Native launcher source code is in `features/org.eclipse.equinox.executable.feature/`.
- See `features/org.eclipse.equinox.executable.feature/README.md` for detailed build instructions
- Build scripts: `library/<platform>/build.sh` or `build.bat`
- Requires platform-specific compilers and libraries

## CI/CD

### GitHub Actions Workflows
- **build.yml**: Main build workflow (Linux, Windows, macOS)
- **unit-tests.yml**: Comprehensive test execution
- **codeql.yml**: Security analysis
- **tck**: OSGi TCK compliance testing
- **pr-checks.yml**: Pull request validation
- **copilot-setup-steps.yml**: Copilot agent environment setup

### Jenkins
The project also uses Jenkins CI (see `Jenkinsfile`) for:
- Native binary builds on multiple platforms
- Release builds
- Integration with Eclipse infrastructure

## Known Issues and Workarounds

### 1. Java Version Requirement
**Issue**: Build fails with "class file version 65.0" error if using Java 17 or earlier.

**Cause**: Maven extensions (Tycho pomless) require Java 21.

**Solution**: Always use Java 21 as JAVA_HOME:
```bash
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
```

### 2. Missing Parent POM
**Issue**: Build fails with "Could not find artifact org.eclipse:eclipse-platform-parent" or "'parent.relativePath' points at wrong local POM".

**Cause**: Parent POM is from eclipse-platform-parent repository. The pom.xml has `<relativePath>../eclipse-platform-parent</relativePath>` which expects the parent repository to be cloned alongside this one.

**Solution**: 
- For SNAPSHOT versions: Clone eclipse-platform-parent repository alongside equinox:
```bash
cd /path/to/workspace
git clone https://github.com/eclipse-platform/eclipse.platform.releng.git eclipse-platform-parent
```
- For released versions: Maven should auto-download from eclipse repositories. If issues persist, ensure network connectivity to:
  - https://repo.eclipse.org/content/repositories/eclipse/

**Note**: This is a common setup for Eclipse projects that share a parent POM.

### 3. Native Binary Dependencies
**Issue**: Native launcher tests fail or native binaries are missing.

**Cause**: Missing equinox.binaries repository.

**Solution**: Clone equinox.binaries repository:
```bash
cd /path/to/workspace
git clone https://github.com/eclipse-equinox/equinox.binaries.git
# Set environment variable
export EQUINOX_BINARIES_LOC=/path/to/workspace/equinox.binaries
```

### 4. Git History Required
**Issue**: Build warnings about "Using 'none' timestamp provider".

**Cause**: Shallow git clone without history.

**Solution**: Use full clone (not shallow):
```bash
git clone --depth 0 https://github.com/eclipse-equinox/equinox.git
# Or if already cloned shallow:
git fetch --unshallow
```

### 5. Platform-Specific Build Issues

**Linux - GTK Missing**:
```bash
sudo apt-get update
sudo apt-get install libgtk-3-dev
```

**Windows - Compiler Missing**:
- Install Visual Studio 2022 Community Edition
- Include "Desktop development with C++" workload

**macOS - Xcode Missing**:
```bash
xcode-select --install
```

## Code Style and Conventions

### Java Code
- Follow Eclipse coding conventions
- Use tabs for indentation (per .settings in bundles)
- Maximum line length: 120 characters (generally)
- Javadoc required for public APIs

### OSGi Manifest Headers
- Use semantic versioning
- Properly declare Import-Package and Export-Package
- Use bnd.bnd where available for manifest generation

### Commit Messages
- Reference issue numbers: "Bug 12345 - Description"
- Use "Signed-off-by" for Eclipse Contributor Agreement compliance

## Debugging Tips

### Enable Tycho Debug Output
```bash
mvn clean verify -X -Dtycho.debug.resolver=true
```

### Skip Tests During Development
```bash
mvn clean install -DskipTests=true
```

### Build Only Changed Modules
```bash
mvn clean verify -pl :bundle-name -am
```

### View Effective POM
```bash
mvn help:effective-pom
```

## Important Files and Locations

- **pom.xml**: Root build configuration
- **bundles/org.eclipse.osgi/pom.xml**: Core framework bundle configuration
- **.github/workflows/**: CI/CD pipeline definitions
- **releng/tcks.target**: Target platform for TCK tests
- **CONTRIBUTING.md**: Contribution guidelines
- **LICENSE**: Eclipse Public License 2.0
- **Jenkinsfile**: Jenkins pipeline configuration

## External Resources

- **Homepage**: https://equinox.eclipseprojects.io/
- **Bug Tracker**: https://github.com/eclipse-equinox/equinox/issues
- **Discussions**: https://github.com/eclipse-equinox/equinox/discussions
- **OSGi Specifications**: https://docs.osgi.org/
- **Eclipse Contributing**: https://www.eclipse.org/projects/handbook/

## Quick Reference Commands

```bash
# Set Java 21 (REQUIRED)
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64

# Full clean build
mvn clean verify --batch-mode

# Build without tests (faster)
mvn clean verify -DskipTests=true --batch-mode

# Build single bundle
cd bundles/<bundle-name>
mvn clean verify -Pbuild-individual-bundles

# Run TCK tests
cd bundles/<bundle-name>
mvn clean verify -Pbuild-individual-bundles -Ptck

# Check for dependency issues
mvn dependency:tree

# Format code (if formatter configured)
mvn tycho-code-format:format-code

# Clean all build artifacts
mvn clean
```

## Tips for Efficient Development

1. **Start Small**: Build and test individual bundles before full builds
2. **Use Profiles**: Leverage Maven profiles to control what gets built
3. **Cache Dependencies**: Maven's local repository caches dependencies (~/.m2/repository)
4. **Parallel Builds**: Use `--threads 1C` for parallel module builds (use cautiously)
5. **Skip Unchanged**: Maven's incremental build usually handles this automatically
6. **Read Logs**: Tycho provides detailed OSGi resolution logs when issues occur
7. **Check Workflows**: GitHub Actions workflows show working build configurations

## Making Changes

When making changes to this repository:

1. **Understand the Impact**: Equinox is a reference implementation, changes affect the entire OSGi ecosystem
2. **Test Thoroughly**: Run TCK tests for any framework changes
3. **Maintain Compatibility**: API/behavior changes need careful review
4. **Update Tests**: Add or update tests for new functionality
5. **Check All Platforms**: Changes should work on Linux, Windows, and macOS
6. **Follow Process**: Sign commits (ECA) and reference issues

## Getting Help

- Check existing issues and discussions on GitHub
- Review OSGi specifications for standardized behavior
- Ask on eclipse-equinox mailing list: equinox-dev@eclipse.org
- Check Eclipse Platform documentation for related functionality
