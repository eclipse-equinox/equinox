## KeyStore support for Eclipse

**Draft Requirements and High-level Discussion**

On the path to enabling code-signer based security in Eclipse via
enabling the Java SecurityManager, several infrastructure improvements
must be implemented in both the RCP and the IDE. One of the first
enhancements required is the ability to manage the user's keys and
trusted certificates. In Java, the interface that abstracts these
concepts is the `java.security.KeyStore`. This API provides methods for
getting and setting implementations of the `java.security.Key` and
`java.security.Certificate` into an abstract storage repository. This
store can be backed by a flat file (as with the default Java KeyStore -
`"JKS"`), an encrypted file (as with the JCE KeyStore - `"JCEKS"`) or
common file formats like PKCS12. Other options include Smartcards, or
network based repositories. There are several usages for the KeyStore in
Eclipse:

1.  As a repository for Certificates used for trust decisions:
    1.  When installing and loading signed classes from bundles
    2.  When making SSL connections
2.  As a repository for a user's Keys:
    1.  When used for signing Jar files
    2.  When needed for SSL client authentication
    3.  Passwords for connecting to CVS (via PBEKey)
3.  For validating a password for login to the Platform

The method of installing KeyStores into the system is the Java
Cryptography Architecture (JCA), which relies on installing and
accessing implementations of `java.security.Provider` via the
`java.security.Security` class. The Provider class is responsible for
maintaining an internal list of supported algorithms that is supports,
of which KeyStore is one example. The algorithms supported are each of a
specific type, for example `“KeyStore.JKS“` or `“KeyStore.PKCS12”`.
Providers maintain their list of supported algorithm types internally,
and can be registered into the system declaratively in
`%JRE_HOME%\lib\security` by classname like so:

    #
    # List of providers and their preference orders (see above):
    #
    security.provider.1=sun.security.provider.Sun
    security.provider.2=sun.security.rsa.SunRsaSign
    security.provider.3=com.sun.net.ssl.internal.ssl.Provider
    security.provider.4=com.sun.crypto.provider.SunJCE
    security.provider.5=sun.security.jgss.SunProvider

When registered declaratively, the implementation of the Provider class
must be visible to the extension classloader (`%JRE_HOME%\lib\ext`) or
lower (i.e.: on the boot classpath). Providers can also be registered
dynamically, like so:

``` java
import java.security.Security;
...
Security.insertProviderAt( new MyProvider(), 1);
```

See the Java Cryptography Architecture specification for more
information on the JCA.

Obtaining an instance of a KeyStore is done via the static `getInstance`
method on the KeyStore class. The 0-argument `getInstance` method will
return an instance of the type specified by the Security property
`keystore.type`, by calling the providers in order until the first
Provider which supports the type is found. The other `getInstance`
methods allow the caller to specify a specific type or a specific
provider and type.

The KeyStore engine may then require an InputStream from which to load,
potentially with a password which is used to verify the integrity of the
store. There is no contract that a KeyStore requires a password, nor is
there a contract for requiring an InputStream (consider the case of a
Smartcard-based KeyStore which is implicitly available when the
Smartcard is inserted). An example of obtaining and loading a KeyStore:

``` java
KeyStore keyStore = KeyStore.getInstance("JKS");
String fileName = System.getProperty("java.home") +
   "/lib/security/cacerts";
FileInputStream stream = new FileInputStream(new File(fileName));
keyStore.load( stream, "storeit".toCharArray());
```

In the context of Eclipse, the system needs to understand what potential
KeyStore support is available (JKS,JCEKS,PKCS12,etc) from the runtime
JRE. It also needs to be able to manage the System default KeyStore, and
zero or more user KeyStores where users can manage their own trust model
and credentials (by convention in traditional Java applications, this is
a `JKS` at `%USER_HOME%\.keystore`). These KeyStores are used for
determining trust as bundles and code are loaded, and will be used for
storing Keys for use in signing code.

Here is a list of initial actions that are required to support KeyStore
in the system:

### Viewing the available KeyStore providers

As mentioned earlier, the Java security system is preconfigured with a
list of providers available in `%JRE_HOME%\lib\security`, and providers
can be dynamically installed into the system. A user interface is
required for viewing the security configuration of the system for
advanced users (like developers), although it will likely be hidden in a
typical RCP application.

### Configuring a KeyStore provider's extended attributes

There are several additional attributes that are useful for managing
Eclipse’s interaction with KeyStore instances. One example is how to
collect a KeyStore password when it is required. Matching attributes
need to be contributed for each security provider which contributes a
KeyStore type. Here is a list of potential attributes:

1.  Whether a password is required, and how to gather it
2.  Whether an InputStream is required, and how to locate it
3.  Whether the entry passwords are different than the store password,
    and how to gather them
4.  What file extensions are supported by this KeyStore provider (e.g.:
    for decorating files in the IDE, or determining what is supported by
    a File-\>Open operation)
5.  .....

### Viewing the system KeyStore (cacerts)

A typical Java application (plus built in APIs like `JarInputStream`,
and the OSGi `JarVerifier`) rely on trust that is derived from a
Certificate's presence in the system KeyStore, which is located at
`%JRE_HOME%\lib\cacerts`. The `cacerts` file is a standard Java KeyStore
(`"JKS"`) and has a default password of `"storeit"` (without quotes). In
some scenarios, this file will be writeable by the user who is running
the Platform - in other scenarios the file permissions will be such that
the file is read-only. This is a common limitation on systems where the
JRE is shared across multiple applications or users. Here are some
scenarios that must be supported by the Platform:

1.  Viewing the list of trusted Certificates
2.  Viewing a particular Certificate's details
3.  Removing a trusted Certificate from the KeyStore (\*\*if allowed)
4.  Importing a trusted Certificate into the KeyStore (\*\*if allowed)
5.  Changing the modifiable attributes of a Certificate (e.g.: alias)
    (\*\*if allowed)
6.  ....

\*\*Edit will only be allowed if the user has write access to the JRE
\<tt\>cacerts\</tt\> file

### Viewing the user KeyStore (.keystore)

In addition to the system cacerts file, many Java applications use a
Java KeyStore (JKS) in the user’s `%HOME%` directory to store Keys and
Certificates. This may not be the ideal location for an Eclipse
application, and will thus be configurable. Regardless of location, the
following user related scenarios will have to be supported:

1.  Viewing the list of user Keys and trusted Certificates
2.  Viewing a Key or Certificate's details
3.  Generating a new Key or Certificate
4.  Importing a new Key or Certificate
5.  Removing an existing Key or Certificate
6.  Changing the modifiable attributes of a Key or Certificate (e.g.:
    alias, password)
7.  .....

### Changing the system and user KeyStore configurations

As mentioned above, there are cases where the KeyStore may not be a
standard `"JKS"` KeyStore, and another KeyStore implementation may be
appropriate (Smartcards, etc). The system must then allow for the type
and other details of the user KeyStore to be configurable. This may also
be true for the System KeyStore, depending on how much signature and
trust management responsibility is removed from the JRE and implemented
in OSGI and the Eclipse runtime.

[Category:Equinox](Category:Equinox "wikilink") [KeyStore support for
Eclipse](Category:Security "wikilink")