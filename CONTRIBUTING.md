# Contributing to Eclipse Equinox

Thanks for your interest in this project.

## Project description

Eclipse Equinox is an implementation of the OSGi R6 core framework
specification, a set of bundles that implement various optional OSGi services
and other infrastructure for running OSGi-based systems.

* https://projects.eclipse.org/projects/eclipse.equinox

## Developer resources

Information regarding source code management, builds, coding standards, and more.

The project maintains the following source code repositories

* https://github.com/eclipse-equinox/equinox
* https://github.com/eclipse-equinox/equinox.binaries
* https://github.com/eclipse-equinox/p2

This project uses GitHub to track ongoing development and issues.

* Search for issues: https://github.com/eclipse-equinox/equinox/issues
* Search for historical issues: https://eclipse.org/bugs/buglist.cgi?product=Equinox
* Create a new report: https://github.com/eclipse-equinox/equinox/issues/new

Be sure to search for existing bugs before you create another one. Remember that
contributions are always welcome!

### Setting up the Development Environment automatically, using the Eclipse Installer (Oomph)

[![Create Eclipse Development Environment for Equinox](https://download.eclipse.org/oomph/www/setups/svg/Equinox.svg)](https://www.eclipse.org/setups/installer/?url=https://raw.githubusercontent.com/eclipse-equinox/equinox/master/releng/org.eclipse.equinox.releng/EquinoxConfiguration.setup&show=true "Click to open Eclipse-Installer Auto Launch or drag into your running installer")

### Running the Technology Compatibility Kit (TCK)

To run a TCK on a bundle you just need to go to the directory (e.g. `bundles/org.eclipse.equinox.cm`) 
and run:

`mvn clean verify -Pbuild-individual-bundles -Pbree-libs -Ptck`

### Building the native executable and launcher library binaries

The source code of the platform specific executables and launcher libraries for Equinox is located in the [org.eclipse.equinox.executable.feature](features/org.eclipse.equinox.executable.feature).
For details and instructions how to build it see its [README.md](features/org.eclipse.equinox.executable.feature/README.md).

## Eclipse Contributor Agreement

Before your contribution can be accepted by the project team contributors must
electronically sign the Eclipse Contributor Agreement (ECA).

* http://www.eclipse.org/legal/ECA.php

Commits that are provided by non-committers must have a Signed-off-by field in
the footer indicating that the author is aware of the terms by which the
contribution has been provided to the project. The non-committer must
additionally have an Eclipse Foundation account and must have a signed Eclipse
Contributor Agreement (ECA) on file.

For more information, please see the Eclipse Committer Handbook:
https://www.eclipse.org/projects/handbook/#resources-commit

## Contact

Contact the project developers via the project's "dev" list.

* https://dev.eclipse.org/mailman/listinfo/equinox-dev