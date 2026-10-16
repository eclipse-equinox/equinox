## Overview

Since version 1.1.0, FrameworkAdmin API and its implementation enables
not only a bundle ***but also a Java program***, which is not OSGi based
application, to do the followings in a framework independent way.

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

This documents introduce how to use FrameworkAdmin from Java programs.

## Provider of FrameworkAdmin

To realize it, the provider should do

1.  implement a subclass of **FrameworkAdminFactory** whose
    createFrameworkAdmin() returns a FrameworkAdmin object which doesn’t
    use BundleContext at all.
2.  declare the fully qualified class name of it in public.
      - for instance, the name of the class in
        org.eclipse.equinox.frameworkadmin.equinox bundle is declared as
        **org.eclipse.equinox.frameworkadmin.equinox.internal.EquinoxFrameworkAdminFactoryImpl**.

## Client Java program of FrameworkAdmin

On the other hand, a client Java program should do

1.  set classpath properly in order to load required classes.
      - Taking **org.eclipse.equinox.frameworkadmin.equinox** version
        1.1.0 as an example, set **org.eclipse.osgi_\*.jar**,
        **org.eclipse.equinox.frameworkadmin_\*.jar** and
        **org.eclipse.equinox.frameworkadmin.equinox_\*.jar** to its
        classpath.
2.  in the code, call static method
    FrameworkAdminFactory.getInstance(className) with the fully
    qualified class name of the FrameworkAdminFactory subclass for the
    target framework implementation.
    **org.eclipse.equinox.frameworkadmin.equinox.internal.EquinoxFrameworkAdminFactoryImpl**
    in order to get a FrameworkAdmin object for Equinox.

After instantiate a FrameworkAdmin object for the target framework
implementation, do as same as a bundle which gets an FrameworkAdmin
object from the service registry on running framework does for
configuring and launching a framework.

## Example code

Example code of a client Java program is included in
**org.eclipse.equinox.frameworkadmin.examples** plug-in. In order to run
it,

1.  set properties file called "setting.properties" properly to your
    environment in advance and set required plugins into its classpath .
2.  run Main class as a Java application from PDE (IDE)

