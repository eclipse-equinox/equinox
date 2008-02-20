/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.transforms;

import java.util.ArrayList;

import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class TransformerList extends ServiceTracker {

	private volatile boolean stale = true;
	private StreamTransformer[] transformers;

	public TransformerList(BundleContext context) throws InvalidSyntaxException {
		super(context, context.createFilter("(objectClass=" //$NON-NLS-1$
				+ Object.class.getName() + ')'), null);
		open();
	}

	public synchronized StreamTransformer[] getTransformers() {
		if (stale) {
			rebuildTransformerArray();
		}
		return transformers;
	}

	private void rebuildTransformerArray() {
		Object[] services = getServices();
		stale = false;
		if (services == null) {
			transformers = new StreamTransformer[0];
		} else {
			ArrayList transformerList = new ArrayList(services.length);
			for (int i = 0; i < services.length; i++) {
				Object object = services[i];
				if (object instanceof StreamTransformer)
					transformerList.add(object);
				else {

					ProxyStreamTransformer transformer;
					try {
						transformer = new ProxyStreamTransformer(object);
						transformerList.add(transformer);
					} catch (SecurityException e) {
						TransformerHook.log(FrameworkLogEntry.ERROR,
								"Problem creating transformer", e); //$NON-NLS-1$
					} catch (NoSuchMethodException e) {
						TransformerHook.log(FrameworkLogEntry.ERROR,
								"Problem creating transformer", e); //$NON-NLS-1$
					}
				}
			}
			this.transformers = (StreamTransformer[]) transformerList
					.toArray(new StreamTransformer[transformerList.size()]);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
	 */
	public Object addingService(ServiceReference reference) {
		try {
			return super.addingService(reference);
		} finally {
			stale = true;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.util.tracker.ServiceTracker#modifiedService(org.osgi.framework.ServiceReference,
	 *      java.lang.Object)
	 */
	public void modifiedService(ServiceReference reference, Object service) {
		super.modifiedService(reference, service);
		stale = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference,
	 *      java.lang.Object)
	 */
	public void removedService(ServiceReference reference, Object service) {
		super.removedService(reference, service);
		stale = true;
	}
}
