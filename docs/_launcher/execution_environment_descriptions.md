---
layout: post
title: Execution Environment Descriptions
summary: Understanding OSGi Execution Environments and Java compatibility
---

* The generated Toc will be an ordered list
{:toc}

## Overview

Execution Environments (EEs) are a key concept in OSGi that describe the capabilities of the Java runtime environment. They define which Java packages and classes are available to bundles and help ensure compatibility between bundles and the runtime environment.

## What is an Execution Environment?

An Execution Environment is a specification that:

- Defines the Java platform capabilities (Java SE 8, Java SE 17, etc.)
- Lists available system packages
- Specifies the Java version and capabilities
- Ensures bundle compatibility with the runtime

Bundles declare their required execution environment in their `MANIFEST.MF`:

```
Bundle-RequiredExecutionEnvironment: JavaSE-17
```

Or using Bundle-RequiredCapability (OSGi R7+):

```
Require-Capability: osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=17))"
```

## Standard Execution Environments

### Modern Java Environments

| Execution Environment | Java Version | Description |
|-----------------------|--------------|-------------|
| JavaSE-21 | Java SE 21 | Java SE 21 LTS |
| JavaSE-17 | Java SE 17 | Java SE 17 LTS (current Eclipse minimum) |
| JavaSE-11 | Java SE 11 | Java SE 11 LTS |
| JavaSE-1.8 | Java SE 8 | Java SE 8 LTS (deprecated in newer Eclipse) |

### Legacy Java Environments (Deprecated)

| Execution Environment | Java Version | Status |
|-----------------------|--------------|--------|
| JavaSE-1.7 | Java SE 7 | No longer supported |
| JavaSE-1.6 | Java SE 6 | No longer supported |
| J2SE-1.5 | Java SE 5 | No longer supported |
| J2SE-1.4 | Java SE 1.4 | No longer supported |
| J2SE-1.3 | Java SE 1.3 | No longer supported |

## Execution Environment Files

Execution Environment definitions are stored as `.ee` files in the Equinox framework. These files define:

- The environment name
- Java platform version
- Available system packages
- Provided capabilities

### EE File Format

Example `JavaSE-17.ee` structure:

```properties
# Java SE 17 Execution Environment
-Djava.specification.name=Java Platform API Specification
-Djava.specification.version=17
-Dosgi.java.profile.name=JavaSE-17

# System packages
org.osgi.framework.system.packages = \
 java.lang,\
 java.lang.annotation,\
 java.lang.invoke,\
 java.lang.module,\
 java.lang.ref,\
 java.lang.reflect,\
 java.io,\
 java.net,\
 java.nio,\
 java.util,\
 javax.net,\
 javax.net.ssl,\
 # ... more packages
```

### Locating EE Files

EE files are typically found in:
- Equinox framework bundle: `org.eclipse.osgi`
- Path: `org/eclipse/osgi/internal/framework/EquinoxConfiguration.java`
- Embedded in the OSGi framework implementation

## How Execution Environments Work

### Bundle Resolution

1. **Declaration**: Bundle declares required EE in manifest
2. **Framework Check**: Equinox verifies if the current JVM satisfies the requirement
3. **Package Availability**: Framework ensures required packages are available
4. **Resolution**: Bundle resolves only if EE requirements are met

### Multiple EE Support

Bundles can specify multiple acceptable execution environments:

```
Bundle-RequiredExecutionEnvironment: JavaSE-17, JavaSE-11
```

The bundle will run if any of the specified environments are available.

### Version Compatibility

Execution Environments follow Java's backward compatibility:
- JavaSE-17 satisfies requirements for JavaSE-11, JavaSE-1.8, etc.
- Bundles built for older EEs run on newer Java versions
- Bundles built for newer EEs may not run on older Java versions

## Configuring Execution Environments

### Setting the Execution Environment

The OSGi framework automatically detects the JVM's capabilities and configures the appropriate execution environment. However, you can override this:

#### Via System Property

```bash
eclipse -vmargs -Dosgi.java.profile=file:/path/to/JavaSE-17.profile
```

#### In config.ini

```properties
osgi.java.profile=file:/path/to/JavaSE-17.profile
```

### Custom Execution Environments

For testing or special scenarios, you can create custom EE files:

1. Create a `.ee` file with required packages and capabilities
2. Reference it via `osgi.java.profile` system property
3. Ensure all required packages are listed

## Execution Environment vs Java Version

### Key Differences

| Aspect | Execution Environment | Java Version |
|--------|----------------------|--------------|
| Scope | OSGi framework concept | JDK/JRE version |
| Purpose | Bundle compatibility | Runtime capabilities |
| Granularity | Package-level | Version-level |
| Flexibility | Can be customized | Fixed by JDK |

### Why Both Matter

