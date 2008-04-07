/*******************************************************************************
 * Copyright (c) 1997-2007 by ProSyst Software GmbH
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

import java.util.Dictionary;
import java.util.Vector;
import org.eclipse.equinox.internal.ds.*;
import org.eclipse.equinox.internal.ds.model.*;
import org.osgi.framework.*;
import org.osgi.service.component.*;

/**
 * ComponentContextImpl.java
 * 
 * @author Valentin Valchev, Nina Ruseva
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class ComponentContextImpl implements ComponentContext {

	/* ComponentInstance instance */
	private ComponentInstanceImpl componentInstance;
	/* ComponentDescription */
	private ServiceComponentProp scp;

	private Bundle usingBundle;

	private SCRManager mgr;

	public ComponentContextImpl(ServiceComponentProp scp, Bundle usingBundle, ComponentInstanceImpl ci, SCRManager mgr) {
		this.scp = scp;
		this.componentInstance = ci;
		this.usingBundle = usingBundle;
		this.mgr = mgr;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.component.ComponentContext#getProperties()
	 */
	public Dictionary getProperties() {
		return (Dictionary) scp.getProperties().clone();
	}

	/*
	 * (non-Javadoc) Returns the service object for the specified service
	 * reference name.
	 * 
	 * @param name The name of a service reference as specified in a <code>reference</code>
	 * element in this component's description. @return A service object for the
	 * referenced service or <code>null</code> if the reference cardinality is
	 * <code>0..1</code> or <code>0..n</code> and no matching service is
	 * available. @throws ComponentException If the Service Component Runtime
	 * catches an exception while activating the target service.
	 * 
	 * @see org.osgi.service.component.ComponentContext#locateService(java.lang.String)
	 */
	public Object locateService(String name) {
		if (scp.references == null) {
			return null;
		}
		if (Activator.DEBUG) {
			Activator.log.debug(0, 10070, name, null, false);
			// //Activator.log.debug("ComponentContextImpl.locateService(): " +
			// name, null);
		}
		Vector references = scp.references;
		for (int i = 0; i < references.size(); i++) {
			Reference reference = (Reference) references.elementAt(i);
			ComponentReference ref = reference.reference;

			// find the Reference Description with the specified name
			if (ref.name.equals(name)) {
				ServiceReference serviceReference = null;
				synchronized (ref.serviceReferences) {
					if (!ref.serviceReferences.isEmpty()) {
						serviceReference = (ServiceReference) ref.serviceReferences.keys().nextElement();
					}
				}
				try {
					if (serviceReference == null) {
						// try to find service in the FW
						ServiceReference[] serviceReferences = scp.bc.getServiceReferences(ref.interfaceName, ref.target);
						if (serviceReferences != null && serviceReferences.length > 0) {
							// the servicese references are sorted by
							// service.reference and service.id
							// so get the first one in the list
							serviceReference = serviceReferences[0];
						}

					}
					if (serviceReference != null) {
						Object cached = componentInstance.bindedServices.get(serviceReference);
						if (cached != null) {
							// will skip the circularity checking in
							// InstanceProcess.getService
							return cached;
						}
						Object theService = InstanceProcess.staticRef.getService(reference, serviceReference);
						// the service object could be null because of
						// circularity
						if (theService != null) {
							componentInstance.bindedServices.put(serviceReference, theService);
							return theService;
						}
					}
				} catch (Throwable t) {
					if (t instanceof ComponentException) {
						throw (ComponentException) t;
					}
					throw new ComponentException("Exception occurred while locating service for interface " + name, t); //$NON-NLS-1$
				}
				if (Activator.DEBUG) {
					Activator.log.debug("ComponentContextImpl.locateService(): error, service not found - " + ref.interfaceName + "; the comp. context belongs to " + scp.name, null);
				}
			}
		}
		return null;
	}

	/*
	 * Returns the service objects for the specified service reference name.
	 * 
	 * @param name The name of a service reference as specified in a <code>reference</code>
	 * element in this component's description. @return An array of service
	 * objects for the referenced service or <code>null</code> if the
	 * reference cardinality is <code>0..1</code> or <code>0..n</code> and
	 * no matching service is available.
	 * 
	 * @throws ComponentException If the Service Component Runtime catches an
	 * exception while activating a target service.
	 * 
	 * @see org.osgi.service.component.ComponentContext#locateServices(java.lang.String)
	 */
	public Object[] locateServices(String name) {
		if (scp.references == null) {
			return null;
		}

		if (Activator.DEBUG) {
			Activator.log.debug(0, 10071, name, null, false);
			// //Activator.log.debug("ComponentContextImpl.locateServices(): " +
			// name, null);
		}
		Vector references = scp.references;
		for (int i = 0; i < references.size(); i++) {
			Reference reference = (Reference) references.elementAt(i);
			ComponentReference ref = reference.reference;

			if (ref.name.equals(name)) {
				ServiceReference[] serviceReferences = null;
				try {
					serviceReferences = scp.bc.getServiceReferences(ref.interfaceName, ref.target);
					if (serviceReferences != null) {
						Vector theServices = new Vector(5);
						Object service;
						for (int j = 0; j < serviceReferences.length; j++) {
							// check whether the service is cached - this will
							// skip
							// the circularity checking in
							// InstanceProcess.getService
							service = componentInstance.bindedServices.get(serviceReferences[j]);
							if (service == null) {
								service = InstanceProcess.staticRef.getService(reference, serviceReferences[j]);
							}
							// the service object could be null because of
							// circularity
							if (service != null) {
								theServices.addElement(service);
								componentInstance.bindedServices.put(serviceReferences[j], service);
							}
						}
						if (!theServices.isEmpty()) {
							Object ret[] = new Object[theServices.size()];
							for (int j = 0; j < ret.length; j++) {
								ret[j] = theServices.elementAt(j);
							}
							return ret;
						}
					}
				} catch (Throwable t) {
					if (t instanceof ComponentException) {
						throw (ComponentException) t;
					}
					throw new ComponentException("Exception occurred while locating services for interface " + name, t);
				}
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.component.ComponentContext#locateService(String
	 *      name, ServiceReference reference)
	 */
	public Object locateService(String name, ServiceReference serviceReference) {
		if (scp.references == null) {
			return null;
		}
		if (Activator.DEBUG) {
			Activator.log.debug(0, 10070, name + " by service reference : " + serviceReference, null, false);
			// //Activator.log.debug("ComponentContextImpl.locateService(): " +
			// name, null);
		}
		Vector references = scp.references;
		try {
			for (int i = 0; i < references.size(); i++) {
				Reference reference = (Reference) references.elementAt(i);
				ComponentReference ref = reference.reference;

				if (ref.name.equals(name)) {
					if (serviceReference == null || !ref.serviceReferences.containsKey(serviceReference)) {
						// the serviceReference is not bound to the specified
						// reference
						if (Activator.DEBUG) {
							String referenceToString = (serviceReference == null) ? null : serviceReference.toString();
							Activator.log.debug(0, 10072, referenceToString, null, false);
						}
						return null;
					}

					Object cached = componentInstance.bindedServices.get(serviceReference);
					if (cached != null) {
						// will skip the circularity checking in
						// InstanceProcess.getService
						return cached;
					}
					Object theService = InstanceProcess.staticRef.getService(reference, serviceReference);
					// the service object could be null because of circularity
					if (theService != null) {
						componentInstance.bindedServices.put(serviceReference, theService);
						return theService;
					}
				}
			}
		} catch (Throwable t) {
			if (t instanceof ComponentException) {
				throw (ComponentException) t;
			}
			throw new ComponentException("Exception occurred while locating service for interface " + name, t);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.component.ComponentContext#getBundleContext()
	 */
	public BundleContext getBundleContext() {
		return scp.bc;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.component.ComponentContext#getUsingBundle()
	 */
	public Bundle getUsingBundle() {
		// this is only for service factories!
		ServiceComponent componentDescription = scp.serviceComponent;
		if ((componentDescription.provides == null) || (!componentDescription.serviceFactory)) {
			return null;
		}
		return usingBundle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.component.ComponentContext#getComponentInstance()
	 */
	public ComponentInstance getComponentInstance() {
		return componentInstance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.component.ComponentContext#enableComponent(java.lang.String)
	 */
	public void enableComponent(String name) {
		mgr.enableComponent(name, scp.bc.getBundle());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.component.ComponentContext#disableComponent(java.lang.String)
	 */
	public void disableComponent(String name) {
		mgr.disableComponent(name, scp.bc.getBundle());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.component.ComponentContext#getServiceReference()
	 */
	public ServiceReference getServiceReference() {
		if (scp.serviceComponent.provides != null) {
			ServiceRegistration reg = scp.registration;
			if (reg != null) {
				return reg.getReference();
			}
		}
		return null;
	}

}
