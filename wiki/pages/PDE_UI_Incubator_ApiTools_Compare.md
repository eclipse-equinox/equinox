**API Comparison Tool**

## What is it for?

The tool compares APIs exposed by two versions of your product. It
reports changes that might break backward compatibility and provides a
list of backward-compatible modifications.

Reports are produced as XML and HTML files. Here is the sample report
showing APIs modified in a way that might break backward compatibility:

![ApiToolsBreakageReport.JPG](ApiToolsBreakageReport.JPG
"ApiToolsBreakageReport.JPG")

and here is the sample report on backward compatible API changes:

![ApiToolsCompatibleReport.JPG](ApiToolsCompatibleReport.JPG
"ApiToolsCompatibleReport.JPG")

## What’s an API?

For **OSGi bundles** the definition of API is taken from the approach
used throughout Eclipse and described in this excellent article by Jim
des Rivieres: [Eclipse
API](http://www.eclipse.org/articles/article.php?file=Article-API-Use/index.html);
in particluar, section "How to tell API from non-API".

API tools mostly follow API definitions outlined in that article. One
thing that is evolved since that article was written is the wide-spread
adoption of OSGi model. As a result, API tools use OSGi manifest to
determine API packages (and don't use Javadoc presence for this
purpose). All packages exported in manifest files are deemed to be API
packages unless they have "x-internal" or "x-friends" qualifiers.

In order to reduce number of false positives during API comparison:

  - addition of new elements (methods, fields, and such) to non-final
    classes is not treated as an API breach
  - addition of new elements to interfaces can be configured by a user
    to be treated as an API breakage or not

For **non-bundles** (Java JARs, etc.) it assumes that all packages are
APIs unless they have ".internal" in the package name.

To allow for "special cases" API comparison tool has an exclusion
mechanism - users can select specific API breaches not to appear on the
report. The exclusions are specific, i.e., "removal of method ABC from
class XYZ is OK". The vision for this is that exclusion mechanism will
be superseded with [Javadoc
processing](http://wiki.eclipse.org/index.php/API_Javadoc_tags).

Some links useful in determining what is an API:

[How to Use the Eclipse
API](http://www.eclipse.org/articles/Article-API%20use/eclipse-api-usage-rules.html)
- APIs in Eclipse from consumer's view point

[Binary
Compatibility](http://java.sun.com/docs/books/jls/third_edition/html/binaryComp.html)
- The Java Language Specification

## How does the tool work?

The tool contains two operations: create an API snapshot and compare the
code against API snapshot.

Both operations can be accessed via menus or run in a batch mode.

The dialog to create API snapshot looks like this:

![ApiToolsCollect.JPG](ApiToolsCollect.JPG "ApiToolsCollect.JPG")

And the dialog to compare code with the API snapshot looks like this:

![ApiToolsCompare.JPG](ApiToolsCompare.JPG "ApiToolsCompare.JPG")

## Batch processing

The tool can be used in the batch mode. The command line is:

ApiTools { -create | -compare } -control <control_file_name>

Control file is an XML file that specifies repository location, location
of the input or output, and a few options. Those files can be created
from UI dialogs using "Export" commands.

## What's a repository?

Repository is a place where the tool stores API snapshots. At present
the only type of repository supported is a folder on the file system.
The location of the folder should be specified in the "Repository" tab:

![ApiToolsRepository.JPG](ApiToolsRepository.JPG
"ApiToolsRepository.JPG")

## Do I need to specify something else?

For comparison, report location should be specified on the "Report" tab:

![ApiToolsReport.JPG](ApiToolsReport.JPG "ApiToolsReport.JPG")

This is the folder where XML and HTML reports will be placed by the
tool.

While not a requirement, check the options tab:

![ApiToolsOptions.JPG](ApiToolsOptions.JPG "ApiToolsOptions.JPG")

  - Report APIs modified in backward-compatible way - only breaking
    changes will be detected and reported if this box is unchecked.
  - Report missing source containers - uncheck this option if you
    compare one bundle against the snapshot made from a whole product.
  - Report additions on interfaces as API breakage - adding new method
    to an interface is a breaking change, but a lot of interfaces in
    Eclipse has a soft "do not implement this interface" rule which make
    it OK. Uncheck this box to reduce the amount of noise generated;
    check this box to report this as a breaking change.
  - Exclude known differences - the file containing known differences
    can be placed in the repository to eliminate those differences from
    subsequent comparison results.

## Getting the Pieces

Source code is stored in the PDE incubator
(:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse) in the
HEAD/pde-incubator/api-tooling/plugins.

There are two plug-ins:

  - org.eclipse.pde.api.tools
  - org.eclipse.pde.api.tools.ui

The Eclipse 3.3M4 or later should be used to compile those plug-ins.

## Resources

[Eclipse Bugzilla](https://bugs.eclipse.org/bugs/) - Eclipse bug
tracking database.

[PDE Incubator](http://wiki.eclipse.org/index.php/PDE_UI_Incubator) -
New projects that might be incorporated into PDE in future.

[PDE UI Home Page](http://www.eclipse.org/pde/pde-ui/) - The main PDE UI
web site.

[API Compare](Category:API "wikilink") [API
Compare](Category:Equinox "wikilink")