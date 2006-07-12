/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.resolver;

import java.io.IOException;
import java.util.*;
import org.eclipse.equinox.ds.Activator;
import org.eclipse.equinox.ds.Log;
import org.eclipse.equinox.ds.instance.InstanceProcess;
import org.eclipse.equinox.ds.model.*;
import org.eclipse.equinox.ds.workqueue.WorkDispatcher;
import org.eclipse.equinox.ds.workqueue.WorkQueue;
import org.osgi.framework.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentException;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Resolver - resolves the Service Components.  This includes creating Component 
 * Configurations, resolving the required referenced services, and checking for 
 * circular dependencies.
 * 
 * The Resolver implements AllServiceListener so it can be informed about service
 * changes in the framework.
 * 
 * @version $Revision: 1.3 $
 */
public class Resolver implements AllServiceListener, WorkDispatcher {

	/* set this to true to compile in debug messages */
	private static final boolean DEBUG = false;

	/** 
	 * next free component id.
	 * See OSGi R4 Specification section 112.6 "Component Properties"
	 */
	private static long componentid;

	/* ServiceTracker for configurationAdmin */
	public ServiceTracker configAdminTracker;

	/**
	 * Service Component instances need to be built.
	 */
	private static final int BUILD = 1;

	/**
	 * Service Component instances to bind dynamically
	 */
	public static final int DYNAMICBIND = 3;

	/**
	 * Main class for the SCR
	 */
	private Activator main;

	public InstanceProcess instanceProcess;

	/**
	 * List of {@link ComponentConfiguration}s - the currently "enabled" 
	 * Component Configurations.
	 */
	public List enabledComponentConfigurations;

	/**
	 * List of {@link ComponentConfiguration}s - the currently "satisfied" 
	 * Component Configurations.  Note that to be satisfied a Component 
	 * Configuration must first be enabled, so this list is a subset of 
	 * {@link Resolver#enabledComponentConfigurations enabledComponentConfigurations}.
	 */
	public List satisfiedComponentConfigurations;

	/**
	 * A map of name:Service Component (String):({@link ComponentDescription})
	 */
	public Map enabledCDsByName;

	private WorkQueue workQueue;

	/**
	 * Resolver constructor
	 * 
	 * @param main Main class of SCR
	 */
	public Resolver(Activator main) {
		this.main = main;

		componentid = 1;

		// for now use Main's workqueue
		workQueue = main.workQueue;

		enabledComponentConfigurations = new ArrayList();
		satisfiedComponentConfigurations = new ArrayList();
		enabledCDsByName = new HashMap();

		configAdminTracker = new ServiceTracker(main.context, ConfigurationAdmin.class.getName(), null);
		configAdminTracker.open();

		instanceProcess = new InstanceProcess(main);

		//start listening to ServiceChanged events
		main.context.addServiceListener(this);

	}

	/**
	 * Clean up the SCR is shutting down
	 */
	public void dispose() {

		//stop listening to ServiceChanged events
		main.context.removeServiceListener(this);

		instanceProcess.dispose();
		instanceProcess = null;

		configAdminTracker.close();
		configAdminTracker = null;

		enabledComponentConfigurations = null;
		satisfiedComponentConfigurations = null;
		enabledCDsByName = null;

	}

