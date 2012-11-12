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
package org.eclipse.osgi.internal.framework;

import java.io.*;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.container.Module.Settings;
import org.eclipse.osgi.container.Module.StartOptions;
import org.eclipse.osgi.container.Module.State;
import org.eclipse.osgi.container.Module.StopOptions;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ContainerEvent;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ModuleEvent;
import org.eclipse.osgi.framework.internal.core.Msg;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;
import org.eclipse.osgi.internal.permadmin.EquinoxSecurityManager;
import org.eclipse.osgi.signedcontent.*;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.Storage;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.*;
import org.osgi.service.resolver.ResolutionException;

public class EquinoxBundle implements Bundle, BundleReference {

	static class SystemBundle extends EquinoxBundle implements Framework {

		SystemBundle(ModuleContainer moduleContainer, EquinoxContainer equinoxContainer) {
			super(moduleContainer, equinoxContainer);
		}

		@Override
		public void init() throws BundleException {
			((SystemModule) getModule()).init();
		}

		@Override
		public FrameworkEvent waitForStop(long timeout) throws InterruptedException {
			ContainerEvent event = ((SystemModule) getModule()).waitForStop(timeout);
			return new FrameworkEvent(EquinoxContainerAdaptor.getType(event), this, null);
		}

		@Override
		Module createSystemModule(ModuleContainer moduleContainer) {
			return new EquinoxSystemModule(moduleContainer);
		}

		@SuppressWarnings("unused")
		@Override
		public void stop(final int options) throws BundleException {
			getEquinoxContainer().checkAdminPermission(this, AdminPermission.EXECUTE);
			((EquinoxSystemModule) getModule()).asyncStop();
		}

		@Override
		public void stop() throws BundleException {
			stop(0);
		}

		@Override
		public void update(InputStream input) throws BundleException {
			getEquinoxContainer().checkAdminPermission(this, AdminPermission.LIFECYCLE);
			try {
				if (input != null)
					input.close();
			} catch (IOException e) {
				// do nothing
			}
			((EquinoxSystemModule) getModule()).asyncUpdate();
		}

		@Override
		public void update() throws BundleException {
			update(null);
		}

		@Override
		public void uninstall() throws BundleException {
			getEquinoxContainer().checkAdminPermission(this, AdminPermission.LIFECYCLE);
			throw new BundleException(Msg.BUNDLE_SYSTEMBUNDLE_UNINSTALL_EXCEPTION, BundleException.INVALID_OPERATION);
		}

	}

	private final EquinoxContainer equinoxContainer;
	private final Module module;
	private final Object monitor = new Object();
	private BundleContextImpl context;

	class EquinoxSystemModule extends SystemModule {
		public EquinoxSystemModule(ModuleContainer container) {
			super(container);
		}

		@Override
		public Bundle getBundle() {
			return EquinoxBundle.this;
		}

		@Override
		protected void cleanup(ModuleRevision revision) {
			// Nothing to do
		}

		@Override
		protected void initWorker() throws BundleException {
			equinoxContainer.getConfiguration().setConfiguration(Constants.FRAMEWORK_UUID, new UniversalUniqueIdentifier().toString());
			equinoxContainer.init();
			startWorker0();
		}

		@Override
		protected void stopWorker() throws BundleException {
			super.stopWorker();
			stopWorker0();
			equinoxContainer.close();
		}

