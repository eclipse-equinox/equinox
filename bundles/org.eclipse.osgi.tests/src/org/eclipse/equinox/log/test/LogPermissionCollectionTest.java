/*******************************************************************************
 * Copyright (c) 2011 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log.test;

import junit.framework.TestCase;
import org.eclipse.equinox.log.LogPermission;
import org.eclipse.equinox.log.LogPermissionCollection;

public class LogPermissionCollectionTest extends TestCase {

	public void testImplies() {

		LogPermission permission = new LogPermission("*", "*"); //$NON-NLS-1$//$NON-NLS-2$
		LogPermissionCollection permissionCollection = new LogPermissionCollection();
		assertFalse(permissionCollection.implies(permission));
		permissionCollection.add(permission);
		assertTrue(permissionCollection.implies(permission));
	}

}
