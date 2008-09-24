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
package org.eclipse.equinox.internal.ds;

import java.util.*;
import org.eclipse.equinox.internal.ds.model.*;
import org.osgi.framework.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentException;

/**
 * Resolver.java
 * 
 * @author Valentin Valchev
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 * @version 1.1
 */

public final class Resolver implements WorkPerformer {

	// these strings are used only for debugging purpose
	static final String[] WORK_TITLES = {"BUILD ", "DYNAMICBIND "};

	/**
	 * Service Component instances need to be built.
	 */
	public static final int BUILD = 1;

	/**
	 * Service Component instances need to be rebound
	 */
	public static final int DYNAMICBIND = 2;

	/* Holds the enabled SCPs*/
	protected Vector scpEnabled;

	/**
	 * List of ComponentDescriptionProps, which are currently "satisfied"
	 * Component Configurations. this list is a subset of
	 * {@link Resolver#scpEnabled scpEnabled}.
	 */
	public Vector satisfiedSCPs;

	private InstanceProcess instanceProcess;

	private Object syncLock = new Object();

	public SCRManager mgr;

	// TODO: Add a hashtable connecting servicereference to a list of References
	// which they are bound to
	// This way the search of references to unbind becomes faster when there are
	// plenty of components.
	// Keep in mind that build process is asynchronous.

	/**
	 * Resolver constructor
	 * 
	 */
	Resolver(SCRManager mgr) {
		scpEnabled = new Vector();
		satisfiedSCPs = new Vector();
		instanceProcess = new InstanceProcess(this);
		this.mgr = mgr;
	}

	public Object getSyncLock() {
		return syncLock;
	}

	// This method should be called when the event processing thread is blocked
	// in a user code
	void queueBlocked() {
		syncLock = new Object();
		instanceProcess = new InstanceProcess(this);
	}

	// -- begin *enable* component routines
	/**
	 * enableComponents - called by the dispatchWorker
	 * 
	 * @param serviceComponents -
	 *            a list of all component descriptions for a single bundle to be
	 *            enabled Receive ArrayList of enabled CD's from ComponentCache
	 *            For each CD add to list of enabled create list of CD:CD+P
	 *            create list of CD+P:ref ( where ref is a Reference Object)
	 *            resolve CD+P
	 */
	void enableComponents(Vector serviceComponents) {
		long start = 0l;
		if (Activator.DEBUG) {
			Activator.log.debug(0, 10062, serviceComponents != null ? serviceComponents.toString() : "null", null, false);
			// //Activator.log.debug("Resolver.enableComponents(): " +
			// serviceComponents, null);
		}
		if (Activator.PERF) {
			start = System.currentTimeMillis();
		}

		synchronized (syncLock) {
			Configuration[] configs = null;

			if (serviceComponents != null) {
				for (int i = 0; i < serviceComponents.size(); i++) {
					ServiceComponent current = (ServiceComponent) serviceComponents.elementAt(i);
					// don't enable components which are not marked enabled
					// this is done here, not in the activator just because it
					// saves a little memory
					if (!current.enabled) {
						if (Activator.DEBUG) {
							Activator.log.debug(0, 10019, current.name, null, false);
							// //Activator.log.debug("Resolver.enableComponents():
							// ignoring not enabled component " + current.name,
							// null);
						}
						continue;
					}

					// check for a Configuration properties for this component
					try {
						String filter = "(|(" + Constants.SERVICE_PID + '=' + current.name + ")(" + ConfigurationAdmin.SERVICE_FACTORYPID + '=' + current.name + "))";
						configs = ConfigurationManager.listConfigurations(filter);
					} catch (Exception e) {
						Activator.log.error("[SCR] Cannot list configurations for component " + current.name, e);
					}
					// if no Configuration
					if (configs == null || configs.length == 0) {
						// create ServiceComponent + Prop
						map(current, (Dictionary) null);
					} else {
						// if ManagedServiceFactory
						Configuration config = configs[0];
						if (config.getFactoryPid() != null) {
							// if ComponentFactory is specified
							if (current.factory != null) {
								Activator.log.error("[SCR - Resolver] Cannot specify both ComponentFactory and ManagedServiceFactory\n" + "The name of the ComponentFactory component is " + current.name, null);
								continue; // skip current component
							}
							if (Activator.DEBUG) {
								Activator.log.debug("[SCR - Resolver] Resolver.enableComponents(): " + current.name + " as *managed service factory*", null);
							}
							try {
								configs = ConfigurationManager.listConfigurations("(service.factoryPid=" + config.getFactoryPid() + ")");
							} catch (Exception e) {
								Activator.log.error("[SCR] Cannot list configurations for component " + current.name, e);
							}
							// for each MSF set of properties(P), map(CD, new
							// CD+P(CD,P))
							if (configs != null) {
								for (int index = 0; index < configs.length; index++) {
									map(current, configs[index]);
								}
							}
						} else {
							if (Activator.DEBUG) {
								Activator.log.debug("[SCR - Resolver] Resolver.enableComponents(): " + current.name + " as *service*", null);
							} // if Service, not ManagedServiceFactory
							map(current, config);
						}
					} // end has configuration
				} // end process all components!
			}
		}

		buildNewlySatisfied();

		if (Activator.PERF) {
			start = System.currentTimeMillis() - start;
			Activator.log.info((serviceComponents != null ? "[DS perf] " + serviceComponents.size() : "[DS perf]") + " Components enabled for " + start + " ms");
		}
	}

