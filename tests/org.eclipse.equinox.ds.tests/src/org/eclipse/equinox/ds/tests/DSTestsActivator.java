/*******************************************************************************
 * Copyright (c) 1997-2009 by ProSyst Software GmbH
 * http://www.prosyst.com
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.tests;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class DSTestsActivator implements BundleActivator {

	private static DSTestsActivator instance;
	private BundleContext context;
	
	public DSTestsActivator() {
		instance = this;
	}
	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		this.context = null;
	}

	public static BundleContext getContext() {
		return instance != null ? instance.context : null;
	}
	
	public static void activateSCR() {
		activateBundle("org.apache.felix.scr");
		activateBundle("org.eclipse.equinox.cm");
		activateBundle("org.eclipse.equinox.log");
	}
	
	private static void activateBundle(String symbolicName) {
		if (instance != null) {
			Bundle[] bundles = instance.context.getBundles();
			for (Bundle bundle : bundles) {
				if (symbolicName.equals(bundle.getSymbolicName())) {
					if (bundle.getState() != Bundle.ACTIVE) {
						try {
							bundle.start(Bundle.START_TRANSIENT);
						}catch (BundleException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
}
