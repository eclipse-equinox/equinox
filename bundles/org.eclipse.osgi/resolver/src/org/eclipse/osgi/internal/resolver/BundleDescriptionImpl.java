/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.util.ArrayList;
import java.util.Dictionary;

import org.eclipse.osgi.framework.internal.core.KeyedElement;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Bundle;

public class BundleDescriptionImpl implements BundleDescription, KeyedElement {
	private long bundleId = -1;	
	private String uniqueId;
	private String location;
	private int state;
	private Version version;
	private HostSpecification host;
	private PackageSpecification[] packages;
	private String[] providedPackages;
	private BundleSpecification[] requiredBundles;
	private State containingState;
	private Object userObject;

	public BundleDescriptionImpl() {
	}
	public String getLocation() {
		return location;
	}
	public boolean isResolved() {
		return (state & Bundle.RESOLVED) != 0;
	}
	public State getContainingState() {
		return containingState;
	}
	public int getState() {
		return state;
	}
	public Version getVersion() {
		return version;
	}
	public BundleDescription[] getFragments() {
		if (host != null)
			return new BundleDescription[0];
		return ((StateImpl) containingState).getFragments(this);
	}
	public Dictionary getManifest() {
		// TODO Auto-generated method stub
		return null;
	}
	public HostSpecification getHost() {
		return host;
	}
	public void setContainingState(State value) {
		containingState = value;
	}
	public void setHost(HostSpecification host) {
		this.host = host;
		if (host != null) 
			((VersionConstraintImpl) host).setBundle(this);
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public void setPackages(PackageSpecification[] packages) {
		this.packages = packages;
		if (packages != null)
			for (int i = 0; i < packages.length; i++)
				((VersionConstraintImpl) packages[i]).setBundle(this);
	}
	public void setProvidedPackages(String[] providedPackages) {
		this.providedPackages = providedPackages;
	}
	public void setRequiredBundles(BundleSpecification[] requiredBundles) {
		this.requiredBundles = requiredBundles;
		if (requiredBundles != null)
			for (int i = 0; i < requiredBundles.length; i++)
				((VersionConstraintImpl) requiredBundles[i]).setBundle(this);				
	}
	public void setState(int state) {
		this.state = state;
	}
	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}
	public void setVersion(Version version) {
		this.version = version;
	}
	public PackageSpecification[] getPackages() {
		if (packages == null)
			return new PackageSpecification[0];
		return packages;
	}
	public String[] getProvidedPackages() {
		if (providedPackages == null)
			return new String[0];
		return providedPackages;
	}
	public BundleSpecification[] getRequiredBundles() {
		if (requiredBundles == null)
			return new BundleSpecification[0];
		return requiredBundles;
	}
	public BundleSpecification getRequiredBundle(String name) {
		if (requiredBundles == null)
			return null;
		for (int i = 0; i < requiredBundles.length; i++)
			if (requiredBundles[i].getName().equals(name))
				return requiredBundles[i];
		return null;
	}
	public String getUniqueId() {
		return uniqueId;
	}
	public PackageSpecification getPackage(String name) {
		if (packages == null)
			return null;
		for (int i = 0; i < packages.length; i++)
			if (packages[i].getName().equals(name))
				return packages[i];
		return null;
	}
	public String toString() {
		return getUniqueId() + "_" + getVersion();
	}
	public long getBundleId() {
		return bundleId;
	}
	public void setBundleId(long bundleId) {
		this.bundleId = bundleId;
	}
	public Object getKey() {
		return new Long(bundleId);
	}
	public boolean compare(KeyedElement other) {
		if (!(other instanceof BundleDescriptionImpl))
			return false;
		BundleDescriptionImpl otherBundleDescription = (BundleDescriptionImpl) other;
		return bundleId == otherBundleDescription.bundleId;
	}
	public int getKeyHashCode() {
		return (int) (bundleId % Integer.MAX_VALUE);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.osgi.service.resolver.BundleDescription#getUnsatisfiedConstraints
	 */
	public VersionConstraint[] getUnsatisfiedConstraints() {
		if (containingState == null)
			// it is a bug in the client to call this method when not attached to a state
			throw new IllegalStateException("Does not belong to a state"); //$NON-NLS-1$		
		ArrayList unsatisfied = new ArrayList(); 
		if (host != null && !host.isResolved() && !isResolvable(host))
			unsatisfied.add(host);		
		if (requiredBundles != null)
			for (int i = 0; i < requiredBundles.length; i++)
				if (!requiredBundles[i].isResolved() && !isResolvable(requiredBundles[i]))
					unsatisfied.add(requiredBundles[i]);
		if (packages != null)
			for (int i = 0; i < packages.length; i++)
				if (!packages[i].isResolved() && !isResolvable(packages[i]))
					unsatisfied.add(packages[i]);			
		return (VersionConstraint[]) unsatisfied.toArray(new VersionConstraint[unsatisfied.size()]);
	}
	/**
	 * @param specification
	 * @return
	 */
	private boolean isResolvable(PackageSpecification specification) {
		if (specification.isExported())
			return true;
		PackageSpecification exported = ((StateImpl) containingState).getExportedPackage(specification.getName(), null);
		if (exported == null)
			return false;
		return specification.isSatisfiedBy(exported.getVersionSpecification());		
	}
	private boolean isResolvable(VersionConstraint specification) {		
		BundleDescription[] availableBundles = containingState.getBundles(specification.getName());
		for (int i = 0; i < availableBundles.length; i++)
			if (availableBundles[i].isResolved() && specification.isSatisfiedBy(availableBundles[i].getVersion()))
				return true;
		return false;
	}
	public Object getUserObject() {
		return userObject;
	}
	public void setUserObject(Object userObject) {
		this.userObject = userObject;
	}
	public int hashCode() {		
		if (uniqueId == null)
			return (int) (bundleId % Integer.MAX_VALUE);
		return (int) ((bundleId * (uniqueId.hashCode())) % Integer.MAX_VALUE);
	}
	public boolean equals(Object object) {
		if (!(object instanceof BundleDescription))
			return false;
		BundleDescription other = (BundleDescription) object;
		return this.bundleId == other.getBundleId() && (this.uniqueId == null & other.getUniqueId() == null || this.uniqueId != null && this.uniqueId.equals(other.getUniqueId()));
	}
}