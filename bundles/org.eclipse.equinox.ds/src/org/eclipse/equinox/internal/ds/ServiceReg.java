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

import org.eclipse.equinox.internal.ds.impl.ComponentInstanceImpl;
import org.eclipse.equinox.internal.ds.model.ServiceComponentProp;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentException;

/**
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 * @version 1.2
 */

final class ServiceReg implements ServiceFactory {

	static boolean dontDisposeInstances = true;

	// tracking the instance usage and re-instantiation
	private int useCount = 0;
	private ComponentInstanceImpl instance;

	// model
	private ServiceComponentProp scp;

	static {
		String tmp = Activator.bc.getProperty("equinox.scr.dontDisposeInstances"); //$NON-NLS-1$
		dontDisposeInstances = (tmp != null) ? !tmp.equalsIgnoreCase("false") : true; //$NON-NLS-1$
	}

	ServiceReg(ServiceComponentProp scp, ComponentInstanceImpl instance) {
		this.scp = scp;
		this.instance = instance;
	}

	// ServiceFactory.getService method.
	public Object getService(Bundle bundle, ServiceRegistration registration) {
		try {
			if (instance == null) {
				instance = InstanceProcess.staticRef.buildComponent(bundle, scp, null, false);
				//instance could be null if the component is already disposed
				if (instance == null) {
					return null;
				}
			}
			synchronized (this) {
				useCount++;
			}
			if (Activator.DEBUG) {
				Activator.log.debug("ServiceReg.getService(): " + NLS.bind(Messages.SERVICE_USAGE_COUNT, scp.name, Integer.toString(useCount)) + ", object = " + instance.getInstance(), null); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return instance.getInstance();
		} catch (Exception e) {
			if (!(e instanceof ComponentException)) {
				Activator.log.error(NLS.bind(Messages.CANNOT_CREATE_INSTANCE, scp.name), e);
				return null;
			}
			throw (ComponentException) e;
		}
	}

	// ServiceFactory.ungetService method.
	public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
		boolean shallDispose = false;
		synchronized (this) {
			useCount--;
			if (useCount == 0) {
				shallDispose = true;
			}
		}
		if (shallDispose) {
			if (!dontDisposeInstances && !scp.serviceComponent.immediate) {
				//dispose instance only if disposing is allowed and the component is not immediate one.

				//Immediate components are custom case - according to me, their instances should not be disposed
				// because they are probably needed during the whole component's life  
				if (Activator.DEBUG) {
					Activator.log.debug(NLS.bind(Messages.SERVICE_NO_LONGER_USED, scp.name, service), null);
				}
				// dispose only the instance - don't dispose the component
				// itself!
				scp.disposeObj(service, ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED);
				// delete the instance so it can be garbage collected!
				instance = null;
			} else {
				if (Activator.DEBUG) {
					Activator.log.debug("ServiceReg.ungetService(): " + NLS.bind(Messages.SERVICE_USAGE_COUNT, scp.name, Integer.toString(useCount)), null); //$NON-NLS-1$
				}
			}
		} else {
			if (useCount < 0) {
				Activator.log.warning("ServiceReg.ungetService(): " + NLS.bind(Messages.SERVICE_USAGE_COUNT, scp.name, Integer.toString(useCount)), new Exception("Debug callstack")); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (Activator.DEBUG) {
				Activator.log.debug("ServiceReg.ungetService(): " + NLS.bind(Messages.SERVICE_USAGE_COUNT, scp.name, Integer.toString(useCount)), null); //$NON-NLS-1$
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return scp.name + " Service Registration"; //$NON-NLS-1$
	}
}
