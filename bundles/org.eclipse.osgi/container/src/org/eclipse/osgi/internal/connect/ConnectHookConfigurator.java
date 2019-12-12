/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package org.eclipse.osgi.internal.connect;

import static org.eclipse.osgi.internal.framework.EquinoxContainer.sneakyThrow;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ModuleEvent;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.container.builders.OSGiManifestBuilderFactory;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.framework.EquinoxContainer.ConnectModules;
import org.eclipse.osgi.internal.hookregistry.ActivatorHookFactory;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.hookregistry.HookConfigurator;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.internal.hookregistry.StorageHookFactory;
import org.eclipse.osgi.internal.hookregistry.StorageHookFactory.StorageHook;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapperChain;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.connect.ConnectFramework;
import org.osgi.framework.connect.ConnectModule;

public class ConnectHookConfigurator implements HookConfigurator {

	@Override
	public void addHooks(final HookRegistry hookRegistry) {
		final ConnectModules connectModules = hookRegistry.getContainer().getConnectModules();
		ConnectFramework connectFramework = connectModules.getConnectFramework();

		hookRegistry.addStorageHookFactory(new StorageHookFactory<Object, Object, StorageHook<Object, Object>>() {
			@Override
			protected StorageHook<Object, Object> createStorageHook(Generation generation) {
				ConnectModule tmp = null;
				try {
					tmp = connectModules.getConnectModule(generation.getBundleInfo().getLocation());
				} catch (IllegalStateException e) {
					if (!(e.getCause() instanceof BundleException)) {
						throw e;
					}
				}
				final ConnectModule m = tmp;

				return new StorageHook<Object, Object>(generation, this.getClass()) {
					boolean hasModule = false;

					@Override
					public void save(Object saveContext, DataOutputStream os) throws IOException {
						os.writeBoolean(m != null);
					}

					@Override
					public void load(Object loadContext, DataInputStream is) throws IOException {
						hasModule = is.readBoolean();
					}

					@Override
					public void validate() throws IllegalStateException {
						// make sure we have the module still from the factory
						if (hasModule && m == null) {
							throw new IllegalStateException("Connect Factory no longer has the module at locataion: " + generation.getBundleInfo().getLocation()); //$NON-NLS-1$
						}
					}

					@Override
					public ModuleRevisionBuilder adaptModuleRevisionBuilder(ModuleEvent operation, Module origin, ModuleRevisionBuilder builder) {
						if (m != null) {
							try {
								ConnectContent content = m.getContent();
								return content.getHeaders().map((h) -> {
									try {
										return OSGiManifestBuilderFactory.createBuilder(h);
									} catch (BundleException e) {
										sneakyThrow(e);
									}
									return null; // should never get here
								}).orElse(null);
							} catch (IOException e) {
								sneakyThrow(new BundleException("Error reading bundle.", BundleException.READ_ERROR, e)); //$NON-NLS-1$
							}
						}
						return null;
					}
				};
			}

			@Override
			public URLConnection handleContentConnection(Module module, String location, InputStream in) {
				if (location == null) {
					location = module.getLocation();
				}
				try {
					ConnectModule m = connectModules.getConnectModule(location);
					if (m != null) {
						return ConnectInputStream.URL_CONNECTION_INSTANCE;
					}
				} catch (IllegalStateException e) {
					if (e.getCause() instanceof BundleException) {
						sneakyThrow(e.getCause());
					}
				}
				return null;
			}
		});

		if (connectFramework == null) {
			return;
		}

		hookRegistry.addClassLoaderHook(new ClassLoaderHook() {
			@Override
			public ModuleClassLoader createClassLoader(ClassLoader parent, EquinoxConfiguration configuration, BundleLoader delegate, Generation generation) {
				ConnectModule m = connectModules.getConnectModule(generation.getBundleInfo().getLocation());
				if (m != null) {
					BundleFile bundlefile = generation.getBundleFile();
					if (bundlefile instanceof BundleFileWrapperChain) {
						BundleFileWrapperChain chain = (BundleFileWrapperChain) bundlefile;
						while (chain.getNext() != null) {
							chain = chain.getNext();
						}
						bundlefile = chain.getBundleFile();
					}
					if (bundlefile instanceof ConnectBundleFile) {
						return ((ConnectBundleFile) bundlefile).getClassLoader().map((l) //
						-> new DelegatingConnectClassLoader(parent, configuration, delegate, generation, l)).orElse(null);
					}
				}
				return null;
			}
		});

		hookRegistry.addActivatorHookFactory(new ActivatorHookFactory() {

			@Override
			public BundleActivator createActivator() {
				final List<BundleActivator> activators = new ArrayList<>();
				connectFramework.createBundleActivator().ifPresent((a) -> activators.add(a));
				return new BundleActivator() {
					@Override
					public void start(BundleContext context) throws Exception {
						for (BundleActivator activator : activators) {
							activator.start(context);
						}
					}

					@Override
					public void stop(BundleContext context) throws Exception {
						for (BundleActivator activator : activators) {
							activator.stop(context);
						}
					}
				};
			}
		});
	}
}