	/**
	 * Enable Service Components - create Component Configuration(s) for the 
	 * Service Components and try to satisfy their dependencies.
	 * 
	 * <p>
	 * For each Service Component ({@link ComponentDescription}) check 
	 * ConfigurationAdmin for properties and create a Component Configuration 
	 * ({@link ComponentConfiguration}).
	 * </p>
	 * 
	 * <p>
	 * If a {@link org.osgi.service.cm.ManagedServiceFactory ManagedServiceFactory}
	 * is registered for the Service Component, we may create multiple Component
	 * Configurations.
	 * </p>
	 * 
	 * <p>
	 * After the Component Configuration(s) are created, call 
	 * {@link Resolver#resolve(ServiceEvent) getEligible(null)} to try to
	 * satisfy them.
	 * </p>
	 * 
	 * @param componentDescriptions - a List of {@link ComponentDescription}s to 
	 *        be enabled 
	 */
	public void enableComponents(List componentDescriptions) throws ComponentException {

		Iterator it = componentDescriptions.iterator();
		while (it.hasNext()) {
			ComponentDescription cd = (ComponentDescription) it.next();

			// add to our enabled lookup list
			enabledCDsByName.put(cd.getName(), cd);

			// check for a Configuration properties for this component
			Configuration config = null;
			try {
				ConfigurationAdmin configurationAdmin = (ConfigurationAdmin) configAdminTracker.getService();
				if (configurationAdmin != null) {
					config = configurationAdmin.getConfiguration(cd.getName(), cd.getBundleContext().getBundle().getLocation());
				}
			} catch (IOException e) {
				// Log it and continue
				Log.log(1, "[SCR] IOException when getting Configuration Properties. ", e);
			}

			// if no Configuration
			if (config == null) {
				// create ComponentConfiguration
				map(cd, null);

			} else {

				// if ManagedServiceFactory
				if (config.getFactoryPid() != null) {

					// if ComponentFactory is specified
					if (cd.getFactory() != null) {
						throw new ComponentException("incompatible to specify both ComponentFactory and ManagedServiceFactory are incompatible");
					}

					Configuration[] configs = null;
					try {
						ConfigurationAdmin cm = (ConfigurationAdmin) configAdminTracker.getService();
						configs = cm.listConfigurations("(service.factoryPid=" + config.getFactoryPid() + ")");
					} catch (InvalidSyntaxException e) {
						Log.log(1, "[SCR] InvalidSyntaxException when getting CM Configurations. ", e);
					} catch (IOException e) {
						Log.log(1, "[SCR] IOException when getting CM Configurations. ", e);
					}

					// for each MSF set of properties(P), map(CD,P)
					if (configs != null) {
						for (int i = 0; i < configs.length; i++) {
							map(cd, configs[i].getProperties());
						}
					}
				} else {
					// if Service
					map(cd, config.getProperties());
				}
			}
		}
		// resolve
		resolve(null);
	}

	/**
	 * Combine ConfigAdmin properties with a Service Component 
	 * ({@link ComponentDescription}) to create a Component Configuration 
	 * ({@link ComponentConfiguration}), and add it to our list of enabled 
	 * Component Configurations ({@link Resolver#enabledComponentConfigurations}).
	 * 
	 * The ConfigAdmin properties are combined with the properties from the 
	 * Service Component's XML.
	 * 
	 * @param cd Service Component
	 * @param configAdminProps ConfigAdmin properties for this Component
	 *        Configuration 
	 */
	public ComponentConfiguration map(ComponentDescription cd, Dictionary configAdminProps) {
		return doMap(cd, configAdminProps, cd.getFactory() != null);
	}

	/**
	 * Create a Component Configuration of a Service Component that has the
	 * "factory" attribute.  
	 * 
	 * @see Resolver#map(ComponentDescription, Dictionary)
	 * @see ComponentConfiguration#componentFactory
	 */
	public ComponentConfiguration mapFactoryInstance(ComponentDescription cd, Dictionary configAdminProps) {
		return doMap(cd, configAdminProps, false);
	}

	private ComponentConfiguration doMap(ComponentDescription cd, Dictionary configAdminProps, boolean componentFactory) {

		// Create CD+P

		// calculate the component configuration's properties
		Hashtable properties = initProperties(cd, configAdminProps);

		// for each Reference Description, create a reference object
		List references = new ArrayList();
		Iterator it = cd.getReferenceDescriptions().iterator();
		while (it.hasNext()) {
			ReferenceDescription referenceDesc = (ReferenceDescription) it.next();

			// create new Reference Object
			Reference ref = new Reference(referenceDesc, properties);
			references.add(ref);

		}
		references = !references.isEmpty() ? references : Collections.EMPTY_LIST;

		ComponentConfiguration componentConfiguration = new ComponentConfiguration(cd, references, properties, componentFactory);

		//for each Reference, set it's "parent" (the component configuration)
		it = componentConfiguration.getReferences().iterator();
		while (it.hasNext()) {
			Reference reference = (Reference) it.next();

			// set parent component configuration
			reference.setComponentConfiguration(componentConfiguration);
		}

		cd.addComponentConfiguration(componentConfiguration);

		// add CD+P to set
		enabledComponentConfigurations.add(componentConfiguration);

		return componentConfiguration;
	}

