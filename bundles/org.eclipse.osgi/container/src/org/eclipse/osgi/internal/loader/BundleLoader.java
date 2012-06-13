/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.loader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.util.KeyedElement;
import org.eclipse.osgi.framework.util.KeyedHashSet;
import org.eclipse.osgi.internal.loader.buddy.PolicyHandler;
import org.eclipse.osgi.internal.resolver.StateBuilder;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;
import org.osgi.framework.wiring.BundleWiring;

/**
 * This object is responsible for all classloader delegation for a bundle.
 * It represents the loaded state of the bundle.  BundleLoader objects
 * are created lazily; care should be taken not to force the creation
 * of a BundleLoader unless it is necessary.
 * @see org.eclipse.osgi.internal.loader.BundleLoaderProxy
 */
public class BundleLoader implements ClassLoaderDelegate {
	public final static String DEFAULT_PACKAGE = "."; //$NON-NLS-1$
	public final static String JAVA_PACKAGE = "java."; //$NON-NLS-1$
	public final static byte FLAG_IMPORTSINIT = 0x01;
	public final static byte FLAG_HASDYNAMICIMPORTS = 0x02;
	public final static byte FLAG_HASDYNAMICEIMPORTALL = 0x04;
	public final static byte FLAG_CLOSED = 0x08;
	public final static byte FLAG_LAZYTRIGGER = 0x10;

	public final static ClassContext CLASS_CONTEXT = AccessController.doPrivileged(new PrivilegedAction<ClassContext>() {
		public ClassContext run() {
			return new ClassContext();
		}
	});
	public final static ClassLoader FW_CLASSLOADER = getClassLoader(Framework.class);

	private static final int PRE_CLASS = 1;
	private static final int POST_CLASS = 2;
	private static final int PRE_RESOURCE = 3;
	private static final int POST_RESOURCE = 4;
	private static final int PRE_RESOURCES = 5;
	private static final int POST_RESOURCES = 6;
	private static final int PRE_LIBRARY = 7;
	private static final int POST_LIBRARY = 8;

	/* the proxy */
	final private BundleLoaderProxy proxy;
	/* Bundle object */
	final BundleHost bundle;
	final private PolicyHandler policy;
	/* List of package names that are exported by this BundleLoader */
	final private Collection<String> exportedPackages;
	final private Collection<String> substitutedPackages;
	/* List of required bundle BundleLoaderProxy objects */
	final BundleLoaderProxy[] requiredBundles;
	/* List of indexes into the requiredBundles list of reexported bundles */
	final int[] reexportTable;
	/* cache of required package sources. Key is packagename, value is PackageSource */
	final private KeyedHashSet requiredSources;

	// note that the following non-final must be access using synchronization
	/* cache of imported packages. Key is packagename, Value is PackageSource */
	private KeyedHashSet importedSources;
	/* If not null, list of package stems to import dynamically. */
	private String[] dynamicImportPackageStems;
	/* If not null, list of package names to import dynamically. */
	private String[] dynamicImportPackages;
	/* loader flags */
	private byte loaderFlags = 0;
	/* The is the BundleClassLoader for the bundle */
	private BundleClassLoader classloader;
	private ClassLoader parent;

	/**
	 * Returns the package name from the specified class name.
	 * The returned package is dot seperated.
	 *
	 * @param name   Name of a class.
	 * @return Dot separated package name or null if the class
	 *         has no package name.
	 */
	public final static String getPackageName(String name) {
		if (name != null) {
			int index = name.lastIndexOf('.'); /* find last period in class name */
			if (index > 0)
				return name.substring(0, index);
		}
		return DEFAULT_PACKAGE;
	}

	/**
	 * Returns the package name from the specified resource name.
	 * The returned package is dot seperated.
	 *
	 * @param name   Name of a resource.
	 * @return Dot separated package name or null if the resource
	 *         has no package name.
	 */
	public final static String getResourcePackageName(String name) {
		if (name != null) {
			/* check for leading slash*/
			int begin = ((name.length() > 1) && (name.charAt(0) == '/')) ? 1 : 0;
			int end = name.lastIndexOf('/'); /* index of last slash */
			if (end > begin)
				return name.substring(begin, end).replace('/', '.');
		}
		return DEFAULT_PACKAGE;
	}

