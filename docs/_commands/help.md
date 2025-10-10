---
layout: post
title: help
summary: Provides help for available commands
---

## Description

{{page.summary}}

This command displays information about all available console commands, including their names, descriptions, and parameters. It can be used to get general help for all commands or specific help for a particular command.

## Parameters

- **No parameters** - Displays help for all available commands
- **command_name** - Displays help for a specific command
- **-scope <scope_name>** - Displays help only for commands in the specified scope

## Usage

```
help
help <command_name>
help -scope <scope_name>
```

## Examples

### Get help for all commands

```
g! help
```

This will list all available commands with their descriptions and parameters.

### Get help for a specific command

```
g! help wires

wires - Prints information about the wiring of a particular bundle
   scope: wiring
   parameters:
      long
```

### Get help for commands in a specific scope

```
g! help -scope equinox
```

This will display only commands that belong to the "equinox" scope.

**Related links:**

- [Eclipse Console Shell](https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fconsole_shell.htm)
- [Maven Central](https://mvnrepository.com/artifact/org.eclipse.platform/org.eclipse.equinox.console)
- [Source code](https://github.com/eclipse-equinox/equinox/blob/master/bundles/org.eclipse.equinox.console/src/org/eclipse/equinox/console/commands/HelpCommand.java)

