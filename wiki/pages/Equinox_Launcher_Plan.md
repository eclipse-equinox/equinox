The following is a proposal for the new Equinox Launcher in support of
the 3.3 plan item for improving the launching experience
[\[1](#Links "wikilink")\]. This work will need to happen in conjunction
with any changes that are being made to startup.jar
[\[2](#Links "wikilink")\][\[3](#Links "wikilink")\]. This new launcher
will do the following:

  - Use JNI to start the VM [\[4](#Links "wikilink")\]
  - Allow for updating the launcher
  - Allow for SWT widgets in the splash screen
    [\[5](#Links "wikilink")\][\[6](#Links "wikilink")\]

## Affected Projects

The following projects will be affected by this work:

  - equinox-incubator/org.eclipse.equinox.launcher: A new project will
    be create in the equinox incubator to hold the new launcher code
    until it is ready for use.
  - org.eclipse.equinox.startup: startup.jar is moving to this new
    project. Changes necessary for this work should be done here.
  - platform-launcher: This project contains the source for the current
    eclipse launcher.
  - org.eclipse.platform: This project contains the current startup.jar
    (plus other branding bits)
  - org.eclipse.swt: SWT will need to provide support for creating a
    Shell around an existing handle.
  - org.eclipse.osgi: The EclipseStarter will need changes during
    framework startup.
  - org.eclipse.ui.workbench: The workbench will need to check to see if
    a Display already exists and to use it instead of creating a new
    one.
  - A splash bundle: The splash bundle is associated with a product, and
    could be org.eclipse.platform. Or a new bundle
    (org.eclipse.platform.splash) could be created.

## JNI Launching

Instead of execing java, the equinox launcher will load the jvm shared
library and use JNI to create the vm. To do this the launcher will need
to do the following:

  - Find the jvm shared library.
  - Setup the LD_LIBRARY_PATH on those platforms that require it.
  - Find the startup.jar. By default it is simply startup.jar in the
    root of eclipse. The launcher also supports specifying the jar using
    the -jar argument. See also the
    [startup.jar](#Startup.jar_in_the_plugins_Directory "wikilink")
    section below.
  - Find the Main class. We don't want to parse the manifest to get the
    Main-Class header. By default we will assume
    org.eclipse.core.launcher.Main. We should provide an argument -class
    where this can be specified.
  - Start the VM using JNI, and invoke the Main.main(String\[\]) method.

### Finding the shared Library

We have an existing strategy for finding a java executable. We should
look for the jvm library in a location relative to the java executable.

  - We should also allow the user to specify the library directly using
    the -vm argument.
  - We should also support reading a properties file containing the
    needed information. The Apache Harmony project
    [\[7](#Links "wikilink")\] has similar requirements.

<!-- end list -->

  - Some VMs will contain more than one library (ie client & server, or
    j9vm & classic). The launcher will need to understand the JRE's
    arguments that are used to select a particular vm (-client, -server,
    -hotspot, -Xj9)
  - On Windows, we may also need to consult the registry to find the vm
    library. The key `[HKCU|HKLM]\Software\JavaSoft\Java Runtime
    Environment` lists installed jvms by version number.

### Adding JNI Callback methods

Using JNI to launch the VM allows us to register native methods that can
be called from Java. These methods can be use to avoid having Java exec
the launcher in order to pass information back through shared memory.
They can also be used in support of an SWT splash screen.

## Splitting the Launcher into Executable and Library

We should split the launcher into a small executable and a separate
shared library. The executable would simply parse the eclipse.ini file
and find the appropriate eclipse shared library. The shared library
would do all the other work (showing a splash screen, launching the vm).
This will allow the following:

  - When the user starts eclipse by invoking Java directly, the startup
    code can load the eclipse library in order to display a splash
    screen. One particular use case here is self hosting.
  - The shared library can be versioned, allowing us to update the
    launcher by simply dropping in a new library.
  - The shared library can in theory be loaded by others to start
    eclipse in a custom manner.
  - The eclipse.ini can now be used to potentially choose between
    different launchers

## Startup.jar in the plugins Directory

Startup.jar will be moved into the plugins directory and named
org.eclipse.equinox.startup. This means that the native launcher will
need to search for the jar if it is not specified on the command line.

The search would involve finding all files in the plugins directory
whose name matches the pattern
"org.eclipse.equinox.launcher_<version>.jar" and use the one with the
highest version number. A similar technique would be used to find the
launcher shared library.

## SWT Splash Screen

The launcher should display a static splash screen bitmap as soon as
possible.

  - The bitmap to display should be specified as an argument (on the
    command line or in eclipse.ini) If no argument is specified, as can
    look for a splash.bmp in the root of eclipse. Products should be
    encouraged to specify this argument as it will enable showing the
    splash screen earlier than what is currently possible.
  - If no bitmap is found, we should register a showSplash method with
    the VM so that startup.jar can call back with the location of a
    bitmap to display. This is analagous to the current method of
    startup.jar execing another launcher with the location of the bitmap
    to display, except that this is cleaner in that it does not involve
    a separate process.

The splash screen is displayed natively on the main thread and then we
start the VM. The handle to the splash window is passed to java through
JNI. Once SWT is loaded, this handle can then be used to create a Shell
around the splash screen. This requires support from SWT. The native
splash window will need to be created in prescribed manner so that SWT
can easily wrap it with a Shell. Prototypes have been created which
create Shells in this manner for the win32, gtk and carbon windowing
systems. No problems are anticipated for motif.

Native methods to register with startup.jar include the following:

1.  showSplash: display a splash screen or update the bitmap of an
    existing splash screen
2.  dispatchMessages: process any events (ie paint) for the splash that
    may be waiting
3.  closeSplash: close the splash screen
4.  getSplashHandle: get the handle of an existing splash screen

## EclipseStarter, Startup Monitor/Animator, and Splash Bundle

org.eclipse.osgi will provide an StartupMonitor interface. Interested
products should register an implementation of this interface as an OSGi
service. Startup.jar will provide a default StartupMonitor that simply
uses native calls to paint the splash screen.

During startup, the EclipseStarter will periodically check to see if a
StartupMonitor has been registered. If no monitor is registered, the
default StartupMonitor from startup.jar is used.

Once a service has been detected, then EclipseStarter will call an
initialize/startup method. Then it will call a
getMonitorUpdateRunnable() method. Once it has that runnable, it will
periodically run it to allow the StartupMonitor to update.

Note that this StartupMonitor is more general than the splash screen and
does not need to do anything with the UI. For example a startup monitor
could be registered that simply outputs progress to a console.

### Periodic Update Calls

Currently there are a couple of places where the main thread waits on a
semaphore while work is being done on another thread. The biggest
example of this is when the start level gets incremented. Instead of
waiting and doing nothing, the main thead can instead periodically call
the StartMonitor to update.

We should also try to have a similar situation while the
update.configurator bundle is installing everything into the framework.

### Splash Bundle as a StartupMonitor

A product can register a splash bundle as a startup monitor. Such a
splash bundle would depend on SWT and be able to create a Shell around
the native window handle. This bundle could provide some mechanism to
allow others to contribute to the splash screen. At a minimum it should
provide support for a progress monitor since the native launcher will no
longer provide one.

The splash bundle should be careful to only perform SWT operations when
being initialized or when its update runnable is called. This will
ensure that all SWT operations occur on the proper thread.

### Activating the Splash Bundle

The splash bundle should register itself as a service when it gets
activated. There are a couple of different ways to get the bundle
activated during startup:

1.  The osgi.bundles list. The splash bundle can be placed on this list:
    1.  The splash bundle can be set at start level 1. In this case,
        org.eclipse.swt and its appropriate fragment will also need to
        be on the list.
    2.  The splash bundle can be set at start level 4. In this case, the
        bundle is activated after update.configurator, so swt will have
        been installed and doesn't need to be specified on the
        osgi.bundles list.
2.  The splash bundle can be specified as Eclipse-LazyStart and the
    Application can instantiate it when the application is started. In
    this case, the splash bundle won't be around for the initial
    startup. (This is analagous to the current situation, the splash
    screen does not begin to show progress until after the application
    has started)
3.  Some other configuration/provisioning bundle starts it.

## Splash Screen and the UI

It will the the UI and the splash bundle that define the API for
contributing to the splash screen. See the [Splash Screen
Improvements](Splash_Screen_Improvements "wikilink") page.

## Links

1.  [Bug 154088](https://bugs.eclipse.org/bugs/show_bug.cgi?id=154088)
    Improve the launching experience.
2.  [Bug 159122](https://bugs.eclipse.org/bugs/show_bug.cgi?id=159122)
    Move the startup.jar code
3.  [Bug 107738](https://bugs.eclipse.org/bugs/show_bug.cgi?id=107738)
    Investigate if the startup.jar can be delivered as a plugin.
4.  [Bug 82518](https://bugs.eclipse.org/bugs/show_bug.cgi?id=82518) Use
    JNI to Launch Eclipse
5.  [Bug 111539](https://bugs.eclipse.org/bugs/show_bug.cgi?id=111539)
    Report splash screen progress based on jobs.
6.  [Bug 141792](https://bugs.eclipse.org/bugs/show_bug.cgi?id=141792)
    API for custom startup progress reporting.
7.  [Apache Harmony](http://harmony.apache.org/)
8.  [Equinox Launcher VM Info](Equinox_Launcher_VM_Info "wikilink") -
    list of VMs that have been tested/verified with new launcher

[Launcher](Category:Equinox "wikilink")
[Category:Launcher](Category:Launcher "wikilink")