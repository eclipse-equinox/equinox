/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.*;
import java.util.*;
import org.eclipse.equinox.socket.ServerSocketInterface;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.cm.*;

/**
 * This class is the main configuration class for Http Service. It creates the
 * thread pool and the listeners and responds to configuration changes.
 */

public class HttpConfiguration implements ManagedService, ManagedServiceFactory {

	/** default HttpListener object for 'http' protocol
	 * This object is set if ConfigurationAdmin has NOT configured any
	 * HttpListeners and we are not waiting to be configured.
	 */
	private HttpListener defaultHttpListener;
	/** default HttpListener object for 'https' protocol
	 * This object is set if ConfigurationAdmin has NOT configured any
	 * HttpListeners and we are not waiting to be configured.
	 */
	private HttpListener defaultHttpsListener;
	/** Hashtable of HttpListener object configured by the ConfigurationAdmin
	 * Configuration PID (String) => HttpListener object
	 */
	Hashtable configuredListeners;

	protected Http http;

	protected HttpThreadPool pool;

	protected final static String enviroKeyHttpPort = "org.osgi.service.http.port"; //$NON-NLS-1$
	protected final static String enviroKeyHttpsPort = "org.osgi.service.http.port.secure"; //$NON-NLS-1$
	protected final static String enviroKeyHttpAddress = "org.eclipse.equinox.http.address"; //$NON-NLS-1$

	protected final static String HTTPSERVICEPID = "org.eclipse.equinox.http.Http"; //$NON-NLS-1$
	protected ServiceRegistration managedService;
	protected final static String HTTPSERVICEFACTORYPID = "org.eclipse.equinox.http.HttpFactory"; //$NON-NLS-1$
	protected ServiceRegistration managedServiceFactory;

	protected final static String keyHttpMinThreads = "http.minThreads"; //$NON-NLS-1$
	protected final static String keyHttpMaxThreads = "http.maxThreads"; //$NON-NLS-1$
	protected final static String keyHttpThreadPriority = "http.threadPriority"; //$NON-NLS-1$
	protected final static String keyHttpAddress = "http.address"; //$NON-NLS-1$
	protected final static String keyHttpPort = "http.port"; //$NON-NLS-1$
	protected final static String keyHttpScheme = "http.scheme"; //$NON-NLS-1$
	protected final static String keyHttpTimeout = "http.timeout"; //$NON-NLS-1$

	protected static final int DEFAULT_MINTHREADS = 4;
	protected static final int DEFAULT_MAXTHREADS = 20;
	protected static final int DEFAULT_THREADPRIOTRITY = Thread.NORM_PRIORITY;
	protected static String DEFAULT_HTTP_ADDRESS = "ALL"; //$NON-NLS-1$
	protected static int DEFAULT_HTTP_PORT = 80;
	protected static int DEFAULT_HTTPS_PORT = 443;
	protected static final int DEFAULT_TIMEOUT = 30;

	/** Current minimum number of threads in the thread pool */
	private int minThreads = DEFAULT_MINTHREADS;
	/** Current maximum number of threads in the thread pool */
	private int maxThreads = DEFAULT_MAXTHREADS;
	/** Current priority of the the threads in the thread pool */
	private int threadPriority = DEFAULT_THREADPRIOTRITY;

	private boolean active;

	/**
	 * Constructor - create the Http Configuration object and start it.
	 */
	protected HttpConfiguration(Http http) throws IOException {
		this.http = http;
		active = true;

		initialize();
	}

	/**
	 * Initializes fields.
	 * <p>
	 */
	protected void initialize() {
		setDefaultPorts();
		/* Create configured listeners before thread pool
		 * in case we exceed the number of threads */
		configuredListeners = new Hashtable(7);
		synchronized (configuredListeners) {
			createDefaultListeners();
		}

		pool = new HttpThreadPool(http, minThreads, maxThreads, threadPriority);

		registerManagedService();
		registerManagedServiceFactory();
	}

