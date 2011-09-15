/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.resolver;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.equinox.internal.resolver.FilterParser.FilterComponent;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.service.resolver.extras.*;
import org.osgi.framework.*;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.resource.*;
import org.osgi.framework.wiring.*;
import org.osgi.service.resolver.Environment;
import org.osgi.service.resolver.ResolutionException;

public class EquinoxResolverHook implements ResolverHookFactory, ResolverHook {
	private final State state;
	private final Environment environment;
	private final Map<Resource, Map<String, List<Wire>>> wiring;
	private final Map<Resource, BundleDescription> resourceToDescription = new HashMap<Resource, BundleDescription>();

	public EquinoxResolverHook(State state, Environment environment) {
		this.state = state;
		this.environment = environment;
		Map<Resource, List<Wire>> envWiring = environment.getWiring();
		this.wiring = new HashMap<Resource, Map<String, List<Wire>>>();
		if (envWiring != null) {
			for (Map.Entry<Resource, List<Wire>> wires : envWiring.entrySet()) {
				Map<String, List<Wire>> mappedWires = new HashMap<String, List<Wire>>();
				this.wiring.put(wires.getKey(), mappedWires);
				for (Wire wire : wires.getValue()) {
					String namespace = wire.getRequirement().getNamespace();
					List<Wire> namespaceWires = mappedWires.get(namespace);
					if (namespaceWires == null) {
						namespaceWires = new ArrayList<Wire>();
						mappedWires.put(namespace, namespaceWires);
					}
					namespaceWires.add(wire);
				}
			}
		}
	}

	public Map<Resource, List<Wire>> resolve(Collection<? extends Resource> mandatoryResources, Collection<? extends Resource> optionalResources) throws ResolutionException {
		if (mandatoryResources == null)
			mandatoryResources = Collections.emptyList();
		if (optionalResources == null)
			optionalResources = Collections.emptyList();
		if (mandatoryResources.isEmpty() && optionalResources.isEmpty())
			return Collections.emptyMap();

		// Populate the Equinox State with the initial set of resolved resources
		for (Resource resource : wiring.keySet()) {
			state.addBundle(createDescription(resource));
		}
		// resolve the existing resources according to the wiring
		if (!wiring.isEmpty())
			state.resolve();
		// all descriptions added up to this point must be resolved.
		for (BundleDescription description : resourceToDescription.values()) {
			if (!description.isResolved())
				throw new ResolutionException("Could not resolve the resource: " + description.getUserObject()); //$NON-NLS-1$
		}
		// Populate the Equinox State with the initial set of mandatory/optional resources
		for (Resource resource : mandatoryResources)
			state.addBundle(createDescription(resource));
		for (Resource resource : optionalResources)
			state.addBundle(createDescription(resource));

		Map<Resource, List<Wire>> result = new HashMap<Resource, List<Wire>>();
		state.resolve();

		List<BundleDescription> unresolvedMandatory = new ArrayList<BundleDescription>(mandatoryResources.size());
		for (Resource resource : mandatoryResources) {
			BundleDescription description = resourceToDescription.get(resource);
			if (!description.isResolved())
				unresolvedMandatory.add(description);
			else
				addWires(result, description);
		}
		if (!unresolvedMandatory.isEmpty()) {
			// TODO need to make a better error message here
			throw new ResolutionException("Could not resolve mandatory resources: " + unresolvedMandatory); //$NON-NLS-1$
		}

		for (Resource resource : optionalResources) {
			BundleDescription description = resourceToDescription.get(resource);
			if (description.isResolved())
				addWires(result, description);
		}

		return result;
	}