	protected ServiceComponentProp map(ServiceComponent component, Configuration config) {
		Dictionary configProps = null;
		if (config != null) {
			try {
				configProps = config.getProperties();
			} catch (IllegalStateException ise) {
				// the configuration may have beed deleted already
			}
		}
		ServiceComponentProp scp = map(component, configProps);
		if (config != null) {
			// set the service PID & Factory Pid
			String pid = config.getPid();
			String fpid = config.getFactoryPid();
			if (pid != null)
				scp.properties.put(Constants.SERVICE_PID, pid);
			if (fpid != null)
				scp.properties.put(ConfigurationAdmin.SERVICE_FACTORYPID, fpid);
		}
		return scp;
	}

	/**
	 * Create the SCP and add to the maps
	 * 
	 * @param component the component for which SCP will be created 
	 * @param configProperties CM configuration properties
	 */
	public ServiceComponentProp map(ServiceComponent component, Dictionary configProperties) {
		ServiceComponentProp scp = null;
		try {
			if (Activator.DEBUG) {
				Activator.log.debug(0, 10063, component.name, null, false);
				// //Activator.log.debug("Resolver.map(): Creating SCP for
				// component " + component.name, null);
			}
			scp = new ServiceComponentProp(component, configProperties, mgr);

			// Get all the required service reference descriptions for this
			Vector referenceDescriptions = component.references;

			// for each Reference Description, create a reference object
			if (referenceDescriptions != null && !referenceDescriptions.isEmpty()) {
				Vector references = new Vector(referenceDescriptions.size());

				for (int i = 0; i < referenceDescriptions.size(); i++) {
					// create new Reference Object and add to CD+P:ref map
					ComponentReference cRef = (ComponentReference) referenceDescriptions.elementAt(i);

					Reference ref = new Reference(cRef, scp, scp.getProperties());
					references.addElement(ref);
				}
				scp.references = references;
			}
			component.addServiceComponentProp(scp);
			scpEnabled.addElement(scp);

		} catch (Throwable t) {
			Activator.log.error("[SCR] Unexpected exception while creating configuration for component " + component, t);
		}
		return scp;
	}