	protected void registerManagedService() {
		/* Register a Managed Service to handle updates to the general configuration values */
		Hashtable properties = new Hashtable(7);

		properties.put(Constants.SERVICE_VENDOR, "IBM"); //$NON-NLS-1$
		properties.put(Constants.SERVICE_DESCRIPTION, HttpMsg.OSGi_Http_Service_IBM_Implementation_16);
		properties.put(Constants.SERVICE_PID, HTTPSERVICEPID);

		managedService = http.context.registerService(ManagedService.class.getName(), this, properties);
	}

	protected void registerManagedServiceFactory() {
		/* Register a Managed Service Factory to handle updates to the unique configuration values */
		Hashtable properties = new Hashtable(7);

		properties.put(Constants.SERVICE_VENDOR, "IBM"); //$NON-NLS-1$
		properties.put(Constants.SERVICE_DESCRIPTION, HttpMsg.OSGi_Http_Service_IBM_Implementation_16);
		properties.put(Constants.SERVICE_PID, HTTPSERVICEFACTORYPID);

		managedServiceFactory = http.context.registerService(ManagedServiceFactory.class.getName(), this, properties);
	}

	/**
	 * Create default Http Listeners if requested.
	 * <p>
	 */
	protected void createDefaultListeners() {
		if (DEFAULT_HTTP_PORT != -1) {
			try {
				defaultHttpListener = new HttpListener(http, this, createProperties(DEFAULT_HTTP_ADDRESS, DEFAULT_HTTP_PORT, "http", 30));//$NON-NLS-1$
			} catch (IOException e) {
				http.logError(HttpMsg.HTTP_UNEXPECTED_IOEXCEPTION, e);
				http.logError(HttpMsg.HTTP_UNEXPECTED_RUNTIMEEXCEPTION, e);
			}
		}

	}

	/**
	 * Close default Http Listeners if present.
	 * <p>
	 */
	protected void closeDefaultListeners() {
		if (defaultHttpListener != null) {
			defaultHttpListener.close();
			defaultHttpListener = null;
		}

		if (defaultHttpsListener != null) {
			defaultHttpsListener.close();
			defaultHttpsListener = null;
		}
	}

	/**
	 * Create default Http Listeners if requested.
	 * <p>
	 */
	protected void setDefaultPorts() {
		BundleContext context = http.context;

		String property = context.getProperty(enviroKeyHttpPort);
		if (property != null) {
			try {
				DEFAULT_HTTP_PORT = Integer.parseInt(property);
			} catch (NumberFormatException e) {
				http.logWarning(NLS.bind(HttpMsg.HTTP_DEFAULT_PORT_FORMAT_EXCEPTION, enviroKeyHttpPort), e);
			}
		}

		property = context.getProperty(enviroKeyHttpsPort);
		if (property != null) {
			try {
				DEFAULT_HTTPS_PORT = Integer.parseInt(property);
			} catch (NumberFormatException e) {
				http.logWarning(NLS.bind(HttpMsg.HTTP_DEFAULT_PORT_FORMAT_EXCEPTION, enviroKeyHttpsPort), e);
			}
		}

		DEFAULT_HTTP_ADDRESS = context.getProperty(enviroKeyHttpAddress);
		if (DEFAULT_HTTP_ADDRESS == null) {
			DEFAULT_HTTP_ADDRESS = "ALL"; //$NON-NLS-1$
		}
	}

	protected Dictionary createProperties(String address, int port, String scheme, int socketTimeout) {
		Hashtable properties = new Hashtable(31);

		properties.put(Constants.SERVICE_VENDOR, "IBM"); //$NON-NLS-1$
		properties.put(Constants.SERVICE_DESCRIPTION, HttpMsg.OSGi_Http_Service_IBM_Implementation_16);
		properties.put(Constants.SERVICE_PID, "org.eclipse.equinox.http.HttpService-" + scheme); //$NON-NLS-1$
		properties.put(keyHttpAddress, address);
		properties.put(keyHttpPort, new Integer(port));
		properties.put(keyHttpScheme, scheme);
		properties.put(keyHttpTimeout, new Integer(socketTimeout));

		return (properties);
	}

