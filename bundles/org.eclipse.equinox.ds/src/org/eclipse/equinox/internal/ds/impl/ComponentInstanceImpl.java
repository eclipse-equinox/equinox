/*******************************************************************************
 * Copyright (c) 1997, 2010 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ds.impl;

import java.util.*;
import org.apache.felix.scr.Component;
import org.eclipse.equinox.internal.ds.Activator;
import org.eclipse.equinox.internal.ds.InstanceProcess;
import org.eclipse.equinox.internal.ds.model.ServiceComponentProp;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.*;

/**
 * ComponentInstanceImpl.java
 * 
 * @author Valentin Valchev
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 */

public class ComponentInstanceImpl implements ComponentInstance {

	private Object instance;
	private ServiceComponentProp scp;
	private ComponentContext componentContext;

	// ServiceReference to service objects which are binded to this instance
	public Hashtable bindedServices = new Hashtable(11);

	public ComponentInstanceImpl(Object instance, ServiceComponentProp scp) {
		this.instance = instance;
		this.scp = scp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.component.ComponentInstance#dispose()
	 */
	public void dispose() {
		if (scp == null) {
			// already disposed!
			return;
		}
		if (Activator.DEBUG) {
			Activator.log.debug("ComponentInstanceImpl.dispose(): disposing instance of component " + scp.name, null); //$NON-NLS-1$
		}
		if (!scp.isComponentFactory() && scp.serviceComponent.factory != null) {
			// this is a component factory instance, so dispose SCP
			scp.serviceComponent.componentProps.removeElement(scp);
			Vector toDispose = new Vector(1);
			toDispose.addElement(scp);
			InstanceProcess.resolver.disposeComponentConfigs(toDispose, ComponentConstants.DEACTIVATION_REASON_DISPOSED);
			if (scp != null) {
				scp.setState(Component.STATE_DISPOSED);
			}
		} else {
			scp.dispose(this, ComponentConstants.DEACTIVATION_REASON_DISPOSED);
		}

		// free service references if some are left ungotten
		freeServiceReferences();
		scp = null;
		componentContext = null;
		instance = null;
	}

	// check whether some cached service references are not yet removed and
	// ungotten
	public void freeServiceReferences() {
		if (!bindedServices.isEmpty()) {
			Enumeration keys = bindedServices.keys();
			while (keys.hasMoreElements()) {
				ServiceReference reference = (ServiceReference) keys.nextElement();
				bindedServices.remove(reference);
				scp.bc.ungetService(reference);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.component.ComponentInstance#getInstance()
	 */
	public Object getInstance() {
		return instance;
	}

	public ComponentContext getComponentContext() {
		return componentContext;
	}

	public void setComponentContext(ComponentContext componentContext) {
		this.componentContext = componentContext;
	}

}
