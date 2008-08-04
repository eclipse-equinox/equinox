
package org.eclipse.equinox.weaving.aspectj;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class WeavingServiceFactory implements ServiceFactory {

    public Object getService(final Bundle bundle,
            final ServiceRegistration registration) {
        return (new WeavingService());
    }

    public void ungetService(final Bundle bundle,
            final ServiceRegistration registration, final Object service) {
        //nothing here
    }

}
