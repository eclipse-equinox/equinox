## Framework

### Overall Goals

1.  Scalability and Performance. Monitor overall performance and memory
    consumption which includes the addition of new performance tests for
    new features. Scale up or down to allow Equinox to scale down to
    small embedded environments as well as scaling up to large server
    environments.
2.  Robustness. Provide APIs for clients where needed and fix critical
    bugs. Develop new tests to fill in gaps in the automated tests.
3.  Consumability. Make it easier for users to get Eclipse, install it
    on their systems, and configure it for their use. Make it easier for
    developers to use our APIs.
4.  Future. Deliver new features specified in the next OSGi
    specification R4.2.

### OSGi R4.2

  - Updates to Conditional Permission Admin service (RFC 120)
    [Bug 242799](https://bugs.eclipse.org/bugs/show_bug.cgi?id=242799) -
    IBM
  - Service Registry Hooks (RFC 126)
    [Bug 244625](https://bugs.eclipse.org/bugs/show_bug.cgi?id=244625) -
    IBM
  - SystemBundle (Framework) booting (RFC 132)
    [Bug 244443](https://bugs.eclipse.org/bugs/show_bug.cgi?id=244443) -
    IBM
  - Support for peer and child frameworks running in the same VM. (RFC
    138, still very early) - IBM

### Performance and Size

  - Investigate places where we can rid the framework of bulky and
    unnecessary abstraction layers to reduce size and improve
    performance (service registry abstraction, resolver abstraction
    etc.).
  - Make "uses" clause usable for large installs.

### Reliability

  - Review and fix thread safety issues in the Framework
    [Bug 245251](https://bugs.eclipse.org/bugs/show_bug.cgi?id=245251) -
    IBM

## Milestones

### Current Milestone Plan: M3 - October 31, 2008

  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Implement
    (RFC 138) - Tom, Erin
  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Implement
    (RFC 126)
    [Bug 244625](https://bugs.eclipse.org/bugs/show_bug.cgi?id=244625) -
    BJ
  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Implement
    (RFC 132)
    [Bug 244443](https://bugs.eclipse.org/bugs/show_bug.cgi?id=244443) -
    Tom
  - ![Image:Glass.gif](images/Glass.gif "Image:Glass.gif") API for contributing
    Custom Execution Environments
    [Bug 240724](https://bugs.eclipse.org/bugs/show_bug.cgi?id=240724)

## Future Plans

### M4 - December 12th, 2008

  - ![Image:Glass.gif](images/Glass.gif "Image:Glass.gif") Focus on
    reliability, investigate thread safety issues - Tom, BJ
  - ![Image:Glass.gif](images/Glass.gif "Image:Glass.gif") Investigate
    improvements to the resolver algorithm for uses clause.

### M5 - January 30th, 2009 - Major Feature Freeze

### M6 - March 12th, 2009 - API Freeze

  - Polish API javadoc, package.html doc

### M7 - May 1st, 2009 - Development Complete

  - Polish items
  - Performance work
  - Testing and test framework improvements

## Previous Plans

### M1 - August 8th, 2008

  - Bug triage
  - Fixing critical defects

### M2 - September 19th, 2008

  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Planning
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Focus on
    3.4.1 defects
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Port 3.4.1
    fixes to 3.5 stream
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Implement
    (RFC 120)
    [Bug 242799](https://bugs.eclipse.org/bugs/show_bug.cgi?id=242799) -
    Tom
  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Started
    implementing (RFC 132)
    [Bug 244443](https://bugs.eclipse.org/bugs/show_bug.cgi?id=244443) -
    Tom

## Legend

![Image:Glass.gif](images/Glass.gif "Image:Glass.gif") Needs some investigation

![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Patch in
progress

![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Bug fixed /
Feature added

[Plan](Category:Equinox "wikilink")