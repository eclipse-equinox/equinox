![Otj.png](Otj.png "Otj.png")

# Why Object Teams?

### Team spirit for your objects

Building complex systems from isolated objects often yields poor
structure which readily decays during system evolution. Objects should
**team-up** in order to co-operate and jointly deliver complex
behaviors. Objects play specific **roles** within a given Team.

### Context based dispatch

**Role instances** are attached as **specializers** to existing objects.
Object behavior is controlled by the currently **active context** that
determines which roles are active at a given point in time. Contexts are
reified into **team instances**, which may further be used to mediate
between roles and maintain state of the collaboration.

### Modules larger than classes

On the road to re-use of modules larger than classes two approaches
compete: **frameworks** and **components**. For many applications white
box frameworks are too fragile and black box components too rigid.
Object Teams provide a middle road which balances **encapsulation** and
**adaptability**.

# OT/J Language Definition (OTJLD)

  - This is the definitive point of reference for OT/J
      - Current version online: <http://www.objectteams.org/def/1.3.1>
      - Frozen release 1.3 as printable document:
        <http://www.objectteams.org/def/OTJLDv1.3-final.pdf>

# First Reading

  - **[OT/J Primer](OTJ_Primer "wikilink")** -- *work in progress*
  - Either of the following
    [publications](http://www.objectteams.org/publications/) can be read
    as an introduction to OT/J:
      - [Object Teams: Improving Modularity for Crosscutting
        Collaborations](http://www.objectteams.org/publications/#NODe02)
        -- (Net.ObjectDays, 2002)
      - [Model-View-Controller and Object Teams: A Perfect Match of
        Paradigms.](http://www.objectteams.org/publications/#AOSD03) --
        (AOSD 2003)
      - [A Precise Model for Contextual Roles: The Programming Language
        ObjectTeams/Java](http://www.objectteams.org/publications/#JAO07)
        -- (Journal Applied Ontology 2007)
  - The **[Examples](:Category:Object_Teams_Examples "wikilink")**
    category shows a few examples ranging from introductory to slightly
    advanced.
  - **[Object Teams
    Patterns](:Category:Object_Teams_Patterns "wikilink")** show OT/J in
    action regarding recurring problems and their solutions.
  - **[Static Homepage](http://www.eclipse.org/objectteams)** which has
    all the resources like downloads etc.

# Implementation

## Compiler

  -
    **Compatibility**
    OT/J is compiled by a modified version of the Eclipse compiler for
    Java. This means that the OT/J compiler can also compile any Java
    program. While OT/J introduces some new keywords most of these words
    are still treated as normal identifiers *until* the keyword
    **`team`** has been parsed ( -- *"scoped keywords"*). This provides
    the greatest possible compatibility at the syntax level.

<!-- end list -->

  -
    **Compiler invocation**
    The compiler can be invoked either from the
    ObjectTeamsDevelopmentTooling (OTDT) or as a [command line compiler
    (ecotj)](http://www.eclipse.org/objectteams/download.php#DL25).

For compiling OT/J source outside Eclipse please see either of:

:\* [JDT
help](http://help.eclipse.org/topic/org.eclipse.jdt.doc.user/tasks/task-using_batch_compiler.htm)
(*yes, ecotj can be used exactly like ecj from
[JDT_Core](JDT_Core "wikilink")*).

:\* [OTHowtos/Compiling With
Ant](OTHowtos/Compiling_With_Ant "wikilink").

:\* [Compiling OT/J with
Maven](http://objectteams.wordpress.com/2011/06/between-the-times/) --
Maven sites:
[parent-pom](http://download.eclipse.org/objectteams/maven/3/sites/objectteams-otdre-parent-pom/usage.html)
([for traditional
OTRE](http://download.eclipse.org/objectteams/maven/3/sites/objectteams-parent-pom/usage.html))
- [example
project](http://download.eclipse.org/objectteams/maven/3/sites/objectteams-compile-test/usage.html)

  -
    **Compiler output**
    The compiler produces **regular Java `.class` files**, which are
    enriched with OT/J specific meta data using Java bytecode
    attributes. These meta data are interpreted by the OTRE (see below)
    in order to weave `playedBy` and `callin` bindings into base
    classes. This implies that OT/J programs need to be launched with
    the OTRE enabled. Other than that any recent JVM (≥1.5) can be used.

## Object Teams Runtime Environment (OTRE)

As mentioned, running an OT/J application requires the OTRE. Currently
two alternative modes are supported how the OTRE can be **linked** into
an OT/J application:

  - **JPLIS**:

<!-- end list -->

  -
    This technology is part of the Java standard since version 1.5.
    Launching an application in JPLIS mode only requires a few items
    added to the classpath and an `-javaagent:...` argument, which means
    a non-invasive way of launching OT/J programs like plain Java
    programs.

<!-- end list -->

  - **[OT/Equinox](:Category:OTEquinox "wikilink")**:

<!-- end list -->

  -
    By integration of OT/J with the
    [Equinox](:Category:Equinox "wikilink") component framework, also
    Eclipse plug-ins (RCP, OSGi bundles) can leverage OT/J. In this case
    running OT/J code only requires to have the OT/Equinox feature
    installed on top of Equinox.

Within the OTDT the OTRE is enabled by a new checkbox adjacent to the
JRE configuration (see the [user
guide](http://help.eclipse.org/topic/org.eclipse.objectteams.otdt.doc/guide/running.html)).

### Weaving into system classes

  -
    In contrast to an earlier architecture in JPLIS mode the OTRE can
    weave into more classes, i.e., even system classes from Java's
    `rt.jar` can potentially be bound as base classes ''(note, that any
    classes loaded *before* the OTRE starts operation can still not be
    woven).''

### Launching from the command line

  -
    For running OT/J applications outside Eclipse please see
    [OTHowtos/Running From
    Commandline](OTHowtos/Running_From_Commandline "wikilink").

### Build-time weaving

  -
    While load-time weaving has the advantage of easier deployment - no
    tweaking of the classpath is required to ensure that the right
    version of a class is loaded - it may be difficult to integrate in
    some application containers. As a fallback mechanism, the OT weaver
    can be used to process all affected classes already at build-time.
    The resulting woven classes can then be executed on any JVM (≥1.5)
    without using the load-time weaver.
      - [OTHowtos/BuildTime
        Weaver](OTHowtos/BuildTime_Weaver "wikilink")

[Category:Object Teams](Category:Object_Teams "wikilink")