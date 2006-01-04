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
package org.eclipse.equinox.ds;

import java.util.*;
import org.eclipse.equinox.ds.model.ComponentDescription;
import org.eclipse.equinox.ds.model.ComponentDescriptionCache;
import org.eclipse.equinox.ds.resolver.Resolver;
import org.eclipse.equinox.ds.tracker.BundleTracker;
import org.eclipse.equinox.ds.tracker.BundleTrackerCustomizer;
import org.eclipse.equinox.ds.workqueue.WorkDispatcher;
import org.eclipse.equinox.ds.workqueue.WorkQueue;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * 
 * Main class for the SCR. This class will start the SCR bundle and begin
 * processing other bundles.
 * 
 * @version $Revision: 1.1 $
 */
public class Activator implements BundleActivator, BundleTrackerCustomizer, WorkDispatcher {

	public BundleContext context;
	private FrameworkHook framework;
	private ComponentDescriptionCache cache;
	private BundleTracker bundleTracker;
	public Resolver resolver;
	private ServiceTracker packageAdminTracker;
	public WorkQueue workQueue;

	private Hashtable bundleToComponentDescriptions;
	private Hashtable bundleToLastModified;

	/**
	 * New Service Components (ComponentDescription)s are added
	 */
	public static final int ADD = 1;

	/**
	 * Start the SCR bundle.
	 * 
	 * @param bundleContext BundleContext for SCR implementation.
	 */
	public void start(BundleContext bundleContext) {
		this.context = bundleContext;
		framework = new FrameworkHook();
		Log.init(context);
		cache = new ComponentDescriptionCache(this);
		bundleToComponentDescriptions = new Hashtable();
		bundleToLastModified = new Hashtable();

		packageAdminTracker = new ServiceTracker(context, PackageAdmin.class.getName(), null);
		packageAdminTracker.open();

		//notify this object when bundles enter (or exit) the Bundle.ACTIVE state
		bundleTracker = new BundleTracker(context, Bundle.ACTIVE, this);

		workQueue = new WorkQueue("SCR Work Queue"); //$NON-NLS-1$
		workQueue.setDaemon(true); // make sure the work queue is daemon
		resolver = new Resolver(this);
		workQueue.start();
		bundleTracker.open();
	}

	/**
	 * Stop the SCR bundle.
	 * 
	 * @param bundleContext BundleContext for SCR implementation.
	 */
	public void stop(BundleContext bundleContext) {

		bundleTracker.close();

		//process all remaining events in queue and then shut it down
		workQueue.closeAndJoin();

		resolver.dispose();
		packageAdminTracker.close();

		//shut down cache (write to disk)
		cache.dispose();

		Log.dispose();

		cache = null;
		framework = null;
		resolver = null;
		bundleToComponentDescriptions = null;
		bundleToLastModified = null;
		this.context = null;
	}

