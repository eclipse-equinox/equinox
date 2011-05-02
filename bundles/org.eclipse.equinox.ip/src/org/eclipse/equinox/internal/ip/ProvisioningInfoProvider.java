/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ip;

import java.util.Dictionary;

import org.osgi.service.provisioning.ProvisioningService;

/**
 * Provides a set (can be empty) of configuration properties used by the Initial
 * Provisioning Bundle. The properties obtained by the providers become part of
 * the Provisioning Dictionary. Arbitrary number of Configuration Loaders could
 * be registered as services in the OSGi framework. Different providers could
 * export different set of configuration properties. If a property is provided
 * by more than one provider, the Provisioning Bundle uses this one, which is
 * exported by a provider with the higher service ranking.
 * 
 * @author Avgustin Marinov
 * @author Pavlin Dobrev
 * @version 1.0
 */

public interface ProvisioningInfoProvider {

	/**
	 * This property specifies URL for connecting to the Management Server.
	 */
	public static final String MANAGER_URL = "equinox.provisioning.manager.url";

	/**
	 * This property specifies the host of the Gateway Manager that Management
	 * Server management agent bundle must connect to. If this property is not
	 * present in the dictionary, then Management Server bundle will listen for
	 * a back end device to push this information to.
	 */
	public final static String GM_HOST = "equinox.provisioning.gm.host";

	/**
	 * This manifest header determines the packed into the provisioning agent
	 * bundle providers that are to be started.
	 */
	public final static String PROVIDERS = "PrvInfo-Providers";

	/**
	 * Initializes provider. Give to the provider a reference to
	 * ProvisioningService dictionary. Provider can return properties that are
	 * to be set properties in the provisioning dictionary. It is not necessary
	 * for provider to insert explicitly a property unless it has to be listed
	 * through the <code>Dictionary.keys()</code> method or it evokes action
	 * (as ProvisioningService.PROVISIONIN_REFERENCE).
	 * 
	 * @param prvSrv
	 *            provisioning service reference
	 * @return the initial data. It can be null.
	 * @throws Exception
	 *             on initialization error
	 */
	public Dictionary init(ProvisioningService prvSrv) throws Exception;

	/**
	 * Gets a property with a supplied key from the provider. This method is
	 * invoked by the Provisioning Agent when
	 * <code>Dictionary.get(propertyKey)</code> is invoked for a propertyKey
	 * which is not already stored in the ProvisioningDictionary. The method is
	 * invoked subsequently on all registered providers ordered by service
	 * ranking until some of them returns value different from <code>null</code>.
	 * 
	 * @param propertyKey
	 *            the key.
	 * @return the property value or <code>null</code> if there is no such
	 *         property.
	 */
	public Object get(Object propertyKey);
}