		void asyncStop() throws BundleException {
			lockStateChange(ModuleEvent.STOPPED);
			try {
				if (Module.ACTIVE_SET.contains(getState())) {
					// TODO this still has a chance of a race condition:
					// multiple threads could get started if stop is called over and over
					Thread t = new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								stop();
							} catch (BundleException e) {
								// TODO not sure we can even log if this fails
								e.printStackTrace();
							}
						}
					}, "Framework stop"); //$NON-NLS-1$
					t.start();
				}
			} finally {
				unlockStateChange(ModuleEvent.STOPPED);
			}
		}

		void asyncUpdate() throws BundleException {
			lockStateChange(ModuleEvent.UPDATED);
			try {
				if (Module.ACTIVE_SET.contains(getState())) {
					Thread t = new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								update();
							} catch (BundleException e) {
								e.printStackTrace();
								// TODO not sure we can even log if this fails
							}
						}
					}, "Framework update"); //$NON-NLS-1$
					t.start();
				}
			} finally {
				unlockStateChange(ModuleEvent.UPDATED);
			}
		}
	}

	private class EquinoxModule extends Module {

		@Override
		protected void startWorker() throws BundleException {
			startWorker0();
		}

		@Override
		protected void stopWorker() throws BundleException {
			stopWorker0();
		}

		public EquinoxModule(Long id, String location, ModuleContainer container, EnumSet<Settings> settings, int startlevel) {
			super(id, location, container, settings, startlevel);
		}

		@Override
		public Bundle getBundle() {
			return EquinoxBundle.this;
		}

		@Override
		protected void cleanup(ModuleRevision revision) {
			Generation generation = (Generation) revision.getRevisionInfo();
			generation.delete();
			if (revision.equals(getCurrentRevision())) {
				// uninstall case
				generation.getBundleInfo().delete();
			}
		}
	}

	EquinoxBundle(ModuleContainer moduleContainer, EquinoxContainer equinoxContainer) {
		this.equinoxContainer = equinoxContainer;
		this.module = createSystemModule(moduleContainer);
	}

	public EquinoxBundle(Long id, String location, ModuleContainer moduleContainer, EnumSet<Settings> settings, int startlevel, EquinoxContainer equinoxContainer) {
		this.equinoxContainer = equinoxContainer;
		this.module = new EquinoxModule(id, location, moduleContainer, settings, startlevel);
	}

	Module createSystemModule(ModuleContainer moduleContainer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int compareTo(Bundle bundle) {
		long idcomp = getBundleId() - bundle.getBundleId();
		return (idcomp < 0L) ? -1 : ((idcomp > 0L) ? 1 : 0);
	}

	@Override
	public int getState() {
		switch (module.getState()) {
			case INSTALLED :
				return Bundle.INSTALLED;
			case RESOLVED :
				return Bundle.RESOLVED;
			case STARTING :
			case LAZY_STARTING :
				return Bundle.STARTING;
			case ACTIVE :
				return Bundle.ACTIVE;
			case STOPPING :
				return Bundle.STOPPING;
			case UNINSTALLED :
				return Bundle.UNINSTALLED;
			default :
				throw new IllegalStateException("No valid bundle state for module state: " + module.getState()); //$NON-NLS-1$
		}
	}

	@Override
	public void start(int options) throws BundleException {
		module.start(getStartOptions(options));
	}

	private static StartOptions[] getStartOptions(int options) {
		if (options == 0) {
			return new StartOptions[0];
		}
		Collection<StartOptions> result = new ArrayList<Module.StartOptions>(2);
		if ((options & Bundle.START_TRANSIENT) != 0) {
			result.add(StartOptions.TRANSIENT);
		}
		if ((options & Bundle.START_ACTIVATION_POLICY) != 0) {
			result.add(StartOptions.USE_ACTIVATION_POLICY);
		}
		return result.toArray(new StartOptions[result.size()]);
	}

	@Override
	public void start() throws BundleException {
		module.start();
	}

	@Override
	public void stop(int options) throws BundleException {
		module.stop(getStopOptions(options));
	}

	private StopOptions[] getStopOptions(int options) {
		if ((options & Bundle.STOP_TRANSIENT) == 0) {
			return new StopOptions[0];
		}
		return new StopOptions[] {StopOptions.TRANSIENT};
	}

	@Override
	public void stop() throws BundleException {
		module.stop();
	}

	@Override
	public void update(InputStream input) throws BundleException {
		try {
			Storage storage = equinoxContainer.getStorage();
			storage.update(module, storage.getContentConnection(module, null, input));
		} catch (IOException e) {
			throw new BundleException("Error reading bundle content.", e); //$NON-NLS-1$
		}
	}

	@Override
	public void update() throws BundleException {
		update(null);
	}

	@Override
	public void uninstall() throws BundleException {
		Storage storage = equinoxContainer.getStorage();
		storage.getModuleContainer().uninstall(module);
	}

	@Override
	public Dictionary<String, String> getHeaders() {
		return getHeaders(null);
	}

	@Override
	public Dictionary<String, String> getHeaders(String locale) {
		equinoxContainer.checkAdminPermission(this, AdminPermission.METADATA);
		Generation current = (Generation) module.getCurrentRevision().getRevisionInfo();
		return current.getHeaders(locale);
	}

	@Override
	public long getBundleId() {
		return module.getId();
	}

	@Override
	public String getLocation() {
		equinoxContainer.checkAdminPermission(getBundle(), AdminPermission.METADATA);
		return module.getLocation();
	}

	@Override
	public ServiceReference<?>[] getRegisteredServices() {
		checkValid();
		BundleContextImpl current = getBundleContextImpl();
		return current == null ? null : equinoxContainer.getServiceRegistry().getRegisteredServices(current);
	}

	@Override
	public ServiceReference<?>[] getServicesInUse() {
		checkValid();
		BundleContextImpl current = getBundleContextImpl();
		return current == null ? null : equinoxContainer.getServiceRegistry().getServicesInUse(current);
	}

	@Override
	public boolean hasPermission(Object permission) {
		Generation current = (Generation) module.getCurrentRevision().getRevisionInfo();
		ProtectionDomain domain = current.getDomain();
		if (domain != null) {
			if (permission instanceof Permission) {
				SecurityManager sm = System.getSecurityManager();
				if (sm instanceof EquinoxSecurityManager) {
					/*
					 * If the FrameworkSecurityManager is active, we need to do checks the "right" way.
					 * We can exploit our knowledge that the security context of FrameworkSecurityManager
					 * is an AccessControlContext to invoke it properly with the ProtectionDomain.
					 */
					AccessControlContext acc = new AccessControlContext(new ProtectionDomain[] {domain});
					try {
						sm.checkPermission((Permission) permission, acc);
						return true;
					} catch (Exception e) {
						return false;
					}
				}
				return domain.implies((Permission) permission);
			}
			return false;
		}
		return true;
	}

	@Override
	public URL getResource(String name) {
		try {
			equinoxContainer.checkAdminPermission(this, AdminPermission.RESOURCE);
		} catch (SecurityException e) {
			return null;
		}
		checkValid();
		if (isFragment()) {
			return null;
		}

		try {
			ModuleClassLoader classLoader = getModuleClassLoader();
			if (classLoader != null) {
				return classLoader.getResource(name);
			}
		} catch (ResolutionException e) {
			// ignore
		}

		return new ClasspathManager((Generation) module.getCurrentRevision().getRevisionInfo(), null).findLocalResource(name);
	}

	@Override
	public String getSymbolicName() {
		return module.getCurrentRevision().getSymbolicName();
	}

	@Override
	public Version getVersion() {
		return module.getCurrentRevision().getVersion();
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		try {
			equinoxContainer.checkAdminPermission(this, AdminPermission.CLASS);
		} catch (SecurityException e) {
			throw new ClassNotFoundException(name, e);
		}
		checkValid();
		try {
			ModuleClassLoader classLoader = getModuleClassLoader();
			if (classLoader != null) {
				return classLoader.loadClass(name);
			}
		} catch (ResolutionException e) {
			throw new ClassNotFoundException(name, e);
		} catch (ClassNotFoundException e) {
			if (State.LAZY_STARTING.equals(module.getState())) {
				try {
					module.start(StartOptions.LAZY_TRIGGER);
				} catch (BundleException e1) {
					equinoxContainer.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.WARNING, e.getMessage(), e);
				}
			}
			throw e;
		}
		throw new ClassNotFoundException("No class loader available for the bundle.  The bundle is likely a fragment."); //$NON-NLS-1$
	}

	private ModuleClassLoader getModuleClassLoader() throws ResolutionException {
		resolve();
		return AccessController.doPrivileged(new PrivilegedAction<ModuleClassLoader>() {
			@Override
			public ModuleClassLoader run() {
				ModuleWiring wiring = getModule().getCurrentRevision().getWiring();
				if (wiring != null) {
					ModuleLoader moduleLoader = wiring.getModuleLoader();
					if (moduleLoader instanceof BundleLoader) {
						return ((BundleLoader) moduleLoader).getModuleClassLoader();
					}
				}
				return null;
			}
		});
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		try {
			equinoxContainer.checkAdminPermission(this, AdminPermission.RESOURCE);
		} catch (SecurityException e) {
			return null;
		}
		checkValid();
		if (isFragment()) {
			return null;
		}

		try {
			ModuleClassLoader classLoader = getModuleClassLoader();
			if (classLoader != null) {
				Enumeration<URL> result = classLoader.getResources(name);
				return result.hasMoreElements() ? result : null;
			}
		} catch (ResolutionException e) {
			// ignore
		}

		return new ClasspathManager((Generation) module.getCurrentRevision().getRevisionInfo(), null).findLocalResources(name);
	}

	@Override
	public Enumeration<String> getEntryPaths(String path) {
		try {
			equinoxContainer.checkAdminPermission(this, AdminPermission.RESOURCE);
		} catch (SecurityException e) {
			return null;
		}
		checkValid();
		Generation current = (Generation) getModule().getCurrentRevision().getRevisionInfo();
		return current.getBundleFile().getEntryPaths(path);
	}

	@Override
	public URL getEntry(String path) {
		try {
			equinoxContainer.checkAdminPermission(this, AdminPermission.RESOURCE);
		} catch (SecurityException e) {
			return null;
		}
		checkValid();
		Generation current = (Generation) getModule().getCurrentRevision().getRevisionInfo();
		return current.getEntry(path);
	}

	@Override
	public long getLastModified() {
		return module.getLastModified();
	}

	@Override
	public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
		try {
			equinoxContainer.checkAdminPermission(this, AdminPermission.RESOURCE);
		} catch (SecurityException e) {
			return null;
		}
		checkValid();
		try {
			resolve();
		} catch (ResolutionException e) {
			// ignore
		}
		return Storage.findEntries(getGenerations(), path, filePattern, recurse ? BundleWiring.FINDENTRIES_RECURSE : 0);
	}

	@Override
	public BundleContext getBundleContext() {
		equinoxContainer.checkAdminPermission(this, AdminPermission.CONTEXT);
		return createBundleContext(true);
	}

	BundleContextImpl createBundleContext(boolean checkPermission) {
		if (isFragment()) {
			// fragments cannot have contexts
			return null;
		}
		synchronized (this.monitor) {
			if (context == null) {
				// only create the context if we are starting, active or stopping
				// this is so that SCR can get the context for lazy-start bundles
				if (Module.ACTIVE_SET.contains(module.getState())) {
					context = new BundleContextImpl(this, equinoxContainer);
				}
			}
			return context;
		}
	}

	private BundleContextImpl getBundleContextImpl() {
		synchronized (this.monitor) {
			return context;
		}
	}

	@Override
	public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
		SignedContentFactory factory = equinoxContainer.getSignedContentFactory();
		if (factory == null) {
			return Collections.emptyMap();
		}
		try {
			SignedContent signedContent = factory.getSignedContent(this);
			SignerInfo[] infos = signedContent.getSignerInfos();
			if (infos.length == 0)
				return Collections.emptyMap();
			Map<X509Certificate, List<X509Certificate>> results = new HashMap<X509Certificate, List<X509Certificate>>(infos.length);
			for (int i = 0; i < infos.length; i++) {
				if (signersType == SIGNERS_TRUSTED && !infos[i].isTrusted())
					continue;
				Certificate[] certs = infos[i].getCertificateChain();
				if (certs == null || certs.length == 0)
					continue;
				List<X509Certificate> certChain = new ArrayList<X509Certificate>();
				for (int j = 0; j < certs.length; j++)
					certChain.add((X509Certificate) certs[j]);
				results.put((X509Certificate) certs[0], certChain);
			}
			return results;
		} catch (Exception e) {
			return Collections.emptyMap();
		}
	}

	@Override
	public final <A> A adapt(Class<A> adapterType) {
		checkAdaptPermission(adapterType);
		return adapt0(adapterType);
	}

	@SuppressWarnings("unchecked")
	private <A> A adapt0(Class<A> adapterType) {
		if (adapterType.isInstance(this))
			return (A) this;

		if (AccessControlContext.class.equals(adapterType)) {
			Generation current = (Generation) module.getCurrentRevision().getRevisionInfo();
			ProtectionDomain domain = current.getDomain();
			return (A) (domain == null ? null : new AccessControlContext(new ProtectionDomain[] {domain}));
		}

		if (BundleContext.class.equals(adapterType)) {
			try {
				return (A) getBundleContext();
			} catch (SecurityException e) {
				return null;
			}
		}

		if (BundleRevision.class.equals(adapterType)) {
			if (module.getState().equals(State.UNINSTALLED)) {
				return null;
			}
			return (A) module.getCurrentRevision();
		}

		if (BundleRevisions.class.equals(adapterType)) {
			return (A) module.getRevisions();
		}

		if (BundleStartLevel.class.equals(adapterType)) {
			return (A) module;
		}

		if (BundleWiring.class.equals(adapterType)) {
			if (module.getState().equals(State.UNINSTALLED)) {
				return null;
			}
			ModuleRevision revision = module.getCurrentRevision();
			if (revision == null) {
				return null;
			}
			return (A) revision.getWiring();
		}

		if (getBundleId() == 0) {
			if (Framework.class.equals(adapterType)) {
				// TODO
			}

			if (FrameworkStartLevel.class.equals(adapterType)) {
				return (A) equinoxContainer.getStorage().getModuleContainer().getFrameworkStartLevel();
			}

			if (FrameworkWiring.class.equals(adapterType)) {
				return (A) equinoxContainer.getStorage().getModuleContainer().getFrameworkWiring();
			}
		}

		// Equinox extras
		if (Module.class.equals(adapterType)) {
			return (A) module;
		}
		return null;
	}

	/**
	 * Check for permission to get a service.
	 */
	private <A> void checkAdaptPermission(Class<A> adapterType) {
		SecurityManager sm = System.getSecurityManager();
		if (sm == null) {
			return;
		}
		sm.checkPermission(new AdaptPermission(adapterType.getName(), this, AdaptPermission.ADAPT));
	}

	@Override
	public File getDataFile(String filename) {
		checkValid();
		Generation current = (Generation) module.getCurrentRevision().getRevisionInfo();
		return current.getBundleInfo().getDataFile(filename);
	}

	@Override
	public Bundle getBundle() {
		return this;
	}

	public Module getModule() {
		return module;
	}

	private final void checkValid() {
		if (module.getState().equals(State.UNINSTALLED))
			throw new IllegalStateException("Module has been uninstalled."); //$NON-NLS-1$
	}

	public boolean isFragment() {
		return (getModule().getCurrentRevision().getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
	}

	void startWorker0() throws BundleException {
		BundleContextImpl current = createBundleContext(false);
		if (current == null) {
			throw new BundleException("Unable to create bundle context!"); //$NON-NLS-1$
		}
		try {
			current.start();
		} catch (BundleException e) {
			current.close();
			synchronized (EquinoxBundle.this.monitor) {
				context = null;
			}
			throw e;
		}
	}

	void stopWorker0() throws BundleException {
		BundleContextImpl current = getBundleContextImpl();
		if (current != null) {
			try {
				current.stop();
			} finally {
				current.close();
			}
			synchronized (EquinoxBundle.this.monitor) {
				context = null;
			}
		}
	}

	void resolve() throws ResolutionException {
		if (!Module.RESOLVED_SET.contains(module.getState())) {
			module.getContainer().resolve(Arrays.asList(module), true);
		}
	}

	List<Generation> getGenerations() {
		List<Generation> result = new ArrayList<Generation>();
		ModuleRevision current = getModule().getCurrentRevision();
		result.add((Generation) current.getRevisionInfo());
		ModuleWiring wiring = current.getWiring();
		if (wiring != null) {
			for (ModuleWire hostWire : wiring.getProvidedModuleWires(HostNamespace.HOST_NAMESPACE)) {
				result.add((Generation) hostWire.getRequirer().getRevisionInfo());
			}
		}
		return result;
	}

	EquinoxContainer getEquinoxContainer() {
		return equinoxContainer;
	}

	public String toString() {
		return getModule().getCurrentRevision().toString();
	}
}
