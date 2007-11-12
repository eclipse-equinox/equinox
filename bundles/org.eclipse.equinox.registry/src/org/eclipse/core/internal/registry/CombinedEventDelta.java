/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * For performance, modified extensions and extension points are stored in two forms:
 * - organized in buckets by IDs of extension points (for listeners on specific ext.point)
 * - aggregated in one list (for global listeners) 
 */
public class CombinedEventDelta {

	final private boolean addition; // true: objects were added; false: objects were removed

	// the object manager from which all the objects contained in this delta will be found
	private IObjectManager objectManager;

	// an empty array trail used to reduce re-allocations
	final static private int arrayGrowthSpace = 5;

	private Map extensionsByID = null; // extension point ID -> List of Integer extensions IDs
	private Map extPointsByID = null; // extension point ID -> List of Integer extension point IDs

	private ArrayList allExtensions = null; // List of Integer IDs
	private ArrayList allExtensionPoints = null; // List if Integer IDs

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

	private List getExtensionsBucket(String id) {
		if (extensionsByID == null)
			extensionsByID = new HashMap();
		List extensions = (List) extensionsByID.get(id);
		if (extensions == null) {
			extensions = new ArrayList(arrayGrowthSpace);
			extensionsByID.put(id, extensions);
		}
		return extensions;
	}

	private List getExtPointsBucket(String id) {
		if (extPointsByID == null)
			extPointsByID = new HashMap();
		List extensionPoints = (List) extPointsByID.get(id);
		if (extensionPoints == null) {
			extensionPoints = new ArrayList(arrayGrowthSpace);
			extPointsByID.put(id, extensionPoints);
		}
		return extensionPoints;
	}

	private List getExtPointsGlobal() {
		if (allExtensionPoints == null)
			allExtensionPoints = new ArrayList();
		return allExtensionPoints;
	}

	private List getExtensionsGlobal() {
		if (allExtensions == null)
			allExtensions = new ArrayList();
		return allExtensions;
	}

	public void rememberExtensionPoint(ExtensionPoint extensionPoint) {
		String bucketId = extensionPoint.getUniqueIdentifier();
		Object extPt = new Integer(extensionPoint.getObjectId());
		getExtPointsBucket(bucketId).add(extPt);
		getExtPointsGlobal().add(extPt);
	}

	public void rememberExtension(ExtensionPoint extensionPoint, int ext) {
		String bucketId = extensionPoint.getUniqueIdentifier();
		Object extension = new Integer(ext);

		getExtensionsBucket(bucketId).add(extension);
		getExtensionsGlobal().add(extension);
	}

	public void rememberExtensions(ExtensionPoint extensionPoint, int[] exts) {
		if (exts == null)
			return;
		if (exts.length == 0)
			return;
		for (int i = 0; i < exts.length; i++)
			rememberExtension(extensionPoint, exts[i]);
	}

	public IExtensionPoint[] getExtensionPoints(String id) {
		List extensionPoints = null;
		if (id != null && extPointsByID != null)
			extensionPoints = (List) extPointsByID.get(id);
		else if (id == null)
			extensionPoints = allExtensionPoints;
		if (extensionPoints == null) // no changes that fit the filter 
			return null;
		int size = extensionPoints.size();
		ArrayList result = new ArrayList(size);
		for (int i = 0; i < size; i++) {
			Integer extPt = (Integer) extensionPoints.get(i);
			IExtensionPoint extensionPoint = new ExtensionPointHandle(objectManager, extPt.intValue());
			result.add(extensionPoint);
		}
		if (result.size() == 0)
			return null;
		return (IExtensionPoint[]) result.toArray(new IExtensionPoint[result.size()]);
	}

	public IExtension[] getExtensions(String id) {
		List extensions = null;
		if (id != null && extensionsByID != null)
			extensions = (List) extensionsByID.get(id);
		else if (id == null)
			extensions = allExtensions;
		if (extensions == null) // no changes that fit the filter 
			return null;
		int size = extensions.size();
		ArrayList result = new ArrayList(size);
		for (int i = 0; i < size; i++) {
			Integer ext = (Integer) extensions.get(i);
			IExtension extension = new ExtensionHandle(objectManager, ext.intValue());
			result.add(extension);
		}
		return (IExtension[]) result.toArray(new IExtension[result.size()]);
	}
}
