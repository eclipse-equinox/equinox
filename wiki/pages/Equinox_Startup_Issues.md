### Overview

As per
[bug 173742](https://bugs.eclipse.org/bugs/show_bug.cgi?id=173742), some
users are having issues when surrounding the move of the `startup.jar`
from the root of the Eclipse install to the `plugins/` directory. The
purpose of this page is to outline the reasons for the move, the
problems that people are having, and proposed solutions to these
problems.

### Background

One of the major downfalls of the Eclipse Update story is that it is not
completely updateable; users are not able to use update manager to
update between major releases. This was because the `startup.jar` was
not a real bundle and not versioned. By moving the `startup.jar` code to
the `plugins/` directory and making it a bundle, the update manager can
now update this code in future releases of Eclipse.

The Equinox launchers were also designed to address the startup
experience with respect to the splash screen.
[Bug 154088](https://bugs.eclipse.org/bugs/show_bug.cgi?id=154088) was
the plan item for this work. See also the wiki page on [Splash Screen
Improvements](Splash_Screen_Improvements "wikilink").

This work was previously outlined in [Equinox
Launcher](Equinox_Launcher "wikilink") and [Equinox Launcher
Plan](Equinox_Launcher_Plan "wikilink") as well as in messages sent to
the mailing lists and outlined weekly in the Eclipse Architecture
Meeting Minutes. It was first released to the Eclipse SDK builds in the
first integration build after Eclipse 3.3 M4. (the integration build
from December 19, 2006)

### Problem: Startup.jar

Clients with scripts which started Eclipse directly from Java (`java
-jar startup.jar`) must now be altered to point to the new JAR location.

### Possible Solutions

#### Do nothing

It is still completely possible to start eclipse using ` java -jar
 `<launcher>. The only change is that <launcher> is no longer called
startup.jar and it is no longer in the root. No longer being in the root
is not a significant issue. The problem is that the name now contains a
version number which could change if the eclipse install is updated.

The story going forward would be that people who wish to start with Java
directly would have to either modify their scripts to point to the
correct launcher JAR, rename it to have a name they prefer (without
version number), or copy the launcher JAR to the Eclipse install root
and rename it to be `startup.jar` (this is equivalent to the rename
without version number).

#### Bring back the old startup.jar

Revert from having a launcher bundle to having a `startup.jar` in the
install root.

#### Create a new startup.jar

Write a new "thin" `startup.jar` which looks for the launcher bundle in
the `plugins/` directory and then calls it.

**Issues Requiring Investigation**: "Find the real launcher bundle and
call it", actually means, create a new classloader containing the
launcher bundle and use reflection to invoke its main method. This
introduction of an extra classloader means the following issues must be
considered:

1.  osgi.parentClassloader. OSGi is started in a StartupClassLoader. The
    parent of this classloader depends on the property
    osgi.parentClassloader.
2.  Security. The launcher jar sets up the security manager when one is
    used.

Both of these issues must be fully understood before creating a thin
startup.jar. If either of them require handling in the thin jar, then
this jar is no longer thin and becomes almost as large as the real
launcher bundle. This then undoes the benefits of moving the launch code
to the plugins directory and has the added drawback of duplicating code.
In which case we would be better off reverting to the old way or doing
nothing.

The following is more information on the issues above.

1.  The thin startup.jar will be the only thing on the app classpath
    assuming it is launched with a simple "java -jar startup.jar"
    command. This means anyone accessing the system classloader
    (ClassLoader.getSystemClassLoader) will no longer get a classloader
    that contains the classes from org.eclipse.equinox.launcher. The
    osgi.parentClassloader sets the parent of the StartupClassLoader and
    the parent classloader that is used for all bundle classloaders.
    Currently the values supported are "fwk", "app", "ext" and "boot".
    The value "boot" is the default. If the value of "app" is used then
    the parent classloader will only be able to access the content in
    the thin startup.jar. Clearly this is a corner case, we should never
    encourage the use of the application classloader for anything inside
    eclipse anyway.
2.  There are a few things to consider for security. The thin
    startup.jar should do all of its work before calling the Main class
    in org.eclipse.equinox.launcher. The equinox launcher Main class may
    setup a security manager. If a class from the thin startup.jar
    attempts to perform a priviledged operation then it may get a
    security exception. The thin startup.jar should also use a simple
    URLClassLoader that is **not** extended by a class in the thin
    startup.jar. This ensures that the classloader used to load the
    equinox launcher always has AllPermission because URLClassLoader is
    from the boot classpath.

### Other Issues

#### Running the launcher exe headless and unattended \[RESOLVED\]

The launcher exe could display a dialog and block a headless unattended
application. This has been resolved with .

#### Eclipse.exe does not propagate exit code \[RESOLVED\]

In some cases the launcher exe does not propagate the exit code from
java. This is easily fixed and is tracked by .

#### Running the launcher exe on a machine without a WS \[RESOLVED\]

The eclipse 3.3 launcher will not run on a machine that does not contain
the appropriate window system libraries (ie gtk, motif). This is not new
and was true of the old eclipse launchers as well.

The eclipse 3.4 launcher is now capable of running without a WS on linux
and solaris. . AIX still requires motif to be installed ().

#### Eclipse.exe does not provide normal console stream \[RESOLVED\]

On Windows, The eclipse launcher is linked as a GUI application, and
hence does not get the normal console streams. This also causes us
problems with the `-console` option. Windows now ships with a second
executable eclipsec.exe which is linked as a console application which
solves all the issues.

On the other platforms, the console stream problem was caused by eclipse
running as 2 processes (eclipse and java). This is resolved when
launching using JNI invocation to start the java vm.

### Links

1.  [Equinox Launcher](Equinox_Launcher "wikilink")

2.  [Equinox Launcher Plan](Equinox_Launcher_Plan "wikilink")

3.  [Splash Screen Improvements](Splash_Screen_Improvements "wikilink")

4.  [Bug 173742](https://bugs.eclipse.org/bugs/show_bug.cgi?id=173742):
    Please reinstate a unified way to launch using Java

5.  : \[Workbench\] \[IDE\] Improve the launching experience

6.  : \[Launcher\] need option to suppress error dialogs

7.  : \[launcher\] create a console friendly eclipse.exe

8.  : \[Launcher\] Launcher should propagate java return code

[Category:Equinox](Category:Equinox "wikilink")
[Category:Releng](Category:Releng "wikilink")
[Category:Launcher](Category:Launcher "wikilink")