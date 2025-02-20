---
layout: default
title: wires [bundleID]
summary: Prints information about the wiring of a particular bundle
---

## Description

{{page.summary}}

This command is useful for debugging OSGi applications. Especially when you want to see how Bundle A is wired to other bundles and why. 

## Example

The following shows the wiring of the **Gson** bundle in an application, and you want to see which other bundles are consuming this gson bundle.

- Start your app 
- The bundle for equinox console should be started

```
-runbundles: \
    org.eclipse.equinox.console;version='[1.4.800,1.4.801)',\
```

Example commands on the equinox console. 

```
help

wires - Prints information about the wiring of a particular bundle
   scope: wiring
   parameters:
      long   


lb gson
START LEVEL 1
   ID|State      |Level|Name
   18|Active     |    1|Gson (2.10.1)|2.10.1

g! wires 18
Bundle com.google.gson 2.10.1:
is wired to:
	 - org.eclipse.osgi 3.21.0.v20240717-2103
	   - because of Import-Package: sun.misc; resolution:="optional"
	   - because of Require-Capability: osgi.ee; filter:="(&(osgi.ee=JavaSE)(version=1.7))"
	   - because of Require-Capability: osgi.ee; filter:="(|(&(osgi.ee=JavaSE)(version=1.7))(&(osgi.ee=JavaSE)(version=1.8)))"
and is consumed by:
	 - com.myapp.services 1.0.0
	   - because it Import-Package: com.google.gson; version="[2.10.0,3.0.0)"
	   - because it Import-Package: com.google.gson.reflect; version="[2.10.0,3.0.0)"
	 - com.myapp.foo 1.0.0.SNAPSHOT
	   - because it Import-Package: com.google.gson; version="2.10.1"
	 - com.myapp.bar.uitest.lib 1.0.0
	   - because it Import-Package: com.google.gson
	 - com.myapp.utils.csvconverter 1.0.0.202502201107
	   - because it Import-Package: com.google.gson; version="[2.10.0,3.0.0)"
	 - com.myapp.wrappers.3rdparty.google 1.0.0.SNAPSHOT
	   - because it Import-Package: com.google.gson.stream
	 - com.myapp.wrappers.com.ebay.oauth.client 1.10.0
	   - because it Import-Package: com.google.gson; resolution:="optional"
	   - because it Import-Package: com.google.gson.annotations; resolution:="optional"
	 - com.myapp.wrappers.com.paypal.sdk 2.5.106
	   - because it Import-Package: com.google.gson
	 - stripe-java 22.8.0
	   - because it Import-Package: com.google.gson; version="[2.10.0,3.0.0)"
	   - because it Import-Package: com.google.gson.annotations; version="[2.10.0,3.0.0)"
	   - because it Import-Package: com.google.gson.reflect; version="[2.10.0,3.0.0)"
	   - because it Import-Package: com.google.gson.stream; version="[2.10.0,3.0.0)"
```

You see a list of bundles which consume Bundle `com.google.gson 2.10.1` and also why.



**Related links:**

- [Eclipse Console Shell](https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fconsole_shell.htm) 
- [Maven Central](https://mvnrepository.com/artifact/org.eclipse.platform/org.eclipse.equinox.console)
- [Source code](https://github.com/eclipse-equinox/equinox/blob/ac75ce248e29b36ef8fd9e94e38869de3043907f/bundles/org.eclipse.equinox.console/src/org/eclipse/equinox/console/commands/WireCommand.java#L50)

