package org.eclipse.osgi.container;

import java.util.*;
import org.osgi.resource.Resource;

/**
 * @since 3.10
 */
public class ResolutionReport implements org.eclipse.osgi.framework.report.ResolutionReport {
	public static class Builder {
		private final Map<Resource, List<Entry>> resourceToEntries = new HashMap<Resource, List<Entry>>();

		public void addEntry(Resource resource, Entry.Type type) {
			List<Entry> entries = resourceToEntries.get(resource);
			if (entries == null) {
				entries = new ArrayList<Entry>();
				resourceToEntries.put(resource, entries);
			}
			entries.add(new EntryImpl(type));
		}

		public ResolutionReport build() {
			return new ResolutionReport(resourceToEntries);
		}
	}

	private static class EntryImpl implements Entry {
		private final Type type;

		EntryImpl(Type type) {
			this.type = type;
		}

		@Override
		public Type getType() {
			return type;
		}
	}

	private final Map<Resource, List<ResolutionReport.Entry>> entries;

	ResolutionReport(Map<Resource, List<ResolutionReport.Entry>> entries) {
		this.entries = Collections.unmodifiableMap(new HashMap<Resource, List<ResolutionReport.Entry>>(entries));
	}

	@Override
	public Map<Resource, List<Entry>> getEntries() {
		return entries;
	}
}
