

Adaptor Hooks
=============

Contents
--------

*   [1 Overview](#Overview)
*   [2 Hookable Adaptor](#Hookable-Adaptor)
    *   [2.1 The Base Adaptor](#The-Base-Adaptor)
    *   [2.2 The Hook Registry](#The-Hook-Registry)
        *   [2.2.1 Hook Configurators](#Hook-Configurators)
        *   [2.2.2 Discovering Hook Configurators](#Discovering-Hook-Configurators)
            *   [2.2.2.1 hookconfigurators.properties files](#hookconfigurators.properties-files)
            *   [2.2.2.2 osgi.hook.configurators property](#osgi.hook.configurators-property)
            *   [2.2.2.3 osgi.hook.configurators.include property](#osgi.hook.configurators.include-property)
            *   [2.2.2.4 osgi.hook.configurators.exclude property](#osgi.hook.configurators.exclude-property)
*   [3 Hook interfaces](#Hook-interfaces)
    *   [3.1 Adaptor Hook](#Adaptor-Hook)
    *   [3.2 Bundle File Factory Hook](#Bundle-File-Factory-Hook)
    *   [3.3 Bundle File Wrapper Factory Hook](#Bundle-File-Wrapper-Factory-Hook)
    *   [3.4 Bundle Watcher Hook](#Bundle-Watcher-Hook)
    *   [3.5 Class Loading Hook](#Class-Loading-Hook)
    *   [3.6 Class Loading Stats Hook](#Class-Loading-Stats-Hook)
    *   [3.7 Storage Hook](#Storage-Hook)
*   [4 Bundle Files](#Bundle-Files)
*   [5 Class Loaders](#Class-Loaders)
*   [6 Built-in Hook Configurators](#Built-in-Hook-Configurators)
    *   [6.1 DevClassLoadingHook configurator](#DevClassLoadingHook-configurator)
    *   [6.2 EclipseStorageHook configurator](#EclipseStorageHook-configurator)
    *   [6.3 EclipseLogHook](#EclipseLogHook)
    *   [6.4 EclipseErrorHandler](#EclipseErrorHandler)
    *   [6.5 EclipseAdaptorHook](#EclipseAdaptorHook)
    *   [6.6 EclipseClassLoadingHook](#EclipseClassLoadingHook)
    *   [6.7 EclipseLazyStarter](#EclipseLazyStarter)
    *   [6.8 StatsManager](#StatsManager)
    *   [6.9 SignedBundleHook](#SignedBundleHook)
*   [7 Examples](#Examples)

Overview
--------

Since Eclipse 3.0 the Framework Adaptor API has been available in the Equinox OSGi Framework. A framework adaptor implementation is called upon by the Equinox OSGi Framework to perform a number of tasks. A framework adaptor may be used to add functionality to the framework.

A single framework adaptor is specified when the framework is launched. By default in Eclipse 3.0 this is set to the EclipseAdaptor. In order to add new functionality in an adaptor in Eclipse 3.0 and 3.1 it is required that the adaptor implementation either re-implement the complete framework adaptor API or extend one of the existing framework adaptor implementations. This makes it impossible for two parties to add new functionality to the framework in separate adaptors at the same time because the Equinox OSGi Framework can only be configured to use one adaptor.

In Eclipse 3.2 a new hookable adaptor has been included that is used by default as the framework adaptor. The framework adaptor API has remained unchanged for the most part in Eclipse 3.2. What has changed is the actual implementation of the adaptor API. A new implementation of the adaptor API is now included which provides hooks that others can implement to provide functionality to the adaptor implementation.

Hookable Adaptor
----------------

The hookable adaptor is implemented in the package org.eclipse.osgi.baseadaptor. This adaptor implementation provides all of the default behavior required of a FrameworkAdaptor to provide an OSGi R4 compliant Framework. It also provides many hooks that allow others to insert additional functionality into the framework through what are called framework extension bundles. See the OSGi Core Specification chapter 3.15 "Extension Bundles" for more information.

Framework extension bundles are fragments of the system bundle (org.eclipse.osgi). As a fragment they can provide extra classes which the framework can use. Framework extensions which provide hook implementations must be identified before the framework is launched so that the content of the extension is available when the adaptor is created and used. To do this the **osgi.framework.extensions** property can be used to specify a list of framework extension names. A framework extension bundle can define a set of hook implementations that are configured with the hookable adaptor (using a hookconfigurators.properties file).

When using the **osgi.framework.extensions** property to specify a framework extension bundle the bundle symbolic name of the extension bundle must be used. In order for the extension bundle to be found and added to the framework implementation classpath it must be co-located in the same directory as the org.eclipse.osgi bundle at runtime and it must be a single jar (a directory does not work). In a normal eclipse runtime installation you would add your binary framework extension bundle jar to the directory eclipse/plugins/ of your eclipse installation. You also need to add the necessary **osgi.framework.extensions** property to the config.ini of you eclipse installation. For example, you have a framework extension bundle with the bundle symbolic name org.eclipse.equinox.samples.simplehook contained in the binary jar org.eclipse.equinox.samples.simplehook\_1.0.0.jar and eclipse is installed in the root directory of your PC. Place the file org.eclipse.equinox.samples.simplehook\_1.0.0.jar in the following directory structure of your eclipse installation:

`/eclipse/plugins/org.eclipse.osgi_3.2.0.v20060510.jar
/eclipse/plugins/org.eclipse.equinox.samples.simplehook_1.0.0.jar

    /eclipse/plugins/org.eclipse.osgi_3.2.0.v20060510.jar
    /eclipse/plugins/org.eclipse.equinox.samples.simplehook_1.0.0.jar` 

Add the following property to the /eclipse/configuration/config.ini file:

`osgi.framework.extensions=org.eclipse.equinox.samples.simplehook

    osgi.framework.extensions=org.eclipse.equinox.samples.simplehook` 

When developing (and self-hosting) a framework extension bundle in a workspace you must also import the source for org.eclipse.osgi into the same workspace. This will co-locate your framework extension bundle and the org.eclipse.osgi bundle in your workspace directory to allow the framework to discover your framework extension bundle specified by the **osgi.framework.extensions** property. When self-hosting a framework extension an easy way to set the **osgi.framework.extensions** property is by setting the VM argument when launching (e.g. -Dosgi.framework.extensions=org.eclipse.equinox.samples.simplehook)

Due to a bug ([Bug 206611](https://bugs.eclipse.org/bugs/show_bug.cgi?id=206611)), you **must** add the version number as a suffix to the jar, like "`jar.file_1.0.0.0.jar`", "`jar.file.jar`" won't be found!

### The Base Adaptor

The class org.eclipse.osgi.baseadaptor.BaseAdaptor implements the interface org.eclipse.osgi.framework.adaptor.FrameworkAdaptor. This class is used by default as the adaptor of the framework. You should avoid extending this class, instead you should use hook implementations to add functionality to the BaseAdaptor.

In some cases it may be impossible to do what you want with the current set of adaptor hooks. In this case you may be forced to extend the BaseAdaptor class to provide your own adaptor implementation. If you find yourself in this situation then you should open a bug against Framework Equinox requesting a new hook method or interface.

### The Hook Registry

The hook registry is implemented in the class org.eclipse.osgi.baseadaptor.HookRegistry. The hook registry is used to store all the hooks configured in the framework. When the hook registry is initialized it will discover all the hook configurators installed in the framework and will call on them to add hooks to the registry. The BaseAdaptor uses the hook registry at runtime to lookup and use the different hooks configured with the registry.

#### Hook Configurators

Hook configurators must implement the org.eclipse.osgi.baseadaptor.HookConfigurator interface. Hook configurators can add one or more hook implementations to the hook registry using the various add methods on the registry.

#### Discovering Hook Configurators

In order for a hook configurator to be discovered by the hook registry its implementation must be accessable by the framework's classloader. This implies that hook configurators must be built into the framework itself (org.eclipse.osgi) or be supplied by a framework extension bundle. Again a framework extension bundle is really just a fragment to Framework (i.e org.eclipse.osgi or the System Bundle).

A hook configurator also must be declared in one of the following ways to indicate to the hook registry which classes should be loaded as hook configurators.

##### hookconfigurators.properties files

A hookconfigurators.properties file can be used to declare a list of hook configator classes. The key hook.configurators is used in a hook configurators properties file to specify a comma separated list of fully qualified hook configurator classes. For example, the Equinox Framework (org.eclipse.osgi.jar) is shipped with a default set of hook configurators specified in a hookconfigurators.properties file:

`hook.configurators= 
   org.eclipse.osgi.internal.baseadaptor.BaseHookConfigurator,
   org.eclipse.osgi.internal.baseadaptor.DevClassLoadingHook,
   org.eclipse.core.runtime.internal.adaptor.EclipseStorageHook,
   org.eclipse.core.runtime.internal.adaptor.EclipseLogHook,
   org.eclipse.core.runtime.internal.adaptor.EclipseErrorHandler,
   org.eclipse.core.runtime.internal.adaptor.EclipseAdaptorHook,
   org.eclipse.core.runtime.internal.adaptor.EclipseClassLoadingHook,
   org.eclipse.core.runtime.internal.adaptor.EclipseLazyStarter,
   org.eclipse.core.runtime.internal.stats.StatsManager,
   org.eclipse.osgi.internal.verifier.SignedBundleHook

    hook.configurators= 
       org.eclipse.osgi.internal.baseadaptor.BaseHookConfigurator,
       org.eclipse.osgi.internal.baseadaptor.DevClassLoadingHook,
       org.eclipse.core.runtime.internal.adaptor.EclipseStorageHook,
       org.eclipse.core.runtime.internal.adaptor.EclipseLogHook,
       org.eclipse.core.runtime.internal.adaptor.EclipseErrorHandler,
       org.eclipse.core.runtime.internal.adaptor.EclipseAdaptorHook,
       org.eclipse.core.runtime.internal.adaptor.EclipseClassLoadingHook,
       org.eclipse.core.runtime.internal.adaptor.EclipseLazyStarter,
       org.eclipse.core.runtime.internal.stats.StatsManager,
       org.eclipse.osgi.internal.verifier.SignedBundleHook` 

Quite a few hook configurators are automatically enabled by default within the Equinox Framework. The only hook configurator required by Equinox to be a fully functional OSGi R4 Framework is the org.eclipse.osgi.internal.baseadaptor.BaseHookConfigurator. All other configurators declared above add extra functionality needed by eclipse and may be disabled if you do not require them.

Extension bundles may provide their own hookconfigurators.properties file to specify additional hook configurators. The hook registry will discover all hookconfigurator.properties files on its classpath and will merge all declared configurator classes into one list.

##### osgi.hook.configurators property

The osgi.hook.configurators configuration property is used to specify the list of hook configurators. If this property is set then the list of configurators specified will be the only configurators used. If this property is set then the hookconfigurators.properties files will not be processed for additional configurators. This property can be used in a config.ini to lock down the set of configurators to a specific set.

##### osgi.hook.configurators.include property

The osgi.hook.configurators.include configuration property is used to add additional hook configurators. This is helpful for configuring optional hook configurators. Hooks that should be enabled by default should be included in a hookconfigurators.properties file. This property is ignored if the osgi.hook.configurators is set.

##### osgi.hook.configurators.exclude property

The osgi.hook.configurators.exclude configuration property is used to exclude any hook configurators. This is helpful for disabling hook configurators that are specified in hookconfigurators.properties files. This property is ignored if the osgi.hook.configurators is set.

Hook interfaces
---------------

The org.eclipse.osgi.baseadaptor.hooks package contains the interface definitions for hooks that may be configured with the hook registry. Hook objects should be considered singletons. A hook configurator is responsible for constructing the singleton hook object and registering it with the hook registry. The singleton hook objects are then used by the adaptor to perform a specific task. The exception to this rule is the StorageHook interface. A StorageHook may create one StorageHook object for each bundle installed in the framework if it is required.

### Adaptor Hook

An AdaptorHook hooks into the BaseAdaptor class. This hook is useful for inserting code into the lifecycle operations of the framework like start, stopping and stop. For example, if you have an OSGi service that must be registered when the framework starts then you would use the AdaptorHook#frameworkStart to do so. This hook also has other methods for providing the FrameworkLog implementation, handing runtime errors etc.

### Bundle File Factory Hook

A BundleFileFactoryHook creates [#Bundle Files](#Bundle-Files). The BaseAdaptor implementation understands bundle files contained in jars or directories (the two typical formats used to deliver plug-ins in eclipse). Additional formats can be supported by supplying a BundleFileFactoryHook which understands the a different format.

### Bundle File Wrapper Factory Hook

A BundleFileWrapperFactoryHook creates objects which wrap [#Bundle Files](#Bundle-Files). Wrapper objects are useful when you need to intercept access to bundle file content. For example, this hook is useful for implementing signed bundle support where each entry of a bundle file should be verified against a signature.  
**Warning:** Cached plugins will be loaded from cache, and load much less files with the BundleFile.getEntry(...) method. If you would like to convert, for example, each plugin.xml of each plugin on each startup, you have to start Eclipse always with the "`-clean`" option.

### Bundle Watcher Hook

The BundleWatcher is the only hook interface which is not defined in the org.eclipse.osgi.baseadaptor.hooks package. The BundleWatcher interface is part of the org.eclipse.osgi.framework.adaptor package. This hook is useful for tracking bundle lifecycle operations. This inteface provides additional START/END operation types that are not available on the org.osgi.framework.BundleListener. For example, with this hook you can track when an installation process has started and ended.

### Class Loading Hook

The ClassLoadingHook is used to add features to the bundle classloader. This hook is used to add such capabilities as searching for native code, adding classpath entries to a bundle, creating classloaders, and modifying class bytes.

### Class Loading Stats Hook

The ClassLoaderStatsHook allows a hook to record statistics about classloading. This hook is called before and after each class and resource load request. Other interesting things can be done with this hook such as lazy bundle activation on first class load.

### Storage Hook

The StorageHook is used to persist data for each bundle installed in the Framework. This hook is the only hook type that is not considered a singleton. Each bundle installed in the system will get one instance of each StorageHook type associated with it.

Bundle Files
------------

The org.eclipse.osgi.baseadaptor.bundlefile package contains the BundleFile API. The BundleFile class is used to abstract access to the bundle content. Conceptually a BundleFile is similar the the ZipFile class in java.util.zip. The Equinox Framework will use a BundleFile to access all content from a bundle. Different BundleFile types can be used to support alternative bundle formats. By default the Equinox Framework supports zipped bundles (ZipBundleFile) and bundles extracted into a directory (DirBundleFile). A BundleFileFactoryHook can be used to add support for additional bundle file formats.

Class Loaders
-------------

Built-in Hook Configurators
---------------------------

The Equinox Framework includes a hookconfigurators.properties file that specifies a list of hook configurators which are enabled by default. The only required hook configurator is org.eclipse.osgi.internal.baseadaptor.BaseHookConfigurator. All other hook configurators provide hook implementations to add extra functionality needed by the rest of eclipse.

### DevClassLoadingHook configurator

The org.eclipse.osgi.internal.baseadaptor.DevClassLoadingHook hook configurator adds a ClassLoadingHook implementation needed for running bundles from the Eclipse development environment. This ClassLoadingHook will add additional classpath entries for the plug-in projects being developed in your workspace. The extra classpath entries are needed to load the classes from the output folders of your plug-in projects. This hook configurator will only add the ClassLoadingHook if the platform is in development mode (i.e. launched with the -dev option or the system property osgi.dev is set).

### EclipseStorageHook configurator

The org.eclipse.core.runtime.internal.adaptor.EclipseStorageHook hook configurator adds a StorageHook implementation which provides the following functionality:

1.  Caches additional data for each bundle from the bundle manifest. This improves startup performance by reading the data from the cache instead of reading and parsing each bundle manifest on startup.
2.  Converts old-style Eclipse plugin.xml files to bundle manifests for backwards compatibility reasons.
3.  Stores data required to support the Eclipse-LazyStart bundle manifest header.

### EclipseLogHook

The org.eclipse.core.runtime.internal.adaptor.EclipseLogHook hook configurator adds an AdaptorHook implementation which creates a log implementation for persisting log messages. This hook also registers a separate FrameworkLog service used to log performance messages.

### EclipseErrorHandler

The org.eclipse.core.runtime.internal.adaptor.EclipseErrorHandler hook configurator adds an AdaptorHook implementation for handling runtime errors. This hook will exit the platform if a runtime exception of type VirtualMachineError or ThreadDeath is thrown. If the system property eclipse.exitOnError=false then the platform will not exit on these errors.

### EclipseAdaptorHook

The org.eclipse.core.runtime.internal.adaptor.EclipseAdaptorHook hook configurator adds an AdaptorHook implementation which provides the following functionality:

1.  Registers xml parser services
2.  Registers Location services
3.  Registers URLConverter service
4.  Registers EnvironmentInfo service
5.  Registers PlatformAdmin service
6.  Registers PluginConverter service
7.  Registers BundleLocalization service
8.  Stops lazy started bundles in correct dependency order on shutdown
9.  Sets org.osgi.framework.bootdelegation=* if it is not already set to any value
10.  Sets eclipse.ee.install.verify=false if it is not already set to any value

### EclipseClassLoadingHook

The org.eclipse.core.runtime.internal.adaptor.EclipseClassLoadingHook hook configurator adds a ClassLoadingHook which provides the following functionality:

1.  Searches for native libraries using the eclipse platform lookup strategy. This looks in specific directories in a bundle for a native library according to the values of the properties osgi.ws, osgi.arch, osgi.os, and osgi.nl.
2.  Searches for classpath entries according to the eclipse platform lookup strategy. This allows $arg$ variables to be used in Bundle-ClassPath entries to specify substitution for ws, os, and nl.
3.  Searches for external classpath entries that start with "external:"
4.  Defines Package objects for the bundle classloader

### EclipseLazyStarter

The org.eclipse.core.runtime.internal.adaptor.EclipseLazyStarter hook configurator adds a ClassLoadingStatsHook to provide the support for Eclipse-LazyStart header. This hook will activate a bundle that has Eclipse-LazyStart enabled upon first class load. This hook requires that EclipseStorageHook and EclipseAdaptorHook be configured in the hook registry in order to work.

### StatsManager

The org.eclipse.core.runtime.internal.stats.StatsManager hook configurator adds BundleWatcher and ClassLoadingStatsHook implementations to support org.eclipse.core.tools in gathering bundle and class loading statistics.

### SignedBundleHook

The org.eclipse.osgi.internal.verifier.SignedBundleHook hook configurator adds AdaptorHook and BundleFileFactoryWrapperHook implementations to support signed bundles. This hook also registers a CertificateVerifierFactory service which can be used by others to find out certificate information from signed bundles.

Examples
--------

