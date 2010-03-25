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
