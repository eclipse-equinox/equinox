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
import org.eclipse.virgo.teststubs.osgi.framework.StubBundle;
import org.eclipse.virgo.teststubs.osgi.framework.StubBundleContext;
import org.eclipse.virgo.teststubs.osgi.framework.StubServiceReference;
import org.eclipse.virgo.teststubs.osgi.framework.StubServiceRegistration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.EventHook;
import org.osgi.framework.hooks.service.FindHook;

@SuppressWarnings("deprecation")
public class RegionServiceEventHookTests {

    private FindHook mockFindHook;

    private ServiceEvent serviceEvent;

    private Collection<BundleContext> contexts;

    private Bundle eventBundle;

    @Before
    public void setUp() throws Exception {
        this.mockFindHook = EasyMock.createMock(FindHook.class);
        this.eventBundle = new StubBundle();
        StubServiceReference<Object> stubServiceReference = new StubServiceReference<Object>(new StubServiceRegistration<Object>(
            (StubBundleContext) this.eventBundle.getBundleContext(), Object.class.getName()));
        this.serviceEvent = new ServiceEvent(ServiceEvent.REGISTERED, stubServiceReference);
        this.contexts = new HashSet<BundleContext>();
        StubBundleContext stubListenerBundleContext = new StubBundleContext();
        this.contexts.add(stubListenerBundleContext);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testEventAllowed() {
        this.mockFindHook = new FindHook() {

            @Override
            public void find(BundleContext context, String name, String filter, boolean allServices, Collection<ServiceReference<?>> references) {
            }
        };
        EventHook eventHook = new RegionServiceEventHook(this.mockFindHook);
        eventHook.event(this.serviceEvent, this.contexts);
        assertEquals(1, this.contexts.size());
    }

    @Test
    public void testEventNotAllowed() {
        this.mockFindHook = new FindHook() {

            @Override
            public void find(BundleContext context, String name, String filter, boolean allServices, Collection<ServiceReference<?>> references) {
                references.clear();
            }
        };
        EventHook eventHook = new RegionServiceEventHook(this.mockFindHook);
        eventHook.event(this.serviceEvent, this.contexts);
        assertTrue(this.contexts.isEmpty());
    }

}