The following is a pseudo code of it. Please see the actual code in
**org.eclipse.equinox.frameworkadmin.examples** plug-in for more detail.

    public static void main(String[] args) {

    String className = "org.eclipse.equinox.frameworkadmin.equinox.internal.EquinoxFrameworkAdminFactory";
                         <--only this part is dependent on the target framework implementation.

    FrameworkAdmin fwAdmin = FrameworkAdminFactory.getInstance(className);

    // After instanciating FrameworkAdmin object, completely same code can be used
    // as the case that you get the object from a service registry on OSGi framework.
    Manipulator manipulator = fwAdmin.getManipulator();
    ConfigData configData = manipulator.getConfigData();
    LauncherData launcherData = manipulator.getLauncherData();

    // 1. Set Parameters to LaunchData.
    launcherData.setJvm(new File("C:\Java\jre1.5.0_09\bin\java.exe"));
    launcherData.setJvmArgs(new String[] {"-Dms40"});
    launcherData.setFwPersistentDataLocation(new File("C:\eclipse\configuration"), true);
    launcherData.setFwJar(new File("C:\eclipse\plugins\org.eclipse.osgi_3.3.0.v20070208.jar");
    launcherData.setFwConfigLocation(new File("C:\eclipse\configuration"));

    // 2. Set Parameters to ConfigData.
    configData.addBundle(new BundleInfo( bundleLocation, startlevel, markedAsStartedOrNot);
        :
    configData.setBeginningFwStartLevel(6);
    configData.setInitialBundleStartLevel(5);
    configData.setFwDependentProp("osgi.console","9000");

    // 3. Save them.
    manipulator.save(false);

    // 4. Launch it.
    Process process = fwAdmin.launch(manipulator, new File("C:\eclipse");
    }

## Support for ConfiguratorManipulator

In case of using FrameworkAdmin which a bundle gets from service
registry on running framework, *save* method will save info of bundles
to be installed into not only framework configuration files but also
configurator configuration files according to the availability of
ConfiguratorManipulator services. See
[Configurator](Configurator "wikilink") for more details.

However, in case of using it from Java programs, we need some trick to
get a proper ConfiguratorManipulator object to be created. Since ver
1.2.0, we propose the following way to support it.

### Our proposal

1.  Abstract classs, ConfiguratorManipulatorFactory in
    org.eclipse.equinox.configurator package is defined. An
    implementator of a ConfiguratorManipulator can provide its subclass.
    The class must be deployed in its classpath to be loaded.
2.  In FrameworkAdminFactory\#createFrameworkAdmin()
    1.  checks a system property keyed by predefined *magic key* and
    2.  returns a FrameworkAdmin object which doesn't use any
        ConfiguratorManipulator object will be returned, if the value is
        null. Otherwise return a FrameworkAdmin object which uses a
        ConfiguratorManipulator object returned by
        ConfiguratorManipulatorFactory.getInstance(value).

### Alternatives

There might be other ways to support it:

  - Alternative1: no magic system properties are used.

<!-- end list -->

1.  Abstract classs, ConfiguratorManipulatorFactory in
    org.eclipse.equinox.configurator package is defined. An
    implementator of a ConfiguratorManipulator can provide its subclass.
    The class must be deployed in its classpath to be loaded.
2.  Define
    FramworkAdminFactory.getInstance(classNameOfFrameworkAdminFactoryImpl,classNameOfConfiguratorManipulatorFactoryImpl
    )
3.  a Java program calls this method specifying explicitly both class
    names of factory implementations.

<!-- end list -->

  - Demerit of this: Configurator itself is different concept from
    FrameworkAdmin. So it's not beautiful that an element related with
    Configurator is included in FrameworkAdminFactory. On the other
    hand, whether Configurator is supported or not is up to its
    implementation.

<!-- end list -->

  - Alternative2: two magic system properties are used.

<!-- end list -->

1.  Abstract classs, ConfiguratorManipulatorFactory in
    org.eclipse.equinox.configurator package is defined. An
    implementator of a ConfiguratorManipulator can provide its subclass.
    The class must be deployed in its classpath to be loaded.
2.  Define FramworkAdminFactory.getInstance(), which has no arguments.
3.  a Java program calls this method. The class names of both factory
    implementations will be retrieved from system properties keyed by
    *magic keys* for each.

<!-- end list -->

  - This could be another promising way. The magic key related with
    FrameworkAdmin will be included in
    FrameworkAdminFactory.getInstance(). Personally, I prefer to avoid
    "magic stuff" in FrameworkAdmin API spec.

### Example code

Example code of a client Java program is included in
**org.eclipse.equinox.frameworkadmin.examples** plug-in. In order to run
it,

1.  set properties file called "setting.properties" properly to your
    environment in advance and set required plugins into its classpath.
2.  run Main class as a Java application from PDE (IDE)

The following is a pseudo code of it. Please see the actual code in
**org.eclipse.equinox.frameworkadmin.examples** plug-in for more detail.

    public static void main(String[] args) {

    String MAGIC_KEY="org.eclipse.equinox.configuratorManipulatorFactory";
            // defined by ConfiguratorManipulatorFactory.SYSTEM_PROPERTY_KEY
    System.setProperty(MAGIC_KEY,
    "org.eclipse.equinox.simpleconfigurator.manipulator.internal.SimpleConfiguratorManipulatorFactoryImpl");

    String className = "org.eclipse.equinox.frameworkadmin.equinox.internal.EquinoxFrameworkAdminFactory";
    FrameworkAdmin fwAdmin = FrameworkAdminFactory.getInstance(className);
    // following code is just as same as previous one.

## Limitations

There are some limitations on using FrameworkAdmin from Java programs.

For current FrameworkAdmin design (version 1.1.0), there are several
limitations:

  - how the Java program can control the framework launched by the
    FrameworkAdmin is out of scope of it.
      - For Equinox, you can access to a console of the framework via
        telnet if you set its port number.
      - You can access to a management agent bundle, which is configured
        to be installed and started at a framework launch, in its own
        way.
  - FrameworkAdmin doesn't support launching a framework in the same
    process yet. It is one of the future work.

For current FrameworkAdmin implementation provided by this Equinox
incubator subproject, there are several limitations:

  - **org.eclipse.equinox.frameworkadmin.equinox_\*.jar** (since ver.
    1.2.0)
      - support for launching a framework from Java programs, including
        ConfiguratorManipulator
          - No support for a function of expecting bundles state even
            for the implementation of Equinox (because the
            implementation for Equinox gets PlatformAdmin object from a
            service registry on a running framework and use it to
            resolve bundles).
          - A function of getting prerequisite bundles info are realized
            according to the Required-Bundle manifest header info. (it
            doesn't resolve actually).

<!-- end list -->

  - **org.eclipse.equinox.frameworkadmin.knopflerfish_\*.jar** (since
    ver. 1.2.0) and
  - **org.eclipse.equinox.frameworkadmin.felix_\*.jar** (since ver.
    1.2.0)
      - support for launching a framework from Java programs, including
        ConfiguratorManipulator
          - No support for a function of expecting bundles state as same
            as used on a framework.
          - A function of getting prerequisite bundles info are realized
            according to the Required-Bundle manifest header info as
            same as used on a framework. (it doesn't resolve actually).

## References

1.  [Equinox_FrameworkAdmin](Equinox_FrameworkAdmin "wikilink")

[Framework Admin](Category:Equinox "wikilink")