/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rob Harrop - SpringSource Inc. (bug 253942)
 *******************************************************************************/

package org.eclipse.osgi.internal.baseadaptor;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import org.eclipse.core.runtime.adaptor.LocationManager;
import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.baseadaptor.hooks.StorageHook;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.util.KeyedElement;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;

public class BaseStorageHook implements StorageHook, AdaptorHook {
	public static final String KEY = BaseStorageHook.class.getName();
	public static final int HASHCODE = KEY.hashCode();
	public static final int DEL_BUNDLE_STORE = 0x01;
	public static final int DEL_GENERATION = 0x02;
	private static final int STORAGE_VERSION = 1;
	public static final String EXTERNAL_LIB_PREFIX = "external:"; //$NON-NLS-1$
	public static final String VARIABLE_DELIM_STRING = "$"; //$NON-NLS-1$
	public static final char VARIABLE_DELIM_CHAR = '$';
	public static String COMPOSITE_HEADER = "Equinox-CompositeBundle"; //$NON-NLS-1$
	public static String COMPOSITE_BUNDLE = "composite"; //$NON-NLS-1$
	public static String SURROGATE_BUNDLE = "surrogate"; //$NON-NLS-1$

	/** bundle's file name */
	private String fileName;
	/** native code paths for this BundleData */
	private String[] nativePaths;
	/** bundle generation */
	private int generation = 1;
	/** Is bundle a reference */
	private boolean reference;

	private BaseData bundleData;
	private BaseStorage storage;
	private File bundleStore;
	private File dataStore;

	public BaseStorageHook(BaseStorage storage) {
		this.storage = storage;
	}

	public int getStorageVersion() {
		return STORAGE_VERSION;
	}

	/**
	 * @throws BundleException  
	 */
	public StorageHook create(BaseData bundledata) throws BundleException {
		BaseStorageHook storageHook = new BaseStorageHook(storage);
		storageHook.bundleData = bundledata;
		return storageHook;
	}

	public void initialize(Dictionary<String, String> manifest) throws BundleException {
		BaseStorageHook.loadManifest(bundleData, manifest);
	}

