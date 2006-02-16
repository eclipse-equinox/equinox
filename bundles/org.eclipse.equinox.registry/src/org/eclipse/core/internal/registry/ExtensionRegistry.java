/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.internal.registry.spi.ConfigurationElementDescription;
import org.eclipse.core.internal.registry.spi.ConfigurationElementAttribute;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.core.runtime.spi.RegistryStrategy;
import org.eclipse.osgi.storagemanager.StorageManager;
import org.eclipse.osgi.util.NLS;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * An implementation for the extension registry API.
 */
public class ExtensionRegistry implements IExtensionRegistry {

	protected class ListenerInfo {
		public String filter;
		public IRegistryChangeListener listener;

		public ListenerInfo(IRegistryChangeListener listener, String filter) {
			this.listener = listener;
			this.filter = filter;
		}

		/**
		 * Used by ListenerList to ensure uniqueness.
		 */
		public boolean equals(Object another) {
			return another instanceof ListenerInfo && ((ListenerInfo) another).listener == this.listener;
		}
	}

	// used to enforce concurrent access policy for readers/writers
	private ReadWriteMonitor access = new ReadWriteMonitor();

	// deltas not broadcasted yet. Deltas are kept organized by the namespace name (objects with the same namespace are grouped together)
	private transient Map deltas = new HashMap(11);

	//storage manager associated with the registry cache
	protected StorageManager cacheStorageManager;

	// all registry change listeners
	private transient ListenerList listeners = new ListenerList();

	private RegistryObjectManager registryObjects = null;

	// set to "true" if registry was able to use cache to populate it's content. 
	// if "false", content is empty and might need to be filled in
	protected boolean isRegistryFilledFromCache = false;

	// Table reader associated with this extension registry
	protected TableReader theTableReader = new TableReader(this);

	private Object masterToken; // use to get full control of the registry; objects created as "static" 
	private Object userToken; // use to modify non-persisted registry elements

	protected RegistryStrategy strategy; // overridable portions of the registry functionality

	public RegistryObjectManager getObjectManager() {
		return registryObjects;
	}

	/**
	 * Sets new cache file manager. If existing file manager was owned by the registry,
	 * closes it.
	 *  
	 * @param newFileManager - new cache file manager
	 * @param registryOwnsManager - true: life cycle of the file manager is controlled by the registry
	 */
	protected void setFileManager(File cacheBase, boolean isCacheReadOnly) {
		if (cacheStorageManager != null)
			cacheStorageManager.close(); // close existing file manager first

		if (cacheBase != null) {
			cacheStorageManager = new StorageManager(cacheBase, isCacheReadOnly ? "none" : null, isCacheReadOnly); //$NON-NLS-1$
			try {
				cacheStorageManager.open(!isCacheReadOnly);
			} catch (IOException e) {
				// Ignore the exception. The registry will be rebuilt from source.
			}
		}
	}

	/**
	 * Adds and resolves all extensions and extension points provided by the
	 * plug-in.
	 * <p>
	 * A corresponding IRegistryChangeEvent will be broadcast to all listeners
	 * interested on changes in the given plug-in.
	 * </p>
	 */
	private void add(Contribution element) {
		access.enterWrite();
		try {
			basicAdd(element, true);
			fireRegistryChangeEvent();
		} finally {
			access.exitWrite();
		}
	}

	/* Utility method to help with array concatenations */
	static Object concatArrays(Object a, Object b) {
		Object[] result = (Object[]) Array.newInstance(a.getClass().getComponentType(), Array.getLength(a) + Array.getLength(b));
		System.arraycopy(a, 0, result, 0, Array.getLength(a));
		System.arraycopy(b, 0, result, Array.getLength(a), Array.getLength(b));
		return result;
	}

	private String addExtension(int extension) {
		Extension addedExtension = (Extension) registryObjects.getObject(extension, RegistryObjectManager.EXTENSION);
		String extensionPointToAddTo = addedExtension.getExtensionPointIdentifier();
		ExtensionPoint extPoint = registryObjects.getExtensionPointObject(extensionPointToAddTo);
		//orphan extension
		if (extPoint == null) {
			registryObjects.addOrphan(extensionPointToAddTo, extension);
			return null;
		}
		// otherwise, link them
		int[] newExtensions;
		int[] existingExtensions = extPoint.getRawChildren();
		newExtensions = new int[existingExtensions.length + 1];
		System.arraycopy(existingExtensions, 0, newExtensions, 0, existingExtensions.length);
		newExtensions[newExtensions.length - 1] = extension;
		link(extPoint, newExtensions);
		return recordChange(extPoint, extension, IExtensionDelta.ADDED);
	}

	/**
	 * Looks for existing orphan extensions to connect to the given extension
	 * point. If none is found, there is nothing to do. Otherwise, link them.
	 */
	private String addExtensionPoint(int extPoint) {
		ExtensionPoint extensionPoint = (ExtensionPoint) registryObjects.getObject(extPoint, RegistryObjectManager.EXTENSION_POINT);
		int[] orphans = registryObjects.removeOrphans(extensionPoint.getUniqueIdentifier());
		if (orphans == null)
			return null;
		link(extensionPoint, orphans);
		return recordChange(extensionPoint, orphans, IExtensionDelta.ADDED);
	}

