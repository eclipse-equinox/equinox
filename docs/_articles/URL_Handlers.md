---
layout: post
title: URL Handlers
summary: Documentation of the custom URL protocols supported by Equinox OSGi Framework for accessing bundle resources and installing bundles by reference
---

* The generated Toc will be an ordered list
{:toc}

## Overview

The Equinox OSGi Framework provides several custom URL protocols (URLStreamHandlers) that enable specialized operations within the OSGi environment. These URL handlers facilitate access to bundle resources and provide efficient mechanisms for installing and managing bundles.

This document describes the following URL protocols supported by Equinox:

- **bundleentry://** - Access entries directly from a bundle's storage
- **bundleresource://** - Access resources through a bundle's classloader
- **reference:** - Install bundles by reference without copying content

## Bundle Entry URL Protocol

### Protocol Name

`bundleentry://`

### Purpose

The `bundleentry://` protocol provides direct access to entries (files and directories) within a bundle's storage. This protocol uses the bundle's base storage location and retrieves entries directly from the bundle file, bypassing the classloader mechanism.

### URL Syntax

```
bundleentry://<bundle-id>[.<framework-id>][:<index>]/<path>
```

**Components:**

- **bundle-id** - The numeric ID of the bundle
- **framework-id** - (Optional) The hash code of the framework instance, used to distinguish between multiple framework instances
- **index** - (Optional) Resource index for classpath entries (default: 0)
- **path** - The path to the entry within the bundle

### Usage

The `bundleentry://` protocol is primarily used internally by the OSGi framework when calling `Bundle.getEntry()` or `Bundle.findEntries()`. These methods return URLs using the `bundleentry://` protocol to access files within a bundle.

**Example URLs:**

```
bundleentry://42/META-INF/MANIFEST.MF
bundleentry://42.fwk123456/plugin.xml
bundleentry://42:1/icons/sample.gif
```

### Behavior

- Accesses entries directly from the bundle's storage
- Does not involve the bundle's classloader
- Can access any file in the bundle, even those not on the classpath
- Supports both files and directories
- Returns `null` or throws `FileNotFoundException` if the entry doesn't exist

### Example Code

```java
Bundle bundle = context.getBundle(42);

// Get entry URL using bundleentry protocol
URL entryURL = bundle.getEntry("META-INF/MANIFEST.MF");
System.out.println(entryURL); // bundleentry://42.fwk123456/META-INF/MANIFEST.MF

// Open connection and read content
try (InputStream in = entryURL.openStream()) {
    // Read manifest content
    Manifest manifest = new Manifest(in);
}

// Find all entries matching a pattern
Enumeration<URL> entries = bundle.findEntries("/icons", "*.png", false);
while (entries.hasMoreElements()) {
    URL iconURL = entries.nextElement();
    // Each URL uses bundleentry:// protocol
    System.out.println(iconURL);
}
```

## Bundle Resource URL Protocol

### Protocol Name

`bundleresource://`

### Purpose

The `bundleresource://` protocol provides access to resources through a bundle's classloader. Unlike `bundleentry://`, this protocol respects the bundle's classpath configuration and follows OSGi class loading rules, including imported packages and fragment contributions.

### URL Syntax

```
bundleresource://<bundle-id>[.<framework-id>][:<index>]/<path>
```

**Components:**

- **bundle-id** - The numeric ID of the bundle
- **framework-id** - (Optional) The hash code of the framework instance
- **index** - (Optional) Classpath index indicating which classpath entry contains the resource
- **path** - The path to the resource

### Usage

The `bundleresource://` protocol is used internally when calling `Bundle.getResource()` or when loading resources through the bundle's classloader. This protocol searches for resources using the same mechanism as class loading.

**Example URLs:**

```
bundleresource://42/com/example/config.properties
bundleresource://42.fwk123456/templates/default.xml
bundleresource://42:2/resources/data.json
```

### Behavior

