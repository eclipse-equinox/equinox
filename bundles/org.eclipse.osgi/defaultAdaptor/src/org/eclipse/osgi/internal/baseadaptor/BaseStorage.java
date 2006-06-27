/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.baseadaptor;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.eclipse.core.runtime.adaptor.LocationManager;
import org.eclipse.core.runtime.internal.adaptor.EclipseAdaptorMsg;
import org.eclipse.osgi.baseadaptor.*;
import org.eclipse.osgi.baseadaptor.bundlefile.*;
import org.eclipse.osgi.baseadaptor.hooks.*;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.debug.FrameworkDebugOptions;
import org.eclipse.osgi.framework.internal.core.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.KeyedHashSet;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.storagemanager.ManagedOutputStream;
import org.eclipse.osgi.storagemanager.StorageManager;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;

public class BaseStorage {
	private static final String RUNTIME_ADAPTOR = FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME + "/eclipseadaptor"; //$NON-NLS-1$
	private static final String OPTION_PLATFORM_ADMIN = RUNTIME_ADAPTOR + "/debug/platformadmin"; //$NON-NLS-1$
	private static final String OPTION_PLATFORM_ADMIN_RESOLVER = RUNTIME_ADAPTOR + "/debug/platformadmin/resolver"; //$NON-NLS-1$
	private static final String OPTION_MONITOR_PLATFORM_ADMIN = RUNTIME_ADAPTOR + "/resolver/timing"; //$NON-NLS-1$
	private static final String OPTION_RESOLVER_READER = RUNTIME_ADAPTOR + "/resolver/reader/timing"; //$NON-NLS-1$	
	private static final String PROP_FRAMEWORK_EXTENSIONS = "osgi.framework.extensions"; //$NON-NLS-1$
	private static final String PROP_BUNDLE_STORE = "osgi.bundlestore"; //$NON-NLS-1$
	// The name of the bundle data directory
	static final String DATA_DIR_NAME = "data"; //$NON-NLS-1$
	// System property used to determine whether State saver needs to be enabled
	private static final String PROP_ENABLE_STATE_SAVER = "eclipse.enableStateSaver"; //$NON-NLS-1$
	static final String BUNDLEFILE_NAME = "bundlefile"; //$NON-NLS-1$
	// System property used to clean the osgi configuration area
	private static final String PROP_CLEAN = "osgi.clean"; //$NON-NLS-1$

	/** The current bundle data version */
	public static final byte BUNDLEDATA_VERSION = 18;
	/**
	 * flag to indicate a framework extension is being intialized
	 */
	public static final byte EXTENSION_INITIALIZE = 0x01;
	/**
	 * flag to indicate a framework extension is being installed
	 */
	public static final byte EXTENSION_INSTALLED = 0x02;
	/**
	 * flag to indicate a framework extension is being uninstalled
	 */
	public static final byte EXTENSION_UNINSTALLED = 0x04;
	/**
	 * flag to indicate a framework extension is being updated
	 */
	public static final byte EXTENSION_UPDATED = 0x08;
	private static final String PERM_DATA_FILE = ".permdata"; //$NON-NLS-1$
	private static final byte PERMDATA_VERSION = 1;

	private BaseAdaptor adaptor;
	// assume a file: installURL
	private String installPath;
	private StorageManager storageManager;
	private StateManager stateManager;
	// no need to synchronize on storageHooks because the elements are statically set in initialize
	private KeyedHashSet storageHooks = new KeyedHashSet(5, false);
	private BundleContext context;
	/**
	 * The add URL method used to support framework extensions
	 */
	private Method addURLMethod;
	/**
	 * The list of configured framework extensions
	 */
	private String[] configuredExtensions;

	private long timeStamp = 0;
	private int initialBundleStartLevel = 1;
	private long nextId = 1;
	/**
	 * directory containing installed bundles 
	 */
	private File bundleStoreRoot;

	private BasePermissionStorage permissionStorage;
	private StateSaver stateSaver;
	private boolean invalidState;
	private boolean storageManagerClosed;

	BaseStorage() {
		// make constructor package private
	}

	public void initialize(BaseAdaptor adaptor) throws IOException {
		this.adaptor = adaptor;
		setDebugOptions();
		if (Boolean.valueOf(FrameworkProperties.getProperty(BaseStorage.PROP_CLEAN)).booleanValue())
			cleanOSGiCache();
		// initialize the addURLMethod to support framework extensions
		ClassLoader fwloader = BaseStorage.class.getClassLoader();
		if (fwloader != null)
			addURLMethod = findaddURLMethod(fwloader.getClass());
		// we need to set the install path as soon as possible so we can determine
		// the absolute location of install relative URLs
		Location installLoc = LocationManager.getInstallLocation();
		if (installLoc != null) {
			URL installURL = installLoc.getURL();
			// assume install URL is file: based
			installPath = installURL.getPath();
		}
		boolean readOnlyConfiguration = LocationManager.getConfigurationLocation().isReadOnly();
		storageManager = initFileManager(LocationManager.getOSGiConfigurationDir(), readOnlyConfiguration ? "none" : null, readOnlyConfiguration); //$NON-NLS-1$
		storageManagerClosed = false;
		// initialize the storageHooks
		StorageHook[] hooks = adaptor.getHookRegistry().getStorageHooks();
		for (int i = 0; i < hooks.length; i++)
			storageHooks.add(hooks[i]);
	}

