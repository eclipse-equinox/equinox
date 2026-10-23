## A recap of the situation

Today in Eclipse 3.2 the bundle classloader in Equinox delegates every
class/resource load to the boot class loader before using the normal
OSGi delegation. This is because we set the
org.osgi.framework.bootdelegation property to '\*' by default.

This has been the default to support backwards compatibility. Many
bundles expect to get access to all classes available from the VM boot
classpath without specifying the constraint in their bundle manifest
(i.e. Import-Package or Require-Bundle). This is not the default
behavior of OSGi R4. The OSGi specification mandates that a bundle
declare all package depenencies using either Import-Package or
Require-Bundle. The one exception to this rule is the java.\* packages
which is always delegated to the boot classpath. All other packages
dependencies must be declared in the bundle's manifest file.

## The problem

This is not a good default behavior for Eclipse for the following
reasons:

1.  Performance - When we delegate every class load to the boot
    classloader we end up with tons of ClassNotFoundExceptions because
    we ask the boot classloader for everything even though we know the
    classes should come from another bundle. For example, we delegate
    every org.eclipse.\* class. Measurements show bootdelegation is
    taking up to 5-7% of startup time to get to the JDK perspective.
2.  Isolation - Delegating every class load to boot classpath does not
    allow us to isolate bundles from the packages on the boot classpath.
    This prevents us from shipping and using our own version of the
    package in a bundle. See
    [bug 29007](https://bugs.eclipse.org/bugs/show_bug.cgi?id=29007) and
    [bug 145585](https://bugs.eclipse.org/bugs/show_bug.cgi?id=145585).
3.  Proper dependency declaration - Proper componentization requires
    that components accurately specify their dependencies. We should
    require that components (Bundles/Plug-ins) are accurately specifying
    their package dependencies.

## The solution

The proposed solution in
[Bug 162231](https://bugs.eclipse.org/bugs/show_bug.cgi?id=162231) is
the following:

1.  By default the org.osgi.framework.bootdelegation configuration
    property is not set. It used to be it is set to '\*'.
2.  A new option has been be added to Equinox to enable a backwards
    compatibility option. The option is
    **osgi.compatibility.bootdelegation**. This option is be enabled by
    default in the Equinox Launcher, but it defaults to false if the
    org.eclipse.osgi jar is run directly (set it to "true" to make it
    behave like the Equinox Launcher). When this option is enabled a
    last resort boot delegation occurs after all other steps in the OSGi
    delegation process have been exhausted.

The advantage of this approach is it gives us the performance
improvements and isolation we want, but it provides a level of
compatibility for bundles which expect access to all packages provided
by the boot classpath. During the 3.1 development cycle we attempted to
disable boot delegation by default. In the end we had to back out of
this change because we did not have a good story for backwards
compatibility. With the proposed solution we should have much better
success.

## The issues

### Tooling

We need help from PDE to flag warnings to developers that are accessing
packages from the VM without specifying proper dependencies in their
bundle manifests. See
[bug 164188](https://bugs.eclipse.org/bugs/show_bug.cgi?id=164188).

### Compatibility

Some bundles today include packages that are provided by the boot
classpath. For example, some bundles include their own versions of
javax.xml.parsers package. With org.osgi.framework.bootdelegation set to
'\*' these types of bundles are not actually loading classes from these
packages, instead they are getting the classes provided by the boot
classpath. This is probably not the behavior developers were expecting.

With the proposed changes these types of bundles will now be able to
load and use the classes they include in themselves because we will ask
their classloader for the class before asking boot. This is probably
more like the behavior the developer is expecting but it does add more
burden to such a bundle because now we will have multiple versions of
the same package available in the framework.

### System Bundle reexport

Currently the org.eclipse.core.runtime bundle uses Require-Bundle to
access the system bundle and reexports it (i.e. Require-Bundle:
org.eclipse.osgi; visibility:=reexport). The problem with this is that
the system bundle *org.eclipse.osgi* exports all packages available from
the current VM execution environement. If a bundle uses Require-Bundle
to access org.eclipse.osgi or any other bundle that reexports
org.eclipse.osgi (e.g. org.eclipse.core.runtime) they will see the
packages from the VM execution environment which the system bundle
exports. This will prevent a bundle from exporting their own version of
the classes provided by the boot classpath.

Bundles that want to provide their own version of a package that is on
the boot classpath must **not** use Require-Bundle to access the
packages in org.eclipse.osgi. This implies that the **must** use
Import-Package to access the packages they need.

## Bugs/Links

[Bug 162231 disable bootdelegation \* by
default](https://bugs.eclipse.org/bugs/show_bug.cgi?id=162231)

[Bug 29007 classloading extension - hide some system
packages](https://bugs.eclipse.org/bugs/show_bug.cgi?id=29007)

[Bug 145585 Need ability to "override" bootstrap
classes](https://bugs.eclipse.org/bugs/show_bug.cgi?id=145585)

[Bug 164188 Need tooling for access rules on vm
packages](https://bugs.eclipse.org/bugs/show_bug.cgi?id=164188)

[Boot Delegation](Category:Equinox "wikilink")