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

package org.eclipse.equinox.internal.transforms;

import java.util.ArrayList;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A dynamic list of transformers.
 */
public class TransformerList extends ServiceTracker {

	/**
	 * The stale state of this list.  Set to true every time a new matching service instance is detected.
	 */
	private volatile boolean stale = true;

	/**
	 * Local cache of transformers.
	 */
	private StreamTransformer[] transformers;

	/**
	 * Create a new instance of this list.
	 * @param context the context to track
	 * @throws InvalidSyntaxException thrown if there's an issue listening for changes to the given transformer type
	 */
	public TransformerList(BundleContext context) throws InvalidSyntaxException {
		super(context, context.createFilter("(objectClass=" //$NON-NLS-1$
				+ Object.class.getName() + ')'), null);
		open();
	}

	/**
	 * Return the transformers being monitored by this list.  
	 * If the list is stale it will first be rebuilt.
	 * @return the transformers.
	 */
	public synchronized StreamTransformer[] getTransformers() {
		if (stale) {
			rebuildTransformerArray();
		}
		return transformers;
	}

	/**
	 * Consults the bundle context for services of the transformer type and builds the internal cache.
	 */
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
						TransformerHook.log(FrameworkLogEntry.ERROR, "Problem creating transformer", e); //$NON-NLS-1$
					} catch (NoSuchMethodException e) {
						TransformerHook.log(FrameworkLogEntry.ERROR, "Problem creating transformer", e); //$NON-NLS-1$
					}
				}
			}
			this.transformers = (StreamTransformer[]) transformerList.toArray(new StreamTransformer[transformerList.size()]);
		}

	}

	public Object addingService(ServiceReference reference) {
		try {
			return super.addingService(reference);
		} finally {
			stale = true;
		}
	}

	public void modifiedService(ServiceReference reference, Object service) {
		super.modifiedService(reference, service);
		stale = true;
	}

	public void removedService(ServiceReference reference, Object service) {
		super.removedService(reference, service);
		stale = true;
	}
}
