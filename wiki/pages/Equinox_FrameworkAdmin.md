## Overview

### Background

Currently, the focus of OSGi Service Platform Specification has been to
standardize a runtime infrastructure. On the other hand, how to
configure and launch a framework is not addressed. In other words, the
ways how to configure a launch of OSGi framework and how to launch it
depend on its implementation. Therefore, it is difficult to manage
scenarios such as bootstrapping OSGi on a clean system in a framework
independent way.

We would like to address this problem by proposing FrameworkAdmin
service.

### What does FrameworkAdmin service enable?

FrameworkAdmin service allows a bundle running on a framework to do the
followings in a framework independent way.

  - Set configurations required to launch a framework, such as
      - Java VM location,
      - Java VM arguments,
      - executable launcher location, if used
      - beginning start level and initial bundle start level of a
        framework,
      - Bundles to be installed and started with their start levels,
      - framework configuration location,
      - framework persistent date location,
  - Launch a framework with the specified configurations.
  - get the expected state of a framework when it runs.

<font color="red">In addition, FrameworkAdmin API and its implementation
can be used for not a bundle but a Java program, which is not OSGi based
application, to do the same things in a framework independent way (since
version 1.1.0).</font> See [\#Example 2](#Example_2 "wikilink") and
[Using FrameworkAdmin from Java
programs](Using_FrameworkAdmin_from_Java_programs "wikilink") for more
detail.

## Scenarios that the API would help

  - PDE UI: to ease the targeting of various frameworks. Currently PDE
    UI depends on ad-hoc API.
    Provisioning agent: to setup a non running framework
    Building an application: to create at build time all the config
    files required to run the exported application
    Running the framework: to configure and start a framework from
    either a java command or launcher

See [\[1](#references "wikilink")\] [\[2](#references "wikilink")\].

## Terminologies used in here

  - Framework config files: configuration files that will be read by a
    framework implementation on startup, e.g. config.ini in the
    directory specified by osgi.configuration.area system property for
    equinox.
    Executable launcher: an executable framework launcher file, e.g.
    eclipse.exe for Eclipse.
    Launcher config files: configuration files that will be read by an
    executable launcher, e.g. eclipse.ini for eclipse.exe.
    Bundles state: state of bundles that represents what kinds of
    bundles are installed, resolved and to be started on a framework.
    Framework persistent data location: under this location (including
    recursive subdirectories), a framework keeps data that should be
    kept persistently.

## How does it work ?

I’ll explain how it works using some examples. There are two types of
usage: usage by a bundle on a running framework and usage by a Java
program.

### Example 1

A bundle will get a FrameworkAdmin object via service registry on
running framework and use it.

#### Example 1-1

Let me assume that there is a bundle, which wants to configure a launch
of a target framework implementation, e.g. Equinox, and launch it.

1.  On the framework, there is a registered FrameworkAdmin service for
    Equinox.
      - There might be several registered FrameworkAdmin services for
        different frameworks.
2.  The client checks if a FrameworkAdmin service for its target
    framework implementation (Equinox) is available or not.
3.  If it is available, the client gets the FrameworkAdmin service
    object from the service registry. Otherwise, the client cannot
    realize what it wants.
4.  The client creates a new Manipulator object from the FrameworkAdmin
    object.
      - This Manipulator object has no parameters set yet.
5.  The client sets configurations to the Manipulator object
    1.  The client gets the references of ConfigData of the Manipulator
        object and sets parameters to it such as
          - beginning start level and initial bundle start level of a
            framework,
          - Bundles to be installed and started with their start levels,
    2.  The client gets the references of LauncherData of the
        Manipulator object and sets parameters to it such as
          - Java VM location,
          - Java VM arguments,
          - framework implementation location,
          - framework configuration location,
          - framework persistent date location.
6.  The client saves the configuration according to the parameters set
    to the Manipulator object.
7.  The client launches a framework using the saved configuration by
    FrameworkAdmin\#launch() with the Manipulator object as an argument.

#### Example 1-2

Let me assume that there is a bundle, which wants to get configurations
for a launch of Knopflerfish and launch another framework , Equinox,
with the same set of bundles installed.

1.  On the framework, there are registered FrameworkAdmin services for
    both Equinox and Knopflerfish.
2.  The client checks if FrameworkAdmin services for both its target
    framework implementations (Equinox and Knopflerfish) are available
    or not.
3.  If available, the client gets both FrameworkAdmin service objects
    from the service registry. Otherwise, the client cannot realize what
    it wants.
4.  Load configurations of a launch for Knopflerfish.
    1.  The client creates a new Manipulator object from the
        FrameworkAdmin object for Knopflerfish.
          - This Manipulator object has no parameters set yet.
    2.  The client gets the references of LauncherData of the
        Manipulator object for Knopferfish and sets parameters to it
        such as.
          - framework configuration location,
          - framework persistent date location.
    3.  The client loads configurations according to the parameters set,
        such as the framework configuration location and the framework
        persistent data location.
5.  Copy configurations information from the Manipulator object for
    Knoflerfish to the one for Equinox.
    1.  The client creates a new Manipulator object from the
        FrameworkAdmin object for Equinox.
    2.  The client copies parameters of the Manipulator object for
        Knopflerfish to the ones of the Manipulator object for Equinox.
    3.  The client gets the references of LauncherData of the
        Manipulator object for Equinox and resets some parameters to it
        such as
          - framework implementation location,
          - framework configuration location,
          - framework persistent date location.
6.  The client saves the configuration according to the parameters set
    to the Manipulator object for Equinox.
7.  The client launches a framework Equinox using the saved
    configuration by launch() method of the FrameworkAdmin object for
    Equinox with the Manipulator object for Equinox as an argument .

#### Example 1-3

Let me assume that there is a bundle, which wants to get configurations
for a launch of Felix and expect bundles state if that configurations
are used for a launch.

1.  On the framework, there are registered FrameworkAdmin services for
    Felix.
2.  The client checks if a FrameworkAdmin service for the target
    framework implementation (Felix) is available or not.
3.  If available, the client gets a FrameworkAdmin service object from
    the service registry. Otherwise, the client cannot realize what it
    wants.
4.  Expect bundles state if the specified locations are used for a
    launch.
    1.  The client creates a new Manipulator object from the
        FrameworkAdmin object for Felix.
          - This Manipulator object has no parameters set yet.
    2.  The client gets the references of LauncherData of the
        Manipulator object and sets parameters to it such as.
          - framework implementation location,
          - framework configuration location,
          - framework persistent date location.
    3.  The client loads configurations according to the parameters set,
        such as the framework configuration location and the framework
        persistent data location.
    4.  The client creates a BundlesState object from the Manipulator
        object. At its creation, the BundlesState object contains
        bundles which the Manipulator object has at that time.
    5.  The client expects the bundles state by calling
        BundlesState\#getExpectedState(), which tries to resolve bundles
        set and returns value representing which bundles are resolved
        and which are not.
    6.  If the client wants to modify installed bundles for some reason,
        for example in order to make all bundles resolved, the following
        procedures would be done.
        1.  The following two steps will be done repeatedly until the
            client satisfies with the expected state.
            1.  The client adds or removes bundles to be installed and
                started with their start levels to the BundlesState.
            2.  The client expects the bundles state by calling calling
                BundlesState\#getExpectedState(),
        2.  If the client satisfies with the expected state and wants to
            reflect the modification to the BundlesState object to the
            configuration, do the followings:
            1.  The client gets the references of ConfigData of the
                Manipulator object.
            2.  set bundles the BundleState keeps to the ConfigData.
            3.  The client saves the configuration according to the
                parameters set to the Manipulator object.

### Example 2

A Java program will get a FrameworkAdmin object via static factory
method defined newly in version 1.1.0 and use it.

1.  Required class should be accessible by classloaders in advance.
      - the FrameworkAdmin implementation classes support this feature
        for a target framework implementation.
      - and prerequisite classes for those classes.
2.  A Java program will get a FrameworkAdmin object by
    FrameworkAdminFactory.getInstance(name) with *magic name of the
    FrameworkAdminFactory implementation class*, which is declared by
    the implementator in advance.
      - <font color="red">The following steps are just same as
        [\#Example 1-1](#Example_1-1 "wikilink")</font>.
3.  The client creates a new Manipulator object from the FrameworkAdmin
    object.
      - This Manipulator object has no parameters set yet.
4.  The client sets configurations to the Manipulator object
    1.  The client gets the references of ConfigData of the Manipulator
        object and sets parameters to it such as
          - beginning start level and initial bundle start level of a
            framework,
          - Bundles to be installed and started with their start levels,
    2.  The client gets the references of LauncherData of the
        Manipulator object and sets parameters to it such as
          - Java VM location,
          - Java VM arguments,
          - framework implementation location,
          - framework configuration location,
          - framework persistent date location.
5.  The client saves the configuration according to the parameters set
    to the Manipulator object.
6.  The client launches a framework using the saved configuration by
    FrameworkAdmin\#launch() with the Manipulator object as an argument.

See [Using FrameworkAdmin from Java
programs](Using_FrameworkAdmin_from_Java_programs "wikilink") for more
detail.

## API: Package org.eclipse.equinox.frameworkadmin

Interfaces defined in *org.eclipse.equinox.frameworkadmin* package will
be introduced. See the codes and Java docs for more details.

### FrameworkAdmin

The only interface whose implementation will be registered into the OSGi
service registry in this package is FrameworkAdmin.

The client bundle will get the appropriate FrameworkAdmin service object
that can meet its requirements by filtering service properties in the
service registry. An example of filtering is the target framework
implementation and its version. As for filtering, [later
section](#Provider_of_these_APIs "wikilink") will explain it in detail.
Then, the client will get new instance of Manipulator which plays a main
role in this API.

The interface has a method which might return the Manipulator object
initialized according to the running framework and launcher state.
Detail of this will be introduced
[later](#Current_our_proposal "wikilink").

In addition, it also has a method to launch a framework according to the
specified Manipulator object.

### Manipulator

A client bundle can get a Manipulator object by
FrameworkAdmin\#getManipulator(). This object keeps both a ConfigData
object and a LauncherData object, each of which have setter and getter
methods on parameters to be required for a framework launch. The client
can set some parameters to be required for launching a framework via
ConfigData object or LauncherData object. The parameters set in those
objects can be saved into configuration files. Where to save is
determined according to the parameters set at that time in a framework
and launcher independent way.

The client also can load parameters from framework config files and
launcher config files in the specified locations. In addition, the
persistently stored data in the framework persistent location specified
should be taken into consideration.

Once parameters are set to this object by either setting via a
ConfigData or a LauncherData or loading from configuration files,
FrameworkAdmin\#launch(Manipulator, File) will launch a framework
according to the current parameters set in this object. It might invoke
a framework by java command or executing the specified launcher. How it
will be launched will be up to the FrameworkAdmin implementation and
parameters set at that time.

  - For example, if the implementation supports launching by an
    executable launcher and a laucher is set to the Manipulator object,
    FrameworkAdmin\#launch(Manipulator, File) will invoke a framework by
    executing the launcher.

Remember the parameters set to this object should be saved before a
launch.

In addition, Manipulator enables for a client bundle to get the
BundlesState object, which will be used for expecting bundles state.

### ConfigData

ConfigData is a class that keeps some parameters mainly related with
framework internal behavior for a Manipulator object such as,

  - Bundles list to be installed, with their persistently marked flag as
    started or not and their start levels.
  - Beginning start level and initial bundle start level of a framework.
  - System properties dependent on a framework implementation.
  - System properties independent of a framework implementation.

For most framework implementations, these parameters will be saved into
their framework config files. However, this API doesn't restrict where
to save.

  - For example, a framework implementation and a launcher
    implementation might need to save these parameters not in framework
    config files but in a launcher config files. It depends on the
    target implementations.

### LauncherData

LauncherData is a class that keeps some parameters mainly related with
information required before launching framework instance for a
Manipulator object such as,

  - Location of Java VM.
  - Java VM arguments.
  - Location of launcher (if launcher is used)
  - Launcher specific arguments (if launcher is used)
  - Location of framework implementation.
  - Location of framework config files.
  - Location of framework persistent data, where a framework
    implementation saves information to be kept persistently, such as
    bundles state, permissions of bundles, start level information,
    private persistent storages for bundles, and so on.
  - Clean flag: a flag to clean the specified location of framework
    persistent data.

For most framework implementations, these parameters will be saved into
their launcher config files, if executable launcher is used. However,
this API doesn't restrict where to save.

  - As in Apache Felix, location of framework persistent data should be
    written in its framework config file. Therefore, the implementation
    of Manipulator for Felix should save it into not in launcher config
    files but framework config files.

### BundlesState

BundlesState object is used to predict the bundles state of a framework.

A BundlesState object will be created by a Manipulator object. At its
creation, bundles state according to the parameters that the Manipulator
object keeps will be created in this object.

This object reads the manifest information of bundles and tries to
resolve bundles as a running framework would do.

### FrameworkAdminFactory

This is an abstract class. The client of the subclass of it is supposed
to be not a bundle but a Java program. Using it, developers can launch a
framework from Java program. The details will be introduced in [Using
FrameworkAdmin from Java
programs](Using_FrameworkAdmin_from_Java_programs "wikilink").

## Provider of FrameworkAdmin

First of all, we supposed that an implementation of this API will be
provided for each framework implementation. We expect it will be
implemented by the team developing the framework, since it requires
knowledge of the framework internals.

For clients to filter the proper service object, the provider must
register the FrameworkAdmin object with service properties keyed by the
followings.

  - FrameworkAdmin.SERVICE_PROP_KEY_FW_NAME: String; name of the
    framework
    FrameworkAdmin.SERVICE_PROP_KEY_FW_VERSION: String; version of
    the framework
    FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_NAME: String; name of
    the launcher
    FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_VERSION: String;
    version of the launcher

<!-- end list -->

  - Current Limitation: there is no version range.Version Range might be
    required.
  - Offline, the implementator of FrameworkAdmin must declare these
    values and client must know them.

There is another service property name to be standardized.

  - FrameworkAdmin.SERVICE_PROP_KEY_RUNNING_SYSTEM_FLAG: true if
    its getRunningManipulator () returns the initialized Manipulator
    object of running system.

<!-- end list -->

  - Detail of it would be introduced
    [later](#Current_proposal "wikilink").

## Client of FrameworkAdmin

A client bundle can get appropriate FrameworkAdmin service object from
an OSGi service registry by filtering the service properties described
previously in order to get the Manipulator objects for the framework and
launcher implementation which the client want to manipulate.

## How to handle running framework information?

There is a need for manipulating configurations of not only a
non-running framework but also a running framework. Two kinds of
manipulator objects of the running framework come up to our mind:

1.  a manipulator representing as the same state of the running
    framework at that time.
2.  a manipulator representing the state of a framework launched by
    using the framework configuration location and framework persistent
    data location, which is used for the running framework.

The current FrameworkAdmin (tagged as v20070212-1630) only supports for
the first one.

### Current proposal to realize it

The bundle will do as follows when it registers a FrameworkAdmin service
for any framework and launcher;

1.  The bundle checks if the running framework can be managed by a
    FrameworkAdmin object that itself will register. The way how the
    bundle checks it is dependent of its framework and launcher
    implementation.
2.  If no, the bundle registers a FrameworkAdmin service for the target
    framework and launcher implementation, whose getRunningManipulator()
    returns null, with specified service properties including the ones
    representing framework name/version and launcher name/version.
3.  If yes, the bundle registers a FrameworkAdmin service for the
    framework and launcher implementation, whose getRunningManipulator()
    returns a Manipulator object initialized according to the running
    system. The registration is done with specified service properties
    including not only the ones representing framework name/version and
    launcher name/version, but also
    **FrameworkAdmin.SERVICE_PROP_KEY_RUNNING_SYSTEM_FLAG**="true".

When the bundle should register a FrameworkAdmin service is not
addressed. However, the typical timing is at its start up.

A client bundle can get the initialized Manipulator object as follows;

    String filter = “(FrameworkAdmin.SERVICE_PROP_KEY_RUNNING_SYSTEM_FLAG=true)”.
    ServiceReference references[] = context.getServiceReferences(FrameworkAdmin.class.getName(), filter);
    FrameworkAdmin FrameworkAdmin = (FrameworkAdmin) context.getService(references[0]);
    Manipulator manipulator = FrameworkAdmin.getManipulatorRunning(); <-- initialized.

### Scenario

One of the scenarios is

1.  A client gets a manipulator object representing the running
    framework *A*.
2.  After the client overwrites framework configuration location and
    framework persistent data location of it, it saves it and launch it
    (framework *B*).
3.  As a result, the bundles running on framework *A* at that time will
    be running on framework *B*.

## [Configurator](Configurator "wikilink")

## [Using FrameworkAdmin from Java programs](Using_FrameworkAdmin_from_Java_programs "wikilink")

## Bundles provided by this incubator subproject

1.  "**org.eclipse.equinox.frameworkadmin**": API related with
    FrameworkAdmin and ConfiguratorManipulator. In addition, utility
    classes used for implementing
    "org.eclipse.equinox.frameworkadmin.equinox",
    "org.eclipse.equinox.frameworkadmin.knopflerfish",
    "org.eclipse.equinox.frameworkadmin.felix" is stored.
      - <font color="red">Since v1.3.0, Configurator interface and
        utility classes used for
        "org.eclipse.equinox.simpleconfigurator" and
        "org.eclipse.equinox.simpleconfigurator.manipulator" plugins
        were eliminated from this plugin (moved into
        "org.eclipse.equinox.simpleconfigurator" plugin) according to
        the request of
        [bug\#175809](https://bugs.eclipse.org/bugs/show_bug.cgi?id=175809).
        </font>
2.  "**org.eclipse.equinox.frameworkadmin.equinox**": Implementation of
    FrameworkAdmin service for Equinox. See javadoc of BundleActivator
    of this plug-in for more detail.
      - It supports taking framework persistent data into consideration
        and supports resolving bundles, if running on the Equinox.
      - It supports taking a configurator bundle into consideration.
      - Both bundle format in a JAR and a Directory are supported since
        v.1.5.0.
      - bundle location starting with "**reference:**" is supported
        since v.1.5.0.
          - If
            BundleContext.getProperty(*EquinoxConstants.PROP_KEY_USE_REFERENCE*)
            does not equal "false", Manipulator\#save(..) will add
            "reference:" to any bundle location specified osgi.bundles
            in order to avoid caching its bundle jar. Otherwise, it will
            add nothing to any bundle location.
      - Manipulator\#getTimeStamp() is supported since v.1.5.0.
      - Even if LauncherData.getFwJar() == null, it will be
        automatically set in some operation,such as
        Manipulator\#save(..) if framework jar
        (org.eclipse.osgi._\*.jar) is included in
        ConfigData.getBundles().
      - *Limitation of current implementation*:
          - To read framework persistently stored data, State objects
            created by PlatformaAdmin are used. That means this bundle
            has dependeny on them.
          - Bundles whose location is set to "update@\*\*\*\*" are not
            supported.
          - FrameworkAdmin.getRunningManipulator() returns not null but
            a Manipulator object according to the vendor name and the
            bundle version of running system bundle. The launcher name
            and version are not checked.
              - For checking the launcher name and version (in future),
                eclipse.exe will need to tell them to a fw launching.
          - Initial bundle start level cannot be set. A value set by
            ConfigData.setInitialBundleStartLevel(int) will be set to
            only bunldes installed by config.ini and
            EclipseUpdater(updateconfigurator). It's due to the current
            Equinox implementation.
          - It supports launching a framework from Java programs,
              - except a function of expecting bundles state because its
                implementation gets PlatformAdmin object from a service
                registry on a running framework and use it to resolve
                bundles (<font color="red">Since version 1.1.0</font>).
3.  "**org.eclipse.equinox.frameworkadmin.felix**": Implementation of
    FrameworkAdmin service for Apache Felix. See javadoc of
    BundleActivator of this plug-in for more detail.
      - Essentially, this bundle should be provided by the implementator
        of Felix. This implementation is tentative.
      - No supports for taking framework persistent data into
        consideration nor supports resolving bundles.
      - It supports taking a configurator bundle into consideration.
      - Launching a framework from Java programs is supported.
      - No supports for Manipulator\#getTimeStamp().
4.  "**org.eclipse.equinox.frameworkadmin.knopflerfish**":
    Implementation of FrameworkAdmin service for Knopflerfish. See
    javadoc of BundleActivator of this plug-in for more detail.
      - Essentially, this bundle should be provided by the implementator
        of Knopflerfish. This implementation is tentative.
      - No supports for taking framework persistent data into
        consideration nor supports resolving bundles.
      - It supports taking a configurator bundle into consideration.
      - Launching a framework from Java programs is supported.
      - No supports for Manipulator\#getTimeStamp().
5.  "**org.eclipse.equinox.simpleconfigurator**": Implementation of
    SimpleConfigurator.
      - Framework property keyed by
        *SimpleConfiguratorConstants.PROP_KEY_CONFIGURL* is used for
        SimpleConfigurator to do life cycle control of bundles. The file
        specified by the returned url is read by SimpleConfigurator and
        do life cycle control according to it. If improper value or null
        is returned, SimpleConfigurator doesn't do it.
      - Framework property keyed by
        *SimpleConfiguratorConstants.PROP_KEY_EXCLUSIVE_INSTALLATION*
        equals "true" ignoring case,
        Configurator.applyConfiguration(url) will uninstall the
        installed bundles which are not listed in the simpleconfigurator
        config file after install bundles listed. Otherwise, it never
        uninstall any bundles.
      - If this bundle is running on Equinox, Framework property keyed
        by ''SimpleConfiguratorConstants.PROP_KEY_USE_REFERENCE does
        not equal "false" ignoring case, when a SimpleConfigurator
        installs a bundle, "reference:" is added to its bundle location
        in order to avoid caching its bundle jar. Otherwise, it will add
        nothing to any bundle location.
          - Default: true
              - <font color="red"> This function is enabled only in case
                of running on Equinox because there is no standardized
                way to install a bundle without caching and other
                frameworks don't support it yet.</font>
6.  "**org.eclipse.equinox.simpleconfigurator.manipulator**":
    Implementation of ConfiguratorManipulator for SimpleConfigurator.
7.  "**org.eclipse.equinox.frameworkadmin.examples**": examples using
    FrameworkAdmin.
      - In order to run these tests, <span style="color:red">read
        **setting.properties** in
        **org.eclipse.equinox.frameworkadmin.examples** package of this
        plug-in.
      - This properties file should be set properly in advance.</span>
      - <span style="color:red"> Example of a Java program using
        FrameworkAdmin is added (Since version 1.1.0)</span>

To understand how a provider implements the APIs, see bundle 1 and 2. To
understand how a client uses the API, see bundle 1 and 7.

## TBD

### Which interface should a method of launch operation belong to?

Currently, a method of launch operation belongs to FrameworkAdmin
interface. I can separate this API into another interface like
*"FrameworkLauncher"*. But I am not sure how much merit of it would be.

  - One merit of the separation is, we can restrict some bundles so that
    can configure a framework but cannot launch it, by not giving
    ServicePermission("GET","FrameworkLauncher").
  - One demerit of it is, number of interface will be increased by one,
    even if this interface only has one method.

### Version range

Currently, specifying version ranges of framework and launcher that the
implementation can support are not supported at the registration of a
FrameworkAdmin implementation. It might be required in future.

### Launching a framework instance in the Same Process

At this point, launching a framework instance in the same process is not
considered yet in this API, explicitly. However, it will be supported in
the future.

  - What is not sure is “general way of launching a framework instance
    in the same process”. The API might be able to support it (JavaVM
    and JavaVM arguments are not used for it obviously).

### File or URL or Location newly defined?

Currently, LauncherData keeps most information related with file
location, such as location of framework jar implementation, framework
configuration, framework persistent data, and so on, as a File object.
Due to this design, these must exist on the local machine. It might be
better URL or newly defined Location instead of File.

### Explicit specifying a framework jar

Currently, a client bundle needs to call LauncherData.setFwJar(File) in
advance to doing some operation. Another design can be thought:

  - Instead of having no LauncherData.setFwJar(File) method, client
    should call ConfigData.addBundle(BundleInfo of framework jar) as one
    of bundles set to ConfigData. The implementation will identify a
    framework jar among them.

The problems of this approach would be:

  - Current OSGi specification doen's standardize how to identify if a
    jar is framework or not.
      - Identifying a bundle whose manifest has export-package header of
        "org.osgi.framework" as a framework jar might be used, although
        it is not a standardized way.
  - If the number of bundles which are identified as framework jar is
    more than one, how to determine the one to be activated ?
      - Maybe, the firstly added one among those framework jars each of
        which startlevel is 0 would be chosen.
  - Currently, FrameworkAdmin implementation of Equinox (since v.1.4.0)
    support some operation without setting fwJar explicitly by the way
    described above. To indentify an equinox(osgi) framework, Eclipse
    based bundle naming format (symbolicname_version.jar) assumption is
    used.

## References

1.  [Requirements for a new update manager (of
    Eclipse)](http://wiki.eclipse.org/index.php/Requirements_for_a_new_update_manager)
2.  [Requirements discussed in OSGi Alliance Enterprise
    Workshop](http://www2.osgi.org/EnterpriseWorkshop/Requirements)
3.  [Equinox: Runtime options of
    Eclipse 3.2](http://help.eclipse.org/help32/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html)
4.  [Felix: Usage](http://cwiki.apache.org/FELIX/felix-usage.html)
5.  [Knopflerfish: Usage](http://www.knopflerfish.org/running.html)

[Framework Admin](Category:Equinox "wikilink")