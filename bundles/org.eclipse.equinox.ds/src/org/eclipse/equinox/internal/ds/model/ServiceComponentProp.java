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
package org.eclipse.equinox.internal.ds.model;

import java.security.*;
import java.util.*;
import org.eclipse.equinox.internal.ds.*;
import org.eclipse.equinox.internal.ds.impl.ComponentContextImpl;
import org.eclipse.equinox.internal.ds.impl.ComponentInstanceImpl;
import org.osgi.framework.*;
import org.osgi.service.component.*;

/**
 * The ServiceComponentProp represents a ServiceComponent mapped to it's CM
 * properties.
 * 
 * When the ServiceComponent contains plain CM properties, the mapping of
 * ServiceComponentProp to ServiceComponent is 1:1.
 * 
 * However, when the ServiceComponent is a ManagedServiceFactory, there are so
 * many ServiceComponentProp objects created as many are the configurations
 * associated with the component.
 * 
 * @author Valentin Valchev
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 * @version 1.2
 */

public class ServiceComponentProp implements PrivilegedExceptionAction {
	public static final int DISPOSED = 0;
	public static final int DISPOSING = 1;
	public static final int SATISFIED = 2;
	public static final int BUILDING = 3;
	public static final int BUILT = 4;

	public ServiceRegistration registration;
	public String name;
	public ServiceComponent serviceComponent;
	public Hashtable properties;
	public Vector instances = new Vector(2); // ComponentInstance objects
	public BundleContext bc;
	public Vector references;

	private boolean kindOfFactory;

	// This flag is used to check whether the component is a component factory.
	// Since the component factory creates new ServiceComponentProp objects they
	// have to
	// be marked as non component factory components in order to prevent newly
	// registered
	// component factories as services.
	protected boolean isComponentFactory = false;

	//Holds the component's state
	private int state = SATISFIED;

	/**
	 * List of names (Strings) of Component Configurations we should not
	 * activate during the activation of this Component Configuration. This is
	 * populated by the {@link org.eclipse.equinox.internal.ds.Resolver Resolver}
	 * and used by
	 * {@link org.eclipse.equinox.internal.ds.InstanceProcess InstanceProcess}.
	 */
	protected Vector delayActivateSCPNames;

	private SCRManager mgr;

	// next free component id
	private static long componentid = 0;

	public ServiceComponentProp(ServiceComponent serviceComponent, Dictionary configProperties, SCRManager mgr) {

		this.serviceComponent = serviceComponent;
		this.name = serviceComponent.name;
		this.bc = serviceComponent.bc;

		initProperties(configProperties);

		// cache locally if it is some kind of a factory
		kindOfFactory = serviceComponent.factory != null || serviceComponent.serviceFactory;
		isComponentFactory = serviceComponent.factory != null;

		// used for component context
		this.mgr = mgr;
	}

	/**
	 * This method will dispose the service component instance. Along with the
	 * service component itself and the properties files.
	 */
	public void dispose() {
		if (Activator.DEBUG) {
			Activator.log.debug(0, 10035, name, null, false);
			// //Activator.log.debug("ServiceComponentProp.dispose(): ", null);
		}
		setState(DISPOSED);
		while (!instances.isEmpty()) {
			ComponentInstanceImpl current = (ComponentInstanceImpl) instances.firstElement();
			dispose(current);
			current.dispose();
		}
	}

	/**
	 * getProperties
	 * 
	 * @return Dictionary properties
	 */
	public Hashtable getProperties() {
		return properties != null ? properties : serviceComponent.properties;
	}

	/**
	 * This method will call the activate method on the specified object.
	 * 
	 * @param usingBundle
	 *            the bundle that is using the component - this hase only means
	 *            when the component is factory
	 * @param componentInstance
	 *            the component instance whcich will be activated.
	 * @throws Exception
	 *             could be thrown if the activate fails for some reason but NOT
	 *             in case, if the instance doesn't define an activate method.
	 */
	public void activate(Bundle usingBundle, ComponentInstanceImpl componentInstance) throws Exception {
		if (Activator.DEBUG) {
			Activator.log.debug(0, 10036, name, null, false);
			// //Activator.log.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ServiceComponentProp.activate():
			// name: " + name, null);
			Activator.log.debug(0, 10039, (usingBundle != null ? usingBundle.getSymbolicName() : null), null, false);
			// //Activator.log.debug(
			// // "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ServiceComponentProp.using
			// bundle: "
			// // + (usingBundle != null ? usingBundle.getSymbolicName() :
			// null),
			// // null);
			Activator.log.debug(0, 10037, componentInstance.toString(), null, false);
			// //Activator.log.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ServiceComponentProp.instance:
			// " + componentInstance, null);
		}

		// call the activate method on the Service Component
		serviceComponent.activate(componentInstance.getInstance(), componentInstance.getComponentContext());
	}

