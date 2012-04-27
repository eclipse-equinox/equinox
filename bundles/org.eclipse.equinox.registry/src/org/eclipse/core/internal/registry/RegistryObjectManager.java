/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

import java.lang.ref.SoftReference;
import java.util.*;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.spi.RegistryContributor;

/**
 * This class manage all the object from the registry but does not deal with their dependencies.
 * It serves the objects which are either directly obtained from memory or read from a cache.
 * It also returns handles for objects.
 */
public class RegistryObjectManager implements IObjectManager {
	//Constants used to get the objects and their handles
	static public final byte CONFIGURATION_ELEMENT = 1;
	static public final byte EXTENSION = 2;
	static public final byte EXTENSION_POINT = 3;
	static public final byte THIRDLEVEL_CONFIGURATION_ELEMENT = 4;

	static final int CACHE_INITIAL_SIZE = 512; //This value has been picked because it is the minimal size required to startup an RCP app. (FYI, eclipse requires 3 growths).
	static final float DEFAULT_LOADFACTOR = 0.75f; //This is the default factor used in reference map.

	static final int[] EMPTY_INT_ARRAY = new int[0];
	static final String[] EMPTY_STRING_ARRAY = new String[0];

	static final ExtensionHandle[] EMPTY_EXTENSIONS_ARRAY = new ExtensionHandle[0];

	static int UNKNOWN = -1;

	// key: extensionPointName, value: object id
	private HashtableOfStringAndInt extensionPoints; //This is loaded on startup. Then entries can be added when loading a new plugin from the xml.
	// key: object id, value: an object
	private ReferenceMap cache; //Entries are added by getter. The structure is not thread safe.
	//key: int, value: int
	private OffsetTable fileOffsets = null; //This is read once on startup when loading from the cache. Entries are never added here. They are only removed to prevent "removed" objects to be reloaded.

	private int nextId = 1; //This is only used to get the next number available.

	//Those two data structures are only used when the addition or the removal of a plugin occurs.
	//They are used to keep track on a contributor basis of the extension being added or removed
	private KeyedHashSet newContributions; //represents the contributers added during this session.
	private Object formerContributions; //represents the contributers encountered in previous sessions. This is loaded lazily.

	private HashMap contributors; // key: contributor ID; value: contributor name
	private HashMap removedContributors; // key: contributor ID; value: contributor name
	private KeyedHashSet namespacesIndex; // registry elements (extension & extensionpoints) indexed by namespaces

	// Map key: extensionPointFullyQualifiedName, value int[] of orphan extensions. 
	// The orphan access does not need to be synchronized because the it is protected by the lock in extension registry.
	private Object orphanExtensions;

	private KeyedHashSet heldObjects = new KeyedHashSet(); //strong reference to the objects that must be hold on to

	//Indicate if objects have been removed or added from the table. This only needs to be set in a couple of places (addNamespace and removeNamespace)
	private boolean isDirty = false;

	private boolean fromCache = false;

	private ExtensionRegistry registry;

	// TODO this option is not used
	// OSGI system properties.  Copied from EclipseStarter
	public static final String PROP_NO_REGISTRY_FLUSHING = "eclipse.noRegistryFlushing"; //$NON-NLS-1$

	public RegistryObjectManager(ExtensionRegistry registry) {
		extensionPoints = new HashtableOfStringAndInt();
		if ("true".equalsIgnoreCase(RegistryProperties.getProperty(PROP_NO_REGISTRY_FLUSHING))) { //$NON-NLS-1$
			cache = new ReferenceMap(ReferenceMap.HARD, CACHE_INITIAL_SIZE, DEFAULT_LOADFACTOR);
		} else {
			cache = new ReferenceMap(ReferenceMap.SOFT, CACHE_INITIAL_SIZE, DEFAULT_LOADFACTOR);
		}
		newContributions = new KeyedHashSet();

		this.registry = registry;
	}

