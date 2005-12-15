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
package org.eclipse.equinox.log;

import org.osgi.framework.*;

/**
 * LogReaderServiceFactory class.
 */

public class LogReaderServiceFactory implements ServiceFactory {
	protected Activator log;

	protected LogReaderServiceFactory(Activator log) {
		this.log = log;
	}

	/**
	 * ServiceFactory.getService method.
	 */
	public Object getService(Bundle bundle, ServiceRegistration registration) {
		return (new LogReaderService(log, bundle));
	}

	/**
	 * ServiceFactory.ungetService method.
	 */
	public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
		((LogReaderService) service).close();
	}
}
