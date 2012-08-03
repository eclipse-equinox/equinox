/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.storage;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.container.ModuleRevisionBuilder.GenericInfo;
import org.eclipse.osgi.container.builders.OSGiManifestBuilderFactory;
import org.eclipse.osgi.container.namespaces.EclipsePlatformNamespace;
import org.eclipse.osgi.container.namespaces.EquinoxNativeCodeNamespace;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.FilePath;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.internal.baseadaptor.AdaptorMsg;
import org.eclipse.osgi.internal.baseadaptor.AdaptorUtil;
import org.eclipse.osgi.internal.container.LockSet;
import org.eclipse.osgi.internal.framework.*;
import org.eclipse.osgi.internal.hookregistry.*;
import org.eclipse.osgi.internal.hookregistry.StorageHookFactory.StorageHook;
import org.eclipse.osgi.internal.log.EquinoxLogServices;
import org.eclipse.osgi.internal.permadmin.SecurityAdmin;
import org.eclipse.osgi.next.internal.debug.Debug;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.*;
import org.eclipse.osgi.storage.url.reference.ReferenceInputStream;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.framework.wiring.BundleWiring;

public class Storage {
	public static final int VERSION = 1;
	public static final String BUNDLE_DATA_DIR = "data"; //$NON-NLS-1$
	public static final String BUNDLE_FILE_NAME = "bundleFile"; //$NON-NLS-1$
	public static final String FRAMEWORK_INFO = "framework.info"; //$NON-NLS-1$
	public static final String ECLIPSE_SYSTEMBUNDLE = "Eclipse-SystemBundle"; //$NON-NLS-1$
	public static final String DELETE_FLAG = ".delete"; //$NON-NLS-1$
	public static final String LIB_TEMP = "libtemp"; //$NON-NLS-1$

	private static final String J2SE = "J2SE-"; //$NON-NLS-1$
	private static final String JAVASE = "JavaSE-"; //$NON-NLS-1$
	private static final String PROFILE_EXT = ".profile"; //$NON-NLS-1$
	static final SecureAction secureAction = AccessController.doPrivileged(SecureAction.createSecureAction());

	private final EquinoxContainer equinoxContainer;
	private final String installPath;
	private final Location osgiLocation;
	private final File childRoot;
	private final File parentRoot;
	private final PermissionData permissionData;
	private final SecurityAdmin securityAdmin;
	private final ModuleContainerAdaptor adaptor;
	private final ModuleDatabase moduleDatabase;
	private final ModuleContainer moduleContainer;
	private final Object saveMonitor = new Object();
	private long lastSavedTimestamp = -1;
	private final LockSet<Long> idLocks = new LockSet<Long>(false);
	private final MRUBundleFileList mruList = new MRUBundleFileList();