	/**
	 * Initialize the object manager. Return true if the initialization succeeded, false otherwise
	 */
	synchronized boolean init(long timeStamp) {
		TableReader reader = registry.getTableReader();
		Object[] results = reader.loadTables(timeStamp);
		if (results == null) {
			return false;
		}
		fileOffsets = (OffsetTable) results[0];
		extensionPoints = (HashtableOfStringAndInt) results[1];
		nextId = ((Integer) results[2]).intValue();
		fromCache = true;

		if (!registry.useLazyCacheLoading()) {
			//TODO Here we could grow all the tables to the right size (ReferenceMap)
			reader.setHoldObjects(true);
			markOrphansHasDirty(getOrphans());
			fromCache = reader.readAllCache(this);
			formerContributions = getFormerContributions();
		}
		return fromCache;
	}

	synchronized void addContribution(Contribution contribution) {
		isDirty = true;
		Object Id = contribution.getKey();

		KeyedElement existingContribution = getFormerContributions().getByKey(Id);
		if (existingContribution != null) { // move it from former to new contributions
			removeContribution(Id);
			newContributions.add(existingContribution);
		} else
			existingContribution = newContributions.getByKey(Id);

		if (existingContribution != null) // merge
			((Contribution) existingContribution).mergeContribution(contribution);
		else
			newContributions.add(contribution);

		updateNamespaceIndex(contribution, true);
	}

	// TODO make ExtensionPoint, Extension provide namespace in a same way (move it to the RegistryObject?)
	// See if all the registryObjects have the same namespace. If not, return null.
	// Also can return null if empty array is passed in or objects are of an unexpected type
	private String findCommonNamespaceIdentifier(RegistryObject[] registryObjects) {
		String namespaceName = null;
		for (int i = 0; i < registryObjects.length; i++) {
			RegistryObject currentObject = registryObjects[i];
			String tmp = null;
			if (currentObject instanceof ExtensionPoint)
				tmp = ((ExtensionPoint) currentObject).getNamespace();
			else if (currentObject instanceof Extension)
				tmp = ((Extension) currentObject).getNamespaceIdentifier();

			if (namespaceName == null) {
				namespaceName = tmp;
				continue;
			}
			if (!namespaceName.equals(tmp)) {
				return null;
			}
		}
		return namespaceName;
	}

	synchronized void removeExtensionPointFromNamespaceIndex(int extensionPoint, String namespaceName) {
		RegistryIndexElement indexElement = getNamespaceIndex(namespaceName);
		indexElement.updateExtensionPoint(extensionPoint, false);
	}

	synchronized void removeExtensionFromNamespaceIndex(int extensions, String namespaceName) {
		RegistryIndexElement indexElement = getNamespaceIndex(namespaceName);
		indexElement.updateExtension(extensions, false);
	}

	// Called from a synchronized method
	private void updateNamespaceIndex(Contribution contribution, boolean added) {
		// if all extension points are from the same namespace combine them in one block and add them all together
		int[] contribExtensionPoints = contribution.getExtensionPoints();
		RegistryObject[] extensionPointObjects = getObjects(contribExtensionPoints, EXTENSION_POINT);
		String commonExptsNamespace = null;
		if (contribExtensionPoints.length > 1)
			commonExptsNamespace = findCommonNamespaceIdentifier(extensionPointObjects);
		if (commonExptsNamespace != null) {
			RegistryIndexElement indexElement = getNamespaceIndex(commonExptsNamespace);
			indexElement.updateExtensionPoints(contribExtensionPoints, added);
		} else {
			for (int i = 0; i < contribExtensionPoints.length; i++) {
				String namespaceName = ((ExtensionPoint) extensionPointObjects[i]).getNamespace();
				RegistryIndexElement indexElement = getNamespaceIndex(namespaceName);
				indexElement.updateExtensionPoint(contribExtensionPoints[i], added);
			}
		}

		// if all extensions are from the same namespace combine them in one block and add them all together
		int[] contrExtensions = contribution.getExtensions();
		RegistryObject[] extensionObjects = getObjects(contrExtensions, EXTENSION);
		String commonExtNamespace = null;
		if (contrExtensions.length > 1)
			commonExtNamespace = findCommonNamespaceIdentifier(extensionObjects);
		if (commonExtNamespace != null) {
			RegistryIndexElement indexElement = getNamespaceIndex(commonExtNamespace);
			indexElement.updateExtensions(contrExtensions, added);
		} else {
			for (int i = 0; i < contrExtensions.length; i++) {
				String namespaceName = ((Extension) extensionObjects[i]).getNamespaceIdentifier();
				RegistryIndexElement indexElement = getNamespaceIndex(namespaceName);
				indexElement.updateExtension(contrExtensions[i], added);
			}
		}
	}

