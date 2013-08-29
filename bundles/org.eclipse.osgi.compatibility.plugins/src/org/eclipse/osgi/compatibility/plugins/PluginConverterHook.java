/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.compatibility.plugins;

import java.io.File;
import java.io.IOException;

import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.internal.hookregistry.ActivatorHookFactory;
import org.eclipse.osgi.internal.hookregistry.BundleFileWrapperFactoryHook;
import org.eclipse.osgi.internal.hookregistry.HookConfigurator;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapper;
import org.eclipse.osgi.storage.bundlefile.FileBundleEntry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class PluginConverterHook implements HookConfigurator {
	@Override
	public void addHooks(final HookRegistry hookRegistry) {
		PluginConverterImpl tempConverter;
		try {
			tempConverter = new PluginConverterImpl(hookRegistry);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		final PluginConverterImpl converter = tempConverter;
		hookRegistry.addBundleFileWrapperFactoryHook(new BundleFileWrapperFactoryHook() {

			@Override
			public BundleFileWrapper wrapBundleFile(final BundleFile bundleFile, Generation generation, boolean base) {
				if (!base) {
					return null;
				}
				return new BundleFileWrapper(bundleFile) {

					@Override
					public BundleEntry getEntry(String path) {
						BundleEntry entry = bundleFile.getEntry(path);
						if (!PluginConverterImpl.OSGI_BUNDLE_MANIFEST.equals(path)) {
							return entry;
						}
						Headers<String, String> headers = null;
						if (entry != null) {
							try {
								headers = Headers.parseManifest(entry.getInputStream());
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
							if (headers.containsKey(Constants.BUNDLE_MANIFESTVERSION)) {
								return entry;
							}
							if (headers.containsKey(Constants.BUNDLE_SYMBOLICNAME)) {
								return entry;
							}
						}
						try {
							File manifest = converter.convertManifest(bundleFile.getBaseFile(), null, true, null, true, null, false);
							if (manifest == null) {
								return entry;
							}
							return new FileBundleEntry(manifest, PluginConverterImpl.OSGI_BUNDLE_MANIFEST);
						} catch (PluginConversionException e) {
							throw new RuntimeException(e);
						}
					}
				};
			}
		});

		hookRegistry.addActivatorHookFactory(new ActivatorHookFactory() {

			@Override
			public BundleActivator createActivator() {
				return new BundleActivator() {
					ServiceRegistration<PluginConverter> reg;

					@Override
					public void start(BundleContext context) throws Exception {
						reg = context.registerService(PluginConverter.class, converter, null);
					}

					@Override
					public void stop(BundleContext context) throws Exception {
						reg.unregister();
					}
				};
			}
		});
	}
}
