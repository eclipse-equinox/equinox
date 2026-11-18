In late July 2008 the Equinox project is moved from the Eclipse project
to the RT project. This page documents the migration of the various
components of Equinox to the RT project.

### CVS Projects

The following table maps existing Equinox projects to their new location
in the RT CVS repository. We must copy all history over to the new
location in RT and keep all old tags in the old Eclipse repository. Once
all projects have been copied with history over to the RT project then
we will delete the HEAD content from the old eclipse repository and
place a readme in each project indicating the new RT location. We must
also preserve all existing committer access rights to the projects that
get copied to the new RT location. We do not want to introduce a more
fine level of access control to the equinox projects.

Here is a summary of the changes

  - All projects must keep their same committer unix group access (e.g.
    equinox-framework, equinox-bundles, eclipse.equinox.p2-dev,
    eclipse.equinox.p2-dev)
  - The projects from equinox-bundles are getting moved into three
    different locations in the RT repository, but they are all keeping
    the existing committer access rights to equinox-bundles. No new
    committer access groups are to be created
  - All p2 content located at
    dev.eclipse.org:/cvsroot/eclipse/org.eclipse.equinox/p2 will be
    moved to the RT repository.
  - All security content located at
    dev.eclipse.org:/cvsroot/eclipse/org.eclipse.equinox/security will
    be moved to the RT repository

