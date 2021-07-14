/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;

/**
 * A resolution report implementation used by the container for resolution operations.
 * @since 3.10
 */
class ModuleResolutionReport implements ResolutionReport {

	static class Builder {
		private final Map<Resource, List<Entry>> resourceToEntries = new HashMap<>();

		public void addEntry(Resource resource, Entry.Type type, Object data) {
			List<Entry> entries = resourceToEntries.get(resource);
			if (entries == null) {
				entries = new ArrayList<>();
				resourceToEntries.put(resource, entries);
			}
			entries.add(new EntryImpl(type, data));
		}

		public ModuleResolutionReport build(Map<Resource, List<Wire>> resolutionResult, ResolutionException cause) {
			return new ModuleResolutionReport(resolutionResult, resourceToEntries, cause);
		}
	}

	static class EntryImpl implements Entry {
		private final Object data;
		private final Type type;

		EntryImpl(Type type, Object data) {
			this.type = type;
			this.data = data;
		}

		@Override
		public Object getData() {
			return data;
		}

		@Override
		public Type getType() {
			return type;
		}
	}

	private final Map<Resource, List<Entry>> entries;
	private final ResolutionException resolutionException;
	private final Map<Resource, List<Wire>> resolutionResult;

	ModuleResolutionReport(Map<Resource, List<Wire>> resolutionResult, Map<Resource, List<Entry>> entries, ResolutionException cause) {
		this.entries = entries == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(entries));
		this.resolutionResult = resolutionResult == null ? Collections.emptyMap() : Collections.unmodifiableMap(resolutionResult);
		this.resolutionException = cause;
	}

	@Override
	public Map<Resource, List<Entry>> getEntries() {
		return entries;
	}

	@Override
	public ResolutionException getResolutionException() {
		return resolutionException;
	}

	Map<Resource, List<Wire>> getResolutionResult() {
		return resolutionResult;
	}

	private static String getResolutionReport0(String prepend, ModuleRevision revision, Map<Resource, List<ResolutionReport.Entry>> reportEntries, Set<BundleRevision> visited) {
		if (prepend == null) {
			prepend = ""; //$NON-NLS-1$
		}
		if (visited == null) {
			visited = new HashSet<>();
		}
		if (visited.contains(revision)) {
			return ""; //$NON-NLS-1$
		}
		visited.add(revision);
		StringBuilder result = new StringBuilder();
		String id = revision.getRevisions().getModule().getId().toString();
		result.append(prepend).append(revision.getSymbolicName()).append(" [").append(id).append("]").append('\n'); //$NON-NLS-1$ //$NON-NLS-2$

		List<ResolutionReport.Entry> revisionEntries = reportEntries.get(revision);
		if (revisionEntries == null) {
			result.append(prepend).append("  ").append(Msg.ModuleResolutionReport_NoReport); //$NON-NLS-1$
		} else {
			for (ResolutionReport.Entry entry : revisionEntries) {
				printResolutionEntry(result, prepend + "  ", entry, reportEntries, visited); //$NON-NLS-1$
			}
		}
		return result.toString();
	}

	private static void printResolutionEntry(StringBuilder result, String prepend, ResolutionReport.Entry entry, Map<Resource, List<ResolutionReport.Entry>> reportEntries, Set<BundleRevision> visited) {
		switch (entry.getType()) {
			case MISSING_CAPABILITY :
				result.append(prepend).append(Msg.ModuleResolutionReport_UnresolvedReq).append(printRequirement(entry.getData())).append('\n');
				break;
			case SINGLETON_SELECTION :
				result.append(prepend).append(Msg.ModuleResolutionReport_AnotherSingleton).append(entry.getData()).append('\n');
				break;
			case UNRESOLVED_PROVIDER :
				@SuppressWarnings("unchecked")
				Map<Requirement, Set<Capability>> unresolvedProviders = (Map<Requirement, Set<Capability>>) entry.getData();
				for (Map.Entry<Requirement, Set<Capability>> unresolvedRequirement : unresolvedProviders.entrySet()) {
					// for now only printing the first possible unresolved candidates
					Set<Capability> unresolvedCapabilities = unresolvedRequirement.getValue();
					if (!unresolvedCapabilities.isEmpty()) {
						Capability unresolvedCapability = unresolvedCapabilities.iterator().next();
						// make sure this is not a case of importing and exporting the same package
						if (!unresolvedRequirement.getKey().getResource().equals(unresolvedCapability.getResource())) {
							result.append(prepend).append(Msg.ModuleResolutionReport_UnresolvedReq).append(printRequirement(unresolvedRequirement.getKey())).append('\n');
							result.append(prepend).append("  -> ").append(printCapability(unresolvedCapability)).append('\n'); //$NON-NLS-1$
							result.append(getResolutionReport0(prepend + "     ", (ModuleRevision) unresolvedCapability.getResource(), reportEntries, visited)); //$NON-NLS-1$
						}
					}
				}
				break;
			case FILTERED_BY_RESOLVER_HOOK :
				result.append(Msg.ModuleResolutionReport_FilteredByHook).append('\n');
				break;
			case USES_CONSTRAINT_VIOLATION :
				result.append(prepend).append(Msg.ModuleResolutionReport_UsesConstraintError).append('\n');
				result.append("  ").append(entry.getData()); //$NON-NLS-1$
				break;
			default :
				result.append(Msg.ModuleResolutionReport_Unknown).append("type=").append(entry.getType()).append(" data=").append(entry.getData()).append('\n'); //$NON-NLS-1$ //$NON-NLS-2$
				break;
		}
	}

	private static Object printCapability(Capability cap) {
		if (PackageNamespace.PACKAGE_NAMESPACE.equals(cap.getNamespace())) {
			return Constants.EXPORT_PACKAGE + ": " + createOSGiCapability(cap); //$NON-NLS-1$
		} else if (BundleNamespace.BUNDLE_NAMESPACE.equals(cap.getNamespace())) {
			return Constants.BUNDLE_SYMBOLICNAME + ": " + createOSGiCapability(cap); //$NON-NLS-1$
		} else if (HostNamespace.HOST_NAMESPACE.equals(cap.getNamespace())) {
			return Constants.BUNDLE_SYMBOLICNAME + ": " + createOSGiCapability(cap); //$NON-NLS-1$
		}
		return Constants.PROVIDE_CAPABILITY + ": " + cap.toString(); //$NON-NLS-1$
	}

	private static String createOSGiCapability(Capability cap) {
		Map<String, Object> attributes = new HashMap<>(cap.getAttributes());
		Map<String, String> directives = cap.getDirectives();
		String name = String.valueOf(attributes.remove(cap.getNamespace()));
		return name + ModuleRevision.toString(attributes, false, true) + ModuleRevision.toString(directives, true, true);
	}

	private static String printRequirement(Object data) {
		if (!(data instanceof Requirement)) {
			return String.valueOf(data);
		}
		Requirement req = (Requirement) data;
		if (PackageNamespace.PACKAGE_NAMESPACE.equals(req.getNamespace())) {
			return Constants.IMPORT_PACKAGE + ": " + createOSGiRequirement(req, PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE); //$NON-NLS-1$
		} else if (BundleNamespace.BUNDLE_NAMESPACE.equals(req.getNamespace())) {
			return Constants.REQUIRE_BUNDLE + ": " + createOSGiRequirement(req, BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE); //$NON-NLS-1$
		} else if (HostNamespace.HOST_NAMESPACE.equals(req.getNamespace())) {
			return Constants.FRAGMENT_HOST + ": " + createOSGiRequirement(req, HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE); //$NON-NLS-1$
		}
		return Constants.REQUIRE_CAPABILITY + ": " + req.toString(); //$NON-NLS-1$
	}

	private static String createOSGiRequirement(Requirement requirement, String... versions) {
		Map<String, String> directives = new HashMap<>(requirement.getDirectives());
		String filter = directives.remove(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		if (filter == null)
			throw new IllegalArgumentException("No filter directive found:" + requirement); //$NON-NLS-1$
		FilterImpl filterImpl;
		try {
			filterImpl = FilterImpl.newInstance(filter);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Invalid filter directive", e); //$NON-NLS-1$
		}
		Map<String, String> matchingAttributes = filterImpl.getStandardOSGiAttributes(versions);
		String name = matchingAttributes.remove(requirement.getNamespace());
		if (name == null)
			throw new IllegalArgumentException("Invalid requirement: " + requirement); //$NON-NLS-1$
		return name + ModuleRevision.toString(matchingAttributes, false, true) + ModuleRevision.toString(directives, true, true);
	}

	@Override
	public String getResolutionReportMessage(Resource resource) {
		return getResolutionReport0(null, (ModuleRevision) resource, getEntries(), null);
	}
}
