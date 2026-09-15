## Starting Eclipse

When you unzip Eclipse, the directory layout looks something like this:

```
   eclipse/
      features/         ''the directory containing Eclipse features''
      plugins/          ''the directory containing Eclipse plugins''
      eclipse.exe       ''platform executable''
      eclipse.ini
      eclipsec.exe              ''(windows only) console executable''
      epl-v10.html      ''the EPL license''
       jre/         ''the JRE to run Eclipse with''
      notice.html
      readme
```

You can start Eclipse by running `eclipse.exe` on Windows or `eclipse`
on other platforms. This small launcher essentially finds and loads the
JVM. On Windows, the eclipsec.exe console executable can be used for
improved command line behavior.

Alternatively, you can launch Eclipse by directly invoking the JVM as
follows:

```
   java -jar eclipse/plugins/org.eclipse.equinox.launcher_1.0.0.v20070606.jar
```

  -
    **NOTES:**
      - The version of org.eclipse.equinox.launcher in the above command
        must match the version actually shipped with Eclipse. For more
        details on launching Eclipse using Java (not eclipse.exe) with
        the launcher, see [Starting Eclipse Commandline With Equinox
        Launcher](Starting_Eclipse_Commandline_With_Equinox_Launcher "wikilink").
      - When running on **Java ≥ 9**, you may have to make some
        non-default system modules available, e.g., by adding
        `--add-modules ALL-SYSTEM` to the command line (please check the
        release notes on supported Java versions per Eclipse version).

## Find the JVM

If a JVM is installed in the `eclipse/jre` directory, Eclipse will use
it; otherwise the launcher will consult the eclipse.ini file and the
system path variable. Eclipse **DOES NOT** consult the `JAVA_HOME`
environment variable.

To explicitly specify a JVM of your choice, you can use the `-vm`
command line argument:

```
   eclipse -vm c:\jre\bin\javaw.exe              ''start Java by executing the specified java executable
   eclipse -vm c:\jre\bin\client\jvm.dll         ''start Java by loading the jvm in the eclipse process
```