	/**
	 * Initialize Properties for a CD+P
	 * 
	 * The property elements provide default or supplemental property values if
	 * not overridden by the properties retrieved from Configuration Admin.
	 * 
	 * The property and properties elements are processed in top to bottom
	 * order. This allows later elements to override property values defined by
	 * earlier elements. There can be many property and properties elements and
	 * they may be interleaved.
	 * 
	 * @return Dictionary properties
	 */
	private Hashtable initProperties(ComponentDescription cd, Dictionary configAdminProps) {

		Hashtable properties = new Hashtable();

		// 0) add Reference target properties
		Iterator it = cd.getReferenceDescriptions().iterator();
		while (it.hasNext()) {
			ReferenceDescription referenceDesc = (ReferenceDescription) it.next();
			if (referenceDesc.getTarget() != null) {
				properties.put(referenceDesc.getName() + ".target", referenceDesc.getTarget());
			}
		}

		// 1) get properties from Service Component XML, in parse order
		properties.putAll(cd.getProperties());

		// 2) Add configAdmin properties
		if (configAdminProps != null) {
			Enumeration keys = configAdminProps.keys();
			while (keys.hasMoreElements()) {
				Object key = keys.nextElement();
				properties.put(key, configAdminProps.get(key));
			}
		}

		// add component.name and component.id (cannot be overridden)
		properties.put(ComponentConstants.COMPONENT_NAME, cd.getName());
		properties.put(ComponentConstants.COMPONENT_ID, new Long(getNextComponentId()));

		// add component.factory if it's a factory
		if (cd.getFactory() != null) {
			properties.put(ComponentConstants.COMPONENT_FACTORY, cd.getFactory());
		}

		// add ObjectClass so we can match target filters before actually being
		// registered
		List servicesProvided = cd.getServicesProvided();
		if (!servicesProvided.isEmpty()) {
			properties.put(Constants.OBJECTCLASS, servicesProvided.toArray(new String[servicesProvided.size()]));
		}

		return properties;
	}

	/**
	 * Disable Service Components.
	 * 
	 * For each Service Component ({@link ComponentDescription}),
	 * dispose of all of it's Component Configurations 
	 * ({@link ComponentConfiguration}s).
	 * 
	 * @see Resolver#disposeComponentConfigurations(List)
	 * 
	 * @param componentDescriptions List of {@link ComponentConfiguration}s to
	 *        disable
	 */
	public void disableComponents(List componentDescriptions) {

		// Received list of CDs to disable
		Iterator it = componentDescriptions.iterator();
		while (it.hasNext()) {

			// get the CD
			ComponentDescription cd = (ComponentDescription) it.next();

			disposeComponentConfigurations((List) ((ArrayList) cd.getComponentConfigurations()).clone());

			cd.clearComponentConfigurations();

			enabledCDsByName.remove(cd.getName());
		}

	}

	/**
	 * Dispose of Component Configurations ({@link ComponentConfiguration}s).
	 * 
	 * Remove Component Configurations from satisfied and enabled lists, and send
	 * to InstanceProcess to be unregistered, deactivated, and unbound.
	 * 
	 * @see InstanceProcess#disposeComponentConfigurations(List)
	 * 
	 * @param componentConfigurations List of {@link ComponentConfiguration}s
	 */
	public void disposeComponentConfigurations(List componentConfigurations) {
		// unregister, deactivate, and unbind
		satisfiedComponentConfigurations.removeAll(componentConfigurations);
		enabledComponentConfigurations.removeAll(componentConfigurations);
		instanceProcess.disposeComponentConfigurations(componentConfigurations);
	}

