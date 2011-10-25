/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.runtime.internal.adaptor;

import java.io.IOException;
import java.net.URLConnection;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import org.eclipse.core.runtime.adaptor.LocationManager;
import org.eclipse.osgi.baseadaptor.*;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.debug.FrameworkDebugOptions;
import org.eclipse.osgi.framework.internal.core.*;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.baseadaptor.AdaptorUtil;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.osgi.framework.*;

public class EclipseAdaptorHook implements AdaptorHook, HookConfigurator {
	/** The SAX factory name */
	public static final String SAXFACTORYNAME = "javax.xml.parsers.SAXParserFactory"; //$NON-NLS-1$
	/** The DOM factory name */
	public static final String DOMFACTORYNAME = "javax.xml.parsers.DocumentBuilderFactory"; //$NON-NLS-1$
	private static final String RUNTIME_ADAPTOR = FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME + "/eclipseadaptor"; //$NON-NLS-1$
	private static final String OPTION_CONVERTER = RUNTIME_ADAPTOR + "/converter/debug"; //$NON-NLS-1$
	private static final String OPTION_LOCATION = RUNTIME_ADAPTOR + "/debug/location"; //$NON-NLS-1$
	private static final String OPTION_CACHEDMANIFEST = RUNTIME_ADAPTOR + "/debug/cachedmanifest"; //$NON-NLS-1$
	static final boolean SET_TCCL_XMLFACTORY = "true".equals(FrameworkProperties.getProperty("eclipse.parsers.setTCCL", "true"));//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	private BaseAdaptor adaptor;
	private boolean noXML = false;
	private List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>(10);

	/**
	 * @throws BundleException  
	 */
	public void frameworkStart(BundleContext context) throws BundleException {
		registrations.clear();
		registerEndorsedXMLParser(context);
		Dictionary<String, Object> locationProperties = new Hashtable<String, Object>(1);
		Location location = LocationManager.getUserLocation();
		if (location != null) {
			locationProperties.put("type", LocationManager.PROP_USER_AREA); //$NON-NLS-1$
			registrations.add(context.registerService(Location.class.getName(), location, locationProperties));
		}
		location = LocationManager.getInstanceLocation();
		if (location != null) {
			locationProperties.put("type", LocationManager.PROP_INSTANCE_AREA); //$NON-NLS-1$
			registrations.add(context.registerService(Location.class.getName(), location, locationProperties));
		}
		location = LocationManager.getConfigurationLocation();
		if (location != null) {
			locationProperties.put("type", LocationManager.PROP_CONFIG_AREA); //$NON-NLS-1$
			registrations.add(context.registerService(Location.class.getName(), location, locationProperties));
		}
		location = LocationManager.getInstallLocation();
		if (location != null) {
			locationProperties.put("type", LocationManager.PROP_INSTALL_AREA); //$NON-NLS-1$
			registrations.add(context.registerService(Location.class.getName(), location, locationProperties));
		}

		location = LocationManager.getEclipseHomeLocation();
		if (location != null) {
			locationProperties.put("type", LocationManager.PROP_HOME_LOCATION_AREA); //$NON-NLS-1$
			registrations.add(context.registerService(Location.class.getName(), location, locationProperties));
		}

		Dictionary<String, Object> urlProperties = new Hashtable<String, Object>();
		urlProperties.put("protocol", new String[] {"bundleentry", "bundleresource"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		registrations.add(context.registerService(URLConverter.class.getName(), new URLConverterImpl(), urlProperties));

		registrations.add(AdaptorUtil.register(org.eclipse.osgi.service.environment.EnvironmentInfo.class.getName(), EclipseEnvironmentInfo.getDefault(), context));
		registrations.add(AdaptorUtil.register(PlatformAdmin.class.getName(), adaptor.getPlatformAdmin(), context));
		PluginConverter converter = PluginConverterImpl.getDefault();
		if (converter == null)
			converter = new PluginConverterImpl(adaptor, context);
		registrations.add(AdaptorUtil.register(PluginConverter.class.getName(), converter, context));
		String builtinEnabled = FrameworkProperties.getProperty(ConsoleManager.PROP_CONSOLE_ENABLED, ConsoleManager.CONSOLE_BUNDLE);
		if ("true".equals(builtinEnabled)) { //$NON-NLS-1$
			registrations.add(AdaptorUtil.register(CommandProvider.class.getName(), new EclipseCommandProvider(context), context));
		}
		registrations.add(AdaptorUtil.register(org.eclipse.osgi.service.localization.BundleLocalization.class.getName(), new BundleLocalizationImpl(), context));
	}

	private void registerEndorsedXMLParser(BundleContext bc) {
		try {
			Class.forName(SAXFACTORYNAME);
			registrations.add(bc.registerService(SAXFACTORYNAME, new ParsingService(true), null));
			Class.forName(DOMFACTORYNAME);
			registrations.add(bc.registerService(DOMFACTORYNAME, new ParsingService(false), null));
		} catch (ClassNotFoundException e) {
			noXML = true;
			if (Debug.DEBUG_ENABLED) {
				String message = EclipseAdaptorMsg.ECLIPSE_ADAPTOR_ERROR_XML_SERVICE;
				adaptor.getFrameworkLog().log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, message, 0, e, null));
			}
		}
	}

