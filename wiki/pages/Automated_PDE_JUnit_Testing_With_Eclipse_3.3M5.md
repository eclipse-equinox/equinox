## History

After 3.3M4, Modeling builds [no longer ran their JUnit
tests](https://bugs.eclipse.org/bugs/show_bug.cgi?id=170831), due to
changes in the way Eclipse packages its startup jars. The fix is
trivial, but requires a change to
[org.eclipse.test/library.xml](http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.test/library.xml?view=log)
in addition to your local releng code.

`[echo] Running org.eclipse.someproject.tests.AllTests`
`[java] Exception in thread "main" java.lang.NoClassDefFoundError: org/eclipse/core/launcher/Main`

  -
    \- or -

`[echo] Running org.eclipse.emf.test.build.AllSuites`
`[java] Unable to access jarfile `*`{dd}`*`/2.3.0/N200701240200/testing/N200701240200/testing/target/eclipse/startup.jar`

## Migration Guide

  - These changes should apply to anyone using PDE for builds & included
    JUnit tests.

|                                                                                                                                                              |                                                                 |                                  |                                                                                        |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------- | -------------------------------- | -------------------------------------------------------------------------------------- |
| **Filename**                                                                                                                                                 | **Explanation**                                                 | **Before**                       | **After**                                                                              |
| [runtests](http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.mdt/org.eclipse.ocl.releng/builder/tests/scripts/runtests?root=Modeling_Project&view=markup) | switch from startup.jar to org.eclipse.equinox.launcher_\*.jar | `cp="eclipse/startup.jar"`       | `` cp=`find eclipse/ -name "org.eclipse.equinox.launcher_*.jar" \| sort \| head -1` `` |
| [runtests](http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.mdt/org.eclipse.ocl.releng/builder/tests/scripts/runtests?root=Modeling_Project&view=markup) | switch Main classes                                             | `org.eclipse.core.launcher.Main` | `org.eclipse.equinox.launcher.Main`                                                    |

  - These changes may be specific to only [Modeling project
    builds](Modeling_Project_Builds "wikilink"), including [EMFT
    builds](EMFT_Procedures "wikilink").

|                                                                                                                                                                                      |                                                                                                                                                                   |                                                                  |                                        |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------- | -------------------------------------- |
| **Filename**                                                                                                                                                                         | **Explanation**                                                                                                                                                   | **Before**                                                       | **After**                              |
| [relengbuildgtk.sh](http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.mdt/org.eclipse.ocl.releng/builder/tests/configs/local/relengbuildgtk.sh?root=Modeling_Project&view=markup) | don't pass -cp into runtests script to override value set there                                                                                                   | `runtests -os linux -ws gtk -arch x86` `-cp eclipse/startup.jar` | `runtests -os linux -ws gtk -arch x86` |
| [runtests](http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.mdt/org.eclipse.ocl.releng/builder/tests/scripts/runtests?root=Modeling_Project&view=markup)                         | remove -noupdate flag (not [supported](http://help.eclipse.org/help32/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html) anymore) | `org.eclipse.*.launcher.Main` `-noupdate`                        | `org.eclipse.*.launcher.Main`          |

  - These changes apply to you if you use org.eclipse.test (eg., in your
    map file, or if you run org.eclipse.test/library.xml).

`plugin@org.eclipse.test=v20070217,:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse,`

This is fixed by moving up to the latest version (v20070217) of
[org.eclipse.test/library.xml](http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.test/library.xml?view=log),
per [bug 171756](https://bugs.eclipse.org/bugs/show_bug.cgi?id=171756).

## Running Tests

To run tests, you'll need something like this:

`$JAVA_HOME/bin/java $Xflags -enableassertions -cp $cp \`
`  org.eclipse.equinox.launcher.Main -ws $ws -os $os -arch $arch \`
`  -application org.eclipse.ant.core.antRunner -data $workspaceDir \`
`  -file test.xml $antTestTarget \`
`  $Dflags -Dws=$ws -Dos=$os -Darch=$arch -D$installmode=true $J2SE15flags \`
`  $properties -logger org.apache.tools.ant.DefaultLogger`

The variables noted in the above example can be seen in detail in the
[runtests](http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.mdt/org.eclipse.ocl.releng/builder/tests/scripts/runtests?root=Modeling_Project&view=markup)
shell script.

## Links

  - [FAQ How do I run Eclipse?](FAQ_How_do_I_run_Eclipse? "wikilink")
    (including starting eclipse with `java -cp ... org.eclipse....Main`)
  - [Starting Eclipse Commandline With Equinox
    Launcher](Starting_Eclipse_Commandline_With_Equinox_Launcher "wikilink")
    (Ant, Shell, Windows Batch/Cmd)
  - [Eclipse Program Launcher](http://www.eclipse.org/swt/launcher.html)
    (including commandline flags)
  - [Eclipse Runtime
    Options](http://help.eclipse.org/help32/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html)
    (including commandline flags)
  - [Equinox Launcher](Equinox_Launcher "wikilink") (history of changes
    including related Bugzillas)
  - [Eclipse Test
    Framework](http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.test/testframework.html?view=co)

## Bugs

  - <https://bugs.eclipse.org/bugs/show_bug.cgi?id=173742>
    <i>(discussion about startup.jar)</i>
  - ~~<https://bugs.eclipse.org/bugs/show_bug.cgi?id=171756>~~ RESOLVED
  - ~~<https://bugs.eclipse.org/bugs/show_bug.cgi?id=170831>~~ RESOLVED
  - ~~<https://bugs.eclipse.org/bugs/show_bug.cgi?id=62760>~~ RESOLVED

[Category:Equinox](Category:Equinox "wikilink")
[Category:Releng](Category:Releng "wikilink")
[Category:Modeling](Category:Modeling "wikilink")