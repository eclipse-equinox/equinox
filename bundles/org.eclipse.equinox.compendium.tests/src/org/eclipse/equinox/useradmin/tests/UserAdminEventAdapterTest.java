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

import java.util.Dictionary;
import java.util.Hashtable;
import junit.framework.TestCase;
import org.eclipse.equinox.compendium.tests.Activator;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.*;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

public class UserAdminEventAdapterTest extends TestCase {

	private UserAdmin userAdmin;
	private ServiceReference userAdminReference;

	boolean locked = false;
	Object lock = new Object();

	protected void setUp() throws Exception {
		Activator.getBundle(Activator.BUNDLE_EVENT).start();
		Activator.getBundle(Activator.BUNDLE_USERADMIN).start();
		userAdminReference = Activator.getBundleContext().getServiceReference(UserAdmin.class.getName());
		userAdmin = (UserAdmin) Activator.getBundleContext().getService(userAdminReference);
	}

	protected void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(userAdminReference);
		Activator.getBundle(Activator.BUNDLE_USERADMIN).stop();
		Activator.getBundle(Activator.BUNDLE_EVENT).stop();
	}

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
		Dictionary handlerProps = new Hashtable();

		handlerProps.put(EventConstants.EVENT_TOPIC, topics);
		ServiceRegistration reg = Activator.getBundleContext().registerService(EventHandler.class.getName(), handler, handlerProps);

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
