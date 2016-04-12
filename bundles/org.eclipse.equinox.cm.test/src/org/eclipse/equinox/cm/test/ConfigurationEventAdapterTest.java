/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.*;

public class ConfigurationEventAdapterTest {

	private ConfigurationAdmin cm;
	private ServiceReference<ConfigurationAdmin> cmReference;

	boolean locked = false;
	Object lock = new Object();

	@Before
	public void setUp() throws Exception {
		Activator.getBundle("org.eclipse.equinox.event").start();
		Activator.getBundle("org.eclipse.equinox.cm").start();
		cmReference = Activator.getBundleContext().getServiceReference(ConfigurationAdmin.class);
		cm = Activator.getBundleContext().getService(cmReference);
	}

	@After
	public void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(cmReference);
		Activator.getBundle("org.eclipse.equinox.cm").stop();
		Activator.getBundle("org.eclipse.equinox.event").stop();
	}

	@Test
	public void testConfigurationEvent() throws Exception {

		Configuration config = cm.getConfiguration("test");
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put("testkey", "testvalue");
		config.update(props);

		EventHandler handler = new EventHandler() {
			public void handleEvent(Event event) {
				synchronized (lock) {
					locked = false;
					lock.notify();
				}
			}

		};
		String[] topics = new String[] {"org/osgi/service/cm/ConfigurationEvent/*"};
		Dictionary<String, Object> handlerProps = new Hashtable<String, Object>();

		handlerProps.put(EventConstants.EVENT_TOPIC, topics);
		ServiceRegistration<EventHandler> reg = Activator.getBundleContext().registerService(EventHandler.class, handler, handlerProps);

		synchronized (lock) {
			config.update(props);
			locked = true;
			lock.wait(5000);
			assertFalse(locked);
		}

		synchronized (lock) {
			config.delete();
			locked = true;
			lock.wait(5000);
			assertFalse(locked);
		}

		reg.unregister();
	}

	@Test
	public void testConfigurationFactoryEvent() throws Exception {

		Configuration config = cm.createFactoryConfiguration("test");
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put("testkey", "testvalue");
		config.update(props);

		EventHandler handler = new EventHandler() {
			public void handleEvent(Event event) {
				synchronized (lock) {
					locked = false;
					lock.notify();
				}
			}

		};
		String[] topics = new String[] {"org/osgi/service/cm/ConfigurationEvent/*"};
		Dictionary<String, Object> handlerProps = new Hashtable<String, Object>();
		handlerProps.put(EventConstants.EVENT_TOPIC, topics);
		ServiceRegistration<EventHandler> reg = Activator.getBundleContext().registerService(EventHandler.class, handler, handlerProps);

		synchronized (lock) {
			config.update(props);
			locked = true;
			lock.wait(5000);
			assertFalse(locked);
		}

		synchronized (lock) {
			config.delete();
			locked = true;
			lock.wait(5000);
			assertFalse(locked);
		}

		reg.unregister();
	}

}
