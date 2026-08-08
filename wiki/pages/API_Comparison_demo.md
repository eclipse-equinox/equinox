Target audience: software developers, build engineers, Eclipse users.

Estimated demo time: 15 - 20 minutes.

## Setup

Setup includes downloading a recent version of Eclipse, building API
tools plug-ins from the source code, and installing things to compare.

1.  Download Eclipse 3.3M5 or later
2.  Download the following plug-ins from the PDE incubator
    **:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse** found under
    **pde-incubator/api-tooling/plugins**

<!-- end list -->

  - org.eclipse.pde.api.tools
  - org.eclipse.pde.api.tools.ui

<li>

Either use self-hosting or export those plug-ins as Deployable plug-ins
and add them to the Eclipse install to be used for the demo. If using
Sun VM increase PermGen size by using the following VM arguments:
**-vmargs -Xmx768m -XX:PermSize=128m -XX:MaxPermSize=256m**

</li>

<li>

Get a "baseline" product for comparison. I recommend downloading and
unzipping Eclipse 3.1 for this

</li>

<li>

Get a "new" product for comparison. I recommend downloading and
unzipping Eclipse 3.2 for this

</li>

</ol>

## Demo

Depending on the audience, first few minutes might be well spent to
describe what an API is and why API control is important for the
audience.

Describe how the tool can help by quickly identifying breaking changes
and backward-compatible changes.

  - Start the instance of Eclipse prepared in Step 1.
  - Describe that you are about to prepare the baseline snapshot.

<!-- end list -->

  - Select menu API Tools -\> Collect APIs
  - Specify name of the snapshot (Eclipse) and version (3.1)
  - Switch to the Source tab; change value of the Target platform to the
    Eclipse location created in the Step 4. Select all Target bundles to
    be included in the snapshot
  - Switch to the Repository tab; specify in the Repository location an
    existing folder to hold the snapshot
  - Press OK

<li>

Describe that you are about to compare a newer version of the product
against the baseline

</li>

  - Select menu API Tools -\> Compare APIs
  - Select "Eclipse" as a snapshot name; specify version as "3.2"
  - Switch to the repository tab and select the folder in which the
    snapshot was saved
  - Ensure that "Eclipse" is selected in the dropdown box for the name
    is the baseline snapshot
  - Ensure that version of the baseline snapshot is "3.1"
  - Switch to the Source tab; change value of the Target platform to the
    Eclipse location created in the Step 5. Select all Target bundles to
    be included in the comparison
  - Switch to the Report tab and specify the directory where reports
    will be stored. Make sure that "Open report(s)" checkbox is checked
  - Switch to Options tab and make sure that both Report APIs modified
    in backward-compatible way and Check container versions checkbox are
    checked
  - Press OK

</ul>

## Look at the results

  - **Breaking API changes**: Comment on the detected changes. For
    example, org.osgi.service.condpermadmin.Condition$BooleanCondition
    was removed due to the change in OSGi specification.
  - **Breaking API changes - false positives**: For example, use removal
    of the method from the org.eclipse.core.runtime.IExtension. The
    method was removed, but the old method is supplied in the
    compatibility fragment that has a classpath overriding normal
    processing. Some cases are just too exotic to detect with the tool.
  - Take a look at **non-breaking API changes** that could be used as a
    reminder for the "New and Noteworthy"
  - Take a look at the suggested bundle **version compliance report**

## Conclusion

Depending on the goal of the presentation, several points could be
highlighted in the conclusion:

  - Why this work is important for the audience (reduces time wasted by
    developers to adapt to a new version of the 3rd party software;
    makes customers happier as they will be broken less often by your
    updates)
  - Current state of the work (can be used, but output has to be
    reviewed by a human being)
  - Future plans (Javadoc tags; central repository; reference finder)

## Resources

[API Comparison
tool](http://wiki.eclipse.org/index.php/PDE_UI_Incubator_ApiTools_Compare)

[Equinox Demos](http://wiki.eclipse.org/index.php/Equinox_Demos)

[PDE UI Main page](http://www.eclipse.org/pde/pde-ui/)

[API Compare Demo](Category:API "wikilink") [API Compare
Demo](Category:Equinox "wikilink")