	/**
	 * This method will call the deactivate method on the specified Object.
	 * Notice, that this is not the ComponentInstance object, but the real
	 * instance.
	 * 
	 * @param componentInstance
	 *            component instance wrapper
	 */
	private void deactivate(ComponentInstanceImpl componentInstance) {
		if (Activator.DEBUG) {
			Activator.log.debug(0, 10038, name, null, false);
			// //Activator.log.debug("ServiceComponentProp.deactivate(): " +
			// name, null);
		}
		serviceComponent.deactivate(componentInstance.getInstance(), componentInstance.getComponentContext());
	}

	/**
	 * Call the bind method for each of the Referenced Services in this Service
	 * Component
	 * 
	 * @param componentInstance
	 * @throws Exception
	 */
	public void bind(ComponentInstance componentInstance) throws Exception {
		// Get all the required service Reference Descriptions for this Service
		// Component
		// call the Bind method if the Reference Description includes one
		if (references != null) {
			for (int i = 0; i < references.size(); i++) {
				Reference ref = (Reference) references.elementAt(i);
				if (ref.reference.bind != null) {
					bindReference(ref, componentInstance);
				} else if (Activator.DEBUG) {
					Activator.log.debug(0, 10040, ref.reference.name, null, false);
					// //Activator.log.debug(
					// // "ServiceComponentProp:bind(): the reference '" +
					// ref.name + "' doesn't specify bind method",
					// // null);
				}
			}
		}
	}

	/**
	 * Call the unbind method for each of the Referenced Services in this
	 * Serivce Component
	 * 
	 * @param componentInstance
	 *            the object which unbind method(s) will be called, if ANY!
	 */
	public void unbind(ComponentInstance componentInstance) {
		// call the unBind method if the Reference Description includes one
		if (references != null) {
			for (int i = references.size() - 1; i >= 0; i--) {
				Reference ref = (Reference) references.elementAt(i);
				if (ref.reference.unbind != null) {
					unbindReference(ref, componentInstance);
				}
			}
		}
	}

	public Object createInstance() throws Exception {
		assertCreateSingleInstance();
		return serviceComponent.createInstance();
	}

	boolean locked = false;
	int waiting = 0;

	Bundle bundle;
	Object inst;

	synchronized void lock(Bundle usingBundle, Object instance) {
		while (locked) {
			try {
				waiting++;
				wait();
			} catch (Exception _) {
			}
			waiting--;
		}
		locked = true;
		bundle = usingBundle;
		inst = instance;
	}

	synchronized void unlock() {
		locked = false;
		bundle = null;
		inst = null;
		if (waiting > 0)
			notifyAll();
	}

	public Object run() throws Exception {
		Bundle bundle = this.bundle;
		Object instance = inst;
		unlock();
		return build(bundle, instance, false);
	}

	public ComponentInstanceImpl build(Bundle usingBundle, Object instance, boolean security) throws Exception {
		if (getState() == DISPOSED) {
			if (Activator.DEBUG) {
				Activator.log.debug("Cannot build component, because it is already disposed: " + this, null);
			}
			return null;
		}

		if (security) {
			this.lock(usingBundle, instance);
			try {
				return (ComponentInstanceImpl) AccessController.doPrivileged(this);
			} catch (PrivilegedActionException pae) {

			}
		}
		ComponentInstanceImpl componentInstance = null;
		if (instance == null) {
			if (!kindOfFactory) {
				// it is a plain service, this is because this method
				// is also called from ServiceReg
				if (instances.isEmpty()) {
					instance = createInstance();
				} else {
					componentInstance = (ComponentInstanceImpl) instances.firstElement();
				}
			} else {
				instance = createInstance();
			}
		}
		if (componentInstance == null) {
			componentInstance = new ComponentInstanceImpl(instance, this);
			componentInstance.setComponentContext(new ComponentContextImpl(this, usingBundle, componentInstance, mgr));
			instances.addElement(componentInstance);
			bind(componentInstance);
			try {
				activate(usingBundle, componentInstance);
			} catch (Exception e) {
				//must unbind and dispose this component instance 
				InstanceProcess.resolver.removeFromSatisfiedList(this);
				if (instances.removeElement(componentInstance)) {
					unbind(componentInstance);
				}
				throw e;
			}
		}

		return componentInstance;
	}

