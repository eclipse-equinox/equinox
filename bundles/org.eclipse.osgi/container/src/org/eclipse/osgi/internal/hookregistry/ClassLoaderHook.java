/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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

package org.eclipse.osgi.internal.hookregistry;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.internal.loader.classpath.ClasspathEntry;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;

/**
 * A class loading hook that hooks into a module class loader
 */
public abstract class ClassLoaderHook {
	/**
	 * Gets called by a classpath manager before defining a class. This method
	 * allows a class loading hook to process the bytes of a class that is about to
	 * be defined and return a transformed byte array.
	 * 
	 * @param name           the name of the class being defined
	 * @param classbytes     the bytes of the class being defined
	 * @param classpathEntry the ClasspathEntry where the class bytes have been read
	 *                       from.
	 * @param entry          the BundleEntry source of the class bytes
	 * @param manager        the class path manager used to define the requested
	 *                       class
	 * @return a transformed array of classbytes or null if the original bytes
	 *         should be used.
	 */
	public byte[] processClass(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry,
			ClasspathManager manager) {
		return null;
	}

	/**
	 * Gets called by a classpath manager before defining a class. This method
	 * allows a class loading hook to reject a transformation to the class bytes by
	 * a
	 * {@link #processClass(String, byte[], ClasspathEntry, BundleEntry, ClasspathManager)
	 * processClass} method.
	 * 
	 * @param name             the name of the class being defined
	 * @param transformedBytes the transformed bytes of the class being defined
	 * @param classpathEntry   the ClasspathEntry where the class bytes have been
	 *                         read from
	 * @param entry            the BundleEntry source of the class bytes
	 * @param manager          the class path manager used to define the requested
	 *                         class
	 * @return returns true if the modified bytes should be rejected; otherwise
	 *         false is returned
	 */
	public boolean rejectTransformation(String name, byte[] transformedBytes, ClasspathEntry classpathEntry,
			BundleEntry entry, ClasspathManager manager) {
		return false;
	}

	/**
	 * Gets called by a classpath manager when looking for ClasspathEntry objects.
	 * This method allows a class loading hook to add additional ClasspathEntry
	 * objects
	 * 
	 * @param cpEntries        the list of ClasspathEntry objects currently
	 *                         available for the requested classpath
	 * @param cp               the name of the requested classpath
	 * @param hostmanager      the classpath manager the requested ClasspathEntry is
	 *                         for
	 * @param sourceGeneration the source generation of the requested ClasspathEntry
	 * @return true if a ClasspathEntry has been added to cpEntries
	 */
	public boolean addClassPathEntry(ArrayList<ClasspathEntry> cpEntries, String cp, ClasspathManager hostmanager,
			Generation sourceGeneration) {
		return false;
	}

	/**
	 * Gets called by a base data during
	 * {@link ModuleClassLoader#findLibrary(String)}. A this method is called for
	 * each configured class loading hook until one class loading hook returns a
	 * non-null value. If no class loading hook returns a non-null value then the
	 * default behavior will be used.
	 * 
	 * @param generation the bundle generation to find a native library for.
	 * @param libName    the name of the native library.
	 * @return The absolute path name of the native library or null.
	 */
	public String findLocalLibrary(Generation generation, String libName) {
		return null;
	}

	/**
	 * Gets called by a bundle loader when {@link BundleLoader#getClassLoader()} is
	 * called the first time in order to allow a hook to create the class loader.
	 * This should rarely, if ever be overridden. The default implementation returns
	 * null indicating the built-in implementation should be used. Only one hook is
	 * able to provide the implementation of the module class loader and the first
	 * one to return non-null wins.
	 *
	 * @param parent        the parent classloader
	 * @param configuration the equinox configuration
	 * @param delegate      the delegate for this classloader
	 * @param generation    the generation for this class loader
	 * @return returns an implementation of a module class loader or
	 *         <code>null</code> if the built-in implemention is to be used.
	 */
	public ModuleClassLoader createClassLoader(ClassLoader parent, EquinoxConfiguration configuration,
			BundleLoader delegate, Generation generation) {
		// do nothing
		return null;
	}

	/**
	 * Gets called by a classpath manager at the end of
	 * {@link BundleLoader#getClassLoader()} is called the first time and a class
	 * loader is created.
	 * 
	 * @param classLoader the newly created bundle classloader
	 */
	public void classLoaderCreated(ModuleClassLoader classLoader) {
		// do nothing
	}