	/**
	 * Get the Eligible Components
	 * 
	 * loop through CD+P list of enabled get references check if eligible if
	 * true add to eligible list send to Instance Process
	 * 
	 */
	void getEligible(ServiceEvent event) {
		if (scpEnabled.isEmpty())
			return; // check for any enabled configurations

		if (Activator.DEBUG) {
			Activator.log.debug(0, 10020, event.toString(), null, false);
			////Activator.log.debug("Resolver.getEligible(): processing service event " + event, null);
			String eventType = "";
			if (event.getType() == ServiceEvent.UNREGISTERING) {
				eventType = "UNREGISTERING";
			} else if (event.getType() == ServiceEvent.REGISTERED) {
				eventType = "REGISTERED";
			} else if (event.getType() == ServiceEvent.MODIFIED) {
				eventType = "MODIFIED";
			}
			Activator.log.debug(0, 10050, eventType, null, false);
		}

		Object target = null;
		Vector resolvedComponents = null;
		switch (event.getType()) {
			case ServiceEvent.REGISTERED :
				Vector componentsWithStaticRefs;
				synchronized (syncLock) {
					componentsWithStaticRefs = selectStaticBind(satisfiedSCPs, event.getServiceReference());
					if (componentsWithStaticRefs != null) {
						removeAll(satisfiedSCPs, componentsWithStaticRefs);
					}
				}
				if (componentsWithStaticRefs != null) {
					instanceProcess.disposeInstances(componentsWithStaticRefs);
				}

				synchronized (syncLock) {
					resolvedComponents = resolveEligible();
					//no need to sync here
					target = selectDynamicBind(resolvedComponents, event.getServiceReference());

					// build the newly satisfied components
					removeAll(resolvedComponents, satisfiedSCPs);
					if (!resolvedComponents.isEmpty()) {
						addAll(satisfiedSCPs, resolvedComponents);
					}
				}

				//do synchronous bind
				if (target != null) {
					instanceProcess.dynamicBind((Vector) target);
				}

				if (!resolvedComponents.isEmpty()) {
					instanceProcess.buildComponents(resolvedComponents, false);
				}

				break;
			case ServiceEvent.UNREGISTERING :
				Vector newlyUnsatisfiedSCPs;
				synchronized (syncLock) {
					newlyUnsatisfiedSCPs = (Vector) satisfiedSCPs.clone();
					removeAll(newlyUnsatisfiedSCPs, resolveEligible());
					if (!newlyUnsatisfiedSCPs.isEmpty()) {
						removeAll(satisfiedSCPs, newlyUnsatisfiedSCPs);
					}
				}
				if (!newlyUnsatisfiedSCPs.isEmpty()) {
					// synchronously dispose newly unsatisfied components
					instanceProcess.disposeInstances(newlyUnsatisfiedSCPs);
				}

				Vector componentsToDispose;
				Vector newlySatisfiedSCPs = null;
				synchronized (syncLock) {
					//check for components with static reference to this service
					componentsToDispose = selectStaticUnBind(satisfiedSCPs, event.getServiceReference());
					if (componentsToDispose != null) {
						removeAll(satisfiedSCPs, componentsToDispose);
					}
				}
				//dispose instances from staticUnbind
				if (componentsToDispose != null) {
					instanceProcess.disposeInstances(componentsToDispose);
				}

				synchronized (syncLock) {
					// Pass in the set of currently resolved components, check each one -
					// do we need to unbind
					target = selectDynamicUnBind(satisfiedSCPs, event.getServiceReference());

					if (componentsToDispose != null) {
						// some components with static references were disposed. Try to build them again
						// get list of newly satisfied SCPs and build them
						newlySatisfiedSCPs = resolveEligible();
						removeAll(newlySatisfiedSCPs, satisfiedSCPs);
						if (!newlySatisfiedSCPs.isEmpty()) {
							addAll(satisfiedSCPs, newlySatisfiedSCPs);
						}
					}
				}

				instanceProcess.dynamicUnBind((Hashtable) target); // do synchronous unbind

				if (newlySatisfiedSCPs != null && !newlySatisfiedSCPs.isEmpty()) {
					instanceProcess.buildComponents(newlySatisfiedSCPs, false);
				}

				return;

			case ServiceEvent.MODIFIED :
				synchronized (syncLock) {
					// check for newly unsatisfied components and synchronously
					// dispose them
					newlyUnsatisfiedSCPs = (Vector) satisfiedSCPs.clone();
					removeAll(newlyUnsatisfiedSCPs, resolveEligible());
					if (!newlyUnsatisfiedSCPs.isEmpty()) {
						removeAll(satisfiedSCPs, newlyUnsatisfiedSCPs);
					}
				}

				if (!newlyUnsatisfiedSCPs.isEmpty()) {
					instanceProcess.disposeInstances(newlyUnsatisfiedSCPs);
				}

				synchronized (syncLock) {
					//check for components with static reference to this service
					componentsToDispose = selectStaticUnBind(satisfiedSCPs, event.getServiceReference());
					if (componentsToDispose != null) {
						removeAll(satisfiedSCPs, componentsToDispose);
					}
				}

				if (componentsToDispose != null) {
					instanceProcess.disposeInstances(componentsToDispose);
				}

				synchronized (syncLock) {
					// dynamic unbind
					// check each satisfied scp - do we need to unbind
					target = selectDynamicUnBind(satisfiedSCPs, event.getServiceReference());
				}

				if (target != null) {
					instanceProcess.dynamicUnBind((Hashtable) target);
				}

				synchronized (syncLock) {
					// dynamic bind
					target = selectDynamicBind(satisfiedSCPs, event.getServiceReference());

					// get list of newly satisfied SCPs and build them
					newlySatisfiedSCPs = resolveEligible();
					removeAll(newlySatisfiedSCPs, satisfiedSCPs);
					if (!newlySatisfiedSCPs.isEmpty()) {
						addAll(satisfiedSCPs, newlySatisfiedSCPs);
					}
				}

				if (target != null) {
					instanceProcess.dynamicBind((Vector) target);
				}
				if (!newlySatisfiedSCPs.isEmpty()) {
					instanceProcess.buildComponents(newlySatisfiedSCPs, false);
				}
		}
	}