	public void disposeObj(Object obj) {
		ComponentInstanceImpl ci = null;
		synchronized (instances) {
			for (int i = 0; i < instances.size(); i++) {
				ci = (ComponentInstanceImpl) instances.elementAt(i);
				if (ci.getInstance() == obj) {
					break;
				} else {
					ci = null;
				}
			}
		}
		if (ci != null) {
			dispose(ci);
			return;
		}
		throw new RuntimeException("The Object '" + obj + "' is not created by the component named " + name);
	}

	public void dispose(ComponentInstanceImpl componentInstance) {
		if (!instances.removeElement(componentInstance)) {
			return; //the instance is already disposed  
		}

		deactivate(componentInstance);
		unbind(componentInstance);
	}

	/**
	 * Call the bind method for this referenceDescription
	 * 
	 * @param componentReference
	 * @param componentInstance
	 * @throws Exception
	 * 
	 */
	public void bindReference(Reference reference, ComponentInstance componentInstance) throws Exception {

		// bind the services ONLY if bind method is specified!
		if (reference.reference.bind == null) {
			return;
		}

		// ok, proceed!
		if (Activator.DEBUG) {
			Activator.log.debug(0, 10041, serviceComponent.name + " -> " + reference.reference, null, false);
			// //Activator.log.debug(
			// // "ServiceComponentProp.bindReferences(): component " +
			// serviceComponent.name + " to " + componentReference,
			// // null);
		}

		ServiceReference[] serviceReferences = null;

		// if there is a published service, then get the ServiceObject and call
		// bind
		try {
			// get all registered services using this target filter
			serviceReferences = bc.getServiceReferences(reference.reference.interfaceName, reference.reference.target);
		} catch (Exception e) {
			Activator.log.error("[SCR] Cannot get references for " + reference.reference.interfaceName, e);
			throw e;
			// rethrow exception so resolver is eventually notified that this
			// SCP is bad
		}

		// bind only if there is at least ONE service
		if (serviceReferences != null && serviceReferences.length > 0) {
			// the component binds to the first available service only!
			switch (reference.reference.cardinality) {
				case ComponentReference.CARDINALITY_1_1 :
				case ComponentReference.CARDINALITY_0_1 :
					reference.reference.bind(reference, componentInstance, serviceReferences[0]);
					break;
				case ComponentReference.CARDINALITY_1_N :
				case ComponentReference.CARDINALITY_0_N :
					// bind to all services
					for (int i = 0; i < serviceReferences.length; i++) {
						reference.reference.bind(reference, componentInstance, serviceReferences[i]);
					}
					break;
			}
		} else {
			if (Activator.DEBUG) {
				Activator.log.debug("ServiceComponentProp.bindReference(): The service is not yet registered, but it is already instantiated", null);
			}
		}
	}

	/**
	 * Call the unbind method for this Reference Description
	 * 
	 * @param reference
	 * @param componentInstance
	 */
	public void unbindReference(Reference reference, ComponentInstance componentInstance) {

		// unbind the services ONLY if unbind method is specified!
		if (reference.reference.unbind == null) {
			return;
		}

		// ok, proceed!

		if (Activator.DEBUG) {
			Activator.log.debug(0, 10042, serviceComponent.name + " <- " + reference.reference, null, false);
			// //Activator.log.debug(
			// // "ServiceComponentProp.unbindReference(): component " + name +
			// " from " + componentReference,
			// // null);
		}

		Vector serviceReferences = reference.reference.serviceReferences;
		for (int i = serviceReferences.size() - 1; i >= 0; i--) {
			reference.reference.unbind(reference, componentInstance, (ServiceReference) serviceReferences.elementAt(i));
		}
	}

