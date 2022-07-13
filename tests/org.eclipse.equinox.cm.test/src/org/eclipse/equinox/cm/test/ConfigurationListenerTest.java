/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others
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
package org.eclipse.equinox.cm.test;

import static org.junit.Assert.assertFalse;

import java.util.Dictionary;
import java.util.Hashtable;
import org.junit.*;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.*;

public class ConfigurationListenerTest {

	private ConfigurationAdmin cm;
	private ServiceReference<ConfigurationAdmin> reference;
	boolean locked = false;
	Object lock = new Object();

	@Before
	public void setUp() throws Exception {
		Activator.getBundle("org.eclipse.equinox.cm").start();
		reference = Activator.getBundleContext().getServiceReference(ConfigurationAdmin.class);
		cm = Activator.getBundleContext().getService(reference);
	}

	@After
	public void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(reference);
		Activator.getBundle("org.eclipse.equinox.cm").stop();
	}

	@Test
	public void testListener() throws Exception {

		Configuration config = cm.getConfiguration("test");
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("testkey", "testvalue");
		config.update(props);

		ConfigurationListener listener = new ConfigurationListener() {
			public void configurationEvent(ConfigurationEvent event) {
				synchronized (lock) {
					locked = false;
					lock.notify();
				}
			}
		};
		ServiceRegistration<ConfigurationListener> reg = Activator.getBundleContext().registerService(ConfigurationListener.class, listener, null);

		synchronized (lock) {
			config.update(props);
			locked = true;
			lock.wait(5000);
			assertFalse(locked);
		}

		reg.unregister();
		config.delete();
	}

}
