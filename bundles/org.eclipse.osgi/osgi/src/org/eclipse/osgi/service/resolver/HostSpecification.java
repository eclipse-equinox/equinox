package org.eclipse.osgi.service.resolver;

/**
 * A representation of one host bundle constraint as seen in a 
 * bundle manifest and managed by a state and resolver.
 */
public interface HostSpecification extends VersionConstraint {

	public boolean reloadHost();

}
