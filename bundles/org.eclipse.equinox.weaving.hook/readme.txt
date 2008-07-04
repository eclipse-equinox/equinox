This version works with Eclipse 3.2 Final only. Checkout projects org.aspectj.osgi,
org.aspectj.osgi.service.weaving and org.eclipse.osgi_3.2.0. The OSGi bundle is 
needed to run the org.aspectj.osgi Framework Extension bundle fragment as a 
source project (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=143696).

To run the accompanying tests checkout all the projects in aspects/tests and
open the WeavingTest.lauch configuration.

To run the demos checkout the projects in aspects/demos.
- HelloWorld: run HelloWorldTest in the demo.hello bundle as a JUnit Plug-in 
test with the config.ini in the org.aspectj.osgi project.
- Tooltip: run "Tooltip Demo.launch" in demo.eclipse.tooltip as an 
Eclipse Application using the config.ini in the same project.
- Tracing: run "Eclipse Tracing Demo.launch" in demo.eclipse.tooltip as an 
Eclipse Application using the config.ini in the same project.