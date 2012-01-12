/*******************************************************************************
 * Copyright (c) 1997-2010 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *    Andrew Teirney		 - bug.id = 278732
 *******************************************************************************/
package org.eclipse.equinox.internal.ds;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import org.apache.felix.scr.Component;
import org.eclipse.equinox.internal.ds.model.*;
import org.eclipse.equinox.internal.util.event.Queue;
import org.eclipse.equinox.internal.util.threadpool.ThreadPoolManager;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentException;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Acts as a "Bundle Manager" - a listener for bundle events. Whenever a bundle
 * is stopped or started it will invoke the resolver to respectively enable or
 * disable the contained components. Notice, the SynchronousBundleListener
 * bundle listeners are called prior bundle event is completed.
 * 
 * It is also a listener for update and delete configuration events, sent by
 * Configuration Admin, thus handling the changes in configurations which are meant 
 * to configure the properties of DS components
 * 
 * @author Maria Ivanova
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 */

public class SCRManager implements ServiceListener, SynchronousBundleListener, ConfigurationListener, WorkPerformer, PrivilegedAction {

	/** work action type */
	public final int ENABLE_COMPONENTS = 1;
	public final int DISABLE_COMPONENTS = 2;

	protected Hashtable bundleToServiceComponents;
	protected Hashtable processingBundles = new Hashtable(5);
	protected Queue queue;
	private Resolver resolver;

	private WorkThread workThread;
	protected boolean running = false;
	protected boolean stopped = false;
	private ServiceTracker threadPoolManagerTracker;
	private boolean hasRegisteredServiceListener = false;
	private ComponentStorage storage;

	/**
	 * Constructs the SCRManager.
	 */
	public SCRManager() {
		hasRegisteredServiceListener = true;
		queue = new Queue(10);
		if (Activator.startup)
			Activator.timeLog("Queue instantiated for "); //$NON-NLS-1$

		threadPoolManagerTracker = new ServiceTracker(Activator.bc, ThreadPoolManager.class.getName(), null);
		threadPoolManagerTracker.open();
		if (Activator.startup)
			Activator.timeLog("Threadpool service tracker opened for "); //$NON-NLS-1$

		resolver = new Resolver(this);
		if (Activator.startup)
			Activator.timeLog("Resolver instantiated for "); //$NON-NLS-1$

		resolver.synchronizeServiceReferences();
		if (Activator.startup)
			Activator.timeLog("resolver.synchronizeServiceReferences() method took "); //$NON-NLS-1$

		String storageClass = Activator.bc.getProperty("scr.storage.class"); //$NON-NLS-1$
		if (storageClass == null) {
			storageClass = "org.eclipse.equinox.internal.ds.storage.file.FileStorage"; //$NON-NLS-1$
		}
		try {
			storage = (ComponentStorage) Class.forName(storageClass).getConstructor(new Class[] {BundleContext.class}).newInstance(new Object[] {Activator.bc});
		} catch (Exception e) {
			Activator.log(null, LogService.LOG_ERROR, NLS.bind(Messages.COULD_NOT_CREATE_INSTANCE, storageClass), e);
		}
		if (Activator.startup)
			Activator.timeLog("Creating storage took "); //$NON-NLS-1$

		Activator.bc.addBundleListener(this);
	}

	public void startIt() {
		// loop through the currently installed bundles
		Bundle[] bundles = Activator.bc.getBundles();
		if (bundles != null) {
			for (int i = 0; i < bundles.length; i++) {
				Bundle current = bundles[i];
				// try to process the active ones.
				if (current.getState() == Bundle.ACTIVE) {
					startedBundle(current);
				} else if (current.getState() == Bundle.STARTING) {
					String lazy = (String) current.getHeaders("").get(Constants.BUNDLE_ACTIVATIONPOLICY); //$NON-NLS-1$
					if (lazy != null && lazy.indexOf(Constants.ACTIVATION_LAZY) >= 0) {
						startedBundle(current);
					}
				}
			}
		}
	}

