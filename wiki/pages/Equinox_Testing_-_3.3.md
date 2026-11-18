## Test Machines

Eclipse 3.3 M6

  - Tom - Application Model, OSGi
  - Andrew - Launcher/Splash Screen
  - Oleg - Extension Registry
  - Simon - Server-side
  - John - Jobs
  - DJ - File-system, OSGi
  - Pascal - PDE/Build

Test machine allocation (post 3.3 M6)

  - MacOSx - Pascal
  - Linux (RedHat) - DJ
  - Linux (Suse) - Andrew
  - Windows XP - Oleg, Simon
  - Windows Vista - John

## Equinox

### Application Model

The following test scenario use [Application Model
Demo](Equinox_Application_Model_Demo "wikilink") presented at
[EclipseCon 2007](http://www.eclipsecon.org/2007/). Checkout the
following projects from the equinox incubator and follow the steps
below: <code>

`equinox-incubator/demos/app-model/org.eclipse.equinox.examples.app.selector`
`equinox-incubator/demos/app-model/org.eclipse.equinox.examples.sharedisplay`
`equinox-incubator/demos/app-model/org.eclipse.swt.examples.addressbook`
`equinox-incubator/demos/app-model/org.eclipse.swt.examples.browserexample`
`equinox-incubator/demos/app-model/org.eclipse.swt.examples.clipboard`
`equinox-incubator/demos/app-model/org.eclipse.swt.examples.graphics`
`equinox-incubator/demos/app-model/org.eclipse.swt.examples.paint`

</code>

1.  Export a project using the file
    /org.eclipse.equinox.examples.app.selector/AppSelector.product
2.  Launch the product with the -console option (e.g. eclipse -console)
3.  Attempt to launch all the applications using the application
    selector. Verify that the status of each application changes to
    **running**
4.  Stop all the applications by closing each individual application
    window (except for the application selector window). Verify that the
    status for each application changes to **inactive**
5.  Run the **apps** console command to get a list of available
    applications
6.  Start each launchable application using the **startApp** console
    command. Note that you can use a substring of the application id
    when launching (e.g. startApp paint). Verify that the status for
    each application started is reflected in the application selector.
    You can also verify the status by running the **apps** command.
7.  Stop each app using the **stopApp** console command. Verify that
    each application window is closed and the proper status is reflected
    in the application selector.
8.  Lock the paint application by running the console command **lockApp
    paint**.
9.  Attempt to start the paint application by running the console
    command **startApp paint**. Verify that an exception is thrown
    indicating the application is locked. Also run the console command
    **apps** to verify that paint application is locked.
10. Close and restart the application selector. Verify the paint
    application is persistently locked.
11. Unlock the paint application by running the console comand
    **unlockApp paint**.
12. Attempt to start the paint application by running the console
    command **startApp paint**. Verify that the application is launched.
    Close and restart the application selector. Verify the paint
    application is not locked.
13. Schedule a reccuring launch of the paint application by running the
    console command **schedApp paint (minute=\*) true**. This schedules
    the application to be launched once every minute. Run the apps
    command to confirm that the paint application is scheduled. Verify
    that the paint application is launched within 1 minute.
14. Close the paint application. Wait for 1 minute and and verify that
    the paint application is launched again.
15. Close and restart the application selector. Verify that the paint
    application is still scheduled. Wait for 1 minute and verify that
    the paint application is launched.
16. Unschedule the paint application by running the console command
    **unschedApp paint**. Verify the paint application is no longer
    scheduled. Close the paint application and wait for 2 minutes and
    verify the paint application is not launched.
17. Close and restart the application selector. Verify that the paint
    application is no longer scheduled.

### Extension Registry

The main test of the extension registry is that Eclipse starts with no
error messages twice in a row: start it in a clean install (no caches),
shutdown, and start it again (uses caches). If you can open the Error
log and see no error messages in the log after this, the extension
registry works.

All the rest of the testing is really done by automated tests and by
other teams using the registry. However, if you have some spare time,
here are some more tests that can be run.

#### **Part I. Startup / Shutdown / Caches**

1.  Download Eclipse SDK and unzip it in a new directory. If using a Sun
    VM, create a shortcut increasing memory size: **-vmargs -Xmx768m
    -XX:PermSize=128m -XX:MaxPermSize=256m**
2.  Start Eclipse (it will parse xml files and create caches)
3.  Create a new workspace
4.  Check that no errors are in the Error log
5.  Close Eclipse
6.  Start Eclipse again (it uses cached information this time)
7.  Check that no errors are in the Error log
8.  Close Eclipse

#### **Part II. Automated tests debug**

1.  Start Eclipse
2.  Check that no errors are in the Error log
3.  Connect to the CVS repository
    **:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse** and get the
    following plug-ins from it:

<!-- end list -->

  - org.eclipse.core.tests.harness
  - org.eclipse.core.tests.runtime
  - org.eclipse.test.performance

<li>

No errors should be generated as plug-ins compile

</li>

<li>

Select "All Runtime Tests" launch configuration and Run it

</li>

<li>

All tests should pass

</li>

<li>

Select "All Runtime Tests" configuration and Debug it

</li>

<li>

All tests should pass; observe console to see if any error messages
appear. The following console output is expected:

</li>

  - If running on 1.4 VM some JDT bundles will be unresolved (they need
    Java 1.5 or Java 1.6)
  - If running on 1.5 VM some JDT bundles will be unresolved (they need
    Java 1.6)
  - "Reading registry cache"
  - "Using registry cache..."
  - "Cumulative parse time so far"

</ol>

#### **Part III. Reaction to bundle events**

1.  Use the “new plug-in” wizard to create a plug-in project that
    contributes a menu item (Use "Hello World" template)
2.  Create a new launch configuration under "Eclipse Application", set
    program arguments to "-clean -console"
3.  Debug this launch configuration. Observe if all usual menus are in
    the usual places. Check that "Sample Menu" menu from the new plug-in
    is present
4.  In the console of the host Eclipse type "ss" and note the ID of the
    plug-in you created in the step 1.
5.  In the console of the host Eclipse type "uninstall <id_from_step_4>"
6.  The "Sample menu" item should be gone

#### **Part IV. Standalone registry run**

Several "simple" (non-OSGi) registry tests are run within regular
Eclipse tests. The purpose of this test is to see if the extension
registry can be used in an environment that really doesn’t have OSGi.

Create a workspace with the registry bundle, test bundle, and the
supplement bundle:

  - Import **org.eclipse.equinox.registry** and
    **org.eclipse.equinox.common** plug-ins from the target (Import -\>
    Plug-in development -\> Plug-ins and Fragments)
  - Check out **org.eclipse.equinox.supplement** bundle from CVS
    **:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse**
  - Check out **org.eclipse.equinox.test.standalone** project from the
    Git under
    **<git://git.eclipse.org/gitroot/equinox/rt.equinox.incubator.git/components/tests/>**
  - Run the launch configuration "All Equinox Standalone Tests"
  - Test(s) should pass

### Launcher & Splash Screen

  - command-line args
  - specifying a splash (both in a jar and not)
  - specifying a library
  - failure cases
  - mutliple versions of the launcher JAR (exe search algorithm)
  - headless mode with the exe (no dialogs)
  - plug-ins not co-located with exe
  - splash screen demos from EclipseCon 2007

### OSGi

  - Running the JAR
  - configuring the JAR

#### Running the OSGi TCK

The project equinox-incubator/org.eclipse.equinox.tcksetup in the
equinox incubator provides the necessary scripts and configuration
settings to run the osgi TCK. Checkout the project and follow the
instructions in the readme file in the root of the project.

#### Using the registry with Knopflerfish

  - Download and extract [Knopflerfish
    v2.0.1](http://www.knopflerfish.org) to (for instance) `c:/kf`.
  - Create a folder for your bundles `mkdir
    c:/kf/knopflerfish.org/osgi/jars/eclipse`
  - Get the org.eclipse.equinox.registry and org.eclipse.equinox.common
    bundles from the latest build and put them in
    `c:/kf/knopflerfish.org/osgi/jars/eclipse`.
  - Get the org.eclipse.equinox.supplement bundle from the latest
    Equinox build and put it in
    `c:/kf/knopflerfish.org/osgi/jars/eclipse.`
  - <em>Optional:</em> Rename the 3 Equinox bundles so they are easier
    to reference in the script.
  - Download the example bundle from
    [here](http://www.eclipse.org/equinox/demos/org.eclipse.equinox.examples.registry_1.0.0.200703210940.jar)
    and put it in `c:/kf/knopflerfish.org/osgi/jars/eclipse.`. (and
    optionally rename it to remove the version)
  - Edit the appropriate xargs file so we start the essential bundles
    and add System properties for the registry:

<!-- end list -->

    -Dorg.knopflerfish.gosg.jars=file:jars/
    -Declipse.registry.nulltoken=true
    -Declipse.createRegistry=false

    -istart log/log_all-2.0.0.jar
    -istart cm/cm_all-2.0.0.jar
    -istart util/util-2.0.0.jar
    -istart crimson/crimson-2.0.0.jar
    -istart console/console_all-2.0.0.jar
    -istart consoletty/consoletty-2.0.0.jar
    -istart frameworkcommands/frameworkcommands-2.0.0.jar
    -istart eclipse/org.eclipse.equinox.common.jar
    -istart eclipse/org.eclipse.equinox.supplement.jar
    -istart eclipse/org.eclipse.equinox.registry.jar
    -istart eclipse/org.eclipse.equinox.examples.registry.jar

  - Start Knopflerfish

<!-- end list -->

    java -jar framework.jar

### Server-Side

  - Test on other servers (test bridge)
  - Extension Registry and JSPs
  - Test pieces used with other compatible components

## Runtime

### File-system

Information on the EFS can be found [here](EFS "wikilink").

### Jobs

  - The Jobs JUnit tests can be run both as plug-in unit tests and as
    regular Java JUnit tests. We need to work on the test framework to
    be able to integrate running regular JUnit tests and have their
    results printed out with the rest of the test results.

## PDE/Build

  - There is a good demo from
    [EclipseCon 2007](http://www.eclipsecon.org/2007/).
  - Bundle/Plug-in export.
  - Headless builds.
      - Reproduce product export.
  - Packager
  - Plug-in/Fragment classpaths
  - Extensible API
  - Cycle detection
  - Review contribution of [integration
    tests](https://bugs.eclipse.org/bugs/show_bug.cgi?id=177677)
  - JNLP

[Testing 3.3](Category:Equinox "wikilink")