	/**
	 * Called by a {@link BundleLoader#findClass(String)} method before delegating
	 * to the resolved constraints and local bundle for a class load. If this method
	 * returns null then normal delegation is done. If this method returns a
	 * non-null value then the rest of the delegation process is skipped and the
	 * returned value is used. If this method throws a
	 * <code>ClassNotFoundException</code> then the calling
	 * {@link BundleLoader#findClass(String)} method re-throws the exception.
	 * 
	 * @param name        the name of the class to find
	 * @param classLoader the module class loader
	 * @return the class found by this hook or null if normal delegation should
	 *         continue
	 * @throws ClassNotFoundException to terminate the delegation and throw an
	 *                                exception
	 */
	public Class<?> preFindClass(String name, ModuleClassLoader classLoader) throws ClassNotFoundException {
		return null;
	}

	/**
	 * Called by a {@link BundleLoader#findClass(String)} method after delegating to
	 * the resolved constraints and local bundle for a class load. This method will
	 * only be called if no class was found from the normal delegation.
	 * 
	 * @param name        the name of the class to find
	 * @param classLoader the bundle class loader
	 * @return the class found by this hook or null if normal delegation should
	 *         continue
	 * @throws ClassNotFoundException to terminate the delegation and throw an
	 *                                exception
	 */
	public Class<?> postFindClass(String name, ModuleClassLoader classLoader) throws ClassNotFoundException {
		return null;
	}

	/**
	 * Called by a {@link BundleLoader#findResource(String)} before delegating to
	 * the resolved constraints and local bundle for a resource load. If this method
	 * returns null then normal delegation is done. If this method returns a
	 * non-null value then the rest of the delegation process is skipped and the
	 * returned value is used. If this method throws an
	 * <code>FileNotFoundException</code> then the delegation is terminated.
	 * 
	 * @param name        the name of the resource to find
	 * @param classLoader the bundle class loader
	 * @return the resource found by this hook or null if normal delegation should
	 *         continue
	 * @throws FileNotFoundException to terminate the delegation
	 */
	public URL preFindResource(String name, ModuleClassLoader classLoader) throws FileNotFoundException {
		return null;
	}

	/**
	 * Called by a {@link BundleLoader#findResource(String)} after delegating to the
	 * resolved constraints and local bundle for a resource load. This method will
	 * only be called if no resource was found from the normal delegation.
	 * 
	 * @param name        the name of the resource to find
	 * @param classLoader the bundle class loader
	 * @return the resource found by this hook or null if normal delegation should
	 *         continue
	 * @throws FileNotFoundException to terminate the delegation
	 */
	public URL postFindResource(String name, ModuleClassLoader classLoader) throws FileNotFoundException {
		return null;
	}

	/**
	 * Called by a {@link BundleLoader#findResources(String)} before delegating to
	 * the resolved constraints and local bundle for a resource load. If this method
	 * returns null then normal delegation is done. If this method returns a
	 * non-null value then the rest of the delegation process is skipped and the
	 * returned value is used. If this method throws an
	 * <code>FileNotFoundException</code> then the delegation is terminated
	 * 
	 * @param name        the name of the resource to find
	 * @param classLoader the bundle class loader
	 * @return the resources found by this hook or null if normal delegation should
	 *         continue
	 * @throws FileNotFoundException to terminate the delegation
	 */
	public Enumeration<URL> preFindResources(String name, ModuleClassLoader classLoader) throws FileNotFoundException {
		return null;
	}

	/**
	 * Called by a {@link BundleLoader#findResources(String)} after delegating to
	 * the resolved constraints and local bundle for a resource load. This method
	 * will only be called if no resources were found from the normal delegation.
	 * 
	 * @param name        the name of the resource to find
	 * @param classLoader the bundle class loader
	 * @return the resources found by this hook or null if normal delegation should
	 *         continue
	 * @throws FileNotFoundException to terminate the delegation
	 */
	public Enumeration<URL> postFindResources(String name, ModuleClassLoader classLoader) throws FileNotFoundException {
		return null;
	}

	/**
	 * Called by a {@link ClasspathManager} before normal delegation. If this method
	 * returns a non-null value then the rest of the delegation process is skipped
	 * and the returned value is used.
	 * 
	 * @param name        the name of the library to find
	 * @param classLoader the bundle class loader
	 * @return the library found by this hook or null if normal delegation should
	 *         continue
	 * @throws FileNotFoundException to terminate the delegation
	 */
	public String preFindLibrary(String name, ModuleClassLoader classLoader) throws FileNotFoundException {
		return null;
	}

	/**
	 * Called by a {@link ClasspathManager} after normal delegation. This method
	 * will only be called if no library was found from the normal delegation.
	 * 
	 * @param name        the name of the library to find
	 * @param classLoader the bundle class loader
	 * @return the library found by this hook or null if normal delegation should
	 *         continue
	 */
	public String postFindLibrary(String name, ModuleClassLoader classLoader) {
		return null;
	}