	protected ServerSocketInterface createServerSocket(String address, int port, String scheme) throws IOException {
		InetAddress netAddress = null;
		if (address != null) {
			try {
				netAddress = InetAddress.getByName(address);
			} catch (UnknownHostException uhe) {
				http.logWarning(NLS.bind(HttpMsg.HTTP_HOST_UNKNOWN, address), uhe);
			}
		}
		if ("http".equalsIgnoreCase(scheme)) //$NON-NLS-1$
		{
			try {
				ServerSocketInterface ssi = new HttpServerSocket(port, 50, netAddress);
				ssi.setAddress(address);
				return ssi;
			} catch (IOException e) {
				http.logError(NLS.bind(HttpMsg.HTTP_PORT_IN_USE_EXCEPTION, new Integer(port)), e);

				throw e;
			}
		}

		if ("https".equalsIgnoreCase(scheme)) //$NON-NLS-1$
		{
			try {
				ServerSocketInterface ssi = http.createSSLServerSocket(port, 50, netAddress);
				ssi.setAddress(address);
				return ssi;
			} catch (IOException e) {
				http.logError(NLS.bind(HttpMsg.HTTP_PORT_IN_USE_EXCEPTION, new Integer(port)), e);

				throw e;
			}
		}

		throw new IOException(NLS.bind(HttpMsg.HTTP_INVALID_SCHEME_EXCEPTION, scheme));
	}

	void close() {
		active = false;

		managedService.unregister();

		managedServiceFactory.unregister();

		if (defaultHttpListener != null) {
			defaultHttpListener.close();
			defaultHttpListener = null;
		}

		if (defaultHttpsListener != null) {
			defaultHttpsListener.close();
			defaultHttpsListener = null;
		}

		Enumeration listeners = configuredListeners.elements();
		while (listeners.hasMoreElements()) {
			HttpListener listener = (HttpListener) listeners.nextElement();

			listener.close();
		}

		pool.close();
		pool = null;
	}

	/**
	 * Return a descriptive name of this ManagedServiceFactory.
	 *
	 * @return the name for the factory, which might be localized
	 */
	public String getName() {
		return HttpMsg.IBM_Http_Service_37;
	}

