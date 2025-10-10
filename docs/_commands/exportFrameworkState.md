---
layout: post
title: exportFrameworkState [path] [persistWirings]
summary: Exports the current framework state including the wiring information
---

## Description

{{page.summary}}

This command exports the current OSGi framework state to a file, which includes information about bundles and their wiring. This is useful for debugging, analysis, or creating snapshots of the framework's state at a particular point in time.

## Parameters

- **path** (required) - The file path where the framework state will be exported
- **persistWirings** (optional) - Boolean flag to control whether wiring information should be included (default: true)

## Usage

```
exportFrameworkState <path>
exportFrameworkState <path> <persistWirings>
```

## Examples

### Export framework state with wiring information

```
g! exportFrameworkState /tmp/framework-state.dat
Exporting ModuleDatabase to /tmp/framework-state.dat...
```

This will export the complete framework state including all wiring information to the specified file.

### Export framework state without wiring information

```
g! exportFrameworkState /tmp/framework-state-no-wiring.dat false
Exporting ModuleDatabase to /tmp/framework-state-no-wiring.dat...
```

This will export the framework state but exclude the wiring information, resulting in a smaller file.

## Use Cases

- **Debugging** - Capture the framework state when issues occur for later analysis
- **Testing** - Save framework states to reproduce specific scenarios
- **Documentation** - Export framework configuration for documentation purposes
- **Performance Analysis** - Compare framework states over time to identify changes

**Related links:**

- [Eclipse Console Shell](https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fconsole_shell.htm)
- [Maven Central](https://mvnrepository.com/artifact/org.eclipse.platform/org.eclipse.equinox.console)
- [Source code](https://github.com/eclipse-equinox/equinox/blob/master/bundles/org.eclipse.equinox.console/src/org/eclipse/equinox/console/commands/ExportStateCommand.java)

