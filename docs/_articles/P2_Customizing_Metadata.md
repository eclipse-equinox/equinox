---
layout: post
title: P2 Customizing Metadata
summary: How to customize p2 metadata using advice files (p2.inf) for bundles, features, and products
---

* The generated Toc will be an ordered list
{:toc}

## Overview

The Eclipse p2 provisioning platform automatically generates metadata for installable units (IUs) from bundles, features, and products. However, sometimes the default metadata needs to be augmented or customized. This is accomplished using **advice files**, specifically `p2.inf` files.

An advice file is a Java properties file that contains key-value pairs providing additional instructions or metadata for an installable unit. These files allow you to:

- Define additional capabilities that an IU provides
- Specify additional requirements/dependencies
- Add custom properties
- Define touchpoint instructions for installation/uninstallation
- Create additional installable units related to the container
- Specify update descriptors

## Advice File Locations

Advice files can be placed in different locations depending on the type of artifact you're customizing:

### For Bundles
Place the advice file at:
```
META-INF/p2.inf
```

### For Features
Place the advice file alongside the `feature.xml`:
```
feature_directory/p2.inf
```

### For Products
Place the advice file alongside the `.product` file:
```
product_directory/p2.inf
```

## File Format

The `p2.inf` file uses the Java properties file format with key-value pairs:

```properties
# This is a comment
key = value
another.key = another value
```

Most properties use an indexing scheme with `<property>.<index>.<attribute>` to distinguish between multiple similar items:

```properties
provides.0.namespace = org.eclipse.equinox.p2.iu
provides.0.name = my.bundle
provides.1.namespace = java.package
provides.1.name = org.example.api
```

## Version Substitution Parameters

Two special parameters are available for version substitution:

- **`$version$`** - The complete version string of the containing installable unit
- **`$qualifier$`** - Only the qualifier portion of the version

Example:
```properties
units.0.version = $version$
units.0.update.range = [0.0.0,$version$)
```

If the bundle version is `1.2.3.v20231012`, then:
- `$version$` resolves to `1.2.3.v20231012`
- `$qualifier$` resolves to `v20231012`

## Capability Properties (Provides)

Use `provides` properties to define capabilities that the installable unit offers:

### Syntax
```properties
provides.<#>.namespace = <capability_namespace>
provides.<#>.name = <capability_name>
provides.<#>.version = <version>
```

### Attributes
- **namespace** (required) - The namespace of the capability
- **name** (required) - The name of the capability
- **version** (optional) - The version of the capability (default: 1.0.0)

### Example
```properties
# Provide a Java package
provides.0.namespace = java.package
provides.0.name = org.example.api
provides.0.version = 1.0.0

# Provide an IU capability
provides.1.namespace = org.eclipse.equinox.p2.iu
provides.1.name = my.custom.capability
```

## Requirement Properties (Requires)

Use `requires` properties to specify dependencies/capabilities that the installable unit needs:

### Syntax
```properties
requires.<#>.namespace = <required_namespace>
requires.<#>.name = <required_name>
requires.<#>.range = <version_range>
requires.<#>.greedy = <true|false>
requires.<#>.optional = <true|false>
requires.<#>.multiple = <true|false>
requires.<#>.filter = <LDAP_filter>
```

### Attributes
- **namespace** (required) - The namespace of the required capability
- **name** (required) - The name of the required capability
- **range** (optional) - The version range (default: 0.0.0 means any version)
- **greedy** (optional) - Whether the requirement is greedy (default: true)
- **optional** (optional) - Whether the requirement is optional (default: false)
- **multiple** (optional) - Whether multiple providers can satisfy (default: false)
- **filter** (optional) - LDAP filter for conditional requirements

### Examples

Basic requirement:
```properties
requires.0.namespace = org.eclipse.equinox.p2.iu
requires.0.name = org.eclipse.core.runtime
requires.0.range = [3.10.0,4.0.0)
```

Optional requirement:
```properties
requires.0.namespace = java.package
requires.0.name = javax.servlet
requires.0.greedy = false
requires.0.optional = true
```

Conditional requirement with filter:
```properties
requires.0.namespace = org.eclipse.equinox.p2.iu
requires.0.name = org.eclipse.swt.gtk.linux.x86_64
requires.0.range = [3.0.0,4.0.0)
requires.0.filter = (&(osgi.os=linux)(osgi.arch=x86_64))
```

## IU Properties

Add custom properties to the installable unit:

### Syntax
```properties
properties.<#>.name = <property_name>
properties.<#>.value = <property_value>
```

### Example
```properties
properties.0.name = org.eclipse.equinox.p2.name
properties.0.value = My Custom Bundle Name
properties.1.name = org.eclipse.equinox.p2.description
properties.1.value = A detailed description of this bundle
properties.2.name = org.eclipse.equinox.p2.provider
properties.2.value = Eclipse Foundation
```

## Additional Installable Units

You can define completely separate installable units related to the container IU:

