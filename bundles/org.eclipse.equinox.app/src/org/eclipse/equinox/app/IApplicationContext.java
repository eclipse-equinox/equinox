/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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
	 * A system property that may be set by an application to specify exit data
	 * for the application. The value of the property must be a <code>String</code>.
	 * <p>
	 * Typically applications do not need to set this property.  If an error is detected
	 * while launching or running an application then the launcher will set this property 
	 * automatically in order to display a message to the end user.  An application may
	 * set this property for the following reasons:
	 * <ul>
	 *   <li>To provide the command line arguments to relaunch the eclipse platform.  See
	 *   {@link IApplication#EXIT_RELAUNCH}</li>
	 *   <li>To provide an error message that will be displayed to the end user.  This will
	 *   cause an error dialog to be displayed to the user, this option should not be used
	 *   by headless applications.</li>
	 *   <li>To suppress all error dialogs displayed by the launcher this property can be 
	 *   set to the empty <code>String</code>.  This is useful for 
	 *   headless applications where error dialogs must never be displayed.</li>
	 * </ul>
	 * </p>
	 * @since 1.3
	 */
	public static final String EXIT_DATA_PROPERTY = "eclipse.exitdata"; //$NON-NLS-1$

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
	 * Exit object that indicates the application result will be delivered asynchronously.
	 * This object must be returned by the method {@link IApplication#start(IApplicationContext)}
	 * for applications which deliver a result asynchronously with the method
	 * {@link IApplicationContext#setResult(Object, IApplication)}.
	 * @since 1.3
	 */
	public static final Object EXIT_ASYNC_RESULT = new Object();

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

	/**
	 * Sets the result of the application asynchronously.  This method can only be used
	 * after the application's {@link IApplication#start(IApplicationContext) start} 
	 * method has returned the value of {@link IApplicationContext#EXIT_ASYNC_RESULT}.
	 * <p>
	 * The specified application must be the same application instance which is 
	 * associated with this application context.  In other word the application instance
	 * for which {@link IApplication#start(IApplicationContext)} was called with this 
	 * application context; otherwise an <code>IllegalArgumentException</code> is
	 * thrown.
	 * </p>
	 * 
	 * @param result the result value for the application.  May be null.
	 * @param application the application instance associated with this application context
	 * @throws IllegalStateException if {@link IApplicationContext#EXIT_ASYNC_RESULT} was
	 *   not returned by the application's {@link IApplication#start(IApplicationContext) start}
	 *   method or if the result has already been set for this application context.
	 * @throws IllegalArgumentException if the specified application is not the same 
	 *   application instance associated with this application context.
	 *   
	 * 
	 * @since 1.3
	 */
	public void setResult(Object result, IApplication application);

}
