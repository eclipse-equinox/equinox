/*******************************************************************************
 * Copyright (c) 1997-2011 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *    Joerg-Christian Boehme - bug.id = 246757
 *******************************************************************************/
package org.eclipse.equinox.internal.ds.model;

import java.security.*;
import java.util.*;
import org.apache.felix.scr.Component;
import org.eclipse.equinox.internal.ds.*;
import org.eclipse.equinox.internal.ds.impl.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.component.*;
import org.osgi.service.log.LogService;

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
 */

public class ServiceComponentProp implements Component, PrivilegedExceptionAction {

	public ServiceRegistration registration;
	public String name;
	public ServiceComponent serviceComponent;
	public Hashtable properties;
	public Vector instances = new Vector(2); // ComponentInstance objects
	public BundleContext bc;
	public Vector references;

	// This flag is used to check whether the component is a component factory.
	// Since the component factory creates new ServiceComponentProp objects they
	// have to
	// be marked as non component factory components in order to prevent newly
	// registered
	// component factories as services.
	protected boolean isComponentFactory = false;

	//Holds the component's state
	private int state = STATE_UNSATISFIED;

	/**
	 * List of names (Strings) of Component Configurations we should not
	 * activate during the activation of this Component Configuration. This is
	 * populated by the {@link org.eclipse.equinox.internal.ds.Resolver Resolver}
	 * and used by
	 * {@link org.eclipse.equinox.internal.ds.InstanceProcess InstanceProcess}.
	 */
	protected Vector delayActivateSCPNames;

	private SCRManager mgr;
	private ReadOnlyDictionary readOnlyProps;

	// next free component id
	private static long componentid = 0;

	public ServiceComponentProp(ServiceComponent serviceComponent, Dictionary configProperties, SCRManager mgr) {

		this.serviceComponent = serviceComponent;
		this.name = serviceComponent.name;
		this.bc = serviceComponent.bc;

		properties = initProperties(configProperties, null);

		isComponentFactory = serviceComponent.factory != null;

		// used for component context
		this.mgr = mgr;
	}

	/**
	 * This method will dispose the service component instance. Along with the
	 * service component itself and the properties files.
	 */
	public void dispose(int deactivateReason) {
		if (Activator.DEBUG) {
			Activator.log.debug("ServiceComponentProp.dispose(): " + name, null); //$NON-NLS-1$
		}
		try {
			while (!instances.isEmpty()) {
				ComponentInstanceImpl current = (ComponentInstanceImpl) instances.firstElement();
				dispose(current, deactivateReason);
				current.dispose();
			}
		} finally {
			setState(STATE_UNSATISFIED);
		}
	}

	/**
	 * getProperties
	 * 
	 * @return Dictionary properties
	 */
	public Dictionary getProperties() {
		if (readOnlyProps == null) {
			readOnlyProps = new ReadOnlyDictionary(properties != null ? properties : serviceComponent.properties);
		} else {
			// the scp properties may have been modified by configuration
			// update the instance with the current properties
			readOnlyProps.updateDelegate(properties != null ? properties : serviceComponent.properties);
		}
		return readOnlyProps;
	}