	synchronized int[] getExtensionPointsFrom(String id) {
		KeyedElement tmp = newContributions.getByKey(id);
		if (tmp == null)
			tmp = getFormerContributions().getByKey(id);
		if (tmp == null)
			return EMPTY_INT_ARRAY;
		return ((Contribution) tmp).getExtensionPoints();
	}

	synchronized boolean hasContribution(String id) {
		Object result = newContributions.getByKey(id);
		if (result == null)
			result = getFormerContributions().getByKey(id);
		return result != null;
	}

	private KeyedHashSet getFormerContributions() {
		KeyedHashSet result;
		if (fromCache == false)
			return new KeyedHashSet(0);

		if (formerContributions == null || (result = ((KeyedHashSet) ((formerContributions instanceof SoftReference) ? ((SoftReference) formerContributions).get() : formerContributions))) == null) {
			result = registry.getTableReader().loadContributions();
			formerContributions = new SoftReference(result);
		}
		return result;
	}

	synchronized public void add(RegistryObject registryObject, boolean hold) {
		if (registryObject.getObjectId() == UNKNOWN) {
			int id = nextId++;
			registryObject.setObjectId(id);
		}
		cache.put(registryObject.getObjectId(), registryObject);
		if (hold)
			hold(registryObject);
	}

	private void remove(RegistryObject registryObject, boolean release) {
		cache.remove(registryObject.getObjectId());
		if (release)
			release(registryObject);
	}

	synchronized void remove(int id, boolean release) {
		RegistryObject toRemove = (RegistryObject) cache.get(id);
		if (fileOffsets != null)
			fileOffsets.removeKey(id);
		if (toRemove != null)
			remove(toRemove, release);
	}

	private void hold(RegistryObject toHold) {
		heldObjects.add(toHold);
	}

	private void release(RegistryObject toRelease) {
		heldObjects.remove(toRelease);
	}

	public synchronized Object getObject(int id, byte type) {
		return basicGetObject(id, type);
	}

	private Object basicGetObject(int id, byte type) {
		Object result = cache.get(id);
		if (result != null)
			return result;
		if (fromCache)
			result = load(id, type);
		if (result == null)
			throw new InvalidRegistryObjectException();
		cache.put(id, result);
		return result;
	}

	// The current impementation of this method assumes that we don't cache dynamic 
	// extension. In this case all extensions not yet loaded (i.e. not in the memory cache) 
	// are "not dynamic" and we actually check memory objects to see if they are dynamic.
	//
	// If we decide to allow caching of dynamic objects, the implementation
	// of this method would have to retrieved the object from disk and check
	// its "dynamic" status. The problem is that id alone is not enough to get the object
	// from the disk; object type is needed as well.
	public boolean shouldPersist(int id) {
		Object result = cache.get(id);
		if (result != null)
			return ((RegistryObject) result).shouldPersist();
		return true;
	}

