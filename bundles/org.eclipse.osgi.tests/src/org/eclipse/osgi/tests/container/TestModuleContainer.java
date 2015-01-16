/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.container;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.container.Module.StartOptions;
import org.eclipse.osgi.container.Module.State;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ContainerEvent;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ModuleEvent;
import org.eclipse.osgi.container.builders.OSGiManifestBuilderFactory;
import org.eclipse.osgi.container.namespaces.EclipsePlatformNamespace;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.eclipse.osgi.tests.container.dummys.*;
import org.eclipse.osgi.tests.container.dummys.DummyModuleDatabase.DummyContainerEvent;
import org.eclipse.osgi.tests.container.dummys.DummyModuleDatabase.DummyModuleEvent;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.*;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.*;
import org.osgi.framework.wiring.*;
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

		final List<Throwable> installErrors = new ArrayList<Throwable>(0);
		// just trying to pound the container with a bunch of installs
		ExecutorService executor = Executors.newFixedThreadPool(10);
		Bundle[] bundles = context.getBundles();
		for (final Bundle bundle : bundles) {
			if (bundle.getBundleId() == 0)
				continue;
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						ModuleRevisionBuilder builder = OSGiManifestBuilderFactory.createBuilder(asMap(bundle.getHeaders("")));
						container.install(null, bundle.getLocation(), builder, null);
					} catch (Throwable t) {
						t.printStackTrace();
						synchronized (installErrors) {
							installErrors.add(t);
						}
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
		container.resolve(new ArrayList<Module>(), false);
		List<Module> modules = container.getModules();
		for (Module module : modules) {
			if (module.getCurrentRevision().getWiring() == null) {
				System.out.println("Could not resolve module: " + module.getCurrentRevision());
			}
		}
		return adaptor.getDatabase();
	}

	static <K, V> Map<K, V> asMap(Dictionary<K, V> dictionary) {
		Map<K, V> map = new HashMap<K, V>();
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
		adaptor.getContainer().resolve(new ArrayList<Module>(), false);
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
		adaptor.getContainer().resolve(new ArrayList<Module>(), false);
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
		container.resolve(Arrays.asList(h1v1, h1v2, f1v1, b3), true);
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
		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(true), Collections.<String, String> emptyMap());
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

		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(true), Collections.<String, String> emptyMap());
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
		ResolverHookFactory resolverHookFactory = new ResolverHookFactory() {

			@Override
			public ResolverHook begin(Collection<BundleRevision> triggers) {
				return new ResolverHook() {

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
			}
		};
		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(false), Collections.<String, String> emptyMap(), resolverHookFactory);
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
		final Collection<BundleRevision> disabled = new ArrayList<BundleRevision>();
		ResolverHookFactory resolverHookFactory = new ResolverHookFactory() {

			@Override
			public ResolverHook begin(Collection<BundleRevision> triggers) {
				return new ResolverHook() {

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
			}
		};
		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(false), Collections.<String, String> emptyMap(), resolverHookFactory);
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
		List<DummyModuleEvent> expected = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(c1, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c2, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c3, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c4, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c5, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c6, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c7, ModuleEvent.RESOLVED, State.RESOLVED)));
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
		List<DummyModuleEvent> expected = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(systemBundle, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c1, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c2, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c3, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c4, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c5, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c6, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c7, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(systemBundle, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c1, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c2,
				ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c3, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c4, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c5, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c6, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c7, ModuleEvent.RESOLVED, State.RESOLVED)));
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
		List<DummyModuleEvent> expected = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(c1, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c2, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c3, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c4, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c5, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c6, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c7, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c7, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c7, ModuleEvent.STARTED, State.ACTIVE)));
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
		List<DummyModuleEvent> expected = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(c7, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c7, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c4, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c5, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c6, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c7, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c4, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c5, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c6, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c7, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(c7, ModuleEvent.STARTING,
				State.STARTING), new DummyModuleEvent(c7, ModuleEvent.STARTED, State.ACTIVE)));
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
		List<DummyModuleEvent> expected = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(c4, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c4, ModuleEvent.UPDATED, State.INSTALLED), new DummyModuleEvent(c4, ModuleEvent.RESOLVED, State.RESOLVED),

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
		expected = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(c4, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c4, ModuleEvent.UNINSTALLED, State.UNINSTALLED), new DummyModuleEvent(c6, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c7, ModuleEvent.UNRESOLVED, State.INSTALLED)));
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
		expected = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(c4, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c4, ModuleEvent.UNINSTALLED, State.UNINSTALLED), new DummyModuleEvent(c6, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(c7, ModuleEvent.UNRESOLVED, State.INSTALLED)));
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

		Collection<ModuleRevision> requirers = new ArrayList<ModuleRevision>();
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

		Collection<ModuleRevision> requirers = new HashSet<ModuleRevision>();
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
		List<DummyModuleEvent> expected = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(lazy1, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(lazy1, ModuleEvent.LAZY_ACTIVATION, State.LAZY_STARTING)));
		assertEvents(expected, actual, true);

		lazy1.start(StartOptions.LAZY_TRIGGER);

		actual = database.getModuleEvents();
		expected = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(lazy1, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(lazy1, ModuleEvent.STARTED, State.ACTIVE)));
		assertEvents(expected, actual, true);

		container.refresh(Arrays.asList(lazy1));

		actual = database.getModuleEvents();
		expected = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(lazy1, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(lazy1, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(lazy1, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(lazy1, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(lazy1, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(lazy1, ModuleEvent.STARTED, State.ACTIVE)));
		assertEvents(expected, actual, true);

		container.update(lazy1, OSGiManifestBuilderFactory.createBuilder(getManifest("lazy1_v1.MF")), null);
		actual = database.getModuleEvents();
		expected = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(lazy1, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(lazy1, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(lazy1, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(lazy1, ModuleEvent.UPDATED, State.INSTALLED), new DummyModuleEvent(lazy1, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(lazy1, ModuleEvent.LAZY_ACTIVATION, State.LAZY_STARTING)));
		assertEvents(expected, actual, true);

		container.refresh(Arrays.asList(lazy1));
		actual = database.getModuleEvents();
		expected = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(lazy1, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(lazy1, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(lazy1, ModuleEvent.UNRESOLVED, State.INSTALLED), new DummyModuleEvent(lazy1, ModuleEvent.RESOLVED, State.RESOLVED), new DummyModuleEvent(lazy1, ModuleEvent.LAZY_ACTIVATION, State.LAZY_STARTING)));
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
		List<DummyContainerEvent> expectedContainerEvents = new ArrayList<DummyContainerEvent>(Arrays.asList(new DummyContainerEvent(ContainerEvent.START_LEVEL, systemBundle, null)));
		Assert.assertEquals("Wrong container events.", expectedContainerEvents, actualContainerEvents);

		actual = database.getModuleEvents(3);
		List<DummyModuleEvent> expected = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(lazy1, ModuleEvent.LAZY_ACTIVATION, State.LAZY_STARTING), new DummyModuleEvent(c4, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c4, ModuleEvent.STARTED, State.ACTIVE)));
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
		expectedContainerEvents = new ArrayList<DummyContainerEvent>(Arrays.asList(new DummyContainerEvent(ContainerEvent.START_LEVEL, systemBundle, null), new DummyContainerEvent(ContainerEvent.STARTED, systemBundle, null)));

		actual = database.getModuleEvents(2);
		expected = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(systemBundle, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(systemBundle, ModuleEvent.STARTED, State.ACTIVE)));
		assertEvents(expected, actual, true);

		container.getFrameworkStartLevel().setStartLevel(3);

		actualContainerEvents = database.getContainerEvents(1);
		expectedContainerEvents = new ArrayList<DummyContainerEvent>(Arrays.asList(new DummyContainerEvent(ContainerEvent.START_LEVEL, systemBundle, null)));
		Assert.assertEquals("Wrong container events.", expectedContainerEvents, actualContainerEvents);

		actual = database.getModuleEvents(3);
		expected = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(lazy1, ModuleEvent.LAZY_ACTIVATION, State.LAZY_STARTING), new DummyModuleEvent(c4, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c4, ModuleEvent.STARTED, State.ACTIVE)));
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
		Map<String, String> configuration = new HashMap<String, String>();
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
			List<DummyModuleEvent> expectedModuleEvents = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(systemBundle, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(systemBundle, ModuleEvent.STARTED, State.ACTIVE)));
			assertEvents(expectedModuleEvents, actualModuleEvents, true);

			List<DummyContainerEvent> actualContainerEvents = database.getContainerEvents();
			List<DummyContainerEvent> expectedContainerEvents = new ArrayList<DummyContainerEvent>(Arrays.asList(new DummyContainerEvent(ContainerEvent.STARTED, systemBundle, null)));
			Assert.assertEquals("Wrong container events.", expectedContainerEvents, actualContainerEvents);

			container.getFrameworkStartLevel().setStartLevel(100);
			actualModuleEvents = database.getModuleEvents(14);
			expectedModuleEvents = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(c7, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c7, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c6, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c6, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c5, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c5, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c4, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c4, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c3, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c3, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c2, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c2,
					ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c1, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c1, ModuleEvent.STARTED, State.ACTIVE)));

			actualContainerEvents = database.getContainerEvents(1);
			expectedContainerEvents = new ArrayList<DummyContainerEvent>(Arrays.asList(new DummyContainerEvent(ContainerEvent.START_LEVEL, systemBundle, null)));
			Assert.assertEquals("Wrong container events.", expectedContainerEvents, actualContainerEvents);
		} else {
			actualModuleEvents = database.getModuleEvents(16);
			List<DummyModuleEvent> expectedModuleEvents = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(systemBundle, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c7, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c7, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c6, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c6, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c5, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c5, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c4, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c4, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c3, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c3, ModuleEvent.STARTED,
					State.ACTIVE), new DummyModuleEvent(c2, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c2, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(c1, ModuleEvent.STARTING, State.STARTING), new DummyModuleEvent(c1, ModuleEvent.STARTED, State.ACTIVE), new DummyModuleEvent(systemBundle, ModuleEvent.STARTED, State.ACTIVE)));
			assertEvents(expectedModuleEvents, actualModuleEvents, true);

			List<DummyContainerEvent> actualContainerEvents = database.getContainerEvents();
			List<DummyContainerEvent> expectedContainerEvents = new ArrayList<DummyContainerEvent>(Arrays.asList(new DummyContainerEvent(ContainerEvent.STARTED, systemBundle, null)));
			Assert.assertEquals("Wrong container events.", expectedContainerEvents, actualContainerEvents);
		}

		if (beginningStartLevel == 1) {
			container.getFrameworkStartLevel().setStartLevel(1);
			actualModuleEvents = database.getModuleEvents(14);
			List<DummyModuleEvent> expectedModuleEvents = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(c1, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c1, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c2, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c2, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c3, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c3, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c4, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c4, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c5, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c5, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c6, ModuleEvent.STOPPING,
					State.STOPPING), new DummyModuleEvent(c6, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c7, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c7, ModuleEvent.STOPPED, State.RESOLVED)));
			assertEvents(expectedModuleEvents, actualModuleEvents, true);

			List<DummyContainerEvent> actualContainerEvents = database.getContainerEvents(1);
			List<DummyContainerEvent> expectedContainerEvents = new ArrayList<DummyContainerEvent>(Arrays.asList(new DummyContainerEvent(ContainerEvent.START_LEVEL, systemBundle, null)));
			Assert.assertEquals("Wrong container events.", expectedContainerEvents, actualContainerEvents);
		}

		systemBundle.stop();

		if (beginningStartLevel == 1) {
			actualModuleEvents = database.getModuleEvents(2);
			List<DummyModuleEvent> expectedModuleEvents = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(systemBundle, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(systemBundle, ModuleEvent.STOPPED, State.RESOLVED)));
			assertEvents(expectedModuleEvents, actualModuleEvents, true);
		} else {
			actualModuleEvents = database.getModuleEvents(16);
			List<DummyModuleEvent> expectedModuleEvents = new ArrayList<DummyModuleEvent>(Arrays.asList(new DummyModuleEvent(systemBundle, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c1, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c1, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c2, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c2, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c3, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c3, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c4, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c4, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c5, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c5, ModuleEvent.STOPPED,
					State.RESOLVED), new DummyModuleEvent(c6, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c6, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(c7, ModuleEvent.STOPPING, State.STOPPING), new DummyModuleEvent(c7, ModuleEvent.STOPPED, State.RESOLVED), new DummyModuleEvent(systemBundle, ModuleEvent.STOPPED, State.RESOLVED)));
			assertEvents(expectedModuleEvents, actualModuleEvents, true);
		}
		List<DummyContainerEvent> actualContainerEvents = database.getContainerEvents();
		List<DummyContainerEvent> expectedContainerEvents = new ArrayList<DummyContainerEvent>(Arrays.asList(new DummyContainerEvent(ContainerEvent.STOPPED, systemBundle, null)));
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

		dynamicWire = container.resolveDynamic("org.osgi.framework.wiring", dynamic1.getCurrentRevision());
		Assert.assertNotNull("No dynamic wire found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "org.osgi.framework.wiring", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));

		dynamicWire = container.resolveDynamic("c1.b", dynamic1.getCurrentRevision());
		Assert.assertNotNull("No dynamic wire found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "c1.b", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));

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

		dynamicWire = container.resolveDynamic("c4.a", dynamic1.getCurrentRevision());
		Assert.assertNotNull("No dynamic wire found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "c4.a", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));

		dynamicWire = container.resolveDynamic("c4.b", dynamic1.getCurrentRevision());
		Assert.assertNotNull("No dynamic wire found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "c4.b", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
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

		dynamicWire = container.resolveDynamic("f1.a", dynamic3.getCurrentRevision());
		Assert.assertNotNull("Dynamic wire not found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "f1.a", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));

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

		dynamicWire = container.resolveDynamic("c4.b", dynamic3.getCurrentRevision());
		Assert.assertNotNull("No dynamic wire found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "c4.b", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
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

		ModuleWire dynamicWire = container.resolveDynamic("h1.a", dynamic3.getCurrentRevision());
		Assert.assertNull("Dynamic wire found.", dynamicWire);
		dynamicWire = container.resolveDynamic("f1.a", dynamic3.getCurrentRevision());
		Assert.assertNull("Dynamic wire found.", dynamicWire);

		Module h1 = installDummyModule("h1_v1.MF", "h1_v1", container);

		dynamicWire = container.resolveDynamic("h1.a", dynamic3.getCurrentRevision());
		Assert.assertNotNull("Dynamic wire not found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "h1.a", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));

		dynamicWire = container.resolveDynamic("f1.a", dynamic3.getCurrentRevision());
		Assert.assertNotNull("Dynamic wire not found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "f1.a", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));

		ModuleWiring h1Wiring = h1.getCurrentRevision().getWiring();
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

		Module a2 = installDummyModule("bug457118.a2.MF", "a2", container);
		Module b2 = installDummyModule("bug457118.b2.MF", "b2", container);
		Module c2 = installDummyModule("bug457118.c2.MF", "c2", container);
		Module d2 = installDummyModule("bug457118.d2.MF", "d2", container);

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
		List<DummyModuleEvent> result = new ArrayList<DummyModuleDatabase.DummyModuleEvent>();
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
