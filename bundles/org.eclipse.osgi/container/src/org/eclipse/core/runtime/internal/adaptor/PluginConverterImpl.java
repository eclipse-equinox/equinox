/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
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
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.adaptor.LocationManager;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.baseadaptor.DevClassPathHelper;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;

/**
 * Internal class.
 */
public class PluginConverterImpl implements PluginConverter {
	public static boolean DEBUG = false;
	/** bundle manifest type unknown */
	static public final byte MANIFEST_TYPE_UNKNOWN = 0x00;
	/** bundle manifest type bundle (META-INF/MANIFEST.MF) */
	static public final byte MANIFEST_TYPE_BUNDLE = 0x01;
	/** bundle manifest type plugin (plugin.xml) */
	static public final byte MANIFEST_TYPE_PLUGIN = 0x02;
	/** bundle manifest type fragment (fragment.xml) */
	static public final byte MANIFEST_TYPE_FRAGMENT = 0x04;
	/** bundle manifest type jared bundle */
	static public final byte MANIFEST_TYPE_JAR = 0x08;
	private static final String SEMICOLON = "; "; //$NON-NLS-1$
	private static final String UTF_8 = "UTF-8"; //$NON-NLS-1$
	private static final String LIST_SEPARATOR = ",\n "; //$NON-NLS-1$
	private static final String LINE_SEPARATOR = "\n "; //$NON-NLS-1$
	private static final String DOT = "."; //$NON-NLS-1$
	private static int MAXLINE = 511;
	private BundleContext context;
	private FrameworkAdaptor adaptor;
	private BufferedWriter out;
	private IPluginInfo pluginInfo;
	private File pluginManifestLocation;
	private ZipFile pluginZip;
	private Dictionary<String, String> generatedManifest;
	private byte manifestType;
	private Version target;
	private Dictionary<String, String> devProperties;
	static final Version TARGET31 = new Version(3, 1, 0);
	static final Version TARGET32 = new Version(3, 2, 0);
	private static final String MANIFEST_VERSION = "Manifest-Version"; //$NON-NLS-1$
	private static final String PLUGIN_PROPERTIES_FILENAME = "plugin"; //$NON-NLS-1$
	private static PluginConverterImpl instance;
	@SuppressWarnings("deprecation")
	private static final String[] ARCH_LIST = {org.eclipse.osgi.service.environment.Constants.ARCH_PA_RISC, org.eclipse.osgi.service.environment.Constants.ARCH_PPC, org.eclipse.osgi.service.environment.Constants.ARCH_SPARC, org.eclipse.osgi.service.environment.Constants.ARCH_X86, org.eclipse.osgi.service.environment.Constants.ARCH_AMD64, org.eclipse.osgi.service.environment.Constants.ARCH_IA64};
	static public final String FRAGMENT_MANIFEST = "fragment.xml"; //$NON-NLS-1$
	static public final String GENERATED_FROM = "Generated-from"; //$NON-NLS-1$
	static public final String MANIFEST_TYPE_ATTRIBUTE = "type"; //$NON-NLS-1$
	private static final String[] OS_LIST = {org.eclipse.osgi.service.environment.Constants.OS_AIX, org.eclipse.osgi.service.environment.Constants.OS_HPUX, org.eclipse.osgi.service.environment.Constants.OS_LINUX, org.eclipse.osgi.service.environment.Constants.OS_MACOSX, org.eclipse.osgi.service.environment.Constants.OS_QNX, org.eclipse.osgi.service.environment.Constants.OS_SOLARIS, org.eclipse.osgi.service.environment.Constants.OS_WIN32};
	protected static final String PI_RUNTIME = "org.eclipse.core.runtime"; //$NON-NLS-1$
	protected static final String PI_BOOT = "org.eclipse.core.boot"; //$NON-NLS-1$
	protected static final String PI_RUNTIME_COMPATIBILITY = "org.eclipse.core.runtime.compatibility"; //$NON-NLS-1$
	static public final String PLUGIN_MANIFEST = "plugin.xml"; //$NON-NLS-1$
	private static final String COMPATIBILITY_ACTIVATOR = "org.eclipse.core.internal.compatibility.PluginActivator"; //$NON-NLS-1$
	private static final String[] WS_LIST = {org.eclipse.osgi.service.environment.Constants.WS_CARBON, org.eclipse.osgi.service.environment.Constants.WS_GTK, org.eclipse.osgi.service.environment.Constants.WS_MOTIF, org.eclipse.osgi.service.environment.Constants.WS_PHOTON, org.eclipse.osgi.service.environment.Constants.WS_WIN32};
	private static final String IGNORE_DOT = "@ignoredot@"; //$NON-NLS-1$

