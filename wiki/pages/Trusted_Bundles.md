## High level overview

The following is a stakeholder needs and high level design document for
'Trusted Bundles' - security related functionality being developed for
the Eclipse 3.4 release. We will be building upon the 3.3 work of
applying signatures to the bundles that make up the Eclipse platform.

![01_value_identification.jpg](images/01_value_identification.jpg
"01_value_identification.jpg")
![02_process_definition.jpg](images/02_process_definition.jpg
"02_process_definition.jpg")
![03_system_architecture.jpg](images/03_system_architecture.jpg
"03_system_architecture.jpg")
![04_component_architecture.jpg](images/04_component_architecture.jpg
"04_component_architecture.jpg")

## Stakeholders and needs

The exercise of defining a direction for security functionality must
begin with the identification of these stakeholder and an evaluation of
their needs.

### Stakeholder identification

There are several identifiable classes of stakeholders when considering
the needs of the Eclipse ecosystem. A list of the identifiable
stakeholders follows:

  - **End-users** - People who are users of Eclipse platform based
    technologies. There are at least two classes:
      - **Enterprise** - End-users who are using an Eclipse platform
        application in a large enterprise deployment context
      - **Standalone** - End-users who are using a unzip-and-run Eclipse
        installation
  - **Administrators** - People tasked with managing a multi-user
    installation of an Eclipse platform based application
  - **Developers** - People who code bundles for the Eclipse RCP
  - **Community** - The general Eclipse community, including the
    Foundation and partner companies

### Needs evaluation

Across these stakeholders, there are the following identifiable needs:

  - **End-users**
      - Increased security from potentially malicious active content
        packaged as OSGi bundles
      - Security from subverted or misdirected administration (E)
      - Accommodation of a potentially low level of security competency
      - Usability and simplicity in setting and maintaining a secure
        configuration
      - Acceptable performance while using the Eclipse platform with
        security enabled

<!-- end list -->

  - **Administrators**
      - Increased security from potentially malicious active content
        packaged as OSGi bundles
      - Dynamicity of administration to deal with active threats
      - Simplicity of administration for a deployment wide configuration
      - Extensibility to contribute alternative implementations of
        security services

<!-- end list -->

  - **Developers**
      - Consistency with existing Java deployment packaging formats
      - Compatibility with existing and upcoming Java and OSGi security
        technologies
      - Extensibility to contribute alternate implementations of
        security services

<!-- end list -->

  - **Community**
      - A solid reputation as a platform which has security in mind for
        its users and member companies

## System intent

Given these defined stakeholders and needs, the primary intent of the
solution can be defined as:

`To increase the security of the Eclipse platform`

This addresses the most fundamental needs of the most important
stakeholders. The beneficiaries of the solution will be Eclipse users,
administrators and community members – such as member companies and the
foundation itself. The operand – which is the object of the value
related change – is the Eclipse platform itself, and the value related
attribute is security. In this case the transformation will be
increasing.

See the [system intent
diagram](images/01_value_identification.jpg
"01_value_identification.jpg")
which details stakeholders, needs and defines the system intent.

## System goal

With the system intent established as increasing the security of the
platform, a specific goal which will meet that intent must be defined.
If malicious content is defined as the avenue to decreasing security,
then restricting that malicious content is a way to increase security.
The overall system goal is thus defined as:

`by restricting active content`

The specific process of restricting breaks down into several constituent
processes:

  - <u>Identifying</u> active content as it is installed into the system
  - <u>Enforcing</u> the system policy given identified content
  - <u>Alerting</u> the user when the system encounters unknown content
  - <u>Managing</u> the configuration of the system with respect to
    known content
  - <u>Extending</u> the system with additional mechanisms for
    identification and enforcement

From our needs statements, we also have the following <b><u>subsidiary
goals</u></b> that must be addressed by the solution:

  - Achieving simplicity and usability in user interaction
  - Maintaining consistency and compatibility with Java and OSGi
    standards
  - Achieving acceptable performance
  - Providing sufficient extensibility mechanisms

See the [system goal
diagram](images/02_process_definition.jpg
"02_process_definition.jpg") which
specializes the intent into a specific system goal.

## System concept

The proposed concept takes advantage of the existing functionality of
OSGi with respect to signed bundle content and extends it with a model
that will enforce constraints on the integrity of the content and the
identity of the content signer as bundles are loaded into the system.
Specifically, the concept:

`using a bundle-signer based security system at load time`

