/*
 * This file is part of the Eclipse Virgo project.
 *
 * Copyright (c) 2011 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    VMware Inc. - initial contribution
 */

package org.eclipse.virgo.kernel.osgi.region.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;

import org.easymock.EasyMock;
import org.eclipse.virgo.kernel.osgi.region.Region;
import org.eclipse.virgo.kernel.osgi.region.RegionDigraph;
import org.eclipse.virgo.teststubs.osgi.framework.StubBundle;
import org.eclipse.virgo.teststubs.osgi.framework.StubBundleContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.hooks.bundle.EventHook;
import org.osgi.framework.hooks.bundle.FindHook;

public class RegionBundleEventHookTests {

    private FindHook mockFindHook;

    private BundleEvent bundleEvent;

    private Collection<BundleContext> contexts;

    private Bundle eventBundle;

    private RegionDigraph mockRegionDigraph;

    private ThreadLocal<Region> threadLocal;

    @Before
    public void setUp() throws Exception {
        this.mockRegionDigraph = EasyMock.createMock(RegionDigraph.class);
        this.mockFindHook = EasyMock.createMock(FindHook.class);
        this.eventBundle = new StubBundle();
        this.bundleEvent = new BundleEvent(BundleEvent.STARTED, this.eventBundle, this.eventBundle);
        this.contexts = new HashSet<BundleContext>();
        StubBundleContext stubListenerBundleContext = new StubBundleContext();
        this.contexts.add(stubListenerBundleContext);
        this.threadLocal = new ThreadLocal<Region>();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testEventAllowed() {
        this.mockFindHook = new FindHook() {

            @Override
            public void find(BundleContext context, Collection<Bundle> bundles) {
            }
        };
        EventHook eventHook = new RegionBundleEventHook(this.mockRegionDigraph, this.mockFindHook, this.threadLocal);
        eventHook.event(this.bundleEvent, this.contexts);
        assertEquals(1, this.contexts.size());
    }

    @Test
    public void testEventNotAllowed() {
        this.mockFindHook = new FindHook() {

            @Override
            public void find(BundleContext context, Collection<Bundle> bundles) {
                bundles.clear();
            }
        };
        EventHook eventHook = new RegionBundleEventHook(this.mockRegionDigraph, this.mockFindHook, this.threadLocal);
        eventHook.event(this.bundleEvent, this.contexts);
        assertTrue(this.contexts.isEmpty());
    }

}