	public synchronized RegistryObject[] getObjects(int[] values, byte type) {
		if (values.length == 0) {
			switch (type) {
				case EXTENSION_POINT :
					return ExtensionPoint.EMPTY_ARRAY;
				case EXTENSION :
					return Extension.EMPTY_ARRAY;
				case CONFIGURATION_ELEMENT :
				case THIRDLEVEL_CONFIGURATION_ELEMENT :
					return ConfigurationElement.EMPTY_ARRAY;
			}
		}

		RegistryObject[] results = null;
		switch (type) {
			case EXTENSION_POINT :
				results = new ExtensionPoint[values.length];
				break;
			case EXTENSION :
				results = new Extension[values.length];
				break;
			case CONFIGURATION_ELEMENT :
			case THIRDLEVEL_CONFIGURATION_ELEMENT :
				results = new ConfigurationElement[values.length];
				break;
		}
		for (int i = 0; i < values.length; i++) {
			results[i] = (RegistryObject) basicGetObject(values[i], type);
		}
		return results;
	}

	synchronized ExtensionPoint getExtensionPointObject(String xptUniqueId) {
		int id;
		if ((id = extensionPoints.get(xptUniqueId)) == HashtableOfStringAndInt.MISSING_ELEMENT)
			return null;
		return (ExtensionPoint) getObject(id, EXTENSION_POINT);
	}

	public Handle getHandle(int id, byte type) {
		switch (type) {
			case EXTENSION_POINT :
				return new ExtensionPointHandle(this, id);

			case EXTENSION :
				return new ExtensionHandle(this, id);

			case CONFIGURATION_ELEMENT :
				return new ConfigurationElementHandle(this, id);

			case THIRDLEVEL_CONFIGURATION_ELEMENT :
			default : //avoid compiler error, type should always be known
				return new ThirdLevelConfigurationElementHandle(this, id);
		}
	}

	public Handle[] getHandles(int[] ids, byte type) {
		Handle[] results = null;
		int nbrId = ids.length;
		switch (type) {
			case EXTENSION_POINT :
				if (nbrId == 0)
					return ExtensionPointHandle.EMPTY_ARRAY;
				results = new ExtensionPointHandle[nbrId];
				for (int i = 0; i < nbrId; i++) {
					results[i] = new ExtensionPointHandle(this, ids[i]);
				}
				break;

			case EXTENSION :
				if (nbrId == 0)
					return ExtensionHandle.EMPTY_ARRAY;
				results = new ExtensionHandle[nbrId];
				for (int i = 0; i < nbrId; i++) {
					results[i] = new ExtensionHandle(this, ids[i]);
				}
				break;

			case CONFIGURATION_ELEMENT :
				if (nbrId == 0)
					return ConfigurationElementHandle.EMPTY_ARRAY;
				results = new ConfigurationElementHandle[nbrId];
				for (int i = 0; i < nbrId; i++) {
					results[i] = new ConfigurationElementHandle(this, ids[i]);
				}
				break;

			case THIRDLEVEL_CONFIGURATION_ELEMENT :
				if (nbrId == 0)
					return ConfigurationElementHandle.EMPTY_ARRAY;
				results = new ThirdLevelConfigurationElementHandle[nbrId];
				for (int i = 0; i < nbrId; i++) {
					results[i] = new ThirdLevelConfigurationElementHandle(this, ids[i]);
				}
				break;
		}
		return results;
	}

	synchronized ExtensionPointHandle[] getExtensionPointsHandles() {
		return (ExtensionPointHandle[]) getHandles(extensionPoints.getValues(), EXTENSION_POINT);
	}

	synchronized ExtensionPointHandle getExtensionPointHandle(String xptUniqueId) {
		int id = extensionPoints.get(xptUniqueId);
		if (id == HashtableOfStringAndInt.MISSING_ELEMENT)
			return null;
		return (ExtensionPointHandle) getHandle(id, EXTENSION_POINT);
	}

