package org.eclipse.osgi.service.resolver;


/**
 * A representation of one package import constraint as seen in a 
 * bundle manifest and managed by a state and resolver.
 */
public interface PackageSpecification extends VersionConstraint {

	public boolean isExported();
}
