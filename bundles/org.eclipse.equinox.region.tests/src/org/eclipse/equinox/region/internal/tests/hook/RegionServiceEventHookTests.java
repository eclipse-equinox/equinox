/*******************************************************************************
 * Copyright (c) 2011 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SpringSource, a division of VMware - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.equinox.region.internal.tests.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import org.easymock.EasyMock;
import org.eclipse.equinox.region.internal.tests.RegionReflectionUtils;
import org.eclipse.virgo.teststubs.osgi.framework.*;
import org.junit.*;
import org.osgi.framework.*;
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
		StubServiceReference<Object> stubServiceReference = new StubServiceReference<Object>(new StubServiceRegistration<Object>((StubBundleContext) this.eventBundle.getBundleContext(), Object.class.getName()));
		this.serviceEvent = new ServiceEvent(ServiceEvent.REGISTERED, stubServiceReference);
		this.contexts = new HashSet<BundleContext>();
		StubBundleContext stubListenerBundleContext = new StubBundleContext();
		this.contexts.add(stubListenerBundleContext);
	}

	@After
	public void tearDown() throws Exception {
		// nothing
	}

	@Test
	public void testEventAllowed() {
		this.mockFindHook = new FindHook() {

			@Override
			public void find(BundleContext context, String name, String filter, boolean allServices, Collection<ServiceReference<?>> references) {
				// nothing
			}
		};
		EventHook eventHook = RegionReflectionUtils.newRegionServiceEventHook(this.mockFindHook);
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
		EventHook eventHook = RegionReflectionUtils.newRegionServiceEventHook(this.mockFindHook);
		eventHook.event(this.serviceEvent, this.contexts);
		assertTrue(this.contexts.isEmpty());
	}

}
