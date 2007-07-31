/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.perf;

import java.util.Random;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.tests.services.resolver.AbstractStateTest;
import org.osgi.framework.Version;

public class BasePerformanceTest extends AbstractStateTest {
	private Random random;

	public static Test suite() {
		return new TestSuite(BasePerformanceTest.class);
	}

	public BasePerformanceTest(String name) {
		super(name);
	}

	protected State buildRandomState(int size) {
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
				String packageName = "package." + exportedPackages++;
				Version packageVersion = Version.parseVersion("1.0.0");
				exportPackages[j] = stateFactory.createExportPackageDescription(packageName, packageVersion, null, null, true, null);
			}
			int importPackageCount = Math.min(exportPackageCount, random.nextInt(5));
			int importedPackageIndex = random.nextInt(exportPackageCount + 1);
			ImportPackageSpecification[] importPackages = new ImportPackageSpecification[importPackageCount];
			for (int j = 0; j < importPackages.length; j++) {
				int index = importedPackageIndex++;
				if (importedPackageIndex > exportPackageCount)
					importedPackageIndex = 1;
				String packageName = "package." + index;
				importPackages[j] = stateFactory.createImportPackageSpecification(packageName, new VersionRange("1.0.0"), null, null, null, null, null);
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

}
