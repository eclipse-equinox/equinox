package org.eclipse.equinox.resolver.tests;

import java.util.Collection;
import java.util.Map;

import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.ResourceConstants;

public class RootRequirement implements Requirement {
	private final Collection<Capability> target;

	public RootRequirement(Resource resource) {
		target = resource.getCapabilities(ResourceConstants.IDENTITY_NAMESPACE);
	}

	public String getNamespace() {
		throw new UnsupportedOperationException();
	}

	public Map<String, String> getDirectives() {
		throw new UnsupportedOperationException();
	}

	public Map<String, Object> getAttributes() {
		throw new UnsupportedOperationException();
	}

	public Resource getResource() {
		throw new UnsupportedOperationException();
	}

	public boolean matches(Capability capability) {
		throw new UnsupportedOperationException();
	}

	public Collection<Capability> getTarget() {
		return target;
	}
}