	/**
	 * Call the unbind method for this Reference Description
	 * 
	 * @param ref
	 * @param instance
	 * @param serviceObject
	 * @throws Exception
	 */
	public void unbindDynamicReference(Reference ref, ComponentInstance instance, ServiceReference serviceReference) throws Exception {
		try {
			if (Activator.DEBUG) {
				Activator.log.debug("ServiceComponentProp.unbindDynamicReference(): component = " + name + ", reference = " + ref.reference.name, null);
			}
			// check if we need to rebind
			switch (ref.reference.cardinality) {
				case ComponentReference.CARDINALITY_0_1 :
				case ComponentReference.CARDINALITY_1_1 :
					if (ref.reference.bind != null) {
						bindReference(ref, instance);
					}
			}
			ref.reference.unbind(ref, instance, serviceReference);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	// -- begin helper methods
	/**
	 * Initialize Properties for this Component
	 * 
	 * The property elements provide default or supplemental property values if
	 * not overridden by the properties retrieved from Configuration Admin
	 * 
	 * @param configProperties
	 *            the configuration properties
	 */
	private void initProperties(Dictionary configProperties) {
		// default component service properties
		Properties propertyDescriptions = serviceComponent.properties;
		if (propertyDescriptions != null && !propertyDescriptions.isEmpty()) {
			properties = (Hashtable) propertyDescriptions.clone();
		}

		// set the component.name
		if (properties == null) {
			properties = new Hashtable(7);
		}

		// put the references in the properties
		if (serviceComponent.references != null) {
			ComponentReference ref;
			for (int i = 0; i < serviceComponent.references.size(); i++) {
				ref = (ComponentReference) serviceComponent.references.elementAt(i);
				if (ref.target != null) {
					properties.put(ref.name + ComponentConstants.REFERENCE_TARGET_SUFFIX, ref.target);
				}
			}
		}

		// properties from Configuration Admin
		if (configProperties != null && !configProperties.isEmpty()) {
			for (Enumeration keys = configProperties.keys(); keys.hasMoreElements();) {
				Object key = keys.nextElement();
				Object val = configProperties.get(key);
				properties.put(key, val);
			}
		}

		// always set the component name & the id
		Long nextId = new Long(getNewComponentID());
		properties.put(ComponentConstants.COMPONENT_ID, nextId);
		properties.put(ComponentConstants.COMPONENT_NAME, serviceComponent.name);

		if (serviceComponent.provides != null) {
			String[] provides = new String[serviceComponent.provides.length];
			System.arraycopy(serviceComponent.provides, 0, provides, 0, provides.length);
			properties.put(Constants.OBJECTCLASS, provides);
		}
	}

	private void assertCreateSingleInstance() {
		if (!kindOfFactory && !instances.isEmpty()) {
			throw new ComponentException("Instance of '" + name + "'is already created!");
		}
	}

	public String toString() {
		return name;
	}

	public void setRegistration(ServiceRegistration reg) {
		registration = reg;
	}

	/**
	 * Add a new Component Configuration name we should not activate in order to
	 * prevent a cycle.
	 * 
	 * @see ServiceComponentProp#delayActivateSCPNames
	 */
	public void setDelayActivateSCPName(String scpName) {
		if (delayActivateSCPNames == null) {
			delayActivateSCPNames = new Vector(1);
			delayActivateSCPNames.addElement(scpName);
		} else if (!delayActivateSCPNames.contains(scpName)) {
			delayActivateSCPNames.addElement(scpName);
		}
	}

	public Vector getDelayActivateSCPNames() {
		return delayActivateSCPNames;
	}

	public boolean isComponentFactory() {
		return isComponentFactory;
	}

	// used by the ComponentFactoryImpl to mark component configs created by it
	// that
	// they are not component factories. This avoids subsecuent registrations of
	// the same component factory.
	public void setComponentFactory(boolean isComponentFactory) {
		this.isComponentFactory = isComponentFactory;
	}

	static synchronized long getNewComponentID() {
		return componentid++;
	}

	public boolean isKindOfFactory() {
		return kindOfFactory;
	}

	public synchronized int getState() {
		return state;
	}

	public synchronized void setState(int state) {
		this.state = state;
	}

}
