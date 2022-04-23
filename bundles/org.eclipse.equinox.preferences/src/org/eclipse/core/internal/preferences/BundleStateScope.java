/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.preferences;

import org.eclipse.core.internal.runtime.MetaDataKeeper;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.*;
import org.osgi.framework.Bundle;

/**
 * The Bundle State Scope supply bundles with two items:
 * <ul>
 * <li>{@link #getNode(String)} returns a node for the bundle workspace
 * preferences</li>
 * <li>{@link #getLocation()} returns the state location of the bundle that
 * acquires the service</li>
 * </ul>
 * The service could be acquired for example with declarative services the
 * following way:
 *
 * <pre>
 * @Reference(target = "(type=bundle)")
 * private IScopeContext scopeContext;
 * </pre>
 */
public class BundleStateScope implements IScopeContext {
	private IPath bundleLocation;
	private final Bundle usingBundle;

	public BundleStateScope(Bundle usingBundle) {
		this.usingBundle = usingBundle;
	}

	@Override
	public String getName() {
		return InstanceScope.INSTANCE.getName() + "/" + usingBundle.getSymbolicName(); //$NON-NLS-1$
	}

	@Override
	public IEclipsePreferences getNode(String qualifier) {
		return (IEclipsePreferences) InstanceScope.INSTANCE.getNode(usingBundle.getSymbolicName()).node(qualifier);
	}

	@Override
	public synchronized IPath getLocation() {
		if (bundleLocation == null) {
			bundleLocation = MetaDataKeeper.getMetaArea().getStateLocation(usingBundle);
			bundleLocation.toFile().mkdirs();
		}
		return bundleLocation;
	}

}
