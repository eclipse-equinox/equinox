**API Comparison Tool – Developer’s docs**

## Main functionality

The tool helps developers track API changes. It allows:

  - Detect binary incompatible changes
  - Create reports containing all changed (could be used as a base of
    “What’s New”)
  - Report incorrect bundle versions based on API changes

The tool takes a "source" and creates API snapshot from it. The tool use
OSGi bundle(s), RCP product, or Java JAR(s) as a "source".

The snapshot is an XML file that is stored in the file system. Another
version of the "source" can be compared to the snapshot to detect API
changes.

For example, using this tool Eclipse SDK 3.2 can be compared to the
version 3.3 to detect incompatible changes, produce skeletons for
"what’s new" reports, and check to see if versions of bundles have
been properly incremented.

## Additional functionality

The tool supports "exclusion" mechanism that allows known problems to be
omitted from the reports. The exclusion files follow the same format as
XML API reports and, in fact, are expected to be produced from the
reports by users.

The tool can be run both from Eclipse and in a headless mode.

## Current status

The tool received a moderate amount of testing and is ready to be used.

## Potential developments

At present directory on the file system as used as a **repository for
API snapshots**. This approach is fine for a small workgroup, but
something more scalable (like an SQL database) eventually will be needed
to store API snapshots

The API description is being extracted from .class file. Alternatively,
a module could be added to collect it from **JDT model**.

**Fine-grained rules:** A number of places in Eclipse have fine-grained
API usage rules. Most common example would be API interfaces. A number
of them has text in Javadoc declaring that users should not implement
those interfaces. As such, adding methods to those interfaces is not a
breaking change from Eclipse view point, but the tool has no way to
detect such fine grained restrictions. A possible solution would be
usage of new [Javadoc tags](http://wiki.eclipse.org/API_Javadoc_tags) to
provide this information in a standard way.

## Technology

![ApiToolsProcess.JPG](ApiToolsProcess.JPG "ApiToolsProcess.JPG")

Major points:

  - API information is extracted from .class files using custom parser
  - API/non-API determination is done based on OSGi manifest files
  - Detailed XML reports are produced
  - XSLT is used transform reports into readable HTML
  - System was designed in a modular fashion (source, repository,
    reports are replacable)

At present code is contained in 3 bundles:

  - org.eclipse.pde.api.tools – the "core" functionality
  - org.eclipse.pde.api.tools.ui – UI elements exposing "core"
    functionality
  - org.eclipse.pde.api.tools.tests – tests

The bundles contain code for both API comparison tool and reference
extraction tool.

The classes SnapshotOperationsBundle and SnapshotOperations from the
"core" bundle provide access points for the UI and headless invocations.
The information on the source code to be processed and processing
options is passed via IApiControl structure.

The "core" bundle contains specialized parser for .class files that
allows us to extract only information needed by API tools. As the parser
bypasses most of the information in the .class files, parsing is rather
efficient.

**Creating API snapshot**

From the view point of the tool, the source code it is processing is
organized in containers (IApiContainer). Containers have a unique string
ID, version, and location. ID and version are persisted in the
repository when information is written out; location only there while
API collection takes place. IApiContainer structures that could be used
to represent individual OSGi bundles, Java JARs or directories that
contain source code. See classes SourceContainerBundle and
SourceContainer.

The bundle version uses information contained in the manifest.mf files.
To obtain it, it relies on the internal PDEState helper class from PDE
Core.

Collector class takes a list of containers. It discovers all .class
files in the containers and process them. For directories, all
sub-directories are included. For bundles, re-exported bundles are
included as if they were present in the container. The inclusion of
re-exported process is done recursively.

Collector processes all classes contained in containers in three steps:

  - inherited elements (methods and fields) from non-API classes are
    recursively merged into APIs
  - non-API classes are identified and removed from the memory model
  - memory model is cleaned up to remove information not visible in
    normal usage (members with default visibility, protected elements of
    final classes, non-API superclasses and interfaces).

Once the processing is done, in-memory model has no elements with
default visibility. At this point "public" visibility flags are reset
and all elements with non-specified visibility have public visibility.
(This is done to simplify storage and comparison procedures.)

Classes in the package "org.eclipse.pde.api.tools.internal.model" used
to store Java model read from .class files in memory.

Class ApiSnapshot contains all the information along with helper methods
to simplify access to the containers. ApiContainer along with ID and
version has a list of API classes and interfaces. All those classes have
methods to support storage of contained information in XML format.

IApiRepository describes an interface to the repository for API
snapshots. At present, it only implemented to allow snapshots be stored
in a directory on a file system; however, as may other pieces, it is
designed to be pluggable.

**Comparing API snapshots**

Comparator class compares two API snapshots. Comparison is asymmetrical.
To perform comparison, for each old classifier it attempts to find a
matching classifier in the new API snapshot. If no such match found, the
classifier marked as "removed". (Similarly, new classifiers with no
matches in old classifiers are marked as "added".)

If match is found, ApiElement diff(ApiElement newElement, DiffContext
context) method is called. The method is present on all levels if API
elements and is called recursively to produce ApiElement that contain
only differences between "old" and "new" elements. Method will return
null if no differences are found.

At each level differences are aggregated. For example, processing for
the class will find class-specific differences (visibility, inheritance
chain) and will include differences of methods and fields contained by
the class.

If full processing is requested, comparison is run twice swapping "old"
and "new" snapshots. The second pass is used to identify "added"
non-breaking changed. Information on the current state comparison passed
around in the DiffContext class.

The result of both comparisons are stored in the ApiComparisonResult
which passed to report generation routines.

The XML report generation utilizes ability of ApiElement’s create XML
output. As ApiComparisonResult contains only elements that changed,
writing it out produces ready (but hard to read) report on changes. To
produce easy-to-read report, XSLT Ant task is used to convert XML report
into HTML. The report/format.xslt file contains processing instruction
and styles are stored in the report/style.css file (see
ComparisonHtmlReport).

## Links

[API Tools](http://wiki.eclipse.org/PDE_UI_Incubator_ApiTools)

[API Tool User
Guide](http://wiki.eclipse.org/index.php/PDE_UI_Incubator_ApiTools_Compare)

[API Tools Demo](http://wiki.eclipse.org/API_Comparison_demo)

[PDE Incubator](http://wiki.eclipse.org/index.php/PDE_UI_Incubator) -
New projects that might be incorporated into PDE in future.

[API Compare for developers](Category:API "wikilink") [API Compare for
developers](Category:Equinox "wikilink")