/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.compatibility.state;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.*;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.*;

class StateConverter {
	private final State state;

	StateConverter(State state) {
		this.state = state;
	}

	BundleDescription createDescription(BundleRevision resource) {
		Version version = Version.emptyVersion;
		String symbolicNameSpecification = null;
		Collection<Capability> idList = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);

		if (idList.size() > 1) {
			throw new IllegalArgumentException("Bogus osgi.identity: " + idList); //$NON-NLS-1$
		} else if (idList.size() == 1) {
			Capability id = idList.iterator().next();
			Map<String, Object> idAttrs = new HashMap<>(id.getAttributes());
			String symbolicName = (String) idAttrs.remove(IdentityNamespace.IDENTITY_NAMESPACE);
			symbolicNameSpecification = symbolicName + toString(idAttrs, "=", true) + toString(id.getDirectives(), ":=", true); //$NON-NLS-1$ //$NON-NLS-2$
			version = (Version) idAttrs.remove(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
		}

		List<ExportPackageDescription> exportPackages = new ArrayList<>();
		List<GenericDescription> provideCapabilities = new ArrayList<>();
		List<ImportPackageSpecification> importPackages = new ArrayList<>();
		List<GenericSpecification> requireCapabilities = new ArrayList<>();
		List<HostSpecification> fragmentHost = new ArrayList<>(0);
		List<BundleSpecification> requireBundles = new ArrayList<>();

		Collection<Capability> capabilities = resource.getCapabilities(null);

		Capability osgiIdentity = null;
		for (Capability capability : capabilities) {
			String namespace = capability.getNamespace();
			if (IdentityNamespace.IDENTITY_NAMESPACE.equals(namespace)) {
				osgiIdentity = capability;
			} else if (BundleRevision.HOST_NAMESPACE.equals(namespace) || BundleRevision.BUNDLE_NAMESPACE.equals(namespace)) {
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
			if (BundleRevision.BUNDLE_NAMESPACE.equals(namespace)) {
				requireBundles.addAll(createRequireBundle(requirement));
			} else if (BundleRevision.HOST_NAMESPACE.equals(namespace)) {
				fragmentHost.addAll(createFragmentHost(requirement));
			} else if (BundleRevision.PACKAGE_NAMESPACE.equals(namespace)) {
				importPackages.addAll(createImportPackage(requirement));
			} else {
				requireCapabilities.addAll(createRequireCapability(requirement));
			}
		}

		BundleDescription result = state.getFactory().createBundleDescription(resource.getBundle().getBundleId(), symbolicNameSpecification, version, resource.getBundle().getLocation(), requireBundles.toArray(new BundleSpecification[requireBundles.size()]), fragmentHost.size() == 0 ? null : fragmentHost.get(0), importPackages.toArray(new ImportPackageSpecification[importPackages.size()]), exportPackages.toArray(new ExportPackageDescription[exportPackages.size()]), null, null, requireCapabilities.toArray(new GenericSpecification[requireCapabilities.size()]), provideCapabilities.toArray(new GenericDescription[provideCapabilities.size()]), null);
		result.setUserObject(resource);
		GenericDescription[] genericDescs = result.getGenericCapabilities();
		for (GenericDescription genericDesc : genericDescs) {
			if (IdentityNamespace.IDENTITY_NAMESPACE.equals(genericDesc.getType()))
				genericDesc.setUserObject(osgiIdentity);
		}
		return result;

	}

	private List<ExportPackageDescription> creatExportPackage(Capability capability) {
		Map<String, Object> attributes = new HashMap<>(capability.getAttributes());
		Map<String, String> directives = capability.getDirectives();
		String packageName = (String) attributes.remove(PackageNamespace.PACKAGE_NAMESPACE);
		// remove invalid attributes
		attributes.remove(PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE);
		attributes.remove(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
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

		String declaration = capability.getNamespace() + toString(attributes, "=", false) + toString(directives, ":=", true); //$NON-NLS-1$//$NON-NLS-2$
		List<GenericDescription> result = state.getFactory().createGenericDescriptions(declaration);
		for (GenericDescription genericDescription : result) {
			genericDescription.setUserObject(capability);
		}
		return result;
	}

	private List<BundleSpecification> createRequireBundle(Requirement requirement) {
		String declaration = createOSGiRequirement(requirement, BundleNamespace.BUNDLE_NAMESPACE, BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
		List<BundleSpecification> result = state.getFactory().createBundleSpecifications(declaration);
		for (BundleSpecification bundleSpecification : result) {
			bundleSpecification.setUserObject(requirement);
		}
		return result;
	}

	private List<HostSpecification> createFragmentHost(Requirement requirement) {
		String declaration = createOSGiRequirement(requirement, HostNamespace.HOST_NAMESPACE, HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
		List<HostSpecification> result = state.getFactory().createHostSpecifications(declaration);
		for (HostSpecification hostSpecification : result) {
			hostSpecification.setUserObject(requirement);
		}
		return result;
	}

	private List<ImportPackageSpecification> createImportPackage(Requirement requirement) {
		String declaration = createOSGiRequirement(requirement, PackageNamespace.PACKAGE_NAMESPACE, PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
		List<ImportPackageSpecification> result = state.getFactory().createImportPackageSpecifications(declaration);
		for (ImportPackageSpecification importPackageSpecification : result) {
			importPackageSpecification.setUserObject(requirement);
		}
		return result;
	}

	private List<GenericSpecification> createRequireCapability(Requirement requirement) {
		Map<String, String> directives = new HashMap<>(requirement.getDirectives());
		String filter = directives.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		if (filter != null) {
			directives.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, escapeFilterInput(filter));
		}
		String declaration = requirement.getNamespace() + toString(requirement.getAttributes(), "=", false) + toString(directives, ":=", true); //$NON-NLS-1$ //$NON-NLS-2$
		List<GenericSpecification> result = state.getFactory().createGenericSpecifications(declaration);
		for (GenericSpecification genericSpecification : result) {
			genericSpecification.setUserObject(requirement);
		}
		return result;
	}

	// We have to re-escape the escape characters in the filter string
	private static String escapeFilterInput(final String filter) {
		boolean escaped = false;
		int inlen = filter.length();
		int outlen = inlen << 1; /* inlen * 2 */

		char[] output = new char[outlen];
		filter.getChars(0, inlen, output, inlen);

		int cursor = 0;
		for (int i = inlen; i < outlen; i++) {
			char c = output[i];
			switch (c) {
				case '\\' :
					output[cursor] = '\\';
					cursor++;
					escaped = true;
					break;
			}

			output[cursor] = c;
			cursor++;
		}

		return escaped ? new String(output, 0, cursor) : filter;
	}

	private String createOSGiRequirement(Requirement requirement, String namespace, String... versions) {
		Map<String, String> directives = new HashMap<>(requirement.getDirectives());
		String filter = directives.remove(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		if (filter == null)
			throw new IllegalArgumentException("No filter directive found:" + requirement); //$NON-NLS-1$
		FilterImpl parser;
		try {
			parser = FilterImpl.newInstance(filter);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Invalid filter directive", e); //$NON-NLS-1$
		}
		Map<String, String> matchingAttributes = parser.getStandardOSGiAttributes(versions);
		String name = matchingAttributes.remove(namespace);
		if (name == null)
			throw new IllegalArgumentException("Invalid requirement: " + requirement); //$NON-NLS-1$
		return name + toString(matchingAttributes, "=", true) + toString(directives, ":=", true); //$NON-NLS-1$ //$NON-NLS-2$
	}

	static <V> String toString(Map<String, V> map, String assignment, boolean stringsOnly) {
		if (map.isEmpty())
			return ""; //$NON-NLS-1$
		Set<Entry<String, V>> set = map.entrySet();
		StringBuilder sb = new StringBuilder();
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
