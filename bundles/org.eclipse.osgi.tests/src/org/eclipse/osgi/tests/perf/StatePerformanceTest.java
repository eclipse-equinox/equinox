/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.perf;

import java.io.*;
import java.util.Random;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.tests.services.resolver.AbstractStateTest;
import org.osgi.framework.Version;

public class StatePerformanceTest extends AbstractStateTest {
	private Random random;

	public static Test suite() {
		return new TestSuite(StatePerformanceTest.class);
	}

	public StatePerformanceTest(String name) {
		super(name);
	}

	private State buildRandomState(int size) {
		State state = buildEmptyState();
		StateObjectFactory stateFactory = state.getFactory();
		BundleDescription[] bundles = new BundleDescription[size];
		int exportedPackages = 0;
		for (int i = 0; i < bundles.length; i++) {
			long bundleId = i;
			String symbolicName = "bundle" + i;
			Version version = new Version(1, 0, 0);

			int exportPackageCount = random.nextInt(5);
			ExportPackageDescription[] exportPackages = new ExportPackageDescription[exportPackageCount];
			for (int j = 0; j < exportPackages.length; j++) {
				String packageName = "package." + ++exportedPackages;
				Version packageVersion = Version.parseVersion("1.0.0");
				exportPackages[j] = stateFactory.createExportPackageDescription(packageName, packageVersion, null, null, null, null, null, true);
			}
			int importPackageCount = Math.min(exportPackageCount, random.nextInt(5));
			int importedPackageIndex = random.nextInt(exportPackageCount + 1);
			ImportPackageSpecification[] importPackages = new ImportPackageSpecification[importPackageCount];
			for (int j = 0; j < importPackages.length; j++) {
				int index = importedPackageIndex++;
				if (importedPackageIndex > exportPackageCount)
					importedPackageIndex = 1;
				String packageName = "package." + index;
				importPackages[j] = stateFactory.createImportPackageSpecification(packageName,new VersionRange("1.0.0"), null, null, null, ImportPackageSpecification.RESOLUTION_STATIC, null);
			}
			
			BundleSpecification[] requiredBundles = new BundleSpecification[Math.min(i, random.nextInt(5))];
			for (int j = 0; j < requiredBundles.length; j++) {
				int requiredIndex = random.nextInt(i);
				String requiredName = bundles[requiredIndex].getSymbolicName();
				Version requiredVersion = bundles[requiredIndex].getVersion();
				boolean export = random.nextInt(10) > 6;
				boolean optional = random.nextInt(10) > 8;
				requiredBundles[j] = stateFactory.createBundleSpecification(requiredName, new VersionRange(requiredVersion.toString()), export, optional);
			}

			bundles[i] = stateFactory.createBundleDescription(bundleId, symbolicName, version, symbolicName, requiredBundles, (HostSpecification) null, importPackages, exportPackages, null, random.nextDouble() > 0.05);
			state.addBundle(bundles[i]);
		}
		return state;
	}

	protected void setUp() throws Exception {
		super.setUp();
		// uses a constant seed to prevent variation on results
		this.random = new Random(0);
	}

	private State storeAndRetrieve(State toStore) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		toStore.getFactory().writeState(toStore, baos);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		return toStore.getFactory().readState(bais);
	}

	public void testCreation() {
		final int stateSize = 5000;
		new PerformanceTestRunner() {
			protected void test() {
				buildRandomState(stateSize);
			}
		}.run(this, 10, 10);
	}

	private void testResolution(int stateSize, int repetitions) {
		final State originalState = buildRandomState(stateSize);
		new PerformanceTestRunner() {
			protected void test() {
				originalState.resolve(false);
			}
		}.run(this, 10, repetitions);
	}

	public void testResolution100() throws IOException {
		testResolution(100, 500);
	}

	public void testResolution1000() throws IOException {
		testResolution(1000, 15);
	}

	public void testResolution500() throws IOException {
		testResolution(500, 50);
	}

	public void testResolution5000() throws IOException {
		testResolution(5000, 1);
	}

	public void testStoreAndRetrieve() {
		int stateSize = 5000;
		final State originalState = buildRandomState(stateSize);
		new PerformanceTestRunner() {
			protected void test() {
				try {
					storeAndRetrieve(originalState);
				} catch (IOException e) {
					CoreTest.fail("", e);
				}
			}
		}.run(this, 10, 10);
	}

}