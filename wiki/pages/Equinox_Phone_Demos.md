## [Nokia 9300 Communicator](http://www.nokiausa.com/phones/9300)

### Local Machine Setup

The first thing you need to do is ensure that your machine can talk to
your phone. We used Bluetooth for our communications. We also downloaded
and installed the [Nokia PC Suite](http://nokia.com/pcsuite) so we could
use the File Manager tool for transfering files from your local machine
to the phone.

### Java on the phone

The Java VM on the phone is J9 Personal Profile 1.0 (ppro10) Well, that
was the VM on *our* phone. :-)

### Java on your local machine

To make things easier, we set up the demos on our local machine, ensured
they were working ok, and then transfered them to the phone. So the
first thing we had to do was ensure that we had the right VM set up
locally.

  - First you need to download the [Foundation 1.0/Personal Profile 1.0
    VM](J9 "wikilink").
  - **Ensure that the JAVA_HOME environment variable is not set. Having
    it set confuses other Javas (including J9) into using different
    directory structures.**
  - Add the VM as an Installed JRE (`Window -> Preferences -> Java ->
    Installed JREs`).
  - Add the 2 extra libraries as external JARs (`ppro-ui.jar`,
    `ppro-extras.jar`).
  - Add the following default VM arg: `-jcl:ppro10`.

### Running a Java application on the phone

  - The file-system can be referenced like Windows with `C:` and `D:`
    (the `D:` drive is the memory card).
  - The J9 VM is associated with the `.j9` file-type. You create a `.j9`
    file with all the command-line arguments for your application and
    then when you click on it in the file manager it will run J9.
  - The file contains everything that you want on the command line
    except the reference to the VM executable. e.g.

<!-- end list -->

    -cp foo.jar com.Foo

  - *Note:* any arguments **after** the class name on the commandline
    are passed to the class as arguments to the application and are
    **not** interpreted by the VM. Therefore if you are trying to set
    system properties, it should be done early in the command-line. e.g.

<!-- end list -->

    -Dfoo=bar -cp foo.jar com.Foo

  - Use the file manager on the phone to select the `.j9`. Opening it
    will launch the Java application.

### Running an Eclipse Application on the phone

#### XML