	/**
	 * BundleLoader runtime constructor. This object is created lazily
	 * when the first request for a resource is made to this bundle.
	 *
	 * @param bundle Bundle object for this loader.
	 * @param proxy the BundleLoaderProxy for this loader.
	 * @exception org.osgi.framework.BundleException
	 */
	protected BundleLoader(BundleHost bundle, BundleLoaderProxy proxy) throws BundleException {
		this.bundle = bundle;
		this.proxy = proxy;
		try {
			bundle.getBundleData().open(); /* make sure the BundleData is open */
		} catch (IOException e) {
			throw new BundleException(Msg.BUNDLE_READ_EXCEPTION, e);
		}
		BundleDescription description = proxy.getBundleDescription();
		// init the require bundles list.
		BundleDescription[] required = description.getResolvedRequires();
		if (required.length > 0) {
			// get a list of re-exported symbolic names
			Set<String> reExportSet = new HashSet<String>(required.length);
			BundleSpecification[] requiredSpecs = description.getRequiredBundles();
			if (requiredSpecs != null && requiredSpecs.length > 0)
				for (int i = 0; i < requiredSpecs.length; i++)
					if (requiredSpecs[i].isExported())
						reExportSet.add(requiredSpecs[i].getName());

			requiredBundles = new BundleLoaderProxy[required.length];
			int[] reexported = new int[required.length];
			int reexportIndex = 0;
			for (int i = 0; i < required.length; i++) {
				requiredBundles[i] = getLoaderProxy(required[i]);
				if (reExportSet.contains(required[i].getSymbolicName()))
					reexported[reexportIndex++] = i;
			}
			if (reexportIndex > 0) {
				reexportTable = new int[reexportIndex];
				System.arraycopy(reexported, 0, reexportTable, 0, reexportIndex);
			} else {
				reexportTable = null;
			}
			requiredSources = new KeyedHashSet(10, false);
		} else {
			requiredBundles = null;
			reexportTable = null;
			requiredSources = null;
		}

		// init the provided packages set
		ExportPackageDescription[] exports = description.getSelectedExports();
		if (exports != null && exports.length > 0) {
			exportedPackages = Collections.synchronizedCollection(exports.length > 10 ? new HashSet<String>(exports.length) : new ArrayList<String>(exports.length));
			initializeExports(exports, exportedPackages);
		} else {
			exportedPackages = Collections.synchronizedCollection(new ArrayList<String>(0));
		}

		ExportPackageDescription substituted[] = description.getSubstitutedExports();
		if (substituted.length > 0) {
			substitutedPackages = substituted.length > 10 ? new HashSet<String>(substituted.length) : new ArrayList<String>(substituted.length);
			for (int i = 0; i < substituted.length; i++)
				substitutedPackages.add(substituted[i].getName());
		} else {
			substitutedPackages = null;
		}

		//This is the fastest way to access to the description for fragments since the hostdescription.getFragments() is slow
		BundleFragment[] fragmentObjects = bundle.getFragments();
		BundleDescription[] fragments = new BundleDescription[fragmentObjects == null ? 0 : fragmentObjects.length];
		for (int i = 0; i < fragments.length; i++)
			fragments[i] = fragmentObjects[i].getBundleDescription();
		// init the dynamic imports tables
		if (description.hasDynamicImports())
			addDynamicImportPackage(description.getImportPackages());
		// ...and its fragments
		for (int i = 0; i < fragments.length; i++)
			if (fragments[i].isResolved() && fragments[i].hasDynamicImports())
				addDynamicImportPackage(fragments[i].getImportPackages());

		//Initialize the policy handler
		String buddyList = null;
		try {
			buddyList = bundle.getBundleData().getManifest().get(Constants.BUDDY_LOADER);
		} catch (BundleException e) {
			// do nothing; buddyList == null
		}
		policy = buddyList != null ? new PolicyHandler(this, buddyList, bundle.getFramework().getPackageAdmin()) : null;
		if (policy != null)
			policy.open(bundle.getFramework().getSystemBundleContext());
	}

	private void initializeExports(ExportPackageDescription[] exports, Collection<String> exportNames) {
		for (int i = 0; i < exports.length; i++) {
			if (proxy.forceSourceCreation(exports[i])) {
				if (!exportNames.contains(exports[i].getName())) {
					// must force filtered and reexport sources to be created early
					// to prevent lazy normal package source creation.
					// We only do this for the first export of a package name. 
					proxy.createPackageSource(exports[i], true);
				}
			}
			exportNames.add(exports[i].getName());
		}
	}

	public synchronized KeyedHashSet getImportedSources(KeyedHashSet visited) {
		if ((loaderFlags & FLAG_IMPORTSINIT) != 0)
			return importedSources;
		BundleDescription bundleDesc = proxy.getBundleDescription();
		ExportPackageDescription[] packages = bundleDesc.getResolvedImports();
		if (packages != null && packages.length > 0) {
			if (importedSources == null)
				importedSources = new KeyedHashSet(packages.length, false);
			for (int i = 0; i < packages.length; i++) {
				if (packages[i].getExporter() == bundleDesc)
					continue; // ignore imports resolved to this bundle
				PackageSource source = createExportPackageSource(packages[i], visited);
				if (source != null)
					importedSources.add(source);
			}
		}
		loaderFlags |= FLAG_IMPORTSINIT;
		return importedSources;
	}

	public synchronized boolean isLazyTriggerSet() {
		return (loaderFlags & FLAG_LAZYTRIGGER) != 0;
	}

	public void setLazyTrigger() throws BundleException {
		synchronized (this) {
			loaderFlags |= FLAG_LAZYTRIGGER;
		}
		BundleLoaderProxy.secureAction.start(bundle, Bundle.START_TRANSIENT | BundleHost.LAZY_TRIGGER);
	}

	final PackageSource createExportPackageSource(ExportPackageDescription export, KeyedHashSet visited) {
		BundleLoaderProxy exportProxy = getLoaderProxy(export.getExporter());
		if (exportProxy == null)
			// TODO log error!!
			return null;
		PackageSource requiredSource = exportProxy.getBundleLoader().findRequiredSource(export.getName(), visited);
		PackageSource exportSource = exportProxy.createPackageSource(export, false);
		if (requiredSource == null)
			return exportSource;
		return createMultiSource(export.getName(), new PackageSource[] {requiredSource, exportSource});
	}

	private static PackageSource createMultiSource(String packageName, PackageSource[] sources) {
		if (sources.length == 1)
			return sources[0];
		List<SingleSourcePackage> sourceList = new ArrayList<SingleSourcePackage>(sources.length);
		for (int i = 0; i < sources.length; i++) {
			SingleSourcePackage[] innerSources = sources[i].getSuppliers();
			for (int j = 0; j < innerSources.length; j++)
				if (!sourceList.contains(innerSources[j]))
					sourceList.add(innerSources[j]);
		}
		return new MultiSourcePackage(packageName, sourceList.toArray(new SingleSourcePackage[sourceList.size()]));
	}

	/*
	 * get the loader proxy for a bundle description
	 */
	public final BundleLoaderProxy getLoaderProxy(BundleDescription source) {
		Object userObject = source.getUserObject();
		if (!(userObject instanceof BundleLoaderProxy)) {
			// may need to force the proxy to be created
			long exportingID = source.getBundleId();
			BundleHost exportingBundle = (BundleHost) bundle.getFramework().getBundle(exportingID);
			if (exportingBundle == null)
				return null;
			userObject = exportingBundle.getLoaderProxy();
		}
		return (BundleLoaderProxy) userObject;
	}

