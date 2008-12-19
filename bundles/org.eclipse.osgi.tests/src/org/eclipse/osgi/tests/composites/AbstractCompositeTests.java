/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.composites;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.eclipse.osgi.tests.bundles.BundleInstaller;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;
import org.osgi.service.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class AbstractCompositeTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(AbstractCompositeTests.class);
	}

	ServiceRegistration installerReg;
	ServiceReference linkBundleFactoryRef;
	CompositeBundleFactory linkBundleFactory;

	protected void setUp() throws Exception {
		super.setUp();
		linkBundleFactoryRef = OSGiTestsActivator.getContext().getServiceReference(CompositeBundleFactory.class.getName());
		assertNotNull("LinkBundleFactory reference is null", linkBundleFactoryRef); //$NON-NLS-1$
		linkBundleFactory = (CompositeBundleFactory) OSGiTestsActivator.getContext().getService(linkBundleFactoryRef);
		assertNotNull("LinkBundleFactory service is null", linkBundleFactory); //$NON-NLS-1$

		installerReg = OSGiTestsActivator.getContext().registerService(BundleInstaller.class.getName(), installer, null);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if (linkBundleFactoryRef != null) {
			OSGiTestsActivator.getContext().ungetService(linkBundleFactoryRef);
			linkBundleFactoryRef = null;
			linkBundleFactory = null;
		}
		if (installerReg != null)
			installerReg.unregister();
	}

	CompositeBundle createCompositeBundle(CompositeBundleFactory factory, String location, Map configuration, Map linkManifest, boolean start, boolean security) {
		if (configuration == null)
			configuration = new HashMap();
		if (security)
			configuration.put(Constants.FRAMEWORK_SECURITY, "osgi"); //$NON-NLS-1$
		if (linkManifest == null) {
			linkManifest = new HashMap();
			linkManifest.put(Constants.BUNDLE_SYMBOLICNAME, location);
		}
		CompositeBundle composite = null;
		try {
			composite = factory.installCompositeBundle(configuration, location, linkManifest);
		} catch (BundleException e) {
			fail("Unexpected exception creating composite bundle", e); //$NON-NLS-1$
		}
		assertNotNull("Composite is null", composite); //$NON-NLS-1$
		assertEquals("Wrong composite location", location, composite.getLocation()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, composite.getCompositeFramework().getState()); //$NON-NLS-1$
		SurrogateBundle surrogate = composite.getSurrogateBundle();
		assertNotNull("Surrogate is null", surrogate); //$NON-NLS-1$
		assertEquals("Wrong surrogte location", location, surrogate.getLocation()); //$NON-NLS-1$
		if (start)
			startCompositeBundle(composite, false);
		return composite;
	}

	InputStream getBundleInput() throws BundleException {
		URL bundlejar = OSGiTestsActivator.getContext().getBundle().getEntry("test_files/security/bundles/signed.jar"); //$NON-NLS-1$
		if (bundlejar == null)
			throw new BundleException("Cannot find test bundle jar"); //$NON-NLS-1$
		try {
			return bundlejar.openStream();
		} catch (IOException e) {
			throw new BundleException("Cannot open test bundle stream", e); //$NON-NLS-1$
		}
	}

	void waitForStateChange(Bundle bundle, int changeFrom) {
		for (int i = 0; i < 10 && (bundle.getState() & changeFrom) != 0; i++)
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// nothing
			}
		assertTrue("Wrong bundle state: " + bundle.getState(), (bundle.getState() & changeFrom) == 0); //$NON-NLS-1$

	}

	Bundle installIntoCurrent(String name) {
		try {
			Bundle testBundle = installer.installBundle(name);
			installer.resolveBundles(new Bundle[] {testBundle});
			return testBundle;
		} catch (BundleException e) {
			fail("failed to install test bundle", e); //$NON-NLS-1$
		}
		return null;
	}

	Bundle installIntoChild(Framework framework, String name) {
		try {
			return framework.getBundleContext().installBundle(installer.getBundleLocation(name));
		} catch (BundleException e) {
			fail("failed to install test bundle", e); //$NON-NLS-1$
		}
		return null;
	}

	void startCompositeBundle(CompositeBundle composite, boolean expectFailure) {
		boolean childFrameworkActive = composite.getCompositeFramework().getState() == Bundle.ACTIVE;
		try {
			composite.start();
			if (expectFailure)
				fail("Expected an exception starting composite bundle"); //$NON-NLS-1$
		} catch (BundleException e) {
			if (!expectFailure)
				fail("Failed to start composite", e); //$NON-NLS-1$
		}
		Framework childFramework = composite.getCompositeFramework();
		int expectedState = expectFailure && !childFrameworkActive ? Bundle.STARTING : Bundle.ACTIVE;
		assertEquals("Wrong state for SystemBundle", expectedState, childFramework.getState()); //$NON-NLS-1$
	}

	void stopCompositeBundle(CompositeBundle composite) {
		Framework childFramework = composite.getCompositeFramework();
		try {
			composite.stop();
		} catch (BundleException e) {
			fail("Unexpected error stopping composite", e); //$NON-NLS-1$
		}
		FrameworkEvent stopEvent = null;
		try {
			stopEvent = childFramework.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertNotNull("Stop event is null", stopEvent); //$NON-NLS-1$
		assertEquals("Wrong stopEvent", FrameworkEvent.STOPPED, stopEvent.getType()); //$NON-NLS-1$

		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, childFramework.getState()); //$NON-NLS-1$
	}

	void uninstallCompositeBundle(CompositeBundle composite) {
		Framework childFramework = composite.getCompositeFramework();
		try {
			composite.uninstall();
		} catch (BundleException e) {
			fail("Unexpected error uninstalling composite", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, childFramework.getState()); //$NON-NLS-1$
	}

	PackageAdmin getPackageAdmin(Bundle framework) {
		BundleContext context = framework.getBundleContext();
		return (PackageAdmin) context.getService(context.getServiceReference(PackageAdmin.class.getName()));
	}

	CompositeBundleFactory getFactory(Bundle framework) {
		BundleContext context = framework.getBundleContext();
		assertNotNull("Child context is null.", context); //$NON-NLS-1$
		ServiceReference ref = context.getServiceReference(CompositeBundleFactory.class.getName());
		assertNotNull("Factory reference is null", ref); //$NON-NLS-1$
		CompositeBundleFactory factory = (CompositeBundleFactory) context.getService(ref);
		assertNotNull("factory service is null", factory); //$NON-NLS-1$
		// just release now to make testcode simple
		context.ungetService(ref);
		return factory;
	}

	class TestServiceListener implements ServiceListener {
		final int[] results = new int[4];

		public int[] getResults() {
			return results;
		}

		public void serviceChanged(ServiceEvent event) {
			switch (event.getType()) {
				case ServiceEvent.REGISTERED :
					++results[0];
					break;
				case ServiceEvent.MODIFIED :
					++results[1];
					break;
				case ServiceEvent.MODIFIED_ENDMATCH :
					++results[2];
					break;
				case ServiceEvent.UNREGISTERING :
					++results[3];
					break;
			}
		}
	}

	public class TestTrackerCustomizer implements ServiceTrackerCustomizer {
		final int[] results = new int[3];

		public int[] getResults() {
			return results;
		}

		public Object addingService(ServiceReference reference) {
			++results[0];
			return reference;
		}

		public void modifiedService(ServiceReference reference, Object service) {
			++results[1];
		}

		public void removedService(ServiceReference reference, Object service) {
			++results[2];
		}
	}
}