	private static class ParsingService implements ServiceFactory<Object> {
		private final boolean isSax;

		public ParsingService(boolean isSax) {
			this.isSax = isSax;
		}

		public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
			BundleHost host = (bundle instanceof BundleHost) ? (BundleHost) bundle : null;
			if (!SET_TCCL_XMLFACTORY || bundle == null)
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
				ClassLoader cl = host.getClassLoader();
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

	/**
	 * @throws BundleException  
	 */
	public void frameworkStop(BundleContext context) throws BundleException {
		printStats();
		if (!noXML)
			PluginParser.releaseXMLParsing();
		// unregister services
		for (ServiceRegistration<?> registration : registrations)
			registration.unregister();
		registrations.clear();
	}

	private void printStats() {
		FrameworkDebugOptions debugOptions = FrameworkDebugOptions.getDefault();
		if (debugOptions == null)
			return;
		String registryParsing = debugOptions.getOption("org.eclipse.core.runtime/registry/parsing/timing/value"); //$NON-NLS-1$
		if (registryParsing != null)
			MessageHelper.debug("Time spent in registry parsing: " + registryParsing); //$NON-NLS-1$
		String packageAdminResolution = debugOptions.getOption("debug.packageadmin/timing/value"); //$NON-NLS-1$
		if (packageAdminResolution != null)
			System.out.println("Time spent in package admin resolve: " + packageAdminResolution); //$NON-NLS-1$			
		String constraintResolution = debugOptions.getOption("org.eclipse.core.runtime.adaptor/resolver/timing/value"); //$NON-NLS-1$
		if (constraintResolution != null)
			System.out.println("Time spent resolving the dependency system: " + constraintResolution); //$NON-NLS-1$ 
	}

	public void frameworkStopping(BundleContext context) {
		// do nothing
	}

	public void addProperties(Properties properties) {
		// do nothing
	}

	/**
	 * @throws IOException  
	 */
	public URLConnection mapLocationToURLConnection(String location) throws IOException {
		// do nothing
		return null;
	}

	public void handleRuntimeError(Throwable error) {
		// do nothing
	}

	public FrameworkLog createFrameworkLog() {
		// do nothing
		return null;
	}

	public void initialize(BaseAdaptor initAdaptor) {
		this.adaptor = initAdaptor;
		// EnvironmentInfo has to be initialized first to compute defaults for system context (see bug 88925)
		EclipseEnvironmentInfo.getDefault();
		setDebugOptions();
	}

	private void setDebugOptions() {
		FrameworkDebugOptions options = FrameworkDebugOptions.getDefault();
		// may be null if debugging is not enabled
		if (options == null)
			return;
		PluginConverterImpl.DEBUG = options.getBooleanOption(OPTION_CONVERTER, false);
		BasicLocation.DEBUG = options.getBooleanOption(OPTION_LOCATION, false);
		CachedManifest.DEBUG = options.getBooleanOption(OPTION_CACHEDMANIFEST, false);
	}

	public void addHooks(HookRegistry hookRegistry) {
		hookRegistry.addAdaptorHook(this);
	}

}