	/**
	 * Add an event to the queue. The event will be forwarded to target service
	 * as soon as possible.
	 * 
	 * @param upEv
	 *            event, holding info for update/deletion of a configuration.
	 */
	public void addEvent(Object upEv, boolean securityCall) {
		try {
			synchronized (queue) {
				queue.put(upEv);
				if (!running) {
					if (queue.size() > 0) {
						running = true;
						workThread = new WorkThread(this);
						if (securityCall) {
							AccessController.doPrivileged(this);
							return;
						}
						ThreadPoolManager threadPool = (ThreadPoolManager) threadPoolManagerTracker.getService();
						if (threadPool != null) {
							threadPool.execute(workThread, Thread.MAX_PRIORITY, "Component Resolve Thread"); //$NON-NLS-1$
						} else {
							new Thread(workThread, "Component Resolve Thread").start(); //$NON-NLS-1$
						}
					}
				} else if (workThread.waiting > 0) {
					queue.notifyAll();
				}
			}
		} catch (Throwable e) {
			Activator.log(null, LogService.LOG_ERROR, Messages.UNEXPECTED_EXCEPTION, e);
		}
	}

	public Object run() {
		ThreadPoolManager threadPool = (ThreadPoolManager) threadPoolManagerTracker.getService();
		if (threadPool != null) {
			threadPool.execute(workThread, Thread.MAX_PRIORITY, "Component Resolve Thread"); //$NON-NLS-1$
		} else {
			new Thread(workThread, "Component Resolve Thread").start(); //$NON-NLS-1$
		}
		return null;
	}

	public void queueBlocked() {
		resolver.queueBlocked();
		synchronized (queue) {
			running = false;
			addEvent(null, Activator.security); // will result in starting new
			// WorkThread to process the queued work
		}
	}

	/**
	 * This methods takes the input parameters and creates a Queued object and
	 * queues it. The thread is notified.
	 * 
	 * @param d
	 *            Dispatcher for this item
	 * @param a
	 *            Action for this item
	 * @param o
	 *            Object for this item
	 * @param securityCall specifies whether to use security privileged call
	 */
	public void enqueueWork(WorkPerformer d, int a, Object o, boolean securityCall) {
		addEvent(new QueuedJob(d, a, o), securityCall);
	}

	/**
	 * Stops this thread, making it getting out of method run.
	 */
	public void stopIt() {
		stopped = true;
		disposeBundles();
		if (queue != null) {
			queue.clear();
		}
		if (running) {
			synchronized (queue) {
				queue.notify();
			}
			int counter = 0;

			while (running && counter < 20) {
				// wait maximum 2 seconds to complete current task in the queue
				try {
					Thread.sleep(100);
				} catch (InterruptedException ie) {
					//nothing to do
				}
				counter++;
			}
		}
		stopped = true;
		threadPoolManagerTracker.close();
		storage.stop();
	}

	public void serviceChanged(ServiceEvent sEv) {

		resolver.getEligible(sEv);
	}

