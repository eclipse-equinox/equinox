/*******************************************************************************
 * Copyright (c) 2022, 2022 Hannes Wellmann and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.cm.test;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import org.eclipse.core.runtime.Platform;
import org.junit.After;
import org.junit.Before;
import org.osgi.framework.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public abstract class AbstractCMTest {

	protected ConfigurationAdmin cm;
	private ServiceReference<ConfigurationAdmin> reference;

	private Configuration configuration = null;

	@Before
	public void setUp() throws Exception {
		getBundle("org.eclipse.equinox.cm").start();
		reference = getBundleContext().getServiceReference(ConfigurationAdmin.class);
		cm = getBundleContext().getService(reference);
	}

	Configuration saveAndUpdate(Configuration config) throws IOException {
		if (configuration != null) {
			fail("Already a configuration saved for this test case");
		}
		configuration = config;
		config.update();
		return config;
	}

	@After
	public void tearDown() throws Exception {
		if (configuration != null) {
			configuration.delete();
		}
		getBundleContext().ungetService(reference);
		getBundle("org.eclipse.equinox.cm").stop();
	}

	static BundleContext getBundleContext() {
		return FrameworkUtil.getBundle(AbstractCMTest.class).getBundleContext();
	}

	static Bundle getBundle(String symbolicName) {
		return Platform.getBundle(symbolicName);
	}

	static <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
		return getBundleContext().registerService(clazz, service, properties);
	}

	static <K, V> Dictionary<K, V> emptyDictionary() {
		return FrameworkUtil.asDictionary(Collections.emptyMap());
	}

	static <K, V> Dictionary<K, V> dictionaryOf(K key, V value) {
		return FrameworkUtil.asDictionary(Collections.singletonMap(key, value));
	}
}
