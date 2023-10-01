/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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
package org.eclipse.osgi.container;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import org.eclipse.osgi.container.Module.StartOptions;

/**
 * A module loader is what connects a {@link ModuleWiring} to a real
 * classloader.
 * 
 * @since 3.10
 */
public abstract class ModuleLoader {
	/**
	 * Returns entries in wiring this module loader is associated with.
	 * 
	 * @param path        The path name in which to look.
	 * @param filePattern The file name pattern for selecting entries in the
	 *                    specified path
	 * @param options     The options for listing resource names.
	 * @return An unmodifiable list of URL objects for each matching entry, or an
	 *         empty list if no matching entry could be found
	 * @see ModuleWiring#findEntries(String, String, int)
	 */
	protected abstract List<URL> findEntries(String path, String filePattern, int options);

	/**
	 *
	 * @return TODO
	 * @see ModuleWiring#listResources(String, String, int)
	 */
	protected abstract Collection<String> listResources(String path, String filePattern, int options);

	/**
	 * Returns the class loader for this module loader. A <code>null</code> value
	 * will be returned if this module loader is for a fragment.
	 * 
	 * @return The class loader for this module loader.
	 * @see ModuleWiring#getClassLoader()
	 */
	protected abstract ClassLoader getClassLoader();

	/**
	 * Is called by {@link Module#start(Module.StartOptions...)} when using the
	 * {@link StartOptions#LAZY_TRIGGER} option is used.
	 * 
	 * @return false if the trigger was not previously set; otherwise true is
	 *         returned
	 */
	protected abstract boolean getAndSetTrigger();

	/**
	 * Returns true if the lazy trigger is set for this module loader
	 * 
	 * @return true if the lazy trigger is set for this module loader
	 */
	public abstract boolean isTriggerSet();

	/**
	 * Dynamically loads fragment revisions to this already resolved module loader.
	 * 
	 * @param fragments the fragments to load
	 */
	protected abstract void loadFragments(Collection<ModuleRevision> fragments);
}
