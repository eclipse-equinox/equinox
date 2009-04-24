/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.baseadaptor.hooks;

import java.io.IOException;
import java.net.URLConnection;
import java.util.Properties;
import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.baseadaptor.HookRegistry;
import org.eclipse.osgi.framework.adaptor.EventPublisher;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * An AdaptorHook hooks into the <code>BaseAdaptor</code> class.
 * @see BaseAdaptor
 * @see HookRegistry#getAdaptorHooks()
 * @see HookRegistry#addAdaptorHook(AdaptorHook)
 * @since 3.2
 */
public interface AdaptorHook {
	/**
	 * Gets called by the adaptor during {@link FrameworkAdaptor#initialize(EventPublisher)}.
	 * This method allows an adaptor hook to save the adaptor object for later.
	 * @param adaptor the adaptor object associated with this AdaptorHook.
	 */
	public void initialize(BaseAdaptor adaptor);

	/**
	 * Gets called by the adaptor during {@link FrameworkAdaptor#frameworkStart(BundleContext)}.
	 * This method allows an adaptor hook to execute code when the framework is starting 
	 * (e.g. to register services).
	 * @param context the system bundle context
	 * @throws BundleException if an error occurs
	 */
	public void frameworkStart(BundleContext context) throws BundleException;

	/**
	 * Gets called by the adaptor during {@link FrameworkAdaptor#frameworkStop(BundleContext)}.
	 * This method allows an adaptor hook to execute code when the framework is stopped
	 * (e.g. to unregister services).
	 * @param context the system bundle context
	 * @throws BundleException if an error occurs.
	 */
	public void frameworkStop(BundleContext context) throws BundleException;

	/**
	 * Gets called by the adaptor during {@link FrameworkAdaptor#frameworkStopping(BundleContext)}.
	 * This method allows an adaptor hook to execute code when the framework is about to start 
	 * the shutdown process.
	 * @param context the system bundle context
	 */
	public void frameworkStopping(BundleContext context);

	/**
	 * Gets called by the adaptor during {@link FrameworkAdaptor#getProperties()}.
	 * This method allows an adaptor hook to add property values to the adaptor 
	 * properties object.
	 * @param properties the adaptor properties object.
	 */
	public void addProperties(Properties properties);

	/**
	 * Gets called by the adaptor during {@link FrameworkAdaptor#mapLocationToURLConnection(String)}.
	 * The adaptor will call this method for each configured adaptor hook until one 
	 * adaptor hook returns a non-null value.  If no adaptor hook returns a non-null value 
	 * then the adaptor will perform the default behavior.
	 * @param location a bundle location string to be converted to a URLConnection
	 * @return the URLConnection converted from the bundle location or null.
	 * @throws IOException if an error occured creating the URLConnection
	 */
	public URLConnection mapLocationToURLConnection(String location) throws IOException;

	/**
	 * Gets called by the adaptor during {@link FrameworkAdaptor#handleRuntimeError(Throwable)}.
	 * The adaptor will call this method for each configured adaptor hook.
	 * @param error the unexpected error that occured.
	 */
	public void handleRuntimeError(Throwable error);

	/**
	 * Gets called by the adaptor during {@link FrameworkAdaptor#getFrameworkLog()}.
	 * The adaptor will call this method for each configured adaptor hook until one 
	 * adaptor hook returns a non-null value.  If no adaptor hook returns a non-null value 
	 * then the adaptor will return null.
	 * @return a FrameworkLog object or null.
	 */
	public FrameworkLog createFrameworkLog();
}
