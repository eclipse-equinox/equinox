package activator.error2;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		throw new RuntimeException();
	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
