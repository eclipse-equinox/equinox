/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.defaultadaptor;

import java.io.*;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.*;

import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.eclipse.osgi.framework.adaptor.Version;
import org.eclipse.osgi.framework.adaptor.core.AbstractBundleData;
import org.eclipse.osgi.framework.debug.Debug;
import org.osgi.framework.*;

/**
 * The <code>BundleData</code> represents a single bundle that is persistently 
 * stored by a <code>FrameworkAdaptor</code>.  A <code>BundleData</code> creates
 * the ClassLoader for a bundle, finds native libraries installed in the
 * FrameworkAdaptor for the bundle, creates data files for the bundle,
 * used to access bundle entries, manifest information, and getting and saving
 * metadata.
 * 
 */
public class DefaultBundleData extends AbstractBundleData implements Cloneable {

	public static final String METADATA_BUNDLE_GEN = "METADATA_BUNDLE_GEN";
	public static final String METADATA_BUNDLE_LOC = "METADATA_BUNDLE_LOC";
	public static final String METADATA_BUNDLE_REF = "METADATA_BUNDLE_REF";
	public static final String METADATA_BUNDLE_NAME = "METADATA_BUNDLE_NAME";
	public static final String METADATA_BUNDLE_NCP = "METADATA_BUNDLE_NCP";
	public static final String METADATA_BUNDLE_ABSL = "METADATA_BUNDLE_ABSL";
	public static final String METADATA_BUNDLE_STATUS = "METADATA_BUNDLE_STATUS";
	public static final String METADATA_BUNDLE_METADATA = "METADATA_BUNDLE_METADATA";

	/** top level bundle directory */
	protected File dir;
	
	/** bundle data directory */
	protected File dirData;

	/** current generation directory */
	protected File dirGeneration;

	/** bundle id */
	protected long id;

	/** bundle location */
	protected String location;

	/** Is bundle a reference */
	protected boolean reference;
	
	/** Bundle's metadata **/
	protected MetaData metadata;
	
	/** bundle file */
	protected File file;

	/** BundleFile object for this BundleData */
	protected BundleFile bundleFile;

	/** bundle's name */
	protected String name;
	
	/** theses values are loaded from the manifest */
	protected String uniqueId;
	protected Version version;
	protected boolean isFragment = false;
	protected String classpath;
	protected String activator;
	protected String executionEnvironment;
	protected String dynamicImports;
	
	/** bundle generation */
	protected int generation = 1;

	/** native code paths for this BundleData */
	protected String[] nativepaths;

	/** the DefaultAdaptor for this BundleData */
	protected DefaultAdaptor adaptor;
	
	protected int startLevel = -1;
	
	protected int status = 0;
	
	/**
	 * Read data from an existing directory.
	 * This constructor is used by getInstalledBundles.
	 *
	 * @param directory The top level bundle directory.
	 * @throws NumberFormatException if the directory is not a
	 * number, the directory contains a ".delete" file or
	 * the directory does not contain a ".bundle" file.
	 * @throws IOException If an error occurs initializing the bundle data.
	 */
	public DefaultBundleData(DefaultAdaptor adaptor){
		this.adaptor = adaptor;
	}

	/**
	 * Read data from an existing directory.
	 * This constructor is used by getInstalledBundles.
	 *
	 * @param directory The top level bundle directory.
	 * @throws NumberFormatException if the directory is not a
	 * number, the directory contains a ".delete" file or
	 * the directory does not contain a ".bundle" file.
	 * @throws IOException If an error occurs initializing the bundle data.
	 */
	public void initializeExistingBundle(String directory) throws IOException {
        id = Long.parseLong(directory);
        dir = new File(adaptor.bundleRootDir, directory);

        /* if the file is not a directory */
        if (!dir.exists() || !dir.isDirectory())
            throw new NumberFormatException();
        File delete = new File(dir, ".delete");

        /* and the directory is not marked for delete */
        if (delete.exists())
        {
            throw new NumberFormatException();
        }

		try
		{
        	loadFromMetaData();
		}
		catch (IOException ioe)
		{
			throw new NumberFormatException();
		}

        dirData = new File(dir, adaptor.dataDirName);
        dirGeneration = new File(dir, String.valueOf(generation));

        if (reference)
		{
			file = new File(name);
		}
		else
		{
			file = new File(dirGeneration, name);
		}
		bundleFile = BundleFile.createBundleFile(file,this);
		loadFromManifest();
    }	