	void buildNewlySatisfied() {
		Vector resolvedComponents;
		synchronized (syncLock) {
			findDependencyCycles();
			resolvedComponents = resolveEligible();
			removeAll(resolvedComponents, satisfiedSCPs);
			if (!resolvedComponents.isEmpty()) {
				addAll(satisfiedSCPs, resolvedComponents);
			}
		}

		if (!resolvedComponents.isEmpty()) {
			instanceProcess.buildComponents(resolvedComponents, false);
		}
	}

	/**
	 * Notifies the resolver that a component has been disposed. 
	 * It should accordingly update its data structures if needed
	 *
	 **/
	public void componentDisposed(ServiceComponentProp scp) {
		synchronized (syncLock) {
			int ind = satisfiedSCPs.indexOf(scp);
			if (ind >= 0) {
				satisfiedSCPs.removeElementAt(ind);
			}
		}
	}

	private Vector resolveEligible() {
		try {
			Vector enabledSCPs = (Vector) scpEnabled.clone();
			for (int k = enabledSCPs.size() - 1; k >= 0; k--) {
				ServiceComponentProp scp = (ServiceComponentProp) enabledSCPs.elementAt(k);
				Vector refs = scp.references;
				for (int i = 0; refs != null && i < refs.size(); i++) {
					// Loop though all the references (dependencies)for a given
					// scp. If a dependency is not met, remove it's associated
					// scp and re-run the algorithm
					Reference reference = (Reference) refs.elementAt(i);
					if (reference != null) {
						boolean resolved = !reference.isRequiredFor(scp.serviceComponent) || reference.hasProviders();

						if (!resolved) {
							if (Activator.DEBUG) {
								Activator.log.debug("Resolver.resolveEligible(): reference '" + reference.reference.name + "' of component '" + scp.name + "' is not resolved", null);
							}
							enabledSCPs.removeElementAt(k);
							break;
						} else if (scp.getState() == ServiceComponentProp.DISPOSED) {
							scp.setState(ServiceComponentProp.SATISFIED);
						}
					}
				}
				// check if the bundle providing the service has permission to
				// register the provided interface(s)
				if (scp.serviceComponent.provides != null && System.getSecurityManager() != null) {
					String[] provides = scp.serviceComponent.provides;
					boolean hasPermission = true;
					int i = 0;
					for (; i < provides.length; i++) {
						// make sure bundle has permission to register the service
						try {
							if (!scp.bc.getBundle().hasPermission(new ServicePermission(provides[i], ServicePermission.REGISTER))) {
								hasPermission = false;
								break;
							}
						} catch (IllegalStateException ise) {
							// the bundle of the service component is uninstalled
							// System.out.println("IllegalStateException occured
							// while processing component "+scp);
							// ise.printStackTrace();
							hasPermission = false;
							break;
						} catch (Throwable t) {
							// System.out.println("Exception occured processing
							// component "+scp);
							// t.printStackTrace();
							hasPermission = false;
							break;
						}
					}
					if (!hasPermission) {
						if (Activator.DEBUG) {
							Activator.log.debug("Resolver.resolveEligible(): Cannot satisfy component '" + scp.name + "' because its bundle does not have permissions to register service with interface " + provides[i], null);
						}
						scpEnabled.removeElementAt(k);
						enabledSCPs.removeElementAt(k);
						continue;
					}
				}
			}

			if (Activator.DEBUG) {
				Activator.log.debug(0, 10021, enabledSCPs.toString(), null, false);
				////Activator.log.debug("Resolver:resolveEligible(): resolved components = " + enabledSCPs, null);
			}
			return enabledSCPs;
		} catch (Throwable e) {
			Activator.log.error("[SCR] Unexpected exception occurred!", e);
			return null;
		}
	}

