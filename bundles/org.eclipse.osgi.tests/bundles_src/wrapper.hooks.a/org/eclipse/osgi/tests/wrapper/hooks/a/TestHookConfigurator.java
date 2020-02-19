/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
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
package org.eclipse.osgi.tests.wrapper.hooks.a;

import java.io.*;
import java.net.*;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.internal.hookregistry.*;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.*;

public class TestHookConfigurator implements HookConfigurator {
	public void addHooks(HookRegistry hookRegistry) {

		BundleFileWrapperFactoryHook modifyContent = new BundleFileWrapperFactoryHook() {

			@Override
			public BundleFileWrapper wrapBundleFile(BundleFile bundleFile, Generation generation, boolean base) {
				return new BundleFileWrapper(bundleFile) {

					@Override
					public BundleEntry getEntry(String path) {
						final BundleEntry original = super.getEntry(path);
						final byte[] content = "CUSTOM_CONTENT".getBytes();
						if ("data/resource1".equals(path)) {
							return new BundleEntry() {

								@Override
								public long getTime() {
									return original.getTime();
								}

								@Override
								public long getSize() {
									return content.length;
								}

								@Override
								public String getName() {
									return original.getName();
								}

								@Override
								public URL getLocalURL() {
									return original.getLocalURL();
								}

								/**
								 * @throws IOException
								 */
								@Override
								public InputStream getInputStream() throws IOException {
									return new ByteArrayInputStream(content);
								}

								@Override
								public URL getFileURL() {
									return original.getFileURL();
								}
							};
						}
						return original;
					}

				};
			}
		};
		BundleFileWrapperFactoryHook noop = new BundleFileWrapperFactoryHook() {
			@Override
			public BundleFileWrapper wrapBundleFile(BundleFile bundleFile, Generation generation, boolean base) {
				return new BundleFileWrapper(bundleFile) {
					// Do nothing to test multiple wrappers
				};
			}
		};
		// add no-op before the getResourceURL override
		hookRegistry.addBundleFileWrapperFactoryHook(noop);

		// add a hook that modifies content
		hookRegistry.addBundleFileWrapperFactoryHook(modifyContent);

		hookRegistry.addBundleFileWrapperFactoryHook(new BundleFileWrapperFactoryHook() {

			@Override
			public BundleFileWrapper wrapBundleFile(BundleFile bundleFile, Generation generation, boolean base) {
				return new BundleFileWrapper(bundleFile) {
					@Override
					public URL getResourceURL(String path, Module hostModule, int index) {
						// just making sure the wrapper getResourceURL is never called
						throw new RuntimeException("Should not be called");
					}

					@Override
					protected URL createResourceURL(BundleEntry bundleEntry, Module hostModule, int index, String path) {
						final URL url = super.createResourceURL(bundleEntry, hostModule, index, path);
						if (url == null) {
							return null;
						}
						try {
							return new URL("custom", "custom", 0, path, new URLStreamHandler() {

								@Override
								protected URLConnection openConnection(URL u) throws IOException {
									// TODO Auto-generated method stub
									return url.openConnection();
								}
							});
						} catch (MalformedURLException e) {
							throw new RuntimeException(e);
						}
					}

				};
			}
		});

		// And add no-op after
		hookRegistry.addBundleFileWrapperFactoryHook(noop);
	}
}
