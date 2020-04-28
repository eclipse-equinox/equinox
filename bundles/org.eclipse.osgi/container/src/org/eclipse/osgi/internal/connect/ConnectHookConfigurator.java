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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ModuleEvent;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
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
import org.osgi.framework.connect.ConnectModule;
import org.osgi.framework.connect.ModuleConnector;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;

public class ConnectHookConfigurator implements HookConfigurator {
	static final Collection<String> CONNECT_TAG_NAMESPACES = new ArrayList<>(Arrays.asList(
			BundleNamespace.BUNDLE_NAMESPACE, HostNamespace.HOST_NAMESPACE, IdentityNamespace.IDENTITY_NAMESPACE));

	@Override
	public void addHooks(final HookRegistry hookRegistry) {
		final ConnectModules connectModules = hookRegistry.getContainer().getConnectModules();
		ModuleConnector moduleConnector = connectModules.getModuleConnector();

		hookRegistry.addStorageHookFactory(new StorageHookFactory<Object, Object, StorageHook<Object, Object>>() {
			@Override
			protected StorageHook<Object, Object> createStorageHook(Generation generation) {
				final ConnectModule m = connectModules.getConnectModule(generation.getBundleInfo().getLocation());

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
							builder.getCapabilities()
								.stream() //
								.filter(c -> CONNECT_TAG_NAMESPACES.contains(c.getNamespace())) //
								.forEach((c) -> {
									c.getAttributes().compute(IdentityNamespace.CAPABILITY_TAGS_ATTRIBUTE, (k, v) -> {
										if (v == null) {
											return Collections.singletonList(ConnectContent.TAG_OSGI_CONNECT);
										}
										if (v instanceof List) {
											@SuppressWarnings({"unchecked", "rawtypes"})
											List<String> l = new ArrayList<>((List) v);
											l.add(ConnectContent.TAG_OSGI_CONNECT);
											return Collections.unmodifiableList(l);
										}
										// should not get here, but just recover 
										return Arrays.asList(v, ConnectContent.TAG_OSGI_CONNECT);
									});
								});
							return builder;
						}
						return null;
					}
				};
			}

			@Override
			public URLConnection handleContentConnection(Module module, String location, InputStream in) {
				if (in != null) {
					// Do not call ModuleConnector method connect when input stream is non null.
					return null;
				}
				if (location == null) {
					location = module.getLocation();
				}
				try {
					ConnectModule m = connectModules.connect(location);
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

		if (moduleConnector == null) {
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
				moduleConnector.newBundleActivator().ifPresent((a) -> activators.add(a));
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
