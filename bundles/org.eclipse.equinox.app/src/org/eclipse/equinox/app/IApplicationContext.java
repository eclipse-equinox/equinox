/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
import org.eclipse.core.runtime.IProduct;
import org.osgi.service.application.ApplicationDescriptor;

/**
 * The context used to start an application.
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
	 * Will end the splash screen for the application.  This method should be 
	 * called after the application is ready.
	 */
	public void applicationRunning();

	/**
	 * Returns the product which was selected when running this Eclipse instance
	 * or <code>null</code> if none
	 * @return the current product or <code>null</code> if none
	 */
	public IProduct getProduct();
}