	public static PluginConverterImpl getDefault() {
		return instance;
	}

	public PluginConverterImpl(FrameworkAdaptor adaptor, BundleContext context) {
		this.context = context;
		this.adaptor = adaptor;
		instance = this;
	}

	private void init() {
		// need to make sure these fields are cleared out for each conversion.
		out = null;
		pluginInfo = null;
		pluginManifestLocation = null;
		pluginZip = null;
		generatedManifest = new Hashtable<String, String>(10);
		manifestType = MANIFEST_TYPE_UNKNOWN;
		target = null;
		devProperties = null;
	}

	private void fillPluginInfo(File pluginBaseLocation) throws PluginConversionException {
		pluginManifestLocation = pluginBaseLocation;
		if (pluginManifestLocation == null)
			throw new IllegalArgumentException();
		InputStream pluginFile = null;
		try {
			try {
				pluginFile = findPluginManifest(pluginBaseLocation);
			} catch (IOException e) {
				throw new PluginConversionException(NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONVERTER_FILENOTFOUND, pluginBaseLocation.getAbsolutePath()), e);
			}
			if (pluginFile == null)
				throw new PluginConversionException(NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONVERTER_FILENOTFOUND, pluginBaseLocation.getAbsolutePath()));
			pluginInfo = parsePluginInfo(pluginFile);
		} finally {
			if (pluginZip != null)
				try {
					pluginZip.close();
					pluginZip = null;
				} catch (IOException e) {
					// ignore
				}
		}
		String validation = pluginInfo.validateForm();
		if (validation != null)
			throw new PluginConversionException(validation);
	}

	private Set<String> filterExport(Set<String> exportToFilter, Collection<String> filter) {
		if (filter == null || filter.contains("*")) //$NON-NLS-1$
			return exportToFilter;
		Set<String> filteredExport = new HashSet<String>(exportToFilter.size());
		for (String anExport : exportToFilter) {
			for (String aFilter : filter) {
				int dotStar = aFilter.indexOf(".*"); //$NON-NLS-1$
				if (dotStar != -1)
					aFilter = aFilter.substring(0, dotStar);
				if (anExport.equals(aFilter)) {
					filteredExport.add(anExport);
					break;
				}
			}
		}
		return filteredExport;
	}

	private List<String> findOSJars(File pluginRoot, String path, boolean filter) {
		path = path.substring(4);
		List<String> found = new ArrayList<String>(0);
		for (int i = 0; i < OS_LIST.length; i++) {
			//look for os/osname/path
			String searchedPath = "os/" + OS_LIST[i] + "/" + path; //$NON-NLS-1$ //$NON-NLS-2$
			if (new File(pluginRoot, searchedPath).exists())
				found.add(searchedPath + (filter ? ";(os=" + WS_LIST[i] + ")" : "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			//look for os/osname/archname/path
			for (int j = 0; j < ARCH_LIST.length; j++) {
				searchedPath = "os/" + OS_LIST[i] + "/" + ARCH_LIST[j] + "/" + path; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (new File(pluginRoot, searchedPath).exists()) {
					found.add(searchedPath + (filter ? ";(& (os=" + WS_LIST[i] + ") (arch=" + ARCH_LIST[j] + ")" : "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
			}
		}
		return found;
	}

	private InputStream findPluginManifest(File baseLocation) throws IOException {
		//Here, we can not use the bundlefile because it may explode the jar and returns a location from which we will not be able to derive the jars location 
		if (pluginZip != null)
			try {
				pluginZip.close();
			} catch (IOException e) {
				// ignore
			}
		pluginZip = null;
		if (!baseLocation.isDirectory()) {
			manifestType |= MANIFEST_TYPE_JAR;
			pluginZip = new ZipFile(baseLocation);
		}

		if (pluginZip != null) {
			ZipEntry manifestEntry = pluginZip.getEntry(PLUGIN_MANIFEST);
			if (manifestEntry != null) {
				manifestType |= MANIFEST_TYPE_PLUGIN;
				return pluginZip.getInputStream(manifestEntry);
			}
		} else {
			File manifestFile = new File(baseLocation, PLUGIN_MANIFEST);
			if (manifestFile.exists()) {
				manifestType |= MANIFEST_TYPE_PLUGIN;
				return new FileInputStream(manifestFile);
			}
		}

		if (pluginZip != null) {
			ZipEntry manifestEntry = pluginZip.getEntry(FRAGMENT_MANIFEST);
			if (manifestEntry != null) {
				manifestType |= MANIFEST_TYPE_PLUGIN;
				return pluginZip.getInputStream(manifestEntry);
			}
		} else {
			File manifestFile = new File(baseLocation, FRAGMENT_MANIFEST);
			if (manifestFile.exists()) {
				manifestType |= MANIFEST_TYPE_FRAGMENT;
				return new FileInputStream(manifestFile);
			}
		}

		return null;
	}

	private List<String> findWSJars(File pluginRoot, String path, boolean filter) {
		path = path.substring(4);
		List<String> found = new ArrayList<String>(0);
		for (int i = 0; i < WS_LIST.length; i++) {
			String searchedPath = "ws/" + WS_LIST[i] + path; //$NON-NLS-1$
			if (new File(pluginRoot, searchedPath).exists()) {
				found.add(searchedPath + (filter ? ";(ws=" + WS_LIST[i] + ")" : "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		return found;
	}

	protected void fillManifest(boolean compatibilityManifest, boolean analyseJars) {
		generateManifestVersion();
		generateHeaders();
		generateClasspath();
		generateActivator();
		generatePluginClass();
		if (analyseJars)
			generateProvidePackage();
		generateRequireBundle();
		generateLocalizationEntry();
		generateEclipseHeaders();
		if (compatibilityManifest) {
			generateTimestamp();
		}
	}

	@SuppressWarnings("deprecation")
	public void writeManifest(File generationLocation, Dictionary<String, String> manifestToWrite, boolean compatibilityManifest) throws PluginConversionException {
		long start = System.currentTimeMillis();
		try {
			File parentFile = new File(generationLocation.getParent());
			parentFile.mkdirs();
			generationLocation.createNewFile();
			if (!generationLocation.isFile()) {
				String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONVERTER_ERROR_CREATING_BUNDLE_MANIFEST, this.pluginInfo.getUniqueId(), generationLocation);
				throw new PluginConversionException(message);
			}
			// replaces any eventual existing file
			manifestToWrite = new Hashtable<String, String>((Hashtable) manifestToWrite);
			// MANIFEST.MF files must be written using UTF-8
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(generationLocation), UTF_8));
			writeEntry(MANIFEST_VERSION, manifestToWrite.remove(MANIFEST_VERSION));
			writeEntry(GENERATED_FROM, manifestToWrite.remove(GENERATED_FROM)); //Need to do this first uptoDate check expect the generated-from tag to be in the first line
			// always attempt to write the Bundle-ManifestVersion header if it exists (bug 109863)
			writeEntry(Constants.BUNDLE_MANIFESTVERSION, manifestToWrite.remove(Constants.BUNDLE_MANIFESTVERSION));
			writeEntry(Constants.BUNDLE_NAME, manifestToWrite.remove(Constants.BUNDLE_NAME));
			writeEntry(Constants.BUNDLE_SYMBOLICNAME, manifestToWrite.remove(Constants.BUNDLE_SYMBOLICNAME));
			writeEntry(Constants.BUNDLE_VERSION, manifestToWrite.remove(Constants.BUNDLE_VERSION));
			writeEntry(Constants.BUNDLE_CLASSPATH, manifestToWrite.remove(Constants.BUNDLE_CLASSPATH));
			writeEntry(Constants.BUNDLE_ACTIVATOR, manifestToWrite.remove(Constants.BUNDLE_ACTIVATOR));
			writeEntry(Constants.BUNDLE_VENDOR, manifestToWrite.remove(Constants.BUNDLE_VENDOR));
			writeEntry(Constants.FRAGMENT_HOST, manifestToWrite.remove(Constants.FRAGMENT_HOST));
			writeEntry(Constants.BUNDLE_LOCALIZATION, manifestToWrite.remove(Constants.BUNDLE_LOCALIZATION));
			// always attempt to write the Export-Package header if it exists (bug 109863)
			writeEntry(Constants.EXPORT_PACKAGE, manifestToWrite.remove(Constants.EXPORT_PACKAGE));
			// always attempt to write the Provide-Package header if it exists (bug 109863)
			writeEntry(Constants.PROVIDE_PACKAGE, manifestToWrite.remove(Constants.PROVIDE_PACKAGE));
			writeEntry(Constants.REQUIRE_BUNDLE, manifestToWrite.remove(Constants.REQUIRE_BUNDLE));
			Enumeration<String> keys = manifestToWrite.keys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				writeEntry(key, manifestToWrite.get(key));
			}
			out.flush();
		} catch (IOException e) {
			String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONVERTER_ERROR_CREATING_BUNDLE_MANIFEST, this.pluginInfo.getUniqueId(), generationLocation);
			throw new PluginConversionException(message, e);
		} finally {
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					// only report problems writing to/flushing the file
				}
		}
		if (DEBUG)
			System.out.println("Time to write out converted manifest to: " + generationLocation + ": " + (System.currentTimeMillis() - start) + "ms."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private void generateLocalizationEntry() {
		generatedManifest.put(Constants.BUNDLE_LOCALIZATION, PLUGIN_PROPERTIES_FILENAME);
	}

	private void generateManifestVersion() {
		generatedManifest.put(MANIFEST_VERSION, "1.0"); //$NON-NLS-1$ 
	}

	private boolean requireRuntimeCompatibility() {
		ArrayList<PluginParser.Prerequisite> requireList = pluginInfo.getRequires();
		for (Iterator<PluginParser.Prerequisite> iter = requireList.iterator(); iter.hasNext();) {
			if (iter.next().getName().equalsIgnoreCase(PI_RUNTIME_COMPATIBILITY))
				return true;
		}
		return false;
	}

	private void generateActivator() {
		if (!pluginInfo.isFragment())
			if (!requireRuntimeCompatibility()) {
				String pluginClass = pluginInfo.getPluginClass();
				if (pluginClass != null && !pluginClass.trim().equals("")) //$NON-NLS-1$
					generatedManifest.put(Constants.BUNDLE_ACTIVATOR, pluginClass);
			} else {
				generatedManifest.put(Constants.BUNDLE_ACTIVATOR, COMPATIBILITY_ACTIVATOR);
			}
	}

	private void generateClasspath() {
		String[] classpath = pluginInfo.getLibrariesName();
		if (classpath.length != 0)
			generatedManifest.put(Constants.BUNDLE_CLASSPATH, getStringFromArray(classpath, LIST_SEPARATOR));
	}

	private void generateHeaders() {
		if (TARGET31.compareTo(target) <= 0)
			generatedManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		generatedManifest.put(Constants.BUNDLE_NAME, pluginInfo.getPluginName());
		generatedManifest.put(Constants.BUNDLE_VERSION, pluginInfo.getVersion());
		generatedManifest.put(Constants.BUNDLE_SYMBOLICNAME, getSymbolicNameEntry());
		String provider = pluginInfo.getProviderName();
		if (provider != null)
			generatedManifest.put(Constants.BUNDLE_VENDOR, provider);
		if (pluginInfo.isFragment()) {
			StringBuffer hostBundle = new StringBuffer();
			hostBundle.append(pluginInfo.getMasterId());
			String versionRange = getVersionRange(pluginInfo.getMasterVersion(), pluginInfo.getMasterMatch()); // TODO need to get match rule here!
			if (versionRange != null)
				hostBundle.append(versionRange);
			generatedManifest.put(Constants.FRAGMENT_HOST, hostBundle.toString());
		}
	}

	/*
	 * Generates an entry in the form: 
	 * 	<symbolic-name>[; singleton=true]
	 */
	private String getSymbolicNameEntry() {
		// false is the default, so don't bother adding anything 
		if (!pluginInfo.isSingleton())
			return pluginInfo.getUniqueId();
		StringBuffer result = new StringBuffer(pluginInfo.getUniqueId());
		result.append(SEMICOLON);
		result.append(Constants.SINGLETON_DIRECTIVE);
		String assignment = TARGET31.compareTo(target) <= 0 ? ":=" : "="; //$NON-NLS-1$ //$NON-NLS-2$
		result.append(assignment).append("true"); //$NON-NLS-1$
		return result.toString();
	}

	private void generatePluginClass() {
		if (requireRuntimeCompatibility()) {
			String pluginClass = pluginInfo.getPluginClass();
			if (pluginClass != null)
				generatedManifest.put(Constants.PLUGIN_CLASS, pluginClass);
		}
	}

	@SuppressWarnings("deprecation")
	private void generateProvidePackage() {
		Set<String> exports = getExports();
		if (exports != null && exports.size() != 0) {
			generatedManifest.put(TARGET31.compareTo(target) <= 0 ? Constants.EXPORT_PACKAGE : Constants.PROVIDE_PACKAGE, getStringFromCollection(exports, LIST_SEPARATOR));
		}
	}

	@SuppressWarnings("deprecation")
	private void generateRequireBundle() {
		ArrayList<PluginParser.Prerequisite> requiredBundles = pluginInfo.getRequires();
		if (requiredBundles.size() == 0)
			return;
		StringBuffer bundleRequire = new StringBuffer();
		for (Iterator<PluginParser.Prerequisite> iter = requiredBundles.iterator(); iter.hasNext();) {
			PluginParser.Prerequisite element = iter.next();
			StringBuffer modImport = new StringBuffer(element.getName());
			String versionRange = getVersionRange(element.getVersion(), element.getMatch());
			if (versionRange != null)
				modImport.append(versionRange);
			if (element.isExported()) {
				if (TARGET31.compareTo(target) <= 0)
					modImport.append(';').append(Constants.VISIBILITY_DIRECTIVE).append(":=").append(Constants.VISIBILITY_REEXPORT);//$NON-NLS-1$
				else
					modImport.append(';').append(Constants.REPROVIDE_ATTRIBUTE).append("=true");//$NON-NLS-1$
			}
			if (element.isOptional()) {
				if (TARGET31.compareTo(target) <= 0)
					modImport.append(';').append(Constants.RESOLUTION_DIRECTIVE).append(":=").append(Constants.RESOLUTION_OPTIONAL);//$NON-NLS-1$
				else
					modImport.append(';').append(Constants.OPTIONAL_ATTRIBUTE).append("=true");//$NON-NLS-1$
			}
			bundleRequire.append(modImport.toString());
			if (iter.hasNext())
				bundleRequire.append(LIST_SEPARATOR);
		}
		generatedManifest.put(Constants.REQUIRE_BUNDLE, bundleRequire.toString());
	}

	private void generateTimestamp() {
		// so it is easy to tell which ones are generated
		generatedManifest.put(GENERATED_FROM, Long.toString(getTimeStamp(pluginManifestLocation, manifestType)) + ";" + MANIFEST_TYPE_ATTRIBUTE + "=" + manifestType); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@SuppressWarnings("deprecation")
	private void generateEclipseHeaders() {
		if (pluginInfo.isFragment())
			return;

		String pluginClass = pluginInfo.getPluginClass();
		if (pluginInfo.hasExtensionExtensionPoints() || (pluginClass != null && !pluginClass.trim().equals(""))) //$NON-NLS-1$
			generatedManifest.put(TARGET32.compareTo(target) <= 0 ? Constants.ECLIPSE_LAZYSTART : Constants.ECLIPSE_AUTOSTART, "true"); //$NON-NLS-1$
	}

	private Set<String> getExports() {
		Map<String, List<String>> libs = pluginInfo.getLibraries();
		if (libs == null)
			return null;

		//If we are in dev mode, then add the binary folders on the list libs with the export clause set to be the cumulation of the export clause of the real libs   
		if (devProperties != null || DevClassPathHelper.inDevelopmentMode()) {
			String[] devClassPath = DevClassPathHelper.getDevClassPath(pluginInfo.getUniqueId(), devProperties);
			// collect export clauses
			List<String> allExportClauses = new ArrayList<String>(libs.size());
			Set<Map.Entry<String, List<String>>> libEntries = libs.entrySet();
			for (Iterator<Map.Entry<String, List<String>>> iter = libEntries.iterator(); iter.hasNext();) {
				Map.Entry<String, List<String>> element = iter.next();
				allExportClauses.addAll(element.getValue());
			}
			if (devClassPath != null) {
				// bug 88498
				// if there is a devClassPath defined for this plugin and the @ignoredot@ flag is true
				// then we will ignore the '.' library specified in the plugin.xml
				String[] ignoreDotProp = DevClassPathHelper.getDevClassPath(IGNORE_DOT, devProperties);
				if (devClassPath.length > 0 && ignoreDotProp != null && ignoreDotProp.length > 0 && "true".equals(ignoreDotProp[0])) //$NON-NLS-1$
					libs.remove(DOT);
				for (int i = 0; i < devClassPath.length; i++)
					libs.put(devClassPath[i], allExportClauses);
			}
		}

		Set<String> result = new TreeSet<String>();
		Set<Map.Entry<String, List<String>>> libEntries = libs.entrySet();
		for (Iterator<Map.Entry<String, List<String>>> iter = libEntries.iterator(); iter.hasNext();) {
			Map.Entry<String, List<String>> element = iter.next();
			List<String> filter = element.getValue();
			if (filter.size() == 0) //If the library is not exported, then ignore it
				continue;
			String libEntryText = element.getKey().trim();
			File libraryLocation;
			if (libEntryText.equals(DOT))
				libraryLocation = pluginManifestLocation;
			else {
				// in development time, libEntries may contain absolute locations (linked folders)				
				File libEntryAsPath = new File(libEntryText);
				libraryLocation = libEntryAsPath.isAbsolute() ? libEntryAsPath : new File(pluginManifestLocation, libEntryText);
			}
			Set<String> exports = null;
			if (libraryLocation.exists()) {
				if (libraryLocation.isFile())
					exports = filterExport(getExportsFromJAR(libraryLocation), filter); //TODO Need to handle $xx$ variables
				else if (libraryLocation.isDirectory())
					exports = filterExport(getExportsFromDir(libraryLocation), filter);
			} else {
				List<String> expandedLibs = getLibrariesExpandingVariables(element.getKey(), false);
				exports = new HashSet<String>();
				for (Iterator<String> iterator = expandedLibs.iterator(); iterator.hasNext();) {
					String libName = iterator.next();
					File libFile = new File(pluginManifestLocation, libName);
					if (libFile.isFile()) {
						exports.addAll(filterExport(getExportsFromJAR(libFile), filter));
					}
				}
			}
			if (exports != null)
				result.addAll(exports);
		}
		return result;
	}

	private Set<String> getExportsFromDir(File location) {
		return getExportsFromDir(location, ""); //$NON-NLS-1$
	}

	private Set<String> getExportsFromDir(File location, String packageName) {
		String prefix = (packageName.length() > 0) ? (packageName + '.') : ""; //$NON-NLS-1$
		String[] files = location.list();
		Set<String> exportedPaths = new HashSet<String>();
		boolean containsFile = false;
		if (files != null)
			for (int i = 0; i < files.length; i++) {
				if (!isValidPackageName(files[i]))
					continue;
				File pkgFile = new File(location, files[i]);
				if (pkgFile.isDirectory())
					exportedPaths.addAll(getExportsFromDir(pkgFile, prefix + files[i]));
				else
					containsFile = true;
			}
		if (containsFile)
			// Allow the default package to be provided.  If the default package
			// contains a File then use "." as the package name to provide for default.
			if (packageName.length() > 0)
				exportedPaths.add(packageName);
			else
				exportedPaths.add(DOT);
		return exportedPaths;
	}

	private Set<String> getExportsFromJAR(File jarFile) {
		Set<String> names = new HashSet<String>();
		ZipFile file = null;
		try {
			file = new ZipFile(jarFile);
		} catch (IOException e) {
			String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONVERTER_PLUGIN_LIBRARY_IGNORED, jarFile, pluginInfo.getUniqueId());
			adaptor.getFrameworkLog().log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, message, 0, e, null));
			return names;
		}
		//Run through the entries
		for (Enumeration<? extends ZipEntry> entriesEnum = file.entries(); entriesEnum.hasMoreElements();) {
			ZipEntry entry = entriesEnum.nextElement();
			String name = entry.getName();
			if (!isValidPackageName(name))
				continue;
			int lastSlash = name.lastIndexOf("/"); //$NON-NLS-1$
			//Ignore folders that do not contain files
			if (lastSlash != -1) {
				if (lastSlash != name.length() - 1 && name.lastIndexOf(' ') == -1)
					names.add(name.substring(0, lastSlash).replace('/', '.'));
			} else {
				// Allow the default package to be provided.  If the default package
				// contains a File then use "." as the package name to provide for default.
				names.add(DOT);
			}
		}
		try {
			file.close();
		} catch (IOException e) {
			// Nothing to do
		}
		return names;
	}

	private List<String> getLibrariesExpandingVariables(String libraryPath, boolean filter) {
		String var = hasPrefix(libraryPath);
		if (var == null) {
			List<String> returnValue = new ArrayList<String>(1);
			returnValue.add(libraryPath);
			return returnValue;
		}
		if (var.equals("ws")) { //$NON-NLS-1$
			return findWSJars(pluginManifestLocation, libraryPath, filter);
		}
		if (var.equals("os")) { //$NON-NLS-1$
			return findOSJars(pluginManifestLocation, libraryPath, filter);
		}
		return new ArrayList<String>(0);
	}

	//return a String representing the string found between the $s
	private String hasPrefix(String libPath) {
		if (libPath.startsWith("$ws$")) //$NON-NLS-1$
			return "ws"; //$NON-NLS-1$
		if (libPath.startsWith("$os$")) //$NON-NLS-1$
			return "os"; //$NON-NLS-1$
		if (libPath.startsWith("$nl$")) //$NON-NLS-1$
			return "nl"; //$NON-NLS-1$
		return null;
	}

	private boolean isValidPackageName(String name) {
		if (name.indexOf(' ') > 0 || name.equalsIgnoreCase("META-INF") || name.startsWith("META-INF/")) //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		return true;
	}

	/**
	 * Parses the plugin manifest to find out: - the plug-in unique identifier -
	 * the plug-in version - runtime/libraries entries - the plug-in class -
	 * the master plugin (for a fragment)
	 */
	private IPluginInfo parsePluginInfo(InputStream pluginLocation) throws PluginConversionException {
		InputStream input = null;
		try {
			input = new BufferedInputStream(pluginLocation);
			return new PluginParser(adaptor, context, target).parsePlugin(input);
		} catch (Exception e) {
			String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONVERTER_ERROR_PARSING_PLUGIN_MANIFEST, pluginManifestLocation);
			throw new PluginConversionException(message, e);
		} finally {
			if (input != null)
				try {
					input.close();
				} catch (IOException e) {
					//ignore exception
				}
		}
	}

	public static boolean upToDate(File generationLocation, File pluginLocation, byte manifestType) {
		if (!generationLocation.isFile())
			return false;
		String secondLine = null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(generationLocation)));
			reader.readLine();
			secondLine = reader.readLine();
		} catch (IOException e) {
			// not a big deal - we could not read an existing manifest
			return false;
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					// ignore
				}
		}
		String tag = GENERATED_FROM + ": "; //$NON-NLS-1$
		if (secondLine == null || !secondLine.startsWith(tag))
			return false;

		secondLine = secondLine.substring(tag.length());
		ManifestElement generatedFrom;
		try {
			generatedFrom = ManifestElement.parseHeader(PluginConverterImpl.GENERATED_FROM, secondLine)[0];
		} catch (BundleException be) {
			return false;
		}
		String timestampStr = generatedFrom.getValue();
		try {
			return Long.parseLong(timestampStr.trim()) == getTimeStamp(pluginLocation, manifestType);
		} catch (NumberFormatException nfe) {
			// not a big deal - just a bogus existing manifest that will be ignored
		}
		return false;
	}

	public static long getTimeStamp(File pluginLocation, byte manifestType) {
		if ((manifestType & MANIFEST_TYPE_JAR) != 0)
			return pluginLocation.lastModified();
		else if ((manifestType & MANIFEST_TYPE_PLUGIN) != 0)
			return new File(pluginLocation, PLUGIN_MANIFEST).lastModified();
		else if ((manifestType & MANIFEST_TYPE_FRAGMENT) != 0)
			return new File(pluginLocation, FRAGMENT_MANIFEST).lastModified();
		else if ((manifestType & MANIFEST_TYPE_BUNDLE) != 0)
			return new File(pluginLocation, Constants.OSGI_BUNDLE_MANIFEST).lastModified();
		return -1;
	}

	private void writeEntry(String key, String value) throws IOException {
		if (value != null && value.length() > 0) {
			out.write(splitOnComma(key + ": " + value)); //$NON-NLS-1$
			out.write('\n');
		}
	}

	private String splitOnComma(String value) {
		if (value.length() < MAXLINE || value.indexOf(LINE_SEPARATOR) >= 0)
			return value; // assume the line is already split
		String[] values = ManifestElement.getArrayFromList(value);
		if (values == null || values.length == 0)
			return value;
		StringBuffer sb = new StringBuffer(value.length() + ((values.length - 1) * LIST_SEPARATOR.length()));
		for (int i = 0; i < values.length - 1; i++)
			sb.append(values[i]).append(LIST_SEPARATOR);
		sb.append(values[values.length - 1]);
		return sb.toString();
	}

	private String getStringFromArray(String[] values, String separator) {
		if (values == null)
			return ""; //$NON-NLS-1$
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < values.length; i++) {
			if (i > 0)
				result.append(separator);
			result.append(values[i]);
		}
		return result.toString();
	}

	private String getStringFromCollection(Collection<String> collection, String separator) {
		StringBuffer result = new StringBuffer();
		boolean first = true;
		for (Iterator<String> i = collection.iterator(); i.hasNext();) {
			if (first)
				first = false;
			else
				result.append(separator);
			result.append(i.next());
		}
		return result.toString();
	}

	public synchronized Dictionary<String, String> convertManifest(File pluginBaseLocation, boolean compatibility, String targetVersion, boolean analyseJars, Dictionary<String, String> devProps) throws PluginConversionException {
		long start = System.currentTimeMillis();
		if (DEBUG)
			System.out.println("Convert " + pluginBaseLocation); //$NON-NLS-1$
		init();
		this.target = targetVersion == null ? TARGET32 : new Version(targetVersion);
		this.devProperties = devProps;
		fillPluginInfo(pluginBaseLocation);
		fillManifest(compatibility, analyseJars);
		if (DEBUG)
			System.out.println("Time to convert manifest for: " + pluginBaseLocation + ": " + (System.currentTimeMillis() - start) + "ms."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return generatedManifest;
	}

	public synchronized File convertManifest(File pluginBaseLocation, File bundleManifestLocation, boolean compatibilityManifest, String targetVersion, boolean analyseJars, Dictionary<String, String> devProps) throws PluginConversionException {
		convertManifest(pluginBaseLocation, compatibilityManifest, targetVersion, analyseJars, devProps);
		if (bundleManifestLocation == null) {
			String cacheLocation = FrameworkProperties.getProperty(LocationManager.PROP_MANIFEST_CACHE);
			bundleManifestLocation = new File(cacheLocation, pluginInfo.getUniqueId() + '_' + pluginInfo.getVersion() + ".MF"); //$NON-NLS-1$
		}
		if (upToDate(bundleManifestLocation, pluginManifestLocation, manifestType))
			return bundleManifestLocation;
		writeManifest(bundleManifestLocation, generatedManifest, compatibilityManifest);
		return bundleManifestLocation;
	}

	private String getVersionRange(String reqVersion, String matchRule) {
		if (reqVersion == null)
			return null;

		Version minVersion = Version.parseVersion(reqVersion);
		String versionRange;
		if (matchRule != null) {
			if (matchRule.equalsIgnoreCase(IModel.PLUGIN_REQUIRES_MATCH_PERFECT)) {
				versionRange = new VersionRange(minVersion, true, minVersion, true).toString();
			} else if (matchRule.equalsIgnoreCase(IModel.PLUGIN_REQUIRES_MATCH_EQUIVALENT)) {
				versionRange = new VersionRange(minVersion, true, new Version(minVersion.getMajor(), minVersion.getMinor() + 1, 0, ""), false).toString(); //$NON-NLS-1$
			} else if (matchRule.equalsIgnoreCase(IModel.PLUGIN_REQUIRES_MATCH_COMPATIBLE)) {
				versionRange = new VersionRange(minVersion, true, new Version(minVersion.getMajor() + 1, 0, 0, ""), false).toString(); //$NON-NLS-1$
			} else if (matchRule.equalsIgnoreCase(IModel.PLUGIN_REQUIRES_MATCH_GREATER_OR_EQUAL)) {
				// just return the reqVersion here without any version range
				versionRange = reqVersion;
			} else {
				versionRange = new VersionRange(minVersion, true, new Version(minVersion.getMajor() + 1, 0, 0, ""), false).toString(); //$NON-NLS-1$
			}
		} else {
			versionRange = new VersionRange(minVersion, true, new Version(minVersion.getMajor() + 1, 0, 0, ""), false).toString(); //$NON-NLS-1$
		}

		StringBuffer result = new StringBuffer();
		result.append(';').append(Constants.BUNDLE_VERSION_ATTRIBUTE).append('=');
		result.append('\"').append(versionRange).append('\"');
		return result.toString();
	}
}
