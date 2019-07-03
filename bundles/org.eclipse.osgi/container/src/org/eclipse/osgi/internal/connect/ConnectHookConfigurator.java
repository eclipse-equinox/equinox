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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ModuleEvent;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.container.builders.OSGiManifestBuilderFactory;
import org.eclipse.osgi.internal.connect.ConnectBundleFileFactory.ConnectBundleFileWrapper;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
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
import org.eclipse.osgi.storage.url.reference.ReferenceInputStream;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.connect.ConnectFactory;
import org.osgi.framework.connect.ConnectModule;

public class ConnectHookConfigurator implements HookConfigurator {
	static final ConnectModule NULL_MODULE = new ConnectModule() {
		@Override
		public ConnectContent getContent() throws IOException {
			throw new IOException();
		}
	};

	static final byte[] EMPTY_JAR;
	static {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			JarOutputStream jos = new JarOutputStream(baos);
			ZipEntry bootBundlePropsEntry = new ZipEntry("ConnectBundle.properties"); //$NON-NLS-1$
			jos.putNextEntry(bootBundlePropsEntry);
			Properties bootBundleProps = new Properties();
			bootBundleProps.setProperty("ConnectBundle", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			bootBundleProps.store(jos, "ConnectBundle"); //$NON-NLS-1$
			jos.close();
			EMPTY_JAR = baos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void addHooks(final HookRegistry hookRegistry) {
		ConnectFactory connectFactory = hookRegistry.getContainer().getConnectFactory();
		final ConnectModules connectModules = new ConnectModules(connectFactory);

		URL configUrl = hookRegistry.getContainer().getLocations().getConfigurationLocation().getURL();
		final File storage = new File(configUrl.getPath());
		final File emptyJar = new File(storage, "connectEmptyBundle.jar"); //$NON-NLS-1$
		if (connectFactory != null && !emptyJar.exists()) {
			try {
				Files.write(emptyJar.toPath(), EMPTY_JAR);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

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
						if (hasModule && connectModules.getConnectModule(generation.getBundleInfo().getLocation()) == null) {
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
				ConnectModule m = connectModules.getConnectModule(location);
				if (m != null) {
					return new URLConnection(null) {
						@Override
						public void connect() throws IOException {
							connected = true;
						}

						public InputStream getInputStream() throws IOException {
							return new ReferenceInputStream(emptyJar);
						}
					};
				}
				return null;
			}
		});

		if (connectFactory == null) {
			return;
		}

		hookRegistry.addClassLoaderHook(new ClassLoaderHook() {
			@Override
			public ModuleClassLoader createClassLoader(ClassLoader parent, EquinoxConfiguration configuration, BundleLoader delegate, Generation generation) {
				ConnectModule m = connectModules.getConnectModule(generation.getBundleInfo().getLocation());
				if (m != null) {
					BundleFile bundlefile = generation.getBundleFile();
					if (bundlefile instanceof BundleFileWrapperChain) {
						ConnectBundleFileWrapper content = ((BundleFileWrapperChain) bundlefile).getWrappedType(ConnectBundleFileWrapper.class);
						if (content != null) {
							return content.getConnectBundleFile().getClassLoader().map((l) //
							-> new DelegatingConnectClassLoader(parent, configuration, delegate, generation, l)).orElse(null);
						}
					}
				}
				return null;
			}
		});

		final Debug debug = hookRegistry.getContainer().getConfiguration().getDebug();
		hookRegistry.addBundleFileWrapperFactoryHook(new ConnectBundleFileFactory(connectModules, debug));

		hookRegistry.addActivatorHookFactory(new ActivatorHookFactory() {

			@Override
			public BundleActivator createActivator() {
				final List<BundleActivator> activators = new ArrayList<>();
				connectFactory.createBundleActivator().ifPresent((a) -> activators.add(a));
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

	static class ConnectModules {
		final ConnectFactory connectFactory;
		private final ConcurrentMap<String, ConnectModule> connectModules = new ConcurrentHashMap<>();
		volatile File emptyJar;

		public ConnectModules(ConnectFactory connectFactory) {
			this.connectFactory = connectFactory;
		}

		ConnectModule getConnectModule(String location) {
			if (connectFactory == null) {
				return null;
			}
			ConnectModule result = connectModules.computeIfAbsent(location, (l) -> {
				try {
					return connectFactory.getModule(location).orElse(NULL_MODULE);
				} catch (IllegalStateException e) {
					return NULL_MODULE;
				}
			});
			return result == NULL_MODULE ? null : result;
		}

	}

	@SuppressWarnings("unchecked")
	public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
		throw (E) e;
	}
}
