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

package org.eclipse.core.runtime.internal.adaptor;

import java.io.*;
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
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.baseadaptor.AdaptorUtil;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.runnable.ApplicationLauncher;
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

	private BaseAdaptor adaptor;
	private BundleStopper stopper;
	private boolean noXML = false;

	public void frameworkStart(BundleContext context) throws BundleException {
		stopper = null;
		registerEndorsedXMLParser(context);
		Location location = LocationManager.getUserLocation();
		Hashtable locationProperties = new Hashtable(1);
		if (location != null) {
			locationProperties.put("type", LocationManager.PROP_USER_AREA); //$NON-NLS-1$
			context.registerService(Location.class.getName(), location, locationProperties);
		}
		location = LocationManager.getInstanceLocation();
		if (location != null) {
			locationProperties.put("type", LocationManager.PROP_INSTANCE_AREA); //$NON-NLS-1$
			context.registerService(Location.class.getName(), location, locationProperties);
		}
		location = LocationManager.getConfigurationLocation();
		if (location != null) {
			locationProperties.put("type", LocationManager.PROP_CONFIG_AREA); //$NON-NLS-1$
			context.registerService(Location.class.getName(), location, locationProperties);
		}
		location = LocationManager.getInstallLocation();
		if (location != null) {
			locationProperties.put("type", LocationManager.PROP_INSTALL_AREA); //$NON-NLS-1$
			context.registerService(Location.class.getName(), location, locationProperties);
		}

		Dictionary urlProperties = new Hashtable();
		urlProperties.put("protocol", new String[] {"bundleentry", "bundleresource"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		context.registerService(URLConverter.class.getName(), new URLConverterImpl(), urlProperties);

		AdaptorUtil.register(org.eclipse.osgi.service.environment.EnvironmentInfo.class.getName(), EclipseEnvironmentInfo.getDefault(), context);
		AdaptorUtil.register(PlatformAdmin.class.getName(), adaptor.getPlatformAdmin(), context);
		PluginConverter converter = PluginConverterImpl.getDefault();
		if (converter == null)
			converter = new PluginConverterImpl(adaptor, context);
		AdaptorUtil.register(PluginConverter.class.getName(), converter, context);
		AdaptorUtil.register(CommandProvider.class.getName(), new EclipseCommandProvider(context), context);
		AdaptorUtil.register(org.eclipse.osgi.service.localization.BundleLocalization.class.getName(), new BundleLocalizationImpl(), context);
	}

	private void registerEndorsedXMLParser(BundleContext bc) {
		try {
			Class.forName(SAXFACTORYNAME);
			bc.registerService(SAXFACTORYNAME, new SaxParsingService(), new Hashtable());
			Class.forName(DOMFACTORYNAME);
			bc.registerService(DOMFACTORYNAME, new DomParsingService(), new Hashtable());
		} catch (ClassNotFoundException e) {
			noXML = true;
			if (Debug.DEBUG && Debug.DEBUG_ENABLED) {
				String message = EclipseAdaptorMsg.ECLIPSE_ADAPTOR_ERROR_XML_SERVICE;
				adaptor.getFrameworkLog().log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, message, 0, e, null));
			}
		}
	}

	private class SaxParsingService implements ServiceFactory {
		public Object getService(Bundle bundle, ServiceRegistration registration) {
			return SAXParserFactory.newInstance();
		}

		public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
			// Do nothing.
		}
	}

	private class DomParsingService implements ServiceFactory {
		public Object getService(Bundle bundle, ServiceRegistration registration) {
			return DocumentBuilderFactory.newInstance();
		}

		public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
			// Do nothing.
		}
	}

	public void frameworkStop(BundleContext context) throws BundleException {
		// TODO should unregister the services here
		printStats();
		if (!noXML)
			PluginParser.releaseXMLParsing();
	}

	private void printStats() {
		FrameworkDebugOptions debugOptions = FrameworkDebugOptions.getDefault();
		if (debugOptions == null)
			return;
		String registryParsing = debugOptions.getOption("org.eclipse.core.runtime/registry/parsing/timing/value"); //$NON-NLS-1$
		if (registryParsing != null)
			EclipseAdaptorMsg.debug("Time spent in registry parsing: " + registryParsing); //$NON-NLS-1$
		String packageAdminResolution = debugOptions.getOption("debug.packageadmin/timing/value"); //$NON-NLS-1$
		if (packageAdminResolution != null)
			System.out.println("Time spent in package admin resolve: " + packageAdminResolution); //$NON-NLS-1$			
		String constraintResolution = debugOptions.getOption("org.eclipse.core.runtime.adaptor/resolver/timing/value"); //$NON-NLS-1$
		if (constraintResolution != null)
			System.out.println("Time spent resolving the dependency system: " + constraintResolution); //$NON-NLS-1$ 
	}

	public void frameworkStopping(BundleContext context) {
		// Shutdown the ApplicationLauncher service if it is available.
		ServiceReference launcherRef = context.getServiceReference(ApplicationLauncher.class.getName());
		if (launcherRef != null) {
			ApplicationLauncher launcher = (ApplicationLauncher) context.getService(launcherRef);
			// this will force a currently running application to stop.
			launcher.shutdown();
			context.ungetService(launcherRef);
		}
		stopper = new BundleStopper(context, adaptor);
		stopper.stopBundles();
	}

	public void addProperties(Properties properties) {
		// default the bootdelegation to all packages
		if (properties.getProperty(Constants.OSGI_BOOTDELEGATION) == null && !Constants.OSGI_BOOTDELEGATION_NONE.equals(properties.getProperty(Constants.OSGI_JAVA_PROFILE_BOOTDELEGATION)))
			properties.put(Constants.OSGI_BOOTDELEGATION, "*"); //$NON-NLS-1$
		if (properties.getProperty(Constants.ECLIPSE_EE_INSTALL_VERIFY) == null)
			properties.put(Constants.ECLIPSE_EE_INSTALL_VERIFY, "false"); //$NON-NLS-1$
	}

	public URLConnection mapLocationToURLConnection(String location) throws IOException {
		// do nothing
		return null;
	}

	public void handleRuntimeError(Throwable error) {
		// do nothing
	}

	public boolean matchDNChain(String pattern, String[] dnChain) {
		// do nothing
		return false;
	}

	public FrameworkLog createFrameworkLog() {
		// do nothing
		return null;
	}

	public void initialize(BaseAdaptor adaptor) {
		this.adaptor = adaptor;
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
	}

	public BundleStopper getBundleStopper() {
		return stopper;
	}

	public void addHooks(HookRegistry hookRegistry) {
		hookRegistry.addAdaptorHook(this);
	}

}
