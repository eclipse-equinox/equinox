package activator.error3;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		// nothing
	}

	public void stop(BundleContext context) throws Exception {
		throw new RuntimeException();
	}

}
