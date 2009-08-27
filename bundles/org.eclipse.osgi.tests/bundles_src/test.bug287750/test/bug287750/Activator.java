package test.bug287750;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.startlevel.StartLevel;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		StartLevel sl = (StartLevel) context.getService(context.getServiceReference(StartLevel.class.getName()));
		sl.setStartLevel(10);
	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
