## Configurator

Configurator is a concept of an entity that configures bundles state
(install bundles, start bundles, and uninstall bundles) according to the
information retrieved from some location. We call a bundle, which does
it when it starts, “configurator bundle”.

## Examples of Configurator

We have two examples of Configurator bundle; the first one is
UpdateConfigurator, whose main job is searching bundles in the
predefined directory (plugins/) and install all of them as the current
“org.eclipse.update.configurator” plugin does. The second one is
SimpleConfigurator, which will be introduced below.

### SimpleConfigurator

SimpleConfigurator is an implementation of Configurator. In a nutshell,
at its startup, SimpleConfigurator does the followings;

1.  It will get to know the location of the file to be referred by
    calling BundleContext\#getProperty() with the predefined name as a
    key.
2.  It will retrieve the information from the file, which is a text file
    containing a simple list of org.eclipse.equinox.frameworkadmin
    pluginbundles with start level and flag of being started or not.
    (Each line in the file is comma separated strings and has
    information of one bundle, e.g. bundle location, startlevel, flag of
    being started or not)
3.  It will install, set start level, and start bundles according to the
    retrieved information.

For more detail, see Javadoc of BundleActivator of the plugin.

## Configurator Manipulator

If a configurator bundle is listed in the initial bundles list of a fw
config file, the bundles state when fw startup process completes would
be affected by what the configurator bundle does. Therefore, if a
Manipulator implementation takes a configurator bundle into
consideration for its saving config files or expecting bundles state, it
would be beneficial.

We assume as follows.

  - What a client wants to realize is just making bundles state when fw
    startup process completes as the client desires.
      - A client does NOT care about what bundles are installed directly
        by a fw (according to the initial bundles list of the fw config
        files) nor what bundles are installed by a configurator bundle.
  - When loading information from the saved config files, what a client
    wants to know is not only bundles installed directly by a fw but the
    final bundles state when a fw startup completes.

Therefore, it is recommended that these matters are handled
automatically by the FwHandler implementation.

In order to implement such FwHandler objects in configurator bundle
independent way, we would propose an interface, called
ConfiguratorManipulator. For details about ConfiguratorManipulator, see
javadoc of the interface.

### Provider of ConfiguratorManipulator

An implementation of ConfiguratorManipulator must be tightly coupled
with the corresponding Configurator Bundle. Therefore, we assume that
implementator of a Configurator Bundle should implement the
ConfiguratorManipulator.

ConfiguratorManipulator object will be registered in a service registry
with a service property keyed by the following:

  - ConfiguratorManipulator.SERVICE_PROP_KEY_CONFIGURATOR_BUNDLESYMBOLICNAME:
    String; a symbolic name of target Configurator bundle.

### Clients of ConfiguratorManipulator

ConfiguratorManipulator will be used mainly in a Manipulator
implementation. A client of this interface can get to know what kinds of
ConfiguratorBundles can be manipulated by registered
ConfiguratorManipulator services by checking the service property.

## Configurator interface

Currently, we also define Configurator interface. It provides methods
for a client bundle to

  - Apply the bundles state control according to the information
    retrieved from the specified URL to the runtime OSGi environment.
  - Expect bundles state if the bundles state control according to the
    information retrieved from the specified URL were applied to the
    runtime environment, virtually.
      - What kinds of information it retrieves and how it retrieves the
        information from the specified URL are dependent on the
        Configurator implementation.

In the implementation of SimpleConfigurator Bundle, this object is
created and used at its start. However, if there are no needs to apply
bundles state control to the runtime OSGi environment, we don’t need
this interface open to clients.

<font color="red">While this API was located in
org.eclipse.equinox.frameworkadmin plugin until ver. 1.2, it was moved
to org.eclipse.equinox.simpleconfigurator plugin since
org.eclipse.equinox.frameworkadmin plugin of version 1.3.0 according to
the request of
[bug\#175809](https://bugs.eclipse.org/bugs/show_bug.cgi?id=175809).
</font>

## Current Assumption of Configurator Bundle

It is assumed that there is no more than one bundle in an initial
bundles list of a fw config file that controls bundles’ state. If there
are several such bundles in an initial bundles list, the bundles state
after all launching process is completed would be more complicated. The
current implementation of Manipulator only supports the first case. In
addition, it is assumed that a bundle installed and started by a
Configurator Bundle will not control bundles’ state at its start.

## References

1.  [Equinox
    FrameworkAdmin](http://wiki.eclipse.org/index.php/Equinox_FrameworkAdmin)

[Category:Equinox](Category:Equinox "wikilink")