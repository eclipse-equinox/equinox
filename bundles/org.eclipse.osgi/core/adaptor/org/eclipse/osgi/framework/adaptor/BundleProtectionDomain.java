/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.adaptor;

import java.security.PermissionCollection;
import java.security.ProtectionDomain;

/**
 *
 * This is a specialized ProtectionDomain that also has information about the
 * signers and the hash of the bundle.
 * 
 * @version $Revision$
 */
public class BundleProtectionDomain extends ProtectionDomain {

	/**
	 * Constructs a special ProtectionDomain that allows access to signature and
	 * digest information.
	 * 
	 * @param permCollection the PermissionCollection for the Bundle
	 */
	public BundleProtectionDomain(PermissionCollection permCollection) {
		super(null, permCollection);
	}

}
