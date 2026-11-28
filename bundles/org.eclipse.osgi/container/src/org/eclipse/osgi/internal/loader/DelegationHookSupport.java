/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.osgi.internal.loader;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.serviceregistry.HookContext;
import org.eclipse.osgi.internal.serviceregistry.ServiceRegistry;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.delegation.DelegationHook;

/**
 * Calls the registered {@link DelegationHook} services on behalf of a
 * {@link BundleLoader}.
 */
public class DelegationHookSupport {
	private final ServiceRegistry registry;
	private final EquinoxContainer container;
	// registrations that threw an exception are not called again
	private final Set<ServiceRegistration<?>> denied = Collections.synchronizedSet(
			Collections.newSetFromMap(new WeakHashMap<>()));
	private final ThreadLocal<Set<List<Object>>> active = ThreadLocal.withInitial(HashSet::new);

	public DelegationHookSupport(ServiceRegistry registry, EquinoxContainer container) {
		this.registry = registry;
		this.container = container;
	}

	public boolean hasHooks() {
		return registry.hasHooks(DelegationHook.class);
	}

	public Class<?> findClass(ModuleWiring wiring, String name) {
		Object[] result = new Object[1];
		call(wiring, "findClass", name, (hook, reg) -> { //$NON-NLS-1$
			try {
				result[0] = hook.findClass(wiring, name);
			} catch (ClassNotFoundException e) {
				// the hook knows the class but cannot load it; treat as not provided
			}
			return result[0] != null;
		});
		Class<?> clazz = (Class<?>) result[0];
		if (clazz != null && clazz.getClassLoader() instanceof ModuleClassLoader) {
			ModuleWiring provider = ((ModuleClassLoader) clazz.getClassLoader()).getBundleLoader().getWiring();
			provider.addDelegationDependent(wiring);
		}
		return clazz;
	}

	public URL findResource(ModuleWiring wiring, String name) {
		URL[] result = new URL[1];
		call(wiring, "findResource", name, (hook, reg) -> { //$NON-NLS-1$
			result[0] = hook.findResource(wiring, name);
			return result[0] != null;
		});
		return result[0];
	}

	public List<URL> findResources(ModuleWiring wiring, String name) {
		List<URL> result = new ArrayList<>();
		call(wiring, "findResources", name, (hook, reg) -> { //$NON-NLS-1$
			Collection<URL> urls = hook.findResources(wiring, name);
			if (urls != null) {
				result.addAll(urls);
			}
			return false;
		});
		return result;
	}

	/** Calls a hook; returns true if the hooks after it must not be called. */
	@FunctionalInterface
	private interface HookCall {
		boolean call(DelegationHook hook, ServiceRegistration<DelegationHook> registration) throws Exception;
	}

	private void call(ModuleWiring wiring, String method, String name, HookCall hookCall) {
		List<Object> key = Arrays.asList(wiring, method, name);
		Set<List<Object>> running = active.get();
		if (!running.add(key)) {
			return;
		}
		try {
			boolean[] done = new boolean[1];
			registry.notifyHooksPrivileged(DelegationHook.class, method, new HookContext<DelegationHook>() {
				@Override
				public void call(DelegationHook hook, ServiceRegistration<DelegationHook> registration) {
					try {
						done[0] = hookCall.call(hook, registration);
					} catch (Throwable t) {
						denied.add(registration);
						container.getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR,
								registration.getReference().getBundle(), t);
					}
				}

				@Override
				public boolean skipRegistration(ServiceRegistration<?> registration) {
					return done[0] || denied.contains(registration);
				}
			});
		} finally {
			running.remove(key);
		}
	}
}