	/**
	 * Returns the bundle's Service-Component manifest header.  If the bundle
	 * has header, then the bundle will be tracked. If not, null is returned 
	 * and the bundle will not be tracked.
	 * 
	 * If the bundle contains service components, parse the service component xml 
	 * file(s) and create an {@link ComponentDescription ComponentDescription} object for every service 
	 * component in the bundle.  Add the {@link ComponentDescription ComponentDescriptions} to 
	 * the queue to be sent to the resolver.
	 * 
	 * @param bundle Candidate bundle to be tracked.
	 * @return the bundle's Service-Component manifest header or null if the 
	 * bundle does not specify the header.
	 */
	public Object addingBundle(Bundle bundle) {

		List enableComponentDescriptions = new ArrayList();

		PackageAdmin packageAdmin = (PackageAdmin) packageAdminTracker.getService();
		if (packageAdmin.getBundleType(bundle) != 0) {
			return null; // don't process fragments.
		}

		Long bundleID = new Long(bundle.getBundleId());

		// get the bundle's last modified date
		Long bundleLastModified = new Long(bundle.getLastModified());

		// get the last saved value for the bundle's last modified date
		Long bundleOldLastModified = (Long) bundleToLastModified.get(bundleID);

		// compare the two and if changed ( or if first time ) update the maps
		if ((!bundleLastModified.equals(bundleOldLastModified)) || (bundleOldLastModified == null)) {

			// get the BundleContext for this bundle (framework impl dependent)
			BundleContext bundleContext = framework.getBundleContext(bundle);

			// get all ComponentDescriptions for this bundle
			List componentDescriptions = cache.getComponentDescriptions(bundleContext);

			// update map of bundle to ComponentDescriptions
			bundleToComponentDescriptions.put(bundleID, componentDescriptions);

			// update bundle:lastModifiedDate map
			bundleToLastModified.put(bundleID, bundleLastModified);

			// for each CD in bundle set enable flag based on autoenable

			Iterator it = componentDescriptions.iterator();
			while (it.hasNext()) {
				ComponentDescription componentDescription = (ComponentDescription) it.next();
				validate(componentDescription);
				if (componentDescription.isAutoenable() && componentDescription.isValid()) {
					componentDescription.setEnabled(true);
					enableComponentDescriptions.add(componentDescription);
				}
			}
		}

		// publish all CDs to be enabled, to resolver (add to the workqueue and
		// publish event)
		if (!enableComponentDescriptions.isEmpty()) {
			workQueue.enqueueWork(this, ADD, enableComponentDescriptions);
		}
		return bundleID;
	}

	/**
	 * Empty implementation. No work is needed for modifiedBundle.
	 * 
	 * @param bundle
	 * @param object
	 */
	public void modifiedBundle(Bundle bundle, Object object) {

		Long bundleID = new Long(bundle.getBundleId());

		// flush map
		bundleToComponentDescriptions.remove(bundleID);

		// flush map
		bundleToLastModified.remove(bundleID);

	}

	/**
	 * A bundle is going to an in-ACTIVE state.  Dispose and remove it's service
	 * components from the system.  Disposal is done synchronously so all of the 
	 * service components have been disposed before this method returns.
	 * 
	 * @param bundle Bundle becoming untracked.
	 * @param object Value returned by addingBundle.
	 */

	public void removedBundle(Bundle bundle, Object object) {

		List disableComponentDescriptions = new ArrayList();
		Long bundleID = new Long(bundle.getBundleId());

		// get CD's for this bundle
		List ComponentDescriptions = (List) bundleToComponentDescriptions.get(new Long(bundle.getBundleId()));
		if (ComponentDescriptions != null) {
			Iterator it = ComponentDescriptions.iterator();

			// for each CD in bundle
			while (it.hasNext()) {
				ComponentDescription ComponentDescription = (ComponentDescription) it.next();

				// check if enabled && satisfied
				if ((ComponentDescription.isEnabled())) {

					// add to disabled list
					disableComponentDescriptions.add(ComponentDescription);

					// mark disabled
					ComponentDescription.setEnabled(false);
				}
			}
		}

		// remove the bundle from the lists/maps
		bundleToComponentDescriptions.remove(bundleID);
		bundleToLastModified.remove(bundleID);

		resolver.disableComponents(disableComponentDescriptions);
		return;
	}

	/**
	 * Called by Service Component code via ComponentContext
	 * 
	 * Enable the component(s) and put them on the queue for the resolver.
	 * 
	 * @param name The name of a component or <code>null</code> to indicate
	 *        all components in the bundle.
	 * 
	 * @param bundle The bundle which contains the Service Component to be
	 *        enabled
	 */
	public void enableComponent(String name, Bundle bundle) {

		// get all ComponentDescriptions for this bundle
		List componentDescriptions = (List) bundleToComponentDescriptions.get(new Long(bundle.getBundleId()));

		// Create the list of CD's to be enabled
		List enableCDs = new ArrayList();

		if (componentDescriptions != null) {
			Iterator it = componentDescriptions.iterator();

			// for each CD in list
			while (it.hasNext()) {
				ComponentDescription componentDescription = (ComponentDescription) it.next();
				validate(componentDescription);

				// if name is null then enable ALL Component Descriptions in
				// this bundle
				if (name == null) {

					// if CD is valid and is disabled then enable it
					if (componentDescription.isValid() && !componentDescription.isEnabled()) {

						// add to list of CDs to enable
						enableCDs.add(componentDescription);

						// set CD enabled
						componentDescription.setEnabled(true);
					}
				} else {
					if (componentDescription.getName().equals(name)) {

						// if CD is valid and is disabled then enable it
						if (componentDescription.isValid() && !componentDescription.isEnabled()) {

							// add to list of CDs to enable
							enableCDs.add(componentDescription);

							// set CD enabled
							componentDescription.setEnabled(true);
						}
					}
				}
				// else it is either not valid or it is already enabled - do
				// nothing
			}
		}

		// publish to resolver the list of CDs to enable
		if (!enableCDs.isEmpty())
			workQueue.enqueueWork(this, ADD, enableCDs);
	}