- Uses the bundle's classloader to locate resources
- Follows OSGi class loading rules and Import-Package declarations
- Can find resources in:
  - The bundle's own classpath (Bundle-ClassPath)
  - Attached fragment bundles
  - Imported packages (if delegated to providing bundle)
- Respects the bundle's classloader visibility rules
- The index parameter indicates which classpath entry contains the resource

### Difference from Bundle Entry

| Aspect | bundleentry:// | bundleresource:// |
|--------|---------------|-------------------|
| Access Method | Direct bundle storage | Bundle classloader |
| Scope | All bundle files | Classpath resources only |
| Fragment Support | No | Yes |
| Import-Package | No | Yes |
| Used By | Bundle.getEntry() | Bundle.getResource() |
| OSGi Visibility | No | Yes |

### Example Code

```java
Bundle bundle = context.getBundle(42);

// Get resource URL using bundleresource protocol
URL resourceURL = bundle.getResource("com/example/config.properties");
System.out.println(resourceURL); // bundleresource://42.fwk123456/com/example/config.properties

// Load properties from resource
Properties props = new Properties();
try (InputStream in = resourceURL.openStream()) {
    props.load(in);
}

// Get resource through classloader (same as bundle.getResource())
ClassLoader classLoader = bundle.adapt(BundleWiring.class).getClassLoader();
URL clResourceURL = classLoader.getResource("templates/default.xml");
// Returns bundleresource:// URL if found
```

## Reference URL Protocol

### Protocol Name

`reference:`

### Purpose

The `reference:` protocol enables installing bundles by reference rather than by copying their content. When a bundle is installed using a reference URL, the OSGi framework uses the bundle's files from their original location instead of copying them to the framework storage area. This is particularly useful during development and for large bundles.

### URL Syntax

```
reference:<file-url>
```

**Components:**

- **file-url** - A file URL pointing to the bundle location (JAR file or directory)

### Usage

The `reference:` protocol is used when installing bundles through `BundleContext.installBundle()`. The framework recognizes the `reference:` prefix and installs the bundle without copying its content.

**Important:** Only `file:` URLs are supported as the reference target. The framework validates that the referenced URL uses the `file:` protocol.

**Example URLs:**

```
reference:file:/eclipse/plugins/org.eclipse.equinox.common_3.18.0.jar
reference:file:/workspace/mybundle/target/mybundle_1.0.0.jar
reference:file:/development/plugins/org.example.plugin_1.0.0/
```

### Behavior

- The bundle content remains in its original location
- No copying of bundle files to framework storage
- Bundle is read directly from the reference location
- Changes to the bundle files at the reference location are reflected when the bundle is updated or restarted
- Only file URLs are supported (not http, https, etc.)
- The referenced location must be accessible throughout the bundle's lifecycle
- Reduces disk space usage and installation time
- Useful for development and testing scenarios

### Benefits

1. **Development Efficiency** - Changes to bundle content are immediately available after refresh/restart
2. **Reduced Disk Space** - Bundles are not duplicated in framework storage
3. **Faster Installation** - No file copying during bundle installation
4. **Easier Debugging** - Bundle files remain in their original, accessible location

### Considerations

- **File System Access** - The framework must have read access to the referenced location
- **Path Changes** - If the referenced path is moved or deleted, the bundle becomes unavailable
- **Security** - The framework verifies that the referenced location is a local file URL
- **Portability** - Installation locations are typically machine-specific

### Example Code

```java
BundleContext context = bundleActivator.getBundleContext();

// Install bundle by reference (no content copying)
String location = "reference:file:/eclipse/plugins/org.eclipse.equinox.common_3.18.0.jar";
Bundle bundle = context.installBundle(location);

// The bundle is installed but content remains at original location
System.out.println("Bundle installed: " + bundle.getSymbolicName());
System.out.println("Bundle location: " + bundle.getLocation());

// Install bundle by reference from a directory (exploded bundle)
String dirLocation = "reference:file:/development/plugins/mybundle_1.0.0/";
Bundle dirBundle = context.installBundle(dirLocation);

// For comparison: installing without reference (copies content)
String normalLocation = "file:/eclipse/plugins/org.eclipse.equinox.common_3.18.0.jar";
Bundle copiedBundle = context.installBundle(normalLocation);
// This bundle's content is copied to framework storage
```