	// -- begin *disable* component routines
	/**
	 * Disable list of ComponentDescriptions
	 * 
	 * get all CD+P's from CD:CD+P Map get instances from CD+P:list of instance
	 * (1:n) map
	 * 
	 * Strip out of Map all CD+P's Continue to pull string check each Ref
	 * dependency and continue to pull out CD+P's if they become not eligible
	 * Then call Resolver to re-resolve
	 * 
	 * @param componentDescriptions
	 */
	void disableComponents(Vector componentDescriptions) {
		long start = 0l;
		if (Activator.PERF) {
			start = System.currentTimeMillis();
		}

		ServiceComponentProp scp;
		ServiceComponent component;

		Vector removeList = null;

		// Received list of CDs to disable
		if (componentDescriptions != null) {
			for (int i = 0; i < componentDescriptions.size(); i++) {
				// get the first CD
				component = (ServiceComponent) componentDescriptions.elementAt(i);
				component.enabled = false;
				if (Activator.DEBUG) {
					Activator.log.debug(0, 10022, component.name, null, false);
					////Activator.log.debug("Resolver.disableComponents()" + component.name, null);
				}

				// then get the list of SCPs for this CD
				Vector scpList = component.componentProps;
				if (scpList != null) {
					for (int iter = 0; iter < scpList.size(); iter++) {
						scp = (ServiceComponentProp) scpList.elementAt(iter);
						if (removeList == null) {
							removeList = new Vector();
						}
						removeList.addElement(scp);
					}
				}
				if (removeList != null) {
					disposeComponentConfigs(removeList);
					removeList.removeAllElements();
				}
				if (component.componentProps != null) {
					component.componentProps.removeAllElements();
				}
			}
		}

		Vector newlyUnsatisfiedSCPs;
		synchronized (syncLock) {
			// synchronously dispose newly unsatisfied components
			newlyUnsatisfiedSCPs = (Vector) satisfiedSCPs.clone();
			removeAll(newlyUnsatisfiedSCPs, resolveEligible());
			if (!newlyUnsatisfiedSCPs.isEmpty()) {
				removeAll(satisfiedSCPs, newlyUnsatisfiedSCPs);
			}
		}
		if (!newlyUnsatisfiedSCPs.isEmpty()) {
			instanceProcess.disposeInstances(newlyUnsatisfiedSCPs);
		}
		if (Activator.PERF) {
			start = System.currentTimeMillis() - start;
			Activator.log.info("[DS perf] " + componentDescriptions.size() + " Components disabled for " + start + " ms");
		}
	}

	public void disposeComponentConfigs(Vector scps) {
		// unregister, deactivate, and unbind
		synchronized (syncLock) {
			removeAll(satisfiedSCPs, scps);
			removeAll(scpEnabled, scps);
		}
		instanceProcess.disposeInstances(scps);
	}

	// -- end *service listener*

	public void performWork(int workAction, Object workObject) {
		try {
			if (Activator.DEBUG) {
				String work = WORK_TITLES[workAction - 1];
				Activator.log.debug(0, 10023, work + workObject, null, false);
				////Activator.log.debug("Resolver.dispatchWork(): " + work + workObject, null);
			}
			switch (workAction) {
				case BUILD :
					if (workObject != null) {
						Vector queue = (Vector) workObject;
						synchronized (syncLock) {
							// remove unsatisfied configs
							for (int i = queue.size() - 1; i >= 0; i--) {
								if (!satisfiedSCPs.contains(queue.elementAt(i))) {
									//System.out.println("-----BUILD: removing "+queue.elementAt(i));
									queue.removeElementAt(i);
								}
							}
							if (queue.isEmpty())
								return;
						}
						instanceProcess.buildComponents(queue, false);

						// dispose configs that were already tried to dispose while building
						Vector toDispose = null;
						synchronized (syncLock) {
							for (int i = queue.size() - 1; i >= 0; i--) {
								if (!satisfiedSCPs.contains(queue.elementAt(i))) {
									//System.out.println("-----DISPOSE after BUILD: removing "+queue.elementAt(i));
									if (toDispose == null) {
										toDispose = new Vector(2);
									}
									toDispose.addElement(queue.elementAt(i));
								}
							}
							if (toDispose == null)
								return; //nothing to dispose
						}
						instanceProcess.disposeInstances(toDispose);
					}
					break;
				case DYNAMICBIND :
					if (workObject != null) {
						Vector toBind = (Vector) workObject;
						synchronized (syncLock) {
							// remove unsatisfied configs
							for (int i = toBind.size() - 1; i >= 0; i--) {
								Reference ref = (Reference) toBind.elementAt(i);
								if (!satisfiedSCPs.contains(ref.scp)) {
									//System.out.println("--BIND: removing "+ref.scp);
									toBind.removeElementAt(i);
								}
							}
							if (toBind.isEmpty())
								return;
						}
						instanceProcess.dynamicBind(toBind);

					}
					break;
			}
		} catch (Throwable e) {
			Activator.log.error("[SCR] Unexpected exception occurred!", e);
		}
	}

