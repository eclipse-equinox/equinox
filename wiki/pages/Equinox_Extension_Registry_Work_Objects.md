**WORK IN PROGRESS – SUBJECT TO CHANGE**

Ver.2

## Changes from Ver.1

After going through the feedback for the original proposal and some code
prototyping, the Ver.1 proposal is transformed into the Tier II of this
proposal. Schemas are removed from the picture and class type
information is specified in \#getObjects(). The mapping overrides are to
be supported with Java annotations; only exact name matches will be
supported on pre-1.5 VMs. The original proposal can be found at
[1](http://wiki.eclipse.org/Equinox_Extension_Registry_Work_Objects1).

## Proposed solution

After reviewing extension registry usage patterns and the feedback for
the original proposal, it seems that there are three common patterns on
how the registry information is used:

  - (A) typed objects are created based on the configuration elements,

<!-- end list -->

  - (B) portions of the configuration elements trees are transformed
    into hash maps, or

<!-- end list -->

  - (C) values from attributes are retrieved and combined depending on
    location in the configuration elements tree

To make those scenarios easier we can:

  - (C) present an extension registry as a tree supporting a subset of
    XPath expression to retrieve values; provide overrides for get()
    method for primitive Java types (Tier I)

<!-- end list -->

  - (A) provide methods to adapt nodes on the registry tree into a Java
    objects of a consumer-specified type (Tier II)

<!-- end list -->

  - (B) provide methods to adapt nodes on the registry tree into a hash
    maps constructed based on the registry nodes and their relative
    positions (Tier III)

The new methods are going to be be a part of the new IRegistryNode
interface. There will be adaptors allowing switch from IExtensionPoint /
IExtension / IConfigurationElement and a factory to get IRegistryNode
element corresponding to a given registry at the given path.

As you'll see some of the proposed functionality makes extension
registry look like a read-only preferences.

### Tier I: Registry as a tree. Getting typed values, navigation

#### Group 1. Getting typed values

Registry elements would get a set of typed method to retrieve attribute
values, something like:

``` java
interface IRegistryNode {
    ....
    boolean getBoolean(String path, boolean default)
    ....
}
```

The "path" could be:

`   "name" - attribute name for the current configuration element`
`   " element_name:attributeName" - attribute with a given name under the configuration element`
`   "..:attributeName" - value of parent's attribute`

The method overrides will be provided for:

  - int, long
  - float, double
  - boolean

Two versions of the method will be provided for override - one that
returns one value and one that returns an array:

``` java
interface IRegistryNode {
    ....
    int getInt(path, int default);
    int[] getIntValues(path);
    ....
}
```

The path's exact syntax will be fleshed out as we go, but it need to
support hierarchical navigation (absolute and relative), multiplicity
qualifiers, and attribute names. It is aprobably going to assume a
format similar to a subset of XPath:

`   [/extension_point_ID/][/extensionID/]ConfigurationElement_name1/.../ConfigurationElement_nameN:attributeName`

For multiplicity qualifiers, the only supported qualifier at this time
will be the "\[1\]" to indicate that the first matching element is
selected.

In addition, convenience methods will be added to get a resource URL
based on the location of the contributing bundle:

``` java
interface IRegistryNode {
    ....
    URL getResource(nodePath);
    URL getResources(nodePath);
    ....
}
```

(the nodePath would be expected to point to an attribute(s) containing
relative resource path).

#### Group 2. Navigating registry nodes

The following methods will be added to the IRegistryNode:

``` java
public interface IRegistryNode {
    ....
    public IRegistryNode parent();

    public IRegistryNode[] children();

    public String[] attributes();

    public IRegistryNode node(String path);

    public IRegistryNode[] nodes(String path);

    public boolean nodeExists(String path);

    public String name();

    public String absolutePath();
    ....
}
```

Most methods are self-explanatory. For the initial version the \#node()
won't be creating new nodes, but rather only returning existing nodes or
null of no such node was found.

Unlike Preferences, some nodes will have no names and some nodes will
have multiple children with the same name. Nodes with no names will
match "\*" in XPath expressions.

### Tier II. Adapting registry nodes into user-specified objects

Two methods will be provided to adapt a given node into a user-specified
object (or objects, or hash map).

``` java
public interface IRegistryNode {
    ....
    Object toObject(Object[] context, Map classNames);

    Object[] toObjectArray(String path, Object[] context, Map classNames);
    ....
}
```

The classNames map is passed in as an argument and maps element names to
the java.lang.Class objects to be created for those elements.

The new object will be specified using the constructor with the best fit
for the "Object\[\] context" arguments. Best fit will be defined as the
constructor with the largest number of arguments whose type is
assignable from the context\[i\]. The order of the context\[\] will be
preserved as much as possible; arguments not specified will be passed in
as null.

After the object is constructed, it will be injected with values
specified in the XML file. First method injection will be tried with
method name "set" + attribute_name; then field injection will be tried
for the field "attribute_name".

If multiple sub-elements are present in the XML, the "set" method will
be called multiple times.

Expansion possibility: for Java 1.5 classes will be able to override
mapping of attributes and elements to method names and field names using
annotations.

If the created object implements IInitializable\#init(), the init()
method will be called to give the object an opportunity to finalize its
initialization.

If the created object implements IDisposable\#deleted(), then deleted()
method will be called when corresponding registry element is removed.

*TBD: clarify caching of the created objects. Should they be cached by
the caller or will the registry cache them? Soft references vs. getting
the same object vs. memory consumption.*

### Tier III. Hash maps

In Tier II methods are added to create an object or array of objects
corresponding to a registry node. Sometimes such objects are used to
construct hash maps. We can add an extra method to perform this work:

``` java
public interface IRegistryNode {
    ....
    public Map toMap(String keyPath, String realtiveValuePath, Object[] context, Map classNames);
    ....
}
```

The method will use keyPath to find key elements for the hash map, then
will find corresponding nodes based on the relativeValuePath, will
construct objects for those values, and will add (key, value) pairs in
the hash map.

## Why don't we just use EMF

Several people asked about using EMF in this work. Let's consider where
we could use EMF: external view of the extension registry (APIs) and
internal implementation.

  - External view: I don't think it would be right for us to push EMF on
    our consumers by returning EObject-derivatives. Consumers might not
    be familiar with EMF, and \#toObject() methods by design create
    objects of a user-specified classes which are not limited to
    EObjects.

<!-- end list -->

  - Internal implementation: EMF could be used to implement
    IRegistryNode tree. However, it is likely that we'll want to use
    binary format for cache persistence, not XML (both for the CPU
    performance and for the memory footprint). We also very likely to
    use different notification approach - either via OSGi's EventAdmin
    or by providing our own event bus in equinox.common.

## Open questions

  - Event Notifications

For objects created using \#toObject() it is probably their containers
that would be interested in events which makes IDisposable less than
ideal. On the other hand, people who use only Tier I methods still going
to need to have registry event notifications.

It might be a good time to see if we can get away from the listeners and
start relying on an event bus, or, maybe on OSGi's EventAdmin.

The great deal of the difficulty in dealing with events in the current
extension registry comes from the events being asynchronous. As a
result, listeners have to deal with a strange data state where some data
corresponds to the moment when event was fired, and some data is
current. I think we need to reconsider if we really want registry events
to remain asynchronous. (Registry events are not generated during usual
everyday work, but rather when a bundle is installed or removed.) If we
decide to support both synchronous and asynchronous notifications, we
should consider sending the path to modified nodes rather then
IRegistryNode objects.

  - Permissions

Do we need to add some sort of "permission to modify my extension"?

**WORK IN PROGRESS – SUBJECT TO CHANGE**

### Links

1.  [Inversion of
    Control](http://www.martinfowler.com/articles/injection.html):
    "Inversion of Control Containers and the Dependency Injection
    pattern" by Martin Fowler
2.  [Bug 248340](https://bugs.eclipse.org/bugs/show_bug.cgi?id=248340):
    Improve usability of the extension registry
3.  [Bug 221603](https://bugs.eclipse.org/bugs/show_bug.cgi?id=221603):
    Provide a public, reusable RegistryReader

[Top: All Extension Registry Work
Items](Equinox_Extension_Registry_Work "wikilink")

[Category:Equinox](Category:Equinox "wikilink")