	/**
	 * Process a service change
	 * <p>
	 * A change has happened in the OSGi service environment, or new
	 * Component Configurations have been added to the system.
	 * </p>
	 * Depending on the change, take the following actions:
	 * <p>
	 * If new Component Configurations were added (param event is null):
	 *  <ol>
	 *     <li>Check for circularity and mark cycles</li>
	 *     <li>Send newly satisfied Component Configurations to Instance 
	 *     process</li>
	 *  </ol>
	 *  </p>
	 *  <p>
	 * If a service was registered:
	 * <ol>
	 *    <li>Put "Dynamic Bind" events on the queue for any Component 
	 *    Configurations which should be bound to the new service</li>
	 *    <li>Send newly satisfied Component Configurations to Instance 
	 *    process</li>
	 * </ol>
	 * </p>
	 * <p>
	 * If a service was modified:
	 * <ol>
	 *    <li>Synchronously dispose of all Component Configurations that 
	 *    become unsatisfied</li>
	 *    <li>Put "Dynamic Unbind Bind" events on the queue for any remaining 
	 *    Component Configurations which should be unbound from the service</li>
	 *    <li>Put "Dynamic Bind" events on the queue for any Component 
	 *    Configurations which should be bound to the modified service</li>
	 *    <li>Send newly satisfied Component Configurations to Instance 
	 *    process</li>
	 * </ol>
	 * </p>
	 * <p>
	 * If a service was unregistered:
	 * <ol>
	 *    <li>Synchronously dispose of all Component Configurations that 
	 *    become unsatisfied</li>
	 *    <li>Put "Dynamic Unbind Bind" events on the queue for any remaining 
	 *    Component Configurations which should be unbound from the service</li>
	 * </ol>
	 * </p>
	 * 
	 * @param event the service event or null if new component configurations were added to the enabled list
	 */
	private void resolve(ServiceEvent event) {

		// if added component configurations
		if (event == null) {
			// we added a component configuration, so check for circularity and mark
			// cycles
			resolveCycles();

			// get list of newly satisfied component configurations and build them
			List newlySatisfiedComponentConfigurations = resolveSatisfied();
			newlySatisfiedComponentConfigurations.removeAll(satisfiedComponentConfigurations);

			if (!newlySatisfiedComponentConfigurations.isEmpty()) {
				satisfiedComponentConfigurations.addAll(newlySatisfiedComponentConfigurations); // add to satisfiedComponentConfigurations before dispatch
				workQueue.enqueueWork(this, BUILD, newlySatisfiedComponentConfigurations);
			}

		}
		// if service registered
		else if (event.getType() == ServiceEvent.REGISTERED) {

			// dynamic bind
			List dynamicBind = selectDynamicBind(event.getServiceReference());
			if (!dynamicBind.isEmpty()) {
				workQueue.enqueueWork(this, DYNAMICBIND, dynamicBind);
			}

			// get list of newly satisfied component configurations and build them
			List newlySatisfiedComponentConfigurations = resolveSatisfied();
			newlySatisfiedComponentConfigurations.removeAll(satisfiedComponentConfigurations);
			if (!newlySatisfiedComponentConfigurations.isEmpty()) {
				satisfiedComponentConfigurations.addAll(newlySatisfiedComponentConfigurations); // add to satisfiedComponentConfigurations before dispatch
				workQueue.enqueueWork(this, BUILD, newlySatisfiedComponentConfigurations);
			}

		}
		// if service modified
		else if (event.getType() == ServiceEvent.MODIFIED) {

			// check for newly unsatisfied components and synchronously
			// dispose them
			List newlyUnsatisfiedComponentConfigurations = (List) ((ArrayList) satisfiedComponentConfigurations).clone();
			newlyUnsatisfiedComponentConfigurations.removeAll(resolveSatisfied());
			if (!newlyUnsatisfiedComponentConfigurations.isEmpty()) {
				satisfiedComponentConfigurations.removeAll(newlyUnsatisfiedComponentConfigurations);

				instanceProcess.disposeComponentConfigurations(newlyUnsatisfiedComponentConfigurations);
			}

			// dynamic unbind
			// check each satisfied component configuration - do we need to unbind
			Map dynamicUnBind = selectDynamicUnBind(event.getServiceReference());
			if (!dynamicUnBind.isEmpty()) {
				instanceProcess.dynamicUnBind(dynamicUnBind);
			}

			// dynamic bind
			List dynamicBind = selectDynamicBind(event.getServiceReference());
			if (!dynamicBind.isEmpty()) {
				workQueue.enqueueWork(this, DYNAMICBIND, dynamicBind);
			}

			// get list of newly satisfied component configurations and build them
			List newlySatisfiedComponentConfigurations = resolveSatisfied();
			newlySatisfiedComponentConfigurations.removeAll(satisfiedComponentConfigurations);
			if (!newlySatisfiedComponentConfigurations.isEmpty()) {
				satisfiedComponentConfigurations.addAll(newlySatisfiedComponentConfigurations); // add to satisfiedComponentConfigurations before dispatch
				workQueue.enqueueWork(this, BUILD, newlySatisfiedComponentConfigurations);
			}

		}
		// if service unregistering
		else if (event.getType() == ServiceEvent.UNREGISTERING) {

			// check for newly unsatisfied components and
			// synchronously dispose them
			List newlyUnsatisfiedComponentConfigurations = (List) ((ArrayList) satisfiedComponentConfigurations).clone();
			newlyUnsatisfiedComponentConfigurations.removeAll(resolveSatisfied());
			if (!newlyUnsatisfiedComponentConfigurations.isEmpty()) {
				satisfiedComponentConfigurations.removeAll(newlyUnsatisfiedComponentConfigurations);

				instanceProcess.disposeComponentConfigurations(newlyUnsatisfiedComponentConfigurations);
			}

			// dynamic unbind
			Map dynamicUnBind = selectDynamicUnBind(event.getServiceReference());
			if (!dynamicUnBind.isEmpty()) {
				instanceProcess.dynamicUnBind(dynamicUnBind);
			}

		}

	}

