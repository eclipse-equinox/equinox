/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.Module.State;
import org.eclipse.osgi.container.ModuleCapability;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.container.builders.OSGiManifestBuilderFactory;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.eclipse.osgi.tests.container.dummys.DummyContainerAdaptor;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

public class ModuleContainerUsageTest extends AbstractTest {
	static final String EE_INDEX = "internal.ee.index";

	@Test
	public void testMultipleExecutionEnvironments() throws BundleException {

		final List<BundleCapability> executionEnvironments = Collections.synchronizedList(new ArrayList<>());

		/**
		 * A resolver hook that filters packages based on the required EE of a module
		 */
		ResolverHook resolverHook = new ResolverHook() {

			@Override
			public void filterSingletonCollisions(BundleCapability singleton,
					Collection<BundleCapability> collisionCandidates) {
				// don't care about possible collisions
				collisionCandidates.clear();
			}

			@Override
			public void filterResolvable(Collection<BundleRevision> candidates) {
				// don't stop any revision from resolving
			}

			@Override
			public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
				if (PackageNamespace.PACKAGE_NAMESPACE.equals(requirement.getNamespace())) {
					// only filter based on EE for package requirements
					filterBasedOnEE(
							(AtomicReference<Long>) ((ModuleRevision) requirement.getRevision()).getRevisionInfo(),
							candidates);
				}
			}

			private void filterBasedOnEE(AtomicReference<Long> revisionInfo,
					Collection<BundleCapability> candidates) {
				Long eeIndex = revisionInfo.get();
				// if the eeIndex is set for the revision then check if the package is
				// available in the specified EE
				if (eeIndex != null) {
					Iterator<BundleCapability> iCandidates = candidates.iterator();
					while (iCandidates.hasNext()) {
						ModuleCapability candidate = (ModuleCapability) iCandidates.next();
						// only check the ee index for the packages from the system module (id=0)
						if (candidate.getRevision().getRevisions().getModule().getId() == 0) {
							Object candidateEEIndex = candidate.getAttributes().get(EE_INDEX);
							if (candidateEEIndex != null && !eeIndex.equals(candidateEEIndex)) {
								iCandidates.remove();
							}
						}
					}
				}
			}

			@Override
			public void end() {
				// do nothing special at the end of resolution
			}
		};
		DummyContainerAdaptor adaptor = createDummyAdaptor(resolverHook);
		ModuleContainer container = adaptor.getContainer();

		// Notice that there are duplicate packages exported for each EE_INDEX
		StringBuilder systemPackages = new StringBuilder();
		systemPackages.append("test.pkg1; " + EE_INDEX + ":Long=1");
		systemPackages.append(",test.pkg1; test.pkg2; " + EE_INDEX + ":Long=2");
		systemPackages.append(",test.pkg1; test.pkg2; test.pkg3; " + EE_INDEX + ":Long=3");
		systemPackages.append(",test.pkg1; test.pkg2; test.pkg3; test.pkg4; " + EE_INDEX + ":Long=4");
		systemPackages.append(",test.pkg1; test.pkg2; test.pkg3; test.pkg4; test.pkg5; " + EE_INDEX + ":Long=5");

		// Notice that each defined EE capability has an EE_INDEX defined that matches
		// the set of packages
		// above for the specified EE_INDEX
		StringBuilder systemEEs = new StringBuilder();
		systemEEs.append(
				"osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"1.0, 1.1, 1.2, 1.3, 1.4, 1.5\"; " + EE_INDEX
						+ ":Long=1");
		systemEEs.append(
				",osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6\"; " + EE_INDEX
						+ ":Long=2");
		systemEEs.append(
				",osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7\"; "
						+ EE_INDEX + ":Long=3");
		systemEEs.append(
				",osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8\"; "
						+ EE_INDEX + ":Long=4");
		systemEEs.append(
				",osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 9.0\"; "
						+ EE_INDEX + ":Long=5");

