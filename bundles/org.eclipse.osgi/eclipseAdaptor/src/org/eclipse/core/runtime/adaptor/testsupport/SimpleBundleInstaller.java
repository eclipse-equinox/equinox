/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor.testsupport;

import org.eclipse.osgi.internal.resolver.BundleInstaller;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.osgi.framework.BundleException;

public class SimpleBundleInstaller implements BundleInstaller {
	private State state;

	public SimpleBundleInstaller(State state) {
		this.state = state;
	}

	public void installBundle(BundleDescription toInstall) throws BundleException {
		state.addBundle(toInstall);
	}

	public void uninstallBundle(BundleDescription toUninstall) throws BundleException {
		state.removeBundle(toUninstall);
	}

	public void updateBundle(BundleDescription toUpdate) throws BundleException {
		//TODO
	}
}
