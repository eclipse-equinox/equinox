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
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.tests.services.resolver.AbstractStateTest;

public class StatePerformanceTest extends AbstractStateTest {
	private Random random;
	// value copied from StateObjectFactoryImpl
	private static final byte GREATER_OR_EQUAL = 4;

	public StatePerformanceTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		// uses a constant seed to prevent variation on results
		this.random = new Random(0);
	}

	private State storeAndRetrieve(State toStore) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		toStore.getFactory().writeState(toStore, dos);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		DataInputStream dis = new DataInputStream(bais);
		return toStore.getFactory().readState(dis);
	}

	public void testCreation() throws IOException {
		int stateSize = 5000;
		buildRandomState(stateSize);
	}

	public void testStoreAndRetrieve() throws IOException {
		int stateSize = 5000;
		State originalState = buildRandomState(stateSize);
		storeAndRetrieve(originalState);
	}

	public void testResolution5000() throws IOException {
		testResolution(5000);
	}

	public void testResolution1000() throws IOException {
		testResolution(1000);
	}

	public void testResolution500() throws IOException {
		testResolution(500);
	}

	public void testResolution100() throws IOException {
		testResolution(100);
	}

	private void testResolution(int stateSize) throws IOException {
		State originalState = buildRandomState(stateSize);
		State retrievedState = storeAndRetrieve(originalState);
		assertEquals("0.9", 0, retrievedState.getChanges().getChanges().length);
		assertIdentical("1.0", originalState, retrievedState);
		originalState.resolve();
		retrievedState = storeAndRetrieve(originalState);
		assertIdentical("2.0", originalState, retrievedState);
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

			int packageCount = random.nextInt(10);
			PackageSpecification[] packages = new PackageSpecification[packageCount];
			for (int j = 0; j < packages.length; j++) {
				boolean export = false;
				String packageName;
				if (exportedPackages == 0 || (exportedPackages < packages.length && random.nextInt(10) > 6)) {
					export = true;
					packageName = "package." + ++exportedPackages;
				} else
					packageName = "package." + (random.nextInt(exportedPackages) + 1);
				Version packageVersion = new Version("1.0.0");
				packages[j] = stateFactory.createPackageSpecification(packageName, packageVersion, export);
			}
			BundleSpecification[] requiredBundles = new BundleSpecification[Math.min(i, random.nextInt(5))];
			for (int j = 0; j < requiredBundles.length; j++) {
				int requiredIndex = random.nextInt(i);
				String requiredName = bundles[requiredIndex].getSymbolicName();
				Version requiredVersion = bundles[requiredIndex].getVersion();
				boolean export = random.nextInt(10) > 6;
				boolean optional = random.nextInt(10) > 8;
				requiredBundles[j] = stateFactory.createBundleSpecification(requiredName, requiredVersion, GREATER_OR_EQUAL, export, optional);
			}

			int providedPackageCount = random.nextInt(10);
			String[] providedPackages = new String[providedPackageCount];
			for (int j = 0; j < providedPackages.length; j++)
				providedPackages[j] = symbolicName + ".package" + j;
			bundles[i] = stateFactory.createBundleDescription(bundleId, symbolicName, version, symbolicName, requiredBundles, (HostSpecification) null, packages, providedPackages, random.nextDouble() > 0.05);
			state.addBundle(bundles[i]);
		}
		return state;
	}

	public static Test suite() {
		return new TestSuite(StatePerformanceTest.class);
	}

}