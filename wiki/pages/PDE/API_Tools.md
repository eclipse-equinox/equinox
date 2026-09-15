## Introduction

API tooling will assist developers in API maintenance by reporting API
defects such as binary incompatibilities, incorrect plug-in version
numbers, missing or incorrect `@since` tags, and usage of non-API code
between plug-ins. The tooling will be integrated in the Eclipse SDK and
will be used in the automated build process. Specifically, the tooling
is designed to do the following:

  - Identify binary compatibility issues between two versions of a
    software component or product.
  - Update version numbers for plug-ins (bundles) based on the Eclipse
    versioning scheme.
  - Update `@since` tags for newly added classes, interfaces, methods,
    etc.
  - Provide new Javadoc tags and code assist to annotate types with
    special restrictions.
  - Leverage existing information (in `MANIFEST.MF`) to define the
    visibility of packages between bundles.
  - Identify usage of non-API code between plug-ins.
  - Identity leakage of non-API types into API.
  - Identify usage of code from a JRE outside the bounds of the one
    specified in the bundle configuration (`MANIFEST.MF`).

## Present state

API tooling was released to the Eclipse SDK in the PDE project during
the
[Eclipse 3.4](http://www.eclipse.org/eclipse/development/eclipse_project_plan_3_4.html)
release. Our milestone development time line corresponds to that of the
Eclipse SDK (currently in the 3.6 stream).

## Planning

All planned items (and wishes) can be found on our [3.6
plan](PDE/Plan/3.6#API_Tools "wikilink") page.

## Links

[Ant Tasks](PDE/API_Tools/Tasks "wikilink") - Description of the Ant
Tasks available in API Tools

[API Restrictions](PDE/API_Tools/Restrictions "wikilink") - Description
of the restrictions supported on API types via Javadoc tags or
annotations.

[API Tooling Architecture](ApiTools_Architecture "wikilink") - High
level description of the tooling's architecture.

[Evolving Java-based APIs](Evolving_Java-based_APIs "wikilink") - What
is considered an API in Eclipse.

[Version Numbering](Version_Numbering "wikilink") - Guidelines on
versioning plug-ins

[PDE/Incubator](PDE/Incubator "wikilink") - New projects that might be
incorporated into PDE in future.

[PDE UI Home Page](http://www.eclipse.org/pde/pde-ui/) - The main PDE UI
web site.

[API Tools](Category:API "wikilink") [API
Tools](Category:Equinox "wikilink") [API Tools](Category:PDE "wikilink")
[API Tools](Category:Eclipse_Project "wikilink")