### Development Workflow Example

A typical development workflow using reference URLs:

```java
// During development, install bundles by reference
String projectPath = "reference:file:/workspace/com.example.mybundle/target/classes/";
Bundle devBundle = context.installBundle(projectPath);
devBundle.start();

// Make changes to source code and rebuild
// ...

// Refresh the bundle to pick up changes
FrameworkWiring frameworkWiring = context.getBundle(0).adapt(FrameworkWiring.class);
frameworkWiring.refreshBundles(Collections.singleton(devBundle));

// Bundle now uses updated code from reference location
```

## Implementation Details

### URLStreamHandlerFactory

Equinox registers a custom `URLStreamHandlerFactory` that provides handlers for these custom protocols. The factory is implemented in the `org.eclipse.osgi.internal.url.URLStreamHandlerFactoryImpl` class.

### Handler Implementations

- **bundleentry://** - `org.eclipse.osgi.storage.url.bundleentry.Handler`
- **bundleresource://** - `org.eclipse.osgi.storage.url.bundleresource.Handler`
- **reference:** - `org.eclipse.osgi.storage.url.reference.Handler`

### Security

Access to bundle resources through `bundleentry://` and `bundleresource://` URLs is subject to OSGi security checks. If a SecurityManager is installed, the caller must have `AdminPermission[bundle,RESOURCE]` to access bundle resources.

### Framework Integration

These URL handlers are integral to the OSGi framework's operation:

- **Bundle Installation** - The `reference:` protocol is processed during `installBundle()`
- **Resource Access** - The bundle entry and resource protocols are returned by `Bundle.getEntry()` and `Bundle.getResource()`
- **Class Loading** - The `bundleresource://` protocol is used internally during class and resource loading

## Use Cases

### Use Case 1: Accessing Bundle Resources

```java
// Framework uses bundleresource:// for getResource()
URL config = bundle.getResource("config/settings.xml");
// Returns: bundleresource://23.fwk789012/config/settings.xml

// Framework uses bundleentry:// for getEntry()
URL manifest = bundle.getEntry("META-INF/MANIFEST.MF");
// Returns: bundleentry://23.fwk789012/META-INF/MANIFEST.MF
```

### Use Case 2: Development Environment

```java
// Install plugin under development by reference
String devBundle = "reference:file:/workspace/com.example.plugin/bin/";
Bundle bundle = context.installBundle(devBundle);
bundle.start();

// Make code changes, rebuild
// Refresh bundle to reload from reference location
frameworkWiring.refreshBundles(Collections.singleton(bundle));
```

### Use Case 3: Eclipse Plugin Development

Eclipse IDE uses reference URLs for plugins in the workspace:

```
reference:file:/workspace/org.example.plugin/target/classes/
```

This allows developers to:
1. Make changes to plugin code
2. Rebuild the plugin
3. Refresh the plugin in the running Eclipse instance
4. Test changes immediately without reinstalling

### Use Case 4: Programmatic Resource Access

```java
// Get all entries in a directory
Enumeration<URL> entries = bundle.findEntries("/icons", "*.png", true);
while (entries.hasMoreElements()) {
    URL iconURL = entries.nextElement();
    // bundleentry://42.fwk123456/icons/toolbar/save.png
    
    // Open and use the resource
    try (InputStream in = iconURL.openStream()) {
        Image icon = ImageIO.read(in);
        // Use icon...
    }
}
```

## Best Practices

### When to Use Reference URLs

**Recommended:**
- During development when frequently rebuilding bundles
- For large bundles where copying would be inefficient
- In testing environments where bundles may be updated frequently
- When disk space is limited

