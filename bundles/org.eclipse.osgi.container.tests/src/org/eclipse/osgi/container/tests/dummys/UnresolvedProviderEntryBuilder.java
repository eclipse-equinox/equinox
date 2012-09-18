package org.eclipse.osgi.container.tests.dummys;

import java.util.*;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public class UnresolvedProviderEntryBuilder {
	private final Map<Requirement, List<Capability>> data = new HashMap<Requirement, List<Capability>>();

	private Requirement lastRequirement;

	public Map<Requirement, List<Capability>> build() {
		return new HashMap<Requirement, List<Capability>>(data);
	}

	public UnresolvedProviderEntryBuilder capability(Capability value) {
		data.get(lastRequirement).add(value);
		return this;
	}

	public UnresolvedProviderEntryBuilder requirement(Requirement key) {
		data.put(key, new ArrayList<Capability>());
		lastRequirement = key;
		return this;
	}
}