	/**
	 * Create a new instance, or update the configuration of an
	 * existing instance.
	 *
	 * If the PID of the <tt>Configuration</tt> object is new for the Managed Service Factory,
	 * then create a new factory instance, using the configuration
	 * <tt>properties</tt> provided. Else, update the service instance with the
	 * provided <tt>properties</tt>.
	 *
	 * <p>If the factory instance is registered with the Framework, then
	 * the configuration <tt>properties</tt> should be copied to its registry
	 * properties. This is not mandatory and
	 * security sensitive properties should obviously not be copied.
	 *
	 * <p>If this method throws any <tt>Exception</tt>, the
	 * Configuration Admin service must catch it and should log it.
	 *
	 * <p>When the implementation of updated detects any kind of
	 * error in the configuration properties, it should create a
	 * new {@Link ConfigurationException}which describes the problem.
	 *
	 * <p>The Configuration Admin service must call this method on a thread
	 * other than the thread which necessitated the callback. This implies that
	 * implementors of the <tt>ManagedServiceFactory</tt> class can be assured that the
	 * callback will not take place during registration when they
	 * execute the registration in a synchronized method.
	 *
	 * @param pid the PID for this configuration
	 * @param properties the configuration properties
	 * @throws ConfigurationException when the configuration properties are invalid
	 */
	public void updated(final String pid, final Dictionary properties) throws ConfigurationException {
		if (active) {
			String address = DEFAULT_HTTP_ADDRESS;
			int port = DEFAULT_HTTP_PORT;
			String scheme = "http"; //$NON-NLS-1$
			int timeout = DEFAULT_TIMEOUT;

			/* Get configuration values and validate */
			String key = keyHttpPort;
			Object portProperty = properties.get(key);
			if (portProperty != null) /* if null we will just use the default */
			{
				if (!(portProperty instanceof Integer)) {
					throw new ConfigurationException(key, "not an Integer"); //$NON-NLS-1$
				}

				port = ((Integer) portProperty).intValue();

				if ((port < 0) || (port > 65535)) {
					throw new ConfigurationException(key, "must be in the range 0-65535"); //$NON-NLS-1$
				}
			}

			key = keyHttpScheme;
			Object schemeProperty = properties.get(key);
			if (schemeProperty != null) /* if null we will just use the default */
			{
				if (!(schemeProperty instanceof String)) {
					throw new ConfigurationException(key, "not an String"); //$NON-NLS-1$
				}

				scheme = (String) schemeProperty;

				if (!(scheme.equals("http") || scheme.equals("https"))) //$NON-NLS-1$ //$NON-NLS-2$
				{
					throw new ConfigurationException(key, "must be either http or https"); //$NON-NLS-1$
				}
			}

			key = keyHttpAddress;
			Object addressProperty = properties.get(key);
			if (addressProperty != null) /* if null we will just use the default */
			{
				if (!(addressProperty instanceof String)) {
					throw new ConfigurationException(key, "not an String"); //$NON-NLS-1$
				}

				address = (String) addressProperty;
			}

			key = keyHttpTimeout;
			Object timeoutProperty = properties.get(key);
			if (timeoutProperty != null) /* if null we will just use the default */
			{
				if (!(timeoutProperty instanceof Integer)) {
					throw new ConfigurationException(key, "not an Integer"); //$NON-NLS-1$
				}

				timeout = ((Integer) timeoutProperty).intValue();

				if ((timeout < 0) || (timeout > 600)) {
					throw new ConfigurationException(key, "must be in the range 0-600"); //$NON-NLS-1$
				}
			}

			if (schemeProperty == null) {
				if (port == DEFAULT_HTTPS_PORT) {
					scheme = "https"; //$NON-NLS-1$
				}
			} else {
				if (portProperty == null) {
					if (scheme.equals("https")) //$NON-NLS-1$
					{
						port = DEFAULT_HTTPS_PORT;
					}
				}
			}

			if (addressProperty == null) {
				properties.put(keyHttpAddress, address);
			}
			if (schemeProperty == null) {
				properties.put(keyHttpScheme, scheme);
			}
			if (portProperty == null) {
				properties.put(keyHttpPort, new Integer(port));
			}
			if (timeoutProperty == null) {
				properties.put(keyHttpTimeout, new Integer(timeout));
			}

			properties.remove("service.bundleLocation"); /* Don't want to publish this! *///$NON-NLS-1$
			properties.put(Constants.SERVICE_VENDOR, "IBM"); //$NON-NLS-1$
			properties.put(Constants.SERVICE_DESCRIPTION, HttpMsg.OSGi_Http_Service_IBM_Implementation_16);

			/* Configuration values have been validated */
			synchronized (configuredListeners) {
				try {
					AccessController.doPrivileged(new PrivilegedExceptionAction() {
						public Object run() throws ConfigurationException {
							if (configuredListeners.size() == 0) {
								closeDefaultListeners();
							}

							HttpListener listener = (HttpListener) configuredListeners.get(pid);

							try {
								if (listener != null) {
									listener.setProperties(properties);
								} else {
									listener = new HttpListener(http, HttpConfiguration.this, properties);

									configuredListeners.put(pid, listener);
								}
							} catch (IOException e) {
								http.logError(HttpMsg.HTTP_UNEXPECTED_IOEXCEPTION, e);

								if (configuredListeners.size() == 0) {
									createDefaultListeners();
								}

								throw new ConfigurationException(null, e.getMessage());
							}

							return null;
						}
					});
				} catch (PrivilegedActionException pae) {
					throw (ConfigurationException) pae.getException();
				}
			}
		}
	}

