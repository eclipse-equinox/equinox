/*******************************************************************************
 * Copyright (c) 2011 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.virgo.kernel.osgi.region.management.internal;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.eclipse.virgo.kernel.osgi.region.Region;
import org.eclipse.virgo.kernel.osgi.region.RegionDigraph;
import org.eclipse.virgo.kernel.osgi.region.internal.RegionLifecycleListener;
import org.eclipse.virgo.kernel.osgi.region.management.ManageableRegion;
import org.eclipse.virgo.kernel.osgi.region.management.ManageableRegionDigraph;
import org.osgi.framework.BundleContext;

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

    public StandardManageableRegionDigraph(RegionDigraph regionDigraph, String domain, BundleContext bundleContext) {
        this.regionDigraph = regionDigraph;
        this.domain = domain;
        this.regionObjectNameCreator = new RegionObjectNameCreator(domain);
        this.bundleContext = bundleContext;
        this.mbeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    private void registerRegionLifecycleListener() {
        RegionLifecycleListener regionLifecycleListener = new RegionLifecycleListener() {

            public void regionAdded(Region region) {
                addRegion(region);
            }

            public void regionRemoving(Region region) {
                removeRegion(region);
            }

        };
        this.bundleContext.registerService(RegionLifecycleListener.class, regionLifecycleListener, null);
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

        ObjectName name;
        try {
            name = new ObjectName(this.domain + ":type=RegionDigraph");
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid domain name '" + this.domain + "'", e);
        }

        safelyRegisterMBean(this, name);
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
            throw new RuntimeException("MBean registration failed", e);
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

    private void addRegion(Region region) {
        StandardManageableRegion manageableRegion = new StandardManageableRegion(region, this, this.regionDigraph);
        safelyRegisterMBean(manageableRegion, this.regionObjectNameCreator.getRegionObjectName(region.getName()));
        synchronized (this.monitor) {
            this.manageableRegions.put(region.getName(), manageableRegion);
        }
    }

    private void removeRegion(Region region) {
        String regionName = region.getName();
        synchronized (this.monitor) {
            this.manageableRegions.remove(regionName);
        }
        try {
            this.mbeanServer.unregisterMBean(this.regionObjectNameCreator.getRegionObjectName(regionName));
        } catch (MBeanRegistrationException e) {
            e.printStackTrace();
            throw new RuntimeException("Problem unregistering mbean", e);
        } catch (InstanceNotFoundException e) {
        }
    }

}
