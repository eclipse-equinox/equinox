package org.eclipse.osgi.service.resolver;


/**
 * A representation of one bundle import constraint as seen in a 
 * bundle manifest and managed by a state and resolver.
 */
public interface BundleSpecification extends VersionConstraint {

	public boolean isExported();

	public boolean isOptional();

}