	private Vector selectDynamicBind(Vector scps, ServiceReference serviceReference) {
		try {
			Vector toBind = null;
			for (int i = 0, size = scps.size(); i < size; i++) {
				ServiceComponentProp scp = (ServiceComponentProp) scps.elementAt(i);
				// if it is not already eligible it will bind with the static
				// scps
				Vector references = scp.references;
				// it is absolutely legal component if it doesn't contains
				// references!
				if (references != null) {
					for (int j = 0; j < references.size(); j++) {
						Reference reference = (Reference) references.elementAt(j);
						if (reference.bindNewReference(serviceReference, true)) {
							if (toBind == null) {
								toBind = new Vector(2);
							}
							toBind.addElement(reference);
						}
					}
				}
			}
			if (toBind != null && Activator.DEBUG) {
				Activator.log.debug(0, 10025, toBind.toString(), null, false);
				// //Activator.log.debug("Resolver.selectDynamicBind(): selected
				// = " + bindTable, null);
			}
			return toBind;
		} catch (Throwable t) {
			Activator.log.error("[SCR] Unexpected exception occurred!", t);
			return null;
		}
	}

	private Vector selectStaticBind(Vector scps, ServiceReference serviceReference) {
		try {
			Vector toBind = null;
			for (int i = 0, size = scps.size(); i < size; i++) {
				ServiceComponentProp scp = (ServiceComponentProp) scps.elementAt(i);
				if (scp.isComponentFactory()) {
					// the component factory configuration does not have to be reactivated
					continue;
				}
				// if it is not already eligible it will bind with the static
				// scps
				Vector references = scp.references;
				if (references != null) {
					for (int j = 0; j < references.size(); j++) {
						Reference reference = (Reference) references.elementAt(j);
						if (reference.bindNewReference(serviceReference, false)) {
							if (toBind == null) {
								toBind = new Vector(2);
							}
							toBind.addElement(scp);
							break;
						}
					}
				}
			}
			if (toBind != null && Activator.DEBUG) {
				Activator.log.debug(0, 10061, toBind.toString(), null, false);
				// //Activator.log.debug("Resolver.selectStaticBind(): selected
				// = " + toBind, null);
			}
			return toBind;
		} catch (Throwable t) {
			Activator.log.error("[SCR] Unexpected exception occurred!", t);
			return null;
		}
	}

	private Vector selectStaticUnBind(Vector scpsToCheck, ServiceReference serviceReference) {
		try {
			Vector toUnbind = null;
			for (int i = 0, size = scpsToCheck.size(); i < size; i++) {
				ServiceComponentProp scp = (ServiceComponentProp) scpsToCheck.elementAt(i);
				// if it is not already eligible it will bind with the static
				// scps
				Vector references = scp.references;
				// it is absolutely legal component if it doesn't contains
				// references!
				if (references != null) {
					for (int j = 0; j < references.size(); j++) {
						Reference reference = (Reference) references.elementAt(j);
						if (reference.staticUnbindReference(serviceReference)) {
							if (toUnbind == null) {
								toUnbind = new Vector(2);
							}
							toUnbind.addElement(scp);
						}
					}
				}
			}
			if (toUnbind != null)
				if (Activator.DEBUG) {
					Activator.log.debug(0, 10060, toUnbind.toString(), null, false);
					// //Activator.log.debug("Resolver.selectStaticUnBind():
					// selected = " + toUnbind, null);
				}
			return toUnbind;
		} catch (Throwable t) {
			Activator.log.error("[SCR] Unexpected exception occurred!", t);
			return null;
		}
	}

	/**
	 * selectDynamicUnBind Determine which resolved component description with
	 * properties need to unbind from this unregistering service Return map of
	 * reference description and component description with properties, for
	 * each.
	 * 
	 * @param scps
	 * @param serviceReference
	 * @return this is fairly complex to explain ;(
	 */
	private Hashtable selectDynamicUnBind(Vector scps, ServiceReference serviceReference) {
		try {
			if (Activator.DEBUG) {
				Activator.log.debug(0, 10026, null, null, false);
				// //Activator.log.debug("Resolver.selectDynamicUnBind():
				// entered", null);
			}
			Hashtable unbindTable = null; // ReferenceDescription:subTable
			Hashtable unbindSubTable = null; // scp:sr
			for (int i = 0; i < scps.size(); i++) {
				ServiceComponentProp scp = (ServiceComponentProp) scps.elementAt(i);

				Vector references = scp.references;
				// some components may not contain references and it is
				// absolutely valid
				if (references != null) {
					for (int j = 0; j < references.size(); j++) {
						Reference reference = (Reference) references.elementAt(j);
						// Does the scp require this service, use the Reference
						// object to check
						if (reference.dynamicUnbindReference(serviceReference)) {
							if (Activator.DEBUG) {
								Activator.log.debug(0, 10027, scp.toString(), null, false);
								// //Activator.log.debug("Resolver.selectDynamicUnBind():
								// unbinding " + scp, null);
							}
							if (unbindSubTable == null) {
								unbindSubTable = new Hashtable(11);
							}
							unbindSubTable.put(scp, serviceReference);
							if (unbindTable == null) {
								unbindTable = new Hashtable(11);
							}
							unbindTable.put(reference, unbindSubTable);
						} else {
							if (Activator.DEBUG) {
								Activator.log.debug("Resolver.selectDynamicUnBind(): not unbinding " + scp + " service ref=" + serviceReference, null);
							}
						}
					}
				}
			}
			if (unbindTable != null && Activator.DEBUG) {
				Activator.log.debug(0, 10028, unbindTable.toString(), null, false);
				// //Activator.log.debug("Resolver.selectDynamicUnBind():
				// unbindTable is " + unbindTable, null);
			}
			return unbindTable;
		} catch (Throwable t) {
			Activator.log.error("[SCR] Unexpected exception occurred!", t);
			return null;
		}
	}