		ModuleRevisionBuilder systemModuleBuilder = OSGiManifestBuilderFactory.createBuilder(getSystemManifest(),
				Constants.SYSTEM_BUNDLE_SYMBOLICNAME, systemPackages.toString(), systemEEs.toString());
		Module systemBundle = container.install(null, Constants.SYSTEM_BUNDLE_LOCATION, systemModuleBuilder,
				new AtomicReference<>());
		executionEnvironments.addAll(systemBundle.getCurrentRevision()
				.getDeclaredCapabilities(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE));

		Module m1 = installModule(container, "m1", "1.0", "test.pkg1", "JavaSE-1.5",
				executionEnvironments);
		Module m2 = installModule(container, "m2", "1.0", "test.pkg1,test.pkg2", "JavaSE-1.5",
				executionEnvironments);

		Module m3 = installModule(container, "m3", "1.0", "test.pkg1,test.pkg2,test.pkg3", "JavaSE-1.7",
				executionEnvironments);
		Module m4 = installModule(container, "m4", "1.0", "test.pkg1,test.pkg2,test.pkg3,test.pkg4", "JavaSE-1.7",
				executionEnvironments);

		ResolutionReport report = container.resolve(Arrays.asList(m1, m2, m3, m4), false);
		assertEquals("Wrong state for m1.", State.RESOLVED, m1.getState());
		assertEquals("Wrong state for m2.", State.INSTALLED, m2.getState());
		assertEquals("Wrong state for m3.", State.RESOLVED, m3.getState());
		assertEquals("Wrong state for m4.", State.INSTALLED, m4.getState());

		String resolutionErrorM2 = report.getResolutionReportMessage(m2.getCurrentRevision());
		assertNotNull("Expected resolution message", resolutionErrorM2);
		assertTrue("Wrong resolution message.", resolutionErrorM2.contains("test.pkg2"));

		String resolutionErrorM4 = report.getResolutionReportMessage(m4.getCurrentRevision());
		assertNotNull("Expected resolution message", resolutionErrorM4);
		assertTrue("Wrong resolution message.", resolutionErrorM4.contains("test.pkg4"));
	}

	private Map<String, String> getSystemManifest() {
		Map<String, String> result = new HashMap<>();
		result.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		result.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.osgi");
		result.put(Constants.BUNDLE_VERSION, "1.0");
		result.put(Constants.EXPORT_PACKAGE, "org.osgi.framework; version=1.10");
		return result;
	}

	Module installModule(ModuleContainer container, String bsn, String version, String imports, String eeReqs,
			List<BundleCapability> availableEEs) throws BundleException {
		ModuleRevisionBuilder builder = createModuleRevisionBuilder(bsn, version, imports, eeReqs);
		Module m = container.install(null, bsn, builder, new AtomicReference<>());
		// Use a revision info that holds the EE_INDEX of the bundle based
		// on the EE capability that matches the modules EE requirement
		Collection<BundleRequirement> eeBundleReqs = m.getCurrentRevision()
				.getDeclaredRequirements(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);

		eeBundleReqs.stream().findFirst().ifPresent(r -> {
			// notice that we depend on the available EEs
			// to be ordered from least to greatest
			for (BundleCapability ee : availableEEs) {
				if (r.matches(ee)) {
					((AtomicReference<Long>) m.getCurrentRevision().getRevisionInfo())
							.set((Long) ee.getAttributes().get(EE_INDEX));
					return;
				}
			}
		});
		return m;
	}

	@SuppressWarnings("deprecation")
	ModuleRevisionBuilder createModuleRevisionBuilder(String bsn, String version, String imports, String eeReq)
			throws BundleException {
		Map<String, String> manifest = new HashMap<>();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, bsn);
		manifest.put(Constants.BUNDLE_VERSION, version);
		manifest.put(Constants.IMPORT_PACKAGE, imports);
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, eeReq);
		return OSGiManifestBuilderFactory.createBuilder(manifest);
	}
}
