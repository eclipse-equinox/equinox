package test.protocol.handler.user;

import java.net.URL;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		URL testURL = new URL("testing1://test");
		testURL.openConnection();
		System.out.println(testURL);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// nothing
	}

}