	/**
	 * Create data for a new directory.
	 * This constructor is used by installBundle.
	 *
	 * @param id The id of the new bundle.
	 * @param location The location string of the new bundle.
	 */
	public void initializeReferencedBundle(long id, String location, String name) throws IOException {
	    this.id = id;
	    this.location = location;
	    this.name = name;
	    this.reference = true;
	
	    String directory = String.valueOf(id);
	
	    dir = new File(adaptor.bundleRootDir, directory);

	    dirData = new File(dir, adaptor.dataDirName);
	    dirGeneration = new File(dir, String.valueOf(generation));

	    this.file = new File(name);

		loadMetaData();
		setStartLevel(adaptor.getInitialBundleStartLevel());

		if (!getGenerationDir().exists())
		{
			throw new IOException(
				AdaptorMsg.formatter.getString(
					"ADAPTOR_DIRECTORY_CREATE_EXCEPTION",
					getGenerationDir().getPath()));
		}

		this.bundleFile = BundleFile.createBundleFile(this.file, this);
		loadFromManifest();
	}	
	
	/**
	 * Create data for a new directory.
	 * This constructor is used by installBundle.
	 *
	 * @param id The id of the new bundle.
	 * @param location The location string of the new bundle.
	 */
	public void initializeNewBundle(long id, String location, InputStream in) throws IOException {
		this.id = id;
		this.location = location;
		this.name = adaptor.mapLocationToName(location);
		this.reference = false;

		String directory = String.valueOf(id);

		dir = new File(adaptor.bundleRootDir, directory);

		dirData = new File(dir, adaptor.dataDirName);
		dirGeneration = new File(dir, String.valueOf(generation));

		file = new File(dirGeneration, name);

		loadMetaData();
		setStartLevel(adaptor.getInitialBundleStartLevel());

		if (!getGenerationDir().exists())
		{
			throw new IOException(
				AdaptorMsg.formatter.getString(
					"ADAPTOR_DIRECTORY_CREATE_EXCEPTION",
					dirGeneration.getPath()));
		}

		DefaultAdaptor.readFile(in, file);
		bundleFile = BundleFile.createBundleFile(file,this);
		loadFromManifest();
	}
	
   /**
	 * Creates the ClassLoader for the BundleData.  The ClassLoader created
	 * must use the <code>ClassLoaderDelegate</code> to delegate class, resource
	 * and library loading.  The delegate is responsible for finding any resource
	 * or classes imported by the bundle or provided by bundle fragments or 
	 * bundle hosts.  The <code>ProtectionDomain</code> domain must be used
	 * by the Classloader when defining a class.  
	 * @param delegate The <code>ClassLoaderDelegate</code> to delegate to.
	 * @param domain The <code>ProtectionDomain</code> to use when defining a class.
	 * @param bundleclasspath An array of bundle classpaths to use to create this
	 * classloader.  This is specified by the Bundle-ClassPath manifest entry.
	 * @return The new ClassLoader for the BundleData.
	 */
	public org.eclipse.osgi.framework.adaptor.BundleClassLoader createClassLoader(
		ClassLoaderDelegate delegate,
		ProtectionDomain domain,
		String[] bundleclasspath) {
		return adaptor.getElementFactory().createClassLoader(delegate,domain,bundleclasspath,this);
	}

	/**
		 * Gets a <code>URL</code> to the bundle entry specified by path.
		 * This method must not use the BundleClassLoader to find the
		 * bundle entry since the ClassLoader will delegate to find the resource.
		 * @param path The bundle entry path.
		 * @return A URL used to access the entry or null if the entry
		 * does not exist.
		 */
	public URL getEntry(String path) {
		return bundleFile.getURL(path,0);
	}

