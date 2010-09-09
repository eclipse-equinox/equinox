/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.console;

import java.io.InputStream;
import java.io.OutputStream;
import org.osgi.framework.*;

/**
 * A console session service provides the input and output to a single console session.
 * The input will be used by the console to read in console commands.  The output will
 * be used to print the results of console commands. 
 * <p>
 * The console session must be registered as an OSGi service in order to be associated 
 * with a console instance. The console implementation will discover any console session 
 * services and will create a new console instance using the console session for input and 
 * output.  When a session is closed then the console session service will be unregistered 
 * and the console instance will terminate and be disposed of.  The console instance will 
 * also terminate if the console session service is unregistered for any reason.
 * </p>
 * @since 3.6
 */
public abstract class ConsoleSession implements ServiceFactory<Object> {
	private volatile ServiceRegistration<Object> sessionRegistration;

	/**
	 * Called by the console implementation to free resources associated
	 * with this console session.  This method will result in the console
	 * session service being unregistered from the service registry.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public final void close() {
		doClose();
		ServiceRegistration<Object> current = sessionRegistration;
		if (current != null) {
			sessionRegistration = null;
			try {
				current.unregister();
			} catch (IllegalStateException e) {
				// This can happen if the service is in the process of being 
				// unregistered or if another thread unregistered the service.
				// Ignoring the exception.
			}
		}
	}

	/**
	 * Called by the {@link #close()} method to free resources associated 
	 * with this console session.  For example, closing the streams 
	 * associated with the input and output for this session.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected abstract void doClose();

	/**
	 * Returns the input for this console session.  This input will be used
	 * to read console commands from the user of the session.
	 * @return the input for this console session
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public abstract InputStream getInput();

	/**
	 * Returns the output for this console session.  This output will be 
	 * used to write the results of console commands.
	 * @return the output for this console session.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public abstract OutputStream getOutput();

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public final Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
		if (sessionRegistration == null)
			sessionRegistration = registration;
		return this;
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public final void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
		// do nothing
	}

}
