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

import java.util.*;
import org.eclipse.core.internal.dependencies.*;
import org.eclipse.osgi.service.resolver.*;

public class ResolverHelper {

	private final static Version NULL_VERSION = new Version(0, 0, 0);
	private final static IMatchRule GENERAL_MATCHRULE = new GeneralMatchRule();

	static class BundleVersionComparator implements Comparator {
		public int compare(Object arg0, Object arg1) {
			Version v1 = (Version) arg0;
			Version v2 = (Version) arg1;
			return v1.isGreaterThan(v2) ? 1 : v1.equals(v2) ? 0 : -1;
		}
	}

	private final static class GeneralMatchRule implements IMatchRule {
		public boolean isSatisfied(Object constraint, Object available) {
			return ((VersionConstraint) constraint).isSatisfiedBy((Version) available);
		}

		public String toString() {
			return "general"; //$NON-NLS-1$
		}
	}

	public static Element createElement(BundleDescription bundleDescription, DependencySystem system) {
		String name = getSymbolicName(bundleDescription);
		Version version = getVersion(bundleDescription);
		return system.createElement(name, version, createPrerequisites(bundleDescription, system), bundleDescription.isSingleton(), bundleDescription);
	}

	private static Version getVersion(BundleDescription bundleDescription) {
		Version version = bundleDescription.getVersion();
		if (version == null)
			version = Version.emptyVersion;
		return version;
	}

	private static String getSymbolicName(BundleDescription bundleDescription) {
		String name = bundleDescription.getSymbolicName();
		// TODO do we still need to return the number as the name if there is no name?
		if (name == null)
			// could not be null
			name = Long.toString(bundleDescription.getBundleId());
		return name;
	}

	private static Dependency[] createPrerequisites(BundleDescription bundleDesc, DependencySystem system) {
		BundleSpecification[] required = bundleDesc.getRequiredBundles();
		HostSpecification host = bundleDesc.getHost();
		int dependencyCount = required == null ? 0 : required.length;
		if (host != null)
			dependencyCount++;
		if (dependencyCount == 0)
			return new Dependency[0];
		List prereqs = new ArrayList(dependencyCount);
		for (int i = 0; i < required.length; i++)
			// ignore if a bundle requires itself (bug 48568 comment 2)		
			if (!required[i].getName().equals(bundleDesc.getSymbolicName()))
				prereqs.add(createPrerequisite(system, required[i]));
		if (host != null)
			prereqs.add(createPrerequisite(system, host));
		return (Dependency[]) prereqs.toArray(new Dependency[prereqs.size()]);
	}

	private static Dependency createPrerequisite(DependencySystem system, VersionConstraint constraint) {
		boolean optional = (constraint instanceof BundleSpecification) && ((BundleSpecification) constraint).isOptional();
		return system.createDependency(constraint.getName(), GENERAL_MATCHRULE, optional, constraint);
	}

	public static DependencySystem createDependencySystem(ISelectionPolicy policy) {
		return new DependencySystem(new ResolverHelper.BundleVersionComparator(), policy);
	}

	public static DependencySystem buildDependencySystem(State state, ISelectionPolicy selectionPolicy) {
		DependencySystem dependencySystem = createDependencySystem(selectionPolicy);
		BundleDescription[] bundles = state.getBundles();
		for (int i = 0; i < bundles.length; i++)
			dependencySystem.addElement(ResolverHelper.createElement(bundles[i], dependencySystem));
		return dependencySystem;
	}

	public static void remove(BundleDescription description, DependencySystem system) {
		system.removeElement(getSymbolicName(description), getVersion(description));
	}

	public static void add(BundleDescription description, DependencySystem system) {
		system.addElement(createElement(description, system));
	}

	public static void unresolve(BundleDescription bundle, DependencySystem system) {
		Element element = system.getElement(getSymbolicName(bundle), getVersion(bundle));
		if (element != null)
			system.unresolve(new Element[] {element});
	}

	public static void update(BundleDescription newDescription, BundleDescription existing, DependencySystem system) {
		system.removeElement(getSymbolicName(existing), getVersion(existing));
		system.addElement(createElement(newDescription, system));
	}
}