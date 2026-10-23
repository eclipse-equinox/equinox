If you're looking to run JUnit tests headlessly, here's a [bash
script](http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.gef/org.eclipse.gef.releng/builder/tests/configs/local/relengbuildgtk.sh?annotate=1.17&root=Tools_Project)
that does that as part of a headless PDE build. Relevant part starts at
line 191:

`# different ways to get the launcher and Main class`
`if [[-f_eclipse/startup.jar|-f eclipse/startup.jar]]; then`
`  cpAndMain="eclipse/startup.jar org.eclipse.core.launcher.Main"; # up to M4_33`
`elif [[-f_eclipse/plugins/org.eclipse.equinox.launcher.jar|-f eclipse/plugins/org.eclipse.equinox.launcher.jar]]; then`
`  cpAndMain="eclipse/plugins/org.eclipse.equinox.launcher.jar org.eclipse.equinox.launcher.Main"; # M5_33`
`else`
``  cpAndMain=`find eclipse/ -name "org.eclipse.equinox.launcher_*.jar" | sort | head -1`" org.eclipse.equinox.launcher.Main";``
`fi`

`# run tests`
``echo "[runtests] [`date +%H\:%M\:%S`] Launching Eclipse (installmode = $installmode with -enableassertions turned on) ..."``
`execCmd "$JAVA_HOME/bin/java $Xflags -enableassertions -cp $cpAndMain -ws $ws -os $os -arch $arch \`
`-application org.eclipse.ant.core.antRunner -data $workspaceDir -file test.xml $antTestTarget \`
`$Dflags -Dws=$ws -Dos=$os -Darch=$arch -D$installmode=true $J2SE15flags \`
`$properties -logger org.apache.tools.ant.DefaultLogger" $consolelog;`
``echo "[runtests] [`date +%H\:%M\:%S`] Eclipse test run completed. "``

[Category:Releng](Category:Releng "wikilink")
[Launcher](Category:Equinox "wikilink")
[Category:Java](Category:Java "wikilink")
[Category:Launcher](Category:Launcher "wikilink")