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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.wireadmin.Producer;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ProducerCustomizer implements ServiceTrackerCustomizer {

	protected Wire wire;
	protected BundleContext context;

	public ProducerCustomizer(BundleContext context, Wire wire) {
		this.wire = wire;
		this.context = context;
	}

	/**
	 * @see ServiceTrackerCustomizer#addingService(ServiceReference)
	 */
	public Object addingService(ServiceReference reference) {
		Object service = context.getService(reference);
		wire.setProducer((Producer) service, reference);
		return (service);
	}

	/**
	 * @see ServiceTrackerCustomizer#modifiedService(ServiceReference, Object)
	 */
	public void modifiedService(ServiceReference reference, Object service) {
		//the producer flavor filters may have changed
		wire.setProducerProperties(reference);
	}

	/**
	 * @see ServiceTrackerCustomizer#removedService(ServiceReference, Object)
	 */
	public void removedService(ServiceReference reference, Object service) {
		wire.removeProducer();
		context.ungetService(reference);
	}

}
