package org.eclipse.osgi.container;

import java.util.*;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.osgi.resource.Resource;

/**
 * @since 3.10
 */
public class ModuleResolutionReport implements ResolutionReport {
	public static class Builder {
		private final Map<Resource, List<Entry>> resourceToEntries = new HashMap<Resource, List<Entry>>();

		public void addEntry(Resource resource, Entry.Type type, Object data) {
			List<Entry> entries = resourceToEntries.get(resource);
			if (entries == null) {
				entries = new ArrayList<Entry>();
				resourceToEntries.put(resource, entries);
			}
			entries.add(new EntryImpl(type, data));
		}

		public ModuleResolutionReport build() {
			return new ModuleResolutionReport(resourceToEntries);
		}
	}

	private static class EntryImpl implements Entry {
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

	ModuleResolutionReport(Map<Resource, List<Entry>> entries) {
		this.entries = Collections.unmodifiableMap(new HashMap<Resource, List<ResolutionReport.Entry>>(entries));
	}

	@Override
	public Map<Resource, List<Entry>> getEntries() {
		return entries;
	}
}