	/**
	 * Gets called by a classpath manager during
	 * {@link ClasspathManager#findLocalClass(String)} before searching the local
	 * classloader for a class. A classpath manager will call this method for each
	 * configured class loading hook.
	 * 
	 * @param name    the name of the requested class
	 * @param manager the classpath manager used to find and load the requested
	 *                class
	 * @throws ClassNotFoundException to prevent the requested class from loading
	 */
	public void preFindLocalClass(String name, ClasspathManager manager) throws ClassNotFoundException {
		// do nothing
	}

	/**
	 * Gets called by a classpath manager during
	 * {@link ClasspathManager#findLocalClass(String)} after searching the local
	 * classloader for a class. A classpath manager will call this method for each
	 * configured class loading hook.
	 * 
	 * @param name    the name of the requested class
	 * @param clazz   the loaded class or null if not found
	 * @param manager the classpath manager used to find and load the requested
	 *                class
	 * @throws ClassNotFoundException to prevent the requested class from loading.
	 *                                This is highly discouraged because if the
	 *                                class is non-null it is already too late to
	 *                                throw an exception.
	 */
	public void postFindLocalClass(String name, Class<?> clazz, ClasspathManager manager)
			throws ClassNotFoundException {
		// do nothing
	}

	/**
	 * Gets called by a classpath manager during
	 * {@link ClasspathManager#findLocalResource(String)} before searching the local
	 * classloader for a resource. A classpath manager will call this method for
	 * each configured class loading hook.
	 * 
	 * @param name    the name of the requested resource
	 * @param manager the classpath manager used to find the requested resource
	 * @throws NoSuchElementException will prevent the local resource from loading
	 */
	public void preFindLocalResource(String name, ClasspathManager manager) {
		// do nothing
	}

	/**
	 * Gets called by a classpath manager during
	 * {@link ClasspathManager#findLocalResource(String)} after searching the local
	 * classloader for a resource. A classpath manager will call this method for
	 * each configured class loading hook.
	 * 
	 * @param name     the name of the requested resource
	 * @param resource the URL to the requested resource or null if not found
	 * @param manager  the classpath manager used to find the requested resource
	 * @throws NoSuchElementException will prevent the local resource from loading
	 */
	public void postFindLocalResource(String name, URL resource, ClasspathManager manager) {
		// do nothing
	}

	/**
	 * Gets called by a classpath manager after an attempt is made to define a
	 * class. This method allows a class loading hook to record data about a class
	 * definition.
	 * 
	 * @param name           the name of the class that got defined
	 * @param clazz          the class object that got defined or null if an error
	 *                       occurred while defining a class
	 * @param classbytes     the class bytes used to define the class
	 * @param classpathEntry the ClasspathEntry where the class bytes got read from
	 * @param entry          the BundleEntyr source of the class bytes
	 * @param manager        the classpath manager used to define the class
	 */
	public void recordClassDefine(String name, Class<?> clazz, byte[] classbytes, ClasspathEntry classpathEntry,
			BundleEntry entry, ClasspathManager manager) {
		// do nothing
	}

	/**
	 * Returns the parent class loader to be used by all ModuleClassLoaders. A
	 * {@code null} value may be returned if this hook does not supply the parent.
	 * Only one hook is able to provide the implementation of the parent class
	 * loader and the first one to return non-null wins.
	 * 
	 * @param configuration the equinox configuration
	 * @return the parent class loader to be used by all ModuleClassLoaders
	 */
	public ClassLoader getModuleClassLoaderParent(EquinoxConfiguration configuration) {
		// do nothing by default
		return null;
	}

	/**
	 * Returns true if this hook can support invoking
	 * {@link ClassLoaderHook#processClass(String, byte[], ClasspathEntry, BundleEntry, ClasspathManager)
	 * processClass} recursively for the same class name. If false is returned then
	 * a class loading error will occur if recursive class processing is detected.
	 * <p>
	 * This method must return a constant boolean value.
	 * 
	 * @return true if recursing class processing is supported
	 */
	public boolean isProcessClassRecursionSupported() {
		return false;
	}

	/**
	 * Returns the filtered list of ClasspathEntry instances for the given class,
	 * resource or entry. A {@code null} value may be returned in which case the
	 * find process will go over all the host and fragment entries in order to find
	 * the given entity which is the default behavior. Any non-null return value
	 * including an empty list will only look at the entries in the returned list.
	 *
	 * This method is used within
	 * {@link ClasspathManager#findLocalResource(String)},
	 * {@link ClasspathManager#findLocalResources(String)},
	 * {@link ClasspathManager#findLocalClass(String) } and
	 * {@link ClasspathManager#findLocalEntry(String) }
	 *
	 * @param name    the name of the requested class, resource or entry
	 * @param manager the classpath manager used to find the requested class,
	 *                resource or entry
	 * @return the array of ClassPathEntry objects to use to load this given entity
	 */
	public ClasspathEntry[] getClassPathEntries(String name, ClasspathManager manager) {
		// do nothing by default
		return null;
	}
}
