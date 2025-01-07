/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 576643: Clean up and unify Bundle resource classes
 *******************************************************************************/
package org.eclipse.osgi.storage;

import static org.eclipse.osgi.internal.debug.Debug.OPTION_CACHED_MANIFEST;
import static org.eclipse.osgi.internal.debug.Debug.OPTION_DEBUG_STORAGE;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ModuleEvent;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.framework.internal.reliablefile.ReliableFile;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.CaseInsensitiveDictionaryMap;
import org.eclipse.osgi.framework.util.ThreadInfoReport;
import org.eclipse.osgi.internal.connect.ConnectBundleFile;
import org.eclipse.osgi.internal.container.LockSet;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.hookregistry.StorageHookFactory;
import org.eclipse.osgi.internal.hookregistry.StorageHookFactory.StorageHook;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.storage.ContentProvider.Type;
import org.eclipse.osgi.storage.Storage.StorageException;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapperChain;
import org.eclipse.osgi.storage.url.BundleResourceHandler;
import org.eclipse.osgi.storage.url.bundleentry.Handler;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.connect.ConnectModule;

public final class BundleInfo {
	public static final String OSGI_BUNDLE_MANIFEST = JarFile.MANIFEST_NAME;
	public static final String MULTI_RELEASE_HEADER = "Multi-Release"; //$NON-NLS-1$
	public static final String MULTI_RELEASE_VERSIONS = "META-INF/versions/"; //$NON-NLS-1$
	public static final Collection<String> MULTI_RELEASE_FILTER_PREFIXES = Collections.singleton("META-INF/"); //$NON-NLS-1$

	public final class Generation {
		private final long generationId;
		private final Object genMonitor = new Object();
		private final Dictionary<String, String> cachedHeaders;
		private File content;
		private boolean isDirectory;
		private boolean hasPackageInfo;
		private BundleFile bundleFile;
		private Map<String, String> rawHeaders;
		private ModuleRevision revision;
		private ManifestLocalization headerLocalization;
		private ProtectionDomain domain;
		private NativeCodeFinder nativeCodeFinder;
		private List<StorageHook<?, ?>> storageHooks;
		private long lastModified;
		private boolean isMRJar;
		private Type contentType;

		Generation(long generationId) {
			this.generationId = generationId;
			this.cachedHeaders = new CachedManifest(this, Collections.emptyMap());
		}

		Generation(long generationId, File content, boolean isDirectory, Type contentType, boolean hasPackageInfo,
				Map<String, String> cached, long lastModified, boolean isMRJar) {
			this.generationId = generationId;
			this.content = content;
			this.isDirectory = isDirectory;
			this.contentType = contentType;
			this.hasPackageInfo = hasPackageInfo;
			this.cachedHeaders = new CachedManifest(this, cached);
			this.lastModified = lastModified;
			this.isMRJar = isMRJar;
		}

		public BundleFile getBundleFile() {
			synchronized (genMonitor) {
				if (bundleFile == null) {
					if (getBundleId() == 0 && content == null && contentType != Type.CONNECT) {
						bundleFile = new SystemBundleFile();
					} else {
						bundleFile = getStorage().createBundleFile(content, this, isDirectory, true);
					}
				}
				return bundleFile;
			}
		}