	private Set addExtensionsAndExtensionPoints(Contribution element) {
		// now add and resolve extensions and extension points
		Set affectedNamespaces = new HashSet();
		int[] extPoints = element.getExtensionPoints();
		for (int i = 0; i < extPoints.length; i++) {
			String namespace = this.addExtensionPoint(extPoints[i]);
			if (namespace != null)
				affectedNamespaces.add(namespace);
		}
		int[] extensions = element.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			String namespace = this.addExtension(extensions[i]);
			if (namespace != null)
				affectedNamespaces.add(namespace);
		}
		return affectedNamespaces;
	}

	public void addRegistryChangeListener(IRegistryChangeListener listener) {
		// this is just a convenience API - no need to do any sync'ing here		
		addRegistryChangeListener(listener, null);
	}

	public void addRegistryChangeListener(IRegistryChangeListener listener, String filter) {
		synchronized (listeners) {
			listeners.add(new ListenerInfo(listener, filter));
		}
	}

	private void basicAdd(Contribution element, boolean link) {
		registryObjects.addContribution(element);
		if (!link)
			return;
		Set affectedNamespaces = addExtensionsAndExtensionPoints(element);
		setObjectManagers(affectedNamespaces, registryObjects.createDelegatingObjectManager(registryObjects.getAssociatedObjects(element.getContributorId())));
	}

	private void setObjectManagers(Set affectedNamespaces, IObjectManager manager) {
		for (Iterator iter = affectedNamespaces.iterator(); iter.hasNext();) {
			getDelta((String) iter.next()).setObjectManager(manager);
		}
	}

	private void basicRemove(String contributorId) {
		// ignore anonymous namespaces
		Set affectedNamespaces = removeExtensionsAndExtensionPoints(contributorId);
		Map associatedObjects = registryObjects.getAssociatedObjects(contributorId);
		registryObjects.removeObjects(associatedObjects);
		registryObjects.addNavigableObjects(associatedObjects); // put the complete set of navigable objects
		setObjectManagers(affectedNamespaces, registryObjects.createDelegatingObjectManager(associatedObjects));

		registryObjects.removeContribution(contributorId);
		registryObjects.removeContributor(contributorId);
	}

	// allow other objects in the registry to use the same lock
	void enterRead() {
		access.enterRead();
	}

	// allow other objects in the registry to use the same lock	
	void exitRead() {
		access.exitRead();
	}

	/**
	 * Broadcasts (asynchronously) the event to all interested parties.
	 */
	private void fireRegistryChangeEvent() {
		// if there is nothing to say, just bail out
		if (deltas.isEmpty() || listeners.isEmpty())
			return;
		// for thread safety, create tmp collections
		Object[] tmpListeners = listeners.getListeners();
		Map tmpDeltas = new HashMap(this.deltas);
		// the deltas have been saved for notification - we can clear them now
		deltas.clear();
		// do the notification asynchronously
		strategy.scheduleChangeEvent(tmpListeners, tmpDeltas, this);
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExtensionRegistry#getConfigurationElementsFor(java.lang.String)
	 */
	public IConfigurationElement[] getConfigurationElementsFor(String extensionPointId) {
		// this is just a convenience API - no need to do any sync'ing here		
		int lastdot = extensionPointId.lastIndexOf('.');
		if (lastdot == -1)
			return new IConfigurationElement[0];
		return getConfigurationElementsFor(extensionPointId.substring(0, lastdot), extensionPointId.substring(lastdot + 1));
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExtensionRegistry#getConfigurationElementsFor(java.lang.String, java.lang.String)
	 */
	public IConfigurationElement[] getConfigurationElementsFor(String pluginId, String extensionPointSimpleId) {
		// this is just a convenience API - no need to do any sync'ing here
		IExtensionPoint extPoint = this.getExtensionPoint(pluginId, extensionPointSimpleId);
		if (extPoint == null)
			return new IConfigurationElement[0];
		return extPoint.getConfigurationElements();
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExtensionRegistry#getConfigurationElementsFor(java.lang.String, java.lang.String, java.lang.String)
	 */
	public IConfigurationElement[] getConfigurationElementsFor(String pluginId, String extensionPointName, String extensionId) {
		// this is just a convenience API - no need to do any sync'ing here		
		IExtension extension = this.getExtension(pluginId, extensionPointName, extensionId);
		if (extension == null)
			return new IConfigurationElement[0];
		return extension.getConfigurationElements();
	}

	private RegistryDelta getDelta(String namespace) {
		// is there a delta for the plug-in?
		RegistryDelta existingDelta = (RegistryDelta) deltas.get(namespace);
		if (existingDelta != null)
			return existingDelta;

		//if not, create one
		RegistryDelta delta = new RegistryDelta();
		deltas.put(namespace, delta);
		return delta;
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExtensionRegistry#getExtension(java.lang.String)
	 */
	public IExtension getExtension(String extensionId) {
		if (extensionId == null)
			return null;
		int lastdot = extensionId.lastIndexOf('.');
		if (lastdot == -1)
			return null;
		String namespace = extensionId.substring(0, lastdot);

		ExtensionHandle[] extensions = registryObjects.getExtensionsFromNamespace(namespace);
		for (int i = 0; i < extensions.length; i++) {
			ExtensionHandle suspect = extensions[i];
			if (extensionId.equals(suspect.getUniqueIdentifier()))
				return suspect;
		}
		return null;
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExtensionRegistry#getExtension(java.lang.String, java.lang.String)
	 */
	public IExtension getExtension(String extensionPointId, String extensionId) {
		// this is just a convenience API - no need to do any sync'ing here		
		int lastdot = extensionPointId.lastIndexOf('.');
		if (lastdot == -1)
			return null;
		return getExtension(extensionPointId.substring(0, lastdot), extensionPointId.substring(lastdot + 1), extensionId);
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExtensionRegistry#getExtension(java.lang.String, java.lang.String, java.lang.String)
	 */
	public IExtension getExtension(String pluginId, String extensionPointName, String extensionId) {
		// this is just a convenience API - no need to do any sync'ing here		
		IExtensionPoint extPoint = getExtensionPoint(pluginId, extensionPointName);
		if (extPoint != null)
			return extPoint.getExtension(extensionId);
		return null;
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExtensionRegistry#getExtensionPoint(java.lang.String)
	 */
	public IExtensionPoint getExtensionPoint(String xptUniqueId) {
		return registryObjects.getExtensionPointHandle(xptUniqueId);
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExtensionRegistry#getExtensionPoint(java.lang.String, java.lang.String)
	 */
	public IExtensionPoint getExtensionPoint(String elementName, String xpt) {
		access.enterRead();
		try {
			return registryObjects.getExtensionPointHandle(elementName + '.' + xpt);
		} finally {
			access.exitRead();
		}
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExtensionRegistry#getExtensionPoints()
	 */
	public IExtensionPoint[] getExtensionPoints() {
		access.enterRead();
		try {
			return registryObjects.getExtensionPointsHandles();
		} finally {
			access.exitRead();
		}
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExtensionRegistry#getExtensionPoints(java.lang.String)
	 */
	public IExtensionPoint[] getExtensionPoints(String namespaceName) {
		access.enterRead();
		try {
			return registryObjects.getExtensionPointsFromNamespace(namespaceName);
		} finally {
			access.exitRead();
		}
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExtensionRegistry#getExtensions(java.lang.String)
	 */
	public IExtension[] getExtensions(String namespaceName) {
		access.enterRead();
		try {
			return registryObjects.getExtensionsFromNamespace(namespaceName);
		} finally {
			access.exitRead();
		}
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExtensionRegistry#getNamespaces()
	 */
	public String[] getNamespaces() {
		access.enterRead();
		try {
			KeyedElement[] namespaceElements = registryObjects.getNamespacesIndex().elements();
			String[] namespaceNames = new String[namespaceElements.length];
			for (int i = 0; i < namespaceElements.length; i++) {
				namespaceNames[i] = (String) ((RegistryIndexElement) namespaceElements[i]).getKey();
			}
			return namespaceNames;
		} finally {
			access.exitRead();
		}
	}

	public boolean hasContribution(String name) {
		access.enterRead();
		try {
			return registryObjects.hasContribution(name);
		} finally {
			access.exitRead();
		}
	}

	private void link(ExtensionPoint extPoint, int[] extensions) {
		extPoint.setRawChildren(extensions);
		registryObjects.add(extPoint, true);
	}

	/*
	 * Records an extension addition/removal.
	 */
	private String recordChange(ExtensionPoint extPoint, int extension, int kind) {
		// avoid computing deltas when there are no listeners
		if (listeners.isEmpty())
			return null;
		ExtensionDelta extensionDelta = new ExtensionDelta();
		extensionDelta.setExtension(extension);
		extensionDelta.setExtensionPoint(extPoint.getObjectId());
		extensionDelta.setKind(kind);
		getDelta(extPoint.getNamespace()).addExtensionDelta(extensionDelta);
		return extPoint.getNamespace();
	}

	/*
	 * Records a set of extension additions/removals.
	 */
	private String recordChange(ExtensionPoint extPoint, int[] extensions, int kind) {
		if (listeners.isEmpty())
			return null;
		if (extensions == null || extensions.length == 0)
			return null;
		RegistryDelta pluginDelta = getDelta(extPoint.getNamespace());
		for (int i = 0; i < extensions.length; i++) {
			ExtensionDelta extensionDelta = new ExtensionDelta();
			extensionDelta.setExtension(extensions[i]);
			extensionDelta.setExtensionPoint(extPoint.getObjectId());
			extensionDelta.setKind(kind);
			pluginDelta.addExtensionDelta(extensionDelta);
		}
		return extPoint.getNamespace();
	}

	/**
	 * Unresolves and removes all extensions and extension points provided by
	 * the plug-in.
	 * <p>
	 * A corresponding IRegistryChangeEvent will be broadcast to all listeners
	 * interested on changes in the given plug-in.
	 * </p>
	 */
	public void remove(String removedContributorId) {
		access.enterWrite();
		try {
			basicRemove(removedContributorId);
			fireRegistryChangeEvent();
		} finally {
			access.exitWrite();
		}
	}

	//Return the affected namespace
	private String removeExtension(int extensionId) {
		Extension extension = (Extension) registryObjects.getObject(extensionId, RegistryObjectManager.EXTENSION);
		registryObjects.removeExtensionFromNamespaceIndex(extensionId, extension.getNamespaceIdentifier());
		String xptName = extension.getExtensionPointIdentifier();
		ExtensionPoint extPoint = registryObjects.getExtensionPointObject(xptName);
		if (extPoint == null) {
			registryObjects.removeOrphan(xptName, extensionId);
			return null;
		}
		// otherwise, unlink the extension from the extension point
		int[] existingExtensions = extPoint.getRawChildren();
		int[] newExtensions = RegistryObjectManager.EMPTY_INT_ARRAY;
		if (existingExtensions.length > 1) {
			if (existingExtensions.length == 1)
				newExtensions = RegistryObjectManager.EMPTY_INT_ARRAY;

			newExtensions = new int[existingExtensions.length - 1];
			for (int i = 0, j = 0; i < existingExtensions.length; i++)
				if (existingExtensions[i] != extension.getObjectId())
					newExtensions[j++] = existingExtensions[i];
		}
		link(extPoint, newExtensions);
		return recordChange(extPoint, extension.getObjectId(), IExtensionDelta.REMOVED);
	}

	private String removeExtensionPoint(int extPoint) {
		ExtensionPoint extensionPoint = (ExtensionPoint) registryObjects.getObject(extPoint, RegistryObjectManager.EXTENSION_POINT);
		registryObjects.removeExtensionPointFromNamespaceIndex(extPoint, extensionPoint.getNamespace());
		int[] existingExtensions = extensionPoint.getRawChildren();
		if (existingExtensions == null || existingExtensions.length == 0) {
			return null;
		}
		//Remove the extension point from the registry object
		registryObjects.addOrphans(extensionPoint.getUniqueIdentifier(), existingExtensions);
		link(extensionPoint, RegistryObjectManager.EMPTY_INT_ARRAY);
		return recordChange(extensionPoint, existingExtensions, IExtensionDelta.REMOVED);
	}

	private Set removeExtensionsAndExtensionPoints(String contributorId) {
		Set affectedNamespaces = new HashSet();
		int[] extensions = registryObjects.getExtensionsFrom(contributorId);
		for (int i = 0; i < extensions.length; i++) {
			String namespace = this.removeExtension(extensions[i]);
			if (namespace != null)
				affectedNamespaces.add(namespace);
		}

		// remove extension points
		int[] extPoints = registryObjects.getExtensionPointsFrom(contributorId);
		for (int i = 0; i < extPoints.length; i++) {
			String namespace = this.removeExtensionPoint(extPoints[i]);
			if (namespace != null)
				affectedNamespaces.add(namespace);
		}
		return affectedNamespaces;
	}

	public void removeRegistryChangeListener(IRegistryChangeListener listener) {
		synchronized (listeners) {
			listeners.remove(new ListenerInfo(listener, null));
		}
	}

	public ExtensionRegistry(RegistryStrategy registryStrategy, Object masterToken, Object userToken) {
		if (registryStrategy != null)
			strategy = registryStrategy;
		else
			strategy = new RegistryStrategy(null, null);

		this.masterToken = masterToken;
		this.userToken = userToken;
		registryObjects = new RegistryObjectManager(this);

		if (strategy.cacheUse()) {
			// Try to read the registry from the cache first. If that fails, create a new registry
			long start = 0;
			if (debug())
				start = System.currentTimeMillis();

			//The cache is made of several files, find the real names of these other files. If all files are found, try to initialize the objectManager
			if (checkCache()) {
				try {
					theTableReader.setTableFile(cacheStorageManager.lookup(TableReader.TABLE, false));
					theTableReader.setExtraDataFile(cacheStorageManager.lookup(TableReader.EXTRA, false));
					theTableReader.setMainDataFile(cacheStorageManager.lookup(TableReader.MAIN, false));
					theTableReader.setContributionsFile(cacheStorageManager.lookup(TableReader.CONTRIBUTIONS, false));
					theTableReader.setContributorsFile(cacheStorageManager.lookup(TableReader.CONTRIBUTORS, false));
					theTableReader.setNamespacesFile(cacheStorageManager.lookup(TableReader.NAMESPACES, false));
					theTableReader.setOrphansFile(cacheStorageManager.lookup(TableReader.ORPHANS, false));
					isRegistryFilledFromCache = registryObjects.init(computeTimeStamp());
				} catch (IOException e) {
					// The registry will be rebuilt from the xml files. Make sure to clear anything filled
					// from cache so that we won't have partially filled items.
					isRegistryFilledFromCache = false;
					clearRegistryCache();
					log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, 0, RegistryMessages.registry_bad_cache, e));
				}
			}

			if (debug() && isRegistryFilledFromCache)
				System.out.println("Reading registry cache: " + (System.currentTimeMillis() - start)); //$NON-NLS-1$

			if (debug()) {
				if (!isRegistryFilledFromCache)
					System.out.println("Reloading registry from manifest files..."); //$NON-NLS-1$
				else
					System.out.println("Using registry cache..."); //$NON-NLS-1$
			}
		}

		if (debugEvents())
			addRegistryChangeListener(new IRegistryChangeListener() {
				public void registryChanged(IRegistryChangeEvent event) {
					System.out.println(event);
				}
			});

		// Do extra start processing if specified in the registry strategy
		strategy.onStart(this);
	}

	/**
	 * Stops the registry. Registry has to be stopped to properly
	 * close cache and dispose of listeners.
	 * @param key - key token for this registry
	 */
	public void stop(Object key) {
		// If the registry creator specified a key token, check that the key mathches it 
		// (it is assumed that registry owner keeps the key to prevent unautorized accesss).
		if (masterToken != null && masterToken != key) {
			throw new IllegalArgumentException("Unauthorized access to the ExtensionRegistry.stop() method. Check if proper access token is supplied."); //$NON-NLS-1$  
		}

		// Do extra stop processing if specified in the registry strategy
		strategy.onStop(this);

		stopChangeEventScheduler();

		if (cacheStorageManager == null)
			return;

		if (!registryObjects.isDirty() || cacheStorageManager.isReadOnly()) {
			cacheStorageManager.close();
			return;
		}

		File tableFile = null;
		File mainFile = null;
		File extraFile = null;
		File contributionsFile = null;
		File contributorsFile = null;
		File namespacesFile = null;
		File orphansFile = null;

		TableWriter theTableWriter = new TableWriter(this);

		try {
			cacheStorageManager.lookup(TableReader.TABLE, true);
			cacheStorageManager.lookup(TableReader.MAIN, true);
			cacheStorageManager.lookup(TableReader.EXTRA, true);
			cacheStorageManager.lookup(TableReader.CONTRIBUTIONS, true);
			cacheStorageManager.lookup(TableReader.CONTRIBUTORS, true);
			cacheStorageManager.lookup(TableReader.NAMESPACES, true);
			cacheStorageManager.lookup(TableReader.ORPHANS, true);
			tableFile = File.createTempFile(TableReader.TABLE, ".new", cacheStorageManager.getBase()); //$NON-NLS-1$
			mainFile = File.createTempFile(TableReader.MAIN, ".new", cacheStorageManager.getBase()); //$NON-NLS-1$
			extraFile = File.createTempFile(TableReader.EXTRA, ".new", cacheStorageManager.getBase()); //$NON-NLS-1$
			contributionsFile = File.createTempFile(TableReader.CONTRIBUTIONS, ".new", cacheStorageManager.getBase()); //$NON-NLS-1$
			contributorsFile = File.createTempFile(TableReader.CONTRIBUTORS, ".new", cacheStorageManager.getBase()); //$NON-NLS-1$
			namespacesFile = File.createTempFile(TableReader.NAMESPACES, ".new", cacheStorageManager.getBase()); //$NON-NLS-1$
			orphansFile = File.createTempFile(TableReader.ORPHANS, ".new", cacheStorageManager.getBase()); //$NON-NLS-1$
			theTableWriter.setTableFile(tableFile);
			theTableWriter.setExtraDataFile(extraFile);
			theTableWriter.setMainDataFile(mainFile);
			theTableWriter.setContributionsFile(contributionsFile);
			theTableWriter.setContributorsFile(contributorsFile);
			theTableWriter.setNamespacesFile(namespacesFile);
			theTableWriter.setOrphansFile(orphansFile);
		} catch (IOException e) {
			cacheStorageManager.close();
			return; //Ignore the exception since we can recompute the cache
		}
		try {
			if (theTableWriter.saveCache(registryObjects, computeTimeStamp()))
				cacheStorageManager.update(new String[] {TableReader.TABLE, TableReader.MAIN, TableReader.EXTRA, TableReader.CONTRIBUTIONS, TableReader.CONTRIBUTORS, TableReader.NAMESPACES, TableReader.ORPHANS}, new String[] {tableFile.getName(), mainFile.getName(), extraFile.getName(), contributionsFile.getName(), contributorsFile.getName(), namespacesFile.getName(), orphansFile.getName()});
		} catch (IOException e) {
			//Ignore the exception since we can recompute the cache
		}
		theTableReader.close();
		cacheStorageManager.close();
	}

	/*
	 * Clear the registry cache files from the file manager so on next start-up we recompute it.
	 */
	public void clearRegistryCache() {
		String[] keys = new String[] {TableReader.TABLE, TableReader.MAIN, TableReader.EXTRA, TableReader.CONTRIBUTIONS, TableReader.ORPHANS};
		for (int i = 0; i < keys.length; i++)
			try {
				cacheStorageManager.remove(keys[i]);
			} catch (IOException e) {
				log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, IStatus.ERROR, RegistryMessages.meta_registryCacheReadProblems, e));
			}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////
	// Registry Object Factory
	// The factory produces contributions, extension points, extensions, and configuration elements 
	// to be stored in the extension registry.
	protected RegistryObjectFactory theRegistryObjectFactory = null;

	// Override to provide domain-specific elements to be stored in the extension registry
	protected void setElementFactory() {
		theRegistryObjectFactory = new RegistryObjectFactory(this);
	}

	// Lazy initialization.
	public RegistryObjectFactory getElementFactory() {
		if (theRegistryObjectFactory == null)
			setElementFactory();
		return theRegistryObjectFactory;
	}

	TableReader getTableReader() {
		return theTableReader;
	}

	public void log(IStatus status) {
		strategy.log(status);
	}

	public String translate(String key, ResourceBundle resources) {
		return strategy.translate(key, resources);
	}

	public boolean debug() {
		return strategy.debug();
	}

	public boolean debugEvents() {
		return strategy.debugRegistryEvents();
	}

	public boolean useLazyCacheLoading() {
		return strategy.cacheLazyLoading();
	}

	public long computeState() {
		return strategy.getContainerTimestamp();
	}

	public long computeTimeStamp() {
		return strategy.getContributionsTimestamp();
	}

	// Find the first location that contains a cache table file and set file manager to it.
	protected boolean checkCache() {
		for (int index = 0; index < strategy.getLocationsLength(); index++) {
			File possibleCacheLocation = strategy.getStorage(index);
			if (possibleCacheLocation == null)
				break; // bail out on the first null
			setFileManager(possibleCacheLocation, strategy.isCacheReadOnly(index));
			if (cacheStorageManager != null) {
				// check this new location:
				File cacheFile = null;
				try {
					cacheFile = cacheStorageManager.lookup(TableReader.getTestFileName(), false);
				} catch (IOException e) {
					//Ignore the exception. The registry will be rebuilt from the xml files.
				}
				if (cacheFile != null && cacheFile.isFile())
					return true; // found the appropriate location
			}
		}
		return false;
	}

	public boolean filledFromCache() {
		return isRegistryFilledFromCache;
	}

	public Object createExecutableExtension(RegistryContributor defaultContributor, String className, String requestedContributorName) throws CoreException {
		return strategy.createExecutableExtension(defaultContributor, className, requestedContributorName);
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// Registry change events processing

	public IStatus processChangeEvent(Object[] listenerInfos, Map deltas) {
		MultiStatus result = new MultiStatus(RegistryMessages.OWNER_NAME, IStatus.OK, RegistryMessages.plugin_eventListenerError, null);
		for (int i = 0; i < listenerInfos.length; i++) {
			ListenerInfo listenerInfo = (ListenerInfo) listenerInfos[i];
			if (listenerInfo.filter != null && !deltas.containsKey(listenerInfo.filter))
				continue;
			if (listenerInfo.listener instanceof IRegistryChangeListener)
				((IRegistryChangeListener) listenerInfo.listener).registryChanged(new RegistryChangeEvent(deltas, listenerInfo.filter));
		}
		for (Iterator iter = deltas.values().iterator(); iter.hasNext();) {
			((RegistryDelta) iter.next()).getObjectManager().close();
		}
		return result;
	}

	private RegistryEventThread eventThread = null; // registry event loop
	private final List queue = new LinkedList(); // stores registry events info

	// Registry events notifications are done on a separate thread in a sequential manner
	// (first in - first processed)
	public void scheduleChangeEvent(Object[] listenerInfos, Map deltas) {
		QueueElement newElement = new QueueElement(listenerInfos, deltas);
		if (eventThread == null) {
			eventThread = new RegistryEventThread(this);
			eventThread.start();
		}
		synchronized (queue) {
			queue.add(newElement);
			queue.notify();
		}
	}

	// The pair of values we store in the event queue
	private class QueueElement {
		Object[] listenerInfos;
		Map deltas;

		QueueElement(Object[] infos, Map deltas) {
			this.deltas = deltas;
			listenerInfos = infos;
		}
	}

	private class RegistryEventThread extends Thread {
		private ExtensionRegistry registry;

		public RegistryEventThread(ExtensionRegistry registry) {
			super("Extension Registry Event Dispatcher"); //$NON-NLS-1$
			setDaemon(true);
			this.registry = registry;
		}

		public void run() {
			while (true) {
				QueueElement element;
				synchronized (queue) {
					try {
						while (queue.isEmpty())
							queue.wait();
					} catch (InterruptedException e) {
						return;
					}
					element = (QueueElement) queue.remove(0);
				}
				registry.processChangeEvent(element.listenerInfos, element.deltas);
			}
		}
	}

	protected void stopChangeEventScheduler() {
		if (eventThread != null) {
			synchronized (queue) {
				eventThread.interrupt();
				eventThread = null;
			}
		}
	}

	/**
	 * Access check for add/remove operations:
	 * - Master key allows all operations 
	 * - User key allows modifications of non-persisted elements
	 * 
	 * @param key key to the registry supplied by the user
	 * @param persist true if operation affects persisted elements 
	 * @return true is the key grants read/write access to the registry
	 */
	private boolean checkReadWriteAccess(Object key, boolean persist) {
		if (masterToken == key)
			return true;
		if (userToken == key && !persist)
			return true;
		return false;
	}

	public boolean addContribution(InputStream is, IContributor contributor, boolean persist, String contributionName, ResourceBundle translationBundle, Object key) {
		if (!checkReadWriteAccess(key, persist))
			throw new IllegalArgumentException("Unauthorized access to the ExtensionRegistry.addXMLContribution() method. Check if proper access token is supplied."); //$NON-NLS-1$
		if (contributionName == null)
			contributionName = ""; //$NON-NLS-1$

		RegistryContributor internalContributor = (RegistryContributor) contributor;
		registryObjects.addContributor(internalContributor); // only adds a contributor if it is not already present

		String ownerName = internalContributor.getActualName();
		String message = NLS.bind(RegistryMessages.parse_problems, ownerName);
		MultiStatus problems = new MultiStatus(RegistryMessages.OWNER_NAME, ExtensionsParser.PARSE_PROBLEM, message, null);
		ExtensionsParser parser = new ExtensionsParser(problems, this);
		Contribution contribution = getElementFactory().createContribution(internalContributor.getActualId(), persist);

		try {
			parser.parseManifest(strategy.getXMLParser(), new InputSource(is), contributionName, getObjectManager(), contribution, translationBundle);
			if (problems.getSeverity() != IStatus.OK) {
				log(problems);
				return false;
			}
		} catch (ParserConfigurationException e) {
			logError(ownerName, contributionName, e);
			return false;
		} catch (SAXException e) {
			logError(ownerName, contributionName, e);
			return false;
		} catch (IOException e) {
			logError(ownerName, contributionName, e);
			return false;
		} finally {
			try {
				is.close();
			} catch (IOException ioe) {
				// nothing to do
			}
		}
		add(contribution); // the add() method does synchronization
		return true;
	}

	private void logError(String owner, String contributionName, Exception e) {
		String message = NLS.bind(RegistryMessages.parse_failedParsingManifest, owner + "/" + contributionName); //$NON-NLS-1$
		log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, 0, message, e));
	}

	/**
	 * Adds an extension point to the extension registry.
	 * <p>
	 * If the registry is not modifiable, this method is an access controlled method. 
	 * Proper token should be passed as an argument for non-modifiable registries.
	 * </p>
	 * @param identifier Id of the extension point. If non-qualified names is supplied,
	 * it will be converted internally into a fully qualified name
	 * @param contributor the contributor of this extension point
	 * @param persist indicates if contribution should be stored in the registry cache. If false,
	 * contribution is not persisted in the registry cache and is lost on Eclipse restart
	 * @param label display string for the extension point
	 * @param schemaReference reference to the extension point schema. The schema reference 
	 * is a URL path relative to the plug-in installation URL. May be null
	 * @param token the key used to check permissions. Two registry keys are set in the registry
	 * constructor {@link RegistryFactory#createRegistry(org.eclipse.core.runtime.spi.RegistryStrategy, Object, Object)}: 
	 * master token and a user token. Master token allows all operations; user token 
	 * allows non-persisted registry elements to be modified.
	 * @throws IllegalArgumentException if incorrect token is passed in
	 */
	public void addExtensionPoint(String identifier, IContributor contributor, boolean persist, String label, String schemaReference, Object token) throws IllegalArgumentException {
		if (!checkReadWriteAccess(token, persist))
			throw new IllegalArgumentException("Unauthorized access to the ExtensionRegistry.addExtensionPoint() method. Check if proper access token is supplied."); //$NON-NLS-1$

		RegistryContributor internalContributor = (RegistryContributor) contributor;
		registryObjects.addContributor(internalContributor); // only adds a contributor if it is not already present
		String contributorId = internalContributor.getActualId();

		// Extension point Id might not be null
		if (identifier == null) {
			String message = NLS.bind(RegistryMessages.create_failedExtensionPoint, label);
			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, 0, message, null));
		}
		if (schemaReference == null)
			schemaReference = ""; //$NON-NLS-1$

		// addition wraps in a contribution
		Contribution contribution = getElementFactory().createContribution(contributorId, persist);
		ExtensionPoint currentExtPoint = getElementFactory().createExtensionPoint(persist);

		String uniqueId;
		String namespaceName;
		int simpleIdStart = identifier.lastIndexOf('.');
		if (simpleIdStart == -1) {
			namespaceName = contribution.getDefaultNamespace();
			uniqueId = namespaceName + '.' + identifier;
		} else {
			namespaceName = identifier.substring(0, simpleIdStart);
			uniqueId = identifier;
		}
		currentExtPoint.setUniqueIdentifier(uniqueId);
		currentExtPoint.setNamespace(namespaceName);
		String labelNLS = translate(label, null);
		currentExtPoint.setLabel(labelNLS);
		currentExtPoint.setSchema(schemaReference);

		getObjectManager().addExtensionPoint(currentExtPoint, true);

		currentExtPoint.setContributorId(contributorId);

		// array format: {Number of extension points, Number of extensions, Extension Id}
		int[] contributionChildren = new int[3];
		// Put the extension points into this namespace
		contributionChildren[Contribution.EXTENSION_POINT] = 1;
		contributionChildren[Contribution.EXTENSION] = 0;
		contributionChildren[Contribution.EXTENSION + 1] = currentExtPoint.getObjectId();

		contribution.setRawChildren(contributionChildren);

		add(contribution);
	}

	/**
	 * Adds an extension to the extension registry.
	 * <p>
	 * If the registry is not modifiable, this method is an access controlled method. 
	 * Proper token should be passed as an argument for non-modifiable registries.
	 * </p>
	 * @see org.eclipse.core.internal.registry.spi.ConfigurationElementDescription
	 * 
	 * @param identifier Id of the extension. If non-qualified name is supplied,
	 * it will be converted internally into a fully qualified name
	 * @param contributor the contributor of this extension
	 * @param persist indicates if contribution should be stored in the registry cache. If false,
	 * contribution is not persisted in the registry cache and is lost on Eclipse restart
	 * @param label display string for this extension
	 * @param extensionPointId Id of the point being extended. If non-qualified
	 * name is supplied, it is assumed to have the same contributorId as this extension
	 * @param configurationElements contents of the extension
	 * @param token the key used to check permissions. Two registry keys are set in the registry
	 * constructor {@link RegistryFactory#createRegistry(org.eclipse.core.runtime.spi.RegistryStrategy, Object, Object)}: 
	 * master token and a user token. Master token allows all operations; user token 
	 * allows non-persisted registry elements to be modified.
	 * @throws IllegalArgumentException if incorrect token is passed in
	 */
	public void addExtension(String identifier, IContributor contributor, boolean persist, String label, String extensionPointId, ConfigurationElementDescription configurationElements, Object token) throws IllegalArgumentException {
		if (!checkReadWriteAccess(token, persist))
			throw new IllegalArgumentException("Unauthorized access to the ExtensionRegistry.addExtensionPoint() method. Check if proper access token is supplied."); //$NON-NLS-1$
		// prepare namespace information
		RegistryContributor internalContributor = (RegistryContributor) contributor;
		registryObjects.addContributor(internalContributor); // only adds a contributor if it is not already present
		String contributorId = internalContributor.getActualId();

		// addition wraps in a contribution
		Contribution contribution = getElementFactory().createContribution(contributorId, persist);
		Extension currentExtension = getElementFactory().createExtension(persist);

		String simpleId;
		String namespaceName;
		int simpleIdStart = identifier.lastIndexOf('.');
		if (simpleIdStart != -1) {
			simpleId = identifier.substring(simpleIdStart + 1);
			namespaceName = identifier.substring(0, simpleIdStart);
		} else {
			simpleId = identifier;
			namespaceName = contribution.getDefaultNamespace();
		}
		currentExtension.setSimpleIdentifier(simpleId);
		currentExtension.setNamespaceIdentifier(namespaceName);

		String extensionLabelNLS = translate(label, null);
		currentExtension.setLabel(extensionLabelNLS);

		String targetExtensionPointId;
		if (extensionPointId.indexOf('.') == -1) // No dots -> namespace name added at the start
			targetExtensionPointId = contribution.getDefaultNamespace() + '.' + extensionPointId;
		else
			targetExtensionPointId = extensionPointId;
		currentExtension.setExtensionPointIdentifier(targetExtensionPointId);

		getObjectManager().add(currentExtension, true);

		createExtensionData(contributorId, configurationElements, currentExtension, persist);

		currentExtension.setContributorId(contributorId);

		int[] contributionChildren = new int[3];

		contributionChildren[Contribution.EXTENSION_POINT] = 0;
		contributionChildren[Contribution.EXTENSION] = 1;
		contributionChildren[Contribution.EXTENSION + 1] = currentExtension.getObjectId();
		contribution.setRawChildren(contributionChildren);

		add(contribution);
	}

	// Fill in the actual content of this extension
	private void createExtensionData(String contributorId, ConfigurationElementDescription description, RegistryObject parent, boolean persist) {
		ConfigurationElement currentConfigurationElement = getElementFactory().createConfigurationElement(persist);
		currentConfigurationElement.setContributorId(contributorId);
		currentConfigurationElement.setName(description.getName());

		ConfigurationElementAttribute[] descriptionProperties = description.getAttributes();

		if (descriptionProperties != null && descriptionProperties.length != 0) {
			int len = descriptionProperties.length;
			String[] properties = new String[len * 2];
			for (int i = 0; i < len; i++) {
				properties[i * 2] = descriptionProperties[i].getName();
				properties[i * 2 + 1] = translate(descriptionProperties[i].getValue(), null);
			}
			currentConfigurationElement.setProperties(properties);
		} else
			currentConfigurationElement.setProperties(RegistryObjectManager.EMPTY_STRING_ARRAY);

		String value = description.getValue();
		if (value != null)
			currentConfigurationElement.setValue(value);

		getObjectManager().add(currentConfigurationElement, true);

		// process children
		ConfigurationElementDescription[] children = description.getChildren();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				createExtensionData(contributorId, children[i], currentConfigurationElement, persist);
			}
		}

		int[] oldValues = parent.getRawChildren();
		int size = oldValues.length;
		int[] newValues = new int[size + 1];
		for (int i = 0; i < size; i++) {
			newValues[i] = oldValues[i];
		}
		newValues[size] = currentConfigurationElement.getObjectId();
		parent.setRawChildren(newValues);
		currentConfigurationElement.setParentId(parent.getObjectId());
		currentConfigurationElement.setParentType(parent instanceof ConfigurationElement ? RegistryObjectManager.CONFIGURATION_ELEMENT : RegistryObjectManager.EXTENSION);
	}

	public boolean removeExtension(IExtension extension, Object token) throws IllegalArgumentException {
		if (!(extension instanceof ExtensionHandle))
			return false;
		return removeObject(((ExtensionHandle) extension).getObject(), false, token);
	}

	public boolean removeExtensionPoint(IExtensionPoint extensionPoint, Object token) throws IllegalArgumentException {
		if (!(extensionPoint instanceof ExtensionPointHandle))
			return false;
		return removeObject(((ExtensionPointHandle) extensionPoint).getObject(), true, token);
	}

	private boolean removeObject(RegistryObject registryObject, boolean isExtensionPoint, Object token) {
		if (!checkReadWriteAccess(token, registryObject.shouldPersist()))
			throw new IllegalArgumentException("Unauthorized access to the ExtensionRegistry.removeExtension() method. Check if proper access token is supplied."); //$NON-NLS-1$
		int id = registryObject.getObjectId();

		access.enterWrite();
		try {
			String namespace;
			if (isExtensionPoint)
				namespace = removeExtensionPoint(id);
			else
				namespace = removeExtension(id);

			Map removed = new HashMap(1);
			removed.put(new Integer(id), registryObject);
			registryObjects.removeObjects(removed);
			registryObjects.addNavigableObjects(removed);
			getDelta(namespace).setObjectManager(registryObjects.createDelegatingObjectManager(removed));

			registryObjects.unlinkChildFromContributions(id);
			fireRegistryChangeEvent();
		} finally {
			access.exitWrite();
		}
		return true;
	}

	/**
	 * <strong>EXPERIMENTAL</strong>. This method has been added as part of a work in progress. 
	 * There is a guarantee neither that this API will work nor that it will remain the same. 
	 * Please do not use this method without consulting with the Equinox team.
	 */
	public Object getTemporaryUserToken() {
		return userToken;
	}

}
