/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.useradmin.tests;

import junit.framework.TestCase;
import org.eclipse.equinox.compendium.tests.Activator;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.*;

public class UserTest extends TestCase {

	private UserAdmin userAdmin;
	private ServiceReference userAdminReference;

	boolean locked = false;
	Object lock = new Object();

	protected void setUp() throws Exception {
		Activator.getBundle(Activator.BUNDLE_USERADMIN).start();
		userAdminReference = Activator.getBundleContext().getServiceReference(UserAdmin.class.getName());
		userAdmin = (UserAdmin) Activator.getBundleContext().getService(userAdminReference);
	}

	protected void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(userAdminReference);
		Activator.getBundle(Activator.BUNDLE_USERADMIN).stop();
	}

	public void testUserCreateAndRemove() throws Exception {
		User user = (User) userAdmin.createRole("testUserCreate", Role.USER); //$NON-NLS-1$
		assertNotNull(user);
		assertEquals("testUserCreate", user.getName()); //$NON-NLS-1$
		assertTrue(user.getType() == Role.USER);
		assertTrue(userAdmin.removeRole("testUserCreate")); //$NON-NLS-1$
		assertNull(userAdmin.getRole("testUserCreate")); //$NON-NLS-1$
	}

}
