# Equinox Transforms Hook

## Overview

The Equinox Transforms Hook bundle (`org.eclipse.equinox.transforms.hook`) provides a framework for transforming bundle resources at runtime. This OSGi framework extension allows bundles to apply transformations to their content (such as class files, configuration files, or other resources) before they are used by the framework.

## What is a Bundle Transformer?

A bundle transformer intercepts bundle resources as they are accessed and applies transformations to them. This enables powerful capabilities like:
- On-the-fly resource modification
- Content adaptation for different environments
- Resource localization
- Dynamic code instrumentation
- Configuration file preprocessing

## Key Features

- **OSGi Framework Extension**: Integrates deeply with the OSGi framework as a fragment
- **Multiple Transformer Types**: Support for different transformation mechanisms (replace, XSLT, custom)
- **Declarative Configuration**: Transform specifications via bundle headers or configuration files
- **Service-based Architecture**: Transformers registered as OSGi services
- **Extensible**: Custom transformer types can be registered

## Architecture

The transforms hook uses several OSGi extension points:
- **Hook Configurator**: Integrates with the framework startup
- **Bundle File Wrapper Factory**: Wraps bundle files to intercept resource access
- **Activator Hook**: Initializes the transformation infrastructure
- **Bundle Extender**: Scans bundles for transformation declarations

## Transformer Types

### Replace Transformer

The built-in replace transformer allows simple text replacement in resources.

**Configuration format (CSV)**:
```csv
pattern,replacement
old-text,new-text
${property},actual-value
```

### XSLT Transformer

For XML transformations using XSLT stylesheets (requires `org.eclipse.equinox.transforms.xslt` bundle).

### Custom Transformers

Implement the `StreamTransformer` interface and register as an OSGi service to provide custom transformation logic.

## Usage

### Declaring Transformations in a Bundle

Use the `Equinox-Transformer` header in your bundle's `META-INF/MANIFEST.MF`:

```manifest
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-SymbolicName: com.example.mybundle
Bundle-Version: 1.0.0
Equinox-Transformer: /transform.csv
```

Or specify the transformer type explicitly:
```manifest
Equinox-Transformer: replace;/transform.csv
Equinox-Transformer: xslt;/transform.xslt
```

### Transform Configuration File

Create a transformation specification file in your bundle. For the replace transformer, use CSV format:

**`/transform.csv`**:
```csv
# Pattern, Replacement
development.server.url,production.server.url
${environment},production
localhost:8080,example.com
```

### Programmatic Registration

Register transformers as OSGi services:

```java
// Implement the transformer interface
public class MyTransformer implements StreamTransformer {
    @Override
    public InputStream getInputStream(InputStream inputStream, URL transformerUrl) 
            throws IOException {
        // Apply transformation
        return transformedStream;
    }
}

// Register as service
Dictionary<String, Object> properties = new Hashtable<>();
properties.put(TransformTuple.TRANSFORMER_TYPE, "my-custom-type");
context.registerService(StreamTransformer.class, new MyTransformer(), properties);
```

### Specifying What to Transform

Create a transform tuple service to specify which resources in which bundles should be transformed:

```java
// Register URL service with transformer type and resource patterns
Dictionary<String, Object> props = new Hashtable<>();
props.put(TransformTuple.TRANSFORMER_TYPE, "replace");

URL transformSpec = bundle.getEntry("/transform.csv");
context.registerService(URL.class, transformSpec, props);
```

## How It Works

1. **Bundle Installation**: When a bundle is installed, the framework reads its manifest
2. **Extender Detection**: The bundle extender detects the `Equinox-Transformer` header
3. **Service Registration**: Transform specifications are registered as URL services
4. **Resource Access**: When bundle resources are accessed, the hook intercepts the access
5. **Transformation**: Appropriate transformers are applied based on configuration
6. **Delivery**: Transformed content is delivered to the requestor

## Use Cases

### Environment-Specific Configuration

Transform configuration files for different deployment environments:

```csv
# Development to Production transformation
${db.host},production-db.example.com
${db.port},5432
debug=true,debug=false
```

