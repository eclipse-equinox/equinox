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
package org.eclipse.core.runtime.spi;

import org.eclipse.core.runtime.IContributor;

/**
 * This class describes a registry contributor which is an entity that supplies information
 * to the extension registry. Depending on the registry strategy, contributor might delegate 
 * some of its functionality to a "host" contributor. For instance, OSGi registry strategy
 * uses "host" contributor to delegate some functionality from fragments to plug-ins. 
 * <p>
 * This class can be instantiated by the registry Service Providers.
 * </p><p>
 * This class can be used without OSGi running.
 * </p><p>
 * This class can not be extended. 
 * </p><p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost certainly 
 * be broken (repeatedly) as the API evolves.
 * </p>
 * @since org.eclipse.equinox.registry 3.2
 * @noextend This class is not intended to be subclassed by clients.
 */
public final class RegistryContributor implements IContributor {

	/**
	 * Actual ID of the contributor (e.g., "12"). IDs are expected to be unique in the workspace.
	 */
	private String actualContributorId;

	/**
	 * Actual name of the contributor (e.g., "org.eclipse.core.runtime.fragment").
	 */
	private String actualContributorName;

	/**
	 * ID associated with the entity "in charge" of the contributor (e.g., "1"). IDs are expected 
	 * to be unique in the workspace. If contributor does not rely on a host, this value should be 
	 * the same as the actual contributor ID.
	 */
	private String hostId;

	/**
	 * Name of the entity "in charge" of the contributor (e.g. "org.eclipse.core.runtime").
	 * If contributor does not rely on a host, this value should be the same as the actual 
	 * contributor name.
	 */
	private String hostName;

	/**
	 * Constructor for the registry contributor.
	 * <p>
	 * The actual ID is a string identifier for the contributor (e.g., "12") and is expected 
	 * to be unique within the workspace. The actual ID of the contributor must not 
	 * be <code>null</code>.
	 * </p><p>
	 * The actual name is the name associated with the contributor 
	 * (e.g., "org.eclipse.core.runtime.fragment"). The actual name of the contributor must 
	 * not be <code>null</code>. 
	 * </p><p>
	 * The host ID is the identifier associated with the entity "in charge" of the contributor
	 * (e.g., "1"). IDs are expected to be unique in the workspace. If contributor does not 
	 * rely on a host, then <code>null</code> should be used as the host ID.
	 * </p><p>
	 * The host name is the name of the entity "in charge" of the contributor 
	 * (e.g., "org.eclipse.core.runtime"). If contributor does not rely on a host, then
	 * <code>null</code> should be used as the host name.
	 * </p><p>
	 * There should be 1-to-1 mapping between the contributor and the contibutor ID. 
	 * The IDs (either actual or host) can not be re-used in the same registry.
	 * For example, if ID of 12 was used to identify contributorA, the ID of 12 can not
	 * be used to identify contributorB or a host for the contributorC.
	 * </p>
	 * @param actualId contributor identifier
	 * @param actualName name of the contributor
	 * @param hostId id associated with the host, or <code>null</code>
	 * @param hostName name of the host, or <code>null</code>
	 */
	public RegistryContributor(String actualId, String actualName, String hostId, String hostName) {
		this.actualContributorId = actualId;
		this.actualContributorName = actualName;
		if (hostId != null) {
			this.hostId = hostId;
			this.hostName = hostName;
		} else {
			this.hostId = actualId;
			this.hostName = actualName;
		}
	}

	/**
	 * Provides actual ID associated with the registry contributor (e.g., "12"). IDs are expected 
	 * to be unique in the workspace.
	 * 
	 * @return actual ID of the registry contributor 
	 */
	public String getActualId() {
		return actualContributorId;
	}

	/**
	 * Provides actual name of the registry contributor (e.g., "org.eclipe.core.runtime.fragment").
	 *  
	 * @return actual name of the registry contributor 
	 */
	public String getActualName() {
		return actualContributorName;
	}

	/**
	 * Provides ID associated with the entity "in charge" of the contributor (e.g., "1"). IDs are expected 
	 * to be unique in the workspace. If contributor does not rely on a host, this value should be 
	 * the same as the actual contributor ID.
	 * 
	 * @return id of the registry contributor 
	 */
	public String getId() {
		return hostId;
	}

	/**
	 * Provides name of the entity "in charge" of the contributor (e.g., "org.eclipse.core.runtime").
	 * If contributor does not rely on a host, this value should be the same as the actual contributor name.
	 * 
	 * @return name of the registry contributor 
	 */
	public String getName() {
		return hostName;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return actualContributorName + "[" + actualContributorId + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