	/**
	 * Check if a particular component configuration is satisfied. Also checks for circularity. If
	 * component configuration is satisfied it is added to satisfiedComponentConfigurations list, but not sent to
	 * instance process
	 * 
	 * @param componentConfiguration
	 * @return
	 */
	public boolean justResolve(ComponentConfiguration componentConfiguration) {

		// we added a component configuration, so check for circularity and mark
		// cycles
		resolveCycles();

		// get list of newly satisfied component configurations and build them
		List newlySatisfiedComponentConfigurations = resolveSatisfied();
		newlySatisfiedComponentConfigurations.removeAll(satisfiedComponentConfigurations);

		if (!newlySatisfiedComponentConfigurations.contains(componentConfiguration)) {
			return false;
		}
		satisfiedComponentConfigurations.add(componentConfiguration);
		return true;

	}

	/**
	 * Calculate which of the currently enabled Component Configurations 
	 * ({@link Resolver#enabledComponentConfigurations}) are "satisfied".  
	 * 
	 * <p>
	 * An "enabled" Component 
	 * Configuration is "satisfied" if there is at least one OSGi Service
	 * registered that has the correct interface and matches the target filter 
	 * for each of it's required (cardinality = "1..1" or "1..n") references.
	 * </p>
	 * <p>
	 * If a Component Configuration will register a service and security is 
	 * enabled, check if the bundle it comes from has 
	 * {@link ServicePermission#REGISTER} for that service.  If the Component
	 * Configuration does not have the necessary permission it is not "satisfied".
	 * </p>
	 * @return List of {@link ComponentConfiguration}s that are "satisfied"
	 */
	private List resolveSatisfied() {
		List resolvedSatisfiedComponentConfigurations = new ArrayList();

		Iterator it = enabledComponentConfigurations.iterator();
		while (it.hasNext()) {
			ComponentConfiguration componentConfiguration = (ComponentConfiguration) it.next();
			ComponentDescription cd = componentConfiguration.getComponentDescription();

			// check if all the services needed by the component configuration are available
			List refs = componentConfiguration.getReferences();
			Iterator iterator = refs.iterator();
			boolean hasProviders = true;
			while (iterator.hasNext()) {
				Reference reference = (Reference) iterator.next();
				if (reference != null) {
					if (reference.getReferenceDescription().isRequired() && !reference.hasProvider(componentConfiguration.getComponentDescription().getBundleContext())) {
						hasProviders = false;
						break;
					}
				}
			}
			if (!hasProviders)
				continue;

			// check if the bundle providing the service has permission to
			// register the provided interface(s)
			// if a service is provided
			// TODO we can cache the ServicePermission objects
			if (cd.getService() != null && System.getSecurityManager() != null) {
				ProvideDescription[] provides = cd.getService().getProvides();
				Bundle bundle = cd.getBundleContext().getBundle();
				boolean hasPermission = true;
				for (int i = 0; i < provides.length; i++) {
					// make sure bundle has permission to register the service
					if (!bundle.hasPermission(new ServicePermission(provides[i].getInterfacename(), ServicePermission.REGISTER))) {
						hasPermission = false;
						break;
					}
				}
				if (!hasPermission)
					continue;
			}

			// we have providers and permission - this component configuration is satisfied
			resolvedSatisfiedComponentConfigurations.add(componentConfiguration);
		} // end while (more enabled component configurations)
		return resolvedSatisfiedComponentConfigurations.isEmpty() ? Collections.EMPTY_LIST : resolvedSatisfiedComponentConfigurations;
	}

