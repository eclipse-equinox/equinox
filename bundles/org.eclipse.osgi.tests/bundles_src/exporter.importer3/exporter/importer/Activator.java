package exporter.importer;
import exporter.importer.test.Test3;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		new Test3();
	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
