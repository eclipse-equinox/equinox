/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.adaptor.core;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.adaptor.EventPublisher;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.adaptor.IBundleStats;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.util.Headers;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * An abstract FrameworkAdaptor class that has default implementations that most
 * FrameworkAdaptor implementations can use.
 */
public abstract class AbstractFrameworkAdaptor implements FrameworkAdaptor {

	/** Name of the Adaptor manifest file */
	protected final String ADAPTOR_MANIFEST = "ADAPTOR.MF";

	protected EventPublisher eventPublisher;

	/**
	 * The ServiceRegistry object for this FrameworkAdaptor.
	 */
	protected ServiceRegistryImpl serviceRegistry;

	/**
	 * The Properties object for this FrameworkAdaptor
	 */
	protected Properties properties;

	/**
	 * The System Bundle's BundleContext.
	 */
	protected BundleContext context;	//TODO Rename to systemBundleContext

	/**
	 * The initial bundle start level.
	 */
	protected int initialBundleStartLevel = 1;

	/** This adaptor's manifest file */
	protected Headers manifest = null;

	/**
	 * The BundleClassLoader parent to use when creating BundleClassLoaders.
	 * The behavior of the ParentClassLoader will load classes
	 * from the boot strap classloader.
	 */
	protected static ClassLoader bundleClassLoaderParent = new ParentClassLoader();

