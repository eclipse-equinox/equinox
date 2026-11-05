# Eclipse Equinox - Agent Instructions

Guidance for AI agents building, testing and changing this repository (Tycho/Maven based).

## Rules

* Always use Maven (`mvn`), online, with `package` or `verify`. Never `compile` only, never `-o`, no hand-rolled `javac`/`java -jar`.
* Use JDK 25 for Maven (see "Java version").
* Always pass `-Dtycho.baseline.replace=none` when you want to see your own changes (see "Baseline replacement").
* Tests run in the `verify` phase, `mvn test` prints no `Tests run:` line.
* Redirect Maven output to a file and grep it; a run prints very long `Resolving MavenDependencyRoots` lines, filter them with `grep -v MavenDependencyRoots`.
* Add `-am` (or list both modules with `-pl a,b`) when a test must see your changes in another bundle of this repository (see "Building single modules").
* If your change has no effect: check `mvn -v`, that `replaced with the baseline` does not occur in the log, that the changed bundle is in the `Reactor Summary`, and that a `Tests run:` line for your class exists.
* The first build needs network access (target platform, Maven Central, `https://repo.eclipse.org/content/repositories/eclipse/`).

## Quick start (commands)

```sh
export JAVA_HOME=<path to a JDK 25>      # confirm with the "Java version" line of `mvn -v`
LOG=${TMPDIR:-/tmp}/build.log
# Build one bundle, no tests:
mvn -B -pl bundles/<bundle> package -DskipTests -Dtycho.baseline.replace=none
# Run one test class:
mvn -B -pl bundles/org.eclipse.equinox.preferences.tests -am verify \
    -Dtest=PreferenceMetadataTest -Dsurefire.failIfNoSpecifiedTests=false -Dtycho.baseline.replace=none \
    > $LOG 2>&1; grep -v MavenDependencyRoots $LOG | grep -n "Tests run:\|BUILD\|ERROR" | tail -30
# Did Tycho throw away my changes? Must print 0:
grep -c "replaced with the baseline" $LOG
```

## Repository overview

Eclipse Equinox contains the OSGi core framework implementation (`org.eclipse.osgi`), implementations of OSGi services (Log, Configuration Admin, Metatype, Preferences, User Admin, Event Admin, Coordinator, ...), the Eclipse extension registry/preferences/common runtime, the native launcher and executable, and the OSGi TCK runners.

```
bundles/     OSGi bundles (mostly pomless: Tycho derives the build from MANIFEST.MF / bnd.bnd + build.properties; a few keep an explicit pom.xml)
  org.eclipse.osgi/                    core framework (bnd.bnd based)
  org.eclipse.equinox.*/               service implementations
  *.tests, org.eclipse.equinox.cm.test test bundles
features/    Eclipse features, incl. org.eclipse.equinox.executable.feature (native launcher sources, own README.md)
releng/      Oomph setup, tcks.target (target platform for TCK runs)
docs/        Jekyll site
```

The root `pom.xml` lists modules in profiles: `full-build` (default, everything; disable with `-Dskip-default-modules=true`) and a smaller module set for the TCK build.
`org.eclipse.equinox.region.tests` is commented out there (bug 470000), so `-pl bundles/org.eclipse.equinox.region.tests` fails with `Could not find the selected project in the reactor`.

## Java version

* Build with JDK 25, as Jenkins does and as more Eclipse bundles require; Tycho 5.x itself needs at least JDK 21.
  Maven uses `JAVA_HOME` (when set) rather than the `java` on the `PATH`, so check `mvn -v`.
