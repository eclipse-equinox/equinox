/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
package org.eclipse.core.internal.registry;

import java.util.*;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;

/**
 * The class stores extensions and extensions points that have been actually
 * modified by a registry operation.
 *
 * For performance, modified extensions and extension points are stored in two
 * forms: - organized in buckets by IDs of extension points (for listeners on
 * specific ext.point) - aggregated in one list (for global listeners)
 */
public class CombinedEventDelta {

	final private boolean addition; // true: objects were added; false: objects were removed

	// the object manager from which all the objects contained in this delta will be
	// found
	private IObjectManager objectManager;

	// an empty array trail used to reduce re-allocations
	final static private int arrayGrowthSpace = 5;

	private Map<String, List<Integer>> extensionsByID; // extension point ID -> List of Integer extensions IDs
	private Map<String, List<Integer>> extPointsByID; // extension point ID -> List of Integer extension point IDs

	private List<Integer> allExtensions; // List of Integer IDs
	private List<Integer> allExtensionPoints; // List if Integer IDs

	private CombinedEventDelta(boolean addition) {
		this.addition = addition;
	}

	static public CombinedEventDelta recordAddition() {
		return new CombinedEventDelta(true);
	}

	static public CombinedEventDelta recordRemoval() {
		return new CombinedEventDelta(false);
	}

	public boolean isAddition() {
		return addition;
	}

	public boolean isRemoval() {
		return !addition;
	}

	public void setObjectManager(IObjectManager manager) {
		objectManager = manager;
	}

	public IObjectManager getObjectManager() {
		return objectManager;
	}

	private List<Integer> getExtensionsBucket(String id) {
		if (extensionsByID == null) {
			extensionsByID = new HashMap<>();
		}
		List<Integer> extensions = extensionsByID.get(id);
		if (extensions == null) {
			extensions = new ArrayList<>(arrayGrowthSpace);
			extensionsByID.put(id, extensions);
		}
		return extensions;
	}

	private List<Integer> getExtPointsBucket(String id) {
		if (extPointsByID == null) {
			extPointsByID = new HashMap<>();
		}
		List<Integer> extensionPoints = extPointsByID.get(id);
		if (extensionPoints == null) {
			extensionPoints = new ArrayList<>(arrayGrowthSpace);
			extPointsByID.put(id, extensionPoints);
		}
		return extensionPoints;
	}

	private List<Integer> getExtPointsGlobal() {
		if (allExtensionPoints == null) {
			allExtensionPoints = new ArrayList<>();
		}
		return allExtensionPoints;
	}

	private List<Integer> getExtensionsGlobal() {
		if (allExtensions == null) {
			allExtensions = new ArrayList<>();
		}
		return allExtensions;
	}

	public void rememberExtensionPoint(ExtensionPoint extensionPoint) {
		String bucketId = extensionPoint.getUniqueIdentifier();
		Integer extPt = Integer.valueOf(extensionPoint.getObjectId());
		getExtPointsBucket(bucketId).add(extPt);
		getExtPointsGlobal().add(extPt);
	}

	public void rememberExtension(ExtensionPoint extensionPoint, int ext) {
		String bucketId = extensionPoint.getUniqueIdentifier();
		Integer extension = Integer.valueOf(ext);

		getExtensionsBucket(bucketId).add(extension);
		getExtensionsGlobal().add(extension);
	}

	public void rememberExtensions(ExtensionPoint extensionPoint, int[] exts) {
		if (exts == null)
			return;
		if (exts.length == 0)
			return;
		for (int ext : exts)
			rememberExtension(extensionPoint, ext);
	}

	public IExtensionPoint[] getExtensionPoints(String id) {
		List<Integer> extensionPoints = null;
		if (id != null && extPointsByID != null)
			extensionPoints = extPointsByID.get(id);
		else if (id == null)
			extensionPoints = allExtensionPoints;
		if (extensionPoints == null) // no changes that fit the filter
			return null;
		int size = extensionPoints.size();
		ArrayList<IExtensionPoint> result = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			Integer extPt = extensionPoints.get(i);
			IExtensionPoint extensionPoint = new ExtensionPointHandle(objectManager, extPt.intValue());
			result.add(extensionPoint);
		}
		if (result.size() == 0)
			return null;
		return result.toArray(new IExtensionPoint[result.size()]);
	}

	public IExtension[] getExtensions(String id) {
		List<Integer> extensions = null;
		if (id != null && extensionsByID != null) {
			extensions = extensionsByID.get(id);
		} else if (id == null) {
			extensions = allExtensions;
		}
		if (extensions == null) // no changes that fit the filter
			return null;
		int size = extensions.size();
		ArrayList<IExtension> result = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			Integer ext = extensions.get(i);
			IExtension extension = new ExtensionHandle(objectManager, ext.intValue());
			result.add(extension);
		}
		return result.toArray(new IExtension[result.size()]);
	}
}
