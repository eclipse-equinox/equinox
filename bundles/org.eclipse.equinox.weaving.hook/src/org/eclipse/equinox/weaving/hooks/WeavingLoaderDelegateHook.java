/*******************************************************************************
 * Copyright (c) 2008, 2014 Martin Lippert and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Martin Lippert            initial implementation
 *   Martin Lippert            fragment handling fixed
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.equinox.service.weaving.ISupplementerRegistry;
import org.eclipse.equinox.service.weaving.Supplementer;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.osgi.framework.Bundle;

/**
 * This class implements the delegate hook for the class loader to allow
 * supplemented bundles find types and resources from theirs supplementer
 * bundles
 *
 * This works together with the supplementer registry to handle the
 * supplementing mechanism. The supplementer registry controls which bundle is
 * supplemented by which other bundle. This hook implementation uses this
 * information to broaden type and resource visibility according to the
 * supplementer registry information.
 */
public class WeavingLoaderDelegateHook extends ClassLoaderHook {

	private final ThreadLocal<Set<String>> postFindClassCalls = new ThreadLocal<Set<String>>() {

		@Override
		protected Set<String> initialValue() {
			return new HashSet<>();
		}
	};

	private final ThreadLocal<Set<String>> postFindResourceCalls = new ThreadLocal<Set<String>>() {

		@Override
		protected Set<String> initialValue() {
			return new HashSet<>();
		}
	};

	private final ThreadLocal<Set<String>> postFindResourcesCalls = new ThreadLocal<Set<String>>() {

		@Override
		protected Set<String> initialValue() {
			return new HashSet<>();
		}
	};

	private final ISupplementerRegistry supplementerRegistry;

	/**
	 * Create the hook instance for broaden the visibility according to the
	 * supplementing mechansism.
	 *
	 * @param supplementerRegistry The supplementer registry to be used by this hook
	 *                             for information retrieval which bundles are
	 *                             supplemented by which other bundles (needs to not
	 *                             be null)
	 */
	public WeavingLoaderDelegateHook(final ISupplementerRegistry supplementerRegistry) {
		this.supplementerRegistry = supplementerRegistry;
	}

	/**
	 *
	 * @see org.eclipse.osgi.internal.hookregistry.ClassLoaderHook#postFindClass(java.lang.String,
	 *      org.eclipse.osgi.internal.loader.ModuleClassLoader)
	 */
	@Override
	public Class<?> postFindClass(final String name, final ModuleClassLoader classLoader)
			throws ClassNotFoundException {
		final long bundleID = classLoader.getBundle().getBundleId();

		final String callKey = bundleID + name;
		if (postFindClassCalls.get().contains(callKey)) {
			return null;
		}

		postFindClassCalls.get().add(callKey);
		try {
			final Supplementer[] supplementers = supplementerRegistry.getSupplementers(bundleID);
			if (supplementers != null) {
				for (Supplementer supplementer : supplementers) {
					try {
						final Bundle bundle = supplementer.getSupplementerHost();
						if (bundle.getState() != Bundle.UNINSTALLED) {
							final Class<?> clazz = bundle.loadClass(name);
							if (clazz != null) {
								return clazz;
							}
						}
					} catch (final ClassNotFoundException e) {
					}
				}
			}
		} finally {
			postFindClassCalls.get().remove(callKey);
		}

		return null;
	}

	/**
	 *
	 * @see org.eclipse.osgi.internal.hookregistry.ClassLoaderHook#postFindResource(java.lang.String,
	 *      org.eclipse.osgi.internal.loader.ModuleClassLoader)
	 */
	@Override
	public URL postFindResource(final String name, final ModuleClassLoader classLoader) throws FileNotFoundException {
		final long bundleID = classLoader.getBundle().getBundleId();

		final String callKey = bundleID + name;
		if (postFindResourceCalls.get().contains(callKey)) {
			return null;
		}

		postFindResourceCalls.get().add(callKey);
		try {
			final Supplementer[] supplementers = supplementerRegistry.getSupplementers(bundleID);
			if (supplementers != null) {
				for (Supplementer supplementer : supplementers) {
					try {
						final URL resource = supplementer.getSupplementerHost().getResource(name);
						if (resource != null) {
							return resource;
						}
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			}
		} finally {
			postFindResourceCalls.get().remove(callKey);
		}

		return null;
	}

	/**
	 *
	 * @see org.eclipse.osgi.internal.hookregistry.ClassLoaderHook#postFindResources(java.lang.String,
	 *      org.eclipse.osgi.internal.loader.ModuleClassLoader)
	 */
	@Override
	public Enumeration<URL> postFindResources(final String name, final ModuleClassLoader classLoader)
			throws FileNotFoundException {
		final long bundleID = classLoader.getBundle().getBundleId();

		final String callKey = bundleID + name;
		if (postFindResourcesCalls.get().contains(callKey)) {
			return null;
		}

		postFindResourcesCalls.get().add(callKey);
		try {
			final Supplementer[] supplementers = supplementerRegistry.getSupplementers(bundleID);
			if (supplementers != null) {
				for (Supplementer supplementer : supplementers) {
					try {
						final Enumeration<URL> resource = supplementer.getSupplementerHost().getResources(name);
						if (resource != null) {
							// TODO: if more than one enumeration is found, we should return all items
							return resource;
						}
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			}
		} finally {
			postFindResourcesCalls.get().remove(callKey);
		}

		return null;
	}
}