**Not Recommended:**
- In production deployments (use regular installation)
- When bundles need to be portable across systems
- When the reference location may not be stable

### URL Protocol Selection

The framework automatically selects the appropriate protocol based on the API used:

- Use `Bundle.getEntry()` when you need direct access to bundle files
- Use `Bundle.getResource()` when you need resources respecting OSGi visibility
- Use `reference:` prefix when installing bundles during development

### Handling URL Instances

```java
// URLs from the framework already use the correct protocol
URL entryURL = bundle.getEntry("file.txt");
URL resourceURL = bundle.getResource("file.txt");

// Don't construct these URLs manually
// Let the framework provide them through the bundle API

// If you need to open a connection
URLConnection conn = entryURL.openConnection();
try (InputStream in = conn.getInputStream()) {
    // Read content
}
```

## Troubleshooting

### Bundle Entry Not Found

**Symptom:** `FileNotFoundException` when accessing `bundleentry://` URL

**Possible Causes:**
- Entry path is incorrect (check case sensitivity)
- Entry does not exist in the bundle
- Bundle has been uninstalled or is not resolved

**Solution:**
```java
// Verify the entry exists first
URL entry = bundle.getEntry("path/to/file.txt");
if (entry != null) {
    // Entry exists, safe to open
    try (InputStream in = entry.openStream()) {
        // Process content
    }
}
```

### Bundle Resource Not Found

**Symptom:** `bundleresource://` URL returns null or throws exception

**Possible Causes:**
- Resource is not on the bundle's classpath (check Bundle-ClassPath)
- Resource is in an imported package not available to this bundle
- Bundle is not resolved or is missing required dependencies

**Solution:**
```java
// Check Bundle-ClassPath in MANIFEST.MF
// Verify the resource path is correct relative to classpath entries
URL resource = bundle.getResource("com/example/data.xml");
if (resource == null) {
    // Resource not found - check Bundle-ClassPath and package imports
    System.err.println("Resource not found on bundle classpath");
}
```

### Reference URL Installation Fails

**Symptom:** Exception when installing bundle with `reference:` URL

**Possible Causes:**
- Referenced URL is not a `file:` URL
- File path does not exist or is not accessible
- Path contains invalid characters or format

**Solution:**
```java
// Ensure reference URL uses file: protocol
String location = "reference:file:" + new File("/path/to/bundle.jar").getAbsolutePath();

// Verify the file exists before installing
File bundleFile = new File("/path/to/bundle.jar");
if (bundleFile.exists()) {
    Bundle bundle = context.installBundle(location);
} else {
    System.err.println("Bundle file not found: " + bundleFile);
}
```

### Security Exceptions

**Symptom:** `SecurityException` when accessing bundle resources

**Possible Cause:** SecurityManager is installed and caller lacks required permissions

**Solution:**
```java
// Ensure caller has AdminPermission[bundle,RESOURCE]
// Or run with appropriate security policy

// In security policy file:
// grant {
//     permission org.osgi.framework.AdminPermission "*", "resource";
// };
```

## Related Topics

- [OSGi Core Specification](https://docs.osgi.org/specification/) - URL Handlers Service Specification (Chapter 52)
- [Equinox Framework](https://www.eclipse.org/equinox/) - Eclipse Equinox home page
- [Boot Delegation](Boot_Delegation.html) - Understanding class and resource loading in Equinox

## References

### Source Code

The URL handler implementations can be found in the Equinox source code:

- `org.eclipse.osgi.storage.url.bundleentry.Handler` - Bundle entry protocol handler
- `org.eclipse.osgi.storage.url.bundleresource.Handler` - Bundle resource protocol handler  
- `org.eclipse.osgi.storage.url.reference.Handler` - Reference protocol handler
- `org.eclipse.osgi.internal.url.URLStreamHandlerFactoryImpl` - URL handler factory

### OSGi Specification

- OSGi Core Release 8, Chapter 52: URL Handlers Service Specification
- OSGi Core Release 8, Chapter 3.9: Resource Loading
