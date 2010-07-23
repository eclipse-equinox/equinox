/*******************************************************************************
 * Copyright (c) 2006, 2010 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.osgi.framework.internal.protocol;

import java.lang.reflect.Method;
import java.util.*;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/*
 * An abstract class for handler factory impls (Stream and Content) that can 
 * handle environments running multiple osgi frameworks with the same VM.
 */
public abstract class MultiplexingFactory {

	protected static final String PACKAGEADMINCLASS = "org.osgi.service.packageadmin.PackageAdmin"; //$NON-NLS-1$
	protected BundleContext context;
	protected FrameworkAdaptor adaptor;
	private List factories; // list of multiplexed factories
	private ServiceTracker packageAdminTracker;

	// used to get access to the protected SecurityManager#getClassContext method
	static class InternalSecurityManager extends SecurityManager {
		public Class[] getClassContext() {
			return super.getClassContext();
		}
	}

	private static InternalSecurityManager internalSecurityManager = new InternalSecurityManager();

	MultiplexingFactory(BundleContext context, FrameworkAdaptor adaptor) {
		this.context = context;
		this.adaptor = adaptor;
		packageAdminTracker = new ServiceTracker(context, PACKAGEADMINCLASS, null);
		packageAdminTracker.open();
	}

	abstract public void setParentFactory(Object parentFactory);

	abstract public Object getParentFactory();

	public boolean isMultiplexing() {
		return getFactories() != null;
	}

	public void register(Object factory) {
		// set parent for each factory so they can do proper delegation
		try {
			Class clazz = factory.getClass();
			Method setParentFactory = clazz.getMethod("setParentFactory", new Class[] {Object.class}); //$NON-NLS-1$
			setParentFactory.invoke(factory, new Object[] {getParentFactory()});
		} catch (Exception e) {
			adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingFactory.class.getName(), FrameworkLogEntry.ERROR, 0, "register", FrameworkLogEntry.ERROR, e, null)); //$NON-NLS-1$
			throw new RuntimeException(e.getMessage(), e);
		}
		addFactory(factory);
	}

	public void unregister(Object factory) {
		removeFactory(factory);
		// close the service tracker
		try {
			// this is brittle; if class does not directly extend MultplexingFactory then this method will not exist, but we do not want a public method here
			Method closeTracker = factory.getClass().getSuperclass().getDeclaredMethod("closePackageAdminTracker", null); //$NON-NLS-1$
			closeTracker.setAccessible(true); // its a private method
			closeTracker.invoke(factory, null);
		} catch (Exception e) {
			adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingFactory.class.getName(), FrameworkLogEntry.ERROR, 0, "unregister", FrameworkLogEntry.ERROR, e, null)); //$NON-NLS-1$
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public Object designateSuccessor() {
		List released = releaseFactories();
		// Note that we do this outside of the sync block above.
		// This is only possible because we do additional locking outside of
		// this class to ensure no other threads are trying to manipulate the
		// list of registered factories.  See Framework class the following methods:
		// Framework.installURLStreamHandlerFactory(BundleContext, FrameworkAdaptor)
		// Framework.installContentHandlerFactory(BundleContext, FrameworkAdaptor)
		// Framework.uninstallURLStreamHandlerFactory
		// Framework.uninstallContentHandlerFactory()
		if (released == null || released.isEmpty())
			return getParentFactory();
		Object successor = released.remove(0);
		try {
			Class clazz = successor.getClass();
			Method register = clazz.getMethod("register", new Class[] {Object.class}); //$NON-NLS-1$		
			for (Iterator it = released.iterator(); it.hasNext();) {
				register.invoke(successor, new Object[] {it.next()});
			}
		} catch (Exception e) {
			adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingFactory.class.getName(), FrameworkLogEntry.ERROR, 0, "designateSuccessor", FrameworkLogEntry.ERROR, e, null)); //$NON-NLS-1$
			throw new RuntimeException(e.getMessage(), e);
		}
		closePackageAdminTracker(); // close tracker
		return successor;
	}

	private void closePackageAdminTracker() {
		packageAdminTracker.close();
	}

	public Object findAuthorizedFactory(List ignoredClasses) {
		List current = getFactories();
		Class[] classStack = internalSecurityManager.getClassContext();
		for (int i = 0; i < classStack.length; i++) {
			Class clazz = classStack[i];
			if (clazz == InternalSecurityManager.class || clazz == MultiplexingFactory.class || ignoredClasses.contains(clazz))
				continue;
			if (hasAuthority(clazz))
				return this;
			if (current == null)
				continue;
			for (Iterator it = current.iterator(); it.hasNext();) {
				Object factory = it.next();
				try {
					Method hasAuthorityMethod = factory.getClass().getMethod("hasAuthority", new Class[] {Class.class}); //$NON-NLS-1$
					if (((Boolean) hasAuthorityMethod.invoke(factory, new Object[] {clazz})).booleanValue()) {
						return factory;
					}
				} catch (Exception e) {
					adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingFactory.class.getName(), FrameworkLogEntry.ERROR, 0, "findAuthorizedURLStreamHandler-loop", FrameworkLogEntry.ERROR, e, null)); //$NON-NLS-1$
					throw new RuntimeException(e.getMessage(), e);
				}
			}
		}
		return null;
	}

	public boolean hasAuthority(Class clazz) {
		PackageAdmin packageAdminService = (PackageAdmin) packageAdminTracker.getService();
		if (packageAdminService != null) {
			return packageAdminService.getBundle(clazz) != null;
		}
		return false;
	}

	private synchronized List getFactories() {
		return factories;
	}

	private synchronized List releaseFactories() {
		if (factories == null)
			return null;

		List released = new LinkedList(factories);
		factories = null;
		return released;
	}

	private synchronized void addFactory(Object factory) {
		List updated = (factories == null) ? new LinkedList() : new LinkedList(factories);
		updated.add(factory);
		factories = updated;
	}

	private synchronized void removeFactory(Object factory) {
		List updated = new LinkedList(factories);
		updated.remove(factory);
		factories = updated.isEmpty() ? null : updated;
	}
}
