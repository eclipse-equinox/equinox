package org.eclipse.osgi.service.resolver;


/**
 * VersionConstraints represent the relationship between two bundles (in the 
 * case of bundle requires) or a bundle and a package (in the case of import/export).
 */
public interface VersionConstraint extends Cloneable {

	public byte NO_MATCH = 0;
	public byte PERFECT_MATCH = 1;
	public byte EQUIVALENT_MATCH = 2;
	public byte COMPATIBLE_MATCH = 3;
	public byte GREATER_EQUAL_MATCH = 4;

	public String getName();

	public Version getVersionSpecification();
	
	public Version getActualVersion();
	
	public byte getMatchingRule();
	
	public BundleDescription getBundle();

	public BundleDescription getSupplier();

	public boolean isResolved();
	
	boolean isSatisfiedBy(Version version);	
}
