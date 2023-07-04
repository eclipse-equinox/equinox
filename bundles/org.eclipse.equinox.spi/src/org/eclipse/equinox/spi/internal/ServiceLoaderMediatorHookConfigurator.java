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

import static org.eclipse.osgi.internal.debug.Debug.OPTION_DEBUG_LOADER_CDS;

import java.util.List;
import java.util.Map;

import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.hookregistry.HookConfigurator;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

public class ServiceLoaderMediatorHookConfigurator implements HookConfigurator {
	private static ServiceLoaderMediatorHook mediatorHook;
	static Tracing tracing;

	@Override
	public void addHooks(HookRegistry hookRegistry) {
		tracing = new Tracing(hookRegistry.getConfiguration());

		ServiceLoaderMediatorHook hook = new ServiceLoaderMediatorHook();
		hookRegistry.addClassLoaderHook(hook);
		mediatorHook = hook;
	}

	public static class Activator implements BundleActivator {
		private ServiceRegistration<DebugOptionsListener> tracingRegistration;

		@Override
		public void start(BundleContext context) throws Exception {
			if (mediatorHook != null) { // Is null, if not added to osgi.framework.extensions path

				Map<String, String> properties = Map.of(DebugOptions.LISTENER_SYMBOLICNAME, Tracing.NAME);
				tracingRegistration = context.registerService(DebugOptionsListener.class, tracing,
						FrameworkUtil.asDictionary(properties));

				mediatorHook.start(context);
			}
		}

		@Override
		public void stop(BundleContext context) throws Exception {
			if (mediatorHook != null) {
				mediatorHook.stop();

				tracingRegistration.unregister();
				tracingRegistration = null;
			}
		}
	}

	static class Tracing implements DebugOptionsListener {
		private static final String NAME = "org.eclipse.equinox.spi"; //$NON-NLS-1$

		private static final String OPTION_DEBUG_REGISTRATIONS = NAME + "/debug/registrations"; //$NON-NLS-1$
		private static volatile boolean DEBUG_REGISTRATIONS = false;

		private final Debug debug;

		public Tracing(EquinoxConfiguration configuration) {
			debug = configuration.getDebug();
			optionsChanged(configuration.getDebugOptions());
		}

		@Override
		public void optionsChanged(DebugOptions options) {
			DEBUG_REGISTRATIONS = options.getBooleanOption(OPTION_DEBUG_REGISTRATIONS, false);
		}

		void debugRegistrations(String operation, Map<String, List<String>> services, Bundle bundle) {
			if (ServiceLoaderMediatorHookConfigurator.Tracing.DEBUG_REGISTRATIONS) {
				services.forEach((serviceType, providers) -> {
					debug.trace(OPTION_DEBUG_LOADER_CDS,
							operation + " providers for service '" + serviceType + "' (from bundle " + bundle //$NON-NLS-1$ //$NON-NLS-2$
							+ "): " + providers); //$NON-NLS-1$
				});
			}
		}

	}

}
