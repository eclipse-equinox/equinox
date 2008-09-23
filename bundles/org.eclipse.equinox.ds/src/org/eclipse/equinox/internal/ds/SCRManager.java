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

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import org.eclipse.equinox.internal.ds.model.ServiceComponent;
import org.eclipse.equinox.internal.ds.model.ServiceComponentProp;
import org.eclipse.equinox.internal.util.event.Queue;
import org.eclipse.equinox.internal.util.ref.Log;
import org.eclipse.equinox.internal.util.threadpool.ThreadPoolManager;
import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.component.ComponentConstants;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Manager of update and delete events, forwarded by ConfigurationImpl to the
 * corresponding ManagedService(Factories). As those events are asynchronuos, a
 * separate thread is engaged for their execution.
 * 
 * @author Maria Ivanova
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 * @version 1.2
 */

public class SCRManager implements ServiceListener, SynchronousBundleListener, ConfigurationListener, WorkPerformer, PrivilegedAction {

	protected Hashtable bundleToServiceComponents;
	public BundleContext bc;
	protected Queue queue;
	public static Log log;
	private Resolver resolver;

	private WorkThread workThread;
	protected boolean running = false;
	protected boolean stopped = false;
	private ServiceTracker threadPoolManagerTracker;
	private boolean hasRegisteredServiceListener = false;

	/** work action type */
	public final int ENABLE_COMPONENTS = 1;
	public final int DISABLE_COMPONENTS = 2;

	private ComponentStorage storage;

	/**
	 * Constructs the SCRManager.
	 */
	public SCRManager(BundleContext bc, Log log) {
		this.bc = bc;
		SCRManager.log = log;

		security = Log.security();

		hasRegisteredServiceListener = true;
		queue = new Queue(10);
		if (Activator.startup)
			Activator.timeLog(110); /* 110 = "Queue instantiated for " */

		threadPoolManagerTracker = new ServiceTracker(bc, ThreadPoolManager.class.getName(), null);
		threadPoolManagerTracker.open();
		if (Activator.startup)
			Activator.timeLog(111);
		/*111 = "Threadpool service tracker opened for "*/

		resolver = new Resolver(this);
		if (Activator.startup)
			Activator.timeLog(112); /* 112 = "Resolver instantiated for " */

		bc.addBundleListener(this);
		if (Activator.startup)
			Activator.timeLog(105); /* 105 = "addBundleListener() method took " */

		String storageClass = bc.getProperty("scr.storage.class");
		if (storageClass == null) {
			storageClass = "org.eclipse.equinox.internal.ds.storage.file.FileStorage";
		}
		try {
			storage = (ComponentStorage) Class.forName(storageClass).getConstructor(new Class[] {BundleContext.class}).newInstance(new Object[] {bc});
		} catch (Exception e) {
			log.error("[SCR - SCRManager] could not create instance for " + storageClass, e);
		}
	}