	/**
	 * Initializes the ServiceRegistry, loads the properties for this
	 * FrameworkAdaptor and initializes all the Vector and Hashtable capacity,
	 * increment and factor values.
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#initialize()
	 */
	public void initialize(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
		serviceRegistry = new ServiceRegistryImpl();
		serviceRegistry.initialize();
		loadProperties();
		readAdaptorManifest();
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getProperties()
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#mapLocationToURLConnection(String)
	 */
	public URLConnection mapLocationToURLConnection(String location) throws BundleException {
		try {
			return (new URL(location).openConnection());
		} catch (IOException e) {
			throw new BundleException(AdaptorMsg.formatter.getString("ADAPTOR_URL_CREATE_EXCEPTION", location), e);
		}
	}

	/**
	 * Always returns -1 to indicate that this operation is not supported by this
	 * FrameworkAdaptor.  Extending classes should override this method if
	 * they support this operation.
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getTotalFreeSpace()
	 */
	public long getTotalFreeSpace() throws IOException {
		return -1;
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getServiceRegistry()
	 */
	public org.eclipse.osgi.framework.adaptor.ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#frameworkStart(org.osgi.framework.BundleContext)
	 */
	public void frameworkStart(BundleContext context) throws BundleException
	{
		this.context = context;
		BundleResourceHandler.setContext(context);
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#frameworkStop(org.osgi.framework.BundleContext)
	 */
	public void frameworkStop(BundleContext context) throws BundleException
	{
		this.context = null;
		BundleResourceHandler.setContext(null);
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#frameworkStopping()
	 */
	public void frameworkStopping() {
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getExportPackages()
	 */
	public String getExportPackages()
	{
		if (manifest == null)
			return null;
		return (String) manifest.get(Constants.EXPORT_PACKAGE);
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getExportServices()
	 */
	public String getExportServices()
	{
		if (manifest == null)
			return null;
		return (String) manifest.get(Constants.EXPORT_SERVICE);
	}

	public String getProvidePackages() {
		if (manifest == null)
			return null;
		return (String) manifest.get(Constants.PROVIDE_PACKAGE);
	}

	/**
	 * Returns the EventPublisher for this FrameworkAdaptor.
	 * @return The EventPublisher.
	 */
	public EventPublisher getEventPublisher(){
		return eventPublisher;
	}

	public int getInitialBundleStartLevel() {
		return initialBundleStartLevel;
	}

	public void setInitialBundleStartLevel(int value) {
		initialBundleStartLevel = value;
	}

	public IBundleStats getBundleStats() {
		return null;
	}

	/**
	 * This method locates and reads the osgi.properties file.
	 * If the system property <i>org.eclipse.osgi.framework.internal.core.properties</i> is specifed, its value
	 * will be used as the name of the file instead of
	 * <tt>osgi.properties</tt>.   There are 3 places to look for these properties.  These
	 *  3 places are searched in the following order, stopping when the properties are found.
	 *
	 *  <ol>
	 *  <li>Look for a file in the file system
	 *  <li>Look for a resource in the FrameworkAdaptor's package
	 *  </ol>
	 */
	protected void loadProperties() {
		properties = new Properties();

		String resource = System.getProperty(Constants.OSGI_PROPERTIES, Constants.DEFAULT_OSGI_PROPERTIES);

		try
		{
			InputStream in = null;
			File file = new File(resource);
			if (file.exists())
			{
				in = new FileInputStream(file);
			}

			if (in == null)
			{
				in = getClass().getResourceAsStream(resource);
			}


			if (in != null)
			{
				try
				{
					properties.load(new BufferedInputStream(in));
				}
				finally
				{
					try
					{
						in.close();
					}
					catch (IOException ee)
					{
					}
				}
			}
			else
			{
				if (Debug.DEBUG && Debug.DEBUG_GENERAL)
					Debug.println("Skipping osgi.properties: " + resource);
			}
		}
		catch (IOException e)
		{
			if (Debug.DEBUG && Debug.DEBUG_GENERAL)
			{
				Debug.println("Unable to load osgi.properties: " + e.getMessage());
			}
		}
	}

	/**
	 * Reads and initializes the adaptor BundleManifest object.  The
	 * BundleManifest is used by the getExportPackages() and getExportServices()
	 * methods of the adpator.
	 */
	protected void readAdaptorManifest() {
		InputStream in = null;
		// walk up the class hierarchy until we find the ADAPTOR_MANIFEST.
		Class adaptorClazz = getClass();
		while (in == null && AbstractFrameworkAdaptor.class.isAssignableFrom(adaptorClazz) ) {
			in = adaptorClazz.getResourceAsStream(ADAPTOR_MANIFEST);
			adaptorClazz = adaptorClazz.getSuperclass();
		}

		if (in == null) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Unable to find adaptor bundle manifest " + ADAPTOR_MANIFEST);
			}
			manifest = new Headers(new Properties());
			return;
		}
		try {
			manifest = Headers.parseManifest(in);
		} catch (BundleException e) {
			Debug.println("Unable to read adaptor bundle manifest " + ADAPTOR_MANIFEST);
		}
	}

	public BundleData createSystemBundleData() throws BundleException{
		return new SystemBundleData(this);
	}

	abstract public AdaptorElementFactory getElementFactory();

	/**
	 * Does a recursive copy of one directory to another.
	 * @param inDir input directory to copy.
	 * @param outDir output directory to copy to.
	 * @throws IOException if any error occurs during the copy.
	 */
	public static void copyDir(File inDir, File outDir) throws IOException{
		String[] files = inDir.list();
		if (files != null && files.length>0) {
			outDir.mkdir();
			for (int i=0; i<files.length; i++) {
				File inFile = new File(inDir,files[i]);
				File outFile = new File(outDir,files[i]);
				if (inFile.isDirectory()) {
					copyDir(inFile,outFile);
				}
				else {
					InputStream in = new FileInputStream(inFile);
					readFile(in,outFile);
				}
			}
		}
	}

	/**
	 * Read a file from an InputStream and write it to the file system.
	 *
     * @param is InputStream from which to read.
     * @param file output file to create.
     * @exception IOException
     */
	public static void readFile(InputStream in, File file) throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);

			byte buffer[] = new byte[1024];
			int count;
			while ((count = in.read(buffer, 0, buffer.length)) > 0) {
				fos.write(buffer, 0, count);
			}

			fos.close();
			fos = null;

			in.close();
			in = null;
		} catch (IOException e) {
			// close open streams
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException ee) {
				}
			}

			if (in != null) {
				try {
					in.close();
				} catch (IOException ee) {
				}
			}

			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Unable to read file");
				Debug.printStackTrace(e);
			}

			throw e;
		}	
	}

	public ClassLoader getBundleClassLoaderParent() {
		return bundleClassLoaderParent;
	}

	/**
	 * Empty parent classloader.  This is used by default as the BundleClassLoader
	 * parent.
	 */
	protected static class ParentClassLoader extends ClassLoader {
		protected ParentClassLoader() {
			super(null);
		}
	}
}
