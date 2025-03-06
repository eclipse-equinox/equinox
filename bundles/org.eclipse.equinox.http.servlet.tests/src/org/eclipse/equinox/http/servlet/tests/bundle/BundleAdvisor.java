/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests.bundle;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/*
 * The BundleAdvisor is responsible for starting and stopping bundles, given
 * a Bundle-SymbolicName. It is an OSGi dependent class.
 */
public class BundleAdvisor extends Object {
	private final BundleContext bundleContext;

	public BundleAdvisor(BundleContext bundleContext) {
		super();
		if (bundleContext == null) {
			throw new IllegalArgumentException("bundleContext must not be null"); //$NON-NLS-1$
		}
		this.bundleContext = bundleContext;
	}

	private Bundle getBundle(String symbolicName) {
		if (symbolicName == null) {
			throw new IllegalArgumentException("symbolicName must not be null"); //$NON-NLS-1$
		}
		BundleContext context = getBundleContext();
		Bundle[] bundles = context.getBundles();
		for (Bundle bundle : bundles) {
			String bsn = bundle.getSymbolicName();
			boolean match = symbolicName.equals(bsn);
			if (match) {
				return bundle;
			}
		}
		throw new IllegalArgumentException("Failed to find bundle: " + symbolicName); //$NON-NLS-1$
	}

	private BundleContext getBundleContext() {
		return bundleContext;
	}

	public void startBundle(String symbolicName) throws BundleException {
		Bundle bundle = getBundle(symbolicName);
		int state = bundle.getState();
		if (state == Bundle.ACTIVE) {
			return;
		}
		bundle.start(Bundle.START_TRANSIENT);
	}

	public void stopBundle(String symbolicName) throws BundleException {
		Bundle bundle = getBundle(symbolicName);
		bundle.stop();
	}
}
