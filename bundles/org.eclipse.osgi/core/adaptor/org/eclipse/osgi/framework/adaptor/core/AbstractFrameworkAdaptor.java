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

package org.eclipse.osgi.framework.adaptor.core;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import org.eclipse.osgi.framework.adaptor.EventPublisher;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.core.Msg;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * An abstract FrameworkAdaptor class that has default implementations that most
 * FrameworkAdaptor implementations can use.
 */
public abstract class AbstractFrameworkAdaptor implements FrameworkAdaptor {

	protected EventPublisher eventPublisher;

	/**
	 * The ServiceRegistry object for this FrameworkAdaptor.
	 */
	protected ServiceRegistry serviceRegistry;

	/**
	 * The Properties object for this FrameworkAdaptor
	 */
	protected Properties properties;

	/**
	 * The System Bundle's BundleContext.
	 */
	protected BundleContext context;

	/**
	 * The Vector Initial Capacity value.
	 */
	protected int vic;

	/**
	 * The Vector Capacity Increment value.
	 */
	protected int vci;

	/**
	 * The Hashtable Initial Capacity value.
	 */
	protected int hic;

	/**
	 * The Hashtable Load Factor value.
	 */
	protected float hlf;

	/**
	 * The initial bundle start level.
	 */
	protected int initialBundleStartLevel = 1;
	/**
	 * Initializes the ServiceRegistry, loads the properties for this
	 * FrameworkAdaptor and initializes all the Vector and Hashtable capacity,
	 * increment and factor values.
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#initialize()
	 */
	public void initialize(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
		serviceRegistry = new ServiceRegistry();
		serviceRegistry.initialize();
		loadProperties();
		vic = 10;
		vci = 10;
		hic = 10;
		hlf = 0.75f;
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
			throw new BundleException(Msg.formatter.getString("ADAPTOR_URL_CREATE_EXCEPTION", location), e);
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
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getVectorInitialCapacity()
	 */
	public int getVectorInitialCapacity() {
		return vic;
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getVectorCapacityIncrement()
	 */
	public int getVectorCapacityIncrement(){
		return vci;
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getHashtableInitialCapacity()
	 */
	public int getHashtableInitialCapacity() {
		return hic;
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getHashtableLoadFactor()
	 */
	public float getHashtableLoadFactor() {
		return hlf;
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#frameworkStart(org.osgi.framework.BundleContext)
	 */
	public void frameworkStart(BundleContext context) throws BundleException
	{
		this.context = context;
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#frameworkStop(org.osgi.framework.BundleContext)
	 */
	public void frameworkStop(BundleContext context) throws BundleException
	{
		this.context = null;
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getExportPackages()
	 */
	public String getExportPackages()
	{
		return null;
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getExportServices()
	 */
	public String getExportServices()
	{
		return null;
	}

	/**
	 * Returns the EventPublisher for this FrameworkAdaptor.
	 * @return The EventPublisher.
	 */
	public EventPublisher getEventPublisher(){
		return eventPublisher;
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

		String resource = System.getProperty(Constants.KEY_OSGI_PROPERTIES, Constants.DEFAULT_OSGI_PROPERTIES);

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
	
	public int getInitialBundleStartLevel() {
		return initialBundleStartLevel;
	}

	public void setInitialBundleStartLevel(int value) {
		initialBundleStartLevel = value;
	}

}
