/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.baseadaptor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook;
import org.eclipse.osgi.baseadaptor.hooks.StorageHook;
import org.eclipse.osgi.baseadaptor.loader.BaseClassLoader;
import org.eclipse.osgi.baseadaptor.loader.ClasspathManager;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.protocol.bundleentry.Handler;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.baseadaptor.ArrayMap;
import org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;

/**
 * The BundleData implementation used by the BaseAdaptor.
 * @see BaseAdaptor
 * @see BundleData
 * @see StorageHook
 * @see ClassLoadingHook
 * @since 3.2
 */
public class BaseData implements BundleData {
	private final static boolean COPY_NATIVES = Boolean.valueOf(FrameworkProperties.getProperty("osgi.classloader.copy.natives")).booleanValue(); //$NON-NLS-1$
	private long id;
	private BaseAdaptor adaptor;
	private Bundle bundle;
	private int startLevel = -1;
	private int status = 0;
	private StorageHook[] storageHooks;
	private String location;
	private long lastModified;
	protected BundleFile bundleFile;
	private ArrayMap<Object, BundleFile> bundleFiles;
	private boolean dirty = false;
	protected Dictionary<String, String> manifest;
	// This field is only used by PDE source lookup, and is set by a hook (bug 126517).  It serves no purpose at runtime.
	protected String fileName;
	// This is only used to keep track of when the same native library is loaded more than once
	protected Collection<String> loadedNativeCode;

	///////////////////// Begin values from Manifest     /////////////////////
	private String symbolicName;
	private Version version;
	private String activator;
	private String classpath;
	private String executionEnvironment;
	private String dynamicImports;
	private int type;

	///////////////////// End values from Manifest       /////////////////////

	/**
	 * Constructs a new BaseData with the specified id for the specified adaptor
	 * @param id the id of the BaseData
	 * @param adaptor the adaptor of the BaseData
	 */
	public BaseData(long id, BaseAdaptor adaptor) {
		this.id = id;
		this.adaptor = adaptor;
	}

	/**
	 * This method calls all the configured class loading hooks {@link ClassLoadingHook#createClassLoader(ClassLoader, ClassLoaderDelegate, BundleProtectionDomain, BaseData, String[])} 
	 * methods until on returns a non-null value.  If none of the class loading hooks returns a non-null value 
	 * then the default classloader implementation is used.
	 * @see BundleData#createClassLoader(ClassLoaderDelegate, BundleProtectionDomain, String[])
	 */
	public BundleClassLoader createClassLoader(ClassLoaderDelegate delegate, BundleProtectionDomain domain, String[] bundleclasspath) {
		ClassLoadingHook[] hooks = adaptor.getHookRegistry().getClassLoadingHooks();
		ClassLoader parent = adaptor.getBundleClassLoaderParent();
		BaseClassLoader cl = null;
		for (int i = 0; i < hooks.length && cl == null; i++)
			cl = hooks[i].createClassLoader(parent, delegate, domain, this, bundleclasspath);
		if (cl == null)
			cl = new DefaultClassLoader(parent, delegate, domain, this, bundleclasspath);
		return cl;
	}

	public final URL getEntry(final String path) {
		if (System.getSecurityManager() == null)
			return getEntry0(path);
		return AccessController.doPrivileged(new PrivilegedAction<URL>() {
			public URL run() {
				return getEntry0(path);
			}
		});
	}

	final URL getEntry0(String path) {
		BundleEntry entry = getBundleFile().getEntry(path);
		if (entry == null)
			return null;
		path = BundleFile.fixTrailingSlash(path, entry);
		try {
			//use the constant string for the protocol to prevent duplication
			return new URL(Constants.OSGI_ENTRY_URL_PROTOCOL, Long.toString(id) + BundleResourceHandler.BID_FWKID_SEPARATOR + Integer.toString(adaptor.hashCode()), 0, path, new Handler(entry, adaptor));
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public final Enumeration<String> getEntryPaths(String path) {
		return getBundleFile().getEntryPaths(path);
	}

	/**
	 * This method calls each configured classloading hook {@link ClassLoadingHook#findLibrary(BaseData, String)} method 
	 * until the first one returns a non-null value.
	 * @see BundleData#findLibrary(String)
	 */
	public String findLibrary(String libname) {
		ClassLoadingHook[] hooks = adaptor.getHookRegistry().getClassLoadingHooks();
		String result = null;
		for (int i = 0; i < hooks.length; i++) {
			result = hooks[i].findLibrary(this, libname);
			if (result != null)
				break;
		}
		// check to see if this library has been loaded by another class loader
		if (result != null)
			synchronized (this) {
				if (loadedNativeCode == null)
					loadedNativeCode = new ArrayList<String>(1);
				if (loadedNativeCode.contains(result) || COPY_NATIVES) {
					// we must copy the library to a temp space to allow another class loader to load the library
					String temp = copyToTempLibrary(result);
					if (temp != null)
						result = temp;
				} else {
					loadedNativeCode.add(result);
				}
			}
		return result;
	}

	private String copyToTempLibrary(String result) {
		try {
			return adaptor.getStorage().copyToTempLibrary(this, result);
		} catch (IOException e) {
			adaptor.getFrameworkLog().log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, e.getMessage(), 0, e, null));
		}
		return null;
	}

