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

import org.osgi.framework.Bundle;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.startlevel.StartLevel;

@SuppressWarnings("deprecation")
public class StartLevelImpl implements StartLevel {

	private final FrameworkStartLevel frameworkStartLevel;

	public StartLevelImpl(FrameworkStartLevel frameworkStartLevel) {
		this.frameworkStartLevel = frameworkStartLevel;
	}

	@Override
	public int getStartLevel() {
		return frameworkStartLevel.getStartLevel();
	}

	@Override
	public void setStartLevel(int startlevel) {
		frameworkStartLevel.setStartLevel(startlevel);
	}

	@Override
	public int getBundleStartLevel(Bundle bundle) {
		return bundle.adapt(BundleStartLevel.class).getStartLevel();
	}

	@Override
	public void setBundleStartLevel(Bundle bundle, int startlevel) {
		bundle.adapt(BundleStartLevel.class).setStartLevel(startlevel);
	}

	@Override
	public int getInitialBundleStartLevel() {
		return frameworkStartLevel.getInitialBundleStartLevel();
	}

	@Override
	public void setInitialBundleStartLevel(int startlevel) {
		frameworkStartLevel.setInitialBundleStartLevel(startlevel);
	}

	@Override
	public boolean isBundlePersistentlyStarted(Bundle bundle) {
		return bundle.adapt(BundleStartLevel.class).isPersistentlyStarted();
	}

	@Override
	public boolean isBundleActivationPolicyUsed(Bundle bundle) {
		return bundle.adapt(BundleStartLevel.class).isActivationPolicyUsed();
	}
}