	// used by the ComponentFactoryImpl to build new Component configurations
	public ServiceComponentProp mapNewFactoryComponent(ServiceComponent component, Dictionary configProperties) {
		synchronized (syncLock) {
			// create a new scp (adds to resolver enabledSCPs list)
			ServiceComponentProp newSCP = map(component, configProperties);
			newSCP.setComponentFactory(false); // avoid registration of new
			// ComponentFactory

			// we added a SCP, so check for circularity and mark cycles
			findDependencyCycles();

			// get list of newly satisfied SCPs and check whether the new SCP is
			// satisfied
			Vector newlySatisfiedSCPs = resolveEligible();
			removeAll(newlySatisfiedSCPs, satisfiedSCPs);
			if (!newlySatisfiedSCPs.contains(newSCP)) {
				scpEnabled.removeElement(newSCP);
				throw new ComponentException("Cannot resolve instance of " + newSCP + " with properties " + configProperties);
			}
			satisfiedSCPs.addElement(newSCP);

			return newSCP;
		}
	}

	/**
	 * Check through the enabled list for cycles. Cycles can only exist if every
	 * service is provided by a Service Component (not legacy OSGi). If the
	 * cycle has no optional dependencies, log an error and disable a Component
	 * Configuration in the cycle. If cycle can be "broken" by an optional
	 * dependency, make a note (stored in the
	 * {@link ServiceComponentProp#delayActivateSCPNames} Vector).
	 * 
	 * @throws CircularityException
	 *             if cycle exists with no optional dependencies
	 */
	private void findDependencyCycles() {
		Vector emptyVector = new Vector();
		try {
			// find the SCPs that resolve using other SCPs and record their
			// dependencies
			Hashtable dependencies = new Hashtable();

			for (int i = scpEnabled.size() - 1; i >= 0; i--) {
				ServiceComponentProp enabledSCP = (ServiceComponentProp) scpEnabled.elementAt(i);
				if (enabledSCP.references != null) {
					Vector dependencyVector = new Vector(1);
					for (int j = 0; j < enabledSCP.references.size(); j++) {
						Reference reference = (Reference) enabledSCP.references.elementAt(j);

						// see if it resolves to one of the other enabled SCPs
						ServiceComponentProp[] providerSCPs = reference.selectProviders(scpEnabled);
						if (providerSCPs != null) {
							for (int k = 0; k < providerSCPs.length; k++) {
								dependencyVector.addElement(new ReferenceSCPWrapper(reference, providerSCPs[k]));
							}
						}
					} // end for

					if (!dependencyVector.isEmpty()) {
						// SCP resolves using some other SCPs, could be a cycle
						dependencies.put(enabledSCP, dependencyVector);
					} else {
						dependencies.put(enabledSCP, emptyVector);
					}
				}
			} // end for

			// traverse dependency tree and look for cycles
			Hashtable visited = new Hashtable(11);
			Enumeration keys = dependencies.keys();

			while (keys.hasMoreElements()) {
				ServiceComponentProp scp = (ServiceComponentProp) keys.nextElement();
				if (!visited.containsKey(scp)) {
					Vector currentStack = new Vector(2);
					checkDependencies(scp, visited, dependencies, currentStack);
				}
			}
		} catch (CircularityException e) {
			Activator.log.error("[SCR] Circularity Exception found for component: " + e.getCausingComponent().serviceComponent, e);
			// disable offending SCP
			scpEnabled.removeElement(e.getCausingComponent());
			// try again
			findDependencyCycles();
		}
	}

