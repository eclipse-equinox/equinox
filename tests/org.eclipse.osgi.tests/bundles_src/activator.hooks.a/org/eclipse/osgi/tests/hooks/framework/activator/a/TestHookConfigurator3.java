/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
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
package org.eclipse.osgi.tests.hooks.framework.activator.a;

import java.util.List;
import org.eclipse.osgi.internal.hookregistry.HookConfigurator;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class TestHookConfigurator3 implements HookConfigurator {
	public static volatile List<String> events;

	@Override
	public void addHooks(HookRegistry hookRegistry) {
		hookRegistry.addActivatorHookFactory(() -> new BundleActivator() {
			@Override
			public void start(BundleContext context) throws Exception {
				if (events != null) {
					events.add("HOOK3 STARTED");
				}
			}

			@Override
			public void stop(BundleContext context) throws Exception {
				if (events != null) {
					events.add("HOOK3 STOPPED");
				}
			}
		});

	}
}
