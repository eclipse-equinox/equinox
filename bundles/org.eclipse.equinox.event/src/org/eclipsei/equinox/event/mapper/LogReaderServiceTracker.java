/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipsei.equinox.event.mapper;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @version $Revision: 1.4 $
 */
public class LogReaderServiceTracker extends ServiceTracker {
	private final LogReaderServiceListener	listener;
	private ServiceReference				reference;

	public LogReaderServiceTracker(BundleContext context,
			LogReaderServiceListener listener) {
		super(context, LogReaderService.class.getName(), null);
		this.listener = listener;
	}

	public Object addingService(ServiceReference reference) {
		Object object = super.addingService(reference);
		if ((object != null) && (this.reference == null)
				&& (object instanceof LogReaderService)) {
			this.reference = reference;
			listener.logReaderServiceAdding(reference,
					(LogReaderService) object);
		}
		return object;
	}

	public void removedService(ServiceReference reference, Object service) {
		if ((service != null) && (this.reference.equals(reference))
				&& (service instanceof LogReaderService)) {
			listener.logReaderServiceRemoved(reference,
					(LogReaderService) service);
			this.reference = null;
		}
		super.removedService(reference, service);
		//this method calls ungetService()
	}
}