	public void installNativeCode(String[] nativepaths) throws BundleException {
		adaptor.getStorage().installNativeCode(this, nativepaths);
	}

	public File getDataFile(String path) {
		return adaptor.getStorage().getDataFile(this, path);
	}

	public Dictionary<String, String> getManifest() throws BundleException {
		if (manifest == null)
			manifest = adaptor.getStorage().loadManifest(this);
		return manifest;
	}

	public long getBundleID() {
		return id;
	}

	public final String getLocation() {
		return location;
	}

	/**
	 * Sets the location of this bundledata
	 * @param location the location of this bundledata
	 */
	public final void setLocation(String location) {
		this.location = location;
	}

	public final long getLastModified() {
		return lastModified;
	}

	/**
	 * Sets the last modified time stamp of this bundledata
	 * @param lastModified the last modified time stamp of this bundledata
	 */
	public final void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public synchronized void close() throws IOException {
		if (bundleFile != null)
			getBundleFile().close(); // only close the bundleFile if it already exists.
		if (bundleFiles != null) {
			for (BundleFile bundlefile : bundleFiles.getValues())
				bundlefile.close();
			bundleFiles.clear();
		}
	}

	public void open() throws IOException {
		getBundleFile().open();
	}

	public final void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

	/**
	 * Returns the bundle object of this BaseData
	 * @return the bundle object of this BaseData
	 */
	public final Bundle getBundle() {
		return bundle;
	}

	public int getStartLevel() {
		return startLevel;
	}

	public int getStatus() {
		return status;
	}

	/**
	 * This method calls each configured storage hook {@link StorageHook#forgetStartLevelChange(int)} method.
	 * If one returns true then this bundledata is not marked dirty.
	 * @see BundleData#setStartLevel(int)
	 */
	public void setStartLevel(int value) {
		startLevel = setPersistentData(value, true, startLevel);
	}

	/**
	 * This method calls each configured storage hook {@link StorageHook#forgetStatusChange(int)} method.
	 * If one returns true then this bundledata is not marked dirty.
	 * @see BundleData#setStatus(int)
	 */
	public void setStatus(int value) {
		status = setPersistentData(value, false, status);
	}

	private int setPersistentData(int value, boolean isStartLevel, int orig) {
		StorageHook[] hooks = getStorageHooks();
		for (int i = 0; i < hooks.length; i++)
			if (isStartLevel) {
				if (hooks[i].forgetStartLevelChange(value))
					return value;
			} else {
				if (hooks[i].forgetStatusChange(value))
					return value;
			}
		if (value != orig)
			dirty = true;
		return value;
	}

	/**
	 * @throws IOException  
	 */
	public void save() throws IOException {
		adaptor.getStorage().save(this);
	}

	/**
	 * Returns true if this bundledata is dirty
	 * @return true if this bundledata is dirty
	 */
	public boolean isDirty() {
		return dirty;
	}

	/**
	 * Sets the dirty flag for this BaseData
	 * @param dirty the dirty flag
	 */
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public final String getSymbolicName() {
		return symbolicName;
	}

	/**
	 * Sets the symbolic name of this BaseData
	 * @param symbolicName the symbolic name
	 */
	public final void setSymbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	public final Version getVersion() {
		return version;
	}

	/**
	 * Sets the version of this BaseData
	 * @param version the version
	 */
	public final void setVersion(Version version) {
		this.version = version;
	}

	public final int getType() {
		return type;
	}

	/**
	 * Sets the type of this BaseData
	 * @param type the type
	 */
	public final void setType(int type) {
		this.type = type;
	}

