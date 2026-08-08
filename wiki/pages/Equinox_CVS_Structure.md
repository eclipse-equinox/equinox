## Where Should Equinox Components Go?

The CVS components for the eclipse.equinox sub-project use the following
folder layout:

<code>

` org.eclipse.equinox/`<component-name>`/bundles/`<bundle-project-name>
` org.eclipse.equinox/`<component-name>`/features/`<feature-project-name>

</code>

Where **component-name** is the same name used for the bugzilla
component and Unix group. For example, p2, security, framework, bundles
etc. and **bundle-project-name** is the bundle symbolic name for the
bundle project and **feature-project-name** is the symbolic name of the
feature. This way each component is grouped under one directory. The
**bundles** directory is intended to separate the actual bundle projects
from other content associated with the component. For example, we may
want a **docs** folder to place documents about the component and a
**features** to contain the features associated with a component. So the
overall structure looks something like this:

<code>

` org.eclipse.equinox/`
`                    p2/`
`                       bundles/`
`                              org.eclipse.equinox.p2.artifact.optimizers`
`                              org.eclipse.equinox.p2.artifact.processors`
`                              org.eclipse.equinox.p2.artifact.repository`
`                              org.eclipse.equinox.p2.console`
`                             ...`
`                       features/`
`                       docs/`
`                    security/`
`                       bundles/`
`                              org.eclipse.equinox.security`
`                              org.eclipse.equinox.security.tests`
`                              org.eclipse.equinox.security.ui`
`                              org.eclipse.equinox.security.win32.x86`
`                       features/`
`                       docs/`

</code>

Since the creation of the top-level RT project, Equinox has lived in the
RT repo at /cvsroot/rt/org.eclipse.equinox. Prior to the creation of RT,
Equinox was found in the [Eclipse Project](Eclipse_Project "wikilink")
repository at /cvsroot/eclipse/org.eclipse.equinox.

[CVS Structure](Category:Equinox "wikilink")