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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.eclipse.equinox.spi.tests.impl.TestServiceConsumerImpl;
import org.eclipse.equinox.spi.tests.impl.TestServiceImpl;
import org.eclipse.equinox.spi.tests.service.TestService;
import org.eclipse.equinox.spi.tests.service.TestServiceConsumer;
import org.eclipse.osgi.internal.framework.ContextFinder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.util.tracker.ServiceTracker;

class ServiceLoaderMediatorTest {

	private static ClassLoader originalTCCL;

	private static Bundle aBundle = FrameworkUtil.getBundle(ServiceLoaderMediatorTest.class);

	@BeforeAll
	static void setTCCL() {
		// In PDE Plug-in tests the Context Classloader is set by PDE to enable JUnit.
		// For this test, set it back to the usual default ContextFinder.
		originalTCCL = Thread.currentThread().getContextClassLoader();
		BundleContext bundleContext = aBundle.getBundleContext();
		ServiceReference<ClassLoader> reference = bundleContext.getServiceReference(ClassLoader.class);
		@SuppressWarnings("restriction")
		ContextFinder contextFinder = (ContextFinder) bundleContext.getService(reference);
		Thread.currentThread().setContextClassLoader(contextFinder);
	}

	@AfterAll
	static void resetTCCL() {
		Thread.currentThread().setContextClassLoader(originalTCCL);
	}

	// --- test cases ---

	@Test
	void serviceLoadFromProvider() throws Exception {
		installSimpleProvider("service.provider1", "val100");
		installSimpleConsumer("service.consumer1");

		TestServiceConsumer serviceConsumer = getTestServiceConsumer();
		assertServiceIsPresent(serviceConsumer, "val100");
	}

	@Test
	void serviceLoadFromDelayedProvider() throws Exception {
		installSimpleConsumer("service.consumer2");

		TestServiceConsumer serviceConsumer = getTestServiceConsumer();
		assertServiceIsNotPresent(serviceConsumer);

		installSimpleProvider("service.provider2", "val200");

		serviceConsumer = getTestServiceConsumer();
		assertServiceIsPresent(serviceConsumer, "val200");
	}

	// OSGi service registration

	@Test
	void registrationAsOSGiService() throws Exception {
		BundleContext context = aBundle.getBundleContext();

		Bundle provider = installSimpleProvider("service.provider3", "val300");

		assertNull(context.getServiceReference(TestService.class));

		startBundle(provider);

		assertNotNull(context.getServiceReference(TestService.class));
	}

	// TODO: Test registration as OSGi service (and not being registered as service)

	@Test
	void unregistrationAsOSGiService() throws Exception {
		BundleContext context = aBundle.getBundleContext();

		Bundle provider = installSimpleProvider("service.provider4", "val400");

		startBundle(provider);
		assertNotNull(context.getServiceReference(TestService.class));
		stopBundle(provider);
		assertNull(context.getServiceReference(TestService.class));
	}

	// Services providers and consumers in fragments

	@Test
	void serviceLoadFromFragmentProvider() throws Exception {

		installEmptyBundle("a.bundle5");
		installSimpleProvider("service.provider.fragment5", "val500", "Fragment-Host: a.bundle");

		installSimpleConsumer("service.consumer5");

		TestServiceConsumer serviceConsumer = getTestServiceConsumer();
		assertServiceIsPresent(serviceConsumer, "val500");
	}

	@Test
	void registrationAsOSGiServiceFromFragmentProvider() throws Exception {

		Bundle providerHost = installEmptyBundle("a.bundle6");
		installSimpleProvider("service.provider.fragment6", "val600", "Fragment-Host: a.bundle");

		assertThatOSGiServiceIsNotAvailable();

		startBundle(providerHost);

		assertThatOSGiServiceIsAvailable();
	}

	@Test
	void unregistrationAsOSGiServiceFromFragmentProvider() throws Exception {
		Bundle providerHost = installEmptyBundle("a.bundle7");
		startBundle(providerHost);
		installSimpleProvider("service.provider.fragment7", "val700", "Fragment-Host: a.bundle");

		assertThatOSGiServiceIsAvailable();

		stopBundle(providerHost);

		assertThatOSGiServiceIsNotAvailable();
	}

	@Test
	void serviceConsumedFromFragment() throws Exception {
		installSimpleProvider("service.provider8", "val800");

		// Add Service-Component header to host, since they are ignored on fragments
		installEmptyBundle("a.bundle8", "Bundle-ActivationPolicy: lazy", "Service-Component: OSGI-INF/*.xml");
		installSimpleConsumer("service.consumer.fragment8", "Fragment-Host: a.bundle");

		TestServiceConsumer serviceConsumer = getTestServiceConsumer();
		assertServiceIsPresent(serviceConsumer, "val800");
	}

	// TODO: Test with and without restricted visibility of consumers

