/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import javax.xml.parsers.SAXParserFactory;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.adaptor.Version;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.debug.DebugOptions;
import org.eclipse.osgi.framework.internal.defaultadaptor.*;
import org.eclipse.osgi.framework.stats.StatsManager;
import org.eclipse.osgi.internal.resolver.StateImpl;
import org.eclipse.osgi.internal.resolver.StateManager;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.*;

public class EclipseAdaptor extends DefaultAdaptor {
	public static boolean MONITOR_BUNDLES = false;
	public static boolean MONITOR_CLASSES = false;
	public static boolean MONITOR_RESOURCE_BUNDLES = false;
	public static String TRACE_FILENAME = "runtime.traces"; //$NON-NLS-1$
	public static String TRACE_FILTERS = "trace.properties"; //$NON-NLS-1$
	public static boolean TRACE_CLASSES = false;
	public static boolean TRACE_BUNDLES = false;

	private static final String RUNTIME_ADAPTOR = "org.eclipse.core.runtime.adaptor";

	//Option names for spies
	private static final String OPTION_MONITOR_BUNDLES = RUNTIME_ADAPTOR + "/monitor/bundles"; //$NON-NLS-1$
	private static final String OPTION_MONITOR_CLASSES = RUNTIME_ADAPTOR + "/monitor/classes"; //$NON-NLS-1$
	private static final String OPTION_MONITOR_RESOURCEBUNDLES = RUNTIME_ADAPTOR + "/monitor/resourcebundles"; //$NON-NLS-1$
	private static final String OPTION_TRACE_BUNDLES = RUNTIME_ADAPTOR + "/trace/bundleActivation"; //$NON-NLS-1$
	private static final String OPTION_TRACE_CLASSES = RUNTIME_ADAPTOR + "/trace/classLoading"; //$NON-NLS-1$
	private static final String OPTION_TRACE_FILENAME = RUNTIME_ADAPTOR + "/trace/filename"; //$NON-NLS-1$
	private static final String OPTION_TRACE_FILTERS = RUNTIME_ADAPTOR + "/trace/filters"; //$NON-NLS-1$
	private static final String OPTION_STATE_READER = RUNTIME_ADAPTOR + "/state/reader";//$NON-NLS-1$
	private static final String OPTION_RESOLVER = RUNTIME_ADAPTOR + "/resolver/timing"; //$NON-NLS-1$
	private static final String OPTION_RESOLVER_READER = RUNTIME_ADAPTOR + "/resolver/reader/timing"; //$NON-NLS-1$
	public static final byte BUNDLEDATA_VERSION = 3;
	public static final byte NULL = 0;
	public static final byte OBJECT = 1;

	private int startLevel = 1;
	private long timeStamp = 0;
	private String installURL = null; 

	public EclipseAdaptor(String[] args) {
		super(args);
		setDebugOptions();
	}
	
	protected StateManager createStateManager() {
		readHeaders();
		checkLocationAndReinitialize();
		stateManager = new StateManager(getStateBaseLocation(), timeStamp);
		StateImpl systemState = stateManager.getSystemState();
		if (systemState != null)
			return stateManager;
		systemState = stateManager.createSystemState();				
		Vector installedBundles = getInstalledBundles();
		if (installedBundles == null)
			return stateManager;
		StateObjectFactory factory = stateManager.getFactory();
		for (Iterator iter = installedBundles.iterator(); iter.hasNext(); ) {
			BundleData toAdd = (BundleData) iter.next();
			try {
				Dictionary manifest = toAdd.getManifest();
				BundleDescription newDescription = factory.createBundleDescription(manifest, toAdd.getLocation(),toAdd.getBundleID());
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

	private void checkLocationAndReinitialize() {
		if (timeStamp == 0) {
			installURL = EclipseStarter.getSysPath();
			return;
		}
		if( ! EclipseStarter.getSysPath().equals(installURL) ) {
			//delete the metadata file and the framework file when the location of the basic bundles has changed 
			getMetaDataLocation().delete();
			getMetaDataFile().delete();
			stateManager.getStateLocation().delete();
			installURL = EclipseStarter.getSysPath();
		}
	}
	
	private void readHeaders() {
		File metadata = getMetaDataLocation();
		if (!metadata.isFile())
			return;
		
		try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(metadata)));
			try {
				if (in.readByte() == BUNDLEDATA_VERSION) {
					timeStamp = in.readLong();
					installURL = in.readUTF();
					startLevel = in.readInt();
					nextId = in.readInt();
				}
			} finally {
				in.close();
			}
		} catch (IOException e) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Error reading framework metadata: " + e.getMessage());
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
		register(org.eclipse.osgi.service.environment.EnvironmentInfo.class.getName(), EnvironmentInfo.getDefault(), bundle);
		register(PlatformAdmin.class.getName(), stateManager, bundle);
		registerEndorsedXMLParser();
	}