	/**
	 * Gets all of the bundle entries that exist under the specified path.
	 * For example: <p>
	 * <code>getEntryPaths("/META-INF")</code> <p>
	 * This will return all entries from the /META-INF directory of the bundle.
	 * @param path The path to a directory in the bundle.
	 * @return An Enumeration of the entry paths or null if the specified path
	 * does not exist.
	 */
	public Enumeration getEntryPaths(String path) {
		return bundleFile.getEntryPaths(path);
	}

	/**
	 * Returns the absolute path name of a native library. The BundleData
	 * ClassLoader invokes this method to locate the native libraries that 
	 * belong to classes loaded from this BundleData. Returns 
	 * null if the library does not exist in this BundleData.
	 * @param libname The name of the library to find the absolute path to.
	 * @return The absolute path name of the native library or null if
	 * the library does not exist.
	 */
	public String findLibrary(String libname) {
		String mappedName = System.mapLibraryName(libname);
		String path = null;

		if (Debug.DEBUG && Debug.DEBUG_LOADER)
		{
			Debug.println("  mapped library name: "+mappedName);
		}

		path = findNativePath(mappedName);

		if (path == null){
			if (Debug.DEBUG && Debug.DEBUG_LOADER)
			{
				Debug.println("  library does not exist: "+mappedName);
			}
			path = findNativePath(libname);
		}

		if (Debug.DEBUG && Debug.DEBUG_LOADER)
		{
			Debug.println("  returning library: "+path);
		}
		return path;
	}

	protected String findNativePath(String libname) {
		String path = null;
		if (!libname.startsWith("/")) {
			libname = '/' + libname;
		}
		if (nativepaths != null) {
			for (int i=0; i<nativepaths.length; i++){
				if(nativepaths[i].endsWith(libname)){
					File nativeFile = bundleFile.getFile(nativepaths[i]);
					path = nativeFile.getAbsolutePath();
				}
			}
		}
		return path;
	}