	public void startIt() {
		// loop through the currently installed bundles
		Bundle[] bundles = bc.getBundles();
		if (bundles != null) {
			for (int i = 0; i < bundles.length; i++) {
				Bundle current = bundles[i];
				// try to process the active ones.
				if (current.getState() == Bundle.ACTIVE) {
					startedBundle(current);
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
	public void addEvent(Object upEv, boolean security) {
		try {
			synchronized (queue) {
				queue.put(upEv);
				if (!running) {
					if (queue.size() > 0) {
						running = true;
						workThread = new WorkThread(this);
						if (security) {
							AccessController.doPrivileged(this);
							return;
						}
						ThreadPoolManager threadPool = (ThreadPoolManager) threadPoolManagerTracker.getService();
						if (threadPool != null) {
							threadPool.execute(workThread, Thread.MAX_PRIORITY, "Component Resolve Thread");
						} else {
							new Thread(workThread, "Component Resolve Thread").start();
						}
					}
				} else if (workThread.waiting > 0) {
					queue.notifyAll();
				}
			}
		} catch (Throwable e) {
			Activator.log.error("[SCR] Unexpected exception occurred!", e);
		}
	}

	static boolean security = false;

	public Object run() {
		ThreadPoolManager threadPool = (ThreadPoolManager) threadPoolManagerTracker.getService();
		if (threadPool != null) {
			threadPool.execute(workThread, Thread.MAX_PRIORITY, "Component Resolve Thread");
		} else {
			new Thread(workThread, "Component Resolve Thread").start();
		}
		return null;
	}

	public void queueBlocked() {
		resolver.queueBlocked();
		synchronized (queue) {
			running = false;
			addEvent(null, security); // will result in starting new
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
	 */
	public void enqueueWork(WorkPerformer d, int a, Object o, boolean security) {
		addEvent(new QueuedJob(d, a, o), security);
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
			log.info("[DS perf] Started processing bundle event " + event);
		}
		int type = event.getType();
		if (type == BundleEvent.STOPPING) {
			stoppingBundle(event.getBundle());
		} else if (type == BundleEvent.STARTED) {
			startedBundle(event.getBundle());
		} else if (type == BundleEvent.UNINSTALLED && Activator.DBSTORE) {
			try {
				storage.deleteComponentDefinitions(event.getBundle().getBundleId());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (Activator.PERF) {
			start = System.currentTimeMillis() - start;
			log.info("[DS perf] Processed bundle event '" + event + "' for " + start + " ms");
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
				Activator.log.debug(" Resolver.configurationEvent(): pid = " + event.getPid() + ", fpid = " + event.getFactoryPid(), null);
			}
			if (Activator.PERF) {
				start = System.currentTimeMillis();
				log.info("[DS perf] Started processing configuration event " + event);
			}

			String pid = event.getPid();
			String fpid = event.getFactoryPid();
			for (Enumeration keys = bundleToServiceComponents.keys(); keys.hasMoreElements();) {
				if (Activator.DEBUG) {
					Activator.log.debug(0, 10013, null, null, false);
					// //Activator.log.debug(" hasNext", null);
				}
				Vector bundleComps = (Vector) bundleToServiceComponents.get(keys.nextElement());
				// bundleComps may be null since bundleToServiceComponents
				// may have been modified by another thread
				if (bundleComps != null) {
					for (int i = 0; i < bundleComps.size(); i++) {
						ServiceComponent sc = (ServiceComponent) bundleComps.elementAt(i);
						String name = sc.name;
						if (Activator.DEBUG) {
							Activator.log.debug(0, 10014, name, null, false);
							// //Activator.log.debug(" component name " + name,
							// null);
						}
						if (name.equals(pid) || name.equals(fpid)) {
							if (name.equals(fpid) && sc.factory != null) {
								Activator.log.error("[SCR - SCRManager] ComponentFactory " + name + " cannot be managed using factory configuration!", null);
								return;
							}
							if (sc.enabled) {
								if (Activator.DEBUG) {
									Activator.log.debug(0, 10015, pid, null, false);
									// //Activator.log.debug("
									// Resolver.configurationEvent(): found
									// component - " + pid, null);
								}
								processConfigurationEvent(event, sc);
							}
							return;
						}
					}
				}
			}
		} catch (Throwable e) {
			Activator.log.error("[SCR] Error while processing configuration event for " + event.getReference().getBundle(), e);
		} finally {
			if (Activator.PERF) {
				start = System.currentTimeMillis() - start;
				log.info("[DS perf] Processed configuration event '" + event + "' for " + start + " ms");
			}
		}
	}

	private void processConfigurationEvent(ConfigurationEvent event, ServiceComponent sc) {
		Configuration[] config = null;

		String pid = event.getPid();
		String fpid = event.getFactoryPid();

		switch (event.getType()) {
			case ConfigurationEvent.CM_UPDATED :

				String filter = (fpid != null ? "(&" : "") + "(" + Constants.SERVICE_PID + "=" + pid + ")" + (fpid != null ? ("(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + fpid + "))") : "");
				try {
					config = ConfigurationManager.listConfigurations(filter);
				} catch (IOException e) {
					log.error("Error while listing CM Configurations", e);
				} catch (InvalidSyntaxException e) {
					log.error("Error while listing CM Configurations", e);
				}

				if (config == null) {
					// The configuration may have been deleted by the now
					// If it does not exist we must do nothing
					return;
				}

				// if NOT a factory
				if (fpid == null) {
					// there is only one SCP for this SC, so we can disable the SC
					Vector components = new Vector();
					components.addElement(sc);
					resolver.disableComponents(components);

					// now re-enable the SC - the resolver will pick up the new
					// config
					sc.enabled = true;
					resolver.enableComponents(components);

					// If a MSF
					// create a new SCP or update an existing one
				} else {

					// get scp with this PID
					ServiceComponentProp scp = sc.getComponentPropByPID(pid);

					// if only the no-props scp exists, replace it
					if (scp == null && sc.componentProps != null && sc.componentProps.size() == 1 && (((ServiceComponentProp) sc.componentProps.elementAt(0)).getProperties().get(Constants.SERVICE_PID) == null)) {
						scp = (ServiceComponentProp) sc.componentProps.elementAt(0);
					}
					// if old scp exists, dispose of it
					if (scp != null) {
						// config already exists - dispose of it
						sc.componentProps.removeElement(scp);
						Vector components = new Vector();
						components.addElement(scp);
						resolver.disposeComponentConfigs(components);
					}

					// create a new scp (adds to resolver enabledSCPs list)
					resolver.map(sc, config[0]);

					// kick the resolver to figure out if SCP is satisfied, etc
					resolver.enableComponents(null);
				}

				break;
			case ConfigurationEvent.CM_DELETED :

				// if not a factory
				if (fpid == null) {

					// there is only one SCP for this SC, so we can disable the SC
					Vector components = new Vector();
					components.addElement(sc);
					resolver.disableComponents(components);

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
						resolver.disableComponents(components);
						// now re-enable the SC - the resolver will create SCP
						// with no configAdmin properties
						sc.enabled = true;
						resolver.enableComponents(components);
					} else {
						// we can just dispose this SCP
						sc.componentProps.removeElement(scp);
						Vector components = new Vector();
						components.addElement(scp);
						resolver.disposeComponentConfigs(components);
					}
				}
				break;
		}
	}

	private void disposeBundles() {
		log.info("disposeBundles()");
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
					bundleName = (bundleName == null || "".equals(bundleName)) ? bundle.getLocation() : bundleName;
					log.debug(0, 10016, bundleName, null, false);
					// //log.debug("SCRManager.stoppingBundle(" + bundleName
					// + ')', null);
				}
				resolver.disableComponents(components);
				if (bundleToServiceComponents.size() == 0) {
					hasRegisteredServiceListener = false;
					bc.removeServiceListener(this);
				}
			}
		}
	}

	synchronized void startedBundle(Bundle bundle) {
		long start = 0l;
		if (Activator.PERF) {
			start = System.currentTimeMillis();
		}
		if (bundleToServiceComponents != null) {
			if (bundleToServiceComponents.get(bundle) != null) {
				// the bundle is already processed - skipping it
				return;
			}
		}

		Dictionary allHeaders = bundle.getHeaders();

		if (!((allHeaders.get(ComponentConstants.SERVICE_COMPONENT)) != null)) {
			// no component descriptions in this bundle
			return;
		}

		Vector components = storage.loadComponentDefinitions(bundle.getBundleId());
		if (components != null && !components.isEmpty()) {
			if (!hasRegisteredServiceListener) {
				hasRegisteredServiceListener = true;
				bc.addServiceListener(this);
			}
			if (Activator.PERF) {
				start = System.currentTimeMillis() - start;
				log.info("[DS perf] The components for bundle " + bundle + " are parsed for " + start + " ms");
			}
			if (bundleToServiceComponents == null) {
				bundleToServiceComponents = new Hashtable(11);
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
						Activator.log.error("[SCR] Found components with duplicated names inside their bundle! This component will not be processed: " + comp, null);
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
							Activator.log.warning("[SCR] Found components with duplicated names! Details: \nComponent1 : " + comp + "\nComponent2: " + comp2, null);
						}
					}
				}

				if (comp.autoenable) {
					comp.enabled = true;
				}
			}
			// store the components in the cache
			bundleToServiceComponents.put(bundle, components);
			// this will also resolve the component dependencies!
			enqueueWork(this, ENABLE_COMPONENTS, components, false);
		}
	}

	public void enableComponent(String name, Bundle bundle) {
		changeComponent(name, bundle, true);
	}

	private void changeComponent(String name, Bundle bundle, boolean enable) {
		try {
			Vector componentsToProcess = null;

			if (Activator.DEBUG) {
				String message = (enable ? "SCRManager.enableComponent(): " : "SCRManager.disableComponent(): ").concat(name != null ? name : "*all*") + " from bundle " + bundle.getSymbolicName();
				Activator.log.debug(message, null);
			}
			if (bundleToServiceComponents == null) {
				// already disposed!
				return;
			}
			Vector bundleComponents = (Vector) bundleToServiceComponents.get(bundle);
			if (bundleComponents != null) {
				if (name != null) {
					for (int i = 0; i < bundleComponents.size(); i++) {
						ServiceComponent component = (ServiceComponent) bundleComponents.elementAt(i);
						if (component.name.equals(name) && component.enabled != enable) {
							component.enabled = enable;
							componentsToProcess = new Vector(2);
							componentsToProcess.addElement(component);
							break;
						}
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
							}
						}
					} else {
						Activator.log.warning("[SCRManager] Cannot dispose all components of a bundle at once!", null);
					}
				}

			}
			// publish to resolver the list of SCs to enable
			if (componentsToProcess != null && !componentsToProcess.isEmpty()) {
				if (enable) {
					enqueueWork(this, ENABLE_COMPONENTS, componentsToProcess, security);
				} else {
					enqueueWork(this, DISABLE_COMPONENTS, componentsToProcess, security);
				}
			}
			if (Activator.DEBUG) {
				Activator.log.debug(0, 10018, null, null, false);
				// //Activator.log.debug("changeComponent method end", null);
			}
		} catch (Throwable e) {
			Activator.log.error("[SCR] Unexpected exception occurred!", e);
		}
	}

	/**
	 * QueuedJob represents the items placed on the asynch dispatch queue.
	 */
	public class QueuedJob {
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
				log.error("[SCR] Error dispatching work ", t);
			}
		}

		public String toString() {
			return "[QueuedJob] WorkPerformer: " + performer + "; actionType " + actionType;
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
		} else if (workAction == DISABLE_COMPONENTS) {
			resolver.disableComponents((Vector) workObject);
		}
	}

}
