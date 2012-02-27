/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.baseadaptor.loader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.*;
import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingStatsHook;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.internal.baseadaptor.AdaptorMsg;
import org.eclipse.osgi.internal.baseadaptor.ArrayMap;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;

/**
 * A helper class for <code>BaseClassLoader</code> implementations.  This class will keep track of 
 * <code>ClasspathEntry</code> objects for the host bundle and any attached fragment bundles.  This 
 * class takes care of searching the <code>ClasspathEntry</code> objects for a base class loader
 * implementation.  Additional behavior may be added to a classpath manager by configuring 
 * <code>ClassLoadingHook</code> and <code>ClassLoadingStatsHook</code>.
 * @see BaseClassLoader
 * @see ClassLoadingHook
 * @see ClassLoadingStatsHook
 * @since 3.2
 */
public class ClasspathManager {
	private static final FragmentClasspath[] emptyFragments = new FragmentClasspath[0];
	private final static String PROP_CLASSLOADER_LOCK = "osgi.classloader.lock"; //$NON-NLS-1$
	private final static String VALUE_CLASSNAME_LOCK = "classname"; //$NON-NLS-1$
	private final static boolean LOCK_CLASSNAME = VALUE_CLASSNAME_LOCK.equals(FrameworkProperties.getProperty(PROP_CLASSLOADER_LOCK));
	private final static Class<?>[] NULL_CLASS_RESULT = new Class[2];

	private final BaseData data;
	private final String[] classpath;
	private final BaseClassLoader classloader;
	private final boolean isParallelClassLoader;
	private final Map<String, Thread> classNameLocks = new HashMap<String, Thread>(5);

	// Note that PDE has internal dependency on this field type/name (bug 267238)
	private ClasspathEntry[] entries;
	// Note that PDE has internal dependency on this field type/name (bug 267238)
	private FragmentClasspath[] fragments = emptyFragments;
	// a Map<String,String> where "libname" is the key and libpath" is the value
	private ArrayMap<String, String> loadedLibraries = null;
	// used to detect recusive defineClass calls for the same class on the same class loader (bug 345500)
	private ThreadLocal<Collection<String>> currentlyDefining = new ThreadLocal<Collection<String>>();

	/**
	 * Constructs a classpath manager for the given host base data, classpath and base class loader
	 * @param data the host base data for this classpath manager
	 * @param classpath the host classpath for this classpath manager
	 * @param classloader the BaseClassLoader for this classpath manager
	 */
	public ClasspathManager(BaseData data, String[] classpath, BaseClassLoader classloader) {
		this.data = data;
		this.classpath = classpath;
		this.classloader = classloader;
		isParallelClassLoader = (classloader instanceof ParallelClassLoader) ? ((ParallelClassLoader) classloader).isParallelCapable() : false;
	}

	/**
	 * initializes this classpath manager.  This must be called after all existing fragments have been
	 * attached and before any resources/classes are loaded using this classpath manager.
	 * <p>
	 * After the classpath manager is initialized all configured class loading hooks 
	 * {@link ClassLoadingHook#initializedClassLoader(BaseClassLoader, BaseData)} methods are called.
	 * </p>
	 */
	public void initialize() {
		entries = buildClasspath(classpath, this, data, classloader == null ? null : classloader.getDomain());
		ClassLoadingHook[] hooks = data.getAdaptor().getHookRegistry().getClassLoadingHooks();
		if (classloader != null)
			for (int i = 0; i < hooks.length; i++)
				hooks[i].initializedClassLoader(classloader, data);
	}

