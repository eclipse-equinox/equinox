---
layout: post
title: man
summary: Displays manual/help information for commands
---

## Description

{{page.summary}}

The `man` command is an alias for the `help` command, providing the same functionality with a Unix-like command name. It displays help information for available console commands.

## Parameters

- **No parameters** - Displays help for all available commands
- **command_name** - Displays help for a specific command or multiple commands

## Usage

```
man
man <command_name>
man <command_name1> <command_name2> ...
```

## Examples

### Get general help

```
g! man
```

This will list all available commands with their descriptions and parameters.

### Get help for a specific command

```
g! man wires

wires - Prints information about the wiring of a particular bundle
   scope: wiring
   parameters:
      long
```

### Get help for multiple commands

```
g! man wires disconnect
```

This will display help information for both the `wires` and `disconnect` commands.

**Related links:**

- [Eclipse Console Shell](https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fconsole_shell.htm)
- [Maven Central](https://mvnrepository.com/artifact/org.eclipse.platform/org.eclipse.equinox.console)
- [Source code](https://github.com/eclipse-equinox/equinox/blob/master/bundles/org.eclipse.equinox.console/src/org/eclipse/equinox/console/commands/ManCommand.java)

