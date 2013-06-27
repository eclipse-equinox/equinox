/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.framework;

import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.debug.FrameworkDebugOptions;
import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.eclipse.osgi.internal.permadmin.EquinoxSecurityManager;
import org.eclipse.osgi.internal.permadmin.SecurityAdmin;
import org.eclipse.osgi.internal.url.EquinoxFactoryManager;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.localization.BundleLocalization;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.eclipse.osgi.storage.BundleLocalizationImpl;
import org.eclipse.osgi.storage.url.BundleResourceHandler;
import org.eclipse.osgi.storage.url.BundleURLConverter;
import org.osgi.framework.*;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.resolver.Resolver;
import org.osgi.service.startlevel.StartLevel;

/**
 * This class activates the System Bundle.
 */

public class SystemBundleActivator implements BundleActivator {
	private EquinoxFactoryManager urlFactoryManager;
	private List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>(10);
	private SecurityManager setSecurityManagner;

	@SuppressWarnings("deprecation")
	public void start(BundleContext bc) throws Exception {
		registrations.clear();
		EquinoxBundle bundle = (EquinoxBundle) bc.getBundle();

		bundle.getEquinoxContainer().systemStart(bc);

		EquinoxConfiguration configuration = bundle.getEquinoxContainer().getConfiguration();
		installSecurityManager(configuration);
		bundle.getEquinoxContainer().getLogServices().start(bc);

		urlFactoryManager = new EquinoxFactoryManager(bundle.getEquinoxContainer());
		urlFactoryManager.installHandlerFactories(bc);

		FrameworkDebugOptions dbgOptions = (FrameworkDebugOptions) configuration.getDebugOptions();
		dbgOptions.start(bc);

		SecurityAdmin sa = bundle.getEquinoxContainer().getStorage().getSecurityAdmin();
		ClassLoader tccl = bundle.getEquinoxContainer().getContextFinder();

		registerLocations(bc, bundle.getEquinoxContainer().getLocations());
		register(bc, EnvironmentInfo.class, bundle.getEquinoxContainer().getConfiguration(), null);
		register(bc, PackageAdmin.class, bundle.getEquinoxContainer().getPackageAdmin(), null);
		register(bc, StartLevel.class, bundle.getEquinoxContainer().getStartLevel(), null);

		register(bc, PermissionAdmin.class, sa, null);
		register(bc, ConditionalPermissionAdmin.class, sa, null);

		register(bc, Resolver.class, bundle.getEquinoxContainer().getStorage().getAdaptor().getResolver(), null);

		register(bc, DebugOptions.class, dbgOptions, null);

		Hashtable<String, Object> props = new Hashtable<String, Object>(7);
		if (tccl != null) {
			props.clear();
			props.put("equinox.classloader.type", "contextClassLoader"); //$NON-NLS-1$ //$NON-NLS-2$
			register(bc, ClassLoader.class, tccl, props);
		}

		props.clear();
		props.put("protocol", new String[] {BundleResourceHandler.OSGI_ENTRY_URL_PROTOCOL, BundleResourceHandler.OSGI_RESOURCE_URL_PROTOCOL}); //$NON-NLS-1$
		register(bc, URLConverter.class, new BundleURLConverter(), props);

		register(bc, BundleLocalization.class, new BundleLocalizationImpl(), null);

		boolean setTccl = "true".equals(bundle.getEquinoxContainer().getConfiguration().getConfiguration("eclipse.parsers.setTCCL", "true")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		register(bc, SAXParserFactory.class, new ParsingService(true, setTccl), false, null);
		register(bc, DocumentBuilderFactory.class, new ParsingService(false, setTccl), false, null);

		bundle.getEquinoxContainer().getStorage().getExtensionInstaller().startExtensionActivators(bc);
	}

	private void installSecurityManager(EquinoxConfiguration configuration) throws BundleException {
		String securityManager = configuration.getConfiguration(Constants.FRAMEWORK_SECURITY);
		if (System.getSecurityManager() != null && securityManager != null) {
			throw new BundleException("Cannot specify the \"" + Constants.FRAMEWORK_SECURITY + "\" configuration property when a security manager is already installed."); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (securityManager == null) {
			securityManager = configuration.getConfiguration(EquinoxConfiguration.PROP_EQUINOX_SECURITY, configuration.getProperty("java.security.manager")); //$NON-NLS-1$
		}
		if (securityManager != null) {
			SecurityManager sm = System.getSecurityManager();
			if (sm == null) {
				if (securityManager.length() == 0)
					sm = new SecurityManager(); // use the default one from java
				else if (securityManager.equals(Constants.FRAMEWORK_SECURITY_OSGI))
					sm = new EquinoxSecurityManager(); // use an OSGi enabled manager that understands postponed conditions
				else {
					// try to use a specific classloader by classname
					try {
						Class<?> clazz = Class.forName(securityManager);
						sm = (SecurityManager) clazz.newInstance();
					} catch (ClassNotFoundException e) {
						// do nothing
					} catch (ClassCastException e) {
						// do nothing
					} catch (InstantiationException e) {
						// do nothing
					} catch (IllegalAccessException e) {
						// do nothing
					}
				}
				if (sm == null)
					throw new NoClassDefFoundError(securityManager);
				if (configuration.getDebug().DEBUG_SECURITY)
					Debug.println("Setting SecurityManager to: " + sm); //$NON-NLS-1$
				System.setSecurityManager(sm);
				setSecurityManagner = sm;
				return;
			}
		}
	}

	private void registerLocations(BundleContext bc, EquinoxLocations equinoxLocations) {
		Dictionary<String, Object> locationProperties = new Hashtable<String, Object>(1);
		Location location = equinoxLocations.getUserLocation();
		if (location != null) {
			locationProperties.put("type", EquinoxLocations.PROP_USER_AREA); //$NON-NLS-1$
			register(bc, Location.class, location, locationProperties);
		}
		location = equinoxLocations.getInstanceLocation();
		if (location != null) {
			locationProperties.put("type", EquinoxLocations.PROP_INSTANCE_AREA); //$NON-NLS-1$
			register(bc, Location.class, location, locationProperties);
		}
		location = equinoxLocations.getConfigurationLocation();
		if (location != null) {
			locationProperties.put("type", EquinoxLocations.PROP_CONFIG_AREA); //$NON-NLS-1$
			register(bc, Location.class, location, locationProperties);
		}
		location = equinoxLocations.getInstallLocation();
		if (location != null) {
			locationProperties.put("type", EquinoxLocations.PROP_INSTALL_AREA); //$NON-NLS-1$
			register(bc, Location.class, location, locationProperties);
		}

		location = equinoxLocations.getEclipseHomeLocation();
		if (location != null) {
			locationProperties.put("type", EquinoxLocations.PROP_HOME_LOCATION_AREA); //$NON-NLS-1$
			register(bc, Location.class, location, locationProperties);
		}
	}

	public void stop(BundleContext bc) throws Exception {
		EquinoxBundle bundle = (EquinoxBundle) bc.getBundle();

		bundle.getEquinoxContainer().getStorage().getExtensionInstaller().stopExtensionActivators(bc);

		FrameworkDebugOptions dbgOptions = (FrameworkDebugOptions) bundle.getEquinoxContainer().getConfiguration().getDebugOptions();
		dbgOptions.stop(bc);

		urlFactoryManager.uninstallHandlerFactories();

		// unregister services
		for (ServiceRegistration<?> registration : registrations)
			registration.unregister();
		registrations.clear();
		bundle.getEquinoxContainer().getLogServices().stop(bc);
		unintallSecurityManager();
		bundle.getEquinoxContainer().systemStop(bc);
	}

	private void unintallSecurityManager() {
		if (System.getSecurityManager() == setSecurityManagner)
			System.setSecurityManager(null);
		setSecurityManagner = null;
	}

	private void register(BundleContext context, Class<?> serviceClass, Object service, Dictionary<String, Object> properties) {
		register(context, serviceClass, service, true, properties);
	}

	@SuppressWarnings("unchecked")
	private void register(BundleContext context, Class<?> serviceClass, Object service, boolean setRanking, Dictionary<String, Object> properties) {
		if (properties == null)
			properties = new Hashtable<String, Object>(7);
		Dictionary<String, String> headers = context.getBundle().getHeaders();
		properties.put(Constants.SERVICE_VENDOR, headers.get(Constants.BUNDLE_VENDOR));
		if (setRanking) {
			properties.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
		}
		properties.put(Constants.SERVICE_PID, context.getBundle().getBundleId() + "." + service.getClass().getName()); //$NON-NLS-1$
		registrations.add(context.registerService((Class<Object>) serviceClass, service, properties));
	}

	private static class ParsingService implements ServiceFactory<Object> {
		private final boolean isSax;
		private final boolean setTccl;

		public ParsingService(boolean isSax, boolean setTccl) {
			this.isSax = isSax;
			this.setTccl = setTccl;
		}

		public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
			if (!setTccl || bundle == null)
				return createService();
			/*
			 * Set the TCCL while creating jaxp factory instances to the
			 * requesting bundles class loader.  This is needed to 
			 * work around bug 285505.  There are issues if multiple 
			 * xerces implementations are available on the bundles class path
			 * 
			 * The real issue is that the ContextFinder will only delegate
			 * to the framework class loader in this case.  This class
			 * loader forces the requesting bundle to be delegated to for
			 * TCCL loads.
			 */
			final ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
			try {
				BundleWiring wiring = bundle.adapt(BundleWiring.class);
				ClassLoader cl = wiring == null ? null : wiring.getClassLoader();
				if (cl != null)
					Thread.currentThread().setContextClassLoader(cl);
				return createService();
			} finally {
				Thread.currentThread().setContextClassLoader(savedClassLoader);
			}
		}

		private Object createService() {
			if (isSax)
				return SAXParserFactory.newInstance();
			return DocumentBuilderFactory.newInstance();
		}

		public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
			// Do nothing.
		}
	}
}
