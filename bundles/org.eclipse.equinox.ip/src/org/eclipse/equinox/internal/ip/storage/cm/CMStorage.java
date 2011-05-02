/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ip.storage.cm;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.equinox.internal.ip.ProvisioningStorage;
import org.eclipse.equinox.internal.ip.impl.Log;
import org.eclipse.equinox.internal.ip.impl.ProvisioningAgent;
import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.provisioning.ProvisioningService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Implements Storage by using CM.
 * 
 * @author Avgustin Marinov
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class CMStorage implements ProvisioningStorage, ManagedService, BundleActivator {

	/** PID for Configuration Manager */
	public static final String PID = "equinox.provisioning.provisioning.pid";
	/**
	 * This system property is used for determining if to be used this
	 * storage/provider.
	 */
	public static final String CM_SUPPORT = "equinox.provisioning.cm.support";

	/** Registration as Managed Service. */
	private ServiceRegistration msSReg;
	/** Registration as Storage Service. */
	private ServiceRegistration sSReg;

	/** Tracker for ProvisioningService. */
	private ServiceTracker prvSrvTracker;
	/** Tracker for ConfigurationAdmin. */
	private ServiceTracker cmTracker;

	// /** Initial data data.*/
	// private Dictionary info;
	/** BundleContext. */
	private BundleContext bc;

	/** Counter is used for skipping "own update()-s". */
	private int counter;

	private boolean infoLoaded;
	private boolean skipStore;

	private boolean registering;

	/**
	 * Opens tracker an register itself as ManagedService.
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bc) throws Exception {
		Log.debug = ProvisioningAgent.getBoolean("equinox.provisioning.debug");
		boolean cmsupport = true;
		if (ProvisioningAgent.bc.getProperty(CM_SUPPORT) != null)
			if (ProvisioningAgent.bc.getProperty(CM_SUPPORT).equals("false"))
				cmsupport = false;
		if (!cmsupport) {
			Log.debug(this + " is not supported provider/storage!");
			return;
		} // Do not be used as provider or storage

		this.bc = bc;
		counter = 0;
		registering = false;

		prvSrvTracker = new ServiceTracker(bc, ProvisioningService.class.getName(), null);
		prvSrvTracker.open();

		cmTracker = new ServiceTracker(bc, ConfigurationAdmin.class.getName(), null);
		cmTracker.open();

		Dictionary props = new Hashtable(1, 1.0F);
		props.put(Constants.SERVICE_PID, PID);
		msSReg = bc.registerService(ManagedService.class.getName(), this, props);
		Log.debug("Managed Service Registered.");
	}

	/**
	 * Closes cmTracker and unregister service.
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bc) {
		Log.debug("Close " + this + '.');
		bc = null;
		try {
			if (sSReg != null) {
				sSReg.unregister();
				sSReg = null;
			}
		} catch (Exception _) {
		}
		try {
			if (msSReg != null) {
				msSReg.unregister();
				msSReg = null;
			}
		} catch (Exception _) {
		}

		prvSrvTracker.close();
		prvSrvTracker = null;

		cmTracker.close();
		cmTracker = null;
	}

	/**
	 * @see org.eclipse.equinox.internal.ip.ProvisioningStorage#getStoredInfo()
	 */
	public Dictionary getStoredInfo() {
		try {
			ConfigurationAdmin cm = (ConfigurationAdmin) cmTracker.getService();
			if (cm == null) {
				return null;
			}
			Configuration config = cm.getConfiguration(PID);
			Dictionary info = config.getProperties();
			infoLoaded = true;
			return info;
		} catch (IOException ioe) {
			Log.debug(ioe);
		}
		return null;
	}

	/**
	 * @see org.eclipse.equinox.internal.ip.ProvisioningStorage#store(java.util.Dictionary)
	 */
	public void store(Dictionary provisioningData) {
		try {
			synchronized (this) {
				if (skipStore) {
					skipStore = false;
					return;
				}
			}
			ConfigurationAdmin cm = (ConfigurationAdmin) cmTracker.getService();
			if (cm == null) {
				return;
			}
			Configuration config = cm.getConfiguration(PID);

			synchronized (this) {
				Log.debug("Store data into CM");
				config.update(provisioningData);
				counter++;
			}
		} catch (IOException e) {
			// Should be impossible
			Log.debug(e);
		}
	}

	/**
	 * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
	 */
	public void updated(Dictionary props) {
		boolean register = false;
		synchronized (this) {
			if (registering) {
				try {
					wait();
				} catch (InterruptedException ioe) {
				} // Should not happened
			}
			if (sSReg == null && bc != null) {
				registering = true;
				register = true;
			}
		}

		if (register) {
			try {
				sSReg = bc.registerService(ProvisioningStorage.class.getName(), this, null);
				Log.debug(this + " Registered.");
			} catch (Exception e) {
				Log.debug(e);
			}
			synchronized (this) {
				registering = false;
				notifyAll();
			}
			return;
		}

		Log.debug("CM Storage Updated : counter = " + counter);
		synchronized (this) {
			if (counter != 0) {
				counter--;
				return;
			} // This is an own update() so one updated() will be skipped
		}

		if (props == null) {
			return;
		}

		ProvisioningService prvSrv = (ProvisioningService) prvSrvTracker.getService();
		if (prvSrv != null) {
			synchronized (this) {
				if (!infoLoaded) {
					skipStore = true;
					infoLoaded = true;
				}
			}
			prvSrv.addInformation(props);
		}
	}

	/**
	 * Returns name of Storage.
	 * 
	 * @return name.
	 */
	public String toString() {
		return "CM Storage";
	}
}
