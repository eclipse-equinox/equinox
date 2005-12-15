/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.instance;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import org.eclipse.equinox.ds.Activator;
import org.eclipse.equinox.ds.Log;
import org.eclipse.equinox.ds.model.*;
import org.eclipse.equinox.ds.resolver.Reference;
import org.eclipse.equinox.ds.resolver.Resolver;
import org.eclipse.equinox.ds.service.ComponentContextImpl;
import org.eclipse.equinox.ds.service.ComponentInstanceImpl;
import org.osgi.framework.*;
import org.osgi.service.component.*;

/**
 * This class provides the following function
 * 
 * Build of a component instance - includes activate, bind, instantiate. Dispose
 * of a component instance - includes deactivate, unbind, unreference.
 * 
 */
public class BuildDispose {

	private InvokeMethod invoke;

	/**
	 * Counts re-entry in to the
	 * {@link BuildDispose#buildComponentConfigInstance(Bundle, ComponentDescriptionProp)} method. This
	 * is used to handle circular dependencies.
	 */
	private int stackCount;

	/**
	 * Used with stackCount to handle circular dependencies in the
	 * {@link BuildDispose#buildComponentConfigInstance(Bundle, ComponentDescriptionProp)} method.
	 */
	private List delayedBindList;

	/** Main SCR class */
	private Activator main;

	/**
	 * @param main Main SCR class
	 */
	BuildDispose(Activator main) {
		invoke = new InvokeMethod(this);
		this.main = main;
		stackCount = 0;
		delayedBindList = new ArrayList();
	}

	/**
	 * dispose - cleanup since the SCR is shutting down
	 */
	void dispose() {
		synchronized (this) {
			invoke = null;
			main = null;
		}
	}

	/**
	 * Create an instance of a Component Configuration ({@link ComponentDescriptionProp}).
	 * Instantiate the Service Component implementation class and create a
	 * {@link ComponentInstanceImpl} to track it. Create a
	 * {@link ComponentContext},
	 * {@link BuildDispose#bind(ComponentDescriptionProp, ComponentInstanceImpl) bind}
	 * the Service Component's references and call the implementation class'
	 * "activate" method.
	 * 
	 * This method also handles possible circluarities. The method is re-entrant
	 * and tracks how many times we have entered the method. If a possible
	 * circularity is encountered while binding references (it is detected
	 * earlier by the resolver) then the bind action is delayed until the last
	 * time we exit this method to "break" the cirularity. See the OSGi R4
	 * specification section 112.3.5 for more information about Cirular
	 * References.
	 * 
	 * @param usingBundle - if this Component Instance is being created to
	 *        return from a BundleContext.getService() call, this is the bundle
	 *        which is "using" this instance. Otherwise null.
	 * 
	 * @param cdp - Component Configuration to create an instance of
	 * 
	 * @throws ComponentException
	 * 
	 * @return ComponentInstance
	 * 
	 */

	public ComponentInstance buildComponentConfigInstance(Bundle usingBundle, ComponentDescriptionProp cdp) throws ComponentException {

		synchronized (this) {

			// keep track of how many times we have re-entered this method
			stackCount++;

			ComponentInstanceImpl componentInstance = null;

			try {
				ComponentDescription componentDescription = cdp.getComponentDescription();

				Object instance = createInstance(componentDescription);

				componentInstance = instantiate(cdp, instance);

				createComponentContext(usingBundle, componentInstance);

				bind(componentInstance);

				activate(componentInstance);

				cdp.addInstance(componentInstance);

			} catch (ComponentException e) {
				Log.log(1, "[SCR] Error attempting to build component ", e);
				throw e;

			} finally {

				// keep track of how many times we have re-entered this method
				stackCount--;

			}

			// if this is the last time in this method and we have "delayed"
			// bind
			// actions to do (there was a circularity during bind)
			if (stackCount == 0 && !delayedBindList.isEmpty()) {
				// put delayed dynamic binds on the queue.
				// (this is used to handle circularity)
				main.resolver.instanceProcess.workQueue.enqueueWork(main.resolver, Resolver.DYNAMICBIND, ((ArrayList) delayedBindList).clone());
				delayedBindList.clear();
			}

			return componentInstance;
		} // end synchronized(this)
	}

