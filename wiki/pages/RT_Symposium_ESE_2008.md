This page summaries the slides, discussion and outcome of the RT
symposium at the Eclipse Summit Europe 2008.

## Towards the Symposium

Upfront to the event we ask attendees to prepare answers for the
following questions:

  - What is missing from the technology?
  - What are the three most important problems with the technology?
  - What are the biggest implementation challenges?
  - What is your vision for the future?
  - What should the projects do (concrete implementation items, general
    wishes, possible synergies between projects)?
  - What confuses you because of too similar but slightly different ways
    of doing it?

These slides summarizes the answers we got for those questions:

[RT-Symposium at ESE 2008 - Slides
(pdf)](Media:ESE2008-RT-Symposium.pdf "wikilink")

## Discussion Topics

After going through these answers we listed and prioritized topics for
the second part of the symposium (votes in brackets):

  - Services and Extensions (11-13)
  - Combining technologies, problem/technology-matching, Examples, Best
    Practices (16-20)
  - API compatibility / Deprecation (7)
  - Cloud / distributed, intra-process-communication, sca, soa,
    remote-OSGi/services (13-14)
  - IP process (2)
  - Builds, shipping, provisioning, p2 (18)

<!-- end list -->

  - Roadmap

## Best Practices, Examples

We briefly discussed an example of build, for example, RCP apps together
with EclipseLink. Other practices and issues emerged from that
discussion:

  - OSGifying libraries: Use Orbit / Fix Orbit
  - Granularity
  - Dependency Injection
  - Classloading

<!-- end list -->

  - Best practices wiki (crosscutting): It would be great to create some
    wiki pages describing best practices and examples how to use RT
    projects together and what to choose from where.

## Build, Shipping, Provisioning, P2

We started to talk about experiences from people using different
technologies for these issues within their projects and listed them:

  - PDE Build
  - Maven
  - Buckminster
  - Ivy
  - Custom Ant
  - Ant4Eclipse
  - Non-OSGi stuff

<!-- end list -->

  - Hudson
  - Bamboo
  - CruiseControl

(-Dequinox.ds.print=true) (some good fried when using declarative
services)

## Roadmap for the RT projects

  - 7 projects at the moment
  - distributions (meaningful RT project distros)
  - Target Platform elements
  - Different repo?

We also talked about improving the collaboration between the different
RT projects

  - build infrastructure
  - awareness
  - education
  - examples

## Ian Skerrett's Video Podcast

Ian Skerrett took a short video of Jeff talking about the RT Symposium:
[Video-Podcast: Jeff McAffer on the RT Symposium at
ESE 2008](http://blip.tv/file/1486425)

[Category:Equinox](Category:Equinox "wikilink")