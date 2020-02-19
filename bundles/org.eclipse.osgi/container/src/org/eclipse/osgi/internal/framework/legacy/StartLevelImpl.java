/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package org.eclipse.osgi.internal.framework.legacy;

import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.osgi.framework.Bundle;
import org.osgi.service.startlevel.StartLevel;

@SuppressWarnings("deprecation")
public class StartLevelImpl implements StartLevel {

	private final ModuleContainer container;

	public StartLevelImpl(ModuleContainer container) {
		this.container = container;
	}

	@Override
	public int getStartLevel() {
		return container.getFrameworkStartLevel().getStartLevel();
	}

	@Override
	public void setStartLevel(int startlevel) {
		container.getFrameworkStartLevel().setStartLevel(startlevel);
	}

	@Override
	public int getBundleStartLevel(Bundle bundle) {
		return getModule(bundle).getStartLevel();
	}

	@Override
	public void setBundleStartLevel(Bundle bundle, int startlevel) {
		getModule(bundle).setStartLevel(startlevel);
	}

	@Override
	public int getInitialBundleStartLevel() {
		return container.getFrameworkStartLevel().getInitialBundleStartLevel();
	}

	@Override
	public void setInitialBundleStartLevel(int startlevel) {
		container.getFrameworkStartLevel().setInitialBundleStartLevel(startlevel);
	}

	@Override
	public boolean isBundlePersistentlyStarted(Bundle bundle) {
		return getModule(bundle).isPersistentlyStarted();
	}

	@Override
	public boolean isBundleActivationPolicyUsed(Bundle bundle) {
		return getModule(bundle).isActivationPolicyUsed();
	}

	static Module getModule(Bundle bundle) {
		if (bundle instanceof EquinoxBundle) {
			return ((EquinoxBundle) bundle).getModule();
		}
		throw new IllegalArgumentException("Bundle is not from an equinox framework: " + bundle.getClass()); //$NON-NLS-1$
	}
}
