---
layout: post
title: disconnect
summary: Disconnect from telnet or ssh console
---

## Description

{{page.summary}}

This command allows you to gracefully disconnect from a remote console session (telnet or ssh). When invoked, the command prompts for confirmation before closing the connection. The default action is to disconnect if no input is provided or if you enter 'y'.

## Parameters

This command takes no parameters.

## Usage

```
disconnect
```

## Example

Connect to a remote Equinox console via telnet or ssh, then use the disconnect command:

```
g! disconnect
Disconnect from console? (y/n; default=y) 
```

Press Enter or type 'y' to disconnect, or type 'n' to cancel:

```
g! disconnect
Disconnect from console? (y/n; default=y) y
[Connection closed]
```

**Related links:**

- [Eclipse Console Shell](https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fconsole_shell.htm)
- [Maven Central](https://mvnrepository.com/artifact/org.eclipse.platform/org.eclipse.equinox.console)
- [Source code](https://github.com/eclipse-equinox/equinox/blob/master/bundles/org.eclipse.equinox.console/src/org/eclipse/equinox/console/commands/DisconnectCommand.java)