	private void addWires(Map<Resource, List<Wire>> result, BundleDescription resolved) {
		Resource resource = (Resource) resolved.getUserObject();
		if (result.get(resource) != null || wiring.get(resource) != null)
			return; // already processed or previously resolved
		BundleWiring bundleWiring = resolved.getWiring();
		List<BundleWire> bundleWires = bundleWiring.getRequiredWires(null);
		List<Wire> wires = new ArrayList<Wire>(bundleWires.size());
		result.put(resource, wires);
		for (BundleWire bundleWire : bundleWires) {
			final Resource requirer = (Resource) ((BundleDescription) bundleWire.getRequirer()).getUserObject();
			final Resource provider = (Resource) ((BundleDescription) bundleWire.getProvider()).getUserObject();

			final Requirement requirement = (Requirement) ((SpecificationReference) bundleWire.getRequirement()).getSpecification().getUserObject();
			final Capability capability;
			BundleDescription capabilityDescription;
			if (BundleRevision.HOST_NAMESPACE.equals(requirement.getNamespace()) || BundleRevision.BUNDLE_NAMESPACE.equals(requirement.getNamespace())) {
				List<Capability> capabilities = provider.getCapabilities(requirement.getNamespace());
				capability = capabilities.get(0);
				capabilityDescription = (BundleDescription) bundleWire.getProvider();
			} else {
				BaseDescription baseDescription = ((DescriptionReference) bundleWire.getCapability()).getDescription();
				capabilityDescription = baseDescription.getSupplier();
				capability = (Capability) baseDescription.getUserObject();
			}

			addWires(result, capabilityDescription);

			wires.add(new Wire() {
				public Resource getRequirer() {
					return requirer;
				}

				public Requirement getRequirement() {
					return requirement;
				}

				public Resource getProvider() {
					return provider;
				}

				public Capability getCapability() {
					return capability;
				}

				public String toString() {
					return "[" + requirer + ':' + requirement + "] -> [" + provider + ':' + capability + ']'; //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
		}
	}

	Capability getOriginalCapability(Capability capability) {
		if (BundleRevision.HOST_NAMESPACE.equals(capability.getNamespace()) || BundleRevision.BUNDLE_NAMESPACE.equals(capability.getNamespace())) {
			Resource provider = (Resource) ((BundleDescription) capability.getResource()).getUserObject();
			List<Capability> capabilities = provider.getCapabilities(capability.getNamespace());
			return capabilities.get(0);
		}
		return (Capability) ((DescriptionReference) capability).getDescription().getUserObject();
	}

	private BundleDescription createDescription(Resource resource) {
		BundleDescription description = resourceToDescription.get(resource);
		if (description != null)
			return description;
		Collection<Capability> idList = resource.getCapabilities(ResourceConstants.IDENTITY_NAMESPACE);
		if (idList.size() != 1)
			throw new IllegalArgumentException("Bogus osgi.identity: " + idList); //$NON-NLS-1$
		Capability id = idList.iterator().next();

		Map<String, Object> idAttrs = new HashMap<String, Object>(id.getAttributes());

		String symbolicName = (String) idAttrs.remove(ResourceConstants.IDENTITY_NAMESPACE);
		Version version = (Version) idAttrs.remove(ResourceConstants.IDENTITY_VERSION_ATTRIBUTE);

		String symbolicNameSpecification = symbolicName + toString(idAttrs, "=", true) + toString(id.getDirectives(), ":=", true); //$NON-NLS-1$ //$NON-NLS-2$

		List<ExportPackageDescription> exportPackages = new ArrayList<ExportPackageDescription>();
		List<GenericDescription> provideCapabilities = new ArrayList<GenericDescription>();
		List<ImportPackageSpecification> importPackages = new ArrayList<ImportPackageSpecification>();
		List<GenericSpecification> requireCapabilities = new ArrayList<GenericSpecification>();
		List<HostSpecification> fragmentHost = new ArrayList<HostSpecification>(0);
		List<BundleSpecification> requireBundles = new ArrayList<BundleSpecification>();

		Collection<Capability> capabilities = resource.getCapabilities(null);

		Capability osgiIdentity = null;
		for (Capability capability : capabilities) {
			String namespace = capability.getNamespace();
			if (ResourceConstants.IDENTITY_NAMESPACE.equals(namespace)) {
				osgiIdentity = capability;
				continue;
			} else if (namespace == null || BundleRevision.HOST_NAMESPACE.equals(namespace) || BundleRevision.BUNDLE_NAMESPACE.equals(namespace)) {
				continue;
			} else if (BundleRevision.PACKAGE_NAMESPACE.equals(namespace)) {
				exportPackages.addAll(creatExportPackage(capability));
			} else {
				provideCapabilities.addAll(createProvideCapability(capability));
			}
		}

		Collection<Requirement> requirements = resource.getRequirements(null);
		for (Requirement requirement : requirements) {
			String namespace = requirement.getNamespace();
			if (namespace == null || !environment.isEffective(requirement)) {
				continue;
			} else if (BundleRevision.BUNDLE_NAMESPACE.equals(namespace)) {
				requireBundles.addAll(createRequireBundle(requirement));
			} else if (BundleRevision.HOST_NAMESPACE.equals(namespace)) {
				fragmentHost.addAll(createFragmentHost(requirement));
			} else if (BundleRevision.PACKAGE_NAMESPACE.equals(namespace)) {
				importPackages.addAll(createImportPackage(requirement));
			} else {
				requireCapabilities.addAll(createRequireCapability(requirement));
			}
		}

		BundleDescription result = state.getFactory().createBundleDescription(state.getHighestBundleId() + 1, symbolicNameSpecification, version, null, requireBundles.toArray(new BundleSpecification[requireBundles.size()]), fragmentHost.size() == 0 ? null : fragmentHost.get(0), importPackages.toArray(new ImportPackageSpecification[importPackages.size()]), exportPackages.toArray(new ExportPackageDescription[exportPackages.size()]), null, null, requireCapabilities.toArray(new GenericSpecification[requireCapabilities.size()]), provideCapabilities.toArray(new GenericDescription[provideCapabilities.size()]), null);
		resourceToDescription.put(resource, result);
		result.setUserObject(resource);
		GenericDescription[] genericDescs = result.getGenericCapabilities();
		for (GenericDescription genericDesc : genericDescs) {
			if (ResourceConstants.IDENTITY_NAMESPACE.equals(genericDesc.getType()))
				genericDesc.setUserObject(osgiIdentity);
		}
		return result;

	}

	private List<ExportPackageDescription> creatExportPackage(Capability capability) {
		Map<String, Object> attributes = new HashMap<String, Object>(capability.getAttributes());
		Map<String, String> directives = capability.getDirectives();
		String packageName = (String) attributes.remove(BundleRevision.PACKAGE_NAMESPACE);
		// remove invalid attributes
		attributes.remove(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
		attributes.remove(Constants.BUNDLE_VERSION_ATTRIBUTE);
		String declaration = packageName + toString(attributes, "=", true) + toString(directives, ":=", true); //$NON-NLS-1$//$NON-NLS-2$
		List<ExportPackageDescription> result = state.getFactory().createExportPackageDescriptions(declaration);
		for (ExportPackageDescription export : result) {
			export.setUserObject(capability);
		}
		return result;
	}

	private List<GenericDescription> createProvideCapability(Capability capability) {
		Map<String, Object> attributes = capability.getAttributes();
		Map<String, String> directives = capability.getDirectives();
		if ("osgi.ee".equals(capability.getNamespace())) { //$NON-NLS-1$
			attributes = new HashMap<String, Object>(attributes);
			attributes.remove("x-equinox-ee"); //$NON-NLS-1$
		}
		String declaration = capability.getNamespace() + toString(attributes, "=", false) + toString(directives, ":=", true); //$NON-NLS-1$//$NON-NLS-2$
		List<GenericDescription> result = state.getFactory().createGenericDescriptions(declaration);
		for (GenericDescription genericDescription : result) {
			genericDescription.setUserObject(capability);
		}
		return result;
	}

	private List<BundleSpecification> createRequireBundle(Requirement requirement) {
		String declaration = createOSGiRequirement(requirement, BundleRevision.BUNDLE_NAMESPACE, Constants.BUNDLE_VERSION_ATTRIBUTE);
		List<BundleSpecification> result = state.getFactory().createBundleSpecifications(declaration);
		for (BundleSpecification bundleSpecification : result) {
			bundleSpecification.setUserObject(requirement);
		}
		return result;
	}

	private List<HostSpecification> createFragmentHost(Requirement requirement) {
		String declaration = createOSGiRequirement(requirement, BundleRevision.HOST_NAMESPACE, Constants.BUNDLE_VERSION_ATTRIBUTE);
		List<HostSpecification> result = state.getFactory().createHostSpecifications(declaration);
		for (HostSpecification hostSpecification : result) {
			hostSpecification.setUserObject(requirement);
		}
		return result;
	}

	private List<ImportPackageSpecification> createImportPackage(Requirement requirement) {
		String declaration = createOSGiRequirement(requirement, BundleRevision.PACKAGE_NAMESPACE, Constants.VERSION_ATTRIBUTE, Constants.BUNDLE_VERSION_ATTRIBUTE);
		List<ImportPackageSpecification> result = state.getFactory().createImportPackageSpecifications(declaration);
		for (ImportPackageSpecification importPackageSpecification : result) {
			importPackageSpecification.setUserObject(requirement);
		}
		return result;
	}

	private List<GenericSpecification> createRequireCapability(Requirement requirement) {
		Map<String, String> directives = new HashMap<String, String>(requirement.getDirectives());
		// always remove the effective directive; all requirements are effective at this point
		directives.remove(Constants.EFFECTIVE_DIRECTIVE);
		String declaration = requirement.getNamespace() + toString(requirement.getAttributes(), "=", false) + toString(requirement.getDirectives(), ":=", true); //$NON-NLS-1$ //$NON-NLS-2$
		List<GenericSpecification> result = state.getFactory().createGenericSpecifications(declaration);
		for (GenericSpecification genericSpecification : result) {
			genericSpecification.setUserObject(requirement);
		}
		return result;
	}

	private String createOSGiRequirement(Requirement requirement, String namespace, String... versions) {
		Map<String, String> directives = new HashMap<String, String>(requirement.getDirectives());
		String filter = directives.remove(ResourceConstants.REQUIREMENT_FILTER_DIRECTIVE);
		if (filter == null)
			throw new IllegalArgumentException("No filter directive found:" + requirement); //$NON-NLS-1$
		FilterParser parser = new FilterParser(filter);
		FilterComponent component = null;
		try {
			component = parser.parse();
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Invalid filter directive", e); //$NON-NLS-1$
		}
		Map<String, String> matchingAttributes = component.getStandardOSGiAttributes(versions);
		String name = matchingAttributes.remove(namespace);
		if (name == null)
			throw new IllegalArgumentException("Invalid requirement: " + requirement); //$NON-NLS-1$
		return name + toString(matchingAttributes, "=", true) + toString(directives, ":=", true); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public ResolverHook begin(Collection<BundleRevision> triggers) {
		return this;
	}

	public void filterResolvable(Collection<BundleRevision> candidates) {
		// nothing
	}

	public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
		// clear all collisions; environment is expected to handle singletons
		collisionCandidates.clear();
		return;
	}

	public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
		// this is quick and dirty way to do this; we know the resources are BundleDescriptions
		BundleDescription description = (BundleDescription) requirement.getResource();
		Resource originalResource = (Resource) description.getUserObject();
		Map<String, List<Wire>> resolvedWires = wiring.get(originalResource);
		if (resolvedWires != null) {
			filterAccordingToWiring(requirement, originalResource, description, resolvedWires, candidates);
		} else {
			filterAccordingToEnvironment(requirement, candidates);
		}
	}

	private void filterAccordingToEnvironment(BundleRequirement requirement, Collection<BundleCapability> candidates) {
		Requirement originalRequirement = (Requirement) ((SpecificationReference) requirement).getSpecification().getUserObject();
		Collection<Capability> envCapabilities = environment.findProviders(originalRequirement);
		// first find new resources that should be added to the state
		boolean added = false;
		for (Capability capability : envCapabilities) {
			Resource originalResource = capability.getResource();
			BundleDescription existing = resourceToDescription.get(originalResource);
			if (existing == null) {
				BundleDescription toAdd = createDescription(originalResource);
				state.addBundle(toAdd);
				added = true;
			}
		}
		if (added)
			return; // do nothing resolver needs to recall us
		// Now filter out the candidates from the resolver according to the environment
		for (Iterator<BundleCapability> iCandidates = candidates.iterator(); iCandidates.hasNext();) {
			BaseDescription description = ((DescriptionReference) iCandidates.next()).getDescription();
			Capability originalCapability;
			if (BundleRevision.HOST_NAMESPACE.equals(requirement.getNamespace()) || BundleRevision.BUNDLE_NAMESPACE.equals(requirement.getNamespace())) {
				Resource provider = (Resource) description.getUserObject();
				List<Capability> hostCapabilities = provider.getCapabilities(requirement.getNamespace());
				originalCapability = hostCapabilities.get(0);
			} else {
				originalCapability = (Capability) description.getUserObject();
			}
			if (!envCapabilities.contains(originalCapability))
				iCandidates.remove();
		}
		if (candidates.size() > 1) {
			if (candidates instanceof Sortable<?>) {
				final List<Capability> sorted = new ArrayList<Capability>(envCapabilities);
				((Sortable<BundleCapability>) candidates).sort(new Comparator<BundleCapability>() {
					public int compare(BundleCapability o1, BundleCapability o2) {
						Capability orig1 = getOriginalCapability(o1);
						Capability orig2 = getOriginalCapability(o2);
						int o1Index = sorted.indexOf(orig1);
						int o2Index = sorted.indexOf(orig2);
						return o1Index - o2Index;
					}
				});
			}
		}
	}

	private Resource getHostResource(Resource originalResource, Map<String, List<Wire>> resolvedWires) {
		if (resolvedWires == null)
			return null;
		List<Wire> hostWires = resolvedWires.get(BundleRevision.HOST_NAMESPACE);
		if (hostWires == null)
			return null;
		return hostWires.size() > 0 ? hostWires.get(0).getCapability().getResource() : null;
	}

	@SuppressWarnings("unchecked")
	private void filterAccordingToWiring(Requirement requirement, Resource originalResource, BundleDescription description, Map<String, List<Wire>> resolvedWires, Collection<BundleCapability> candidates) {
		if (description.getHost() != null && !BundleRevision.HOST_NAMESPACE.equals(requirement.getNamespace())) {
			originalResource = getHostResource(originalResource, resolvedWires);
			resolvedWires = originalResource == null ? Collections.EMPTY_MAP : wiring.get(originalResource);
		}
		List<Wire> namespaceWires = resolvedWires.get(requirement.getNamespace());
		if (namespaceWires == null) {
			candidates.clear();
		} else {
			for (Iterator<BundleCapability> iCandidates = candidates.iterator(); iCandidates.hasNext();) {
				Capability candidate = iCandidates.next();
				Resource origProvider = (Resource) ((BaseDescription) candidate.getResource()).getUserObject();
				Collection<Capability> origCapabilities = origProvider.getCapabilities(candidate.getNamespace());
				boolean found = false;
				wireLoop: for (Wire wire : namespaceWires) {
					for (Capability capability : origCapabilities) {
						if (wire.getCapability().equals(capability)) {
							found = true;
							break wireLoop;
						}
					}
				}
				if (!found)
					iCandidates.remove();
			}
		}
	}

	public void end() {
		// nothing
	}

	static <V> String toString(Map<String, V> map, String assignment, boolean stringsOnly) {
		if (map.isEmpty())
			return ""; //$NON-NLS-1$
		Set<Entry<String, V>> set = map.entrySet();
		StringBuffer sb = new StringBuffer();
		for (Entry<String, V> entry : set) {
			sb.append("; "); //$NON-NLS-1$
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof List) {
				@SuppressWarnings("unchecked")
				List<Object> list = (List<Object>) value;
				if (list.size() == 0)
					continue;
				Object component = list.get(0);
				String className = component.getClass().getName();
				String type = className.substring(className.lastIndexOf('.') + 1);
				sb.append(key).append(':').append("List<").append(type).append(">").append(assignment).append('"'); //$NON-NLS-1$ //$NON-NLS-2$
				for (Object object : list)
					sb.append(object).append(',');
				sb.setLength(sb.length() - 1);
				sb.append('"');
			} else {
				String type = ""; //$NON-NLS-1$
				if (!(value instanceof String) && !stringsOnly) {
					String className = value.getClass().getName();
					type = ":" + className.substring(className.lastIndexOf('.') + 1); //$NON-NLS-1$
				}
				sb.append(key).append(type).append(assignment).append('"').append(value).append('"');
			}
		}
		return sb.toString();
	}
}