	/**
	 * Dispose of a Component Configuration.
	 * 
	 * Unregister this Component Configuration's service, if it has one.
	 * 
	 * Call
	 * {@link BuildDispose#disposeComponentInstance(ComponentDescriptionProp, ComponentInstance)}
	 * for each instance of this Component Configuration. * Dispose of the
	 * {@link ComponentDescriptionProp} object.
	 * 
	 * Synchronously dispose of this Component Configuration. This method will
	 * not return until all instances of this Component Configuration are
	 * disposed.
	 * 
	 * @param cdp Component Configuration to dispose
	 */
	void disposeComponentConfig(ComponentDescriptionProp cdp) {

		synchronized (this) {
			// unregister this cdp's service
			ServiceRegistration serviceRegistration = cdp.getServiceRegistration();
			if (serviceRegistration != null) {
				try {
					serviceRegistration.unregister();
				} catch (IllegalStateException e) {
					// service has already been unregistered, no problem
				}
			}

			// get all instances for this component
			Iterator it = cdp.getInstances().iterator();
			while (it.hasNext()) {
				disposeComponentInstance((ComponentInstanceImpl) it.next());
			}
			cdp.removeAllInstances();

			// clean up this CDP object
			it = cdp.getReferences().iterator();
			while (it.hasNext()) {
				Reference reference = (Reference) it.next();

				reference.clearServiceReferences();
			}
			cdp.clearDelayActivateCDPNames();
		}
	}

	/**
	 * Dispose of the Component Instance.
	 * 
	 * Deactivate the instance, call unbind methods, and release any services it
	 * was using.
	 * 
	 * Synchronously disposes of a Component Configuration instance. This method
	 * does not return until the instance has been deactivated and unbound, and
	 * any services used by the instance have been released.
	 * 
	 * @param ci instance of cdp to dispose
	 */
	public void disposeComponentInstance(ComponentInstance ci) {

		synchronized (this) {
			ComponentInstanceImpl componentInstance = (ComponentInstanceImpl) ci;
			deactivate(componentInstance);
			unbind(componentInstance);

			// if this was the last instance of the CDP, make sure we unget
			// any lingering references, just in case
			ComponentDescriptionProp cdp = componentInstance.getComponentDescriptionProp();
			if (cdp.getInstances().isEmpty()) {
				BundleContext context = cdp.getComponentDescription().getBundleContext();
				Iterator referenceItr = cdp.getReferences().iterator();
				while (referenceItr.hasNext()) {
					Reference reference = (Reference) referenceItr.next();

					Iterator serviceReferenceItr = reference.getServiceReferences().iterator();
					while (serviceReferenceItr.hasNext()) {
						context.ungetService((ServiceReference) serviceReferenceItr.next());

					}
				}
			}

			componentInstance = null;
		}
	}

	/**
	 * Instantiate an instance of the implementation class of this Service
	 * Component
	 * 
	 * @param cd Service Component
	 * 
	 * @return an instance of the implementation class of cd
	 * 
	 * @throws ComponentException and logs an error if there is an exception
	 *         loading the class or instantiating the instance
	 * 
	 */

	private Object createInstance(ComponentDescription cd) throws ComponentException {
		Object instance = null;
		String classname = cd.getImplementation().getClassname();
		try {
			instance = cd.getBundleContext().getBundle().loadClass(classname).newInstance();
		} catch (IllegalAccessException e) {
			Log.log(1, "[SCR] IllegalAccessException attempting to create instance ", e);
			throw new ComponentException(e.getMessage());
		} catch (ClassNotFoundException e) {
			Log.log(1, "[SCR] ClassNotFoundException attempting to create instance. ", e);
			throw new ComponentException(e.getMessage());
		} catch (InstantiationException e) {
			Log.log(1, "[SCR] InstantiationException attempting to create instance. ", e);
			throw new ComponentException(e.getMessage());
		}

		return instance;
	}

