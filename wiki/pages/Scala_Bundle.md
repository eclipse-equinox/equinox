## Overview

A quote from the scala website states:

[Scala](http://www.scala-lang.org/) is a general purpose programming
language designed to express common programming patterns in a concise,
elegant, and type-safe way. It smoothly integrates features of
object-oriented and functional languages. It is also fully interoperable
with Java.

So it seems to be able to interoperate with Java, but can it
interoperate with OSGi? The answer seems to be yes. This document will
demostrate a simple hello world scala bundle. Much of this is already
documented on Neil Bartlett's blog at [An OSGi Bundle… built in
Scala](http://neilbartlett.name/blog/2007/04/06/an-osgi-bundle-built-in-scala/).
The point of this document is to quickly get a hello world scala bundle
running in a self-hosting PDE workspace.

## The Scala plugin

There is a [Scala Eclipse
plugin](http://www.scala-lang.org/downloads/eclipse/index.html)
available for developing scala code. First install this plugin into
Eclipse. The web site indicates that only 3.2 is supported but it works
fine on the latest 3.3 builds. This will install the necessary tools for
compiling scala code in your workspace. But in order to develop bundles
with scala code you need to make the scala library available for bundles
to depend on. The scala library is packaged as a bundle but it is
embedded in the ch.epfl.lamp.sdt.compiler plugin. In order to make the
scala library available stand alone you must copy the
lib/scala-library.jar file from the ch.epfl.lamp.sdt.compiler plugin
into the eclipse/plugins directory of your development target. This will
allow bundles in your workspace to use the scala library without
requiring the complete scala development environment.

## A Scala bundle

Unfortunately PDE and Scala projects to not work together out of box. A
little .project file hacking is needed in order to compile scala code in
a plugin project. Use the following steps to create a simple plugin
project for Scala code.

### Create a Plugin project for Scala code

1.  Create a Plugin project. New-\>Project-\>Plug-in Project.
2.  Name the project hello.scala
3.  In the Target Platform select "an OSGi framework". Click Next and
    finish
4.  Edit the .project file of the hello.scala project. Make the
    <buildSpec> and <natures> sections look like the following:

<buildSpec>
`  `<buildCommand>
`    `<name>`org.eclipse.jdt.core.javabuilder`</name>
`    `<arguments>
`    `</arguments>
`  `</buildCommand>
`  `<buildCommand>
`    `<name>`org.eclipse.pde.ManifestBuilder`</name>
`    `<arguments>
`    `</arguments>
`  `</buildCommand>
`  `<buildCommand>
`    `<name>`org.eclipse.pde.SchemaBuilder`</name>
`    `<arguments>
`    `</arguments>
`  `</buildCommand>
`  `<buildCommand>
`    `<name>`ch.epfl.lamp.sdt.core.scalabuilder`</name>
`    `<arguments>
`    `</arguments>
`  `</buildCommand>
</buildSpec>
<natures>
`  `<nature>`ch.epfl.lamp.sdt.core.scalanature`</nature>
`  `<nature>`org.eclipse.pde.PluginNature`</nature>
`  `<nature>`org.eclipse.jdt.core.javanature`</nature>
</natures>

This will hook in the scala nature and scala builder for building scala
code in your workspace.

### Add a dependency on the scala library

Scala code gets compiled into class files which will depend on the scala
library at runtime. A bundle which contains compiled scala code will
require the scala library. For simplicity the easiest thing to so is to
use Reqiure-Bundle to gain runtime access to the scala library. Add the
following bundle manifest header to the hello.scala META-INF/MANIFEST.MF
file:

`Require-Bundle: scala_library`

### Create a scala bundle activator

1.  Create a scala class in the hello.scala package.
    New-\>Other-\>Scala-\>Scala Class.
2.  Name the scala class ScalaActivatory. Click Finish.
3.  Make the class extend org.osgi.framework.BundleActivator and
    implement the start and stop method.

`package hello.scala;`

`import org.osgi.framework._`

`class ScalaActivator extends BundleActivator{`
`  def start(context: BundleContext) {`
`   Console.printf("Hello from the {0} bundle.\n",`
`                    context.getBundle().getSymbolicName());`
`  }`

`  def stop(context: BundleContext) {`
`  }`
`}`

After you have created the ScalaActivator you will have to add a
Bundle-Activator header to your bundle manifest to use the
ScalaActivator:

`Bundle-Activator: hello.scala.ScalaActivator`

### Run the scala bundle on Equinox

1.  Create a new OSGi Framework launch configuration. Run-\>Open Run
    Dialog-\>OSGi Framework-\>New
2.  On the Bundles click Deselect All.
3.  Select the hello.scala bundle from the Workspace
4.  Click Add Required Bundles.
5.  Click Run.

## Build the scala bundle jar

So far this example has run the scala bundle from your workspace in a
typical eclipse self-hosting environment. If you try to export the
plugin project from your workspace then you will notice the .scala files
are not compiled into .class file. This is because PDE Build only knows
how to compile Java code by default. It may be possible to plug the
scala compiler into some custom callbacks script in PDE Build ...

[Category:Equinox](Category:Equinox "wikilink")