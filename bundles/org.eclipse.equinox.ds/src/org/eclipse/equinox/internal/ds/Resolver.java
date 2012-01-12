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
 *    Andrew Teirney		 - bug.id = 278732
 *    Simon Kaegi			 - bug.id = 296750
 *******************************************************************************/
package org.eclipse.equinox.internal.ds;

import java.util.*;
import org.apache.felix.scr.Component;
import org.eclipse.equinox.internal.ds.model.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentException;
import org.osgi.service.log.LogService;

/**
 * Resolver.java
 * 
 * @author Valentin Valchev
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 */
public final class Resolver implements WorkPerformer {

	// these strings are used only for debugging purpose
	static final String[] WORK_TITLES = {"BUILD ", "DYNAMICBIND ", "DISPOSE "}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	/**
	 * Service Component instances need to be built.
	 */
	public static final int BUILD = 1;

	/**
	 * Service Component instances need to be rebound
	 */
	public static final int DYNAMICBIND = 2;

	/**
	 * Service Component instances need to be disposed
	 */
	public static final int DISPOSE = 3;

	/* Holds the enabled SCPs*/
	protected Vector scpEnabled;

	private InstanceProcess instanceProcess;

	private Object syncLock = new Object();

	private Hashtable serviceReferenceTable = new Hashtable();

	public SCRManager mgr;

	// TODO: Add a hashtable connecting servicereference to a list of References
	// which they are bound to
	// This way the search of references to unbind becomes faster when there are
	// plenty of components.
	// Keep in mind that build process is asynchronous.

	static {
		/** preload some DS bundle classes to avoid classloader deadlocks */
		Reference.class.getName();
		SCRUtil.class.getName();
	}

	/**
	 * Resolver constructor
	 * 
	 */
	Resolver(SCRManager mgr) {
		scpEnabled = new Vector();
		//		satisfiedSCPs = new Vector();
		instanceProcess = new InstanceProcess(this);
		this.mgr = mgr;
	}

	void synchronizeServiceReferences() {
		synchronized (syncLock) {
			try {
				ServiceReference[] references = Activator.bc.getAllServiceReferences(null, null);
				serviceReferenceTable.clear();
				if (references != null) {
					for (int i = 0; i < references.length; i++) {
						serviceReferenceTable.put(references[i], Boolean.TRUE);
					}
				}
			} catch (InvalidSyntaxException e) {
				Activator.log(Activator.bc, LogService.LOG_WARNING, "Resolver(): " + NLS.bind(Messages.INVALID_TARGET_FILTER, ""), e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
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
			Activator.log.debug("Resolver.enableComponents(): " + (serviceComponents != null ? serviceComponents.toString() : "null"), null); //$NON-NLS-1$ //$NON-NLS-2$
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
							Activator.log.debug("Resolver.enableComponents(): ignoring not enabled component " + current.name, null); //$NON-NLS-1$
						}
						continue;
					}
					if (current.componentProps != null && current.componentProps.size() > 0) {
						//component is already enabled and processed. Skipping it.
						continue;
					}

					current.setState(Component.STATE_UNSATISFIED);

					if (current.getConfigurationPolicy() == ServiceComponent.CONF_POLICY_IGNORE) {
						//skip looking for configurations 
						map(current, (Dictionary) null);
						continue;
					}

					// check for a Configuration properties for this component
					try {
						String filter = "(|(" + Constants.SERVICE_PID + '=' + current.getConfigurationPID() + ")(" + ConfigurationAdmin.SERVICE_FACTORYPID + '=' + current.getConfigurationPID() + "))"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						configs = Activator.listConfigurations(filter);
					} catch (Exception e) {
						Activator.log(null, LogService.LOG_ERROR, NLS.bind(Messages.CANT_LIST_CONFIGURATIONS, current.name), e);
					}
					// if no Configuration
					if (configs == null || configs.length == 0) {
						if (current.getConfigurationPolicy() != ServiceComponent.CONF_POLICY_REQUIRE) {
							// create ServiceComponent + Prop
							map(current, (Dictionary) null);
						} else {
							String customReason = Activator.configAdmin != null ? "" : Messages.CONFIG_ADMIN_SERVICE_NOT_AVAILABLE; //$NON-NLS-1$
							if (Activator.DEBUG) {
								Activator.log.debug(NLS.bind(Messages.COMPONENT_REQURES_CONFIGURATION_ACTIVATION, current.name) + customReason, null);
							}
						}
					} else {
						// if ManagedServiceFactory
						Configuration config = configs[0];
						if (config.getFactoryPid() != null && config.getFactoryPid().equals(current.getConfigurationPID())) {
							// if ComponentFactory is specified
							if (current.factory != null) {
								Activator.log(current.bc, LogService.LOG_ERROR, NLS.bind(Messages.REGISTERED_AS_COMPONENT_AND_MANAGED_SERVICE_FACORY, current.name), null);
								continue; // skip current component
							}
							if (Activator.DEBUG) {
								Activator.log.debug("[SCR - Resolver] Resolver.enableComponents(): " + current.name + " as *managed service factory*", null); //$NON-NLS-1$ //$NON-NLS-2$
							}
							try {
								configs = Activator.listConfigurations("(service.factoryPid=" + config.getFactoryPid() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
							} catch (Exception e) {
								Activator.log(null, LogService.LOG_ERROR, NLS.bind(Messages.CANT_LIST_CONFIGURATIONS, current.name), e);
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
								Activator.log.debug("[SCR - Resolver] Resolver.enableComponents(): " + current.name + " as *service*", null); //$NON-NLS-1$ //$NON-NLS-2$
							} // if Service, not ManagedServiceFactory
							map(current, config);
						}
					} // end has configuration
				} // end process all components!
			}
		}

