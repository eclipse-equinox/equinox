/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.condition.Condition;

public class IllegalStateExceptionTests extends AbstractBundleTests {

	@Test
	public void testUninstall() throws Exception {
		Bundle bundle = installer.installBundle("test");
		bundle.uninstall();
		// uninstall again should not throw an exception
		bundle.uninstall();
	}

	@Test
	public void testCreateFilter() throws Exception {
		Filter testFilter = getInvalidContext().createFilter("(test=illegalstateexception)");
		assertNotNull("Null filter returned", testFilter);
	}

	@Test
	public void testGetBundle() throws Exception {
		BundleContext invalidContext = getInvalidContext();

		assertNotNull("Null bundle returned", invalidContext.getBundle());
		assertNotNull("Null system bundle returned", invalidContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION));
		assertNotNull("Null system bundle returned", invalidContext.getBundle(Constants.SYSTEM_BUNDLE_ID));
	}

	@Test
	public void testGetProperty() throws Exception {
		BundleContext invalidContext = getInvalidContext();

		assertNotNull("Null UUID Property", invalidContext.getProperty(Constants.FRAMEWORK_UUID));
	}

	@Test
	public void testRemoveServiceListener() throws Exception {
		BundleContext invalidContext = getInvalidContext();

		invalidContext.removeServiceListener(new ServiceListener() {
			@Override
			public void serviceChanged(ServiceEvent event) {
				// nothing
			}
		});
	}

	@Test
	public void testRemoveBundleListener() throws Exception {
		BundleContext invalidContext = getInvalidContext();

		invalidContext.removeBundleListener(new BundleListener() {
			@Override
			public void bundleChanged(BundleEvent event) {
				// nothing
			}
		});
	}

	@Test
	public void testFrameworkListener() throws Exception {
		BundleContext invalidContext = getInvalidContext();

		invalidContext.removeFrameworkListener(new FrameworkListener() {
			@Override
			public void frameworkEvent(FrameworkEvent event) {
				// nothing
			}
		});
	}

	@Test
	public void testContextUngetService() throws Exception {
		BundleContext invalidContext = getInvalidContext();

		invalidContext.ungetService(getContext().getServiceReference("org.osgi.service.condition.Condition"));
	}

	@Test
	public void testServiceObjectsUngetService() throws Exception {
		Bundle bundle = installer.installBundle("test");
		bundle.start();
		BundleContext context = bundle.getBundleContext();
		ServiceReference<Condition> ref = context.getServiceReference(Condition.class);
		ServiceObjects<Condition> serviceObjects = context.getServiceObjects(ref);
		Condition c = serviceObjects.getService();
		bundle.stop();
		serviceObjects.ungetService(c);
	}

	@Test
	public void testUnregister() throws Exception {
		Bundle bundle = installer.installBundle("test");
		bundle.start();
		BundleContext context = bundle.getBundleContext();
		ServiceRegistration<Condition> reg = context.registerService(Condition.class, Condition.INSTANCE, null);
		reg.unregister();
		reg.unregister();
	}

	public BundleContext getInvalidContext() throws Exception {
		Bundle bundle = installer.installBundle("test");
		bundle.start();
		BundleContext context = bundle.getBundleContext();
		bundle.stop();
		return context;
	}
}