	private Object load(int id, byte type) {
		TableReader reader = registry.getTableReader();
		if (fileOffsets == null)
			return null;
		int offset = fileOffsets.get(id);
		if (offset == Integer.MIN_VALUE)
			return null;
		switch (type) {
			case CONFIGURATION_ELEMENT :
				return reader.loadConfigurationElement(offset);

			case THIRDLEVEL_CONFIGURATION_ELEMENT :
				return reader.loadThirdLevelConfigurationElements(offset, this);

			case EXTENSION :
				return reader.loadExtension(offset);

			case EXTENSION_POINT :
			default : //avoid compile errors. type must always be known
				return reader.loadExtensionPointTree(offset, this);
		}
	}

	synchronized int[] getExtensionsFrom(String contributorId) {
		KeyedElement tmp = newContributions.getByKey(contributorId);
		if (tmp == null)
			tmp = getFormerContributions().getByKey(contributorId);
		if (tmp == null)
			return EMPTY_INT_ARRAY;
		return ((Contribution) tmp).getExtensions();
	}

	synchronized boolean addExtensionPoint(ExtensionPoint currentExtPoint, boolean hold) {
		String uniqueId = currentExtPoint.getUniqueIdentifier();
		if (extensionPoints.get(uniqueId) != HashtableOfStringAndInt.MISSING_ELEMENT)
			return false;
		add(currentExtPoint, hold);
		extensionPoints.put(uniqueId, currentExtPoint.getObjectId());
		return true;
	}

	synchronized void removeExtensionPoint(String extensionPointId) {
		int pointId = extensionPoints.removeKey(extensionPointId);
		if (pointId == HashtableOfStringAndInt.MISSING_ELEMENT)
			return;
		remove(pointId, true);
	}

	public boolean isDirty() {
		return isDirty;
	}

	public void markDirty() {
		isDirty = true;
	}

	synchronized void removeContribution(Object contributorId) {
		boolean removed = newContributions.removeByKey(contributorId);
		if (removed == false) {
			removed = getFormerContributions().removeByKey(contributorId);
			if (removed)
				formerContributions = getFormerContributions(); //This forces the removed namespace to stay around, so we do not forget about removed namespaces
		}

		if (removed) {
			isDirty = true;
			return;
		}

	}

	private Map getOrphans() {
		Object result = orphanExtensions;
		if (orphanExtensions == null && !fromCache) {
			result = new HashMap();
			orphanExtensions = result;
		} else if (orphanExtensions == null || (result = ((HashMap) ((orphanExtensions instanceof SoftReference) ? ((SoftReference) orphanExtensions).get() : orphanExtensions))) == null) {
			result = registry.getTableReader().loadOrphans();
			orphanExtensions = new SoftReference(result);
		}
		return (HashMap) result;
	}

	void addOrphans(String extensionPoint, int[] extensions) {
		Map orphans = getOrphans();
		int[] existingOrphanExtensions = (int[]) orphans.get(extensionPoint);

		if (existingOrphanExtensions != null) {
			// just add
			int[] newOrphanExtensions = new int[existingOrphanExtensions.length + extensions.length];
			System.arraycopy(existingOrphanExtensions, 0, newOrphanExtensions, 0, existingOrphanExtensions.length);
			System.arraycopy(extensions, 0, newOrphanExtensions, existingOrphanExtensions.length, extensions.length);
			orphans.put(extensionPoint, newOrphanExtensions);
		} else {
			// otherwise this is the first one
			orphans.put(extensionPoint, extensions);
		}
		markOrphansHasDirty(orphans);
	}

	void markOrphansHasDirty(Map orphans) {
		orphanExtensions = orphans;
	}

