---
layout: post
title: Boot Delegation
summary: Understanding boot delegation in Equinox OSGi Framework and how to configure class loading from the boot classpath
---

* The generated Toc will be an ordered list
{:toc}

## Overview

Boot delegation in the Equinox OSGi Framework controls which classes can be loaded directly from the parent (boot) classloader, bypassing the normal OSGi class loading mechanism. This feature is particularly important when dealing with JDK classes and framework extensions.

In a standard OSGi environment, each bundle has its own classloader and explicitly declares its dependencies through Import-Package or Require-Bundle headers. However, there are scenarios where bundles need access to classes from the boot classpath (JDK classes, VM-provided classes) without explicitly importing them.

## How Boot Delegation Works

When a bundle attempts to load a class, the Equinox framework follows the OSGi delegation model:

1. **Check java.\* packages** - These are always delegated to the parent classloader
2. **Boot delegation check** - If the package is in the boot delegation list, attempt to load from parent classloader
3. **Search imported packages** - Load from explicitly imported packages
4. **Search required bundles** - Load from required bundles
5. **Search local bundle** - Search within the bundle itself
6. **Dynamic imports** - Attempt dynamic package imports
7. **Compatibility boot delegation** - As a fallback, try parent classloader again (if enabled)

The boot delegation mechanism operates at step 2, allowing early delegation to the parent classloader for specified packages.

## Configuration Properties

### org.osgi.framework.bootdelegation

The primary property for configuring boot delegation is `org.osgi.framework.bootdelegation`. This property accepts a comma-separated list of package names that should be delegated to the parent classloader.

**Syntax:**
```
org.osgi.framework.bootdelegation=package1,package2.*,package3.subpackage
```

**Examples:**

Delegate specific package:
```
org.osgi.framework.bootdelegation=sun.reflect
```

Delegate package and all subpackages (using wildcard):
```
org.osgi.framework.bootdelegation=sun.reflect.*
```

Delegate all packages:
```
org.osgi.framework.bootdelegation=*
```

Delegate multiple packages:
```
org.osgi.framework.bootdelegation=sun.reflect,sun.misc,com.sun.*
```

**Wildcard Rules:**

- `package.*` - Matches the package and all its subpackages (e.g., `sun.reflect.*` matches `sun.reflect.Reflection` and `sun.reflect.generics.Reflector`)
- `package` - Matches only the exact package (e.g., `sun.reflect` matches only classes directly in `sun.reflect`)
- `*` - Matches all packages (use with caution as it can bypass OSGi modularization)

### osgi.compatibility.bootdelegation

The `osgi.compatibility.bootdelegation` property provides backward compatibility for older applications that relied on implicit boot delegation.

**Values:**
- `true` - Enable compatibility mode (try parent classloader as last resort)
- `false` - Disable compatibility mode (default for embedded frameworks)

**Default behavior:**
- When running Eclipse from the launcher: typically defaults to `true`
- When embedding Equinox programmatically: defaults to `false`

When enabled, this property allows bundles to load classes from the boot classpath even if they don't explicitly import them and the class is not in the boot delegation list. This happens as a last resort after all other class loading mechanisms have been attempted.

**Example in config.ini:**
```properties
osgi.compatibility.bootdelegation=false
```

**Example programmatically:**
```java
Map<String, Object> configuration = new HashMap<>();
configuration.put("osgi.compatibility.bootdelegation", "true");
Equinox equinox = new Equinox(configuration);
```

### osgi.context.bootdelegation

The `osgi.context.bootdelegation` property controls whether boot delegation is enabled when loading classes from the context classloader.

**Values:**
- `true` - Enable context boot delegation (default)
- `false` - Disable context boot delegation

**Default:** `true`

This property is particularly useful when dealing with thread context classloaders and class loading that happens outside the normal bundle classloader chain.

### osgi.java.profile.bootdelegation

This property controls how the `org.osgi.framework.bootdelegation` property defined in Java profiles should be processed.

**Values:**
- `ignore` - Ignore boot delegation from Java profile (default)
- `override` - Java profile boot delegation overrides system property
- `none` - Ignore boot delegation from both Java profile and system properties

**Default:** `ignore`

## Use Cases

### Loading JDK Internal Classes

Some applications need access to internal JDK classes that are not part of the standard exported API.

