### Wrap GWT Plug-in Yourself

My team has been interested for some time in exposing web-based
interfaces to some of the capabilities wrapped up in our RCP plugins. At
EclipseCon I attended the tutorial offered by the Google Web Toolkit
(GWT) team and I thought "Gee, wouldn't it be neat if you could put a
GWT-based interface on a servlet running in an Equinox http server?" As
it turns out, it is neat, and here's a rough description of how to do
it:

First, I downloaded the Equinox Jetty server plugins as discussed here:

<http://www.eclipse.org/equinox/server/http_in_equinox.php>

Note that you have to use the Jetty stuff because
org.eclipse.equinox.http doesn't support Servlet 2.4, which appears to
be required by GWT.

Next, I wrapped up gwt-user.jar and gwt-dev-windows.jar in a plugin that
I called com.google.gwt. One critical point here - the jars include
javax.servlet, which is also included with Equinox. These classes
conflict and cause a nasty classloader error. In order to avoid this,
you just have to \*not\* export the javax.servlet packages from this new
GWT plugin. Many thanks to Pascal of the Eclipse platform team for
helping me to track this problem down in between sessions at
EclipseCon\!

Armed with Equinox and GWT, I made an example plugin containing a basic
application that the GWT team presented at EclipseCon, called Chattr. In
that plugin I extended the extension points provided by Equinox to
register both the static html resources and the Chattr servlet, as shown
below:

    <?xml version="1.0" encoding="UTF-8"?>
    <?eclipse version="3.2"?>
    <plugin>
       <extension
             point="org.eclipse.equinox.http.registry.resources">
          <resource
                alias="/"
                base-name="/www"/>
       </extension>
       <extension
             point="org.eclipse.equinox.http.registry.servlets">
          <servlet
                alias="/org.eclipsecon.gwt.chattr.Chattr/chattr"
                class="org.eclipsecon.gwt.chattr.server.ChatServiceImpl"/>
       </extension>

    </plugin>

Finally, since GWT uses reflection to deserialize the classes passed to
and from the servlet, the GWT plugin needs to be able to access the
classes in the example plugin described above. Since the example plugin
already depends on the GWT plugin, we can't create a direct dependency
in the opposite direction. So, I used buddy classloading. In the GWT
plugin, I put this:

    Eclipse-BuddyPolicy: registered

In the example plugin, I put this:

    Eclipse-RegisterBuddy: com.google.gwt

Finally, I created an Equinox OSGI Framework run configuration, added my
example plugin and all required plugins, and started it up. Pointing
Firefox at the OSGI http port shows that everything is working fine. I
confirmed that I could access classes in another plugin from my example
plugin.

Of course, this doesn't allow for the magic of GWT self-hosting, but I
think it's a great start.

### Client side debugging

The GWT toolkit comes with a "hosted browser" to run the applications in
debug mode. This avoids any need to compile the Java code to JavaScript
during the development cylcle.

To run the application using that browser we need to create a new "Java
Application" launch configuration and add gwt-dev-windows.jar to the
classpath. We have to add the "src" folder to the user entries too as
the browser needs access to the source code. The main class to be
selected is the com.google.gwt.dev.GWTShell.

Another thing we need is to copy the nocache.html file from the compiled
version into the www folder. This is required by the hosted browser and
as we are not using the built-in server we need to provide this file by
hand. This file is rarely modified, so we just need to do it once.

As we don't need the built-in web server from the hosted browser, we can
safely add the "-noserver" argument in the launch configuration.

At this point we are ready to run both the server (Equinox) and the
client (GWT hosted browser). It's amazing to debug server and client
side at same time. Enjoy\!

### GWT 1.4

I have followed the instructions here and I ran into a few problems
(when using GWT 1.4 and Equinox). I don't know if these problems are
just specific to GWT 1.4. I posting these here in case others have these
problems too:

  - com.google.gwt needs to depend on javax.servlet. I seemed to get
    that "nasty" class cast exception without it.
  - swt-\*.dll and gwt-ll.dll need to be added to com.google.gwt. It
    seems that the hosted browser uses SWT, so the DLL is needed (I
    guess they use an older version of SWT)
  - I could not find the nocache file anywhere. The instructions say
    "copy it from the compiled version to the www directory", however, I
    thought the www directory was the "compiled" version. Anyways, it
    seems to work without this.
  - Finally, these instructions assume you have compiled your GWT to
    javascript. I created a builder for my project that does this
    automatically for me.