	public BundleLoaderProxy getLoaderProxy() {
		return proxy;
	}

	/*
	 * Close the the BundleLoader.
	 *
	 */
	synchronized void close() {
		if ((loaderFlags & FLAG_CLOSED) != 0)
			return;
		if (classloader != null)
			classloader.close();
		if (policy != null)
			policy.close(bundle.getFramework().getSystemBundleContext());
		loaderFlags |= FLAG_CLOSED; /* This indicates the BundleLoader is destroyed */
	}

	/**
	 * This method loads a class from the bundle.  The class is searched for in the
	 * same manner as it would if it was being loaded from a bundle (i.e. all
	 * hosts, fragments, import, required bundles and local resources are searched.
	 *
	 * @param      name     the name of the desired Class.
	 * @return     the resulting Class
	 * @exception  java.lang.ClassNotFoundException  if the class definition was not found.
	 */
	final public Class<?> loadClass(String name) throws ClassNotFoundException {
		BundleClassLoader bcl = createClassLoader();
		// The instanceof check here is just to be safe.  The javadoc contract stated in BundleClassLoader
		// mandate that BundleClassLoaders be an instance of ClassLoader.
		if (name.length() > 0 && name.charAt(0) == '[' && bcl instanceof ClassLoader)
			return Class.forName(name, false, (ClassLoader) bcl);
		return bcl.loadClass(name);
	}

	/**
	 * This method gets a resource from the bundle.  The resource is searched 
	 * for in the same manner as it would if it was being loaded from a bundle 
	 * (i.e. all hosts, fragments, import, required bundles and 
	 * local resources are searched).
	 *
	 * @param name the name of the desired resource.
	 * @return the resulting resource URL or null if it does not exist.
	 */
	final URL getResource(String name) {
		return createClassLoader().getResource(name);
	}

	public final synchronized ClassLoader getParentClassLoader() {
		if (parent != null)
			return parent;
		createClassLoader();
		return parent;
	}

	final public synchronized BundleClassLoader createClassLoader() {
		if (classloader != null)
			return classloader;
		String[] classpath;
		try {
			classpath = bundle.getBundleData().getClassPath();
		} catch (BundleException e) {
			// no classpath
			classpath = new String[0];
			bundle.getFramework().publishFrameworkEvent(FrameworkEvent.ERROR, bundle, e);
		}
		if (classpath == null) {
			// no classpath
			classpath = new String[0];
			bundle.getFramework().publishFrameworkEvent(FrameworkEvent.ERROR, bundle, new BundleException(Msg.BUNDLE_NO_CLASSPATH_MATCH, BundleException.MANIFEST_ERROR));
		}
		BundleClassLoader bcl = createBCLPrevileged(bundle.getProtectionDomain(), classpath);
		parent = getParentPrivileged(bcl);
		classloader = bcl;
		return classloader;
	}

	/**
	 * Finds a class local to this bundle.  Only the classloader for this bundle is searched.
	 * @param name The name of the class to find.
	 * @return The loaded Class or null if the class is not found.
	 * @throws ClassNotFoundException 
	 */
	Class<?> findLocalClass(String name) throws ClassNotFoundException {
		if (Debug.DEBUG_LOADER)
			Debug.println("BundleLoader[" + this + "].findLocalClass(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		try {
			Class<?> clazz = createClassLoader().findLocalClass(name);
			if (Debug.DEBUG_LOADER && clazz != null)
				Debug.println("BundleLoader[" + this + "] found local class " + name); //$NON-NLS-1$ //$NON-NLS-2$
			return clazz;
		} catch (ClassNotFoundException e) {
			if (e instanceof StatusException) {
				if ((((StatusException) e).getStatusCode() & StatusException.CODE_ERROR) != 0)
					throw e;
			}
			return null;
		}
	}

	/**
	 * Finds the class for a bundle.  This method is used for delegation by the bundle's classloader.
	 */
	public Class<?> findClass(String name) throws ClassNotFoundException {
		return findClass(name, true);
	}

	Class<?> findClass(String name, boolean checkParent) throws ClassNotFoundException {
		ClassLoader parentCL = getParentClassLoader();
		if (checkParent && parentCL != null && name.startsWith(JAVA_PACKAGE))
			// 1) if startsWith "java." delegate to parent and terminate search
			// we want to throw ClassNotFoundExceptions if a java.* class cannot be loaded from the parent.
			return parentCL.loadClass(name);
		return findClassInternal(name, checkParent, parentCL);
	}