The concept uses the cryptographic formats used by Jar file format for
verifying the integrity of signed bundles. Authenticating the signers
will be enabled through standard chains of X.509 certificates. This
standard signature and certificate format will be joined with the
lifecycle of the OSGi runtime, allowing the ability to enable or disable
bundles based on the content integrity and signer authenticity.

An evaluation of the concept with respect to secondary needs is required
to assess the appropriateness of the solution:

  - <b>Usability and simplicity</b>
      - Concept of 'signers' and 'trust' is familiar from secured
        browser connections (SSL) and signed email (S/MIME)
      - Level of granularity is appropriately broad at the bundle level,
        instead of at the level of classes and resources
      - Not as complex as full Java2 enablement scenario, for all
        classes of stakeholders (users, developers and administrators)
      - Alert widget and enable/disable flow could support “safe
        staging” style of user interaction:
        <http://www.andrewpatrick.ca/CHI2003/HCISEC/hcisec-workshop-whitten.pdf>

<!-- end list -->

  - <b>Consistency and compatibility</b>
      - Uses standard Jar file format, including Jar signatures:
        <http://java.sun.com/j2se/1.4.2/docs/guide/jar/jar.html#Signed%20JAR%20File>
      - Does not preclude Java2 permission enablement, subsystems
        required for this solution are a subset of those required to
        enable Java2 permission-based security
      - Potentially simplifies Java2 permission story: Trust,
        expiration, usage, etc can be dealt with early in bundle
        lifecycle

<!-- end list -->

  - <b>Performance</b>
      - Not yet measured, potential impact of cryptographic operations
        can be mitigated by sensible and secure caching

<!-- end list -->

  - <b>Extensibility</b>
      - Can augment trust with additional engines (centralized,
        replicated, hardware-based, etc.)
      - Can replace policy with more robust implementation (supporting
        key-usage, whitelists/blacklists, revocation, etc)

See the ![system concept diagram](images/03_system_architecture.jpg
"03_system_architecture.jpg") which
describes the specific process and decomposed it into constituent
processes.

## Level 1 decomposition

The <b><u>authentication subsystem</u></b> is responsible for
<u>identifying</u> content; specifically it performs the function of
<u>verifying</u> signed content, and <u>authenticating</u> trusted
signers. The subsystem is responsible for processing bundles as they are
loaded from the filesystem into the OSGI runtime. There are two engines
implemented by the authentication subsystem, the verification engine and
the authentication engine. The verification engine exposes an API for
inspecting the details of a piece of signed content. The verification
engine uses the authentication engine to establish the authenticity of a
signer by verifying the certificate chain presented alongside the signed
content.

The <b><u>authorization subsystem</u></b> is responsible for
<u>enforcing</u> a policy based on a piece of signed content.
Specifically, it does this by <u>authorizing</u> the bundle to load
based on its authentication status and a defined policy. If the bundle
is not authorized, then it is placed into the disabled state as
supported by Equinox. In this state, the bundle will not be activated,
nor will it contribute services or Eclipse extensions into the system.
If it is authorized, then the bundle is allowed to load as normal.

The <b><u>alerts subsystem</u></b> is responsible for <u>alerting</u>
the user when unknown or potentially malicious content is encountered.
It monitors when bundles are disabled by the authorization subsystem and
supports <u>notifying</u> the user via a notification widget on the
status line. When there are no problems in the system, a green shield
will be displayed, meaning that the system is running in compliance with
the defined authorization policy. If a bundle is disabled due to an
authorization failure, the widget will throb a yellow or red shield,
depending on the severity of the situation. The user clicks on this
widget to invoke the management subsystem, where they can deal with the
situation in a manner consistent with the concept of “safe staging”.

The <b><u>management subsystem</u></b> is responsible for
<u>managing</u> the configuration of the system. The particular
functions that it supports include <u>setting</u> the launch parameters
required to enable the system, <u>adding</u> and <u>removing</u> trusted
signers to the installed trust engines, and <u>editing</u> the
parameters of the current authorization policy.

The <b><u>extension mechanisms</u></b> support deployers and developers
in <u>extending</u> the system with additional and alternate
implementations of certain services. Specifically, the system allows for
the installation of additional trust engines for authorizing content,
for the installation and specification of alternate policy engines, and
for the contribution of alternate user interfaces for managing
enablement, trust and policy.

## Level 2 subsystem decompositions

<i>Coming soon</i>

[Category:Equinox](Category:Equinox "wikilink") [Trusted
Bundles](Category:Security "wikilink")