	public Storage(EquinoxContainer container) throws IOException {
		equinoxContainer = container;

		// we need to set the install path as soon as possible so we can determine
		// the absolute location of install relative URLs
		Location installLoc = container.getLocations().getInstallLocation();
		URL installURL = installLoc.getURL();
		// assume install URL is file: based
		installPath = installURL.getPath();

		Location configLocation = container.getLocations().getConfigurationLocation();
		Location parentConfigLocation = configLocation.getParentLocation();
		Location osgiParentLocation = null;
		if (parentConfigLocation != null) {
			osgiParentLocation = parentConfigLocation.createLocation(null, parentConfigLocation.getDataArea(EquinoxContainer.NAME), true);
		}
		this.osgiLocation = configLocation.createLocation(osgiParentLocation, configLocation.getDataArea(EquinoxContainer.NAME), configLocation.isReadOnly());
		this.childRoot = new File(osgiLocation.getURL().getFile());

		if (Boolean.valueOf(container.getConfiguration().getConfiguration(EquinoxConfiguration.PROP_CLEAN)).booleanValue()) {
			cleanOSGiStorage(osgiLocation, childRoot);
		}
		if (!this.osgiLocation.isReadOnly()) {
			this.childRoot.mkdirs();
		}
		Location parent = this.osgiLocation.getParentLocation();
		parentRoot = parent == null ? null : new File(osgiLocation.getURL().getFile(), EquinoxContainer.NAME);

		File frameworkInfo = getFile(FRAMEWORK_INFO, true);
		DataInputStream in = null;
		if (frameworkInfo.exists()) {
			try {
				in = new DataInputStream(new FileInputStream(frameworkInfo));
			} catch (IOException e) {
				// do nothing
			}
		}
		try {
			Map<Long, Generation> generations = loadGenerations(in);
			this.permissionData = loadPermissionData(in);
			this.securityAdmin = new SecurityAdmin(null, this.permissionData);
			this.adaptor = new EquinoxContainerAdaptor(equinoxContainer, this, generations);
			this.moduleDatabase = new ModuleDatabase(adaptor);
			this.moduleContainer = new ModuleContainer(this.adaptor, this.moduleDatabase);
			if (in != null) {
				moduleDatabase.load(in);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// just move on
				}
			}
		}
		checkSystemBundle();
	}

	private static PermissionData loadPermissionData(DataInputStream in) throws IOException {
		PermissionData permData = new PermissionData();
		if (in != null) {
			permData.readPermissionData(in);
		}
		return permData;
	}

	private void checkSystemBundle() {
		Module systemModule = moduleContainer.getModule(0);
		Generation newGeneration = null;
		try {
			if (systemModule == null) {
				BundleInfo info = new BundleInfo(this, 0, 0);
				newGeneration = info.createGeneration();

				File contentFile = getSystemContent();
				newGeneration.setContent(contentFile);

				ModuleRevisionBuilder builder = getBuilder(newGeneration);
				systemModule = moduleContainer.install(null, Constants.SYSTEM_BUNDLE_LOCATION, builder, newGeneration);
				moduleContainer.resolve(Arrays.asList(systemModule), false);
			} else {
				ModuleRevision currentRevision = systemModule.getCurrentRevision();
				Generation currentGeneration = currentRevision == null ? null : (Generation) currentRevision.getRevisionInfo();
				if (currentGeneration == null) {
					throw new IllegalStateException("No current revision for system bundle."); //$NON-NLS-1$
				}
				try {
					ModuleRevisionBuilder newBuilder = getBuilder(currentGeneration);
					if (needUpdate(currentRevision, newBuilder)) {
						newGeneration = currentGeneration.getBundleInfo().createGeneration();
						moduleContainer.update(systemModule, newBuilder, newGeneration);
						moduleContainer.refresh(Arrays.asList(systemModule));
					}
				} catch (BundleException e) {
					throw new IllegalStateException("Could not create a builder for the system bundle.", e); //$NON-NLS-1$
				}
			}
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException("Error occurred while checking the system module.", e); //$NON-NLS-1$
		} finally {
			if (newGeneration != null) {
				newGeneration.getBundleInfo().unlockGeneration(newGeneration);
			}
		}
	}

	public void close() {
		try {
			save();
		} catch (IOException e) {
			getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, "Error saving on shutdown", e);
		}

		// close all the generations
		List<Module> modules = moduleContainer.getModules();
		for (Module module : modules) {
			for (ModuleRevision revision : module.getRevisions().getModuleRevisions()) {
				Generation generation = (Generation) revision.getRevisionInfo();
				if (generation != null) {
					generation.close();
				}
			}
		}
		mruList.shutdown();
	}

	private boolean needUpdate(ModuleRevision currentRevision, ModuleRevisionBuilder newBuilder) {
		if (!currentRevision.getVersion().equals(newBuilder.getVersion())) {
			return true;
		}
		// only do this advanced check if in dev mode
		if (!equinoxContainer.getConfiguration().inDevelopmentMode()) {
			return false;
		}
		List<ModuleCapability> currentCapabilities = currentRevision.getModuleCapabilities(null);
		List<GenericInfo> newCapabilities = newBuilder.getCapabilities();
		if (currentCapabilities.size() != newCapabilities.size()) {
			return true;
		}

		int size = currentCapabilities.size();
		for (int i = 0; i < size; i++) {
			if (!equivilant(currentCapabilities.get(i), newCapabilities.get(i))) {
				return true;
			}
		}
		return false;
	}

	private boolean equivilant(ModuleCapability moduleCapability, GenericInfo genericInfo) {
		if (!moduleCapability.getNamespace().equals(genericInfo.getNamespace())) {
			return false;
		}
		if (!moduleCapability.getAttributes().equals(genericInfo.getAttributes())) {
			return false;
		}
		if (!moduleCapability.getDirectives().equals(genericInfo.getDirectives())) {
			return false;
		}
		return true;
	}

	private void cleanOSGiStorage(Location location, File root) {
		if (location.isReadOnly() || !AdaptorUtil.rm(root)) {
			equinoxContainer.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, "The -clean (osgi.clean) option was not successful. Unable to clean the storage area: " + root.getAbsolutePath(), null); //$NON-NLS-1$
		}
	}

	public ModuleDatabase getModuleDatabase() {
		return moduleDatabase;
	}

	public ModuleContainerAdaptor getAdaptor() {
		return adaptor;
	}

	public ModuleContainer getModuleContainer() {
		return moduleContainer;
	}

	public EquinoxConfiguration getConfiguration() {
		return equinoxContainer.getConfiguration();
	}

	public EquinoxLogServices getLogServices() {
		return equinoxContainer.getLogServices();
	}

	public boolean isReadOnly() {
		return osgiLocation.isReadOnly();
	}

	public URLConnection getContentConnection(Module module, String bundleLocation, final InputStream in) throws IOException {
		if (in != null) {
			return new URLConnection(null) {
				/**
				 * @throws IOException  
				 */
				public void connect() throws IOException {
					connected = true;
				}

				/**
				 * @throws IOException  
				 */
				public InputStream getInputStream() throws IOException {
					return (in);
				}
			};
		}
		if (module == null) {
			if (bundleLocation == null) {
				throw new IllegalArgumentException("Module and location cannot be null"); //$NON-NLS-1$
			}
			return getContentConnection(bundleLocation);
		}
		return getContentConnection(getUpdateLocation(module));
	}

	private String getUpdateLocation(final Module module) {
		if (System.getSecurityManager() == null)
			return getUpdateLocation0(module);
		return AccessController.doPrivileged(new PrivilegedAction<String>() {
			public String run() {
				return getUpdateLocation0(module);
			}
		});
	}

	String getUpdateLocation0(Module module) {
		ModuleRevision current = module.getCurrentRevision();
		Generation generation = (Generation) current.getRevisionInfo();
		String updateLocation = generation.getHeaders().get(Constants.BUNDLE_UPDATELOCATION);
		return updateLocation == null ? module.getLocation() : updateLocation;
	}

	private URLConnection getContentConnection(String spec) throws IOException {
		return new URL(spec).openConnection();
	}

	public Generation install(Module origin, String bundleLocation, URLConnection content) throws BundleException {
		if (osgiLocation.isReadOnly()) {
			throw new BundleException("The framework storage area is read only.", BundleException.INVALID_OPERATION); //$NON-NLS-1$
		}
		URL sourceURL = content.getURL();
		InputStream in;
		try {
			in = content.getInputStream();
		} catch (IOException e) {
			throw new BundleException("Error reading bundle content.", e); //$NON-NLS-1$
		}
		boolean isReference = in instanceof ReferenceInputStream;
		File staged = stageContent(in, sourceURL);
		Generation generation = null;
		Long lockedID = getNextRootID();
		try {
			BundleInfo info = new BundleInfo(this, lockedID, 0);
			generation = info.createGeneration();

			File contentFile = getContentFile(staged, isReference, lockedID, generation.getGenerationId());
			generation.setContent(contentFile);
			setStorageHooks(generation);

			ModuleRevisionBuilder builder = getBuilder(generation);
			Module m = moduleContainer.install(origin, bundleLocation, builder, generation);
			if (!lockedID.equals(m.getId())) {
				// this revision is already installed. delete the generation
				generation.delete();
				return (Generation) m.getCurrentRevision().getRevisionInfo();
			}
			return generation;
		} catch (Throwable t) {
			if (!isReference) {
				try {
					delete(staged);
				} catch (IOException e) {
					// tried our best
				}
			}
			if (generation != null) {
				generation.delete();
				generation.getBundleInfo().delete();
			}
			if (t instanceof BundleException) {
				throw (BundleException) t;
			}
			throw new BundleException("Error occurred installing a bundle.", t); //$NON-NLS-1$
		} finally {
			if (generation != null) {
				generation.getBundleInfo().unlockGeneration(generation);
			}
			idLocks.unlock(lockedID);
		}
	}

	private void setStorageHooks(Generation generation) throws BundleException {
		if (generation.getBundleInfo().getBundleId() == 0) {
			return; // ignore system bundle
		}
		List<StorageHookFactory<?, ?, ?>> factories = new ArrayList<StorageHookFactory<?, ?, ?>>(getConfiguration().getHookRegistry().getStorageHookFactories());
		List<StorageHook<?, ?>> hooks = new ArrayList<StorageHook<?, ?>>(factories.size());
		for (Iterator<StorageHookFactory<?, ?, ?>> iFactories = factories.iterator(); iFactories.hasNext();) {
			@SuppressWarnings("unchecked")
			StorageHookFactory<Object, Object, StorageHook<Object, Object>> next = (StorageHookFactory<Object, Object, StorageHook<Object, Object>>) iFactories.next();
			StorageHook<Object, Object> hook = next.createStorageHook(generation);
			hooks.add(hook);
		}
		generation.setStorageHooks(Collections.unmodifiableList(hooks), true);
		for (StorageHook<?, ?> hook : hooks) {
			hook.initialize(generation.getHeaders());
		}
	}

	public ModuleRevisionBuilder getBuilder(Generation generation) throws BundleException {
		Dictionary<String, String> headers = generation.getHeaders();
		Map<String, String> mapHeaders;
		if (headers instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, String> unchecked = (Map<String, String>) headers;
			mapHeaders = unchecked;
		} else {
			mapHeaders = new HashMap<String, String>();
			for (Enumeration<String> eKeys = headers.keys(); eKeys.hasMoreElements();) {
				String key = eKeys.nextElement();
				mapHeaders.put(key, headers.get(key));
			}
		}
		if (generation.getBundleInfo().getBundleId() != 0) {
			return OSGiManifestBuilderFactory.createBuilder(mapHeaders);
		}
		// First we must make sure the VM profile has been loaded
		loadVMProfile(generation);
		// dealing with system bundle find the extra capabilities and exports
		String extraCapabilities = getSystemExtraCapabilities();
		String extraExports = getSystemExtraPackages();
		return OSGiManifestBuilderFactory.createBuilder(mapHeaders, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, extraExports, extraCapabilities);
	}

	private String getSystemExtraCapabilities() {
		EquinoxConfiguration equinoxConfig = equinoxContainer.getConfiguration();
		StringBuilder result = new StringBuilder();

		String systemCapabilities = equinoxConfig.getConfiguration(Constants.FRAMEWORK_SYSTEMCAPABILITIES);
		if (systemCapabilities != null) {
			result.append(systemCapabilities).append(", "); //$NON-NLS-1$
		}

		String extraSystemCapabilities = equinoxConfig.getConfiguration(Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA);
		if (extraSystemCapabilities != null) {
			result.append(extraSystemCapabilities).append(", "); //$NON-NLS-1$
		}

		result.append(EclipsePlatformNamespace.ECLIPSE_PLATFORM_NAMESPACE).append("; "); //$NON-NLS-1$
		result.append(EquinoxConfiguration.PROP_OSGI_OS).append("=").append(equinoxConfig.getOS()).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
		result.append(EquinoxConfiguration.PROP_OSGI_WS).append("=").append(equinoxConfig.getWS()).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
		result.append(EquinoxConfiguration.PROP_OSGI_ARCH).append("=").append(equinoxConfig.getOSArch()).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
		result.append(EquinoxConfiguration.PROP_OSGI_NL).append("=").append(equinoxConfig.getNL()); //$NON-NLS-1$

		String osName = equinoxConfig.getConfiguration(Constants.FRAMEWORK_OS_NAME);
		osName = osName == null ? null : osName.toLowerCase();
		String processor = equinoxConfig.getConfiguration(Constants.FRAMEWORK_PROCESSOR);
		processor = processor == null ? null : processor.toLowerCase();
		String osVersion = equinoxConfig.getConfiguration(Constants.FRAMEWORK_OS_VERSION);
		osVersion = osVersion == null ? null : osVersion.toLowerCase();
		String language = equinoxConfig.getConfiguration(Constants.FRAMEWORK_LANGUAGE);
		language = language == null ? null : language.toLowerCase();

		result.append(", "); //$NON-NLS-1$
		result.append(EquinoxNativeCodeNamespace.EQUINOX_NATIVECODE_NAMESPACE).append("; "); //$NON-NLS-1$
		result.append(EquinoxNativeCodeNamespace.CAPABILITY_OS_NAME_ATTRIBUTE).append("=").append(osName).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
		result.append(EquinoxNativeCodeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE).append("=").append(processor).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
		result.append(EquinoxNativeCodeNamespace.CAPABILITY_OS_VERSION_ATTRIBUTE + ":Version").append("=").append(osVersion).append("; "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		result.append(EquinoxNativeCodeNamespace.CAPABILITY_LANGUAGE_ATTRIBUTE).append("=").append(language); //$NON-NLS-1$
		// TODO need a way to configure in arbitrary native code matching attributes

		return result.toString();
	}

	private String getSystemExtraPackages() {
		EquinoxConfiguration equinoxConfig = equinoxContainer.getConfiguration();
		StringBuilder result = new StringBuilder();

		String systemPackages = equinoxConfig.getConfiguration(Constants.FRAMEWORK_SYSTEMPACKAGES);
		if (systemPackages != null) {
			result.append(systemPackages);
		}

		String extraSystemPackages = equinoxConfig.getConfiguration(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
		if (extraSystemPackages != null) {
			if (result.length() > 0) {
				result.append(", "); //$NON-NLS-1$
			}
			result.append(extraSystemPackages);
		}

		return result.toString();
	}

	public Generation update(Module module, URLConnection content) throws BundleException {
		if (osgiLocation.isReadOnly()) {
			throw new BundleException("The framework storage area is read only.", BundleException.INVALID_OPERATION); //$NON-NLS-1$
		}
		URL sourceURL = content.getURL();
		InputStream in;
		try {
			in = content.getInputStream();
		} catch (IOException e) {
			throw new BundleException("Error reading bundle content.", e); //$NON-NLS-1$
		}
		boolean isReference = in instanceof ReferenceInputStream;
		File staged = stageContent(in, sourceURL);

		ModuleRevision current = module.getCurrentRevision();
		Generation currentGen = (Generation) current.getRevisionInfo();

		BundleInfo bundleInfo = currentGen.getBundleInfo();
		Generation newGen = bundleInfo.createGeneration();

		try {
			File contentFile = getContentFile(staged, isReference, bundleInfo.getBundleId(), newGen.getGenerationId());
			newGen.setContent(contentFile);
			setStorageHooks(newGen);

			ModuleRevisionBuilder builder = getBuilder(newGen);
			moduleContainer.update(module, builder, newGen);
		} catch (Throwable t) {
			if (!isReference) {
				try {
					delete(staged);
				} catch (IOException e) {
					// tried our best
				}
			}
			newGen.delete();
			if (t instanceof BundleException) {
				throw (BundleException) t;
			}
			throw new BundleException("Error occurred installing a bundle.", t); //$NON-NLS-1$
		} finally {
			bundleInfo.unlockGeneration(newGen);
		}
		return newGen;
	}

	private File getContentFile(final File staged, final boolean isReference, final long bundleID, final long generationID) throws BundleException {
		if (System.getSecurityManager() == null)
			return getContentFile0(staged, isReference, bundleID, generationID);
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<File>() {
				public File run() throws BundleException {
					return getContentFile0(staged, isReference, bundleID, generationID);
				}
			});
		} catch (PrivilegedActionException e) {
			if (e.getException() instanceof BundleException)
				throw (BundleException) e.getException();
			throw (RuntimeException) e.getException();
		}
	}

	File getContentFile0(File staged, boolean isReference, long bundleID, long generationID) throws BundleException {
		File contentFile;
		if (!isReference) {
			File generationRoot = new File(childRoot, bundleID + "/" + generationID); //$NON-NLS-1$
			if (!generationRoot.mkdirs()) {
				throw new BundleException("Could not create generation directory: " + generationRoot.getAbsolutePath()); //$NON-NLS-1$
			}
			contentFile = new File(generationRoot, BUNDLE_FILE_NAME);
			if (!staged.renameTo(contentFile)) {
				throw new BundleException("Error while renaming bundle file to final location: " + contentFile); //$NON-NLS-1$
			}
		} else {
			contentFile = staged;
		}
		return contentFile;
	}

	public File getFile(String path, boolean checkParent) {
		// first check the child location
		File childPath = new File(childRoot, path);
		// now check the parent
		if (checkParent && parentRoot != null) {
			if (childPath.exists()) {
				return childPath;
			}
			File parentPath = new File(parentRoot, path);
			if (parentPath.exists()) {
				// only use the parent file only if it exists;
				return parentPath;
			}
		}
		// did not exist in both locations; use the child path
		return childPath;
	}

	private File stageContent(final InputStream in, final URL sourceURL) throws BundleException {
		if (System.getSecurityManager() == null)
			return stageContent0(in, sourceURL);
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<File>() {
				public File run() throws BundleException {
					return stageContent0(in, sourceURL);
				}
			});
		} catch (PrivilegedActionException e) {
			if (e.getException() instanceof BundleException)
				throw (BundleException) e.getException();
			throw (RuntimeException) e.getException();
		}
	}

	File stageContent0(InputStream in, URL sourceURL) throws BundleException {
		File outFile = null;
		try {
			if (in instanceof ReferenceInputStream) {
				URL reference = ((ReferenceInputStream) in).getReference();
				if (!"file".equals(reference.getProtocol())) //$NON-NLS-1$
					throw new BundleException(NLS.bind(AdaptorMsg.ADAPTOR_URL_CREATE_EXCEPTION, reference));
				return new File(reference.getPath());
			}

			outFile = File.createTempFile(BUNDLE_FILE_NAME, ".tmp", childRoot); //$NON-NLS-1$
			String protocol = sourceURL == null ? null : sourceURL.getProtocol();

			if ("file".equals(protocol)) { //$NON-NLS-1$
				File inFile = new File(sourceURL.getPath());
				if (inFile.isDirectory()) {
					// need to delete the outFile because it is not a directory
					outFile.delete();
					AdaptorUtil.copyDir(inFile, outFile);
				} else {
					AdaptorUtil.readFile(in, outFile);
				}
			} else {
				AdaptorUtil.readFile(in, outFile);
			}
			return outFile;
		} catch (IOException e) {
			if (outFile != null) {
				outFile.delete();
			}
			throw new BundleException(AdaptorMsg.BUNDLE_READ_EXCEPTION, BundleException.READ_ERROR, e);
		}
	}

	private Long getNextRootID() throws BundleException {
		moduleDatabase.lockRead();
		try {
			Long nextID = moduleDatabase.getNextId();
			boolean lockedID;
			try {
				lockedID = idLocks.tryLock(nextID, 5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new BundleException("Failed to obtain id locks for installation.", BundleException.STATECHANGE_ERROR, e); //$NON-NLS-1$
			}
			if (!lockedID) {
				throw new BundleException("Failed to obtain id locks for installation.", BundleException.STATECHANGE_ERROR); //$NON-NLS-1$
			}
			return nextID;
		} finally {
			moduleDatabase.unlockRead();
		}
	}

	/**
	 * Attempts to set the permissions of the file in a system dependent way.
	 * @param file the file to set the permissions on
	 */
	public void setPermissions(File file) {
		String commandProp = getConfiguration().getConfiguration(EquinoxConfiguration.PROP_SETPERMS_CMD);
		if (commandProp == null)
			commandProp = getConfiguration().getConfiguration(Constants.FRAMEWORK_EXECPERMISSION);
		if (commandProp == null)
			return;
		String[] temp = ManifestElement.getArrayFromList(commandProp, " "); //$NON-NLS-1$
		List<String> command = new ArrayList<String>(temp.length + 1);
		boolean foundFullPath = false;
		for (int i = 0; i < temp.length; i++) {
			if ("[fullpath]".equals(temp[i]) || "${abspath}".equals(temp[i])) { //$NON-NLS-1$ //$NON-NLS-2$
				command.add(file.getAbsolutePath());
				foundFullPath = true;
			} else
				command.add(temp[i]);
		}
		if (!foundFullPath)
			command.add(file.getAbsolutePath());
		try {
			Runtime.getRuntime().exec(command.toArray(new String[command.size()])).waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public BundleFile createBundleFile(File content, Generation generation, boolean isDirectory, boolean isBase) {
		BundleFile result;
		try {
			if (isDirectory) {
				result = new DirBundleFile(content);
			} else {
				result = new ZipBundleFile(content, generation, mruList);
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not create bundle file.", e); //$NON-NLS-1$
		}
		return wrapBundleFile(result, generation, isBase);
	}

	public BundleFile createNestedBundleFile(String nestedDir, BundleFile bundleFile, Generation generation) {
		// here we assume the content is a path offset into the base bundle file;  create a NestedDirBundleFile
		return wrapBundleFile(new NestedDirBundleFile(bundleFile, nestedDir), generation, false);
	}

	public BundleFile wrapBundleFile(BundleFile bundleFile, Generation generation, boolean isBase) {
		// try creating a wrapper bundlefile out of it.
		List<BundleFileWrapperFactoryHook> wrapperFactories = getConfiguration().getHookRegistry().getBundleFileWrapperFactoryHooks();
		BundleFileWrapperChain wrapped = wrapperFactories.isEmpty() ? null : new BundleFileWrapperChain(bundleFile, null);
		for (BundleFileWrapperFactoryHook wrapperFactory : wrapperFactories) {
			BundleFile wrapperBundle = wrapperFactory.wrapBundleFile(bundleFile, generation, isBase);
			if (wrapperBundle != null && wrapperBundle != bundleFile)
				bundleFile = wrapped = new BundleFileWrapperChain(wrapperBundle, wrapped);
		}

		return bundleFile;
	}

	public void compact() {
		if (!osgiLocation.isReadOnly()) {
			compact(childRoot);
		}
	}

	private void compact(File directory) {
		if (getConfiguration().getDebug().DEBUG_GENERAL)
			Debug.println("compact(" + directory.getPath() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		String list[] = directory.list();
		if (list == null)
			return;

		int len = list.length;
		for (int i = 0; i < len; i++) {
			if (BUNDLE_DATA_DIR.equals(list[i]))
				continue; /* do not examine the bundles data dir. */
			File target = new File(directory, list[i]);
			// if the file is a directory
			if (!target.isDirectory())
				continue;
			File delete = new File(target, DELETE_FLAG);
			// and the directory is marked for delete
			if (delete.exists()) {
				// if rm fails to delete the directory and .delete was removed
				if (!AdaptorUtil.rm(target) && !delete.exists()) {
					try {
						// recreate .delete
						FileOutputStream out = new FileOutputStream(delete);
						out.close();
					} catch (IOException e) {
						if (getConfiguration().getDebug().DEBUG_GENERAL)
							Debug.println("Unable to write " + delete.getPath() + ": " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			} else {
				compact(target); /* descend into directory */
			}
		}
	}

	void delete(final File delete) throws IOException {
		if (System.getSecurityManager() == null) {
			delete0(delete);
		} else {
			try {
				AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
					public Object run() throws IOException {
						delete0(delete);
						return null;
					}
				});
			} catch (PrivilegedActionException e) {
				if (e.getException() instanceof IOException)
					throw (IOException) e.getException();
				throw (RuntimeException) e.getException();
			}
		}
	}

	void delete0(File delete) throws IOException {
		if (!AdaptorUtil.rm(delete)) {
			/* create .delete */
			FileOutputStream out = new FileOutputStream(new File(delete, DELETE_FLAG));
			out.close();
		}
	}

	public void save() throws IOException {
		if (isReadOnly()) {
			return;
		}
		if (System.getSecurityManager() == null) {
			save0();
		} else {
			try {
				AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
					public Object run() throws IOException {
						save0();
						return null;
					}
				});
			} catch (PrivilegedActionException e) {
				if (e.getException() instanceof IOException)
					throw (IOException) e.getException();
				throw (RuntimeException) e.getException();
			}
		}
	}

	void save0() throws IOException {
		DataOutputStream out = null;
		boolean lockedLocation = false;
		moduleDatabase.lockRead();
		try {
			lockedLocation = osgiLocation.lock();
			synchronized (this.saveMonitor) {
				if (lastSavedTimestamp == moduleDatabase.getTimestamp())
					return;
				out = new DataOutputStream(new FileOutputStream(getFile(FRAMEWORK_INFO, false)));
				saveGenerations(out);
				savePermissionData(out);
				moduleDatabase.store(out, true);
				lastSavedTimestamp = moduleDatabase.getTimestamp();
			}
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// tried our best
				}
			}
			if (lockedLocation) {
				osgiLocation.release();
			}
			moduleDatabase.unlockRead();
		}
	}

	private void savePermissionData(DataOutputStream out) throws IOException {
		permissionData.savePermissionData(out);
	}

	private void saveGenerations(DataOutputStream out) throws IOException {
		List<Module> modules = moduleContainer.getModules();
		List<Generation> generations = new ArrayList<Generation>();
		for (Module module : modules) {
			ModuleRevision revision = module.getCurrentRevision();
			if (revision != null) {
				Generation generation = (Generation) revision.getRevisionInfo();
				if (generation != null) {
					generations.add(generation);
				}
			}
		}
		out.writeInt(VERSION);
		out.writeInt(generations.size());
		for (Generation generation : generations) {
			BundleInfo bundleInfo = generation.getBundleInfo();
			out.writeLong(bundleInfo.getBundleId());
			out.writeLong(bundleInfo.getNextGenerationId());
			out.writeLong(generation.getGenerationId());
			out.writeBoolean(generation.isDirectory());
			out.writeBoolean(generation.hasPackageInfo());
			if (bundleInfo.getBundleId() == 0) {
				// just write empty string for system bundle content in this case
				out.writeUTF(""); //$NON-NLS-1$
			} else {
				out.writeUTF(new FilePath(installPath).makeRelative(new FilePath(generation.getContent().getAbsolutePath())));
			}
		}

		saveStorageHookData(out, generations);
	}

	private void saveStorageHookData(DataOutputStream out, List<Generation> generations) throws IOException {
		List<StorageHookFactory<?, ?, ?>> factories = getConfiguration().getHookRegistry().getStorageHookFactories();
		out.writeInt(factories.size());
		for (StorageHookFactory<?, ?, ?> factory : factories) {
			out.writeUTF(factory.getKey());
			out.writeInt(factory.getStorageVersion());

			// create a temporary in memory stream so we can figure out the length
			ByteArrayOutputStream tempBytes = new ByteArrayOutputStream();
			DataOutputStream temp = new DataOutputStream(tempBytes);
			try {
				Object saveContext = factory.createSaveContext();
				for (Generation generation : generations) {
					if (generation.getBundleInfo().getBundleId() == 0) {
						continue; // ignore system bundle
					}
					@SuppressWarnings({"rawtypes", "unchecked"})
					StorageHook<Object, Object> hook = generation.getStorageHook((Class) factory.getClass());
					hook.save(saveContext, temp);
				}
			} finally {
				temp.close();
			}
			out.writeInt(tempBytes.size());
			out.write(tempBytes.toByteArray());
		}
	}

	private Map<Long, Generation> loadGenerations(DataInputStream in) throws IOException {
		if (in == null) {
			return new HashMap<Long, Generation>(0);
		}
		int version = in.readInt();
		if (version < VERSION)
			throw new IOException("Perstence version is not correct for loading: " + version + " expecting: " + VERSION); //$NON-NLS-1$ //$NON-NLS-2$

		int numInfos = in.readInt();
		Map<Long, Generation> result = new HashMap<Long, Generation>(numInfos);
		List<Generation> generations = new ArrayList<BundleInfo.Generation>(numInfos);
		for (int i = 0; i < numInfos; i++) {
			long infoId = in.readLong();
			long nextGenId = in.readLong();
			long generationId = in.readLong();
			boolean isDirectory = in.readBoolean();
			boolean hasPackageInfo = in.readBoolean();
			String contentPath = in.readUTF();

			File content;
			if (infoId == 0) {
				content = getSystemContent();
				isDirectory = content != null ? content.isDirectory() : false;
			} else {
				content = new File(contentPath);
			}

			if (content != null && !content.isAbsolute()) {
				// make sure it has the absolute location instead
				content = new File(installPath, contentPath);
			}

			BundleInfo info = new BundleInfo(this, infoId, nextGenId);
			Generation generation = info.restoreGeneration(generationId, content, isDirectory, hasPackageInfo);
			result.put(infoId, generation);
			generations.add(generation);
		}

		loadStorageHookData(generations, in);
		return result;
	}

	private void loadStorageHookData(List<Generation> generations, DataInputStream in) throws IOException {
		List<StorageHookFactory<?, ?, ?>> factories = new ArrayList<StorageHookFactory<?, ?, ?>>(getConfiguration().getHookRegistry().getStorageHookFactories());
		Map<Generation, List<StorageHook<?, ?>>> hookMap = new HashMap<Generation, List<StorageHook<?, ?>>>();
		int numFactories = in.readInt();
		for (int i = 0; i < numFactories; i++) {
			String factoryName = in.readUTF();
			int version = in.readInt();
			StorageHookFactory<Object, Object, StorageHook<Object, Object>> factory = null;
			for (Iterator<StorageHookFactory<?, ?, ?>> iFactories = factories.iterator(); iFactories.hasNext();) {
				@SuppressWarnings("unchecked")
				StorageHookFactory<Object, Object, StorageHook<Object, Object>> next = (StorageHookFactory<Object, Object, StorageHook<Object, Object>>) iFactories.next();
				if (next.getKey().equals(factoryName)) {
					factory = next;
					iFactories.remove();
					break;
				}
			}
			int dataSize = in.readInt();
			byte[] bytes = new byte[dataSize];
			in.readFully(bytes);
			if (factory != null) {
				DataInputStream temp = new DataInputStream(new ByteArrayInputStream(bytes));
				try {
					if (factory.isCompatibleWith(version)) {
						Object loadContext = factory.createLoadContext(version);
						for (Generation generation : generations) {
							if (generation.getBundleInfo().getBundleId() == 0) {
								continue; // ignore system bundle
							}
							StorageHook<Object, Object> hook = factory.createStorageHook(generation);
							hook.load(loadContext, temp);
							getHooks(hookMap, generation).add(hook);
						}
					} else {
						// recover by reinitializing the hook
						for (Generation generation : generations) {
							if (generation.getBundleInfo().getBundleId() == 0) {
								continue; // ignore system bundle
							}
							StorageHook<Object, Object> hook = factory.createStorageHook(generation);
							hook.initialize(generation.getHeaders());
							getHooks(hookMap, generation).add(hook);
						}
					}
				} catch (BundleException e) {
					throw new IOException(e);
				} finally {
					temp.close();
				}
			}
		}
		// now we need to recover for any hooks that are left
		for (Iterator<StorageHookFactory<?, ?, ?>> iFactories = factories.iterator(); iFactories.hasNext();) {
			@SuppressWarnings("unchecked")
			StorageHookFactory<Object, Object, StorageHook<Object, Object>> next = (StorageHookFactory<Object, Object, StorageHook<Object, Object>>) iFactories.next();
			// recover by reinitializing the hook
			for (Generation generation : generations) {
				if (generation.getBundleInfo().getBundleId() == 0) {
					continue; // ignore system bundle
				}
				StorageHook<Object, Object> hook = next.createStorageHook(generation);
				try {
					hook.initialize(generation.getHeaders());
					getHooks(hookMap, generation).add(hook);
				} catch (BundleException e) {
					throw new IOException(e);
				}
			}
		}
		// now set the hooks to the generations
		for (Generation generation : generations) {
			generation.setStorageHooks(Collections.unmodifiableList(getHooks(hookMap, generation)), false);
		}
	}

	private static List<StorageHook<?, ?>> getHooks(Map<Generation, List<StorageHook<?, ?>>> hookMap, Generation generation) {
		List<StorageHook<?, ?>> result = hookMap.get(generation);
		if (result == null) {
			result = new ArrayList<StorageHook<?, ?>>();
			hookMap.put(generation, result);
		}
		return result;
	}

	private File getSystemContent() {
		String frameworkValue = equinoxContainer.getConfiguration().getConfiguration(EquinoxConfiguration.PROP_FRAMEWORK);
		if (frameworkValue == null || !frameworkValue.startsWith("file:")) { //$NON-NLS-1$
			return null;
		}
		// TODO assumes the location is a file URL
		File result = new File(frameworkValue.substring(5));
		if (!result.exists()) {
			throw new IllegalStateException("Configured framework location does not exist: " + result.getAbsolutePath()); //$NON-NLS-1$
		}
		return result;
	}

	private void loadVMProfile(Generation systemGeneration) {
		EquinoxConfiguration equinoxConfig = equinoxContainer.getConfiguration();
		Properties profileProps = findVMProfile(systemGeneration);
		String systemExports = equinoxConfig.getConfiguration(Constants.FRAMEWORK_SYSTEMPACKAGES);
		// set the system exports property using the vm profile; only if the property is not already set
		if (systemExports == null) {
			systemExports = profileProps.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
			if (systemExports != null)
				equinoxConfig.setConfiguration(Constants.FRAMEWORK_SYSTEMPACKAGES, systemExports);
		}

		// set the org.osgi.framework.bootdelegation property according to the java profile
		String type = equinoxConfig.getConfiguration(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE_BOOTDELEGATION); // a null value means ignore
		String profileBootDelegation = profileProps.getProperty(Constants.FRAMEWORK_BOOTDELEGATION);
		if (EquinoxConfiguration.PROP_OSGI_BOOTDELEGATION_OVERRIDE.equals(type)) {
			if (profileBootDelegation == null)
				equinoxConfig.clearConfiguration(Constants.FRAMEWORK_BOOTDELEGATION); // override with a null value
			else
				equinoxConfig.setConfiguration(Constants.FRAMEWORK_BOOTDELEGATION, profileBootDelegation); // override with the profile value
		} else if (EquinoxConfiguration.PROP_OSGI_BOOTDELEGATION_NONE.equals(type))
			equinoxConfig.clearConfiguration(Constants.FRAMEWORK_BOOTDELEGATION); // remove the bootdelegation property in case it was set
		// set the org.osgi.framework.executionenvironment property according to the java profile
		if (equinoxConfig.getConfiguration(Constants.FRAMEWORK_EXECUTIONENVIRONMENT) == null) {
			// get the ee from the java profile; if no ee is defined then try the java profile name
			String ee = profileProps.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, profileProps.getProperty(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE_NAME));
			if (ee != null)
				equinoxConfig.setConfiguration(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, ee);
		}
		// set the org.osgi.framework.system.capabilities property according to the java profile
		if (equinoxConfig.getConfiguration(Constants.FRAMEWORK_SYSTEMCAPABILITIES) == null) {
			String systemCapabilities = profileProps.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES);
			if (systemCapabilities != null)
				equinoxConfig.setConfiguration(Constants.FRAMEWORK_SYSTEMCAPABILITIES, systemCapabilities);
		}
	}

	private Properties findVMProfile(Generation systemGeneration) {
		Properties result = new Properties();
		// Find the VM profile name using J2ME properties
		String j2meConfig = System.getProperty(EquinoxConfiguration.PROP_J2ME_MICROEDITION_CONFIGURATION);
		String j2meProfiles = System.getProperty(EquinoxConfiguration.PROP_J2ME_MICROEDITION_PROFILES);
		String vmProfile = null;
		String javaEdition = null;
		Version javaVersion = null;
		if (j2meConfig != null && j2meConfig.length() > 0 && j2meProfiles != null && j2meProfiles.length() > 0) {
			// save the vmProfile based off of the config and profile
			// use the last profile; assuming that is the highest one
			String[] j2meProfileList = ManifestElement.getArrayFromList(j2meProfiles, " "); //$NON-NLS-1$
			if (j2meProfileList != null && j2meProfileList.length > 0)
				vmProfile = j2meConfig + '_' + j2meProfileList[j2meProfileList.length - 1];
		} else {
			// No J2ME properties; use J2SE properties
			// Note that the CDC spec appears not to require VM implementations to set the
			// javax.microedition properties!!  So we will try to fall back to the 
			// java.specification.name property, but this is pretty ridiculous!!
			String javaSpecVersion = System.getProperty(EquinoxConfiguration.PROP_JVM_SPEC_VERSION);
			// set the profile and EE based off of the java.specification.version
			// TODO We assume J2ME Foundation and J2SE here.  need to support other profiles J2EE ...
			if (javaSpecVersion != null) {
				StringTokenizer st = new StringTokenizer(javaSpecVersion, " _-"); //$NON-NLS-1$
				javaSpecVersion = st.nextToken();
				String javaSpecName = System.getProperty(EquinoxConfiguration.PROP_JVM_SPEC_NAME);
				// See bug 291269 we check for Foundation Specification and Foundation Profile Specification
				if (javaSpecName != null && (javaSpecName.indexOf("Foundation Specification") >= 0 || javaSpecName.indexOf("Foundation Profile Specification") >= 0)) //$NON-NLS-1$ //$NON-NLS-2$
					vmProfile = "CDC-" + javaSpecVersion + "_Foundation-" + javaSpecVersion; //$NON-NLS-1$ //$NON-NLS-2$
				else {
					// look for JavaSE if 1.6 or greater; otherwise look for J2SE
					Version v16 = new Version("1.6"); //$NON-NLS-1$
					javaEdition = J2SE;
					try {
						javaVersion = new Version(javaSpecVersion);
						if (v16.compareTo(javaVersion) <= 0)
							javaEdition = JAVASE;
					} catch (IllegalArgumentException e) {
						// do nothing
					}
					vmProfile = javaEdition + javaSpecVersion;
				}
			}
		}
		InputStream profileIn = null;
		// check for the java profile property for a url
		String propJavaProfile = equinoxContainer.getConfiguration().getConfiguration(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE);
		if (propJavaProfile != null)
			try {
				// we assume a URL
				profileIn = new URL(propJavaProfile).openStream();
			} catch (IOException e) {
				// try using a relative path in the system bundle
				profileIn = findInSystemBundle(systemGeneration, propJavaProfile);
			}
		if (profileIn == null && vmProfile != null) {
			// look for a profile in the system bundle based on the vm profile
			String javaProfile = vmProfile + PROFILE_EXT;
			profileIn = findInSystemBundle(systemGeneration, javaProfile);
			if (profileIn == null)
				profileIn = getNextBestProfile(systemGeneration, javaEdition, javaVersion);
		}
		if (profileIn == null)
			// the profile url is still null then use the osgi min profile in OSGi by default
			profileIn = findInSystemBundle(systemGeneration, "JavaSE-1.6.profile"); //$NON-NLS-1$
		if (profileIn != null) {
			try {
				result.load(new BufferedInputStream(profileIn));
			} catch (IOException e) {
				// TODO consider logging ...
			} finally {
				try {
					profileIn.close();
				} catch (IOException ee) {
					// do nothing
				}
			}
		}
		// set the profile name if it does not provide one
		if (result.getProperty(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE_NAME) == null)
			if (vmProfile != null)
				result.put(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE_NAME, vmProfile.replace('_', '/'));
			else
				// last resort; default to the absolute minimum profile name for the framework
				result.put(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE_NAME, "JavaSE-1.6"); //$NON-NLS-1$
		return result;
	}

	private InputStream getNextBestProfile(Generation systemGeneration, String javaEdition, Version javaVersion) {
		if (javaVersion == null || (javaEdition != J2SE && javaEdition != JAVASE))
			return null; // we cannot automatically choose the next best profile unless this is a J2SE or JavaSE vm
		InputStream bestProfile = findNextBestProfile(systemGeneration, javaEdition, javaVersion);
		if (bestProfile == null && javaEdition == JAVASE)
			// if this is a JavaSE VM then search for a lower J2SE profile
			bestProfile = findNextBestProfile(systemGeneration, J2SE, javaVersion);
		return bestProfile;
	}

	private InputStream findNextBestProfile(Generation systemGeneration, String javaEdition, Version javaVersion) {
		InputStream result = null;
		int minor = javaVersion.getMinor();
		do {
			result = findInSystemBundle(systemGeneration, javaEdition + javaVersion.getMajor() + "." + minor + PROFILE_EXT); //$NON-NLS-1$
			minor = minor - 1;
		} while (result == null && minor > 0);
		return result;
	}

	private InputStream findInSystemBundle(Generation systemGeneration, String entry) {
		BundleFile systemContent = systemGeneration.getBundleFile();
		BundleEntry systemEntry = systemContent != null ? systemContent.getEntry(entry) : null;
		InputStream result = null;
		if (systemEntry != null) {
			try {
				result = systemEntry.getInputStream();
			} catch (IOException e) {
				// Do nothing
			}
		}
		if (result == null) {
			// Check the ClassLoader in case we're launched off the Java boot classpath
			ClassLoader loader = getClass().getClassLoader();
			result = loader == null ? ClassLoader.getSystemResourceAsStream(entry) : loader.getResourceAsStream(entry);
		}
		return result;
	}

	public static Enumeration<URL> findEntries(List<Generation> generations, String path, String filePattern, int options) {
		List<BundleFile> bundleFiles = new ArrayList<BundleFile>(generations.size());
		for (Generation generation : generations)
			bundleFiles.add(generation.getBundleFile());
		// search all the bundle files
		List<String> pathList = listEntryPaths(bundleFiles, path, filePattern, options);
		// return null if no entries found
		if (pathList.size() == 0)
			return null;
		// create an enumeration to enumerate the pathList
		final String[] pathArray = pathList.toArray(new String[pathList.size()]);
		final Generation[] generationArray = generations.toArray(new Generation[generations.size()]);
		return new Enumeration<URL>() {
			private int curPathIndex = 0;
			private int curDataIndex = 0;
			private URL nextElement = null;

			public boolean hasMoreElements() {
				if (nextElement != null)
					return true;
				getNextElement();
				return nextElement != null;
			}

			public URL nextElement() {
				if (!hasMoreElements())
					throw new NoSuchElementException();
				URL result = nextElement;
				// force the next element search
				getNextElement();
				return result;
			}

			private void getNextElement() {
				nextElement = null;
				if (curPathIndex >= pathArray.length)
					// reached the end of the pathArray; no more elements
					return;
				while (nextElement == null && curPathIndex < pathArray.length) {
					String curPath = pathArray[curPathIndex];
					// search the generation until we have searched them all
					while (nextElement == null && curDataIndex < generationArray.length)
						nextElement = generationArray[curDataIndex++].getEntry(curPath);
					// we have searched all datas then advance to the next path 
					if (curDataIndex >= generationArray.length) {
						curPathIndex++;
						curDataIndex = 0;
					}
				}
			}
		};
	}

	/**
	 * Returns the names of resources available from a list of bundle files.
	 * No duplicate resource names are returned, each name is unique.
	 * @param bundleFiles the list of bundle files to search in
	 * @param path The path name in which to look.
	 * @param filePattern The file name pattern for selecting resource names in
	 *        the specified path.
	 * @param options The options for listing resource names.
	 * @return a list of resource names.  If no resources are found then
	 * the empty list is returned.
	 * @see BundleWiring#listResources(String, String, int)
	 */
	public static List<String> listEntryPaths(List<BundleFile> bundleFiles, String path, String filePattern, int options) {
		// a list used to store the results of the search
		List<String> pathList = new ArrayList<String>();
		Filter patternFilter = null;
		Hashtable<String, String> patternProps = null;
		if (filePattern != null) {
			// Optimization: If the file pattern does not include a wildcard  or escape  char then it must represent a single file.
			// Avoid pattern matching and use BundleFile.getEntry() if recursion was not requested.
			if ((options & BundleWiring.FINDENTRIES_RECURSE) == 0 && filePattern.indexOf('*') == -1 && filePattern.indexOf('\\') == -1) {
				if (path.length() == 0)
					path = filePattern;
				else
					path += path.charAt(path.length() - 1) == '/' ? filePattern : '/' + filePattern;
				for (BundleFile bundleFile : bundleFiles) {
					if (bundleFile.getEntry(path) != null && !pathList.contains(path))
						pathList.add(path);
				}
				return pathList;
			}
			// For when the file pattern includes a wildcard.
			try {
				// create a file pattern filter with 'filename' as the key
				patternFilter = FilterImpl.newInstance("(filename=" + sanitizeFilterInput(filePattern) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				// create a single hashtable to be shared during the recursive search
				patternProps = new Hashtable<String, String>(2);
			} catch (InvalidSyntaxException e) {
				// TODO something unexpected happened; log error and return nothing
				//				Bundle b = context == null ? null : context.getBundle();
				//				eventPublisher.publishFrameworkEvent(FrameworkEvent.ERROR, b, e);
				return pathList;
			}
		}
		// find the entry paths for the datas
		for (BundleFile bundleFile : bundleFiles) {
			listEntryPaths(bundleFile, path, patternFilter, patternProps, options, pathList);
		}
		return pathList;
	}

	private static String sanitizeFilterInput(String filePattern) throws InvalidSyntaxException {
		StringBuffer buffer = null;
		boolean foundEscape = false;
		for (int i = 0; i < filePattern.length(); i++) {
			char c = filePattern.charAt(i);
			switch (c) {
				case '\\' :
					// we either used the escape found or found a new escape.
					foundEscape = foundEscape ? false : true;
					if (buffer != null)
						buffer.append(c);
					break;
				case '(' :
				case ')' :
					if (!foundEscape) {
						if (buffer == null) {
							buffer = new StringBuffer(filePattern.length() + 16);
							buffer.append(filePattern.substring(0, i));
						}
						// must escape with '\'
						buffer.append('\\');
					} else {
						foundEscape = false; // used the escape found
					}
					if (buffer != null)
						buffer.append(c);
					break;
				default :
					// if we found an escape it has been used
					foundEscape = false;
					if (buffer != null)
						buffer.append(c);
					break;
			}
		}
		if (foundEscape)
			throw new InvalidSyntaxException("Trailing escape characters must be escaped.", filePattern); //$NON-NLS-1$
		return buffer == null ? filePattern : buffer.toString();
	}

	private static List<String> listEntryPaths(BundleFile bundleFile, String path, Filter patternFilter, Hashtable<String, String> patternProps, int options, List<String> pathList) {
		if (pathList == null)
			pathList = new ArrayList<String>();
		Enumeration<String> entryPaths = bundleFile.getEntryPaths(path);
		if (entryPaths == null)
			return pathList;
		while (entryPaths.hasMoreElements()) {
			String entry = entryPaths.nextElement();
			int lastSlash = entry.lastIndexOf('/');
			if (patternProps != null) {
				int secondToLastSlash = entry.lastIndexOf('/', lastSlash - 1);
				int fileStart;
				int fileEnd = entry.length();
				if (lastSlash < 0)
					fileStart = 0;
				else if (lastSlash != entry.length() - 1)
					fileStart = lastSlash + 1;
				else {
					fileEnd = lastSlash; // leave the lastSlash out
					if (secondToLastSlash < 0)
						fileStart = 0;
					else
						fileStart = secondToLastSlash + 1;
				}
				String fileName = entry.substring(fileStart, fileEnd);
				// set the filename to the current entry
				patternProps.put("filename", fileName); //$NON-NLS-1$
			}
			// prevent duplicates and match on the patternFilter
			if (!pathList.contains(entry) && (patternFilter == null || patternFilter.matchCase(patternProps)))
				pathList.add(entry);
			// recurse only into entries that are directories
			if (((options & BundleWiring.FINDENTRIES_RECURSE) != 0) && !entry.equals(path) && entry.length() > 0 && lastSlash == (entry.length() - 1))
				listEntryPaths(bundleFile, entry, patternFilter, patternProps, options, pathList);
		}
		return pathList;
	}

	public String copyToTempLibrary(Generation generation, String absolutePath) {
		File libTempDir = new File(childRoot, LIB_TEMP);
		// we assume the absolutePath is a File path
		File realLib = new File(absolutePath);
		String libName = realLib.getName();
		// find a temp dir for the bundle data and the library;
		File bundleTempDir = null;
		File libTempFile = null;
		// We need a somewhat predictable temp dir for the libraries of a given bundle;
		// This is not strictly necessary but it does help scenarios where one native library loads another native library without using java.
		// On some OSes this causes issues because the second library is cannot be found.
		// This has been worked around by the bundles loading the libraries in a particular order (and setting some LIB_PATH env).
		// The one catch is that the libraries need to be in the same directory and they must use their original lib names.
		//
		// This bit of code attempts to do that by using the bundle ID as an ID for the temp dir along with an incrementing ID 
		// in cases where the temp dir may already exist.
		Long bundleID = new Long(generation.getBundleInfo().getBundleId());
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			bundleTempDir = new File(libTempDir, bundleID.toString() + "_" + new Integer(i).toString()); //$NON-NLS-1$
			libTempFile = new File(bundleTempDir, libName);
			if (bundleTempDir.exists()) {
				if (libTempFile.exists())
					continue; // to to next temp file
				break;
			}
			break;
		}
		if (!bundleTempDir.exists()) {
			bundleTempDir.mkdirs();
			bundleTempDir.deleteOnExit();
			// This is just a safeguard incase the VM is terminated unexpectantly, it also looks like deleteOnExit cannot really work because
			// the VM likely will still have a lock on the lib file at the time of VM exit.
			File deleteFlag = new File(libTempDir, DELETE_FLAG);
			if (!deleteFlag.exists()) {
				// need to create a delete flag to force removal the temp libraries
				try {
					FileOutputStream out = new FileOutputStream(deleteFlag);
					out.close();
				} catch (IOException e) {
					// do nothing; that would mean we did not make the temp dir successfully
				}
			}
		}
		// copy the library file
		try {
			InputStream in = new FileInputStream(realLib);
			AdaptorUtil.readFile(in, libTempFile);
			// set permissions if needed
			setPermissions(libTempFile);
			libTempFile.deleteOnExit(); // this probably will not work because the VM will probably have the lib locked at exit
			// return the temporary path
			return libTempFile.getAbsolutePath();
		} catch (IOException e) {
			equinoxContainer.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, e.getMessage(), e);
			return null;
		}
	}

	public SecurityAdmin getSecurityAdmin() {
		return securityAdmin;
	}
}