| dev.eclipse.org:/cvsroot/eclipse (old repo)                                       | dev.eclipse.org:/cvsroot/rt/org.eclipse.equinox (new repo)    |
| --------------------------------------------------------------------------------- | ------------------------------------------------------------- |
| Compendium                                                                        |                                                               |
| org.eclipse.equinox.app                                                           | compendium/bundles/org.eclipse.equinox.app                    |
| org.eclipse.equinox.cm                                                            | compendium/bundles/org.eclipse.equinox.cm                     |
| org.eclipse.equinox.device                                                        | compendium/bundles/org.eclipse.equinox.device                 |
| org.eclipse.equinox.ds                                                            | compendium/bundles/org.eclipse.equinox.ds                     |
| org.eclipse.equinox.event                                                         | compendium/bundles/org.eclipse.equinox.event                  |
| org.eclipse.equinox.http                                                          | compendium/bundles/org.eclipse.equinox.http                   |
| org.eclipse.equinox/bundles/bundles/org.eclipse.equinox.http.jetty5               | compendium/bundles/org.eclipse.equinox.http.jetty5            |
| org.eclipse.equinox/bundles/bundles/org.eclipse.equinox.http.jetty6               | compendium/bundles/org.eclipse.equinox.http.jetty6            |
| org.eclipse.equinox.http.servlet                                                  | compendium/bundles/org.eclipse.equinox.http.servlet           |
| org.eclipse.equinox.io                                                            | compendium/bundles/org.eclipse.equinox.io                     |
| org.eclipse.equinox.ip                                                            | compendium/bundles/org.eclipse.equinox.ip                     |
| org.eclipse.equinox.log                                                           | compendium/bundles/org.eclipse.equinox.log                    |
| org.eclipse.equinox.metatype                                                      | compendium/bundles/org.eclipse.equinox.metatype               |
| org.eclipse.equinox.preferences                                                   | compendium/bundles/org.eclipse.equinox.preferences            |
| org.eclipse.equinox.useradmin                                                     | compendium/bundles/org.eclipse.equinox.useradmin              |
| org.eclipse.equinox.util                                                          | compendium/bundles/org.eclipse.equinox.util                   |
| org.eclipse.equinox.wireadmin                                                     | compendium/bundles/org.eclipse.equinox.wireadmin              |
| org.eclipse.osgi.services                                                         | compendium/bundles/org.eclipse.osgi.services                  |
| org.eclipse.osgi.util                                                             | compendium/bundles/org.eclipse.osgi.util                      |
| Components                                                                        |                                                               |
| org.eclipse.equinox.common                                                        | components/bundles/org.eclipse.equinox.common                 |
| org.eclipse.equinox.registry                                                      | components/bundles/org.eclipse.equinox.registry               |
| org.eclipse.equinox/bundles/bundles/org.eclipse.equinox.transforms.xslt           | components/bundles/org.eclipse.equinox.transforms.xslt        |
| org.eclipse.equinox/bundles/bundles/org.eclipse.equinox.transforms.hook           | components/bundles/org.eclipse.equinox.transforms.hook        |
| Framework                                                                         |                                                               |
| org.eclipse.osgi                                                                  | framework/bundles/org.eclipse.osgi                            |
| org.eclipse.osgi.tests                                                            | framework/bundles/org.eclipse.osgi.tests                      |
| org.eclipse.equinox.launcher                                                      | framework/bundles/org.eclipse.equinox.launcher                |
| org.eclipse.equinox.executable                                                    | framework/bundles/org.eclipse.equinox.executable              |
| p2                                                                                |                                                               |
| org.eclipse.equinox/p2/org.eclipse.equinox.p2.releng                              | p2/org.eclipse.equinox.p2.releng                              |
| org.eclipse.equinox/p2/bundles/ie.wombat.jbdiff                                   | p2/bundles/ie.wombat.jbdiff                                   |
| org.eclipse.equinox/p2/bundles/ie.wombat.jbdiff.test                              | p2/bundles/ie.wombat.jbdiff.test                              |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.frameworkadmin                 | p2/bundles/org.eclipse.equinox.frameworkadmin                 |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.frameworkadmin.equinox         | p2/bundles/org.eclipse.equinox.frameworkadmin.equinox         |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.frameworkadmin.test            | p2/bundles/org.eclipse.equinox.frameworkadmin.test            |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.artifact.optimizers         | p2/bundles/org.eclipse.equinox.p2.artifact.optimizers         |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.artifact.processors         | p2/bundles/org.eclipse.equinox.p2.artifact.processors         |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.artifact.repository         | p2/bundles/org.eclipse.equinox.p2.artifact.repository         |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.console                     | p2/bundles/org.eclipse.equinox.p2.console                     |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.core                        | p2/bundles/org.eclipse.equinox.p2.core                        |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.director                    | p2/bundles/org.eclipse.equinox.p2.director                    |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.director.app                | p2/bundles/org.eclipse.equinox.p2.director.app                |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.directorywatcher            | p2/bundles/org.eclipse.equinox.p2.directorywatcher            |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.engine                      | p2/bundles/org.eclipse.equinox.p2.engine                      |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.exemplarysetup              | p2/bundles/org.eclipse.equinox.p2.exemplarysetup              |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.extensionlocation           | p2/bundles/org.eclipse.equinox.p2.extensionlocation           |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.garbagecollector            | p2/bundles/org.eclipse.equinox.p2.garbagecollector            |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.installer                   | p2/bundles/org.eclipse.equinox.p2.installer                   |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.jarprocessor                | p2/bundles/org.eclipse.equinox.p2.jarprocessor                |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.metadata                    | p2/bundles/org.eclipse.equinox.p2.metadata                    |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.metadata.generator          | p2/bundles/org.eclipse.equinox.p2.metadata.generator          |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.metadata.repository         | p2/bundles/org.eclipse.equinox.p2.metadata.repository         |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.publisher                   | p2/bundles/org.eclipse.equinox.p2.publisher                   |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.reconciler                  | p2/bundles/org.eclipse.equinox.p2.reconciler                  |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.reconciler.dropins          | p2/bundles/org.eclipse.equinox.p2.reconciler.dropins          |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.sar                         | p2/bundles/org.eclipse.equinox.p2.sar                         |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.selfhosting                 | p2/bundles/org.eclipse.equinox.p2.selfhosting                 |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.tests                       | p2/bundles/org.eclipse.equinox.p2.tests                       |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.tests.optimizers            | p2/bundles/org.eclipse.equinox.p2.tests.optimizers            |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.tools                       | p2/bundles/org.eclipse.equinox.p2.tools                       |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.touchpoint.eclipse          | p2/bundles/org.eclipse.equinox.p2.touchpoint.eclipse          |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.touchpoint.natives          | p2/bundles/org.eclipse.equinox.p2.touchpoint.natives          |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.ui                          | p2/bundles/org.eclipse.equinox.p2.ui                          |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.ui.admin                    | p2/bundles/org.eclipse.equinox.p2.ui.admin                    |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.ui.admin.rcp                | p2/bundles/org.eclipse.equinox.p2.ui.admin.rcp                |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.ui.sdk                      | p2/bundles/org.eclipse.equinox.p2.ui.sdk                      |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.updatechecker               | p2/bundles/org.eclipse.equinox.p2.updatechecker               |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.updatesite                  | p2/bundles/org.eclipse.equinox.p2.updatesite                  |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.simpleconfigurator             | p2/bundles/org.eclipse.equinox.simpleconfigurator             |
| org.eclipse.equinox/p2/bundles/org.eclipse.equinox.simpleconfigurator.manipulator | p2/bundles/org.eclipse.equinox.simpleconfigurator.manipulator |
| Security                                                                          |                                                               |
| org.eclipse.equinox/security/bundles/org.eclipse.equinox.security                 | security/bundles/org.eclipse.equinox.security                 |
| org.eclipse.equinox/security/bundles/org.eclipse.equinox.security.tests           | security/bundles/org.eclipse.equinox.security.tests           |
| org.eclipse.equinox/security/bundles/org.eclipse.equinox.security.ui              | security/bundles/org.eclipse.equinox.security.ui              |
| org.eclipse.equinox/security/bundles/org.eclipse.equinox.security.win32.x86       | security/bundles/org.eclipse.equinox.security.win32.x86       |
| org.eclipse.equinox/security/bundles/org.eclipse.equinox.security.macosx          | security/bundles/org.eclipse.equinox.security.macosx          |
| Server-Side                                                                       |                                                               |
| org.eclipse.equinox.http.registry                                                 | server-side/bundles/org.eclipse.equinox.http.registry         |
| org.eclipse.equinox.http.servletbridge                                            | server-side/bundles/org.eclipse.equinox.http.servletbridge    |
| org.eclipse.equinox.jsp.jasper                                                    | server-side/bundles/org.eclipse.equinox.jsp.jasper            |
| org.eclipse.equinox.jsp.jasper.registry                                           | server-side/bundles/org.eclipse.equinox.jsp.jasper.registry   |
| org.eclipse.equinox.servletbridge                                                 | server-side/bundles/org.eclipse.equinox.servletbridge         |

