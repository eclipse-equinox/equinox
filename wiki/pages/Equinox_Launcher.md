## The Launcher and its shared library

The launcher executable, eclipse.exe, is in 2 pieces: the executable,
and a shared library (eg: eclipse_1006.dll). The executable lives in
the root of the eclipse install. The shared library is in a platform
specific fragment, org.eclipse.equinox.launcher.\[config\], in the
plugins directory.

With the majority of the launcher code in a shared library that lives in
a fragment means that that portion of the launch code can be updated
from an update site. Also, when starting from java, the shared library
can be loaded via JNI in order to display the splash screen.

## Startup.jar

Eclipse can be started directly with java using, for example:

    java -jar plugins/org.eclipse.equinox.launcher_1.0.0.v20070208a.jar

It is also possible to copy this bundle into the root and name it
startup.jar. In this case it would be possible to start with `java -jar
startup.jar`.

## Finding a VM, Using JNI Invocation or Executing Java

JNI launching does not currently work on all platforms for all vms.
Because of this, the launcher can start the Java Virtual Machine either
in process through the JNI Invocation API, or in a separate process by
executing the java launcher. Which method is used depends on how the vm
was found.

**Most platforms, we use JNI launching unless a -vm argument was given
that points directly to a java executable.**

More specifically, a virtual machine and launch method is chosen as
follows:

#### No -vm specified

When no -vm is specified, the launcher looks for a virtual machine first
in a jre directory in the root of eclipse and then on the search path.
If java is found in either location, then we look for a jvm shared
library (jvm.dll on window, libjvm.so on \*nix platforms) relative to
that java executable.

  - If a jvm shared library is found we load it and use the JNI
    invocation api to start the vm.
  - If no jvm shared library is found, we exec the java launcher to
    start the vm in a new process.

#### \-vm specified on command line or in eclipse.ini

Eclipse can be started with "-vm <location>" to indicate a virtual
machine to use. There are several possibilities for the value of
<location>:

1.  java.exe/javaw.exe: <location> is a path to a java launcher. We exec
    that java launcher to start the vm in a new process.
2.  jvm.dll or libjvm.so: <location> is a path to a jvm shared library.
    We attempt to load that library and use the JNI Invocation API to
    start the vm in the current process.
3.  vmDesc.ee: <location> is a path to a file ending in ".ee". This file
    is expected to describe the execution environment for a jvm. See the
    [Execution Environment
    Descriptions](Execution_Environment_Descriptions "wikilink") page.
4.  directory: <location> is a directory. We look in that directory for:
    (1) a default.ee file, (2) a java launcher or (3) the jvm shared
    library. If we find the jvm shared library, we use JNI invocation.
    If we find a launcher, we attempt to find a jvm library in known
    locations relative to the launcher. If we find one, we use JNI
    invocation. If no jvm library is found, we exec java in a new
    process.

#### Exceptions

  - **gtk.linux.ppc**, **gtk.linux.x86_64**, and **motif.aix.ppc**:
    Getting JNI to work on these platforms has been problematic for
    older vms. For these platforms, **we exec java unless the -vm
    argument directly specifies a jvm shared library**. Or, if the -vm
    argument specifies a directory in which no java executable was found
    but a jvm shared library was found.
  - **MacOSX**: On the mac we are always launching via the JNI
    invocation API. We use
    "/System/Library/Frameworks/JavaVM.framework/Versions/Current/JavaVM"
    as the jvm library. When a -vm argument is given, we parse it for
    the version of java to specify to JavaVM.

## Command line arguments

There are several arguments that can be specified to the eclipse
launcher. Arguments that are new are named using the convention
--launcher.<arg>, old arguments keep the same name as before.

  - ` --launcher.library  `<path/to/eclipse shared lib>. This indicates
    the path to the eclipse shared library to use. By default, the
    launcher looks in the plugins directory for the matching
    org.eclipse.equinox.launcher.\[config\] fragment with the highest
    version. The shared library is found in that fragment.

<!-- end list -->

  -
    If the path points to a directory, we use the file
    eclipse_<version>\[.<extension>\] with the highest version number.

<!-- end list -->

  - `-startup <path/to/launcher.jar>`. This indicates the path to the
    launcher jar. By default, the launcher looks in the plugins
    directory for the org.eclipse.equinox.launcher bundle with the
    highest version and uses it. If this bundle is not found, then the
    launcher will revert to old behaviour and look for startup.jar in
    the root.

<!-- end list -->

  -
    If a relative path is given, the launcher will check first relative
    to the working directory, and second relative to the root of the
    eclipse install (which is the location of the eclipse.exe)

<!-- end list -->

  - `-showsplash <path/to/splash.bmp>`. If the splash.bmp is specified
    to the launcher, then the splash screen can be displayed before the
    java vm is even started. If no splash screen is specified, then once
    java is started, the Main class will find the splash.bmp as before
    and call back to the launcher to display it.

<!-- end list -->

  -
    The following are examples of possible values for the path:
    1\) org.eclipse.platform : The launcher will look in the plugins
    directory for the bundle org.eclipse.platform_<version> with the
    highest version number, it will then use the "splash.bmp" file in
    this directory.
    2\) path/to/somewhere/org.eclipse.platform : The launcher will
    separate this into a path (path/to/somewhere) and a prefix
    (org.eclipse.platform). The path indicates the directory (absolute
    or relative to the eclipse root) in which to search for the splash
    bitmap in the same way as (1).
    3\) /path/to/somewhere/splash.bmp : An absolute path to the bitmap
    to use.

<!-- end list -->

  - `--launcher.suppressErrors`. If this is specified, then the launcher
    will not display a message box with errors if a problem was
    encountered. This allows the launcher to be used in unattended tests
    or builds without blocking on an error.

## Splash Screen

The splash screen can contain SWT widgets. The progress bar and text on
the splash screen (including the build id) are done using SWT. The
launcher itself only displays a static bitmap.

#### Getting an early splash screen

The launcher can display the splash screen before even starting the java
virtual machine. This requires that 1) the vm is started using JNI
invocation, and 2) the location of the splash bitmap is specified to the
launcher (usually in the eclipse.ini file).

See the description of the -showsplash argument above.

[Launcher](Category:Equinox "wikilink")
[Category:Launcher](Category:Launcher "wikilink")