	/**
	 * Listen for service change events
	 * 
	 * @param event
	 */
	public void serviceChanged(ServiceEvent event) {

		ServiceReference reference = event.getServiceReference();
		int eventType = event.getType();

		if (DEBUG) {
			System.out.println("ServiceChanged: serviceReference = " + reference);
			System.out.println("ServiceChanged: Event type = " + eventType + " , reference.getBundle() = " + reference.getBundle());
		}

		// if ((reference.getProperty(ComponentConstants.COMPONENT_ID) == null)

		switch (eventType) {
			case ServiceEvent.MODIFIED :
			case ServiceEvent.REGISTERED :
			case ServiceEvent.UNREGISTERING :

				resolve(event);
				break;
		}

	}

	/**
	 * Called asynchronously by the work queue thread to perform work.
	 * <p>
	 * There are two possible work actions:
	 * <ul>
	 *    <li>BUILD - workObject is a list of Component Configurations to be
	 *    sent to the Instance process.  The Component Configurations have become
	 *    satisfied.  Check that the Component Configurations are still satisfied 
	 *    (system state may have changed while they were waiting on the work 
	 *    queue) and send them to the instance process 
	 *    ({@link InstanceProcess#registerComponentConfigurations(List)}).
	 *    </li>
	 *    <li>DYNAMICBIND - workObject is a List of References that need to be 
	 *    dynamically bound.  Check that the Component Configurations are still 
	 *    satisfied (system state may have changed while they were waiting on 
	 *    the work queue) and send them to the instance process 
	 *    ({@link InstanceProcess#dynamicBind(List)}).
	 * </ul>
	 * </p>
	 * @param workAction {@link Resolver#BUILD} or {@link Resolver#DYNAMICBIND}
	 * @param workObject a List of {@link ComponentConfiguration}s if workAction
	 *        is {@link Resolver#BUILD} or a List of {@link Reference}s if workAction 
	 *        is {@link Resolver#DYNAMICBIND} 
	 * @see org.eclipse.equinox.ds.workqueue.WorkDispatcher#dispatchWork(int,
	 *      java.lang.Object)
	 */
	public void dispatchWork(int workAction, Object workObject) {
		Iterator it;
		switch (workAction) {
			case BUILD :
				// only build if component configurations are still satisfied
				List queueComponentConfigurations = (List) workObject;
				List componentConfigurations = new ArrayList(queueComponentConfigurations.size());
				it = queueComponentConfigurations.iterator();
				while (it.hasNext()) {
					ComponentConfiguration componentConfiguration = (ComponentConfiguration) it.next();
					if (this.satisfiedComponentConfigurations.contains(componentConfiguration)) {
						componentConfigurations.add(componentConfiguration);
					}
				}
				if (!componentConfigurations.isEmpty()) {
					instanceProcess.registerComponentConfigurations(componentConfigurations);
				}
				break;
			case DYNAMICBIND :
				// only dynamicBind if component configurations are still satisfied
				List references = (List) workObject;
				it = references.iterator();
				while (it.hasNext()) {
					if (!this.satisfiedComponentConfigurations.contains(((Reference) it.next()).getComponentConfiguration())) {
						// modifies underlying list
						it.remove();
					}
				}
				if (!references.isEmpty()) {
					instanceProcess.dynamicBind(references);
				}
				break;
		}
	}

