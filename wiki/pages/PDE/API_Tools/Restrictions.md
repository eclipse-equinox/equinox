Special Javadoc tags are used to annotate code with API information. The
Javadoc tags are intended to replace existing `component.xml` files
(which have to be maintained manually). The tooling defines a fixed set
of restrictions that can be assocaited with an extensible set of javadoc
tags. The restrictions currently supported are:

1.  `@noreference` - Indicates that other bundles must not reference
    this member by name. I.e., the member is internal. This tag is
    intended to be used very rarely when a public class wants to
    restrict access to one of its members, but is not intended for
    general usage. When this tag is applied to a type (class, interface,
    enum or annotation) it acts as though it has been applied to all of
    the members of that type. If the tag is applied to a type it does
    not apply to final fields.
2.  `@noimplement` - Indicates that other bundles must not implement
    this interface. This tag is ignored for all types except for
    interfaces.
3.  `@noextend` - Indicates that other bundles must not extend the class
    or interface it appears on. This tag is ignored for all members that
    are not interfaces or classes.
4.  `@noinstantiate` - Indicates that other bundles must not create
    instances of this class. This tag is ignored for all other types
    that are not classes.
5.  `@nooverride` - Indicates that other bundles must not extend
    (re-implement with a call to the overridden parent) or re-implement
    (with no call to the overridden parent) this method. This tag is
    ignored for all other members except method declarations.

## Defining API Restrictions

API Tools provides Javadoc tags to explicitly document and restrict the
use of API. The following tables summarize the Javadoc tags supported by
each member and the semantics of each tag.

A client refers to a plug-in or bundle that requires the bundle where
the associated API is defined. Restrictions are not applied in the same
bundle where API is defined. For example, a bundle that defines an
interface as `@noimplement` is also allowed to provide an implementation
of that interface.

|                | Class | Interface | Enum | Annotation | Method | Constructor | Field (non-final) |
| -------------- | ----- | --------- | ---- | ---------- | ------ | ----------- | ----------------- |
| @noimplement   |       |           |      |            |        |             |                   |
| @noextend      |       |           |      |            |        |             |                   |
| @noinstantiate |       |           |      |            |        |             |                   |
| @nooverride    |       |           |      |            |        |             |                   |
| @noreference   |       |           |      |            |        |             |                   |

**Tag Restrictions**

When applied to a type or member, each tag has a specific meaning. The
following table describes what each tag means when applied.

<table>
<caption><strong>Tag Semantics</strong></caption>
<thead>
<tr class="header">
<th><p>@noimplement</p></th>
<th><p>Indicates that clients must not implement this interface. Any class using the implements keyword for the associated interface or parent of the associated interface where there is no implementing superclass will be flagged with problem.</p></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>@noextend</p></td>
<td><p>Indicates that clients must not extend this class or interface. Any class or interface using the extends keyword for the associated type will be flagged with a problem.</p></td>
</tr>
<tr class="even">
<td><p>@noinstantiate</p></td>
<td><p>Indicates that clients must not instantiate this class. Any code that instantiates the associated class with any constructor will be flagged with a problem.</p></td>
</tr>
<tr class="odd">
<td><p>@nooverride</p></td>
<td><p>Indicates that clients must not redeclare this method. Any subclass that defines a method that overrides the associated method will be flagged with a problem.</p></td>
</tr>
<tr class="even">
<td><p>@noreference</p></td>
<td><p>Indicates that clients must not reference this type (class, interface, enum, or annotation), method, constructor, or non-final field. Any code that directly invokes the associated method or constructor or references the associated non-final field will be flagged with a problem.</p>
<p>When the tag is used on a type, it behaves as though the tag was added to any of the types members. For example adding the tag to class will flag any references to any methods or non-final fields from that class.</p></td>
</tr>
</tbody>
</table>

## Links

[Ant Tasks](PDE/API_Tools/Tasks "wikilink") - Description of the Ant
Tasks available in API Tools

[API Tooling Architecture](ApiTools_Architecture "wikilink") - High
level description of the tooling's architecture.

[Evolving Java-based APIs](Evolving_Java-based_APIs "wikilink") - What
is considered an API in Eclipse.

[Version Numbering](Version_Numbering "wikilink") - Guidelines on
versioning plug-ins

[PDE/Incubator](PDE/Incubator "wikilink") - New projects that might be
incorporated into PDE in future.

[PDE UI Home Page](http://www.eclipse.org/pde/pde-ui/) - The main PDE UI
web site.

[API Tools](Category:API "wikilink") [API
Tools](Category:Equinox "wikilink") [API Tools](Category:PDE "wikilink")
[API Tools](Category:Eclipse_Project "wikilink")