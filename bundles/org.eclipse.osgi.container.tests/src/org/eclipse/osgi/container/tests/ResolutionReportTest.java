/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.*;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.tests.dummys.*;
import org.eclipse.osgi.framework.report.ResolutionReport;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Resource;
import org.osgi.service.resolver.ResolutionException;

public class ResolutionReportTest extends AbstractTest {
	@Test
	public void testResolutionReportListenerService() throws Exception {
		DummyResolverHook hook = new DummyResolverHook();
		registerService(org.osgi.framework.hooks.resolver.ResolverHookFactory.class, new DummyResolverHookFactory(hook));
		getSystemBundle().adapt(FrameworkWiring.class).resolveBundles(Collections.singleton(getSystemBundle()));
		assertEquals("No resolution report listener callback", 1, hook.getResolutionReports().size());
		assertNotNull("Resolution report was null", hook.getResolutionReports().get(0));
	}

	@Test
	public void testResolutionReportListenerModule() throws Exception {
		DummyResolverHook hook = new DummyResolverHook();
		DummyContainerAdaptor adaptor = createDummyAdaptor(hook);
		ModuleContainer container = adaptor.getContainer();
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);
		container.resolve(Arrays.asList(systemBundle), true);
		assertEquals("No resolution report listener callback", 1, hook.getResolutionReports().size());
		assertNotNull("Resolution report was null", hook.getResolutionReports().get(0));
	}

	@Test
	public void testResolutionReportBuilder() {
		org.eclipse.osgi.container.ResolutionReport.Builder builder = new org.eclipse.osgi.container.ResolutionReport.Builder();
		ResolutionReport report = builder.build();
		assertNotNull("Resolution report was null", report);
	}

	@Test
	public void testFilteredByResolverHook() throws Exception {
		DummyResolverHook hook = new DummyResolverHook() {
			@Override
			public void filterResolvable(Collection<BundleRevision> candidates) {
				candidates.clear();
			}
		};
		DummyContainerAdaptor adaptor = createDummyAdaptor(hook);
		ModuleContainer container = adaptor.getContainer();
		Module module = installDummyModule("resolution.report.a.MF", "resolution.report.a", container);
		try {
			container.resolve(Collections.singleton(module), true);
			fail("Resolution should not have succeeded");
		} catch (ResolutionException e) {
			// Okay.
		}
		ResolutionReport report = hook.getResolutionReports().get(0);
		Map<Resource, List<ResolutionReport.Entry>> resourceToEntries = report.getEntries();
		assertNotNull("No entries", resourceToEntries);
		assertEquals("Wrong number of total entries", 1, resourceToEntries.size());
		List<ResolutionReport.Entry> entries = resourceToEntries.get(module.getCurrentRevision());
		assertNotNull("No entry for resource", entries);
		assertEquals("Wrong number of entries", 1, entries.size());
		ResolutionReport.Entry entry = entries.get(0);
		assertEquals("Wrong type", ResolutionReport.Entry.Type.FILTERED_BY_RESOLVER_HOOK, entry.getType());
	}

	@Test
	public void testFilteredBySingletonNoneResolved() throws Exception {
		DummyResolverHook hook = new DummyResolverHook();
		DummyContainerAdaptor adaptor = createDummyAdaptor(hook);
		ModuleContainer container = adaptor.getContainer();
		Module resolutionReporta = installDummyModule("resolution.report.a.MF", "resolution.report.a", container);
		Module resolutionReportaV1 = installDummyModule("resolution.report.a.v1.MF", "resolution.report.a.v1", container);
		try {
			container.resolve(Arrays.asList(resolutionReporta, resolutionReportaV1), true);
			fail("Resolution should not have succeeded");
		} catch (ResolutionException e) {
			// Okay.
		}
		ResolutionReport report = hook.getResolutionReports().get(0);
		Map<Resource, List<ResolutionReport.Entry>> resourceToEntries = report.getEntries();
		assertNotNull("No entries", resourceToEntries);
		assertEquals("Wrong number of total entries", 1, resourceToEntries.size());
		List<ResolutionReport.Entry> entries = resourceToEntries.get(resolutionReporta.getCurrentRevision());
		assertNotNull("No entry for resource", entries);
		assertEquals("Wrong number of entries", 1, entries.size());
		ResolutionReport.Entry entry = entries.get(0);
		assertEquals("Wrong type", ResolutionReport.Entry.Type.SINGLETON, entry.getType());
	}

	@Test
	public void testFilteredBySingletonHighestVersionResolved() throws Exception {
		DummyResolverHook hook = new DummyResolverHook();
		DummyContainerAdaptor adaptor = createDummyAdaptor(hook);
		ModuleContainer container = adaptor.getContainer();
		Module resolutionReporta = installDummyModule("resolution.report.a.MF", "resolution.report.a", container);
		Module resolutionReportaV1 = installDummyModule("resolution.report.a.v1.MF", "resolution.report.a.v1", container);
		container.resolve(Arrays.asList(resolutionReportaV1), true);
		hook.getResolutionReports().clear();
		try {
			container.resolve(Arrays.asList(resolutionReporta), true);
			fail("Resolution should not have succeeded");
		} catch (ResolutionException e) {
			// Okay.
		}
		ResolutionReport report = hook.getResolutionReports().get(0);
		Map<Resource, List<ResolutionReport.Entry>> resourceToEntries = report.getEntries();
		assertNotNull("No entries", resourceToEntries);
		assertEquals("Wrong number of total entries", 1, resourceToEntries.size());
		List<ResolutionReport.Entry> entries = resourceToEntries.get(resolutionReporta.getCurrentRevision());
		assertNotNull("No entry for resource", entries);
		assertEquals("Wrong number of entries", 1, entries.size());
		ResolutionReport.Entry entry = entries.get(0);
		assertEquals("Wrong type", ResolutionReport.Entry.Type.SINGLETON, entry.getType());
	}

	@Test
	public void testFilteredBySingletonLowestVersionResolved() throws Exception {
		DummyResolverHook hook = new DummyResolverHook();
		DummyContainerAdaptor adaptor = createDummyAdaptor(hook);
		ModuleContainer container = adaptor.getContainer();
		Module resolutionReporta = installDummyModule("resolution.report.a.MF", "resolution.report.a", container);
		container.resolve(Arrays.asList(resolutionReporta), true);
		hook.getResolutionReports().clear();
		Module resolutionReportaV1 = installDummyModule("resolution.report.a.v1.MF", "resolution.report.a.v1", container);
		try {
			container.resolve(Arrays.asList(resolutionReportaV1), true);
			fail("Resolution should not have succeeded");
		} catch (ResolutionException e) {
			// Okay.
		}
		ResolutionReport report = hook.getResolutionReports().get(0);
		Map<Resource, List<ResolutionReport.Entry>> resourceToEntries = report.getEntries();
		assertNotNull("No entries", resourceToEntries);
		assertEquals("Wrong number of total entries", 1, resourceToEntries.size());
		List<ResolutionReport.Entry> entries = resourceToEntries.get(resolutionReportaV1.getCurrentRevision());
		assertNotNull("No entry for resource", entries);
		assertEquals("Wrong number of entries", 1, entries.size());
		ResolutionReport.Entry entry = entries.get(0);
		assertEquals("Wrong type", ResolutionReport.Entry.Type.SINGLETON, entry.getType());
	}
}