	/**
	 * Closes all the classpath entry resources for this classpath manager.
	 *
	 */
	public void close() {
		if (entries != null) {
			for (int i = 0; i < entries.length; i++) {
				if (entries[i] != null) {
					try {
						entries[i].getBundleFile().close();
					} catch (IOException e) {
						data.getAdaptor().getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, data.getBundle(), e);
					}
				}
			}
		}
		for (int i = 0; i < fragments.length; i++)
			fragments[i].close();
	}

	/**
	 * Attaches the specified sourcedata, sourcedomain and sourceclasspath to this classpath manager
	 * @param sourcedata the source fragment BundleData that should be attached.
	 * @param sourcedomain the source fragment domain that should be attached.
	 * @param sourceclasspath the source fragment classpath that should be attached.
	 */
	public void attachFragment(BundleData sourcedata, ProtectionDomain sourcedomain, String[] sourceclasspath) {
		try {
			sourcedata.open(); /* make sure the BundleData is open */
		} catch (IOException e) {
			((BaseData) sourcedata).getAdaptor().getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, ((BaseData) sourcedata).getBundle(), e);
		}
		ClasspathEntry[] fragEntries = buildClasspath(sourceclasspath, this, (BaseData) sourcedata, sourcedomain);
		FragmentClasspath fragClasspath = new FragmentClasspath((BaseData) sourcedata, fragEntries, sourcedomain);
		insertFragment(fragClasspath);
	}

	private synchronized void insertFragment(FragmentClasspath fragClasspath) {
		FragmentClasspath[] newFragments = new FragmentClasspath[fragments.length + 1];
		// Find a place in the fragment list to insert this fragment.
		long fragID = fragClasspath.getBundleData().getBundleID();
		int insert = 0;
		for (int i = 0; i < fragments.length; i++) {
			long otherID = fragments[i].getBundleData().getBundleID();
			if (insert == 0 && fragID < otherID) {
				newFragments[i] = fragClasspath;
				insert = 1;
			}
			newFragments[i + insert] = fragments[i];
		}
		// This fragment has the highest ID; put it at the end of the list.
		if (insert == 0)
			newFragments[fragments.length] = fragClasspath;
		fragments = newFragments;
	}

	private static ClasspathEntry[] buildClasspath(String[] cp, ClasspathManager hostloader, BaseData sourcedata, ProtectionDomain sourcedomain) {
		ArrayList<ClasspathEntry> result = new ArrayList<ClasspathEntry>(cp.length);
		// add the regular classpath entries.
		for (int i = 0; i < cp.length; i++)
			findClassPathEntry(result, cp[i], hostloader, sourcedata, sourcedomain);
		return result.toArray(new ClasspathEntry[result.size()]);
	}

	/**
	 * Finds all the ClasspathEntry objects for the requested classpath.  This method will first call all
	 * the configured class loading hooks {@link ClassLoadingHook#addClassPathEntry(ArrayList, String, ClasspathManager, BaseData, ProtectionDomain)}
	 * methods.  This allows class loading hooks to add additional ClasspathEntry objects to the result for the 
	 * requested classpath.  Then the local host classpath entries and attached fragment classpath entries are
	 * searched.
	 * @param result a list of ClasspathEntry objects.  This list is used to add new ClasspathEntry objects to.
	 * @param cp the requested classpath.
	 * @param hostloader the host classpath manager for the classpath
	 * @param sourcedata the source EquionoxData to search for the classpath
	 * @param sourcedomain the source domain to used by the new ClasspathEntry
	 */
	public static void findClassPathEntry(ArrayList<ClasspathEntry> result, String cp, ClasspathManager hostloader, BaseData sourcedata, ProtectionDomain sourcedomain) {
		// look in classpath manager hooks first
		ClassLoadingHook[] loaderHooks = sourcedata.getAdaptor().getHookRegistry().getClassLoadingHooks();
		boolean hookAdded = false;
		for (int i = 0; i < loaderHooks.length; i++)
			hookAdded |= loaderHooks[i].addClassPathEntry(result, cp, hostloader, sourcedata, sourcedomain);
		if (!addClassPathEntry(result, cp, hostloader, sourcedata, sourcedomain) && !hookAdded) {
			BundleException be = new BundleException(NLS.bind(AdaptorMsg.BUNDLE_CLASSPATH_ENTRY_NOT_FOUND_EXCEPTION, cp, sourcedata.getLocation()), BundleException.MANIFEST_ERROR);
			sourcedata.getAdaptor().getEventPublisher().publishFrameworkEvent(FrameworkEvent.INFO, sourcedata.getBundle(), be);
		}
	}

	/**
	 * Adds a ClasspathEntry for the requested classpath to the result.  The local host classpath entries
	 * are searched first and then attached fragments classpath entries are searched.  The search stops once the first
	 * classpath entry is found.
	 * @param result a list of ClasspathEntry objects.  This list is used to add new ClasspathEntry objects to.
	 * @param cp the requested classpath.
	 * @param hostloader the host classpath manager for the classpath
	 * @param sourcedata the source EquionoxData to search for the classpath
	 * @param sourcedomain the source domain to used by the new ClasspathEntry
	 * @return true if a ClasspathEntry was added to the result
	 */
	public static boolean addClassPathEntry(ArrayList<ClasspathEntry> result, String cp, ClasspathManager hostloader, BaseData sourcedata, ProtectionDomain sourcedomain) {
		if (cp.equals(".")) { //$NON-NLS-1$
			result.add(hostloader.createClassPathEntry(sourcedata.getBundleFile(), sourcedomain, sourcedata));
			return true;
		}
		ClasspathEntry element = hostloader.getClasspath(cp, sourcedata, sourcedomain);
		if (element != null) {
			result.add(element);
			return true;
		}
		// need to check in fragments for the classpath entry.
		// only check for fragments if the data is the host's data.
		if (hostloader.data == sourcedata)
			for (int i = 0; i < hostloader.fragments.length; i++) {
				FragmentClasspath fragCP = hostloader.fragments[i];
				element = hostloader.getClasspath(cp, fragCP.getBundleData(), fragCP.getDomain());
				if (element != null) {
					result.add(element);
					return true;
				}
			}
		return false;
	}

	/**
	 * Creates a new ClasspathEntry object for the requested classpath if the source exists.
	 * @param cp the requested classpath.
	 * @param sourcedata the source EquionoxData to search for the classpath
	 * @param sourcedomain the source domain to used by the new ClasspathEntry
	 * @return a new ClasspathEntry for the requested classpath or null if the source does not exist.
	 */
	public ClasspathEntry getClasspath(String cp, BaseData sourcedata, ProtectionDomain sourcedomain) {
		BundleFile bundlefile = null;
		File file;
		BundleEntry cpEntry = sourcedata.getBundleFile().getEntry(cp);
		// check for internal library directories in a bundle jar file
		if (cpEntry != null && cpEntry.getName().endsWith("/")) //$NON-NLS-1$
			bundlefile = createBundleFile(cp, sourcedata);
		// check for internal library jars
		else if ((file = sourcedata.getBundleFile().getFile(cp, false)) != null)
			bundlefile = createBundleFile(file, sourcedata);
		if (bundlefile != null)
			return createClassPathEntry(bundlefile, sourcedomain, sourcedata);
		return null;
	}

	/**
	 * Uses the requested classpath as an absolute path to locate a source for a new ClasspathEntry.
	 * @param cp the requested classpath
	 * @param sourcedata the source EquionoxData to search for the classpath
	 * @param sourcedomain the source domain to used by the new ClasspathEntry
	 * @return a classpath entry which uses an absolut path as a source
	 */
	public ClasspathEntry getExternalClassPath(String cp, BaseData sourcedata, ProtectionDomain sourcedomain) {
		File file = new File(cp);
		if (!file.isAbsolute())
			return null;
		BundleFile bundlefile = createBundleFile(file, sourcedata);
		if (bundlefile != null)
			return createClassPathEntry(bundlefile, sourcedomain, sourcedata);
		return null;
	}

	private static BundleFile createBundleFile(Object content, BaseData sourcedata) {
		if (content == null || (content instanceof File && !((File) content).exists()))
			return null;
		try {
			return sourcedata.getAdaptor().createBundleFile(content, sourcedata);
		} catch (IOException e) {
			sourcedata.getAdaptor().getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, sourcedata.getBundle(), e);
		}
		return null;
	}

	private ClasspathEntry createClassPathEntry(BundleFile bundlefile, ProtectionDomain cpDomain, BaseData cpData) {
		ClasspathEntry entry;
		if (classloader != null)
			entry = classloader.createClassPathEntry(bundlefile, cpDomain);
		else
			entry = new ClasspathEntry(bundlefile, null);
		entry.setBaseData(cpData);
		Object domain = entry.getDomain();
		if (domain instanceof BundleProtectionDomain)
			((BundleProtectionDomain) domain).setBundle(cpData.getBundle());
		return entry;
	}

	/**
	 * Finds a local resource by searching the ClasspathEntry objects of the classpath manager.
	 * This method will first call all the configured class loading stats hooks 
	 * {@link ClassLoadingStatsHook#preFindLocalResource(String, ClasspathManager)} methods.  Then it 
	 * will search for the resource.  Finally it will call all the configured class loading stats hooks
	 * {@link ClassLoadingStatsHook#postFindLocalResource(String, URL, ClasspathManager)} methods.
	 * @param resource the requested resource name.
	 * @return the requested resource URL or null if the resource does not exist
	 */
	public URL findLocalResource(String resource) {
		ClassLoadingStatsHook[] hooks = data.getAdaptor().getHookRegistry().getClassLoadingStatsHooks();
		for (int i = 0; i < hooks.length; i++)
			hooks[i].preFindLocalResource(resource, this);
		URL result = null;
		try {
			result = findLocalResourceImpl(resource, -1);
			return result;
		} finally {
			for (int i = 0; i < hooks.length; i++)
				hooks[i].postFindLocalResource(resource, result, this);
		}
	}

	private URL findLocalResourceImpl(String resource, int classPathIndex) {
		URL result = null;
		int curIndex = 0;
		for (int i = 0; i < entries.length; i++) {
			if (entries[i] != null) {
				result = findResourceImpl(resource, entries[i].getBundleFile(), curIndex);
				if (result != null && (classPathIndex == -1 || classPathIndex == curIndex))
					return result;
			}
			curIndex++;
		}
		// look in fragments
		for (int i = 0; i < fragments.length; i++) {
			ClasspathEntry[] fragEntries = fragments[i].getEntries();
			for (int j = 0; j < fragEntries.length; j++) {
				result = findResourceImpl(resource, fragEntries[j].getBundleFile(), curIndex);
				if (result != null && (classPathIndex == -1 || classPathIndex == curIndex))
					return result;
				curIndex++;
			}
		}
		return null;
	}

	/**
	 * Finds the local resources by searching the ClasspathEntry objects of the classpath manager.
	 * @param resource the requested resource name.
	 * @return an enumeration of the the requested resources or null if the resources do not exist
	 */
	public Enumeration<URL> findLocalResources(String resource) {
		List<URL> resources = new ArrayList<URL>(6);
		int classPathIndex = 0;
		for (int i = 0; i < entries.length; i++) {
			if (entries[i] != null) {
				URL url = findResourceImpl(resource, entries[i].getBundleFile(), classPathIndex);
				if (url != null)
					resources.add(url);
			}
			classPathIndex++;
		}
		// look in fragments
		for (int i = 0; i < fragments.length; i++) {
			ClasspathEntry[] fragEntries = fragments[i].getEntries();
			for (int j = 0; j < fragEntries.length; j++) {
				URL url = findResourceImpl(resource, fragEntries[j].getBundleFile(), classPathIndex);
				if (url != null)
					resources.add(url);
				classPathIndex++;
			}
		}
		if (resources.size() > 0)
			return Collections.enumeration(resources);
		return null;
	}

	private URL findResourceImpl(String name, BundleFile bundlefile, int index) {
		return bundlefile.getResourceURL(name, data, index);
	}

	/**
	 * Finds a local entry by searching the ClasspathEntry objects of the classpath manager.
	 * @param path the requested entry path.
	 * @return the requested entry or null if the entry does not exist
	 */
	public BundleEntry findLocalEntry(String path) {
		return findLocalEntry(path, -1);
	}

	/**
	 * Finds a local entry by searching the ClasspathEntry with the specified
	 * class path index.
	 * @param path the requested entry path.
	 * @param classPathIndex the index of the ClasspathEntry to search
	 * @return the requested entry or null if the entry does not exist
	 */
	public BundleEntry findLocalEntry(String path, int classPathIndex) {
		BundleEntry result = null;
		int curIndex = 0;
		for (int i = 0; i < entries.length; i++) {
			if (entries[i] != null) {
				result = findEntryImpl(path, entries[i].getBundleFile());
				if (result != null && (classPathIndex == -1 || classPathIndex == curIndex))
					return result;
			}
			curIndex++;
		}
		// look in fragments
		for (int i = 0; i < fragments.length; i++) {
			ClasspathEntry[] fragEntries = fragments[i].getEntries();
			for (int j = 0; j < fragEntries.length; j++) {
				result = findEntryImpl(path, fragEntries[j].getBundleFile());
				if (result != null && (classPathIndex == -1 || classPathIndex == curIndex))
					return result;
				curIndex++;
			}
		}
		return null;
	}

	/**
	 * Finds the local entries by searching the ClasspathEntry objects of the classpath manager.
	 * @param path the requested entry path.
	 * @return an enumeration of the the requested entries or null if the entries do not exist
	 */
	public Enumeration<BundleEntry> findLocalEntries(String path) {
		List<BundleEntry> objects = new ArrayList<BundleEntry>(6);
		for (int i = 0; i < entries.length; i++) {
			if (entries[i] != null) {
				BundleEntry result = findEntryImpl(path, entries[i].getBundleFile());
				if (result != null)
					objects.add(result);
			}
		}
		// look in fragments
		for (int i = 0; i < fragments.length; i++) {
			ClasspathEntry[] fragEntries = fragments[i].getEntries();
			for (int j = 0; j < fragEntries.length; j++) {
				BundleEntry result = findEntryImpl(path, fragEntries[j].getBundleFile());
				if (result != null)
					objects.add(result);
			}
		}
		if (objects.size() > 0)
			return Collections.enumeration(objects);
		return null;
	}

	private BundleEntry findEntryImpl(String path, BundleFile bundleFile) {
		return bundleFile.getEntry(path);
	}

	/**
	 * Finds a local class by searching the ClasspathEntry objects of the classpath manager.
	 * This method will first call all the configured class loading stats hooks 
	 * {@link ClassLoadingStatsHook#preFindLocalClass(String, ClasspathManager)} methods.  Then it 
	 * will search for the class.  If a class is found then
	 * <ol>
	 *   <li>All configured class loading hooks
	 *       {@link ClassLoadingHook#processClass(String, byte[], ClasspathEntry, BundleEntry, ClasspathManager)}
	 *       methods will be called.</li>
	 *   <li>The class is then defined.</li>  
	 *   <li>Finally, all configured class loading 
	 *       stats hooks {@link ClassLoadingStatsHook#recordClassDefine(String, Class, byte[], ClasspathEntry, BundleEntry, ClasspathManager)}
	 *       methods are called.</li>
	 * </ol>
	 * Finally all the configured class loading stats hooks
	 * {@link ClassLoadingStatsHook#postFindLocalClass(String, Class, ClasspathManager)} methods are called.
	 * @param classname the requested class name.
	 * @return the requested class
	 * @throws ClassNotFoundException if the class does not exist
	 */
	public Class<?> findLocalClass(String classname) throws ClassNotFoundException {
		Class<?> result = null;
		ClassLoadingStatsHook[] hooks = data.getAdaptor().getHookRegistry().getClassLoadingStatsHooks();
		try {
			for (int i = 0; i < hooks.length; i++)
				hooks[i].preFindLocalClass(classname, this);
			result = findLoadedClass(classname);
			if (result != null)
				return result;
			result = findLocalClassImpl(classname, hooks);
			return result;
		} finally {
			for (int i = 0; i < hooks.length; i++)
				hooks[i].postFindLocalClass(classname, result, this);
		}
	}

	private Class<?> findLoadedClass(String classname) {
		if (LOCK_CLASSNAME || isParallelClassLoader) {
			boolean initialLock = lockClassName(classname);
			try {
				return classloader.publicFindLoaded(classname);
			} finally {
				if (initialLock)
					unlockClassName(classname);
			}
		}
		synchronized (classloader) {
			return classloader.publicFindLoaded(classname);
		}
	}

	private Class<?> findLocalClassImpl(String classname, ClassLoadingStatsHook[] hooks) throws ClassNotFoundException {
		Class<?> result = null;
		for (int i = 0; i < entries.length; i++) {
			if (entries[i] != null) {
				result = findClassImpl(classname, entries[i], hooks);
				if (result != null)
					return result;
			}
		}
		// look in fragments.
		for (int i = 0; i < fragments.length; i++) {
			ClasspathEntry[] fragEntries = fragments[i].getEntries();
			for (int j = 0; j < fragEntries.length; j++) {
				result = findClassImpl(classname, fragEntries[j], hooks);
				if (result != null)
					return result;
			}
		}
		throw new ClassNotFoundException(classname);
	}

	private boolean lockClassName(String classname) {
		synchronized (classNameLocks) {
			Object lockingThread = classNameLocks.get(classname);
			Thread current = Thread.currentThread();
			if (lockingThread == current)
				return false;
			while (true) {
				if (lockingThread == null) {
					classNameLocks.put(classname, current);
					return true;
				}
				try {
					classNameLocks.wait();
					lockingThread = classNameLocks.get(classname);
				} catch (InterruptedException e) {
					current.interrupt();
					throw (LinkageError) new LinkageError(classname).initCause(e);
				}
			}
		}
	}

	private void unlockClassName(String classname) {
		synchronized (classNameLocks) {
			classNameLocks.remove(classname);
			classNameLocks.notifyAll();
		}
	}

	private Class<?> findClassImpl(String name, ClasspathEntry classpathEntry, ClassLoadingStatsHook[] hooks) {
		if (Debug.DEBUG_LOADER)
			Debug.println("BundleClassLoader[" + classpathEntry.getBundleFile() + "].findClassImpl(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
		String filename = name.replace('.', '/').concat(".class"); //$NON-NLS-1$
		BundleEntry entry = classpathEntry.getBundleFile().getEntry(filename);
		if (entry == null)
			return null;

		byte[] classbytes;
		try {
			classbytes = entry.getBytes();
		} catch (IOException e) {
			if (Debug.DEBUG_LOADER)
				Debug.println("  IOException reading " + filename + " from " + classpathEntry.getBundleFile()); //$NON-NLS-1$ //$NON-NLS-2$
			throw (LinkageError) new LinkageError("Error reading class bytes: " + name).initCause(e); //$NON-NLS-1$
		}
		if (Debug.DEBUG_LOADER) {
			Debug.println("  read " + classbytes.length + " bytes from " + classpathEntry.getBundleFile() + "/" + filename); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			Debug.println("  defining class " + name); //$NON-NLS-1$
		}

		Collection<String> current = currentlyDefining.get();
		if (current == null) {
			current = new ArrayList<String>(5);
			currentlyDefining.set(current);
		}
		if (current.contains(name))
			return null; // avoid recursive defines (bug 345500)
		try {
			current.add(name);
			return defineClass(name, classbytes, classpathEntry, entry, hooks);
		} catch (Error e) {
			if (Debug.DEBUG_LOADER)
				Debug.println("  error defining class " + name); //$NON-NLS-1$
			throw e;
		} finally {
			current.remove(name);
		}
	}

	/**
	 * Defines the specified class.  This method will first call all the configured class loading hooks 
	 * {@link ClassLoadingHook#processClass(String, byte[], ClasspathEntry, BundleEntry, ClasspathManager)} 
	 * methods.  Then it will call the {@link BaseClassLoader#defineClass(String, byte[], ClasspathEntry, BundleEntry)}
	 * method to define the class. After that, the class loading stat hooks are called to announce the class
	 * definition.
	 * @param name the name of the class to define
	 * @param classbytes the class bytes
	 * @param classpathEntry the classpath entry used to load the class bytes
	 * @param entry the BundleEntry used to load the class bytes
	 * @param statsHooks the class loading stat hooks
	 * @return the defined class
	 */
	private Class<?> defineClass(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry, ClassLoadingStatsHook[] statsHooks) {
		ClassLoadingHook[] hooks = data.getAdaptor().getHookRegistry().getClassLoadingHooks();
		byte[] modifiedBytes = classbytes;
		// The result holds two Class objects.  
		// The first slot to either a pre loaded class or the newly defined class.
		// The second slot is only set to a newly defined class object if it was successfully defined
		Class<?>[] result = NULL_CLASS_RESULT;
		try {
			for (int i = 0; i < hooks.length; i++) {
				modifiedBytes = hooks[i].processClass(name, classbytes, classpathEntry, entry, this);
				if (modifiedBytes != null)
					classbytes = modifiedBytes;
			}
			if (LOCK_CLASSNAME || isParallelClassLoader) {
				boolean initialLock = lockClassName(name);
				try {
					result = defineClassHoldingLock(name, classbytes, classpathEntry, entry);
				} finally {
					if (initialLock)
						unlockClassName(name);
				}
			} else {
				synchronized (classloader) {
					result = defineClassHoldingLock(name, classbytes, classpathEntry, entry);
				}
			}
		} finally {
			for (int i = 0; i < statsHooks.length; i++)
				// only pass the newly defined class to the hook
				statsHooks[i].recordClassDefine(name, result[1], classbytes, classpathEntry, entry, this);
		}
		// return either the pre-loaded class or the newly defined class
		return result[0];
	}

	private Class<?>[] defineClassHoldingLock(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry) {
		Class<?>[] result = new Class[2];
		// must call findLoadedClass here even if it was called earlier,
		// the findLoadedClass and defineClass calls must be atomic
		result[0] = classloader.publicFindLoaded(name);
		if (result[0] == null)
			result[0] = result[1] = classloader.defineClass(name, classbytes, classpathEntry, entry);
		return result;
	}

	/**
	 * Returns the host base data for this classpath manager
	 * @return the host base data for this classpath manager
	 */
	public BaseData getBaseData() {
		return data;
	}

	/**
	 * Returns the fragment classpaths of this classpath manager
	 * @return the fragment classpaths of this classpath manager
	 */
	public FragmentClasspath[] getFragmentClasspaths() {
		return fragments;
	}

	/**
	 * Returns the host classpath entries for this classpath manager
	 * @return the host classpath entries for this classpath manager
	 */
	public ClasspathEntry[] getHostClasspathEntries() {
		return entries;
	}

	/**
	 * Returns the base class loader used by this classpath manager
	 * @return the base class loader used by this classpath manager
	 */
	public BaseClassLoader getBaseClassLoader() {
		return classloader;
	}

	/**
	 * Finds a library for the bundle represented by this class path managert
	 * @param libname the library name
	 * @return The absolution path to the library or null if not found
	 */
	public String findLibrary(String libname) {
		synchronized (this) {
			if (loadedLibraries == null)
				loadedLibraries = new ArrayMap<String, String>(1);
		}
		synchronized (loadedLibraries) {
			// we assume that each classloader will load a small number of of libraries
			// instead of wasting space with a map we iterate over our collection of found libraries
			// each element is a String[2], each array is {"libname", "libpath"}
			String libpath = loadedLibraries.get(libname);
			if (libpath != null)
				return libpath;

			libpath = classloader.getDelegate().findLibrary(libname);
			if (libpath != null)
				loadedLibraries.put(libname, libpath);
			return libpath;
		}
	}

	/**
	 * @see BundleClassLoader#findEntries(String, String, int)
	 */
	public List<URL> findEntries(String path, String filePattern, int options) {
		BaseAdaptor adaptor = getBaseData().getAdaptor();
		List<BundleData> datas = new ArrayList<BundleData>();
		// first get the host bundle file
		datas.add(getBaseData());
		// next get the attached fragments bundle files
		FragmentClasspath[] currentFragments = getFragmentClasspaths();
		for (FragmentClasspath fragmentClasspath : currentFragments)
			datas.add(fragmentClasspath.getBundleData());

		@SuppressWarnings("unchecked")
		List<URL> result = Collections.EMPTY_LIST;
		// now search over all the bundle files
		Enumeration<URL> eURLs = adaptor.findEntries(datas, path, filePattern, options);
		if (eURLs == null)
			return result;
		result = new ArrayList<URL>();
		while (eURLs.hasMoreElements())
			result.add(eURLs.nextElement());
		return Collections.unmodifiableList(result);
	}

	/**
	 * @see BundleClassLoader#listLocalResources(String, String, int)
	 */
	public Collection<String> listLocalResources(String path, String filePattern, int options) {
		List<BundleFile> bundleFiles = new ArrayList<BundleFile>();

		ClasspathEntry[] cpEntries = getHostClasspathEntries();
		for (ClasspathEntry cpEntry : cpEntries)
			bundleFiles.add(cpEntry.getBundleFile());

		FragmentClasspath[] currentFragments = getFragmentClasspaths();
		for (FragmentClasspath fragmentClasspath : currentFragments) {
			ClasspathEntry[] fragEntries = fragmentClasspath.getEntries();
			for (ClasspathEntry cpEntry : fragEntries)
				bundleFiles.add(cpEntry.getBundleFile());
		}

		return getBaseData().getAdaptor().listEntryPaths(bundleFiles, path, filePattern, options);
	}
}
