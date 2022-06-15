/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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

import static org.eclipse.osgi.util.ManifestElement.parseHeader;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.util.ManifestElement;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleWiring;

public class AddDynamicImportTests extends AbstractBundleTests {

	@Test
	public void testAddDynamicImportMultipleTimes() throws Exception {
		ManifestElement[] packageImport = parseHeader(Constants.DYNAMICIMPORT_PACKAGE, "org.osgi.framework");
		// test with three threads in parallel
		runTest(3, (a, b, threadPool) -> {
			for (int i = 0; i < 1000; i++) {
				BundleLoader bl = ((ModuleClassLoader) b.adapt(BundleWiring.class).getClassLoader()).getBundleLoader();
				Map<BundleLoader, Boolean> addedDynamicImport = new ConcurrentHashMap<>();

				Callable<Void> test = () -> {
					addedDynamicImport.computeIfAbsent(bl, bl2 -> {
						// this should clear the miss cache
						bl2.addDynamicImportPackage(packageImport);
						return true;
					});
					b.loadClass("org.osgi.framework.Bundle");
					return null;
				};
				List<Future<Void>> results = threadPool.invokeAll(Arrays.asList(test, test, test));
				for (Future<Void> result : results) {
					result.get(); // propagate exceptions
				}
				refresh(bl);
			}
		});
	}

	@Test
	public void testAddDynamicImportWhileDynamicWiring() throws Exception {
		ManifestElement[] packageImport = parseHeader(Constants.DYNAMICIMPORT_PACKAGE, "org.osgi.framework");
		// test with five threads in parallel
		runTest(5, (a, b, threadPool) -> {
			for (int i = 0; i < 1000; i++) {
				System.out.println("Doing " + i);
				BundleLoader bl = ((ModuleClassLoader) b.adapt(BundleWiring.class).getClassLoader()).getBundleLoader();

				Callable<Void> addDynamic = () -> {
					// this should clear the miss cache
					bl.addDynamicImportPackage(packageImport);
					b.loadClass("org.osgi.framework.Bundle");
					return null;
				};

				AtomicInteger pkgNumber = new AtomicInteger(1);
				Callable<Void> resolveDynamic = () -> {
					a.getResource("test/export/pkg" + pkgNumber.getAndAdd(1) + "/DoesNotExist.txt");
					return null;
				};

				// test with three threads in a parallel stream
				List<Future<Void>> results = threadPool.invokeAll(
						Arrays.asList(addDynamic, resolveDynamic, resolveDynamic, resolveDynamic, resolveDynamic));
				for (Future<Void> result : results) {
					result.get(); // propagate exceptions
				}
				assertEquals("Wrong number of required wires for wiring A", 4,
						a.adapt(BundleWiring.class).getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE).size());
				refresh(bl);
			}
		});
	}

	private void refresh(BundleLoader bl) {
		Module module = bl.getWiring().getRevision().getRevisions().getModule();
		module.getContainer().refresh(Collections.singletonList(module));
	}

	private interface ThrowingBiConsumer<T, U> {
		void accept(T t, U u, ExecutorService executor) throws Exception;
	}

	private void runTest(int threads, ThrowingBiConsumer<Bundle, Bundle> testConsumer) throws Exception {
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

		File bundleAFile = SystemBundleTests.createBundle(config, getName() + "A", headersA);
		File bundleBFile = SystemBundleTests.createBundle(config, getName(), headersB);

		Map<String, String> fwkConfig = new HashMap<>();
		fwkConfig.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		fwkConfig.put(EquinoxConfiguration.PROP_RESOLVER_BATCH_TIMEOUT, "10000000");
		Equinox equinox = new Equinox(fwkConfig);
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();

			Bundle bundleA = systemContext.installBundle(bundleAFile.toURI().toString());
			bundleA.start();

			Bundle bundleB = systemContext.installBundle(bundleBFile.toURI().toString());
			bundleB.start();

			// load the class first will cause a miss cache to be added.
			assertThrows(ClassNotFoundException.class, () -> bundleA.loadClass("org.osgi.framework.Bundle"));
			testConsumer.accept(bundleA, bundleB, executor);
		} finally {
			executor.shutdown();
			stopQuietly(equinox);
		}
	}
}
