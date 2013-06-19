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
import java.util.Enumeration;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.internal.hookregistry.*;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.*;
import org.osgi.framework.*;

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
			public BundleFile wrapBundleFile(final BundleFile bundleFile, Generation generation, boolean base) {
				if (!base) {
					return null;
				}
				return new BundleFile(bundleFile.getBaseFile()) {

					@Override
					public void open() throws IOException {
						bundleFile.open();
					}

					@Override
					public File getFile(String path, boolean nativeCode) {
						return bundleFile.getFile(path, nativeCode);
					}

					@Override
					public Enumeration<String> getEntryPaths(String path) {
						return bundleFile.getEntryPaths(path);
					}

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

					@Override
					public boolean containsDir(String dir) {
						return bundleFile.containsDir(dir);
					}

					@Override
					public void close() throws IOException {
						bundleFile.close();
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
