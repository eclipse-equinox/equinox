/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleException;

public class ReadOnlyState implements State {
	private State target;

	public ReadOnlyState(State target) {
		this.target = target;
	}

	public boolean addBundle(BundleDescription description) {
		throw new UnsupportedOperationException();
	}

	public boolean updateBundle(BundleDescription newDescription) {
		throw new UnsupportedOperationException();
	}

	public BundleDescription removeBundle(long bundleId) {
		throw new UnsupportedOperationException();
	}

	public boolean removeBundle(BundleDescription bundle) {
		throw new UnsupportedOperationException();
	}

	public StateDelta compare(State state) throws BundleException {
		return target.compare(state);
	}

	public StateDelta getChanges() {
		return target.getChanges();
	}

	public BundleDescription[] getBundles() {
		return target.getBundles();
	}

	public BundleDescription getBundle(long id) {
		return target.getBundle(id);
	}

	public BundleDescription getBundle(String symbolicName, Version version) {
		return target.getBundle(symbolicName, version);
	}

	public long getTimeStamp() {
		return target.getTimeStamp();
	}

	public boolean isResolved() {
		return target.isResolved();
	}

	public void resolveConstraint(VersionConstraint constraint, Version actualVersion, BundleDescription supplier) {
		throw new UnsupportedOperationException();
	}

	public void resolveBundle(BundleDescription bundle, int status) {
		throw new UnsupportedOperationException();
	}

	public Resolver getResolver() {
		return null;
	}

	public void setResolver(Resolver value) {
		throw new UnsupportedOperationException();
	}

	public StateDelta resolve(boolean incremental) {
		throw new UnsupportedOperationException();
	}

	public StateDelta resolve() {
		throw new UnsupportedOperationException();
	}

	public StateDelta resolve(BundleDescription[] discard) {
		throw new UnsupportedOperationException();
	}

	public void setOverrides(Object value) {
		throw new UnsupportedOperationException();
	}

	public BundleDescription[] getResolvedBundles() {
		return target.getResolvedBundles();
	}

	public void addStateChangeListener(StateChangeListener listener, int flags) {
		throw new UnsupportedOperationException();
	}

	public void removeStateChangeListener(StateChangeListener listener) {
		throw new UnsupportedOperationException();
	}

	public boolean isEmpty() {
		return target.isEmpty();
	}

	public PackageSpecification[] getExportedPackages() {
		return target.getExportedPackages();
	}

	public BundleDescription[] getBundles(String symbolicName) {
		return target.getBundles(symbolicName);
	}

	public StateObjectFactory getFactory() {
		return target.getFactory();
	}
}