	void addOrphan(String extensionPoint, int extension) {
		Map orphans = getOrphans();
		int[] existingOrphanExtensions = (int[]) orphans.get(extensionPoint);

		if (existingOrphanExtensions != null) {
			// just add
			int[] newOrphanExtensions = new int[existingOrphanExtensions.length + 1];
			System.arraycopy(existingOrphanExtensions, 0, newOrphanExtensions, 0, existingOrphanExtensions.length);
			newOrphanExtensions[existingOrphanExtensions.length] = extension;
			orphans.put(extensionPoint, newOrphanExtensions);
		} else {
			// otherwise this is the first one
			orphans.put(extensionPoint, new int[] {extension});
		}
		markOrphansHasDirty(orphans);
	}

	int[] removeOrphans(String extensionPoint) {
		Map orphans = getOrphans();
		int[] existingOrphanExtensions = (int[]) orphans.remove(extensionPoint);
		if (existingOrphanExtensions != null) {
			markOrphansHasDirty(orphans);
		}
		return existingOrphanExtensions;
	}

	void removeOrphan(String extensionPoint, int extension) {
		Map orphans = getOrphans();
		int[] existingOrphanExtensions = (int[]) orphans.get(extensionPoint);

		if (existingOrphanExtensions == null)
			return;

		markOrphansHasDirty(orphans);
		int newSize = existingOrphanExtensions.length - 1;
		if (newSize == 0) {
			orphans.remove(extensionPoint);
			return;
		}

		int[] newOrphanExtensions = new int[existingOrphanExtensions.length - 1];
		for (int i = 0, j = 0; i < existingOrphanExtensions.length; i++)
			if (extension != existingOrphanExtensions[i])
				newOrphanExtensions[j++] = existingOrphanExtensions[i];

		orphans.put(extensionPoint, newOrphanExtensions);
		return;
	}

	//This method is only used by the writer to reach in
	Map getOrphanExtensions() {
		return getOrphans();
	}

	//	This method is only used by the writer to reach in
	int getNextId() {
		return nextId;
	}

	//	This method is only used by the writer to reach in
	HashtableOfStringAndInt getExtensionPoints() {
		return extensionPoints;
	}

	//	This method is only used by the writer to reach in
	KeyedHashSet[] getContributions() {
		return new KeyedHashSet[] {newContributions, getFormerContributions()};
	}

	// This method is used internally and by the writer to reach in. Notice that it doesn't
	// return contributors marked as removed.
	HashMap getContributors() {
		if (contributors == null) {
			if (fromCache == false)
				contributors = new HashMap();
			else
				contributors = registry.getTableReader().loadContributors();
		}
		return contributors;
	}

	synchronized IContributor[] getContributorsSync() {
		Collection contributorValues = getContributors().values();
		return (IContributor[]) contributorValues.toArray(new IContributor[contributorValues.size()]);
	}

	synchronized RegistryContributor getContributor(String id) {
		RegistryContributor contributor = (RegistryContributor) getContributors().get(id);
		if (contributor != null)
			return contributor;
		// check if we have it among removed contributors - potentially
		// notification of removals might be processed after the contributor
		// marked as removed: 
		if (removedContributors != null)
			return (RegistryContributor) removedContributors.get(id);
		return null;
	}

	// only adds a contributor if it is not already present in the table
	synchronized void addContributor(RegistryContributor newContributor) {
		String key = newContributor.getActualId();
		if (!getContributors().containsKey(key)) {
			isDirty = true;
			if (removedContributors != null)
				removedContributors.remove(key);
			getContributors().put(key, newContributor);
		}
	}

	synchronized void removeContributor(String id) {
		isDirty = true;
		RegistryContributor removed = (RegistryContributor) getContributors().remove(id);
		if (removed != null) {
			if (removedContributors == null)
				removedContributors = new HashMap();
			removedContributors.put(id, removed);
		}
	}

	KeyedHashSet getNamespacesIndex() {
		if (namespacesIndex == null) {
			if (fromCache == false)
				namespacesIndex = new KeyedHashSet(0);
			else
				namespacesIndex = registry.getTableReader().loadNamespaces();
		}
		return namespacesIndex;
	}

