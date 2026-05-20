# Introduction

API Tools has been designed to aid developers maintaining good APIs,
version numbers, mitigating internal usage across bundles and
eliminating internal code leaks to API. The following are the original
use cases discussed while designing the tooling.

### Binary Compatibility Reporting (Batch Mode)

Two versions of the same API profile are compared for [binary
compatibility](http://wiki.eclipse.org/Evolving_Java-based_APIs_2). An
XML file is produced summarizing any incompatibilities. The comparison
tool can be invoked from the command line as a stand alone Java
application specifying the profiles to compare and which parts of the
profiles to consider (for example, only compare portions of the profile
that are deemed to be API).

The report includes errors regarding component version identifiers that
have not been incremented properly. As well, if source code is available
for the "newer" API profile, the report includes missing `@since`
Javadoc tags.

An exclude list should be added to filter out the cases where the binary
incompatibility is "under control", i.e. approved by the PMC. The best
way to maintain the exclude list would be to have a Javadoc tag in the
source code that mentions why this is a breakage. Something like:

`@breakage-addition ......`
`@breakage-remove Type#member .....`

The removals would be located on the parent of the removed member.

Updating the source code improves the traceability of a breakage and
allows readers of the source code to get a better picture without the
need to check another document.

### Binary Compatibility Reporting (IDE Mode)

Workspace projects are compared to a baseline API profile for binary
compatibility. Incompatibilities are flagged in source files using
markers (that also appear in the Problems view). The user configures the
severity of errors produced. A set of external API profiles are managed
in the workspace and one is specified as the baseline against which
workspace projects are compared. The user defines the workspace API
profile as combination of workspace projects and external API
components.

Compatibility errors are updated incrementally or in full depending on
the build setting in the workspace (i.e. auto-build vs. manual/full
build). Error markers are also produced for components with incorrect
version numbers and missing `@since` Javadoc tags. Quick fixes are
available to address the problems.

An exclude list should be added to filter out the cases where the binary
incompatibility is "under control", i.e. approved by the PMC. The best
way to maintain the exclude list would be to have a Javadoc tag in the
source code that mentions why this is a breakage. Something like:

`@breakage-addition ......`
`@breakage-remove Type#member .....`

The removals would be located on the parent of the removed member.

Updating the source code improves the traceability of a breakage and
allows readers of the source code to get a better picture without the
need to check another document.

### API Usage Reporting (Batch Mode)

The most common API usage report locates illegal use of APIs among
components in a single API profile - i.e. access to non-API types,
methods, and fields; and illegal extension, implementing, or
instantiating. The API usage scanner can be invoked as a stand alone
Java application to examine all or specific portions of an API profile
for illegal API use. An XML file is produced as output.

The API scanner should also support scanning for use of a specific
component. For example, rather than scanning component X to determine
what use it makes of other APIs, scan a profile to find all uses of the
API in X.

Another interesting scan would be to report what parts of a profile or
component would be broken when migrating to another version of a
required component. For example, the internals of a component often
change or can be removed in a newer release of the component.

### API Usage Reporting (IDE Mode)

The Eclipse SDK already provides compiler warnings for discouraged
accesses between bundles - which is the same as referencing non-API
code. Rather than duplicate this effort, the integrated tooling could
just report illegal implementing, sub-classing, and instantiation.
Problem markers would be created incrementally, similar to the support
for binary compatibility.

### API Usage Searching (IDE Mode)

Similar to the extensive search facility provided by JDT for searching
projects in the workspace, API tooling could support searching of API
profiles. This would allow to search for all uses of a component, type,
method, etc., from an API profile or component.

### Version Management

In addition to reporting missing `@since` tags and incorrect bundle
version numbers (based on the Eclipse bundle versioning scheme), the
tooling will provide quick fixes to correct these problems.

As well, the tooling will assist developers on determining compatible
version ranges of required bundles (plug-ins). Developers often
increment the lower bound of version ranges of required bundles in each
major release. Usually this makes sense (for example, the debug
platform's UI bundle usually requires the latest debug core bundle).
However, sometimes this is unnecessary, and a bundle may run perfectly
fine with an older version of a required bundle. Given a range of
versions of a required bundle, API tooling will be able to determine
which versions of the bundle satisfy API (and non-API) accesses.

### Building API Components & Baselines

Provide a mechanism to export API components. This could be used in a
build process or from the IDE.

### Javadoc Tags, API Visibilities & Restrictions

The platform provides a default set of Javadoc tags. The tags and the
Java members they apply to are:

1.  `@noreference` - Indicates that other bundles must not reference
    this member by name. I.e., the member is internal. This tag is
    intended to be used very rarely when a public class wants to
    restrict access to one of its members, but is not intended for
    general usage. This tag is ignored on all other members except for
    method declarations and non-final field declarations.
2.  `@noimplement` - Indicates that other bundles must not implement
    this interface. This tag is ignored for all types except for
    interfaces.
3.  `@noextend` - Indicates that other bundles must not extend the class
    or interface it appears on. This tag is ignored for all members that
    are not interfaces or classes.
4.  `@noinstantiate` - Indicates that other bundles must not create
    instances of this class. This tag is ignored for all other types
    that are not classes.
5.  `@nooverride` - Indicates that other bundles must not extend
    (re-implement with a call to the overridden parent) or re-implement
    (with no call to the overridden parent) this method. This tag is
    ignored for all other members except method declarations.

[API Tools](Category:API "wikilink") [API
Tools](Category:Equinox "wikilink") [API Tools](Category:PDE "wikilink")
[API Tools](Category:Eclipse_Project "wikilink")