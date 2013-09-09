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
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.internal.tests.RegionReflectionUtils;
import org.eclipse.virgo.teststubs.osgi.framework.StubBundle;
import org.eclipse.virgo.teststubs.osgi.framework.StubBundleContext;
import org.junit.*;
import org.osgi.framework.*;
import org.osgi.framework.hooks.bundle.EventHook;
import org.osgi.framework.hooks.bundle.FindHook;

public class RegionBundleEventHookTests {

	private BundleEvent bundleEvent;

	private Collection<BundleContext> contexts;

	private Bundle eventBundle;

	private RegionDigraph mockRegionDigraph;

	private ThreadLocal<Region> threadLocal;

	private RegionDigraph digraph;

	private Region mockRegion1;

	private Region mockRegion2;

	private BundleEvent installedEvent1;

	private BundleEvent installedEvent2;

	private StubBundle eventBundle1;

	private StubBundle eventBundle2;

	@Before
	public void setUp() throws Exception {
		this.mockRegionDigraph = EasyMock.createMock(RegionDigraph.class);
		this.eventBundle = new StubBundle();

		this.eventBundle1 = new StubBundle(1L, "my.bundle1", new Version("0"), "loc1");
		this.eventBundle2 = new StubBundle(2L, "my.bundle2", new Version("0"), "loc2");

		this.bundleEvent = new BundleEvent(BundleEvent.STARTED, this.eventBundle, this.eventBundle);

		this.installedEvent1 = new BundleEvent(BundleEvent.INSTALLED, eventBundle1, this.eventBundle);
		this.installedEvent2 = new BundleEvent(BundleEvent.INSTALLED, eventBundle2, this.eventBundle);

		this.contexts = new HashSet<BundleContext>();
		StubBundleContext stubListenerBundleContext = new StubBundleContext();
		this.contexts.add(stubListenerBundleContext);
		this.threadLocal = new ThreadLocal<Region>();

		StubBundle stubSystemBundle = new StubBundle(0L, "osgi.framework", new Version("0"), "loc");
		StubBundleContext stubBundleContext = new StubBundleContext();
		stubBundleContext.addInstalledBundle(stubSystemBundle);
		this.digraph = RegionReflectionUtils.newStandardRegionDigraph(stubBundleContext, new ThreadLocal<Region>());
		this.digraph.createRegion("mockRegion1");
		this.digraph.createRegion("mockRegion2");
		this.mockRegion1 = digraph.getRegion("mockRegion1");
		this.mockRegion2 = digraph.getRegion("mockRegion2");
		this.mockRegion1.addBundle(this.eventBundle);

	}

	@After
	public void tearDown() throws Exception {
		// nothing
	}

	@Test
	public void testEventAllowed() {
		FindHook mockFindHook = new FindHook() {

			@Override
			public void find(BundleContext context, Collection<Bundle> bundles) {
				// nothing
			}
		};
		EventHook eventHook = RegionReflectionUtils.newRegionBundleEventHook(this.mockRegionDigraph, mockFindHook, this.threadLocal);
		eventHook.event(this.bundleEvent, this.contexts);
		assertEquals(1, this.contexts.size());
	}

	@Test
	public void testEventNotAllowed() {
		FindHook mockFindHook = new FindHook() {

			@Override
			public void find(BundleContext context, Collection<Bundle> bundles) {
				bundles.clear();
			}
		};
		EventHook eventHook = RegionReflectionUtils.newRegionBundleEventHook(this.mockRegionDigraph, mockFindHook, this.threadLocal);
		eventHook.event(this.bundleEvent, this.contexts);
		assertTrue(this.contexts.isEmpty());
	}

	@Test
	public void testDefaultRegion() {
		FindHook mockFindHook = new FindHook() {

			@Override
			public void find(BundleContext context, Collection<Bundle> bundles) {
				bundles.clear();
			}
		};

		this.digraph.setDefaultRegion(null);
		EventHook eventHook = RegionReflectionUtils.newRegionBundleEventHook(this.digraph, mockFindHook, this.threadLocal);
		eventHook.event(this.installedEvent1, this.contexts);
		assertTrue(this.digraph.getRegion(this.eventBundle1).equals(this.mockRegion1));

		this.digraph.setDefaultRegion(this.mockRegion2);
		eventHook = RegionReflectionUtils.newRegionBundleEventHook(this.digraph, mockFindHook, this.threadLocal);
		eventHook.event(this.installedEvent2, this.contexts);
		assertTrue(this.digraph.getRegion(this.eventBundle2).equals(this.mockRegion2));

	}

}
