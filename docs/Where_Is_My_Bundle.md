Where Is My Bundle
==================

Sometimes when you start Eclipse, you don't see your bundle and you wonder why. This page will provide a short description of steps that you can take to debug this problem.

Contents
--------

*   [1 Steps To Get More Information](#Steps-To-Get-More-Information)
*   [2 Bundle States](#Bundle-States)
*   [3 OSGi Can't See My Bundle](#OSGi-Can.27t-See-My-Bundle)
    *   [3.1 Eclipse v3.4 and later: p2](#Eclipse-v3.4-and-later:-p2)
    *   [3.2 Eclipse v3.3 and Earlier: OSGi - config.ini](#Eclipse-v3.3-and-Earlier:-OSGi---config.ini)
        *   [3.2.1 Update - platform.xml](#Update---platform.xml)

### Steps To Get More Information

*   **Get a console.** You'll need to run your app such that the system output can be seen. It's usually easiest to run your application from the command-line. On Windows, launch the eclipsec.exe executable if present (Eclipse 3.3 or later), or adjust your eclipse.ini to specify javaw.exe as your VM.
    *   **Windows: Modify console buffer.** Change the properties of the DOS console to show a lot of lines, we're going to have a lot of output here and we don't want things cut off.
*   **Launch with the OSGi console.** Run with the -console and -noexit command-line parameters. Add -consoleLog for good measure to ensure that anything printed to the log file is also printed to the console.
    *   Your Eclipse or app needs to include the org.eclipse.equinox.console bundle and, if 3.8 or later, also include all of the org.apache.gogo.* bundles too
*   **Get the system status.** Now you should have a console with a `osgi>` prompt. Here you can type **`ss`** to get the status of all the bundles. You can also use a portion of a Bundle-SymbolicName with the **`ss`** command. For example, **`ss org.eclipse.core`** will get all bundles with a Bundle-SymbolicName starting with **org.eclipse.core**.
*   **Look for your bundle.** Is your bundle in this list? If not, then OSGi doesn't know about it.
*   **Get the bundle status.** Note the number on the far left column... this is the unique id that OSGi has assigned to your bundle. Write this down, you'll need it later. Also note what state your bundle is in. It should be one of `INSTALLED`, `RESOLVED`, `ACTIVE`, or `<<lazy>>`.
*   **Get more bundle information.** If you type **`bundle 123`** (where _123_ is the bundle id for your bundle), then you will get lots of information about your bundle including which other bundles it is wired to because of its dependencies. You can also use the Bundle-SymbolicName, for example, if you type **`bundle my.symbolic.name`** (where _my.symbolic.name_ is the bundle symbolic name for your bundle), then you will get the same information.
*   **Why is a bundle not RESOLVED?** Use `diag 123` to get an explanation as to why a bundle hasn't been resolved.

### Bundle States

*   Not in the list - If your bundle isn't in the list, then OSGi doesn't know anything about it. See the next section for debugging this problem.
*   **`INSTALLED`** \- This means that OSGi knows about your bundle but there is something wrong and it couldn't resolve. Problems may include thing from missing dependencies to requiring a higher Java VM version than you are running with. To get more information on why your bundle is not resolved try running the **diag <bundle id>** command, where <bundle id> is your bundle id or bundle symbolic name.
*   **`RESOLVED`** \- If your bundle is resolved but you expected it to be started, then try starting your bundle from the command-line with the **`start 123`** command. If it won't start, then you should get some feedback as to what the problem is.
*   **`<<lazy>>`** \- This means your bundle is resolved and is marked to be lazy started. Everything should be ok.
*   **`ACTIVE`** \- your bundle is resolved and has been started, everything should be working as planned.

  

### OSGi Can't See My Bundle

If your bundle isn't in the list when you do an `ss` command on the OSGi console, then there are a couple of steps to take to diagnose the problem.

#### Eclipse v3.4 and later: p2

Eclipse 3.4 saw the introduction use a new update mechanism called [p2](https://help.eclipse.org/helios/topic/org.eclipse.platform.doc.isv/guide/p2_overview.htm) that has completely replaced the older Update Manager. p2 can follow dependencies to install pre-requisites, use mirrors for faster downloads, and garbage collect unneeded bundles. The `plugins` and `features` directories are now managed exclusively by p2, and humans should never add, remove, or modify any of the contents in these directories. p2 does support simple unzipping installations through the [dropins](https://help.eclipse.org/helios/topic/org.eclipse.platform.doc.isv/reference/misc/p2_dropins_format.html) directory, though this should be avoided if at all possible.

See [the docs](https://help.eclipse.org/helios/topic/org.eclipse.platform.doc.user/tasks/tasks-129.htm) for high-level details about using the p2 UI within the Eclipse workbench. p2 also provides support for publishing, installing, updating, and removing software through the [p2 publisher](https://help.eclipse.org/helios/topic/org.eclipse.platform.doc.isv/guide/p2_metadata.html) and [p2 director](https://help.eclipse.org/helios/topic/org.eclipse.platform.doc.isv/guide/p2_director.html).

Note that the dropins directory is only checked on startup and its content is treated as being _optional_; that is, a bundle that cannot be loaded is silently ignored. There is an [IBM service document](http://www-01.ibm.com/support/docview.wss?uid=swg21428463) that provides some strategies for debugging plugin resolution problems in dropins.

#### Eclipse v3.3 and Earlier: OSGi - config.ini

**[Note: these instructions are for Eclipse 3.3 and earlier: those versions which are managed by the Classic Update Manager.](/index.php?title=Note:_these_instructions_are_for_Eclipse_3.3_and_earlier:_those_versions_which_are_managed_by_the_Classic_Update_Manager.&action=edit&redlink=1 "Note: these instructions are for Eclipse 3.3 and earlier: those versions which are managed by the Classic Update Manager. (page does not exist)")**

In your Eclipse configuration area (by default a `configuration/` folder in the install directory) there is a file called `config.ini`. This is a Java properties file which contains properties which are read by OSGi on startup. The important one for you right now is the **`osgi.bundles`** property. This property lists the bundles that OSGi will see on startup and also sets their start level and tells OSGi whether or not to start them right away. By default, Eclipse will have 3 bundles on the list.

        osgi.bundles=org.eclipse.equinox.common@2:start,\
        org.eclipse.update.configurator@3:start,\
        org.eclipse.core.runtime@start

The `org.eclipse.equinox.common` bundle contains utilities and common classes used by other bundles.

The `org.eclipse.core.runtime` bundle contains the application extension point and will help OSGi figure out which application to run. (for instance, the Eclipse SDK)

The `org.eclipse.update.configurator` bundle actually does the work in discovering what other bundles you have, so we don't have to list everything on this list.

You should have at least these 3 bundles in your list.

##### Update - platform.xml

In your Eclipse configuration directory in the `org.eclipse.update` folder, there should be a file named `platform.xml`. This is the file that the Update Configurator uses to determine the rest of the bundles that are known to your system.

The file is organized into _sites_. You should see a site with the location of `platform:/base/` which basically means "everything from the `plugins/` folder in my Eclipse install directory". If you have created application extension locations via the update manager UI or if you have a `links/` folder then you will have a site entry for each one you have created.

If your bundle exists in an enabled site that is in the `platform.xml` file, then everything should be ok. Note that the sites can list either features or plug-ins so you may need to know which feature your plug-in belongs to, in order to figure out if it should be known.

