![](https://www.eclipse.org/equinox/images/logo.png)

# Mission Statement

From a code point of view, Equinox is an implementation of the OSGi core framework specification, a set of bundles that implement various optional OSGi services and other infrastructure for running OSGi-based systems. The Equinox OSGi core framework implementation is used as the reference implementation and as such it implements all the required features of the latest OSGi core framework specification.

More generally, the goal of the Equinox project is to be a first class OSGi community and foster the vision of Eclipse as a landscape of bundles. As part of this, it is responsible for developing and delivering the OSGi framework implementation used for all of Eclipse. In addition. the project is open to:

- Implementation of all aspects of the OSGi specification. The main focus is specifications maintained by the Core Platform Expert Group (CPEG) but also may include implementations specified by other expert groups such as the Enterprise Expert Group (EEG)
- Investigation and research related to future versions of OSGi specifications and related runtime issues
- Development of non-standard infrastructure deemed to be essential to the running and management of OSGi-based systems
- Implementation of key framework services and extensions needed for running Eclipse (e.g., the Eclipse Adaptor, Extension registry) and deemed generally useful to people using OSGi.

# Implemented services an compliance

Equinox implements the follwoing specification with the given level of compliance:

| Chapter | Specification | Status (green = fully compliant, red = partly compliant)|
|---|---|---|
| 10 | [Framework API](https://docs.osgi.org/specification/osgi.core/8.0.0/framework.api.html) | ![](https://gist.githubusercontent.com/eclipse-equinox-bot/d941fe2a4992a018d88e778b48ee3135/raw/tck-badge-framework.svg) |
| 52 | [URL Handlers Service Specification](https://docs.osgi.org/specification/osgi.core/8.0.0/service.url.html) | ![](https://gist.githubusercontent.com/eclipse-equinox-bot/d941fe2a4992a018d88e778b48ee3135/raw/tck-badge-url.svg) |
| 58 | [Resolver Service Specification](https://docs.osgi.org/specification/osgi.core/8.0.0/service.resolver.html) | ![](https://gist.githubusercontent.com/eclipse-equinox-bot/d941fe2a4992a018d88e778b48ee3135/raw/tck-badge-resolver.svg) |
| 101 | [Log Service Specification](https://docs.osgi.org/specification/osgi.core/8.0.0/service.log.html) | ![](https://gist.githubusercontent.com/eclipse-equinox-bot/d941fe2a4992a018d88e778b48ee3135/raw/tck-badge-log.svg) |
| 104 | [Configuration Admin Service Specification](https://docs.osgi.org/specification/osgi.cmpn/8.1.0/service.cm.html) | ![](https://gist.githubusercontent.com/eclipse-equinox-bot/d941fe2a4992a018d88e778b48ee3135/raw/tck-badge-cm.svg) |
| 105 | [Metatype Service Specification](https://docs.osgi.org/specification/osgi.cmpn/8.1.0/service.metatype.html) | ![](https://gist.githubusercontent.com/eclipse-equinox-bot/d941fe2a4992a018d88e778b48ee3135/raw/tck-badge-metatype.svg) |
| 106 | [PreferencesService Specification](https://docs.osgi.org/specification/osgi.cmpn/8.1.0/service.prefs.html) | ![](https://gist.githubusercontent.com/eclipse-equinox-bot/d941fe2a4992a018d88e778b48ee3135/raw/tck-badge-preferences.svg) |
| 107 | [User Admin Service Specification](https://docs.osgi.org/specification/osgi.cmpn/8.1.0/service.useradmin.html) | ![](https://gist.githubusercontent.com/eclipse-equinox-bot/d941fe2a4992a018d88e778b48ee3135/raw/tck-badge-useradmin.svg) |
| 113 | [Event Admin Service Specification](https://docs.osgi.org/specification/osgi.cmpn/8.1.0/service.event.html) | ![](https://gist.githubusercontent.com/eclipse-equinox-bot/d941fe2a4992a018d88e778b48ee3135/raw/tck-badge-event.svg) |
| 130 | [Coordinator Service Specification](https://docs.osgi.org/specification/osgi.cmpn/8.1.0/service.coordinator.html) | ![](https://gist.githubusercontent.com/eclipse-equinox-bot/d941fe2a4992a018d88e778b48ee3135/raw/tck-badge-coordinator.svg) |
| 701 | [Tracker Specification](https://docs.osgi.org/specification/osgi.core/8.0.0/util.tracker.html) | ![](https://gist.githubusercontent.com/eclipse-equinox-bot/d941fe2a4992a018d88e778b48ee3135/raw/tck-badge-tracker.svg) |


# More information

- Homepage: https://www.eclipse.org/equinox/
- Bug tracker: https://github.com/eclipse-equinox/equinox/issues
- Asking questions and share ideas: https://github.com/eclipse-equinox/equinox/discussions

# Contributing
[![Create Eclipse Development Environment for Equinox](https://download.eclipse.org/oomph/www/setups/svg/Equinox.svg)](https://www.eclipse.org/setups/installer/?url=https://raw.githubusercontent.com/eclipse-equinox/equinox/master/releng/org.eclipse.equinox.releng/EquinoxConfiguration.setup&show=true "Click to open Eclipse-Installer Auto Launch or drag into your running installer")

For detailed information about development, testing and builds, see [CONTRIBUTING.md](CONTRIBUTING.md).