	public final String[] getClassPath() throws BundleException {
		ManifestElement[] classpathElements = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, classpath);
		return getClassPath(classpathElements);
	}

	// TODO make classpath a String[] instead of saving a comma separated string.
	public String getClassPathString() {
		return classpath;
	}

	//TODO make classpath a String[] instead of saving a comma separated string.
	public void setClassPathString(String classpath) {
		this.classpath = classpath;
	}

	public final String getActivator() {
		return activator;
	}

	/**
	 * Sets the activator of this BaseData
	 * @param activator the activator
	 */
	public final void setActivator(String activator) {
		this.activator = activator;
	}

	public final String getExecutionEnvironment() {
		return executionEnvironment;
	}

	/**
	 * Sets the execution environment of this BaseData
	 * @param executionEnvironment the execution environment
	 */
	public void setExecutionEnvironment(String executionEnvironment) {
		this.executionEnvironment = executionEnvironment;
	}

	public final String getDynamicImports() {
		return dynamicImports;
	}

	/**
	 * Sets the dynamic imports of this BaseData
	 * @param dynamicImports the dynamic imports
	 */
	public void setDynamicImports(String dynamicImports) {
		this.dynamicImports = dynamicImports;
	}

	/**
	 * Returns the adaptor for this BaseData
	 * @return the adaptor
	 */
	public final BaseAdaptor getAdaptor() {
		return adaptor;
	}

	/**
	 * Returns the BundleFile for this BaseData.  The first time this method is called the
	 * configured storage {@link BaseAdaptor#createBundleFile(Object, BaseData)} method is called.
	 * @return the BundleFile
	 * @throws IllegalArgumentException
	 */
	public synchronized BundleFile getBundleFile() throws IllegalArgumentException {
		if (bundleFile == null)
			try {
				bundleFile = adaptor.createBundleFile(null, this);
			} catch (IOException e) {
				throw (IllegalArgumentException) new IllegalArgumentException(e.getMessage()).initCause(e);
			}
		return bundleFile;
	}

	public synchronized BundleFile getBundleFile(Object content, boolean base) {
		return base ? bundleFile : (bundleFiles == null) ? null : bundleFiles.get(content);
	}

	public synchronized void setBundleFile(Object content, BundleFile bundleFile) {
		if (bundleFiles == null)
			bundleFiles = new ArrayMap<Object, BundleFile>(1);
		bundleFiles.put(content, bundleFile);
	}

	private static String[] getClassPath(ManifestElement[] classpath) {
		if (classpath == null) {
			if (Debug.DEBUG_LOADER)
				Debug.println("  no classpath"); //$NON-NLS-1$
			/* create default BundleClassPath */
			return new String[] {"."}; //$NON-NLS-1$
		}

		List<String> result = new ArrayList<String>(classpath.length);
		for (int i = 0; i < classpath.length; i++) {
			if (Debug.DEBUG_LOADER)
				Debug.println("  found classpath entry " + classpath[i].getValueComponents()); //$NON-NLS-1$
			String[] paths = classpath[i].getValueComponents();
			for (int j = 0; j < paths.length; j++) {
				result.add(paths[j]);
			}
		}

		return result.toArray(new String[result.size()]);
	}

	/**
	 * Returns the storage hook which is keyed by the specified key
	 * @param key the key of the storage hook to get
	 * @return the storage hook which is keyed by the specified key
	 */
	public StorageHook getStorageHook(String key) {
		if (storageHooks == null)
			return null;
		for (int i = 0; i < storageHooks.length; i++)
			if (storageHooks[i].getKey().equals(key))
				return storageHooks[i];
		return null;
	}

	/**
	 * Sets the instance storage hooks for this base data.  This is method
	 * may only be called once for the lifetime of the base data.  Once set,
	 * the list of storage hooks remains constant.
	 * @param storageHooks the storage hook to add
	 */
	public void setStorageHooks(StorageHook[] storageHooks) {
		if (this.storageHooks != null)
			return; // only allow this to be set once.
		this.storageHooks = storageHooks;
	}

	/**
	 * Returns all the storage hooks associated with this BaseData
	 * @return all the storage hooks associated with this BaseData
	 */
	public StorageHook[] getStorageHooks() {
		return storageHooks == null ? new StorageHook[0] : storageHooks;
	}

	/**
	 * Gets called by BundleFile during {@link BundleFile#getFile(String, boolean)}.  This method 
	 * will allocate a File object where content of the specified path may be 
	 * stored for the current generation of the base data.  The returned File object may 
	 * not exist if the content has not previously be stored.
	 * @param path the path to the content to extract from the base data
	 * @return a file object where content of the specified path may be stored.
	 */
	public File getExtractFile(String path) {
		return adaptor.getStorage().getExtractFile(this, path);
	}

	/**
	 * This is only used to support PDE source lookup.  The field named &quot;fileName&quot; 
	 * must be set for PDE to access the absolute path string.
	 * @param fileName an absolute path string to the base bundle file. 
	 */
	// This is only done for PDE source lookup (bug 126517)
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Return a string representation of the bundle that can be used in debug messages.
	 * 
	 * @return String representation of the bundle
	 */
	public String toString() {
		String name = getSymbolicName();
		if (name == null)
			return getLocation();
		Version ver = getVersion();
		if (ver == null)
			return name;
		return name + "_" + ver; //$NON-NLS-1$
	}

	public Enumeration<URL> findLocalResources(String path) {
		String[] cp;
		try {
			cp = getClassPath();
		} catch (BundleException e) {
			cp = new String[0];
		}
		if (cp == null)
			cp = new String[0];
		// this is not optimized; degenerate case of someone calling getResource on an unresolved bundle!
		ClasspathManager cm = new ClasspathManager(this, cp, null);
		cm.initialize();
		Enumeration<URL> result = cm.findLocalResources(path);
		// no need to close ClassPathManager because the BundleFile objects are stored in the BaseData and closed on shutdown or uninstall
		return result;
	}
}
