---
layout: post
title: Application Console Commands
summary: Commands for managing Eclipse applications
---

## Description

{{page.summary}}

The Equinox Application commands provide functionality for managing Eclipse applications, including listing, starting, stopping, scheduling, and locking applications. These commands are available through the console command provider in the `org.eclipse.equinox.app` bundle.

## Commands

### Listing Applications

- **apps** - Lists all installed application IDs with their status
- **activeApps** - Lists all running application IDs

### Application Lifecycle

- **startApp** `<application id> [args...]` - Starts the specified application ID with optional arguments
- **stopApp** `<application id>` - Stops the specified running application ID

### Application Locking

- **lockApp** `<application id>` - Locks the specified application ID (prevents launching new instances)
- **unlockApp** `<application id>` - Unlocks the specified application ID

### Application Scheduling

- **schedApp** `<application id> <time filter> [true|false]` - Schedules the specified application ID to launch at the specified time filter. Can optionally make the schedule recurring.
- **unschedApp** `<application id>` - Unschedules all scheduled applications with the specified application ID

## Examples

### List all installed applications

```
g! apps
org.eclipse.ui.ide.workbench [launchable]
org.eclipse.equinox.app.test [running] [launchable]
org.example.app [not launchable] [locked]
```

This displays all registered applications with their current state, showing whether they are:
- `[running]` - Currently executing
- `[scheduled]` - Scheduled to run at a specific time
- `[launchable]` - Can be launched
- `[not launchable]` - Cannot be launched
- `[locked]` - Locked and cannot launch new instances

### List all running applications

```
g! activeApps
org.eclipse.ui.ide.workbench.12345 [running]
org.eclipse.equinox.app.test.67890 [stopping]
```

This shows all active application instances with their instance IDs and current state (running or stopping).

### Start an application

```
g! startApp org.eclipse.ui.ide.workbench
Launched application instance: org.eclipse.ui.ide.workbench.12345
```

You can also pass arguments to the application:

```
g! startApp org.example.myapp arg1 arg2 arg3
Launched application instance: org.example.myapp.98765
```

### Stop a running application

```
g! stopApp org.eclipse.ui.ide.workbench.12345
Stopped application instance: org.eclipse.ui.ide.workbench.12345
```

You can stop an application by either its instance ID or its application ID.

### Lock an application

```
g! lockApp org.example.myapp
Locked application: org.example.myapp
```

Locking an application prevents new instances from being launched. It does not affect already running instances.

### Unlock an application

```
g! unlockApp org.example.myapp
Unlocked application: org.example.myapp
```

### Schedule an application

```
g! schedApp org.example.myapp "(hour=12)" false
Scheduled application: org.example.myapp
```

The second parameter is a time filter using LDAP filter syntax (RFC 1960). The third parameter indicates whether the schedule should be recurring (true) or one-time (false).

### Unschedule an application

```
g! unschedApp org.example.myapp
Unscheduled application: org.example.myapp
```

## Notes

- Application IDs can be partial matches - the command will find applications that contain the specified ID string
- If a partial ID matches multiple applications, an error will be reported indicating the ID is ambiguous
- Use the full application PID (Process ID) to avoid ambiguity
- Stopping an application by application ID will stop all running instances of that application
- Locked applications cannot launch new instances but existing instances continue to run

**Related links:**

- [OSGi Application Admin Service Specification](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.application.html)
- [Eclipse Console Shell](https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fconsole_shell.htm)
- [Source code](https://github.com/eclipse-equinox/equinox/blob/master/bundles/org.eclipse.equinox.app/src/org/eclipse/equinox/internal/app/AppCommands.java)