Since Eclipse uses an XML parser to read the registry and other files,
you will need to get bundles that provides the XML Parser APIs. A good
candidate for this are 2 bundles (`org.eclipse.ercp.xml`,
`org.eclipse.ercp.xmlParserAPIs`) which are included as part of the
[eRCP](http://eclipse.org/ercp) project. These two bundles are created
as framework extensions so you need to tell OSGi about them by setting a
system property:

    osgi.framework.extensions=org.eclipse.ercp.xmlParserAPIs,org.eclipse.ercp.xml

#### Personal Profile EE

Now when you try and run, you will likely get an exception saying that
OSGi couldn't match the required Execution Environment for some of the
bundles. They are looking for the Founation 1.0 EE but the current VM
doesn't match this. So we need to create a new profile which looks like
this:

    org.osgi.framework.system.packages = \
     javax.microedition.io
    org.osgi.framework.bootdelegation = \
     javax.microedition.io
    org.osgi.framework.executionenvironment = \
     OSGi/Minimum-1.0,\
     OSGi/Minimum-1.1,\
     CDC-1.0/Foundation-1.0,\
     CDC-1.0/PersonalProfile-1.0,\
     CDC-1.1/PersonalProfile-1.1
    osgi.java.profile.name = CDC-1.0/PersonalProfile-1.0

Next we need to set a system property for the new profile which contains
a file URL to the location of the file:

    osgi.java.profile=file:d:/temp/ppro.profile

### Writing to the Console

  - `C:/logs/j9vm` - You *must* have this directory created or your Java
    application will hang when it tries to write something to
    `System.out` or `System.err`.
  - When you run your Java application 2 files will be created in this
    directory, one for `System.err` and one for `System.out`.
  - The files will be named <pid>`_syserr.txt` and <pid>`_sysout.txt`
  - Output will always be to this directory, even if your JAR files are
    on a different drive.

### Extras

  - j9 -io:host:port
  - console telnet

### Networking

In order to set up the phone for a demo, we wanted an IP address. The
best way to do this (since you don't know where you are going to give a
demo or the network status when you are at that location) is to set up
an adhoc network on your local machine and then connect to it from the
phone.

## [SavaJe](http://www.savaje.com)

We also have a [SavaJe](http://www.savaje.com) phone.

### Connecting to the phone

The most convenient way to connect to the phone is to use the USB cable.
To get this to work, I needed to update my drivers. I got updated drives
from sftp.savaje.com. Instructions for installing the drives can be
found in the documentation accompanying the phone, and online at [the
SavaJe Wiki](http://java.savaje.com/wiki). In short, you need to:

  - UnZip the drivers to a temporary location
  - Navigate to this location in the command prompt
  - type Setup -ignore_serialnum (This worked for Windows)
  - Follow the onscreen directions, then plug the phone into your
    computer, and (on Windows) the install wizard will take care of the
    rest (It will install 6 or 7 drivers).

Once the drivers have been installed, go to `System->Hardware->Device
Manager->Modems`. Under Modems, you sould see two entries starting with
GSPDA Mobile Device Modem. In my case, the two had the exact same name,
except the second had '\#2' at the end. In any case, right click the
second entry, and select properties. Go to the Advanced tab, and hit
'Advance Port Settings...'. At the bottom of that screen, take note of
the COM Port Number, you'll need it to connect a Hyperterminal to the
phone.

When you first plug your phone into the computer, it should (This is
what happened for us) get treated as a memory stick, or other removable
drive. At this stage, you can place your application on the drive. Once
you want to use these files on the phone, you need to Unmount the memory
card (Using the Stop Hardware button for windows). This is because the
memory card can't be mounted by the phone and the PC at the same time.
If you want to remount the memory card, you need to unplug teh USB
cable, and plug it back in (There should be a better way to do this, but
I haven't figured it out yet).

To read console output (e.g., System.out.println() statements), you need
to start a hyperterminal, and connect it to the phone. On windows, you
can do this by going to `Program
Files->Accesories->Communications->Hyperterminal`. In the 'Connect
Using' drop-down list, choose the COM port that the GSPDA Mobile Device
Modem drivers are using (2 paragraphs up). Choose 115200 bits per
second, 8 data bits, No parity bit, stop bit = 1, and No flow control.

### Writing a Midlet

#### Local Machine Setup

We want to write a simple Midlet for the Jasper S20 phone. First, we
need a suitable development environment to write, and test, the midlet
locally on our machine. To do this, we installed [Eclipse
ME](http://eclipseme.org/), a plugin for Eclipse. As the homepage
states, you can install this plugin really easily by setting a remote
site, with <http://eclipseme.org/updates/> as the URL.

#### Using Eclipse ME

Once you have EcliseME installed, you'll need to set it up. The website
provides good [documentation](http://eclipseme.org/docs/index.html), but
here are some highlights

  - You'll need to set up at least one device configuation. (`Window ->
    Preferences -> J2ME -> Device Management -> Import`)
  - In our case, we got the configuration file from the Midlet Toolkit
    1.0 that came with the developper version of the phone. We chose the
    configuration with the name SavaJePhoneA
  - EclipseME will add options to create a new Midlet Suite, and to
    create a new Midlet. Using the 'New Midlet' command just creates a
    standard java class that extneds the MIDlet class

#### Deploying the Midlet

Once you've written a Midlet to your satisfaction, you may want to test
it on an actual phone. To deploy your midlet, bring up the context menu
on your Midlet project, and select `J2ME -> Create Package`

In your midlet project, there should be a folder called deployed. In
that folder, there should now be two files \[project name\].jad, and
\[project name\].jar. Place these two files in a directory, on your
phone's memory expansion card, called MidletInstall (We placed the two
files in a subdirectory named after the project, but I'm not sure what
effect, if any, this had). Next replace (or remount) the memory card
back in your phone, and open the Application Manager in the Utilities
menu. You should see your Midlet in the list of applications to install.
Press 'Install', and you're ready to go.

### Writing an Xlet

#### Local Machine Setup

There isn't currently an eclipse plugin for creating Xlets, so a little
more manual work is required. First, we got a suitable JRE. We chose to
use Personal Profile 1.1, which we downloaded using instructions from
the [J9 page](http://wiki.eclipse.org/index.php/J9). The only difference
is that in step 7 we chose 1.1, isntead of 1.0. Also take note of the
last part of that article, Foundation 1.1. It applies to Personal
Profile 1.1 as well. Once you install this JRE into eclipse, you'll
probable need to add ppro-extras.jar, and ppro-ui.jar, using `Add
External Jar`. Ppro-ui.jar, in particular contains the Xlet class, which
your main class needs to implement.

#### Using Elcipse

To set up a project in our workspace, we created a standard Java
project, and chose PPRO1.1 as the associated JRE. Our particular project
required Swing classes, so we added SavaJeDevelopper.jar as a library
(This came with the developper version of the phone). When writing an
Xlet your main class should implement the Xlet interface.

#### Deploying the Xlet

To run your Xlet on the phone, create a directory called savaJe/bundles
on the memory expansion card. Inside this directory create a directory
named after your Xlet project. Inside this directory, you'll need a file
called bundle.jnlp, a file called bundle.policy, and a fodler called
lib. Next, export your Xlet project as a JAR file. You will not need to
export the .classpath or .project files, so uncheck those. Name the file
classes, and choose to export it to your lib directory.

#### .policy file

My .policy file looked like this:

    grant codeBase "sb:/VmCheck/lib/classes.jar" {
      permission java.security.AllPermission;
    };

VmCheck is the name of the directory We created in savaJe/bundles, it's
also the directory containing the .policy file. You should modify your
.policy appropriately.

#### .jnlp file

My .jnlp file looked like this:

    <?xml version="1.0" encoding="utf-8" ?>
    <jnlp codebase="sb:///VmCheck/">

      <resources>
        <j2se version="1.4+"/>
        <jar href="lib/classes.jar"/>
      </resources>

      <information>
        <title>VmCheck</title>
        <vendor>Vendor</vendor>
        <description>Hello</description>
      </information>

      <application-desc main-class="org.eclipse.savaje.simplexlet.SimpleXlet">
      </application-desc>

    </jnlp>

  - The <jnlp codebase> tag contains the name of the root directory for
    the Xlet.
  - 'sb://' means that the rest of the URL (/VmCheck/, in the example
    above) could be in /cf0/savaje/bundles (the memory card) or
    /ffs0/savaje/bundles (the flash drive)
  - the <jar href> is a relative url to your exported project, from the
    root directory. It addes that class to your classpath.
  - the main-class in the <application-desc> tag is the name of your
    Xlet class within the JAR file. You can add command line arguments
    in this section using the <argument> tag.

### Running a normal Java application

Normal java files can be run using JNLP, similarly to xlets. That is,
having a bundle.jnlp file, a bundle.policy file, and a folder called lib
with the jar'ed classes you want to run. As far as we can tell, the jar
file that has the entry point to your java application has to be called
classes.jar, and has to be in the lib folder.

You can add classes and jars to the class path using the jar href tag:
<jar href="lib/addon.jar"/> In this example, addon.jar is not located in
the subfolder lib, as you might except. Instead, it is located in
`I:\home\Output\lib` (I:\\ is the drive name for the memory card, on the
phone that would appear as /cf0/home/Output/lib).

System properties can be set using the property tag:
<property name="[property name]" value="[property value]"/>

[Phone Demos](Category:Equinox "wikilink")