	/**
	 * Remove a factory instance.
	 *
	 * Remove the factory instance associated with the PID. If the instance was
	 * registered with the service registry, it should be unregistered.
	 * <p>If this method throws any <tt>Exception</tt>, the Configuration Admin
	 * service must catch it and should log it.
	 * <p> The Configuration Admin service must call this method on a thread
	 * other than the thread which called <tt>delete()</tt> on the corresponding
	 * <tt>Configuration</tt> object.
	 *
	 * @param pid the PID of the service to be removed
	 */
	public void deleted(final String pid) {
		if (active) {
			synchronized (configuredListeners) {
				AccessController.doPrivileged(new PrivilegedAction() {
					public Object run() {
						HttpListener listener = (HttpListener) configuredListeners.remove(pid);

						if (listener != null) {
							listener.close();
						}

						if (configuredListeners.size() == 0) {
							createDefaultListeners();
						}

						return null;
					}
				});
			}
		}
	}

	/**
	 * Update the configuration for a Managed Service.
	 *
	 * <p>When the implementation of <tt>updated(Dictionary)</tt> detects any kind of
	 * error in the configuration properties, it should create a
	 * new <tt>ConfigurationException</tt> which describes the problem.  This
	 * can allow a management system to provide useful information to
	 * a human administrator.
	 *
	 * <p>If this method throws any other <tt>Exception</tt>, the
	 * Configuration Admin service must catch it and should log it.
	 * <p> The Configuration Admin service must call this method on a thread
	 * other than the thread which initiated the callback. This
	 * implies that implementors of Managed Service can be assured
	 * that the callback will not take place during registration
	 * when they execute the registration in a synchronized method.
	 *
	 * @param properties configuration properties, or <tt>null</tt>
	 * @throws ConfigurationException when the update fails
	 */
	public void updated(Dictionary properties) throws ConfigurationException {
		/* Since updated is called asynchronously, we may have stopped
		 * after the decision was made to call.
		 */
		if (active) {
			if (properties == null) {
				/* We have no configuration; we will just use our defaults */
				return;
			}

			int min = minThreads;
			int max = maxThreads;
			int priority = threadPriority;

			/* Get configuration values and validate */
			String key = keyHttpMinThreads;
			Object property = properties.get(key);
			if (property != null) /* if null we will just use the default */
			{
				if (!(property instanceof Integer)) {
					throw new ConfigurationException(key, "not an Integer"); //$NON-NLS-1$
				}

				min = ((Integer) property).intValue();

				if ((min < 0) || (min > 63)) {
					throw new ConfigurationException(key, "must be in the range 0-63"); //$NON-NLS-1$
				}
			}

			key = keyHttpMaxThreads;
			property = properties.get(key);
			if (property != null) /* if null we will just use the default */
			{
				if (!(property instanceof Integer)) {
					throw new ConfigurationException(key, "not an Integer"); //$NON-NLS-1$
				}

				max = ((Integer) property).intValue();

				if ((max < 0) || (max > 63)) {
					throw new ConfigurationException(key, "must be in the range 0-63"); //$NON-NLS-1$
				}
			}

			key = keyHttpThreadPriority;
			property = properties.get(key);
			if (property != null) /* if null we will just use the default */
			{
				if (!(property instanceof Integer)) {
					throw new ConfigurationException(key, "not an Integer"); //$NON-NLS-1$
				}

				priority = ((Integer) property).intValue();

				if ((priority < Thread.MIN_PRIORITY) || (priority > Thread.MAX_PRIORITY)) {
					throw new ConfigurationException(key, "must be one of the Thread defined priorities"); //$NON-NLS-1$
				}
			}

			/* Configuration values have been validated */
			if ((max != maxThreads) || (min != minThreads)) {
				pool.setSize(min, max);

				/* Get the values from the pool in case it adjusted them */
				minThreads = pool.getLowerSizeLimit();
				maxThreads = pool.getUpperSizeLimit();
			}

			if (priority != threadPriority) {
				pool.setPriority(priority);
				threadPriority = priority;
			}
		}
	}
}