- **Java Version**: Determines available language features and APIs
- **Execution Environment**: Determines which packages are visible to bundles
- Both must be compatible for proper operation

## Eclipse-Specific Considerations

### Minimum Java Requirements

Different Eclipse versions require different minimum Java versions:

| Eclipse Version | Minimum Java | Recommended Java |
|-----------------|--------------|------------------|
| 2024-03+ | Java 17 | Java 17 or 21 |
| 2022-09 to 2023-12 | Java 11 | Java 17 |
| 4.7 to 2022-06 | Java 8 | Java 11 or 17 |
| 4.6 and earlier | Java 7 or 8 | Java 8 |

### Setting Java Version for Eclipse

In `eclipse.ini`:

```
-vm
/path/to/java17/bin
-vmargs
-Dosgi.requiredJavaVersion=17
```

The `-Dosgi.requiredJavaVersion` property ensures Eclipse fails fast if launched with an incompatible Java version.

## Bundle Development Considerations

### Declaring Execution Environments

When developing bundles:

1. **Choose the lowest EE that meets your needs**:
   - Broader compatibility
   - More potential users
   
2. **Use appropriate language features**:
   - Match your code to your declared EE
   - Don't use Java 17 features if declaring JavaSE-11

3. **Test on multiple Java versions**:
   - Verify on minimum required version
   - Test on newer versions for compatibility

### Example Manifest

```
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: My Bundle
Bundle-SymbolicName: com.example.mybundle
Bundle-Version: 1.0.0
Bundle-RequiredExecutionEnvironment: JavaSE-11
Import-Package: org.osgi.framework;version="[1.8,2.0)"
```

### Build-Time Validation

Use tools to validate EE compliance:

- **PDE Build**: Validates against declared EE
- **bnd/bndtools**: Provides EE validation
- **Tycho**: Maven plugin for OSGi builds with EE checking

## Debugging Execution Environment Issues

### Common Problems

1. **Bundle Won't Resolve**:
   - Check required EE vs available EE
   - Use OSGi console: `diag <bundle-id>`

2. **ClassNotFoundException**:
   - Required package not in EE profile
   - Import-Package statement may be needed

3. **UnsupportedClassVersionError**:
   - Bundle compiled for newer Java than runtime
   - Update Java version or recompile bundle

### Diagnostic Commands

In the OSGi console:

```
# Show framework properties
getprop

# Look for:
osgi.java.profile
osgi.java.profile.name
java.specification.version

# Diagnose bundle resolution
diag <bundle-id>
```

### Verbose Logging

Enable resolver logging in `.options` file:

```
org.eclipse.osgi/resolver/debug=true
org.eclipse.osgi/resolver/ee=true
```

## Best Practices

### For Bundle Developers

1. **Declare the minimum required EE**: Don't over-specify
2. **Test on minimum supported Java version**: Ensure compatibility
3. **Use Import-Package for external dependencies**: Don't rely on EE for everything
4. **Document Java requirements**: In README and documentation
5. **Keep up with Java LTS releases**: Consider migrating to newer EEs

### For Application Developers

1. **Check bundle requirements**: Before adding dependencies
2. **Use consistent Java version**: Across development team
3. **Set minimum Java in eclipse.ini**: Use `-Dosgi.requiredJavaVersion`
4. **Test on target Java versions**: Don't assume compatibility
5. **Monitor Java EOL dates**: Plan migrations in advance

### For Framework Administrators

1. **Document required Java version**: For your deployment
2. **Monitor bundle EE requirements**: When updating dependencies
3. **Plan Java upgrades**: Before EOL dates
4. **Test thoroughly**: After Java version changes
5. **Use standard EEs**: Avoid custom profiles unless necessary

## Future of Execution Environments

### Java Module System (JPMS)

Java 9+ introduced the module system:
- Affects package visibility
- Requires careful EE definitions
- OSGi and JPMS can coexist

### Current Direction

- OSGi R8 and later use capabilities instead of EEs
- Transition from `Bundle-RequiredExecutionEnvironment` to `Require-Capability`
- More flexible and fine-grained requirements

### Migration Path

```
# Old style
Bundle-RequiredExecutionEnvironment: JavaSE-17

# New style
Require-Capability: osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=17))"
```

## References

- [OSGi Core Specification](https://docs.osgi.org/specification/) - Chapter on Execution Environments
- [Eclipse Wiki - Execution Environment](https://wiki.eclipse.org/Execution_Environments)
- [Java SE Specifications](https://docs.oracle.com/javase/specs/)
- [Equinox Framework](https://www.eclipse.org/equinox/)

## See Also

- [Equinox Launcher](equinox_launcher.html)
- [Startup Issues](startup_issues.html)
- [Starting Eclipse from Command Line](starting_eclipse_commandline.html)
