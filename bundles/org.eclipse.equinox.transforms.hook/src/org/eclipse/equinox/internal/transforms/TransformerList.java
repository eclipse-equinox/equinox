/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.transforms;

import java.util.HashMap;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.log.EquinoxLogServices;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A dynamic list of transformers.
 */
public class TransformerList extends ServiceTracker<Object, Object> {

	/**
	 * The stale state of this list. Set to true every time a new matching service
	 * instance is detected.
	 */
	private volatile boolean stale = true;

	/**
	 * Local cache of transformers.
	 */
	private HashMap<String, StreamTransformer> transformers = new HashMap<>();
	private final EquinoxLogServices logServices;

	/**
	 * Create a new instance of this list.
	 * 
	 * @param context the context to track
	 * @throws InvalidSyntaxException thrown if there's an issue listening for
	 *                                changes to the given transformer type
	 */
	public TransformerList(BundleContext context, EquinoxLogServices logServices) throws InvalidSyntaxException {
		super(context, context.createFilter("(&(objectClass=" //$NON-NLS-1$
				+ Object.class.getName() + ")(" + TransformTuple.TRANSFORMER_TYPE //$NON-NLS-1$
				+ "=*))"), null); //$NON-NLS-1$
		this.logServices = logServices;
		open();
	}

	/**
	 * Return the transformer of the given type being monitored by this list. If the
	 * list is stale it will first be rebuilt.
	 * 
	 * @param type the type of transformer
	 * @return the transformer or null if no transformer of the given type is
	 *         available.
	 */
	public synchronized StreamTransformer getTransformer(String type) {
		if (stale) {
			rebuildTransformersMap();
		}
		return transformers.get(type);
	}

	public synchronized boolean hasTransformers() {
		if (stale) {
			rebuildTransformersMap();
		}
		return transformers.size() > 0;
	}

	/**
	 * Consults the bundle context for services of the transformer type and builds
	 * the internal cache.
	 */
	private void rebuildTransformersMap() {
		transformers.clear();
		ServiceReference<Object>[] serviceReferences = getServiceReferences();
		stale = false;
		if (serviceReferences == null)
			return;

		for (ServiceReference<Object> serviceReference : serviceReferences) {
			String type = serviceReference.getProperty(TransformTuple.TRANSFORMER_TYPE).toString();
			if (type == null || transformers.get(type) != null)
				continue;
			Object object = getService(serviceReference);
			if (object instanceof StreamTransformer)
				transformers.put(type, (StreamTransformer) object);
			else {
				ProxyStreamTransformer transformer;
				try {
					transformer = new ProxyStreamTransformer(object);
					transformers.put(type, transformer);
				} catch (SecurityException | NoSuchMethodException e) {
					logServices.log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, "Problem creating transformer", e); //$NON-NLS-1$
				}
			}
		}
	}

	public Object addingService(ServiceReference<Object> reference) {
		try {
			return super.addingService(reference);
		} finally {
			stale = true;
		}
	}

	public void modifiedService(ServiceReference<Object> reference, Object service) {
		super.modifiedService(reference, service);
		stale = true;
	}

	public void removedService(ServiceReference<Object> reference, Object service) {
		super.removedService(reference, service);
		stale = true;
	}
}