	// Find or create required index element
	private RegistryIndexElement getNamespaceIndex(String namespaceName) {
		RegistryIndexElement indexElement = (RegistryIndexElement) getNamespacesIndex().getByKey(namespaceName);
		if (indexElement == null) {
			indexElement = new RegistryIndexElement(namespaceName);
			namespacesIndex.add(indexElement);
		}
		return indexElement;
	}

	/**
	 * Collect all the objects that are removed by this operation and store
	 * them in a IObjectManager so that they can be accessed from the appropriate
	 * deltas but not from the registry.
	 */
	synchronized Map getAssociatedObjects(String contributionId) {
		//Collect all the objects associated with this contribution
		int[] xpts = getExtensionPointsFrom(contributionId);
		int[] exts = getExtensionsFrom(contributionId);
		Map actualObjects = new HashMap(xpts.length + exts.length);
		for (int i = 0; i < exts.length; i++) {
			Extension tmp = (Extension) basicGetObject(exts[i], RegistryObjectManager.EXTENSION);
			actualObjects.put(new Integer(exts[i]), tmp);
			collectChildren(tmp, 0, actualObjects);
		}
		for (int i = 0; i < xpts.length; i++) {
			ExtensionPoint xpt = (ExtensionPoint) basicGetObject(xpts[i], RegistryObjectManager.EXTENSION_POINT);
			actualObjects.put(new Integer(xpts[i]), xpt);
		}

		return actualObjects;
	}

	/**
	 * Adds elements to be removed along with the registry object.
	 */
	synchronized void addAssociatedObjects(Map map, RegistryObject registryObject) {
		collectChildren(registryObject, 0, map);
	}

	/**
	 * Add to the set of the objects all extensions and extension points that
	 * could be navigated to from the objects in the set. 
	 */
	synchronized void addNavigableObjects(Map associatedObjects) {
		Map result = new HashMap();
		for (Iterator iter = associatedObjects.values().iterator(); iter.hasNext();) {
			RegistryObject object = (RegistryObject) iter.next();
			if (object instanceof Extension) {
				// add extension point
				ExtensionPoint extPoint = getExtensionPointObject(((Extension) object).getExtensionPointIdentifier());
				if (extPoint == null) // already removed?
					continue;

				Integer extPointIndex = new Integer(extPoint.getKeyHashCode());
				if (!associatedObjects.containsKey(extPointIndex))
					result.put(new Integer(extPoint.getKeyHashCode()), extPoint);

				// add all extensions for the extension point
				int[] extensions = extPoint.getRawChildren();
				for (int j = 0; j < extensions.length; j++) {
					Extension tmp = (Extension) basicGetObject(extensions[j], RegistryObjectManager.EXTENSION);
					if (tmp == null) // already removed
						continue;
					Integer extensionIndex = new Integer(extensions[j]);
					if (!associatedObjects.containsKey(extensionIndex)) {
						result.put(extensionIndex, tmp);
						collectChildren(tmp, 0, result);
					}
				}
			} else if (object instanceof ExtensionPoint) {
				// by now extensions of this extension point have been marked as orphans
				Map orphans = getOrphans();
				String name = ((ExtensionPoint) object).getUniqueIdentifier();
				int[] extensions = (int[]) orphans.get(name);
				if (extensions != null) {
					for (int j = 0; j < extensions.length; j++) {
						Extension tmp = (Extension) basicGetObject(extensions[j], RegistryObjectManager.EXTENSION);
						if (tmp == null) // already removed
							continue;
						Integer extensionIndex = new Integer(extensions[j]);
						if (!associatedObjects.containsKey(extensionIndex)) {
							result.put(extensionIndex, tmp);
							collectChildren(tmp, 0, result);
						}
					}
				}
			}
		}
		associatedObjects.putAll(result);
	}

