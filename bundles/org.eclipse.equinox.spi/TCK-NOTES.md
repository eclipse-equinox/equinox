# ServiceLoader Mediator OSGi TCK: known issues and current status

This note summarizes the investigation into the real OSGi TCK failures for
`org.eclipse.equinox.spi` (`org.osgi.test.cases.serviceloader`), so future
work can pick up where this session left off instead of re-discovering the
same root causes.

## 1. Root cause: hook never activates under the generic bnd/OSGi TCK launcher

`org.eclipse.equinox.spi` is implemented as a framework extension
(`Fragment-Host: org.eclipse.osgi;extension:=framework`).
Equinox's `HookRegistry.initialize()` discovers `hookconfigurators.properties`
(and therefore registers `ServiceLoaderMediatorHook`) by scanning the
classpath of the classloader that loaded `org.eclipse.osgi` itself, and it
only does this scan once, at framework construction time.

PDE/Eclipse's own JUnit launch config works around this by passing
`-Dosgi.framework.extensions=reference:file:...` (an `EclipseStarter`-only
mechanism), which pre-registers the extension before that scan happens.
The generic bnd/aQute launcher used by the real OSGi TCK has no equivalent
mechanism: it just installs `org.eclipse.equinox.spi` as a regular bundle via
`-runrequires`, by which point the scan has already completed, so the
mediator hook never activates and the whole implementation stays inert.

This is a single bootstrap/wiring gap, not several independent bugs: it
explained all 8 of the original 14 real-TCK test failures.

## 2. Fix applied here: `-runpath`

Bnd's `-runpath` (as opposed to `-runrequires`/`-runbundles`) adds a bundle's
jar directly to the framework's own launch classpath, which is exactly what
`osgi.framework.extensions` achieves for PDE.
This repo's `pom.xml` (`tck` profile) and
`org.eclipse.equinox.spi/build.properties` now add
`org.eclipse.equinox.spi` to `-runpath` for its own TCK run (see the
`tck.runpath` property and `pom.model.property.tck.runpath`), which restores
hook activation.
Do **not** add `org.eclipse.osgi` itself to `-runpath` in addition to
`-runfw`: this caused a duplicate-install/system-bundle-lock deadlock
(`BundleException: Unable to acquire the state change lock ... STARTED`).

## 3. Remaining, structural failure: `testAutoRegister`

With the `-runpath` fix in place, `testServiceFactory` and
`testImplNotExtended` pass.
`testAutoRegister` still fails (and cascades into ~10 other test failures)
because it stops and restarts "the mediator bundle" to verify the OSGi
Extender contract (cleanup on stop, re-registration on restart).
For a framework-extension mediator, the fragment's `osgi.extender` capability
is attributed to its **host** bundle in the wiring graph, i.e. the system
bundle (id 0).
Running the TCK self-hosted (the JUnit engine executing *inside* the very
framework under test) means stopping/restarting the system bundle would stop
the test engine itself, so this specific assertion cannot be exercised safely
in this configuration; it is not a bug in the mediator implementation.

Proposed upstream fix (guard, don't remove, the assertion):
**osgi/osgi#969** — adds
`assumeTrue(mediatorBundle.getBundleId() != 0, ...)` before the
stop/restart portion of `testAutoRegister`, so the test is skipped (not
failed) when the mediator resolves to the system bundle.

## 4. Local, temporary mitigation while osgi/osgi#969 is pending

Until the upstream test fix lands, a generic (non-hacky) way to exclude just
`testAutoRegister` from our own TCK run would need bnd/Tycho support for
excluding a single test method without Tycho having to reflectively
enumerate test methods at build time.

- **bndtools/bnd#7421** adds `PostDiscoveryFilter` OSGi-service support to
  `biz.aQute.tester.junit-platform`'s `Activator` (symmetric to the existing
  `TestExecutionListener` service tracking), so a small OSGi component could
  exclude a specific test at runtime. Not yet released/available.
- We also investigated whether JUnit Platform's own `ServiceLoader`-based
  auto-registration of `PostDiscoveryFilter` (contributed via an OSGi
  fragment attached to the `junit-platform-launcher` bundle, i.e. without
  needing the bnd patch at all) could be used as a stopgap. This is *not*
  reliable: JUnit Platform's `SessionPerRequestLauncher` re-runs the
  `ServiceLoader` scan on every `execute()` call using whatever the ambient
  thread context classloader happens to be at that moment, and in our
  experiments this only worked because of Equinox's own `ContextFinder`
  (a framework-internal, cross-bundle delegating classloader) transparently
  making the fragment's `META-INF/services` entry visible — behavior that is
  Equinox-specific and not something we should depend on. We decided not to
  pursue this further; use the upstream bnd fix (#7421) once available
  instead.

## 5. Known local build-environment dependency

Running the real TCK (`-Ptck`) here requires a Tycho build that has the
`useJDK=BREE` / `jre.compilation.profile` toolchain-precedence fix
(**eclipse-tycho/tycho#6314**, merged upstream). Without it, the TCK test
JVM can be launched with the wrong JDK and fail with
`java.lang.NoClassDefFoundError: java/util/SequencedMap` (a JDK 21+ class)
coming from `org.apache.felix.resolver`, unrelated to the actual test logic.

## Current status

With the `-runpath` fix: 12 of the 14 real-TCK tests for this bundle are
expected to pass; `testAutoRegister` remains a structural failure pending
osgi/osgi#969 (or a local exclusion mechanism, see section 4).
There is also a pre-existing, unrelated, intermittent Felix resolver bug
(`IndexOutOfBoundsException` in `ShadowList.replace`) that can surface during
`-resolve`, especially on `clean` builds; retrying usually avoids it.