	// --- utility methods ---

	private Bundle installSimpleProvider(String symbolicName, String value, String... extraHeaders) throws Exception {
		List<String> headers = new ArrayList<>(getServiceProviderHeaders(TestService.class));
		headers.addAll(Arrays.asList(extraHeaders));

		return installBasicBundle(value, symbolicName, root -> {
			copyClassIntoBundle(root, TestServiceImpl.class);
			createServiceproviderConfigurationFile(root, TestService.class, TestServiceImpl.class);
		}, headers);
	}

	private Bundle installSimpleConsumer(String symbolicName, String... extraHeaders) throws Exception {
		List<String> headers = new ArrayList<>(getServiceConsumerHeaders(TestService.class));
		headers.addAll(Arrays.asList(extraHeaders));
		return installBasicBundle(null, symbolicName, root -> {
			copyClassIntoBundle(root, TestServiceConsumerImpl.class);
			copyDSComponentFile(root, TestServiceConsumerImpl.class);
		}, headers);
	}

	private Bundle installEmptyBundle(String symbolicName, String... extraHeaders) throws Exception {
		return installBasicBundle(null, symbolicName, r -> {
		}, Arrays.asList(extraHeaders));
	}

	private static final Set<String> ALL_VALUES = new HashSet<>();

	private <E extends Exception> Bundle installBasicBundle(String value, String symbolicName,
			ThrowingConsumer<Path, E> modifier, List<String> extraHeaders) throws Exception {
		Path bundleRoot = Files.createDirectories(dataDir.resolve(symbolicName));
		if (value != null) {
			if (!ALL_VALUES.add(value)) {
				fail("Value already used in other test-case: " + value);
			}
			Files.writeString(bundleRoot.resolve("value.txt"), value);
		}
		modifier.accept(bundleRoot);
		return installBundle(bundleRoot, symbolicName, extraHeaders);
	}

	private void assertServiceIsNotPresent(TestServiceConsumer serviceConsumer) {
		assertFalse(serviceConsumer.findFirst().isPresent());
	}

	private void assertServiceIsPresent(TestServiceConsumer serviceConsumer, String expectedValue) {
		// for (Iterator<TestService> iterator = serviceConsumer.iterator(); iterator
		// .hasNext();) {
		// TestService service = (TestService) iterator .next();
		// try {
		// assertEquals(expectedValue, service.getValue());
		// return;
		// } catch (Exception e) { // ignore
		// }
		// }
		// if (true) {
		// fail("No service fond");
		// }
		Optional<TestService> service = serviceConsumer.findFirst();
		assertTrue(service.isPresent());
		assertEquals(expectedValue, service.get().getValue());
	}

	private void assertThatOSGiServiceIsNotAvailable() {
		assertNull(aBundle.getBundleContext().getServiceReference(TestService.class));
	}

	private void assertThatOSGiServiceIsAvailable() {
		assertNotNull(aBundle.getBundleContext().getServiceReference(TestService.class));
	}

	// --- file IO ---

	@TempDir
	Path dataDir;

	private static void createServiceproviderConfigurationFile(Path bundleRoot, Class<?> serviceClass,
			Class<?> providerClass)
					throws IOException {
		writeString(bundleRoot.resolve("META-INF/services/" + serviceClass.getName()), providerClass.getName());
	}

	private static void writeString(Path file, String content) throws IOException {
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
	}

	private static void copyClassIntoBundle(Path bundleRoot, Class<?> clazz) throws IOException {
		String classFilename = clazz.getName().replace('.', '/') + ".class";
		copyClassloaderResource(classFilename, bundleRoot.resolve(classFilename));
	}

	private static void copyDSComponentFile(Path bundleRoot, Class<?> clazz) throws IOException {
		String dsServiceFile = "OSGI-INF/" + clazz.getName() + ".xml";
		copyClassloaderResource(dsServiceFile, bundleRoot.resolve(dsServiceFile));
	}

	private static void copyClassloaderResource(String filePath, Path targetFile) throws IOException {
		Files.createDirectories(targetFile.getParent());
		try (InputStream content = ServiceLoaderMediatorTest.class.getClassLoader().getResourceAsStream(filePath)) {
			Files.copy(content, targetFile);
		}
	}

	private interface ThrowingRunnable<E extends Exception> {
		void run() throws E;
	}

	private interface ThrowingConsumer<T, E extends Exception> {
		void accept(T t) throws E;
	}

	// --- OSGi utilities ---

	private static ServiceTracker<TestServiceConsumer, TestServiceConsumer> serviceConsumerTracker;

	@BeforeEach
	void trackOtherBundleServices() {
		serviceConsumerTracker = new ServiceTracker<>(aBundle.getBundleContext(), TestServiceConsumer.class, null) {
			@Override
			public TestServiceConsumer addingService(ServiceReference<TestServiceConsumer> reference) {
				if (reference.getBundle() == this.context.getBundle()) {
					return null;
				}
				return super.addingService(reference);
			}
		};
		serviceConsumerTracker.open();
	}

