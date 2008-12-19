package test.link.d;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.eclipse.osgi.tests.bundles.BundleInstaller;
import org.osgi.framework.*;
import org.osgi.service.framework.CompositeBundle;
import org.osgi.service.framework.CompositeBundleFactory;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		ServiceReference installerRef = context.getServiceReference(BundleInstaller.class.getName());
		if (installerRef == null)
			throw new Exception("Missing Service Permission"); //$NON-NLS-1$
		BundleInstaller installer = (BundleInstaller) context.getService(installerRef);
		if (installer == null)
			throw new Exception("Missing Service Permission"); //$NON-NLS-1$
		context.ungetService(installerRef);

		ServiceReference factoryRef = context.getServiceReference(CompositeBundleFactory.class.getName());
		if (factoryRef == null)
			throw new Exception("Missing Service Permission"); //$NON-NLS-1$
		CompositeBundleFactory factory = (CompositeBundleFactory) context.getService(factoryRef);
		if (factory == null)
			throw new Exception("Missing Service Permission"); //$NON-NLS-1$

		Map linkManifest = new HashMap();
		linkManifest.put(Constants.BUNDLE_SYMBOLICNAME, "childComposite"); //$NON-NLS-1$
		linkManifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		linkManifest.put(Constants.IMPORT_PACKAGE, "org.eclipse.osgi.tests.bundles"); //$NON-NLS-1$
		linkManifest.put(CompositeBundleFactory.COMPOSITE_SERVICE_FILTER_IMPORT, "(objectClass=org.eclipse.osgi.tests.bundles.BundleInstaller)"); //$NON-NLS-1$
		CompositeBundle childComposite = null;
		try {
			childComposite = factory.installCompositeBundle(null, "childComposite", linkManifest); //$NON-NLS-1$
		} catch (SecurityException e) {
			throw new Exception("Missing AllPermissions"); //$NON-NLS-1$
		}

		Bundle test = childComposite.getCompositeFramework().getBundleContext().installBundle(installer.getBundleLocation("test")); //$NON-NLS-1$
		if (test == null)
			throw new Exception("Install of test bundle is null"); //$NON-NLS-1$

		childComposite.start();

		ServiceTracker trackInstaller = new ServiceTracker(childComposite.getBundleContext(), BundleInstaller.class.getName(), null);
		trackInstaller.open();
		BundleInstaller childInstaller = (BundleInstaller) trackInstaller.waitForService(5000);
		if (childInstaller != installer)
			throw new Exception("Unexpected childInstaller: " + childInstaller);
		test.start();
		Object[] expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, test);
		AbstractBundleTests.compareResults(expectedEvents, AbstractBundleTests.simpleResults.getResults(1));
	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
