# Equinox Extension Registry

## Overview

The Equinox Extension Registry bundle (`org.eclipse.equinox.registry`) provides the Eclipse Extension Registry, a powerful mechanism for discovering and managing plugins and their extensions. The registry enables loose coupling between components by allowing them to contribute functionality through declarative extension points.

## What is the Extension Registry?

The Extension Registry is a central catalog that maintains information about:
- **Extension Points**: Named locations where components can contribute functionality
- **Extensions**: Contributions made to extension points
- **Configuration Elements**: Structured data describing extensions

This mechanism allows applications to be extended without modifying existing code, following the Open-Closed Principle.

## Key Features

- **Dynamic extension management**: Add, remove, and modify extensions at runtime
- **Declarative contributions**: Define extensions through XML (`plugin.xml`) or programmatically
- **Namespace support**: Organize extensions by plugin/bundle
- **Change notifications**: Listen for registry changes
- **Adapter support**: Adapt objects to required interfaces
- **Persistence**: Save and restore registry state
- **Standalone operation**: Can be used without full OSGi runtime

## Core API

### Main Interfaces

- **`IExtensionRegistry`**: Main entry point for querying the registry
- **`IExtensionPoint`**: Represents a location where extensions can be added
- **`IExtension`**: Represents a contribution to an extension point
- **`IConfigurationElement`**: Structured data within an extension
- **`IRegistryChangeListener`**: Listen for registry changes
- **`IRegistryEventListener`**: Enhanced listener for registry events

### Getting Started

Access the extension registry through the OSGi service registry:

```java
BundleContext context = ...; // your bundle context
ServiceReference<IExtensionRegistry> ref = context.getServiceReference(IExtensionRegistry.class);
IExtensionRegistry registry = context.getService(ref);
```

Or in a non-OSGi environment:
```java
IExtensionRegistry registry = RegistryFactory.getRegistry();
```

## Usage Examples

### Defining an Extension Point

In your `plugin.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?eclipse version="3.4"?>
<plugin>
   <extension-point id="myExtensionPoint" 
                    name="My Extension Point" 
                    schema="schema/myExtensionPoint.exsd"/>
</plugin>
```

### Contributing an Extension

In another bundle's `plugin.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?eclipse version="3.4"?>
<plugin>
   <extension point="com.example.myExtensionPoint">
      <provider class="com.example.impl.MyProviderImpl">
         <property name="priority" value="10"/>
      </provider>
   </extension>
</plugin>
```

### Querying Extensions

```java
// Get all extensions for a specific extension point
IExtensionPoint point = registry.getExtensionPoint("com.example.myExtensionPoint");
IExtension[] extensions = point.getExtensions();

// Iterate through configuration elements
for (IExtension extension : extensions) {
    IConfigurationElement[] elements = extension.getConfigurationElements();
    for (IConfigurationElement element : elements) {
        String className = element.getAttribute("class");
        // Create executable extension (instantiates the class)
        Object provider = element.createExecutableExtension("class");
    }
}
```

### Listening for Changes

```java
IRegistryEventListener listener = new IRegistryEventListener() {
    @Override
    public void added(IExtension[] extensions) {
        // Handle added extensions
    }
    
    @Override
    public void removed(IExtension[] extensions) {
        // Handle removed extensions
    }
    
    @Override
    public void added(IExtensionPoint[] extensionPoints) {
        // Handle added extension points
    }
    
    @Override
    public void removed(IExtensionPoint[] extensionPoints) {
        // Handle removed extension points
    }
};

// Register listener for all changes
registry.addListener(listener);

// Or register for specific namespace
registry.addListener(listener, "com.example.plugin");
```

### Creating Extensions Programmatically

```java
// Get a registry strategy
IRegistryProvider provider = ...; // obtain provider
IExtensionRegistry registry = provider.getRegistry();

// Create contribution
ContributorFactoryOSGi factory = new ContributorFactoryOSGi();
IContributor contributor = factory.createContributor(bundle);

// Add extension programmatically
registry.addContribution(xmlInputStream, contributor, false, null, null, null);
```

## Extension Point Schema

Extension points can define schemas (`.exsd` files) that:
- Document the structure of valid extensions
- Enable validation in development tools
- Provide auto-completion in XML editors

Example schema excerpt:
```xml
<element name="provider">
   <annotation>
      <documentation>
         A provider that implements the service.
      </documentation>
   </annotation>
   <attribute name="class" type="java.lang.String" use="required">
      <annotation>
         <documentation>
            The fully qualified class name of the provider implementation.
         </documentation>
      </annotation>
   </attribute>
</element>
```

## Dynamic Behavior

The registry supports dynamic scenarios where bundles can be:
- Installed at runtime
- Updated with new extension contributions
- Uninstalled, removing their contributions

Components must handle `InvalidRegistryObjectException` if they hold references to registry objects when bundles are updated or removed.

```java
try {
    String value = configElement.getAttribute("name");
} catch (InvalidRegistryObjectException e) {
    // Extension was removed or became invalid
    // Re-query the registry
}
```

## Adapter Framework

The registry includes an adapter framework for type conversion:

```xml
<extension point="org.eclipse.core.runtime.adapters">
   <factory 
      adaptableType="com.example.SourceType"
      class="com.example.AdapterFactory">
      <adapter type="com.example.TargetType"/>
   </factory>
</extension>
```

Query adapters programmatically:
```java
IAdapterManager manager = ...; // obtain adapter manager
TargetType target = manager.getAdapter(sourceObject, TargetType.class);
```

## Configuration

The registry can be configured through system properties or framework properties:
- **Registry location**: Where to persist registry cache
- **Multi-language support**: Bundle localization
- **Development mode**: Enhanced validation and error reporting

## Use Cases

- **Plugin architectures**: Applications like Eclipse IDE use this for plugin management
- **Service providers**: Discover implementations of service interfaces
- **UI contributions**: Menus, views, perspectives, wizards
- **Content types**: Define and discover content type handlers
- **Custom extension mechanisms**: Build domain-specific extension systems

## Implementation Details

- **Bundle**: `org.eclipse.equinox.registry`
- **Singleton**: Yes - only one instance per framework
- **Activation Policy**: Lazy activation
- **Extension Points Defined**:
  - `org.eclipse.core.runtime.adapters`: For adapter factory contributions

## Requirements

- JavaSE-17 or higher
- `org.eclipse.equinox.common` bundle version 3.15.100 or higher
- Optional: OSGi framework (can run standalone)

## Thread Safety

The registry is thread-safe. Multiple threads can query and modify the registry concurrently.

## Performance Considerations

- **Lazy loading**: Extension contributions are parsed on-demand
- **Caching**: Registry state is cached for performance
- **Weak references**: Registry objects use weak references to avoid memory leaks

## Migration from Legacy APIs

If migrating from older Eclipse versions:
- Use `IRegistryEventListener` instead of deprecated `IRegistryChangeListener`
- Use OSGi services to obtain registry instead of static methods where possible
- Handle dynamic bundle scenarios with `InvalidRegistryObjectException`

## Additional Resources

- Main API package: `org.eclipse.core.runtime`
- SPI package: `org.eclipse.core.runtime.spi`
- Dynamic helpers: `org.eclipse.core.runtime.dynamichelpers`
- [OSGi Compendium Specification](https://docs.osgi.org/specification/)

## License

Eclipse Public License 2.0 (EPL-2.0)