	private Class<?> findClassInternal(String name, boolean checkParent, ClassLoader parentCL) throws ClassNotFoundException {
		if (Debug.DEBUG_LOADER)
			Debug.println("BundleLoader[" + this + "].loadBundleClass(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String pkgName = getPackageName(name);
		boolean bootDelegation = false;
		// follow the OSGi delegation model
		if (checkParent && parentCL != null && bundle.getFramework().isBootDelegationPackage(pkgName))
			// 2) if part of the bootdelegation list then delegate to parent and continue of failure
			try {
				return parentCL.loadClass(name);
			} catch (ClassNotFoundException cnfe) {
				// we want to continue
				bootDelegation = true;
			}
		Class<?> result = null;
		try {
			result = (Class<?>) searchHooks(name, PRE_CLASS);
		} catch (ClassNotFoundException e) {
			throw e;
		} catch (FileNotFoundException e) {
			// will not happen
		}
		if (result != null)
			return result;
		// 3) search the imported packages
		PackageSource source = findImportedSource(pkgName, null);
		if (source != null) {
			// 3) found import source terminate search at the source
			result = source.loadClass(name);
			if (result != null)
				return result;
			throw new ClassNotFoundException(name);
		}
		// 4) search the required bundles
		source = findRequiredSource(pkgName, null);
		if (source != null)
			// 4) attempt to load from source but continue on failure
			result = source.loadClass(name);
		// 5) search the local bundle
		if (result == null)
			result = findLocalClass(name);
		if (result != null)
			return result;
		// 6) attempt to find a dynamic import source; only do this if a required source was not found
		if (source == null) {
			source = findDynamicSource(pkgName);
			if (source != null) {
				result = source.loadClass(name);
				if (result != null)
					return result;
				// must throw CNFE if dynamic import source does not have the class
				throw new ClassNotFoundException(name);
			}
		}

		if (result == null)
			try {
				result = (Class<?>) searchHooks(name, POST_CLASS);
			} catch (ClassNotFoundException e) {
				throw e;
			} catch (FileNotFoundException e) {
				// will not happen
			}
		// do buddy policy loading
		if (result == null && policy != null)
			result = policy.doBuddyClassLoading(name);
		if (result != null)
			return result;
		// hack to support backwards compatibiility for bootdelegation
		// or last resort; do class context trick to work around VM bugs
		if (parentCL != null && !bootDelegation && ((checkParent && bundle.getFramework().compatibiltyBootDelegation) || isRequestFromVM()))
			// we don't need to continue if a CNFE is thrown here.
			try {
				return parentCL.loadClass(name);
			} catch (ClassNotFoundException e) {
				// we want to generate our own exception below
			}
		throw new ClassNotFoundException(name);
	}

	@SuppressWarnings("unchecked")
	private <E> E searchHooks(String name, int type) throws ClassNotFoundException, FileNotFoundException {
		ClassLoaderDelegateHook[] delegateHooks = bundle.getFramework().getDelegateHooks();
		if (delegateHooks == null)
			return null;
		E result = null;
		for (int i = 0; i < delegateHooks.length && result == null; i++) {
			switch (type) {
				case PRE_CLASS :
					result = (E) delegateHooks[i].preFindClass(name, createClassLoader(), bundle.getBundleData());
					break;
				case POST_CLASS :
					result = (E) delegateHooks[i].postFindClass(name, createClassLoader(), bundle.getBundleData());
					break;
				case PRE_RESOURCE :
					result = (E) delegateHooks[i].preFindResource(name, createClassLoader(), bundle.getBundleData());
					break;
				case POST_RESOURCE :
					result = (E) delegateHooks[i].postFindResource(name, createClassLoader(), bundle.getBundleData());
					break;
				case PRE_RESOURCES :
					result = (E) delegateHooks[i].preFindResources(name, createClassLoader(), bundle.getBundleData());
					break;
				case POST_RESOURCES :
					result = (E) delegateHooks[i].postFindResources(name, createClassLoader(), bundle.getBundleData());
					break;
				case PRE_LIBRARY :
					result = (E) delegateHooks[i].preFindLibrary(name, createClassLoader(), bundle.getBundleData());
					break;
				case POST_LIBRARY :
					result = (E) delegateHooks[i].postFindLibrary(name, createClassLoader(), bundle.getBundleData());
					break;
			}
		}
		return result;
	}

	private boolean isRequestFromVM() {
		if (bundle.getFramework().isBootDelegationPackage("*") || !bundle.getFramework().contextBootDelegation) //$NON-NLS-1$
			return false;
		// works around VM bugs that require all classloaders to have access to parent packages
		Class<?>[] context = CLASS_CONTEXT.getClassContext();
		if (context == null || context.length < 2)
			return false;
		// skip the first class; it is the ClassContext class
		for (int i = 1; i < context.length; i++)
			// find the first class in the context which is not BundleLoader or instanceof ClassLoader
			if (context[i] != BundleLoader.class && !ClassLoader.class.isAssignableFrom(context[i])) {
				// only find in parent if the class is not "Class" (Class#forName case) or if the class is not loaded with a BundleClassLoader
				ClassLoader cl = getClassLoader(context[i]);
				if (cl != FW_CLASSLOADER) { // extra check incase an adaptor adds another class into the stack besides an instance of ClassLoader
					if (Class.class != context[i] && !(cl instanceof BundleClassLoader))
						return true;
					break;
				}
			}
		return false;
	}

	private static ClassLoader getClassLoader(final Class<?> clazz) {
		if (System.getSecurityManager() == null)
			return clazz.getClassLoader();
		return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
			public ClassLoader run() {
				return clazz.getClassLoader();
			}
		});
	}

	/**
	 * Finds the resource for a bundle.  This method is used for delegation by the bundle's classloader.
	 */
	public URL findResource(String name) {
		return findResource(name, true);
	}

	URL findResource(String name, boolean checkParent) {
		if ((name.length() > 1) && (name.charAt(0) == '/')) /* if name has a leading slash */
			name = name.substring(1); /* remove leading slash before search */
		String pkgName = getResourcePackageName(name);
		boolean bootDelegation = false;
		ClassLoader parentCL = getParentClassLoader();
		// follow the OSGi delegation model
		// First check the parent classloader for system resources, if it is a java resource.
		if (checkParent && parentCL != null) {
			if (pkgName.startsWith(JAVA_PACKAGE))
				// 1) if startsWith "java." delegate to parent and terminate search
				// we never delegate java resource requests past the parent
				return parentCL.getResource(name);
			else if (bundle.getFramework().isBootDelegationPackage(pkgName)) {
				// 2) if part of the bootdelegation list then delegate to parent and continue of failure
				URL result = parentCL.getResource(name);
				if (result != null)
					return result;
				bootDelegation = true;
			}
		}

		URL result = null;
		try {
			result = (URL) searchHooks(name, PRE_RESOURCE);
		} catch (FileNotFoundException e) {
			return null;
		} catch (ClassNotFoundException e) {
			// will not happen
		}
		if (result != null)
			return result;
		// 3) search the imported packages
		PackageSource source = findImportedSource(pkgName, null);
		if (source != null)
			// 3) found import source terminate search at the source
			return source.getResource(name);
		// 4) search the required bundles
		source = findRequiredSource(pkgName, null);
		if (source != null)
			// 4) attempt to load from source but continue on failure
			result = source.getResource(name);
		// 5) search the local bundle
		if (result == null)
			result = findLocalResource(name);
		if (result != null)
			return result;
		// 6) attempt to find a dynamic import source; only do this if a required source was not found
		if (source == null) {
			source = findDynamicSource(pkgName);
			if (source != null)
				// must return the result of the dynamic import and do not continue
				return source.getResource(name);
		}

		if (result == null)
			try {
				result = (URL) searchHooks(name, POST_RESOURCE);
			} catch (FileNotFoundException e) {
				return null;
			} catch (ClassNotFoundException e) {
				// will not happen
			}
		// do buddy policy loading
		if (result == null && policy != null)
			result = policy.doBuddyResourceLoading(name);
		if (result != null)
			return result;
		// hack to support backwards compatibiility for bootdelegation
		// or last resort; do class context trick to work around VM bugs
		if (parentCL != null && !bootDelegation && ((checkParent && bundle.getFramework().compatibiltyBootDelegation) || isRequestFromVM()))
			// we don't need to continue if the resource is not found here
			return parentCL.getResource(name);
		return result;
	}

	/**
	 * Finds the resources for a bundle.  This  method is used for delegation by the bundle's classloader.
	 */
	public Enumeration<URL> findResources(String name) throws IOException {
		// do not delegate to parent because ClassLoader#getResources already did and it is final!!
		if ((name.length() > 1) && (name.charAt(0) == '/')) /* if name has a leading slash */
			name = name.substring(1); /* remove leading slash before search */
		String pkgName = getResourcePackageName(name);
		Enumeration<URL> result = null;
		try {
			result = searchHooks(name, PRE_RESOURCES);
		} catch (ClassNotFoundException e) {
			// will not happen
		} catch (FileNotFoundException e) {
			return null;
		}
		if (result != null)
			return result;
		// start at step 3 because of the comment above about ClassLoader#getResources
		// 3) search the imported packages
		PackageSource source = findImportedSource(pkgName, null);
		if (source != null)
			// 3) found import source terminate search at the source
			return source.getResources(name);
		// 4) search the required bundles
		source = findRequiredSource(pkgName, null);
		if (source != null)
			// 4) attempt to load from source but continue on failure
			result = source.getResources(name);

		// 5) search the local bundle
		// compound the required source results with the local ones
		Enumeration<URL> localResults = findLocalResources(name);
		result = compoundEnumerations(result, localResults);
		// 6) attempt to find a dynamic import source; only do this if a required source was not found
		if (result == null && source == null) {
			source = findDynamicSource(pkgName);
			if (source != null)
				return source.getResources(name);
		}
		if (result == null)
			try {
				result = searchHooks(name, POST_RESOURCES);
			} catch (ClassNotFoundException e) {
				// will not happen
			} catch (FileNotFoundException e) {
				return null;
			}
		if (policy != null) {
			Enumeration<URL> buddyResult = policy.doBuddyResourcesLoading(name);
			result = compoundEnumerations(result, buddyResult);
		}
		return result;
	}

	private boolean isSubPackage(String parentPackage, String subPackage) {
		String prefix = (parentPackage.length() == 0 || parentPackage.equals(DEFAULT_PACKAGE)) ? "" : parentPackage + '.'; //$NON-NLS-1$
		return subPackage.startsWith(prefix);
	}

	public Collection<String> listResources(String path, String filePattern, int options) {
		String pkgName = getResourcePackageName(path.endsWith("/") ? path : path + '/'); //$NON-NLS-1$
		if ((path.length() > 1) && (path.charAt(0) == '/')) /* if name has a leading slash */
			path = path.substring(1); /* remove leading slash before search */
		boolean subPackages = (options & BundleWiring.LISTRESOURCES_RECURSE) != 0;
		List<String> packages = new ArrayList<String>();
		// search imported package names
		KeyedHashSet importSources = getImportedSources(null);
		if (importSources != null) {
			KeyedElement[] imports = importSources.elements();
			for (KeyedElement keyedElement : imports) {
				String id = ((PackageSource) keyedElement).getId();
				if (id.equals(pkgName) || (subPackages && isSubPackage(pkgName, id)))
					packages.add(id);
			}
		}

		// now add package names from required bundles
		if (requiredBundles != null) {
			KeyedHashSet visited = new KeyedHashSet(false);
			visited.add(bundle); // always add ourselves so we do not recurse back to ourselves
			for (BundleLoaderProxy requiredProxy : requiredBundles) {
				BundleLoader requiredLoader = requiredProxy.getBundleLoader();
				requiredLoader.addProvidedPackageNames(requiredProxy.getSymbolicName(), pkgName, packages, subPackages, visited);
			}
		}

		boolean localSearch = (options & BundleWiring.LISTRESOURCES_LOCAL) != 0;
		List<String> result = new ArrayList<String>();
		Set<String> importedPackages = new HashSet<String>(0);
		for (String name : packages) {
			// look for import source
			PackageSource externalSource = findImportedSource(name, null);
			if (externalSource != null) {
				// record this package is imported
				importedPackages.add(name);
			} else {
				// look for require bundle source
				externalSource = findRequiredSource(name, null);
			}
			// only add the content of the external source if this is not a localSearch
			if (externalSource != null && !localSearch) {
				String packagePath = name.replace('.', '/');
				Collection<String> externalResources = externalSource.listResources(packagePath, filePattern);
				for (String resource : externalResources) {
					if (!result.contains(resource)) // prevent duplicates; could happen if the package is split or exporter has fragments/multiple jars
						result.add(resource);
				}
			}
		}

		// now search locally
		Collection<String> localResources = createClassLoader().listLocalResources(path, filePattern, options);
		for (String resource : localResources) {
			String resourcePkg = getResourcePackageName(resource);
			if (!importedPackages.contains(resourcePkg) && !result.contains(resource))
				result.add(resource);
		}
		return result;
	}

	/*
	 * This method is used by Bundle.getResources to do proper parent delegation.
	 */
	public Enumeration<URL> getResources(String name) throws IOException {
		if ((name.length() > 1) && (name.charAt(0) == '/')) /* if name has a leading slash */
			name = name.substring(1); /* remove leading slash before search */
		String pkgName = getResourcePackageName(name);
		// follow the OSGi delegation model
		// First check the parent classloader for system resources, if it is a java resource.
		Enumeration<URL> result = null;
		if (pkgName.startsWith(JAVA_PACKAGE) || bundle.getFramework().isBootDelegationPackage(pkgName)) {
			// 1) if startsWith "java." delegate to parent and terminate search
			// 2) if part of the bootdelegation list then delegate to parent and continue of failure
			ClassLoader parentCL = getParentClassLoader();
			result = parentCL == null ? null : parentCL.getResources(name);
			if (pkgName.startsWith(JAVA_PACKAGE))
				return result;
		}
		return compoundEnumerations(result, findResources(name));
	}

	public static <E> Enumeration<E> compoundEnumerations(Enumeration<E> list1, Enumeration<E> list2) {
		if (list2 == null || !list2.hasMoreElements())
			return list1;
		if (list1 == null || !list1.hasMoreElements())
			return list2;
		List<E> compoundResults = new ArrayList<E>();
		while (list1.hasMoreElements())
			compoundResults.add(list1.nextElement());
		while (list2.hasMoreElements()) {
			E item = list2.nextElement();
			if (!compoundResults.contains(item)) //don't add duplicates
				compoundResults.add(item);
		}
		return Collections.enumeration(compoundResults);
	}

	/**
	 * Finds a resource local to this bundle.  Only the classloader for this bundle is searched.
	 * @param name The name of the resource to find.
	 * @return The URL to the resource or null if the resource is not found.
	 */
	URL findLocalResource(final String name) {
		return createClassLoader().findLocalResource(name);
	}

	/**
	 * Returns an Enumeration of URLs representing all the resources with
	 * the given name. Only the classloader for this bundle is searched.
	 *
	 * @param  name the resource name
	 * @return an Enumeration of URLs for the resources
	 */
	Enumeration<URL> findLocalResources(String name) {
		return createClassLoader().findLocalResources(name);
	}

	/**
	 * Returns the absolute path name of a native library.
	 *
	 * @param      name   the library name
	 * @return     the absolute path of the native library or null if not found
	 */
	public String findLibrary(final String name) {
		if (System.getSecurityManager() == null)
			return findLocalLibrary(name);
		return AccessController.doPrivileged(new PrivilegedAction<String>() {
			public String run() {
				return findLocalLibrary(name);
			}
		});
	}

	final String findLocalLibrary(final String name) {
		String result = null;
		try {
			result = (String) searchHooks(name, PRE_LIBRARY);
		} catch (FileNotFoundException e) {
			return null;
		} catch (ClassNotFoundException e) {
			// will not happen
		}
		if (result != null)
			return result;
		result = bundle.getBundleData().findLibrary(name);
		if (result != null)
			return result;

		// look in fragments imports ...
		BundleFragment[] fragments = bundle.getFragments();
		if (fragments != null)
			for (int i = 0; i < fragments.length; i++) {
				result = fragments[i].getBundleData().findLibrary(name);
				if (result != null)
					return result;
			}
		try {
			return (String) searchHooks(name, POST_LIBRARY);
		} catch (FileNotFoundException e) {
			return null; // this is not necessary; but being consistent in case another step is added below
		} catch (ClassNotFoundException e) {
			// will not happen
		}
		return null;
	}

	/*
	 * Return the bundle we are associated with.
	 */
	public final AbstractBundle getBundle() {
		return bundle;
	}

	private BundleClassLoader createBCLPrevileged(final BundleProtectionDomain pd, final String[] cp) {
		// Create the classloader as previleged code if security manager is present.
		if (System.getSecurityManager() == null)
			return createBCL(pd, cp);

		return AccessController.doPrivileged(new PrivilegedAction<BundleClassLoader>() {
			public BundleClassLoader run() {
				return createBCL(pd, cp);
			}
		});

	}

	BundleClassLoader createBCL(final BundleProtectionDomain pd, final String[] cp) {
		BundleClassLoader bcl = bundle.getBundleData().createClassLoader(BundleLoader.this, pd, cp);
		// attach existing fragments to classloader
		BundleFragment[] fragments = bundle.getFragments();
		if (fragments != null)
			for (int i = 0; i < fragments.length; i++) {
				try {
					bcl.attachFragment(fragments[i].getBundleData(), fragments[i].getProtectionDomain(), fragments[i].getBundleData().getClassPath());
				} catch (BundleException be) {
					bundle.getFramework().publishFrameworkEvent(FrameworkEvent.ERROR, bundle, be);
				}
			}

		// finish the initialization of the classloader.
		bcl.initialize();
		return bcl;
	}

	/**
	 * Return a string representation of this loader.
	 * @return String
	 */
	public final String toString() {
		BundleData result = bundle.getBundleData();
		return result == null ? "BundleLoader.bundledata == null!" : result.toString(); //$NON-NLS-1$
	}

	/**
	 * Return true if the target package name matches
	 * a name in the DynamicImport-Package manifest header.
	 *
	 * @param pkgname The name of the requested class' package.
	 * @return true if the package should be imported.
	 */
	private final synchronized boolean isDynamicallyImported(String pkgname) {
		if (this instanceof SystemBundleLoader)
			return false; // system bundle cannot dynamically import
		// must check for startsWith("java.") to satisfy R3 section 4.7.2
		if (pkgname.startsWith("java.")) //$NON-NLS-1$
			return true;

		/* quick shortcut check */
		if ((loaderFlags & FLAG_HASDYNAMICIMPORTS) == 0)
			return false;

		/* "*" shortcut */
		if ((loaderFlags & FLAG_HASDYNAMICEIMPORTALL) != 0)
			return true;

		/* match against specific names */
		if (dynamicImportPackages != null)
			for (int i = 0; i < dynamicImportPackages.length; i++)
				if (pkgname.equals(dynamicImportPackages[i]))
					return true;

		/* match against names with trailing wildcards */
		if (dynamicImportPackageStems != null)
			for (int i = 0; i < dynamicImportPackageStems.length; i++)
				if (pkgname.startsWith(dynamicImportPackageStems[i]))
					return true;

		return false;
	}

	final void addExportedProvidersFor(String symbolicName, String packageName, List<PackageSource> result, KeyedHashSet visited) {
		if (!visited.add(bundle))
			return;

		// See if we locally provide the package.
		PackageSource local = null;
		if (isExportedPackage(packageName))
			local = proxy.getPackageSource(packageName);
		else if (isSubstitutedExport(packageName)) {
			result.add(findImportedSource(packageName, visited));
			return; // should not continue to required bundles in this case
		}
		// Must search required bundles that are exported first.
		if (requiredBundles != null) {
			int size = reexportTable == null ? 0 : reexportTable.length;
			int reexportIndex = 0;
			for (int i = 0; i < requiredBundles.length; i++) {
				if (local != null) {
					// always add required bundles first if we locally provide the package
					// This allows a bundle to provide a package from a required bundle without 
					// re-exporting the whole required bundle.
					requiredBundles[i].getBundleLoader().addExportedProvidersFor(symbolicName, packageName, result, visited);
				} else if (reexportIndex < size && reexportTable[reexportIndex] == i) {
					reexportIndex++;
					requiredBundles[i].getBundleLoader().addExportedProvidersFor(symbolicName, packageName, result, visited);
				}
			}
		}

		// now add the locally provided package.
		if (local != null && local.isFriend(symbolicName))
			result.add(local);
	}

	final void addProvidedPackageNames(String symbolicName, String packageName, List<String> result, boolean subPackages, KeyedHashSet visitied) {
		if (!visitied.add(bundle))
			return;
		for (String exported : exportedPackages) {
			if (exported.equals(packageName) || (subPackages && isSubPackage(packageName, exported))) {
				if (!result.contains(exported))
					result.add(exported);
			}
		}
		if (substitutedPackages != null)
			for (String substituted : substitutedPackages) {
				if (substituted.equals(packageName) || (subPackages && isSubPackage(packageName, substituted))) {
					if (!result.contains(substituted))
						result.add(substituted);
				}
			}
		if (requiredBundles != null) {
			int size = reexportTable == null ? 0 : reexportTable.length;
			int reexportIndex = 0;
			for (int i = 0; i < requiredBundles.length; i++) {
				if (reexportIndex < size && reexportTable[reexportIndex] == i) {
					reexportIndex++;
					requiredBundles[i].getBundleLoader().addProvidedPackageNames(symbolicName, packageName, result, subPackages, visitied);
				}
			}
		}
	}

	final boolean isExportedPackage(String name) {
		return exportedPackages.contains(name);
	}

	final boolean isSubstitutedExport(String name) {
		return substitutedPackages == null ? false : substitutedPackages.contains(name);
	}

	private void addDynamicImportPackage(ImportPackageSpecification[] packages) {
		if (packages == null)
			return;
		List<String> dynamicImports = new ArrayList<String>(packages.length);
		for (int i = 0; i < packages.length; i++)
			if (ImportPackageSpecification.RESOLUTION_DYNAMIC.equals(packages[i].getDirective(Constants.RESOLUTION_DIRECTIVE)))
				dynamicImports.add(packages[i].getName());
		if (dynamicImports.size() > 0)
			addDynamicImportPackage(dynamicImports.toArray(new String[dynamicImports.size()]));
	}

	/**
	 * Adds a list of DynamicImport-Package manifest elements to the dynamic
	 * import tables of this BundleLoader.  Duplicate packages are checked and
	 * not added again.  This method is not thread safe.  Callers should ensure
	 * synchronization when calling this method.
	 * @param packages the DynamicImport-Package elements to add.
	 */
	private void addDynamicImportPackage(String[] packages) {
		if (packages == null)
			return;

		loaderFlags |= FLAG_HASDYNAMICIMPORTS;
		int size = packages.length;
		List<String> stems;
		if (dynamicImportPackageStems == null) {
			stems = new ArrayList<String>(size);
		} else {
			stems = new ArrayList<String>(size + dynamicImportPackageStems.length);
			for (int i = 0; i < dynamicImportPackageStems.length; i++) {
				stems.add(dynamicImportPackageStems[i]);
			}
		}

		List<String> names;
		if (dynamicImportPackages == null) {
			names = new ArrayList<String>(size);
		} else {
			names = new ArrayList<String>(size + dynamicImportPackages.length);
			for (int i = 0; i < dynamicImportPackages.length; i++) {
				names.add(dynamicImportPackages[i]);
			}
		}

		for (int i = 0; i < size; i++) {
			String name = packages[i];
			if (isDynamicallyImported(name))
				continue;
			if (name.equals("*")) { /* shortcut *///$NON-NLS-1$
				loaderFlags |= FLAG_HASDYNAMICEIMPORTALL;
				return;
			}

			if (name.endsWith(".*")) //$NON-NLS-1$
				stems.add(name.substring(0, name.length() - 1));
			else
				names.add(name);
		}

		size = stems.size();
		if (size > 0)
			dynamicImportPackageStems = stems.toArray(new String[size]);

		size = names.size();
		if (size > 0)
			dynamicImportPackages = names.toArray(new String[size]);
	}

	/**
	 * Adds a list of DynamicImport-Package manifest elements to the dynamic
	 * import tables of this BundleLoader.  Duplicate packages are checked and
	 * not added again.
	 * @param packages the DynamicImport-Package elements to add.
	 */
	public final synchronized void addDynamicImportPackage(ManifestElement[] packages) {
		if (packages == null)
			return;
		List<String> dynamicImports = new ArrayList<String>(packages.length);
		List<ImportPackageSpecification> dynamicImportSpecs = new ArrayList<ImportPackageSpecification>(packages.length);
		for (ManifestElement dynamicImportElement : packages) {
			String[] names = dynamicImportElement.getValueComponents();
			for (String name : names)
				dynamicImports.add(name);
			StateBuilder.addImportPackages(dynamicImportElement, dynamicImportSpecs, 2, true);
		}
		if (dynamicImports.size() > 0) {
			addDynamicImportPackage(dynamicImports.toArray(new String[dynamicImports.size()]));
			BundleDescription revision = getLoaderProxy().getBundleDescription();
			State state = revision.getContainingState();
			state.addDynamicImportPackages(revision, dynamicImportSpecs.toArray(new ImportPackageSpecification[dynamicImportSpecs.size()]));
		}
	}

	synchronized public void attachFragment(BundleFragment fragment) throws BundleException {
		ExportPackageDescription[] exports = proxy.getBundleDescription().getSelectedExports();
		if (classloader == null) {
			initializeExports(exports, exportedPackages);
			return;
		}
		String[] classpath = fragment.getBundleData().getClassPath();
		if (classpath != null)
			classloader.attachFragment(fragment.getBundleData(), fragment.getProtectionDomain(), classpath);
		initializeExports(exports, exportedPackages);
	}

	/*
	 * Finds a packagesource that is either imported or required from another bundle.
	 * This will not include an local package source
	 */
	private PackageSource findSource(String pkgName) {
		if (pkgName == null)
			return null;
		PackageSource result = findImportedSource(pkgName, null);
		if (result != null)
			return result;
		// Note that dynamic imports are not checked to avoid aggressive wiring (bug 105779)  
		return findRequiredSource(pkgName, null);
	}

	private PackageSource findImportedSource(String pkgName, KeyedHashSet visited) {
		KeyedHashSet imports = getImportedSources(visited);
		if (imports == null)
			return null;
		synchronized (imports) {
			return (PackageSource) imports.getByKey(pkgName);
		}
	}

	private PackageSource findDynamicSource(String pkgName) {
		if (isDynamicallyImported(pkgName)) {
			ExportPackageDescription exportPackage = bundle.getFramework().getAdaptor().getState().linkDynamicImport(proxy.getBundleDescription(), pkgName);
			if (exportPackage != null) {
				PackageSource source = createExportPackageSource(exportPackage, null);
				synchronized (this) {
					if (importedSources == null)
						importedSources = new KeyedHashSet(false);
				}
				synchronized (importedSources) {
					importedSources.add(source);
				}
				return source;
			}
		}
		return null;
	}

	private PackageSource findRequiredSource(String pkgName, KeyedHashSet visited) {
		if (requiredBundles == null)
			return null;
		synchronized (requiredSources) {
			PackageSource result = (PackageSource) requiredSources.getByKey(pkgName);
			if (result != null)
				return result.isNullSource() ? null : result;
		}
		if (visited == null)
			visited = new KeyedHashSet(false);
		visited.add(bundle); // always add ourselves so we do not recurse back to ourselves
		List<PackageSource> result = new ArrayList<PackageSource>(3);
		for (int i = 0; i < requiredBundles.length; i++) {
			BundleLoader requiredLoader = requiredBundles[i].getBundleLoader();
			requiredLoader.addExportedProvidersFor(proxy.getSymbolicName(), pkgName, result, visited);
		}
		// found some so cache the result for next time and return
		PackageSource source;
		if (result.size() == 0) {
			// did not find it in our required bundles lets record the failure
			// so we do not have to do the search again for this package.
			source = NullPackageSource.getNullPackageSource(pkgName);
		} else if (result.size() == 1) {
			// if there is just one source, remember just the single source 
			source = result.get(0);
		} else {
			// if there was more than one source, build a multisource and cache that.
			PackageSource[] srcs = result.toArray(new PackageSource[result.size()]);
			source = createMultiSource(pkgName, srcs);
		}
		synchronized (requiredSources) {
			requiredSources.add(source);
		}
		return source.isNullSource() ? null : source;
	}

	/*
	 * Gets the package source for the pkgName.  This will include the local package source
	 * if the bundle exports the package.  This is used to compare the PackageSource of a 
	 * package from two different bundles.
	 */
	public final PackageSource getPackageSource(String pkgName) {
		PackageSource result = findSource(pkgName);
		if (!isExportedPackage(pkgName))
			return result;
		// if the package is exported then we need to get the local source
		PackageSource localSource = proxy.getPackageSource(pkgName);
		if (result == null)
			return localSource;
		if (localSource == null)
			return result;
		return createMultiSource(pkgName, new PackageSource[] {result, localSource});
	}

	private ClassLoader getParentPrivileged(final BundleClassLoader bcl) {
		if (System.getSecurityManager() == null)
			return bcl.getParent();

		return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
			public ClassLoader run() {
				return bcl.getParent();
			}
		});
	}

	static final class ClassContext extends SecurityManager {
		// need to make this method public
		public Class<?>[] getClassContext() {
			return super.getClassContext();
		}
	}

	static public void closeBundleLoader(BundleLoaderProxy proxy) {
		if (proxy == null)
			return;
		// First close the BundleLoader
		BundleLoader loader = proxy.getBasicBundleLoader();
		if (loader != null)
			loader.close();
		proxy.setStale();
		// if proxy is not null then make sure to unset user object
		// associated with the proxy in the state
		BundleDescription description = proxy.getBundleDescription();
		// must set it back to the bundle object; not null
		// need to make sure the user object is a BundleReference
		description.setUserObject(proxy.getBundleData());
	}
}
