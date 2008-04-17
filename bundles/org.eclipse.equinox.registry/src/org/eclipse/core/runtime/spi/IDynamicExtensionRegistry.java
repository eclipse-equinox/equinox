/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.spi;

import org.eclipse.core.runtime.IContributor;

/**
 * This interface provides an extra degree of access to an extension registry that
 * might be useful to registry implementers.
 * <p>
 * At this time functionality available through this interface is not intended to 
 * be used with the default Eclipse extension registry.
 * </p><p>
 * <b>Note:</b> This class/interface is part of an interim SPI that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this SPI will almost certainly 
 * be broken (repeatedly) as the SPI evolves.
 * </p><p>
 * This interface is not intended to be extended by clients.
 * </p><p>
 * This interface should not be implemented by clients.
 * </p><p>
 * This interface can be used without OSGi running.
 * </p>
 * @since 3.4
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IDynamicExtensionRegistry {

	/**
	 * Removes all extensions and extension points provided by the contributor.
	 * <p>
	 * This method is an access controlled method. Access tokens are specified when the registry 
	 * is constructed by the registry implementers.
	 * </p>
	 * @see org.eclipse.core.runtime.RegistryFactory#createRegistry(RegistryStrategy, Object, Object)
	 * @param contributor the contributor to be removed
	 * @param key registry access key
	 */
	public void removeContributor(IContributor contributor, Object key);

	/**
	 * Finds out if registry has the contributor. 
	 * @param contributor registry contributor
	 * @return true if the registry has this contributor; false otherwise
	 */
	public boolean hasContributor(IContributor contributor);

	/**
	 * Returns all contributors associated with the registry at this time.
	 * @return all contributors associated with the registry
	 */
	public IContributor[] getAllContributors();

}
