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
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Bundle;

public class ResolverImpl implements Resolver {
	private State state;
	private IDependencySystem dependencies;
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
		IResolutionDelta delta;
		try {
			delta = dependencies.resolve();
		} catch (IDependencySystem.CyclicSystemException e) {
			//TODO: this should be logged instead
			e.printStackTrace();
			return;
		}
		processInnerDelta(delta);
		resolvePackages();
	}
	public void setState(State newState) {
		// to avoid infinite (mutual) recursion
		if (state == newState)
			return;
		// if it was linked to a previous state, unlink first
		if (state != null)
			state.setResolver(null);
		state = newState;
		if (newState != null)
			state.setResolver(this);
		// forget any dependency state created before
		flush();
	}
	/*
	 * Applies changes in the constraint system to the state object.
	 */
	private void processInnerDelta(IResolutionDelta delta) {
		//	now apply changes reported in the inner delta to the state
		IElementChange[] changes = delta.getAllChanges();
		for (int i = 0; i < changes.length; i++) {
			IElement element = changes[i].getElement();
			BundleDescription bundle =  (BundleDescription) element.getUserObject();	
			int kind = changes[i].getKind();
			if ((kind & IElementChange.RESOLVED) != 0) {
				state.resolveBundle(bundle, Bundle.RESOLVED);
				resolveConstraints(element, bundle);
			} else if ((kind & IElementChange.UNRESOLVED) != 0)
				state.resolveBundle(bundle, Bundle.INSTALLED);
			else if (kind == IElementChange.LINKAGE_CHANGED)
				resolveConstraints(element, bundle);
		}
	}
	private void resolveConstraints(IElement element, BundleDescription bundle) {
		// tells the state that some of the constraints have
		// changed
		IDependency[] dependencies = element.getDependencies();
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
						Version toExport = required[j].getVersionSpecification();
						PackageSpecification existing = (PackageSpecification) availablePackages.get(required[j].getName());
						Version existingVersion = existing == null ? null : existing.getVersionSpecification();
						if (existingVersion == null || (toExport != null && toExport.isGreaterThan(existingVersion)))
							availablePackages.put(required[j].getName(), required[j]);
					}
			}
			/* now try to resolve imported packages */
			for (int i = 0; i < initialBundles.length; i++) {
				PackageSpecification[] required = initialBundles[i].getPackages();
				for (int j = 0; j < required.length; j++) {
					PackageSpecification exported = (PackageSpecification) availablePackages.get(required[j].getName());
					Version exportedVersion = exported == null ? null : exported.getVersionSpecification();
					Version importedVersion = required[j].getVersionSpecification();
					if (exported == null || (importedVersion != null && (exportedVersion == null || !exportedVersion.matchGreaterOrEqualTo(importedVersion)))) {
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
				state.resolveConstraint(required[j], exported.getVersionSpecification(), exported.getBundle());
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
		if (bundle.getUniqueId() == null)
			return;
		IElementSet bundleElementSet = dependencies.getElementSet(bundle.getUniqueId());
		Collection requiring = bundleElementSet.getRequiringElements(bundle.getVersion());
		for (Iterator requiringIter = requiring.iterator(); requiringIter.hasNext(); ) {
			IElement requiringElement = (IElement) requiringIter.next();
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