	private void setDebugOptions() {
		DebugOptions options = DebugOptions.getDefault();
		// may be null if debugging is not enabled
		if (options == null)
			return;
		MONITOR_BUNDLES = options.getBooleanOption(OPTION_MONITOR_BUNDLES, false);
		org.eclipse.osgi.framework.internal.core.Bundle.MONITOR_BUNDLES = MONITOR_BUNDLES;
		MONITOR_CLASSES = options.getBooleanOption(OPTION_MONITOR_CLASSES, false);
		MONITOR_RESOURCE_BUNDLES = options.getBooleanOption(OPTION_MONITOR_RESOURCEBUNDLES, false);
		TRACE_CLASSES = options.getBooleanOption(OPTION_TRACE_CLASSES, false);
		TRACE_BUNDLES = options.getBooleanOption(OPTION_TRACE_BUNDLES, false);
		TRACE_FILENAME = options.getOption(OPTION_TRACE_FILENAME);
		TRACE_FILTERS = options.getOption(OPTION_TRACE_FILTERS);
		StateManager.DEBUG_READER = options.getBooleanOption(OPTION_RESOLVER_READER, false);
		StateImpl.DEBUG_RESOLVER = options.getBooleanOption(OPTION_RESOLVER, false);
	}

	private void registerEndorsedXMLParser() {
		if (!is14VMorGreater())
			return;
		new ParsingService();
	}
	private static boolean is14VMorGreater() {
		final String DELIM = ".";
		String vmVersionString = System.getProperty("java.version"); //$NON-NLS-1$
		StringTokenizer tokenizer = new StringTokenizer(vmVersionString, DELIM);
		int major, minor;
		// major
		if (tokenizer.hasMoreTokens()) {
			major = Integer.parseInt(tokenizer.nextToken());
			if (major > 1)
				return true;
		}

		// minor
		if (tokenizer.hasMoreTokens()) {
			minor = Integer.parseInt(tokenizer.nextToken());
			if (minor > 3)
				return true;
		}
		return false;
	}
	private class ParsingService implements ServiceFactory {
		public static final String SAXFACTORYNAME = "javax.xml.parsers.SAXParserFactory";

		public Object getService(Bundle bundle, ServiceRegistration registration) {
			return SAXParserFactory.newInstance();
		}

		public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
		}

