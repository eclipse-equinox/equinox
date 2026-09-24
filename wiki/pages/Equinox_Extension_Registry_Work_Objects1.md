# **OLD VERSION - please see [Equinox Extension Registry Work](Equinox_Extension_Registry_Work "wikilink") for the current design**

## Problem

When developers use extension registry, the most common first step is to
translate registry artifacts (IExtension, IConfigurationElement) into
user-specific classes. Even in a most trivial case that results in a few
lines of extra code. The "proper" implementation that takes into account
multiplicity, dynamic registry events, and error handling takes a bit of
knowledge and effort.

As a result we have to deal with:

  - Extra code that has to be written by each developer for parsing and
    walking extension registry;
  - Inconsistent treatment of extension information not conforming to
    the schema;
  - Problems in synchronizing user model and registry.

## Proposed solution

Let's assume that extension point schemas can specify Java classes
corresponding to schema elements. When such information can be used to
construct Java classes and inject them with the values specified in the
plugin.xml files.

## Details

In order to construct user-specific objects, extension registry will use
constructor injection to propagate context to the objects and setter
injection to propagate arguments from the plugin.xml.

### Consumer-facing APIs

**IExtensionRegistry** will get a new method

``` java
    Object[] getObjects(String path, String scope, Object context, Class ofClass)
```

where

  - path: ConfEelementName1\[/.../ ConfEelementName\] \[@attrName\]
  - scope: registry/extensionPointID\[/extensionID\]
  - context: a Java object to be passed to the constructor, may be null
  - ofClass: expected Java class of the result

The path will be constructed with XPath syntax in mind (although it is
unlikely that full XPath will be needed). The scope will be constructed
with the view of potential merge of the extension registry and
preferences into one mechanism It is likely that multiple variations of
this method will be provided to account for optional arguments,
multiplicity, and typing of the result.

**IExtension** will get a new method

``` java
    Object[] getObjects(Object context, Class ofType)
```

The extension registry will cache the objects for the duration of the
session. Consumers should not cache the objects but rather feel free to
ask the extension registry whenever necessary.

### Provider-facing APIs

A new interface will be added:

``` java
public interface IRegistryObject {
    public boolean init();
    public void dispose();
}
```

On objects implementing this interface, the init() method will be called
as a final step of the object creation (after all injections are
processed). The displose() method will be called to indicate that the
corresponding extension has been removed from the framework.

#### Extension point schema: class

The extension point schema will be used to pass additional information.
Elements can have the Java class specified:

``` xml
<element name ="myElement">
    <class name = "org.abc.MyClass"/>
</element>
```

The Java class specified in this way would have to have either

  - a default no-argument constructor (if objects created from the
    extensions are context-free), or
  - a single argument constructor with the type corresponding to the
    context (if objects are to be created in a context)

The "context" here is any Java object that can be passed to the
extension registry at the time it is prompted for extension information.

*TBD consider an optional attribute
multiplicity="singletonAll|singletonInContext|...".*

#### Default setters

The generated classes will use pre-defined method and field injection.

When an attribute named "abc" is found in the XML, the user class will
be polled first for the method "setABC(abc.Class)" when for the field
named "abc". (The method name will be case-insensitive).

Similar processing will be employed for the contained elements. If a
sequence of elements is described in the XML, the "set…" method will be
called multiple times.

#### Extension point schema: special setters

Several special arguments can be injected using explicit setter
instructions: extension ID, extension name, extension contributor.

``` xml
<element name ="myElement">
    <class name = "org.abc.MyClass"/>
    <set method="setID" special="extension:id"/>
    <set field="extensionName" special="extension: name"/>
</element>
```

The supported values are:

  - "extension:id",
  - "extension:name",
  - "extension:contributor".

### TBD Sateless objects vs. objects with state

``` xml
<element name ="myElement">
    <class name = "org.abc.MyClass" [stateAware="true|false"]/>
</element>
```

The user-objects will be cached as soft references. As such they might
be purged from the memory and re-constructed. This works fine for
stateless objects. We need to investigate if there is a need for
state-aware objects and, if so, add a way to specify this in the schema.

This would address state-aware objects for the duration of the session.
Is there a need to persist such objects between sessions? ISerializable?

### TBD typed values in the extension point schema

How about creating setter overrides specific to primitive types, arrays,
and so on? It might save some more code (for instance, developer would
not have to convert from String to int) at the expense of added
complexity of the APIs.

### TBD Implementation: on top of the current registry or independent?

The first inclination is to do the implementation on top of the existing
extension registry. However, this creates an obvious duplication in
memory usage and some extra processing.

It might be worth while to investigate creation of an independent
mechanism based on the contents of the plugin.xml. Such mechanism would
avoid creation of registry artifacts altogether and only expose
user-specific objects.

Also, for such "separate" solution an optional Java-1.5+ processing can
be added where injection is done completely via constructors. (Java 1.5
gives annotations that make possible to match constructor arguments to
the attributes specified in plugin.xml.) (Splitting existing registry
into 1.4 and 1.5 parts won't be practical.)

Also, for such "separate" implementation explore if EMF helps with
creating a model from XML.

My inclination would be to go with the "current registry + objects on
top" for 3.5 stream as it is way more practical. Having this
implementation in the 3.5 stream would allow us to get feedback earlier.
If it goes well, we can use what we learn from 3.5 to create a
"separate" implementation for 4.0 and, potentially, relegate current
implementation to the compatibility layer.

*Prerequisite for this item:* get a clear understanding on caching of
user objects and modified registry.

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