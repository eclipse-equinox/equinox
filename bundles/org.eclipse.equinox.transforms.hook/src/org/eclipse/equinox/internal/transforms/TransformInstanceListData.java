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

import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.eclipse.osgi.internal.log.EquinoxLogServices;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Class that represents a dynamic list of TransformTuples that have been
 * registered against a particular transform type.
 */
public class TransformInstanceListData extends ServiceTracker<URL, URL> {
	/**
	 * Used when there are no transform data types
	 */
	private static final String[] EMPTY_TYPES = new String[0];
	/**
	 * Stale state of the transform list. Set to true whenever one of the
	 * ServiceTrackerCustomization methods are invoked.
	 */
	private volatile boolean stale = true;

	/**
	 * Map from transformer class -> tuple array
	 */
	private Map<String, TransformTuple[]> transformerToTuple = new HashMap<>();

	/**
	 * List of all tuples in the system.
	 */
	private List<TransformTuple> rawTuples = new ArrayList<>();

	/**
	 * Map from bundle ID -> boolean representing whether or not a given bundle
	 * currently has any transforms registered against it.
	 */
	private Map<String, Boolean> bundleIdToTransformPresence = new HashMap<>();
	private final EquinoxLogServices logServices;

	/**
	 * Create a new transform list bound to the given context. If new transforms are
	 * registered against the given context the contents of this list will change.
	 * 
	 * @param context     the bundle context
	 * @throws InvalidSyntaxException thrown if there's an issue listening for
	 *                                changes to the given transformer type
	 */
	public TransformInstanceListData(BundleContext context, EquinoxLogServices logServices)
			throws InvalidSyntaxException {
		super(context, context.createFilter("(&(objectClass=" //$NON-NLS-1$
				+ URL.class.getName() + ")(" + TransformTuple.TRANSFORMER_TYPE //$NON-NLS-1$
				+ "=*))"), null); //$NON-NLS-1$
		this.logServices = logServices;
		open();
	}

	/**
	 * Return the transforms types currently held by this list. If a change has been
	 * detected since the last request this list will be rebuilt.
	 * 
	 * @return the transforms types currently held by this list
	 */
	public synchronized String[] getTransformTypes() {
		if (stale)
			rebuildTransformMap();

		if (transformerToTuple.size() == 0)
			return EMPTY_TYPES;
		return transformerToTuple.keySet().toArray(new String[transformerToTuple.size()]);
	}

	/**
	 * Return the transforms of a particular type currently held by this list. If a
	 * change has been detected since the last request this list will be rebuilt.
	 * 
	 * @return the transforms currently held by this list
	 */
	public synchronized TransformTuple[] getTransformsFor(String type) {
		if (stale)
			rebuildTransformMap();

		return transformerToTuple.get(type);
	}

	/**
	 * Return whether or not there are any transforms who's bundle pattern matches
	 * the ID of the provided bundle. Only transforms with a present transform
	 * handler are considered during the invocation of this method.
	 * 
	 * @param bundle the bundle to test
	 * @return the presence of associated transforms.
	 */
	public synchronized boolean hasTransformsFor(Bundle bundle) {
		if (stale)
			rebuildTransformMap();

		String bundleName = bundle.getSymbolicName();
		Boolean hasTransformsFor = bundleIdToTransformPresence.get(bundleName);

		if (hasTransformsFor == null) {
			hasTransformsFor = Boolean.FALSE;
			for (TransformTuple tuple : rawTuples) {
				if (tuple.bundlePattern.matcher(bundleName).matches()) {
					hasTransformsFor = Boolean.TRUE;
				}
			}

			bundleIdToTransformPresence.put(bundleName, hasTransformsFor);
		}

		return hasTransformsFor.booleanValue();
	}

	/**
	 * Consults the bundle context for services of the transformer type type and
	 * builds the internal cache.
	 */
	private void rebuildTransformMap() {
		transformerToTuple.clear();
		rawTuples.clear();
		bundleIdToTransformPresence.clear();

		ServiceReference<URL>[] serviceReferences = getServiceReferences();
		stale = false;
		if (serviceReferences == null)
			return;

		for (ServiceReference<URL> serviceReference : serviceReferences) {
			String type = serviceReference.getProperty(TransformTuple.TRANSFORMER_TYPE).toString();
			URL url = getService(serviceReference);
			TransformTuple[] transforms;
			try {
				transforms = CSVParser.parse(url, logServices);
				TransformTuple[] existing = transformerToTuple.get(type);
				if (existing != null) {
					TransformTuple[] newTransforms = new TransformTuple[existing.length + transforms.length];
					System.arraycopy(existing, 0, newTransforms, 0, existing.length);
					System.arraycopy(transforms, 0, newTransforms, existing.length, transforms.length);
					transformerToTuple.put(type, newTransforms);
				} else
					transformerToTuple.put(type, transforms);
				for (TransformTuple transform : transforms) {
					rawTuples.add(transform);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public URL addingService(ServiceReference<URL> reference) {
		try {
			return super.addingService(reference);
		} finally {
			stale = true;
		}
	}

	@Override
	public void modifiedService(ServiceReference<URL> reference, URL service) {
		super.modifiedService(reference, service);
		stale = true;
	}

	@Override
	public void removedService(ServiceReference<URL> reference, URL service) {
		super.removedService(reference, service);
		stale = true;
	}
}
