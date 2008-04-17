/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime;

/**
 * This interface describes a registry contributor - an entity that supplies information
 * to the extension registry. 
 * <p>
 * Registry contributor objects can be obtained by calling {@link IExtensionPoint#getContributor()}, 
 * {@link IExtension#getContributor()}, and {@link IConfigurationElement#getContributor()}.
 * Alternatively, a contributor factory appropriate for the registry in use can be called to directly
 * obtain an IContributor object.
 * </p><p>
 * This interface can be used without OSGi running.
 * </p><p>
 * This interface is not intended to be implemented or extended by clients.
 * </p>
 * @see org.eclipse.core.runtime.ContributorFactoryOSGi
 * @see org.eclipse.core.runtime.ContributorFactorySimple
 * 
 * @since org.eclipse.equinox.registry 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IContributor {

	/**
	 * Provides name of the contributor (e.g., "org.eclipse.core.runtime").
	 * 
	 * @return name of the registry contributor 
	 */
	public String getName();
}
