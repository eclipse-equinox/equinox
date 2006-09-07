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
import org.osgi.service.application.ApplicationDescriptor;

/**
 * The context used to start an application.
 */
public interface IApplicationContext {
	/**
	 * A key used to store arguments for the application.  The content of this argument 
	 * is unchecked and should conform to the expectations of
	 * the application being invoked.  Typically this is a 
	 * <code>String</code> array. <p>
	 */
	public static final String APPLICATION_ARGS = "application.args"; //$NON-NLS-1$

	/**
	 * The arguments used for the application.  The arguments from 
	 * {@link ApplicationDescriptor#launch(Map)} are used as the arguments
	 * for this context when an application is launched. <p>
	 * 
	 * If the {@link #APPLICATION_ARGS} key is not set in the original 
	 * map then the value from {@link ApplicationInfo#getApplicationArgs()}
	 * will be set for the value of {@link #APPLICATION_ARGS} in the returned
	 * map.
	 * @return a map of application arguments.
	 */
	public Map getArguments();

	/**
	 * Will end the splash screen for the application.  This method should be 
	 * called after the application is ready.
	 */
	public void endSplashScreen();
}
