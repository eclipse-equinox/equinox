package org.eclipse.equinox.weaving.aspectj;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class WeavingServiceFactory implements ServiceFactory {

	public Object getService(Bundle bundle, ServiceRegistration registration) {
		return (new WeavingService());
	}

	public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
		//nothing here
	}

}
