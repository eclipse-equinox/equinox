## Execution Environments in API Tools

[Execution Environments](Execution_Environments "wikilink") are a neat
concept within OSGi, however, things can become complicated when
developing against multiple EEs. When you want to contribute code to
Eclipse, chances are that you will be asked to downgrade your code to
the lowest possible execution environment.

For example, suppose that you want to contribute a Twitter client to
Eclipse, or any other open source platform. If you are already
accustomed to the OSGi way of working you would probably want to split
your code into several independent bundles (AKA modules or plug-ins). At
least you would provide a module named org.eclipse.twitter.core and one
called org.eclipse.twitter.ui. To be really useful, it would be the bomb
if the core bundle could run on, say, a watch. For watches, and other
minimal devices there is a special Java environment which is a subset of
Java 1.4 (without regular expressions unfortunately) called Foundation
1.1 or CDC 1.1/Foundation 1.1. There is also a Foundation 1.0 but this
is really stone age and hardly not used. From what I know, Eclipse uses
the 1.1 as a minimum level.

Your org.eclipse.twitter.ui package would normally not require such a
primitive environment, however, if somebody wanted to implement a
twitter client on a watch, he or she could at least use your core to
handle the difficult stuff.

### Specify an Execution environment

[Specify an Execution Environment](image:EEShot1.png "wikilink")

Execution Environments for a specific (plug-in) bundle can be specified
in the manifest editor. It is generally only necessary to specify the
lowest EEs that your bundles require. In the preferences there is also a
section Execution Environments in the Java section. Just type "exec" in
the preference search box to get the indicated page. This merely states
which installed JRE's are able to provide the required Execution
Environment. It is clear that a Java 5 JRE cannot provide a Java 6
Execution Environment. The other way around is possible but that
generally provides more than is required and can lead to confusion.

[Java Execution Environments Preference
Page](image:EEShot2.png "wikilink")

For example, a Java 5 JRE can provide the "split()" method for String
but this method is not available in Foundation 1.1. Even if your
execution environment was set to CDC 1.1/Foundation 1.1 in your manifest
file, this problem would only be detected when it was executed against a
Foundation 1.1 JRE.

### Setup API Tooling

However, we want Eclipse to guard this and warn us when we try to use
illegal (future) methods or classes. In order to set this up, we first
have to get the available execution environments from the Eclipse update
site. In order to do this, go to the preferences and type 'API' in the
search box. Then select the "API Errors/Warnings" page. If you open the
first section "General" and select "Error" for the "Invalid references
..." field, the "Supported Environments" box will be accessible and you
can click on the provided link.

[Enable the Supported Environments dialog](image:EEShot3.png "wikilink")

Clicking this link will open the familiar P2 installer dialog. Select
the proper download site and then download all available Execution
Environments.

[Downloading the Execution Environments](image:EEShot4.png "wikilink")

Make sure to switch on the required Errors and/or Warnings. There are
convenient buttons available to flip all switches at once to the desired
state. Be restrictive if you are serious about your execution
environment.

[Result](image:EEShot5.png "wikilink")

### Activating the Project for API tooling

The last thing to do is to activate the project for API tooling, only
setting the Execution Environment in the Manifest file will not do
anything. Before a project is created, you are able to specify if the
API Tooling must be activated by checking the "Enable API Analysis"
field.

[Activate API Tooling](image:EEShot6.png "wikilink")

This will set a project nature that in turn provides special builder
classes to check for EE JRE incompatibilities (amongst other things.) If
you have forgotten to check this box then you can also do it later.
Right-click your plug-in project and select the "API Tooling Setup"
option from the PDE Menu in the context menu. This will apply the nature
after the project was already created.

[Activate API Tooling](image:EEShot7.png "wikilink")

A dialog box will appear to warn you that an API baseline is not set.
You can ignore this if you only want to use the API tooling to specify
the Execution Environment.

[API Tooling warning](image:EEShot8.png "wikilink")

### Checking your Source

Surely, some of your sources will be showing the red cross. Open a
source. Press CTRL+. (dot) to go to the first error and hoover to find
the cause. This could look something like this:

[API Tooling in Action](image:EEShot9.png "wikilink")

## External Links

[Execution Environments
(EE)](http://eclipsesource.com/blogs/2008/10/08/tip-eclipse-osgi-and-execution-environments/)

[API Tools](Category:API "wikilink") [API
Tools](Category:Equinox "wikilink") [API Tools](Category:PDE "wikilink")
[API Tools](Category:Eclipse_Project "wikilink")