	public final void bundleChanged(BundleEvent event) {
		long start = 0l;
		if (Activator.PERF) {
			start = System.currentTimeMillis();
			Activator.log.info("[DS perf] Started processing bundle event " + event); //$NON-NLS-1$
		}
		int type = event.getType();
		if (type == BundleEvent.STOPPING) {
			stoppingBundle(event.getBundle());
		} else if (type == BundleEvent.STARTED) {
			startedBundle(event.getBundle());
		} else if (type == BundleEvent.LAZY_ACTIVATION) {
			startedBundle(event.getBundle());
		} else if (type == BundleEvent.UNINSTALLED && Activator.DBSTORE) {
			storage.deleteComponentDefinitions(event.getBundle().getBundleId());
		}
		if (Activator.PERF) {
			start = System.currentTimeMillis() - start;
			Activator.log.info("[DS perf] Processed bundle event '" + event + "' for " + start + "ms"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	// -- begin 'CM listener'
	/**
	 * Listen for configuration changes
	 * 
	 * Service Components can receive properties from the Configuration Admin
	 * service. If a Service Component is activated and it�s properties are
	 * updated in the Configuration Admin service, the SCR must deactivate the
	 * component and activate the component again using the new properties.
	 * 
	 * @param event
	 *            ConfigurationEvent
	 */
	public void configurationEvent(ConfigurationEvent event) {
		if (bundleToServiceComponents != null && !bundleToServiceComponents.isEmpty()) {
			addEvent(event, true);
		}
	}

	protected void processConfigurationEvent(ConfigurationEvent event) {
		if (bundleToServiceComponents == null || bundleToServiceComponents.isEmpty()) {
			// no components found till now
			return;
		}
		long start = 0l;
		try {
			if (Activator.DEBUG) {
				Activator.log.debug(" Resolver.configurationEvent(): pid = " + event.getPid() + ", fpid = " + event.getFactoryPid(), null); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (Activator.PERF) {
				start = System.currentTimeMillis();
				Activator.log.info("[DS perf] Started processing configuration event " + event); //$NON-NLS-1$
			}

			String pid = event.getPid();
			String fpid = event.getFactoryPid();
			for (Enumeration keys = bundleToServiceComponents.keys(); keys.hasMoreElements();) {
				Vector bundleComps = (Vector) bundleToServiceComponents.get(keys.nextElement());
				// bundleComps may be null since bundleToServiceComponents
				// may have been modified by another thread
				if (bundleComps != null) {
					for (int i = 0; i < bundleComps.size(); i++) {
						ServiceComponent sc = (ServiceComponent) bundleComps.elementAt(i);
						if (sc.getConfigurationPolicy() == ServiceComponent.CONF_POLICY_IGNORE) {
							//skip processing of this component - it is not interested in configuration changes
							continue;
						}
						String name = sc.getConfigurationPID();
						if (name.equals(pid) || name.equals(fpid)) {
							if (name.equals(fpid) && sc.factory != null) {
								Activator.log(sc.bc, LogService.LOG_ERROR, NLS.bind(Messages.FACTORY_CONF_NOT_APPLICABLE_FOR_COMPONENT_FACTORY, sc.name), null);
								return;
							}
							if (sc.enabled) {
								if (Activator.DEBUG) {
									Activator.log.debug("SCRManager.processConfigurationEvent(): found component - " + pid, null); //$NON-NLS-1$
								}
								processConfigurationEvent(event, sc);
							}
							return;
						}
					}
				}
			}
		} catch (Throwable e) {
			Activator.log(null, LogService.LOG_ERROR, NLS.bind(Messages.ERROR_PROCESSING_CONFIGURATION, event.getReference().getBundle()), e);
		} finally {
			if (Activator.PERF) {
				start = System.currentTimeMillis() - start;
				Activator.log.info("[DS perf] Processed configuration event '" + event + "' for " + start + "ms"); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
			}
		}
	}

	private void processConfigurationEvent(ConfigurationEvent event, ServiceComponent sc) {
		Configuration[] config = null;

		String pid = event.getPid();
		String fpid = event.getFactoryPid();

		switch (event.getType()) {
			case ConfigurationEvent.CM_UPDATED :

				String filter = (fpid != null ? "(&" : "") + "(" + Constants.SERVICE_PID + "=" + pid + ")" + (fpid != null ? ("(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + fpid + "))") : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
				try {
					config = Activator.listConfigurations(filter);
				} catch (IOException e) {
					Activator.log(null, LogService.LOG_ERROR, Messages.ERROR_LISTING_CONFIGURATIONS, e);
				} catch (InvalidSyntaxException e) {
					Activator.log(null, LogService.LOG_ERROR, Messages.ERROR_LISTING_CONFIGURATIONS, e);
				}

				if (config == null) {
					// The configuration may have been deleted by the now
					// If it does not exist we must do nothing
					return;
				}

				// if NOT a factory
				if (fpid == null) {
					// there is only one SCP for this SC
					boolean requiresRestart = true;
					if (sc.isNamespaceAtLeast11() && sc.modifyMethodName != "") { //$NON-NLS-1$
						ServiceComponentProp scp = sc.getServiceComponentProp();
						if (scp != null && scp.isBuilt()) {
							//process only built components
							requiresRestart = processConfigurationChange(scp, config[0]);
						}
					}
					if (requiresRestart) {
						// there is only one SCP for this SC, so we can disable the SC
						Vector components = new Vector();
						components.addElement(sc);
						resolver.disableComponents(components, ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_MODIFIED);

						// now re-enable the SC - the resolver will pick up the new config
						sc.enabled = true;
						resolver.enableComponents(components);
					}

					// If a MSF
					// create a new SCP or update an existing one
				} else {

					// get scp with this PID
					ServiceComponentProp scp = sc.getComponentPropByPID(pid);

					// if only the no-props scp exists, replace it
					if (scp == null && sc.componentProps != null) {
						synchronized (sc.componentProps) {
							if (sc.componentProps.size() == 1 && (((ServiceComponentProp) sc.componentProps.elementAt(0)).getProperties().get(Constants.SERVICE_PID) == null)) {
								scp = (ServiceComponentProp) sc.componentProps.elementAt(0);
							}
						}
					}
					boolean requiresRestart = true;
					if (sc.isNamespaceAtLeast11() && sc.modifyMethodName != "" && scp != null) { //$NON-NLS-1$
						if (scp.isBuilt()) {
							//process only built components
							requiresRestart = processConfigurationChange(scp, config[0]);
						}
					}
					if (requiresRestart) {
						// if old scp exists, dispose it
						if (scp != null) {
							// config already exists - dispose of it
							sc.componentProps.removeElement(scp);
							Vector components = new Vector();
							components.addElement(scp);
							resolver.disposeComponentConfigs(components, ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_MODIFIED);
							scp.setState(Component.STATE_DISPOSED);
						}

						// create a new scp (adds to resolver enabledSCPs list)
						resolver.map(sc, config[0]);

						// kick the resolver to figure out if SCP is satisfied, etc
						resolver.enableComponents(null);
					}
				}

				break;
			case ConfigurationEvent.CM_DELETED :

				// if not a factory
				if (fpid == null) {

					// there is only one SCP for this SC, so we can disable the SC
					Vector components = new Vector();
					components.addElement(sc);
					resolver.disableComponents(components, ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED);

					// now re-enable the SC - the resolver will create SCP with
					// no configAdmin properties
					sc.enabled = true;
					resolver.enableComponents(components);
				} else {
					// config is a factory

					// get SCP created for this config (with this PID)
					ServiceComponentProp scp = sc.getComponentPropByPID(pid);

					// if this was the last SCP created for this factory
					if (sc.componentProps.size() == 1) {
						// there is only one SCP for this SC, so we can disable the
						// SC
						Vector components = new Vector();
						components.addElement(sc);
						resolver.disableComponents(components, ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED);
						// now re-enable the SC - the resolver will create SCP
						// with no configAdmin properties
						sc.enabled = true;
						sc.setState(Component.STATE_UNSATISFIED);
						resolver.enableComponents(components);
					} else {
						// we can just dispose this SCP
						sc.componentProps.removeElement(scp);
						Vector components = new Vector();
						components.addElement(scp);
						resolver.disposeComponentConfigs(components, ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED);
						scp.setState(Component.STATE_DISPOSED);
					}
				}
				break;
		}
	}

	/**
	 * Process the modification of the specified component. 
	 * If it cannot be modified, the method will return <code>true</code> indicating the component has to be restarted.
	 * @param scp the component to modify
	 * @param config the configuration that brings the new properties
	 * @return true, if the component needs restart (to be deactivated and then eventually activated again)
	 */
	private boolean processConfigurationChange(ServiceComponentProp scp, Configuration config) {
		boolean result = false;
		Hashtable currentProps = scp.properties;
		Dictionary newProps = config.getProperties();
		Enumeration keys = currentProps.keys();
		Vector checkedFilters = new Vector();
		while (keys.hasMoreElements() && !result) {
			String key = (String) keys.nextElement();
			if (key.endsWith(".target")) { //$NON-NLS-1$
				checkedFilters.addElement(key);
				String newFilter = (String) newProps.get(key);
				Reference reference = null;
				String refName = key.substring(0, key.length() - ".target".length()); //$NON-NLS-1$
				Vector references = scp.references;
				for (int i = 0; i < references.size(); i++) {
					reference = (Reference) references.elementAt(i);
					if (reference.reference.name.equals(refName)) {
						break;
					}
					reference = null;
				}
				//check if there is a reference corresponding to the target property
				if (reference != null) {
					if (newFilter != null) {
						if (!newFilter.equals(currentProps.get(key))) {
							//the filter differs the old one
							result = result || !reference.doSatisfy(newFilter);
						}
					} else {
						//the target filter is removed. Using the default filter to check
						if (reference.policy == ComponentReference.POLICY_STATIC) {
							result = result || !reference.doSatisfy("(objectClass=" + reference.reference.interfaceName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
				}
			}
		}

		//now check the new properties if they have new target properties defined
		keys = newProps.keys();
		while (keys.hasMoreElements() && !result) {
			String key = (String) keys.nextElement();
			if (key.endsWith(".target") && !checkedFilters.contains(key)) { //$NON-NLS-1$
				Reference reference = null;
				String refName = key.substring(0, key.length() - ".target".length()); //$NON-NLS-1$
				Vector references = scp.references;
				for (int i = 0; i < references.size(); i++) {
					reference = (Reference) references.elementAt(i);
					if (reference.reference.name.equals(refName)) {
						break;
					}
					reference = null;
				}
				//check if there is a reference corresponding to the target property
				if (reference != null) {
					result = result || !reference.doSatisfy((String) newProps.get(key));
				}
			}
		}

		if (!result) {
			//do process component modification via the InstanceProcess
			try {
				InstanceProcess.staticRef.modifyComponent(scp, newProps);
			} catch (ComponentException ce) {
				//could happen if the modify method is not found
				result = true;
			}
		}
		return result;
	}

	private void disposeBundles() {
		// dispose ALL bundles
		if (bundleToServiceComponents != null) {
			for (Enumeration e = bundleToServiceComponents.keys(); e.hasMoreElements();) {
				Bundle bundle = (Bundle) e.nextElement();
				stoppingBundle(bundle);
			}
			bundleToServiceComponents.clear();
			bundleToServiceComponents = null;
		}
	}

	void stoppingBundle(Bundle bundle) {
		if (bundleToServiceComponents != null) {
			Vector components = (Vector) bundleToServiceComponents.remove(bundle);
			// disable the components which the bundle provides
			if (components != null) {
				if (Activator.DEBUG) {
					String bundleName = bundle.getSymbolicName();
					bundleName = (bundleName == null || "".equals(bundleName)) ? bundle.getLocation() : bundleName; //$NON-NLS-1$
					Activator.log.debug("SCRManager.stoppingBundle : " + bundleName, null); //$NON-NLS-1$
				}
				resolver.disableComponents(components, ComponentConstants.DEACTIVATION_REASON_BUNDLE_STOPPED);

				//set disposed state to all components since some of them might be still referenced by the ScrService
				for (int i = 0; i < components.size(); i++) {
					ServiceComponent sc = (ServiceComponent) components.elementAt(i);
					sc.setState(Component.STATE_DISPOSED);
				}
				if (bundleToServiceComponents.size() == 0) {
					hasRegisteredServiceListener = false;
					Activator.bc.removeServiceListener(this);
				}
			}
		}
	}

	void startedBundle(Bundle bundle) {
		synchronized (processingBundles) {
			if (processingBundles.get(bundle) != null) {
				//the bundle is already being processed
				return;
			}
			processingBundles.put(bundle, ""); //$NON-NLS-1$
		}
		try {
			startedBundle2(bundle);
		} finally {
			processingBundles.remove(bundle);
		}
	}

	void startedBundle2(Bundle bundle) {
		long start = 0l;
		if (Activator.PERF) {
			start = System.currentTimeMillis();
		}
		if (bundleToServiceComponents != null && bundleToServiceComponents.get(bundle) != null) {
			// the bundle is already processed - skipping it
			return;
		}

		String dsHeader = null;
		Dictionary allHeaders = bundle.getHeaders(""); //$NON-NLS-1$

		if (!((dsHeader = (String) allHeaders.get(ComponentConstants.SERVICE_COMPONENT)) != null)) {
			// no component descriptions in this bundle
			return;
		}

		Vector components = storage.loadComponentDefinitions(bundle, dsHeader);
		if (components != null && !components.isEmpty()) {
			if (!hasRegisteredServiceListener) {
				hasRegisteredServiceListener = true;
				Activator.bc.addServiceListener(this);
				resolver.synchronizeServiceReferences();
			}
			if (Activator.PERF) {
				start = System.currentTimeMillis() - start;
				Activator.log.info("[DS perf] The components of bundle " + bundle + " are parsed for " + start + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			if (bundleToServiceComponents == null) {
				synchronized (this) {
					if (bundleToServiceComponents == null) {
						bundleToServiceComponents = new Hashtable(11);
					}
				}
			}

			//check whether component's names are unique
			ServiceComponent comp;
			ServiceComponent comp2;
			L1: for (int i = 0; i < components.size(); i++) {
				comp = (ServiceComponent) components.elementAt(i);
				//check if unique in its bundle
				for (int j = i + 1; j < components.size(); j++) {
					comp2 = (ServiceComponent) components.elementAt(j);
					if (comp.name.equals(comp2.name)) {
						Activator.log(comp.bc, LogService.LOG_ERROR, NLS.bind(Messages.FOUND_COMPONENTS_WITH_DUPLICATED_NAMES, comp), null);
						//removing one of the components
						components.remove(i);
						i--;
						continue L1;
					}
				}
				//check if the component is globally unique
				Enumeration keys = bundleToServiceComponents.keys();
				while (keys.hasMoreElements()) {
					Vector components2 = (Vector) bundleToServiceComponents.get(keys.nextElement());
					for (int j = 0; j < components2.size(); j++) {
						comp2 = (ServiceComponent) components2.elementAt(j);
						if (comp.name.equals(comp2.name)) {
							Activator.log(comp.bc, LogService.LOG_WARNING, NLS.bind(Messages.FOUND_COMPONENTS_WITH_DUPLICATED_NAMES2, comp, comp2), null);
						}
					}
				}

				if (comp.autoenable) {
					comp.enabled = true;
				}
			}
			// store the components in the cache
			bundleToServiceComponents.put(bundle, components.clone());
			if (workThread != null && workThread.processingThread == Thread.currentThread()) {
				//we are in the queue thread already. Processing synchronously the job
				resolver.enableComponents(components);
			} else {
				// this will also resolve the component dependencies!
				enqueueWork(this, ENABLE_COMPONENTS, components, false);
				synchronized (components) {
					long startTime = System.currentTimeMillis();
					try {
						while (!components.isEmpty() && (System.currentTimeMillis() - startTime < WorkThread.BLOCK_TIMEOUT)) {
							components.wait(1000);
						}
						if (System.currentTimeMillis() - startTime >= WorkThread.BLOCK_TIMEOUT) {
							Activator.log(null, LogService.LOG_WARNING, NLS.bind(Messages.TIMEOUT_REACHED_ENABLING_COMPONENTS, getBundleName(bundle), Integer.toString(WorkThread.BLOCK_TIMEOUT)), null);
						}
					} catch (InterruptedException e) {
						//do nothing
					}
				}
			}
		}
	}

	private String getBundleName(Bundle b) {
		if (b.getSymbolicName() != null) {
			return b.getSymbolicName();
		}
		return b.getLocation();
	}

	public void enableComponent(String name, Bundle bundle) {
		changeComponent(name, bundle, true);
	}

	private void changeComponent(String name, Bundle bundle, boolean enable) {
		if (bundleToServiceComponents == null) {
			// already disposed!
			return;
		}
		try {
			Vector componentsToProcess = null;

			if (Activator.DEBUG) {
				String message = (enable ? "SCRManager.enableComponent(): " : "SCRManager.disableComponent(): ").concat(name != null ? name : "*all*") + " from bundle " + getBundleName(bundle); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				Activator.log.debug(message, null);
			}
			Vector bundleComponents = (Vector) bundleToServiceComponents.get(bundle);
			if (bundleComponents != null) {
				if (name != null) {
					boolean found = false;
					for (int i = 0; i < bundleComponents.size(); i++) {
						ServiceComponent component = (ServiceComponent) bundleComponents.elementAt(i);
						if (component.name.equals(name)) {
							found = true;
							if (component.enabled != enable) {
								component.enabled = enable;
								component.setState(enable ? Component.STATE_ENABLING : Component.STATE_DISABLING);
								componentsToProcess = new Vector(2);
								componentsToProcess.addElement(component);
								break;
							}
						}
					}
					if (!found) {
						throw new IllegalArgumentException(NLS.bind(Messages.COMPONENT_NOT_FOUND, name, bundle));
					}
				} else {
					if (enable) {
						// processing null parameter should be done only when
						// enabling components
						ServiceComponent sc;
						componentsToProcess = new Vector();
						for (int i = 0; i < bundleComponents.size(); i++) {
							sc = ((ServiceComponent) bundleComponents.elementAt(i));
							if (!sc.enabled) {
								componentsToProcess.addElement(sc);
								sc.enabled = enable;
								sc.setState(Component.STATE_ENABLING);
							}
						}
					}
				}

			}
			// publish to resolver the list of SCs to enable
			if (componentsToProcess != null && !componentsToProcess.isEmpty()) {
				if (enable) {
					enqueueWork(this, ENABLE_COMPONENTS, componentsToProcess, Activator.security);
				} else {
					enqueueWork(this, DISABLE_COMPONENTS, componentsToProcess, Activator.security);
				}
			}
		} catch (IllegalArgumentException iae) {
			throw iae;
		} catch (Throwable e) {
			Activator.log(null, LogService.LOG_ERROR, Messages.UNEXPECTED_EXCEPTION, e);
		}
	}

	/**
	 * QueuedJob represents the items placed on the asynch dispatch queue.
	 */
	static class QueuedJob {
		final WorkPerformer performer;
		/** the required type of action to do */
		final int actionType;
		/** work input data to be performed */
		final Object workToDo;

		/**
		 * Constructor for work queue item
		 * 
		 * @param d
		 *            Dispatcher for this item
		 * @param a
		 *            Action for this item
		 * @param o
		 *            Object for this item
		 */
		QueuedJob(WorkPerformer d, int a, Object o) {
			performer = d;
			actionType = a;
			workToDo = o;
		}

		void dispatch() {
			try {
				/* Call the WorkPerformer to process the work. */
				performer.performWork(actionType, workToDo);
			} catch (Throwable t) {
				Activator.log(null, LogService.LOG_ERROR, Messages.ERROR_DISPATCHING_WORK, t);
			}
		}

		public String toString() {
			return "[QueuedJob] WorkPerformer: " + performer + "; actionType " + actionType; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	// -- begin enable/disable components
	/**
	 * disableComponent - The specified component name must be in the same
	 * bundle as this component. Called by SC componentContext method
	 * 
	 * @param name
	 *            The name of a component to disable
	 * @param bundle
	 *            The bundle which contains the Service Component to be disabled
	 */
	public void disableComponent(String name, Bundle bundle) {
		changeComponent(name, bundle, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.ds.util.WorkPerformer#performWork(int,
	 *      java.lang.Object)
	 */
	public void performWork(int workAction, Object workObject) {
		if (workAction == ENABLE_COMPONENTS) {
			resolver.enableComponents((Vector) workObject);
			//notify that the component enabling has finished
			synchronized (workObject) {
				((Vector) workObject).clear();
				workObject.notify();
			}
		} else if (workAction == DISABLE_COMPONENTS) {
			resolver.disableComponents((Vector) workObject, ComponentConstants.DEACTIVATION_REASON_DISABLED);
		}
	}

	protected void configAdminRegistered(ConfigurationAdmin configAdmin, ServiceReference caReference) {
		if (bundleToServiceComponents == null || bundleToServiceComponents.isEmpty()) {
			// no components found till now
			return;
		}
		Vector toProcess = new Vector(1);
		Configuration[] configs = null;
		try {
			configs = configAdmin.listConfigurations(null);
		} catch (Exception e) {
			Activator.log(null, LogService.LOG_ERROR, Messages.ERROR_LISTING_CONFIGURATIONS, e);
		}
		if (configs == null || configs.length == 0) {
			//no configurations found
			return;
		}
		//process all components to find such that need to be evaluated when ConfigAdmin is available
		for (Enumeration keys = bundleToServiceComponents.keys(); keys.hasMoreElements();) {
			Vector bundleComps = (Vector) bundleToServiceComponents.get(keys.nextElement());
			// bundleComps may be null since bundleToServiceComponents
			// may have been modified by another thread
			if (bundleComps != null) {
				for (int i = 0; i < bundleComps.size(); i++) {
					ServiceComponent sc = (ServiceComponent) bundleComps.elementAt(i);
					if (sc.getConfigurationPolicy() == ServiceComponent.CONF_POLICY_IGNORE) {
						//skip processing of this component - it is not interested in configuration changes
						continue;
					}
					if (sc.enabled) {
						String componentPID = sc.getConfigurationPID();
						for (int j = 0; j < configs.length; j++) {
							if (configs[j].getPid().equals(componentPID) || componentPID.equals(configs[j].getFactoryPid())) {
								if (componentPID.equals(configs[j].getFactoryPid()) && sc.factory != null) {
									Activator.log(sc.bc, LogService.LOG_ERROR, NLS.bind(Messages.FACTORY_CONF_NOT_APPLICABLE_FOR_COMPONENT_FACTORY, sc.name), null);
									break;
								}
								//found a configuration for this component
								if (sc.componentProps == null || sc.componentProps.size() == 0) {
									if (sc.getConfigurationPolicy() == ServiceComponent.CONF_POLICY_REQUIRE) {
										//the component is not yet mapped because it is requiring configuration. Try to process it
										toProcess.addElement(sc);
									}
								} else {
									//process the component configurations and eventually modify or restart them
									ConfigurationEvent ce = new ConfigurationEvent(caReference, ConfigurationEvent.CM_UPDATED, configs[j].getFactoryPid(), configs[j].getPid());
									configurationEvent(ce);
								}
								break;
							}
						}
					}
				}
			}
		}
		if (toProcess.size() > 0) {
			enqueueWork(this, ENABLE_COMPONENTS, toProcess, false);
		}
	}

	public Component[] getComponents() {
		if (bundleToServiceComponents == null || bundleToServiceComponents.isEmpty()) {
			// no components found till now
			return null;
		}
		Vector result = new Vector();
		Enumeration en = bundleToServiceComponents.keys();
		while (en.hasMoreElements()) {
			Bundle b = (Bundle) en.nextElement();
			Vector serviceComponents = (Vector) bundleToServiceComponents.get(b);
			for (int i = 0; i < serviceComponents.size(); i++) {
				ServiceComponent sc = (ServiceComponent) serviceComponents.elementAt(i);
				if (sc.componentProps != null && !sc.componentProps.isEmpty()) {
					//add the created runtime components props
					result.addAll(sc.componentProps);
				} else {
					//add the declared component itself
					result.add(sc);
				}
			}
		}
		if (!result.isEmpty()) {
			Component[] res = new Component[result.size()];
			result.copyInto(res);
			return res;
		}
		return null;
	}

	public Component[] getComponents(Bundle bundle) {
		if (bundleToServiceComponents == null || bundleToServiceComponents.isEmpty()) {
			// no components found till now
			return null;
		}
		Vector serviceComponents = (Vector) bundleToServiceComponents.get(bundle);
		if (serviceComponents != null) {
			Vector result = new Vector();
			for (int i = 0; i < serviceComponents.size(); i++) {
				ServiceComponent sc = (ServiceComponent) serviceComponents.elementAt(i);
				if (sc.componentProps != null && !sc.componentProps.isEmpty()) {
					//add the created runtime components props
					result.addAll(sc.componentProps);
				} else {
					//add the declared component itself
					result.add(sc);
				}
			}
			if (!result.isEmpty()) {
				Component[] res = new Component[result.size()];
				result.copyInto(res);
				return res;
			}
		}
		return null;
	}

	public Component[] getComponents(String componentName) {
		if (bundleToServiceComponents == null || bundleToServiceComponents.isEmpty()) {
			// no components found till now
			return null;
		}
		Vector result = new Vector();
		Enumeration en = bundleToServiceComponents.keys();
		while (en.hasMoreElements()) {
			Bundle b = (Bundle) en.nextElement();
			Vector serviceComponents = (Vector) bundleToServiceComponents.get(b);
			for (int i = 0; i < serviceComponents.size(); i++) {
				ServiceComponent sc = (ServiceComponent) serviceComponents.elementAt(i);
				if (sc.getName().equals(componentName)) {
					if (sc.componentProps != null && !sc.componentProps.isEmpty()) {
						// add the created runtime components props
						result.addAll(sc.componentProps);
					} else {
						// add the declared component itself
						result.add(sc);
					}
					break;
				}
			}
		}
		if (!result.isEmpty()) {
			Component[] res = new Component[result.size()];
			result.copyInto(res);
			return res;
		}
		return null;
	}

}
