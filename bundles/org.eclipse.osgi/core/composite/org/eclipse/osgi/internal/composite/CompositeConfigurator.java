/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.composite;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.security.AllPermission;
import java.security.ProtectionDomain;
import java.util.*;
import org.eclipse.osgi.baseadaptor.*;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook;
import org.eclipse.osgi.baseadaptor.loader.*;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.internal.module.*;
import org.eclipse.osgi.service.internal.composite.CompositeModule;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;
import org.osgi.service.framework.CompositeBundle;
import org.osgi.service.framework.CompositeBundleFactory;

public class CompositeConfigurator implements SynchronousBundleListener, HookConfigurator, AdaptorHook, ClassLoadingHook, CompositeBundleFactory, CompositeResolveHelperRegistry {

	// the base adaptor
	private BaseAdaptor adaptor;
	// the composite bundle factory service reference
	private ServiceRegistration factoryService;
	// the system bundle context
	private BundleContext systemContext;
	// The composite resolver helpers
	private final Collection helpers = new ArrayList(0);

	public void addHooks(HookRegistry hookRegistry) {
		// this is an adaptor hook to register the composite factory and 
		// to shutdown child frameworks on shutdown
		hookRegistry.addAdaptorHook(this);
		// this is a class loading hook in order to create special class loaders for composites
		hookRegistry.addClassLoadingHook(this);
	}

	public void addProperties(Properties properties) {
		// nothing
	}

	public FrameworkLog createFrameworkLog() {
		// nothing
		return null;
	}

	/**
	 * @throws BundleException  
	 */
	public void frameworkStart(BundleContext context) throws BundleException {
		this.systemContext = context;
		context.addBundleListener(this);
		addHelpers(context.getBundles());
		// this is a composite resolve helper registry; add it to the resolver
		((ResolverImpl) adaptor.getState().getResolver()).setCompositeResolveHelperRegistry(this);
		// register this as the composite bundle factory
		factoryService = context.registerService(new String[] {CompositeBundleFactory.class.getName()}, this, null);
	}

	public void frameworkStop(BundleContext context) {
		// unregister the factory
		if (factoryService != null)
			factoryService.unregister();
		factoryService = null;
		// stop any child frameworks than may still be running.
		stopFrameworks();
		context.removeBundleListener(this);
		removeAllHelpers();
	}

	public void frameworkStopping(BundleContext context) {
		// nothing
	}

	public void handleRuntimeError(Throwable error) {
		// nothing
	}

	public void initialize(BaseAdaptor initAdaptor) {
		this.adaptor = initAdaptor;
	}

	public URLConnection mapLocationToURLConnection(String location) {
		// nothing
		return null;
	}

	public boolean matchDNChain(String pattern, String[] dnChain) {
		// nothing
		return false;
	}

	public CompositeBundle installCompositeBundle(Map frameworkConfig, String location, Map compositeManifest) throws BundleException {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			// must have AllPermission to do this
			sm.checkPermission(new AllPermission());
		// make a local copy of the manifest first
		compositeManifest = new HashMap(compositeManifest);
		// make sure the manifest is valid
		CompositeHelper.validateCompositeManifest(compositeManifest);

		try {
			// get an in memory input stream to jar content of the composite we want to install
			InputStream content = CompositeHelper.getCompositeInput(frameworkConfig, compositeManifest);
			CompositeBundle result = (CompositeBundle) systemContext.installBundle(location, content);
			// set the permissions
			CompositeHelper.setCompositePermissions(location, systemContext);
			return result;
		} catch (IOException e) {
			throw new BundleException("Error creating composite bundle", e); //$NON-NLS-1$
		}
	}

	private void stopFrameworks() {
		Bundle[] allBundles = systemContext.getBundles();
		// stop each child framework
		for (int i = 0; i < allBundles.length; i++) {
			if (!(allBundles[i] instanceof CompositeBundle))
				continue;
			CompositeBundle composite = (CompositeBundle) allBundles[i];
			try {
				Framework child = composite.getCompositeFramework();
				child.stop();
				// need to wait for each child to stop
				child.waitForStop(30000);
				// TODO need to figure out a way to invalid the child
			} catch (Throwable t) {
				// TODO consider logging
				t.printStackTrace();
			}
		}
	}

	public CompositeResolveHelper getCompositeResolveHelper(BundleDescription bundle) {
		// Composite bundles implement the resolver helper
		synchronized (helpers) {
			if (helpers.size() == 0)
				return null;
			for (Iterator iHelpers = helpers.iterator(); iHelpers.hasNext();) {
				CompositeBase composite = (CompositeBase) iHelpers.next();
				if (composite.getBundleId() == bundle.getBundleId())
					// If we found a resolver helper bundle; return it
					return composite;
			}
			return null;
		}
	}

	public boolean addClassPathEntry(ArrayList cpEntries, String cp, ClasspathManager hostmanager, BaseData sourcedata, ProtectionDomain sourcedomain) {
		// nothing
		return false;
	}

	public BaseClassLoader createClassLoader(ClassLoader parent, ClassLoaderDelegate delegate, BundleProtectionDomain domain, BaseData data, String[] bundleclasspath) {
		if ((data.getType() & (BundleData.TYPE_COMPOSITEBUNDLE | BundleData.TYPE_SURROGATEBUNDLE)) == 0)
			return null;
		// only create composite class loaders for bundles that are of type composite | surrogate
		CompositeModule compositeModule = (CompositeModule) ((CompositeBase) data.getBundle()).getCompanionBundle();
		if (compositeModule == null) {
			throw new IllegalStateException("Could not find companion bundle for " + data.getBundle());
		}
		ClassLoaderDelegate companionDelegate = compositeModule.getDelegate();
		if (companionDelegate == null) {
			throw new IllegalStateException("Could not find the companion bundle delegate for" + compositeModule);
		}
		return new CompositeClassLoader(parent, delegate, companionDelegate, data);
	}

	public String findLibrary(BaseData data, String libName) {
		// nothing
		return null;
	}

	public ClassLoader getBundleClassLoaderParent() {
		// nothing
		return null;
	}

	public void initializedClassLoader(BaseClassLoader baseClassLoader, BaseData data) {
		// nothing
	}

	public byte[] processClass(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry, ClasspathManager manager) {
		// nothing
		return null;
	}

	private void addHelpers(Bundle[] bundles) {
		synchronized (helpers) {
			for (int i = 0; i < bundles.length; i++)
				addHelper(bundles[i]);
		}
	}

	private void addHelper(Bundle bundle) {
		if (!(bundle instanceof CompositeBase))
			return;
		synchronized (helpers) {
			if (!helpers.contains(bundle))
				helpers.add(bundle);
		}
	}

	private void removeHelper(Bundle bundle) {
		if (!(bundle instanceof CompositeBase))
			return;
		synchronized (helpers) {
			helpers.remove(bundle);
		}
	}

	private void removeAllHelpers() {
		synchronized (helpers) {
			helpers.clear();
		}
	}

	public void bundleChanged(BundleEvent event) {
		switch (event.getType()) {
			case BundleEvent.INSTALLED :
				addHelper(event.getBundle());
				break;
			case BundleEvent.UNINSTALLED :
				removeHelper(event.getBundle());
				break;
			default :
				break;
		}
	}
}
