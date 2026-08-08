## Getting Started

### Getting the Source Code

The source code for this implementation is available from the
**dev.eclipse.org** CVS server in the **/cvsroot/eclipse** respository.
You need to check out three projects from **HEAD**. There is a team
project set file to assist you with this.

1.  Add the CVS server to your repositories view
    (:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse).
2.  Expand the server and "HEAD" elements in the tree.
3.  Check out the the **/org.eclipse.pde.api.tools.doc** project.
4.  In your Package Explorer, select the **projectSet.psf** file (for
    extssh access) or **pserverProjectSet.psf** file (for pserver
    access) in the root folder of the "/org.eclipse.pde.api.tools.doc
    project".
5.  Select **File \> Import**. On the first page of the import wizard
    select **Team \> Team Project Set** and press "Next".
6.  On the second page of the wizard, the
    "org.eclipse.pde.api.tools.releng\\projectSet.psf" should already be
    specified as the file to import. If not, choose it. Press "Finish".
7.  When asked for a user name and password although you chose the
    **pserverProjectSet.psf**, just enter **anonymous** as user name and
    leave the password field empty.

The PDE API Tools projects and tests will be added to your workspace.

### Bugs and Enhancement Requests

The API tooling project uses [Bugzilla](https://bugs.eclipse.org/bugs/)
for tracking bugs and enhancement requests. [Active API tooling
bugs](https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=&classification=Eclipse&product=PDE&component=API+Tools&long_desc_type=allwordssubstr&long_desc=&bug_file_loc_type=allwordssubstr&bug_file_loc=&status_whiteboard_type=allwordssubstr&status_whiteboard=&keywords_type=allwords&keywords=&bug_status=UNCONFIRMED&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&emailtype1=substring&email1=&emailtype2=substring&email2=&bugidtype=include&bug_id=&votes=&chfieldfrom=&chfieldto=Now&chfieldvalue=&cmdtype=doit&order=Reuse+same+sort+as+last+time&field0-0-0=noop&type0-0-0=noop&value0-0-0=)
can be found with this query. All bugs should be filed in with the
**Eclipse** project, **PDE** product, under the **API Tools** component.

### Miscellaneous

For all other developer information / questions please visit the [API
Tools website](http://www.eclipse.org/pde/pde-api-tools/index.php).

## Testing

Manual testing must be done for all aspects of API tooling. To help with
what should be tested (and what is the expected result) you can refer to
the [API tools test
plan](http://www.eclipse.org/pde/pde-api-tools/test_plans/test_plans.php)
page.

### Running the JUnit Tests

The JUnit tests run as standard JUnit tests (it can also run as JUnit
plug-in tests). However, the tests require one mandatory VM argument:

`-DrequiredBundles=`<path to directory containing standard Eclipse plug-ins>

The required bundles are used as a pool when resolving required bundles
for test bundles in the suite.

The tests can be run individually, or you can run them all from the
APIToolsTestSuite class.

[API Tools](Category:API "wikilink") [API
Tools](Category:Equinox "wikilink") [API Tools](Category:PDE "wikilink")
[API Tools](Category:Eclipse_Project "wikilink")