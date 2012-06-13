/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.baseadaptor.hooks;

import java.io.*;
import java.util.Dictionary;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.util.KeyedElement;
import org.osgi.framework.BundleException;

/**
 * A StorageHook hooks into the persistent storage loading and saving.  A StorageHook gets 
 * associated with each BaseData object installed in the adaptor.<p>
 * A StorageHook extends {@link KeyedElement}, the key used for the element must be the 
 * fully qualified string name of the StorageHook implementation class.
 * @see BaseData#getStorageHook(String)
 * @since 3.2
 */
public interface StorageHook extends KeyedElement {
	/**
	 * Returns the storage version of this storage hook.  This version 
	 * is used by the storage to check the consistency of cached persistent 
	 * data.  Any time a storage hook changes the format of its persistent 
	 * data the storage version should be incremented. 
	 * @return the storage version of this storage hook
	 */
	int getStorageVersion();

	/**
	 * Creates an uninitialized storage hook for the specified bundledata.  This method 
	 * is called when a bundle is installed or updated.  The returned storage hook will be 
	 * used for the new contents of the bundle.  The returned hook will have its 
	 * {@link #initialize(Dictionary)} method called to initialize the storage hook.
	 * @param bundledata a base data the created storage hook will be associated with
	 * @return an uninitialized storage hook
	 * @throws BundleException if any error occurs
	 */
	StorageHook create(BaseData bundledata) throws BundleException;

	/**
	 * Initializes this storage hook with the content of the specified bundle manifest.  
	 * This method is called when a bundle is installed or updated.
	 * @see #create(BaseData)
	 * @see #copy(StorageHook)
	 * @param manifest the bundle manifest to load into this storage hook
	 * @throws BundleException if any error occurs
	 */
	void initialize(Dictionary<String, String> manifest) throws BundleException;

	/**
	 * Creates a new storage hook and loads the data from the specified 
	 * input stream into the storage hook.  This method is called during startup to 
	 * load all the persistently installed bundles. <p>
	 * It is important that this method and the {@link #save(DataOutputStream)} method 
	 * stay in sync.  This method must be able to successfully read the data saved by the
	 * {@link #save(DataOutputStream)} method.
	 * @param bundledata a base data the loaded storage hook will be associated with
	 * @param is an input stream used to load the storage hook's data from.
	 * @return a loaded storage hook
	 * @see #save(DataOutputStream)
	 * @throws IOException if any error occurs
	 */
	StorageHook load(BaseData bundledata, DataInputStream is) throws IOException;

	/**
	 * Saves the data from this storage hook into the specified output stream.  This method
	 * is called if some persistent data has changed for the bundle. <p>
	 * It is important that this method and the {@link #load(BaseData, DataInputStream)}
	 * method stay in sync.  This method must be able to save data which the 
	 * {@link #load(BaseData, DataInputStream)} method can ready successfully.
	 * @see #load(BaseData, DataInputStream)
	 * @param os an output stream used to save the storage hook's data from.
	 * @throws IOException if any error occurs
	 */
	void save(DataOutputStream os) throws IOException;

	/**
	 * Copies the data from the specified storage hook into this storage hook.  This method 
	 * is called when a bundle is updated to copy the data from the original bundle to a 
	 * new storage hook.  Then this storage will be initialized with the new bundle's 
	 * manifest using the {@link #initialize(Dictionary)} method.
	 * @see #create(BaseData)
	 * @see #initialize(Dictionary)
	 * @param storageHook the original storage hook to copy data out of.
	 */
	void copy(StorageHook storageHook);

	/**
	 * Validates the data in this storage hook, if the data is invalid then an illegal state 
	 * exception is thrown
	 * @throws IllegalArgumentException if the data is invalid
	 */
	void validate() throws IllegalArgumentException;

	/**
	 * Returns the manifest for the data in this storage hook, or null if this hook does
	 * not provide the manifest.  Most hooks should return null from this method.  This 
	 * method may be used to provide special handling of manifest loading.  For example,
	 * to provide a cached manfest or to do automatic manifest generation.
	 * @param firstLoad true if this is the very first time this manifest is being loaded.
	 * @return the manifest for the data in this storage hook, or null if this hook does
	 * not provide the manifest
	 * @throws BundleException 
	 */
	Dictionary<String, String> getManifest(boolean firstLoad) throws BundleException;

	/**
	 * Gets called by a base data during {@link BundleData#setStatus(int)}.
	 * A base data will call this method for each configured storage hook it
	 * is associated with until one storage hook returns true.  If all configured storage 
	 * hooks return false then the BaseData will be marked dirty and will cause the 
	 * status to be persistently saved.
	 * @param status the new status of the base data
	 * @return false if the status is not to be persistently saved; otherwise true is returned
	 */
	boolean forgetStatusChange(int status);

	/**
	 * Gets called by a base data during {@link BundleData#setStartLevel(int)}.
	 * A base data will call this method for each configured storage hook it
	 * is associated with until one storage hook returns true.  If all configured storage 
	 * hooks return false then the BaseData will be marked dirty and will cause the 
	 * start level to be persistently saved.
	 * @param startlevel the new startlevel of the base data
	 * @return false if the startlevel is not to be persistently saved; otherwise true is returned
	 */
	boolean forgetStartLevelChange(int startlevel);
}
