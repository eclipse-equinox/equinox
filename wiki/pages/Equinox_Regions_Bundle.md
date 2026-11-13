Steps to build the Equinox regions bundle:

1.  Ensure you are running Eclipse Indigo or later so that you have a
    suitable version of JUnit available. (Still to confirm whether this
    is the best way to access JUnit.)
2.  If you want, install egit into Eclipse from
    <http://download.eclipse.org/egit/updates>.
3.  Clone the git repo at:
    <http://git.eclipse.org/c/equinox/rt.equinox.bundles.git/> - for
    instance committers can issue "git clone
    <ssh://><committerId>@git.eclipse.org/gitroot/equinox/rt.equinox.bundles.git".
4.  Import the org.eclipse.equinox.region.tests and
    org.eclipse.equinox.region from this git repo into Eclipse.
5.  Clone the git repo at:
    <http://git.eclipse.org/c/equinox/rt.equinox.framework.git/> - for
    instance committers can issue "git clone
    <ssh://><committerId>@git.eclipse.org/gitroot/equinox/rt.equinox.framework.git".
6.  Import the project org.eclipse.osgi from this git repo into Eclipse.
7.  In Eclipse's preferences, go to Plug-in Development-\>Target
    Platform and select "Region Test Target"
8.  Open up the file
    org.eclipse.equinox.region.tests/regionTestTarget.target with the
    the Target Editor (should be the default editor).
9.  On the upper right hand corner there is a link "Set as Target
    Platform". Click that link. This should download the necessary
    eazymock and aspectj runtime bundles to compile and run against.
10. All compile errors should be gone. Actually you will have one in
    org.eclipse.equinox.region.tests.system.RegionPerformanceTests. This
    can be ignored for now since it is only used if you run the
    performance tests.
11. To run the tests right click on the class
    org.eclipse.equinox.region.tests.AllTests and select Run As -\>
    JUnit Plug-in Test.
12. If the tests pass, you can export the regions bundle by right
    clicking on the org.eclipse.equinox.region project and selecting
    export -\> plug-in development-\> deployable plug-ins and fragments.

(Some of these steps may be more generally applicable. This page can be
refactored as necessary.)

[Category:Equinox](Category:Equinox "wikilink")