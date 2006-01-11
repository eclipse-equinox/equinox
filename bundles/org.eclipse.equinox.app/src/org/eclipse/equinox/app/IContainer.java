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

/**
 * A container is used to launch applications.
 * @see IAppContext
 * @see IApplication
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.2
 */
public interface IContainer {
	/**
	 * Launch an application with the given application context.  The given 
	 * context must have its {@link IAppContext#setAppStatus(int)} method called by 
	 * the container when the returned application has stopped.
	 * @param context the application context to launch an application with
	 * @return the application which was launched
	 * @throws Exception if any errors occur while launching the application
	 */
	IApplication launch(IAppContext context) throws Exception;

	/**
	 * returns true if this container only allows one running application
	 * at a time.
	 * @return true if this container only allows one running application
	 * at a time.
	 */
	boolean isSingletonContainer();
}
