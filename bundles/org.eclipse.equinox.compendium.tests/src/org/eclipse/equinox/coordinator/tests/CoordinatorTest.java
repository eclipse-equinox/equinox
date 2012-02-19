package org.eclipse.equinox.coordinator.tests;

import junit.framework.TestCase;
import org.eclipse.equinox.compendium.tests.Activator;
import org.osgi.framework.ServiceReference;
import org.osgi.service.coordinator.Coordinator;

public abstract class CoordinatorTest extends TestCase {
	protected Coordinator coordinator;
	protected ServiceReference coordinatorRef;

	protected void setUp() throws Exception {
		Activator.getBundle(Activator.BUNDLE_COORDINATOR).start();
		coordinatorRef = Activator.getBundleContext().getServiceReference(Coordinator.class.getName());
		coordinator = (Coordinator) Activator.getBundleContext().getService(coordinatorRef);
	}

	protected void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(coordinatorRef);
		Activator.getBundle(Activator.BUNDLE_COORDINATOR).stop();
	}
}
