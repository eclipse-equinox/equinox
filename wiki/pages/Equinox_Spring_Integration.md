Discussion of integrating Spring with OSGi are going on in the
[Spring-OSGi mailing list](http://groups.google.com/group/spring-osgi/).

The following are steps to getting started with integrating Spring with
the Equinox OSGi implementation. These directions assume you already
have access to the Spring-OSGi Subversion repository. If you don't have
access, stay tuned for details on when the repository will be made
public.

Preliminaries:

  - Download [Maven 2](http://maven.apache.org/download.html)
  - Install Maven 2 according to provided instructions in README.txt
  - Install the [Subversion Plug-in for
    Eclipse](http://subclipse.tigris.org/install.html)
  - Create your Eclipse workspace, and associate it with Maven by
    running "mvn -Declipse.workspace=<path-to-eclipse-workspace>
    eclipse:add-maven-repo"

Spring:

  - Checkout Spring from the Spring CVS repository:
    :pserver:anonymous@springframework.cvs.sourceforge.net:/cvsroot/springframework
  - Within Eclipse, run the build.xml in the project root directory with
    target "mvn.install.jars"; You will need to set the JAVA_HOME
    environment variable, and ensure Maven is on your PATH (on the
    Environment tab of the Ant launch configuration). This Ant script
    will put the Spring JARs in your local Maven repository.

Spring-OSGI:

  - Checkout Spring-OSGi from Subversion
  - Populate your local maven repository with the OSGi frameworks:
    Execute the /spring-osgi/add-frameworks.xml ant script; if you run
    this from within Eclipse you may need to add the maven/bin directory
    to your path in the Ant launch configuration dialog.
  - On the command line, cd to the root of the spring-osgi project in
    your workspace. Run the maven command "mvn clean install" to compile
    and deploy this as an OSGi bundle in your local maven repository.

Running the spell checking example:

  - Run "mvn install" from the spring-osgi/samples/spell-checker
    directory.
  - You can now either hack the file
    spring-osgi/samples/spell-checker/run/config.ini to fix the paths to
    your local maven repository, and run with the provided build.xml
    script, or ...
  - Copy the bundles listed in config.ini into some directory on your
    local disk (c:\\spring-osgi\\plugins); The util-1.0-SNAPSHOT.jar and
    apache-commons-1.0.jar won't be found, but they don't seem to be
    needed for anything
  - Change the Plugin Development target platform to be this directory
    (c:\\spring-osgi)
  - Create an OSGi Framework launch configuration
  - Set the start level of each bundle so that they are started in this
    order:
      - common-lib-1.0.jar
      - spring-core-2.1.jar
      - spring-aop-2.1.jar
      - spring-beans-2.1.jar
      - spring-context-2.1.jar
      - spring-osgi-2.1.jar
      - dictionary-service-1.0.jar
      - spellcheck-service-1.0.jar
  - Finally, run the launch configuration. From the console, manually
    increment the start level until all bundles are active

[Category:Equinox](Category:Equinox "wikilink")