	/**
	 * Calculate which of the currently satisfied component configurations 
	 * ({@link Resolver#satisfiedComponentConfigurations}) need to be dynamically bound to an OSGi
	 * service.
	 * 
	 * @param serviceReference the service
	 * @return a List of {@link Reference}s that need to be dynamically bound 
	 *         to this service
	 */
	private List selectDynamicBind(ServiceReference serviceReference) {
		List bindList = new ArrayList();
		Iterator it = satisfiedComponentConfigurations.iterator();
		while (it.hasNext()) {
			ComponentConfiguration componentConfiguration = (ComponentConfiguration) it.next();
			List references = componentConfiguration.getReferences();
			Iterator refIt = references.iterator();
			while (refIt.hasNext()) {
				Reference reference = (Reference) refIt.next();
				if (reference.dynamicBindReference(serviceReference)) {
					bindList.add(reference);
				}
			}
		}
		return bindList;
	}

	/**
	 * An OSGi service is unregistering, calculate which of the satisfied 
	 * Component Configurations need to dynamically unbind from it.
	 * <p>
	 *  A Component Configuration needs to dynamically unbind from a service
	 *  if it was bound to the service and the reference it was policy="dynamic".
	 *  </p>
	 * @param serviceReference
	 * @return a Map of {@link Reference}:{@link ServiceReference} to unbind
	 */
	private Map selectDynamicUnBind(ServiceReference serviceReference) {

		Map unbindJobs = new Hashtable();

		Iterator it = satisfiedComponentConfigurations.iterator();
		while (it.hasNext()) {
			ComponentConfiguration componentConfiguration = (ComponentConfiguration) it.next();
			List references = componentConfiguration.getReferences();
			Iterator it_ = references.iterator();
			while (it_.hasNext()) {
				Reference reference = (Reference) it_.next();
				// Is reference dynamic and bound to this service? - must unbind
				if (reference.dynamicUnbindReference(serviceReference)) {
					unbindJobs.put(reference, serviceReference);
				}
			}
		}
		return unbindJobs.isEmpty() ? Collections.EMPTY_MAP : unbindJobs;
	}

	/**
	 * Doubly-linked node used to traverse the dependency tree in order to
	 * find cycles.
	 *  
	 * @version $Revision: 1.3 $
	 */
	static private class ReferenceComponentConfiguration {
		public Reference ref;
		public ComponentConfiguration producer;

		protected ReferenceComponentConfiguration(Reference ref, ComponentConfiguration producer) {
			this.ref = ref;
			this.producer = producer;
		}
	}

	/**
	 * Check through the enabled list for cycles. Cycles can only exist if every
	 * service is provided by a Service Component (not legacy OSGi). If the cycle 
	 * has no optional dependencies, log an error and disable a Component 
	 * Configuration in the cycle. If cycle can be "broken" by an optional 
	 * dependency, make a note (stored in the 
	 * {@link ComponentConfiguration#delayActivateComponentConfigurationNames} List).
	 * 
	 * @throws CircularityException if cycle exists with no optional
	 *         dependencies
	 */
	private void resolveCycles() {

		try {
			// find the component configurations that resolve using other component configurations and record their
			// dependencies
			Hashtable dependencies = new Hashtable();
			Iterator it = enabledComponentConfigurations.iterator();
			while (it.hasNext()) {
				ComponentConfiguration enabledComponentConfiguration = (ComponentConfiguration) it.next();
				List dependencyList = new ArrayList();
				Iterator refIt = enabledComponentConfiguration.getReferences().iterator();
				while (refIt.hasNext()) {
					Reference reference = (Reference) refIt.next();

					// see if it resolves to one of the other enabled component configurations
					ComponentConfiguration providerComponentConfiguration = reference.findProviderComponentConfiguration(enabledComponentConfigurations);
					if (providerComponentConfiguration != null) {
						dependencyList.add(new ReferenceComponentConfiguration(reference, providerComponentConfiguration));
					}
				} // end while(more references)

				if (!dependencyList.isEmpty()) {
					// component configuration resolves using some other component configurations, could be a cycle
					dependencies.put(enabledComponentConfiguration, dependencyList);
				} else {
					dependencies.put(enabledComponentConfiguration, Collections.EMPTY_LIST);
				}
			} // end while (more enabled component configurations)

			//traverse dependency tree and look for cycles
			Set visited = new HashSet();
			it = dependencies.keySet().iterator();
			while (it.hasNext()) {
				ComponentConfiguration componentConfiguration = (ComponentConfiguration) it.next();
				if (!visited.contains(componentConfiguration)) {
					List currentStack = new ArrayList();
					traverseDependencies(componentConfiguration, visited, dependencies, currentStack);
				}
			}
		} catch (CircularityException e) {
			// log the error
			Log.log(LogService.LOG_ERROR, "[SCR] Circularity Exception.", e);

			// disable offending component configuration
			enabledComponentConfigurations.remove(e.getCircularDependency());

			// try again
			resolveCycles();
		}
	}

