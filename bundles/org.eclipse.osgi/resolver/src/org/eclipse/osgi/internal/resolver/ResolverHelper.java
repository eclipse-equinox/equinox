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

public class ResolverHelper {

	private final static Version NULL_VERSION = new Version(0, 0, 0);

	static class BundleVersionComparator implements Comparator {
		public int compare(Object arg0, Object arg1) {
			Version v1 = (Version) arg0;
			Version v2 = (Version) arg1;
			return v1.isGreaterThan(v2) ? 1 : v1.isPerfect(v2) ? 0 : -1;
		}
	}
	private static final IMatchRule COMPATIBLE = new EclipseCompatibleMatchRule();
	private static final IMatchRule EQUIVALENT = new EclipseEquivalentMatchRule();
	private static final IMatchRule GREATER_OR_EQUAL = new EclipseGreaterOrEqualMatchRule();
	private static final IMatchRule PERFECT = new EclipsePerfectMatchRule();

	public static IMatchRule getMatchRule(int b) {
		switch (b) {
			case VersionConstraint.EQUIVALENT_MATCH :
				return EQUIVALENT;
			case VersionConstraint.GREATER_EQUAL_MATCH :
				return GREATER_OR_EQUAL;
			case VersionConstraint.PERFECT_MATCH :
				return PERFECT;
			case VersionConstraint.COMPATIBLE_MATCH :
				return COMPATIBLE;
			case VersionConstraint.NO_MATCH :
				return COMPATIBLE;
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
	private final static class EclipsePerfectMatchRule implements IMatchRule {
		public boolean isSatisfied(Object required, Object available) {
			return ((Version) available).isPerfect((Version) required);
		}
		public String toString() {
			return "perfect"; //$NON-NLS-1$
		}
	}
	private final static class EclipseCompatibleMatchRule implements IMatchRule {
		public boolean isSatisfied(Object required, Object available) {
			return ((Version) available).isCompatibleWith((Version) required);
		}
		public String toString() {
			return "compatible"; //$NON-NLS-1$
		}
	}
	private final static class EclipseGreaterOrEqualMatchRule implements IMatchRule {
		public boolean isSatisfied(Object required, Object available) {
			return ((Version) available).isGreaterOrEqualTo((Version) required);
		}
		public String toString() {
			return "greaterOrEqual"; //$NON-NLS-1$
		}
	}
	private final static class EclipseEquivalentMatchRule implements IMatchRule {
		public boolean isSatisfied(Object required, Object available) {
			return ((Version) available).isEquivalentTo((Version) required);
		}
		public String toString() {
			return "equivalent"; //$NON-NLS-1$
		}
	}
	public static IElement createElement(BundleDescription bundleDescription, IDependencySystem system) {
		String uniqueId = getUniqueId(bundleDescription);
		Version version = getVersion(bundleDescription);
		return system.createElement(uniqueId, version, createPrerequisites(bundleDescription, system), true, bundleDescription);
	}
	private static Version getVersion(BundleDescription bundleDescription) {
		Version version = bundleDescription.getVersion();
		if (version == null)
			version = Version.EMPTY_VERSION;
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
			system.unresolve(new IElement[]{element});
	}
}