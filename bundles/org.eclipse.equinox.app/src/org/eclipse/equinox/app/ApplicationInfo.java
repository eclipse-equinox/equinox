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

import org.eclipse.core.runtime.IProduct;
import org.eclipse.equinox.internal.app.*;
import org.eclipse.osgi.service.environment.EnvironmentInfo;

/**
 * This class provides methods to get application information such as
 * command line arguments, application arguments and product information.
 */
public class ApplicationInfo {
	/**
	 * Returns the command line args provided to the Eclipse runtime layer when it was first run.
	 * The returned value does not include arguments consumed by the lower levels of Eclipse
	 * (e.g., OSGi or the launcher).
	 * Note that individual platform runnables may be provided with different arguments
	 * if they are being run individually rather than with <code>Platform.run()</code>.
	 * <p>
	 * Clients are also able to acquire the {@link EnvironmentInfo} service and query it for
	 * the command-line arguments.
	 * </p>
	 * @return the command line used to start the platform
	 */
	public static String[] getCommandLineArgs() {
		return CommandLineArgs.getAllArgs();
	}

	/**
	 * Returns the arguments not consumed by the framework implementation itself.  Which
	 * arguments are consumed is implementation specific. These arguments are available 
	 * for use by the application.
	 * 
	 * @return the array of command line arguments not consumed by the framework.
	 */
	public static String[] getApplicationArgs() {
		return CommandLineArgs.getApplicationArgs();
	}

	/**
	 * Returns the product which was selected when running this Eclipse instance
	 * or <code>null</code> if none
	 * @return the current product or <code>null</code> if none
	 */
	public static IProduct getProduct() {
		EclipseAppContainer container = Activator.getContainer();
		return container == null ? null : container.getProduct();
	}
}
