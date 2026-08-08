Launching Eclipse with the new Equinox jar is as easy as
<b style="color:darkgreen">A</b>, <b style="color:orange">B</b>,
<b style="color:darkred">C</b>.

Note that the initial releases of Eclipse 3.3 on Windows have a
configuration bug preventing the following approach from working. See
[this mail
thread](http://dev.eclipse.org/mhonarc/lists/equinox-dev/msg03416.html)
for details.

### <b style="color:darkgreen">A</b>nt Script

``` xml
  <!-- set path to eclipse folder. If local folder, use '.'; otherwise, use c:\path\to\eclipse or /path/to/eclipse/ -->
  <property name="eclipse.home" value="."/>

  <nowiki><!--  get path to equinox jar inside ${eclipse.home} folder (copy/rename actual jar) --></nowiki>
  <copy tofile="''${eclipse.home}''/eclipse/plugins/org.eclipse.equinox.launcher.jar">
    <fileset dir="''${eclipse.home}''/eclipse/plugins"
      includes="**/org.eclipse.equinox.launcher_*.jar"/>
  </copy>

  <nowiki><!-- start Eclipse w/ java --></nowiki>
  <java classpath="''${eclipse.home}''/eclipse/plugins/org.eclipse.equinox.launcher.jar"
  .../>
```

Or, if you are using Ant 1.7 and don't like copying resources around (or
don't have permission to do so) this appears to work to set the path to
the newest available equinox launcher jar in a property for later use:

``` xml
  <!-- set path to eclipse folder. If local folder, use '.'; otherwise, use c:\path\to\eclipse or /path/to/eclipse/ -->
  <property name="eclipse.home" value="."/>

  <!-- store path to newest launcher JAR in path id 'newest.equinox.launcher.path.id' -->
  <path id="newest.equinox.launcher.path.id">
    <first count="1">
      <sort>
        <fileset dir="${eclipse.home}/eclipse/plugins" includes="**/org.eclipse.equinox.launcher_*.jar"/>

        <nowiki><!-- Seems the default order is oldest > newest so we must reverse it.
            The 'reverse' and 'date' comparators are in the internal antlib
            org.apache.tools.ant.types.resources.comparators.
         --></nowiki>
        <reverse ns="antlib:org.apache.tools.ant.types.resources.comparators">
          <nowiki><!-- 'date' inherits 'reverse's namespace --></nowiki>
          <date/>
        </reverse>
      </sort>
    </first>
  </path>

  <!-- turn the path into a property -->
  <property name="equinox.launcher.jar.location" refid="newest.equinox.launcher.path.id" />

  <!-- you can now reference the jar through the property ${equinox.launcher.jar.location} -->
  <echo message="Using equinox launcher jar: ${equinox.launcher.jar.location}" />
```

### <b style="color:orange">B</b>ash Shell Script

` #!/bin/bash`

` # set path to eclipse folder. If the same folder as this script, use the default; otherwise, use /path/to/eclipse/`
`` eclipsehome=`dirname $BASH_SOURCE`;``

` # get path to equinox jar inside $eclipsehome folder`
` cp=$(find `*`$eclipsehome`*` -name "org.eclipse.equinox.launcher_*.jar" | sort | tail -1);`

` # start Eclipse w/ java`
` /opt/java50/bin/java -cp $cp org.eclipse.equinox.launcher.Main ...`

### <b style="color:darkred">C</b>md/Bat Script

Save this as eclipse.cmd. This has been tested with Windows XP Pro (SP2)
as well as Windows 8.1 Pro.

` @echo off`

` :: set path to eclipse folder. If local folder, use '.'; otherwise, use c:\path\to\eclipse`
` set ECLIPSEHOME=.`

` :: get path to equinox jar inside ECLIPSEHOME folder`
` for /f "delims= tokens=1" %%c in ('dir /B /S /OD "%ECLIPSEHOME%\plugins\org.eclipse.equinox.launcher_*.jar"') do set EQUINOXJAR=%%c`

` :: start Eclipse w/ java`
` echo Using %EQUINOXJAR% to start up Eclipse...`
` java -jar "%EQUINOXJAR%" ...`

### Variations

  - If you're looking to run JUnit tests headlessly, see [Headless JUnit
    Testing](Starting_Eclipse_Commandline_With_Equinox_Launcher/Headless_JUnit_Testing "wikilink").

<!-- end list -->

  - If you're looking to the Equinox p2 Admin UI, see [p2 Admin
    UI](Starting_Eclipse_Commandline_With_Equinox_Launcher/p2_Admin_UI "wikilink").

[Category:Releng](Category:Releng "wikilink")
[Launcher](Category:Equinox "wikilink")
[Category:Java](Category:Java "wikilink")
[Category:Launcher](Category:Launcher "wikilink")