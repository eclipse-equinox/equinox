/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.wireadmin;

import org.osgi.framework.*;
import org.osgi.service.wireadmin.Consumer;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ConsumersCustomizer implements ServiceTrackerCustomizer {

	protected BundleContext context;
	protected WireAdmin wireAdmin;

	public ConsumersCustomizer(BundleContext context, WireAdmin wireAdmin) {
		this.context = context;
		this.wireAdmin = wireAdmin;
	}

	/**
	 * @see ServiceTrackerCustomizer#addingService(ServiceReference)
	 */
	public Object addingService(ServiceReference reference) {
		Consumer service = (Consumer) context.getService(reference);
		String pid = (String) reference.getProperty("service.pid"); //$NON-NLS-1$

		try {
			//if a wire contains this producer, the wire notify it
			if (wireAdmin.getWires(wireAdmin.consumerFilter + pid + ")") == null) { //$NON-NLS-1$
				wireAdmin.notifyConsumer(service, pid);
			}
		} catch (InvalidSyntaxException ex) {
			ex.printStackTrace();
		}
		return (service);
	}

	/**
	 * @see ServiceTrackerCustomizer#modifiedService(ServiceReference, Object)
	 */
	public void modifiedService(ServiceReference reference, Object service) {
		//do nothing
	}

	/**
	 * @see ServiceTrackerCustomizer#removedService(ServiceReference, Object)
	 */
	public void removedService(ServiceReference reference, Object service) {

		context.ungetService(reference);
	}
}
