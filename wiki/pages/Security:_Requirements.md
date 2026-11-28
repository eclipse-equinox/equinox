Document for collecting Security requirements

## Security services (JCA integration)

Requirements surrounding the ability of a plug-in provider to contribute
services which integrate with the core Java Cryptography Architecture
(JCA)

  - Support plugging JCA/JCE classes into the platform dynamically via
    services or extensions
  - No changes required to the core JCA programming model (e.g.: no
    ContextClassLoader trickery)

## Credential management

Requirements for the management of passwords, keys and trusted
certificates.

  - Use Java-standard APIs like KeyStore, CertStore, etc.
  - Support a password management (or perhaps service management) UI for
    storing passwords to CVS, etc.
      - DSDP-TM Usecase:
        [196445](https://bugs.eclipse.org/bugs/show_bug.cgi?id=196445) -
        should be a (potentially backward compatible) replacement of the
        current Eclipse Keyring, but with
          - Better or pluggable encryption
          - UI for entering the master password instead of having it on
            startup commandline
          - Access to the opened password database only for trusted
            modules
  - User interface for managing KeyStores for code signing trust
    (cacerts, user's .keystore, etc)
  - Handle keystore file types (\*.keystore,\*.jks,\*.jceks,\*.p12 etc)
    in project filesystems
  - Plug KeyStore instances into the platform for use during code
    signing (and someday other - e.g.: mail signing) operations
  - Prompt for passwords for KeyStores and their aliases when used

## Code Authentication (Signed bundles)

Eclipse should run with fully signed code and support granular runtime
permission checking using the infrastructure provided by the Java
SecurityManager. In addition to core SecurityManager enablement,
additional methodologies should be supported which trade granular
security with higher performance and simper security administration:

### General requirements for signed bundles

  - Show the signer information/configuration of classes in jars and
    projects
  - Configure a project to be signed after compile using a system
    KeyStore or a project specific KeyStore
  - Manually cause signing to occur from project context menu
  - Ensure that all scenarios support pluggable policy engines

### Specific usage scenario requirements

#### Requirements for signature checking when installing (Update Manager)

  - Persist trust decisions across client restarts
  - User interface for managing 'do not prompt me again' signers

#### Requirements for signature checking when loading bundles (OSGi)

  - User interface for managing permissions granted to bundle signers

#### Requirements for signature checking when executing code at runtime (Java2 Security)

  - Run with a fully-integrated SecurityManager
  - Simple support for launching with a security manager (ie: a checkbox
    in the launch config)
  - Define domain specific Permissions for eclipse concepts (e.g.
    ViewPermission, ActionPermission)
  - Make EMF generated code secure: define model specific permissions
    and use them in the generated code
  - Find a way to run non trusted plug-ins in a sandbox
  - Ability to run a workspace project as if it was a signed and
    packaged jar
  - Scan the Eclipse RCP codebase and ensure that doPrivileged blocks
    are inserted in appropriate places
  - Run a code scan with each build, and post results in the same
    location as JUNIT results
  - User interface for managing permissions granted to bundle signers
  - Extension-registry support security when registering/finding
    extensions. Therefore an extension-point must be able to specify
    permissions for registering and finding extensions

## User Authentication (JAAS integration)

The platform should support Authentication to the platform via a
standardized login infrastructure.

  - Support login to the platform
  - Allow other plugins to hook into login, and provide Principle
    instances to associate with a Subject on login. Would be desireable
    to \*not\* have to provide a new/additional login module, and have
    either an OSGi service (runtime API), and or an extension point that
    allow plugins to create/associate principals with a Subject upon
    login.

[Category:Equinox](Category:Equinox "wikilink")
[Requirements](Category:Security "wikilink")