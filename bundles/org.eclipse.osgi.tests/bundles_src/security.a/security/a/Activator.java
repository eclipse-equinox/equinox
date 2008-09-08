package security.a;

import java.net.URL;
import java.util.Enumeration;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		Enumeration urls = context.getBundle().findEntries("resources", "frag.a.txt", false); //$NON-NLS-1$//$NON-NLS-2$
		if (urls == null || !urls.hasMoreElements())
			throw new Exception("Did not find any resources"); //$NON-NLS-1$
		while (urls.hasMoreElements()) {
			URL url = (URL) urls.nextElement();
			if (url == null)
				throw new Exception("The URL is null"); //$NON-NLS-1$
		}
	}

	public void stop(BundleContext context) throws Exception {
		//nothing
	}

}
