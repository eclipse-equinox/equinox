/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import java.io.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.adaptor.core.AdaptorElementFactory;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.debug.DebugOptions;
import org.eclipse.osgi.framework.internal.defaultadaptor.DefaultAdaptor;
import org.eclipse.osgi.framework.internal.defaultadaptor.DefaultBundleData;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.stats.StatsManager;
import org.eclipse.osgi.internal.resolver.StateImpl;
import org.eclipse.osgi.internal.resolver.StateManager;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.osgi.framework.*;

public class EclipseAdaptor extends DefaultAdaptor {
	public static final String PROP_CLEAN = "osgi.clean"; //$NON-NLS-1$
	static final String F_LOG = ".log"; //$NON-NLS-1$

	//TODO rename it to Eclipse-PluginClass	
	public static final String PLUGIN_CLASS = "Plugin-Class"; //$NON-NLS-1$

	public static final String ECLIPSE_AUTOSTART = "Eclipse-AutoStart"; //$NON-NLS-1$
	//TODO rename constant to ECLIPSE_AUTOSTART_EXCEPTIONS	
	public static final String ECLIPSE_AUTOSTART_EXCEPTIONS = "exceptions"; //$NON-NLS-1$

	public static final String SAXFACTORYNAME = "javax.xml.parsers.SAXParserFactory"; //$NON-NLS-1$
	public static final String DOMFACTORYNAME = "javax.xml.parsers.DocumentBuilderFactory"; //$NON-NLS-1$

	private static final String RUNTIME_ADAPTOR = FRAMEWORK_SYMBOLICNAME + "/eclipseadaptor"; //$NON-NLS-1$
	private static final String OPTION_STATE_READER = RUNTIME_ADAPTOR + "/state/reader";//$NON-NLS-1$
	private static final String OPTION_RESOLVER = RUNTIME_ADAPTOR + "/resolver/timing"; //$NON-NLS-1$
	private static final String OPTION_PLATFORM_ADMIN = RUNTIME_ADAPTOR + "/debug/platformadmin"; //$NON-NLS-1$
	private static final String OPTION_PLATFORM_ADMIN_RESOLVER = RUNTIME_ADAPTOR + "/debug/platformadmin/resolver"; //$NON-NLS-1$
	private static final String OPTION_MONITOR_PLATFORM_ADMIN = RUNTIME_ADAPTOR + "/resolver/timing"; //$NON-NLS-1$
	private static final String OPTION_RESOLVER_READER = RUNTIME_ADAPTOR + "/resolver/reader/timing"; //$NON-NLS-1$
	private static final String OPTION_CONVERTER = RUNTIME_ADAPTOR + "/converter/debug"; //$NON-NLS-1$

	public static final byte BUNDLEDATA_VERSION = 9;
	public static final byte NULL = 0;
	public static final byte OBJECT = 1;
	//Indicate if the framework is stopping

	private static EclipseAdaptor instance;

	private long timeStamp = 0;
	private String installURL = null;

	/*
	 * Should be instantiated only by the framework (through reflection). 
	 */
	public EclipseAdaptor(String[] args) {
		super(args);
		instance = this;
		setDebugOptions();
	}

	public static EclipseAdaptor getDefault() {
		return instance;
	}

	public void initialize(EventPublisher eventPublisher) {
		if (Boolean.getBoolean(EclipseAdaptor.PROP_CLEAN))
			cleanOSGiCache();
		super.initialize(eventPublisher);
	}

	protected void initBundleStoreRootDir() {
		File configurationLocation = LocationManager.getOSGiConfigurationDir();
		if (configurationLocation != null) {
			bundleStoreRootDir = new File(configurationLocation, LocationManager.BUNDLES_DIR);
			bundleStore = bundleStoreRootDir.getAbsolutePath();
		} else {
			// last resort just default to "bundles"
			bundleStore = LocationManager.BUNDLES_DIR;
			bundleStoreRootDir = new File(bundleStore);
		}

		/* store bundleStore back into adaptor properties for others to see */
		properties.put(BUNDLE_STORE, bundleStoreRootDir.getAbsolutePath());
	}

	protected FrameworkLog createFrameworkLog() {
		if (frameworkLog != null)
			return frameworkLog;
		return EclipseStarter.createFrameworkLog();
	}

