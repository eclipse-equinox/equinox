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
package org.eclipse.equinox.ds.instance;

import java.io.IOException;
import java.util.*;
import org.eclipse.equinox.ds.Activator;
import org.eclipse.equinox.ds.Log;
import org.eclipse.equinox.ds.model.ComponentDescription;
import org.eclipse.equinox.ds.model.ComponentConfiguration;
import org.eclipse.equinox.ds.resolver.Reference;
import org.eclipse.equinox.ds.service.ComponentFactoryImpl;
import org.eclipse.equinox.ds.service.ComponentInstanceImpl;
import org.eclipse.equinox.ds.workqueue.WorkQueue;
import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentFactory;

/**
 * Register services for satisfied Component Configurations and listen for 
 * configuration changes from ConfigAdmin
 * 
 */
public class InstanceProcess implements ConfigurationListener {

	/* set this to true to compile in debug messages */
	private static final boolean DEBUG = false;

	/** Main SCR class */
	private Activator main;

	/* Actually does the work of building and disposing of instances */
	public BuildDispose buildDispose;

	protected WorkQueue workQueue;

	//configListener service registered by this class
	private ServiceRegistration configListener;

	/**
	 * Handle Instance processing
	 * 
	 * @param main - the Main class of the SCR
	 */
	public InstanceProcess(Activator main) {

		this.main = main;

		// use Main's workqueue
		workQueue = main.workQueue;
		buildDispose = new BuildDispose(main);

		configListener = main.context.registerService(ConfigurationListener.class.getName(), this, null);

	}

	/**
	 * dispose cleanup, the SCR is shutting down
	 */
	public void dispose() {

		if (configListener != null) {
			main.context.ungetService(configListener.getReference());
		}

		buildDispose.dispose();
		buildDispose = null;
		main = null;
		workQueue = null;
	}

	/**
	 * Register and possibly activate Component Configurations.
	 * 
	 * Activate each Service Component that has the attribute immediate=true.
	 * 
	 * If Service Component has a service element, register the service or
	 * service factory.
	 * 
	 * If Service Component has the factory attribute set, register a 
	 * {@link org.osgi.service.component.ComponentFactory}.
	 * 
	 * @param componentConfigurations - List of satisfied Component Configurations
	 */
	public void registerComponentConfigurations(List componentConfigurations) {

		ComponentConfiguration componentConfiguration;
		ComponentDescription cd;
		String factoryPid = null;

		// loop through CD+P list

		Iterator it = componentConfigurations.iterator();
		while (it.hasNext()) {
			componentConfiguration = (ComponentConfiguration) it.next();
			cd = componentConfiguration.getComponentDescription();
			if (DEBUG)
				System.out.println("InstanceProcess: buildInstances: component name = " + cd.getName());


			// ComponentFactory
			if (componentConfiguration.isComponentFactory()) {
				if (DEBUG)
					System.out.println("InstanceProcess: buildInstances: ComponentFactory");
				// check if MSF
				ConfigurationAdmin configurationAdmin = (ConfigurationAdmin) main.resolver.configAdminTracker.getService();

				if (configurationAdmin != null) {
					try {
						Configuration config = configurationAdmin.getConfiguration(cd.getName());
						if (config != null)
							factoryPid = config.getFactoryPid();
					} catch (IOException e) {
						Log.log(1, "[SCR] Error attempting to create componentFactory. ", e);
					}
				}

				// if MSF throw exception - can't be ComponentFactory and
				// MSF
				if (factoryPid != null) {
					throw new org.osgi.service.component.ComponentException("ManagedServiceFactory and ConfigurationFactory are incompatible");
				}

				// if the factory attribute is set on the component element
				// then register a component factory service
				// for the Service Component on behalf of the Service
				// Component.
				componentConfiguration.setServiceRegistration(cd.getBundleContext().registerService(ComponentFactory.class.getName(), new ComponentFactoryImpl(componentConfiguration, main), componentConfiguration.getProperties()));
				continue; // break so we do not create an instance
			} 
			
			// if component is immediate or a factory instance - create instance
			// if it is a factory instance, we need to create it before we register its service
			if (cd.isImmediate() || (cd.getFactory() != null)) {
				try {
					buildDispose.buildComponentConfigInstance(null, componentConfiguration);
				} catch (ComponentException e) {
					Log.log(1, "[SCR] Error attempting to build Component.", e);
				}
			}
			
			// if Service
			if (cd.getService() != null) {
				RegisterComponentService.registerService(this, componentConfiguration);
			}

		}// end while(more componentConfigurations)
	}

	/**
	 * 
	 * Dispose of Component Configurations
	 * 
	 * @param componentConfigurations - list of Component Configurations to be
	 *         disposed
	 */

	public void disposeComponentConfigurations(List componentConfigurations) {

		// loop through CD+P list to be disposed
		Iterator it = componentConfigurations.iterator();
		while (it.hasNext()) {
			ComponentConfiguration componentConfiguration = (ComponentConfiguration) it.next();

			// dispose component
			buildDispose.disposeComponentConfig(componentConfiguration);
		}
	}

	/**
	 * Dynamically bind references.
	 * 
	 * @param references List of {@link Reference}s to dynamically bind
	 */
	public void dynamicBind(List references) {
		Iterator it = references.iterator();
		while (it.hasNext()) {
			Reference reference = (Reference) it.next();
			List instances = reference.getComponentConfiguration().getInstances();
			Iterator it2 = instances.iterator();
			while (it2.hasNext()) {
				ComponentInstanceImpl compInstance = (ComponentInstanceImpl) it2.next();
				buildDispose.bindReference(reference, compInstance);
			}
		}
	}

