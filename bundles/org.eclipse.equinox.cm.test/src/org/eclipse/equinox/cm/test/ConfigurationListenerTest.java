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
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationListener;

public class ConfigurationListenerTest extends AbstractCMTest {

	boolean locked = false;
	Object lock = new Object();

	@Test
	public void testListener() throws Exception {

		Configuration config = cm.getConfiguration("test");
		Dictionary<String, Object> props = dictionaryOf("testkey", "testvalue");
		config.update(props);

		ConfigurationListener listener = event -> {
			synchronized (lock) {
				locked = false;
				lock.notify();
			}
		};
		ServiceRegistration<ConfigurationListener> reg = registerService(ConfigurationListener.class, listener, null);

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
