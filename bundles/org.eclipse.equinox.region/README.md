# Equinox Regions Bundle

## Overview

The Equinox Regions bundle (`org.eclipse.equinox.region`) provides support for creating isolated regions of bundles within an OSGi framework. A region is a subset of bundles that has controlled visibility to bundles, packages, and services from other regions, enabling modularity and isolation in complex OSGi applications.

## What are Regions?

A **region** is a subset of the bundles in an OSGi framework that is "weakly" isolated from other regions. Regions are connected via directed edges in a **region digraph**, where each edge is associated with a filter that controls what is visible from one region to another.

Key characteristics:
- Each bundle belongs to exactly one region
- Regions control visibility of bundles, packages, and services between regions
- Regions provide "weak" isolation - bundles can still discover other bundles through certain means (e.g., Wire Admin)
- The region digraph is a simple (no loops), labeled, directed graph

## Use Cases

Regions are useful for:
- **Application isolation**: Running multiple applications in the same OSGi framework with controlled interaction
- **Security boundaries**: Limiting what bundles in one region can see or access from other regions
- **Modular systems**: Organizing large systems into logical regions with well-defined interfaces
- **Multi-tenancy**: Supporting multiple tenants with isolated bundles in a single framework

## Core API

### Main Interfaces

- **`Region`**: Represents a region - a subset of bundles with a unique name
- **`RegionDigraph`**: The directed graph of regions and their connections
- **`RegionFilter`**: Controls what bundles, packages, and services are visible across a region connection
- **`RegionFilterBuilder`**: Builder for creating region filters

### Getting Started

To use regions, you need to obtain the `RegionDigraph` service from the OSGi service registry:

```java
BundleContext context = ...; // your bundle context
ServiceReference<RegionDigraph> ref = context.getServiceReference(RegionDigraph.class);
RegionDigraph digraph = context.getService(ref);
```

### Creating Regions

```java
// Create a new region
Region appRegion = digraph.createRegion("application-region");

// Install a bundle in the region
Bundle bundle = appRegion.installBundle("file:/path/to/bundle.jar");

// Or associate an already installed bundle
appRegion.addBundle(existingBundle);
```

### Connecting Regions

Regions are connected via filtered edges:

```java
// Create a region for the application
Region appRegion = digraph.createRegion("application");

// Create a region for shared services
Region servicesRegion = digraph.createRegion("services");

// Create a filter that allows the application region to see certain packages
RegionFilterBuilder filterBuilder = digraph.createRegionFilterBuilder();
filterBuilder.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(osgi.wiring.package=com.example.api)");
filterBuilder.allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(symbolicname=com.example.service)");
RegionFilter filter = filterBuilder.build();

// Connect the regions
appRegion.connectRegion(servicesRegion, filter);
```

### Region Filters

Filters control what is visible across region boundaries using OSGi filter syntax:

- **`VISIBLE_BUNDLE_NAMESPACE`**: Control bundle visibility by symbolic name, version, etc.
- **`VISIBLE_PACKAGE_NAMESPACE`**: Control package visibility
- **`VISIBLE_SERVICE_NAMESPACE`**: Control service visibility
- **`VISIBLE_REQUIRE_NAMESPACE`**: Control visibility of other capabilities

Example filters:
```java
// Allow all bundles from vendor "example"
filterBuilder.allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(vendor=example)");

// Allow specific package
filterBuilder.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(osgi.wiring.package=org.osgi.service.http)");

// Allow services implementing a specific interface
filterBuilder.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(objectClass=org.osgi.service.http.HttpService)");
```

## Implementation Details

- **Fragment Host**: This bundle is a fragment to the `system.bundle`, integrating deeply with the OSGi framework
- **Extension Bundle**: Uses `ExtensionBundle-Activator` to initialize the region support
- **OSGi Hooks**: Implements various OSGi hooks to intercept and control bundle, package, and service visibility

## Thread Safety

All region implementations are thread-safe and can be accessed concurrently from multiple threads.

## Additional Resources

- API Package: `org.eclipse.equinox.region`
- Management Package: `org.eclipse.equinox.region.management` (for JMX support)
- OSGi Specification: [OSGi Core Specification](https://docs.osgi.org/specification/)

## Requirements

- JavaSE-1.8 or higher
- OSGi Core Framework

## License

Eclipse Public License 2.0 (EPL-2.0)