	/**
	 * This method will call the activate method on the specified object.
	 * 
	 * @param usingBundle
	 *            the bundle that is using the component - this has only means
	 *            when the component is factory
	 * @param componentInstance
	 *            the component instance which will be activated.
	 * @throws Exception
	 *             could be thrown if the activate fails for some reason but NOT
	 *             in case, if the instance doesn't define an activate method.
	 */
	public void activate(Bundle usingBundle, ComponentInstanceImpl componentInstance) throws Exception {
		if (Activator.DEBUG) {
			Activator.log.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ServiceComponentProp.activate(): name: " + name, null); //$NON-NLS-1$
			Activator.log.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ServiceComponentProp.activate(): using bundle: " + (usingBundle != null ? usingBundle.getSymbolicName() : null), null); //$NON-NLS-1$
			Activator.log.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ServiceComponentProp.activate(): instance: " + componentInstance.toString(), null); //$NON-NLS-1$
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
	private void deactivate(ComponentInstanceImpl componentInstance, int deactivateReason) {
		if (Activator.DEBUG) {
			Activator.log.debug("ServiceComponentProp.deactivate(): " + name, null); //$NON-NLS-1$
		}
		serviceComponent.deactivate(componentInstance.getInstance(), componentInstance.getComponentContext(), deactivateReason);
	}

	/**
	 * This method will update the properties of the component
	 * 
	 * @param newProps
	 *            the new properties
	 * @throws Exception
	 *             could be thrown if the modify method fails for some reason but NOT
	 *             in case, if the instance doesn't define modify method
	 */
	public void modify(Dictionary newProps) throws Exception {
		if (Activator.DEBUG) {
			Activator.log.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ServiceComponentProp.modify(): name: " + name, null); //$NON-NLS-1$
		}
		Hashtable oldProperties = null;
		if (references != null && references.size() > 0) {
			oldProperties = (Hashtable) properties.clone();
		}
		//1. update the properties
		properties = initProperties(newProps, (Long) properties.get(ComponentConstants.COMPONENT_ID));
		//2. call the modify method on the Service Component for all instances of this scp
		for (int i = 0; i < instances.size(); i++) {
			ComponentInstanceImpl componentInstance = (ComponentInstanceImpl) instances.elementAt(i);
			Activator.log.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ServiceComponentProp.modify(): instance: " + componentInstance.toString(), null); //$NON-NLS-1$
			serviceComponent.modified(componentInstance.getInstance(), componentInstance.getComponentContext());
		}
		//3. modify the bound services if necessary
		if (oldProperties != null) {
			handleBoundServicesUpdate(oldProperties, properties);
		}

		//4. if the component configuration is registered as a service, 
		// modify the service’s service properties 
		if (registration != null) {
			registration.setProperties(getPublicServiceProperties());
		}
	}

	/**
	 * Call the bind method for each of the Referenced Services in this Service
	 * Component
	 * 
	 * @param componentInstance
	 * @return true if all mandatory references are bound
	 * @throws Exception
	 */
	public boolean bind(ComponentInstance componentInstance) throws Exception {
		// Get all the required service Reference Descriptions for this ServiceComponent
		// call the Bind method if the Reference Description includes one
		if (references != null) {
			for (int i = 0; i < references.size(); i++) {
				Reference ref = (Reference) references.elementAt(i);
				ClassCircularityError ccError = null;
				if (ref.reference.bind != null) {
					try {
						bindReference(ref, componentInstance);
					} catch (ClassCircularityError cce) {
						ccError = cce;
						Activator.log(bc, LogService.LOG_ERROR, NLS.bind(Messages.ERROR_BINDING_REFERENCE, ref.reference), cce);
					}
					if (ref.reference.bindMethod == null || ccError != null || !ref.isBound()) {
						//the bind method is not found and called for some reason or it has thrown exception
						if (ref.reference.cardinality == ComponentReference.CARDINALITY_1_1 || ref.reference.cardinality == ComponentReference.CARDINALITY_1_N) {
							Activator.log(null, LogService.LOG_WARNING, "Could not bind a reference of component " + name + ". The reference is: " + ref.reference, null); //$NON-NLS-1$ //$NON-NLS-2$
							//unbind the already bound references
							for (int j = i - 1; j >= 0; j--) {
								ref = (Reference) references.elementAt(i);
								if (ref.reference.unbind != null) {
									unbindReference(ref, componentInstance);
								}
							}
							if (ccError != null) {
								//rethrow the error so it is further processed according to the use case
								throw ccError;
							}
							return false;
						}
					}
				} else {
					if (Activator.DEBUG) {
						Activator.log.debug("ServiceComponentProp.bind(): the folowing reference doesn't specify bind method: " + ref.reference.name, null); //$NON-NLS-1$
					}
				}
			}
		}
		return true;
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
				//
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
		Bundle b = this.bundle;
		Object instance = inst;
		unlock();
		return build(b, instance, false);
	}

	public ComponentInstanceImpl build(Bundle usingBundle, Object instance, boolean security) throws Exception {
		if (getState() == STATE_DISPOSED) {
			if (Activator.DEBUG) {
				Activator.log.debug("Cannot build component, because it is already disposed: " + this, null); //$NON-NLS-1$
			}
			return null;
		}

		if (security) {
			this.lock(usingBundle, instance);
			try {
				return (ComponentInstanceImpl) AccessController.doPrivileged(this);
			} catch (PrivilegedActionException pae) {
				//
			}
		}
		ComponentInstanceImpl componentInstance = null;
		if (instance == null) {
			if (!serviceComponent.serviceFactory) {
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
			if (bind(componentInstance)) {
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
			} else {
				//must remove from satisfied list and remove the instance
				InstanceProcess.resolver.removeFromSatisfiedList(this);
				instances.removeElement(componentInstance);
				throw new ComponentException(NLS.bind(Messages.COMPONENT_WAS_NOT_BUILT, serviceComponent));
			}
		}
		setState(Component.STATE_ACTIVE);
		return componentInstance;
	}

	public void disposeObj(Object obj, int deactivateReason) {
		ComponentInstanceImpl ci = null;
		synchronized (instances) {
			for (int i = 0; i < instances.size(); i++) {
				ci = (ComponentInstanceImpl) instances.elementAt(i);
				if (ci.getInstance() == obj) {
					break;
				}
				ci = null;
			}
		}
		if (ci != null) {
			dispose(ci, deactivateReason);
			return;
		}
		throw new RuntimeException(NLS.bind(Messages.INVALID_OBJECT, obj, name));
	}

	public void dispose(ComponentInstanceImpl componentInstance, int deactivateReason) {
		if (!instances.removeElement(componentInstance)) {
			return; //the instance is already disposed  
		}
		deactivate(componentInstance, deactivateReason);
		unbind(componentInstance);
		if (instances.isEmpty()) {
			//there are no active instances. The component is lazy enabled now
			setState(Component.STATE_REGISTERED);
		}
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

		if (Activator.DEBUG) {
			Activator.log.debug("ServiceComponentProp.bindReference(): component " + serviceComponent.name + " -> " + reference.reference, null); //$NON-NLS-1$ //$NON-NLS-2$
		}

		ServiceReference[] serviceReferences = null;

		// if there is a published service, then get the ServiceObject and call bind
		try {
			// get all registered services using this target filter
			serviceReferences = bc.getServiceReferences(reference.reference.interfaceName, reference.getTarget());
		} catch (Exception e) {
			Activator.log(bc, LogService.LOG_ERROR, NLS.bind(Messages.CANNOT_GET_REFERENCES, reference.reference.interfaceName), e);
			throw e;
			// rethrow exception so resolver is eventually notified that this SCP is bad
		}

		// bind only if there is at least ONE service
		if (serviceReferences != null && serviceReferences.length > 0) {
			// the component binds to the first available service only!
			if (reference.reference.bind != null) {
				switch (reference.reference.cardinality) {
					case ComponentReference.CARDINALITY_1_1 :
					case ComponentReference.CARDINALITY_0_1 :
						for (int i = 0; i < serviceReferences.length; i++) {
							ServiceReference oldBoundService = (reference.reference.policy_option == ComponentReference.POLICY_OPTION_GREEDY && reference.reference.serviceReferences.size() > 0 ? (ServiceReference) reference.reference.serviceReferences.keys().nextElement() : null);
							boolean bound = reference.reference.bind(reference, componentInstance, serviceReferences[i]);
							if (bound) {
								if (oldBoundService != null) {
									//unbind the previous bound service reference in case we are handling service reference update due to greedy policy option
									reference.reference.unbind(reference, componentInstance, oldBoundService);
								}
								break;
							}
						}
						break;
					case ComponentReference.CARDINALITY_1_N :
					case ComponentReference.CARDINALITY_0_N :
						// bind to all services
						for (int i = 0; i < serviceReferences.length; i++) {
							reference.reference.bind(reference, componentInstance, serviceReferences[i]);
						}
						break;
				}
			} else if (reference.reference.policy == ComponentReference.POLICY_STATIC) {
				//in case there is no bind method for static reference save the current matching service references
				reference.setBoundServiceReferences(serviceReferences);
			}
		} else {
			if (Activator.DEBUG) {
				Activator.log.debug("ServiceComponentProp.bindReference(): The service is not yet registered, but it is already instantiated", null); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Handles the update of the bound services in case the target properties have changed
	 * @param oldProps the old component properties
	 * @param newProps the new component properties
	 */
	private void handleBoundServicesUpdate(Hashtable oldProps, Dictionary newProps) {
		Enumeration keys = oldProps.keys();
		Vector checkedFilters = new Vector();
		//check for changed target filters in the properties
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			if (key.endsWith(".target")) { //$NON-NLS-1$
				checkedFilters.addElement(key);
				String newFilter = (String) newProps.get(key);
				Reference reference = null;
				String refName = key.substring(0, key.length() - ".target".length()); //$NON-NLS-1$
				for (int i = 0; i < references.size(); i++) {
					reference = (Reference) references.elementAt(i);
					if (reference.reference.name.equals(refName)) {
						break;
					}
					reference = null;
				}
				//check if there is a reference corresponding to the target property
				if (reference != null && reference.reference.policy == ComponentReference.POLICY_DYNAMIC) {
					if (newFilter != null) {
						if (!newFilter.equals(oldProps.get(key))) {
							//the filter differs the old one - update the reference bound services
							processReferenceBoundServices(reference, newFilter);
						}
					} else {
						//the target filter is removed. using the default one 
						processReferenceBoundServices(reference, "(objectClass=" + reference.reference.interfaceName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
		}
		//now check for newly added target filters
		keys = newProps.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			if (key.endsWith(".target") && !checkedFilters.contains(key)) { //$NON-NLS-1$
				Reference reference = null;
				String refName = key.substring(0, key.length() - ".target".length()); //$NON-NLS-1$
				for (int i = 0; i < references.size(); i++) {
					reference = (Reference) references.elementAt(i);
					if (reference.reference.name.equals(refName)) {
						break;
					}
					reference = null;
				}
				//check if there is a reference corresponding to the target property
				if (reference != null && reference.reference.policy == ComponentReference.POLICY_DYNAMIC) {
					processReferenceBoundServices(reference, (String) newProps.get(key));
				}
			}
		}
	}

	private void processReferenceBoundServices(Reference reference, String newTargetFilter) {
		reference.setTarget(newTargetFilter);
		ServiceReference[] refs = null;
		try {
			refs = bc.getServiceReferences(reference.reference.interfaceName, newTargetFilter);
		} catch (InvalidSyntaxException e) {
			Activator.log(bc, LogService.LOG_WARNING, "[SCR] " + NLS.bind(Messages.INVALID_TARGET_FILTER, newTargetFilter), e); //$NON-NLS-1$
			return;
		}

		if (refs == null) {
			//must remove all currently bound services
			if (reference.reference.bind != null) {
				if (reference.reference.serviceReferences.size() > 0) {
					for (int i = 0; i < instances.size(); i++) {
						ComponentInstance componentInstance = (ComponentInstance) instances.elementAt(i);
						unbindReference(reference, componentInstance);
					}
				}
			}
		} else {
			//find out which services has to be bound and which unbound
			if (reference.reference.bind != null) {
				Vector servicesToUnbind = new Vector();
				Enumeration keys = reference.reference.serviceReferences.keys();
				while (keys.hasMoreElements()) {
					Object serviceRef = keys.nextElement();
					boolean found = false;
					for (int i = 0; i < refs.length; i++) {
						if (refs[i] == serviceRef) {
							found = true;
							break;
						}
					}
					if (!found) {
						//the bound service reference is not already in the satisfied references set.
						servicesToUnbind.addElement(serviceRef);
					}
				}
				if ((reference.reference.cardinality == ComponentReference.CARDINALITY_0_N || reference.reference.cardinality == ComponentReference.CARDINALITY_1_N) && (reference.reference.serviceReferences.size() - servicesToUnbind.size()) < refs.length) {
					//there are more services to bind
					for (int i = 0; i < refs.length; i++) {
						keys = reference.reference.serviceReferences.keys();
						boolean found = false;
						while (keys.hasMoreElements()) {
							Object serviceRef = keys.nextElement();
							if (serviceRef == refs[i]) {
								found = true;
								break;
							}
						}
						if (!found) {
							for (int j = 0; j < instances.size(); j++) {
								ComponentInstance componentInstance = (ComponentInstance) instances.elementAt(j);
								try {
									reference.reference.bind(reference, componentInstance, refs[i]);
								} catch (Exception e) {
									Activator.log(null, LogService.LOG_ERROR, NLS.bind(Messages.ERROR_BINDING_REFERENCE, reference), e);
								}
							}
						}
					}
				}
				//finally unbind all services that do not match the target filter
				for (int i = 0; i < servicesToUnbind.size(); i++) {
					for (int j = 0; j < instances.size(); j++) {
						ComponentInstance componentInstance = (ComponentInstance) instances.elementAt(j);
						try {
							unbindDynamicReference(reference, componentInstance, (ServiceReference) servicesToUnbind.elementAt(i));
						} catch (Exception e) {
							Activator.log(null, LogService.LOG_ERROR, NLS.bind(Messages.ERROR_UNBINDING_REFERENCE2, reference), e);
						}
					}
				}
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
			Activator.log.debug("ServiceComponentProp.unbindReference(): component " + serviceComponent.name + " <- " + reference.reference, null); //$NON-NLS-1$ //$NON-NLS-2$
		}

		Enumeration serviceReferences = reference.reference.serviceReferences.keys();
		while (serviceReferences.hasMoreElements()) {
			reference.reference.unbind(reference, componentInstance, (ServiceReference) serviceReferences.nextElement());
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
		if (Activator.DEBUG) {
			Activator.log.debug("ServiceComponentProp.unbindDynamicReference(): component = " + name + ", reference = " + ref.reference.name, null); //$NON-NLS-1$ //$NON-NLS-2$
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
	}

	/**
	 * Call the updated method for this Reference
	 * 
	 * @param ref the reference
	 * @param instance the component instance
	 * @param serviceReference the service reference which properties have changed
	 */
	public void updatedReference(Reference ref, ComponentInstance instance, ServiceReference serviceReference) {
		if (Activator.DEBUG) {
			Activator.log.debug("ServiceComponentProp.updatedReference(): component = " + name + ", reference = " + ref.reference.name, null); //$NON-NLS-1$ //$NON-NLS-2$
		}
		ref.reference.updated(ref, instance, serviceReference);
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
	 * @param componentId specifies the component ID. If null, a new one will be generated
	 * @return the fully initialized properties
	 */
	private Hashtable initProperties(Dictionary configProperties, Long componentId) {
		Hashtable result = null;
		// default component service properties
		Properties propertyDescriptions = serviceComponent.properties;
		if (propertyDescriptions != null && !propertyDescriptions.isEmpty()) {
			result = (Hashtable) propertyDescriptions.clone();
		}

		// set the component.name
		if (result == null) {
			result = new Hashtable(7);
		}

		// put the references in the properties
		if (serviceComponent.references != null) {
			ComponentReference ref;
			for (int i = 0; i < serviceComponent.references.size(); i++) {
				ref = (ComponentReference) serviceComponent.references.elementAt(i);
				if (ref.target != null) {
					result.put(ref.name + ComponentConstants.REFERENCE_TARGET_SUFFIX, ref.target);
				}
			}
		}

		// properties from Configuration Admin
		if (configProperties != null && !configProperties.isEmpty()) {
			for (Enumeration keys = configProperties.keys(); keys.hasMoreElements();) {
				Object key = keys.nextElement();
				Object val = configProperties.get(key);
				result.put(key, val);
			}
		}

		// always set the component name & the id
		Long nextId = (componentId == null) ? new Long(getNewComponentID()) : componentId;
		result.put(ComponentConstants.COMPONENT_ID, nextId);
		result.put(ComponentConstants.COMPONENT_NAME, serviceComponent.name);

		if (serviceComponent.provides != null) {
			String[] provides = new String[serviceComponent.provides.length];
			System.arraycopy(serviceComponent.provides, 0, provides, 0, provides.length);
			result.put(Constants.OBJECTCLASS, provides);
		}
		return result;
	}

	private void assertCreateSingleInstance() {
		if (!serviceComponent.serviceFactory && !instances.isEmpty()) {
			throw new ComponentException(NLS.bind(Messages.INSTANCE_ALREADY_CREATED, name));
		}
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("ServiceComponentProp["); //$NON-NLS-1$
		buffer.append("\n\tname = ").append(name); //$NON-NLS-1$
		buffer.append("\n\tstate = ").append(SCRUtil.getStateStringRepresentation(state)); //$NON-NLS-1$
		StringBuffer buf = new StringBuffer(200);
		if (properties != null) {
			buf.append('{');
			Enumeration keys = properties.keys();
			while (keys.hasMoreElements()) {
				Object key = keys.nextElement();
				buf.append(key).append('=').append(SCRUtil.getStringRepresentation(properties.get(key)));
				if (keys.hasMoreElements()) {
					buf.append(", "); //$NON-NLS-1$
				}
			}
			buf.append('}');
		}
		buffer.append("\n\tproperties = ").append(buf.toString()); //$NON-NLS-1$
		buffer.append("]"); //$NON-NLS-1$
		return buffer.toString();
	}

	public void setRegistration(ServiceRegistration reg) {
		registration = reg;
	}

	/**
	 * Removes the private properties and returns the public properties that are set to the registered service provided by this component
	 * @return the public properties used in the registration of the service which is provided by this component
	 */
	public Hashtable getPublicServiceProperties() {
		//remove the private properties from the component properties before registering as service
		Hashtable publicProps = (Hashtable) properties.clone();
		Enumeration keys = properties.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			if (key.startsWith(".")) { //$NON-NLS-1$
				publicProps.remove(key);
			}
		}
		return publicProps;
	}

	/**
	 * Add a new Component Configuration name we should not activate in order to
	 * prevent a cycle.
	 * 
	 * @see ServiceComponentProp#delayActivateSCPNames
	 */
	public void setDelayActivateSCPName(String scpName) {
		if (Activator.DEBUG) {
			Activator.log.debug("Setting delay activate SCP: " + scpName, null); //$NON-NLS-1$
		}
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
		return serviceComponent.serviceFactory;
	}

	public synchronized int getState() {
		return state;
	}

	public synchronized void setState(int state) {
		this.state = state;
	}

	public void disable() {
		if (getState() == STATE_DISPOSED) {
			throw new IllegalStateException(Messages.COMPONENT_DISPOSED);
		} else if (getState() != STATE_DISABLED) {
			mgr.disableComponent(serviceComponent.name, serviceComponent.bundle);
		}
	}

	public void enable() {
		if (getState() == STATE_DISPOSED) {
			throw new IllegalStateException(Messages.COMPONENT_DISPOSED);
		} else if (getState() == STATE_DISABLED) {
			mgr.enableComponent(serviceComponent.name, serviceComponent.bundle);
		}
	}

	public String getActivate() {
		return serviceComponent.activateMethodName;
	}

	public Bundle getBundle() {
		return serviceComponent.bundle;
	}

	public String getClassName() {
		return serviceComponent.implementation;
	}

	public ComponentInstance getComponentInstance() {
		if (!instances.isEmpty()) {
			//TODO we have multiple instances when the component is service factory
			//Perhaps we have to list in ScrService Component for each instance
			return (ComponentInstance) instances.firstElement();
		}
		return null;
	}

	public String getConfigurationPolicy() {
		return serviceComponent.configurationPolicy;
	}

	public String getDeactivate() {
		return serviceComponent.deactivateMethodName;
	}

	public String getFactory() {
		return serviceComponent.factory;
	}

	public long getId() {
		return ((Long) properties.get(ComponentConstants.COMPONENT_ID)).longValue();
	}

	public String getModified() {
		if (!serviceComponent.isNamespaceAtLeast11()) {
			return null;
		}
		return serviceComponent.modifyMethodName;
	}

	public String getName() {
		return serviceComponent.name;
	}

	public org.apache.felix.scr.Reference[] getReferences() {
		if (references != null && !references.isEmpty()) {
			org.apache.felix.scr.Reference[] res = new org.apache.felix.scr.Reference[references.size()];
			references.copyInto(res);
			return res;
		}
		return null;
	}

	public String[] getServices() {
		return serviceComponent.provides;
	}

	public boolean isActivateDeclared() {
		if (!serviceComponent.isNamespaceAtLeast11()) {
			return false;
		}
		return serviceComponent.activateMethodDeclared;
	}

	public boolean isDeactivateDeclared() {
		if (!serviceComponent.isNamespaceAtLeast11()) {
			return false;
		}
		return serviceComponent.deactivateMethodDeclared;
	}

	public boolean isDefaultEnabled() {
		return serviceComponent.autoenable;
	}

	public boolean isImmediate() {
		return serviceComponent.isImmediate();
	}

	public boolean isServiceFactory() {
		return serviceComponent.serviceFactory;
	}

	//Some helper methods according to the component's state
	public boolean isBuilt() {
		return state == STATE_ACTIVATING || state == STATE_ACTIVE || state == STATE_FACTORY || state == STATE_REGISTERED;
	}

	public boolean isUnsatisfied() {
		return state == STATE_UNSATISFIED || state == STATE_DEACTIVATING || state == STATE_DISABLED || state == STATE_DISPOSING || state == STATE_DISPOSED;
	}

}