	/**
	 * Recursively do a depth-first traversal of a dependency tree, looking for
	 * cycles.
	 * <p>
	 * If a cycle is found, calls
	 * {@link Resolver#processDependencyCycle(ReferenceSCPWrapper, Vector)}.
	 * </p>
	 * 
	 * @param scp
	 *            current node in dependency tree
	 * @param visited
	 *            holdes the visited nodes
	 * @param dependencies
	 *            Dependency tree - a Hashtable of ({@link ServiceComponentProp}):(Vector
	 *            of {@link ReferenceSCPWrapper}s)
	 * @param currentStack
	 *            Vector of {@link ReferenceSCPWrapper}s - the history of our
	 *            traversal so far (the path back to the root of the tree)
	 * @throws CircularityException
	 *             if an cycle with no optional dependencies is found.
	 */
	private void checkDependencies(ServiceComponentProp scp, Hashtable visited, Hashtable dependencies, Vector currentStack) throws CircularityException {

		// the component has already been visited and it's dependencies checked
		// for cycles
		if (visited.containsKey(scp)) {
			return;
		}

		Vector refSCPs = (Vector) dependencies.get(scp);
		if (refSCPs != null) {
			for (int i = 0; i < refSCPs.size(); i++) {
				ReferenceSCPWrapper refSCP = (ReferenceSCPWrapper) refSCPs.elementAt(i);
				if (currentStack.contains(refSCP)) {
					// may throw circularity exception
					processDependencyCycle(refSCP, currentStack);
					continue;
				}
				currentStack.addElement(refSCP);

				checkDependencies(refSCP.producer, visited, dependencies, currentStack);

				currentStack.removeElement(refSCP);
			}
		}
		visited.put(scp, "");
	}

	/**
	 * A cycle was detected. SCP is referenced by the last element in
	 * currentStack. Throws CircularityException if the cycle does not contain
	 * an optional dependency, else choses a point at which to "break" the cycle
	 * (the break point must be immediately after an optional dependency) and
	 * adds a "cycle note".
	 * 
	 * @see ServiceComponentProp#delayActivateSCPNames
	 */
	private void processDependencyCycle(ReferenceSCPWrapper refSCP, Vector currentStack) throws CircularityException {
		// find an optional dependency
		ReferenceSCPWrapper optionalRefSCP = null;
		for (int i = currentStack.indexOf(refSCP); i < currentStack.size(); i++) {
			ReferenceSCPWrapper cycleRefSCP = (ReferenceSCPWrapper) currentStack.elementAt(i);
			if (!cycleRefSCP.ref.isRequired()) {
				optionalRefSCP = cycleRefSCP;
				break;
			}
		}
		if (optionalRefSCP == null) {
			throw new CircularityException(refSCP.ref.scp);
		}
		// check whether the optional reference is static - this is not allowed
		// because of the way components with static refereces are built
		if (optionalRefSCP.ref.policy == ComponentReference.POLICY_STATIC) {
			Activator.log.error("[SCR] Static optional reference detected in a component cycle " + "and it will be removed.The referece is " + optionalRefSCP.ref.reference, null);

			optionalRefSCP.ref.scp.references.removeElement(optionalRefSCP.ref);
		}

		// the dependent component will be processed with delay whenever
		// necessary
		optionalRefSCP.ref.scp.setDelayActivateSCPName(optionalRefSCP.producer.serviceComponent.name);
	}

	// used to remove all elements of vector which occur in another vector
	private void removeAll(Vector src, Vector elementsToRemove) {
		for (int i = src.size() - 1; i >= 0; i--) {
			if (elementsToRemove.contains(src.elementAt(i))) {
				src.removeElementAt(i);
			}
		}
	}

	// used to add all elements of vector in another vector
	private void addAll(Vector src, Vector elementsToAdd) {
		for (int i = 0; i < elementsToAdd.size(); i++) {
			if (!src.contains(elementsToAdd.elementAt(i))) {
				src.addElement(elementsToAdd.elementAt(i));
			}
		}
	}

	public void removeFromSatisfiedList(ServiceComponentProp scp) {
		synchronized (syncLock) {
			satisfiedSCPs.remove(scp);
		}
	}

	/**
	 * Used to traverse the dependency tree in order to find cycles.
	 * 
	 */
	private class ReferenceSCPWrapper {
		public Reference ref;
		public ServiceComponentProp producer;

		protected ReferenceSCPWrapper(Reference ref, ServiceComponentProp producer) {
			this.ref = ref;
			this.producer = producer;
		}

		public String toString() {
			return "Reference : " + ref + " ::: SCP : " + producer;
		}
	}

}