### Syntax
```properties
units.<#>.id = <iu_id>
units.<#>.version = <version>
units.<#>.provides.<#>.namespace = <namespace>
units.<#>.provides.<#>.name = <name>
units.<#>.provides.<#>.version = <version>
units.<#>.requires.<#>.namespace = <namespace>
units.<#>.requires.<#>.name = <name>
units.<#>.requires.<#>.range = <version_range>
units.<#>.properties.<#>.name = <property_name>
units.<#>.properties.<#>.value = <property_value>
units.<#>.update.id = <update_id>
units.<#>.update.range = <version_range>
```

### Example

This example from the Equinox Executable feature creates a legacy compatibility IU:

```properties
# Define separate legacy IU org.eclipse.equinox.executable
units.0.id = org.eclipse.equinox.executable
units.0.version = $version$
units.0.properties.0.name = org.eclipse.equinox.p2.name
units.0.properties.0.value = org.eclipse.equinox.executable
units.0.update.id = org.eclipse.equinox.executable
units.0.update.range = [0.0.0,$version$)

units.0.provides.0.namespace = org.eclipse.equinox.p2.iu
units.0.provides.0.name = org.eclipse.equinox.executable
units.0.provides.0.version = $version$

units.0.requires.0.namespace = org.eclipse.equinox.p2.iu
units.0.requires.0.name = org.eclipse.equinox.executable.feature.group
units.0.requires.0.range = [$version$,$version$]
```

## Update Descriptor

Specify how updates should be handled for the installable unit:

### Syntax
```properties
update.id = <update_id>
update.range = <version_range>
update.severity = <0|1>
update.description = <description>
```

### Attributes
- **update.id** - The identifier for update purposes
- **update.range** - Version range indicating which versions this IU updates
- **update.severity** (optional) - 0 for normal, 1 for high severity
- **update.description** (optional) - Description of the update

### Example
```properties
update.id = my.bundle
update.range = [0.0.0,$version$)
update.severity = 0
```

## Touchpoint Instructions

Touchpoint instructions define actions to be performed during installation, uninstallation, configuration, or unconfiguration of an IU.

### Syntax
```properties
instructions.<phase> = <action1>;<action2>;...
instructions.<phase>.import = <imported_action_namespace>
```

### Phases
- **install** - Actions performed during installation
- **uninstall** - Actions performed during uninstallation
- **configure** - Actions performed during configuration
- **unconfigure** - Actions performed during unconfiguration

### Common Touchpoint Actions

The Eclipse touchpoint provides several built-in actions:

- **addJvmArg(jvmArg:<arg>)** - Add JVM argument
- **removeJvmArg(jvmArg:<arg>)** - Remove JVM argument
- **setProgramProperty(propName:<name>,propValue:<value>)** - Set program property
- **setStartLevel(startLevel:<level>)** - Set bundle start level
- **markStarted(started:<true|false>)** - Mark bundle as started
- **addRepository(location:<url>,type:<0|1>,enabled:<true|false>)** - Add repository
- **mkdir(path:<path>)** - Create directory
- **rmdir(path:<path>)** - Remove directory
- **copy(source:<src>,target:<dest>,overwrite:<true|false>)** - Copy file
- **remove(path:<path>)** - Remove file
- **chmod(targetDir:<dir>,targetFile:<file>,permissions:<perms>,options:<opts>)** - Change file permissions

### Examples

Add JVM arguments:
```properties
instructions.install = addJvmArg(jvmArg:-Xmx1024m);
instructions.install.import = org.eclipse.equinox.p2.touchpoint.eclipse.addJvmArg
```

Set bundle start level:
```properties
instructions.configure = setStartLevel(startLevel:2);markStarted(started:true);
```

Create directory and copy files:
```properties
instructions.install = mkdir(path:${installFolder}/custom);\
  copy(source:@artifact,target:${installFolder}/custom/config.xml,overwrite:true);
```

Set program properties:
```properties
instructions.configure = setProgramProperty(propName:eclipse.p2.profile,propValue:CustomProfile);
```

## Meta-Requirements

Meta-requirements specify conditions about the profile (installation environment) that must be met for an IU to be installed:

### Syntax
```properties
metaRequirements.<#>.namespace = <namespace>
metaRequirements.<#>.name = <name>
metaRequirements.<#>.range = <version_range>
metaRequirements.<#>.greedy = <true|false>
```

### Example
```properties
metaRequirements.0.namespace = org.eclipse.equinox.p2.iu
metaRequirements.0.name = org.eclipse.equinox.p2.ui.sdk.feature.group
metaRequirements.0.range = [0.0.0,1.0.0)
```

## Complete Examples

### Example 1: Bundle with Optional Dependencies

```properties
# Add optional servlet dependencies
requires.0.namespace = java.package
requires.0.name = javax.servlet
requires.0.greedy = false
requires.0.optional = true

requires.1.namespace = java.package
requires.1.name = javax.servlet.http
requires.1.greedy = false
requires.1.optional = true
```

### Example 2: Feature with Legacy IU