```properties
org.osgi.framework.bootdelegation=sun.misc,sun.reflect.*,com.sun.jndi.*
```

**Note:** Relying on internal JDK classes is discouraged as they may change or be removed in future Java versions. Use with caution and consider alternatives.

### Framework Extensions

When using framework extensions that add classes to the system bundle, boot delegation may be needed to ensure proper access.

### Testing Scenarios

During testing, you may want to allow bundles to access test framework classes from the boot classpath:

```properties
org.osgi.framework.bootdelegation=org.junit.*,org.hamcrest.*
```

### Legacy Application Migration

When migrating legacy applications to OSGi, boot delegation can provide a temporary bridge:

```properties
osgi.compatibility.bootdelegation=true
```

However, this should be a temporary measure. The long-term solution is to properly modularize the application with correct Import-Package declarations.

## Configuration Methods

### Using config.ini

In an Eclipse installation, edit the `configuration/config.ini` file:

```properties
org.osgi.framework.bootdelegation=sun.reflect,sun.misc.*
osgi.compatibility.bootdelegation=false
```

### Programmatic Configuration

When embedding Equinox:

```java
Map<String, Object> configuration = new HashMap<>();
configuration.put(Constants.FRAMEWORK_BOOTDELEGATION, "sun.reflect,sun.misc.*");
configuration.put("osgi.compatibility.bootdelegation", "false");

Equinox equinox = new Equinox(configuration);
equinox.start();
```

### System Properties

Boot delegation can also be configured via system properties:

```bash
java -Dorg.osgi.framework.bootdelegation=sun.reflect,sun.misc.* -jar org.eclipse.osgi.jar
```

### Runtime Arguments

When starting Eclipse:

```bash
eclipse -vmargs -Dorg.osgi.framework.bootdelegation=sun.reflect.*
```

## Best Practices

### Minimize Boot Delegation

**Recommendation:** Use boot delegation sparingly. Over-reliance on boot delegation defeats the purpose of OSGi modularization.

**Why:** Boot delegation bypasses the explicit dependency management that makes OSGi valuable. It can lead to:
- Hidden dependencies
- Version conflicts
- Reduced portability
- Harder debugging

### Prefer Explicit Imports

Instead of relying on boot delegation, bundles should explicitly declare their dependencies:

```
Import-Package: sun.misc;resolution:=optional
```

This makes dependencies explicit and allows the OSGi framework to properly manage versions and resolve conflicts.

### Use Specific Packages

Avoid using wildcards unnecessarily:

**Good:**
```properties
org.osgi.framework.bootdelegation=sun.misc.Unsafe
```

**Acceptable:**
```properties
org.osgi.framework.bootdelegation=sun.misc.*
```

**Avoid:**
```properties
org.osgi.framework.bootdelegation=*
```

Using `*` delegates all packages to the boot classloader, which can cause:
- Performance issues
- Unexpected class loading behavior
- Difficulty in debugging class loading problems

### Document Why Boot Delegation Is Needed

When using boot delegation, document the reason in your configuration:

```properties
# Required for legacy JNDI integration
org.osgi.framework.bootdelegation=com.sun.jndi.*
```

### Test Without Boot Delegation First

During development, test your bundles without boot delegation to identify missing imports:

```properties
osgi.compatibility.bootdelegation=false
```

This helps identify missing Import-Package declarations that should be added to your manifest.

## Troubleshooting

### ClassNotFoundException Despite Boot Delegation

**Symptom:** Bundle throws `ClassNotFoundException` even though the package is in boot delegation list.

**Possible causes:**

1. **Typo in package name** - Verify the package name exactly matches
2. **Class not in boot classpath** - The class must be available in the parent classloader
3. **Incorrect wildcard** - Ensure you're using `package.*` not just `package*`

**Solution:**
- Verify the package is correctly specified in `org.osgi.framework.bootdelegation`
- Check that the class is actually available in the JDK or boot classpath
- Enable boot delegation debugging (see Debugging section)

### Conflicting Class Versions

**Symptom:** Wrong version of a class is loaded when boot delegation is enabled.

**Possible cause:** Boot delegation loads the class from the parent classloader instead of the version bundled with your application.

**Solution:**
- Remove the package from boot delegation
- Import the package explicitly to use the bundled version
- Use a fragment bundle to override boot classpath classes

### Performance Issues

