/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ip;

import java.util.Dictionary;

/**
 * Provides persistent storage for the Initial Provisioning Dictionary. This
 * service is used by the Provisioning Agent to store the provisioning data.
 * 
 * @author Avgustin Marinov, e-mail: a_marinov@prosyst.bg
 * @author Pavlin Dobrev
 * @version 1.0
 */

public interface ProvisioningStorage {

	/**
	 * This method is invoked by Provisioning Agent on startup. The storage
	 * returns the stored data. After that the provisioning service is
	 * responsible to put the data into the provisioning service dictionary.
	 * 
	 * @return the stored info
	 * @throws Exception
	 *             an exception if I/O error occurs
	 */
	public Dictionary getStoredInfo() throws Exception;

	/**
	 * This method is invoked by provisioning service on provisioning data
	 * update. The storage must store the data.
	 * 
	 * @param provisioningData
	 *            Provisioning Data dictionary.
	 * @throws Exception
	 *             if the data cannot be stored
	 */
	public void store(Dictionary provisioningData) throws Exception;
}
