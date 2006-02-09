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

package org.eclipse.osgi.baseadaptor.loader;

import java.io.*;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.*;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.*;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingStatsHook;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.internal.baseadaptor.AdaptorMsg;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;

/**
 * A helper class for <code>BaseClassLoader</code> implementations.  This class will keep track of 
 * <code>ClasspathEntry</code> objects for the host bundle and any attached fragment bundles.  This 
 * class takes care of seaching the <code>ClasspathEntry</code> objects for a base class loader
 * implementation.  Additional behavior may be added to a classpath manager by configuring 
 * <code>ClassLoadingHook</code> and <code>ClassLoadingStatsHook</code>.
 * @see BaseClassLoader
 * @see ClassLoadingHook
 * @see ClassLoadingStatsHook
 * @since 3.2
 */
public class ClasspathManager {
	private static final FragmentClasspath[] emptyFragments = new FragmentClasspath[0];
	private static final int BUF_SIZE = 8 * 1024;

	private BaseData data;
	private String[] classpath;
	private ClasspathEntry[] entries;
	private BaseClassLoader classloader;
	private FragmentClasspath[] fragments = emptyFragments;

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
	}

	/**
	 * initializes this classpath manager.  This must be called after all existing fragments have been
	 * attached and before any resources/classes are loaded using this classpath manager.
	 */
	public void initialize() {
		entries = buildClasspath(classpath, this, data, classloader.getDomain());
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
		ArrayList result = new ArrayList(cp.length);
		// add the regular classpath entries.
		for (int i = 0; i < cp.length; i++)
			findClassPathEntry(result, cp[i], hostloader, sourcedata, sourcedomain);
		return (ClasspathEntry[]) result.toArray(new ClasspathEntry[result.size()]);
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
	public static void findClassPathEntry(ArrayList result, String cp, ClasspathManager hostloader, BaseData sourcedata, ProtectionDomain sourcedomain) {
		// look in classpath manager hooks first
		ClassLoadingHook[] loaderHooks = sourcedata.getAdaptor().getHookRegistry().getClassLoadingHooks();
		boolean hookAdded = false;
		for (int i = 0; i < loaderHooks.length; i++)
			hookAdded |= loaderHooks[i].addClassPathEntry(result, cp, hostloader, sourcedata, sourcedomain);
		if (!addClassPathEntry(result, cp, hostloader, sourcedata, sourcedomain) && !hookAdded) {
			BundleException be = new BundleException(NLS.bind(AdaptorMsg.BUNDLE_CLASSPATH_ENTRY_NOT_FOUND_EXCEPTION, cp, sourcedata.getLocation()));
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
	public static boolean addClassPathEntry(ArrayList result, String cp, ClasspathManager hostloader, BaseData sourcedata, ProtectionDomain sourcedomain) {
		if (cp.equals(".")) { //$NON-NLS-1$
			result.add(hostloader.createClassPathEntry(sourcedata.getBundleFile(), sourcedomain));
			return true;
		}
		Object element = hostloader.getClasspath(cp, sourcedata, sourcedomain);
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
			bundlefile = new NestedDirBundleFile(sourcedata.getBundleFile(), cp);
		// check for internal library jars
		else if ((file = sourcedata.getBundleFile().getFile(cp, false)) != null)
			bundlefile = createBundleFile(file, sourcedata);
		if (bundlefile != null)
			return createClassPathEntry(bundlefile, sourcedomain);
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
			return createClassPathEntry(bundlefile, sourcedomain);
		return null;
	}

	private static BundleFile createBundleFile(File file, BaseData sourcedata) {
		if (file == null || !file.exists())
			return null;
		try {
			return sourcedata.getAdaptor().createBundleFile(file, sourcedata);
		} catch (IOException e) {
			sourcedata.getAdaptor().getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, sourcedata.getBundle(), e);
		}
		return null;
	}

	private ClasspathEntry createClassPathEntry(BundleFile bundlefile, ProtectionDomain cpDomain) {
		return classloader.createClassPathEntry(bundlefile, cpDomain);
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
			result = findLocalResourceImpl(resource);
			return result;
		} finally {
			for (int i = 0; i < hooks.length; i++)
				hooks[i].postFindLocalResource(resource, result, this);
		}
	}

	private URL findLocalResourceImpl(String resource) {
		URL result = null;
		for (int i = 0; i < entries.length; i++) {
			if (entries[i] != null) {
				result = findResourceImpl(resource, entries[i].getBundleFile());
				if (result != null)
					return result;
			}
		}
		// look in fragments
		for (int i = 0; i < fragments.length; i++) {
			ClasspathEntry[] fragEntries = fragments[i].getEntries();
			for (int j = 0; j < fragEntries.length; j++) {
				result = findResourceImpl(resource, fragEntries[j].getBundleFile());
				if (result != null)
					return result;
			}
		}
		return null;
	}

	/**
	 * Finds the local resources by searching the ClasspathEntry objects of the classpath manager.
	 * @param resource the requested resource name.
	 * @return an enumeration of the the requested resources or null if the resources do not exist
	 */
	public Enumeration findLocalResources(String resource) {
		Vector resources = new Vector(6); // use a Vector instead of ArrayList because we need an enumeration
		for (int i = 0; i < entries.length; i++) {
			if (entries[i] != null) {
				URL url = findResourceImpl(resource, entries[i].getBundleFile(), resources.size());
				if (url != null)
					resources.addElement(url);
			}
		}
		// look in fragments
		for (int i = 0; i < fragments.length; i++) {
			ClasspathEntry[] fragEntries = fragments[i].getEntries();
			for (int j = 0; j < fragEntries.length; j++) {
				URL url = findResourceImpl(resource, fragEntries[j].getBundleFile(), resources.size());
				if (url != null)
					resources.addElement(url);
			}
		}
		if (resources.size() > 0)
			return resources.elements();
		return null;
	}

	private URL findResourceImpl(String name, BundleFile bundlefile) {
		return findResourceImpl(name, bundlefile, 0);
	}

	private URL findResourceImpl(String name, BundleFile bundlefile, int index) {
		return bundlefile.getResourceURL(name, data.getBundleID(), index);
	}

	/**
	 * Finds a local entry by searching the ClasspathEntry objects of the classpath manager.
	 * @param path the requested entry path.
	 * @return the requested entry or null if the entry does not exist
	 */
	public BundleEntry findLocalEntry(String path) {
		BundleEntry result = null;
		for (int i = 0; i < entries.length; i++) {
			if (entries[i] != null) {
				result = findEntryImpl(path, entries[i].getBundleFile());
				if (result != null)
					return result;
			}
		}
		// look in fragments
		for (int i = 0; i < fragments.length; i++) {
			ClasspathEntry[] fragEntries = fragments[i].getEntries();
			for (int j = 0; j < fragEntries.length; j++) {
				result = findEntryImpl(path, fragEntries[j].getBundleFile());
				if (result != null)
					return result;
			}
		}
		return null;
	}

	/**
	 * Finds the local entries by searching the ClasspathEntry objects of the classpath manager.
	 * @param path the requested entry path.
	 * @return an enumeration of the the requested entries or null if the entries do not exist
	 */
	public Enumeration findLocalEntries(String path) {
		Vector objects = new Vector(6); // use a Vector instead of ArrayList because we need an enumeration
		for (int i = 0; i < entries.length; i++) {
			if (entries[i] != null) {
				BundleEntry result = findEntryImpl(path, entries[i].getBundleFile());
				if (result != null)
					objects.addElement(result);
			}
		}
		// look in fragments
		for (int i = 0; i < fragments.length; i++) {
			ClasspathEntry[] fragEntries = fragments[i].getEntries();
			for (int j = 0; j < fragEntries.length; j++) {
				BundleEntry result = findEntryImpl(path, fragEntries[j].getBundleFile());
				if (result != null)
					objects.addElement(result);
			}
		}
		if (objects.size() > 0)
			return objects.elements();
		return null;
	}

	private BundleEntry findEntryImpl(String path, BundleFile bundleFile) {
		return bundleFile.getEntry(path);
	}

	/**
	 * Finds a local class by searching the ClasspathEntry objects of the classpath manager.
	 * This method will first call all the configured class loading stats hooks 
	 * {@link ClassLoadingStatsHook#preFindLocalClass(String, ClasspathManager)} methods.  Then it 
	 * will search for the class.  If a class is found then all configured class loading hooks
	 * {@link ClassLoadingHook#processClass(String, byte[], ClasspathEntry, BundleEntry, ClasspathManager)}
	 * methods will be called.  Finally all the configured class loading stats hooks
	 * {@link ClassLoadingStatsHook#postFindLocalClass(String, Class, ClasspathManager)} methods are called.
	 * @param classname the requested class name.
	 * @return the requested class
	 * @throws ClassNotFoundException if the class does not exist
	 */
	public Class findLocalClass(String classname) throws ClassNotFoundException {
		Class result = null;
		ClassLoadingStatsHook[] hooks = data.getAdaptor().getHookRegistry().getClassLoadingStatsHooks();
		try {
			for (int i = 0; i < hooks.length; i++)
				hooks[i].preFindLocalClass(classname, this);
			result = findLocalClassImpl(classname, hooks);
			return result;
		} finally {
			for (int i = 0; i < hooks.length; i++)
				hooks[i].postFindLocalClass(classname, result, this);
		}
	}

	private Class findLocalClassImpl(String classname, ClassLoadingStatsHook[] hooks) throws ClassNotFoundException {
		// must call findLoadedClass here even if it was called earlier,
		// the findLoadedClass and defineClass calls must be atomic
		synchronized (classloader) {
			Class result = classloader.publicFindLoaded(classname);
			if (result != null)
				return result;
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
		}
		throw new ClassNotFoundException(classname);
	}

	private Class findClassImpl(String name, ClasspathEntry classpathEntry, ClassLoadingStatsHook[] hooks) {
		if (Debug.DEBUG && Debug.DEBUG_LOADER)
			Debug.println("BundleClassLoader[" + data + "].findClass(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
		String filename = name.replace('.', '/').concat(".class"); //$NON-NLS-1$
		BundleEntry entry = classpathEntry.getBundleFile().getEntry(filename);
		if (entry == null)
			return null;

		byte[] classbytes;
		try {
			classbytes = entry.getBytes();
		} catch (IOException e) {
			if (Debug.DEBUG && Debug.DEBUG_LOADER)
				Debug.println("  IOException reading " + filename + " from " + data); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}

		if (Debug.DEBUG && Debug.DEBUG_LOADER) {
			Debug.println("  read " + classbytes.length + " bytes from " + filename); //$NON-NLS-1$ //$NON-NLS-2$
			Debug.println("  defining class " + name); //$NON-NLS-1$
		}

		try {
			Class result = defineClass(name, classbytes, classpathEntry, entry);
			for (int i = 0; i < hooks.length; i++)
				hooks[i].recordClassDefine(name, result, classbytes, classpathEntry, entry, this);
			return result;
		} catch (Error e) {
			if (Debug.DEBUG && Debug.DEBUG_LOADER)
				Debug.println("  error defining class " + name); //$NON-NLS-1$
			throw e;
		}
	}

	/**
	 * Defines the specified class.  This method will first call all the configured class loading hooks 
	 * {@link ClassLoadingHook#processClass(String, byte[], ClasspathEntry, BundleEntry, ClasspathManager)} 
	 * methods.  Then it will call the {@link BaseClassLoader#defineClass(String, byte[], ClasspathEntry, BundleEntry)}
	 * method to define the class.
	 * @param name the name of the class to define
	 * @param classbytes the class bytes
	 * @param classpathEntry the classpath entry used to load the class bytes
	 * @param entry the BundleEntry used to load the class bytes
	 * @return the defined class
	 */
	private Class defineClass(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry) {
		ClassLoadingHook[] hooks = data.getAdaptor().getHookRegistry().getClassLoadingHooks();
		byte[] modifiedBytes = classbytes;
		for (int i = 0; i < hooks.length; i++) {
			modifiedBytes = hooks[i].processClass(name, classbytes, classpathEntry, entry, this);
			if (modifiedBytes != null)
				classbytes = modifiedBytes;
		}
		return classloader.defineClass(name, classbytes, classpathEntry, entry);
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

}
