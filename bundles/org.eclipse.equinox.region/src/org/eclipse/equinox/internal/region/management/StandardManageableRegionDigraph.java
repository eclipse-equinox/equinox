/*******************************************************************************
 * Copyright (c) 2011, 2012 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.equinox.internal.region.management;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.*;
import org.eclipse.equinox.internal.region.RegionLifecycleListener;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.management.ManageableRegion;
import org.eclipse.equinox.region.management.ManageableRegionDigraph;
import org.osgi.framework.*;

/**
 * {@link StandardManageableRegionDigraph} is a {@link ManageableRegionDigraph} that delegates to the
 * {@link RegionDigraph}.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Thread safe.
 */
public final class StandardManageableRegionDigraph implements ManageableRegionDigraph {

	private final RegionDigraph regionDigraph;

	private final String domain;

	private final RegionObjectNameCreator regionObjectNameCreator;

	private final Map<String, ManageableRegion> manageableRegions = new ConcurrentHashMap<String, ManageableRegion>();

	private final BundleContext bundleContext;

	private final Object monitor = new Object();

	private final MBeanServer mbeanServer;

	private final ObjectName mbeanName;

	private ServiceRegistration<RegionLifecycleListener> listenerRegistration;

	private final String frameworkUUID;

	private final RegionLifecycleListener regionLifecycleListener = new RegionLifecycleListener() {

		public void regionAdded(Region region) {
			addRegion(region);
		}

		public void regionRemoving(Region region) {
			removeRegion(region);
		}

	};

	public StandardManageableRegionDigraph(RegionDigraph regionDigraph, String domain, BundleContext bundleContext) {
		this.regionDigraph = regionDigraph;
		this.domain = domain;
		this.bundleContext = bundleContext;
		this.mbeanServer = ManagementFactory.getPlatformMBeanServer();
		this.frameworkUUID = bundleContext.getProperty(Constants.FRAMEWORK_UUID);
		this.regionObjectNameCreator = new RegionObjectNameCreator(domain, this.frameworkUUID);
		String name = this.domain + ":type=RegionDigraph"; //$NON-NLS-1$
		if (frameworkUUID != null)
			name += ",frameworkUUID=" + frameworkUUID; //$NON-NLS-1$
		try {
			mbeanName = new ObjectName(name);
		} catch (MalformedObjectNameException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Invalid domain name '" + domain + "' resulting in an invalid object name '" + name + "'", e); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		}

	}

	private void registerRegionLifecycleListener() {
		synchronized (this.monitor) {
			listenerRegistration = this.bundleContext.registerService(RegionLifecycleListener.class, regionLifecycleListener, null);
		}
	}

	private void unregisterRegionLifecycleListener() {
		synchronized (this.monitor) {
			listenerRegistration.unregister();
		}
	}

	public void registerMBean() {
		registerRegionLifecycleListener();
		synchronized (this.monitor) {
			// The following alien call is unavoidable to ensure consistency.
			Set<Region> regions = this.regionDigraph.getRegions();
			for (Region region : regions) {
				addRegion(region);
			}
		}

		safelyRegisterMBean(this, mbeanName);
	}

	public void unregisterMbean() {
		unregisterRegionLifecycleListener();
		Collection<String> currentRegionNames;
		synchronized (this.monitor) {
			currentRegionNames = new ArrayList<String>(this.manageableRegions.keySet());
		}
		for (String regionName : currentRegionNames) {
			removeRegion(regionName);
		}
		try {
			this.mbeanServer.unregisterMBean(this.mbeanName);
		} catch (MBeanRegistrationException e) {
			e.printStackTrace();
			throw new RuntimeException("Problem unregistering mbean", e); //$NON-NLS-1$
		} catch (InstanceNotFoundException e) {
			// Something else unregistered the bean
		}
	}

	private void safelyRegisterMBean(Object mbean, ObjectName name) {
		try {
			try {
				this.mbeanServer.registerMBean(mbean, name);
			} catch (InstanceAlreadyExistsException e) {
				// Recover as this happens when a JVM is reused.
				this.mbeanServer.unregisterMBean(name);
				this.mbeanServer.registerMBean(mbean, name);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("MBean registration failed", e); //$NON-NLS-1$
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public ManageableRegion[] getRegions() {
		List<ManageableRegion> regions = new ArrayList<ManageableRegion>();
		synchronized (this.monitor) {
			for (ManageableRegion manageableRegion : this.manageableRegions.values()) {
				regions.add(manageableRegion);
			}
		}
		return regions.toArray(new ManageableRegion[regions.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	public ManageableRegion getRegion(String regionName) {
		return this.manageableRegions.get(regionName);
	}

	void addRegion(Region region) {
		StandardManageableRegion manageableRegion = new StandardManageableRegion(region, this, this.regionDigraph);
		safelyRegisterMBean(manageableRegion, this.regionObjectNameCreator.getRegionObjectName(region.getName()));
		synchronized (this.monitor) {
			this.manageableRegions.put(region.getName(), manageableRegion);
		}
	}

	void removeRegion(Region region) {
		removeRegion(region.getName());
	}

	private void removeRegion(String regionName) {
		synchronized (this.monitor) {
			this.manageableRegions.remove(regionName);
		}
		try {
			this.mbeanServer.unregisterMBean(this.regionObjectNameCreator.getRegionObjectName(regionName));
		} catch (MBeanRegistrationException e) {
			e.printStackTrace();
			throw new RuntimeException("Problem unregistering mbean", e); //$NON-NLS-1$
		} catch (InstanceNotFoundException e) {
			// Something else unregistered the bean
		}
	}

}