	private void setDebugOptions() {
		FrameworkDebugOptions options = FrameworkDebugOptions.getDefault();
		// may be null if debugging is not enabled
		if (options == null)
			return;
		StateManager.DEBUG = options != null;
		StateManager.DEBUG_READER = options.getBooleanOption(OPTION_RESOLVER_READER, false);
		StateManager.MONITOR_PLATFORM_ADMIN = options.getBooleanOption(OPTION_MONITOR_PLATFORM_ADMIN, false);
		StateManager.DEBUG_PLATFORM_ADMIN = options.getBooleanOption(OPTION_PLATFORM_ADMIN, false);
		StateManager.DEBUG_PLATFORM_ADMIN_RESOLVER = options.getBooleanOption(OPTION_PLATFORM_ADMIN_RESOLVER, false);
	}

	protected StorageManager initFileManager(File baseDir, String lockMode, boolean readOnly) {
		StorageManager sManager = new StorageManager(baseDir, lockMode, readOnly);
		try {
			sManager.open(!readOnly);
		} catch (IOException ex) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Error reading framework metadata: " + ex.getMessage()); //$NON-NLS-1$
				Debug.printStackTrace(ex);
			}
			String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_FILEMANAGER_OPEN_ERROR, ex.getMessage());
			FrameworkLogEntry logEntry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, message, 0, ex, null);
			adaptor.getFrameworkLog().log(logEntry);
		}
		return sManager;
	}

	public boolean isReadOnly() {
		return storageManager.isReadOnly();
	}

	public void compact() throws IOException {
		if (!isReadOnly())
			compact(getBundleStoreRoot());
	}

	private void compact(File directory) {
		if (Debug.DEBUG && Debug.DEBUG_GENERAL)
			Debug.println("compact(" + directory.getPath() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		String list[] = directory.list();
		if (list == null)
			return;

		int len = list.length;
		for (int i = 0; i < len; i++) {
			if (BaseStorage.DATA_DIR_NAME.equals(list[i]))
				continue; /* do not examine the bundles data dir. */
			File target = new File(directory, list[i]);
			// if the file is a directory
			if (!target.isDirectory())
				continue;
			File delete = new File(target, ".delete"); //$NON-NLS-1$
			// and the directory is marked for delete
			if (delete.exists()) {
				// if rm fails to delete the directory and .delete was removed
				if (!AdaptorUtil.rm(target) && !delete.exists()) {
					try {
						// recreate .delete
						FileOutputStream out = new FileOutputStream(delete);
						out.close();
					} catch (IOException e) {
						if (Debug.DEBUG && Debug.DEBUG_GENERAL)
							Debug.println("Unable to write " + delete.getPath() + ": " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			} else {
				compact(target); /* descend into directory */
			}
		}
	}

	public long getFreeSpace() throws IOException {
		// cannot implement this without native code!
		return -1;
	}

	public File getDataFile(BaseData data, String path) {
		BaseStorageHook storageHook = (BaseStorageHook) data.getStorageHook(BaseStorageHook.KEY);
		if (storageHook == null)
			return null;
		return storageHook.getDataFile(path);
	}

	BaseAdaptor getAdaptor() {
		return adaptor;
	}

	public void installNativeCode(BaseData data, String[] nativepaths) throws BundleException {
		if (nativepaths.length > 0) {
			BaseStorageHook storageHook = (BaseStorageHook) data.getStorageHook(BaseStorageHook.KEY);
			if (storageHook != null)
				storageHook.installNativePaths(nativepaths);
		}
	}

	public Dictionary loadManifest(BaseData data) throws BundleException {
		return loadManifest(data, false);
	}

	public Dictionary loadManifest(BaseData bundleData, boolean firstTime) throws BundleException {
		Dictionary result = null;
		StorageHook[] dataStorageHooks = bundleData.getStorageHooks();
		for (int i = 0; i < dataStorageHooks.length && result == null; i++)
			result = dataStorageHooks[i].getManifest(firstTime);
		if (result == null)
			result = AdaptorUtil.loadManifestFrom(bundleData);
		if (result == null)
			throw new BundleException(NLS.bind(AdaptorMsg.MANIFEST_NOT_FOUND_EXCEPTION, Constants.OSGI_BUNDLE_MANIFEST, bundleData.getLocation()));
		return result;
	}

	public File getExtractFile(BaseData data, String path) {
		BaseStorageHook storageHook = (BaseStorageHook) data.getStorageHook(BaseStorageHook.KEY);
		if (storageHook == null)
			return null;
		// first check the child generation dir
		File childGenDir = storageHook.getGenerationDir();
		if (childGenDir != null) {
			File childPath = new File(childGenDir, path);
			if (childPath.exists())
				return childPath;
		}
		// now check the parent
		File parentGenDir = storageHook.getParentGenerationDir();
		if (parentGenDir != null) {
			// there is a parent generation check if the file exists
			File parentPath = new File(parentGenDir, path);
			if (parentPath.exists())
				// only use the parent generation file if it exists; do not extract there
				return parentPath;
		}
		// did not exist in both locations; create a file for extraction.
		File bundleGenerationDir = storageHook.createGenerationDir();
		/* if the generation dir exists, then we have place to cache */
		if (bundleGenerationDir != null && bundleGenerationDir.exists())
			return new File(bundleGenerationDir, path);
		return null;
	}

	public BaseData[] getInstalledBundles() {
		return readBundleDatas();
	}

	private BaseData[] readBundleDatas() {
		InputStream bundleDataStream = findStorageStream(LocationManager.BUNDLE_DATA_FILE);
		if (bundleDataStream == null)
			return null;
		try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(bundleDataStream));
			try {
				byte version = in.readByte();
				if (version != BUNDLEDATA_VERSION)
					return null;
				timeStamp = in.readLong();
				initialBundleStartLevel = in.readInt();
				nextId = in.readLong();

				int numStorageHooks = in.readInt();
				StorageHook[] storageHooks = adaptor.getHookRegistry().getStorageHooks();
				if (numStorageHooks != storageHooks.length)
					return null; // must have the same number of storagehooks to properly read the data
				for (int i = 0; i < numStorageHooks; i++) {
					Object storageKey = storageHooks[i].getKey();
					int storageVersion = storageHooks[i].getStorageVersion();
					if (!storageKey.equals(in.readUTF()) || storageVersion != in.readInt())
						return null; // some storage hooks have changed must throw the data away.
				}

				int bundleCount = in.readInt();
				ArrayList result = new ArrayList(bundleCount);
				long id = -1;
				boolean bundleDiscarded = false;
				for (int i = 0; i < bundleCount; i++) {
					boolean error = false;
					BaseData data = null;
					try {
						id = in.readLong();
						if (id != 0) {
							data = loadBaseData(id, in);
							data.getBundleFile();
							StorageHook[] dataStorageHooks = data.getStorageHooks();
							for (int j = 0; j < dataStorageHooks.length; j++)
								dataStorageHooks[j].validate();
							if (Debug.DEBUG && Debug.DEBUG_GENERAL)
								Debug.println("BundleData created: " + data); //$NON-NLS-1$
							processExtension(data, EXTENSION_INITIALIZE);
							result.add(data);
						}
					} catch (IllegalArgumentException e) {
						// may be from data.getBundleFile()
						bundleDiscarded = true;
						error = true;
					} catch (BundleException e) {
						// should never happen
						bundleDiscarded = true;
						error = true;
					} catch (IOException e) {
						bundleDiscarded = true;
						error = true;
						if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
							Debug.println("Error reading framework metadata: " + e.getMessage()); //$NON-NLS-1$ 
							Debug.printStackTrace(e);
						}
					}
					if (error && data != null) {
						BaseStorageHook storageHook = (BaseStorageHook) data.getStorageHook(BaseStorageHook.KEY);
						storageHook.delete(true, BaseStorageHook.DEL_BUNDLE_STORE);
					}
				}
				if (bundleDiscarded)
					FrameworkProperties.setProperty(EclipseStarter.PROP_REFRESH_BUNDLES, "true"); //$NON-NLS-1$
				return (BaseData[]) result.toArray(new BaseData[result.size()]);
			} finally {
				in.close();
			}
		} catch (IOException e) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Error reading framework metadata: " + e.getMessage()); //$NON-NLS-1$ 
				Debug.printStackTrace(e);
			}
		}
		return null;
	}

	private void saveAllData(boolean shutdown) {
		if (storageManagerClosed)
			try {
				storageManager.open(!LocationManager.getConfigurationLocation().isReadOnly());
				storageManagerClosed = false;
			} catch (IOException e) {
				String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_FILEMANAGER_OPEN_ERROR, e.getMessage());
				FrameworkLogEntry logEntry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, message, 0, e, null);
				adaptor.getFrameworkLog().log(logEntry);
			}
		saveBundleDatas();
		saveStateData(shutdown);
		savePermissionStorage();
		if (shutdown)
			stateManager.stopDataManager();
	}

	private BasePermissionStorage readPermissionData() {
		BasePermissionStorage result = new BasePermissionStorage(this);
		InputStream permDataStream = findStorageStream(PERM_DATA_FILE);
		if (permDataStream == null)
			return result;
		try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(permDataStream));
			try {
				if (PERMDATA_VERSION != in.readByte())
					return result;
				// read the default permissions first
				int numPerms = in.readInt();
				if (numPerms > 0) {
					String[] perms = new String[numPerms];
					for (int i = 0; i < numPerms; i++)
						perms[i] = in.readUTF();
					result.setPermissionData(null, perms);
				}
				int numLocs = in.readInt();
				if (numLocs > 0)
					for (int i = 0; i < numLocs; i++) {
						String loc = in.readUTF();
						numPerms = in.readInt();
						String[] perms = new String[numPerms];
						for (int j = 0; j < numPerms; j++)
							perms[j] = in.readUTF();
						result.setPermissionData(loc, perms);
					}
				int numCondPerms = in.readInt();
				if (numCondPerms > 0) {
					String[] condPerms = new String[numCondPerms];
					for (int i = 0; i < numCondPerms; i++)
						condPerms[i] = in.readUTF();
					result.saveConditionalPermissionInfos(condPerms);
				}
				result.setDirty(false);
			} finally {
				in.close();
			}
		} catch (IOException e) {
			adaptor.getFrameworkLog().log(new FrameworkEvent(FrameworkEvent.ERROR, context.getBundle(), e));
		}
		return result;
	}

	private void savePermissionStorage() {
		if (permissionStorage == null || isReadOnly() || !permissionStorage.isDirty())
			return;
		try {
			ManagedOutputStream fmos = storageManager.getOutputStream(PERM_DATA_FILE);
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(fmos));
			boolean error = true;
			try {
				out.writeByte(PERMDATA_VERSION);
				// always write the default permissions first
				String[] defaultPerms = permissionStorage.getPermissionData(null);
				out.writeInt(defaultPerms == null ? 0 : defaultPerms.length);
				if (defaultPerms != null)
					for (int i = 0; i < defaultPerms.length; i++)
						out.writeUTF(defaultPerms[i]);
				String[] locations = permissionStorage.getLocations();
				out.writeInt(locations == null ? 0 : locations.length);
				if (locations != null)
					for (int i = 0; i < locations.length; i++) {
						out.writeUTF(locations[i]);
						String[] perms = permissionStorage.getPermissionData(locations[i]);
						out.writeInt(perms == null ? 0 : perms.length);
						if (perms != null)
							for (int j = 0; j < perms.length; j++)
								out.writeUTF(perms[j]);
					}
				String[] condPerms = permissionStorage.getConditionalPermissionInfos();
				out.writeInt(condPerms == null ? 0 : condPerms.length);
				if (condPerms != null)
					for (int i = 0; i < condPerms.length; i++)
						out.writeUTF(condPerms[i]);
				out.close();
				permissionStorage.setDirty(false);
				error = false;
			} finally {
				// if something happens, don't close a corrupt file
				if (error) {
					fmos.abort();
					try {
						out.close();
					} catch (IOException e) {/*ignore*/
					}
				}
			}
		} catch (IOException e) {
			adaptor.getFrameworkLog().log(new FrameworkEvent(FrameworkEvent.ERROR, context.getBundle(), e));
			return;
		}
	}

	private void saveBundleDatas() {
		// the cache and the state match
		if (stateManager == null || isReadOnly() || (timeStamp == stateManager.getSystemState().getTimeStamp() && !stateManager.saveNeeded()))
			return;
		try {
			ManagedOutputStream fmos = storageManager.getOutputStream(LocationManager.BUNDLE_DATA_FILE);
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(fmos));
			boolean error = true;
			try {
				out.writeByte(BUNDLEDATA_VERSION);
				out.writeLong(stateManager.getSystemState().getTimeStamp());
				out.writeInt(initialBundleStartLevel);
				out.writeLong(nextId);

				StorageHook[] storageHooks = adaptor.getHookRegistry().getStorageHooks();
				out.writeInt(storageHooks.length);
				for (int i = 0; i < storageHooks.length; i++) {
					out.writeUTF((String) storageHooks[i].getKey());
					out.writeInt(storageHooks[i].getStorageVersion());
				}

				Bundle[] bundles = context.getBundles();
				out.writeInt(bundles.length);
				for (int i = 0; i < bundles.length; i++) {
					long id = bundles[i].getBundleId();
					out.writeLong(id);
					if (id != 0) {
						BundleData data = ((org.eclipse.osgi.framework.internal.core.AbstractBundle) bundles[i]).getBundleData();
						saveBaseData((BaseData) data, out);
					}
				}
				out.close();
				// update the 'timeStamp' after the changed Meta data is saved.
				timeStamp = stateManager.getSystemState().getTimeStamp();
				error = false;
			} finally {
				// if something happens, don't close a corrupt file
				if (error) {
					fmos.abort();
					try {
						out.close();
					} catch (IOException e) {/*ignore*/
					}
				}
			}
		} catch (IOException e) {
			adaptor.getFrameworkLog().log(new FrameworkEvent(FrameworkEvent.ERROR, context.getBundle(), e));
			return;
		}
	}

	private void saveStateData(boolean shutdown) {
		if (stateManager == null || isReadOnly() || !stateManager.saveNeeded())
			return;
		try {
			File stateTmpFile = File.createTempFile(LocationManager.STATE_FILE, ".new", LocationManager.getOSGiConfigurationDir()); //$NON-NLS-1$
			File lazyTmpFile = File.createTempFile(LocationManager.LAZY_FILE, ".new", LocationManager.getOSGiConfigurationDir()); //$NON-NLS-1$
			if (shutdown)
				stateManager.shutdown(stateTmpFile, lazyTmpFile);
			else
				synchronized (stateManager) {
					stateManager.update(stateTmpFile, lazyTmpFile);
				}
			storageManager.lookup(LocationManager.STATE_FILE, true);
			storageManager.lookup(LocationManager.LAZY_FILE, true);
			storageManager.update(new String[] {LocationManager.STATE_FILE, LocationManager.LAZY_FILE}, new String[] {stateTmpFile.getName(), lazyTmpFile.getName()});
		} catch (IOException e) {
			adaptor.getFrameworkLog().log(new FrameworkEvent(FrameworkEvent.ERROR, context.getBundle(), e));
		}
	}

	public PermissionStorage getPermissionStorage() throws IOException {
		if (permissionStorage == null)
			permissionStorage = readPermissionData();
		return permissionStorage;
	}

	public int getInitialBundleStartLevel() {
		return initialBundleStartLevel;
	}

	public void setInitialBundleStartLevel(int value) {
		this.initialBundleStartLevel = value;
		requestSave();
	}

	public void save(BaseData data) throws IOException {
		if (data.isDirty()) {
			timeStamp--; // Change the value of the timeStamp, as a marker that something changed.
			requestSave();
			data.setDirty(false);
		}
	}

	public BundleOperation installBundle(String location, URLConnection source) {
		BaseData data = createBaseData(getNextBundleId(), location);
		return new BundleInstall(data, source, this);
	}

	public BundleOperation updateBundle(BaseData data, URLConnection source) {
		return new BundleUpdate(data, source, this);
	}

	public BundleOperation uninstallBundle(BaseData data) {
		return new BundleUninstall(data, this);
	}

	protected Object getBundleContent(BaseData bundledata) throws IOException {
		BaseStorageHook storageHook = (BaseStorageHook) bundledata.getStorageHook(BaseStorageHook.KEY);
		if (storageHook == null)
			throw new IllegalStateException();
		return storageHook.isReference() ? new File(storageHook.getFileName()) : new File(storageHook.getGenerationDir(), storageHook.getFileName());
	}

	public BundleFile createBundleFile(Object content, BaseData data) throws IOException {
		boolean base = false;
		if (content == null) {
			// this must be a request for the base bundlefile
			base = true;
			// get the content of this bundle
			content = getBundleContent(data);
		}
		BundleFile result = null;
		// Ask factories before doing the default behavior
		BundleFileFactoryHook[] factories = adaptor.getHookRegistry().getBundleFileFactoryHooks();
		for (int i = 0; i < factories.length && result == null; i++)
			result = factories[i].createBundleFile(content, data, base);

		// No factories configured or they declined to create the bundle file; do default
		if (result == null && content instanceof File) {
			File file = (File) content;
			if (file.isDirectory())
				result =  new DirBundleFile(file);
			else 
				result = new ZipBundleFile(file, data);
		}
		if (result == null)
			// nothing we can do; must throw exception for the content
			throw new IOException("Cannot create bundle file for content of type: " + content.getClass().getName()); //$NON-NLS-1$

		// try creating a wrapper bundlefile out of it.
		BundleFileWrapperFactoryHook[] wrapperFactories = adaptor.getHookRegistry().getBundleFileWrapperFactoryHooks();
		for (int i = 0; i < wrapperFactories.length; i++) {
			BundleFile wrapperBundle = wrapperFactories[i].wrapBundleFile(result, content, data, base);
			if (wrapperBundle != null)
				result = wrapperBundle;
		}
		return result;
	}

	public synchronized StateManager getStateManager() {
		if (stateManager != null)
			return stateManager;
		stateManager = readStateData();
		checkSystemState(stateManager.getSystemState());
		return stateManager;
	}

	private void checkSystemState(State state) {
		BundleDescription[] bundles = state.getBundles();
		if (bundles == null)
			return;
		boolean removedBundle = false;
		for (int i = 0; i < bundles.length; i++) {
			if (context.getBundle(bundles[i].getBundleId()) == null) {
				state.removeBundle(bundles[i]);
				removedBundle = true;
			}
		}
		if (removedBundle)
			state.resolve(false); // do a full resolve
		BundleDescription systemBundle = state.getBundle(0);
		if (systemBundle == null || !systemBundle.isResolved())
			// this would be a bug in the framework
			throw new IllegalStateException();
	}

	private StateManager readStateData() {
		File[] stateFiles = findStorageFiles(new String[] {LocationManager.STATE_FILE, LocationManager.LAZY_FILE});
		File stateFile = stateFiles[0];
		File lazyFile = stateFiles[1];

		stateManager = new StateManager(stateFile, lazyFile, context, timeStamp);
		State systemState = null;
		if (!invalidState) {
			systemState = stateManager.readSystemState();
			if (systemState != null)
				return stateManager;
		}
		systemState = stateManager.createSystemState();
		Bundle[] installedBundles = context.getBundles();
		if (installedBundles == null)
			return stateManager;
		StateObjectFactory factory = stateManager.getFactory();
		for (int i = 0; i < installedBundles.length; i++) {
			AbstractBundle toAdd = (AbstractBundle) installedBundles[i];
			try {
				// make sure we get the real manifest as if this is the first time.
				Dictionary toAddManifest = loadManifest((BaseData) toAdd.getBundleData(), true);
				BundleDescription newDescription = factory.createBundleDescription(systemState, toAddManifest, toAdd.getLocation(), toAdd.getBundleId());
				systemState.addBundle(newDescription);
			} catch (BundleException be) {
				// just ignore bundle datas with invalid manifests
			}
		}
		// we do not set the cached timestamp here because we want a new one to be used from the new system state object (bug 132978)
		// we need the state resolved
		systemState.resolve();
		invalidState = false;
		return stateManager;
	}

	private File[] findStorageFiles(String[] fileNames) {
		File[] storageFiles = new File[fileNames.length];
		try {
			for (int i = 0; i < storageFiles.length; i++)
				storageFiles[i] = storageManager.lookup(fileNames[i], false);
		} catch (IOException ex) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Error reading state file " + ex.getMessage()); //$NON-NLS-1$
				Debug.printStackTrace(ex);
			}
		}
		boolean success = true;
		for (int i = 0; i < storageFiles.length; i++)
			if (storageFiles[i] == null || !storageFiles[i].isFile()) {
				success = false;
				break;
			}
		if (success)
			return storageFiles;
		//if it does not exist, try to read it from the parent
		Location parentConfiguration = null;
		Location currentConfiguration = LocationManager.getConfigurationLocation();
		if (currentConfiguration != null && (parentConfiguration = currentConfiguration.getParentLocation()) != null) {
			try {
				File stateLocationDir = new File(parentConfiguration.getURL().getFile(), FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME);
				StorageManager newFileManager = initFileManager(stateLocationDir, "none", true); //$NON-NLS-1$);
				for (int i = 0; i < storageFiles.length; i++)
					storageFiles[i] = newFileManager.lookup(fileNames[i], false);
				newFileManager.close();
			} catch (IOException ex) {
				if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
					Debug.println("Error reading state file " + ex.getMessage()); //$NON-NLS-1$
					Debug.printStackTrace(ex);
				}
			}
		} else {
			try {
				//it did not exist in either place, so create it in the original location
				if (!isReadOnly()) {
					for (int i = 0; i < storageFiles.length; i++)
						storageFiles[i] = storageManager.lookup(fileNames[i], true);
				}
			} catch (IOException ex) {
				if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
					Debug.println("Error reading state file " + ex.getMessage()); //$NON-NLS-1$
					Debug.printStackTrace(ex);
				}
			}
		}
		return storageFiles;
	}

	public void frameworkStart(BundleContext fwContext) throws BundleException {
		this.context = fwContext;
		// System property can be set to enable state saver or not.
		if (Boolean.valueOf(FrameworkProperties.getProperty(BaseStorage.PROP_ENABLE_STATE_SAVER, "true")).booleanValue()) //$NON-NLS-1$
			stateSaver = new StateSaver();

	}

	public void frameworkStop(BundleContext fwContext) throws BundleException {
		if (stateSaver != null)
			stateSaver.shutdown();
		saveAllData(true);
		storageManager.close();
		storageManagerClosed = true;
	}

	public void frameworkStopping(BundleContext fwContext) {
		// do nothing in storage
	}

	public void addProperties(Properties properties) {
		// set the extension support if we found the addURL method
		if (addURLMethod != null)
			properties.put(Constants.SUPPORTS_FRAMEWORK_EXTENSION, "true"); //$NON-NLS-1$
		// store bundleStore back into adaptor properties for others to see
		properties.put(BaseStorage.PROP_BUNDLE_STORE, getBundleStoreRoot().getAbsolutePath());
	}

	private InputStream findStorageStream(String fileName) {
		InputStream storageStream = null;
		try {
			storageStream = storageManager.getInputStream(fileName);
		} catch (IOException ex) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Error reading framework metadata: " + ex.getMessage()); //$NON-NLS-1$
				Debug.printStackTrace(ex);
			}
		}
		if (storageStream == null) {
			Location currentConfiguration = LocationManager.getConfigurationLocation();
			Location parentConfiguration = null;
			if (currentConfiguration != null && (parentConfiguration = currentConfiguration.getParentLocation()) != null) {
				try {
					File bundledataLocationDir = new File(parentConfiguration.getURL().getFile(), FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME);
					StorageManager newStorageManager = initFileManager(bundledataLocationDir, "none", true); //$NON-NLS-1$
					storageStream = newStorageManager.getInputStream(fileName);
					newStorageManager.close();
				} catch (MalformedURLException e1) {
					// This will not happen since all the URLs are derived by us
					// and we are GODS!
				} catch (IOException e1) {
					// That's ok we will regenerate the .bundleData
				}
			}
		}
		return storageStream;
	}

	protected void saveBaseData(BaseData bundledata, DataOutputStream out) throws IOException {
		StorageHook[] hooks = bundledata.getStorageHooks();
		out.writeInt(hooks.length);
		for (int i = 0; i < hooks.length; i++) {
			out.writeUTF((String) hooks[i].getKey());
			hooks[i].save(out);
		}
	}

	protected BaseData loadBaseData(long id, DataInputStream in) throws IOException {
		BaseData result = new BaseData(id, adaptor);
		int numHooks = in.readInt();
		StorageHook[] hooks = new StorageHook[numHooks];
		for (int i = 0; i < numHooks; i++) {
			String hookKey = in.readUTF();
			StorageHook storageHook = (StorageHook) storageHooks.getByKey(hookKey);
			if (storageHook == null)
				throw new IOException();
			hooks[i] = storageHook.load(result, in);
		}
		result.setStorageHooks(hooks);
		return result;
	}

	protected BaseData createBaseData(long id, String location) {
		BaseData result = new BaseData(id, adaptor);
		result.setLocation(location);
		return result;
	}

	public String getInstallPath() {
		return installPath;
	}

	private void cleanOSGiCache() {
		File osgiConfig = LocationManager.getOSGiConfigurationDir();
		if (!AdaptorUtil.rm(osgiConfig)) {
			// TODO log error?
		}
	}

	/**
	 * Processes an extension bundle
	 * @param bundleData the extension bundle data 
	 * @param type the type of extension bundle
	 * @throws BundleException on any errors or if the extension bundle type is not supported
	 */
	protected void processExtension(BaseData bundleData, byte type) throws BundleException {
		if ((bundleData.getType() & BundleData.TYPE_FRAMEWORK_EXTENSION) != 0) {
			validateExtension(bundleData);
			processFrameworkExtension(bundleData, type);
		} else if ((bundleData.getType() & BundleData.TYPE_BOOTCLASSPATH_EXTENSION) != 0) {
			validateExtension(bundleData);
			processBootExtension(bundleData, type);
		}
	}

	/**
	 * Validates the extension bundle metadata
	 * @param bundleData the extension bundle data
	 * @throws BundleException if the extension bundle metadata is invalid
	 */
	private void validateExtension(BundleData bundleData) throws BundleException {
		Dictionary extensionManifest = bundleData.getManifest();
		if (extensionManifest.get(Constants.IMPORT_PACKAGE) != null)
			throw new BundleException(NLS.bind(AdaptorMsg.ADAPTOR_EXTENSION_IMPORT_ERROR, bundleData.getLocation()));
		if (extensionManifest.get(Constants.REQUIRE_BUNDLE) != null)
			throw new BundleException(NLS.bind(AdaptorMsg.ADAPTOR_EXTENSION_REQUIRE_ERROR, bundleData.getLocation()));
		if (extensionManifest.get(Constants.BUNDLE_NATIVECODE) != null)
			throw new BundleException(NLS.bind(AdaptorMsg.ADAPTOR_EXTENSION_NATIVECODE_ERROR, bundleData.getLocation()));
	}

	/**
	 * Processes a framework extension bundle
	 * @param bundleData the extension bundle data
	 * @param type the type of extension bundle
	 * @throws BundleException on errors or if framework extensions are not supported
	 */
	protected void processFrameworkExtension(BaseData bundleData, byte type) throws BundleException {
		if (addURLMethod == null)
			throw new BundleException("Framework extensions are not supported.", new UnsupportedOperationException()); //$NON-NLS-1$
		if ((type & (EXTENSION_UNINSTALLED | EXTENSION_UPDATED)) != 0)
			// if uninstalled or updated then do nothing framework must be restarted.
			return;

		// first make sure this BundleData is not on the pre-configured osgi.framework.extensions list
		String[] extensions = getConfiguredExtensions();
		for (int i = 0; i < extensions.length; i++)
			if (extensions[i].equals(bundleData.getSymbolicName()))
				return;
		File[] files = getExtensionFiles(bundleData);
		if (files == null)
			return;
		ClassLoader cl = getClass().getClassLoader();
		for (int i = 0; i < files.length; i++) {
			if (files[i] == null)
				continue;
			Throwable exceptionLog = null;
			try {
				addURLMethod.invoke(cl, new Object[] {files[i].toURL()});
			} catch (InvocationTargetException e) {
				exceptionLog = e.getTargetException();
			} catch (Throwable t) {
				exceptionLog = t;
			} finally {
				if (exceptionLog != null)
					adaptor.getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, bundleData.getBundle(), exceptionLog);
			}
		}
		try {
			cl.loadClass("thisIsNotAClass"); // initialize the new urls  //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			// do nothing
		} 
	}

	/**
	 * Returns a list of configured extensions
	 * @return a list of configured extensions
	 */
	protected String[] getConfiguredExtensions() {
		if (configuredExtensions != null)
			return configuredExtensions;
		String prop = FrameworkProperties.getProperty(BaseStorage.PROP_FRAMEWORK_EXTENSIONS);
		if (prop == null || prop.trim().length() == 0)
			configuredExtensions = new String[0];
		else
			configuredExtensions = ManifestElement.getArrayFromList(prop);
		return configuredExtensions;
	}

	/**
	 * Processes a boot extension bundle
	 * @param bundleData the extension bundle data
	 * @param type the type of extension bundle
	 * @throws BundleException on errors or if boot extensions are not supported
	 */
	protected void processBootExtension(BundleData bundleData, byte type) throws BundleException {
		throw new BundleException("Boot classpath extensions are not supported.", new UnsupportedOperationException()); //$NON-NLS-1$
	}

	private void initBundleStoreRoot() {
		File configurationLocation = LocationManager.getOSGiConfigurationDir();
		if (configurationLocation != null)
			bundleStoreRoot = new File(configurationLocation, LocationManager.BUNDLES_DIR);
		else
			// last resort just default to "bundles"
			bundleStoreRoot = new File(LocationManager.BUNDLES_DIR);
	}

	public File getBundleStoreRoot() {
		if (bundleStoreRoot == null)
			initBundleStoreRoot();
		return bundleStoreRoot;
	}

	/**
	 * Returns a list of classpath files for an extension bundle
	 * @param bundleData the bundle data for an extension bundle
	 * @return a list of classpath files for an extension bundle
	 */
	protected File[] getExtensionFiles(BaseData bundleData) {
		File[] files = null;
		try {
			String[] paths = bundleData.getClassPath();
			// TODO need to be smarter about dev path here
			if (FrameworkProperties.getProperty("osgi.dev") != null) { //$NON-NLS-1$
				String[] origPaths = paths;
				paths = new String[origPaths.length + 1];
				System.arraycopy(origPaths, 0, paths, 0, origPaths.length);
				paths[paths.length - 1] = "bin"; //$NON-NLS-1$
			}
			ArrayList results = new ArrayList(paths.length);
			for (int i = 0; i < paths.length; i++) {
				if (".".equals(paths[i])) //$NON-NLS-1$
					results.add(bundleData.getBundleFile().getBaseFile());
				else {
					File result = bundleData.getBundleFile().getFile(paths[i], false);
					if (result != null)
						results.add(result);
				}
			}
			return (File[]) results.toArray(new File[results.size()]);
		} catch (BundleException e) {
			adaptor.getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, bundleData.getBundle(), e);
		}
		return files;
	}

	void requestSave() {
		// Only when the State saver is enabled will the stateSaver be started.
		if (stateSaver == null)
			return;
		stateSaver.requestSave();
	}

	/**
	 * Updates the state mananager with an updated/installed/uninstalled bundle
	 * @param bundleData the modified bundle
	 * @param type the type of modification
	 * @throws BundleException
	 */
	public void updateState(BundleData bundleData, int type) throws BundleException {
		if (stateManager == null) {
			invalidState = true;
			return;
		}
		State systemState = stateManager.getSystemState();
		switch (type) {
			case BundleEvent.UPDATED :
				systemState.removeBundle(bundleData.getBundleID());
				// fall through to INSTALLED
			case BundleEvent.INSTALLED :
				BundleDescription newDescription = stateManager.getFactory().createBundleDescription(systemState, bundleData.getManifest(), bundleData.getLocation(), bundleData.getBundleID());
				systemState.addBundle(newDescription);
				break;
			case BundleEvent.UNINSTALLED :
				systemState.removeBundle(bundleData.getBundleID());
				break;
		}
	}

	private static Method findaddURLMethod(Class clazz) {
		if (clazz == null)
			return null; // ends the recursion when getSuperClass returns null
		try {
			Method result = clazz.getDeclaredMethod("addURL", new Class[] {URL.class}); //$NON-NLS-1$ 
			result.setAccessible(true);
			return result;
		} catch (NoSuchMethodException e) {
			// do nothing look in super class below
		} catch (SecurityException e) {
			// if we do not have the permissions then we will not find the method
		}
		return findaddURLMethod(clazz.getSuperclass());
	}

	private class StateSaver implements Runnable {
		private long delay_interval = 30000; // 30 seconds.
		private long max_total_delay_interval = 1800000; // 30 minutes.
		private boolean shutdown = false;
		private long lastSaveTime = 0;
		private Thread runningThread = null;

		StateSaver() {
			String prop = FrameworkProperties.getProperty("eclipse.stateSaveDelayInterval"); //$NON-NLS-1$
			if (prop != null) {
				try {
					long val = Long.parseLong(prop);
					if (val >= 1000 && val <= 1800000) {
						delay_interval = val;
						max_total_delay_interval = val * 60;
					}
				} catch (NumberFormatException e) {
					// ignore
				}
			}
		}

		public void run() {
			State systemState = adaptor.getState();
			synchronized (systemState) {
				long firstSaveTime = lastSaveTime;
				long curSaveTime = 0;
				long delayTime;
				do {
					do {
						if ((System.currentTimeMillis() - firstSaveTime) > max_total_delay_interval)
							// Waiting time has been too long, so break to start saving State data to file.
							break;
						delayTime = Math.min(delay_interval, lastSaveTime - curSaveTime);
						curSaveTime = lastSaveTime;
						// wait for other save requests 
						try {
							if (!shutdown)
								systemState.wait(delayTime);
						} catch (InterruptedException ie) {
							// force break from do/while loops
							curSaveTime = lastSaveTime;
							break;
						}

						// Continue the loop if 'lastSaveTime' is increased again during waiting.
					} while (!shutdown && curSaveTime < lastSaveTime);
					// Save State and Meta data.
					saveAllData(false);
					// Continue the loop if Saver is asked again during saving State data to file.
				} while (!shutdown && curSaveTime < lastSaveTime);
				runningThread = null; // clear runningThread
			}
		}

		void shutdown() {
			State systemState = adaptor.getState();
			Thread joinWith = null;
			synchronized (systemState) {
				shutdown = true;
				joinWith = runningThread;
				systemState.notifyAll(); // To wakeup sleeping thread.
			}
			try {
				if (joinWith != null)
					// There should be no deadlock when 'shutdown' is true.
					joinWith.join();
			} catch (InterruptedException ie) {
				if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
					Debug.println("Error shutdowning StateSaver: " + ie.getMessage()); //$NON-NLS-1$
					Debug.printStackTrace(ie);
				}
			}
		}

		void requestSave() {
			State systemState = adaptor.getState();
			synchronized (systemState) {
				lastSaveTime = System.currentTimeMillis();
				if (runningThread == null) {
					runningThread = new Thread(this, "State Saver"); //$NON-NLS-1$
					runningThread.start();
				}
			}
		}
	}

	public long getNextBundleId() {
		return nextId++;
	}

}
