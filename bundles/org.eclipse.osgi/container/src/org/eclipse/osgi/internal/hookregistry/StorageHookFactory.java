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

package org.eclipse.osgi.internal.hookregistry;

import java.io.*;
import java.util.Dictionary;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.osgi.framework.BundleException;

/**
 * A StorageHookFactory hooks into the persistent storage loading and saving of bundle {@link Generation generations}.
 * A factory creates StorageHook instances that get associated with each Generation object installed.<p>
 * @see Generation#getStorageHook(Class)
 * @param <S> the StorageHook type
 * @param <L> the load context type
 * @param <H> the save context type
 */
public abstract class StorageHookFactory<S, L, H extends StorageHookFactory.StorageHook<S, L>> {
	protected final String KEY = this.getClass().getName().intern();

	/**
	 * Returns the storage version of this storage hook.  This version 
	 * is used by the storage to check the consistency of cached persistent 
	 * data.  Any time a storage hook changes the format of its persistent 
	 * data the storage version should be incremented. 
	 * @return the storage version of this storage hook
	 */
	public abstract int getStorageVersion();

	/**
	 * Returns the implementation class name for the hook implementation
	 * @return the implementation class name for the hook implementation
	 */
	public final String getKey() {
		return KEY;
	}

	/**
	 * Returns true if the persisted version is compatible with the 
	 * current version of this storage hook.  The default implementation
	 * returns true if the specified version is identical to the current
	 * version.  Implementations must override this method if they
	 * want to support other (older) versions for migration purposes.
	 * @param version the persisted version
	 * @return true if the persisted version is compatible with 
	 * the current version.
	 */
	public boolean isCompatibleWith(int version) {
		return getStorageVersion() == version;
	}

	/**
	 * Creates a save context object for a storage hook.  The 
	 * save context is passed to the {@link StorageHook#save(Object, DataOutputStream)}
	 * for each generation being persisted by the framework.
	 * @return a save context object
	 */
	public S createSaveContext() {
		return null;
	}

	/**
	 * Creates a load context object for a storage hook. The
	 * load context is passed to the {@link StorageHook#load(Object, DataInputStream)}
	 * for each generation being loaded from persistent storage
	 * by the framework.
	 * @param version the persistent version
	 * @return the load context object
	 */
	public L createLoadContext(int version) {
		return null;
	}

	/**
	 * Creates a storage hook for the specified generation.
	 * @param generation the generation for the storage hook
	 * @return a storage hook
	 */
	public abstract H createStorageHook(Generation generation);

	/**
	 * A storage hook for a specific generation object.  This hook
	 * is responsible for persisting and loading data associated
	 * with a specific generation.
	 *
	 * @param <S> the save context type
	 * @param <L> the load context type
	 */
	public static abstract class StorageHook<S, L> {
		private final Class<? extends StorageHookFactory<S, L, ? extends StorageHook<S, L>>> factoryClass;
		private final Generation generation;

		public StorageHook(Generation generation, Class<? extends StorageHookFactory<S, L, ? extends StorageHook<S, L>>> factoryClass) {
			this.generation = generation;
			this.factoryClass = factoryClass;
		}

		public Generation getGeneration() {
			return generation;
		}

		/**
		 * Initializes this storage hook with the content of the specified bundle manifest.  
		 * This method is called when a bundle is installed or updated.
		 * @param manifest the bundle manifest to load into this storage hook
		 * @throws BundleException if any error occurs
		 */
		public abstract void initialize(Dictionary<String, String> manifest) throws BundleException;

		/**
		 * Loads the data from the specified 
		 * input stream into the storage hook.  This method is called during startup to 
		 * load all the persistently installed bundles. <p>
		 * It is important that this method and the {@link #save(Object, DataOutputStream)} method 
		 * stay in sync.  This method must be able to successfully read the data saved by the
		 * {@link #save(Object, DataOutputStream)} method.
		 * @param is an input stream used to load the storage hook's data from.
		 * @see #save(Object, DataOutputStream)
		 * @throws IOException if any error occurs
		 */
		public abstract void load(L loadContext, DataInputStream is) throws IOException;

		/**
		 * Saves the data from this storage hook into the specified output stream.  This method
		 * is called if some persistent data has changed for the bundle. <p>
		 * It is important that this method and the {@link #load(Object, DataInputStream)}
		 * method stay in sync.  This method must be able to save data which the 
		 * {@link #load(Object, DataInputStream)} method can ready successfully.
		 * @see #load(Object, DataInputStream)
		 * @param os an output stream used to save the storage hook's data from.
		 * @throws IOException if any error occurs
		 */
		public abstract void save(S saveContext, DataOutputStream os) throws IOException;

		/**
		 * Validates the data in this storage hook, if the data is invalid then an illegal state 
		 * exception is thrown
		 * @throws IllegalArgumentException if the data is invalid
		 */
		public void validate() throws IllegalArgumentException {
			// do nothing by default
		}

		/**
		 * The storage hook factory class of this storage hook
		 * @return the storage hook factory class
		 */
		public Class<? extends StorageHookFactory<S, L, ? extends StorageHook<S, L>>> getFactoryClass() {
			return factoryClass;
		}
	}
}