		public ParsingService() {
			context.registerService(SAXFACTORYNAME, this, new Hashtable());
		}
	}
	public void frameworkStop(BundleContext context) throws BundleException {
		saveMetaData();
		super.frameworkStop(context);
		if (DebugOptions.getDefault() != null) {
			System.out.println("Time spent in registry parsing: " + DebugOptions.getDefault().getOption("org.eclipse.core.runtime/registry/parsing/timing/value"));
			System.out.println("Time spent in package admin resolve: " + DebugOptions.getDefault().getOption("debug.packageadmin/timing/value"));
			System.out.println("Time spent resolving the dependency system: " + DebugOptions.getDefault().getOption("org.eclipse.core.runtime.adaptor/resolver/timing/value"));
		}
	}

	private File getMetaDataLocation() {
		String configArea = (String) System.getProperty("osgi.configuration.area");
		return new File(configArea, ".bundledata");
	}
	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getInstalledBundles()
	 */
	public Vector getInstalledBundles() {
		File metadata = getMetaDataLocation();
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
				Vector result = new Vector(bundleCount);
				for (int i = 0; i < bundleCount; i++) {
					try {
						try {
							long id = in.readLong();
							if (id != 0) {
								EclipseBundleData data = (EclipseBundleData) getElementFactory().getBundleData(this);
								loadMetaDataFor(data, in);
								data.initializeExistingBundle(Long.toString(id));
								if (Debug.DEBUG && Debug.DEBUG_GENERAL)
									Debug.println("BundleData created: " + data);
								result.addElement(data);
							}
						} catch (NumberFormatException e) {
							// should never happen
						}
					} catch (IOException e) {
						if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
							Debug.println("Error reading framework metadata: " + e.getMessage());
							Debug.printStackTrace(e);
						}
					}
				}				
				return result;
			} finally {
				in.close();
			}
		} catch (IOException e) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Error reading framework metadata: " + e.getMessage());
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
		data.setName(readString(in, false));
		data.setUniqueId(readString(in, false));
		data.setVersion(new Version(readString(in, false)));
		data.setActivator(readString(in, false));
		data.setPluginClass(readString(in, false));
		data.setLegacy(readString(in, false));
		data.setClassPath(readString(in, false));
		data.setNativeCodePath(readString(in, false));
		data.setExecutionEnvironment(readString(in, false));
		data.setDynamicImports(readString(in, false));
		data.setGeneration(in.readInt());
		data.setStartLevel(in.readInt());
		data.setStatus(in.readInt());
		data.setReference(in.readBoolean());
		data.setFragment(in.readBoolean());
	}
	protected void saveMetaDataFor(BundleData data, DataOutputStream out) throws IOException {
		if (data.getBundleID() == 0 || !(data instanceof DefaultBundleData)) {
			out.writeByte(NULL);
			return;
		}
		EclipseBundleData bundleData = (EclipseBundleData) data;
		out.writeByte(OBJECT);
		writeStringOrNull(out, bundleData.getLocation());
		writeStringOrNull(out, bundleData.getName());
		writeStringOrNull(out, bundleData.getUniqueId());
		writeStringOrNull(out, bundleData.getVersion().toString());
		writeStringOrNull(out, bundleData.getActivator());
		writeStringOrNull(out, bundleData.getPluginClass());
		writeStringOrNull(out, bundleData.isLegacy());
		writeStringOrNull(out, bundleData.getClassPath());
		writeStringOrNull(out, bundleData.getNativeCodePath());
		writeStringOrNull(out, bundleData.getExecutionEnvironment());
		writeStringOrNull(out, bundleData.getDynamicImports());
		out.writeInt(bundleData.getGeneration());
		out.writeInt(bundleData.getStartLevel());
		out.writeInt(bundleData.getStatus());
		out.writeBoolean(bundleData.isReference());
		out.writeBoolean(bundleData.isFragment());
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

	public File getBundleRootDir() {
		super.getBundleDir();
		return bundleRootDir;
	}

	public String getDataDirName() {
		return dataDirName;
	}

	public void saveMetaData() {
		File metadata = getMetaDataLocation();
		// the cache and the state match
		if (timeStamp == stateManager.getSystemState().getTimeStamp() && metadata.isFile())
			return;
		try {
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(metadata)));
			try {
				out.write(BUNDLEDATA_VERSION);
				out.writeLong(stateManager.getSystemState().getTimeStamp());
				out.writeUTF(installURL);
				out.writeInt(startLevel);				
				out.writeLong(nextId);
				Bundle[] bundles = context.getBundles();
				out.writeInt(bundles.length);
				for (int i = 0; i < bundles.length; i++) {
					long id = bundles[i].getBundleId();
					out.writeLong(id);
					if (id != 0) {
						BundleData data = ((org.eclipse.osgi.framework.internal.core.Bundle) bundles[i]).getBundleData();
						saveMetaDataFor(data, out);
					}
				}
			} finally {
				out.close();
			}
		} catch (IOException e) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Error writing framework metadata: " + e.getMessage());
				Debug.printStackTrace(e);
			}
		}
	}

	public int getStartLevel() {
		return startLevel;
	}

	public void setStartLevel(int value) {
		startLevel = value;
	}

	public IBundleStats getBundleStats() {
		return StatsManager.getDefault();
	}
	
	protected File getStateBaseLocation() {
		return new File(System.getProperty("osgi.configuration.area"));
	}

	protected File getMetaDataFile() {
		File configAreaDirectory = getStateBaseLocation();
		if (!configAreaDirectory.exists())
			configAreaDirectory.mkdirs();
		return new File(configAreaDirectory, ".framework");
	}
}