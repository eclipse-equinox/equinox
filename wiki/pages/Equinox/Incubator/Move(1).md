In late July 2008 the Equinox project moved from the Eclipse project to
the RT project. All of the graduated equinox projects have moved to the
new RT CVS repository. The Equinox incubator still has not moved. This
page documents the migration of the various components of the Equinox
incubator to the RT CVS repository.

### CVS Projects

The bug [258483](https://bugs.eclipse.org/bugs/show_bug.cgi?id=258483)
was opened to gather the list of projects that we would like to move
over the the RT Equinox incubator. The old Eclipse Equinox incubator
repository has a lot of stale and abandoned projects. We only want to
migrate the projects to the RT Equinox incubator that are active and
still have interest from the community.

The following table maps existing Equinox incubator projects to their
new location in the RT CVS repository. We must copy all history over to
the new location in RT and keep all old tags in the old Eclipse
repository. Once all projects have been copied with history over to the
RT project then we will delete the HEAD content from the old eclipse
repository and place a readme file in each project indicating the new RT
location. We must also preserve all existing committer access rights to
the projects that get copied to the new RT location. We do not want to
introduce a more fine level of access control to the equinox incubator
projects.

Here is a summary of the changes

  - All projects must keep their same committer unix group access (e.g.
    rt.equinox.incubator)
  - No new committer access groups are to be created.
  - All projects will be placed in a subfolder of org.eclipse.equinox
    called **incubator**
  - Each main area will have a subfolder under **incubator**. For
    example, components, compendium, framework, p2, security,
    server-side

| dev.eclipse.org:/cvsroot/eclipse/equinox-incubator (old repo)                       | dev.eclipse.org:/cvsroot/rt/org.eclipse.equinox/incubator (new repo)                |
| ----------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| Aspects                                                                             |                                                                                     |
| aspects/build.ibm.oti                                                               | aspects/bundles/build.ibm.oti                                                       |
| aspects/org.eclipse.equinox.weaving.hook                                            | aspects/bundles/org.eclipse.equinox.weaving.hook                                    |
| aspects/org.eclipse.equinox.weaving.aspectj                                         | aspects/bundles/org.eclipse.equinox.weaving.aspectj                                 |
| aspects/org.eclipse.equinox.weaving.caching                                         | aspects/bundles/org.eclipse.equinox.weaving.caching                                 |
| aspects/org.eclipse.equinox.weaving.caching.j9                                      | aspects/bundles/org.eclipse.equinox.weaving.caching.j9                              |
| aspects/org.eclipse.equinox.weaving.build                                           | aspects/bundles/org.eclipse.equinox.weaving.build                                   |
| aspects/org.eclipse.equinox.weaving.feature                                         | aspects/bundles/org.eclipse.equinox.weaving.feature                                 |
| aspects/org.eclipse.equinox.weaving.site                                            | aspects/bundles/org.eclipse.equinox.weaving.site                                    |
| aspects/tests/org.eclipse.equinox.weaving.tests                                     | aspects/tests/org.eclipse.equinox.weaving.tests                                     |
| aspects/tests/org.eclipse.equinox.weaving.tests.aunit                               | aspects/tests/org.eclipse.equinox.weaving.tests.aunit                               |
| aspects/tests/org.eclipse.equinox.weaving.tests.bundles.aspectWeaving               | aspects/tests/org.eclipse.equinox.weaving.tests.bundles.aspectWeaving               |
| aspects/tests/org.eclipse.equinox.weaving.tests.bundles.aspectWeavingLT             | aspects/tests/org.eclipse.equinox.weaving.tests.bundles.aspectWeavingLT             |
| aspects/tests/org.eclipse.equinox.weaving.tests.bundles.aspectWeavingLT2            | aspects/tests/org.eclipse.equinox.weaving.tests.bundles.aspectWeavingLT2            |
| aspects/tests/org.eclipse.equinox.weaving.tests.bundles.bundleSupplement            | aspects/tests/org.eclipse.equinox.weaving.tests.bundles.bundleSupplement            |
| aspects/tests/org.eclipse.equinox.weaving.tests.bundles.bundleSupplementWithRequire | aspects/tests/org.eclipse.equinox.weaving.tests.bundles.bundleSupplementWithRequire |
| aspects/tests/org.eclipse.equinox.weaving.tests.bundles.exportSupplement            | aspects/tests/org.eclipse.equinox.weaving.tests.bundles.exportSupplement            |
| aspects/tests/org.eclipse.equinox.weaving.tests.bundles.fragment                    | aspects/tests/org.eclipse.equinox.weaving.tests.bundles.fragment                    |
| aspects/tests/org.eclipse.equinox.weaving.tests.bundles.importSupplement            | aspects/tests/org.eclipse.equinox.weaving.tests.bundles.importSupplement            |
| aspects/tests/org.eclipse.equinox.weaving.tests.bundles.extraSpareBundle            | aspects/tests/org.eclipse.equinox.weaving.tests.bundles.extraSpareBundle            |
| aspects/tests/org.eclipse.equinox.weaving.tests.bundles.spareBundle                 | aspects/tests/org.eclipse.equinox.weaving.tests.bundles.spareBundle                 |
| aspects/tests/org.eclipse.equinox.weaving.tests.bundles.wildBundleSupplement        | aspects/tests/org.eclipse.equinox.weaving.tests.bundles.wildBundleSupplement        |
| aspects/tests/org.eclipse.equinox.weaving.tests.performance                         | aspects/tests/org.eclipse.equinox.weaving.tests.performance                         |
| aspects/tests/org.eclipse.equinox.weaving.tests.remoteAspect                        | aspects/tests/org.eclipse.equinox.weaving.tests.remoteAspect                        |
| aspects/tests/org.eclipse.equinox.weaving.tests.remoteAspectFragment                | aspects/tests/org.eclipse.equinox.weaving.tests.remoteAspectFragment                |
| aspects/demos/org.eclipse.equinox.weaving.demo.hello                                | aspects/tests/org.eclipse.equinox.weaving.demo.hello                                |
| aspects/demos/org.eclipse.equinox.weaving.demo.hello.aspects                        | aspects/tests/org.eclipse.equinox.weaving.demo.hello.aspects                        |
| aspects/demos/org.eclipse.equinox.weaving.demo.target                               | aspects/tests/org.eclipse.equinox.weaving.demo.target                               |
| Compendium                                                                          |                                                                                     |
| org.eclipse.equinox.log                                                             | compendium/bundles/org.eclipse.equinox.log                                          |
| org.eclipse.equinox.log.test                                                        | compendium/tests/org.eclipse.equinox.log.test                                       |
| org.eclipse.equinox.autoconf                                                        | compendium/bundles/org.eclipse.equinox.autoconf                                     |
| org.eclipse.equinox.deploymentadmin                                                 | compendium/bundles/org.eclipse.equinox.deploymentadmin                              |
| Components                                                                          |                                                                                     |
| tests/org.eclipse.equinox.tests.standalone                                          | components/tests/org.eclipse.equinox.tests.standalone                               |
| Framework                                                                           |                                                                                     |
| org.eclipse.equinox.initializer                                                     | framework/bundles/org.eclipse.equinox.initializer                                   |
| p2                                                                                  |                                                                                     |
| org.eclipse.equinox.frameworkadmin.examples                                         | p2/bundles/org.eclipse.equinox.frameworkadmin.examples                              |
| org.eclipse.equinox.frameworkadmin.felix                                            | p2/bundles/org.eclipse.equinox.frameworkadmin.felix                                 |
| org.eclipse.equinox.frameworkadmin.knopflerfish                                     | p2/bundles/org.eclipse.equinox.frameworkadmin.knopflerfish                          |
| provisioning/org.eclipse.equinox.p2.profile.recovery                                | p2/bundles/org.eclipse.equinox.p2.profile.recovery                                  |
| provisioning/org.eclipse.equinox.p2.repositoryoptimizer                             | p2/bundles/org.eclipse.equinox.p2.repositoryoptimizer                               |
| provisioning/org.eclipse.equinox.p2.selfgenerator                                   | p2/bundles/org.eclipse.equinox.p2.selfgenerator                                     |
| provisioning/org.eclipse.equinox.p2.sharedprofile                                   | p2/bundles/org.eclipse.equinox.p2.sharedprofile                                     |
| provisioning/org.eclipse.equinox.prov.war.feature                                   | p2/bundles/org.eclipse.equinox.prov.war.feature                                     |
| Security                                                                            |                                                                                     |
| security/org.eclipse.equinox.security.sample                                        | security/bundles/org.eclipse.equinox.security.sample                                |
| security/org.eclipse.equinox.security.provider                                      | security/bundles/org.eclipse.equinox.security.provider                              |
| security/org.eclipse.equinox.security.boot.jre14x                                   | security/bundles/org.eclipse.equinox.security.boot.jre14x                           |
| security/org.eclipse.equinox.security.boot.jre15x                                   | security/bundles/org.eclipse.equinox.security.boot.jre15x                           |
| Server-Side                                                                         |                                                                                     |
| org.eclipse.equinox.http.helper                                                     | server-side/bundles/org.eclipse.equinox.http.helper                                 |
| org.eclipse.equinox.servletbridge.feature                                           | server-side/bundles/org.eclipse.equinox.servletbridge.feature                       |
| Monitoring                                                                          |                                                                                     |
| org.eclipse.equinox.jmx.client                                                      | monitoring/bundles/org.eclipse.equinox.jmx.client                                   |
| org.eclipse.equinox.jmx.client.rmi                                                  | monitoring/bundles/org.eclipse.equinox.jmx.client.rmi                               |
| org.eclipse.equinox.jmx.client.xmlrpc                                               | monitoring/bundles/org.eclipse.equinox.jmx.client.xmlrpc                            |
| org.eclipse.equinox.jmx.client.feature                                              | monitoring/bundles/org.eclipse.equinox.jmx.client.feature                           |
| org.eclipse.equinox.jmx.common                                                      | monitoring/bundles/org.eclipse.equinox.jmx.common                                   |
| org.eclipse.equinox.jmx.common.feature                                              | monitoring/bundles/org.eclipse.equinox.jmx.common.feature                           |
| org.eclipse.equinox.jmx.server                                                      | monitoring/bundles/org.eclipse.equinox.jmx.server                                   |
| org.eclipse.equinox.jmx.server.rmi                                                  | monitoring/bundles/org.eclipse.equinox.jmx.server.rmi                               |
| org.eclipse.equinox.jmx.server.xmlrpc                                               | monitoring/bundles/org.eclipse.equinox.jmx.server.xmlrpc                            |
| org.eclipse.equinox.jmx.vm                                                          | monitoring/bundles/org.eclipse.equinox.jmx.vm                                       |
| org.eclipse.equinox.preferences.jmx                                                 | monitoring/bundles/org.eclipse.equinox.preferences.jmx                              |
| org.eclipse.equinox.registry.jmx                                                    | monitoring/bundles/org.eclipse.equinox.registry.jmx                                 |
| org.eclipse.osgi.jmx                                                                | monitoring/bundles/org.eclipse.osgi.jmx                                             |
| org.eclipse.swt.jmx                                                                 | monitoring/bundles/org.eclipse.swt.jmx                                              |
| org.eclipse.core.resources.jmx                                                      | monitoring/bundles/org.eclipse.core.resources.jmx                                   |
| org.eclipse.equinox.jmx.server.feature                                              | monitoring/bundles/org.eclipse.equinox.jmx.server.feature                           |
| net.sourceforge.mx4j                                                                | monitoring/bundles/net.sourceforge.mx4j                                             |
| org.apache.xmlrpc                                                                   | monitoring/bundles/org.apache.xmlrpc                                                |
| demos                                                                               |                                                                                     |
| demos/\*                                                                            | demos/\*                                                                            |

### Bugzilla Bugs

The equinox incubator bugzilla inbox has already been moved to
**RT-\>Equinox-\>Incubator** and the inbox has remained
**equinox.incubator-inbox@eclipse.org**

[Category:Equinox](Category:Equinox "wikilink")