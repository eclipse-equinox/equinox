/*******************************************************************************
 * Copyright (c) 2026, 2026 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.spi.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.spi.tests.impl.TestServiceConsumerImpl;
import org.eclipse.equinox.spi.tests.impl.TestServiceImpl;
import org.eclipse.equinox.spi.tests.service.TestService;
import org.eclipse.equinox.spi.tests.service.TestServiceConsumer;
import org.eclipse.osgi.internal.framework.ContextFinder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

class ServiceLoaderMediatorTest {

	private static ClassLoader originalTCCL;

	@BeforeAll
	static void setTCCL() {
		// In PDE Plug-in tests the Context Classloader is set by PDE to enable JUnit.
		// For this test, set it back to the usual default ContextFinder.
		originalTCCL = Thread.currentThread().getContextClassLoader();
		BundleContext bundleContext = getBundleContext();
		ServiceReference<ClassLoader> reference = bundleContext.getServiceReference(ClassLoader.class);
		@SuppressWarnings("restriction")
		ContextFinder contextFinder = (ContextFinder) bundleContext.getService(reference);
		Thread.currentThread().setContextClassLoader(contextFinder);
	}

	@AfterAll
	static void resetTCCL() {
		Thread.currentThread().setContextClassLoader(originalTCCL);
	}

	@Test
	void simpleLoad() throws Exception {

		Path providerRoot = dataDir.resolve("provider");
		copyClassIntoBundle(providerRoot, TestServiceImpl.class);
		createServiceproviderConfigurationFile(providerRoot, TestService.class, TestServiceImpl.class);
		installBundle(providerRoot, "service.provider", getServiceProviderHeaders(TestService.class));

		Path consumerRoot = dataDir.resolve("consumer");
		copyClassIntoBundle(consumerRoot, TestServiceConsumerImpl.class);
		copyDSComponentFile(consumerRoot, TestServiceConsumerImpl.class);
		installBundle(consumerRoot, "service.consumer", getServiceConsumerHeaders(TestService.class));

		TestServiceConsumer serviceConsumer = getTestServiceConsumer();

		assertTrue(serviceConsumer.findFirst());
	}

	@Test
	void loadFromDelayedProvider() throws Exception {

		Path consumerRoot = dataDir.resolve("consumer");
		copyClassIntoBundle(consumerRoot, TestServiceConsumerImpl.class);
		copyDSComponentFile(consumerRoot, TestServiceConsumerImpl.class);
		installBundle(consumerRoot, "service.consumer", getServiceConsumerHeaders(TestService.class));

		TestServiceConsumer serviceConsumer = getTestServiceConsumer();

		assertFalse(serviceConsumer.findFirst());

		Path providerRoot = dataDir.resolve("provider");
		copyClassIntoBundle(providerRoot, TestServiceImpl.class);
		createServiceproviderConfigurationFile(providerRoot, TestService.class, TestServiceImpl.class);
		installBundle(providerRoot, "service.provider", getServiceProviderHeaders(TestService.class));
		assertTrue(serviceConsumer.findFirst());
	}

	// TODO: Test services provided and consumed by fragments

	// TODO: Test with and without restricted visibility of consumers
	// TODO: Test registration as OSGi service (and not being registered as service)

	// TODO: Test services are registered as OSGi service when the bundle is started
	// and removed when the bundle is stopped.

	// --- utility methods ---

	// --- file IO ---

	@TempDir
	Path dataDir;

	static void createServiceproviderConfigurationFile(Path bundleRoot, Class<?> serviceClass, Class<?> providerClass)
			throws IOException {
		writeString(bundleRoot.resolve("META-INF/services/" + serviceClass.getName()), providerClass.getName());
	}

	static void writeString(Path file, String content) throws IOException {
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
	}

	static void copyClassIntoBundle(Path bundleRoot, Class<?> clazz) throws IOException {
		String classFilename = clazz.getName().replace('.', '/') + ".class";
		copyClassloaderResource(classFilename, bundleRoot.resolve(classFilename));
	}

	static void copyDSComponentFile(Path bundleRoot, Class<?> clazz) throws IOException {
		String dsServiceFile = "OSGI-INF/" + clazz.getName() + ".xml";
		copyClassloaderResource(dsServiceFile, bundleRoot.resolve(dsServiceFile));
	}

	static void copyClassloaderResource(String filePath, Path targetFile) throws IOException {
		Files.createDirectories(targetFile.getParent());
		try (InputStream content = ServiceLoaderMediatorTest.class.getClassLoader().getResourceAsStream(filePath)) {
			Files.copy(content, targetFile);
		}
	}

	// --- OSGi utilities ---

	static ServiceTracker<TestServiceConsumer, TestServiceConsumer> serviceConsumerTracker;

	@BeforeAll
	static void trackOtherBundleServices() {
		serviceConsumerTracker = new ServiceTracker<>(getBundleContext(), TestServiceConsumer.class, null) {
			@Override
			public TestServiceConsumer addingService(ServiceReference<TestServiceConsumer> reference) {
				if (reference.getBundle() == getBundleContext().getBundle()) {
					return null;
				}
				return super.addingService(reference);
			}
		};
		serviceConsumerTracker.open();
	}

	TestServiceConsumer getTestServiceConsumer() {
		TestServiceConsumer service = serviceConsumerTracker.getService();
		assertNotSame(getBundleContext().getBundle(), FrameworkUtil.getBundle(service.getClass()));
		return service;
	}

	static String[] getServiceProviderHeaders(Class<?> serviceClass) {
		return new String[] {
				"Require-Capability: osgi.extender;filter:=\"(osgi.extender=osgi.serviceloader.registrar)\"",
				"Provide-Capability: osgi.serviceloader;osgi.serviceloader=" + serviceClass.getName() };
	}

	static String[] getServiceConsumerHeaders(Class<?> serviceClass) {
		return new String[] { "Require-Capability: "
				+ String.join(",", "osgi.extender;filter:=\"(osgi.extender=osgi.serviceloader.processor)\"",
						"osgi.serviceloader;filter:=\"(osgi.serviceloader=" + serviceClass.getName()
						+ ")\";cardinality:=multiple;resolution:=optional"), };
	}

	static BundleContext getBundleContext() {
		Bundle bundle = FrameworkUtil.getBundle(ServiceLoaderMediatorTest.class);
		return bundle.getBundleContext();
	}

	void installBundle(Path bundleRoot, String bundleSymbolicName, String... extraHeaders)
			throws IOException, BundleException {
		String bree = "JavaSE-" + Runtime.version().feature();
		String content = String.format("""
				Manifest-Version: 1.0
				Bundle-ManifestVersion: 2
				Bundle-SymbolicName: %s
				Bundle-Version: 1.0.0.qualifier
				Bundle-RequiredExecutionEnvironment: %s
				Import-Package: org.eclipse.equinox.spi.tests.service
				Service-Component: OSGI-INF/*.xml
				""", bundleSymbolicName, bree) + String.join("\n", extraHeaders);

		writeString(bundleRoot.resolve("META-INF/MANIFEST.MF"), content);

		Bundle bundle = getBundleContext().installBundle(bundleRoot.toUri().toString());
		bundle.start();
		installedBundles.add(bundle);
	}

	private final List<Bundle> installedBundles = new ArrayList<>();

	@AfterEach
	void removeInstalledBundles() throws BundleException {
		for (Bundle bundle : installedBundles) {
			bundle.stop();
			bundle.uninstall();
		}
	}

}
