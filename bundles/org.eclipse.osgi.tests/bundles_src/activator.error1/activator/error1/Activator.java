package activator.error1;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public Activator() {
		throw new RuntimeException();
	}

	public void start(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
