The Eclipse and Equinox projects follow a well established build and
development rhythm. Getting to know this rhythm will help you to become
a better Eclipse/Equinox contributor or committer. If you have a feature
request or favorite bug that you'd like to see fixed, it's also useful
to know about this rhythm so you can understand when is a good time to
push on a bug, and to understand why your bug seems to be ignored for a
long time and then suddenly get fixed much later.

All of the information here is general guidance and advice rather than
formal policy. Consult the build schedule or other knowledgeable
committers in your area for the most accurate and up to date
information. Some component teams operate differently than others, so if
you are interested in working on a particular component it's a good idea
to find out the local customs and practices of that component in case
they differ from the general advice here.

## Days

Each day at 6PM EST/EDT is the nightly integration build. These builds
run against the HEAD revision in the master branch of the Git
repository, so whatever is in the repository at the time the build
checkout occurs is what gets built and tested. Releasing code between
6-7PM is generally a bad idea because you may be caught in the middle of
the checkout and cause compile errors in the build. Any deviations from
the regular nightly build schedule can be found on the releng [build
schedule](http://www.eclipse.org/eclipse/platform-releng/buildSchedule.html).

### Branches used

These builds run against the designated build branch for each
repository. Some repositories build out of master, some build out of a
branch called "integration". To find out what branch is used for a
particular repository, consult the
[repositories.txt](https://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/plain/cje-production/streams/repositories_master.txt)
file. Only release changes to the build branch if you intend for it to
be included in the integration builds.

## Milestones

Eclipse project milestones occur on roughly 3-week intervals. There is
typically an extra week allocated for holidays around Christmas, and an
extra week on the milestone that spans
[EclipseCon](http://eclipsecon.org). Final milestone builds generally
occur on a Friday (ideally Thursday, but sometimes on Saturday if things
go really wrong). The very beginning of a milestone is a great time to
be releasing your biggest code changes. This gives a month or more of
builds to shake out any bugs before hitting the milestone stabilization
week. Extra care needs to be taken as milestone stabilization week
approaches to avoid disrupting the carefully timed test, fix, and build
cycle of the milestone stabilization week itself. During that
stabilization week, the code base is frozen from Tuesday onwards, except
to fix regressions that are found during that week's testing. Anything
else should be considered an exception and be reviewed and approved not
only by the project lead but also by the PMC.

The stabilization week of a milestone has its own special schedule.
Generally, this involves:

  - Two I-builds per day, Monday through Wednesday.
  - A "warm-up" build on Sunday. This is a last chance to get in changes
    and verify them before the milestone week begins in earnest. Changes
    made on Monday must be tested very carefully before committing them.
  - Two builds on Monday, with the aim of producing a "test candidate"
    build to be used during Tuesday's testing.
  - A full day of testing on Tuesday. While builds continue on Tuesday,
    that is simply to have something scheduled, in case there is a bug
    so bad it interferes with testing, plus it is to help committers in
    our wide-spread timezones. Committers should not feel tempted to
    divert away from testing efforts to fix and release code simply
    because there are builds on Tuesday. Ideally, component leads will
    prepare a brief list of new features to test or bugs to verify. If
    in doubt, ask your component lead and/or test "Eclipse as a whole"
    -- try things you normally don't use, try a "mouseless test", read
    the help for various dialogs, etc.
  - Two builds I-builds on Wednesday, with the aim to produce a
    candidate milestone build to be ready on Thursday morning
  - No builds are scheduled on Thursday, but rebuilds can be requested
    by committers to release fixes for severe or high impact bugs.
    Thursday is the formal "sign-off" day. Before signing off, component
    leads need to decide if re-testing is required, or if they can
    simply confirm no changes have been made since the last good test.
  - On Friday the build is packaged, renamed, put in place on download
    servers at eclipse.org. There is much rejoicing, and/or committers
    dive into the next milestone of development. But, please remember
    the main branches remain frozen until the official note that the
    milestone is ready (That is, your component "signing-off" is not
    sufficient. There is always a chance a rebuild will be needed.)

Different components will have various other rituals during milestone
week. Some teams like to verify each bug fixed during the milestone, and
others may have different criteria for what fixes are appropriate on any
given day of milestone week. Consult with a seasoned committer on the
component you are interested in to learn the local customs during
milestone week.

Any deviations from the regular milestone week build schedule can be
found on the releng [build
schedule](http://www.eclipse.org/eclipse/platform-releng/buildSchedule.html).

## Releases

The Eclipse and Equinox projects follow the annual eclipse.org
simultaneous release schedule. Typically this means:

  - A release at the end of every quarter (March, June, September and
    December)

The release is broken into milestones. Eclipse platform and equinox
contribute to Milestone M1 and M3 apart for release candidate
milestones.

All feature work is completed by the end of RC1, the start of the
release end-game schedule. The release end-game involves progressively
increasing testing and lock down on changes according to the end-game
plan. The release end-game has its own special build schedule and
conventions on what changes are appropriate at any given time. End-game
plans can change slightly from release to release, but see the [Neon
freeze
plan](https://www.eclipse.org/eclipse/development/plans/freeze_plan_4_6.php)
for a flavor of the conventions and processes during the release
end-game.

## References

  - [The Eclipse
    Way](http://www.eclipsecon.org/2005/presentations/econ2005-eclipse-way.pdf)
    - presentation by John Wiegand and Erich Gamma on the Eclipse
    project development process
  - [The Eclipse weather
    forecast](http://www.eclipse.org/membership/weather/2009/Current.php)
  - [Eclipse Foundation master release
    timeline](http://www.eclipse.org/projects/timeline/)

[Category:How to Contribute](Category:How_to_Contribute "wikilink")
[Category:Eclipse Project](Category:Eclipse_Project "wikilink")
[Category:Equinox](Category:Equinox "wikilink")
[Eclipse_Platform_Releng](Category:Eclipse_Platform_Releng "wikilink")