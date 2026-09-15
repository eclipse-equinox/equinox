## Overview

The Equinox team has been working with the OSGi Alliance to standardize
the Eclipse Lazy Start mechanism (i.e.
Eclipse-LazyStart/Eclipse-AutoStart headers). The current plan is to add
a lazy activation policy to the OSGi specification for the upcoming OSGi
R4.1 specification. For more details on the current OSGi design proposal
see [OSGi design](http://bundles.osgi.org/Design/LazyStart)

## Why should the Eclipse Community Care

The lazy activation policy built into the Equinox Framework is a
powerful concept which allows Eclipse to launch with as few bundles
active as possible. This concept is very important to Eclipse
applications for both scalability and startup performance.

### We already know that\!\! What has changed?

Yeah, this is old news. We have relied on this behavior for years.

This is true, but the support for this has only been available in the
Equinox OSGi Framework. This means your bundles are tied to the Equinox
Framework implementation and may not work as expected on other OSGi
Framework implementations.

The following is a summary of what has changed in the OSGi proposal from
what Equinox currently supports in version 3.2.

#### New Bundle-ActivationPolicy Header

Of course OSGi would not want to add a new header with **Eclipse** in
the header name (e.g. Eclipse-LazyStart). A new header has been proposed
Bundle-ActivationPolicy. See the [OSGi
design](http://bundles.osgi.org/Design/LazyStart) for more details.

#### Found some *bugs*

During the specification process a few *bugs* were found in the original
lazy activation policy of Eclipse. For example, there is a well known
ClassCircularityError bug
[bug 5875](https://bugs.eclipse.org/bugs/show_bug.cgi?id=5875). This has
lead to a subtle change in the way bundles are lazy activated. Consider
the following example:

A system contains three Bundles: X, Y and Z. Bundle X has an interface
x.X. Bundle Y has a class y.Y which implements x.X. Bundle Z has a class
z.Z which extends y.Y. In this example, a request is made to load class
z.Z. Class z.Z is thus the trigger class.

In Eclipse 3.2 the following occurs to lazy activate the bundles:

1.  When loading class z.Z the Framework notes that Bundle Z has a lazy
    activation policy.
2.  **Before** searching for the class the framework activates bundle Z
    then proceeds to find and define class z.Z.
3.  When defining z.Z the class y.Y is loaded from Bundle Y.
4.  The Framework notes that Bundle Y has a lazy activation policy.
5.  **Before** searching for the class the framework activates bundle Y
    then proceeds to find and define class y.Y.
6.  When defining y.Y the class x.X is loaded from Bundle X.
7.  The Framework notes that Bundle X has a lazy activation policy.
8.  **Before** searching for the class the framework activates bundle X
    then proceeds to find and define class x.X.
9.  Finally the class z.Z is returned to the client that caused it to
    load. In this case the activation order was be Z, Y, X.

This approach is flawed because bundles can be activated by a thread
while that thread is in the process of defining a class. There are two
important reasons this must be avoided.

1.  **Deadlock issues** - The thread defining a class must lock the
    classloader while it is defining a class (when it calls
    ClassLoader.defineClass). A thread locking a classloader of one
    bundle while activating another bundle will lead to deadlocks
2.  **ClassCircularityErrors** - Activating a bundle while defining a
    class can lead to scenarios that cause the thread defining the class
    to request the same class be loaded again while attempting to
    activate the bundle. This leads to the ClassCircularityErrors
    demonstrated in
    [bug 5875](https://bugs.eclipse.org/bugs/show_bug.cgi?id=5875).

The [OSGi design](http://bundles.osgi.org/Design/LazyStart) for lazy
activation uses the following approach to avoid these issues (using the
same X, Y, Z bundles from above):

1.  When loading class z.Z the Framework notes that Bundle Z has a lazy
    activation policy
2.  Bundle Z is added to the stack of bundles that must be activated
    after the trigger class (z.Z) is defined.
3.  When defining z.Z the class y.Y is loaded from Bundle Y.
4.  The Framework notes that Bundle Y has a lazy activation policy
5.  Bundle Y is added to the stack of bundles that must be activated
    after the trigger class (z.Z) is defined.
6.  When defining y.Y the class x.X is loaded from Bundle X.
7.  The Framework notes that Bundle X has a lazy activation policy
8.  Bundle X is added to the stack of bundles that must be activated
    after the trigger class (z.Z) is defined.
9.  Finally the class z.Z is successfully defined.
10. Now each bundle on the set is activated in LIFO order. In this case
    the activation order would be X, Y, Z.

Notice that the order of activation has been changed from Z, Y, X to X,
Y, Z.

## What do I need to change in my bundle

### Should I change to the new Bundle-ActivationPolicy Header?

The Equinox Framework version 3.3 will continue to support the
Eclipse-LazyStart and the deprecated Eclipse-AutoStart headers. Bundle
developers that want their bundles to work across other OSGi Framework
implementations should add the Bundle-ActivationPolicy header to their
manifest. In most cases you can simply add the following header

`Bundle-ActivationPolicy: lazy`

In many cases it is reasonable to also retain the old Eclipse-LazyStart
or Eclipse-AutoStart headers if you need your bundle to continue to work
on older versions of Eclipse.

#### The exceptions directive

When Eclipse-LazyStart is true, the 'exceptions' attribute specifies a
list of packages that **will not** cause the bundle to be activated when
classes are loaded from them. When Eclipse-LazyStart is false, the
'exceptions' attribute specifies a list of packages that **will** cause
the bundle to be activated when classes are loaded from them. The
exception rules apply to all classes in the listed packages.

The new Bundle-ActivationPolicy header has directives ('include' and
'exclude') that can be used to include or exclude packages in lazy
activation policy. Consider the following Eclipse-LazyStart header:

`Eclipse-LazyStart: true; exceptions="org.eclipse.foo1, org.eclipse.foo2"`

The example specifies that a bundle must be activated for any classes
that are loaded from this bundle, except the classes in the packages
'org.eclipse.foo1' and 'org.eclipse.foo2'. The equivalent
Bundle-ActivationPolicy would be the following:

`Bundle-ActivationPolicy: lazy; exclude:="org.eclipse.foo1, org.eclipse.foo2"`

When the value of false is used for the Eclipse-LazyStart then the
packages listed in the 'exceptions' attribute are the only packages that
will cause a lazy activation. Consider the following Eclipse-LazyStart
header:

`Eclipse-LazyStart: false; exceptions="org.eclipse.foo1, org.eclipse.foo2"`

The example specifies that a bundle must be activated for only classes
the classes in the packages 'org.eclipse.foo1' and 'org.eclipse.foo2'
that are loaded from a bundle. The equivalent Bundle-ActivationPolicy
would be the following:

`Bundle-ActivationPolicy: lazy; include:="org.eclipse.foo1, org.eclipse.foo2"`

### What about the activation order changes?

Yeah in the above example the activation order was changed from Z, Y, X
to X, Y, Z. That must break backwards compatibility?\!\!

Depending on the activation order in the above example to be Z, Y, X is
very brittle and unpredictable. For example, nothing prevents another
thread from accessing classes in Bundle X first causing it to be
activated before Bundle Z is activated. The contract for lazy activation
is that the bundle will be activated before a client uses a class loaded
from that bundle. The new design still honors this contract but it
improves upon the original Eclipse design to fix the deadlock and
ClassCircularityError issues.

We expect very few (if any) bundles will need to worry about this
change. Please let us know if you encounter a situation where this new
approach breaks you.

[Category:Equinox](Category:Equinox "wikilink")