	/**
	 * Recursively do a depth-first traversal of a dependency tree, looking for 
	 * cycles.
	 * <p>
	 * If a cycle is found, calls 
	 * {@link Resolver#handleDependencyCycle(ReferenceComponentConfiguration, List)}.
	 * </p>
	 * 
	 * @param componentConfiguration current node in dependency tree
	 * @param visited Set of {@link ComponentConfiguration} that are visited 
	 *        nodes
	 * @param dependencies Dependency tree - a Hashtable of 
	 * ({@link ComponentConfiguration}):(List of {@link ReferenceComponentConfiguration}s)
	 * @param currentStack List of {@link ReferenceComponentConfiguration}s - the history of our
	 *        traversal so far (the path back to the root of the tree)
	 * @throws CircularityException if an cycle with no optional dependencies is
	 * found.
	 */
	private void traverseDependencies(ComponentConfiguration componentConfiguration, Set visited, Hashtable dependencies, List currentStack) throws CircularityException {

		// the component has already been visited and it's dependencies checked
		// for cycles
		if (visited.contains(componentConfiguration)) {
			return;
		}

		List refComponentConfigurations = (List) dependencies.get(componentConfiguration);
		Iterator it = refComponentConfigurations.iterator();
		// first, add the component configuration's dependencies
		while (it.hasNext()) {

			ReferenceComponentConfiguration refComponentConfiguration = (ReferenceComponentConfiguration) it.next();

			if (currentStack.contains(refComponentConfiguration)) {
				// may throw circularity exception
				handleDependencyCycle(refComponentConfiguration, currentStack);
				return;
			}
			currentStack.add(refComponentConfiguration);

			traverseDependencies(refComponentConfiguration.producer, visited, dependencies, currentStack);

			currentStack.remove(refComponentConfiguration);
		}
		// finally write the component configuration
		visited.add(componentConfiguration);

	}

	/**
	 * A cycle was detected. component configuration is referenced by the last element in
	 * currentStack. Throws CircularityException if the cycle does not contain
	 * an optional dependency, else choses a point at which to
	 * "break" the cycle (the break point must be immediately after an
	 * optional dependency) and adds a "cycle note".
	 * 
	 * @see ComponentConfiguration#delayActivateComponentConfigurationNames
	 */
	private void handleDependencyCycle(ReferenceComponentConfiguration refComponentConfiguration, List currentStack) throws CircularityException {
		ListIterator cycleIterator = currentStack.listIterator(currentStack.indexOf(refComponentConfiguration));

		// find an optional dependency
		ReferenceComponentConfiguration optionalRefComponentConfiguration = null;
		while (cycleIterator.hasNext()) {
			ReferenceComponentConfiguration cycleRefComponentConfiguration = (ReferenceComponentConfiguration) cycleIterator.next();
			if (!cycleRefComponentConfiguration.ref.getReferenceDescription().isRequired()) {
				optionalRefComponentConfiguration = cycleRefComponentConfiguration;
				break;
			}
		}

		if (optionalRefComponentConfiguration == null) {
			// no optional dependency
			throw new CircularityException(refComponentConfiguration.ref.getComponentConfiguration());
		}

		// add note not to initiate activation of next dependency
		optionalRefComponentConfiguration.ref.getComponentConfiguration().setDelayActivateComponentConfigurationName(optionalRefComponentConfiguration.producer.getComponentDescription().getName());
	}

	/**
	 * Method to return the next available component id.
	 * 
	 * @return next component id.
	 */
	private long getNextComponentId() {
		synchronized (this) {
			return componentid++;
		}
	}

}