	private TestServiceConsumer getTestServiceConsumer() throws InterruptedException {
		Object[] services = serviceConsumerTracker.getServices();
		for (int i = 0; i < 100 && services == null; i++) {
			Thread.sleep(10);
			services = serviceConsumerTracker.getServices();
		}
		if (services.length > 1) {
			System.out.println("More");
		}
		TestServiceConsumer service = (TestServiceConsumer) services[0];
		assertNotSame(aBundle, FrameworkUtil.getBundle(service.getClass()));
		return service;
	}

	private static List<String> getServiceProviderHeaders(Class<?> serviceClass) {
		return List.of("Require-Capability: osgi.extender;filter:=\"(osgi.extender=osgi.serviceloader.registrar)\"",
				"Provide-Capability: osgi.serviceloader;osgi.serviceloader=" + serviceClass.getName());
	}

	private static List<String> getServiceConsumerHeaders(Class<?> serviceClass) {
		return List.of("Bundle-ActivationPolicy: lazy",
				"Require-Capability: osgi.extender;filter:=\"(osgi.extender=osgi.serviceloader.processor)\",osgi.serviceloader;filter:=\"(osgi.serviceloader="
						+ serviceClass.getName() + ")\";cardinality:=multiple;resolution:=optional");
	}

	private Bundle installBundle(Path bundleRoot, String bundleSymbolicName, List<String> extraHeaders)
			throws Exception {

		String bree = "JavaSE-" + Runtime.version().feature();
		String content = String.format("""
				Manifest-Version: 1.0
				Bundle-ManifestVersion: 2
				Bundle-SymbolicName: %s
				Bundle-Version: 1.0.0.qualifier
				Bundle-RequiredExecutionEnvironment: %s
				Import-Package: org.eclipse.equinox.spi.tests.service, org.osgi.framework
				Service-Component: OSGI-INF/*.xml
				""", bundleSymbolicName, bree) + String.join("\n", extraHeaders) + "\n";

		writeString(bundleRoot.resolve("META-INF/MANIFEST.MF"), content);

		BundleContext context = aBundle.getBundleContext();
		Bundle bundle = context.installBundle(bundleRoot.toUri().toString());
		installedBundles.add(bundle);

		Bundle systemBundle = context.getBundle(Constants.SYSTEM_BUNDLE_ID);
		if (!systemBundle.adapt(FrameworkWiring.class).resolveBundles(List.of(bundle))) {
			System.err.println("Failed to resolve " + bundle);
		}
		refreshFramework(installedBundles);
		int state = bundle.getState();
		if (state != Bundle.RESOLVED && state != Bundle.STARTING && state != Bundle.ACTIVE) {
			System.err.println("Not resolved but in state " + state + ": " + bundle);
		}
		return bundle;
	}

	private void startBundle(Bundle bundle) throws BundleException, InterruptedException {
		changeBundleState(bundle, () -> bundle.start(), BundleEvent.STARTED);
		if (bundle.getState() != Bundle.ACTIVE) {
			fail("Not started " + bundle);
		}
	}

	private void stopBundle(Bundle bundle) throws BundleException, InterruptedException {
		changeBundleState(bundle, () -> bundle.stop(), BundleEvent.STOPPED);
		if (bundle.getState() != Bundle.RESOLVED) {
			fail("Not stopped " + bundle);
		}
	}

	private void changeBundleState(Bundle bundle, ThrowingRunnable<BundleException> bundleChange, int eventType)
			throws BundleException, InterruptedException {
		CountDownLatch changed = new CountDownLatch(1);
		BundleListener listener = e -> {
			if (e.getType() == eventType && e.getBundle() == bundle) {
				changed.countDown();
			}
		};
		BundleContext context = aBundle.getBundleContext();
		context.addBundleListener(listener);
		bundleChange.run();
		changed.await();
		Thread.sleep(10); // TODO: Make sure all other kind of listeners are delivered
		context.removeBundleListener(listener);
	}

	private final List<Bundle> installedBundles = new ArrayList<>();

	@AfterEach
	void removeInstalledBundles() throws BundleException, InterruptedException {
		serviceConsumerTracker.close();
		for (Bundle bundle : installedBundles) {
			bundle.uninstall();
		}
		refreshFramework(null);
	}

	private void refreshFramework(List<Bundle> toRefresh) throws InterruptedException {
		CountDownLatch refresh = new CountDownLatch(1);
		Bundle systemBundle = aBundle.getBundleContext().getBundle(Constants.SYSTEM_BUNDLE_ID);
		systemBundle.adapt(FrameworkWiring.class).refreshBundles(toRefresh, event -> {
			refresh.countDown();
		});
		refresh.await();
	}

}