	synchronized void removeObjects(Map associatedObjects) {
		//Remove the objects from the main object manager so they can no longer be accessed.
		Collection allValues = associatedObjects.values();
		for (Iterator iter = allValues.iterator(); iter.hasNext();) {
			RegistryObject toRemove = (RegistryObject) iter.next();
			remove((toRemove).getObjectId(), true);
			if (toRemove instanceof ExtensionPoint)
				removeExtensionPoint(((ExtensionPoint) toRemove).getUniqueIdentifier());
		}
	}

	IObjectManager createDelegatingObjectManager(Map object) {
		return new TemporaryObjectManager(object, this);
	}

	private void collectChildren(RegistryObject ce, int level, Map collector) {
		ConfigurationElement[] children = (ConfigurationElement[]) getObjects(ce.getRawChildren(), level == 0 || ce.noExtraData() ? RegistryObjectManager.CONFIGURATION_ELEMENT : RegistryObjectManager.THIRDLEVEL_CONFIGURATION_ELEMENT);
		for (int j = 0; j < children.length; j++) {
			collector.put(new Integer(children[j].getObjectId()), children[j]);
			collectChildren(children[j], level + 1, collector);
		}
	}

	public void close() {
		//do nothing.
	}

	public ExtensionRegistry getRegistry() {
		return registry;
	}

	// Called from a synchronized method only
	private boolean unlinkChildFromContributions(KeyedElement[] contributions, int id) {
		for (int i = 0; i < contributions.length; i++) {
			Contribution candidate = (Contribution) contributions[i];
			if (candidate == null)
				continue;
			if (candidate.hasChild(id)) {
				candidate.unlinkChild(id);
				if (candidate.isEmpty())
					removeContribution(candidate.getContributorId());
				return true;
			}
		}
		return false;
	}

	synchronized boolean unlinkChildFromContributions(int id) {
		if (unlinkChildFromContributions(newContributions.elements, id))
			return true;
		return unlinkChildFromContributions(getFormerContributions().elements, id);
	}

	synchronized public ExtensionPointHandle[] getExtensionPointsFromNamespace(String namespaceName) {
		RegistryIndexElement indexElement = getNamespaceIndex(namespaceName);
		int[] namespaceExtensionPoints = indexElement.getExtensionPoints();
		return (ExtensionPointHandle[]) getHandles(namespaceExtensionPoints, EXTENSION_POINT);
	}

	// This method filters out extensions with no extension point
	synchronized public ExtensionHandle[] getExtensionsFromNamespace(String namespaceName) {
		RegistryIndexElement indexElement = getNamespaceIndex(namespaceName);
		int[] namespaceExtensions = indexElement.getExtensions();

		// filter extensions with no extension point (orphan extensions)
		List tmp = new ArrayList();
		Extension[] exts = (Extension[]) getObjects(namespaceExtensions, EXTENSION);
		for (int i = 0; i < exts.length; i++) {
			if (getExtensionPointObject(exts[i].getExtensionPointIdentifier()) != null)
				tmp.add(getHandle(exts[i].getObjectId(), EXTENSION));
		}
		if (tmp.size() == 0)
			return EMPTY_EXTENSIONS_ARRAY;
		ExtensionHandle[] result = new ExtensionHandle[tmp.size()];
		return (ExtensionHandle[]) tmp.toArray(result);
	}

	public ExtensionHandle[] getExtensionsFromContributor(String contributorId) {
		int[] ids = getExtensionsFrom(contributorId); // never null
		return (ExtensionHandle[]) getHandles(ids, RegistryObjectManager.EXTENSION);
	}

	public ExtensionPointHandle[] getExtensionPointsFromContributor(String contributorId) {
		int[] ids = getExtensionPointsFrom(contributorId); // never null
		return (ExtensionPointHandle[]) getHandles(ids, RegistryObjectManager.EXTENSION_POINT);
	}
}
