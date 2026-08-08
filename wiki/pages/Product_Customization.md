See [bug 154099](https://bugs.eclipse.org/bugs/show_bug.cgi?id=154099)
for the plan item that corresponds to this page. The problems discussed
here can be thought of largely as transformation problems and wherever
possible a solution along those lines is proposed.

# Problem: Extension Grooming

It is often the case that RCP developers or even product packagers would
like to include specific extensions from plug-ins but are are unable (or
unwilling) to include all of the extensions. There are various reasons
for this, such as: context menu bloat, gratuitous (and harmful) startup
extensions, collisions in identifier namespaces (two "Navigator" views),
or any number of other issues that make elegant integration impossible.
In the RCP scenario there are similar concerns such as including only a
subset of views (ie: Outline view but not Problems), including the
workspace components without the IDE baggage, including Java model
support without any (or a minimal subset) of the user interface. Because
there are so many ways in which developers may want to censor or augment
the extensions contributed by plug-ins a general solution may be
applicable.

## Solution 1: XSLT

One such solution is introducing into the extension registry the ability
to apply XSLT stylesheets to plugin.xml files prior to their being
parsed.

Advantages:

  - the sky is the limit - the possibilities for modification are
    virtually endless
  - XSLT is a reasonably well-known and understood technology

Disadvantages:

  - some scenarios would be prohibitively difficult to achieve using
    XSLT alone. For instance, changing externalized strings would
    require not only the XSLT transformation but also fragments
    containing new properties files in which the new strings would
    reside
  - God Mode - the potential for mistake (and abuse) is high and the
    difficulty in tracing and reporting problems is very high

There are several ways in which this support could be implemented.

### Implementation 1: System Properties

The simplest way would be to look for particular system properties as a
bundle is read by the extension registry. The system property could be
globally applicable or somehow qualified by the bundle identifier. If
found, the value of this property would be resolved into a location on
the file system that contains a stylesheet. If found, the sheet is
applied to the bundle.

Advantages:

  - simplicity - the implementation for this is trivial
  - easy to experiment with - removing this support should it prove
    unfeasible would be easy

Disadvantages:

  - no tooling support. Adding the system properties would be a manual
    affair that would somehow need to be added to the product launch.
    Takes the customization out of the product itself and makes the
    successful product launch dependent on launch setup (which isn't
    necessarily unreasonable)

### Implementation 2 : Manifest Directives

When loading a non-fragment bundle check for the presence of fragments
who's manifests contain a XSLT header. If present, that header is parsed
and the resulting transformation is applied to the host bundle (and
optionally all fragments).

Advantages:

  - conceptually similar to a patch

Disadvantages:

  - easy for 3rd party plug-in authors to subvert other vendors plug-ins
    by providing a fragment in their namespace
  - possibility for multiple fragments to each provide XSLT transforms.
    Which one wins?
  - not able to supply global transforms in this way without some
    expensive searching - ie: finding all bundles with a global XSLT
    manifest directive

### Implementation 3: Transformation Service

Create an (optional) OSGi service that is consulted by the registry that
is capable of providing transformations for plug-in.xml.

Advantages:

  - concept possibly extendable to other problems
  - possibly has the best tooling support. Concrete Java APIs to follow
    and extend

Disadvantages:

  - heavyweight and (compared to other options) time consuming to
    implement

See the Generalized OSGi Transformations section below for a possible
solution.

## Example Stylesheets

### Suppressing Startup Extensions

Too many unnecessary startup extensions. Remove them all.

` `<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
`   `<xsl:template match="extension[@point='org.eclipse.ui.startup']">
`   `</xsl:template>
`   `<xsl:template match="node()|@*">
`       `<xsl:copy>
`           `<xsl:apply-templates select="node()|@*"/>
`       `</xsl:copy>
`   `</xsl:template>
` `</xsl:stylesheet>

### Suppressing An Action

The convert line delimiters action appears on the File menu in the
Eclipse SDK. This (irrationally or not) upsets a lot of people. The
following stylesheet would remove the action from the menu.

` `<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
`   `<xsl:template match="actionSet[@id='org.eclipse.ui.edit.text.actionSet.convertLineDelimitersTo']">
`   `</xsl:template>
`   `<xsl:template match="node()|@*">
`       `<xsl:copy>
`           `<xsl:apply-templates select="node()|@*"/>
`       `</xsl:copy>
`   `</xsl:template>
` `</xsl:stylesheet>

### Suppressing Views

An application is being built on top of the IDE component that isn't
properly an IDE. As such, most of the views do not apply with the
exception of the Task view. Suppress all non-Task views.

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
`    `<xsl:template match="view">
`       `<xsl:if test="@id='org.eclipse.ui.views.TaskList'">
`           `<xsl:copy>
`                   `<xsl:apply-templates select="node()|@*"/>
`               `</xsl:copy>
`       `</xsl:if>
`    `</xsl:template>
`    `<xsl:template match="node()|@*">
`       `<xsl:copy>
`           `<xsl:apply-templates select="node()|@*"/>
`       `</xsl:copy>
`    `</xsl:template>
</xsl:stylesheet>

### Reconciling Keybindings

There are some issues with certain keybindings not having the
appropriate sequences. For instance, the Show View menu bindings are
either specified as M2+M3+Q or M1+M3+Q. These could be reconciled with
templates similar to the following which fixes the "Other..." command
only. This is accomplished via the not(parameter) predicate which will
only match key elements that do not have parameter children.

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
`    `<xsl:template match="key[@commandId='org.eclipse.ui.views.showView' and not(parameter)]">
`        `<key>
`            `<xsl:copy-of select="@*[not(name()='sequence')]"/>
`            `<xsl:attribute name="sequence">`M1+M3+Q Q`</xsl:attribute>
`        `</key>
`    `</xsl:template>
`    `<xsl:template match="node()|@*">
`        `<xsl:copy>
`            `<xsl:apply-templates select="node()|@*"/>
`        `</xsl:copy>
`    `</xsl:template>
</xsl:stylesheet>

### Action Set Default Visibility

There are too many action sets visible by default. Disable some.

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
`    `<xsl:template match="actionSet[@id='org.eclipse.ui.WorkingSetActionSet']">
`        `<actionSet>
`            `<xsl:copy-of select="node()|@*[not(name()='visible')]" />
`            `<xsl:attribute name="visible">`false`</xsl:attribute>
`        `</actionSet>
`    `</xsl:template>
`    `<xsl:template match="node()|@*">
`        `<xsl:copy>
`            `<xsl:apply-templates select="node()|@*"/>
`        `</xsl:copy>
`    `</xsl:template>
</xsl:stylesheet>

### Moving Menus

To conserve space move the Compare With/Replace With menus under the
Team menu.

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
`    `<xsl:template match="menu[@id='replaceWithMenu' or @id='compareWithMenu']">
`       `<xsl:if test="@id='replaceWithMenu'">` `
`           `<xsl:copy-of select="following-sibling::menu[@id='team.main']" />
`       `</xsl:if>
`       `

<menu>

`           `<xsl:attribute name="path">`team.main/`<xsl:value-of select="@path"/></xsl:attribute>
`           `<xsl:copy-of select="node()|@*[not(name()='path')]" />
`       `

</menu>

`    `</xsl:template>
`    `<xsl:template match="menu[@id='team.main']" />`    `

`   `<xsl:template match="action[starts-with(@menubarPath, 'replaceWithMenu') or starts-with(@menubarPath, 'compareWithMenu')]">
`       `<action>
`           `<xsl:attribute name="menubarPath">`team.main/`<xsl:value-of select="@menubarPath"/></xsl:attribute>
`           `<xsl:copy-of select="node()|@*[not(name()='menubarPath')]" />
`       `</action>
`   `</xsl:template>

`    `<xsl:template match="node()|@*">
`        `<xsl:copy>
`            `<xsl:apply-templates select="node()|@*"/>
`        `</xsl:copy>
`    `</xsl:template>
</xsl:stylesheet>

This style sheet needs to be applied to all plug-ins that add
contributions to the Compare With/Replace With menus.

### Renaming Views

A product has two seperate and disjoint notions of what a "package" is
so the Java Package Explorer is misleading and confusing. Rename it.

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
`    `<xsl:template match="view[@id='org.eclipse.jdt.ui.PackageExplorer']">
`       `<view>
`           `<xsl:attribute name="name">`Java Explorer`</xsl:attribute>
`           `<xsl:copy-of select="node()|@*[not(name()='name')]" />
`       `</view>
`    `</xsl:template>
`    `<xsl:template match="node()|@*">
`        `<xsl:copy>
`            `<xsl:apply-templates select="node()|@*"/>
`        `</xsl:copy>
`    `</xsl:template>
</xsl:stylesheet>

Note that this solution is not ideal as the name is hardcoded and not
externalized. Unfortunately, it's not possible for us to supply this
string in an externalized form. It would be nice if we could supply it
in a fragment and have the ManifestLocation code be able to pick it up
from there but unfortunately it does not work that way. If a string is
not available in a plugin.properties file it is not searched for in
fragment properties files. The location solution could possibly be
solved by having a table of strings present in the XSLT file and somehow
choosing the correct string in the template.

# Problem: Manifest Updates

It's possible that an application wishes to alter the manifest headers
of its various component bundles. It might be something as trivial as
renaming the bundles (for whatever unwholesome reason) or as interesting
as changing the version range of required bundles (when they were
specified in error or too conservatively).

See the Generalized OSGi Transformations section below for a possible
solution.

# Problem: Class File Updates

It's possible that an application wishes to reuse certain code but is
unable to do so due to critical bugs. Given the source code the
application provider is able to fix these bugs but is
hesitant/unable/unwilling to repackage the problematic bundle directly.
It would be nice to be able to patch the class libraries of bundles with
updated versions of classes to address such scenarios.

See the Generalized OSGi Transformations section below for a possible
solution. The above problems could be addressed by a generalized OSGi
transformation service. It may be possible to implement such a beast in
terms of a BundleFileWrapperFactoryHook. Here is a brief outline for how
this could be implemented.

# Generalized OSGi Transformations

## Solution 1: Simple Wrapper/Smart Transformer

We create a BundleFileWrapperFactoryHook that creates a transformative
BundleFile. In the bundle file get\* methods we attempt to access
services of a particular type (the transformative type). If present, we
serially ask these transformative services if they wish to modify the
last state of the file (starting with the content in the base file). If
they do, we transform the content and push it to the next transformer
(if any) otherwise we return it to the caller. In this scenario the
transformer services are responsible for determining if they can modify
the content or not.

Advantages:

  - simple to implement
  - everyone loves a pipeline

Disadvantages:

  - we introduce complexity into the transformers. They themselves may
    need to implement some kind of extension mechanism in order to
    handle all of the cases they need to handle. Ie: an XSLT transformer
    needs to know what kind of files to work on and what stylesheets to
    apply.

## Solution 2: Smart Wrapper/Simple Transformer

As above, we create a BundleFileWrapperFactoryHook that wrappers bundle
content. When a BundleFile is requested we inspect the bundle and its
fragments and search for some token file that contains transformation
instructions. If such a file doesn't exist but we have some globally
applicable file we use that instead. We wrapper the bundle file with a
transformer primed by these instructions. These instructions would be a
series of triples : file path or expression, transformer class name, and
transformer data. When an entry is requested on this transformed bundle
file we inspect these instructions and if there's a match we invoke the
specified transformer with the file to be transformed as well as the
specified data.

Advantages:

  - easier to implement transformative services

Disadvantages:

  - less flexibility

## Solution 3: Baseclasses That Make Transforms Easy

Instead of implementing a new architecture on top of the
BundleFileWrapperFactoryHook to handle transforms we instead create an
BundleFileWrapperFactoryHook baseclass that makes doing transforms easy.
In addition to the simple baseclass utilities for handling CSV parsing,
extensibility by service, and stream piping can be provided.

Advantages:

  - easy to implement
  - flexible
  - no duplication of wrapering already found in the
    BundleFileWrapperFactoryHook infrastructure

Disadvantages:

  - not as easy for transformer developers to use

This solution has been implemented and now resides in the Equinox
Incubator
[1](http://dev.eclipse.org/viewcvs/index.cgi/equinox-incubator/) in the
projects that start with org.eclipse.equinox.transform. Please see
[Equinox Transforms](Equinox_Transforms "wikilink") for more
information.

[Category:Equinox](Category:Equinox "wikilink")