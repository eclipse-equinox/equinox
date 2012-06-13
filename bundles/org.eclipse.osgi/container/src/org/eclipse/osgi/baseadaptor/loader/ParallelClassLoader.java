/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.baseadaptor.loader;

/**
 * A parallel class loader.  Parallel class loaders are thread safe class loaders
 * which can handle multiple threads loading classes and resources from them at 
 * the same time.  This is important for OSGi class loaders because the 
 * class loader delegate in OSGi is not strictly hierarchical, instead the 
 * delegation is grid based and may have cycles.
 * <p>
 * The {@link ClasspathManager} handles parallel capable class loaders 
 * differently from other class loaders.  For parallel capable 
 * class loaders when {@link ClasspathManager#findLocalClass(String)} is 
 * called a lock will be obtained for the class name being searched while 
 * calling {@link BaseClassLoader#publicFindLoaded(String)} and 
 * {@link BaseClassLoader#defineClass(String, byte[], ClasspathEntry, org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry)}.
 * This prevents other threads from trying to searching for the same class at the
 * same time.  For other class loaders the class loader lock is obtained 
 * instead.  This prevents other threads from trying to search for any 
 * class while the lock is held. 
 * </p>
 * <p>
 * <b>Note:</b> This interface is part of an interim API that is still under 
 * development. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will may 
 * be broken (repeatedly) as the API evolves.
 * </p>
 * @since 3.5
 */
public interface ParallelClassLoader extends BaseClassLoader {
	/**
	 * Indicates if this class loader is parallel capable.  Even
	 * if a class loader is able to be parallel capable there are some
	 * restrictions imposed by the VM which may prevent a class loader 
	 * from being parallel capable.  For example, some VMs may lock 
	 * the class loader natively before delegating to a class loader.
	 * This type of locking will prevent a class loader from being 
	 * parallel capable.
	 * @return true if this class loader is parallel capable; false otherwise.
	 */
	boolean isParallelCapable();
}