	/**
	 * Create a ComponentInstanceImpl object to track an instance of a Component
	 * Configuration's implementation class.
	 * 
	 * @param cdp Component Configuration
	 * 
	 * @param instance instance of Component Configuration's implementation
	 *        class to track
	 * 
	 * @return ComponentInstanceImpl
	 * 
	 */
	private ComponentInstanceImpl instantiate(ComponentDescriptionProp cdp, Object instance) {
		return new ComponentInstanceImpl(main, cdp, instance);
	}

	/**
	 * Acquire and bind services for this Component Configuration instance.
	 * 
	 * Calls
	 * {@link BuildDispose#bindReference(Reference, ComponentInstanceImpl)} for
	 * each reference in the Component Configuration that has a bind method.
	 * 
	 * @param componentInstance instance to bind
	 * 
	 */
	private void bind(ComponentInstanceImpl componentInstance) {
		// Get all the required service Reference Descriptions for this Service
		// Component
		// bind them in order
		List references = componentInstance.getComponentDescriptionProp().getReferences();
		// call the Bind method if the Reference Description includes one
		Iterator itr = references.iterator();
		while (itr.hasNext()) {
			Reference reference = (Reference) itr.next();
			if (reference.getReferenceDescription().getBind() != null) {
				bindReference(reference, componentInstance);
			}
		}
	}

	/**
	 * Try to acquire a service and call the bind method on a Component
	 * Configuration instance for a reference.
	 * 
	 * If the reference's cardinality is 0..n or 1..n, bind will be called for
	 * each registered service which satisfies the reference. If the cardinality
	 * is 0..1 or 1..1, bind will be called with the service that has the
	 * highest service.ranking and service.id
	 * 
	 * If an exception occurs during bind, we log an error and continue.
	 * 
	 * @param reference Reference to bind
	 * 
	 * @param componentInstance Component Configuration instance to call bind
	 *        methods on.
	 * 
	 */
	void bindReference(Reference reference, ComponentInstanceImpl componentInstance) {
		synchronized (this) {
			ServiceReference[] serviceReferences = null;
			// if there is a published service, then get the ServiceObject and
			// call bind
			try {
				// get All Registered services using this target filter
				serviceReferences = componentInstance.getComponentContext().getBundleContext().getServiceReferences(reference.getReferenceDescription().getInterfacename(), reference.getTarget());

				// If a bind method throws an exception, SCR must log an error
				// message containing the exception with the Log Service but
				// the activation of the component configuration does not fail.
			} catch (InvalidSyntaxException e) {
				Log.log(1, "[SCR] InvalidSyntaxException attempting to bindReference ", e);
			}

			// if cardinality is 0..1 or 0..n, it is OK if there is nothing to
			// bind with
			if (!reference.getReferenceDescription().isRequired() && serviceReferences == null) {
				// that's OK
				return;
			}

			// sort by service ranking and service id
			Arrays.sort(serviceReferences);

			// we only want to bind one service
			if (reference.getReferenceDescription().getCardinalityHigh() == 1) {
				bindServiceToReference(reference, serviceReferences[0], componentInstance);
				// here we can bind more than one service, if availible
			} else {
				for (int j = 0; j < serviceReferences.length; j++) {
					bindServiceToReference(reference, serviceReferences[j], componentInstance);
				}
			}
		} // end synchronized(this)

	}

