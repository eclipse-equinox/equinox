# Usage Examples for OS-Specific Directory Placeholders

This document provides practical examples of how to use the new `@user.data`, `@user.data.shared`, and `@user.documents` placeholders in your Eclipse-based applications.

## Basic Usage

### Example 1: User-Specific Workspace Location

Store the workspace in the user's application data directory:

**eclipse.ini:**
```ini
-startup
plugins/org.eclipse.equinox.launcher_*.jar
-data
@user.data/MyApp/workspace
-vmargs
-Xms256m
-Xmx1024m
```

**Result on different platforms:**
- **Windows:** `C:\Users\username\AppData\Roaming\MyApp\workspace`
- **Linux:** `/home/username/.MyApp/workspace` (note the dot prefix)
- **macOS:** `/Users/username/Library/Application Support/MyApp/workspace`

### Example 2: Shared Configuration Location

Store configuration that should be shared across all users:

**eclipse.ini:**
```ini
-configuration
@user.data.shared/MyCompanyApp/config
```

**Result on different platforms:**
- **Windows:** `C:\ProgramData\MyCompanyApp\config`
- **Linux:** `/srv/MyCompanyApp/config`
- **macOS:** `/Library/Application Support/MyCompanyApp/config`

### Example 3: Documents-Based Projects

Store projects in the user's documents folder:

**eclipse.ini:**
```ini
-data
@user.documents/MyProjects
```

**Result on different platforms:**
- **Windows:** `C:\Users\username\Documents\MyProjects`
- **Linux:** `/home/username/MyProjects`
- **macOS:** `/Users/username/Documents/MyProjects`

## Programmatic Usage

### Setting Properties at Runtime

You can also use these placeholders when setting OSGi properties programmatically:

```java
// Set the instance location using @user.data
System.setProperty("osgi.instance.area", "@user.data/myapp/workspace");

// Set the configuration location using @user.data.shared
System.setProperty("osgi.configuration.area", "@user.data.shared/myapp/config");
```

### Reading Resolved Paths

After Eclipse has resolved the placeholders, you can read the actual paths:

```java
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.BundleContext;

public class LocationExample {
    public void printLocations(BundleContext context) {
        // Get the instance location (workspace)
        Location instanceLocation = Platform.getInstanceLocation();
        if (instanceLocation != null) {
            System.out.println("Workspace: " + instanceLocation.getURL());
        }
        
        // Get the configuration location
        Location configLocation = Platform.getConfigurationLocation();
        if (configLocation != null) {
            System.out.println("Configuration: " + configLocation.getURL());
        }
    }
}
```

## Advanced Usage

### Example 4: Hybrid Approach

Combine multiple placeholders for different purposes:

**eclipse.ini:**
```ini
# User-specific workspace
-data
@user.data/MyApp/workspace

# Shared plugins/features
-configuration
@user.data.shared/MyApp/config

# User projects in documents
-Dproject.location=@user.documents/MyAppProjects
```

### Example 5: Multi-Instance Setup

Run multiple instances with separate data:

**Instance 1 - eclipse.ini:**
```ini
-data
@user.data/MyApp/instance1
```

**Instance 2 - eclipse.ini:**
```ini
-data
@user.data/MyApp/instance2
```

### Example 6: Portable Application with Fallback

Provide a portable option with fallback to user data:

**eclipse.ini:**
```ini
# Try to use portable mode if this file exists
-data
./workspace

# If above fails, use user-specific location
# (Note: Eclipse will use the first valid location)
```

Or configure programmatically:

```java
File portableWorkspace = new File("./workspace");
if (!portableWorkspace.exists() || !portableWorkspace.canWrite()) {
    // Fall back to user data directory
    System.setProperty("osgi.instance.area", "@user.data/myapp/workspace");
}
```

## Platform-Specific Considerations

### Windows

On Windows, the placeholders resolve to well-known Windows folders:
- `@user.data` uses the roaming profile, so data follows the user across machines in domain environments
- `@user.data.shared` requires administrator privileges to write to
- Use `@user.documents` for user-facing files that should appear in Windows Explorer

### Linux

On Linux, special handling is applied:
- `@user.data` automatically prefixes the first path segment with a dot (hidden directory convention)
- Example: `@user.data/myapp` becomes `~/.myapp`
- Only the first segment is hidden: `@user.data/myapp/subdir` becomes `~/.myapp/subdir`

### macOS

On macOS, the placeholders align with Apple's guidelines:
- `@user.data` uses the Library/Application Support directory (hidden from users by default)
- `@user.data.shared` uses the system-wide Library/Application Support
- `@user.documents` places files in the user's Documents folder (visible in Finder)

## Troubleshooting

### Placeholder Not Resolved

If the placeholder is not being resolved:

1. Check that you're using the correct syntax (must start with `@`)
2. Ensure the native launcher library is present and loaded
3. Check Eclipse log for any error messages
4. Verify the placeholder name is spelled correctly

### Permission Issues

If you encounter permission errors:

- For `@user.data`: Usually no special permissions needed
- For `@user.data.shared`: May require administrator/root privileges on some systems
- For `@user.documents`: Check that the Documents folder exists and is writable

### Debugging Path Resolution

Enable debug output to see how placeholders are resolved:

**eclipse.ini:**
```ini
-debug
-consoleLog
```

Then check the console output for messages about location resolution.

## Migration from Old Placeholders

If you're migrating from older placeholder usage:

### Before (using @user.home):
```ini
-data
@user.home/.myapp/workspace
```

### After (using @user.data):
```ini
-data
@user.data/myapp/workspace
```

**Advantages:**
- Follows OS conventions automatically
- Hidden directory on Linux without manual dot prefix
- Uses proper Application Support on macOS
- Uses AppData\Roaming on Windows instead of user home

## Best Practices

1. **Use @user.data for application data**: Store caches, logs, and internal state here
2. **Use @user.data.shared for templates and defaults**: Store read-only shared resources
3. **Use @user.documents for user files**: Only for files the user directly interacts with
4. **Always include your application name**: `@user.data/MyApp/...` to avoid conflicts
5. **Test on all platforms**: Placeholder resolution is platform-specific

## Compatibility

These placeholders are available starting from Eclipse Platform version X.Y.Z (TODO: update with actual version).

For backward compatibility with older Eclipse versions, consider:
- Providing fallback configuration using traditional paths
- Detecting Eclipse version at runtime and adjusting accordingly
- Documenting minimum required Eclipse version for your application
