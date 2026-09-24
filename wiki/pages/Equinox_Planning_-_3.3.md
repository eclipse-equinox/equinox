Equinox Planning - 3.3

## General Planning Notes

**Note:** This is just a random collection of notes and thoughts and
does not reflect the actual committed plan items for the Equinox team.

  - Declarative Services
      - Implementation
      - Tooling

<!-- end list -->

  - Update Manager
      - The update manager code ownership is migrating from Toronto to
        Ottawa. We need to keep on top of bug reports and current
        problems to ensure that nothing slips by for 3.2.1.
      - We need to investigate refactoring the JarProcessor code. It was
        put into the Update bundles at the end of the 3.2 cycle but is
        more generally useful.
      - JarProcessor - might be some more work here w.r.t. JAR signing
        and nested JARs

<!-- end list -->

  - Provisioning
      - Prototype
      - Investigate MEG
      - How to deliver Eclipse?
          - Small installer download?
          - Dynamic web content that builds zip?

<!-- end list -->

  - PDE/Build
      - Continue investigation into inter-operability with Maven
      - Incremental building

<!-- end list -->

  - Component Model
      - The programming model.
      - Investigate Spring, DS, etc.
      - We want to get people off the Platform class.
      - Need to have something easy for people to use when they migrate
        their code.

<!-- end list -->

  - OSGi Framework (refactor/etc)

<!-- end list -->

  - Investigation into Security
      - Login authentication
      - Securing the code base (Java 2 security)

<!-- end list -->

  - Launching
      - Launching the VM in-process
      - Splash Screen
      - fast workspace switching (currently we don't have any lifecycle
        events for location areas)

<!-- end list -->

  - Preferences
      - API fixes
      - use everywhere - get people to adapt and get off the old JFace
        and Runtime prefs code
      - should we create more scopes?
      - Work on an RFC for scopes as they seem generally useful.
        (particularly for searching)
      - use common storage solution (described below)

<!-- end list -->

  - Version Tools
      - We have started working on some version verification tools to
        help people cope with the new plug-in (and feature) version
        number story.
      - Super cool goal is to complete these tools and have them
        integrated into the SDK.
      - Cool goal is to have them available as either part of the Core
        Tools or PDE Tools.

<!-- end list -->

  - Storage Service
      - Many different components have to serialize and they all roll
        their own storage solution whether it be using Properties files,
        ObjectOutputStreams, or another format.
      - We should investigate a common storage service that multiple
        components can leverage.

<!-- end list -->

  - Service Registry
      - Lazy Tracking
      - Scalability issues for event dispatching and registry impl

<!-- end list -->

  - Refactor Test Suites
      - We used to have one test plug-in for each plug-in in the SDK.
      - Now that we have refactored the runtime we should also refactor
        the test suites to match the new bundle structure.

## Milestone Plans

### Eclipse 3.3 M4 - Friday, December 15, 2006

#### New Lancher Work

#### API Tooling

The goal of this work is to provide a tool to help check backward
compatibility on an API level and trace API and non-API dependencies
both upstream and downstream. It is likely that some of the work will be
done in cooperation with the PDE UI team. In M4 we work on:

  - Initial work on the engine for collection and comparison of APIs -
    to be used for backward compatibility checks
  - Initial work on the tool for collection of references to
    APIs/non-APIs in other projects - allowing developers to trace usage
    of their code by other plug-ins
  - Find a home in one of the incubators for this work

#### The Supplement Bundle

Generally speaking, `the org.eclipse.supplement` bundle is a collection
of classes that are required for the registry to run outside of OSGi. It
also should contain the classes necessary to run the registry on another
framework.

Currently this bundle lives in a separate project in the Eclipse
repository so therefore the classes are copied from the org.eclipse.osgi
bundle and live in 2 locations. We need to make it easier to maintain
the code whether it be by combining the projects into one so there is
only one code base, or by some other means.

The goal for 3.3 M4 is to have the supplement bundle work completed. The
code should be maintained from a single location be able to run both
with and without OSGi present.

#### Orbit

Continued contribution of bundles to the Orbit project.

### Eclipse 3.3 M5 - Friday, February 9, 2007

#### API Tooling

The goal is to have something consumable by end users for this
milestone. Build details still need to be worked out, but we would like
to at least have something that the user can download and
build/install/use.

#### Orbit

  - Contribute the JMX bundles which are currently part of the Equinox
    Incubator.
  - Verify that all the Eclipse SDK bundles that are intended to be a
    part of Orbit, have been contributed

#### Preferences

There are some lingering bugs that we will take care of as well as
releasing helper code for plug-in developers who are creating their own
preference scopes. *Deferred to next milestone*

#### Framework Handler

We are currently investigating a common API for handling framework and
launcher configuration and startup. This involves creating an API for
setting parameters, etc which will work across multiple frameworks. We
intend release the initial APIs into the Equinox incubator for comment
for the M5 milestone.

#### Equinox Launcher

The new equinox launcher is now in. The following items need to be
addressed for M5

  - Determine impact on build/export
    [168616](https://bugs.eclipse.org/bugs/show_bug.cgi?id=168616)
  - Support launching java.exe in the old way,
    [168775](https://bugs.eclipse.org/bugs/show_bug.cgi?id=168775)
  - Figure out problems on x86_64, ppc, & aix.
    [168271](https://bugs.eclipse.org/bugs/show_bug.cgi?id=168271)
    [168278](https://bugs.eclipse.org/bugs/show_bug.cgi?id=168278)
    [168281](https://bugs.eclipse.org/bugs/show_bug.cgi?id=168281)
  - Console issues on Windows
    [167310](https://bugs.eclipse.org/bugs/show_bug.cgi?id=167310)
  - support reading a VM description file

#### Jobs

  - The org.eclipse.core.jobs plugin contains APIs that would be useful
    in standalone Java applications. We should ensure this

plugin can be used simply by adding it to the classpath of an arbitrary
Java application. In particular, see
[124968](https://bugs.eclipse.org/bugs/show_bug.cgi?id=124968).

  - The debug framework has a notion of background units of work:
    IRequest. This is currently not based on jobs because some debugger
    implementations may not want to use the job manager's scheduling and
    thread pool to execute these tasks. Investigate whether there is an
    interesting abstraction that can be pulled from the commonality
    between IRequest and Job.

### Eclipse 3.3 M6 - Friday, March 23, 2007 (API Freeze)

#### EclipseCon

  - EclipseCon 2007 is March 5-8, 2007 so a lot of time has been put
    into talks, tutorials, and demos for the conference.

#### API Tooling

#### Orbit

  - Ensure that all relevant Platform bundles that can be contributed to
    Orbit, are.
  - Help the Release Engineering team modify the Platform builds to
    consume as many Orbit bundles as possible.

### Eclipse 3.3 M7/RCO - Friday, May 4, 2007 (Feature Complete)

#### Orbit

  - Help release Ant wrapper to SDK builds
  - Work through remaining issues with JUnit and wrapper bundle
  - Verify license files in platform contributed bundles
  - More work on the IP Log file

#### Performance

  - A performance dynamic team will be created and we will participate.

[Planning 3.3](Category:Equinox "wikilink")