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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.*;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.tests.dummys.*;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.*;
import org.osgi.service.resolver.ResolutionException;

public class ResolutionReportTest extends AbstractTest {
	// TODO Add test for dynamic resolution in conjunction with UNRESOLVED_PROVIDER entries.

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
		org.eclipse.osgi.container.ModuleResolutionReport.Builder builder = new org.eclipse.osgi.container.ModuleResolutionReport.Builder();
		ResolutionReport report = builder.build();
		assertNotNull("Resolution report was null", report);
	}

	@Test
	public void testResolutionReportEntryFilteredByResolverHook() throws Exception {
		DummyResolverHook hook = new DummyResolverHook() {
			@Override
			public void filterResolvable(Collection<BundleRevision> candidates) {
				candidates.clear();
			}
		};
		DummyContainerAdaptor adaptor = createDummyAdaptor(hook);
		ModuleContainer container = adaptor.getContainer();
		Module module = installDummyModule("resolution.report.a.MF", "resolution.report.a", container);
		assertResolutionDoesNotSucceed(container, Arrays.asList(module));
		ResolutionReport report = hook.getResolutionReports().get(0);
		Map<Resource, List<ResolutionReport.Entry>> resourceToEntries = report.getEntries();
		assertResolutionReportEntriesSize(resourceToEntries, 1);
		List<ResolutionReport.Entry> entries = resourceToEntries.get(module.getCurrentRevision());
		assertResolutionReportEntriesSize(entries, 1);
		ResolutionReport.Entry entry = entries.get(0);
		assertResolutionReportEntryTypeFilteredByResolverHook(entry.getType());
		assertResolutionReportEntryDataNull(entry.getData());
	}

	@Test
	public void testResolutionReportEntrySingletonSelectionNoneResolved() throws Exception {
		DummyResolverHook hook = new DummyResolverHook();
		DummyContainerAdaptor adaptor = createDummyAdaptor(hook);
		ModuleContainer container = adaptor.getContainer();
		Module resolutionReporta = installDummyModule("resolution.report.a.MF", "resolution.report.a", container);
		Module resolutionReportaV1 = installDummyModule("resolution.report.a.v1.MF", "resolution.report.a.v1", container);
		assertResolutionDoesNotSucceed(container, Arrays.asList(resolutionReporta, resolutionReportaV1));
		ResolutionReport report = hook.getResolutionReports().get(0);
		Map<Resource, List<ResolutionReport.Entry>> resourceToEntries = report.getEntries();
		assertResolutionReportEntriesSize(resourceToEntries, 1);
		List<ResolutionReport.Entry> entries = resourceToEntries.get(resolutionReporta.getCurrentRevision());
		assertResolutionReportEntriesSize(entries, 1);
		ResolutionReport.Entry entry = entries.get(0);
		assertResolutionReportEntryTypeSingletonSelection(entry.getType());
		assertResolutionReportEntryDataNotNull(entry.getData());
	}

	@Test
	public void testResolutionReportEntrySingletonSelectionHighestVersionResolved() throws Exception {
		DummyResolverHook hook = new DummyResolverHook();
		DummyContainerAdaptor adaptor = createDummyAdaptor(hook);
		ModuleContainer container = adaptor.getContainer();
		Module resolutionReporta = installDummyModule("resolution.report.a.MF", "resolution.report.a", container);
		Module resolutionReportaV1 = installDummyModule("resolution.report.a.v1.MF", "resolution.report.a.v1", container);
		container.resolve(Arrays.asList(resolutionReportaV1), true);
		clearResolutionReports(hook);
		assertResolutionDoesNotSucceed(container, Arrays.asList(resolutionReporta));
		ResolutionReport report = hook.getResolutionReports().get(0);
		Map<Resource, List<ResolutionReport.Entry>> resourceToEntries = report.getEntries();
		assertResolutionReportEntriesSize(resourceToEntries, 1);
		List<ResolutionReport.Entry> entries = resourceToEntries.get(resolutionReporta.getCurrentRevision());
		assertResolutionReportEntriesSize(entries, 1);
		ResolutionReport.Entry entry = entries.get(0);
		assertResolutionReportEntryTypeSingletonSelection(entry.getType());
		assertResolutionReportEntryDataNotNull(entry.getData());
	}

	@Test
	public void testResolutionReportEntrySingletonSelectionLowestVersionResolved() throws Exception {
		DummyResolverHook hook = new DummyResolverHook();
		DummyContainerAdaptor adaptor = createDummyAdaptor(hook);
		ModuleContainer container = adaptor.getContainer();
		Module resolutionReporta = installDummyModule("resolution.report.a.MF", "resolution.report.a", container);
		container.resolve(Arrays.asList(resolutionReporta), true);
		clearResolutionReports(hook);
		Module resolutionReportaV1 = installDummyModule("resolution.report.a.v1.MF", "resolution.report.a.v1", container);
		assertResolutionDoesNotSucceed(container, Arrays.asList(resolutionReportaV1));
		ResolutionReport report = hook.getResolutionReports().get(0);
		Map<Resource, List<ResolutionReport.Entry>> resourceToEntries = report.getEntries();
		assertResolutionReportEntriesSize(resourceToEntries, 1);
		List<ResolutionReport.Entry> entries = resourceToEntries.get(resolutionReportaV1.getCurrentRevision());
		assertResolutionReportEntriesSize(entries, 1);
		ResolutionReport.Entry entry = entries.get(0);
		assertResolutionReportEntryTypeSingletonSelection(entry.getType());
		assertResolutionReportEntryDataNotNull(entry.getData());
	}

	@Test
	public void testResolutionReportEntryMissingCapability() throws Exception {
		DummyResolverHook hook = new DummyResolverHook();
		DummyContainerAdaptor adaptor = createDummyAdaptor(hook);
		ModuleContainer container = adaptor.getContainer();
		Module resolutionReportB = installDummyModule("resolution.report.b.MF", "resolution.report.b", container);
		assertResolutionDoesNotSucceed(container, Arrays.asList(resolutionReportB));
		ResolutionReport report = hook.getResolutionReports().get(0);
		Map<Resource, List<ResolutionReport.Entry>> resourceToEntries = report.getEntries();
		assertResolutionReportEntriesSize(resourceToEntries, 1);
		List<ResolutionReport.Entry> entries = resourceToEntries.get(resolutionReportB.getCurrentRevision());
		assertResolutionReportEntriesSize(entries, 1);
		ResolutionReport.Entry entry = entries.get(0);
		assertResolutionReportEntryTypeMissingCapability(entry.getType());
		assertResolutionReportEntryDataMissingCapability(entry.getData(), "osgi.wiring.package", "resolution.report.a");
	}

	@Test
	public void testResolutionReportEntryUnresolvedProvider01() throws Exception {
		DummyResolverHook hook = new DummyResolverHook();
		DummyContainerAdaptor adaptor = createDummyAdaptor(hook);
		ModuleContainer container = adaptor.getContainer();
		Module resolutionReportC = installDummyModule("resolution.report.c.MF", "resolution.report.c", container);
		Module resolutionReportD = installDummyModule("resolution.report.d.MF", "resolution.report.d", container);
		assertResolutionDoesNotSucceed(container, Arrays.asList(resolutionReportC, resolutionReportD));
		ResolutionReport report = hook.getResolutionReports().get(0);
		Map<Resource, List<ResolutionReport.Entry>> resourceToEntries = report.getEntries();
		assertResolutionReportEntriesSize(resourceToEntries, 2);

		List<ResolutionReport.Entry> entries = resourceToEntries.get(resolutionReportC.getCurrentRevision());
		assertResolutionReportEntriesSize(entries, 1);
		ResolutionReport.Entry entry = entries.get(0);
		assertResolutionReportEntryTypeUnresolvedProvider(entry.getType());
		assertResolutionReportEntryDataUnresolvedProvider(entry.getData(), "resolution.report.d");

		entries = resourceToEntries.get(resolutionReportD.getCurrentRevision());
		assertResolutionReportEntriesSize(entries, 1);
		entry = entries.get(0);
		assertResolutionReportEntryTypeMissingCapability(entry.getType());
		assertResolutionReportEntryDataMissingCapability(entry.getData(), "does.not.exist", null);
	}

	@Test
	public void testResolutionReportEntryUnresolvedProvider02() throws Exception {
		DummyResolverHook hook = new DummyResolverHook();
		DummyContainerAdaptor adaptor = createDummyAdaptor(hook);
		ModuleContainer container = adaptor.getContainer();
		Module resolutionReportE = installDummyModule("resolution.report.e.MF", "resolution.report.e", container);
		Module resolutionReportF = installDummyModule("resolution.report.f.MF", "resolution.report.f", container);
		Module resolutionReportG = installDummyModule("resolution.report.g.MF", "resolution.report.g", container);
		assertResolutionSucceeds(container, Arrays.asList(resolutionReportG, resolutionReportE, resolutionReportF));
		ResolutionReport report = hook.getResolutionReports().get(0);
		Map<Resource, List<ResolutionReport.Entry>> resourceToEntries = report.getEntries();
		assertResolutionReportEntriesSize(resourceToEntries, 2);

		List<ResolutionReport.Entry> entries = resourceToEntries.get(resolutionReportG.getCurrentRevision());
		assertResolutionReportEntriesSize(entries, 1);
		ResolutionReport.Entry entry = entries.get(0);
		assertResolutionReportEntryTypeUnresolvedProvider(entry.getType());
		assertResolutionReportEntryDataUnresolvedProvider(entry.getData(), "resolution.report.f");

		entries = resourceToEntries.get(resolutionReportF.getCurrentRevision());
		assertResolutionReportEntriesSize(entries, 1);
		entry = entries.get(0);
		assertResolutionReportEntryTypeMissingCapability(entry.getType());
		assertResolutionReportEntryDataMissingCapability(entry.getData(), "does.not.exist", null);
	}

	private void clearResolutionReports(DummyResolverHook hook) {
		hook.getResolutionReports().clear();
	}

	private void assertResolutionDoesNotSucceed(ModuleContainer container, Collection<Module> modules) {
		try {
			container.resolve(modules, true);
			fail("Resolution should not have succeeded");
		} catch (ResolutionException e) {
			// Okay.
		}
	}

	private void assertResolutionSucceeds(ModuleContainer container, Collection<Module> modules) {
		try {
			container.resolve(modules, false);
			// Okay
		} catch (ResolutionException e) {
			fail("Unexpected resolution exception");
		}
	}

	private void assertResolutionReportEntriesNotNull(Map<Resource, List<ResolutionReport.Entry>> entries) {
		assertNotNull("Resolution report entries was null", entries);
	}

	private void assertResolutionReportEntriesSize(Map<Resource, List<ResolutionReport.Entry>> entries, int expected) {
		assertResolutionReportEntriesNotNull(entries);
		assertEquals("Wrong number of total resolution report entries", expected, entries.size());
	}

	private void assertResolutionReportEntriesNotNull(List<ResolutionReport.Entry> entries) {
		assertNotNull("Resolution report entries for resource was null", entries);
	}

	private void assertResolutionReportEntriesSize(List<ResolutionReport.Entry> entries, int expected) {
		assertResolutionReportEntriesNotNull(entries);
		assertEquals("Wrong number of resolution report entries", expected, entries.size());
	}

	private void assertResolutionReportEntryDataNotNull(Object data) {
		assertNotNull("No resolution report entry data", data);
	}

	private void assertResolutionReportEntryDataNull(Object data) {
		assertEquals("Unexpected resolution report entry data", null, data);
	}

	private void assertResolutionReportEntryTypeFilteredByResolverHook(ResolutionReport.Entry.Type type) {
		assertResolutionReportEntryType(ResolutionReport.Entry.Type.FILTERED_BY_RESOLVER_HOOK, type);
	}

	private void assertResolutionReportEntryTypeSingletonSelection(ResolutionReport.Entry.Type type) {
		assertResolutionReportEntryType(ResolutionReport.Entry.Type.SINGLETON_SELECTION, type);
	}

	private void assertResolutionReportEntryTypeUnresolvedProvider(ResolutionReport.Entry.Type type) {
		assertResolutionReportEntryType(ResolutionReport.Entry.Type.UNRESOLVED_PROVIDER, type);
	}

	private void assertResolutionReportEntryDataMissingCapability(Object data, String namespace, String namespaceValue) {
		assertResolutionReportEntryDataNotNull(data);
		assertTrue("Wrong resolution report entry data type", data instanceof Requirement);
		Requirement requirement = (Requirement) data;
		assertEquals("Wrong requirement namespace", namespace, requirement.getNamespace());
		if (namespaceValue == null)
			return;
		assertTrue("Wrong requirement namespace value", requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE).contains(namespace + "=" + namespaceValue));
	}

	private void assertResolutionReportEntryDataUnresolvedProvider(Object data, String osgiIdentity) {
		assertResolutionReportEntryDataNotNull(data);
		assertTrue("Wrong resolution report entry data type", data instanceof Resource);
		Resource resource = (Resource) data;
		Capability capability = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0);
		assertEquals("Wrong unresolved provider", osgiIdentity, capability.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
	}

	private void assertResolutionReportEntryTypeMissingCapability(ResolutionReport.Entry.Type type) {
		assertResolutionReportEntryType(ResolutionReport.Entry.Type.MISSING_CAPABILITY, type);
	}

	private void assertResolutionReportEntryType(ResolutionReport.Entry.Type expected, ResolutionReport.Entry.Type actual) {
		assertEquals("Wrong resolution report entry type", expected, actual);
	}
}