	/**
	 * Bind a Component Configuration instance to a particular
	 * {@link ServiceReference}.
	 * 
	 * @param reference
	 * 
	 * @param serviceReference
	 * 
	 * @param componentInstance
	 * 
	 */
	private void bindServiceToReference(Reference reference, ServiceReference serviceReference, ComponentInstanceImpl componentInstance) {
		ReferenceDescription rd = reference.getReferenceDescription();
		// make sure we have not already bound this object
		if (!reference.bindedToServiceReference(serviceReference) && rd.getBind() != null) {

			try {
				//only look up bind method once
				Method method = rd.getBindMethod();
				if (method == null) {
					method = invoke.findBindOrUnbindMethod(componentInstance, reference, serviceReference, rd.getBind());
					rd.setBindMethod(method);
				}
				if (method == null) {
					// could be circularity break
					return;
				}
				Object param = null;
				if (method.getParameterTypes()[0].equals(ServiceReference.class)) {
					param = serviceReference;
				} else {
					// componentInstance.getServiceObject(...) is populated by
					// the findBindOrUnbindMethod function
					param = reference.getServiceObject(serviceReference);

					// ... but not if the bind method was cached
					if (param == null) {
						param = getService(reference, serviceReference);
						if (param == null) {
							// could be circularity break
							return;
						}
						reference.addServiceReference(serviceReference, param);
					}
				}

				invoke.bindComponent(method, componentInstance.getInstance(), param);
				// If a bind method throws an exception, SCR must log an error
				// message containing
				// the exception with the Log Service but the activation of the
				// component configuration does not fail.
			} catch (IllegalAccessException e) {
				Log.log(1, "[SCR] IllegalAccessException attempting to bind Service to Reference ", e);
			} catch (InvocationTargetException e) {
				Log.log(1, "[SCR] InvocationTargetException attempting to bind Service to Reference ", e);

			}

			// track this serviceReference so we know to unbind it later
			reference.addServiceReference(serviceReference);
		}
	}

	/**
	 * Acquire a service object from a {@link ServiceReference}.
	 * 
	 * This method checks if "getting" the service could cause a cycle. If so,
	 * it breaks the cycle and returns null.
	 * 
	 * @param consumerCDP Component Configuration which wants to acquire the
	 *        service
	 * @param reference
	 * @param serviceReference
	 * 
	 * @return the service object or null if it would cause a circularity
	 */
	public Object getService(Reference reference, ServiceReference serviceReference) {
		// check if getting this service would cause a circularity
		if (couldCauseCycle(reference, serviceReference)) {
			return null;
		}

		// getting this service will not cause a circularity
		return reference.getComponentDescriptionProp().getComponentDescription().getBundleContext().getService(serviceReference);

	}

