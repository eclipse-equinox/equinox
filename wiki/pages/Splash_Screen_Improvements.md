The [Equinox Launcher](Equinox_Launcher "wikilink") is the current
proposal from the framework's perspective for implementing a splash
screen that uses SWT. This page cover's the UI perspective.

Improving the launcher is one plan item
([bug 154088](https://bugs.eclipse.org/bugs/show_bug.cgi?id=154088)) for
Eclipse 3.3. One aspect of this is the splash screen. Currently the
splash screen is handled natively by the launcher in a separate process.
Being able to handle the splash screen from java would simplify the
launcher code and make it easier to add functionality without bothering
the SWT team.

### Terminology

We shall consider 3 "components" in this discussion:

1.  The Launcher executable. Native C code, responsible for starting
    java and displaying the splash screen.
2.  Startup.jar. Java Code, the entry point for starting eclipse.
    Creates a classloader for the EclipseStarter which starts the
    framework.
3.  Framework. Loading & Starting bundles, running the application.

## Problem Description

The splash screen must be displayed early. For large products with
thousands of bundles, the framework could take noticeable time to start
up. This means the splash screen should be first shown before starting
the framework, ie either in the Launcher or in the startup.jar.

## Current Implementation

The Launcher starts java and runs the startup.jar. The startup.jar execs
a new instance of the launcher which displays the splash screen. The
Framework communicates with this splash process by writing to its
standard input stream.
\==New Implementation== A number of different methods were prototyped to
see if we would be able to display an SWT splash screen early enough.
These attempts are recorded in
[bug 161569](https://bugs.eclipse.org/bugs/show_bug.cgi?id=161569). The
winning proposal is covered in the [Equinox
Launcher](Equinox_Launcher "wikilink") wiki page.

The Equinox Launcher will display a static bitmap splash screen using
native code. It will then load the vm shared library and start java
using JNI. The handle to the splash window is passed into Java. The
startup code will call back to the native code to process events during
startup. Once SWT is loaded, we can wrap a Shell around the splash
window using the handle. The key point to note is that the splash screen
was created in the same process and thread that the UI will eventually
be using. This is what allows us to wrap the native window in an SWT
Shell. At this point, the splash screen can be handled as normal SWT and
the UI can do what it wants with it.

## UI Embellishments

There are several use cases we would like to handle with the splash that
would become reasonable if we had an SWT splash screen. Although the
uses differ it seems possible to cover all cases with a single pluggable
splash framework.

### Use Cases

#### Vendor Branding

There is a certain class of Eclipse product that is explicitly an
amalgam of other products from multiple vendors. Branding such products
is a challenge because each component vendor wants some piece of the
splash visiblity. One solution is to provide a hand-crafted splash
screen incorporating design elements from all vendors but this solution
falls down when the set of components is dynamic. To handle this case
the platform could provide an extension point that would allow the
augmentation of the splash screen with supplemental icons. Using this
each vendor would provide images that would be shown at some specific
place in the splash and these icons would be shown as soon as the
framework is loaded (and the extensions parsed).

#### Enhanced Progress

Some applications have very complicated and time consuming startup
procedures. For instance, databases are loaded, servers are started,
cows are milked, etc. These applications often have a splash screen that
shows the progression of startup via icons. This is common in OS or
windowing system startup routines. It's conceivable that an RCP
application may want to do this style of splash screen. This differs
from the above scenario in that the icons do not simply just appear with
the framework - they appear as the application does meaningful work. The
platform could provide an API that could be called by startup clients to
update the splash in this way.

#### Ohhh Pretty Colors

Some vendors have very elaborate splash screens in their product, be
they irregular shaped shells or shells with particular alpha blending or
whatnot. It should be possible for these vendors to reproduce the splash
they use in other products within the Eclipse framework. For this
scenario the splash needs not only be extensible but entirely
replaceable. The code that generates the shell needs to be pluggable.

### Implementation

All use cases above could be covered by the following solution. This
assume that we can indeed a) get SWT running before the framework early
in the life-cycle b) we can reuse the SAME SWT instance for the
workbench.

The splash would be need to be pluggable at two layers. The first layer
of pluggability would exist in the startup jar level. Here we would have
code that creates the SWT display and creates the splash shell. The code
that creates the shell (and populates it with initial bitmap content)
should be pluggable so that custom shell shapes can be made. This
plugability might be accomplished in a rudimentary way by adding custom
classes to startup jar and specifying the class to use as a system
property. This kind of customization isn't likely to occur too
frequently so it needn't be terribly easy to accomplish.

The second layer of plugability comes into play once the workbench has
started initialization. At this stage we'd plug in a splash controller -
some single extension that is capable of owning and controlling the
splash content from this time forward. The platform would provide a
default implementation of this controller that provides the current
progress reporting (including color, placement, font, etc). Other
application developers (RCP or otherwise) could provide implementations
that hook in vendor images or elaborate progress or etc. This might be
plugged in via the workbench advisor directly or the extension could be
bound to a product (much like intro)

In order for this to work across all second-layer implementations the
workbench would have to change its startup routine. The first change is
trivial - instead of creating its own display it simply finds the one
that already exists. The other changes are more interesting. The startup
code would need to spin the event loop at wellknown milestones in the
startup so that the splash can update appropriately. For instance,
wherever we're pushing content to the launcher process now would need to
spin the event loop. There are likely other places where the loop needs
spinning as well in order for the splash to remain responsive (and
interesting).

[Category:Equinox](Category:Equinox "wikilink")