	/**
	 * Disable a Service Component - The specified component name must be in the 
	 * bundle as this component. Called by Service Component via ComponentContext.
	 * 
	 * Synchronously disable the component.  All component configurations (CDPs)
	 * are disposed before this method returns.
	 * 
	 * @param name The name of a component to disable
	 * 
	 * @param bundle The bundle which contains the Service Component to be
	 *        disabled
	 */

	public void disableComponent(String name, Bundle bundle) {

		List disableComponentDescriptions = new ArrayList();

		// Get the list of CDs for this bundle
		List componentDescriptionsList = (List) bundleToComponentDescriptions.get(new Long(bundle.getBundleId()));

		if (componentDescriptionsList != null) {
			Iterator it = componentDescriptionsList.iterator();

			// for each ComponentDescription in list
			while (it.hasNext()) {
				ComponentDescription componentDescription = (ComponentDescription) it.next();

				// find the ComponentDescription with the specified name
				if (componentDescription.getName().equals(name)) {

					// if enabled then add to disabled list and mark disabled
					if (componentDescription.isEnabled()) {

						disableComponentDescriptions.add(componentDescription);

						componentDescription.setEnabled(false);

					}
				}
			}
		}

		// publish to resolver the list of CDs to disable
		resolver.disableComponents(disableComponentDescriptions);
		return;
	}

	/**
	 * Put a job on the work queue to be done later (asynchronously) by the work 
	 * queue thread.
	 * @param workAction currently only valid value is Main.ADD
	 * @param workObject work object to be acted upon
	 * @see org.eclipse.equinox.ds.workqueue.WorkDispatcher#dispatchWork(int,
	 *      java.lang.Object)
	 */
	public void dispatchWork(int workAction, Object workObject) {

		List descriptions;
		descriptions = (List) workObject;
		switch (workAction) {
			case ADD :
				resolver.enableComponents(descriptions);
				break;
		}
	}

	/**
	 * Validate the Component Description
	 * 
	 * If error is found log and throw exception.
	 * 
	 * @param cd to be validated
	 * @throws Throwable if fatal problem is found
	 */
	private void validate(ComponentDescription componentDescription) {

		if ((componentDescription.getFactory() != null) && (componentDescription.getService() != null) && (componentDescription.getService().isServicefactory())) {
			componentDescription.setValid(false);
			Log.log(1, "validate cd: ", new Throwable("invalid to specify both ComponentFactory and ServiceFactory"));
		} else if ((componentDescription.isImmediate()) && (componentDescription.getService() != null) && (componentDescription.getService().isServicefactory())) {
			componentDescription.setValid(false);
			Log.log(1, "validate cd: ", new Throwable("invalid to specify both immediate and ServiceFactory"));
		} else if ((componentDescription.isImmediate()) && (componentDescription.getFactory() != null)) {
			componentDescription.setValid(false);
			Log.log(1, "validate cd: ", new Throwable("invalid to specify both immediate and ComponentFactory"));
		} else if ((!componentDescription.isImmediate()) && (componentDescription.getService() == null) && (componentDescription.getFactory() == null)) {
			componentDescription.setValid(false);
			Log.log(1, "validate cd: ", new Throwable("invalid set immediate to false and provide no Service"));
		} else {
			componentDescription.setValid(true);
		}

	}

}