		buildNewlySatisfied(true);

		if (Activator.PERF) {
			start = System.currentTimeMillis() - start;
			Activator.log.info("[DS perf] " + (serviceComponents != null ? Integer.toString(serviceComponents.size()) : "") + " Components enabled for " + Long.toString(start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
				Activator.log.debug("Resolver.map(): Creating SCP for component " + component.name, null); //$NON-NLS-1$
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
			Activator.log(component.bc, LogService.LOG_ERROR, NLS.bind(Messages.ERROR_CREATING_SCP, component), t);
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

		if (Activator.DEBUG) {
			Activator.log.debug("Resolver.getEligible(): processing service event " + event.toString(), null); //$NON-NLS-1$
			String eventType = ""; //$NON-NLS-1$
			if (event.getType() == ServiceEvent.UNREGISTERING) {
				eventType = "UNREGISTERING"; //$NON-NLS-1$
			} else if (event.getType() == ServiceEvent.REGISTERED) {
				eventType = "REGISTERED"; //$NON-NLS-1$
			} else if (event.getType() == ServiceEvent.MODIFIED) {
				eventType = "MODIFIED"; //$NON-NLS-1$
			}
			Activator.log.debug("Service event type: " + eventType, null); //$NON-NLS-1$
		}

		Object target = null;
		Vector resolvedComponents = null;
		switch (event.getType()) {
			case ServiceEvent.REGISTERED :

				synchronized (syncLock) {
					serviceReferenceTable.put(event.getServiceReference(), Boolean.TRUE);
					if (scpEnabled.isEmpty())
						return; // check for any enabled configurations

					//check for any static references with policy option "greedy" that need to be bound with this service reference
					target = selectStaticBind(scpEnabled, event.getServiceReference());
				}

				if (target != null) {
					//dispose instances of components with static reference that needs to be bound with the service reference
					instanceProcess.disposeInstances((Vector) target, ComponentConstants.DEACTIVATION_REASON_REFERENCE);
				}

				synchronized (syncLock) {
					resolvedComponents = getComponentsToBuild();
					target = selectDynamicBind(scpEnabled, event.getServiceReference());
				}

				//do synchronous bind
				if (target != null) {
					Vector unboundRefs = instanceProcess.dynamicBind((Vector) target);
					if (unboundRefs != null) {
						// put delayed dynamic binds on the queue
						// (this is used to handle class circularity errors)
						mgr.enqueueWork(this, Resolver.DYNAMICBIND, unboundRefs, false);
					}
				}

				if (!resolvedComponents.isEmpty()) {
					instanceProcess.buildComponents(resolvedComponents, false);
				}

				break;
			case ServiceEvent.UNREGISTERING :
				Vector componentsToDispose;
				synchronized (syncLock) {
					//check for components with static reference to this service
					componentsToDispose = selectStaticUnBind(scpEnabled, event.getServiceReference(), false);
				}
				//dispose instances from staticUnbind
				if (componentsToDispose != null) {
					instanceProcess.disposeInstances(componentsToDispose, ComponentConstants.DEACTIVATION_REASON_REFERENCE);
				}

				Vector newlyUnsatisfiedSCPs;
				synchronized (syncLock) {
					serviceReferenceTable.remove(event.getServiceReference());
					if (scpEnabled.isEmpty())
						return; // check for any enabled configurations

					newlyUnsatisfiedSCPs = selectNewlyUnsatisfied(event.getServiceReference());
				}
				if (!newlyUnsatisfiedSCPs.isEmpty()) {
					// synchronously dispose newly unsatisfied components
					instanceProcess.disposeInstances(newlyUnsatisfiedSCPs, ComponentConstants.DEACTIVATION_REASON_REFERENCE);
				}

				synchronized (syncLock) {
					// Pass in the set of currently resolved components, check each one -
					// do we need to unbind
					target = selectDynamicUnBind(scpEnabled, event.getServiceReference(), false);

					if (componentsToDispose != null || !newlyUnsatisfiedSCPs.isEmpty()) {
						// some components with static references were disposed. Try to build them again
						// get list of newly satisfied SCPs and build them
						resolvedComponents = getComponentsToBuild();
					}
				}

				instanceProcess.dynamicUnBind((Hashtable) target); // do synchronous unbind

				if (resolvedComponents != null && !resolvedComponents.isEmpty()) {
					instanceProcess.buildComponents(resolvedComponents, false);
				}

				return;

			case ServiceEvent.MODIFIED :
				synchronized (syncLock) {
					if (scpEnabled.isEmpty())
						return; // check for any enabled configurations

					// check for newly unsatisfied components and synchronously
					// dispose them
					newlyUnsatisfiedSCPs = selectNewlyUnsatisfied(event.getServiceReference());
				}

				if (!newlyUnsatisfiedSCPs.isEmpty()) {
					instanceProcess.disposeInstances(newlyUnsatisfiedSCPs, ComponentConstants.DEACTIVATION_REASON_REFERENCE);
				}

				synchronized (syncLock) {
					//check for components with static reference to this service
					componentsToDispose = selectStaticUnBind(scpEnabled, event.getServiceReference(), true);
				}

				if (componentsToDispose != null) {
					instanceProcess.disposeInstances(componentsToDispose, ComponentConstants.DEACTIVATION_REASON_REFERENCE);
				}

				synchronized (syncLock) {
					//check for any static references with policy option "greedy" that need to be bound with this service reference
					componentsToDispose = selectStaticBind(scpEnabled, event.getServiceReference());
				}

				if (componentsToDispose != null) {
					//dispose instances of components with static reference that needs to be bound with the modified service reference
					instanceProcess.disposeInstances(componentsToDispose, ComponentConstants.DEACTIVATION_REASON_REFERENCE);
				}

				Hashtable referencesToUpdate = null;
				synchronized (syncLock) {
					// dynamic unbind
					// check each satisfied scp - do we need to unbind
					target = selectDynamicUnBind(scpEnabled, event.getServiceReference(), true);

					//check references that need to be updated
					referencesToUpdate = selectReferencesToUpdate(scpEnabled, event.getServiceReference());
				}

				if (target != null) {
					instanceProcess.dynamicUnBind((Hashtable) target);
				}
				if (referencesToUpdate != null) {
					instanceProcess.referencePropertiesUpdated(referencesToUpdate);
				}

				synchronized (syncLock) {
					// dynamic bind
					target = selectDynamicBind(scpEnabled, event.getServiceReference());

					// get list of newly satisfied SCPs and build them
					resolvedComponents = getComponentsToBuild();
				}

				if (target != null) {
					Vector unboundRefs = instanceProcess.dynamicBind((Vector) target);
					if (unboundRefs != null) {
						// put delayed dynamic binds on the queue
						// (this is used to handle class circularity errors)
						mgr.enqueueWork(this, Resolver.DYNAMICBIND, unboundRefs, false);
					}
				}
				if (!resolvedComponents.isEmpty()) {
					instanceProcess.buildComponents(resolvedComponents, false);
				}
		}
	}

	public void buildNewlySatisfied(boolean checkForDependencyCycles) {
		Vector resolvedComponents;
		synchronized (syncLock) {
			if (checkForDependencyCycles) {
				findDependencyCycles();
			}
			resolvedComponents = getComponentsToBuild();
		}

		if (!resolvedComponents.isEmpty()) {
			instanceProcess.buildComponents(resolvedComponents, false);
		}
	}

	private Vector getComponentsToBuild() {
		Vector resolvedComponents = resolveEligible();
		// select the satisfied components only
		ServiceComponentProp scp;
		for (int i = resolvedComponents.size() - 1; i >= 0; i--) {
			scp = (ServiceComponentProp) resolvedComponents.elementAt(i);
			if (scp.getState() != Component.STATE_UNSATISFIED) {
				resolvedComponents.removeElementAt(i);
			}
		}
		return resolvedComponents;
	}

	/**
	 * Notifies the resolver that a component has been disposed. 
	 * It should accordingly update its data structures if needed
	 *
	 **/
	public void componentDisposed(ServiceComponentProp scp) {
		//
	}

	private Vector resolveEligible() {
		try {
			Vector enabledSCPs = (Vector) scpEnabled.clone();
			for (int k = enabledSCPs.size() - 1; k >= 0; k--) {
				ServiceComponentProp scp = (ServiceComponentProp) enabledSCPs.elementAt(k);
				try {
					Vector refs = scp.references;
					for (int i = 0; refs != null && i < refs.size(); i++) {
						// Loop though all the references (dependencies)for a given
						// scp. If a dependency is not met, remove it's associated scp and
						// re-run the algorithm
						Reference reference = (Reference) refs.elementAt(i);
						if (reference != null) {
							boolean resolved = !reference.isRequiredFor(scp.serviceComponent) || reference.hasProviders(this.serviceReferenceTable);

							if (!resolved) {
								if (Activator.DEBUG) {
									Activator.log.debug("Resolver.resolveEligible(): reference '" + reference.reference.name + "' of component '" + scp.name + "' is not resolved", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								}
								enabledSCPs.removeElementAt(k);
								break;
							}
						}
					}
				} catch (IllegalStateException ise) {
					//the bundle of the scp is probably already uninstalled
					scpEnabled.removeElementAt(k);
					enabledSCPs.removeElementAt(k);
					continue;
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
						Activator.log(null, LogService.LOG_WARNING, NLS.bind(Messages.COMPONENT_LACKS_APPROPRIATE_PERMISSIONS, scp.name, provides[i]), null);
						removeEnabledSCP(scp);
						enabledSCPs.removeElementAt(k);
						continue;
					}
				}
				if (!scp.isBuilt() && !(scp.getState() == Component.STATE_DEACTIVATING)) {
					scp.setState(Component.STATE_UNSATISFIED);
				}
			}

			if (Activator.DEBUG) {
				Activator.log.debug("Resolver.resolveEligible(): resolved components = " + enabledSCPs.toString(), null); //$NON-NLS-1$
			}
			return enabledSCPs;
		} catch (Throwable e) {
			Activator.log(null, LogService.LOG_ERROR, Messages.UNEXPECTED_EXCEPTION, e);
			return new Vector();
		}
	}

	private Vector selectNewlyUnsatisfied(ServiceReference serviceRef) {
		try {
			Vector result = (Vector) scpEnabled.clone();
			for (int k = result.size() - 1; k >= 0; k--) {
				ServiceComponentProp scp = (ServiceComponentProp) result.elementAt(k);
				Vector refs = scp.references;
				boolean toDispose = false;
				for (int i = 0; refs != null && i < refs.size(); i++) {
					// Loop though all the references (dependencies)for a given
					// scp. If a dependency is not met, remove it's associated
					// scp and re-run the algorithm
					Reference reference = (Reference) refs.elementAt(i);
					if (reference != null) {
						if (serviceRef != null && reference.reference.bind != null && scp.getState() == Component.STATE_ACTIVE && !(reference.dynamicUnbindReference(serviceRef) || reference.staticUnbindReference(serviceRef))) {
							//make quick test - the service reference is not bound to the current component reference
							continue;
						}
						if (serviceRef != null && !isPossibleMatch(reference, serviceRef)) {
							// the service reference is not a possible match. Skipping further checks 
							continue;
						}
						boolean resolved = !reference.isRequiredFor(scp.serviceComponent) || reference.hasProviders(this.serviceReferenceTable);

						if (!resolved && scp.isBuilt()) {
							if (Activator.DEBUG) {
								Activator.log.debug("Resolver.selectNewlyUnsatisfied(): reference '" + reference.reference.name + "' of component '" + scp.name + "' is not resolved", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							}
							toDispose = true;
							break;
						}
					}
				}
				if (!toDispose) {
					result.removeElementAt(k);
				}
			}
			return result;
		} catch (Throwable e) {
			Activator.log(null, LogService.LOG_ERROR, Messages.UNEXPECTED_EXCEPTION, e);
			return new Vector(1);
		}
	}

	private boolean isPossibleMatch(Reference reference, ServiceReference serviceRef) {
		String[] serviceNames = (String[]) serviceRef.getProperty(Constants.OBJECTCLASS);
		boolean hasName = false;
		for (int i = 0; i < serviceNames.length; i++) {
			if (serviceNames[i].equals(reference.interfaceName)) {
				hasName = true;
				break;
			}
		}
		if (!hasName) {
			return false;
		}
		// check target filter
		try {
			Filter filter = FrameworkUtil.createFilter(reference.target);
			if (!filter.match(serviceRef)) {
				return false;
			}
		} catch (InvalidSyntaxException e) {
			return false;
		}
		return true;
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
	void disableComponents(Vector componentDescriptions, int deactivateReason) {
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
				component.setState(Component.STATE_DISABLED);
				if (Activator.DEBUG) {
					Activator.log.debug("Resolver.disableComponents() " + component.name, null); //$NON-NLS-1$
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
					disposeComponentConfigs(removeList, deactivateReason);
					removeList.removeAllElements();
				}
				if (component.componentProps != null) {
					for (int j = 0; j < component.componentProps.size(); j++) {
						scp = (ServiceComponentProp) component.componentProps.elementAt(j);
						scp.setState(Component.STATE_DISPOSED);
					}
					component.componentProps.removeAllElements();
				}
			}
		}

		if (Activator.PERF) {
			start = System.currentTimeMillis() - start;
			Activator.log.info("[DS perf] " + Integer.toString(componentDescriptions.size()) + " Components disabled for " + Long.toString(start) + "ms"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	public void disposeComponentConfigs(Vector scps, int deactivateReason) {
		// unregister, deactivate, and unbind
		synchronized (syncLock) {
			removeAll(scpEnabled, scps);
		}
		instanceProcess.disposeInstances(scps, deactivateReason);
	}

	// -- end *service listener*

	public void performWork(int workAction, Object workObject) {
		try {
			if (Activator.DEBUG) {
				String work = WORK_TITLES[workAction - 1];
				Activator.log.debug("Resolver.performWork(): " + work + workObject, null); //$NON-NLS-1$
			}
			switch (workAction) {
				case BUILD :
					if (workObject != null) {
						instanceProcess.buildComponents((Vector) workObject, false);
					}
					break;
				case DYNAMICBIND :
					if (workObject != null) {
						Vector toBind = (Vector) workObject;
						synchronized (syncLock) {
							// remove unsatisfied configs
							for (int i = toBind.size() - 1; i >= 0; i--) {
								Reference ref = (Reference) toBind.elementAt(i);
								if (ref.scp.isUnsatisfied()) {
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
				case DISPOSE :
					if (workObject != null) {
						instanceProcess.disposeInstances((Vector) workObject, ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED);
					}
					break;
			}
		} catch (Throwable e) {
			Activator.log(null, LogService.LOG_ERROR, Messages.UNEXPECTED_EXCEPTION, e);
		}
	}

	private Vector selectDynamicBind(Vector scps, ServiceReference serviceReference) {
		try {
			Vector toBind = null;
			for (int i = 0, size = scps.size(); i < size; i++) {
				ServiceComponentProp scp = (ServiceComponentProp) scps.elementAt(i);
				if (scp.isUnsatisfied()) {
					//do not check disposed components
					continue;
				}
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
				Activator.log.debug("Resolver.selectDynamicBind(): selected = " + toBind.toString(), null); //$NON-NLS-1$
			}
			return toBind;
		} catch (Throwable t) {
			Activator.log(null, LogService.LOG_ERROR, Messages.UNEXPECTED_EXCEPTION, t);
			return null;
		}
	}

	//Returns the components with static reference that needs to be bound with the service reference. 
	//This can happen if the static reference has policy option "greedy"
	private Vector selectStaticBind(Vector scps, ServiceReference serviceReference) {
		try {
			Vector toBind = null;
			for (int i = 0, size = scps.size(); i < size; i++) {
				ServiceComponentProp scp = (ServiceComponentProp) scps.elementAt(i);
				if (scp.isUnsatisfied()) {
					//do not check disposed components
					continue;
				}
				Vector references = scp.references;
				if (references != null) {
					for (int j = 0; j < references.size(); j++) {
						Reference reference = (Reference) references.elementAt(j);
						if (reference.bindNewReference(serviceReference, false)) {
							if (toBind == null) {
								toBind = new Vector(2);
							}
							toBind.addElement(reference);
						}
					}
				}
			}
			Vector result = null;
			Reference ref = null;
			if (toBind != null) {
				result = new Vector();
				for (int i = 0; i < toBind.size(); i++) {
					ref = (Reference) toBind.elementAt(i);
					if (!result.contains(ref.scp)) {
						result.addElement(ref.scp);
					}
				}
			}

			if (result != null && Activator.DEBUG) {
				Activator.log.debug("Resolver.selectStaticBind(): selected = " + result.toString(), null); //$NON-NLS-1$
			}
			return result;
		} catch (Throwable t) {
			Activator.log(null, LogService.LOG_ERROR, Messages.UNEXPECTED_EXCEPTION, t);
			return null;
		}
	}

	private Vector selectStaticUnBind(Vector scpsToCheck, ServiceReference serviceReference, boolean checkSatisfied) {
		try {
			Vector toUnbind = null;
			for (int i = 0, size = scpsToCheck.size(); i < size; i++) {
				ServiceComponentProp scp = (ServiceComponentProp) scpsToCheck.elementAt(i);
				if (scp.isUnsatisfied()) {
					//the scp is already deactivated
					continue;
				}
				// if it is not already eligible it will bind with the static scps
				Vector references = scp.references;
				// it is absolutely legal component if it doesn't contains references!
				if (references != null) {
					for (int j = 0; j < references.size(); j++) {
						Reference reference = (Reference) references.elementAt(j);
						if (reference.staticUnbindReference(serviceReference)) {
							if (checkSatisfied && reference.isInSatisfiedList(serviceReference)) {
								//the service reference do still satisfy the reference and shall not be unbound
								continue;
							}
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
					Activator.log.debug("Resolver.selectStaticUnBind(): selected = " + toUnbind.toString(), null); //$NON-NLS-1$
				}
			return toUnbind;
		} catch (Throwable t) {
			Activator.log(null, LogService.LOG_ERROR, Messages.UNEXPECTED_EXCEPTION, t);
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
	private Hashtable selectDynamicUnBind(Vector scps, ServiceReference serviceReference, boolean checkSatisfied) {
		try {
			if (Activator.DEBUG) {
				Activator.log.debug("Resolver.selectDynamicUnBind(): entered", null); //$NON-NLS-1$
			}
			Hashtable unbindTable = null; // ReferenceDescription:subTable
			for (int i = 0; i < scps.size(); i++) {
				Hashtable unbindSubTable = null; // scp:sr
				ServiceComponentProp scp = (ServiceComponentProp) scps.elementAt(i);

				if (scp.isUnsatisfied()) {
					//do not check deactivated components
					continue;
				}
				Vector references = scp.references;
				// some components may not contain references and it is
				// absolutely valid
				if (references != null) {
					for (int j = 0; j < references.size(); j++) {
						Reference reference = (Reference) references.elementAt(j);
						// Does the scp require this service, use the Reference
						// object to check
						if (reference.dynamicUnbindReference(serviceReference)) {
							if (checkSatisfied && reference.isInSatisfiedList(serviceReference)) {
								//the service reference do still satisfy the reference and shall not be unbound
								continue;
							}
							if (Activator.DEBUG) {
								Activator.log.debug("Resolver.selectDynamicUnBind(): unbinding " + scp.toString(), null); //$NON-NLS-1$
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
								Activator.log.debug("Resolver.selectDynamicUnBind(): not unbinding " + scp + " service ref=" + serviceReference, null); //$NON-NLS-1$ //$NON-NLS-2$
							}
						}
					}
				}
			}
			if (unbindTable != null && Activator.DEBUG) {
				Activator.log.debug("Resolver.selectDynamicUnBind(): unbindTable is " + unbindTable.toString(), null); //$NON-NLS-1$
			}
			return unbindTable;
		} catch (Throwable t) {
			Activator.log(null, LogService.LOG_ERROR, Messages.UNEXPECTED_EXCEPTION, t);
			return null;
		}
	}

	/**
	 * Determine which component references needs to be updated by their specified updated method due to the current service references properties change
	 * 
	 * @param scps
	 * @param serviceReference
	 * @return Map of <Reference>:<Map of <ServiceComponentProp>:<ServiceReference>>
	 * 
	 */
	private Hashtable selectReferencesToUpdate(Vector scps, ServiceReference serviceReference) {
		try {
			if (Activator.DEBUG) {
				Activator.log.debug("Resolver.selectReferencesToUpdate(): entered", null); //$NON-NLS-1$
			}
			Hashtable referencesTable = null;
			for (int i = 0; i < scps.size(); i++) {
				Hashtable updateSubTable = null;
				ServiceComponentProp scp = (ServiceComponentProp) scps.elementAt(i);

				if (scp.isUnsatisfied() || !scp.serviceComponent.isNamespaceAtLeast12()) {
					//do not check deactivated components or components which are not DS 1.2 compliant
					continue;
				}
				Vector references = scp.references;
				if (references != null) {
					for (int j = 0; j < references.size(); j++) {
						Reference reference = (Reference) references.elementAt(j);
						if (reference.reference.updated == null) {
							//the reference does not have updated method specified
							continue;
						}
						if (reference.isStatic() ? reference.staticUnbindReference(serviceReference) : reference.dynamicUnbindReference(serviceReference)) {
							if (Activator.DEBUG) {
								Activator.log.debug("Resolver.selectReferencesToUpdate(): selected for update reference " + reference.reference.name + " of component " + scp.toString(), null); //$NON-NLS-1$ //$NON-NLS-2$
							}
							if (updateSubTable == null) {
								updateSubTable = new Hashtable(11);
							}
							updateSubTable.put(scp, serviceReference);
							if (referencesTable == null) {
								referencesTable = new Hashtable(11);
							}
							referencesTable.put(reference, updateSubTable);
						}
					}
				}
			}
			if (referencesTable != null && Activator.DEBUG) {
				Activator.log.debug("Resolver.selectReferencesToUpdate(): referencesTable is " + referencesTable.toString(), null); //$NON-NLS-1$
			}
			return referencesTable;
		} catch (Throwable t) {
			Activator.log(null, LogService.LOG_ERROR, Messages.UNEXPECTED_EXCEPTION, t);
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
			Vector eligibleSCPs = resolveEligible();
			if (!eligibleSCPs.contains(newSCP)) {
				removeEnabledSCP(newSCP);
				throw new ComponentException(NLS.bind(Messages.CANT_RESOLVE_COMPONENT_INSTANCE, newSCP, configProperties));
			}
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
			Activator.log(e.getCausingComponent().serviceComponent.bc, LogService.LOG_ERROR, NLS.bind(Messages.CIRCULARITY_EXCEPTION_FOUND, e.getCausingComponent().serviceComponent), e);
			// disable offending SCP
			removeEnabledSCP(e.getCausingComponent());
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
		visited.put(scp, ""); //$NON-NLS-1$
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
			Activator.log(optionalRefSCP.ref.scp.bc, LogService.LOG_ERROR, NLS.bind(Messages.STATIC_OPTIONAL_REFERENCE_TO_BE_REMOVED, optionalRefSCP.ref.reference), null);

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

	private void removeEnabledSCP(ServiceComponentProp scp) {
		scpEnabled.removeElement(scp);
		scp.serviceComponent.componentProps.remove(scp);
		scp.setState(Component.STATE_DISPOSED);
	}

	/**
	 * Reorder the specified SCP and place it at the end of the enabledSCPs list
	 * @param scp the SCP to reorder
	 */
	protected void reorderSCP(ServiceComponentProp scp) {
		synchronized (syncLock) {
			if (scpEnabled.removeElement(scp)) {
				scpEnabled.addElement(scp);
			}
		}
	}

	public void removeFromSatisfiedList(ServiceComponentProp scp) {
		Vector tmp = new Vector();
		tmp.addElement(scp);
		mgr.enqueueWork(this, Resolver.DISPOSE, tmp, false);
	}

	/**
	 * Used to traverse the dependency tree in order to find cycles.
	 * 
	 */
	private static class ReferenceSCPWrapper {
		public Reference ref;
		public ServiceComponentProp producer;

		protected ReferenceSCPWrapper(Reference ref, ServiceComponentProp producer) {
			this.ref = ref;
			this.producer = producer;
		}

		public String toString() {
			return "Reference : " + ref + " ::: SCP : " + producer; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public Component getComponent(long componentId) {
		synchronized (scpEnabled) {
			for (int i = 0; i < scpEnabled.size(); i++) {
				ServiceComponentProp scp = (ServiceComponentProp) scpEnabled.elementAt(i);
				if (scp.getId() == componentId) {
					return scp;
				}
			}
		}
		return null;
	}

}