* With JDK 17 Maven fails within seconds, before building anything (`BndProjectMapping ... has been compiled by a more recent version of the Java Runtime (class file version 65.0)`).
* Bundles are compiled and tested with the JDK running Maven.
  CI and Jenkins additionally pass `-Pbree-libs` (compile against the bundle's required execution environment via the machine-specific `~/.m2/toolchains.xml`); locally it is only needed for BREE-specific compile errors.

## `.mvn` configuration (picked up automatically from the repo root)

* `.mvn/extensions.xml`: registers the `tycho-build` core extension (that is why bundles need no `pom.xml`); the plugin version comes from the parent POM.
* `.mvn/maven.config` contains `-Pbuild-individual-bundles` and `-Dtycho.localArtifacts=ignore`.
  * `build-individual-bundles` adds the `eclipse-hosted` repository (`repo.eclipse.org`) so the parent POM can be found; everything not in the reactor comes from the target platform (I-build).
  * `tycho.localArtifacts=ignore`: Tycho ignores artifacts of this repo previously installed in `~/.m2`, so `mvn install` of A followed by `mvn -pl B` does not make B see A. Both must be in the same reactor invocation.
* There is no `.mvn/jvm.config`.
* Do not edit these files to work around problems; pass `-D`/`-P` options instead.

## Building single modules

* The root `pom.xml` has `<relativePath>../eclipse-platform-parent</relativePath>`, which does not exist in a standalone checkout, so the parent POM comes from the local Maven repository or `repo.eclipse.org` (SNAPSHOT, may be stale).
* `mvn -pl <module> -am ...` builds the module plus all upstream modules of this repo from source.
  `-pl bundles/org.eclipse.equinox.preferences.tests -am` builds osgi, common, registry, preferences, preferences.tests (about 25 s for a single test class).
* Without `-am`, dependencies on other bundles of this repo are **released I-build binaries from the target platform**, not from your working tree.
  Use `-am` (or `-pl a,b`) whenever the test depends on a change in another bundle.
* With `-am` and `-Dtest=X`, upstream modules without a matching test fail the build (`No tests matching pattern ...`), so add `-Dsurefire.failIfNoSpecifiedTests=false`.
* Whole-repo `mvn clean verify` is what CI runs and is slow; `-Dskip-default-modules=true` restricts it to the TCK module set.
* Native launcher binaries (`features/org.eclipse.equinox.executable.feature`) need the `equinox.binaries` repository; override its location with `-Dequinox.binaries.loc=...` or `EQUINOX_BINARIES_LOC` as CI does.
  See the feature's `README.md`.

## Baseline replacement (the main trap)

Symptom: build is green but your change is not in the jar, the tests, or the product that depends on it.

Cause: the parent POM configures `tycho-p2-plugin` with `baselineRepositories` and `tycho.baseline.replace=all`.
In `package` Tycho looks up each bundle by id and version in the baseline (the I-build repository).
If the same `major.minor.micro` exists there, the freshly built jar (and the `-sources` jar) is silently overwritten with the baseline jar, even on a plain `package` of a single bundle:

```
[INFO] MavenProject: org.eclipse.platform:org.eclipse.equinox.preferences:3.12.200-SNAPSHOT @ ...
    The main artifact has been replaced with the baseline version.
    The following attached artifacts have been replaced with the baseline version: [sources]
```

Diagnose: `grep -n "replaced with the baseline\|No baseline version" $LOG`.

Fix, pick one:

1. `-Dtycho.baseline.replace=none` on the command line.
   The log then shows `[WARNING] ... baseline and build artifacts have same version but different contents`, which is harmless.
2. Bump the bundle version (`Bundle-Version` in `META-INF/MANIFEST.MF`, also for `org.eclipse.osgi`).
   The log then shows `No baseline version ...` and your jar is kept.
   Do this only if the bump belongs to the change (`pr-checks.yml` enforces increments for changed bundles); never only to make a local run work.

Does NOT work: `-Dtycho.baseline=disable` (the parent POM hard-codes `<baselineMode>warnCommon</baselineMode>`), `mvn clean`, `-U`, `mvn install` or wiping `~/.m2` (it is not a stale cache).

Never commit `-Dtycho.baseline.replace=none` into a POM or `.mvn` file; CI relies on the baseline.

## Running tests

* Test bundles are `*.tests` (and `org.eclipse.equinox.cm.test`), packaged as `eclipse-test-plugin`; tests run in `verify`.
* Single class: `-Dtest=<SimpleClassName>`.
* Proof that tests ran: a line like `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0` in the log; reports are in `<bundle>/target/surefire-reports`.
* The GitHub `build.yml` build and TCK jobs run with `-Dmaven.test.failure.ignore`; locally a failing test fails the build.
* OSGi TCK, per bundle (~15 s for `org.eclipse.equinox.preferences`):
  `mvn -B -pl bundles/org.eclipse.equinox.preferences verify -Pbree-libs -Ptck`.
  Results: `<bundle>/target/tck-results/TEST-*.xml`.
  CI runs all TCKs with `-Pbuild-individual-bundles -Pbree-libs -Ptck -Dskip-default-modules=true`; TCKs need a JavaSE-17 toolchain (see `pom.xml`).
* UI/native/network tests may need a display or internet; judge failures by whether they relate to your change.

## CI

* `.github/workflows/build.yml`: `clean verify` with `-Papi-check -Dcompare-version-with-baselines.skip=false -Dmaven.test.failure.ignore=true -Dnative=<ws>.<os>.x86_64 -Dequinox.binaries.loc=...` on Linux/Windows/macOS, plus a TCK job.
  Needs full git history (`fetch-depth: 0`, required by the jgit timestamp provider) and `libgtk-3-dev` on Linux.
* `Jenkinsfile`: `mvn clean verify` with `-Pbree-libs -Papi-check -Pjavadoc` on JDK 25, plus on-demand native launcher builds (`LBv<maj>-<min>` tags) that commit binaries to `equinox.binaries`.
* `pr-checks.yml`: freeze period, no merge commits, version checks; the Eclipse Equinox Bot pushes `Version bump(s) for <release> stream` commits.
* A changed bundle with comparator errors needs its qualifier forced: append a line to the bundle's `forceQualifierUpdate.txt` (see commit `04cb8e97d`).

## Code and commit style

Follow the surrounding code first; keep every change small and focused on the task.

* **Commits:**
  * Do not commit on your own initiative.
    Finish the task and suggest a commit message (subject and body) instead.
    If asked to commit, let the user review the message first unless they said to go ahead.
  * Subject: imperative, <= ~72 characters, no trailing period, no prefix (history: `Ship extension point schemas in binary builds`, `Fix potential deadlock in ServiceLoaderMediatorHook.preFindClass()`); `[RelEng]` is used for workflow/infra changes.
  * Body: explain *why* (problem, motivation), not what the diff does; wrap at ~72 characters; reference issues as `Fixes https://github.com/eclipse-equinox/equinox/issues/<n>` (or the full URL of another repository's issue).
  * One logical change per commit, no unrelated formatting.
  * Non-committers need a `Signed-off-by:` trailer (Eclipse Contributor Agreement, see `CONTRIBUTING.md`).
  * AI disclosure: end with `Assisted-by: <Provider> <Model-Family> (<Version/ID>)`, e.g. `Assisted-by: Anthropic Claude Code (claude-opus-5-5)` as used in this repo's history.
    Add other trailers such as `Co-authored-by:` only if the user or your tooling requires it.
* **Java:** tabs for indentation, Eclipse formatter settings in each bundle's `.settings`, lines generally <= 120 characters, do not reformat untouched code.
  Eclipse-owned files keep their EPL-2.0 header with `SPDX-License-Identifier: EPL-2.0`; update the copyright year range of files you modify.
  Never alter license or copyright notices of third-party code (e.g. imported OSGi/Felix sources).
  Use public or package-private top-level types instead of inner classes/interfaces/records (project preference).
* **Javadoc and comments:** short; one or two sentences on what a method does and anything non-obvious.
  Comment *why*, never *what*; no commented-out code.
  Public API needs Javadoc and `@since`.
* **API and versions:** this repo is the OSGi reference implementation; do not break public API or OSGi spec behavior.
  Use `-Papi-check` when touching exported packages (CI does).
  Version bumps follow release streams or API changes (see "Baseline replacement"); do not bump drive-by.
  Manifests: Import/Export-Package must be accurate; use `bnd.bnd` where a bundle already has one.
* **Tests:** add or adjust a focused test in the matching `*.tests` bundle (JUnit 4 and 5 coexist; follow the neighbouring tests); never weaken or delete tests to make a build pass.
* **Hygiene:** `target/`, `bin/`, `.polyglot.*`, `.tycho-consumer-pom.xml` are git-ignored; do not commit build logs or stray files.
  Check `git status` before finishing.
* **Markdown:** start each sentence on a new line (easier review/diff); pad tables so they are readable as plain text.

## Generative AI (Eclipse Foundation guidelines)

This project follows the [Eclipse Foundation Generative AI usage guidelines](https://www.eclipse.org/projects/handbook/#genai).
The human committer stays responsible for everything that is contributed; make that easy for them:

* **Transparency:** disclose AI involvement with the `Assisted-by:` commit trailer (format above).
  For new files that are largely or entirely AI-generated, ask the user whether the handbook's header with the "AI Disclosure" note (`SPDX-License-Identifier: EPL-2.0 and CC0-1.0`) should be used; the copyright holder is the responsible human, never invent a name.
* **Verification:** output must be reviewable and verified.
  Run the relevant tests (see above), do not claim success without evidence, and say what you did not verify.
* **IP cleanliness:** do not copy code from other projects or from memory of other sources; write it yourself or stop and ask.
  Do not add third-party dependencies, bundled libraries or binaries without the user's explicit agreement; they require IP due diligence.
* **Confidentiality:** no secrets, credentials, personal data or other confidential content in code, commits, logs or issues.
* **No autonomous publishing:** do not push, open or merge pull requests, or write to issue trackers on your own; a human reviews and submits.

## References

* OSGi specifications https://docs.osgi.org/

