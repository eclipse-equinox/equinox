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
import org.eclipse.core.dependencies.*;
import org.eclipse.core.internal.dependencies.DependencySystem;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Constants;

public class ResolverHelper {

	private final static Version NULL_VERSION = new Version(0, 0, 0);

	static class BundleVersionComparator implements Comparator {
		public int compare(Object arg0, Object arg1) {
			Version v1 = (Version) arg0;
			Version v2 = (Version) arg1;
			return v1.isGreaterThan(v2) ? 1 : v1.matchQualifier(v2) ? 0 : -1;
		}
	}

	private static final IMatchRule MAJOR = new MatchMajorRule();
	private static final IMatchRule MINOR = new MatchMinorRule();
	private static final IMatchRule MICRO = new MatchMicroRule();
	private static final IMatchRule GREATER_OR_EQUAL = new MatchGreaterOrEqualRule();
	private static final IMatchRule QUALIFIER = new MatchQualifierRule();

	public static IMatchRule getMatchRule(int b) {
		switch (b) {
			case VersionConstraint.MINOR_MATCH :
				return MINOR;
			case VersionConstraint.MICRO_MATCH :
				return MICRO;
			case VersionConstraint.GREATER_EQUAL_MATCH :
				return GREATER_OR_EQUAL;
			case VersionConstraint.QUALIFIER_MATCH :
				return QUALIFIER;
			case VersionConstraint.MAJOR_MATCH :
				return MAJOR;
			case VersionConstraint.NO_MATCH :
				return MAJOR;
		}
		throw new IllegalArgumentException("match byte: " + b); //$NON-NLS-1$
	}

	private final static class UnsatisfiableRule implements IMatchRule {
		public boolean isSatisfied(Object required, Object available) {
			return false;
		}

		public String toString() {
			return "unsatisfiable"; //$NON-NLS-1$
		}
	}

	private final static class MatchQualifierRule implements IMatchRule {
		public boolean isSatisfied(Object required, Object available) {
			return ((Version) available).matchQualifier((Version) required);
		}

		public String toString() {
			return Constants.VERSION_MATCH_QUALIFIER;
		}
	}

	private final static class MatchMajorRule implements IMatchRule {
		public boolean isSatisfied(Object required, Object available) {
			return ((Version) available).matchMajor((Version) required);
		}

		public String toString() {
			return Constants.VERSION_MATCH_MAJOR;
		}
	}

	private final static class MatchGreaterOrEqualRule implements IMatchRule {
		public boolean isSatisfied(Object required, Object available) {
			return ((Version) available).matchGreaterOrEqualTo((Version) required);
		}

		public String toString() {
			return Constants.VERSION_MATCH_GREATERTHANOREQUAL;
		}
	}

	private final static class MatchMinorRule implements IMatchRule {
		public boolean isSatisfied(Object required, Object available) {
			return ((Version) available).matchMinor((Version) required);
		}

		public String toString() {
			return Constants.VERSION_MATCH_MINOR;
		}
	}

	private final static class MatchMicroRule implements IMatchRule {
		public boolean isSatisfied(Object required, Object available) {
			return ((Version) available).matchMicro((Version) required);
		}

		public String toString() {
			return Constants.VERSION_MATCH_MICRO;
		}
	}

	public static IElement createElement(BundleDescription bundleDescription, IDependencySystem system) {
		String uniqueId = getUniqueId(bundleDescription);
		Version version = getVersion(bundleDescription);
		return system.createElement(uniqueId, version, createPrerequisites(bundleDescription, system), bundleDescription.isSingleton(), bundleDescription);
	}

	private static Version getVersion(BundleDescription bundleDescription) {
		Version version = bundleDescription.getVersion();
		if (version == null)
			version = Version.emptyVersion;
		return version;
	}

	private static String getUniqueId(BundleDescription bundleDescription) {
		String uniqueId = bundleDescription.getUniqueId();
		if (uniqueId == null)
			// could not be null
			uniqueId = Long.toString(bundleDescription.getBundleId());
		return uniqueId;
	}

	private static IDependency[] createPrerequisites(BundleDescription bundleDesc, IDependencySystem system) {
		BundleSpecification[] required = bundleDesc.getRequiredBundles();
		HostSpecification host = bundleDesc.getHost();
		int dependencyCount = required == null ? 0 : required.length;
		if (host != null)
			dependencyCount++;
		if (dependencyCount == 0)
			return new IDependency[0];
		List prereqs = new ArrayList(dependencyCount);
		for (int i = 0; i < required.length; i++)
			// ignore if a bundle requires itself (bug 48568 comment 2)		
			if (!required[i].getName().equals(bundleDesc.getUniqueId()))
				prereqs.add(createPrerequisite(system, required[i]));
		if (host != null)
			prereqs.add(createPrerequisite(system, host));
		return (IDependency[]) prereqs.toArray(new IDependency[prereqs.size()]);
	}

	private static IDependency createPrerequisite(IDependencySystem system, VersionConstraint constraint) {
		boolean optional = (constraint instanceof BundleSpecification) && ((BundleSpecification) constraint).isOptional();
		Version requiredVersion = constraint.getVersionSpecification();
		if (NULL_VERSION.equals(requiredVersion))
			requiredVersion = null;
		return system.createDependency(constraint.getName(), getMatchRule(constraint.getMatchingRule()), requiredVersion, optional, constraint);
	}

	public static IDependencySystem createDependencySystem(ISelectionPolicy policy) {
		return new DependencySystem(new ResolverHelper.BundleVersionComparator(), policy);
	}

	public static IDependencySystem buildDependencySystem(State state, ISelectionPolicy selectionPolicy) {
		IDependencySystem dependencySystem = createDependencySystem(selectionPolicy);
		BundleDescription[] bundles = state.getBundles();
		for (int i = 0; i < bundles.length; i++)
			dependencySystem.addElement(ResolverHelper.createElement(bundles[i], dependencySystem));
		return dependencySystem;
	}

	public static void remove(BundleDescription description, IDependencySystem system) {
		system.removeElement(getUniqueId(description), getVersion(description));
	}

	public static void add(BundleDescription description, IDependencySystem system) {
		system.addElement(createElement(description, system));
	}

	public static void unresolve(BundleDescription bundle, IDependencySystem system) {
		IElement element = system.getElement(getUniqueId(bundle), getVersion(bundle));
		if (element != null)
			system.unresolve(new IElement[] {element});
	}

	public static void update(BundleDescription newDescription, BundleDescription existing, IDependencySystem system) {
		system.removeElement(getUniqueId(existing), getVersion(existing));
		system.addElement(createElement(newDescription, system));
	}
}