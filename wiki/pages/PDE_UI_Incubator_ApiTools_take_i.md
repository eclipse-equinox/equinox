## Plans

**JDT models as a source**: in addition to the processing of .class
files, add code to create API snapshots from JDT models. The Javadoc
tags then could be used to pass "soft" API restrictions to the tool such
as "this interface can not be implemented by clients".

**Scalable repository**: in addition to files on file system, support
storage of API snapshot in an SQL database. Pros: improved scalability
and reliability; new functionality could be added an a form of queries,
possibly, via Web interface.

## API Comparison tool

**API Comparison tool**: discovers backward compatibility problems and
new APIs

The tool can be used to create API snapshot of a set of bundles / jars /
directories. The snapshot is used as a baseline to compare against
another version of the source code to discover problems in API backward
compatibility and new APIs.

The tool is written in modular fashion. It has an “engine” with
replaceable input and output. Presently OSGi bundles, Java jars, and
directories can be used as an input sources. The snapshot output is
saved into zip-ed xml file. Comparison results are produced as XML file
which is transformed into HTML for ease of reading.

[User guide to the API comparison
tool](http://wiki.eclipse.org/index.php/PDE_UI_Incubator_ApiTools_Compare)

[Developer guide to the API comparison
tool](http://wiki.eclipse.org/index.php/PDE_UI_Incubator_ApiTools_Compare_Dev)

## Reference Extractor tool

The **Reference Extractor** tool is there to answer the following three
questions:

  - Who is using my APIs/non-APIs
  - What my code is using
  - Identify "internal" methods being used

It does it by processing code to extract dependency information from it.
The request are combined in the repository that could be queried to find
downstream dependencies.

[Developer guide to the Reference Extractor
tool](http://wiki.eclipse.org/index.php/PDE_UI_Incubator_ApiTools_ReferenceExtractor_Dev)

## Usecases

These are the usecases that we would like to cover with the API
tooling:

  - API Management: comparison (ensure backward compatibility),
    browsing, visualizing
  -
  - Global Find references: This could be used to find all usages of an
    API from data stored in a "repository". The repository could be a
    database, a file in memory,...
    It could also be used to find all usages of internal codes inside
    bundles. This is a useful information for committers when they want
    modify an internal method.
    They could know who they are going to break and have a proactive way
    to solve this issue (add another method, inform users about a future
    breakage,...)
  - Version checking: It will help users to update theirs bundle version
    numbers according to the kind of changes made in a bundle: fix, new
    API,...
  - Build analysis: JRE used to build vs JRE used to run, check
    dependencies. Check that a plugin uses only API that is available in
    the **lower bound** of the Prerequisites.
  - Prerequisite browsing: find out what we use and why

## Open issues

  - How to markup the code with information for the API tool.
    Information like "can you subclass, instantiate, implement,...".
  - SPI are technically speaking API, but with the freedom of being
    changed at any time.
  - The tool must be able to:
      - run headless without any workspace, embedded in releng builds.
      - notify developers asap of issues
      - must scale
      - input could be: bundles, jars, Eclipse installation, directory
        of .class files or jars
      - should be able to specify what an API is in a input file
      - batch and incremental processing

[API Tooling](Category:API "wikilink") [API
Tooling](Category:Equinox "wikilink")