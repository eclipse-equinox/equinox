/*******************************************************************************
 * Copyright (c) 2023, 2026 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.spi.internal;

import org.eclipse.osgi.internal.hookregistry.HookConfigurator;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ServiceLoaderMediatorHookConfigurator implements HookConfigurator {
	private static ServiceLoaderMediatorHook mediatorHook;

	@Override
	public void addHooks(HookRegistry hookRegistry) {
		ServiceLoaderMediatorHook hook = new ServiceLoaderMediatorHook();
		hookRegistry.addClassLoaderHook(hook);
		mediatorHook = hook;
	}

	static Bundle spiExtensionBundle;

	public static class Activator implements BundleActivator {

		@Override
		public void start(BundleContext context) throws Exception {
			mediatorHook.start(context);
		}

		@Override
		public void stop(BundleContext context) throws Exception {
			mediatorHook.stop();
			spiExtensionBundle = null;
		}

	}

}