```properties
# Define legacy IU for backward compatibility
units.0.id = org.example.legacy.feature
units.0.version = $version$
units.0.update.id = org.example.legacy.feature
units.0.update.range = [0.0.0,$version$)

units.0.provides.0.namespace = org.eclipse.equinox.p2.iu
units.0.provides.0.name = org.example.legacy.feature
units.0.provides.0.version = $version$

units.0.requires.0.namespace = org.eclipse.equinox.p2.iu
units.0.requires.0.name = org.example.new.feature.feature.group
units.0.requires.0.range = [$version$,$version$]

units.0.properties.0.name = org.eclipse.equinox.p2.name
units.0.properties.0.value = Example Legacy Feature
```

### Example 3: Product with Custom Configuration

```properties
# Add custom properties
properties.0.name = org.eclipse.equinox.p2.name
properties.0.value = My Application
properties.1.name = org.eclipse.equinox.p2.description
properties.1.value = Enterprise application built on Eclipse RCP

# Configure JVM arguments
instructions.configure = addJvmArg(jvmArg:-Xms512m);addJvmArg(jvmArg:-Xmx2048m);
instructions.configure.import = org.eclipse.equinox.p2.touchpoint.eclipse.addJvmArg

# Add application-specific capabilities
provides.0.namespace = org.eclipse.equinox.p2.iu
provides.0.name = com.example.myapp.capability
provides.0.version = 1.0.0
```

### Example 4: Platform-Specific Requirements

```properties
# Require platform-specific SWT fragment
requires.0.namespace = org.eclipse.equinox.p2.iu
requires.0.name = org.eclipse.swt.gtk.linux.x86_64
requires.0.range = [3.0.0,4.0.0)
requires.0.filter = (&(osgi.os=linux)(osgi.arch=x86_64)(osgi.ws=gtk))
requires.0.greedy = true

requires.1.namespace = org.eclipse.equinox.p2.iu
requires.1.name = org.eclipse.swt.win32.win32.x86_64
requires.1.range = [3.0.0,4.0.0)
requires.1.filter = (&(osgi.os=win32)(osgi.arch=x86_64)(osgi.ws=win32))
requires.1.greedy = true
```

## Best Practices

### 1. Use Version Substitution
Always use `$version$` instead of hardcoding versions to ensure consistency:
```properties
units.0.version = $version$  # Good
# units.0.version = 1.0.0    # Bad - hardcoded
```

### 2. Make Optional Dependencies Non-Greedy
When adding optional requirements, set `greedy=false` to prevent unnecessary installations:
```properties
requires.0.optional = true
requires.0.greedy = false
```

### 3. Use Appropriate Version Ranges
Be specific with version ranges to avoid compatibility issues:
```properties
# Too broad - might pull in incompatible versions
requires.0.range = 0.0.0

# Better - specifies compatible range
requires.0.range = [3.0.0,4.0.0)
```

### 4. Document Complex Instructions
Use comments to explain non-obvious customizations:
```properties
# Legacy IU for backward compatibility with version 1.x installations
units.0.id = org.example.legacy
```

### 5. Test Thoroughly
After creating or modifying p2.inf files:
- Build the feature/bundle
- Verify the generated metadata in the p2 repository
- Test installation and updates
- Check that touchpoint instructions execute correctly

### 6. Use Filters for Platform-Specific Requirements
Leverage LDAP filters for OS/architecture-specific dependencies:
```properties
requires.0.filter = (&(osgi.os=linux)(osgi.arch=x86_64))
```

## Common Use Cases

### Adding Missing Dependencies
When the automatic metadata generation doesn't capture all dependencies:
```properties
requires.0.namespace = java.package
requires.0.name = org.xml.sax
```

### Creating Compatibility IUs
For maintaining backward compatibility with renamed features:
```properties
units.0.id = old.feature.name
units.0.requires.0.name = new.feature.name.feature.group
```

### Customizing Installation Behavior
To perform special setup during installation:
```properties
instructions.install = mkdir(path:${installFolder}/data);
```

### Setting Custom Properties
For UI display or metadata purposes:
```properties
properties.0.name = org.eclipse.equinox.p2.name
properties.0.value = Human-Readable Name
```

## Troubleshooting

### Metadata Not Applied
- Verify the p2.inf file is in the correct location
- Check the file encoding (should be ISO-8859-1 or UTF-8)
- Ensure property syntax is correct (key=value pairs)
- Rebuild the feature/bundle after making changes

### Version Substitution Not Working
- Verify you're using `$version$` (with dollar signs)
- Check that the container IU has a valid version
- Ensure no extra spaces around the parameter

### Touchpoint Instructions Failing
- Import the action namespace using `instructions.<phase>.import`
- Check action syntax and parameter names
- Verify paths exist or use appropriate actions to create them
- Review installation logs for error messages

## References

- [OSGi Bundle Manifest Headers](https://docs.osgi.org/specification/)
- [Eclipse p2 Repository Format](https://wiki.eclipse.org/Equinox/p2/Repository_Mirroring)
- [Equinox p2 Touchpoint Actions](https://wiki.eclipse.org/Equinox/p2/Engine/Touchpoint_Instructions)

## See Also

- [Equinox p2 Documentation](https://wiki.eclipse.org/Equinox/p2)
- [Building p2 Repositories](https://wiki.eclipse.org/Equinox/p2/Publisher)
- [p2 Director Application](https://wiki.eclipse.org/Equinox/p2/Director_Application)