	/**
	 * Check the "cycle list" put in the cdp by the resolver to see if getting
	 * this reference would cause a circularity.
	 * 
	 * A circularity is only possible if the "producer" of the service is also a
	 * service component.
	 * 
	 * If getting the service could cause a circularity and the reference's
	 * policy is "dynamic", add an entry to the "delayed bind table" which is
	 * processed farther up the call stack by the
	 * {@link BuildDispose#buildComponentConfigInstance(Bundle, ComponentDescriptionProp)} method.
	 * 
	 * @param consumerCDP
	 * @param reference
	 * @param serviceReference
	 * @return if getting the service could cause a circularity
	 */
	private boolean couldCauseCycle(Reference reference, ServiceReference serviceReference) {

		ComponentDescriptionProp consumerCDP = reference.getComponentDescriptionProp();
		// if we are not building a component, no cycles possible
		if (stackCount == 0) {
			return false;
		}

		String producerComponentName = (String) serviceReference.getProperty(ComponentConstants.COMPONENT_NAME);

		// if producer is not a service component, no cycles possible
		if (producerComponentName == null) {
			return false;
		}

		// check if producer is on our "do not activate" list
		if (!consumerCDP.getDelayActivateCDPNames().contains(producerComponentName)) {
			return false;
		}

		// find producer cdp
		ComponentDescriptionProp producerCDP = null;
		Iterator it = main.resolver.satisfiedCDPs.iterator();
		while (it.hasNext()) {
			ComponentDescriptionProp cdp = (ComponentDescriptionProp) it.next();
			if (producerComponentName.equals(cdp.getComponentDescription().getName())) {
				// found the producer cdp
				producerCDP = cdp;
				break;
			}
		}

		if (producerCDP.getComponentDescription().getService().isServicefactory()) {
			// producer is a service factory - there is a new instance for every
			// bundle, so see if one of the instances is used by this bundle
			it = producerCDP.getInstances().iterator();
			Bundle bundle = consumerCDP.getComponentDescription().getBundleContext().getBundle();
			while (it.hasNext()) {
				ComponentInstanceImpl producerComponentInstance = (ComponentInstanceImpl) it.next();
				if (producerComponentInstance.getComponentContext().getUsingBundle().equals(bundle)) {
					// a producer already exists, so no cycle possible
					return false;
				}
			}
		} else {
			// producer is not a service factory - there will only ever be one
			// instance - if it exists then no cycle possible
			if (!producerCDP.getInstances().isEmpty()) {
				return false;
			}
		}

		// producer cdp is not active - do not activate it because that could
		// cause circularity

		// if reference has bind method and policy=dynamic, activate later and
		// bind
		if (reference.getReferenceDescription().getBind() != null && reference.getReferenceDescription().getPolicy().equalsIgnoreCase("dynamic")) {
			// delay bind by putting on the queue later
			delayedBindList.add(reference);
		}

		// can't get service now because of circularity - we will bind later
		// (dynamically) if the reference had a bind method and was dynamic
		return true;
	}

	/**
	 * Call the unbind method for each of the Referenced Services in a Component
	 * Configuration instance
	 * 
	 * @param componentInstance
	 */
	private void unbind(ComponentInstanceImpl componentInstance) {
		// Get all the required service reference descriptions for this Service
		// Component
		List references = componentInstance.getComponentDescriptionProp().getReferences();
		// call the unBind method if the Reference Description includes one
		// unbind in reverse order
		ListIterator itr = references.listIterator(references.size());
		while (itr.hasPrevious()) {
			unbindReference((Reference) itr.previous(), componentInstance);
		}
	}

	/**
	 * Call the unbind method for each {@link ServiceReference} bound to a
	 * Reference for a Component Configuration instance
	 * 
	 * @param reference
	 * @param componentInstance
	 */
	private void unbindReference(Reference reference, ComponentInstanceImpl componentInstance) {
		Iterator itr = reference.getServiceReferences().iterator();
		while (itr.hasNext()) {
			ServiceReference serviceReference = (ServiceReference) itr.next();
			unbindServiceFromReference(reference, componentInstance, serviceReference);
		}
	}

	/**
	 * Dynamically unbind a reference from a Component Configuration instance.
	 * 
	 * First try to re-bind to a new reference as described in the OSGi R4
	 * specification section 112.5.10 "Bound Service Replacement"
	 * 
	 * @param reference
	 * @param componentInstance
	 * @param serviceReference
	 */
	void unbindDynamicReference(Reference reference, ComponentInstanceImpl componentInstance, ServiceReference serviceReference) {
		synchronized (this) {
			// rebind if we can
			bindReference(reference, componentInstance);

			unbindServiceFromReference(reference, componentInstance, serviceReference);

		}

	}