	protected StateManager createStateManager() {
		readHeaders();
		checkLocationAndReinitialize();
		File stateLocation = LocationManager.getConfigurationFile(LocationManager.STATE_FILE);
		stateManager = new StateManager(stateLocation, timeStamp);
		stateManager.setInstaller(new EclipseBundleInstaller());
		StateImpl systemState = stateManager.getSystemState();
		if (systemState != null)
			return stateManager;
		systemState = stateManager.createSystemState();
		BundleData[] installedBundles = getInstalledBundles();
		if (installedBundles == null)
			return stateManager;
		StateObjectFactory factory = stateManager.getFactory();
		for (int i = 0; i < installedBundles.length; i++) {
			BundleData toAdd = (BundleData) installedBundles[i];
			try {
				Dictionary manifest = toAdd.getManifest();
				BundleDescription newDescription = factory.createBundleDescription(manifest, toAdd.getLocation(), toAdd.getBundleID());
				systemState.addBundle(newDescription);
			} catch (BundleException be) {
				// just ignore bundle datas with invalid manifests
			}
		}
		// we need the state resolved
		systemState.setTimeStamp(timeStamp);
		systemState.resolve();
		return stateManager;
	}

	private void cleanOSGiCache() {
		File osgiConfig = LocationManager.getOSGiConfigurationDir();
		if (!rm(osgiConfig)) {
			// TODO log error?
		}
	}

	private void checkLocationAndReinitialize() {
		if (installURL == null) {
			installURL = EclipseStarter.getSysPath(); //TODO This reference to the starter should be avoided
			return;
		}
		if (!EclipseStarter.getSysPath().equals(installURL)) {
			//delete the metadata file and the framework file when the location of the basic bundles has changed 
			LocationManager.getConfigurationFile(LocationManager.BUNDLE_DATA_FILE).delete();
			LocationManager.getConfigurationFile(LocationManager.FRAMEWORK_FILE).delete();
			LocationManager.getConfigurationFile(LocationManager.STATE_FILE).delete();
			installURL = EclipseStarter.getSysPath();
		}
	}

	protected File getMetaDataFile() {
		return LocationManager.getConfigurationFile(LocationManager.FRAMEWORK_FILE);
	}