**Symptom:** Slow class loading or startup time.

**Possible cause:** Using `*` for boot delegation causes all class loading to attempt parent delegation first.

**Solution:**
- Replace `*` with specific packages
- Remove unnecessary packages from boot delegation list
- Profile class loading to identify bottlenecks

### Unexpected Class Loading Behavior

**Symptom:** Classes are loaded from unexpected locations.

**Possible cause:** `osgi.compatibility.bootdelegation=true` allows fallback to boot classloader.

**Solution:**
- Set `osgi.compatibility.bootdelegation=false` to enforce strict OSGi class loading
- Add explicit Import-Package declarations
- Use `bundle <id>` command in OSGi console to inspect bundle wiring

## Debugging

### Enable OSGi Debug Tracing

To debug boot delegation issues, enable OSGi debug tracing:

**In config.ini:**
```properties
osgi.debug=true
osgi.debug.loader=true
```

**Create .options file:**
```properties
org.eclipse.osgi/debug=true
org.eclipse.osgi/debug/loader=true
org.eclipse.osgi/debug/classloader=true
```

Then start with:
```bash
eclipse -debug .options
```

### Use OSGi Console

Start Eclipse with the OSGi console to inspect bundle wiring:

```bash
eclipse -console
```

**Useful commands:**

- `ss` - Show all bundles and their states
- `bundle <id>` - Show detailed information about a bundle
- `diag <id>` - Diagnose why a bundle is not resolved
- `packages <package>` - Show which bundles provide/import a package

### Test Boot Delegation Configuration

To test if a package is in the boot delegation list:

1. Create a minimal test bundle that doesn't import the package
2. Try to load the class from the package
3. If it loads, boot delegation is working; if not, check configuration

**Example test code:**
```java
try {
    Class<?> clazz = bundle.loadClass("sun.misc.Unsafe");
    System.out.println("Boot delegation working for sun.misc");
} catch (ClassNotFoundException e) {
    System.out.println("Boot delegation not configured for sun.misc");
}
```

## Implementation Details

The boot delegation mechanism in Equinox is implemented in the `BundleLoader` class. When a bundle attempts to load a class, the framework:

1. Extracts the package name from the fully qualified class name
2. Checks if the package matches any entry in the boot delegation list (using exact match or wildcard matching)
3. If matched, attempts to load the class from the parent classloader
4. If parent classloader throws `ClassNotFoundException`, continues with normal OSGi delegation
5. As a fallback (if `osgi.compatibility.bootdelegation=true`), tries parent classloader again after all other mechanisms fail

The boot delegation list is configured during framework initialization and is compiled into efficient data structures:
- Exact package matches are stored in a `HashSet` for O(1) lookup
- Wildcard patterns (package stems) are stored in an array for pattern matching
- The special wildcard `*` is handled as a boolean flag for efficiency

## Migration from Non-OSGi Applications

When migrating existing Java applications to OSGi, you may encounter class loading issues. Here's a migration strategy:

### Phase 1: Get It Working

1. Enable compatibility boot delegation:
   ```properties
   osgi.compatibility.bootdelegation=true
   ```

2. Add broad boot delegation if needed:
   ```properties
   org.osgi.framework.bootdelegation=sun.*,com.sun.*
   ```

### Phase 2: Identify Dependencies

1. Set `osgi.compatibility.bootdelegation=false`
2. Note which classes fail to load
3. Determine if they're:
   - JDK classes that should be imported
   - Internal classes that should be avoided
   - Classes that need boot delegation

### Phase 3: Proper Modularization

1. Add explicit Import-Package declarations for JDK classes
2. Remove or replace usage of internal JDK classes
3. Minimize boot delegation to only truly necessary packages
4. Remove `osgi.compatibility.bootdelegation` setting

### Phase 4: Remove Boot Delegation

The final goal is to eliminate boot delegation entirely by:
- Using only standard, exported JDK packages
- Properly declaring all dependencies in manifests
- Avoiding internal APIs

## Related OSGi Concepts

### Parent Classloader

Boot delegation relies on the parent classloader, which is typically the application classloader that loaded the OSGi framework. This classloader has access to:
- JDK classes (java.*, javax.*, etc.)
- Classes on the application classpath
- Framework extension classes

### Framework Extensions

