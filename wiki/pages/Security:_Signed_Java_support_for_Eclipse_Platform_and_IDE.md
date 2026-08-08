To facilitate the adoption of signed Java content on the Eclipse
platform, it is important to address the lack of signing functionality
outside of the basic ‘jarsigner’ tool packaged with the JRE. The Java
code that makes up Eclipse itself is signed as of the 3.3 release, and
the Eclipse update-manager supports verification of those signatures as
features are installed into the system. Future work direction intends to
add more granular signature policy enforcement, including OSGi loadtime
and eventually at runtime via Java’s fine-grained permission
architecture.

There are several areas that will require additional support to create
and manage signed Java code - this document is an attempt to capture a
draft list:

### Show the signer info for signed content

Anywhere that signed content appears in the IDE or the Platform, it must
be possible to navigate to a viewer that shows the verified signature
information. This includes, but is not limited to:

1.  Compiled classes in Projects
2.  Classes contained in Jar files
3.  Plug-ins installed in the Platform\*\*

<i>\*\*Complete in 3.3, see 'Help-\>About' and select 'Plug-in
details'</i>

### Support a Project running as if signed at runtime

When running a debug scenario, it must be possible to run as if the
classes in the workspace projects are inside signed jarfiles. This
implies that a project will have a well formed META-INF/manifest.mf that
lists signer information, as well as the other files that are required
to comprise a valid jarfile signature. For more information on the
jarfile signature format, see
[here](http://java.sun.com/j2se/1.4.2/docs/guide/jar/jar.html).

### View the signer configuration for a Project

It must be possible to configure a Project such that the contents are
signed when compiled and run, and to have default configuration for
signing when exported to a jarfile. The jarfile format supports multiple
signers, this must be supported - perhaps by pairing sets of resources
to be signed with a KeyStore and alias that will be used to reference a
Key that will be used during the signing operation. Minimally, KeyStores
in the following locations should be supported:

1.  Keystores plugged into the system (i.e.: the `%HOME%\.keystore`
    file)
2.  Specific Keystores in the Project

For more information on Credential Management, including how KeyStores
might be made available to the system, see
[here](http://wiki.eclipse.org/Security:_KeyStore_support_for_Eclipse).

### Invoke the signing operation on Projects

Given a signing configuration, it must be possible to initiate the
creation of the supporting files for a valid signature. A project must
be configurable to invoke signing after a successful build is performed,
or at a specific time via the context menu.

### Invoke the signing operation on Jar files

In addition to signing projects, it may be useful to sign jarfiles that
are located in and referenced by projects in the workspace.

### Support enablement of secure modes in <b>Run..</b> configurations

As support for checking signatures at load-time with OSGi and run-time
with Java’s `SecurityManager` are developed, the IDE must also expose
the capability to enable and configure the mechanisms that enable these
functions. In the case of `SecurityManager`, this will involve setting
the –D option to select an implementation via a checkbox and selector in
the <b>Run…</b> dialog.

[Category:Equinox](Category:Equinox "wikilink") [Signed Java support for
Eclipse Platform and IDE](Category:Security "wikilink")