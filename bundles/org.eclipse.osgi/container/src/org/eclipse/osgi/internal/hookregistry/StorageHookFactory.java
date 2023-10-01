/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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

package org.eclipse.osgi.internal.hookregistry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Dictionary;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleContainerAdaptor;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ModuleEvent;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.osgi.framework.BundleException;

/**
 * A StorageHookFactory hooks into the persistent storage loading and saving of
 * bundle {@link Generation generations}. A factory creates StorageHook
 * instances that get associated with each Generation object installed.
 * <p>
 * 
 * @see Generation#getStorageHook(Class)
 * @param <S> the save context type
 * @param <L> the load context type
 * @param <H> the StorageHook type
 */
public abstract class StorageHookFactory<S, L, H extends StorageHookFactory.StorageHook<S, L>> {
	protected final String KEY = this.getClass().getName().intern();

	/**
	 * Returns the storage version of this storage hook. This version is used by the
	 * storage to check the consistency of cached persistent data. Any time a
	 * storage hook changes the format of its persistent data the storage version
	 * should be incremented.
	 * 
	 * @return the storage version of this storage hook
	 */
	public int getStorageVersion() {
		return 0;
	}

	/**
	 * Returns the implementation class name for the hook implementation
	 * 
	 * @return the implementation class name for the hook implementation
	 */
	public final String getKey() {
		return KEY;
	}

	/**
	 * Returns true if the persisted version is compatible with the current version
	 * of this storage hook. The default implementation returns true if the
	 * specified version is identical to the current version. Implementations must
	 * override this method if they want to support other (older) versions for
	 * migration purposes.
	 * 
	 * @param version the persisted version
	 * @return true if the persisted version is compatible with the current version.
	 */
	public boolean isCompatibleWith(int version) {
		return getStorageVersion() == version;
	}

	/**
	 * Creates a save context object for a storage hook. The save context is passed
	 * to the {@link StorageHook#save(Object, DataOutputStream)} for each generation
	 * being persisted by the framework.
	 * 
	 * @return a save context object or {@code null} if no save context is needed
	 */
	public S createSaveContext() {
		return null;
	}

	/**
	 * Creates a load context object for a storage hook. The load context is passed
	 * to the {@link StorageHook#load(Object, DataInputStream)} for each generation
	 * being loaded from persistent storage by the framework.
	 * 
	 * @param version the persistent version
	 * @return the load context object or {@code null} if no load context is needed
	 */
	public L createLoadContext(int version) {
		return null;
	}

	/**
	 * Creates a storage hook for the specified generation.
	 * 
	 * @param generation the generation for the storage hook
	 * @return a storage hook or {@code null} if no hook is needed for the
	 *         generation
	 */
	protected H createStorageHook(Generation generation) {
		return null;
	}

	/**
	 * Creates a storage hook for the specified generation and checks that the
	 * factory class of the storage hook equals the class of this storage hook
	 * factory.
	 *
	 * @param generation - The generation for which a storage hook should be
	 *                   created.
	 * @return A newly created storage hook or {@code null} if no hook is needed for
	 *         the generation
	 * @throws IllegalStateException - If the factory class of the storage hook is
	 *                               not equal to the class of this storage hook
	 *                               factory.
	 */
	public final H createStorageHookAndValidateFactoryClass(Generation generation) {
		H result = createStorageHook(generation);
		if (result == null) {
			return result;
		}
		Class<?> factoryClass = getClass();
		Class<?> factoryClassOfStorageHook = result.getFactoryClass();
		if (!factoryClass.equals(factoryClassOfStorageHook))
			throw new IllegalStateException(String.format(
					"The factory class '%s' of storage hook '%s' does not match the creating factory class of '%s'", //$NON-NLS-1$
					factoryClassOfStorageHook.getName(), result, factoryClass.getName()));
		return result;
	}

	/**
	 * Allows a storage hook factory to handle the {@link URLConnection connection}
	 * to the content for bundle install or update operation.
	 * 
	 * @param module   the module being updated. Will be {@code null} for install
	 *                 operations
	 * @param location the bundle location be installed. Will be {@code null} for
	 *                 update operations
	 * @param in       the input stream for the install or update operation. May be
	 *                 {@code null}
	 * @return a connection to the content or {@code null} to let the framework
	 *         handle the content
	 * @throws IOException if any error occurs which will result in a
	 *                     {@link BundleException} being thrown from the update or
	 *                     install operation.
	 */
	public URLConnection handleContentConnection(Module module, String location, InputStream in) throws IOException {
		return null;
	}

