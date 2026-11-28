## Overview

The Equinox Framework for the Luna release (Equinox 4.4 release) is an
implementation of the OSGi R6 Framework specification. The OSGi R6 Core
framework specification (finalized in March 2014) will contain
enhancements in the following areas:

  - Introduction of Service Scopes to the OSGi Service Registry
    ([RFC 195](http://www.osgi.org/Specifications/Drafts))
  - Improvements of Weaving Hooks
    ([RFC 191](http://www.osgi.org/Specifications/Drafts))
  - Clarification of hooks on the system bundle
    ([RFC 198](http://www.osgi.org/Specifications/Drafts))
  - Native environment namespace
    ([RFC 188](http://www.osgi.org/Specifications/Drafts))
  - Data Transfer Objects
    ([RFC 185](http://www.osgi.org/Specifications/Drafts))
  - Addition of FrameworkWiring.findProviders

From an Equinox perspective these are considered incremental
enhancements. Most, if not all, of these enhancements are in place for
the Luna integrations builds already. A majority of the development
effort during the Luna release is focused on refactoring and in many
cases rewriting the core Equinox Framework implementation to be based on
the OSGi generic dependency model. There are a number of reasons to do
this. Please see the [Equinox
presentation](http://www.eclipsecon.org/2013/sites/eclipsecon.org.2013/files/EclipseCon%202013%20-%20Equinox_0.pdf)
given by Tom Watson at EclipseCon US 2013 for some background. Also see
for the umbrella bug for moving to the new framework. The following
sections discuss the main issues that the community should be aware of
when moving to the Equinox Luna Framework implementation. Please direct
any questions or comments you may have to the [equinox-dev mailing
list](https://dev.eclipse.org/mailman/listinfo/equinox-dev)

## Replacing the Equinox Resolver

The package org.eclipse.osgi.service.resolver contains the Equinox
resolver API. This API and the implementation of that API has been used
by the Equinox framework ever since the Eclipse 3.0 release. Over that
time many things have changed and many things have been learned. The
Equinox Resolver API and it use came about as an afterthought for the
framework implementation and had to evolve over time as changes were
made to the OSGi Core Framework specification. The end result is not
optimal (to put it lightly). There are many issues with the interactions
of the core framework with the resolver implementation.

  - A good locking strategy for thread safety is not well thought out
    and in some cases non-existant.
      - This results in several, hard to reproduce, dead-locks or
        concurrency issues.
  - The Equinox resolver API is strongly typed with respect to
    dependency types.
      - This means that each time OSGi introduces a new type of
        requirement then new API is required to be added to the Equinox
        resolver API.
      - A few OSGi specification releases ago the specification moved to
        defining all OSGi dependency types in terms of a generic
        dependency model. This makes the current implementation, using
        strongly typed dependencies, awkward when considering the
        concepts documented the core specification.
      - Along with the generic dependency model came a new [wiring
        API](http://www.osgi.org/javadoc/r5/core/org/osgi/framework/wiring/package-summary.html).
        This wiring API more accurately models the dependencies in OSGi
        and is a replacement for the deprecated [PackageAdmin
        service](http://www.osgi.org/javadoc/r5/core/org/osgi/service/packageadmin/package-summary.html).
      - The equinox resolver has to do many awkward transformations in
        order to express the internal strongly typed dependencies into
        generic wiring types.

Instead of trying to live with the strongly typed resolver API and do
major work to implement an overall locking strategy, we have decided to
stop using the equinox resolver API altogether in the framework
implementation. We have implemented a container which is used to manage
(install, uninstall, update) generic metadata.

The container becomes the central brain of the core framework
implementation and is responsible for the following:

  - Managing resources
      - Access to meta-data, in the form of generic capabilities and
        requirements from the OSGi wiring API.
      - Lifecycle operations (install, update, start, stop, uninstall)
      - Persistences of the state of the container
  - Resolving dependencies
      - Uses an OSGi R5 Resolver service
  - Provides a central concurrency and locking strategy
      - Properly handle call outs (hooks) while holding no locks
      - Allow for re-entrant read/write locks that allow for multiple
        readers to access the data.
  - Provides a generic API that can be used outside the framework

For the framework, the meta-data associated with a revision comes from a
bundle's manifest, but the meta-data installed into the container does
not care how the meta-data is declared. A module revision builder is
used to create bundle a
[revision](http://www.osgi.org/javadoc/r5/core/org/osgi/framework/wiring/BundleRevision.html)
for installation and update of revisions in the container.

## Redoing the Equinox Framework Specific Hooks

The Equinox Framework has a number of powerful framework specific
[Adaptor_Hooks](Adaptor_Hooks "wikilink"). Much of the internal details
have changed which requires an overhual of the Equinox implementation
specific Hooks. As a result ALL existing Equinox Hook implementations
are broken and will need to migrate.

  - Equinox 3.9 (Kepler) had many different hook interfaces (eight
    altogether)
  - Luna framework has a total of 4 hooks
      - Class Loading Hook - An abstract class that a hook extends.
        Allows for new methods to be added with default implementation
        to avoid breaking hook implementations
      - Activator Hook - Hooks into the activation and shutdown process
        of the framework
      - Bundle File Wrapper hook - Allows a hook to wrap access to a
        bundle file archive (jar, directory)
      - Storage Hook - Allows a hook to persist data into the framework
        storage for each bundle resource in the framework.

In most cases it should be possible to migrate existing hook
implementations over to the new API. Keep in mind that most of the
internal types representing bundle have changed.

## Removal of Old Style Plugin Support

For Luna the framework will no longer support old style Eclipse Plugins
(see ) by default. In order to support the migration to OSGi bundles
during the Eclipse 3.0 release the Equinox framework had support for
transforming old style plugins, into real OSGi bundles at runtime. This
support will no longer be built directly into the framework. A
compatibility fragment called org.eclipse.osgi.compatibility.plugins may
be installed that adds back an implementation of the
org.eclipse.osgi.service.pluginconversion.PluginConverter and the
support for old style Eclipse Plugins at runtime. This compatibility
fragment will need to be installed if you want to done one of the
following:

  - Run old style Eclipse plugins on a Luna based Equinox framework
  - Use PDE from a Luna Eclipse build to develop old style Eclipse
    plugins
  - Use p2 to publish old style Eclipse plugins to a repository

## Removal of the PlatformAdmin Service Implementation

This is directly related to the topic of replacing the Equinox Resolver.
The service interface org.eclipse.osgi.service.resolver.PlatformAdmin is
the base service which other bundles can use to get access to the system
resolver State or create sandbox resolver State objects. This is useful
for gather information about the running state or to model a set of
bundles outside of a framework. For example, in PDE and Tycho to model
the class path when developing and building bundles.

For the Luna release the Equinox framework will no longer provide an
implementation of the org.eclipse.osgi.service.resolver.PlatformAdmin by
default. A compatibility fragment called
org.eclipse.osgi.compatibility.state can be installed that adds back an
implementation of PlatformAdmin. This PlatformAdmin implementation has
some limitations because it is no longer tied directly to the
implementation of the framework. These limitations are all related to
getting information about the running state. The most reliable way to
get information about the running framework is to use the OSGi [wiring
API](http://www.osgi.org/javadoc/r5/core/org/osgi/framework/wiring/package-summary.html).
The compatibility fragment works great for scenarios that use
PlatformAdmin to create sandbox State objects for modeling OSGi bundles
outside of the framework (e.g. PDE, Tycho). The
org.eclipse.osgi.compatibility.state fragment has been added to the RCP
feature and various other Equinox features (see ).

## Requirement on Java 6

Java 6 is now required by the core Equinox OSGi Framework
implementation. The main reason is to gain access to a few new methods
which are useful for implementing the locking strategy of the core
framework implementation and use some of the other convenience methods
in Java 6.

[Category:Equinox](Category:Equinox "wikilink")