FWIW, there are some class loading errors in GWT 1.4.10 (RC1)
<http://code.google.com/p/google-web-toolkit/issues/detail?id=1138>

You should also try
[this](http://wagenknecht.org/blog/archives/2006/08/yet-another-gwt-plug-in.html)
in case you need an Eclipse plug-in. ;)

\--[gunnar](User:Gunnar.wagenknecht.org "wikilink") 01:02, 4 July 2007
(EDT)

### Bundle Updates

If you try and keep your equinox server running and just do updates to
get new bundles, you will run into an issue. This is because GWT does
some security checks to ensure that classes have follow the proper
inheritance hierarchy, and they use the default class loader to check
this (not the use Equinox uses). To fix this, I added

    Thread.currentThread().setContextClassLoader(MyActivator.getDefault().getClass().getClassLoader());

and put this in the service method of my servlet. When a request comes
in, this ensures that the current thread has the proper class loader
attached.

I'm sure there is a better way to do this, but this works for now.

(Actually, I just realized this is the same issue that Gunnar pointed
out... :) )

\--Ian Bull

Ian, I've opened
[issue 1888](http://code.google.com/p/google-web-toolkit/issues/detail?id=1888)
a while ago to make GWT a better player with OSGi. However, it hasn't
been addressed yet. The thing gets really nasty when it tries to load
the GWT serialization policy and your GWT client side module is in a
different bundle than your server side.

Can you elaborate a bit more on the security checks? What about a stack
trace in an issue report in the GWT issue tracker? I suspect it's more a
caching thing that they are doing for serialization/de-serialization.

\--[gunnar](User:Gunnar.wagenknecht.org "wikilink") 01:36, 7 March 2008
(EST)

## GWT Based Planet Eclipse Demo

There is some GWT example code in the [Equinox Server-side Examples
Community](http://sourceforge.net/projects/sse-examples/) on SourceForge
that was demonstrated at EclipseCon 2007 and 2008. You can find the
slides in [this
presenation](http://sse-examples.cvs.sourceforge.net/*checkout*/sse-examples/net.sourceforge.sse-examples.docs/slides/EclipseCon-2008/EclipseCon-2008-Server-Side-Eclipse-Part2.pdf)
starting at page 33.

For convenience, a Team Project Set is available. After importing it
there should be a launch configuration which you can simple run to start
a minimal Equinox Jetty based HttpService to play with the demo.

### Team Project Set

You can download the [Eclipse Team Project
Set](http://sse-examples.cvs.sourceforge.net/*checkout*/sse-examples/net.sourceforge.sse-examples.projectsets/planeteclipse-gwt-example.psf)
for the GWT based Planet Eclipse viewer example.

### Projects

It created the following projects in your workspace.

  - `com.google.gwt.servlet`
    The GWT code (based on `gwt-servlet.jar`) which goes on the server.
    It contains the fix for GWT issue 1888.

<!-- end list -->

  - `com.google.gwt.user`
    The GWT code (based on `gwt-user.jar`) which you use at development
    time. This bundle should **not** go on the server at runtime.

<!-- end list -->

  - `org.eclipse.planet.service`
    An OSGi service for reading feeds from Planet Eclipse.

<!-- end list -->

  - `org.eclipse.planet.service.tests`
    A dummy implementation of the feed service for demonstrating the
    dynamic capabilities of OSGi and GWT.

<!-- end list -->

  - `org.eclipsecon.planet.gwt.client`
    The GWT modul which gets compiled to JavaScript. The compiled output
    is registered with the OSGi HttpService.

<!-- end list -->

  - `org.eclipsecon.planet.gwt.server`
    The server-side implementation of a GWT RPC service registered as
    Servlets with the OSGi HttpService.

[Category:Equinox](Category:Equinox "wikilink")