/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
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

import static java.util.jar.Attributes.Name.MANIFEST_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.Module.StartOptions;
import org.eclipse.osgi.container.Module.State;
import org.eclipse.osgi.container.ModuleCapability;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ContainerEvent;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ModuleEvent;
import org.eclipse.osgi.container.ModuleRequirement;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.container.ModuleWire;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.container.builders.OSGiManifestBuilderFactory;
import org.eclipse.osgi.container.namespaces.EclipsePlatformNamespace;
import org.eclipse.osgi.container.namespaces.EquinoxModuleDataNamespace;
import org.eclipse.osgi.framework.util.ThreadInfoReport;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.eclipse.osgi.tests.container.dummys.DummyCollisionHook;
import org.eclipse.osgi.tests.container.dummys.DummyContainerAdaptor;
import org.eclipse.osgi.tests.container.dummys.DummyDebugOptions;
import org.eclipse.osgi.tests.container.dummys.DummyModuleDatabase;
import org.eclipse.osgi.tests.container.dummys.DummyModuleDatabase.DummyContainerEvent;
import org.eclipse.osgi.tests.container.dummys.DummyModuleDatabase.DummyModuleEvent;
import org.eclipse.osgi.tests.container.dummys.DummyResolverHook;
import org.eclipse.osgi.tests.container.dummys.DummyResolverHookFactory;
import org.eclipse.osgi.util.ManifestElement;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.resource.Namespace;

public class TestModuleContainer extends AbstractTest {

	private static DummyModuleDatabase resolvedModuleDatabase;

	private void setupModuleDatabase() throws BundleException {
		if (resolvedModuleDatabase == null) {
			resolvedModuleDatabase = getDatabase();
		}
	}

	private static final String OSGI_OS = "osgi.os";
	private static final String OSGI_WS = "osgi.ws";
	private static final String OSGI_ARCH = "osgi.arch";

	private DummyModuleDatabase getDatabase() throws BundleException {
		BundleContext context = ((BundleReference) getClass().getClassLoader()).getBundle().getBundleContext();

		DummyContainerAdaptor adaptor = createDummyAdaptor();
		final ModuleContainer container = adaptor.getContainer();

		Bundle systemBundle = context.getBundle(0);
		String extraPackages = context.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
		String extraCapabilities = context.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES);
		extraCapabilities = (extraCapabilities == null ? "" : (extraCapabilities + ", "));
		String osName = context.getProperty(OSGI_OS);
		String wsName = context.getProperty(OSGI_WS);
		String archName = context.getProperty(OSGI_ARCH);
		extraCapabilities += EclipsePlatformNamespace.ECLIPSE_PLATFORM_NAMESPACE + "; " + OSGI_OS + "=" + osName + "; " + OSGI_WS + "=" + wsName + "; " + OSGI_ARCH + "=" + archName;
		ModuleRevisionBuilder systembuilder = OSGiManifestBuilderFactory.createBuilder(asMap(systemBundle.getHeaders("")), Constants.SYSTEM_BUNDLE_SYMBOLICNAME, extraPackages, extraCapabilities);
		container.install(null, systemBundle.getLocation(), systembuilder, null);