		public void close() {
			synchronized (genMonitor) {
				if (bundleFile != null) {
					try {
						bundleFile.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}
		}

		public Dictionary<String, String> getHeaders() {
			return cachedHeaders;
		}

		Map<String, String> getRawHeaders() {
			synchronized (genMonitor) {
				if (rawHeaders == null) {
					BundleFile bFile = getBundleFile();

					if (this.contentType == Type.CONNECT) {
						ConnectBundleFile connectContent = bFile instanceof BundleFileWrapperChain ? //
								((BundleFileWrapperChain) bFile).getWrappedType(ConnectBundleFile.class) : //
								(ConnectBundleFile) bFile;

						Map<String, String> connectHeaders = connectContent.getConnectHeaders();
						if (connectHeaders != null) {
							return rawHeaders = connectHeaders;
						}
					}

					BundleEntry manifest = bFile.getEntry(OSGI_BUNDLE_MANIFEST);
					if (manifest == null) {
						rawHeaders = Collections.emptyMap();
					} else {
						try {
							Map<String, String> merged = ManifestElement.parseBundleManifest(manifest.getInputStream(),
									new CaseInsensitiveDictionaryMap<>());
							// For MRJARs only replace Import-Package and Require-Capability if the
							// versioned values are non-null
							if (Boolean.parseBoolean(merged.get(MULTI_RELEASE_HEADER))) {
								for (int i = getStorage().getRuntimeVersion().getMajor(); i > 8; i--) {
									String versionManifest = MULTI_RELEASE_VERSIONS + i + "/OSGI-INF/MANIFEST.MF"; //$NON-NLS-1$
									BundleEntry versionEntry = getBundleFile().getEntry(versionManifest);
									if (versionEntry != null) {
										Map<String, String> versioned = ManifestElement.parseBundleManifest(
												versionEntry.getInputStream(), new CaseInsensitiveDictionaryMap<>());
										String versionedImport = versioned.get(Constants.IMPORT_PACKAGE);
										String versionedRequireCap = versioned.get(Constants.REQUIRE_CAPABILITY);
										if (versionedImport != null) {
											merged.put(Constants.IMPORT_PACKAGE, versionedImport);
										}
										if (versionedRequireCap != null) {
											merged.put(Constants.REQUIRE_CAPABILITY, versionedRequireCap);
										}
										// found a versioned entry; stop searching for more versions
										break;
									}
								}
							}
							rawHeaders = Collections.unmodifiableMap(merged);
						} catch (RuntimeException e) {
							throw e;
						} catch (Exception e) {
							throw new RuntimeException("Error occurred getting the bundle manifest.", e); //$NON-NLS-1$
						}
					}
				}
				return rawHeaders;
			}
		}

		public Dictionary<String, String> getHeaders(String locale) {
			ManifestLocalization current = getManifestLocalization();
			return current.getHeaders(locale);
		}

		public ResourceBundle getResourceBundle(String locale) {
			ManifestLocalization current = getManifestLocalization();
			String defaultLocale = Locale.getDefault().toString();
			if (locale == null) {
				locale = defaultLocale;
			}
			return current.getResourceBundle(locale, defaultLocale.equals(locale));
		}

		private ManifestLocalization getManifestLocalization() {
			synchronized (genMonitor) {
				if (headerLocalization == null) {
					headerLocalization = new ManifestLocalization(this, getHeaders(), getStorage().getConfiguration()
							.getConfiguration(EquinoxConfiguration.PROP_ROOT_LOCALE, "en")); //$NON-NLS-1$
				}
				return headerLocalization;
			}
		}

		public void clearManifestCache() {
			synchronized (genMonitor) {
				if (headerLocalization != null) {
					headerLocalization.clearCache();
				}
			}
		}

		public long getGenerationId() {
			return this.generationId;
		}

		public long getLastModified() {
			return lastModified;
		}

		public boolean isDirectory() {
			synchronized (this.genMonitor) {
				return this.isDirectory;
			}
		}

		public boolean hasPackageInfo() {
			synchronized (this.genMonitor) {
				return this.hasPackageInfo;
			}
		}

		public boolean isMRJar() {
			synchronized (this.genMonitor) {
				return this.isMRJar;
			}
		}

		public File getContent() {
			synchronized (this.genMonitor) {
				return this.content;
			}
		}

		public Type getContentType() {
			synchronized (this.genMonitor) {
				return this.contentType;
			}
		}

		void setContent(File content, Type contentType) {
			if (getBundleId() == 0) {
				// check connect for content first
				ConnectModule connected = getStorage().getEquinoxContainer().getConnectModules().connect(getLocation());
				if (connected != null) {
					content = null;
					contentType = Type.CONNECT;
				}
			}
			synchronized (this.genMonitor) {
				this.content = content;
				this.isDirectory = content != null && Storage.secureAction.isDirectory(content);
				this.contentType = contentType;
				setLastModified(content);
			}
		}

		private void setLastModified(File content) {
			if (content == null) {
				// Bug 477787: content will be null when the osgi.framework configuration
				// property contains an invalid value.
				lastModified = 0;
				return;
			}
			if (isDirectory)
				content = new File(content, OSGI_BUNDLE_MANIFEST); // $NON-NLS-1$
			lastModified = Storage.secureAction.lastModified(content);
		}

		void setStorageHooks(List<StorageHook<?, ?>> storageHooks, boolean install) {
			synchronized (this.genMonitor) {
				this.storageHooks = storageHooks;
				if (install) {
					this.hasPackageInfo = BundleInfo.hasPackageInfo(getBundleFile());
					this.isMRJar = Boolean.parseBoolean(getRawHeaders().get(MULTI_RELEASE_HEADER));
				}
			}
		}

		@SuppressWarnings("unchecked")
		public <S, L, H extends StorageHook<S, L>> H getStorageHook(
				Class<? extends StorageHookFactory<S, L, H>> factoryClass) {
			synchronized (this.genMonitor) {
				if (this.storageHooks == null)
					return null;
				for (StorageHook<?, ?> hook : storageHooks) {
					if (hook.getFactoryClass().equals(factoryClass)) {
						return (H) hook;
					}
				}
			}
			return null;
		}

		public ModuleRevision getRevision() {
			synchronized (this.genMonitor) {
				return this.revision;
			}
		}

		public void setRevision(ModuleRevision revision) {
			synchronized (this.genMonitor) {
				this.revision = revision;
			}
		}

		public ProtectionDomain getDomain() {
			return getDomain(true);
		}

		public ProtectionDomain getDomain(boolean create) {
			if (getBundleId() == 0 || System.getSecurityManager() == null) {
				return null;
			}
			synchronized (this.genMonitor) {
				if (domain == null && create) {
					if (revision == null) {
						throw new IllegalStateException("The revision is not yet set for this generation."); //$NON-NLS-1$
					}
					domain = getStorage().getSecurityAdmin().createProtectionDomain(revision.getBundle());
				}
				return domain;
			}
		}

		/**
		 * Gets called by BundleFile during {@link BundleFile#getFile(String, boolean)}.
		 * This method will allocate a File object where content of the specified path
		 * may be stored for this generation. The returned File object may not exist if
		 * the content has not previously been stored.
		 * 
		 * @param path the path to the content to extract from the generation
		 * @return a file object where content of the specified path may be stored.
		 * @throws StorageException if the path will escape the persistent storage of
		 *                          the generation
		 */
		public File getExtractFile(String path) {
			return getExtractFile(null, path);
		}

		/**
		 * Gets called by BundleFile during {@link BundleFile#getFile(String, boolean)}.
		 * This method will allocate a File object where content of the specified path
		 * may be stored for this generation. The returned File object may not exist if
		 * the content has not previously been stored.
		 * 
		 * @param path the path to the content to extract from the generation
		 * @param base the base path that is prepended to the path, may be null
		 * @return a file object where content of the specified path may be stored.
		 * @throws StorageException if the path will escape the persistent storage of
		 *                          the generation starting at the specified base
		 */
		public File getExtractFile(String base, String path) {
			StringBuilder baseBuilder = new StringBuilder();
			baseBuilder.append(getBundleId()).append('/').append(getGenerationId());
			if (base != null) {
				baseBuilder.append('/').append(base);
			}

			return getStorage().getFile(baseBuilder.toString(), path, true);
		}

		public void storeContent(File destination, InputStream in, boolean nativeCode) throws IOException {
			Debug debug = getStorage().getConfiguration().getDebug();
			/* the entry has not been cached */
			if (debug.DEBUG_STORAGE)
				debug.trace(OPTION_DEBUG_STORAGE, "Creating file: " + destination.getPath()); //$NON-NLS-1$
			/* create the necessary directories */
			File dir = new File(destination.getParent());
			if (!dir.mkdirs() && !dir.isDirectory()) {
				if (debug.DEBUG_STORAGE)
					debug.trace(OPTION_DEBUG_STORAGE, "Unable to create directory: " + dir.getPath()); //$NON-NLS-1$
				throw new IOException(NLS.bind(Msg.ADAPTOR_DIRECTORY_CREATE_EXCEPTION, dir.getAbsolutePath()));
			}
			/* copy the entry to the cache */
			File tempDest = ReliableFile.createTempFile("staged", ".tmp", dir); //$NON-NLS-1$ //$NON-NLS-2$
			StorageUtil.readFile(in, tempDest);
			if (destination.exists()) {
				// maybe because some other thread already beat us there.
				// just delete our staged copy
				tempDest.delete();
			} else {
				StorageUtil.move(tempDest, destination, getStorage().getConfiguration().getDebug());
			}
			if (nativeCode) {
				getBundleInfo().getStorage().setPermissions(destination);
			}
		}

		public BundleInfo getBundleInfo() {
			return BundleInfo.this;
		}

		public void delete() {
			List<StorageHook<?, ?>> hooks = getStorageHooks();
			if (hooks != null) {
				for (StorageHook<?, ?> hook : hooks) {
					hook.deletingGeneration();
				}
			}
			synchronized (this.genMonitor) {
				// make sure the bundle file is closed
				if (bundleFile != null) {
					try {
						bundleFile.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}
			getBundleInfo().delete(this);
		}

		public URL getEntry(String path) {
			BundleEntry entry = getBundleFile().getEntry(path);
			if (entry == null) {
				return null;
			}
			// use the constant string for the protocol to prevent duplication
			String protocol = BundleResourceHandler.OSGI_ENTRY_URL_PROTOCOL;
			ModuleContainer container = getStorage().getModuleContainer();
			Handler handler = new Handler(container, entry);
			return BundleFile.createURL(protocol, getBundleId(), container, entry, 0, path, handler);
		}

		public String findLibrary(String libname) {
			NativeCodeFinder currentFinder;
			synchronized (this.genMonitor) {
				if (nativeCodeFinder == null) {
					nativeCodeFinder = new NativeCodeFinder(this);
				}
				currentFinder = nativeCodeFinder;
			}
			return currentFinder.findLibrary(libname);
		}

		List<StorageHook<?, ?>> getStorageHooks() {
			synchronized (this.genMonitor) {
				return this.storageHooks;
			}
		}

		public ModuleRevisionBuilder adaptModuleRevisionBuilder(ModuleEvent operation, Module origin,
				ModuleRevisionBuilder builder) {
			List<StorageHook<?, ?>> hooks = getStorageHooks();
			if (hooks != null) {
				for (StorageHook<?, ?> hook : hooks) {
					ModuleRevisionBuilder hookResult = hook.adaptModuleRevisionBuilder(operation, origin, builder);
					if (hookResult != null) {
						builder = hookResult;
					}
				}
			}
			return builder;
		}
	}

	private final Storage storage;
	private final long bundleId;
	private final String location;
	private long nextGenerationId;
	private final Object infoMonitor = new Object();
	private LockSet<Long> generationLocks;

	public BundleInfo(Storage storage, long bundleId, String location, long nextGenerationId) {
		this.storage = storage;
		this.bundleId = bundleId;
		this.location = location;
		this.nextGenerationId = nextGenerationId;
	}

	public long getBundleId() {
		return bundleId;
	}

	public String getLocation() {
		return location;
	}

	Generation createGeneration() throws BundleException {
		synchronized (this.infoMonitor) {
			if (generationLocks == null) {
				generationLocks = new LockSet<>();
			}
			boolean lockedID;
			try {
				lockedID = generationLocks.tryLock(nextGenerationId, 5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new BundleException("Failed to obtain id locks for generation.", //$NON-NLS-1$
						BundleException.STATECHANGE_ERROR, e);
			}
			if (!lockedID) {
				throw new BundleException("Failed to obtain id locks for generation.", //$NON-NLS-1$
						BundleException.STATECHANGE_ERROR,
						new ThreadInfoReport(generationLocks.getLockInfo(nextGenerationId)));
			}
			return new Generation(nextGenerationId++);
		}
	}

	void unlockGeneration(Generation generation) {
		synchronized (this.infoMonitor) {
			if (generationLocks == null) {
				throw new IllegalStateException("The generation id was not locked."); //$NON-NLS-1$
			}
			generationLocks.unlock(generation.getGenerationId());
		}
	}

	Generation restoreGeneration(long generationId, File content, boolean isDirectory, Type contentType,
			boolean hasPackageInfo, Map<String, String> cached, long lastModified, boolean isMRJar) {
		synchronized (this.infoMonitor) {
			return new Generation(generationId, content, isDirectory, contentType, hasPackageInfo, cached, lastModified,
					isMRJar);
		}
	}

	public Storage getStorage() {
		return storage;
	}

	public void delete() {
		try {
			getStorage().delete(getStorage().getFile(Long.toString(getBundleId()), false));
		} catch (IOException e) {
			storage.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.WARNING,
					"Error deleting bunlde info.", e); //$NON-NLS-1$
		}
	}

	void delete(Generation generation) {
		try {
			getStorage().delete(getStorage().getFile(getBundleId() + "/" + generation.getGenerationId(), false)); //$NON-NLS-1$
		} catch (IOException e) {
			storage.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.WARNING, "Error deleting generation.", //$NON-NLS-1$
					e);
		}
	}

	public long getNextGenerationId() {
		synchronized (this.infoMonitor) {
			return nextGenerationId;
		}
	}

	public File getDataFile(String path) {
		File dataRoot = getStorage().getFile(getBundleId() + "/" + Storage.BUNDLE_DATA_DIR, false); //$NON-NLS-1$
		if (!Storage.secureAction.isDirectory(dataRoot) && (storage.isReadOnly()
				|| !(Storage.secureAction.mkdirs(dataRoot) || Storage.secureAction.isDirectory(dataRoot)))) {
			Debug debug = getStorage().getConfiguration().getDebug();
			if (debug.DEBUG_STORAGE)
				debug.trace(OPTION_DEBUG_STORAGE,
						"Unable to create bundle data directory: " + dataRoot.getAbsolutePath()); //$NON-NLS-1$
			return null;
		}
		return path == null ? dataRoot : new File(dataRoot, path);
	}

	// Used to check the bundle manifest file for any package information.
	// This is used when '.' is on the Bundle-ClassPath to prevent reading
	// the bundle manifest for package information when loading classes.
	static boolean hasPackageInfo(BundleFile bundleFile) {
		if (bundleFile == null) {
			return false;
		}
		BundleEntry manifest = bundleFile.getEntry(OSGI_BUNDLE_MANIFEST);
		if (manifest == null) {
			return false;
		}
		try (BufferedReader br = new BufferedReader(new InputStreamReader(manifest.getInputStream()))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.length() < 20)
					continue;
				switch (line.charAt(0)) {
				case 'S':
					if (line.charAt(1) == 'p' && (line.startsWith("Specification-Title: ") //$NON-NLS-1$
							|| line.startsWith("Specification-Version: ") //$NON-NLS-1$
							|| line.startsWith("Specification-Vendor: "))) //$NON-NLS-1$
						return true;
					break;
				case 'I':
					if (line.startsWith("Implementation-Title: ") || line.startsWith("Implementation-Version: ") //$NON-NLS-1$ //$NON-NLS-2$
							|| line.startsWith("Implementation-Vendor: ")) //$NON-NLS-1$
						return true;
					break;
				}
			}
		} catch (IOException ioe) {
			// do nothing
		}
		return false;
	}

