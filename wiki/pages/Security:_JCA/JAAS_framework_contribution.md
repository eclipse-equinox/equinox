## Summary

This document details the IBM Lotus code contribution that contains
enhanced support for the JCA architecture in an Eclipse environment, and
a login framework that is based on Java's login infrastructure - JAAS.
See
[here](http://java.sun.com/j2se/1.4.2/docs/guide/security/CryptoSpec.html)
for more information on JCA, and
[here](http://java.sun.com/javase/6/docs/technotes/guides/security/jaas/JAASRefGuide.html)
for info on JAAS.

## Getting the code

Get the core plugins from CVS head of Eclipse 3.4: `/cvsroot/rt`, under
directory `org.eclipse.equinox/security/bundles`.

**Core plugins:**

  - `org.eclipse.equinox.security.boot.jre15x`
  - `org.eclipse.equinox.security.provider`
  - `org.eclipse.equinox.security.auth`
  - `org.eclipse.equinox.security.ui.default`

Pull the following plug-ins from
`/cvsroot/eclipse/equinox-incubator/security`:

**Test harness:**

  - `org.eclipse.equinox.security.junit`

**Sample application:**

  - `org.eclipse.equinox.security.sample`

## Setting up

The code in `org.eclipse.equinox.security.boot.jre15x` contains code
that proxies JCA & JAAS related provider method calls to implementations
that reside in various plug-ins, and must be on the extension or boot
classpath of the application. Export the project to the filesystem as a
jar and put it explicitly in the default VM arguments of the JRE with
`-Xbootclasspath/a:`<path-to-boot-jar>. Otherwise remember to reference
the Jar explicitly your **Run...** configurations.

  - We intend to integrate this into OSGi and/or the Launcher. Ideas
    welcome, bug here:
    [196988](https://bugs.eclipse.org/bugs/show_bug.cgi?id=196988).

<s>One current issue is that the [Equinox Boot
Delegation](http://wiki.eclipse.org/Equinox_Boot_Delegation) changes
cause trouble because we have some packages that span the boot Jar and
the proxy which it is connected to. We'll fix this ASAP:
[196987](https://bugs.eclipse.org/bugs/show_bug.cgi?id=196987). For now,
set `-Dorg.osgi.framework.bootdelegation=*` in your **Run...** as
well.</s>

## Running the tests

The JUnit tests should ensure that the proxy is set up correctly and
will properly handle requests for LoginModule implementations.

Create a 'JUnit Plug-In Test' **Run...** that specifies the
`org.eclipse.equinox.security.junit.SecurityTestSuite` class in the
`org.eclipse.equinox.security.junit` plug-in. Set it as a 'Headless
Mode' application, and pare the plug-in set down to the minimum required
on the Plug-ins tab. Remember to set
`-Xbootclasspath/a:`<path-to-boot-jar> <s>and
`-Dorg.osgi.framework.bootdelegation=*` in the VM arguments.</s> You
will get NoClassDefFound errors regarding the provider if it is not
setup correctly.

## Running the sample application

There is sample application in the `org.eclipse.equinox.security`
plug-in. It contains a standalone RCP application that uses a
platform-wide JAAS `javax.security.auth.LoginContext` to perform a
login, and then runs the Workbench using the result of that login (a
`javax.security.auth.Subject`). A simple UI then allows inspection of
the Subject.

Create an 'Eclipse Application' **Run...** that specifies the
`org.eclipse.equinox.security.sample.authProduct` product. As with the
tests, remember to set `-Xbootclasspath/a:`<path-to-boot-jar> <s>and
`-Dorg.osgi.framework.bootdelegation=*`</s> in the VM arguments. The
application installs the provider in AuthApplication.java like so:

``` java
Security.setProperty( "login.configuration.provider", "org.eclipse.equinox.security.auth.ConfigurationProvider");
```

The name of the login configuration to use for login is specified in the
plugin_customization.ini file in the data/ subdirectory of the sample
plug-in. By default, the sample uses a built-in configuration named
**'KeyStore**' that will create and authenticate against a `.keystore`
file in the user's workspace. There is also an XML based login
configuration provider provided in the system, and a sample
configuration is available in the jaas_config.xml file in the data/
subdirectory. In AuthApplication.java, this line:

``` java
Security.setProperty( "login.config.url.1", AuthAppPlugin.getDefault().getBundle()
  .getEntry("data/jaas_config.xml").toExternalForm());
```

sets the location of the XML configuration file. Modify the
plugin_customization.ini file to specify **'Win32**' to exercise the
Windows LoginModule referenced in the sample XML configuration - note
that this only works with the Sun JRE for now.

## Comments, criticisms, etc

Comments, criticisms, bugs, requirements, enhancements etc are all
welcome. We'll be watching the equinox-dev mailing lists as well as
BugZilla.

[Matt Flaherty](http://wiki.eclipse.org/User:Mwflaher.us.ibm.com)
[Eric W Li](http://wiki.eclipse.org/User:Eric_w_li.us.ibm.com)

[Category:Equinox](Category:Equinox "wikilink") [JCA/JAAS framework
contribution](Category:Security "wikilink")