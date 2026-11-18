**Reference Extractor Tool – Developer’s docs**

## Main functionality

The reference extraction tool is there to answer the following three
questions:

  - Who is using my APIs/non-APIs
  - What my code is using
  - Identify "internal" methods being used

To clarify this, let’s consider a picture with 3 bundles: MyBundle,
UserBundle, and SystemBundle. Let’s say that my code in the MyBundle and
I am using the SystemBundle. Somebody else wrote UserBundler that uses
both MyBundle and SystemBundle.

To get answers to all the questions above we would run reference
extractor on all 3 bundles and store results in the repository. At a
later time repository could be queried. Developer working on MyBundle
coudl discover where his code is used even if he never heard about
UserBundle.

Processing of queries will aggregate results so that question on who
uses SystemBundle’s code will contain both MyBundle and UserBundle.

## Technology

The reference extractor uses custom parser for .class files. Parser
extracts all references with possible exception of references by
reflection. (Potentially, those could be added with some degree of
"fuzziness" by processing strings contained in the .class files.)

The .class files have references in an "unresolved" format. References
are lazily resolved when queried; classes in the package
“org.eclipse.pde.api.tools.internal.model.ref” used to store and
process references.

The data extracted is grouped by elements being referred and stored in
the XML file. For instance:

<class aName="org.eclipse.core.internal.runtime.RuntimeLog">

<classReference>

<sourceClass class="org.eclipse.core.internal.preferences.PreferencesService"/>

…

<sourceClass class="org.eclipse.core.internal.preferences.EclipsePreferences"/>

</classReference>

<methodReference methodName="log" parameters="org.eclipse.core.runtime.IStatus" type="void">

<sourceClass class="org.eclipse.core.internal.preferences.DefaultPreferences"/>

…

<sourceClass class="org.eclipse.core.internal.preferences.Activator"/>

</methodReference>

</class>

## Current state

The reference extractor itself is working.

However, storage format needs refinement and processing of queries has
only the rudimentary code.

## Common code base

Some code is shared between API comparison tool and reference extractor.
Parser for the .class files; determination of API/non-API status, and
repository support are all shared as well as UI implementation details
and Java models.

## Possible improvements

Aside from completing queries and making it all work, the most
interesting potential development comes from opening free-form queries
to the combined data extracted by API comparison tool and reference
extractor. If SQL database is used as a repository, potentially Web
access could be allowed to the database with support of user-defined
queries. Application could be: determining popularity of APIs, finding
code usage patterns, identifying clients.

## Links

[API Tools](http://wiki.eclipse.org/PDE_UI_Incubator_ApiTools)

[PDE Incubator](http://wiki.eclipse.org/index.php/PDE_UI_Incubator) -
New projects that might be incorporated into PDE in future.

[APITools: Reference Extractor for developers](Category:API "wikilink")
[APITools: Reference Extractor for
developers](Category:Equinox "wikilink")