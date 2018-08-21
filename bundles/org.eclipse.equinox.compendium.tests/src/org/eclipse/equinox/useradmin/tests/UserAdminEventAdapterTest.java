/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.useradmin.tests;

import static org.junit.Assert.assertFalse;

import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.equinox.compendium.tests.Activator;
import org.junit.*;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.*;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

public class UserAdminEventAdapterTest {

	private UserAdmin userAdmin;
	private ServiceReference<UserAdmin> userAdminReference;

	boolean locked = false;
	Object lock = new Object();

	@Before
	public void setUp() throws Exception {
		Activator.getBundle(Activator.BUNDLE_EVENT).start();
		Activator.getBundle(Activator.BUNDLE_USERADMIN).start();
		userAdminReference = Activator.getBundleContext().getServiceReference(UserAdmin.class);
		userAdmin = Activator.getBundleContext().getService(userAdminReference);
	}

	@After
	public void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(userAdminReference);
		Activator.getBundle(Activator.BUNDLE_USERADMIN).stop();
		Activator.getBundle(Activator.BUNDLE_EVENT).stop();
	}

	@Test
	public void testUserAdminEvent() throws Exception {

		EventHandler handler = new EventHandler() {
			public void handleEvent(Event event) {
				synchronized (lock) {
					locked = false;
					lock.notify();
				}
			}

		};
		String[] topics = new String[] {"org/osgi/service/useradmin/UserAdmin/*"}; //$NON-NLS-1$
		Dictionary<String, Object> handlerProps = new Hashtable<String, Object>();

		handlerProps.put(EventConstants.EVENT_TOPIC, topics);
		ServiceRegistration<EventHandler> reg = Activator.getBundleContext().registerService(EventHandler.class, handler, handlerProps);

		synchronized (lock) {
			userAdmin.createRole("testRole", Role.USER); //$NON-NLS-1$
			locked = true;
			lock.wait(5000);
			assertFalse(locked);
		}

		synchronized (lock) {
			userAdmin.removeRole("testRole"); //$NON-NLS-1$
			locked = true;
			lock.wait(5000);
			assertFalse(locked);
		}

		reg.unregister();
	}

}
