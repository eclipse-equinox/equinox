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
package org.eclipse.core.runtime.adaptor;

import org.eclipse.osgi.internal.resolver.BundleInstaller;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Internal class.
 */
//TODO: minimal implementation for now. This could be smarter
public class EclipseBundleInstaller implements BundleInstaller {
	public void installBundle(BundleDescription toInstall) throws BundleException {
		EclipseAdaptor.getDefault().getContext().installBundle(toInstall.getLocation());
	}

	public void uninstallBundle(BundleDescription toUninstallId) throws BundleException {
		Bundle toUninstall = EclipseAdaptor.getDefault().getContext().getBundle(toUninstallId.getBundleId());
		if (toUninstall != null)
			toUninstall.uninstall();
	}

	public void updateBundle(BundleDescription toUpdateId) throws BundleException {
		Bundle toUpdate = EclipseAdaptor.getDefault().getContext().getBundle(toUpdateId.getBundleId());
		if (toUpdate != null)
			toUpdate.update();
	}
}