	/**
	 * Unbind a {@link ServiceReference} from a Component Configuration
	 * instance.
	 * 
	 * Find the unbind method and call it if a method exists. If the
	 * {@link ServiceReference} was acquired, call
	 * {@link BundleContext#ungetService(org.osgi.framework.ServiceReference)}
	 * using the Service Component's bundle context.
	 * 
	 * @param reference
	 * @param componentInstance
	 * @param serviceReference
	 */
	private void unbindServiceFromReference(Reference reference, ComponentInstanceImpl componentInstance, ServiceReference serviceReference) {
		ReferenceDescription rd = reference.getReferenceDescription();
		String unbind = rd.getUnbind();
		Object serviceObject = null;
		Method method = null;
		if (unbind != null) {
			//only look up unbind method once
			method = rd.getUnbindMethod();
			if (method == null) {
				method = invoke.findBindOrUnbindMethod(componentInstance, reference, serviceReference, unbind);
				rd.setUnbindMethod(method);
			}
		}
		if (method != null) {
			Object param = null;
			if (method.getParameterTypes()[0].equals(ServiceReference.class)) {
				param = serviceReference;
			} else {
				// componentInstance.getServiceObject(...) is populated by
				// the findBindOrUnbindMethod function
				serviceObject = reference.getServiceObject(serviceReference);

				// ... but not if the bind method was cached
				if (serviceObject == null) {
					serviceObject = getService(reference, serviceReference);
					if (serviceObject == null) {
						// could be circularity break
						return;
					}
					reference.addServiceReference(serviceReference, serviceObject);
				}
				param = serviceObject;
			}
			// If an unbind method throws an exception, SCR must log an error
			// message
			// containing the exception with the Log Service and the
			// deactivation of the component configuration will continue.
			try {
				invoke.unbindComponent(method, componentInstance.getInstance(), param);
			} catch (InvocationTargetException e) {
				Log.log(1, "[SCR] InvocationTargetException attempting to unbind reference.", e);
			} catch (IllegalAccessException e) {
				Log.log(1, "[SCR] IllegalAccessException attempting to unbind reference.", e);
			}
		} // end if (method != null)

		// release service object
		if (serviceObject != null) {
			componentInstance.getComponentContext().getBundleContext().ungetService(serviceReference);
		}
		reference.removeServiceReference(serviceReference);
	}

	/**
	 * Invoke the activate method on a Component Configuration instance, if it
	 * exists.
	 * 
	 * @param componentInstance
	 * @throws ComponentException
	 */
	private void activate(ComponentInstanceImpl componentInstance) throws ComponentException {

		// If the activate method throws an exception, SCR must log an error
		// message
		// containing the exception with the Log Service and the component
		// configuration is not activated.
		try {
			invoke.activateComponent(componentInstance.getInstance(), componentInstance.getComponentContext());
		} catch (IllegalAccessException e) {
			Log.log(1, "[SCR] IllegalAccessException attempting to activate component ", e);
			throw new ComponentException(e.getMessage());
		} catch (InvocationTargetException e) {
			Log.log(1, "[SCR] InvocationTargetException attempting to activate component ", e);
			throw new ComponentException(e.getMessage());
		}
	}

	/**
	 * Invoke the deactivate method on a Component Configuration instance, if it
	 * exists.
	 * 
	 * @param componentInstance
	 */
	private void deactivate(ComponentInstanceImpl componentInstance) {

		// If the deactivate method throws an exception, SCR must log an error
		// message
		// containing the exception with the Log Service and the component
		// configuration will continue.
		try {
			invoke.deactivateComponent(componentInstance.getInstance(), componentInstance.getComponentContext());
		} catch (InvocationTargetException e) {
			Log.log(1, "[SCR] InvocationTargetException attempting to deactivate component.", e);
		} catch (IllegalAccessException e) {
			Log.log(1, "[SCR] IllegalAccessException attempting to deactivate component. ", e);
		}

	}

	/**
	 * Create and return a new ComponentContext for a Component Configuration
	 * instance
	 * 
	 * @param usingBundle
	 * @param componentInstance
	 * 
	 */
	private void createComponentContext(Bundle usingBundle, ComponentInstanceImpl componentInstance) {
		ComponentContext context = new ComponentContextImpl(main, usingBundle, componentInstance);
		componentInstance.setComponentContext(context);
	}

}