Framework extensions are fragments to the system bundle that can add classes to the framework classpath. These classes are available through boot delegation.

### Class Loading Order

Understanding OSGi class loading order is crucial for using boot delegation effectively:

1. java.* packages (always boot delegated)
2. Boot delegation packages (if configured)
3. Imported packages (explicit dependencies)
4. Required bundles (Require-Bundle dependencies)
5. Local bundle classes (Bundle-ClassPath)
6. Dynamic imports (DynamicImport-Package)
7. Hooks and buddy policy
8. Compatibility boot delegation (if enabled)

### Import-Package vs Boot Delegation

| Aspect | Import-Package | Boot Delegation |
|--------|---------------|-----------------|
| Explicitness | Explicit dependency | Implicit dependency |
| Version control | Supports version ranges | No version control |
| OSGi visibility | Follows OSGi rules | Bypasses OSGi |
| Best practice | Recommended | Use sparingly |
| Performance | Optimized | Additional lookup overhead |

## Examples

### Example 1: Minimal Boot Delegation

Configure only essential internal classes:

**config.ini:**
```properties
org.osgi.framework.bootdelegation=sun.misc.Unsafe
osgi.compatibility.bootdelegation=false
```

### Example 2: Testing Configuration

Allow test frameworks to be boot delegated for easier test setup:

**config.ini for testing:**
```properties
org.osgi.framework.bootdelegation=org.junit.*,org.hamcrest.*,org.mockito.*
osgi.compatibility.bootdelegation=false
```

### Example 3: Legacy Application

Temporary configuration for legacy application migration:

**config.ini:**
```properties
org.osgi.framework.bootdelegation=sun.*,com.sun.*,javax.xml.ws.*
osgi.compatibility.bootdelegation=true
```

### Example 4: Embedded Equinox

Configure boot delegation when embedding Equinox programmatically:

```java
import org.eclipse.osgi.launch.Equinox;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

import java.util.HashMap;
import java.util.Map;

public class EmbeddedEquinoxExample {
    public static void main(String[] args) throws Exception {
        Map<String, Object> config = new HashMap<>();
        
        // Configure storage location
        config.put(Constants.FRAMEWORK_STORAGE, "./osgi-cache");
        
        // Configure boot delegation
        config.put(Constants.FRAMEWORK_BOOTDELEGATION, "sun.reflect,sun.misc.*");
        
        // Disable compatibility boot delegation for strict OSGi behavior
        config.put("osgi.compatibility.bootdelegation", "false");
        
        // Create and start framework
        Framework framework = new Equinox(config);
        framework.start();
        
        // Use framework...
        
        framework.stop();
        framework.waitForStop(0);
    }
}
```

### Example 5: Debugging Boot Delegation

Enable detailed logging to understand class loading:

**config.ini:**
```properties
org.osgi.framework.bootdelegation=sun.misc.*
osgi.debug=true
eclipse.consoleLog=true
```

**.options:**
```properties
org.eclipse.osgi/debug=true
org.eclipse.osgi/debug/loader=true
org.eclipse.osgi/debug/classloader=true
org.eclipse.osgi/debug/loader/findClass=true
```

## References

### OSGi Specification

The boot delegation behavior is specified in the OSGi Core Specification:
- Section 3.8: Class Loading Architecture
- Section 3.9: Resource Loading

### Equinox Implementation

Key classes in the Equinox implementation:
- `org.eclipse.osgi.internal.framework.EquinoxContainer` - Initializes boot delegation configuration
- `org.eclipse.osgi.internal.loader.BundleLoader` - Implements class loading with boot delegation
- `org.eclipse.osgi.internal.framework.EquinoxConfiguration` - Manages configuration properties

### Related Properties

Other class loading related properties:
- `osgi.parentClassloader` - Controls parent classloader selection (boot, ext, app, fwk)
- `osgi.framework.extensions` - Specifies framework extension bundles
- `osgi.context.bootdelegation` - Controls context classloader boot delegation

## See Also

- [OSGi Core Specification](https://docs.osgi.org/specification/) - Official OSGi specifications
- [Equinox Framework](https://www.eclipse.org/equinox/) - Eclipse Equinox home page
- [Where Is My Bundle](Where_Is_My_Bundle.html) - Debugging bundle resolution issues
- [Adaptor Hooks](Adaptor_Hooks.html) - Framework extension mechanisms