	static class CachedManifest extends Dictionary<String, String> implements Map<String, String> {
		private final Map<String, String> cached;
		private final Generation generation;

		CachedManifest(Generation generation, Map<String, String> cached) {
			this.generation = generation;
			this.cached = cached;
		}

		@Override
		public Enumeration<String> elements() {
			return Collections.enumeration(generation.getRawHeaders().values());
		}

		@Override
		public String get(Object key) {
			if (cached.containsKey(key)) {
				return cached.get(key);
			}
			Debug debug = generation.getBundleInfo().getStorage().getConfiguration().getDebug();
			if (!cached.isEmpty() && debug.DEBUG_CACHED_MANIFEST) {
				debug.trace(OPTION_CACHED_MANIFEST, "Header key is not cached: " + key + "; for bundle: " //$NON-NLS-1$ //$NON-NLS-2$
						+ generation.getBundleInfo().getBundleId());
			}
			return generation.getRawHeaders().get(key);
		}

		@Override
		public boolean isEmpty() {
			return generation.getRawHeaders().isEmpty();
		}

		@Override
		public Enumeration<String> keys() {
			return Collections.enumeration(generation.getRawHeaders().keySet());
		}

		@Override
		public String put(String key, String value) {
			return generation.getRawHeaders().put(key, value);
		}

		@Override
		public String remove(Object key) {
			return generation.getRawHeaders().remove(key);
		}

		@Override
		public int size() {
			return generation.getRawHeaders().size();
		}

		@Override
		public boolean containsKey(Object key) {
			return cached.containsKey(key) || generation.getRawHeaders().containsKey(key);
		}

		@Override
		public boolean containsValue(Object value) {
			return cached.containsValue(value) || generation.getRawHeaders().containsValue(value);
		}

		@Override
		public void putAll(Map<? extends String, ? extends String> m) {
			generation.getRawHeaders().putAll(m);
		}

		@Override
		public void clear() {
			generation.getRawHeaders().clear();
		}

		@Override
		public Set<String> keySet() {
			return generation.getRawHeaders().keySet();
		}

		@Override
		public Collection<String> values() {
			return generation.getRawHeaders().values();
		}

		@Override
		public Set<java.util.Map.Entry<String, String>> entrySet() {
			return generation.getRawHeaders().entrySet();
		}
	}

}
