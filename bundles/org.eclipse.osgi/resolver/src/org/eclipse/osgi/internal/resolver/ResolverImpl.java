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
import org.osgi.framework.Bundle;

public class ResolverImpl implements Resolver {
	private State state;
	private DependencySystem dependencies;

	public void resolve(BundleDescription[] reRefresh) {
		// unresolving the given bundles will force them to be re-resolved 
		for (int i = 0; i < reRefresh.length; i++)
			unresolveBundle(reRefresh[i]);
		resolve();
	}

	/**
	 * TODO: need to devise a way to report problems (a la IStatus)
	 */
	public synchronized void resolve() {
		if (state == null)
			throw new IllegalStateException("RESOLVER_NO_STATE"); //$NON-NLS-1$
		if (dependencies == null)
			dependencies = ResolverHelper.buildDependencySystem(state, new Eclipse30SelectionPolicy());
		ResolutionDelta delta = null;
		// try resolving as many times as necessary to remove all cycles
		boolean success;
		do {
			success = true;
			try {
				delta = dependencies.resolve();
			} catch (DependencySystem.CyclicSystemException e) {
				success = false;
				Object[][] cycles = e.getCycles();
				// disable one of the element sets involved in the cycle 
				((ElementSet) cycles[0][0]).setEnabled(false);
			}
		} while (!success);
		processInnerDelta(delta);
		resolvePackages();
	}

	public void setState(State newState) {
		// to avoid infinite (mutual) recursion
		if (state == newState)
			return;
		// if it was linked to a previous state, unlink first
		if (state != null) {
			State oldState = state;
			state = null;
			oldState.setResolver(null);
		}
		state = newState;
		if (newState != null)
			state.setResolver(this);
		// forget any dependency state created before
		flush();
	}

	/*
	 * Applies changes in the constraint system to the state object.
	 */
	private void processInnerDelta(ResolutionDelta delta) {
		//	now apply changes reported in the inner delta to the state
		ElementChange[] changes = delta.getAllChanges();
		for (int i = 0; i < changes.length; i++) {
			Element element = changes[i].getElement();
			BundleDescription bundle = (BundleDescription) element.getUserObject();
			int kind = changes[i].getKind();
			if ((kind & ElementChange.RESOLVED) != 0) {
				state.resolveBundle(bundle, Bundle.RESOLVED);
				resolveConstraints(element, bundle);
			} else if ((kind & ElementChange.UNRESOLVED) != 0)
				state.resolveBundle(bundle, Bundle.INSTALLED);
			else if (kind == ElementChange.LINKAGE_CHANGED)
				resolveConstraints(element, bundle);
		}
	}

	private void resolveConstraints(Element element, BundleDescription bundle) {
		// tells the state that some of the constraints have
		// changed
		Dependency[] dependencies = element.getDependencies();
		for (int j = 0; j < dependencies.length; j++) {
			if (dependencies[j].getResolvedVersionId() == null)
				// an optional requisite that was not resolved
				continue;
			VersionConstraint constraint = (VersionConstraint) dependencies[j].getUserObject();
			Version actualVersion = (Version) dependencies[j].getResolvedVersionId();
			BundleDescription supplier = state.getBundle(constraint.getName(), actualVersion);
			state.resolveConstraint(constraint, actualVersion, supplier);
		}
	}

	public void bundleAdded(BundleDescription bundle) {
		if (dependencies == null)
			return;
		ResolverHelper.add(bundle, dependencies);
	}

	public void bundleRemoved(BundleDescription bundle) {
		if (dependencies == null)
			return;
		ResolverHelper.remove(bundle, dependencies);
	}

	public void bundleUpdated(BundleDescription newDescription, BundleDescription existingDescription) {
		if (dependencies == null)
			return;
		ResolverHelper.update(newDescription, existingDescription, dependencies);
	}

	public State getState() {
		return state;
	}

	public void flush() {
		dependencies = null;
	}

	/*
	 * Ensures that all currently resolved bundles have their import-package
	 * clauses satisfied.
	 */
	private boolean resolvePackages() {
		/* attempt to resolve the proposed bundles */
		Map availablePackages;
		boolean success;
		int tries = 0;
		do {
			tries++;
			success = true;
			BundleDescription[] initialBundles = state.getResolvedBundles();
			availablePackages = new HashMap(11);
			/* do all exports first */
			for (int i = 0; i < initialBundles.length; i++) {
				PackageSpecification[] required = initialBundles[i].getPackages();
				for (int j = 0; j < required.length; j++)
					// override previously exported package if any (could
					// preserve instead)
					if (required[j].isExported()) {
						Version toExport = required[j].getVersionRange().getMinimum();
						PackageSpecification existing = (PackageSpecification) availablePackages.get(required[j].getName());
						Version existingVersion = existing == null ? null : existing.getVersionRange().getMinimum();
						if (existingVersion == null || (toExport != null && toExport.isGreaterThan(existingVersion)))
							availablePackages.put(required[j].getName(), required[j]);
					}
			}
			/* now try to resolve imported packages */
			for (int i = 0; i < initialBundles.length; i++) {
				PackageSpecification[] required = initialBundles[i].getPackages();
				for (int j = 0; j < required.length; j++) {
					PackageSpecification exported = (PackageSpecification) availablePackages.get(required[j].getName());
					Version exportedVersion = exported == null ? null : exported.getVersionRange().getMinimum();
					if (exported == null || !required[j].isSatisfiedBy(exportedVersion)) {
						unresolveRequirementChain(initialBundles[i]);
						success = false;
						// one missing import is enough to discard this bundle
						break;
					}
				}
			}
		} while (!success);
		/* now bind the exports/imports */
		BundleDescription[] resolvedBundles = state.getResolvedBundles();
		for (int i = 0; i < resolvedBundles.length; i++) {
			PackageSpecification[] required = resolvedBundles[i].getPackages();
			for (int j = 0; j < required.length; j++) {
				PackageSpecification exported = (PackageSpecification) availablePackages.get(required[j].getName());
				state.resolveConstraint(required[j], exported.getVersionRange().getMinimum(), exported.getBundle());
			}
		}
		/* return false if a at least one bundle was unresolved during this */
		return tries > 1;
	}

	/*
	 * Unresolves a bundle and all bundles that require it.
	 */
	private void unresolveRequirementChain(BundleDescription bundle) {
		if (!bundle.isResolved())
			return;
		state.resolveBundle(bundle, Bundle.INSTALLED);
		if (bundle.getSymbolicName() == null)
			return;
		ElementSet bundleElementSet = dependencies.getElementSet(bundle.getSymbolicName());
		Collection requiring = bundleElementSet.getRequiringElements(bundle.getVersion());
		for (Iterator requiringIter = requiring.iterator(); requiringIter.hasNext();) {
			Element requiringElement = (Element) requiringIter.next();
			BundleDescription requiringBundle = state.getBundle((String) requiringElement.getId(), (Version) requiringElement.getVersionId());
			if (requiringBundle != null)
				unresolveRequirementChain(requiringBundle);
		}
	}

	private void unresolveBundle(BundleDescription bundle) {
		if (!bundle.isResolved())
			return;
		if (dependencies != null)
			ResolverHelper.unresolve(bundle, dependencies);
	}
}