See the
[launcher](Equinox_Launcher#Finding_a_VM.2C_Using_JNI_Invocation_or_Executing_Java "wikilink")
page for more details on specifying a JVM.

## eclipse.ini

The **most recommended** way to specify a JVM for Eclipse to run in is
to put startup configuration into the
[`eclipse.ini`](eclipse.ini "wikilink") file in the same folder as the
Eclipse executable (`eclipse.exe` on Windows). The Eclipse program
launcher will read arguments from either the command-line or the
configuration file named [`eclipse.ini`](eclipse.ini "wikilink"). To
specify a JVM using configuration file, include the -vm argument in
[`eclipse.ini`](eclipse.ini "wikilink"), for example:

    -vm
    c:/jre/bin/javaw.exe

Note: there are no quotes around this path as would be required when
executing the same from the command-line were the path to contain white
space, etc. This is a common mistake when using Windows.

Eclipse now will launch without additional arguments in the
command-line, with the JVM specified in the
[`eclipse.ini`](eclipse.ini "wikilink") configuration file.

You should always use `-vm` so you can be sure of what VM you are using.
Installers for other applications sometimes modify the system path
variable, thus changing the VM used to launch Eclipse without your
knowing about it.

''*'The format of the `eclipse.ini` file is very particular; it is
strongly recommended to read
***\[\[eclipse.ini|**''eclipse.ini***\]\]*** and follow the examples
there.''' ''

When Eclipse starts, you are prompted to choose a workspace location on
start-up. This behavior can be configured in the Preferences. You can
manually specify the workspace location on the command line, using the
` -data  `<workspace-path> command-line argument.

## OLD: Starting Eclipse 3.2

In Eclipse 3.2 and earlier, there was an additional file in the root of
Eclipse: startup.jar. This jar file contained the classes needed to
start the platform. In 3.3 and above the equivalent classes are in the
org.eclipse.equinox.launcher bundle.

To start 3.2 by directly invoking the JVM use the following command:

```
    java -cp eclipse/startup.jar org.eclipse.core.launcher.Main
```

Eclipse 3.2 did not contain a console version of the executable.

## OLD: Oracle/Sun VM 1.6.0_21 on Windows

The Eclipse 3.3 - 3.6 launchers for Windows had a problem with the
Oracle/Sun Java VM version '1.6.0_21-b06'.

**UPDATE: Oracle/Sun have released a respin of their JDK/JRE to fix
this**, so the recommended resolution of this problem is to download and
re-install version 1.6.0_21-b07' or higher from <http://www.java.com>
(alternative link is <http://java.sun.com/javase/downloads/index.jsp>).
Make sure you have b07 or higher by running `java -version`.

Before the fix was released, there were three choices to work around
this:

1.  switch back to **'1.6.0_20**' (as of July 19, 2010 it can still be
    downloaded
    [here](http://java.sun.com/javase/downloads/widget/jdk_javafx.jsp))
2.  Change the commandline for launching or add the following line after
    "-vmargs" to your [`Eclipse.ini`](Eclipse.ini "wikilink") file:
    `-XX:MaxPermSize=256m`
    ([Detailed instructions/examples](Eclipse.ini "wikilink"))
3.  For 32-bit Helios, download the fixed
    [eclipse_1308.dll](https://bugs.eclipse.org/bugs/attachment.cgi?id=174640)
    and place it into
    `(eclipse_home)/plugins/org.eclipse.equinox.launcher.win32.win32.x86_1.1.0.v20100503`

The Java bug was closed for voting and comments at
[6969236](http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6969236) on
the Java BugParade because the change has been reverted; the related
Eclipse bug report is open for voting and comments at .

## See Also:

  - [FAQ How do I increase the heap size available to
    Eclipse?](FAQ_How_do_I_increase_the_heap_size_available_to_Eclipse? "wikilink")
  - [FAQ How do I increase the permgen size available to
    Eclipse?](FAQ_How_do_I_increase_the_permgen_size_available_to_Eclipse? "wikilink")
  - [FAQ Who shows the Eclipse splash
    screen?](FAQ_Who_shows_the_Eclipse_splash_screen? "wikilink")
  - [The Eclipse Program
    Launcher](http://www.eclipse.org/swt/launcher.html)
  - [Equinox Wiki
    Category](http://wiki.eclipse.org/index.php/Category:Equinox)
  - Running Eclipse 3.3M5+

:\*[Starting Eclipse Commandline With Equinox
Launcher](Starting_Eclipse_Commandline_With_Equinox_Launcher "wikilink")

:\*[Automated PDE JUnit Testing With Eclipse
3.3M5](Automated_PDE_JUnit_Testing_With_Eclipse_3.3M5 "wikilink")

  - [Preparing for Eclipse in Linux: Installing JRE 1.6.0 (Update x) as
    the Default
    Runtime](http://www.64bitjungle.com/ubuntu/install-java-jre-160-update-x-on-hardy-as-the-default-java-runtime)



-----

<font size="-2">This FAQ was originally published in [Official
Eclipse 3.0 FAQs](http://www.eclipsefaq.org). Copyright 2004, Pearson
Education, Inc. All rights reserved. This text is made available here
under the terms of the [Eclipse Public License
v1.0](http://www.eclipse.org/legal/epl-v10.html).</font>

## User Comments

The -data option does not work if a relative path is specified. If this
is true, please point it out the FAQ above. Thank you.

Specifying -vm "c:\\program files\\..." seems to work for galileo.

A comment on the JVM search order (on Windows) - My testing (Windows 7,
Eclipse 3.4.1) shows that Eclipse also looks for a JVM in the registry,
in my case HKLM\\Software\\Wow6432Node\\JavaSoft\\Java Runtime
Environment. I believe the correct search order would be 1: eclipse/jre
directory, 2: eclipse.ini file, 3: registry, 4: System path variable.
Can anyone else confirm this? If that is the case I think the text above
should be updated.