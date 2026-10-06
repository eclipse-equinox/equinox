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

import java.util.List;
import java.util.Map;

import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainerAdaptor;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

public class ServiceLoaderMediatorHookConfigurator implements BundleActivator {
	private ServiceLoaderMediatorHook mediatorHook;
	private ServiceRegistration<ClassLoaderHook> hookRegistration;
	private ServiceRegistration<DebugOptionsListener> debugListenerRegistration;

	@Override
	public void start(BundleContext context) {
		Tracing tracing = new Tracing();
		debugListenerRegistration = context.registerService(DebugOptionsListener.class, tracing,
				FrameworkUtil.asDictionary(Map.of(DebugOptions.LISTENER_SYMBOLICNAME, Tracing.NAME)));

		mediatorHook = new ServiceLoaderMediatorHook(context, tracing);
		hookRegistration = context.registerService(ClassLoaderHook.class, mediatorHook, null);
	}

	@Override
	public void stop(BundleContext context) {
		hookRegistration.unregister();
		hookRegistration = null;
		mediatorHook.stop();
		mediatorHook = null;

		debugListenerRegistration.unregister();
	}

	static class Tracing implements DebugOptionsListener {
		private static final String NAME = "org.eclipse.equinox.spi"; //$NON-NLS-1$

		private static final String OPTION_DEBUG_REGISTRATIONS = NAME + "/debug/registrations"; //$NON-NLS-1$
		private static volatile boolean DEBUG_REGISTRATIONS = false;

		@Override
		public void optionsChanged(DebugOptions options) {
			DEBUG_REGISTRATIONS = options.getBooleanOption(OPTION_DEBUG_REGISTRATIONS, false);
		}

		void debugRegistrations(String operation, Map<String, List<String>> services, Bundle bundle) {
			if (DEBUG_REGISTRATIONS) {
				ModuleContainerAdaptor adaptor = bundle.adapt(Module.class).getContainer().getAdaptor();
				services.forEach((serviceType, providers) -> {
					adaptor.trace(OPTION_DEBUG_REGISTRATIONS,
							operation + " providers for service '" + serviceType + "' (from bundle " + bundle //$NON-NLS-1$ //$NON-NLS-2$
							+ "): " + providers); //$NON-NLS-1$
				});
			}
		}

	}

}