	private void readHeaders() {
		File metadata = LocationManager.getConfigurationFile(LocationManager.BUNDLE_DATA_FILE);
		if (!metadata.isFile())
			return;

		try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(metadata)));
			try {
				if (in.readByte() == BUNDLEDATA_VERSION) {
					timeStamp = in.readLong();
					installURL = in.readUTF();
					initialBundleStartLevel = in.readInt();
					nextId = in.readInt();
				}
			} finally {
				in.close();
			}
		} catch (IOException e) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Error reading framework metadata: " + e.getMessage()); //$NON-NLS-1$
				Debug.printStackTrace(e);
			}
		}
	}

	public AdaptorElementFactory getElementFactory() {
		if (elementFactory == null)
			elementFactory = new EclipseElementFactory();
		return elementFactory;
	}

	public void frameworkStart(BundleContext context) throws BundleException {
		super.frameworkStart(context);
		Bundle bundle = context.getBundle();
		Location location;

		// Less than optimal reference to EclipseStarter here.  Not sure how we can make the location
		// objects available.  They are needed very early in EclipseStarter but these references tie
		// the adaptor to that starter.
		location = LocationManager.getUserLocation();
		Hashtable properties = new Hashtable(1);
		if (location != null) {
			properties.put("type", LocationManager.PROP_USER_AREA); //$NON-NLS-1$
			context.registerService(Location.class.getName(), location, properties);
		}
		location = LocationManager.getInstanceLocation();
		if (location != null) {
			properties.put("type", LocationManager.PROP_INSTANCE_AREA); //$NON-NLS-1$
			context.registerService(Location.class.getName(), location, properties);
		}
		location = LocationManager.getConfigurationLocation();
		if (location != null) {
			properties.put("type", LocationManager.PROP_CONFIG_AREA); //$NON-NLS-1$
			context.registerService(Location.class.getName(), location, properties);
		}
		location = LocationManager.getInstallLocation();
		if (location != null) {
			properties.put("type", LocationManager.PROP_INSTALL_AREA); //$NON-NLS-1$
			context.registerService(Location.class.getName(), location, properties);
		}

		register(org.eclipse.osgi.service.environment.EnvironmentInfo.class.getName(), EnvironmentInfo.getDefault(), bundle);
		register(PlatformAdmin.class.getName(), stateManager, bundle);
		register(PluginConverter.class.getName(), new PluginConverterImpl(context), bundle);
		register(URLConverter.class.getName(), new URLConverterImpl(), bundle);
		register(CommandProvider.class.getName(), new EclipseCommandProvider(context), bundle);
		register(FrameworkLog.class.getName(), getFrameworkLog(), bundle);
		register(org.eclipse.osgi.service.localization.BundleLocalization.class.getName(), new BundleLocalizationImpl(), bundle);
		registerEndorsedXMLParser();
	}

	private void setDebugOptions() {
		DebugOptions options = DebugOptions.getDefault();
		// may be null if debugging is not enabled
		if (options == null)
			return;
		StateManager.DEBUG = options != null;
		StateManager.DEBUG_READER = options.getBooleanOption(OPTION_RESOLVER_READER, false);
		StateManager.MONITOR_PLATFORM_ADMIN = options.getBooleanOption(OPTION_MONITOR_PLATFORM_ADMIN, false);
		StateManager.DEBUG_PLATFORM_ADMIN = options.getBooleanOption(OPTION_PLATFORM_ADMIN, false);
		StateManager.DEBUG_PLATFORM_ADMIN_RESOLVER = options.getBooleanOption(OPTION_PLATFORM_ADMIN_RESOLVER, false);
		PluginConverterImpl.DEBUG = options.getBooleanOption(OPTION_CONVERTER, false);
	}

	private void registerEndorsedXMLParser() {
		try {
			Class.forName(SAXFACTORYNAME);
			context.registerService(SAXFACTORYNAME, new SaxParsingService(), new Hashtable());
			Class.forName(DOMFACTORYNAME);
			context.registerService(DOMFACTORYNAME, new DomParsingService(), new Hashtable());
		} catch (ClassNotFoundException e) {
			// In case the JAXP API is not on the boot classpath
			String message = EclipseAdaptorMsg.formatter.getString("ECLIPSE_ADAPTOR_ERROR_XML_SERVICE"); //$NON-NLS-1$
			getFrameworkLog().log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, message, 0, e, null));
		}
	}

	private class SaxParsingService implements ServiceFactory {
		public Object getService(Bundle bundle, ServiceRegistration registration) {
			return SAXParserFactory.newInstance();
		}

		public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
			//Do nothing.
		}
	}

	private class DomParsingService implements ServiceFactory {
		public Object getService(Bundle bundle, ServiceRegistration registration) {
			return DocumentBuilderFactory.newInstance();
		}

		public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
			//Do nothing.
		}
	}

	public void frameworkStop(BundleContext context) throws BundleException {
		saveMetaData();
		super.frameworkStop(context);
		printStats();
		PluginParser.releaseXMLParsing();
	}

	private void printStats() {
		DebugOptions debugOptions = DebugOptions.getDefault();
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

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getInstalledBundles()
	 */
	public BundleData[] getInstalledBundles() {
		File metadata = LocationManager.getConfigurationFile(LocationManager.BUNDLE_DATA_FILE);
		if (!metadata.isFile())
			return null;
		try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(metadata)));
			try {
				if (in.readByte() != BUNDLEDATA_VERSION)
					return null;
				// skip timeStamp - was read by readTimeStamp
				in.readLong();
				in.readUTF();
				in.readInt();
				in.readLong();

				int bundleCount = in.readInt();
				ArrayList result = new ArrayList(bundleCount);
				long id = -1;
				State state = stateManager.getSystemState();
				long stateTimeStamp = state.getTimeStamp();
				for (int i = 0; i < bundleCount; i++) {
					try {
						try {
							id = in.readLong();
							if (id != 0) {
								EclipseBundleData data = (EclipseBundleData) getElementFactory().createBundleData(this, id);
								loadMetaDataFor(data, in);
								data.initializeExistingBundle();
								if (Debug.DEBUG && Debug.DEBUG_GENERAL)
									Debug.println("BundleData created: " + data); //$NON-NLS-1$ 
								result.add(data);
							}
						} catch (NumberFormatException e) {
							// should never happen
						}
					} catch (IOException e) {
						state.removeBundle(id);
						if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
							Debug.println("Error reading framework metadata: " + e.getMessage()); //$NON-NLS-1$ 
							Debug.printStackTrace(e);
						}
					}
				}
				if (stateTimeStamp != state.getTimeStamp())
					state.resolve(false); //time stamp changed force a full resolve
				return (BundleData[]) result.toArray(new BundleData[result.size()]);
			} finally {
				in.close();
			}
		} catch (IOException e) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Error reading framework metadata: " + e.getMessage()); //$NON-NLS-1$ 
				Debug.printStackTrace(e);
			}
		}
		return null;
	}

	protected void loadMetaDataFor(EclipseBundleData data, DataInputStream in) throws IOException {
		byte flag = in.readByte();
		if (flag == NULL)
			return;
		data.setLocation(readString(in, false));
		data.setFileName(readString(in, false));
		data.setSymbolicName(readString(in, false));
		data.setVersion(new Version(readString(in, false)));
		data.setActivator(readString(in, false));
		data.setAutoStart(in.readBoolean());
		int exceptionsCount = in.readInt();
		String[] autoStartExceptions = exceptionsCount > 0 ? new String[exceptionsCount] : null;
		for (int i = 0; i < exceptionsCount; i++)
			autoStartExceptions[i] = in.readUTF();
		data.setAutoStartExceptions(autoStartExceptions);
		data.setPluginClass(readString(in, false));
		data.setClassPath(readString(in, false));
		data.setNativePaths(readString(in, false));
		data.setExecutionEnvironment(readString(in, false));
		data.setDynamicImports(readString(in, false));
		data.setGeneration(in.readInt());
		data.setStartLevel(in.readInt());
		data.setStatus(in.readInt());
		data.setReference(in.readBoolean());
		data.setFragment(in.readBoolean());
		data.setManifestTimeStamp(in.readLong());
		data.setManifestType(in.readByte());
	}

	public void saveMetaDataFor(DefaultBundleData data) throws IOException {
		if (!((EclipseBundleData) data).isAutoStartable()) {
			timeStamp--; //Change the value of the timeStamp, as a marker that something changed.  
		}
	}

	protected void saveMetaDataFor(BundleData data, DataOutputStream out) throws IOException {
		if (data.getBundleID() == 0 || !(data instanceof DefaultBundleData)) {
			out.writeByte(NULL);
			return;
		}
		EclipseBundleData bundleData = (EclipseBundleData) data;
		out.writeByte(OBJECT);
		writeStringOrNull(out, bundleData.getLocation());
		writeStringOrNull(out, bundleData.getFileName());
		writeStringOrNull(out, bundleData.getSymbolicName());
		writeStringOrNull(out, bundleData.getVersion().toString());
		writeStringOrNull(out, bundleData.getActivator());
		out.writeBoolean(bundleData.isAutoStart());
		String[] autoStartExceptions = bundleData.getAutoStartExceptions();
		if (autoStartExceptions == null)
			out.writeInt(0);
		else {
			out.writeInt(autoStartExceptions.length);
			for (int i = 0; i < autoStartExceptions.length; i++)
				out.writeUTF(autoStartExceptions[i]);
		}
		writeStringOrNull(out, bundleData.getPluginClass());
		writeStringOrNull(out, bundleData.getClassPath());
		writeStringOrNull(out, bundleData.getNativePathsString());
		writeStringOrNull(out, bundleData.getExecutionEnvironment());
		writeStringOrNull(out, bundleData.getDynamicImports());
		out.writeInt(bundleData.getGeneration());
		out.writeInt(bundleData.getStartLevel());
		out.writeInt(bundleData.getPersistentStatus());
		out.writeBoolean(bundleData.isReference());
		out.writeBoolean(bundleData.isFragment());
		out.writeLong(bundleData.getManifestTimeStamp());
		out.writeByte(bundleData.getManifestType());
	}

	private String readString(DataInputStream in, boolean intern) throws IOException {
		byte type = in.readByte();
		if (type == NULL)
			return null;
		if (intern)
			return in.readUTF().intern();
		else
			return in.readUTF();
	}

	private void writeStringOrNull(DataOutputStream out, String string) throws IOException {
		if (string == null)
			out.writeByte(NULL);
		else {
			out.writeByte(OBJECT);
			out.writeUTF(string);
		}
	}

	public void saveMetaData() {
		File metadata = LocationManager.getConfigurationFile(LocationManager.BUNDLE_DATA_FILE);
		// the cache and the state match
		if (timeStamp == stateManager.getSystemState().getTimeStamp() && metadata.isFile())
			return;
		try {
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(metadata)));
			try {
				out.write(BUNDLEDATA_VERSION);
				out.writeLong(stateManager.getSystemState().getTimeStamp());
				out.writeUTF(installURL);
				out.writeInt(initialBundleStartLevel);
				out.writeLong(nextId);
				Bundle[] bundles = context.getBundles();
				out.writeInt(bundles.length);
				for (int i = 0; i < bundles.length; i++) {
					long id = bundles[i].getBundleId();
					out.writeLong(id);
					if (id != 0) {
						BundleData data = ((org.eclipse.osgi.framework.internal.core.AbstractBundle) bundles[i]).getBundleData();
						saveMetaDataFor(data, out);
					}
				}
			} finally {
				out.close();
			}
		} catch (IOException e) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Error writing framework metadata: " + e.getMessage()); //$NON-NLS-1$ 
				Debug.printStackTrace(e);
			}
		}
	}

	public BundleWatcher getBundleWatcher() {
		return StatsManager.getDefault();
	}

	protected BundleContext getContext() {
		return context;
	}

	public void frameworkStopping(BundleContext context) {
		super.frameworkStopping(context);
		new BundleStopper().stopBundles();
	}

	protected void setLog(FrameworkLog log) {
		frameworkLog = log;
	}
}