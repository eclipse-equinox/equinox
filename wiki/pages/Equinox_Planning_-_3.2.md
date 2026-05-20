## Framework

''' Priority items '''

  - Refactoring/refining of the framework
      - two different packagings, one with all the extra stuff in
        extensions (for maximum flexibility) and another with everything
        lumped together the way it is now (for maximum convenience)
      - Look for ways of reducing footprint
      - Look for ways to simplify the framework
  - Update the supplement bundle. It is supposed to be everything in
    org.eclipse.osgi that is not standard OSGi. Locations, NLS, ... We
    don't have a good build story around this so just copy the code for
    now.
  - Look at eliminating the use of our special library lookup techniques
    and investigate the standard OSGi mechanisms (e.g.,
    Bundle-NativeCode)
  - Update the quickstart guide and general how tos on the web
  - Application model: In 3.2 we investigated the MEG application model
    but ran out of time. We should reopen that investigation and see
    where we want to go for 3.3. The primary goal here should be to get
    more flexibility and control wrt the applications. Starting and
    stopping, querying, running several, ...
  - Server-side OSGi: The server side incubator has several parts that
    should be reviewed with an eye to graduating them out of the
    incubator and into the Bundles component. These have to be reviewed
    carefully in all aspects.
  - rework startup.jar: Pascal's favorite topic. This little JAR is
    hugely complicated and subtle. A nightmare to maintain and test.
    There are many different scenarios in which is it used and it does a
    number of things. It also turns out that much of this has to be done
    in situations such as WebStart, the Servlet Bridge and launching
    nested frameworks.
      - Investigate making startup.jar a bundle (both in form/name and
        in location and reality). This gets it out of the rootfile set
        and gets it versioned. As such it is more easily updated and
        managed. Further, if someone wants to launch a nested framework,
        they need this code (or a friend of it)
      - The goal here is to have all the complicated stuff in one place
        and then that is reused in the different scenarios (e.g.,
        imagine launching a nested framework while doing selfhosted
        development)
      - improve the mechanism for discovering osgi.framework.extensions
  - Service performance
      - Registry scaleability
      - ServiceTracker laziness and short circuit when no customizer
  - Security: there has been alot of work in the incubator over the past
    year. We should get up to date on it and what it all means.
    Primarily this means the Java 2 permissions and the code
    modifications. Need to investigate SWORD4J and get familiar with the
    process of securing bundles.

''' Other items '''

  - start all by default in simple configurator
  - resolver
      - breakout
  - file manager packaging
  - runtime clean-up
      - x-internal/friends in refactored runtime
      - run in strict mode/test cases
  - equinox builds/features/downloads
  - patching fragments
  - simple configurator - consolidate platform.cfg files from multiple
    install/updaters
  - component framework
  - be good OSGi citizens
  - overrides
  - security
      - credential store
      - JAAS
      - login
  - pervasive use of "uses"

## Declarative Services

  - implement optimizations
  - tooling

## Releng

''' Priority items '''

  - investigate how to generate componentized JavaDoc. Maven may have a
    story for this but in any event, we should understand how we can
    build JavaDoc for each bundle as we build the bundle.

''' Other items '''

  - create finer grained features (help, update, ...)
  - jar signing

## Core tools and JMX

''' Priority items '''

  - Get JMX code released
  - update the core tools in terms of hooks, options, and implementation
  - expose the core tools "management objects" as mbeans
      - for example, assuming the options are turned on, under a bundle
        there should an "activation" child that, if selected, gets the
        stack trace of when that bundle was activated. Similarly there
        should be a "classes" link

## PDE UI

  - remove use of "plugins dir"
  - secondary classpath
  - import management
  - populate a target via update
  - tools for version management
      - Binary compatibility checker.
      - Version checker to see if the version evolved properly.
      - API inclusion checker (to ensure that all the APIs called are
        available when running with the lower range).
      - Filtered code assist based on the value in the @since tag and
        the version specified in the manifest.mf.
      - Warm fuzzy feeling checker.
  - named targets
  - service constraints
  - splash info gathered in product and fanned into config.ini
  - "Add to target" button
  - tooling around "uses" directives

## PDE Build

''' Priority items '''

  - Maven
      - look at how Maven does incremental building
      - Do a prototype of building Eclipse things using Maven
          - use the Maven OSGi plugin and see how it works, what kind of
            metadata we have to create (do it by had first perhaps)
          - start with the Equinox bundles
          - then try RCP (this brings in SWT (always a problem child)
            and the launchers as well as packaging issues like zipping,
            links, permissions, etc.
          - explore some of the other Maven plugins. There is one that
            packages as RPMs, OS services, installers, ...
  - Wagon: this is the transport layer under Maven's repo client
      - Can this be used to populate update sites?
      - Can this be used to populate targets?
  - Repo indexing: Maven has a repo indexer that could, for example, be
    used to generate packed content or the update site digest
  - investigate Maven's configuability wrt repo structures etc.

<!-- end list -->

  - Mangen: Rob Walker recently contributed mangen (manifest generator)
    to Felix. This does class file analysis to see what packages etc are
    used by a piece of code. We should look at this and see what it can
    do. Is this something that can/should be integrated into our
    development model.

''' Other items '''

  - parallel X build
  - predict size of downloads
  - push
  - populate target

## Update/Provisioning

  - alien configs - be able to modify configurations which aren't the
    currently running one (e.g. remove references to
    PlatformConfiguration\#getDefault)
  - performance and robustness
  - pluggable transports
  - role-based provisioning
  - OBR - OSGi Bundle Repository
  - marketing \#
  - simple configurator use
  - minimize dependanices for update configurator
  - update deltas
      - signed JARs
      - mechanism
  - add to site
  - small update application
  - licensing
  - warm fuzzy tool
  - import package requirements in features
  - maven back-end
  - drag 'n drop install

[Planning 3.2](Category:Equinox "wikilink")