	@SuppressWarnings("deprecation")
	static void loadManifest(BaseData target, Dictionary<String, String> manifest) throws BundleException {
		try {
			target.setVersion(Version.parseVersion(manifest.get(Constants.BUNDLE_VERSION)));
		} catch (IllegalArgumentException e) {
			target.setVersion(new InvalidVersion(manifest.get(Constants.BUNDLE_VERSION)));
		}
		ManifestElement[] bsnHeader = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, manifest.get(Constants.BUNDLE_SYMBOLICNAME));
		int bundleType = 0;
		if (bsnHeader != null) {
			target.setSymbolicName(bsnHeader[0].getValue());
			String singleton = bsnHeader[0].getDirective(Constants.SINGLETON_DIRECTIVE);
			if (singleton == null)
				singleton = bsnHeader[0].getAttribute(Constants.SINGLETON_DIRECTIVE);
			if ("true".equals(singleton)) //$NON-NLS-1$
				bundleType |= BundleData.TYPE_SINGLETON;
		}
		// check that the classpath is valid
		String classpath = manifest.get(Constants.BUNDLE_CLASSPATH);
		ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, classpath);
		target.setClassPathString(classpath);
		target.setActivator(manifest.get(Constants.BUNDLE_ACTIVATOR));
		String host = manifest.get(Constants.FRAGMENT_HOST);
		if (host != null) {
			bundleType |= BundleData.TYPE_FRAGMENT;
			ManifestElement[] hostElement = ManifestElement.parseHeader(Constants.FRAGMENT_HOST, host);
			if (Constants.getInternalSymbolicName().equals(hostElement[0].getValue()) || Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(hostElement[0].getValue())) {
				String extensionType = hostElement[0].getDirective("extension"); //$NON-NLS-1$
				if (extensionType == null || extensionType.equals("framework")) //$NON-NLS-1$
					bundleType |= BundleData.TYPE_FRAMEWORK_EXTENSION;
				else if (extensionType.equals("bootclasspath")) //$NON-NLS-1$
					bundleType |= BundleData.TYPE_BOOTCLASSPATH_EXTENSION;
				else if (extensionType.equals("extclasspath")) //$NON-NLS-1$
					bundleType |= BundleData.TYPE_EXTCLASSPATH_EXTENSION;
			}
		} else {
			String composite = manifest.get(COMPOSITE_HEADER);
			if (composite != null) {
				if (COMPOSITE_BUNDLE.equals(composite))
					bundleType |= BundleData.TYPE_COMPOSITEBUNDLE;
				else
					bundleType |= BundleData.TYPE_SURROGATEBUNDLE;
			}
		}
		target.setType(bundleType);
		target.setExecutionEnvironment(manifest.get(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT));
		target.setDynamicImports(manifest.get(Constants.DYNAMICIMPORT_PACKAGE));
	}

	public StorageHook load(BaseData target, DataInputStream in) throws IOException {
		target.setLocation(AdaptorUtil.readString(in, false));
		target.setSymbolicName(AdaptorUtil.readString(in, false));
		target.setVersion(AdaptorUtil.loadVersion(in));
		target.setActivator(AdaptorUtil.readString(in, false));
		target.setClassPathString(AdaptorUtil.readString(in, false));
		target.setExecutionEnvironment(AdaptorUtil.readString(in, false));
		target.setDynamicImports(AdaptorUtil.readString(in, false));
		target.setStartLevel(in.readInt());
		target.setStatus(in.readInt());
		target.setType(in.readInt());
		target.setLastModified(in.readLong());
		target.setDirty(false); // make sure to reset the dirty bit;

		BaseStorageHook storageHook = new BaseStorageHook(storage);
		storageHook.bundleData = target;
		storageHook.generation = in.readInt();
		storageHook.reference = in.readBoolean();
		storageHook.setFileName(getAbsolute(storageHook.reference, AdaptorUtil.readString(in, false)));
		int nativePathCount = in.readInt();
		storageHook.nativePaths = nativePathCount > 0 ? new String[nativePathCount] : null;
		for (int i = 0; i < nativePathCount; i++)
			storageHook.nativePaths[i] = in.readUTF();
		return storageHook;
	}

	private String getAbsolute(boolean isReference, String path) {
		if (!isReference)
			return path;
		// fileName for bundles installed with reference URLs is stored relative to the install location
		File storedPath = new File(path);
		if (!storedPath.isAbsolute())
			// make sure it has the absolute location instead
			return new FilePath(storage.getInstallPath() + path).toString();
		return path;
	}

	public void save(DataOutputStream out) throws IOException {
		if (bundleData == null)
			throw new IllegalStateException();
		AdaptorUtil.writeStringOrNull(out, bundleData.getLocation());
		AdaptorUtil.writeStringOrNull(out, bundleData.getSymbolicName());
		AdaptorUtil.writeStringOrNull(out, bundleData.getVersion().toString());
		AdaptorUtil.writeStringOrNull(out, bundleData.getActivator());
		AdaptorUtil.writeStringOrNull(out, bundleData.getClassPathString());
		AdaptorUtil.writeStringOrNull(out, bundleData.getExecutionEnvironment());
		AdaptorUtil.writeStringOrNull(out, bundleData.getDynamicImports());
		StorageHook[] hooks = bundleData.getStorageHooks();
		boolean forgetStartLevel = false;
		for (int i = 0; i < hooks.length && !forgetStartLevel; i++)
			forgetStartLevel = hooks[i].forgetStartLevelChange(bundleData.getStartLevel());
		out.writeInt(!forgetStartLevel ? bundleData.getStartLevel() : 1);
		boolean forgetStatus = false;
		// see if we should forget the persistently started flag
		for (int i = 0; i < hooks.length && !forgetStatus; i++)
			forgetStatus = hooks[i].forgetStatusChange(bundleData.getStatus());
		out.writeInt(!forgetStatus ? bundleData.getStatus() : (~Constants.BUNDLE_STARTED) & bundleData.getStatus());
		out.writeInt(bundleData.getType());
		out.writeLong(bundleData.getLastModified());

		out.writeInt(getGeneration());
		out.writeBoolean(isReference());
		String storedFileName = isReference() ? new FilePath(storage.getInstallPath()).makeRelative(new FilePath(getFileName())) : getFileName();
		AdaptorUtil.writeStringOrNull(out, storedFileName);
		if (nativePaths == null)
			out.writeInt(0);
		else {
			out.writeInt(nativePaths.length);
			for (int i = 0; i < nativePaths.length; i++)
				out.writeUTF(nativePaths[i]);
		}

	}

	public int getKeyHashCode() {
		return HASHCODE;
	}

	public boolean compare(KeyedElement other) {
		return other.getKey() == KEY;
	}

	public Object getKey() {
		return KEY;
	}

	public String getFileName() {
		return fileName;
	}

	public int getGeneration() {
		return generation;
	}

	public String[] getNativePaths() {
		return nativePaths;
	}

	public void installNativePaths(String[] installPaths) throws BundleException {
		validateNativePaths(installPaths);
		this.nativePaths = installPaths;
	}

	public void validateNativePaths(String[] paths) throws BundleException {
		for (int i = 0; i < paths.length; i++) {
			if (paths[i].startsWith(EXTERNAL_LIB_PREFIX)) {
				String path = substituteVars(paths[i].substring(EXTERNAL_LIB_PREFIX.length()));
				File nativeFile = new File(path);
				if (!nativeFile.exists())
					throw new BundleException(NLS.bind(AdaptorMsg.BUNDLE_NATIVECODE_EXCEPTION, nativeFile.getAbsolutePath()), BundleException.NATIVECODE_ERROR);
				continue; // continue to next path
			}
			// ensure the file exists in the bundle; it will get extracted later on demand
			BundleEntry nativeEntry = bundleData.getBundleFile().getEntry(paths[i]);
			if (nativeEntry == null)
				throw new BundleException(NLS.bind(AdaptorMsg.BUNDLE_NATIVECODE_EXCEPTION, paths[i]), BundleException.NATIVECODE_ERROR);
		}
	}

	public boolean isReference() {
		return reference;
	}

	public File getBundleStore() {
		if (bundleStore == null)
			bundleStore = new File(storage.getBundleStoreRoot(), String.valueOf(bundleData.getBundleID()));
		return bundleStore;
	}

	public File getDataFile(String path) {
		// lazily initialize dirData to prevent early access to configuration location
		if (dataStore == null)
			dataStore = new File(getBundleStore(), BaseStorage.DATA_DIR_NAME);
		if (path != null && !dataStore.exists() && (storage.isReadOnly() || !dataStore.mkdirs()))
			if (Debug.DEBUG_GENERAL)
				Debug.println("Unable to create bundle data directory: " + dataStore.getPath()); //$NON-NLS-1$
		return path == null ? dataStore : new File(dataStore, path);
	}

	void delete(boolean postpone, int type) throws IOException {
		File delete = null;
		switch (type) {
			case DEL_GENERATION :
				delete = getGenerationDir();
				break;
			case DEL_BUNDLE_STORE :
				delete = getBundleStore();
				break;
		}
		if (delete != null && delete.exists() && (postpone || !AdaptorUtil.rm(delete))) {
			/* create .delete */
			FileOutputStream out = new FileOutputStream(new File(delete, BaseStorage.DELETE_FLAG));
			out.close();
		}
	}

	File getGenerationDir() {
		return new File(getBundleStore(), String.valueOf(getGeneration()));
	}

	File getParentGenerationDir() {
		Location parentConfiguration = null;
		Location currentConfiguration = LocationManager.getConfigurationLocation();
		if (currentConfiguration != null && (parentConfiguration = currentConfiguration.getParentLocation()) != null)
			return new File(parentConfiguration.getURL().getFile(), FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME + '/' + LocationManager.BUNDLES_DIR + '/' + bundleData.getBundleID() + '/' + getGeneration());
		return null;
	}

	File createGenerationDir() {
		File generationDir = getGenerationDir();
		if (!generationDir.exists() && (storage.isReadOnly() || !generationDir.mkdirs()))
			if (Debug.DEBUG_GENERAL)
				Debug.println("Unable to create bundle generation directory: " + generationDir.getPath()); //$NON-NLS-1$
		return generationDir;
	}

	public void setReference(boolean reference) {
		this.reference = reference;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
		// This is only done for PDE source lookup (bug 126517)
		this.bundleData.setFileName(fileName);
	}

	public void copy(StorageHook storageHook) {
		if (!(storageHook instanceof BaseStorageHook))
			throw new IllegalArgumentException();
		BaseStorageHook hook = (BaseStorageHook) storageHook;
		bundleStore = hook.bundleStore;
		dataStore = hook.dataStore;
		generation = hook.generation + 1;
		// fileName and reference will be set by update
	}

	public void validate() throws IllegalArgumentException {
		// do nothing
	}

	/**
	 * @throws BundleException  
	 */
	public Dictionary<String, String> getManifest(boolean firstLoad) throws BundleException {
		// do nothing
		return null;
	}

	public boolean forgetStatusChange(int status) {
		// do nothing
		return false;
	}

	public boolean forgetStartLevelChange(int startlevel) {
		// do nothing
		return false;
	}

	public void initialize(BaseAdaptor adaptor) {
		// do nothing
	}

	/**
	 * @throws BundleException  
	 */
	public void frameworkStart(BundleContext context) throws BundleException {
		// do nothing
	}

	/**
	 * @throws BundleException  
	 */
	public void frameworkStop(BundleContext context) throws BundleException {
		// do nothing
	}

	public void frameworkStopping(BundleContext context) {
		// do nothing
	}

	public void addProperties(Properties properties) {
		// do nothing
	}

	public URLConnection mapLocationToURLConnection(String location) throws IOException {
		// take into account that initial@ is special (bug 268563)
		if (location.startsWith("initial@")) { //$NON-NLS-1$
			location = location.substring(8);
			return new URL(location).openConnection();
		}
		// see if this is an existing location
		Bundle[] bundles = storage.getAdaptor().getContext().getBundles();
		AbstractBundle bundle = null;
		for (int i = 0; i < bundles.length && bundle == null; i++)
			if (location.equals(bundles[i].getLocation()))
				bundle = (AbstractBundle) bundles[i];
		if (bundle == null)
			return null;
		BaseData data = (BaseData) bundle.getBundleData();
		BaseStorageHook hook = (BaseStorageHook) data.getStorageHook(BaseStorageHook.KEY);
		return hook.isReference() ? new URL("reference:file:" + hook.getFileName()).openConnection() : null; //$NON-NLS-1$
	}

	public void handleRuntimeError(Throwable error) {
		// do nothing
	}

	public FrameworkLog createFrameworkLog() {
		// do nothing
		return null;
	}

	public BaseStorage getStorage() {
		return storage;
	}

	public static String substituteVars(String path) {
		StringBuffer buf = new StringBuffer(path.length());
		StringTokenizer st = new StringTokenizer(path, VARIABLE_DELIM_STRING, true);
		boolean varStarted = false; // indicates we are processing a var subtitute
		String var = null; // the current var key
		while (st.hasMoreElements()) {
			String tok = st.nextToken();
			if (VARIABLE_DELIM_STRING.equals(tok)) {
				if (!varStarted) {
					varStarted = true; // we found the start of a var
					var = ""; //$NON-NLS-1$
				} else {
					// we have found the end of a var
					String prop = null;
					// get the value of the var from system properties
					if (var != null && var.length() > 0)
						prop = FrameworkProperties.getProperty(var);
					if (prop == null) {
						try {
							// try using the System.getenv method if it exists (bug 126921)
							Method getenv = System.class.getMethod("getenv", new Class[] {String.class}); //$NON-NLS-1$
							prop = (String) getenv.invoke(null, new Object[] {var});
						} catch (Throwable t) {
							// do nothing; 
							// on 1.4 VMs this throws an error
							// on J2ME this method does not exist
						}
					}
					if (prop != null)
						// found a value; use it
						buf.append(prop);
					else
						// could not find a value append the var name w/o delims 
						buf.append(var == null ? "" : var); //$NON-NLS-1$
					varStarted = false;
					var = null;
				}
			} else {
				if (!varStarted)
					buf.append(tok); // the token is not part of a var
				else
					var = tok; // the token is the var key; save the key to process when we find the end token
			}
		}
		if (var != null)
			// found a case of $var at the end of the path with no trailing $; just append it as is.
			buf.append(VARIABLE_DELIM_CHAR).append(var);
		return buf.toString();
	}
}