### Resource Localization

Adapt resources for different locales or regions on-the-fly.

### Code Instrumentation

Apply bytecode transformations for:
- Debugging
- Performance monitoring
- Security enhancements
- Aspect-oriented programming

### Legacy System Integration

Transform bundle content to work with legacy systems or APIs.

## Advanced Topics

### Custom Stream Transformers

Implement `StreamTransformer` interface:

```java
public interface StreamTransformer {
    /**
     * Transform an input stream.
     *
     * @param inputStream the original stream
     * @param transformerUrl URL to the transformer specification
     * @return transformed stream
     * @throws IOException if transformation fails
     */
    InputStream getInputStream(InputStream inputStream, URL transformerUrl) 
        throws IOException;
}
```

### Transform Tuples

Transform tuples define when and how transformations are applied:

```java
public class TransformTuple {
    public static final String TRANSFORMER_TYPE = "equinox.transformer.type";
    
    // Bundle symbolic name to transform
    private String bundleSymbolicName;
    
    // Resource patterns to transform
    private String[] resourcePatterns;
    
    // Type of transformer to use
    private String transformerType;
    
    // URL to transformer specification
    private URL transformerUrl;
}
```

## Configuration Properties

The hook can be configured via framework properties in the `hookconfigurators.properties` file:

```properties
hook.configurators=org.eclipse.equinox.internal.transforms.TransformerHook
```

## Framework Integration

This bundle is a fragment of `org.eclipse.osgi` (the system bundle), which means:
- It extends the framework's capabilities
- It's loaded early in the framework startup
- It has access to framework internals
- It cannot be started/stopped independently

## Performance Considerations

- **Lazy Transformation**: Resources are transformed only when accessed
- **Caching**: Consider caching transformed resources for frequently accessed content
- **Overhead**: Transformations add overhead; use judiciously
- **Stream Processing**: Transformers should process streams efficiently without loading entire content into memory

## Debugging

Enable framework debugging to see transformation activity:
```
-Dosgi.debug=true
-Dosgi.debug.transforms=true
```

## Requirements

- JavaSE-17 or higher
- OSGi Framework (`org.eclipse.osgi`) version 3.10.0 or higher
- Fragment Host: `org.eclipse.osgi`

## Companion Bundles

- **`org.eclipse.equinox.transforms.xslt`**: Provides XSLT transformation support

## Security Considerations

Transformers can modify bundle content, so:
- Only use trusted transformer implementations
- Validate transformation specifications
- Be aware that transformers run with bundle permissions
- Consider the security implications of dynamic content modification

## Limitations

- Transformations apply to resource streams, not already-loaded classes or content
- Cannot transform bundle metadata (MANIFEST.MF entries)
- Performance overhead for each transformed resource access
- Fragment nature means limited lifecycle control

## Migration and Compatibility

When using transforms:
- Test thoroughly in target environments
- Document transformation requirements
- Consider alternatives like bundle fragments for simpler cases
- Ensure transformer specifications are version-compatible

## Additional Resources

- API Package: `org.eclipse.equinox.internal.transforms` (internal)
- Capability: `osgi.extender;osgi.extender="equinox.transforms.hook"`
- [OSGi Core Specification - Framework Hooks](https://docs.osgi.org/specification/)

## Example: Complete Setup

**1. Create your bundle with transformer declaration:**

`META-INF/MANIFEST.MF`:
```manifest
Bundle-SymbolicName: com.example.app
Bundle-Version: 1.0.0
Equinox-Transformer: /config/transform.csv
```

**2. Add transformation rules:**

`/config/transform.csv`:
```csv
${server.host},production.example.com
${server.port},8080
debug=true,debug=false
```

**3. Resources get transformed at runtime:**

Original `application.properties`:
```properties
server.host=${server.host}
server.port=${server.port}
debug=true
```

Transformed content delivered to application:
```properties
server.host=production.example.com
server.port=8080
debug=false
```

## License

Eclipse Public License 2.0 (EPL-2.0)
