/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
package org.eclipse.osgi.tests.bundles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleWiring;

public class AddDynamicImportTests extends AbstractBundleTests {


	public static Test suite() {
		return new TestSuite(AddDynamicImportTests.class);
	}


	public void testAddDynamicImportMultipleTimes() throws Exception {
		runTest((a, b) -> {
			for (int i = 0; i < 1000; i++) {
				BundleLoader bl = ((ModuleClassLoader) b.adapt(BundleWiring.class).getClassLoader()).getBundleLoader();
				Map<BundleLoader, Boolean> addedDynamicImport = new ConcurrentHashMap<>();

				List<Runnable> runs = new ArrayList<>();
				Runnable test = () -> {
					addedDynamicImport.computeIfAbsent(bl, (bl2) -> {
						// this should clear the miss cache
						bl2.addDynamicImportPackage(createImports("org.osgi.framework"));
						return true;
					});
					try {
						b.loadClass("org.osgi.framework.Bundle");
					} catch (ClassNotFoundException e) {
						fail("Should find class.", e);
					}
				};

				// test with three threads in a parallel stream
				runs.add(test);
				runs.add(test);
				runs.add(test);

				runs.parallelStream().forEach(Runnable::run);

				Module mb = bl.getWiring().getRevision().getRevisions().getModule();
				mb.getContainer().refresh(Collections.singletonList(mb));
			}
		});
	}

	public void testAddDynamicImportWhileDynamicWiring() throws Exception {
		runTest((a, b) -> {

			for (int i = 0; i < 1000; i++) {
				System.out.println("Doing " + i);
				BundleLoader bl = ((ModuleClassLoader) b.adapt(BundleWiring.class).getClassLoader()).getBundleLoader();
				List<Runnable> runs = new ArrayList<>();
				Runnable testAddDynamic = () -> {
					// this should clear the miss cache
					bl.addDynamicImportPackage(createImports("org.osgi.framework"));
					try {
						b.loadClass("org.osgi.framework.Bundle");
					} catch (ClassNotFoundException e) {
						fail("Should find class.", e);
					}
				};

				AtomicInteger pkgNumber = new AtomicInteger(1);
				Runnable testResolveDynamic = () -> {
					a.getResource("test/export/pkg" + pkgNumber.getAndAdd(1) + "/DoesNotExist.txt");
				};

				// test with three threads in a parallel stream
				runs.add(testAddDynamic);
				runs.add(testResolveDynamic);
				runs.add(testResolveDynamic);
				runs.add(testResolveDynamic);
				runs.add(testResolveDynamic);

				runs.parallelStream().forEach(Runnable::run);

				assertEquals("Wrong number of required wires for wiring A", 4,
						a.adapt(BundleWiring.class).getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE).size());
				Module m = bl.getWiring().getRevision().getRevisions().getModule();
				m.getContainer().refresh(Collections.singletonList(m));
			}
		});
	}

	ManifestElement[] createImports(String packages) {
		try {
			return ManifestElement.parseHeader(Constants.DYNAMICIMPORT_PACKAGE, packages);
		} catch (BundleException e) {
			fail("Unexpected Exception", e);
			return null;
		}
	}

	private void runTest(BiConsumer<Bundle, Bundle> testConsumer) {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());

		Map<String, String> headersA = new HashMap<>();
		headersA.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headersA.put(Constants.BUNDLE_SYMBOLICNAME, getName() + "A");
		headersA.put(Constants.DYNAMICIMPORT_PACKAGE, "test.export.*");

		Map<String, String> headersB = new HashMap<>();
		headersB.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headersB.put(Constants.BUNDLE_SYMBOLICNAME, getName() + "B");
		headersB.put(Constants.EXPORT_PACKAGE,
				"test.export.pkg1, test.export.pkg2, test.export.pkg3, test.export.pkg4");

		config.mkdirs();

		try {
			File bundleAFile = SystemBundleTests.createBundle(config, getName() + "A", headersA);
			File bundleBFile = SystemBundleTests.createBundle(config, getName(), headersB);

			Map<String, String> fwkConfig = new HashMap<>();
			fwkConfig.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
			fwkConfig.put(EquinoxConfiguration.PROP_RESOLVER_BATCH_TIMEOUT, "10000000");
			Equinox equinox = new Equinox(fwkConfig);
			try {
				equinox.start();
				BundleContext systemContext = equinox.getBundleContext();

				Bundle bundleA = systemContext.installBundle(bundleAFile.toURI().toString());
				bundleA.start();

				Bundle bundleB = systemContext.installBundle(bundleBFile.toURI().toString());
				bundleB.start();

				// load the class first will cause a miss cache to be added.
				try {
					bundleA.loadClass("org.osgi.framework.Bundle");
					fail("Expected to fail class load.");
				} catch (ClassNotFoundException e) {
					// expected
				}
				testConsumer.accept(bundleA, bundleB);
			} catch (BundleException be) {
				fail("Unexpected exception.", be);
			} finally {
				stopQuietly(equinox);
			}
		} catch (IOException ioe) {
			fail("Unexpected exception", ioe);
		}

	}
}
