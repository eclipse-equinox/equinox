This page lays out milestone plans for the development of the Equinox
Aspects incubator project.

## Current Milestone Plan

### RC1 - June 12, 2009

  - General:
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
        Alignment with the releng headless build
        [bug 256347](https://bugs.eclipse.org/bugs/show_bug.cgi?id=256347)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Prepare for graduation
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") rename
        classes/interfaces in base hook
        [bug 274438](https://bugs.eclipse.org/bugs/show_bug.cgi?id=274438)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Hangs
        after Scala plugin update; after force quit and manual restart,
        it crashes; after second manual restart, it finally runs
        [bug 272428](https://bugs.eclipse.org/bugs/show_bug.cgi?id=272428)

<!-- end list -->

  - Weaving:
      - weaving by SupplementImport fails when using bundle re-exports
        [bug 266664](https://bugs.eclipse.org/bugs/show_bug.cgi?id=266664)

<!-- end list -->

  - Caching:
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        caching service performance problem
        [bug 274749](https://bugs.eclipse.org/bugs/show_bug.cgi?id=274749)

## Future Milestone Plans

### Planned items for 1.1 development

  - General:
      - Eclipse-SupplementBundle is not refactored
        [bug 260041](https://bugs.eclipse.org/bugs/show_bug.cgi?id=260041)
      - more tooling support for Equinox Aspects

<!-- end list -->

  - Weaving:
      - Move spring-dm weaver to equinox aspects
      - investigate other weaver implementations
      - allow more than one weaver service
        [bug 268971](https://bugs.eclipse.org/bugs/show_bug.cgi?id=268971)

<!-- end list -->

  - Caching:

<!-- end list -->

  - Website:
      - add screencast or step-by-step example for RCP apps using
        Equinox Aspects

## Previous Milestone Plans

### M1 - August 8, 2008 (stable build)

  - General:
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") rename
        bundles and start with new version numbering
        [bug 238730](https://bugs.eclipse.org/bugs/show_bug.cgi?id=238730)
        (Martin)

<!-- end list -->

  - Weaving:
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        simplify locating aop.xml definitions
        [bug 237145](https://bugs.eclipse.org/bugs/show_bug.cgi?id=237145)
        (Martin)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") fix
        the handling for dynamically uninstalling aspect bundles
        [bug 229865](https://bugs.eclipse.org/bugs/show_bug.cgi?id=229865)
        (Martin)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        weaving does not work for some bundles
        [bug 237214](https://bugs.eclipse.org/bugs/show_bug.cgi?id=237214)
        (Martin)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        improve handling of service dynamics: Coming and going of
        weaving service must influence aspectized bundles
        [bug 226461](https://bugs.eclipse.org/bugs/show_bug.cgi?id=226461)
        (Heiko)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        exception thrown when aspects are registered
        [bug 241638](https://bugs.eclipse.org/bugs/show_bug.cgi?id=241638)
        (Martin)

<!-- end list -->

  - Caching:
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") add
        handling of bundle and aspect versions to standard caching
        service
        [bug 216398](https://bugs.eclipse.org/bugs/show_bug.cgi?id=216398)
        (Martin)

<!-- end list -->

  - Website:
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") add
        bundle overview page (Martin)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") add
        dev builds to download page (Martin)
      - <span style="color:teal">\[\>1.0 M1\]</span> add screencast for
        hello world example (Heiko, contribution by Gerd Wütherich)

### M2 - September 19, 2008 (stable build)

  - General:
      - <span style="color:teal">\[\>1.0 M2\]</span>
        ![Image:Glass.gif](images/Glass.gif "Image:Glass.gif") Alignment with
        the releng headless build (Heiko)
      - <span style="color:teal">\[\>1.0 M2\]</span>
        ![Image:Glass.gif](images/Glass.gif "Image:Glass.gif") Harmonize
        logging and tracing over the different bundles (Heiko)
      - <span style="color:teal">\[\>1.0 M2\]</span> clean up old
        bugzilla entries

<!-- end list -->

  - Weaving:
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        supplementer mechanism broken without clean configuration
        [bug 243681](https://bugs.eclipse.org/bugs/show_bug.cgi?id=243681)
        (Martin)
      - <span style="color:teal">\[\>1.0 M2\]</span> avoid weaver
        initialization until really needed
        [bug 243685](https://bugs.eclipse.org/bugs/show_bug.cgi?id=243685)
        (Martin)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") aspect
        bundle update is broken
        [bug 244410](https://bugs.eclipse.org/bugs/show_bug.cgi?id=244410)
        (Martin)
      - <span style="color:teal">\[\>1.0 M2\]</span> investige other
        weaving implementations using ClassFileTransformer API

<!-- end list -->

  - Website:
      - <span style="color:teal">\[\>1.0 M2\]</span> add screencast for
        hello world example (Heiko, contribution by Gerd Wütherich)
      - <span style="color:teal">\[\>1.0 M2\]</span> add screencast or
        step-by-step example for RCP apps using Equinox Aspects

### M3 - October 31, 2008 (stable build)

  - General:
      - ![Image:Glass.gif](images/Glass.gif "Image:Glass.gif") Alignment with
        the releng headless build (Heiko)
      - ![Image:Glass.gif](images/Glass.gif "Image:Glass.gif") Harmonize
        logging and tracing over the different bundles (Heiko)
      - <span style="color:teal">\[\>1.0 M3\]</span> clean up old
        bugzilla entries
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        replace the manifest rewriting with the new hook
        [bug 229863](https://bugs.eclipse.org/bugs/show_bug.cgi?id=229863)
        (Martin)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") change
        aspect activation from installed to resolved state
        [bug 247718](https://bugs.eclipse.org/bugs/show_bug.cgi?id=247718)
        (Martin)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        deadlock when using equinox aspects and spring dm extender
        [bug 249613](https://bugs.eclipse.org/bugs/show_bug.cgi?id=249613)
        (Martin)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Supplementing via Require-Bundle may conflict with "uses"
        directive
        [bug 248826](https://bugs.eclipse.org/bugs/show_bug.cgi?id=248826)
        (Martin)

<!-- end list -->

  - Weaving:
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") avoid
        weaver initialization until really needed
        [bug 243685](https://bugs.eclipse.org/bugs/show_bug.cgi?id=243685)
        (Martin)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        replace require-bundle with import-package for weaving.aspectj
        bundle
        [bug 248046](https://bugs.eclipse.org/bugs/show_bug.cgi?id=248046)
        (Martin)
      - <span style="color:teal">\[\>1.0 M3\]</span> investigate weaving
        service implementation for ClassFileTransformer
        [bug 248047](https://bugs.eclipse.org/bugs/show_bug.cgi?id=248047)

<!-- end list -->

  - Website:
      - add screencast for hello world example (Heiko, contribution by
        Gerd Wütherich)
      - <span style="color:teal">\[\>1.0 M3\]</span> add screencast or
        step-by-step example for RCP apps using Equinox Aspects

### M4 - December 12, 2008 (stable build)

  - General:
      - <span style="color:teal">\[\>1.0 M4\]</span> Alignment with the
        releng headless build
        [bug 256347](https://bugs.eclipse.org/bugs/show_bug.cgi?id=256347)
        (Heiko)
      - ![Image:Glass.gif](images/Glass.gif "Image:Glass.gif") Harmonize
        logging and tracing over the different bundles (Heiko)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") clean
        up old bugzilla entries
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") p2.inf
        for equinox aspects
        [bug 255122](https://bugs.eclipse.org/bugs/show_bug.cgi?id=255122)
        (Martin, contributed by Andrew Eisenberg)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Failed
        deploy of war bundle using Spring-DM web extender
        [bug 255156](https://bugs.eclipse.org/bugs/show_bug.cgi?id=255156)
        (Martin)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") An
        endless recursion when weaving against M3
        [bug 253656](https://bugs.eclipse.org/bugs/show_bug.cgi?id=253656)
        (Martin)

<!-- end list -->

  - Weaving:
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        deadlock when using equinox aspects and spring dm extender
        [bug 249613](https://bugs.eclipse.org/bugs/show_bug.cgi?id=249613)
        (Martin)
      - <span style="color:teal">\[\>1.0 M4\]</span> investigate weaving
        service implementation for ClassFileTransformer
        [bug 248047](https://bugs.eclipse.org/bugs/show_bug.cgi?id=248047)

<!-- end list -->

  - Website:
      - <span style="color:teal">\[\>1.0 M4\]</span> add screencast for
        hello world example (Heiko, contribution by Gerd Wütherich)
      - <span style="color:teal">\[\>1.0 M4\]</span> add screencast or
        step-by-step example for RCP apps using Equinox Aspects

### M5 - January 30, 2009 (stable build)

  - General:
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
        Alignment with the releng headless build
        [bug 256347](https://bugs.eclipse.org/bugs/show_bug.cgi?id=256347)
        (Heiko)
      - <span style="color:teal">\[\>1.0 M5\]</span> Prepare for
        graduation

<!-- end list -->

  - Weaving:
      - <span style="color:teal">\[\>1.0 M5\]</span> investigate weaving
        service implementation for ClassFileTransformer
        [bug 248047](https://bugs.eclipse.org/bugs/show_bug.cgi?id=248047)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        exception thrown at startup
        [bug 261089](https://bugs.eclipse.org/bugs/show_bug.cgi?id=261089)
        (Martin)

<!-- end list -->

  - Website:
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") add
        screencast for hello world example (Martin, contribution by Gerd
        Wütherich)
      - <span style="color:teal">\[\>1.0 M5\]</span> add screencast or
        step-by-step example for RCP apps using Equinox Aspects

### M6 - March 13, 2009 (stable build)

  - General:
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
        Alignment with the releng headless build
        [bug 256347](https://bugs.eclipse.org/bugs/show_bug.cgi?id=256347)
        (Martin)
      - <span style="color:teal">\[\>1.0 M6\]</span> Prepare for
        graduation

<!-- end list -->

  - Weaving:
      - <span style="color:teal">\[\>1.0 M6\]</span> investigate weaving
        service implementation for ClassFileTransformer
        [bug 248047](https://bugs.eclipse.org/bugs/show_bug.cgi?id=248047)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        programmaticly enabling/disabling weaving hook from within
        application
        [bug 262229](https://bugs.eclipse.org/bugs/show_bug.cgi?id=262229)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") using
        package versions on import-package in manifests
        [bug 258136](https://bugs.eclipse.org/bugs/show_bug.cgi?id=258136)
      - ![Image:Glass.gif](images/Glass.gif "Image:Glass.gif") enable/disable
        weaving service on a per-bundle basis
        [bug 255682](https://bugs.eclipse.org/bugs/show_bug.cgi?id=255682)

<!-- end list -->

  - Caching:
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        caching service causes system to hang
        [bug 262299](https://bugs.eclipse.org/bugs/show_bug.cgi?id=262299)
        (Martin)

<!-- end list -->

  - Website:
      - <span style="color:teal">\[\>1.0 M6\]</span> add screencast or
        step-by-step example for RCP apps using Equinox Aspects

### M7 - May 1, 2009 (stable build)

  - General:
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
        Alignment with the releng headless build
        [bug 256347](https://bugs.eclipse.org/bugs/show_bug.cgi?id=256347)
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Prepare
        for graduation
      - <span style="color:teal">\[deferred to 1.1\]</span>
        Eclipse-SupplementBundle is not refactored
        [bug 260041](https://bugs.eclipse.org/bugs/show_bug.cgi?id=260041)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") small
        changes to Equinox Aspects
        [bug 270010](https://bugs.eclipse.org/bugs/show_bug.cgi?id=270010)

<!-- end list -->

  - Weaving:
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        investigate weaving service implementation for
        ClassFileTransformer
        [bug 248047](https://bugs.eclipse.org/bugs/show_bug.cgi?id=248047)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        enable/disable weaving service on a per-bundle basis
        [bug 255682](https://bugs.eclipse.org/bugs/show_bug.cgi?id=255682)
      - <span style="color:teal">\[\>1.0 M7\]</span> weaving by
        SupplementImport fails when using bundle re-exports
        [bug 266664](https://bugs.eclipse.org/bugs/show_bug.cgi?id=266664)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        aspects in fragments not found
        [bug 271031](https://bugs.eclipse.org/bugs/show_bug.cgi?id=271031)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        introduce a new header to avoid weaver creation
        [bug 274410](https://bugs.eclipse.org/bugs/show_bug.cgi?id=274410)

<!-- end list -->

  - Caching:
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        investigate caching of generated classes
        [bug 226094](https://bugs.eclipse.org/bugs/show_bug.cgi?id=226094)

<!-- end list -->

  - Website:
      - <span style="color:teal">\[deferred to 1.1\]</span> add
        screencast or step-by-step example for RCP apps using Equinox
        Aspects

## Legend

![Image:Glass.gif](images/Glass.gif "Image:Glass.gif") Needs some investigation

![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Patch in
progress

![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Bug fixed /
Feature added

<span style="color:teal">\[\>1.0 M1\]</span> Moved or continued beyond
that build

[Aspects Plan](Category:Equinox "wikilink")