		final List<Throwable> installErrors = new ArrayList<>(0);
		// just trying to pound the container with a bunch of installs
		ExecutorService executor = Executors.newFixedThreadPool(10);
		Bundle[] bundles = context.getBundles();
		for (final Bundle bundle : bundles) {
			if (bundle.getBundleId() == 0)
				continue;
			executor.execute(() -> {
				try {
					ModuleRevisionBuilder builder = OSGiManifestBuilderFactory.createBuilder(asMap(bundle.getHeaders("")));
					container.install(null, bundle.getLocation(), builder, null);
				} catch (Throwable t) {
					t.printStackTrace();
					synchronized (installErrors) {
						installErrors.add(t);
					}
				}
			});
		}
		executor.shutdown();
		try {
			executor.awaitTermination(2, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		synchronized (installErrors) {
			if (!installErrors.isEmpty()) {
				Assert.assertNull("Unexpected install errors.", installErrors);
			}
		}
		container.resolve(new ArrayList<>(), false);
		List<Module> modules = container.getModules();
		for (Module module : modules) {
			if (module.getCurrentRevision().getWiring() == null) {
				System.out.println("Could not resolve module: " + module.getCurrentRevision());
			}
		}
		return adaptor.getDatabase();
	}

	static <K, V> Map<K, V> asMap(Dictionary<K, V> dictionary) {
		Map<K, V> map = new HashMap<>();
		for (Enumeration<K> eKeys = dictionary.keys(); eKeys.hasMoreElements();) {
			K key = eKeys.nextElement();
			V value = dictionary.get(key);
			map.put(key, value);
		}
		return map;
	}

	@Test
	public void testModuleContainerCreate() {
		createDummyAdaptor();
	}

	// Disabled @Test
	public void testResolveInstallBundles() throws BundleException, IOException {
		setupModuleDatabase();
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		resolvedModuleDatabase.store(new DataOutputStream(bytes), false);
		bytes.close();
		adaptor.getDatabase().load(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		adaptor.getContainer().resolve(new ArrayList<>(), false);
	}

	// Disabled @Test
	public void testResolveInstallBundles01() throws BundleException, IOException {
		setupModuleDatabase();
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		resolvedModuleDatabase.store(new DataOutputStream(bytes), false);
		bytes.close();
		adaptor.getDatabase().load(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		for (int i = 0; i < 50; i++) {
			adaptor.getContainer().refresh(adaptor.getContainer().getModules());
		}
	}

	// Disabled @Test
	public void testResolveAlreadyResolvedBundles() throws BundleException, IOException {
		setupModuleDatabase();
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		resolvedModuleDatabase.store(new DataOutputStream(bytes), true);
		bytes.close();
		adaptor.getDatabase().load(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		adaptor.getContainer().resolve(new ArrayList<>(), false);
	}

	// Disabled @Test
	public void testRefreshSystemBundle() throws BundleException, IOException {
		setupModuleDatabase();
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		resolvedModuleDatabase.store(new DataOutputStream(bytes), true);
		bytes.close();
		adaptor.getDatabase().load(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		adaptor.getContainer().refresh(Arrays.asList(adaptor.getContainer().getModule(0)));
	}

	// disabled @Test
	public void testLoadPerformance() throws BundleException, IOException {
		setupModuleDatabase();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		resolvedModuleDatabase.store(new DataOutputStream(bytes), true);
		bytes.close();
		System.out.println("SIZE: " + bytes.size());
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			DummyContainerAdaptor adaptor = createDummyAdaptor();
			adaptor.getDatabase().load(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		}
		System.out.println("END: " + (System.currentTimeMillis() - start));
	}

	@Test
	public void testSimpleResolve() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);
		ModuleRevision systemRevision = systemBundle.getCurrentRevision();
		container.resolve(Arrays.asList(systemBundle), true);
		ModuleWiring systemWiring = systemRevision.getWiring();
		Assert.assertNotNull("system wiring is null", systemWiring);

		Module b1 = installDummyModule("b1_v1.MF", "b1", container);
		ModuleRevision b1Revision = b1.getCurrentRevision();
		container.resolve(Arrays.asList(b1), true);
		ModuleWiring b1Wiring = b1Revision.getWiring();
		Assert.assertNotNull("b1 wiring is null", b1Wiring);
	}

	@Test
	public void testSimpleUnResolveable() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);
		container.resolve(Arrays.asList(systemBundle), true);

		Module c7 = installDummyModule("c7_v1.MF", "c7", container);
		installDummyModule("c6_v1.MF", "c6", container);

		ResolutionReport report = container.resolve(Arrays.asList(c7), true);
		Assert.assertNotNull("Expected a resolution exception", report.getResolutionException());

		// Should resolve now
		installDummyModule("c4_v1.MF", "c4", container);
		report = container.resolve(Arrays.asList(c7), true);
		Assert.assertNull("Unexpected resoltuion exception", report.getResolutionException());
	}

	@Test
	public void testMultiHost() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);
		Module h1v1 = installDummyModule("h1_v1.MF", "h1_v1", container);
		Module h1v2 = installDummyModule("h1_v2.MF", "h1_v2", container);
		Module f1v1 = installDummyModule("f1_v1.MF", "f1_v1", container);
		Module b3 = installDummyModule("b3_v1.MF", "b3_v1", container);
		ResolutionReport report = container.resolve(Arrays.asList(h1v1, h1v2, f1v1, b3), true);
		Assert.assertNull("Expected no resolution exception.", report.getResolutionException());
	}

	@Test
	public void testMissingHost() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);
		Module f1v1 = installDummyModule("f1_v1.MF", "f1_v1", container);
		Module b3 = installDummyModule("b3_v1.MF", "b3_v1", container);
		ResolutionReport report = container.resolve(Arrays.asList(f1v1, b3), true);
		Assert.assertNotNull("Expected a resolution exception.", report.getResolutionException());

		Module h1v1 = installDummyModule("h1_v1.MF", "h1_v1", container);
		report = container.resolve(Arrays.asList(b3), true);
		Assert.assertNull("Expected no resolution exception.", report.getResolutionException());
		ModuleWiring wiring = b3.getCurrentRevision().getWiring();
		List<ModuleWire> packageWires = wiring.getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
		Assert.assertEquals("Expected 1 import.", 1, packageWires.size());
		ModuleWire pkgWire = packageWires.get(0);
		Assert.assertEquals("Wrong host exporter.", pkgWire.getProviderWiring().getRevision(), h1v1.getCurrentRevision());
	}

	@Test
	public void testFragments01() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		Module systemModule = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);
		Module c1 = installDummyModule("c1_v1.MF", "c1_v1", container);
		Module h2 = installDummyModule("h2_v1.MF", "h2_v1", container);
		Module f2 = installDummyModule("f2_v1.MF", "f2_v1", container);
		container.resolve(Arrays.asList(systemModule, c1, h2, f2), true);

		ModuleWiring wiring = h2.getCurrentRevision().getWiring();
		List<ModuleWire> requiredWires = wiring.getRequiredModuleWires(null);
		Assert.assertEquals("Wrong number of required wires.", 3, requiredWires.size());
		for (ModuleWire wire : requiredWires) {
			ModuleCapability capability = wire.getCapability();
			Assert.assertEquals("Wrong namespace.", PackageNamespace.PACKAGE_NAMESPACE, capability.getNamespace());

			Assert.assertEquals("Wrong requirer.", h2.getCurrentRevision(), wire.getRequirer());

			String pkgName = (String) capability.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
			Assert.assertNotNull("No package name.", pkgName);
			ModuleRevision expectedReqRevision;
			if (pkgName.equals("org.osgi.framework")) {
				expectedReqRevision = h2.getCurrentRevision();
			} else {
				expectedReqRevision = f2.getCurrentRevision();
			}
			Assert.assertEquals("Wrong requirement revision.", expectedReqRevision, wire.getRequirement().getRevision());
		}
	}

	@Test
	public void testFragments02() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		Module systemModule = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);
		Module c1 = installDummyModule("c1_v1.MF", "c1_v1", container);
		Module h2 = installDummyModule("h2_v1.MF", "h2_v1", container);

		container.resolve(Arrays.asList(systemModule, c1, h2), true);

		ModuleWiring h2wiring = h2.getCurrentRevision().getWiring();
		Assert.assertNotNull("Wiring is null.", h2wiring);

		Module f2 = installDummyModule("f2_v1.MF", "f2_v1", container);
		Assert.assertEquals("Wrong state.", State.INSTALLED, f2.getState());
		container.resolve(Arrays.asList(f2), false);
		Assert.assertNull("Expected to not be able to resolve f2.", f2.getCurrentRevision().getWiring());
	}

	@Test
	public void testExecutionEnvironment() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		String extraCapabilities = "osgi.ee; osgi.ee=JavaSE; version:List<Version>=\"1.3, 1.4, 1.5, 1.6, 1.7\"";
		installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, null, null, extraCapabilities, container);
		container.resolve(null, false);

		Module ee1 = installDummyModule("ee1_v1.MF", "ee1", container);
		Module ee2 = installDummyModule("ee2_v1.MF", "ee2", container);
		Module ee3 = installDummyModule("ee3_v1.MF", "ee3", container);
		container.resolve(Arrays.asList(ee1, ee2), true);
		container.resolve(Arrays.asList(ee3), false);

		ModuleWiring ee1Wiring = ee1.getCurrentRevision().getWiring();
		ModuleWiring ee2Wiring = ee2.getCurrentRevision().getWiring();
		ModuleWiring ee3Wiring = ee3.getCurrentRevision().getWiring();
		Assert.assertNotNull("ee1 is not resolved", ee1Wiring);
		Assert.assertNotNull("ee2 is not resolved", ee2Wiring);
		Assert.assertNull("ee3 is resolved", ee3Wiring);

		// make sure the fragment ee requirement did not get merged into the host
		List<ModuleRequirement> ee1Requirements = ee1Wiring.getModuleRequirements(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
		Assert.assertEquals("Wrong number of requirements", 1, ee1Requirements.size());
		List<ModuleWire> ee1Wires = ee1Wiring.getRequiredModuleWires(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
		Assert.assertEquals("Wrong number of wires", 1, ee1Wires.size());

		List<ModuleRequirement> ee2Requirements = ee2Wiring.getModuleRequirements(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
		Assert.assertEquals("Wrong number of requirements", 1, ee2Requirements.size());
		List<ModuleWire> ee2Wires = ee2Wiring.getRequiredModuleWires(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
		Assert.assertEquals("Wrong number of wires", 1, ee2Wires.size());
	}

	@Test
	public void testPlatformFilter01() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		String extraCapabilities = EclipsePlatformNamespace.ECLIPSE_PLATFORM_NAMESPACE + "; osgi.os=foo; osgi.arch=bar";
		installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, null, null, extraCapabilities, container);
		container.resolve(null, false);

		Module platformFilter1 = installDummyModule("platformFilter1_v1.MF", "ee1", container);
		container.resolve(Arrays.asList(platformFilter1), true);

		ModuleWiring platformFilter1Wiring = platformFilter1.getCurrentRevision().getWiring();
		Assert.assertNotNull("platformFilter1 is not resolved", platformFilter1Wiring);
	}

	@Test
	public void testPlatformFilter02() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		String extraCapabilities = EclipsePlatformNamespace.ECLIPSE_PLATFORM_NAMESPACE + "; osgi.os=baz; osgi.arch=boz";
		installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, null, null, extraCapabilities, container);
		container.resolve(null, false);

		Module platformFilter1 = installDummyModule("platformFilter1_v1.MF", "ee1", container);
		container.resolve(Arrays.asList(platformFilter1), false);

		ModuleWiring platformFilter1Wiring = platformFilter1.getCurrentRevision().getWiring();
		Assert.assertNull("platformFilter1 is resolved", platformFilter1Wiring);
	}

	@Test
	public void testInstallCollision01() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);
		installDummyModule("b1_v1.MF", "b1_a", container);
		try {
			installDummyModule("b1_v1.MF", "b1_b", container);
			Assert.fail("Expected to fail installation because of a collision.");
		} catch (BundleException e) {
			// expected
			Assert.assertEquals("Wrong exception type.", BundleException.DUPLICATE_BUNDLE_ERROR, e.getType());
		}
	}

	@Test
	public void testInstallCollision02() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(true), Collections.emptyMap());
		ModuleContainer container = adaptor.getContainer();
		installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);
		installDummyModule("b1_v1.MF", "b1_a", container);
		installDummyModule("b1_v1.MF", "b1_b", container);
	}

	@Test
	public void testUpdateCollision01() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		Module b1_v1 = installDummyModule("b1_v1.MF", "b1_v1", container);
		installDummyModule("b1_v2.MF", "b1_v2", container);
		try {
			container.update(b1_v1, OSGiManifestBuilderFactory.createBuilder(getManifest("b1_v2.MF")), null);
			Assert.fail("Expected to fail update because of a collision.");
		} catch (BundleException e) {
			// expected
			Assert.assertEquals("Wrong exception type.", BundleException.DUPLICATE_BUNDLE_ERROR, e.getType());
		}
	}

	@Test
	public void testUpdateCollision02() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		Module b1_v1 = installDummyModule("b1_v1.MF", "b1_v1", container);
		try {
			container.update(b1_v1, OSGiManifestBuilderFactory.createBuilder(getManifest("b1_v1.MF")), null);
		} catch (BundleException e) {
			Assert.assertNull("Expected to succeed update to same revision.", e);
		}
	}

	@Test
	public void testUpdateCollision03() throws BundleException, IOException {

		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(true), Collections.emptyMap());
		ModuleContainer container = adaptor.getContainer();
		Module b1_v1 = installDummyModule("b1_v1.MF", "b1_v1", container);
		installDummyModule("b1_v2.MF", "b1_v2", container);
		try {
			container.update(b1_v1, OSGiManifestBuilderFactory.createBuilder(getManifest("b1_v2.MF")), null);
		} catch (BundleException e) {
			Assert.assertNull("Expected to succeed update to same revision.", e);
		}
	}

	@Test
	public void testSingleton01() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		Module s1 = installDummyModule("singleton1_v1.MF", "s1_v1", container);
		Module s2 = installDummyModule("singleton1_v2.MF", "s1_v2", container);
		Module s3 = installDummyModule("singleton1_v3.MF", "s1_v3", container);

		container.resolve(null, false);

		Assert.assertFalse("Singleton v1 is resolved.", Module.RESOLVED_SET.contains(s1.getState()));
		Assert.assertFalse("Singleton v2 is resolved.", Module.RESOLVED_SET.contains(s2.getState()));
		Assert.assertTrue("Singleton v3 is not resolved.", Module.RESOLVED_SET.contains(s3.getState()));
	}

	@Test
	public void testSingleton02() throws BundleException, IOException {
		ResolverHookFactory resolverHookFactory = triggers -> new ResolverHook() {

			@Override
			public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
				collisionCandidates.clear();
			}

			@Override
			public void filterResolvable(Collection<BundleRevision> candidates) {
				// nothing
			}

			@Override
			public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
				// nothing
			}

			@Override
			public void end() {
				// nothing
			}
		};
		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(false), Collections.emptyMap(), resolverHookFactory);
		ModuleContainer container = adaptor.getContainer();

		Module s1 = installDummyModule("singleton1_v1.MF", "s1_v1", container);
		Module s2 = installDummyModule("singleton1_v2.MF", "s1_v2", container);
		Module s3 = installDummyModule("singleton1_v3.MF", "s1_v3", container);

		container.resolve(null, false);

		Assert.assertTrue("Singleton v1 is not resolved.", Module.RESOLVED_SET.contains(s1.getState()));
		Assert.assertTrue("Singleton v2 is not resolved.", Module.RESOLVED_SET.contains(s2.getState()));
		Assert.assertTrue("Singleton v3 is not resolved.", Module.RESOLVED_SET.contains(s3.getState()));
	}

	@Test
	public void testSingleton03() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		Module s1 = installDummyModule("singleton1_v1.MF", "s1_v1", container);
		// Resolve s1 first
		container.resolve(null, false);
		Assert.assertTrue("Singleton v1 is not resolved.", Module.RESOLVED_SET.contains(s1.getState()));

		Module s2 = installDummyModule("singleton1_v2.MF", "s1_v2", container);
		Module s3 = installDummyModule("singleton1_v3.MF", "s1_v3", container);

		container.resolve(Arrays.asList(s2, s3), false);

		// Make sure s1 is the only on resolved because it was first resolved
		Assert.assertTrue("Singleton v1 is not resolved.", Module.RESOLVED_SET.contains(s1.getState()));
		Assert.assertFalse("Singleton v2 is resolved.", Module.RESOLVED_SET.contains(s2.getState()));
		Assert.assertFalse("Singleton v3 is resolved.", Module.RESOLVED_SET.contains(s3.getState()));
	}

	@Test
	public void testSingleton04() throws BundleException, IOException {
		final Collection<BundleRevision> disabled = new ArrayList<>();
		ResolverHookFactory resolverHookFactory = triggers -> new ResolverHook() {

			@Override
			public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
				// nothing
			}

			@Override
			public void filterResolvable(Collection<BundleRevision> candidates) {
				candidates.removeAll(disabled);
			}

			@Override
			public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
				// nothing
			}

			@Override
			public void end() {
				// nothing
			}
		};
		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(false), Collections.emptyMap(), resolverHookFactory);
		ModuleContainer container = adaptor.getContainer();

		Module s1_v1 = installDummyModule("singleton1_v1.MF", "s1_v1", container);
		Module s1_v2 = installDummyModule("singleton1_v2.MF", "s1_v2", container);
		Module s1_v3 = installDummyModule("singleton1_v3.MF", "s1_v3", container);

		Module s2_v1 = installDummyModule("singleton2_v1.MF", "s1_v1", container);
		Module s2_v2 = installDummyModule("singleton2_v2.MF", "s1_v2", container);
		Module s2_v3 = installDummyModule("singleton2_v3.MF", "s1_v3", container);

		container.resolve(null, false);

		Assert.assertFalse("Singleton v1 is resolved.", Module.RESOLVED_SET.contains(s1_v1.getState()));
		Assert.assertFalse("Singleton v2 is resolved.", Module.RESOLVED_SET.contains(s1_v2.getState()));
		Assert.assertTrue("Singleton v3 is not resolved.", Module.RESOLVED_SET.contains(s1_v3.getState()));

		Assert.assertFalse("client v1 is resolved.", Module.RESOLVED_SET.contains(s2_v1.getState()));
		Assert.assertFalse("client v2 is resolved.", Module.RESOLVED_SET.contains(s2_v2.getState()));
		Assert.assertTrue("client v3 is not resolved.", Module.RESOLVED_SET.contains(s2_v3.getState()));

		// now disable s1_v3
		disabled.add(s1_v3.getCurrentRevision());
		container.refresh(Arrays.asList(s1_v3));
		Assert.assertFalse("Singleton v1 is resolved.", Module.RESOLVED_SET.contains(s1_v1.getState()));
		Assert.assertTrue("Singleton v2 is not resolved.", Module.RESOLVED_SET.contains(s1_v2.getState()));
		Assert.assertFalse("Singleton v3 is resolved.", Module.RESOLVED_SET.contains(s1_v3.getState()));

		Assert.assertFalse("client v1 is resolved.", Module.RESOLVED_SET.contains(s2_v1.getState()));
		Assert.assertTrue("client v2 is not resolved.", Module.RESOLVED_SET.contains(s2_v2.getState()));
		Assert.assertFalse("client v3 is resolved.", Module.RESOLVED_SET.contains(s2_v3.getState()));

		// now disable s1_v2
		disabled.add(s1_v2.getCurrentRevision());
		container.refresh(Arrays.asList(s1_v2));
		Assert.assertTrue("Singleton v1 is not resolved.", Module.RESOLVED_SET.contains(s1_v1.getState()));
		Assert.assertFalse("Singleton v2 is resolved.", Module.RESOLVED_SET.contains(s1_v2.getState()));
		Assert.assertFalse("Singleton v3 is resolved.", Module.RESOLVED_SET.contains(s1_v3.getState()));

		Assert.assertTrue("client v1 is not resolved.", Module.RESOLVED_SET.contains(s2_v1.getState()));
		Assert.assertFalse("client v2 is resolved.", Module.RESOLVED_SET.contains(s2_v2.getState()));
		Assert.assertFalse("client v3 is resolved.", Module.RESOLVED_SET.contains(s2_v3.getState()));

	}

	@Test
	public void testEventsInstall() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		Module c1 = installDummyModule("c1_v1.MF", "c1_v1", container);
		Module c2 = installDummyModule("c2_v1.MF", "c2_v1", container);
		Module c3 = installDummyModule("c3_v1.MF", "c3_v1", container);
		Module c4 = installDummyModule("c4_v1.MF", "c4_v1", container);
		Module c5 = installDummyModule("c5_v1.MF", "c5_v1", container);
		Module c6 = installDummyModule("c6_v1.MF", "c6_v1", container);
		Module c7 = installDummyModule("c7_v1.MF", "c7_v1", container);

		List<DummyModuleEvent> actual = adaptor.getDatabase().getModuleEvents();
		List<DummyModuleEvent> expected = Arrays.asList(new DummyModuleEvent(c1, ModuleEvent.INSTALLED, State.INSTALLED), new DummyModuleEvent(c2, ModuleEvent.INSTALLED, State.INSTALLED), new DummyModuleEvent(c3, ModuleEvent.INSTALLED, State.INSTALLED), new DummyModuleEvent(c4, ModuleEvent.INSTALLED, State.INSTALLED), new DummyModuleEvent(c5, ModuleEvent.INSTALLED, State.INSTALLED), new DummyModuleEvent(c6, ModuleEvent.INSTALLED, State.INSTALLED), new DummyModuleEvent(c7, ModuleEvent.INSTALLED, State.INSTALLED));
		Assert.assertEquals("Wrong install events.", expected, actual);
	}

	@Test
	public void testEventsResolved() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		DummyModuleDatabase database = adaptor.getDatabase();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		Module c1 = installDummyModule("c1_v1.MF", "c1_v1", container);
		Module c2 = installDummyModule("c2_v1.MF", "c2_v1", container);
		Module c3 = installDummyModule("c3_v1.MF", "c3_v1", container);
		Module c4 = installDummyModule("c4_v1.MF", "c4_v1", container);
		Module c5 = installDummyModule("c5_v1.MF", "c5_v1", container);
		Module c6 = installDummyModule("c6_v1.MF", "c6_v1", container);
		Module c7 = installDummyModule("c7_v1.MF", "c7_v1", container);
		// throw away installed events
		database.getModuleEvents();

		container.resolve(Arrays.asList(c1, c2, c3, c4, c5, c6, c7), true);
		List<DummyModuleEvent> actual = database.getModuleEvents();
		List<DummyModuleEvent> expected = new ArrayList<>(Arrays.asList(new DummyModuleEvent(c1, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c2, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c3, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c4, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c5, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c6, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c7, ModuleEvent.RESOLVED, State.RESOLVED)));
		assertEvents(expected, actual, false);
	}

	@Test
	public void testEventsRefresh() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		DummyModuleDatabase database = adaptor.getDatabase();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		Module c1 = installDummyModule("c1_v1.MF", "c1_v1", container);
		Module c2 = installDummyModule("c2_v1.MF", "c2_v1", container);
		Module c3 = installDummyModule("c3_v1.MF", "c3_v1", container);
		Module c4 = installDummyModule("c4_v1.MF", "c4_v1", container);
		Module c5 = installDummyModule("c5_v1.MF", "c5_v1", container);
		Module c6 = installDummyModule("c6_v1.MF", "c6_v1", container);
		Module c7 = installDummyModule("c7_v1.MF", "c7_v1", container);

		container.resolve(Arrays.asList(c1, c2, c3, c4, c5, c6, c7), true);
		// throw away installed and resolved events
		database.getModuleEvents();

		container.refresh(Arrays.asList(systemBundle));
		List<DummyModuleEvent> actual = database.getModuleEvents();
		List<DummyModuleEvent> expected = new ArrayList<>(Arrays.asList(new DummyModuleEvent(systemBundle, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c1, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c2, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c3, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c4, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c5, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c6, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c7, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(systemBundle, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c1, ModuleEvent.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c2, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c3, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c4, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c5, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c6, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c7, ModuleEvent.RESOLVED, State.RESOLVED)));
		assertEvents(expected, actual, false);
	}

	@Test
	public void testEventsStart() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		DummyModuleDatabase database = adaptor.getDatabase();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		// actually launch the container
		systemBundle.start();

		Module c1 = installDummyModule("c1_v1.MF", "c1_v1", container);
		Module c2 = installDummyModule("c2_v1.MF", "c2_v1", container);
		Module c3 = installDummyModule("c3_v1.MF", "c3_v1", container);
		Module c4 = installDummyModule("c4_v1.MF", "c4_v1", container);
		Module c5 = installDummyModule("c5_v1.MF", "c5_v1", container);
		Module c6 = installDummyModule("c6_v1.MF", "c6_v1", container);
		Module c7 = installDummyModule("c7_v1.MF", "c7_v1", container);
		// throw away installed events
		database.getModuleEvents();

		c7.start();

		List<DummyModuleEvent> actual = database.getModuleEvents();
		List<DummyModuleEvent> expected = new ArrayList<>(Arrays.asList(new DummyModuleEvent(c1, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c2, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c3, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c4, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c5, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c6, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c7, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c7, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c7, ModuleEvent.STARTED, State.ACTIVE)));
		assertEvents(expected, actual, false);
	}

	@Test
	public void testEventsStartRefresh() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		DummyModuleDatabase database = adaptor.getDatabase();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);
		// actually launch the container
		systemBundle.start();

		Module c4 = installDummyModule("c4_v1.MF", "c4_v1", container);
		Module c5 = installDummyModule("c5_v1.MF", "c5_v1", container);
		Module c6 = installDummyModule("c6_v1.MF", "c6_v1", container);
		Module c7 = installDummyModule("c7_v1.MF", "c7_v1", container);

		c7.start();
		// discard events
		database.getModuleEvents();
		container.refresh(Arrays.asList(c4));
		List<DummyModuleEvent> actual = database.getModuleEvents();
		List<DummyModuleEvent> expected = new ArrayList<>(Arrays.asList(new DummyModuleEvent(c7, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c7, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c4, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c5, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c6, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c7, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c4, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c5, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c6, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c7, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c7, ModuleEvent.STARTING, State.STARTING),
				new DummyModuleEvent(c7, ModuleEvent.STARTED, State.ACTIVE)));
		assertEvents(expected, actual, false);
	}

	@Test
	public void testRemovalPending() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		DummyModuleDatabase database = adaptor.getDatabase();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		Module c4 = installDummyModule("c4_v1.MF", "c4_v1", container);
		Module c6 = installDummyModule("c6_v1.MF", "c6_v1", container);
		Module c7 = installDummyModule("c7_v1.MF", "c7_v1", container);

		container.resolve(Arrays.asList(c7), true);
		// throw out installed and resolved events
		database.getModuleEvents();

		ModuleRevision c4Revision0 = c4.getCurrentRevision();
		// updating to identical content
		container.update(c4, OSGiManifestBuilderFactory.createBuilder(getManifest("c4_v1.MF")), null);
		container.resolve(Arrays.asList(c4), true);

		Collection<ModuleRevision> removalPending = container.getRemovalPending();
		Assert.assertEquals("Wrong number of removal pending", 1, removalPending.size());
		Assert.assertTrue("Wrong module removalPending: " + removalPending, removalPending.contains(c4Revision0));

		ModuleRevision c6Revision0 = c6.getCurrentRevision();
		// updating to identical content
		container.update(c6, OSGiManifestBuilderFactory.createBuilder(getManifest("c6_v1.MF")), null);
		container.resolve(Arrays.asList(c6), true);

		removalPending = container.getRemovalPending();
		Assert.assertEquals("Wrong number of removal pending", 2, removalPending.size());
		Assert.assertTrue("Wrong module removalPending: " + removalPending, removalPending.containsAll(Arrays.asList(c4Revision0, c6Revision0)));

		// update again with identical content
		container.update(c4, OSGiManifestBuilderFactory.createBuilder(getManifest("c4_v1.MF")), null);
		container.update(c6, OSGiManifestBuilderFactory.createBuilder(getManifest("c6_v1.MF")), null);
		container.resolve(Arrays.asList(c4, c6), true);

		// Again we only have two since the previous current revisions did not have any dependents
		removalPending = container.getRemovalPending();
		Assert.assertEquals("Wrong number of removal pending", 2, removalPending.size());
		Assert.assertTrue("Wrong module removalPending: " + removalPending, removalPending.containsAll(Arrays.asList(c4Revision0, c6Revision0)));

		container.refresh(null);

		removalPending = container.getRemovalPending();
		Assert.assertEquals("Wrong number of removal pending", 0, removalPending.size());

		List<DummyModuleEvent> actual = database.getModuleEvents();
		List<DummyModuleEvent> expected = new ArrayList<>(Arrays.asList(new DummyModuleEvent(c4, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c4, ModuleEvent.UPDATED, State.INSTALLED), new DummyModuleEvent(c4, ModuleEvent.RESOLVED, State.RESOLVED),

				new DummyModuleEvent(c6, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c6, ModuleEvent.UPDATED, State.INSTALLED), new DummyModuleEvent(c6, ModuleEvent.RESOLVED, State.RESOLVED),

				new DummyModuleEvent(c4, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c4, ModuleEvent.UPDATED, State.INSTALLED), new DummyModuleEvent(c6, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c6, ModuleEvent.UPDATED, State.INSTALLED), new DummyModuleEvent(c4, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c6, ModuleEvent.RESOLVED, State.RESOLVED),

				new DummyModuleEvent(c4, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c6, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c7, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c4, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c6, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c7, ModuleEvent.RESOLVED, State.RESOLVED)));
		assertEvents(expected, actual, false);

		// uninstall c4
		c4Revision0 = c4.getCurrentRevision();
		container.uninstall(c4);
		removalPending = container.getRemovalPending();
		Assert.assertEquals("Wrong number of removal pending", 1, removalPending.size());
		Assert.assertTrue("Wrong module removalPending: " + removalPending, removalPending.containsAll(Arrays.asList(c4Revision0)));

		container.refresh(null);
		actual = database.getModuleEvents();
		expected = new ArrayList<>(Arrays.asList(new DummyModuleEvent(c4, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c4, ModuleEvent.UNINSTALLED, State.UNINSTALLED), new DummyModuleEvent(c6, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c7, ModuleEvent.UNRESOLVED, State.INSTALLED)));
		assertEvents(expected, actual, false);

		// Test bug 411833
		// install c4 again and resolve c6
		c4 = installDummyModule("c4_v1.MF", "c4_v1", container);
		container.resolve(Arrays.asList(c6), true);
		// throw out installed and resolved events
		database.getModuleEvents();

		removalPending = container.getRemovalPending();
		Assert.assertEquals("Wrong number of removal pending", 0, removalPending.size());

		c4Revision0 = c4.getCurrentRevision();
		// uninstall c4, but refresh c6 instead
		// this should result in removal pending c4 to be removed.
		container.uninstall(c4);
		removalPending = container.getRemovalPending();
		Assert.assertEquals("Wrong number of removal pending", 1, removalPending.size());
		Assert.assertTrue("Wrong module removalPending: " + removalPending, removalPending.containsAll(Arrays.asList(c4Revision0)));

		container.refresh(Collections.singletonList(c6));
		actual = database.getModuleEvents();
		expected = new ArrayList<>(Arrays.asList(new DummyModuleEvent(c4, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c4, ModuleEvent.UNINSTALLED, State.UNINSTALLED), new DummyModuleEvent(c6, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c7, ModuleEvent.UNRESOLVED, State.INSTALLED)));
		assertEvents(expected, actual, false);

		removalPending = container.getRemovalPending();
		Assert.assertEquals("Wrong number of removal pending", 0, removalPending.size());
	}

	@Test
	public void testSubstitutableExports01() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		Module sub1 = installDummyModule("sub1_v1.MF", "sub1", container);
		Module sub2 = installDummyModule("sub2_v1.MF", "sub2", container);

		ModuleRevision sub1Revision0 = sub1.getCurrentRevision();

		container.resolve(Arrays.asList(sub2), true);

		container.update(sub1, OSGiManifestBuilderFactory.createBuilder(getManifest("sub1_v2.MF")), null);
		container.resolve(Arrays.asList(sub1), true);

		ModuleWiring sub1Wiring = sub1.getCurrentRevision().getWiring();
		List<BundleCapability> exportedPackages = sub1Wiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		Assert.assertEquals("Wrong number of exported packages: " + exportedPackages, 0, exportedPackages.size());
		List<BundleWire> requiredWires = sub1Wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
		Assert.assertEquals("Wrong number of imported packages: ", 2, requiredWires.size());
		Assert.assertEquals("Wrong provider for package: " + requiredWires.get(1).getProvider(), sub1Revision0, requiredWires.get(1).getProvider());

		container.refresh(Arrays.asList(sub1));

		sub1Wiring = sub1.getCurrentRevision().getWiring();
		exportedPackages = sub1Wiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		Assert.assertEquals("Wrong number of exported packages: " + exportedPackages, 1, exportedPackages.size());
		requiredWires = sub1Wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
		Assert.assertEquals("Wrong number of imported packages: ", 1, requiredWires.size());

	}

	@Test
	public void testSubstitutableExports02() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module a = installDummyModule("sub.a.MF", "a", container);
		Module b = installDummyModule("sub.b.MF", "b", container);
		Module c = installDummyModule("sub.c.MF", "c", container);

		container.resolve(Arrays.asList(a, b, c), true);

		ModuleWiring wiringA = a.getCurrentRevision().getWiring();
		ModuleWiring wiringB = b.getCurrentRevision().getWiring();
		ModuleWiring wiringC = c.getCurrentRevision().getWiring();

		List<ModuleWire> providedWiresA = wiringA.getProvidedModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
		Assert.assertEquals("Wrong number of provided wires.", 2, providedWiresA.size());

		Collection<ModuleRevision> requirers = new ArrayList<>();
		for (ModuleWire wire : providedWiresA) {
			requirers.add(wire.getRequirer());
		}
		Assert.assertTrue("b does not require.", requirers.contains(b.getCurrentRevision()));
		Assert.assertTrue("c does not require.", requirers.contains(c.getCurrentRevision()));

		List<ModuleWire> requiredWiresB = wiringB.getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
		Assert.assertEquals("Wrong number of required wires.", 1, requiredWiresB.size());
		Assert.assertEquals("Unexpected package name.", "javax.servlet", requiredWiresB.iterator().next().getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider.", a.getCurrentRevision(), requiredWiresB.iterator().next().getProvider());

		List<ModuleWire> requiredWiresC = wiringC.getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
		Assert.assertEquals("Wrong number of required wires.", 1, requiredWiresC.size());
		Assert.assertEquals("Wrong number of required wires.", 1, requiredWiresC.size());
		Assert.assertEquals("Unexpected package name.", "javax.servlet", requiredWiresC.iterator().next().getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider.", a.getCurrentRevision(), requiredWiresC.iterator().next().getProvider());

		Module d = installDummyModule("sub.d.MF", "d", container);

		container.resolve(Arrays.asList(d), true);

		ModuleWiring wiringD = d.getCurrentRevision().getWiring();
		List<ModuleWire> requiredWiresD = wiringD.getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
		Assert.assertEquals("Wrong number of required wires.", 2, requiredWiresD.size());
		Assert.assertEquals("Unexpected package name.", "org.ops4j.pax.web.service", requiredWiresD.get(0).getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider.", c.getCurrentRevision(), requiredWiresD.get(0).getProvider());
		Assert.assertEquals("Unexpected package name.", "javax.servlet", requiredWiresD.get(1).getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider.", a.getCurrentRevision(), requiredWiresD.get(1).getProvider());

	}

	@Test
	public void testSubstitutableExports03() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install order does not really matter
		Module g = installDummyModule("sub.g.MF", "g", container);
		Module f = installDummyModule("sub.f.MF", "f", container);
		Module e = installDummyModule("sub.e.MF", "e", container);

		// resolve order does matter so that transitive dependencies are pulled in
		// and cause substitution to happen in a certain way
		container.resolve(Arrays.asList(g, f, e), true);

		ModuleWiring wiringE = e.getCurrentRevision().getWiring();
		ModuleWiring wiringF = f.getCurrentRevision().getWiring();

		List<ModuleWire> providedWiresE = wiringE.getProvidedModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
		Assert.assertEquals("Wrong number of provided wires.", 3, providedWiresE.size());

		Collection<ModuleRevision> requirers = new HashSet<>();
		for (ModuleWire wire : providedWiresE) {
			requirers.add(wire.getRequirer());
		}
		Assert.assertTrue("f does not require.", requirers.remove(f.getCurrentRevision()));
		Assert.assertTrue("g does not require.", requirers.remove(g.getCurrentRevision()));
		Assert.assertTrue("No requirers should be left: " + requirers, requirers.isEmpty());

		List<ModuleWire> providedWiresF = wiringF.getProvidedModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
		Assert.assertEquals("Wrong number of provided wires: " + providedWiresF, 0, providedWiresF.size());
	}

	@Test
	public void testSubstitutableExports04() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install order does not really matter
		installDummyModule("sub.h.MF", "h", container);
		Module i = installDummyModule("sub.i.MF", "i", container);
		installDummyModule("sub.j.MF", "j", container);
		Module k = installDummyModule("sub.k.MF", "k", container);

		// resolve order does matter so that transitive dependencies are pulled in
		// and cause substitution to happen in a certain way
		container.resolve(Arrays.asList(k), true);

		ModuleWiring wiringI = i.getCurrentRevision().getWiring();
		ModuleWiring wiringK = k.getCurrentRevision().getWiring();

		List<ModuleWire> requiredWiresK = wiringK.getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE);

		// I should be the provider for all of K
		Assert.assertEquals("Wrong number of required wires: " + requiredWiresK, 2, requiredWiresK.size());
		for (ModuleWire moduleWire : requiredWiresK) {
			Assert.assertEquals("Wrong provider: " + moduleWire.getProviderWiring(), wiringI, moduleWire.getProviderWiring());
		}
	}

	@Test
	public void testLazy01() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		DummyModuleDatabase database = adaptor.getDatabase();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		// actually launch the container
		systemBundle.start();

		Module lazy1 = installDummyModule("lazy1_v1.MF", "lazy1", container);

		// throw out installed and resolved events
		database.getModuleEvents();

		lazy1.start(StartOptions.USE_ACTIVATION_POLICY);

		List<DummyModuleEvent> actual = database.getModuleEvents();
		List<DummyModuleEvent> expected = new ArrayList<>(Arrays.asList(new DummyModuleEvent(lazy1, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(lazy1, ModuleEvent.LAZY_ACTIVATION, State.LAZY_STARTING)));
		assertEvents(expected, actual, true);

		lazy1.start(StartOptions.LAZY_TRIGGER);

		actual = database.getModuleEvents();
		expected = new ArrayList<>(Arrays.asList(new DummyModuleEvent(lazy1, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(lazy1, ModuleEvent.STARTED, State.ACTIVE)));
		assertEvents(expected, actual, true);

		container.refresh(Arrays.asList(lazy1));

		actual = database.getModuleEvents();
		expected = new ArrayList<>(Arrays.asList(new DummyModuleEvent(lazy1, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(lazy1, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(lazy1, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(lazy1, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(lazy1, ModuleEvent.LAZY_ACTIVATION, State.LAZY_STARTING)));
		assertEvents(expected, actual, true);

		container.update(lazy1, OSGiManifestBuilderFactory.createBuilder(getManifest("lazy1_v1.MF")), null);
		actual = database.getModuleEvents();
		expected = new ArrayList<>(Arrays.asList(new DummyModuleEvent(lazy1, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(lazy1, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(lazy1, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(lazy1, ModuleEvent.UPDATED, State.INSTALLED), new DummyModuleEvent(lazy1, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(lazy1, ModuleEvent.LAZY_ACTIVATION, State.LAZY_STARTING)));
		assertEvents(expected, actual, true);

		container.refresh(Arrays.asList(lazy1));
		actual = database.getModuleEvents();
		expected = new ArrayList<>(Arrays.asList(new DummyModuleEvent(lazy1, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(lazy1, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(lazy1, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(lazy1, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(lazy1, ModuleEvent.LAZY_ACTIVATION, State.LAZY_STARTING)));
		assertEvents(expected, actual, true);
	}

	@Test
	public void testSettings01() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		DummyModuleDatabase database = adaptor.getDatabase();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		// actually launch the container
		systemBundle.start();

		container.getFrameworkStartLevel().setInitialBundleStartLevel(2);
		Module c4 = installDummyModule("c4_v1.MF", "c4_v1", container);
		Module lazy1 = installDummyModule("lazy1_v1.MF", "lazy1", container);

		container.resolve(Arrays.asList(c4, lazy1), true);

		Assert.assertEquals("Wrong startlevel.", 2, c4.getStartLevel());
		Assert.assertEquals("Wrong startlevel.", 2, lazy1.getStartLevel());

		c4.setStartLevel(3);
		lazy1.setStartLevel(3);

		Assert.assertEquals("Wrong startlevel.", 3, c4.getStartLevel());
		Assert.assertEquals("Wrong startlevel.", 3, lazy1.getStartLevel());

		database.getModuleEvents();

		c4.start();
		lazy1.start(StartOptions.USE_ACTIVATION_POLICY);

		List<DummyModuleEvent> actual = database.getModuleEvents();
		Assert.assertEquals("Did not expect any events.", 0, actual.size());

		database.getContainerEvents();
		container.getFrameworkStartLevel().setStartLevel(3);

		List<DummyContainerEvent> actualContainerEvents = database.getContainerEvents(1);
		List<DummyContainerEvent> expectedContainerEvents = new ArrayList<>(Arrays.asList(new DummyContainerEvent(ContainerEvent.START_LEVEL, systemBundle, null)));
		Assert.assertEquals("Wrong container events.", expectedContainerEvents, actualContainerEvents);

		actual = database.getModuleEvents(3);
		List<DummyModuleEvent> expected = new ArrayList<>(Arrays.asList(new DummyModuleEvent(lazy1, ModuleEvent.LAZY_ACTIVATION, State.LAZY_STARTING), new DummyModuleEvent(c4, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c4, ModuleEvent.STARTED, State.ACTIVE)));
		assertEvents(expected, actual, true);

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(bytes);
		database.store(data, true);

		systemBundle.stop();

		// reload into a new container
		adaptor = createDummyAdaptor();
		container = adaptor.getContainer();
		database = adaptor.getDatabase();
		database.load(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

		systemBundle = container.getModule(0);
		Assert.assertNotNull("System bundle is null.", systemBundle);
		Assert.assertTrue("System bundle should always use activation policy.", systemBundle.isActivationPolicyUsed());
		Assert.assertTrue("System bundle should always have its auto-start flag set.", systemBundle.isPersistentlyStarted());

		c4 = container.getModule(c4.getId());
		Assert.assertNotNull("c4 is null", c4);
		lazy1 = container.getModule(lazy1.getId());
		Assert.assertNotNull("lazy1 is null", lazy1);

		Assert.assertFalse("c4 has activation policy set.", c4.isActivationPolicyUsed());
		Assert.assertTrue("c4 is not auto started.", c4.isPersistentlyStarted());
		Assert.assertEquals("c4 has wrong start-level", 3, c4.getStartLevel());
		Assert.assertTrue("lazy1 is using activation policy.", lazy1.isActivationPolicyUsed());
		Assert.assertTrue("lazy1 is not auto started.", lazy1.isPersistentlyStarted());
		Assert.assertEquals("lazy1 has wrong start-level", 3, lazy1.getStartLevel());

		// relaunch the container
		systemBundle.start();

		actualContainerEvents = database.getContainerEvents();
		expectedContainerEvents = new ArrayList<>(Arrays.asList(new DummyContainerEvent(ContainerEvent.START_LEVEL, systemBundle, null), new DummyContainerEvent(ContainerEvent.STARTED, systemBundle, null)));

		actual = database.getModuleEvents(2);
		expected = new ArrayList<>(Arrays.asList(new DummyModuleEvent(systemBundle, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(systemBundle, ModuleEvent.STARTED, State.ACTIVE)));
		assertEvents(expected, actual, true);

		container.getFrameworkStartLevel().setStartLevel(3);

		actualContainerEvents = database.getContainerEvents(1);
		expectedContainerEvents = new ArrayList<>(Arrays.asList(new DummyContainerEvent(ContainerEvent.START_LEVEL, systemBundle, null)));
		Assert.assertEquals("Wrong container events.", expectedContainerEvents, actualContainerEvents);

		actual = database.getModuleEvents(3);
		expected = new ArrayList<>(Arrays.asList(new DummyModuleEvent(lazy1, ModuleEvent.LAZY_ACTIVATION, State.LAZY_STARTING), new DummyModuleEvent(c4, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c4, ModuleEvent.STARTED, State.ACTIVE)));
		assertEvents(expected, actual, true);
	}

	@Test
	public void testTimestampSeeding() throws BundleException, IOException, InterruptedException {
		Assert.assertNotEquals("The timestamps are the same!", createTestContainerAndGetTimestamp(), createTestContainerAndGetTimestamp());
	}

	private long createTestContainerAndGetTimestamp() throws BundleException, IOException, InterruptedException {
		// wait here to ensure current time really has increased
		Thread.sleep(100);
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		DummyModuleDatabase database = adaptor.getDatabase();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		// actually launch the container
		systemBundle.start();

		// install some bundles and set some settings
		container.getFrameworkStartLevel().setInitialBundleStartLevel(2);
		Module c4 = installDummyModule("c4_v1.MF", "c4_v1", container);
		Module lazy1 = installDummyModule("lazy1_v1.MF", "lazy1", container);

		container.resolve(Arrays.asList(c4, lazy1), true);

		// set some settings
		Assert.assertEquals("Wrong startlevel.", 2, c4.getStartLevel());
		Assert.assertEquals("Wrong startlevel.", 2, lazy1.getStartLevel());
		return database.getTimestamp();
	}

	@Test
	public void testEventsStartLevelBeginningAt100() throws BundleException, IOException {
		doTestEventsStartLevel(100);
	}

	@Test
	public void testEventsStartLevelBeginningAt1() throws BundleException, IOException {
		doTestEventsStartLevel(1);
	}

	private void doTestEventsStartLevel(int beginningStartLevel) throws BundleException, IOException {
		Map<String, String> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, String.valueOf(beginningStartLevel));

		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(false), configuration);
		ModuleContainer container = adaptor.getContainer();
		DummyModuleDatabase database = adaptor.getDatabase();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		Module c1 = installDummyModule("c1_v1.MF", "c1_v1", container);
		Module c2 = installDummyModule("c2_v1.MF", "c2_v1", container);
		Module c3 = installDummyModule("c3_v1.MF", "c3_v1", container);
		Module c4 = installDummyModule("c4_v1.MF", "c4_v1", container);
		Module c5 = installDummyModule("c5_v1.MF", "c5_v1", container);
		Module c6 = installDummyModule("c6_v1.MF", "c6_v1", container);
		Module c7 = installDummyModule("c7_v1.MF", "c7_v1", container);
		container.resolve(Arrays.asList(c1, c2, c3, c4, c5, c6, c7), true);
		database.getModuleEvents();

		c1.setStartLevel(70);
		c2.setStartLevel(60);
		c3.setStartLevel(50);
		c4.setStartLevel(40);
		c5.setStartLevel(30);
		c6.setStartLevel(20);
		c7.setStartLevel(10);

		c1.start();
		c2.start();
		c3.start();
		c4.start();
		c5.start();
		c6.start();
		c7.start();

		List<DummyModuleEvent> actualModuleEvents = database.getModuleEvents();
		Assert.assertEquals("Expecting no events.", 0, actualModuleEvents.size());

		systemBundle.start();

		if (beginningStartLevel == 1) {
			actualModuleEvents = database.getModuleEvents(2);
			List<DummyModuleEvent> expectedModuleEvents = new ArrayList<>(Arrays.asList(new DummyModuleEvent(systemBundle, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(systemBundle, ModuleEvent.STARTED, State.ACTIVE)));
			assertEvents(expectedModuleEvents, actualModuleEvents, true);

			List<DummyContainerEvent> actualContainerEvents = database.getContainerEvents();
			List<DummyContainerEvent> expectedContainerEvents = new ArrayList<>(Arrays.asList(new DummyContainerEvent(ContainerEvent.STARTED, systemBundle, null)));
			Assert.assertEquals("Wrong container events.", expectedContainerEvents, actualContainerEvents);

			container.getFrameworkStartLevel().setStartLevel(100);
			actualModuleEvents = database.getModuleEvents(14);
			expectedModuleEvents = new ArrayList<>(Arrays.asList(new DummyModuleEvent(c7, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c7, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c6, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c6, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c5, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c5, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c4, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c4, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c3, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c3, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c2, ModuleEvent.STARTING, State.STARTING),
					new DummyModuleEvent(c2, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c1, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c1, ModuleEvent.STARTED, State.ACTIVE)));

			actualContainerEvents = database.getContainerEvents(1);
			expectedContainerEvents = new ArrayList<>(Arrays.asList(new DummyContainerEvent(ContainerEvent.START_LEVEL, systemBundle, null)));
			Assert.assertEquals("Wrong container events.", expectedContainerEvents, actualContainerEvents);
		} else {
			actualModuleEvents = database.getModuleEvents(16);
			List<DummyModuleEvent> expectedModuleEvents = new ArrayList<>(Arrays.asList(new DummyModuleEvent(systemBundle, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c7, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c7, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c6, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c6, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c5, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c5, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c4, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c4, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c3, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c3, ModuleEvent.STARTED, State.ACTIVE),
					new DummyModuleEvent(c2, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c2, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c1, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c1, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(systemBundle, ModuleEvent.STARTED, State.ACTIVE)));
			assertEvents(expectedModuleEvents, actualModuleEvents, true);

			List<DummyContainerEvent> actualContainerEvents = database.getContainerEvents();
			List<DummyContainerEvent> expectedContainerEvents = new ArrayList<>(Arrays.asList(new DummyContainerEvent(ContainerEvent.STARTED, systemBundle, null)));
			Assert.assertEquals("Wrong container events.", expectedContainerEvents, actualContainerEvents);
		}

		if (beginningStartLevel == 1) {
			container.getFrameworkStartLevel().setStartLevel(1);
			actualModuleEvents = database.getModuleEvents(14);
			List<DummyModuleEvent> expectedModuleEvents = new ArrayList<>(Arrays.asList(new DummyModuleEvent(c1, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c1, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c2, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c2, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c3, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c3, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c4, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c4, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c5, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c5, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c6, ModuleEvent.STOPPING, State.STOPPING),
					new DummyModuleEvent(c6, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c7, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c7, ModuleEvent.STOPPED, State.RESOLVED)));
			assertEvents(expectedModuleEvents, actualModuleEvents, true);

			List<DummyContainerEvent> actualContainerEvents = database.getContainerEvents(1);
			List<DummyContainerEvent> expectedContainerEvents = new ArrayList<>(Arrays.asList(new DummyContainerEvent(ContainerEvent.START_LEVEL, systemBundle, null)));
			Assert.assertEquals("Wrong container events.", expectedContainerEvents, actualContainerEvents);
		}

		systemBundle.stop();

		if (beginningStartLevel == 1) {
			actualModuleEvents = database.getModuleEvents(2);
			List<DummyModuleEvent> expectedModuleEvents = new ArrayList<>(Arrays.asList(new DummyModuleEvent(systemBundle, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(systemBundle, ModuleEvent.STOPPED, State.RESOLVED)));
			assertEvents(expectedModuleEvents, actualModuleEvents, true);
		} else {
			actualModuleEvents = database.getModuleEvents(16);
			List<DummyModuleEvent> expectedModuleEvents = new ArrayList<>(Arrays.asList(new DummyModuleEvent(systemBundle, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c1, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c1, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c2, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c2, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c3, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c3, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c4, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c4, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c5, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c5, ModuleEvent.STOPPED, State.RESOLVED),
					new DummyModuleEvent(c6, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c6, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c7, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c7, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(systemBundle, ModuleEvent.STOPPED, State.RESOLVED)));
			assertEvents(expectedModuleEvents, actualModuleEvents, true);
		}
		List<DummyContainerEvent> actualContainerEvents = database.getContainerEvents();
		List<DummyContainerEvent> expectedContainerEvents = new ArrayList<>(Arrays.asList(new DummyContainerEvent(ContainerEvent.STOPPED, systemBundle, null)));
		Assert.assertEquals("Wrong container events.", expectedContainerEvents, actualContainerEvents);
	}

	@Test
	public void testDynamicImport01() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, null, null, "osgi.ee; osgi.ee=JavaSE; version:Version=\"1.5.0\"", container);

		container.resolve(Arrays.asList(systemBundle), true);

		Module c1 = installDummyModule("c1_v1.MF", "c1_v1", container);
		Module dynamic1 = installDummyModule("dynamic1_v1.MF", "dynamic1_v1", container);

		container.resolve(Arrays.asList(c1, dynamic1), true);

		ModuleWire dynamicWire = container.resolveDynamic("org.osgi.framework", dynamic1.getCurrentRevision());
		Assert.assertNotNull("No dynamic wire found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "org.osgi.framework", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider for the wire found.", systemBundle.getCurrentRevision(), dynamicWire.getProvider());

		dynamicWire = container.resolveDynamic("org.osgi.framework.wiring", dynamic1.getCurrentRevision());
		Assert.assertNotNull("No dynamic wire found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "org.osgi.framework.wiring", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider for the wire found.", systemBundle.getCurrentRevision(), dynamicWire.getProvider());

		dynamicWire = container.resolveDynamic("c1.b", dynamic1.getCurrentRevision());
		Assert.assertNotNull("No dynamic wire found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "c1.b", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider for the wire found.", c1.getCurrentRevision(), dynamicWire.getProvider());
	}

	@Test
	public void testDynamicImport02() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, null, null, "osgi.ee; osgi.ee=JavaSE; version:Version=\"1.5.0\"", container);

		container.resolve(Arrays.asList(systemBundle), true);

		Module c1 = installDummyModule("c1_v1.MF", "c1_v1", container);
		Module c4 = installDummyModule("c4_v1.MF", "c4_v1", container);
		Module dynamic1 = installDummyModule("dynamic1_v1.MF", "dynamic1_v1", container);
		Module dynamic1Frag = installDummyModule("dynamic1.frag_v1.MF", "dynamic1.frag_v1", container);

		container.resolve(Arrays.asList(c1, c4, dynamic1, dynamic1Frag), true);

		ModuleWire dynamicWire = container.resolveDynamic("c1.b", dynamic1.getCurrentRevision());
		Assert.assertNotNull("No dynamic wire found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "c1.b", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider for the wire found.", c1.getCurrentRevision(), dynamicWire.getProvider());

		dynamicWire = container.resolveDynamic("c4.a", dynamic1.getCurrentRevision());
		Assert.assertNotNull("No dynamic wire found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "c4.a", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider for the wire found.", c4.getCurrentRevision(), dynamicWire.getProvider());

		dynamicWire = container.resolveDynamic("c4.b", dynamic1.getCurrentRevision());
		Assert.assertNotNull("No dynamic wire found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "c4.b", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider for the wire found.", c4.getCurrentRevision(), dynamicWire.getProvider());
	}

	@Test
	public void testDynamicImport03() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		DummyModuleDatabase database = adaptor.getDatabase();
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		Module dynamic3 = installDummyModule("dynamic2_v1.MF", "dynamic2_v1", container);

		container.resolve(Arrays.asList(systemBundle, dynamic3), true);

		ModuleWire dynamicWire = container.resolveDynamic("c1.a", dynamic3.getCurrentRevision());
		Assert.assertNull("Dynamic wire found.", dynamicWire);

		Module c1v1 = installDummyModule("c1_v1.MF", "c1_v1", container);
		database.getModuleEvents();

		dynamicWire = container.resolveDynamic("c1.a", dynamic3.getCurrentRevision());
		Assert.assertNotNull("Dynamic wire not found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "c1.a", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider for the wire found.", c1v1.getCurrentRevision(), dynamicWire.getProvider());

		ModuleWiring c1v1Wiring = c1v1.getCurrentRevision().getWiring();
		Assert.assertNotNull("c1 wiring is null.", c1v1Wiring);

		Module c1v2 = installDummyModule("c1_v2.MF", "c1_v2", container);
		container.resolve(Arrays.asList(c1v2), true);
		database.getModuleEvents();

		dynamicWire = container.resolveDynamic("c1.b", dynamic3.getCurrentRevision());
		Assert.assertNotNull("Dynamic wire not found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "c1.b", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider.", c1v1.getCurrentRevision(), dynamicWire.getProvider());
	}

	@Test
	public void testDynamicImport04() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		DummyModuleDatabase database = adaptor.getDatabase();
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		Module dynamic3 = installDummyModule("dynamic2_v1.MF", "dynamic2_v1", container);

		container.resolve(Arrays.asList(systemBundle, dynamic3), true);

		ModuleWire dynamicWire = container.resolveDynamic("h1.a", dynamic3.getCurrentRevision());
		Assert.assertNull("Dynamic wire found.", dynamicWire);

		Module h1 = installDummyModule("h1_v1.MF", "h1_v1", container);
		Module f1 = installDummyModule("f1_v1.MF", "f1_v1", container);
		database.getModuleEvents();

		dynamicWire = container.resolveDynamic("h1.a", dynamic3.getCurrentRevision());
		Assert.assertNotNull("Dynamic wire not found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "h1.a", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider for the wire found.", h1.getCurrentRevision(), dynamicWire.getProvider());

		dynamicWire = container.resolveDynamic("f1.a", dynamic3.getCurrentRevision());
		Assert.assertNotNull("Dynamic wire not found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "f1.a", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider for the wire found.", h1.getCurrentRevision(), dynamicWire.getProvider());

		ModuleWiring h1Wiring = h1.getCurrentRevision().getWiring();
		Assert.assertNotNull("h1 wiring is null.", h1Wiring);

		ModuleWiring f1Wiring = f1.getCurrentRevision().getWiring();
		Assert.assertNotNull("f1 wiring is null.", f1Wiring);
	}

	@Test
	public void testDynamicImport05() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, null, null, "osgi.ee; osgi.ee=JavaSE; version:Version=\"1.5.0\"", container);

		container.resolve(Arrays.asList(systemBundle), true);

		Module c1 = installDummyModule("c1_v1.MF", "c1_v1", container);
		Module c4 = installDummyModule("c4_v1.MF", "c4_v1", container);
		Module dynamic3 = installDummyModule("dynamic3_v1.MF", "dynamic3_v1", container);
		Module dynamic3Frag = installDummyModule("dynamic3.frag_v1.MF", "dynamic3.frag_v1", container);

		container.resolve(Arrays.asList(c1, c4, dynamic3, dynamic3Frag), true);

		ModuleWire dynamicWire = container.resolveDynamic("c4.a", dynamic3.getCurrentRevision());
		Assert.assertNotNull("No dynamic wire found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "c4.a", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider for the wire found.", c4.getCurrentRevision(), dynamicWire.getProvider());

		dynamicWire = container.resolveDynamic("c4.b", dynamic3.getCurrentRevision());
		Assert.assertNotNull("No dynamic wire found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "c4.b", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider for the wire found.", c4.getCurrentRevision(), dynamicWire.getProvider());
	}

	@Test
	public void testDynamicImport06() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		Module dynamic3 = installDummyModule("dynamic2_v1.MF", "dynamic2_v1", container);

		container.resolve(Arrays.asList(systemBundle, dynamic3), true);

		Module f1 = installDummyModule("f1_v1.MF", "f1_v1", container);

		ModuleWire dynamicWire;
		dynamicWire = container.resolveDynamic("h1.a", dynamic3.getCurrentRevision());
		Assert.assertNull("Dynamic wire found.", dynamicWire);
		dynamicWire = container.resolveDynamic("f1.a", dynamic3.getCurrentRevision());
		Assert.assertNull("Dynamic wire found.", dynamicWire);

		Module h1 = installDummyModule("h1_v1.MF", "h1_v1", container);

		dynamicWire = container.resolveDynamic("h1.a", dynamic3.getCurrentRevision());
		Assert.assertNotNull("Dynamic wire not found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "h1.a", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong host revision found.", h1.getCurrentRevision(), dynamicWire.getProvider());

		dynamicWire = container.resolveDynamic("f1.a", dynamic3.getCurrentRevision());
		Assert.assertNotNull("Dynamic wire not found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "f1.a", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong host revision found.", h1.getCurrentRevision(), dynamicWire.getProvider());

		ModuleWiring h1Wiring = h1.getCurrentRevision().getWiring();
		Assert.assertNotNull("h1 wiring is null.", h1Wiring);

		ModuleWiring f1Wiring = f1.getCurrentRevision().getWiring();
		Assert.assertNotNull("f1 wiring is null.", f1Wiring);
	}

	@Test
	public void testDynamicImport07() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		Module dynamic3 = installDummyModule("dynamic2_v1.MF", "dynamic2_v1", container);

		Assert.assertNull("Expected no resolution exception.", container.resolve(Arrays.asList(systemBundle, dynamic3), true).getResolutionException());

		installDummyModule("c6_v1.MF", "c6_v1", container);

		ModuleWire dynamicWire = container.resolveDynamic("c6", dynamic3.getCurrentRevision());
		Assert.assertNull("Dynamic wire found.", dynamicWire);
	}

	@Test
	public void testDynamicImport08() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		DummyModuleDatabase database = adaptor.getDatabase();
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		Module dynamic2 = installDummyModule("dynamic2_v1.MF", "dynamic2_v1", container);

		container.resolve(Arrays.asList(systemBundle, dynamic2), true);

		Module h1 = installDummyModule("h1_v1.MF", "h1_v1", container);
		Module f1 = installDummyModule("f1_v1.MF", "f1_v1", container);
		database.getModuleEvents();
		// make sure h1 is not resolved
		ModuleWiring h1Wiring = h1.getCurrentRevision().getWiring();
		Assert.assertNull("h1 got resolved somehow.", h1Wiring);
		// do not resolve the host first; make sure it gets pulled in while attempting to resolve
		// to a fragment capability.
		ModuleWire dynamicWire = container.resolveDynamic("f1.a", dynamic2.getCurrentRevision());
		Assert.assertNotNull("Dynamic wire not found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "f1.a", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider for the wire found.", h1.getCurrentRevision(), dynamicWire.getProvider());

		h1Wiring = h1.getCurrentRevision().getWiring();
		Assert.assertNotNull("h1 wiring is null.", h1Wiring);

		ModuleWiring f1Wiring = f1.getCurrentRevision().getWiring();
		Assert.assertNotNull("f1 wiring is null.", f1Wiring);
	}

	@Test
	public void testDynamicImportMiss01() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, null, null, "osgi.ee; osgi.ee=JavaSE; version:Version=\"1.5.0\"", container);

		container.resolve(Arrays.asList(systemBundle), true);
		Module c1 = installDummyModule("c1_v1.MF", "c1_v1", container);
		Module dynamic1 = installDummyModule("dynamic1_v1.MF", "dynamic1_v1", container);

		container.resolve(Arrays.asList(c1, dynamic1), true);

		DummyResolverHookFactory factory = (DummyResolverHookFactory) adaptor.getResolverHookFactory();
		DummyResolverHook hook = (DummyResolverHook) factory.getHook();
		hook.getResolutionReports().clear();
		ModuleWire dynamicWire = container.resolveDynamic("org.osgi.framework", dynamic1.getCurrentRevision());
		Assert.assertNotNull("No dynamic wire found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "org.osgi.framework", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong provider for the wire found.", systemBundle.getCurrentRevision(), dynamicWire.getProvider());

		Assert.assertEquals("Wrong number of reports.", 1, hook.getResolutionReports().size());
		hook.getResolutionReports().clear();

		dynamicWire = container.resolveDynamic("does.not.exist", dynamic1.getCurrentRevision());
		Assert.assertNull("Unexpected Dynamic wire found.", dynamicWire);
		Assert.assertEquals("Wrong number of reports.", 1, hook.getResolutionReports().size());

		// Try again; no report should be generated a second time
		hook.getResolutionReports().clear();
		dynamicWire = container.resolveDynamic("does.not.exist", dynamic1.getCurrentRevision());
		Assert.assertNull("Unexpected Dynamic wire found.", dynamicWire);
		Assert.assertEquals("Wrong number of reports.", 0, hook.getResolutionReports().size());
	}

	@Test
	public void testRequireBundleUses() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);
		Module b1 = installDummyModule("require.b1.MF", "b1", container);
		installDummyModule("require.b2.MF", "b2", container);
		installDummyModule("require.b3.MF", "b3", container);
		installDummyModule("require.b4.MF", "b4", container);

		container.resolve(null, false);

		Assert.assertEquals("b1 should not resolve.", State.INSTALLED, b1.getState());
	}

	/*
	 * Test that a resolve process does not blow up because of one unresolvable uses constraint issue
	 */
	@Test
	public void testUses1() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);
		Module uses_a = installDummyModule("uses.a.MF", "a", container);
		Module uses_b = installDummyModule("uses.b.MF", "b", container);
		Module uses_c = installDummyModule("uses.c.MF", "c", container);

		container.resolve(null, false);

		Assert.assertEquals("a should resolve.", State.RESOLVED, uses_a.getState());
		Assert.assertEquals("b should resolve.", State.RESOLVED, uses_b.getState());
		Assert.assertEquals("c should not resolve.", State.INSTALLED, uses_c.getState());
	}

	@Test
	public void testUses1Dynamic() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor(new DummyDebugOptions(Collections.singletonMap("org.eclipse.osgi/resolver/report", "true")));
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);
		Module uses_a = installDummyModule("uses.a.MF", "a", container);
		Module uses_b = installDummyModule("uses.b.MF", "b", container);
		Module uses_c_dynamic = installDummyModule("uses.c.dynamic.MF", "c", container);

		container.resolve(null, false);

		Assert.assertEquals("a should resolve.", State.RESOLVED, uses_a.getState());
		Assert.assertEquals("b should resolve.", State.RESOLVED, uses_b.getState());
		Assert.assertEquals("c should resolve.", State.RESOLVED, uses_c_dynamic.getState());

		ModuleWire dynamicWire = container.resolveDynamic("uses1", uses_c_dynamic.getCurrentRevision());
		Assert.assertNotNull("No dynamic wire.", dynamicWire);

		PrintStream originalOut = Debug.out;
		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		PrintStream testOut = new PrintStream(bytesOut);
		Debug.out = testOut;
		try {
			dynamicWire = container.resolveDynamic("uses2", uses_c_dynamic.getCurrentRevision());
			Assert.assertNull("Dynamic wire found.", dynamicWire);
		} finally {
			Debug.out = originalOut;
			testOut.close();
		}
		String traceOutput = bytesOut.toString();
		Assert.assertTrue("Wrong traceOutput: " + traceOutput, traceOutput.startsWith("org.apache.felix.resolver.reason.ReasonException"));
	}

	/*
	 * Test that split packages are handled ok with uses constraints
	 */
	@Test
	public void testUses2() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);
		Module uses_a = installDummyModule("uses.a.MF", "a", container);
		Module uses_b = installDummyModule("uses.b.MF", "b", container);
		Module uses_d = installDummyModule("uses.d.MF", "d", container);

		container.resolve(null, false);

		Assert.assertEquals("a should resolve.", State.RESOLVED, uses_a.getState());
		Assert.assertEquals("b should resolve.", State.RESOLVED, uses_b.getState());
		Assert.assertEquals("d should resolve.", State.RESOLVED, uses_d.getState());
	}

	/*
	 * Test that split packages are handled ok with uses constraints
	 */
	@Test
	public void testUses3() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);
		Module uses_a = installDummyModule("uses.a.MF", "a", container);
		Module uses_b = installDummyModule("uses.b.MF", "b", container);
		Module uses_e = installDummyModule("uses.e.MF", "e", container);
		Module uses_f = installDummyModule("uses.f.MF", "f", container);
		Module uses_g = installDummyModule("uses.g.MF", "g", container);

		container.resolve(null, false);

		Assert.assertEquals("a should resolve.", State.RESOLVED, uses_a.getState());
		Assert.assertEquals("b should resolve.", State.RESOLVED, uses_b.getState());
		Assert.assertEquals("e should resolve.", State.RESOLVED, uses_e.getState());
		Assert.assertEquals("f should resolve.", State.RESOLVED, uses_f.getState());
		Assert.assertEquals("g should not resolve.", State.INSTALLED, uses_g.getState());
	}

	/*
	 * Test that fragments and uses constraints
	 */
	@Test
	public void testUses4() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);
		Module uses_h = installDummyModule("uses.h.MF", "h", container);
		Module uses_h_frag = installDummyModule("uses.h.frag.MF", "h.frag", container);

		container.resolve(null, false);

		Assert.assertEquals("h should resolve.", State.RESOLVED, uses_h.getState());
		Assert.assertEquals("h.frag should resolve.", State.RESOLVED, uses_h_frag.getState());

		Module uses_i = installDummyModule("uses.i.MF", "i", container);
		Module uses_j = installDummyModule("uses.j.MF", "j", container);

		container.resolve(null, false);

		Assert.assertEquals("i should resolve.", State.RESOLVED, uses_i.getState());
		Assert.assertEquals("j should resolve.", State.RESOLVED, uses_j.getState());

		List<BundleWire> requiredWires = uses_j.getCurrentRevision().getWiring().getRequiredWires(null);
		Assert.assertEquals("Wrong number of wires for j", 2, requiredWires.size());
		for (BundleWire wire : requiredWires) {
			Assert.assertEquals("Wrong provider", uses_i.getCurrentRevision(), wire.getProvider());
		}

		Module uses_j_dynamic = installDummyModule("uses.j.dynamic.MF", "j.dynamic", container);
		container.resolve(null, false);
		ModuleWire dynamicWire = container.resolveDynamic("uses2", uses_j_dynamic.getCurrentRevision());
		Assert.assertNotNull("Null dynamic wire.", dynamicWire);
		Assert.assertEquals("Wrong provider", uses_i.getCurrentRevision(), dynamicWire.getProvider());
	}

	/**
	 * Test optional constraints
	 * @throws BundleException
	 * @throws IOException
	 */
	@Test
	public void testUses5Importer() throws BundleException, IOException {
		doTestUses5("uses.k.importer.MF");
	}

	@Test
	public void testUses5ReqCap() throws BundleException, IOException {
		doTestUses5("uses.k.reqCap.MF");
	}

	@Test
	public void testUses5Requirer() throws BundleException, IOException {
		doTestUses5("uses.k.requirer.MF");
	}

	public void doTestUses5(String kManifest) throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);
		Module uses_k = installDummyModule(kManifest, "k", container);
		Module uses_l = installDummyModule("uses.l.MF", "l", container);
		Module uses_m_conflict1 = installDummyModule("uses.m.conflict1.MF", "m.conflict1", container);
		Module uses_m_conflict2 = installDummyModule("uses.m.conflict2.MF", "m.conflict2", container);

		container.resolve(null, false);

		Assert.assertEquals("k should resolve.", State.RESOLVED, uses_k.getState());
		Assert.assertEquals("l should resolve.", State.RESOLVED, uses_l.getState());
		Assert.assertEquals("m.conflict1 should resolve.", State.RESOLVED, uses_m_conflict1.getState());
		Assert.assertEquals("m.conflict2 should resolve.", State.RESOLVED, uses_m_conflict2.getState());
	}

	@Test
	public void testUses6FragConflicts() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);
		Module uses_n1 = installDummyModule("uses.n1.MF", "n1", container);
		installDummyModule("uses.n2.MF", "n2", container);
		Module uses_n2_frag = installDummyModule("uses.n2.frag.MF", "n2.frag", container);
		Module uses_n3 = installDummyModule("uses.n3.MF", "n3", container);
		ResolutionReport report = container.resolve(null, false);
		Assert.assertNull("resolution report has a resolution exception.", report.getResolutionException());

		Assert.assertEquals("n1 should resolve.", State.RESOLVED, uses_n1.getState());
		// TODO The following should be true, but on the current resolver in Mars the host is thrown away also
		//Assert.assertEquals("n2 should resolve.", State.RESOLVED, uses_n2.getState());
		Assert.assertEquals("n2.frag should not resolve.", State.INSTALLED, uses_n2_frag.getState());
		Assert.assertEquals("n3 should resolve.", State.RESOLVED, uses_n3.getState());
	}

	@Test
	public void testUsesTimeout() throws BundleException {
		// Always want to go to zero threads when idle
		int coreThreads = 0;
		// use the number of processors - 1 because we use the current thread when rejected
		int maxThreads = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);
		// idle timeout; make it short to get rid of threads quickly after resolve
		int idleTimeout = 5;
		// use sync queue to force thread creation
		BlockingQueue<Runnable> queue = new SynchronousQueue<>();
		// try to name the threads with useful name
		ThreadFactory threadFactory = r -> {
			Thread t = new Thread(r, "Resolver thread - UNIT TEST"); //$NON-NLS-1$
			t.setDaemon(true);
			return t;
		};
		// use a rejection policy that simply runs the task in the current thread once the max threads is reached
		RejectedExecutionHandler rejectHandler = (r, exe) -> r.run();
		ExecutorService executor = new ThreadPoolExecutor(coreThreads, maxThreads, idleTimeout, TimeUnit.SECONDS, queue, threadFactory, rejectHandler);
		ScheduledExecutorService timeoutExecutor = new ScheduledThreadPoolExecutor(1);

		Map<String, String> configuration = new HashMap<>();
		configuration.put(EquinoxConfiguration.PROP_RESOLVER_BATCH_TIMEOUT, "5000");
		Map<String, String> debugOpts = Collections.emptyMap();
		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(false), configuration, new DummyResolverHookFactory(), new DummyDebugOptions(debugOpts));
		adaptor.setResolverExecutor(executor);
		adaptor.setTimeoutExecutor(timeoutExecutor);
		ModuleContainer container = adaptor.getContainer();
		for (int i = 1; i <= 1000; i++) {
			for (Map<String, String> manifest : getUsesTimeoutManifests("test" + i)) {
				installDummyModule(manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), container);
			}
		}
		ResolutionReport report = container.resolve(container.getModules(), true);
		Assert.assertNull("Found resolution errors.", report.getResolutionException());
		for (Module module : container.getModules()) {
			Assert.assertEquals("Wrong state of module: " + module, State.RESOLVED, module.getState());
		}
		executor.shutdown();
		timeoutExecutor.shutdown();
		System.gc();
		System.gc();
		System.gc();
	}

	private List<Map<String, String>> getUsesTimeoutManifests(String prefix) {
		List<Map<String, String>> result = new ArrayList<>();
		// x1 bundle
		Map<String, String> x1Manifest = new HashMap<>();
		x1Manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		x1Manifest.put(Constants.BUNDLE_SYMBOLICNAME, prefix + ".x1");
		x1Manifest.put(Constants.EXPORT_PACKAGE, prefix + ".a; version=1.0; uses:=" + prefix + ".b");
		x1Manifest.put(Constants.IMPORT_PACKAGE, prefix + ".b; version=\"[1.1,1.2)\"");
		result.add(x1Manifest);
		// x2 bundle
		Map<String, String> x2Manifest = new HashMap<>();
		x2Manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		x2Manifest.put(Constants.BUNDLE_SYMBOLICNAME, prefix + ".x2");
		x2Manifest.put(Constants.EXPORT_PACKAGE, prefix + ".a; version=1.1; uses:=" + prefix + ".b");
		x2Manifest.put(Constants.IMPORT_PACKAGE, prefix + ".b; version=\"[1.0,1.1)\"");
		result.add(x2Manifest);
		// y1 bundle
		Map<String, String> y1Manifest = new HashMap<>();
		y1Manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		y1Manifest.put(Constants.BUNDLE_SYMBOLICNAME, prefix + ".y1");
		y1Manifest.put(Constants.EXPORT_PACKAGE, prefix + ".b; version=1.0");
		result.add(y1Manifest);
		// y1 bundle
		Map<String, String> y2Manifest = new HashMap<>();
		y2Manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		y2Manifest.put(Constants.BUNDLE_SYMBOLICNAME, prefix + ".y2");
		y2Manifest.put(Constants.EXPORT_PACKAGE, prefix + ".b; version=1.1");
		result.add(y2Manifest);
		// z1 bundle
		Map<String, String> z1Manifest = new HashMap<>();
		z1Manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		z1Manifest.put(Constants.BUNDLE_SYMBOLICNAME, prefix + ".z1");
		z1Manifest.put(Constants.IMPORT_PACKAGE, prefix + ".a, " + prefix + ".b");
		result.add(z1Manifest);
		return result;
	}

	@Test
	public void testOptionalSubstituted() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);
		Module sub_n = installDummyModule("sub.n.MF", "n", container);
		Module sub_l = installDummyModule("sub.l.MF", "l", container);
		Module sub_m = installDummyModule("sub.m.MF", "m", container);

		container.resolve(null, false);

		Assert.assertEquals("l should resolve.", State.RESOLVED, sub_l.getState());
		Assert.assertEquals("m should resolve.", State.RESOLVED, sub_m.getState());
		Assert.assertEquals("n should resolve.", State.RESOLVED, sub_n.getState());
	}

	@Test
	public void testStaticSubstituted() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);
		Module sub_n = installDummyModule("sub.n.static.MF", "n", container);
		Module sub_l = installDummyModule("sub.l.MF", "l", container);
		Module sub_m = installDummyModule("sub.m.MF", "m", container);

		container.resolve(null, false);

		Assert.assertEquals("l should resolve.", State.RESOLVED, sub_l.getState());
		Assert.assertEquals("m should resolve.", State.RESOLVED, sub_m.getState());
		Assert.assertEquals("n should resolve.", State.RESOLVED, sub_n.getState());
	}

	@Test
	public void testMultiCardinalityUses() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		Module p5v100 = installDummyModule("p5_v100.MF", "p5_v100", container);
		Module p5v101 = installDummyModule("p5_v101.MF", "p5_v101", container);
		Module p5v110 = installDummyModule("p5_v110.MF", "p5_v110", container);
		Module p5v111 = installDummyModule("p5_v111.MF", "p5_v111", container);
		installDummyModule("p6_v100.MF", "p6_v100", container);
		installDummyModule("p6_v110.MF", "p6_v110", container);
		installDummyModule("p7_v100.MF", "p7_v100", container);
		installDummyModule("p7_v110.MF", "p7_v110", container);

		container.resolve(null, false);

		Module c6v100 = installDummyModule("c6_v100.MF", "c6_v100", container);
		Module c6v110 = installDummyModule("c6_v110.MF", "c6_v110", container);
		Module c6v130 = installDummyModule("c6_v130.MF", "c6_v130", container);
		Module c6v140 = installDummyModule("c6_v140.MF", "c6_v140", container);
		Module c6v150 = installDummyModule("c6_v150.MF", "c6_v150", container);
		Module c6v170 = installDummyModule("c6_v170.MF", "c6_v170", container);

		//		Module c6v180 = installDummyModule("c6_v180.MF", "c6_v180", container);
		//		Module c6v120 = installDummyModule("c6_v120.MF", "c6_v120", container);

		container.resolve(null, false);

		final String namespace5 = "namespace.5";
		List<ModuleWire> p5v100Provided = p5v100.getCurrentRevision().getWiring().getProvidedModuleWires(namespace5);
		List<ModuleWire> p5v101Provided = p5v101.getCurrentRevision().getWiring().getProvidedModuleWires(namespace5);
		List<ModuleWire> p5v110Provided = p5v110.getCurrentRevision().getWiring().getProvidedModuleWires(namespace5);
		List<ModuleWire> p5v111Provided = p5v111.getCurrentRevision().getWiring().getProvidedModuleWires(namespace5);

		ModuleWiring c6v100Wiring = c6v100.getCurrentRevision().getWiring();
		List<ModuleWire> c6v100Required = c6v100Wiring.getRequiredModuleWires(namespace5);
		Assert.assertEquals("Wrong number of capabilities", 2, c6v100Required.size());
		assertWires(c6v100Required, p5v100Provided, p5v101Provided);

		ModuleWiring c6v110Wiring = c6v110.getCurrentRevision().getWiring();
		List<ModuleWire> c6v110Required = c6v110Wiring.getRequiredModuleWires(namespace5);
		Assert.assertEquals("Wrong number of capabilities", 2, c6v110Required.size());
		assertWires(c6v110Required, p5v100Provided, p5v101Provided);

		ModuleWiring c6v130Wiring = c6v130.getCurrentRevision().getWiring();
		List<ModuleWire> c6v130Required = c6v130Wiring.getRequiredModuleWires(namespace5);
		Assert.assertEquals("Wrong number of capabilities", 2, c6v130Required.size());
		assertWires(c6v130Required, p5v100Provided, p5v101Provided);

		ModuleWiring c6v140Wiring = c6v140.getCurrentRevision().getWiring();
		List<ModuleWire> c6v140Required = c6v140Wiring.getRequiredModuleWires(namespace5);
		Assert.assertEquals("Wrong number of capabilities", 2, c6v140Required.size());
		assertWires(c6v140Required, p5v100Provided, p5v101Provided);

		ModuleWiring c6v150Wiring = c6v150.getCurrentRevision().getWiring();
		List<ModuleWire> c6v150Required = c6v150Wiring.getRequiredModuleWires(namespace5);
		Assert.assertEquals("Wrong number of capabilities", 2, c6v150Required.size());
		assertWires(c6v150Required, p5v110Provided, p5v111Provided);

		ModuleWiring c6v170Wiring = c6v170.getCurrentRevision().getWiring();
		List<ModuleWire> c6v170Required = c6v170Wiring.getRequiredModuleWires(namespace5);
		Assert.assertEquals("Wrong number of capabilities", 2, c6v170Required.size());
		assertWires(c6v170Required, p5v110Provided, p5v111Provided);

		Module c6v160 = installDummyModule("c6_v160.MF", "c6_v160", container);

		container.resolve(null, false);

		Assert.assertNull("Bundle should not be resolved: " + c6v160, c6v160.getCurrentRevision().getWiring());

		container.uninstall(c6v160);

		Module c6v180 = installDummyModule("c6_v180.MF", "c6_v180", container);

		container.resolve(null, false);

		Assert.assertNull("Bundle should not be resolved: " + c6v180, c6v180.getCurrentRevision().getWiring());

		container.uninstall(c6v180);
	}

	@Test
	public void testCompatSingleton() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		Module s1 = installDummyModule("compatSingleton1.MF", "s1", container);
		Module s2 = installDummyModule("compatSingleton2.MF", "s2", container);
		Module s3 = installDummyModule("compatSingleton3.MF", "s3", container);

		String s1Singleton = s1.getCurrentRevision().getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).iterator().next().getDirectives().get(IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE);
		String s2Singleton = s2.getCurrentRevision().getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).iterator().next().getDirectives().get(IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE);
		String s3Singleton = s3.getCurrentRevision().getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).iterator().next().getDirectives().get(IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE);

		Assert.assertEquals("Wrong singleton directive: " + s1, "true", s1Singleton);
		Assert.assertNull("Wrong singleton directive: " + s2, s2Singleton);
		Assert.assertEquals("Wrong singleton directive: " + s3, "true", s3Singleton);
	}

	@Test
	public void testCompatReprovide() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		Module b1 = installDummyModule("compatReprovide1.MF", "b1", container);
		Module b2 = installDummyModule("compatReprovide2.MF", "b2", container);
		Module b3 = installDummyModule("compatReprovide3.MF", "b3", container);

		String b1Visibility = b1.getCurrentRevision().getRequirements(BundleNamespace.BUNDLE_NAMESPACE).iterator().next().getDirectives().get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE);
		String b2Visibility = b2.getCurrentRevision().getRequirements(BundleNamespace.BUNDLE_NAMESPACE).iterator().next().getDirectives().get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE);
		String b3Visibility = b3.getCurrentRevision().getRequirements(BundleNamespace.BUNDLE_NAMESPACE).iterator().next().getDirectives().get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE);

		Assert.assertEquals("Wrong visibility directive: " + b1, BundleNamespace.VISIBILITY_REEXPORT, b1Visibility);
		Assert.assertNull("Wrong visibility directive: ", b2Visibility);
		Assert.assertEquals("Wrong visibility directive: " + b2, BundleNamespace.VISIBILITY_REEXPORT, b3Visibility);
	}

	@Test
	public void testCompatOptional() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		Module b1 = installDummyModule("compatOptional1.MF", "b1", container);
		Module b2 = installDummyModule("compatOptional2.MF", "b2", container);
		Module b3 = installDummyModule("compatOptional3.MF", "b3", container);

		String b1BundleResolution = b1.getCurrentRevision().getRequirements(BundleNamespace.BUNDLE_NAMESPACE).iterator().next().getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
		String b2BundleResolution = b2.getCurrentRevision().getRequirements(BundleNamespace.BUNDLE_NAMESPACE).iterator().next().getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
		String b3BundleResolution = b3.getCurrentRevision().getRequirements(BundleNamespace.BUNDLE_NAMESPACE).iterator().next().getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);

		String b1PackageResolution = b1.getCurrentRevision().getRequirements(PackageNamespace.PACKAGE_NAMESPACE).iterator().next().getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
		String b2PackageResolution = b2.getCurrentRevision().getRequirements(PackageNamespace.PACKAGE_NAMESPACE).iterator().next().getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
		String b3PackageResolution = b3.getCurrentRevision().getRequirements(PackageNamespace.PACKAGE_NAMESPACE).iterator().next().getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);

		Assert.assertEquals("Wrong resolution directive: " + b1, Namespace.RESOLUTION_OPTIONAL, b1BundleResolution);
		Assert.assertNull("Wrong resolution directive: ", b2BundleResolution);
		Assert.assertEquals("Wrong resolution directive: " + b2, Namespace.RESOLUTION_OPTIONAL, b3BundleResolution);

		Assert.assertEquals("Wrong resolution directive: " + b1, Namespace.RESOLUTION_OPTIONAL, b1PackageResolution);
		Assert.assertNull("Wrong resolution directive: ", b2PackageResolution);
		Assert.assertEquals("Wrong resolution directive: " + b2, Namespace.RESOLUTION_OPTIONAL, b3PackageResolution);
	}

	@Test
	public void testCompatProvidePackage() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		Module b1 = installDummyModule("compatProvidePackage1.MF", "b1", container);

		List<ModuleCapability> packageCaps = b1.getCurrentRevision().getModuleCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		Assert.assertEquals("Wrong number of exports", 5, packageCaps.size());

		Assert.assertEquals("Wrong package name.", "foo", packageCaps.get(0).getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong package name.", "faa", packageCaps.get(1).getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong package name.", "bar", packageCaps.get(2).getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong package name.", "baz", packageCaps.get(3).getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		Assert.assertEquals("Wrong package name.", "biz", packageCaps.get(4).getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
	}

	@Test
	public void testBug457118() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);
		Module e = installDummyModule("bug457118.e.MF", "e", container);
		Module a = installDummyModule("bug457118.a.MF", "a", container);
		Module b = installDummyModule("bug457118.b.MF", "b", container);
		Module c = installDummyModule("bug457118.c.MF", "c", container);
		Module d = installDummyModule("bug457118.d.MF", "d", container);

		installDummyModule("bug457118.a2.MF", "a2", container);
		installDummyModule("bug457118.b2.MF", "b2", container);
		installDummyModule("bug457118.c2.MF", "c2", container);
		installDummyModule("bug457118.d2.MF", "d2", container);

		container.resolve(null, true);

		Assert.assertEquals("e should resolve.", State.RESOLVED, e.getState());
		Assert.assertEquals("a should resolve.", State.RESOLVED, a.getState());
		Assert.assertEquals("b should resolve.", State.RESOLVED, b.getState());
		Assert.assertEquals("c should resolve.", State.RESOLVED, c.getState());
		Assert.assertEquals("d should resolve.", State.RESOLVED, d.getState());

		List<ModuleWire> bundleWires = e.getCurrentRevision().getWiring().getRequiredModuleWires(BundleNamespace.BUNDLE_NAMESPACE);
		Assert.assertEquals("Wrong number of bundle wires: " + bundleWires, 1, bundleWires.size());
		Assert.assertEquals("Wrong bundle provider", a.getCurrentRevision(), bundleWires.get(0).getProvider());
	}

	@Test
	public void testBadNativeCode() throws IOException {
		try {
			OSGiManifestBuilderFactory.createBuilder(getManifest("bad.native.code.MF"));
		} catch (BundleException e) {
			Assert.assertEquals("Wrong exception type.", BundleException.MANIFEST_ERROR, e.getType());
		}

	}

	@Test
	public void testNativeWithFilterChars() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install the system.bundle
		String extraCapabilities = "osgi.native; osgi.native.osname=\"Windows NT (unknown)\"";
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, extraCapabilities, container);
		ResolutionReport report = container.resolve(Arrays.asList(systemBundle), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		// install bundle with Bundle-NativeCode
		Map<String, String> nativeCodeManifest = new HashMap<>();
		nativeCodeManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		nativeCodeManifest.put(Constants.BUNDLE_SYMBOLICNAME, "importer");
		nativeCodeManifest.put(Constants.BUNDLE_NATIVECODE, //
				"/lib/mylib.dll; osname=\"win32\"; osname=\"Windows NT (unknown)\"," + //
						"/lib/mylib.lib; osname=\"Linux\"");

		Module nativeCodeModule = installDummyModule(nativeCodeManifest, "nativeCodeBundle", container);

		// unsatisfied optional and dynamic imports do not fail a resolve.
		report = container.resolve(Arrays.asList(nativeCodeModule), true);
		Assert.assertNull("Failed to resolve nativeCodeBundle.", report.getResolutionException());
	}

	@Test
	public void testUTF8LineContinuation() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		String utfString = "a.with.�.multibyte";
		while (utfString.getBytes(StandardCharsets.UTF_8).length < 500) {
			Map<String, String> manifest = getUTFManifest(utfString);
			Module testModule = installDummyModule(manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), container);
			Assert.assertEquals("Wrong bns for the bundle.", utfString, testModule.getCurrentRevision().getSymbolicName());

			ModuleCapability exportPackage = testModule.getCurrentRevision().getModuleCapabilities(PackageNamespace.PACKAGE_NAMESPACE).get(0);
			ModuleRequirement importPackage = testModule.getCurrentRevision().getModuleRequirements(PackageNamespace.PACKAGE_NAMESPACE).get(0);

			String actualPackageName = (String) exportPackage.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
			Assert.assertEquals("Wrong exported package name.", utfString, actualPackageName);

			Assert.assertTrue("import does not match export: " + importPackage, importPackage.matches(exportPackage));

			utfString = "a" + utfString;
		}
	}

	@Test
	public void testDynamicWithOptionalImport() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install the system.bundle
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, null, container);
		ResolutionReport report = container.resolve(Arrays.asList(systemBundle), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		// install an importer
		Map<String, String> optionalImporterManifest = new HashMap<>();
		optionalImporterManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		optionalImporterManifest.put(Constants.BUNDLE_SYMBOLICNAME, "importer");
		optionalImporterManifest.put(Constants.IMPORT_PACKAGE, "exporter; resolution:=optional");
		optionalImporterManifest.put(Constants.DYNAMICIMPORT_PACKAGE, "exporter");
		Module optionalImporterModule = installDummyModule(optionalImporterManifest, "optionalImporter", container);

		// unsatisfied optional and dynamic imports do not fail a resolve.
		report = container.resolve(Arrays.asList(optionalImporterModule), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		//dynamic and optional imports are same. Optional import is not satisfied we should only see the dynamic import
		List<BundleRequirement> importReqsList = optionalImporterModule.getCurrentRevision().getWiring().getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of imports.", 1, importReqsList.size());
		assertEquals("Import was not dynamic", PackageNamespace.RESOLUTION_DYNAMIC, importReqsList.get(0).getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE));

		// install a exporter to satisfy existing optional import
		Map<String, String> exporterManifest = new HashMap<>();
		exporterManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		exporterManifest.put(Constants.BUNDLE_SYMBOLICNAME, "exporter");
		exporterManifest.put(Constants.EXPORT_PACKAGE, "exporter");
		installDummyModule(exporterManifest, "exporter", container);

		ModuleWire dynamicWire = container.resolveDynamic("exporter", optionalImporterModule.getCurrentRevision());
		Assert.assertNotNull("Expected to find a dynamic wire.", dynamicWire);

		// re-resolve importer
		container.refresh(Collections.singleton(optionalImporterModule));

		report = container.resolve(Arrays.asList(optionalImporterModule), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		importReqsList = optionalImporterModule.getCurrentRevision().getWiring().getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of imports.", 2, importReqsList.size());
	}

	@Test
	public void testDynamicWithExport() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install the system.bundle
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, null, container);
		ResolutionReport report = container.resolve(Arrays.asList(systemBundle), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		// install an importer
		Map<String, String> optionalImporterManifest = new HashMap<>();
		optionalImporterManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		optionalImporterManifest.put(Constants.BUNDLE_SYMBOLICNAME, "importer");
		optionalImporterManifest.put(Constants.EXPORT_PACKAGE, "exporter");
		optionalImporterManifest.put(Constants.DYNAMICIMPORT_PACKAGE, "exporter");
		Module optionalImporterModule = installDummyModule(optionalImporterManifest, "optionalImporter", container);

		// unsatisfied optional and dynamic imports do not fail a resolve.
		report = container.resolve(Arrays.asList(optionalImporterModule), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		//dynamic and optional imports are same. Optional import is not satisfied we should only see the dynamic import
		List<BundleRequirement> importReqsList = optionalImporterModule.getCurrentRevision().getWiring().getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of imports.", 1, importReqsList.size());
		assertEquals("Import was not dynamic", PackageNamespace.RESOLUTION_DYNAMIC, importReqsList.get(0).getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE));

		ModuleWire dynamicWire = container.resolveDynamic("exporter", optionalImporterModule.getCurrentRevision());
		Assert.assertNull("Expected no dynamic wire.", dynamicWire);
	}

	@Test
	public void testSubstitutableExport() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install the system.bundle
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, null, container);
		ResolutionReport report = container.resolve(Arrays.asList(systemBundle), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		// install an exporter with substitutable export.
		Map<String, String> exporterManifest = new HashMap<>();
		exporterManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		exporterManifest.put(Constants.BUNDLE_SYMBOLICNAME, "exporter");
		exporterManifest.put(Constants.EXPORT_PACKAGE, "exporter");
		exporterManifest.put(Constants.IMPORT_PACKAGE, "exporter");
		Module moduleSubsExport = installDummyModule(exporterManifest, "exporter", container);
		report = container.resolve(Arrays.asList(moduleSubsExport), true);
		Assert.assertNull("Failed to resolve", report.getResolutionException());
		List<BundleRequirement> reqs = moduleSubsExport.getCurrentRevision().getWiring().getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of imports.", 0, reqs.size());

		container.uninstall(moduleSubsExport);

		exporterManifest = new HashMap<>();
		exporterManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		exporterManifest.put(Constants.BUNDLE_SYMBOLICNAME, "substitutableExporter");
		exporterManifest.put(Constants.EXPORT_PACKAGE, "exporter");
		exporterManifest.put(Constants.IMPORT_PACKAGE, "exporter; pickme=true");

		moduleSubsExport = installDummyModule(exporterManifest, "substitutableExporter", container);

		exporterManifest = new HashMap<>();
		exporterManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		exporterManifest.put(Constants.BUNDLE_SYMBOLICNAME, "exporter");
		exporterManifest.put(Constants.EXPORT_PACKAGE, "exporter; pickme=true");

		Module moduleExport = installDummyModule(exporterManifest, "exporter", container);
		report = container.resolve(Arrays.asList(moduleSubsExport/* ,moduleExport */), true);
		Assert.assertNull("Failed to resolve", report.getResolutionException());

		List<BundleCapability> caps = moduleSubsExport.getCurrentRevision().getWiring().getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of capabilities.", 0, caps.size());

		reqs = moduleSubsExport.getCurrentRevision().getWiring().getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of imports.", 1, reqs.size());

		ModuleWiring wiring = moduleSubsExport.getCurrentRevision().getWiring();
		List<ModuleWire> packageWires = wiring.getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
		Assert.assertEquals("Unexpected number of wires", 1, packageWires.size());
		Assert.assertEquals("Wrong exporter", packageWires.get(0).getProviderWiring().getRevision(), moduleExport.getCurrentRevision());
	}

	@Test
	public void testSubstitutableExportBatch() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(false), Collections.singletonMap(EquinoxConfiguration.PROP_RESOLVER_REVISION_BATCH_SIZE, Integer.toString(1)));
		ModuleContainer container = adaptor.getContainer();

		// install the system.bundle
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, null, container);
		ResolutionReport report = container.resolve(Arrays.asList(systemBundle), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		Map<String, String> manifest = new HashMap<>();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "substitutableExporter");
		manifest.put(Constants.EXPORT_PACKAGE, "exporter; uses:=usedPkg; version=1.1, usedPkg");
		manifest.put(Constants.IMPORT_PACKAGE, "exporter; pickme=true");

		Module moduleSubsExport = installDummyModule(manifest, "substitutableExporter", container);

		manifest = new HashMap<>();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "exporter");
		manifest.put(Constants.EXPORT_PACKAGE, "exporter; version=1.0; pickme=true");
		Module moduleExport = installDummyModule(manifest, "exporter", container);

		manifest = new HashMap<>();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "importer1");
		manifest.put(Constants.IMPORT_PACKAGE, "exporter, usedPkg");
		Module moduleImporter1 = installDummyModule(manifest, "importer1", container);

		manifest = new HashMap<>();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "importer2");
		manifest.put(Constants.EXPORT_PACKAGE, "pkgUser; uses:=exporter");
		manifest.put(Constants.IMPORT_PACKAGE, "exporter, usedPkg");
		Module moduleImporter2 = installDummyModule(manifest, "importer2", container);

		manifest = new HashMap<>();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "importer3");
		manifest.put(Constants.IMPORT_PACKAGE, "pkgUser, exporter");
		Module moduleImporter3 = installDummyModule(manifest, "importer3", container);

		report = container.resolve(Arrays.asList(moduleExport, moduleSubsExport, moduleImporter1, moduleImporter2, moduleImporter3), true);
		Assert.assertNull("Failed to resolve", report.getResolutionException());

		ModuleWiring subsExportWiring = moduleSubsExport.getCurrentRevision().getWiring();
		Collection<String> substituteNames = subsExportWiring.getSubstitutedNames();
		Assert.assertEquals("Wrong number of exports: " + substituteNames, 1, substituteNames.size());
		List<ModuleWire> providedWires = moduleSubsExport.getCurrentRevision().getWiring().getProvidedModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of wires.", 2, providedWires.size());

		ModuleWiring importer3Wiring = moduleImporter3.getCurrentRevision().getWiring();
		for (ModuleWire wire : importer3Wiring.getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE)) {
			if ("exporter".equals(wire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE))) {
				assertEquals("wrong provider", moduleExport.getCurrentRevision(), wire.getProvider());
			}

		}
	}

	@Test
	public void testR3() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		// install the system.bundle
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, null, container);
		ResolutionReport report = container.resolve(Arrays.asList(systemBundle), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		//R3 bundle
		Map<String, String> exporterManifest = new HashMap<>();
		exporterManifest = new HashMap<>();
		exporterManifest.put(Constants.BUNDLE_SYMBOLICNAME, "exporter");
		exporterManifest.put(Constants.EXPORT_PACKAGE, "exporter; version=\"1.1\"");

		Module moduleExport = installDummyModule(exporterManifest, "exporter", container);
		report = container.resolve(Arrays.asList(moduleExport, moduleExport), true);
		Assert.assertNull("Failed to resolve", report.getResolutionException());
		List<BundleRequirement> reqs = moduleExport.getCurrentRevision().getWiring().getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of imports.", 0, reqs.size());

		//R3 bundle
		exporterManifest.clear();
		exporterManifest.put(Constants.BUNDLE_SYMBOLICNAME, "dynamicExporter");
		exporterManifest.put(Constants.EXPORT_PACKAGE, "exporter; version=\"1.0\"");
		exporterManifest.put(Constants.DYNAMICIMPORT_PACKAGE, "exporter");
		Module moduleWithDynExport = installDummyModule(exporterManifest, "dynamicExporter", container);
		report = container.resolve(Arrays.asList(moduleWithDynExport), true);
		Assert.assertNull("Failed to resolve", report.getResolutionException());
		reqs = moduleWithDynExport.getCurrentRevision().getWiring().getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of imports.", 2, reqs.size());

		report = container.resolve(Arrays.asList(moduleWithDynExport), true);
		Assert.assertNull("Failed to resolve", report.getResolutionException());
		reqs = moduleWithDynExport.getCurrentRevision().getWiring().getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of imports.", 2, reqs.size());
		ModuleWiring wiring = moduleWithDynExport.getCurrentRevision().getWiring();
		List<ModuleWire> packageWires = wiring.getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
		Assert.assertEquals("Unexpected number of wires", 1, packageWires.size());
		Assert.assertEquals("Wrong exporter", packageWires.get(0).getProviderWiring().getRevision(), moduleExport.getCurrentRevision());
	}

	private static Map<String, String> getUTFManifest(String packageName) throws IOException, BundleException {
		// using manifest class to force a split line right in the middle of a double byte UTF-8 character
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		{
			Manifest m = new Manifest();
			Attributes a = m.getMainAttributes();
			a.put(MANIFEST_VERSION, "1.0");
			a.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
			a.putValue(Constants.BUNDLE_SYMBOLICNAME, packageName);
			a.putValue(Constants.EXPORT_PACKAGE, packageName);
			a.putValue(Constants.IMPORT_PACKAGE, packageName);
			m.write(out);
		}
		return ManifestElement.parseBundleManifest(new ByteArrayInputStream(out.toByteArray()), null);
	}

	@Test
	public void testPersistence() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install the system.bundle
		installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, null, container);

		Map<String, Object> attrs = new HashMap<>();
		attrs.put("string", "sValue");
		attrs.put("string.list1", Arrays.asList("v1", "v2", "v3"));
		attrs.put("string.list2", Arrays.asList("v4", "v5", "v6"));
		attrs.put("version", Version.valueOf("1.1"));
		attrs.put("version.list", Arrays.asList(Version.valueOf("1.0"), Version.valueOf("2.0"), Version.valueOf("3.0")));
		attrs.put("long", Long.valueOf(12345));
		attrs.put("long.list", Arrays.asList(Long.valueOf(1), Long.valueOf(2), Long.valueOf(3)));
		attrs.put("double", Double.valueOf(1.2345));
		attrs.put("double.list", Arrays.asList(Double.valueOf(1.1), Double.valueOf(1.2), Double.valueOf(1.3)));
		attrs.put("uri", "some.uri");
		attrs.put("set", Arrays.asList("s1", "s2", "s3"));

		// provider with all supported types
		Map<String, String> providerManifest = new HashMap<>();
		providerManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		providerManifest.put(Constants.BUNDLE_SYMBOLICNAME, "provider");
		providerManifest.put(Constants.EXPORT_PACKAGE, "provider; version=1.1; attr1=attr1; attr2=attr2; dir1:=dir1; dir2:=dir2");
		providerManifest.put(Constants.PROVIDE_CAPABILITY, "provider.cap;"//
				+ " string=sValue;"//
				+ " string.list1:List=\"v1,v2,v3\";"//
				+ " string.list2:List<String>=\"v4,v5,v6\";"//
				+ " version:Version=1.1;"//
				+ " version.list:List<Version>=\"1.0,2.0,3.0\";"//
				+ " long:Long=12345;"//
				+ " long.list:List<Long>=\"1,2,3\";"//
				+ " double:Double=1.2345;"//
				+ " double.list:List<Double>=\"1.1,1.2,1.3\";"//
				+ " uri:uri=some.uri;" //
				+ " set:set=\"s1,s2,s3\"");
		Module providerModule = installDummyModule(providerManifest, "provider", container);
		Map<String, Object> providerAttrs = providerModule.getCurrentRevision().getCapabilities("provider.cap").get(0).getAttributes();
		assertEquals("Wrong provider attrs", attrs, providerAttrs);

		Map<String, String> requirerManifest = new HashMap<>();
		requirerManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		requirerManifest.put(Constants.BUNDLE_SYMBOLICNAME, "requirer");
		requirerManifest.put(Constants.IMPORT_PACKAGE, "provider; version=1.1; attr1=attr1; attr2=attr2; dir1:=dir1; dir2:=dir2");
		requirerManifest.put(Constants.REQUIRE_CAPABILITY, "optional;"//
				+ " resolution:=optional; " //
				+ " string=sValue;"//
				+ " string.list1:List=\"v1,v2,v3\";"//
				+ " string.list2:List<String>=\"v4,v5,v6\";"//
				+ " version:Version=1.1;"//
				+ " version.list:List<Version>=\"1.0,2.0,3.0\";"//
				+ " long:Long=12345;"//
				+ " long.list:List<Long>=\"1,2,3\";"//
				+ " double:Double=1.2345;"//
				+ " double.list:List<Double>=\"1.1,1.2,1.3\";"//
				+ " uri:uri=some.uri;" //
				+ " set:set=\"s1,s2,s3\"," //
				+ "provider.cap; filter:=\"(string=sValue)\"," //
				+ "provider.cap; filter:=\"(string.list1=v2)\"," //
				+ "provider.cap; filter:=\"(string.list2=v5)\"," //
				+ "provider.cap; filter:=\"(string.list2=v5)\"," //
				+ "provider.cap; filter:=\"(&(version>=1.1)(version<=1.1.1))\"," //
				+ "provider.cap; filter:=\"(&(version.list=1)(version.list=2))\"," //
				+ "provider.cap; filter:=\"(long>=12344)\"," //
				+ "provider.cap; filter:=\"(long.list=2)\"," //
				+ "provider.cap; filter:=\"(double>=1.2)\"," //
				+ "provider.cap; filter:=\"(double.list=1.2)\"," //
				+ "provider.cap; filter:=\"(uri=some.uri)\"," //
				+ "provider.cap; filter:=\"(set=s2)\"" //
				+ "");
		Module requirerModule = installDummyModule(requirerManifest, "requirer", container);
		Map<String, Object> requirerAttrs = requirerModule.getCurrentRevision().getRequirements("optional").get(0).getAttributes();
		assertEquals("Wrong requirer attrs", attrs, requirerAttrs);
		ResolutionReport report = container.resolve(Collections.singleton(requirerModule), true);
		assertNull("Error resolving.", report.getResolutionException());

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(bytes);
		adaptor.getDatabase().store(data, true);

		// reload into a new container
		adaptor = createDummyAdaptor();
		container = adaptor.getContainer();
		adaptor.getDatabase().load(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

		providerModule = container.getModule("provider");
		providerAttrs = providerModule.getCurrentRevision().getCapabilities("provider.cap").get(0).getAttributes();
		assertEquals("Wrong provider attrs", attrs, providerAttrs);
		assertNotNull("No provider found.", providerModule);

		requirerModule = container.getModule("requirer");
		assertNotNull("No requirer found.", requirerModule);
		requirerAttrs = requirerModule.getCurrentRevision().getRequirements("optional").get(0).getAttributes();
		assertEquals("Wrong requirer attrs", attrs, requirerAttrs);
	}

	@Test
	public void testInvalidAttributes() throws IOException, BundleException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install the system.bundle
		installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, null, container);

		// provider with all supported types
		Map<String, String> invalidAttrManifest = new HashMap<>();
		invalidAttrManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		invalidAttrManifest.put(Constants.BUNDLE_SYMBOLICNAME, "invalid");

		invalidAttrManifest.put(Constants.PROVIDE_CAPABILITY, "provider.cap; invalid:Boolean=true");
		checkInvalidManifest(invalidAttrManifest, container);

		invalidAttrManifest.put(Constants.PROVIDE_CAPABILITY, "provider.cap; invalid:Integer=1");
		checkInvalidManifest(invalidAttrManifest, container);

		invalidAttrManifest.put(Constants.PROVIDE_CAPABILITY, "provider.cap; invalid:List<Boolean>=true");
		checkInvalidManifest(invalidAttrManifest, container);

		invalidAttrManifest.put(Constants.PROVIDE_CAPABILITY, "provider.cap; invalid:List<Integer>=1");
		checkInvalidManifest(invalidAttrManifest, container);
	}

	private void checkInvalidManifest(Map<String, String> invalidAttrManifest, ModuleContainer container) {
		try {
			installDummyModule(invalidAttrManifest, "invalid", container);
			fail("Expected to get a BundleException with MANIFEST_ERROR");
		} catch (BundleException e) {
			// find expected type
			assertEquals("Wrong type.", BundleException.MANIFEST_ERROR, e.getType());
		}
	}

	@Test
	public void testStoreInvalidAttributes() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install the system.bundle
		installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, null, container);

		Integer testInt = Integer.valueOf(1);
		List<Integer> testIntList = Collections.singletonList(testInt);
		ModuleRevisionBuilder builder = new ModuleRevisionBuilder();
		builder.setSymbolicName("invalid.attr");
		builder.setVersion(Version.valueOf("1.0.0"));
		builder.addCapability("test", Collections.emptyMap(), Collections.singletonMap("test", (Object) testInt));
		builder.addCapability("test.list", Collections.emptyMap(), Collections.singletonMap("test.list", (Object) testIntList));
		Module invalid = container.install(null, builder.getSymbolicName(), builder, null);

		Object testAttr = invalid.getCurrentRevision().getCapabilities("test").get(0).getAttributes().get("test");
		assertEquals("Wrong test attr", testInt, testAttr);

		Object testAttrList = invalid.getCurrentRevision().getCapabilities("test.list").get(0).getAttributes().get("test.list");
		assertEquals("Wrong test list attr", testIntList, testAttrList);

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(bytes);
		adaptor.getDatabase().store(data, true);

		List<DummyContainerEvent> events = adaptor.getDatabase().getContainerEvents();
		// make sure we see the errors
		assertEquals("Wrong number of events.", 2, events.size());
		for (DummyContainerEvent event : events) {
			assertEquals("Wrong type of event.", ContainerEvent.ERROR, event.type);
			assertTrue("Wrong type of exception.", event.error instanceof BundleException);
		}

		// reload into a new container
		adaptor = createDummyAdaptor();
		container = adaptor.getContainer();
		adaptor.getDatabase().load(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

		invalid = container.getModule("invalid.attr");
		assertNotNull("Could not find module.", invalid);

		String testIntString = String.valueOf(testInt);
		List<String> testIntStringList = Collections.singletonList(testIntString);
		testAttr = invalid.getCurrentRevision().getCapabilities("test").get(0).getAttributes().get("test");
		assertEquals("Wrong test attr", testIntString, testAttr);

		testAttrList = invalid.getCurrentRevision().getCapabilities("test.list").get(0).getAttributes().get("test.list");
		assertEquals("Wrong test list attr", testIntStringList, testAttrList);
	}

	@Test
	public void testBug483849() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install and resolve host bundle
		Module host = installDummyModule("bug483849.host.MF", "host", container);
		ResolutionReport report = container.resolve(Arrays.asList(host), true);
		Assert.assertNull("Failed to resolve host.", report.getResolutionException());

		// install and dynamically attach a fragment that exports a package and resolve an importer
		Module frag = installDummyModule("bug483849.frag.MF", "frag", container);
		Module importer = installDummyModule("bug483849.importer.MF", "importer", container);
		report = container.resolve(Arrays.asList(frag, importer), true);
		Assert.assertNull("Failed to resolve test fragment and importer.", report.getResolutionException());
		// get the count of package exports
		ModuleWiring wiring = host.getCurrentRevision().getWiring();
		int originalPackageCnt = wiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE).size();

		// update the host to generate a new revision
		Map<String, String> updateManifest = getManifest("bug483849.host.MF");
		ModuleRevisionBuilder updateBuilder = OSGiManifestBuilderFactory.createBuilder(updateManifest);
		container.update(host, updateBuilder, null);
		// refresh host which should force the importer to re-resolve to the new revision
		report = container.refresh(Collections.singleton(host));

		ModuleWiring importerWiring = importer.getCurrentRevision().getWiring();
		Assert.assertNotNull("No wiring for importer.", importerWiring);
		List<ModuleWire> importerPackageWires = importerWiring.getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
		Assert.assertEquals("Wrong number of importer package Wires.", 1, importerPackageWires.size());

		Assert.assertEquals("Wrong provider wiring.", host.getCurrentRevision().getWiring(), importerPackageWires.iterator().next().getProviderWiring());
		Assert.assertEquals("Wrong provider revision.", host.getCurrentRevision(), importerPackageWires.iterator().next().getProviderWiring().getRevision());

		wiring = host.getCurrentRevision().getWiring();
		List<BundleCapability> packages = wiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		Assert.assertEquals("Wrong number of host packages.", originalPackageCnt, packages.size());
	}

	@Test
	public void testStartLevelDeadlock() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		container.getFrameworkStartLevel().setInitialBundleStartLevel(2);

		// install the system.bundle
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, null, container);
		ResolutionReport report = container.resolve(Arrays.asList(systemBundle), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());
		systemBundle.start();

		// install a module
		Map<String, String> manifest = new HashMap<>();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "module.test");
		Module module = installDummyModule(manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), container);
		adaptor.setSlowdownEvents(true);
		module.setStartLevel(1);
		module.start();

		List<DummyContainerEvent> events = adaptor.getDatabase().getContainerEvents();
		for (DummyContainerEvent event : events) {
			Assert.assertNotEquals("Found an error: " + event.error, ContainerEvent.ERROR, event.type);
		}
	}

	@Test
	public void testSystemBundleOnDemandFragments() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install the system.bundle
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, null, container);

		// install an equinox fragment
		Map<String, String> equinoxFragManifest = new HashMap<>();
		equinoxFragManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		equinoxFragManifest.put(Constants.BUNDLE_SYMBOLICNAME, "equinoxFrag");
		equinoxFragManifest.put(Constants.FRAGMENT_HOST, "org.eclipse.osgi");
		Module equinoxFrag = installDummyModule(equinoxFragManifest, "equinoxFrag", container);

		// install a system.bundle fragment
		Map<String, String> systemFragManifest = new HashMap<>();
		systemFragManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		systemFragManifest.put(Constants.BUNDLE_SYMBOLICNAME, "systemFrag");
		systemFragManifest.put(Constants.FRAGMENT_HOST, "system.bundle");
		Module systemFrag = installDummyModule(systemFragManifest, "systemFrag", container);

		ResolutionReport report = container.resolve(Arrays.asList(systemBundle), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		List<ModuleWire> hostWires = systemBundle.getCurrentRevision().getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
		assertEquals("Wrong number of fragments.", 2, hostWires.size());
		Set<ModuleRevision> fragmentRevisions = new HashSet(Arrays.asList(equinoxFrag.getCurrentRevision(), systemFrag.getCurrentRevision()));
		for (ModuleWire hostWire : hostWires) {
			if (!fragmentRevisions.remove(hostWire.getRequirer())) {
				Assert.fail("Unexpected fragment revision: " + hostWire.getRequirer());
			}
		}
	}

	@Test
	public void testUnresolvedHostWithFragmentCycle() throws BundleException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install a host
		Map<String, String> hostManifest = new HashMap<>();
		hostManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		hostManifest.put(Constants.BUNDLE_SYMBOLICNAME, "host");
		hostManifest.put(Constants.BUNDLE_VERSION, "1.0");
		hostManifest.put(Constants.EXPORT_PACKAGE, "host");
		hostManifest.put(Constants.IMPORT_PACKAGE, "host.impl");
		installDummyModule(hostManifest, "host10", container);
		hostManifest.put(Constants.BUNDLE_VERSION, "1.1");
		installDummyModule(hostManifest, "host11", container);
		hostManifest.put(Constants.BUNDLE_VERSION, "1.2");
		installDummyModule(hostManifest, "host12", container);
		//hostManifest.put(Constants.BUNDLE_VERSION, "1.3");
		//installDummyModule(hostManifest, "host13", container);

		// install a host.impl fragment
		Map<String, String> hostImplManifest = new HashMap<>();
		hostImplManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		hostImplManifest.put(Constants.BUNDLE_SYMBOLICNAME, "host.impl");
		hostImplManifest.put(Constants.EXPORT_PACKAGE, "host.impl");
		hostImplManifest.put(Constants.IMPORT_PACKAGE, "host");
		hostImplManifest.put(Constants.FRAGMENT_HOST, "host");
		installDummyModule(hostImplManifest, "hostImpl", container);

		// install an importer of host package
		Map<String, String> hostImporterManifest = new HashMap<>();
		hostImporterManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		hostImporterManifest.put(Constants.BUNDLE_SYMBOLICNAME, "host.importer");
		hostImporterManifest.put(Constants.IMPORT_PACKAGE, "host");
		Module hostImporter = installDummyModule(hostImporterManifest, "hostImporter", container);

		ResolutionReport report = container.resolve(Arrays.asList(hostImporter), true);
		Assert.assertNull("Failed to resolve test.", report.getResolutionException());
	}

	@Test
	public void testMultiHostFragmentWithOverlapImport() throws BundleException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install an exporter
		Map<String, String> exporterManifest = new HashMap<>();
		exporterManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		exporterManifest.put(Constants.BUNDLE_SYMBOLICNAME, "exporter");
		exporterManifest.put(Constants.BUNDLE_VERSION, "1.0");
		exporterManifest.put(Constants.EXPORT_PACKAGE, "exporter");
		installDummyModule(exporterManifest, "exporter", container);

		// install a fragment to the exporter
		Map<String, String> exporterFragManifest = new HashMap<>();
		exporterFragManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		exporterFragManifest.put(Constants.BUNDLE_SYMBOLICNAME, "exporter.frag");
		exporterFragManifest.put(Constants.EXPORT_PACKAGE, "exporter.frag");
		exporterFragManifest.put(Constants.FRAGMENT_HOST, "exporter");
		installDummyModule(exporterFragManifest, "exporter.frag", container);

		// install a host that imports the exporter
		Map<String, String> hostManifest = new HashMap<>();
		hostManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		hostManifest.put(Constants.BUNDLE_SYMBOLICNAME, "host");
		hostManifest.put(Constants.BUNDLE_VERSION, "1.0");
		hostManifest.put(Constants.IMPORT_PACKAGE, "exporter");
		installDummyModule(hostManifest, "host10", container);
		hostManifest.put(Constants.BUNDLE_VERSION, "1.1");
		installDummyModule(hostManifest, "host11", container);
		hostManifest.put(Constants.BUNDLE_VERSION, "1.2");
		installDummyModule(hostManifest, "host12", container);

		// install a fragment that also imports the exporter
		Map<String, String> hostFragManifest = new HashMap<>();
		hostFragManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		hostFragManifest.put(Constants.BUNDLE_SYMBOLICNAME, "host.frag");
		hostFragManifest.put(Constants.FRAGMENT_HOST, "host");
		hostFragManifest.put(Constants.IMPORT_PACKAGE, "exporter; version=0.0");
		Module hostFrag = installDummyModule(hostFragManifest, "host.frag", container);

		ResolutionReport report = container.resolve(Arrays.asList(hostFrag), true);
		Assert.assertNull("Failed to resolve test.", report.getResolutionException());
	}

	@Test
	public void testModuleWiringToString() throws BundleException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install a test module
		Map<String, String> testManifest = new HashMap<>();
		testManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		testManifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.name");
		testManifest.put(Constants.BUNDLE_VERSION, "1.0");
		Module testModule = installDummyModule(testManifest, "host10", container);

		ResolutionReport report = container.resolve(Arrays.asList(testModule), true);
		Assert.assertNull("Failed to resolve test.", report.getResolutionException());

		ModuleRevision revision = testModule.getCurrentRevision();
		ModuleWiring wiring = revision.getWiring();
		Assert.assertEquals("Unexpected wiring.toString()", revision.toString(), wiring.toString());
	}

	@Test
	public void testStartOnResolve() throws BundleException, IOException {
		doTestStartOnResolve(true);
	}

	@Test
	public void testDisableStartOnResolve() throws BundleException, IOException {
		doTestStartOnResolve(false);
	}

	private void doTestStartOnResolve(boolean enabled) throws BundleException, IOException {
		Map<String, String> configuration = new HashMap<>();
		if (!enabled) {
			configuration.put(EquinoxConfiguration.PROP_MODULE_AUTO_START_ON_RESOLVE, Boolean.toString(false));
		}
		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(false), configuration);
		ModuleContainer container = adaptor.getContainer();

		// install the system.bundle
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, null, container);
		ResolutionReport report = container.resolve(Arrays.asList(systemBundle), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());
		systemBundle.start();

		// install a bunch of modules
		Map<String, String> manifest = new HashMap<>();
		List<Module> modules = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			manifest.clear();
			manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
			manifest.put(Constants.BUNDLE_SYMBOLICNAME, "module." + i);
			manifest.put(Constants.IMPORT_PACKAGE, "export");
			Module module = installDummyModule(manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), container);
			try {
				module.start();
				fail("expected a bundle exception.");
			} catch (BundleException e) {
				// do nothing
			}
			modules.add(module);
		}

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "exporter");
		manifest.put(Constants.EXPORT_PACKAGE, "export");
		installDummyModule(manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), container);

		report = container.resolve(Collections.emptySet(), false);
		Assert.assertNull("Found a error.", report.getResolutionException());

		State expectedState = enabled ? State.ACTIVE : State.RESOLVED;
		for (Module module : modules) {
			Assert.assertEquals("Wrong state.", expectedState, module.getState());
		}
	}

	@Test
	public void testResolveDeadlock() throws BundleException, IOException, InterruptedException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install the system.bundle
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, null, container);
		ResolutionReport report = container.resolve(Arrays.asList(systemBundle), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());
		systemBundle.start();

		// install a bunch of modules
		Map<String, String> manifest = new HashMap<>();
		List<Module> modules = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			manifest.clear();
			manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
			manifest.put(Constants.BUNDLE_SYMBOLICNAME, "module." + i);
			modules.add(installDummyModule(manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), container));
		}
		adaptor.setSlowdownEvents(true);
		final ConcurrentLinkedQueue<BundleException> startErrors = new ConcurrentLinkedQueue<>();
		final ExecutorService executor = Executors.newFixedThreadPool(5);
		try {
			for (final Module module : modules) {

				executor.execute(() -> {
					try {
						module.start();
					} catch (BundleException e) {
						startErrors.offer(e);
						e.printStackTrace();
					}
				});
			}
		} finally {
			executor.shutdown();
			executor.awaitTermination(5, TimeUnit.MINUTES);
			systemBundle.stop();
		}

		Assert.assertNull("Found a error.", startErrors.poll());
		List<DummyContainerEvent> events = adaptor.getDatabase().getContainerEvents();
		for (DummyContainerEvent event : events) {
			Assert.assertNotEquals("Found an error.", ContainerEvent.ERROR, event.type);
		}
	}

	class RecurseResolverHook implements ResolverHook {
		volatile ModuleContainer container;
		volatile Module dynamicImport;
		final AtomicInteger id = new AtomicInteger();
		List<IllegalStateException> expectedErrors = Collections.synchronizedList(new ArrayList<IllegalStateException>());

		@Override
		public void filterResolvable(Collection<BundleRevision> candidates) {
			ModuleContainer current = container;
			if (current != null) {
				int nextId = id.incrementAndGet();
				if (nextId >= 2) {
					// Don't do this again
					return;
				}
				Map<String, String> manifest = new HashMap<>();
				manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
				manifest.put(Constants.BUNDLE_SYMBOLICNAME, "module.recurse." + nextId);
				try {
					Module m = installDummyModule(manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), current);
					ResolutionReport report = current.resolve(Collections.singleton(m), false);
					report.getResolutionException();
				} catch (IllegalStateException e) {
					expectedErrors.add(e);
				} catch (BundleException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Module curDynamicImport = dynamicImport;
				if (curDynamicImport != null) {
					try {
						current.resolveDynamic("org.osgi.framework", curDynamicImport.getCurrentRevision());
					} catch (IllegalStateException e) {
						expectedErrors.add(e);
					}
				}
			}
		}

		@Override
		public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
			// nothing
		}

		@Override
		public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
			// nothing
		}

		@Override
		public void end() {
			// nothing
		}

		List<IllegalStateException> getExpectedErrors() {
			return new ArrayList<>(expectedErrors);
		}
	}

	@Test
	public void testRecurseResolutionPermits() throws BundleException, IOException {
		RecurseResolverHook resolverHook = new RecurseResolverHook();
		DummyContainerAdaptor adaptor = createDummyAdaptor(resolverHook);
		final ModuleContainer container = adaptor.getContainer();

		// install the system.bundle
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, null, container);
		ResolutionReport report = container.resolve(Arrays.asList(systemBundle), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());
		systemBundle.start();

		// install a bundle to do dynamic resolution from
		Map<String, String> manifest = new HashMap<>();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "dynamicImport");
		manifest.put(Constants.DYNAMICIMPORT_PACKAGE, "*");
		final Module dynamicImport = installDummyModule(manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), container);
		dynamicImport.start();
		resolverHook.dynamicImport = dynamicImport;
		resolverHook.container = container;

		final AtomicReference<ModuleWire> dynamicWire = new AtomicReference<>();
		Runnable runForEvents = () -> dynamicWire.set(container.resolveDynamic("org.osgi.framework", dynamicImport.getCurrentRevision()));
		adaptor.setRunForEvents(runForEvents);
		// install a bundle to resolve
		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "initial");
		Module m = installDummyModule(manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), container);
		m.start();

		assertNotNull("No Dynamic Wire", dynamicWire.get());
		assertEquals("Wrong number of exected errors.", 2, resolverHook.getExpectedErrors().size());
	}

	@Test
	public void testSystemBundleFragmentsPackageImport() throws BundleException, IOException {
		// install the system.bundle
		Module systemBundle = createContainerWithSystemBundle(true);
		ModuleContainer container = systemBundle.getContainer();

		// install an system.bundle fragment that imports framework package
		Map<String, String> systemFragManifest = new HashMap<>();
		systemFragManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		systemFragManifest.put(Constants.BUNDLE_SYMBOLICNAME, "systemFrag");
		systemFragManifest.put(Constants.FRAGMENT_HOST, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		systemFragManifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework");

		Module systemFrag = installDummyModule(systemFragManifest, "systemFrag", container);

		ResolutionReport report = container.resolve(Arrays.asList(systemFrag), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		List<ModuleWire> hostWires = systemBundle.getCurrentRevision().getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
		assertEquals("Wrong number of fragments.", 1, hostWires.size());
		Assert.assertEquals("Unexpected fragment revision: " + hostWires, systemFrag.getCurrentRevision(), hostWires.get(0).getRequirer());

		List<ModuleWire> systemBundleRequiredWires = systemBundle.getCurrentRevision().getWiring().getRequiredModuleWires(null);
		assertEquals("No required wires expected.", 0, systemBundleRequiredWires.size());
	}

	@Test
	public void testSystemBundleFragmentsNonPayloadRequirements() throws BundleException, IOException {
		// install the system.bundle
		Module systemBundle = createContainerWithSystemBundle(true);
		ModuleContainer container = systemBundle.getContainer();

		// install an system.bundle fragment that imports framework package
		Map<String, String> systemFragManifest = new HashMap<>();
		systemFragManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		systemFragManifest.put(Constants.BUNDLE_SYMBOLICNAME, "systemFrag");
		systemFragManifest.put(Constants.FRAGMENT_HOST, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		systemFragManifest.put(Constants.REQUIRE_CAPABILITY, "osgi.ee; filter:=\"(osgi.ee=JavaSE)\"");

		Module systemFrag = installDummyModule(systemFragManifest, "systemFrag", container);

		ResolutionReport report = container.resolve(Arrays.asList(systemFrag), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		List<ModuleWire> hostWires = systemBundle.getCurrentRevision().getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
		assertEquals("Wrong number of fragments.", 1, hostWires.size());
		Assert.assertEquals("Unexpected fragment revision: " + hostWires, systemFrag.getCurrentRevision(), hostWires.get(0).getRequirer());

		List<ModuleWire> systemBundleRequiredWires = systemBundle.getCurrentRevision().getWiring().getRequiredModuleWires(null);
		assertEquals("No required wires expected.", 0, systemBundleRequiredWires.size());

		List<ModuleWire> fragRequiredWires = systemFrag.getCurrentRevision().getWiring().getRequiredModuleWires(null);
		assertEquals("Wrong number of required wires.", 2, fragRequiredWires.size());
		assertWires(fragRequiredWires, systemBundle.getCurrentRevision().getWiring().getProvidedModuleWires(null));
	}

	@Test
	public void testSystemBundleFragmentsWithPayloadRequirements() throws BundleException, IOException {
		// install the system.bundle
		Module systemBundle = createContainerWithSystemBundle(true);
		ModuleContainer container = systemBundle.getContainer();

		// install an system.bundle fragment that requires a payload requirement from system.bundle
		Map<String, String> systemFragManifest = new HashMap<>();
		systemFragManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		systemFragManifest.put(Constants.BUNDLE_SYMBOLICNAME, "systemFrag");
		systemFragManifest.put(Constants.FRAGMENT_HOST, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		systemFragManifest.put(Constants.REQUIRE_CAPABILITY, "equinox.test; filter:=\"(equinox.test=system)\"");

		Module systemFrag = installDummyModule(systemFragManifest, "systemFrag", container);

		ResolutionReport report = container.resolve(Arrays.asList(systemFrag), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		List<ModuleWire> hostWires = systemBundle.getCurrentRevision().getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
		assertEquals("Wrong number of fragments.", 1, hostWires.size());
		Assert.assertEquals("Unexpected fragment revision: " + hostWires, systemFrag.getCurrentRevision(), hostWires.get(0).getRequirer());

		List<ModuleWire> systemBundleRequiredWires = systemBundle.getCurrentRevision().getWiring().getRequiredModuleWires(null);
		assertEquals("Wrong number of wires.", 1, systemBundleRequiredWires.size());
		assertEquals("Wrong requirer.", systemBundle.getCurrentRevision(), systemBundleRequiredWires.get(0).getRequirer());
		assertEquals("Wrong requirement.", systemFrag.getCurrentRevision(), systemBundleRequiredWires.get(0).getRequirement().getRevision());

		List<ModuleWire> fragRequiredWires = systemFrag.getCurrentRevision().getWiring().getRequiredModuleWires(null);
		assertEquals("Wrong number of required wires.", 1, fragRequiredWires.size());
		assertWires(fragRequiredWires, hostWires);
	}

	@Test
	public void testSystemBundleFragmentRequiresOtherFragment() throws BundleException, IOException {
		// install the system.bundle
		Module systemBundle = createContainerWithSystemBundle(true);
		ModuleContainer container = systemBundle.getContainer();

		// install an system.bundle fragment that provides a capability
		Map<String, String> systemFragManifest1 = new HashMap<>();
		systemFragManifest1.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		systemFragManifest1.put(Constants.BUNDLE_SYMBOLICNAME, "systemFrag1");
		systemFragManifest1.put(Constants.FRAGMENT_HOST, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		systemFragManifest1.put(Constants.PROVIDE_CAPABILITY, "fragment.capability; fragment.capability=test");
		Module systemFrag1 = installDummyModule(systemFragManifest1, "systemFrag1", container);

		// install an system.bundle fragment that requires a fragment capability
		Map<String, String> systemFragManifest2 = new HashMap<>();
		systemFragManifest2.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		systemFragManifest2.put(Constants.BUNDLE_SYMBOLICNAME, "systemFrag2");
		systemFragManifest2.put(Constants.FRAGMENT_HOST, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		systemFragManifest2.put(Constants.REQUIRE_CAPABILITY, "fragment.capability; filter:=\"(fragment.capability=test)\"");
		Module systemFrag2 = installDummyModule(systemFragManifest2, "systemFrag2", container);

		ResolutionReport report = container.resolve(Arrays.asList(systemFrag2), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		List<ModuleWire> hostWires = systemBundle.getCurrentRevision().getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
		assertEquals("Wrong number of fragments.", 2, hostWires.size());

		List<ModuleWire> systemBundleRequiredWires = systemBundle.getCurrentRevision().getWiring().getRequiredModuleWires(null);
		assertEquals("Wrong number of wires.", 1, systemBundleRequiredWires.size());
		assertEquals("Wrong requirer.", systemBundle.getCurrentRevision(), systemBundleRequiredWires.get(0).getRequirer());
		assertEquals("Wrong requirement.", systemFrag2.getCurrentRevision(), systemBundleRequiredWires.get(0).getRequirement().getRevision());
		assertEquals("Wrong provider.", systemBundle.getCurrentRevision(), systemBundleRequiredWires.get(0).getProvider());
		assertEquals("Wrong capability.", systemFrag1.getCurrentRevision(), systemBundleRequiredWires.get(0).getCapability().getRevision());

		List<ModuleWire> fragRequiredWires = systemFrag2.getCurrentRevision().getWiring().getRequiredModuleWires(null);
		assertEquals("Wrong number of required wires.", 1, fragRequiredWires.size());
		assertWires(fragRequiredWires, hostWires);
	}

	@Test
	public void testSystemBundleFragmentRequiresOtherFragmentFailResolution() throws BundleException, IOException {
		// install the system.bundle
		Module systemBundle = createContainerWithSystemBundle(true);
		ModuleContainer container = systemBundle.getContainer();

		// install an system.bundle fragment that provides a capability
		Map<String, String> systemFragManifest1 = new HashMap<>();
		systemFragManifest1.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		systemFragManifest1.put(Constants.BUNDLE_SYMBOLICNAME, "systemFrag1");
		systemFragManifest1.put(Constants.FRAGMENT_HOST, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		systemFragManifest1.put(Constants.PROVIDE_CAPABILITY, "fragment.capability; fragment.capability=test1");
		Module systemFrag1 = installDummyModule(systemFragManifest1, "systemFrag1", container);

		// install an system.bundle fragment that requires a fragment capability, but fails to match
		Map<String, String> systemFragManifest2 = new HashMap<>();
		systemFragManifest2.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		systemFragManifest2.put(Constants.BUNDLE_SYMBOLICNAME, "systemFrag2");
		systemFragManifest2.put(Constants.FRAGMENT_HOST, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		systemFragManifest2.put(Constants.REQUIRE_CAPABILITY, "fragment.capability; filter:=\"(fragment.capability=test4)\"");
		systemFragManifest2.put(Constants.PROVIDE_CAPABILITY, "fragment.capability; fragment.capability=test2");
		Module systemFrag2 = installDummyModule(systemFragManifest2, "systemFrag2", container);

		// install an system.bundle fragment that requires a fragment capability from a fragment that fails to resolve
		Map<String, String> systemFragManifest3 = new HashMap<>();
		systemFragManifest3.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		systemFragManifest3.put(Constants.BUNDLE_SYMBOLICNAME, "systemFrag3");
		systemFragManifest3.put(Constants.FRAGMENT_HOST, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		systemFragManifest3.put(Constants.REQUIRE_CAPABILITY, "fragment.capability; filter:=\"(fragment.capability=test2)\"");
		systemFragManifest3.put(Constants.PROVIDE_CAPABILITY, "fragment.capability; fragment.capability=test3");
		Module systemFrag3 = installDummyModule(systemFragManifest3, "systemFrag3", container);

		ResolutionReport report = container.resolve(Arrays.asList(systemFrag3), true);
		Assert.assertNotNull("Expected failure message", report.getResolutionException());

		List<ModuleWire> hostWires = systemBundle.getCurrentRevision().getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
		assertEquals("Wrong number of fragments.", 1, hostWires.size());
		List<ModuleWire> systemFrag1HostWires = systemFrag1.getCurrentRevision().getWiring().getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
		assertWires(systemFrag1HostWires, hostWires);

		// install a bundle that can satisfy the failed requirement, but it should not be allowed since it is not a fragment
		Map<String, String> provideCapabilityManifest1 = new HashMap<>();
		provideCapabilityManifest1.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		provideCapabilityManifest1.put(Constants.BUNDLE_SYMBOLICNAME, "provideCapabilityBundle1");
		provideCapabilityManifest1.put(Constants.PROVIDE_CAPABILITY, "fragment.capability; fragment.capability=test4");
		installDummyModule(provideCapabilityManifest1, "provideCapabilityBundle1", container);

		hostWires = systemBundle.getCurrentRevision().getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
		assertEquals("Wrong number of fragments.", 1, hostWires.size());
		systemFrag1HostWires = systemFrag1.getCurrentRevision().getWiring().getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
		assertWires(systemFrag1HostWires, hostWires);

		// install a fragment that satisfies the failed requirement
		Map<String, String> systemFragManifest4 = new HashMap<>();
		systemFragManifest4.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		systemFragManifest4.put(Constants.BUNDLE_SYMBOLICNAME, "systemFrag4");
		systemFragManifest4.put(Constants.FRAGMENT_HOST, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		systemFragManifest4.put(Constants.PROVIDE_CAPABILITY, "fragment.capability; fragment.capability=test4");
		Module systemFrag4 = installDummyModule(systemFragManifest4, "systemFrag4", container);

		report = container.resolve(Arrays.asList(systemFrag3), true);
		Assert.assertNull("Failed to resolve.", report.getResolutionException());

		hostWires = systemBundle.getCurrentRevision().getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
		assertEquals("Wrong number of fragments.", 4, hostWires.size());
		systemFrag1HostWires = systemFrag1.getCurrentRevision().getWiring().getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
		List<ModuleWire> systemFrag2HostWires = systemFrag2.getCurrentRevision().getWiring().getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
		List<ModuleWire> systemFrag3HostWires = systemFrag3.getCurrentRevision().getWiring().getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
		List<ModuleWire> systemFrag4HostWires = systemFrag4.getCurrentRevision().getWiring().getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
		assertWires(systemFrag1HostWires, hostWires);
		assertWires(systemFrag2HostWires, hostWires);
		assertWires(systemFrag3HostWires, hostWires);
		assertWires(systemFrag4HostWires, hostWires);

		List<ModuleCapability> fragmentCapabilities = systemBundle.getCurrentRevision().getWiring().getModuleCapabilities("fragment.capability");
		assertEquals("Wrong number of fragment capabilities.", 4, fragmentCapabilities.size());
		// Use set since the order of required and provided wires will be different
		Set<ModuleWire> hostRequiredFragmentCapWires = new HashSet<>(systemBundle.getCurrentRevision().getWiring().getRequiredModuleWires("fragment.capability"));
		Set<ModuleWire> hostProvidedFragmentCapWires = new HashSet<>(systemBundle.getCurrentRevision().getWiring().getProvidedModuleWires("fragment.capability"));
		assertEquals("Wrong number of wires.", 2, hostProvidedFragmentCapWires.size());
		assertEquals("Wrong wires found from host.", hostRequiredFragmentCapWires, hostProvidedFragmentCapWires);
	}

	@Test
	public void testMultipleSystemBundleFragmentsWithSameName() throws BundleException, IOException {
		// install the system.bundle
		Module systemBundle = createContainerWithSystemBundle(true);
		ModuleContainer container = systemBundle.getContainer();

		// install multiple versions of the same fragment
		Map<String, String> systemFragManifest1 = new HashMap<>();
		systemFragManifest1.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		systemFragManifest1.put(Constants.BUNDLE_SYMBOLICNAME, "systemFrag");
		systemFragManifest1.put(Constants.BUNDLE_VERSION, "1.0");
		systemFragManifest1.put(Constants.FRAGMENT_HOST, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		Module systemFrag1 = installDummyModule(systemFragManifest1, "systemFrag1", container);

		// first attempt to resolve the lowest version before installing the others
		ResolutionReport report = container.resolve(Arrays.asList(systemFrag1), true);
		Assert.assertNull("Unexpected failure message", report.getResolutionException());

		List<ModuleWire> hostWires = systemBundle.getCurrentRevision().getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
		assertEquals("Wrong number of fragments.", 1, hostWires.size());
		List<ModuleWire> systemFrag1HostWires = systemFrag1.getCurrentRevision().getWiring().getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
		assertWires(systemFrag1HostWires, hostWires);

		Map<String, String> systemFragManifest2 = new HashMap<>();
		systemFragManifest2.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		systemFragManifest2.put(Constants.BUNDLE_SYMBOLICNAME, "systemFrag");
		systemFragManifest2.put(Constants.BUNDLE_VERSION, "2.0");
		systemFragManifest2.put(Constants.FRAGMENT_HOST, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		Module systemFrag2 = installDummyModule(systemFragManifest2, "systemFrag2", container);

		Map<String, String> systemFragManifest3 = new HashMap<>();
		systemFragManifest3.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		systemFragManifest3.put(Constants.BUNDLE_SYMBOLICNAME, "systemFrag");
		systemFragManifest3.put(Constants.BUNDLE_VERSION, "3.0");
		systemFragManifest3.put(Constants.FRAGMENT_HOST, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		Module systemFrag3 = installDummyModule(systemFragManifest3, "systemFrag3", container);

		report = container.resolve(Arrays.asList(systemFrag2), true);
		Assert.assertNotNull("Expected failure message", report.getResolutionException());
		report = container.resolve(Arrays.asList(systemFrag3), true);
		Assert.assertNotNull("Expected failure message", report.getResolutionException());

		hostWires = systemBundle.getCurrentRevision().getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
		assertEquals("Wrong number of fragments.", 1, hostWires.size());
		systemFrag1HostWires = systemFrag1.getCurrentRevision().getWiring().getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
		assertWires(systemFrag1HostWires, hostWires);

		// uninstall the fragments so we can start over
		container.uninstall(systemFrag1);
		container.uninstall(systemFrag2);
		container.uninstall(systemFrag3);

		// refresh the system bundle to get only it resolved
		report = container.refresh(Collections.singleton(systemBundle));
		Assert.assertNull("Unexpected failure message", report.getResolutionException());
		hostWires = systemBundle.getCurrentRevision().getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
		assertEquals("Wrong number of fragments.", 0, hostWires.size());

		// install the fragments again
		systemFrag1 = installDummyModule(systemFragManifest1, "systemFrag1", container);
		systemFrag2 = installDummyModule(systemFragManifest2, "systemFrag2", container);
		systemFrag3 = installDummyModule(systemFragManifest3, "systemFrag3", container);

		report = container.resolve(Arrays.asList(systemFrag1), true);
		Assert.assertNotNull("Expected failure message", report.getResolutionException());
		report = container.resolve(Arrays.asList(systemFrag2), true);
		Assert.assertNotNull("Expected failure message", report.getResolutionException());
		report = container.resolve(Arrays.asList(systemFrag3), true);
		Assert.assertNull("Unexpected failure message", report.getResolutionException());

		hostWires = systemBundle.getCurrentRevision().getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
		List<ModuleWire> systemFrag3HostWires = systemFrag3.getCurrentRevision().getWiring().getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
		assertWires(systemFrag3HostWires, hostWires);
	}

	@Test
	public void testSystemBundleFragmentsWithNonEffectiveCapsReqs() throws BundleException, IOException {
		// install the system.bundle
		Module systemBundle = createContainerWithSystemBundle(true);
		ModuleContainer container = systemBundle.getContainer();

		ModuleWiring systemWiring = systemBundle.getCurrentRevision().getWiring();

		// install an system.bundle fragment with activator
		Map<String, String> systemFragManifest = new HashMap<>();
		systemFragManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		systemFragManifest.put(Constants.BUNDLE_SYMBOLICNAME, "systemFrag");
		systemFragManifest.put(Constants.FRAGMENT_HOST, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		systemFragManifest.put(Constants.EXTENSION_BUNDLE_ACTIVATOR, "systemFrag.Activator");
		systemFragManifest.put(Constants.REQUIRE_CAPABILITY,
				"does.not.exist; effective:=never; filter:=\"(never=true)\"");
		systemFragManifest.put(Constants.PROVIDE_CAPABILITY,
				"non.effective.cap; non.effective.cap=test; effective:=never");

		Module systemFrag = installDummyModule(systemFragManifest, "systemFrag", container);

		ResolutionReport report = container.resolve(Arrays.asList(systemFrag), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		List<ModuleWire> hostWires = systemWiring.getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
		assertEquals("Wrong number of fragments.", 1, hostWires.size());
		Assert.assertEquals("Unexpected fragment revision: " + hostWires, systemFrag.getCurrentRevision(),
				hostWires.get(0).getRequirer());

		List<ModuleCapability> dataCaps = systemWiring
				.getModuleCapabilities(EquinoxModuleDataNamespace.MODULE_DATA_NAMESPACE);
		assertTrue("Unexpected module data capabilities: " + dataCaps, dataCaps.isEmpty());

		List<ModuleCapability> nonEffectiveCaps = systemBundle.getCurrentRevision().getWiring()
				.getModuleCapabilities("non.effective.cap");
		assertTrue("Unexpected non-effective capabilities: " + nonEffectiveCaps, nonEffectiveCaps.isEmpty());

		List<ModuleRequirement> nonEffectiveReqs = systemWiring.getModuleRequirements("does.not.exist");
		assertTrue("Unexpected non-effective requirements: " + nonEffectiveReqs, nonEffectiveReqs.isEmpty());

		Map<String, String> failResolutionManifest = new HashMap<>();
		failResolutionManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		failResolutionManifest.put(Constants.BUNDLE_SYMBOLICNAME, "failResolution");
		failResolutionManifest.put(Constants.FRAGMENT_HOST, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		failResolutionManifest.put(Constants.REQUIRE_CAPABILITY,
				"non.effective.cap; filter:=\"(non.effective.cap=test)\"");
		Module failResolution = installDummyModule(failResolutionManifest, "failResolution", container);
		report = container.resolve(Arrays.asList(failResolution), false);
		String resolutionMsg = report.getResolutionReportMessage(failResolution.getCurrentRevision());
		assertTrue("Wrong resolution message:" + resolutionMsg, resolutionMsg.contains("non.effective.cap"));
	}

	private Module createContainerWithSystemBundle(boolean resolveSystemBundle) throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install the system.bundle
		String systemCapability = "osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6\", equinox.test; equinox.test=system, osgi.native; osgi.native.osname=test";
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, systemCapability, container);
		if (resolveSystemBundle) {
			ResolutionReport report = container.resolve(Collections.singleton(systemBundle), true);
			Assert.assertNull("Found resolution exception.", report.getResolutionException());
			Assert.assertEquals("System is not resolved.", State.RESOLVED, systemBundle.getState());
		}

		return systemBundle;
	}

	@Test
	public void testSplitPackageUses01() throws BundleException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install a split exporter core that substitutes
		Map<String, String> coreManifest = new HashMap<>();
		coreManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		coreManifest.put(Constants.BUNDLE_SYMBOLICNAME, "core");
		coreManifest.put(Constants.EXPORT_PACKAGE, "pkg1; core=split; mandatory:=core");
		coreManifest.put(Constants.IMPORT_PACKAGE, "pkg1; core=split");

		// install a split exporter misc that requires core and substitutes
		Map<String, String> miscManifest = new HashMap<>();
		miscManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		miscManifest.put(Constants.BUNDLE_SYMBOLICNAME, "misc");
		miscManifest.put(Constants.EXPORT_PACKAGE, "pkg1; misc=split; mandatory:=misc");
		miscManifest.put(Constants.REQUIRE_BUNDLE, "core");

		// install a bundle that imports core and exports pkg2 that uses pkg1 from core
		Map<String, String> importsCoreManifest = new HashMap<>();
		importsCoreManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		importsCoreManifest.put(Constants.BUNDLE_SYMBOLICNAME, "importsCore");
		importsCoreManifest.put(Constants.EXPORT_PACKAGE, "pkg2; uses:=pkg1");
		importsCoreManifest.put(Constants.IMPORT_PACKAGE, "pkg1; core=split");

		// install a bundle that imports pkg2, but requires misc
		Map<String, String> requiresMiscManifest = new HashMap<>();
		requiresMiscManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		requiresMiscManifest.put(Constants.BUNDLE_SYMBOLICNAME, "requiresMisc");
		requiresMiscManifest.put(Constants.IMPORT_PACKAGE, "pkg2");
		requiresMiscManifest.put(Constants.REQUIRE_BUNDLE, "misc");

		installDummyModule(coreManifest, "core", container);
		installDummyModule(miscManifest, "misc", container);
		installDummyModule(importsCoreManifest, "importsCore", container);
		Module requireMisc = installDummyModule(requiresMiscManifest, "requireMisc", container);

		ResolutionReport report = container.resolve(Arrays.asList(requireMisc), true);
		Assert.assertNull("Failed to resolve test.", report.getResolutionException());

		// now test by resolving the split exporters first
		adaptor = createDummyAdaptor();
		container = adaptor.getContainer();

		installDummyModule(coreManifest, "core", container);
		Module misc = installDummyModule(miscManifest, "misc", container);
		report = container.resolve(Arrays.asList(misc), true);
		Assert.assertNull("Failed to resolve test.", report.getResolutionException());

		installDummyModule(importsCoreManifest, "importsCore", container);
		requireMisc = installDummyModule(requiresMiscManifest, "requireMisc", container);
		report = container.resolve(Arrays.asList(requireMisc), true);
		Assert.assertNull("Failed to resolve test.", report.getResolutionException());

		// now test by resolving the split exporters first with a real substitution
		adaptor = createDummyAdaptor();
		container = adaptor.getContainer();

		// install a exporter that substitutes core's export
		Map<String, String> substitutesCoreManifest = new HashMap<>();
		substitutesCoreManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		substitutesCoreManifest.put(Constants.BUNDLE_SYMBOLICNAME, "substitutesCore");
		substitutesCoreManifest.put(Constants.EXPORT_PACKAGE, "pkg1; substitutesCore=true; mandatory:=substitutesCore");

		// change core's import to force it to the substitute
		coreManifest.put(Constants.IMPORT_PACKAGE, "pkg1; substitutesCore=true");
		importsCoreManifest.put(Constants.IMPORT_PACKAGE, "pkg1; substitutesCore=true");

		installDummyModule(substitutesCoreManifest, "substitutesCore", container);
		installDummyModule(coreManifest, "core", container);
		misc = installDummyModule(miscManifest, "misc", container);
		report = container.resolve(Arrays.asList(misc), true);
		Assert.assertNull("Failed to resolve test.", report.getResolutionException());

		installDummyModule(importsCoreManifest, "importsCore", container);
		requireMisc = installDummyModule(requiresMiscManifest, "requireMisc", container);
		report = container.resolve(Arrays.asList(requireMisc), true);
		Assert.assertNull("Failed to resolve test.", report.getResolutionException());

		// not test by doing a full resolve with real substitution
		adaptor = createDummyAdaptor();
		container = adaptor.getContainer();

		installDummyModule(substitutesCoreManifest, "substitutesCore", container);
		installDummyModule(coreManifest, "core", container);
		installDummyModule(miscManifest, "misc", container);
		installDummyModule(importsCoreManifest, "importsCore", container);
		requireMisc = installDummyModule(requiresMiscManifest, "requireMisc", container);

		report = container.resolve(Arrays.asList(requireMisc), true);
		Assert.assertNull("Failed to resolve test.", report.getResolutionException());
	}

	List<String> HTTPCOMPS_AND_EATHER = Arrays.asList( //
			"org.apache.commons.codec_1.9.0.v20170208-1614.MF", //
			"org.apache.commons.logging_1.1.1.v201101211721.MF", //
			"org.apache.httpcomponents.httpclient_4.3.6.v201511171540.MF", //
			"org.apache.httpcomponents.httpclient_4.5.2.v20170208-1614.MF", //
			"org.apache.httpcomponents.httpclient_4.5.2.v20170210-0925.MF", //
			"org.apache.httpcomponents.httpcore_4.3.3.v201411290715.MF", //
			"org.apache.httpcomponents.httpcore_4.4.4.v20161115-1643.MF", //
			"org.apache.httpcomponents.httpcore_4.4.6.v20170210-0925.MF", //
			"org.eclipse.aether.api_1.0.1.v20141111.MF", //
			"org.eclipse.aether.spi_1.0.1.v20141111.MF", //
			"org.eclipse.aether.transport.http_1.0.1.v20141111.MF", //
			"org.eclipse.aether.util_1.0.1.v20141111.MF");

	@Test
	public void testSubstitutionWithMoreThan2Providers() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule( //
				"system.bundle.MF", //
				Constants.SYSTEM_BUNDLE_LOCATION, //
				Constants.SYSTEM_BUNDLE_SYMBOLICNAME, //
				"javax.crypto, javax.crypto.spec, javax.net, javax.net.ssl, javax.security.auth.x500, org.ietf.jgss", //
				"osgi.ee; osgi.ee=JavaSE; version:List<Version>=\"1.3, 1.4, 1.5, 1.6, 1.7\"", //
				container);
		ResolutionReport report = container.resolve(Arrays.asList(systemBundle), true);
		Assert.assertNull("Failed to resolve test.", report.getResolutionException());

		List<Module> modules = new ArrayList<>();
		for (String manifest : HTTPCOMPS_AND_EATHER) {
			modules.add(installDummyModule(manifest, manifest, container));
		}
		report = container.resolve(modules, true);
		Assert.assertNull("Failed to resolve test.", report.getResolutionException());
	}

	@Test
	public void testModuleIDSetting() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install the system.bundle
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, null, container);
		ResolutionReport report = container.resolve(Arrays.asList(systemBundle), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		Map<String, String> manifest = new HashMap<>();

		// test by installing bundles with decreasing IDs
		List<Module> modules = new ArrayList<>();
		for (int i = 5; i > 0; i--) {
			manifest.clear();
			manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
			manifest.put(Constants.BUNDLE_SYMBOLICNAME, String.valueOf(i));
			modules.add(installDummyModule(manifest, i, manifest.get(Constants.BUNDLE_SYMBOLICNAME), container));
		}

		// test that the modules have decreasing ID starting at 5
		long id = 5;
		for (Module module : modules) {
			Assert.assertEquals("Wrong ID found.", id--, module.getId().longValue());
		}

		// test that error occurs when trying to use an existing ID
		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, String.valueOf("test.dup.id"));
		try {
			installDummyModule(manifest, 5, manifest.get(Constants.BUNDLE_SYMBOLICNAME), container);
			fail("Expected to fail installation with duplicate ID.");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	@Test
	public void testAliasBundleNameReport() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);
		container.resolve(Collections.singleton(systemBundle), true);

		Map<String, String> b1Manifest = new HashMap<>();
		b1Manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b1");
		b1Manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		b1Manifest.put(Constants.IMPORT_PACKAGE, "doesnotexist");
		ModuleRevisionBuilder b1Builder = OSGiManifestBuilderFactory.createBuilder(b1Manifest, "alias.name", "", "");
		container.install(systemBundle, "b1", b1Builder, null);

		Map<String, String> b2Manifest = new HashMap<>();
		b2Manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b2");
		b2Manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		b2Manifest.put(Constants.REQUIRE_BUNDLE, "b1");
		ModuleRevisionBuilder b2Builder = OSGiManifestBuilderFactory.createBuilder(b2Manifest);
		Module b2 = container.install(systemBundle, "b2", b2Builder, null);

		ResolutionReport report = container.resolve(Collections.singleton(b2), true);
		String message = report.getResolutionReportMessage(b2.getCurrentRevision());
		assertTrue("Wrong error message: " + message, message.contains("b1") && message.contains("alias.name"));
	}

	@Test
	public void testStartDeadLock() throws BundleException, InterruptedException, IOException {
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch stopLatch = new CountDownLatch(1);

		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(false), Collections.singletonMap(EquinoxConfiguration.PROP_MODULE_LOCK_TIMEOUT, "1"));
		adaptor.setStartLatch(startLatch);
		adaptor.setStopLatch(stopLatch);

		ModuleContainer container = adaptor.getContainer();

		// install the system.bundle
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, null, container);
		ResolutionReport report = container.resolve(Arrays.asList(systemBundle), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());
		systemBundle.start();

		// install a module
		Map<String, String> manifest = new HashMap<>();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "lock.test");
		final Module module = installDummyModule(manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), container);

		final ArrayBlockingQueue<BundleException> startExceptions = new ArrayBlockingQueue<>(2);
		Runnable start = () -> {
			try {
				module.start();
			} catch (BundleException e) {
				startExceptions.offer(e);
			}
		};
		Thread t1 = new Thread(start);
		Thread t2 = new Thread(start);
		t1.start();
		t2.start();

		BundleException startError = startExceptions.poll(10, TimeUnit.SECONDS);
		startLatch.countDown();

		Assert.assertEquals("Wrong cause.", TimeoutException.class, startError.getCause().getClass());
		Assert.assertEquals("Wrong cause.", ThreadInfoReport.class, startError.getCause().getCause().getClass());
		startError.printStackTrace();

		final ArrayBlockingQueue<BundleException> stopExceptions = new ArrayBlockingQueue<>(2);
		Runnable stop = () -> {
			try {
				module.stop();
			} catch (BundleException e) {
				stopExceptions.offer(e);
			}
		};
		Thread tStop1 = new Thread(stop);
		Thread tStop2 = new Thread(stop);
		tStop1.start();
		tStop2.start();

		BundleException stopError = stopExceptions.poll(10, TimeUnit.SECONDS);
		stopLatch.countDown();

		Assert.assertEquals("Wrong cause.", TimeoutException.class, stopError.getCause().getClass());
		Assert.assertEquals("Wrong cause.", ThreadInfoReport.class, stopError.getCause().getCause().getClass());
		stopError.printStackTrace();
	}

	@Test
	public void testUsesWithRequireReexport() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install the system.bundle
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, null, container);
		ResolutionReport report = container.resolve(Arrays.asList(systemBundle), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		// install and resolve used.pkg exporter to force substitution
		Map<String, String> usedPkgExportManifest = new HashMap<>();
		usedPkgExportManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		usedPkgExportManifest.put(Constants.BUNDLE_SYMBOLICNAME, "used.pkg");
		usedPkgExportManifest.put(Constants.EXPORT_PACKAGE, "used.pkg");
		Module moduleUsedPkg = installDummyModule(usedPkgExportManifest, "usedPkg", container);
		report = container.resolve(Arrays.asList(moduleUsedPkg), true);
		Assert.assertNull("Failed to resolve usedPkg.", report.getResolutionException());

		// install part 1 (ui.workbench)
		Map<String, String> split1Manifest = new HashMap<>();
		split1Manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		split1Manifest.put(Constants.BUNDLE_SYMBOLICNAME, "split1");
		split1Manifest.put(Constants.EXPORT_PACKAGE, "split.pkg; uses:=used.pkg, used.pkg");
		split1Manifest.put(Constants.IMPORT_PACKAGE, "used.pkg");
		Module moduleSplit1 = installDummyModule(split1Manifest, "split1", container);

		// install part 2 (e4.ui.ide)
		Map<String, String> split2Manifest = new HashMap<>();
		split2Manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		split2Manifest.put(Constants.BUNDLE_SYMBOLICNAME, "split2");
		split2Manifest.put(Constants.EXPORT_PACKAGE, "split.pkg");
		Module moduleSplit2 = installDummyModule(split2Manifest, "split2", container);

		// install part 3 which requires part 1 and 2, reexports 1 and 2 (ui.ide)
		Map<String, String> split3Manifest = new HashMap<>();
		split3Manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		split3Manifest.put(Constants.BUNDLE_SYMBOLICNAME, "split3");
		split3Manifest.put(Constants.EXPORT_PACKAGE, "split.pkg");
		// the reexport here are not necessary; but cause issues for the resolver
		split3Manifest.put(Constants.REQUIRE_BUNDLE, "split1; visibility:=reexport, split2; visibility:=reexport");
		Module moduleSplit3 = installDummyModule(split3Manifest, "split3", container);

		// install reexporter of part1 (ui)
		Map<String, String> reexporterPart1Manifest = new HashMap<>();
		reexporterPart1Manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		reexporterPart1Manifest.put(Constants.BUNDLE_SYMBOLICNAME, "reexport1");
		reexporterPart1Manifest.put(Constants.REQUIRE_BUNDLE, "split1; visibility:=reexport");
		Module moduleReexport1 = installDummyModule(reexporterPart1Manifest, "reexport1", container);

		// install reexporter of split3
		Map<String, String> reexporterSplit3Manifest = new HashMap<>();
		reexporterSplit3Manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		reexporterSplit3Manifest.put(Constants.BUNDLE_SYMBOLICNAME, "reexportSplit3");
		reexporterSplit3Manifest.put(Constants.REQUIRE_BUNDLE, "split3; visibility:=reexport");
		Module moduleReexportSplit3 = installDummyModule(reexporterSplit3Manifest, "reexportSplit3", container);

		// install test export that requires reexportSplit3 (should get access to all 3 parts)
		Map<String, String> testExporterUses = new HashMap<>();
		testExporterUses.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		testExporterUses.put(Constants.BUNDLE_SYMBOLICNAME, "test.exporter");
		testExporterUses.put(Constants.REQUIRE_BUNDLE, "reexportSplit3");
		testExporterUses.put(Constants.EXPORT_PACKAGE, "export.pkg; uses:=split.pkg");
		Module testExporter = installDummyModule(testExporterUses, "test.exporter", container);

		// install test requirer that requires the exporter and reexport1 (should get access to only part 1)
		// part 1 is a subset of what the exporter has access to so it should resolve
		Map<String, String> testRequireUses = new HashMap<>();
		testRequireUses.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		testRequireUses.put(Constants.BUNDLE_SYMBOLICNAME, "test.requirer");
		testRequireUses.put(Constants.REQUIRE_BUNDLE, "test.exporter, reexport1");
		Module testRequirer = installDummyModule(testRequireUses, "test.requirer", container);

		report = container.resolve(Arrays.asList(moduleSplit1, moduleSplit2, moduleSplit3, moduleReexport1, moduleReexportSplit3, testExporter, testRequirer), true);
		Assert.assertNull("Failed to resolve", report.getResolutionException());
	}

	@Test
	public void testCycleBug570984() throws BundleException, IOException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		// install the system.bundle
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION,
				Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, null, container);
		ResolutionReport report = container.resolve(Arrays.asList(systemBundle), true);
		Assert.assertNull("Failed to resolve system.bundle.", report.getResolutionException());

		Map<String, String> manifestA = new HashMap<>();
		manifestA.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifestA.put(Constants.BUNDLE_SYMBOLICNAME, "a");
		manifestA.put(Constants.BUNDLE_VERSION, "1");
		manifestA.put(Constants.REQUIRE_BUNDLE, "b");
		manifestA.put(Constants.PROVIDE_CAPABILITY, "ca");
		manifestA.put(Constants.REQUIRE_CAPABILITY, "cb");
		Module moduleA = installDummyModule(manifestA, "a", container);

		Map<String, String> manifestB = new HashMap<>();
		manifestB.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifestB.put(Constants.BUNDLE_SYMBOLICNAME, "b");
		manifestB.put(Constants.BUNDLE_VERSION, "1");
		manifestB.put(Constants.REQUIRE_BUNDLE, "a");
		manifestB.put(Constants.PROVIDE_CAPABILITY, "cb");
		manifestB.put(Constants.REQUIRE_CAPABILITY, "ca");
		Module moduleB = installDummyModule(manifestB, "b", container);

		Map<String, String> manifestBF = new HashMap<>();
		manifestBF.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifestBF.put(Constants.BUNDLE_SYMBOLICNAME, "bf");
		manifestBF.put(Constants.BUNDLE_VERSION, "1");
		manifestBF.put(Constants.FRAGMENT_HOST, "b");
		manifestBF.put(Constants.IMPORT_PACKAGE, "e");
		Module moduleBF = installDummyModule(manifestBF, "bf", container);

		report = container.resolve(Arrays.asList(moduleA, moduleB, moduleBF), false);
		Assert.assertNull("Failed to resolve", report.getResolutionException());
		assertEquals("Wrong state for moduleA", State.RESOLVED, moduleA.getState());
		assertEquals("Wrong state for moduleB", State.RESOLVED, moduleB.getState());
		assertEquals("Wrong state for moduleBF", State.INSTALLED, moduleBF.getState());
	}

	@Test
	public void testModuleWiringLookup() throws BundleException {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Map<String, String> manifestCore = new HashMap<>();
		manifestCore.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifestCore.put(Constants.BUNDLE_SYMBOLICNAME, "core");
		manifestCore.put(Constants.BUNDLE_VERSION, "1");
		manifestCore.put(Constants.PROVIDE_CAPABILITY, "core");
		manifestCore.put(Constants.EXPORT_PACKAGE, "core.a, core.b, core.dynamic.a, core.dynamic.b");
		installDummyModule(manifestCore, "core", container);

		Map<String, String> manifestA = new HashMap<>();
		manifestA.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifestA.put(Constants.BUNDLE_SYMBOLICNAME, "a");
		manifestA.put(Constants.BUNDLE_VERSION, "1");
		manifestA.put(Constants.PROVIDE_CAPABILITY, "ca; ca=1, ca; ca=2, cb; cb=1, cb; cb=2, ca; ca=3");
		manifestA.put(Constants.REQUIRE_CAPABILITY, "core");
		manifestA.put(Constants.IMPORT_PACKAGE, "core.a, core.b");
		manifestA.put(Constants.DYNAMICIMPORT_PACKAGE, "core.dynamic.*");
		Module moduleA = installDummyModule(manifestA, "a", container);

		Map<String, String> manifestB = new HashMap<>();
		manifestB.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifestB.put(Constants.BUNDLE_SYMBOLICNAME, "b");
		manifestB.put(Constants.BUNDLE_VERSION, "1");
		manifestB.put(Constants.REQUIRE_CAPABILITY,
				"ca; filter:=\"(ca=1)\", ca; filter:=\"(ca=2)\", cb; filter:=\"(cb=1)\", cb; filter:=\"(cb=2)\", ca; filter:=\"(ca=3)\"");
		Module moduleB = installDummyModule(manifestB, "b", container);

		container.resolve(Arrays.asList(moduleA, moduleB), false);

		ModuleWiring wiringA = moduleA.getCurrentRevision().getWiring();
		List<ModuleCapability> caCaps = wiringA.getModuleCapabilities("ca");
		assertEquals("Wrong number of capabilities", 3, caCaps.size());
		List<ModuleWire> caProvidedWires = wiringA.getProvidedModuleWires("ca");
		assertEquals("Wrong number of wires.", 3, caProvidedWires.size());

		ModuleWiring wiringB = moduleB.getCurrentRevision().getWiring();
		List<ModuleRequirement> caReqs = wiringB.getModuleRequirements("ca");
		assertEquals("Wrong number of requirements", 3, caReqs.size());
		List<ModuleWire> caRequiredWires = wiringB.getRequiredModuleWires("ca");
		assertEquals("Wrong number of wires.", 3, caRequiredWires.size());

		Map<String, String> manifestAFrag = new HashMap<>();
		manifestAFrag.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifestAFrag.put(Constants.BUNDLE_SYMBOLICNAME, "a.frag");
		manifestAFrag.put(Constants.BUNDLE_VERSION, "1");
		manifestAFrag.put(Constants.FRAGMENT_HOST, "a");
		manifestAFrag.put(Constants.PROVIDE_CAPABILITY, "ca; ca=4, ca; ca=5, cb; cb=3, cb; cb=4, ca; ca=6");
		installDummyModule(manifestAFrag, "a.frag", container);

		Map<String, String> manifestBFrag = new HashMap<>();
		manifestBFrag.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifestBFrag.put(Constants.BUNDLE_SYMBOLICNAME, "b.frag");
		manifestBFrag.put(Constants.BUNDLE_VERSION, "1");
		manifestBFrag.put(Constants.FRAGMENT_HOST, "b");
		manifestBFrag.put(Constants.REQUIRE_CAPABILITY,
				"ca; filter:=\"(ca=4)\", ca; filter:=\"(ca=5)\", cb; filter:=\"(cb=3)\", cb; filter:=\"(cb=4)\", ca; filter:=\"(ca=6)\"");
		installDummyModule(manifestBFrag, "b.frag", container);

		container.refresh(Arrays.asList(moduleA, moduleB));

		wiringA = moduleA.getCurrentRevision().getWiring();
		caCaps = wiringA.getModuleCapabilities("ca");
		assertEquals("Wrong number of capabilities", 6, caCaps.size());
		caProvidedWires = wiringA.getProvidedModuleWires("ca");
		assertEquals("Wrong number of wires.", 6, caProvidedWires.size());

		wiringB = moduleB.getCurrentRevision().getWiring();
		caReqs = wiringB.getModuleRequirements("ca");
		assertEquals("Wrong number of requirements", 6, caReqs.size());
		caRequiredWires = wiringB.getRequiredModuleWires("ca");
		assertEquals("Wrong number of wires.", 6, caRequiredWires.size());

		// dynamically resolve a fragment to already resolved host, providing more
		// capabilities and requirements
		Map<String, String> manifestA2Frag = new HashMap<>();
		manifestA2Frag.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifestA2Frag.put(Constants.BUNDLE_SYMBOLICNAME, "a.frag2");
		manifestA2Frag.put(Constants.BUNDLE_VERSION, "1");
		manifestA2Frag.put(Constants.FRAGMENT_HOST, "a");
		manifestA2Frag.put(Constants.PROVIDE_CAPABILITY, "ca; ca=7, ca; ca=8, cb; cb=5, cb; cb=6, ca; ca=9");
		manifestA2Frag.put(Constants.REQUIRE_CAPABILITY, "ca; filter:=\"(ca=6)\"");
		Module moduleAFrag2 = installDummyModule(manifestA2Frag, "a.frag1", container);

		container.resolve(Arrays.asList(moduleAFrag2), true);
		assertEquals("Wrong state for frag2", State.RESOLVED, moduleAFrag2.getState());
		caCaps = wiringA.getModuleCapabilities("ca");
		assertEquals("Wrong number of capabilities", 9, caCaps.size());
		caProvidedWires = wiringA.getProvidedModuleWires("ca");
		assertEquals("Wrong number of wires.", 7, caProvidedWires.size());
		caReqs = wiringA.getModuleRequirements("ca");
		assertEquals("Wrong number of requirements.", 1, caReqs.size());
		caRequiredWires = wiringA.getRequiredModuleWires("ca");
		assertEquals("Wrong number of wires.", 1, caRequiredWires.size());

		List<ModuleRequirement> pkgReqs = wiringA.getModuleRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of requirements.", 3, pkgReqs.size());
		List<ModuleWire> pkgWires = wiringA.getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wring number of wires.", 2, pkgWires.size());

		ModuleWire dynamicImport1 = container.resolveDynamic("core.dynamic.a", moduleA.getCurrentRevision());
		assertNotNull("Dynamic resolve failed", dynamicImport1);

		pkgReqs = wiringA.getModuleRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of requirements.", 3, pkgReqs.size());
		assertEquals("Wrong last package requirement.", PackageNamespace.RESOLUTION_DYNAMIC,
				pkgReqs.get(pkgReqs.size() - 1).getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE));
		pkgWires = wiringA.getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wring number of wires.", 3, pkgWires.size());
		assertEquals("Wrong last package wire.", dynamicImport1, pkgWires.get(pkgWires.size() - 1));

		ModuleWire dynamicImport2 = container.resolveDynamic("core.dynamic.b", moduleA.getCurrentRevision());
		assertNotNull("Dynamic resolve failed", dynamicImport2);
		pkgWires = wiringA.getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wring number of wires.", 4, pkgWires.size());
		assertEquals("Wrong last package wire.", dynamicImport2, pkgWires.get(pkgWires.size() - 1));
	}

	private static void assertWires(List<ModuleWire> required, List<ModuleWire>... provided) {
		for (ModuleWire requiredWire : required) {
			for (List<ModuleWire> providedList : provided) {
				if (providedList.contains(requiredWire)) {
					return;
				}
			}
			Assert.fail("Could not find required wire in expected provider wires: " + requiredWire);
		}
	}

	private void assertEvents(List<DummyModuleEvent> expected, List<DummyModuleEvent> actual, boolean orderMatters) {
		for (List<DummyModuleEvent> expectedCommon = removeFirstListOfCommonEvents(expected); !expectedCommon.isEmpty(); expectedCommon = removeFirstListOfCommonEvents(expected)) {
			List<DummyModuleEvent> actualCommon = removeFirstListOfCommonEvents(actual);
			if (expectedCommon.size() != actualCommon.size()) {
				Assert.assertEquals("Wrong number of events found in: " + actualCommon, expectedCommon.size(), actualCommon.size());
			}
			if (orderMatters) {
				Assert.assertEquals("Wrong events found.", expectedCommon, actualCommon);
			} else {
				for (DummyModuleEvent expectedEvent : expectedCommon) {
					Assert.assertTrue("Missing expected event: " + expectedEvent + " : from " + actualCommon, actualCommon.contains(expectedEvent));
				}
				for (DummyModuleEvent actualEvent : actualCommon) {
					Assert.assertTrue("Found unexpected event: " + actualEvent + " : from " + actualCommon, expectedCommon.contains(actualEvent));
				}
			}
		}
	}

	private List<DummyModuleEvent> removeFirstListOfCommonEvents(List<DummyModuleEvent> events) {
		List<DummyModuleEvent> result = new ArrayList<>();
		if (events.isEmpty()) {
			return result;
		}
		ModuleEvent commonEvent = events.get(0).event;
		for (Iterator<DummyModuleEvent> iEvents = events.iterator(); iEvents.hasNext();) {
			DummyModuleEvent current = iEvents.next();
			if (commonEvent.equals(current.event)) {
				iEvents.remove();
				result.add(current);
			} else {
				break;
			}
		}
		return result;
	}
}
