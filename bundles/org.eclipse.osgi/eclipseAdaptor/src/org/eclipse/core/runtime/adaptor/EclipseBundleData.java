package org.eclipse.core.runtime.adaptor;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.osgi.framework.adaptor.Version;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.defaultadaptor.*;
import org.eclipse.osgi.framework.util.Headers;
import org.osgi.framework.BundleException;

public class EclipseBundleData extends DefaultBundleData {

	private URL base;
	private static String[] libraryVariants = null;

	/**
	 * At the first time will try to find the manifest in an alternative
	 * location if needed.
	 */
	private boolean firstManifestLookup = true;

	// URL protocol designations
	public static final String PROTOCOL = "platform"; //$NON-NLS-1$
	public static final String FILE = "file"; //$NON-NLS-1$
	protected String isLegacy = null;
	protected String pluginClass = null;

	private static String[] buildLibraryVariants() {
		ArrayList result = new ArrayList();
		EnvironmentInfo info = EnvironmentInfo.getDefault();
		result.add("ws/" + info.getWS() + "/"); //$NON-NLS-1$ //$NON-NLS-2$
		result.add("os/" + info.getOS() + "/" + info.getOSArch() + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		result.add("os/" + info.getOS() + "/"); //$NON-NLS-1$ //$NON-NLS-2$
		String nl = info.getNL();
		nl = nl.replace('_', '/');
		while (nl.length() > 0) {
			result.add("nl/" + nl + "/"); //$NON-NLS-1$ //$NON-NLS-2$
			int i = nl.lastIndexOf('/');
			nl = (i < 0) ? "" : nl.substring(0, i); //$NON-NLS-1$
		}
		result.add(""); //$NON-NLS-1$
		return (String[]) result.toArray(new String[result.size()]);
	}

	public EclipseBundleData(DefaultAdaptor adaptor) throws IOException {
		super(adaptor);
	}

	public void initializeNewBundle(long id, String location, InputStream in) throws IOException {
		this.id = id;
		this.location = location;
		this.name = adaptor.mapLocationToName(location);
		this.reference = false;

		dir = new File(((EclipseAdaptor)adaptor).getBundleRootDir(), String.valueOf(id));
		dirData = new File(dir, ((EclipseAdaptor)adaptor).getDataDirName());
		dirGeneration = new File(dir, String.valueOf(generation));
		file = new File(dirGeneration, name);
		setStartLevel(adaptor.getInitialBundleStartLevel());

		if (!getGenerationDir().exists())
		{
			throw new IOException(
					AdaptorMsg.formatter.getString(
							"ADAPTOR_DIRECTORY_CREATE_EXCEPTION",
							dirGeneration.getPath()));
		}
		DefaultAdaptor.readFile(in, file);
		bundleFile = BundleFile.createBundleFile(file, this);
		loadFromManifest();
		initializeBase(location);
	}

	public void initializeReferencedBundle(long id, String location, String name) throws IOException {
		this.id = id;
		this.location = location;
		this.name = name;
		this.reference = true;
	
		dir = new File(((EclipseAdaptor)adaptor).getBundleRootDir(), String.valueOf(id));
		dirData = new File(dir, ((EclipseAdaptor)adaptor).getDataDirName());
		dirGeneration = new File(dir, String.valueOf(generation));
		file = new File(name);
		setStartLevel(adaptor.getInitialBundleStartLevel());
		bundleFile = BundleFile.createBundleFile(file, this);
		loadFromManifest();
		// TODO it is unclear where the generation dir should be and what exactly will go in it
//		if (!getGenerationDir().exists())
//		{
//			throw new IOException(
//					AdaptorMsg.formatter.getString(
//							"ADAPTOR_DIRECTORY_CREATE_EXCEPTION",
//							dirGeneration.getPath()));
//		}
		initializeBase(location);
	}

	public void initializeExistingBundle(String directory) throws IOException {
		id = Long.parseLong(directory);
		dir = new File(((EclipseAdaptor)adaptor).getBundleRootDir(), String.valueOf(id));
		dirData = new File(dir, ((EclipseAdaptor)adaptor).getDataDirName());
		dirGeneration = new File(dir, String.valueOf(generation));
		file = reference ? new File(name) : new File(dirGeneration, name);
		bundleFile = BundleFile.createBundleFile(file,this);
		initializeBase(location);
	}

	void initialize() throws IOException {
		initializeBase(location);
		dir = new File(((EclipseAdaptor)adaptor).getBundleRootDir(), Long.toString(id));
		File delete = new File(dir, ".delete");
		if (delete.exists())
			throw new NumberFormatException();

		dirData = new File(dir, ((EclipseAdaptor)adaptor).getDataDirName());
		dirGeneration = new File(dir, String.valueOf(generation));

		if (reference)
			file = new File(name);
		else 
			file = new File(dirGeneration, name);
		bundleFile = BundleFile.createBundleFile(file, this);
	}
	
	private void initializeBase(String location) throws IOException {
		if (!location.endsWith("/"))
			location += "/";
		try {
			base = new URL(location);
			if (base.getProtocol().equals("reference"))
				base = new URL(base.getFile());
		}
		catch (MalformedURLException e) {
			base = null;
		}

	}

	/**
	 * Returns the absolute path name of a native library. The VM invokes this
	 * method to locate the native libraries that belong to classes loaded with
	 * this class loader. If this method returns <code>null</code>, the VM
	 * searches the library along the path specified as the <code>java.library.path</code>
	 * property.
	 * 
	 * @param libname
	 *                   the library name
	 * @return the absolute path of the native library
	 */
	public String findLibrary(String libName) {
		// first do the standard OSGi lookup using the native clauses
		// in the manifest. If that fails, do the legacy Eclipse lookup.
		String result = super.findLibrary(libName);
		if (result != null)
			return result;
		if (libraryVariants == null)
			libraryVariants = buildLibraryVariants();
		if (libName.length() == 0)
			return null;
		if (libName.charAt(0) == '/' || libName.charAt(0) == '\\')
			libName = libName.substring(1);
		libName = System.mapLibraryName(libName);

		//		if (DEBUG && DEBUG_SHOW_ACTIONS && debugNative(libName))
		//			debug("findLibrary(" + libName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		if (base == null)
			return null;
		String libFileName = null;
		if (base.getProtocol().equals(FILE)) {
			// directly access library
			URL foundPath = searchVariants(new URL[] { base }, libraryVariants, libName);
			if (foundPath != null)
				libFileName = foundPath.getFile();
		} else {
			if (base.getProtocol().equals(PROTOCOL)) {
				URL[] searchList = getSearchURLs(base);
				if ((searchList != null) && (searchList.length != 0)) {
					URL foundPath = searchVariants(searchList, libraryVariants, libName);
					if (foundPath != null)
						libFileName = foundPath.getFile();
				}
			}
		}

		if (libFileName == null)
			return null;
		return new File(libFileName).getAbsolutePath();
	}

	private URL searchVariants(URL[] basePaths, String[] variants, String path) {
		// This method assumed basePaths are 'resolved' URLs
		for (int i = 0; i < variants.length; i++) {
			for (int j = 0; j < basePaths.length; j++) {
				String fileName = basePaths[j].getFile() + variants[i] + path;
				File file = new File(fileName);
				if (!file.exists()) {
					//					if (DEBUG && DEBUG_SHOW_FAILURE)
					//						debug("not found " + file.getAbsolutePath());
					// //$NON-NLS-1$
				} else {
					//					if (DEBUG && DEBUG_SHOW_SUCCESS)
					//						debug("found " + path + " as " +
					// file.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
					try {
						return new URL("file:" + fileName); //$NON-NLS-1$
					} catch (MalformedURLException e) {
						// Intentionally ignore this exception
						// so we continue looking for a matching
						// URL.
					}
				}
			}
		}
		return null;
	}

	private URL[] getSearchURLs(URL target) {
		return new URL[] { target };
	}

	public synchronized Dictionary getManifest() {
		try {
			return getManifest(false);
		} catch (BundleException e) {
			// TODO: handle exception
			return null;
		}
	}
	public synchronized Dictionary getManifest(boolean first) throws BundleException {
		if (manifest == null)
			manifest = first ? loadManifest() : new CachedManifest(this);
		return manifest;
	}

	public synchronized Dictionary loadManifest() throws BundleException {
		Dictionary result = null;
		try {
			URL url = getEntry(Constants.OSGI_BUNDLE_MANIFEST);
			if (url == null)
				throw new BundleException("Error reading manifest " + getLocation());
			try {
				result = Headers.parseManifest(url.openStream());
			} catch (IOException e) {
				throw new BundleException("Error reading manifest " + getLocation(), e);
			}
			return result;
		} catch (BundleException e) {
			if (firstManifestLookup)
				result =  loadManifestFromAlternativeLocation();
			if (result == null)
				// TODO log the error somewhere
				throw new BundleException("Manifest not found: " + getLocation(), e);
		} finally {
			firstManifestLookup = false;
		}
		return result;
	}

	private Dictionary loadManifestFromAlternativeLocation() throws BundleException { 
		String cacheLocation = (String) System.getProperties().get("osgi.manifest.cache");
		File generationLocation = new File(cacheLocation + '/' + computeFileName(this.file.toString()) + ".MF");
		if (!generationLocation.isFile())
			return null;
		URL url = null;
		try {
			url = generationLocation.toURL();
		} catch (MalformedURLException e) {
			//ignore. because the url format is correct
		}
		try {
			return Headers.parseManifest(url.openStream());
		} catch (IOException e) {
			throw new BundleException("Manifest not found: " + getLocation(), e);
		}
	}
	/*
	 * Derives a file name from a a path string. Example: c:\autoexec.bat ->
	 * c__autoexec.bat
	 */
	private String computeFileName(String filePath) {
		StringBuffer newName = new StringBuffer(filePath);
		for (int i = 0; i < filePath.length(); i++) {
			char c = newName.charAt(i);
			if (c == ':' || c == '/' || c == '\\')
				newName.setCharAt(i, '_');
		}
		return newName.toString();
	}

	public void save() {
		// do nothing.  This method is here to override one in the superclass.
	}

	protected void loadMetaData() {
		// do nothing.  This method is here to override one in the superclass.
	}

	protected void loadFromMetaData() {
		// do nothing.  This method is here to override one in the superclass.
	}
	protected void loadFromManifest() throws IOException{
		try {
			getManifest(true);
		} catch (BundleException e) {
			throw new IOException("Unable to properly read manifest for: " + location);
		}
		super.loadFromManifest();
		pluginClass = (String)manifest.get("Plugin-Class");
		isLegacy = (String)manifest.get("Legacy");
	}
	public String isLegacy() {
		return isLegacy;
	}
	public void setLegacy(String value) {
		isLegacy = value;
	}

	public String getPluginClass() {
		return pluginClass;
	}
	public void setPluginClass(String value) {
		pluginClass = value;
	}
	public void setLocation(String value) {
		this.location = value;
	}
	public void setName(String value) {
		this.name = value;
	}
	public void setUniqueId(String value){
		this.uniqueId = value;
	}
	public void setVersion(Version value){
		this.version = value;
	}
	public void setActivator(String value){
		this.activator = value;
	}
	public void setClassPath(String value){
		this.classpath = value;
	}
	public void setFragment(boolean value){
		this.isFragment = value;
	}
	public void setExecutionEnvironment(String value) {
		this.executionEnvironment = value;
	}
	public void setDynamicImports(String value) {
		this.dynamicImports = value;
	}
}
