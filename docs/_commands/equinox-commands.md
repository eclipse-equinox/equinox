---
layout: post
title: Equinox Console Commands
summary: Core OSGi framework management commands
---

## Description

{{page.summary}}

The Equinox Console provides a comprehensive set of commands for managing and inspecting OSGi bundles and the framework. These commands are available in the `equinox` scope and cover bundle lifecycle management, service inspection, system properties, and diagnostics.

## Command Categories

### Controlling the OSGi Framework

- **close** - Shutdown and exit the framework
- **exit** - Exit immediately (System.exit)
- **gc** - Perform a garbage collection
- **init** - Uninstall all bundles
- **setprop** `<key>=<value>` - Set an OSGi property

### Controlling Bundles

- **install** `<url>` - Install a bundle from the given URL
- **start** `(<id>|<location>)` - Start the specified bundle(s)
- **stop** `(<id>|<location>)` - Stop the specified bundle(s)
- **uninstall** `(<id>|<location>)` - Uninstall the specified bundle(s)
- **update** `(<id>|<location>)` - Update the specified bundle(s)
- **refresh** `[(<id>|<location>)]` - Refresh the packages of the specified bundles

### Displaying Status

- **bundle** `(<id>|<location>)` - Display details for the specified bundle(s)
- **bundles** `[filter]` - Display details for all installed bundles
- **ss** `[filter]` - Display installed bundles (short status)
- **status** `[filter]` - Display installed bundles and registered services
- **headers** `(<id>|<location>)` - Print bundle headers
- **packages** `[<pkgname>|<id>|<location>]` - Display imported/exported package details
- **services** `[filter]` - Display registered service details
- **props** - Display system properties
- **getprop** `[name]` - Display system properties with the given name, or all if no name specified
- **threads** `[action] [thread]` - Display threads and thread groups

### Start Level Commands

- **sl** `[(<id>|<location>)]` - Display the start level for the specified bundle or framework
- **setfwsl** `<start_level>` - Set the framework start level
- **setbsl** `<start_level> (<id>|<location>)` - Set the start level for the specified bundle(s)
- **setibsl** `<start_level>` - Set the initial bundle start level

### Eclipse Runtime Commands

- **diag** `[<id>|<location>]` - Display unsatisfied constraints for the specified bundle(s)
- **enableBundle** `(<id>|<location>)` - Enable the specified bundle(s)
- **disableBundle** `(<id>|<location>)` - Disable the specified bundle(s)
- **disabledBundles** - List disabled bundles in the system

### Other Commands

- **exec** `<command>` - Execute a command in a separate process and wait
- **fork** `<command>` - Execute a command in a separate process (don't wait)

## Common Abbreviations

Many commands have short aliases for convenience:
- **b** - alias for `bundle`
- **h** - alias for `headers`
- **i** - alias for `install`
- **p** - alias for `packages`
- **pr** - alias for `props`
- **r** - alias for `refresh`
- **s** - alias for `status`
- **se** - alias for `services`
- **setp** - alias for `setprop`
- **sta** - alias for `start`
- **sto** - alias for `stop`
- **t** - alias for `threads`
- **un** - alias for `uninstall`
- **up** - alias for `update`

## Examples

### List all installed bundles (short status)

```
g! ss
Framework is launched.

id	State       Bundle
0	ACTIVE      org.eclipse.osgi_3.21.0
1	ACTIVE      org.eclipse.equinox.console_1.4.800
```

### Start a bundle

```
g! start 5
```

### Display bundle details

```
g! bundle 1
org.eclipse.equinox.console_1.4.800 [1]
  Id=1, Status=ACTIVE
  ...
```

### View services with filter

```
g! services (objectClass=org.osgi.service.*)
```

### Display thread information

```
g! threads
```

### Set framework start level

```
g! setfwsl 6
```

### Diagnose bundle problems

```
g! diag 5
```

## Notes

- Bundle identifiers can be specified using either numeric ID or symbolic location
- Filter syntax follows LDAP filter specification (RFC 1960)
- Many commands accept multiple bundles as arguments
- Use the `help` command to get detailed information about specific commands

**Related links:**

- [Eclipse Console Shell](https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fconsole_shell.htm)
- [Maven Central](https://mvnrepository.com/artifact/org.eclipse.platform/org.eclipse.equinox.console)
- [Source code](https://github.com/eclipse-equinox/equinox/blob/master/bundles/org.eclipse.equinox.console/src/org/eclipse/equinox/console/commands/EquinoxCommandProvider.java)

