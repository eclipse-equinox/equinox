This page lists requirements for a new provisioning story. It
volunteerly avoids the mention of any specific technology.

## Functional requirements

  - Provision bundles as well as "root files" (startup.jar, exe, etc.)
  - Provision groups of bundles
      - Do you mean bundles organized in a logical tree, branches being
        group names? *Romain C*
  - Provision single bundles
  - Provision non running configuration (manage multiple configurations)
  - Check the validity of the system before installing (a user should
    know whether the bundles it installs will resolve or not)
      - But allow, on user's choice, the ability to "install anyway",
        since the user may be doing things "out of order", or may "know
        what they are doing" so an invalid system is not fatal. *David
        williams*
      - Warn user if installing a new version of a bundle will
        invalidate other (downstream) bundle dependencies.
  - Transparent provisioning of dependent bundles (in case of a
    mismatch, missing dependencies should support be updated)
  - Support rollback in case of failure during the install
  - Support deletion of old bundles after successful install. *Phill P.*
  - Transparent support for mirrors
      - including the ability to support various pluggable mirror
        selection techniques (i.e. locale, round robin, speed, etc)
        *Philippe O.*
      - If access to one mirror fails (at any point), automagically move
        on to the next (using one of the pluggable mirror selection
        techniques) *Wayne Beaton*
      - Avoid selecting a known-failed mirror on subsequent requests
        (for at least some configurable period of time) *Wayne Beaton*
  - Shared pool of plugins (multiple eclipse based products should be
    able to share the same bundles on disk).
  - Ability to process data on install (e.g. workspace metadata)
      - do you mean by that: metadata update? or preferences
        provisioning? or both? *Philippe O.*
  - Support for partial updates (aka fix packs)
      - would you see that go at a lower granularity than a bundle?
        *Philippe O.*
          - In an ideal world that would indeed be cewl. But what a
            release engineering mess would that cause if patches needed
            to be tracked on binary compatibility and such on an
            individual class basis
            (Erik.Vanherck@inventivedesigners.com)
  - Integrate nicely with JNLP deployed systems
  - Integration with other provisioning technologies
  - Support bundle level configurability (set bundle start level, auto
    start, ini file)
  - A simple update API for use in RCP applications. *Phill P.*
      - the update API should enable an eclipse based product to verify
        that it is current (no updates needed) or that it requires an
        update and than performs an update (maybe without any user
        interaction).
  - The format of the descriptors should be based on an open standard
    for feeds that has author, copyright, update time, etc. and can have
    arbitrary XML payloads for encapsulating any data necessary *Alex
    B.*
  - Should be possible to open a web page in an internal browser and
    have Eclipse recognise that there are update sites mentioned like
    Firefox does; e.g. through use of 'link' protocols *Alex B*
  - headless (commandline) support should be either easy to create
    yourself, or the standalone update tool needs serious work. Server
    deployment, scripting, integration in 3th party tools like
    installers .. causes a need for this
    (Erik.Vanherck@inventivedesigners.com)
  - (perhaps obvious) only donwload as little as possible, binary diffs
    would be awesome, but that may be stretching it
    (Erik.Vanherck@inventivedesigners.com)
      - creating of those diffs should be done by the update server, not
        the developers. *Stefan L.*
  - An option to save downloaded items in an archive for offline
    (re)installation. *Richard Gronback*
  - Ability to have symbolic names for URLs to update sites. I'm
    thinking of the current tag that controls where update manager looks
    for "updates for currently installed features". I'm thinking of two
    use cases, the first more important. *David Williams*
      - Currently, with the URL hardcoded by the installed feature, it
        is hard to do anything other than have it point to the location
        for official releases. Some would like to "update" from a
        release to a milestone, or from one milestone to the next
        milestone. It'd be much easier to do this, while still avoiding
        "accidental" updates to non-released code, to allow users to
        "override" the URL (pattern) for where to look for updates. (or
        example, a user might choose to change "webtools/updates" to
        "webtools/milestones" or even "webtools/weeklies".
      - The second use case is really simply a similar solution to a
        different problem. I've seen several cases where Eclipse
        projects forget to include an update site URL in their
        feature.xml. If this happens, nothing can be done until the user
        installs a new version. If there was some way for a user to
        associate a URL with a feature ID, then they could get
        maintenance updates without new installation.
  - Single Wizard user interface *Wayne Beaton*
  - Ensure that installation of patches does not invalidate currently
    installed units (features, etc) See
    [Bug 167016](https://bugs.eclipse.org/bugs/show_bug.cgi?id=167016)
    for a case where this fails in the current update manager. (*DJ
    Houghton*)
  - A simple way to add additional bundles to a configuration. For
    example, a user has a set of ad-hoc bundles that contain various
    tools, utilities, etc. They want to be able to add those bundles to
    an existing configuration without messing around with product
    extensions, links directories, etc. For example, just drag a bundle
    JAR into some place in an "update manager" dialog and have the
    bundle installed automatically. See
    [bug 174517](https://bugs.eclipse.org/bugs/show_bug.cgi?id=174517)
    for example. *John Arthorne*
  - On demand install: should be able to define a core installation of a
    product that users can download and try, and install other optional
    bundles/features as needed. This is particularly useful for large
    eclipse based products. *Dorian Birsan*
  - Distributed updates based on policies: Start on node A, if
    successful update next node, if successful update next node, etc.
    Eclipse on the server side is running in a clustered environment.
    Only one node should update itself at the same time. (*Gunnar
    Wagenknecht*)
  - Security requirements
      - Provides support for the digitally signed metadata
      - Provides api that allows the caller to retrieve signed
        information about artifacts. The signed information includes
        signer certificates, tsa (timestamps authority) certificates,
        signing time, and whether signer/tsa certificates are trusted
        See[Bug 197779](https://bugs.eclipse.org/bugs/show_bug.cgi?id=197779)
        and
        [Bug 157676](https://bugs.eclipse.org/bugs/show_bug.cgi?id=157676)
        (*Matt Flaherty, Eric W Li*)

## Provisioning platform requirements

  - Separation metadata server / byte server
  - Flexible layout of bytes on the "byte server" (for example it is
    sometimes more interesting to download a big zip than many small
    zips)
  - Pluggable transports (Http, ftp, etc.)
      - Regardless of the transport there should be provision to support
        suspendable/resumable transfer of bytes, for those transports
        and byte servers that support it. *Philippe O.*
      - Support for authenticating proxy servers. *Phill P.*
      - Support for client/server authentication (https). *Stefan L.*
      - The UpdateManager/Eclipse should make assiduous efforts to
        retrieve the internet proxy settings specified within the
        operating system specific preferences. *Stefan L.*
  - Metadata for grouping should have an extensible filtering mechanism
    (e.g. os=win32)
  - Ability to express dependencies on other things than bundles (e.g
    JRE \>= 1.5).
      - Ability to ´trigger´ download/upgrade to required JRE version.
        *Stefan L.*
  - Ability to express dependencies on bundles found on other update
    sites and automagically access these update sites to obtain the
    required features *Wayne Beaton*
      - When metadata are used to express filtering or dependencies,
        they should have at least the same expressing power of OSGi
        related mechanisms.*Philippe O.*
  - NLS support for metadata
  - Extensible metadata

## Tooling

  - Ability to generate part of the metadata from bundle level
    dependencies
  - Ability to check and issue warnings when dependencies are expressed
    on things which are packaged by others *Philippe O.*

[Category:Equinox](Category:Equinox "wikilink")
[Category:Provisioning](Category:Provisioning "wikilink")