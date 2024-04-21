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
import org.eclipse.osgi.container.ModuleResolver.ResolveProcess.ResolveLogger;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;

/**
 * A resolution report implementation used by the container for resolution
 * operations.
 * 
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

		public ModuleResolutionReport build(Map<Resource, List<Wire>> resolutionResult, ResolutionException cause,
				ResolveLogger logger) {
			return new ModuleResolutionReport(resolutionResult, resourceToEntries, cause, logger.totalPerm,
					logger.processedPerm, logger.usesPerm, logger.subPerm, logger.importPerm);
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
	private int totalPerm;
	private int processedPerm;
	private int usesPerm;
	private int subPerm;
	private int importPerm;

	ModuleResolutionReport(Map<Resource, List<Wire>> resolutionResult, Map<Resource, List<Entry>> entries,
			ResolutionException cause, int totalPerm, int processedPerm, int usesPerm, int subPerm, int importPerm) {
		this.totalPerm = totalPerm;
		this.processedPerm = processedPerm;
		this.usesPerm = usesPerm;
		this.subPerm = subPerm;
		this.importPerm = importPerm;
		this.entries = entries == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(entries));
		this.resolutionResult = resolutionResult == null ? Collections.emptyMap()
				: Collections.unmodifiableMap(resolutionResult);
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

	private static String getResolutionReport0(String prepend, ModuleRevision revision,
			Map<Resource, List<ResolutionReport.Entry>> reportEntries, Set<BundleRevision> visited) {
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

	private static void printResolutionEntry(StringBuilder result, String prepend, ResolutionReport.Entry entry,
			Map<Resource, List<ResolutionReport.Entry>> reportEntries, Set<BundleRevision> visited) {
		switch (entry.getType()) {
		case MISSING_CAPABILITY:
			result.append(prepend).append(Msg.ModuleResolutionReport_UnresolvedReq)
					.append(ModuleContainer.toString((Requirement) entry.getData())).append('\n');
			break;
		case SINGLETON_SELECTION:
			result.append(prepend).append(Msg.ModuleResolutionReport_AnotherSingleton).append(entry.getData())
					.append('\n');
			break;
		case UNRESOLVED_PROVIDER:
			@SuppressWarnings("unchecked")
			Map<Requirement, Set<Capability>> unresolvedProviders = (Map<Requirement, Set<Capability>>) entry.getData();
			for (Map.Entry<Requirement, Set<Capability>> unresolvedRequirement : unresolvedProviders.entrySet()) {
				// for now only printing the first possible unresolved candidates
				Set<Capability> unresolvedCapabilities = unresolvedRequirement.getValue();
				if (!unresolvedCapabilities.isEmpty()) {
					Capability unresolvedCapability = unresolvedCapabilities.iterator().next();
					// make sure this is not a case of importing and exporting the same package
					if (!unresolvedRequirement.getKey().getResource().equals(unresolvedCapability.getResource())) {
						result.append(prepend).append(Msg.ModuleResolutionReport_UnresolvedReq)
								.append(ModuleContainer.toString(unresolvedRequirement.getKey())).append('\n');
						result.append(prepend).append("  -> ") //$NON-NLS-1$
								.append(ModuleContainer.toString(unresolvedCapability)).append('\n');
						result.append(getResolutionReport0(prepend + "     ", //$NON-NLS-1$
								(ModuleRevision) unresolvedCapability.getResource(), reportEntries, visited));
					}
				}
			}
			break;
		case FILTERED_BY_RESOLVER_HOOK:
			result.append(Msg.ModuleResolutionReport_FilteredByHook).append('\n');
			break;
		case USES_CONSTRAINT_VIOLATION:
			result.append(prepend).append(Msg.ModuleResolutionReport_UsesConstraintError).append('\n');
			result.append("  ").append(entry.getData()); //$NON-NLS-1$
			break;
		default:
			result.append(Msg.ModuleResolutionReport_Unknown).append("type=").append(entry.getType()).append(" data=") //$NON-NLS-1$ //$NON-NLS-2$
					.append(entry.getData()).append('\n');
			break;
		}
	}

	@Override
	public String getResolutionReportMessage(Resource resource) {
		return getResolutionReport0(null, (ModuleRevision) resource, getEntries(), null);
	}

	@Override
	public int getTotalPermutations() {
		return totalPerm;
	}

	@Override
	public int getProcessedPermutations() {
		return processedPerm;
	}

	@Override
	public int getUsesPermutations() {
		return usesPerm;
	}

	@Override
	public int getImportPermutations() {
		return importPerm;
	}

	@Override
	public int getSubstitutionPermutations() {
		return subPerm;
	}
}