	/**
	 * A storage hook for a specific generation object. This hook is responsible for
	 * persisting and loading data associated with a specific generation.
	 *
	 * @param <S> the save context type
	 * @param <L> the load context type
	 */
	public static class StorageHook<S, L> {
		private final Class<? extends StorageHookFactory<S, L, ? extends StorageHook<S, L>>> factoryClass;
		private final Generation generation;

		public StorageHook(Generation generation,
				Class<? extends StorageHookFactory<S, L, ? extends StorageHook<S, L>>> factoryClass) {
			this.generation = generation;
			this.factoryClass = factoryClass;
		}

		/**
		 * The generation associated with this hook.
		 * 
		 * @return the generation associated with this hook.
		 */
		public Generation getGeneration() {
			return generation;
		}

		/**
		 * Initializes this storage hook with the content of the specified bundle
		 * manifest. This method is called when a bundle is installed or updated.
		 * 
		 * @param manifest the bundle manifest to load into this storage hook
		 * @throws BundleException if any error occurs
		 */
		public void initialize(Dictionary<String, String> manifest) throws BundleException {
			// do nothing by default
		}

		/**
		 * Allows a builder to be modified before it is used by the framework to create
		 * a {@link ModuleRevision revision} associated with the bundle
		 * {@link #getGeneration() generation} being installed or updated.
		 * 
		 * @param operation The lifecycle operation event that is in progress using the
		 *                  supplied builder. This will be either
		 *                  {@link ModuleEvent#INSTALLED installed} or
		 *                  {@link ModuleEvent#UPDATED updated}.
		 * @param origin    The module which originated the lifecycle operation. The
		 *                  origin may be {@code null} for {@link ModuleEvent#INSTALLED
		 *                  installed} operations. This is the module passed to the
		 *                  {@link ModuleContainer#install(Module, String, ModuleRevisionBuilder, Object)
		 *                  install} or
		 *                  {@link ModuleContainer#update(Module, ModuleRevisionBuilder, Object)
		 *                  update} method.
		 * @param builder   the builder that will be used to create a new
		 *                  {@link ModuleRevision}.
		 * @return The modified builder or a completely new builder to be used by the
		 *         bundle. A {@code null} value indicates the original builder should be
		 *         used, which may have been modified by adding requirements or
		 *         capabilities.
		 * @see ModuleContainerAdaptor#adaptModuleRevisionBuilder(ModuleEvent, Module,
		 *      ModuleRevisionBuilder, Object)
		 */
		public ModuleRevisionBuilder adaptModuleRevisionBuilder(ModuleEvent operation, Module origin,
				ModuleRevisionBuilder builder) {
			// do nothing
			return null;
		}

		/**
		 * Loads the data from the specified input stream into the storage hook. This
		 * method is called during startup to load all the persistently installed
		 * bundles.
		 * <p>
		 * It is important that this method and the
		 * {@link #save(Object, DataOutputStream)} method stay in sync. This method must
		 * be able to successfully read the data saved by the
		 * {@link #save(Object, DataOutputStream)} method.
		 * 
		 * @param is an input stream used to load the storage hook's data from.
		 * @see #save(Object, DataOutputStream)
		 * @throws IOException if any error occurs
		 */
		public void load(L loadContext, DataInputStream is) throws IOException {
			// do nothing by default
		}

		/**
		 * Saves the data from this storage hook into the specified output stream. This
		 * method is called if some persistent data has changed for the bundle.
		 * <p>
		 * It is important that this method and the
		 * {@link #load(Object, DataInputStream)} method stay in sync. This method must
		 * be able to save data which the {@link #load(Object, DataInputStream)} method
		 * can ready successfully.
		 * 
		 * @see #load(Object, DataInputStream)
		 * @param os an output stream used to save the storage hook's data from.
		 * @throws IOException if any error occurs
		 */
		public void save(S saveContext, DataOutputStream os) throws IOException {
			// do nothing by default
		}

		/**
		 * Gets called during {@link Generation#delete()} to inform the hook that the
		 * generation associated with the hook is being deleted.
		 */
		public void deletingGeneration() {
			// do nothing by default
		}

		/**
		 * Validates the data in this storage hook, if the data is invalid then an
		 * illegal state exception is thrown
		 * 
		 * @throws IllegalStateException if the data is invalid
		 */
		public void validate() throws IllegalStateException {
			// do nothing by default
		}

		/**
		 * The storage hook factory class of this storage hook
		 * 
		 * @return the storage hook factory class
		 */
		public Class<? extends StorageHookFactory<S, L, ? extends StorageHook<S, L>>> getFactoryClass() {
			return factoryClass;
		}
	}
}
