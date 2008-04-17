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

package org.eclipse.equinox.app;

import java.util.Map;
import org.osgi.framework.Bundle;
import org.osgi.service.application.ApplicationDescriptor;

/**
 * The context used to start an application.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @since 1.0
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IApplicationContext {
	/**
	 * A key used to store arguments for the application.  The content of this argument 
	 * is unchecked and should conform to the expectations of the application being invoked.  
	 * Typically this is a <code>String</code> array.
	 * <p>
	 * 
	 * If the map used to launch an application {@link ApplicationDescriptor#launch(Map)} does 
	 * not contain a value for this key then command line arguments used to launch 
	 * the platform are set in the arguments of the application context.
	 */
	public static final String APPLICATION_ARGS = "application.args"; //$NON-NLS-1$

	/**
	 * The arguments used for the application.  The arguments from 
	 * {@link ApplicationDescriptor#launch(Map)} are used as the arguments
	 * for this context when an application is launched.
	 * 
	 * @return a map of application arguments.
	 */
	public Map getArguments();

	/**
	 * This method should be called once the application is completely initialized and running.
	 * This method will perform certain operations that are needed once an application is running.  
	 * One example is bringing down a splash screen if it exists.
	 */
	public void applicationRunning();

	/**
	 * Returns the application associated with this application context.  This information 
	 * is used to guide the runtime as to what application extension to create and execute.
	 * 
	 * @return this product's application or <code>null</code> if none
	 */
	public String getBrandingApplication();

	/**
	 * Returns the name of the product associated with this application context.  
	 * The name is typically used in the title bar of UI windows.
	 * 
	 * @return the name of the product or <code>null</code> if none
	 */
	public String getBrandingName();

	/**
	 * Returns the text description of the product associated with this application context.
	 * 
	 * @return the description of the product or <code>null</code> if none
	 */
	public String getBrandingDescription();

	/** Returns the unique product id of the product associated with this application context.
	 * 
	 * @return the id of the product
	 */
	public String getBrandingId();

	/**
	 * Returns the property with the given key of the product associated with this application context.
	 * <code>null</code> is returned if there is no such key/value pair.
	 * 
	 * @param key the name of the property to return
	 * @return the value associated with the given key or <code>null</code> if none
	 */
	public String getBrandingProperty(String key);

	/**
	 * Returns the bundle which is responsible for the definition of the product associated with 
	 * this application context.
	 * Typically this is used as a base for searching for images and other files 
	 * that are needed in presenting the product.
	 * 
	 * @return the bundle which defines the product or <code>null</code> if none
	 */
	public Bundle getBrandingBundle();
}