	/**
	 * Installs the native code paths for this BundleData.  Each
	 * element of nativepaths must be installed for lookup when findLibrary 
	 * is called.
	 * @param nativepaths The array of native code paths to install for
	 * the bundle.
	 * @throws BundleException If any error occurs during install.
	 */
	public void installNativeCode(String[] nativepaths)
		throws BundleException {
		this.nativepaths = nativepaths;
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<nativepaths.length; i++){
			// extract the native code
			File nativeFile = bundleFile.getFile(nativepaths[i]);
			if (nativeFile == null) {
				throw new BundleException(AdaptorMsg.formatter.getString("BUNDLE_NATIVECODE_EXCEPTION", nativepaths[i]));
			}
			sb.append(nativepaths[i]);
			if (i<nativepaths.length-1) {
				sb.append(",");
			}
		}
		metadata.set(METADATA_BUNDLE_NCP, sb.toString());
	}

	/**
	 * Return the bundle data directory.
	 * Attempt to create the directory if it does not exist.
	 *
	 * @return Bundle data directory.
	 */
	public File getDataFile(String path) {
		if (!dirData.exists() && !dirData.mkdirs())
		{
			if (Debug.DEBUG && Debug.DEBUG_GENERAL)
			{
				Debug.println("Unable to create bundle data directory: "+dirData.getPath());
			}
		}

		return (new File(dirData,path));
	}

	/**
	 * Get the BundleData bundle ID.  This will be used as the bundle
	 * ID by the framework.
	 * @return The BundleData ID.
	 */
	public long getBundleID() {
		return (id);
	}

   /**
	 * Get the BundleData Location.  This will be used as the bundle
	 * location by the framework.
	 * @return the BundleData location.
	 */
	public String getLocation() {
		return (location);
	}

	/**
	 * Close all resources for this BundleData
	 */
	public void close() throws IOException {
		if (bundleFile != null) {
			bundleFile.close();
		}
	}

	/**
	 * Opens all resource for this BundleData.  Reopens the BundleData if
	 * it was previosly closed.
	 */
	public void open() throws IOException {
		bundleFile.open();
	}

	/**
	 * Load the bundle data from the metadata.
	 *
	 * @throws IOException if an read error occurs.
	 * @throws FileNotFoundException if the metadata file does not exist.
	 */
	protected synchronized void loadFromMetaData() throws IOException {
		loadMetaData(); 
		status = metadata.getInt(METADATA_BUNDLE_STATUS, 0);
		startLevel = metadata.getInt(METADATA_BUNDLE_ABSL, 1);
		generation = metadata.getInt(METADATA_BUNDLE_GEN, -1);
		name = metadata.get(METADATA_BUNDLE_NAME, null);
		location = metadata.get(METADATA_BUNDLE_LOC, null);
		reference = metadata.getBoolean(METADATA_BUNDLE_REF, false);

		String npString = metadata.get(METADATA_BUNDLE_NCP, null);
		if (npString != null)
			setNativeCodePath(npString);

		if( generation == -1 || name == null || location == null ) {
			throw new IOException (AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"));		
		}
	}
	
	protected void loadMetaData() throws IOException
	{
	    metadata = (new MetaData(new File(dir,".bundle"),"Bundle metadata"));
		metadata.load();
	}

	protected void loadFromManifest() throws IOException{
		try {
			getManifest();
		} catch (BundleException e) {
			throw new IOException(AdaptorMsg.formatter.getString("ADAPTOR_ERROR_GETTING_MANIFEST",location));
		}

		if (manifest == null) {
			throw new IOException(AdaptorMsg.formatter.getString("ADAPTOR_ERROR_GETTING_MANIFEST",location));
		}
		version = new Version((String)manifest.get(Constants.BUNDLE_VERSION));
		uniqueId = (String)manifest.get(Constants.BUNDLE_GLOBALNAME);
		if (uniqueId == null)
			uniqueId = (String)manifest.get(Constants.BUNDLE_NAME);
		classpath = (String)manifest.get(Constants.BUNDLE_CLASSPATH);
		activator = (String)manifest.get(Constants.BUNDLE_ACTIVATOR);
		String host = (String)manifest.get(Constants.HOST_BUNDLE);
		isFragment = host != null;
		executionEnvironment = (String)manifest.get(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		dynamicImports = (String)manifest.get(Constants.DYNAMICIMPORT_PACKAGE);
	}

	/**
	 * Return the bundle generation directory.
	 * Attempt to create the directory if it does not exist.
	 *
	 * @return Bundle generation directory.
	 */
	protected File getGenerationDir()
	{
		if (!dirGeneration.exists() && !dirGeneration.mkdirs())
		{
			if (Debug.DEBUG && Debug.DEBUG_GENERAL)
			{
				Debug.println("Unable to create bundle jar directory: "+dirGeneration.getPath());
			}
		}

		return(dirGeneration);
	}
	
	/**
	 * Return the bundle file.
	 * Attempt to create the bundle generation directory if it does not exist.
	 *
	 * @return Bundle file.
	 */
	protected File getBundleFile()
	{
		getGenerationDir(); /* create the generation dir if necessary */

		return(file);
	}

	/**
	 * Return the top level bundle directory.
	 *
	 * @return Top level bundle directory.
	 */
	protected File getBundleDir()
	{
		return (dir);
	}

	/**
	 * Save the bundle data in the data file.
	 *
	 * @throws IOException if a write error occurs.
	 */
	public synchronized void save() throws IOException {
		metadata.setInt(METADATA_BUNDLE_STATUS, status);
		metadata.setInt(METADATA_BUNDLE_ABSL, startLevel);
		metadata.setInt(METADATA_BUNDLE_GEN, generation);
		metadata.set(METADATA_BUNDLE_NAME, name);
		metadata.set(METADATA_BUNDLE_LOC, location);
		metadata.setBoolean(METADATA_BUNDLE_REF, reference);
		metadata.save();
	}
	
/**
	* Return a copy of this object with the
	* generation dependent fields updated to
	* the next free generation level.
	*
	* @throws IOException If there are no more available generation levels.
	*/
   protected DefaultBundleData nextGeneration(String name) throws IOException
   {
	   int nextGeneration = generation;

	   while (nextGeneration < Integer.MAX_VALUE)
	   {
		   nextGeneration++;

		   File nextDirGeneration = new File(dir, String.valueOf(nextGeneration));

		   if (nextDirGeneration.exists())
		   {
			   continue;
		   }

		   DefaultBundleData next;
		   try
		   {
			   next = (DefaultBundleData)clone();
		   }
		   catch (CloneNotSupportedException e)
		   {
			 // this shouldn't happen, since we are Cloneable
			 throw new InternalError();
		   }

		   next.generation = nextGeneration;
		   next.dirGeneration = nextDirGeneration;
		   next.name = name;
		   next.file = new File(name);
		   next.reference = true;
		   // null out the manifest to force it to be re-read.
		   next.manifest = null;
		   return(next);
	   }

	   throw new IOException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"));
   }
   
/**
	 * Return a copy of this object with the
	 * generation dependent fields updated to
	 * the next free generation level.
	 *
	 * @throws IOException If there are no more available generation levels.
	 */
	public DefaultBundleData nextGeneration() throws IOException
	{
		int nextGeneration = generation;
	
		while (nextGeneration < Integer.MAX_VALUE)
		{
			nextGeneration++;
	
			File nextDirGeneration = new File(dir, String.valueOf(nextGeneration));
	
			if (nextDirGeneration.exists())
			{
				continue;
			}
	
			DefaultBundleData next;
			try
			{
				next = (DefaultBundleData)clone();
			}
			catch (CloneNotSupportedException e)
			{
			  // this shouldn't happen, since we are Cloneable
			  throw new InternalError();
			}
	
			next.generation = nextGeneration;
			next.dirGeneration = nextDirGeneration;
			if (next.reference) {
				next.reference = false;
				next.name = adaptor.mapLocationToName(location);
			}
			next.file = new File(nextDirGeneration, next.name);
			// null out the manifest to force it to be re-read.
			next.manifest = null;
			return(next);
		}
	
		throw new IOException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"));
	}

	public Bundle getBundle(){
		return bundle;
	}

	public String toString(){
		return location;
	}

	public int getStartLevel() {
		return startLevel;
	}

	public int getStatus() {
		return status;
	}

	public void setStartLevel(int value) {
		startLevel = value;
	}

	public void setStatus(int value) {
		status = value;
	}

	public boolean isReference() {
		return reference;
	}
	public void setReference(boolean value) {
		reference = value;
	}
	public int getGeneration() {
		return generation;
	}
	public void setGeneration(int value) {
		generation = value;
	}

	public String getNativeCodePath() {
		if (nativepaths == null || nativepaths.length == 0)
			return null;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < nativepaths.length; i++) {
			File nativeFile = bundleFile.getFile(nativepaths[i]);
			if (nativeFile != null) {
				sb.append(nativepaths[i]);
				if (i < nativepaths.length - 1)
					sb.append(",");
			}
		}
		return sb.toString();
	}

	protected String[] parseNativeCodePath(String value) {
		if (value == null)
			return null;
		ArrayList result = new ArrayList(5);
		StringTokenizer st = new StringTokenizer(value, ",");
		while (st.hasMoreTokens()) {
			String path = st.nextToken();
			result.add(path);
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	public void setNativeCodePath(String value) {
		nativepaths = parseNativeCodePath(value);
	}

	public String getName() {
		return name;
	}
	public String getUniqueId() {
		return uniqueId;
	}
	public String getClassPath() {
		return classpath;
	}
	public String getActivator() {
		return activator;
	}
	public Version getVersion() {
		return version;
	}
	public boolean isFragment() {
		return isFragment;
	}
	public String getExecutionEnvironment(){
		return executionEnvironment;
	}
	public String getDynamicImports(){
		return dynamicImports;
	}
	public DefaultAdaptor getAdaptor() {
		return adaptor;
	}
}
