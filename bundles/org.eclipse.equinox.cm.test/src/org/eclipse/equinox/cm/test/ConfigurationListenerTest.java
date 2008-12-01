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

import java.util.Properties;
import junit.framework.TestCase;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.*;

public class ConfigurationListenerTest extends TestCase {

	private ConfigurationAdmin cm;
	private ServiceReference reference;
	boolean locked = false;
	Object lock = new Object();

	public ConfigurationListenerTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		Activator.getBundle("org.eclipse.equinox.cm").start();
		reference = Activator.getBundleContext().getServiceReference(ConfigurationAdmin.class.getName());
		cm = (ConfigurationAdmin) Activator.getBundleContext().getService(reference);
	}

	protected void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(reference);
		Activator.getBundle("org.eclipse.equinox.cm").stop();
	}

	public void testListener() throws Exception {

		Configuration config = cm.getConfiguration("test");
		Properties props = new Properties();
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
		ServiceRegistration reg = Activator.getBundleContext().registerService(ConfigurationListener.class.getName(), listener, null);

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
