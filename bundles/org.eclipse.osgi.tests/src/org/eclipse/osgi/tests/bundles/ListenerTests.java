/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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
package org.eclipse.osgi.tests.bundles;


import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.osgi.launch.Equinox;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.startlevel.FrameworkStartLevel;

public class ListenerTests extends AbstractBundleTests {

	private String methodName;
	private List<Bundle> bundles;
	private Equinox equinox;
	private BundleContext bundleContext;

	public void setUp() throws Exception {
		methodName = getName();
		simpleResults = new TestResults();
		bundles = new ArrayList<>();

		Map<String, Object> configuration = createConfiguration();
		equinox = new Equinox(configuration);
		initAndStart(equinox);

		bundleContext = equinox.getBundleContext();

		for (int i = 0; i < 50; i++) {
			Bundle b = installBundle(methodName + i);
			bundles.add(b);
			b.start();
		}
	}

	public void tearDown() throws Exception {
		simpleResults = null;
		for (Bundle b : bundles) {
			if (b != null) {
				b.stop();
				b = null;
			}
		}
		stopQuietly(equinox);
	}

	public void testBundleListenersOrder() throws Exception {

		BundleListener[] expectedBundleListeners = new BundleListener[200];

		int i = 100;
		for (Bundle b : bundles) {
			BundleListener bundleListener = createBundleListener();
			b.getBundleContext().addBundleListener(bundleListener);
			expectedBundleListeners[i] = bundleListener;
			i = i + 2;
		}

		int j = 101;
		for (Bundle b : bundles) {
			BundleListener bundleListener = createBundleListener();
			b.getBundleContext().addBundleListener(bundleListener);
			expectedBundleListeners[j] = bundleListener;
			j = j + 2;
		}

		//synchronous listener will be called first
		i = 0;
		for (Bundle b : bundles) {
			BundleListener bundleListener = createSynchronousBundleListener();
			b.getBundleContext().addBundleListener(bundleListener);
			expectedBundleListeners[i] = bundleListener;
			i = i + 2;
		}

		j = 1;
		for (Bundle b : bundles) {
			BundleListener bundleListener = createSynchronousBundleListener();
			b.getBundleContext().addBundleListener(bundleListener);
			expectedBundleListeners[j] = bundleListener;
			j = j + 2;
		}

		installBundle(methodName + "51");

		Object[] actualBundleListeners = simpleResults.getResults(200);

		assertArrayEquals(expectedBundleListeners, actualBundleListeners);

	}

	public void testFrameworkListenersOrder() throws Exception {
		FrameworkListener[] expectedFrameworkListeners = new FrameworkListener[100];

		int i = 0;
		for (Bundle b : bundles) {
			FrameworkListener frameworkListener = createFrameworkListener();
			b.getBundleContext().addFrameworkListener(frameworkListener);
			expectedFrameworkListeners[i] = frameworkListener;
			i = i + 2;
		}

		int j = 1;
		for (Bundle b : bundles) {
			FrameworkListener frameworkListener = createFrameworkListener();
			b.getBundleContext().addFrameworkListener(frameworkListener);
			expectedFrameworkListeners[j] = frameworkListener;
			j = j + 2;
		}

		equinox.adapt(FrameworkStartLevel.class).setStartLevel(5);

		Object[] actualFrameworkListeners = simpleResults.getResults(100);

		assertArrayEquals(expectedFrameworkListeners, actualFrameworkListeners);
	}

	public void testServiceListenersOrder() throws Exception {
		ServiceListener[] expectedServiceListeners = new ServiceListener[100];

		int i = 0;
		for (Bundle b : bundles) {
			ServiceListener serviceListener = createServiceListener();
			b.getBundleContext().addServiceListener(serviceListener);
			expectedServiceListeners[i] = serviceListener;
			i = i + 2;
		}

		int j = 1;
		for (Bundle b : bundles) {
			ServiceListener serviceListener = createServiceListener();
			b.getBundleContext().addServiceListener(serviceListener);
			expectedServiceListeners[j] = serviceListener;
			j = j + 2;
		}

		Bundle bundle = installBundle(methodName + "51");
		bundle.start();
		ServiceRegistration<Object> reg = bundle.getBundleContext().registerService(Object.class, new Object(), null);

		Object[] actualServiceListeners = simpleResults.getResults(100);

		assertArrayEquals(expectedServiceListeners, actualServiceListeners);

		if (reg != null) {
			reg.unregister();
		}

	}

	private BundleListener createBundleListener() {

		BundleListener bundleListener = new BundleListener() {
			public void bundleChanged(BundleEvent event) {
				simpleResults.addEvent(this);
			}
		};
		return bundleListener;
	}

	private BundleListener createSynchronousBundleListener() {

		SynchronousBundleListener bundleListener = new SynchronousBundleListener() {
			public void bundleChanged(BundleEvent event) {
				simpleResults.addEvent(this);
			}
		};
		return bundleListener;
	}

	private FrameworkListener createFrameworkListener() {
		FrameworkListener frameworkListener = new FrameworkListener() {
			@Override
			public void frameworkEvent(FrameworkEvent event) {
				simpleResults.addEvent(this);
			}
		};
		return frameworkListener;
	}

	private ServiceListener createServiceListener() {
		ServiceListener serviceListener = new ServiceListener() {

			@Override
			public void serviceChanged(ServiceEvent event) {
				simpleResults.addEvent(this);
			}
		};
		return serviceListener;
	}

	private Bundle installBundle(String name) throws BundleException, IOException {
		Bundle bundle = bundleContext.installBundle(name, new BundleBuilder().symbolicName(name).build());
		assertNotNull(name + " bundle does not exist", bundleContext.getBundle(name));
		return bundle;
	}

}