### Bugzilla Bugs

The following table maps the Equinox bugzilla components to their new
location in RT. We would like to keep the inbox e-mail addresses the
same where possible. An exception is the
equinox.bundles-inbox@eclipse.org inbox. The Bundles component is
getting split into three different inboxes (compendium, components, and
server-side). The plan is to move all existing bug in Bundles over to
the compendium inbox and then manually move them to the components and
server-side inboxes where appropriate.

| Eclipse Bugzilla component/inbox                          | RT Bugzilla component/inbox                                   |
| --------------------------------------------------------- | ------------------------------------------------------------- |
| Equinox-\>Bundles / equinox.bundles-inbox@eclipse.org     | Equinox-\>Compendium / equinox.compendium-inbox@eclipse.org   |
| N/A                                                       | Equinox-\>Components / equinox.components-inbox@eclipse.org   |
| Equinox-\>Framework / equinox.framework-inbox@eclipse.org | Equinox-\>Framework / equinox.framework-inbox@eclipse.org     |
| Equinox-\>p2 / equinox.p2-inbox@eclipse.org               | Equinox-\>p2 / equinox.p2-inbox@eclipse.org                   |
| Equinox-\>Security / equinox.security-inbox@eclipse.org   | Equinox-\>Security / equinox.security-inbox@eclipse.org       |
| N/A                                                       | Equinox-\>Server-Side / equinox.server-side-inbox@eclipse.org |

[Category:Equinox](Category:Equinox "wikilink")