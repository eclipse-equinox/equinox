/*******************************************************************************
 * Copyright (c) 1997-2009 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ds;

import org.eclipse.equinox.internal.ds.model.ServiceComponentProp;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.component.*;
import org.osgi.service.log.LogService;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

final class FactoryReg implements ServiceFactory {

	/* the instance created */
	private final ServiceComponentProp component;

	FactoryReg(ServiceComponentProp component) {
		this.component = component;
	}

	// ServiceFactory.getService method.
	public Object getService(Bundle bundle, ServiceRegistration registration) {

		try {
			if (Activator.DEBUG) {
				Activator.log.debug("FactoryReg.getService(): created new service for component " + component.name, null); //$NON-NLS-1$
			}
			ComponentInstance ci = InstanceProcess.staticRef.buildComponent(bundle, component, null, false);
			// ci can be null if the component is already disposed while being built
			if (ci != null) {
				return ci.getInstance();
			}
		} catch (Throwable t) {
			if (!(t instanceof ComponentException)) {
				Activator.log(component.bc, LogService.LOG_ERROR, NLS.bind(Messages.CANT_GET_SERVICE, component.name), t);
			} else {
				throw (ComponentException) t;
			}
		}
		return null;
	}

	// ServiceFactory.ungetService method.
	public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
		if (Activator.DEBUG) {
			Activator.log.debug("FactoryReg.ungetService(): registration = " + registration.toString(), null); //$NON-NLS-1$
		}
		component.disposeObj(service, ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED);
	}

	public String toString() {
		return component.name + " FactoryRegistration"; //$NON-NLS-1$
	}
}