	/**
	 * Dynamically unbind references.
	 * 
	 * @param unbindJobs Map of {@link Reference}:{@link ServiceReference}
	 * to be unbound
	 */
	public void dynamicUnBind(Map unbindJobs) {
		// for each unbind job
		Iterator itr = unbindJobs.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry unbindJob = (Map.Entry) itr.next();
			Reference reference = (Reference) unbindJob.getKey();
			ComponentConfiguration componentConfiguration = reference.getComponentConfiguration();
			ServiceReference serviceReference = (ServiceReference) unbindJob.getValue();

			// get the list of instances created
			List instances = componentConfiguration.getInstances();
			Iterator it = instances.iterator();
			while (it.hasNext()) {
				ComponentInstanceImpl compInstance = (ComponentInstanceImpl) it.next();
				Object instance = compInstance.getInstance();
				if (instance != null) {
					try {
						buildDispose.unbindDynamicReference(reference, compInstance, serviceReference);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}

			// all instances are now unbound
			reference.removeServiceReference(serviceReference);
		}
	}

	/**
	 * Listen for configuration changes
	 * 
	 * Service Components can receive properties from the Configuration Admin
	 * service. If a Component Configuration is activated and it’s properties are
	 * updated in the Configuration Admin service, the SCR must deactivate the
	 * component and activate the component again using the new properties.
	 * 
	 * @param event ConfigurationEvent
	 * 
	 * @see ConfigurationListener#configurationEvent(org.osgi.service.cm.ConfigurationEvent)
	 */
	public void configurationEvent(ConfigurationEvent event) {

		Configuration[] config = null;

		String pid = event.getPid();
		if (DEBUG)
			System.out.println("pid = " + pid);

		String fpid = event.getFactoryPid();
		if (DEBUG)
			System.out.println("fpid = " + fpid);

		//See if this configuration event is for declarative services
		ComponentDescription cd;
		if (fpid != null) {
			// find the fpid == component name in the CD list
			cd = (ComponentDescription) main.resolver.enabledCDsByName.get(fpid);
		} else {
			// find the spid == component name in the CD list
			cd = (ComponentDescription) main.resolver.enabledCDsByName.get(pid);
		}
		if (cd == null) {
			//this configuration event has nothing to do with declarative services
			return;
		}

		switch (event.getType()) {
			case ConfigurationEvent.CM_UPDATED :

				String filter = (fpid != null ? "(&" : "") + "(" + Constants.SERVICE_PID + "=" + pid + ")" + (fpid != null ? ("(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + fpid + "))") : "");

				// Get the config for this service.pid
				ConfigurationAdmin cm = (ConfigurationAdmin) main.resolver.configAdminTracker.getService();
				try {
					config = cm.listConfigurations(filter);
				} catch (IOException e) {
					Log.log(1, "[SCR] Error attempting to list CM Configurations ", e);
				} catch (InvalidSyntaxException e) {
					Log.log(1, "[SCR] Error attempting to list CM Configurations ", e);
				}

				// if NOT a factory
				if (fpid == null) {

					// there is only one component configuration for this CD, so we can disable the
					// CD
					main.resolver.disableComponents(Collections.singletonList(cd));

					// now re-enable the CD - the resolver will pick up the new
					// config
					workQueue.enqueueWork(main, Activator.ADD, Collections.singletonList(cd));

					// If a MSF
					// create a new component configuration or update an existing one
				} else {

					// get component configuration with this PID
					ComponentConfiguration componentConfiguration = cd.getComponentConfigurationByPID(pid);

					// if only the no-props component configuration exists, replace it
					if (componentConfiguration == null && cd.getComponentConfigurations().size() == 1 && ((ComponentConfiguration) cd.getComponentConfigurations().get(0)).getProperties().get(Constants.SERVICE_PID) == null) {
						componentConfiguration = (ComponentConfiguration) cd.getComponentConfigurations().get(0);
					}

					// if old component configuration exists, dispose of it
					if (componentConfiguration != null) {
						// config already exists - dispose of it
						cd.removeComponentConfiguration(componentConfiguration);
						main.resolver.disposeComponentConfigurations(Collections.singletonList(componentConfiguration));
					}

					// create a new component configuration (adds to resolver enabledComponentConfigurations list)
					main.resolver.map(cd, config[0].getProperties());

					// kick the resolver to figure out if component configuration is satisfied, etc
					workQueue.enqueueWork(main, Activator.ADD, Collections.EMPTY_LIST);

				}

				break;
			case ConfigurationEvent.CM_DELETED :

				// if not a factory
				if (fpid == null) {

					// there is only one component configuration for this CD, so we can disable the
					// CD
					main.resolver.disableComponents(Collections.singletonList(cd));

					// now re-enable the CD - the resolver will create component configuration with
					// no
					// configAdmin properties
					workQueue.enqueueWork(main, Activator.ADD, Collections.singletonList(cd));
				} else {
					// config is a factory

					// get component configuration created for this config (with this PID)
					ComponentConfiguration componentConfiguration = cd.getComponentConfigurationByPID(pid);

					// if this was the last component configuration created for this factory
					if (cd.getComponentConfigurations().size() == 1) {

						// there is only one component configuration for this CD, so we can disable
						// the CD
						main.resolver.disableComponents(Collections.singletonList(cd));

						// now re-enable the CD - the resolver will create component configuration
						// with no
						// configAdmin properties
						workQueue.enqueueWork(main, Activator.ADD, Collections.singletonList(cd));

					} else {

						// we can just dispose this component configuration
						cd.removeComponentConfiguration(componentConfiguration);
						disposeComponentConfigurations(Collections.singletonList(componentConfiguration));
						main.resolver.satisfiedComponentConfigurations.remove(componentConfiguration);
						main.resolver.enabledComponentConfigurations.remove(componentConfiguration);

					}
				}
				break;
		}

	}
}
