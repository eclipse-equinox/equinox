/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container.tests;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import junit.framework.Assert;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.container.Module.Event;
import org.eclipse.osgi.container.Module.StartOptions;
import org.eclipse.osgi.container.Module.State;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ContainerEvent;
import org.eclipse.osgi.container.builders.OSGiManifestBuilderFactory;
import org.eclipse.osgi.container.namespaces.EclipsePlatformNamespace;
import org.eclipse.osgi.container.tests.dummys.*;
import org.eclipse.osgi.container.tests.dummys.DummyModuleDataBase.DummyContainerEvent;
import org.eclipse.osgi.container.tests.dummys.DummyModuleDataBase.DummyModuleEvent;
import org.eclipse.osgi.util.ManifestElement;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.*;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.*;
import org.osgi.service.resolver.ResolutionException;

public class TestModuleContainer {

	private static DummyModuleDataBase resolvedModuleDatabase;

	@Before
	public void setup() throws BundleException, ResolutionException {
		if (resolvedModuleDatabase == null) {
			resolvedModuleDatabase = getDatabase();
		}
	}

	private static final String OSGI_OS = "osgi.os";
	private static final String OSGI_WS = "osgi.ws";
	private static final String OSGI_ARCH = "osgi.arch";

	private DummyModuleDataBase getDatabase() throws BundleException, ResolutionException {
		BundleContext context = ((BundleReference) getClass().getClassLoader()).getBundle().getBundleContext();

		DummyModuleDataBase moduleDatabase = new DummyModuleDataBase();
		final ModuleContainer container = createDummyContainer(moduleDatabase);

		Bundle systemBundle = context.getBundle(0);
		String extraCapabilities = context.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES);
		extraCapabilities = (extraCapabilities == null ? "" : (extraCapabilities + ", "));
		String osName = context.getProperty(OSGI_OS);
		String wsName = context.getProperty(OSGI_WS);
		String archName = context.getProperty(OSGI_ARCH);
		extraCapabilities += EclipsePlatformNamespace.ECLIPSE_PLATFORM_NAMESPACE + "; " +
				OSGI_OS + "=" + osName + "; " +
				OSGI_WS + "=" + wsName + "; " +
				OSGI_ARCH + "=" + archName;
		ModuleRevisionBuilder builder = OSGiManifestBuilderFactory.createBuilder(asMap(systemBundle.getHeaders("")), Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null, extraCapabilities);
		container.install(null, systemBundle.getLocation(), builder);

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
						container.install(null, bundle.getLocation(), builder);
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
		return moduleDatabase;
	}

	private static <K, V> Map<K, V> asMap(Dictionary<K, V> dictionary) {
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
		createDummyContainer(new DummyModuleDataBase());
	}

	@Test
	public void testResolveInstallBundles() throws BundleException, ResolutionException, IOException {
		DummyModuleDataBase moduleDataBase = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(moduleDataBase);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		resolvedModuleDatabase.store(new DataOutputStream(bytes), false);
		bytes.close();
		moduleDataBase.load(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		container.resolve(new ArrayList<Module>(), false);
	}

	@Test
	public void testResolveInstallBundles01() throws BundleException, ResolutionException, IOException {
		DummyModuleDataBase moduleDataBase = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(moduleDataBase);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		resolvedModuleDatabase.store(new DataOutputStream(bytes), false);
		bytes.close();
		moduleDataBase.load(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		for (int i = 0; i < 50; i++) {
			container.refresh(container.getModules());
		}
	}

	@Test
	public void testResolveAlreadyResolvedBundles() throws BundleException, ResolutionException, IOException {
		DummyModuleDataBase moduleDataBase = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(moduleDataBase);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		resolvedModuleDatabase.store(new DataOutputStream(bytes), true);
		bytes.close();
		moduleDataBase.load(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		container.resolve(new ArrayList<Module>(), false);
	}

	@Test
	public void testRefreshSystemBundle() throws ResolutionException, BundleException, IOException {
		DummyModuleDataBase moduleDataBase = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(moduleDataBase);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		resolvedModuleDatabase.store(new DataOutputStream(bytes), true);
		bytes.close();
		moduleDataBase.load(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		container.refresh(Arrays.asList(container.getModule(0)));
	}

	@Test
	public void testSimpleResolve() throws BundleException, IOException, ResolutionException {
		ModuleContainer container = createDummyContainer(new DummyModuleDataBase());

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
	public void testSimpleUnResolveable() throws BundleException, IOException, ResolutionException {
		ModuleContainer container = createDummyContainer(new DummyModuleDataBase());

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);
		container.resolve(Arrays.asList(systemBundle), true);

		Module c7 = installDummyModule("c7_v1.MF", "c7", container);
		installDummyModule("c6_v1.MF", "c6", container);

		try {
			container.resolve(Arrays.asList(c7), true);
			// Expected a resolution exception
			Assert.fail("Expected a resolution exception");
		} catch (ResolutionException e) {
			// expected
		}

		// Should resolve now
		installDummyModule("c4_v1.MF", "c4", container);
		container.resolve(Arrays.asList(c7), true);
	}

	@Test
	public void testMultiHost() throws BundleException, IOException, ResolutionException {
		ModuleContainer container = createDummyContainer(new DummyModuleDataBase());
		installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);
		Module h1v1 = installDummyModule("h1_v1.MF", "h1_v1", container);
		Module h1v2 = installDummyModule("h1_v2.MF", "h1_v2", container);
		Module f1v1 = installDummyModule("f1_v1.MF", "f1_v1", container);
		Module b3 = installDummyModule("b3_v1.MF", "b3_v1", container);
		container.resolve(Arrays.asList(h1v1, h1v2, f1v1, b3), true);
	}

	@Test
	public void testFragments01() throws ResolutionException, BundleException, IOException {
		ModuleContainer container = createDummyContainer(new DummyModuleDataBase());
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
	public void testFragments02() throws ResolutionException, BundleException, IOException {
		ModuleContainer container = createDummyContainer(new DummyModuleDataBase());
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
	public void testExecutionEnvironment() throws BundleException, IOException, ResolutionException {
		ModuleContainer container = createDummyContainer(new DummyModuleDataBase());
		String extraCapabilities = "osgi.ee; osgi.ee=JavaSE; version:List<Version>=\"1.3, 1.4, 1.5, 1.6, 1.7\"";
		installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, null, null, extraCapabilities, container);
		container.resolve(null, false);

		Module ee1 = installDummyModule("ee1_v1.MF", "ee1", container);
		Module ee2 = installDummyModule("ee2_v1.MF", "ee2", container);
		container.resolve(Arrays.asList(ee1, ee2), true);

		ModuleWiring ee1Wiring = ee1.getCurrentRevision().getWiring();
		ModuleWiring ee2Wiring = ee2.getCurrentRevision().getWiring();
		Assert.assertNotNull("ee1 is not resolved", ee1Wiring);
		Assert.assertNotNull("ee2 is not resolved", ee2Wiring);

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
	public void testPlatformFilter01() throws BundleException, IOException, ResolutionException {
		ModuleContainer container = createDummyContainer(new DummyModuleDataBase());
		String extraCapabilities = EclipsePlatformNamespace.ECLIPSE_PLATFORM_NAMESPACE + "; osgi.os=foo; osgi.arch=bar";
		installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, null, null, extraCapabilities, container);
		container.resolve(null, false);

		Module platformFilter1 = installDummyModule("platformFilter1_v1.MF", "ee1", container);
		container.resolve(Arrays.asList(platformFilter1), true);

		ModuleWiring platformFilter1Wiring = platformFilter1.getCurrentRevision().getWiring();
		Assert.assertNotNull("platformFilter1 is not resolved", platformFilter1Wiring);
	}

	@Test
	public void testPlatformFilter02() throws BundleException, IOException, ResolutionException {
		ModuleContainer container = createDummyContainer(new DummyModuleDataBase());
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
		ModuleContainer container = createDummyContainer(new DummyModuleDataBase());
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
		DummyModuleDataBase database = new DummyModuleDataBase();
		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(true), new DummyResolverHookFactory(), database, Collections.<String, Object> emptyMap());
		ModuleContainer container = createDummyContainer(database, adaptor);
		installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);
		installDummyModule("b1_v1.MF", "b1_a", container);
		installDummyModule("b1_v1.MF", "b1_b", container);
	}

	@Test
	public void testUpdateCollision01() throws BundleException, IOException {
		ModuleContainer container = createDummyContainer(new DummyModuleDataBase());
		Module b1_v1 = installDummyModule("b1_v1.MF", "b1_v1", container);
		installDummyModule("b1_v2.MF", "b1_v2", container);
		try {
			container.update(b1_v1, OSGiManifestBuilderFactory.createBuilder(getManifest("b1_v2.MF")));
			Assert.fail("Expected to fail update because of a collision.");
		} catch (BundleException e) {
			// expected
			Assert.assertEquals("Wrong exception type.", BundleException.DUPLICATE_BUNDLE_ERROR, e.getType());
		}
	}

	@Test
	public void testUpdateCollision02() throws BundleException, IOException {
		ModuleContainer container = createDummyContainer(new DummyModuleDataBase());
		Module b1_v1 = installDummyModule("b1_v1.MF", "b1_v1", container);
		try {
			container.update(b1_v1, OSGiManifestBuilderFactory.createBuilder(getManifest("b1_v1.MF")));
		} catch (BundleException e) {
			Assert.assertNull("Expected to succeed update to same revision.", e);
		}
	}

	@Test
	public void testUpdateCollision03() throws BundleException, IOException {
		DummyModuleDataBase database = new DummyModuleDataBase();
		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(true), new DummyResolverHookFactory(), database, Collections.<String, Object> emptyMap());
		ModuleContainer container = createDummyContainer(database, adaptor);
		Module b1_v1 = installDummyModule("b1_v1.MF", "b1_v1", container);
		installDummyModule("b1_v2.MF", "b1_v2", container);
		try {
			container.update(b1_v1, OSGiManifestBuilderFactory.createBuilder(getManifest("b1_v2.MF")));
		} catch (BundleException e) {
			Assert.assertNull("Expected to succeed update to same revision.", e);
		}
	}

	@Test
	public void testSingleton01() throws BundleException, IOException, ResolutionException {
		DummyModuleDataBase database = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(database);
		Module s1 = installDummyModule("singleton1_v1.MF", "s1_v1", container);
		Module s2 = installDummyModule("singleton1_v2.MF", "s1_v2", container);
		Module s3 = installDummyModule("singleton1_v3.MF", "s1_v3", container);

		container.resolve(null, false);

		Assert.assertFalse("Singleton v1 is resolved.", Module.RESOLVED_SET.contains(s1.getState()));
		Assert.assertFalse("Singleton v2 is resolved.", Module.RESOLVED_SET.contains(s2.getState()));
		Assert.assertTrue("Singleton v3 is not resolved.", Module.RESOLVED_SET.contains(s3.getState()));
	}

	@Test
	public void testSingleton02() throws BundleException, IOException, ResolutionException {
		DummyModuleDataBase database = new DummyModuleDataBase();
		ResolverHookFactory resolverHookFactory = new ResolverHookFactory() {

			@Override
			public ResolverHook begin(Collection<BundleRevision> triggers) {
				return new ResolverHook() {

					@Override
					public void filterSingletonCollisions(BundleCapability singleton,
							Collection<BundleCapability> collisionCandidates) {
						collisionCandidates.clear();
					}

					@Override
					public void filterResolvable(Collection<BundleRevision> candidates) {
						// nothing
					}

					@Override
					public void filterMatches(BundleRequirement requirement,
							Collection<BundleCapability> candidates) {
						// nothing
					}

					@Override
					public void end() {
						// nothing
					}
				};
			}
		};
		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(false), resolverHookFactory, database, Collections.<String, Object> emptyMap());
		ModuleContainer container = createDummyContainer(database, adaptor);

		Module s1 = installDummyModule("singleton1_v1.MF", "s1_v1", container);
		Module s2 = installDummyModule("singleton1_v2.MF", "s1_v2", container);
		Module s3 = installDummyModule("singleton1_v3.MF", "s1_v3", container);

		container.resolve(null, false);

		Assert.assertTrue("Singleton v1 is not resolved.", Module.RESOLVED_SET.contains(s1.getState()));
		Assert.assertTrue("Singleton v2 is not resolved.", Module.RESOLVED_SET.contains(s2.getState()));
		Assert.assertTrue("Singleton v3 is not resolved.", Module.RESOLVED_SET.contains(s3.getState()));
	}

	@Test
	public void testSingleton03() throws BundleException, IOException, ResolutionException {
		DummyModuleDataBase database = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(database);
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
	public void testSingleton04() throws BundleException, IOException, ResolutionException {
		final Collection<BundleRevision> disabled = new ArrayList<BundleRevision>();
		DummyModuleDataBase database = new DummyModuleDataBase();
		ResolverHookFactory resolverHookFactory = new ResolverHookFactory() {

			@Override
			public ResolverHook begin(Collection<BundleRevision> triggers) {
				return new ResolverHook() {

					@Override
					public void filterSingletonCollisions(BundleCapability singleton,
							Collection<BundleCapability> collisionCandidates) {
						// nothing
					}

					@Override
					public void filterResolvable(Collection<BundleRevision> candidates) {
						candidates.removeAll(disabled);
					}

					@Override
					public void filterMatches(BundleRequirement requirement,
							Collection<BundleCapability> candidates) {
						// nothing
					}

					@Override
					public void end() {
						// nothing
					}
				};
			}
		};
		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(false), resolverHookFactory, database, Collections.<String, Object> emptyMap());
		ModuleContainer container = createDummyContainer(database, adaptor);

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
		DummyModuleDataBase database = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(database);
		Module c1 = installDummyModule("c1_v1.MF", "c1_v1", container);
		Module c2 = installDummyModule("c2_v1.MF", "c2_v1", container);
		Module c3 = installDummyModule("c3_v1.MF", "c3_v1", container);
		Module c4 = installDummyModule("c4_v1.MF", "c4_v1", container);
		Module c5 = installDummyModule("c5_v1.MF", "c5_v1", container);
		Module c6 = installDummyModule("c6_v1.MF", "c6_v1", container);
		Module c7 = installDummyModule("c7_v1.MF", "c7_v1", container);

		List<DummyModuleEvent> actual = database.getModuleEvents();
		List<DummyModuleEvent> expected = Arrays.asList(
				new DummyModuleEvent(c1, Event.INSTALLED, State.INSTALLED),
				new DummyModuleEvent(c2, Event.INSTALLED, State.INSTALLED),
				new DummyModuleEvent(c3, Event.INSTALLED, State.INSTALLED),
				new DummyModuleEvent(c4, Event.INSTALLED, State.INSTALLED),
				new DummyModuleEvent(c5, Event.INSTALLED, State.INSTALLED),
				new DummyModuleEvent(c6, Event.INSTALLED, State.INSTALLED),
				new DummyModuleEvent(c7, Event.INSTALLED, State.INSTALLED));
		Assert.assertEquals("Wrong install events.", expected, actual);
	}

	@Test
	public void testEventsResolved() throws BundleException, IOException, ResolutionException {
		DummyModuleDataBase database = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(database);
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
		List<DummyModuleEvent> expected = new ArrayList<DummyModuleEvent>(Arrays.asList(
				new DummyModuleEvent(c1, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c2, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c3, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c4, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c5, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c6, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c7, Event.RESOLVED, State.RESOLVED)));
		assertEvents(expected, actual, false);
	}

	@Test
	public void testEventsRefresh() throws BundleException, IOException, ResolutionException {
		DummyModuleDataBase database = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(database);
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
		List<DummyModuleEvent> expected = new ArrayList<DummyModuleEvent>(Arrays.asList(
				new DummyModuleEvent(systemBundle, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c1, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c2, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c3, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c4, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c5, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c6, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c7, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(systemBundle, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c1, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c2, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c3, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c4, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c5, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c6, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c7, Event.RESOLVED, State.RESOLVED)));
		assertEvents(expected, actual, false);
	}

	@Test
	public void testEventsStart() throws BundleException, IOException, ResolutionException {
		DummyModuleDataBase database = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(database);
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
		List<DummyModuleEvent> expected = new ArrayList<DummyModuleEvent>(Arrays.asList(
				new DummyModuleEvent(c1, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c2, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c3, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c4, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c5, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c6, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c7, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c7, Event.STARTING, State.STARTING),
				new DummyModuleEvent(c7, Event.STARTED, State.ACTIVE)));
		assertEvents(expected, actual, false);
	}

	@Test
	public void testEventsStartRefresh() throws BundleException, IOException, ResolutionException {
		DummyModuleDataBase database = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(database);
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
		List<DummyModuleEvent> expected = new ArrayList<DummyModuleEvent>(Arrays.asList(
				new DummyModuleEvent(c7, Event.STOPPING, State.STOPPING),
				new DummyModuleEvent(c7, Event.STOPPED, State.RESOLVED),
				new DummyModuleEvent(c4, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c5, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c6, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c7, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c4, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c5, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c6, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c7, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c7, Event.STARTING, State.STARTING),
				new DummyModuleEvent(c7, Event.STARTED, State.ACTIVE)));
		assertEvents(expected, actual, false);
	}

	@Test
	public void testRemovalPending() throws ResolutionException, BundleException, IOException {
		DummyModuleDataBase database = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(database);
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
		container.update(c4, OSGiManifestBuilderFactory.createBuilder(getManifest("c4_v1.MF")));
		container.resolve(Arrays.asList(c4), true);

		Collection<ModuleRevision> removalPending = container.getRemovalPending();
		Assert.assertEquals("Wrong number of removal pending", 1, removalPending.size());
		Assert.assertTrue("Wrong module removalPending: " + removalPending, removalPending.contains(c4Revision0));

		ModuleRevision c6Revision0 = c6.getCurrentRevision();
		// updating to identical content
		container.update(c6, OSGiManifestBuilderFactory.createBuilder(getManifest("c6_v1.MF")));
		container.resolve(Arrays.asList(c6), true);

		removalPending = container.getRemovalPending();
		Assert.assertEquals("Wrong number of removal pending", 2, removalPending.size());
		Assert.assertTrue("Wrong module removalPending: " + removalPending, removalPending.containsAll(Arrays.asList(c4Revision0, c6Revision0)));

		// update again with identical content
		container.update(c4, OSGiManifestBuilderFactory.createBuilder(getManifest("c4_v1.MF")));
		container.update(c6, OSGiManifestBuilderFactory.createBuilder(getManifest("c6_v1.MF")));
		container.resolve(Arrays.asList(c4, c6), true);

		// Again we only have two since the previous current revisions did not have any dependents
		removalPending = container.getRemovalPending();
		Assert.assertEquals("Wrong number of removal pending", 2, removalPending.size());
		Assert.assertTrue("Wrong module removalPending: " + removalPending, removalPending.containsAll(Arrays.asList(c4Revision0, c6Revision0)));

		container.refresh(null);

		removalPending = container.getRemovalPending();
		Assert.assertEquals("Wrong number of removal pending", 0, removalPending.size());

		List<DummyModuleEvent> actual = database.getModuleEvents();
		List<DummyModuleEvent> expected = new ArrayList<DummyModuleEvent>(Arrays.asList(
				new DummyModuleEvent(c4, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c4, Event.UPDATED, State.INSTALLED),
				new DummyModuleEvent(c4, Event.RESOLVED, State.RESOLVED),

				new DummyModuleEvent(c6, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c6, Event.UPDATED, State.INSTALLED),
				new DummyModuleEvent(c6, Event.RESOLVED, State.RESOLVED),

				new DummyModuleEvent(c4, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c4, Event.UPDATED, State.INSTALLED),
				new DummyModuleEvent(c6, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c6, Event.UPDATED, State.INSTALLED),
				new DummyModuleEvent(c4, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c6, Event.RESOLVED, State.RESOLVED),

				new DummyModuleEvent(c4, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c6, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c7, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(c4, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c6, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(c7, Event.RESOLVED, State.RESOLVED)));
		assertEvents(expected, actual, false);
	}

	@Test
	public void testSubstitutableExports() throws ResolutionException, BundleException, IOException {
		DummyModuleDataBase database = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(database);
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		Module sub1 = installDummyModule("sub1_v1.MF", "sub1", container);
		Module sub2 = installDummyModule("sub2_v1.MF", "sub2", container);

		ModuleRevision sub1Revision0 = sub1.getCurrentRevision();

		container.resolve(Arrays.asList(sub2), true);

		container.update(sub1, OSGiManifestBuilderFactory.createBuilder(getManifest("sub1_v2.MF")));
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
	public void testLazy01() throws BundleException, IOException, ResolutionException {
		DummyModuleDataBase database = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(database);
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		// actually launch the container
		systemBundle.start();

		Module lazy1 = installDummyModule("lazy1_v1.MF", "lazy1", container);

		// throw out installed and resolved events
		database.getModuleEvents();

		lazy1.start(StartOptions.USE_ACTIVATION_POLICY);

		List<DummyModuleEvent> actual = database.getModuleEvents();
		List<DummyModuleEvent> expected = new ArrayList<DummyModuleEvent>(Arrays.asList(
				new DummyModuleEvent(lazy1, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(lazy1, Event.LAZY_ACTIVATION, State.LAZY_STARTING)));
		assertEvents(expected, actual, true);

		lazy1.start(StartOptions.LAZY_TRIGGER);

		actual = database.getModuleEvents();
		expected = new ArrayList<DummyModuleEvent>(Arrays.asList(
				new DummyModuleEvent(lazy1, Event.STARTING, State.STARTING),
				new DummyModuleEvent(lazy1, Event.STARTED, State.ACTIVE)));
		assertEvents(expected, actual, true);

		container.refresh(Arrays.asList(lazy1));

		actual = database.getModuleEvents();
		expected = new ArrayList<DummyModuleEvent>(Arrays.asList(
				new DummyModuleEvent(lazy1, Event.STOPPING, State.STOPPING),
				new DummyModuleEvent(lazy1, Event.STOPPED, State.RESOLVED),
				new DummyModuleEvent(lazy1, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(lazy1, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(lazy1, Event.LAZY_ACTIVATION, State.LAZY_STARTING)));
		assertEvents(expected, actual, true);

		lazy1.start(StartOptions.LAZY_TRIGGER);
		// flush events
		database.getModuleEvents();

		container.update(lazy1, OSGiManifestBuilderFactory.createBuilder(getManifest("lazy1_v1.MF")));
		actual = database.getModuleEvents();
		expected = new ArrayList<DummyModuleEvent>(Arrays.asList(
				new DummyModuleEvent(lazy1, Event.STOPPING, State.STOPPING),
				new DummyModuleEvent(lazy1, Event.STOPPED, State.RESOLVED),
				new DummyModuleEvent(lazy1, Event.UNRESOLVED, State.INSTALLED),
				new DummyModuleEvent(lazy1, Event.UPDATED, State.INSTALLED),
				new DummyModuleEvent(lazy1, Event.RESOLVED, State.RESOLVED),
				new DummyModuleEvent(lazy1, Event.LAZY_ACTIVATION, State.LAZY_STARTING)));
		assertEvents(expected, actual, true);
	}

	@Test
	public void testSettings01() throws BundleException, IOException, ResolutionException {
		DummyModuleDataBase database = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(database);
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		// actually launch the container
		systemBundle.start();

		Module c4 = installDummyModule("c4_v1.MF", "c4_v1", container);
		Module lazy1 = installDummyModule("lazy1_v1.MF", "lazy1", container);

		container.resolve(Arrays.asList(c4, lazy1), true);

		Assert.assertEquals("Wrong startlevel.", 1, c4.getStartLevel());
		Assert.assertEquals("Wrong startlevel.", 1, lazy1.getStartLevel());

		c4.setStartLevel(2);
		lazy1.setStartLevel(2);

		Assert.assertEquals("Wrong startlevel.", 2, c4.getStartLevel());
		Assert.assertEquals("Wrong startlevel.", 2, lazy1.getStartLevel());

		database.getModuleEvents();

		c4.start();
		lazy1.start(StartOptions.USE_ACTIVATION_POLICY);

		List<DummyModuleEvent> actual = database.getModuleEvents();
		Assert.assertEquals("Did not expect any events.", 0, actual.size());

		database.getContainerEvents();
		container.getFrameworkStartLevel().setStartLevel(3);

		List<DummyContainerEvent> actualContainerEvents = database.getContainerEvents(1);
		List<DummyContainerEvent> expectedContainerEvents = new ArrayList<DummyContainerEvent>(Arrays.asList(
				new DummyContainerEvent(ContainerEvent.START_LEVEL, systemBundle, null)));
		Assert.assertEquals("Wrong container events.", expectedContainerEvents, actualContainerEvents);

		actual = database.getModuleEvents(3);
		List<DummyModuleEvent> expected = new ArrayList<DummyModuleEvent>(Arrays.asList(
				new DummyModuleEvent(lazy1, Event.LAZY_ACTIVATION, State.LAZY_STARTING),
				new DummyModuleEvent(c4, Event.STARTING, State.STARTING),
				new DummyModuleEvent(c4, Event.STARTED, State.ACTIVE)));
		assertEvents(expected, actual, true);

		systemBundle.stop();

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(bytes);
		database.store(data, true);

		// reload into a new container
		database = new DummyModuleDataBase();
		container = createDummyContainer(database);
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
		Assert.assertEquals("c4 has wrong start-level", 2, c4.getStartLevel());
		Assert.assertTrue("lazy1 is using activation policy.", lazy1.isActivationPolicyUsed());
		Assert.assertTrue("lazy1 is not auto started.", lazy1.isPersistentlyStarted());
		Assert.assertEquals("lazy1 has wrong start-level", 2, lazy1.getStartLevel());

		// relaunch the container
		systemBundle.start();

		actualContainerEvents = database.getContainerEvents();
		expectedContainerEvents = new ArrayList<DummyContainerEvent>(Arrays.asList(
				new DummyContainerEvent(ContainerEvent.START_LEVEL, systemBundle, null),
				new DummyContainerEvent(ContainerEvent.STARTED, systemBundle, null)));

		actual = database.getModuleEvents(2);
		expected = new ArrayList<DummyModuleEvent>(Arrays.asList(
				new DummyModuleEvent(systemBundle, Event.STARTING, State.STARTING),
				new DummyModuleEvent(systemBundle, Event.STARTED, State.ACTIVE)));
		assertEvents(expected, actual, true);

		container.getFrameworkStartLevel().setStartLevel(3);

		actualContainerEvents = database.getContainerEvents(1);
		expectedContainerEvents = new ArrayList<DummyContainerEvent>(Arrays.asList(
				new DummyContainerEvent(ContainerEvent.START_LEVEL, systemBundle, null)));
		Assert.assertEquals("Wrong container events.", expectedContainerEvents, actualContainerEvents);

		actual = database.getModuleEvents(3);
		expected = new ArrayList<DummyModuleEvent>(Arrays.asList(
				new DummyModuleEvent(lazy1, Event.LAZY_ACTIVATION, State.LAZY_STARTING),
				new DummyModuleEvent(c4, Event.STARTING, State.STARTING),
				new DummyModuleEvent(c4, Event.STARTED, State.ACTIVE)));
		assertEvents(expected, actual, true);
	}

	@Test
	public void testEventsStartLevel() throws BundleException, IOException, ResolutionException {
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "100");
		DummyModuleDataBase database = new DummyModuleDataBase();
		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(false), new DummyResolverHookFactory(), database, configuration);
		ModuleContainer container = createDummyContainer(database, adaptor);
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

		actualModuleEvents = database.getModuleEvents(16);
		List<DummyModuleEvent> expectedModuleEvents = new ArrayList<DummyModuleEvent>(Arrays.asList(
				new DummyModuleEvent(systemBundle, Event.STARTING, State.STARTING),
				new DummyModuleEvent(c7, Event.STARTING, State.STARTING),
				new DummyModuleEvent(c7, Event.STARTED, State.ACTIVE),
				new DummyModuleEvent(c6, Event.STARTING, State.STARTING),
				new DummyModuleEvent(c6, Event.STARTED, State.ACTIVE),
				new DummyModuleEvent(c5, Event.STARTING, State.STARTING),
				new DummyModuleEvent(c5, Event.STARTED, State.ACTIVE),
				new DummyModuleEvent(c4, Event.STARTING, State.STARTING),
				new DummyModuleEvent(c4, Event.STARTED, State.ACTIVE),
				new DummyModuleEvent(c3, Event.STARTING, State.STARTING),
				new DummyModuleEvent(c3, Event.STARTED, State.ACTIVE),
				new DummyModuleEvent(c2, Event.STARTING, State.STARTING),
				new DummyModuleEvent(c2, Event.STARTED, State.ACTIVE),
				new DummyModuleEvent(c1, Event.STARTING, State.STARTING),
				new DummyModuleEvent(c1, Event.STARTED, State.ACTIVE),
				new DummyModuleEvent(systemBundle, Event.STARTED, State.ACTIVE)));
		assertEvents(expectedModuleEvents, actualModuleEvents, false);

		List<DummyContainerEvent> actualContainerEvents = database.getContainerEvents();
		List<DummyContainerEvent> expectedContainerEvents = new ArrayList<DummyContainerEvent>(Arrays.asList(
				new DummyContainerEvent(ContainerEvent.START_LEVEL, systemBundle, null),
				new DummyContainerEvent(ContainerEvent.STARTED, systemBundle, null)));
		Assert.assertEquals("Wrong container events.", expectedContainerEvents, actualContainerEvents);

		systemBundle.stop();

		actualContainerEvents = database.getContainerEvents();
		expectedContainerEvents = new ArrayList<DummyContainerEvent>(Arrays.asList(
				new DummyContainerEvent(ContainerEvent.START_LEVEL, systemBundle, null),
				new DummyContainerEvent(ContainerEvent.STOPPED, systemBundle, null)));
		Assert.assertEquals("Wrong container events.", expectedContainerEvents, actualContainerEvents);

		actualModuleEvents = database.getModuleEvents(16);
		expectedModuleEvents = new ArrayList<DummyModuleEvent>(Arrays.asList(
				new DummyModuleEvent(systemBundle, Event.STOPPING, State.STOPPING),
				new DummyModuleEvent(c1, Event.STOPPING, State.STOPPING),
				new DummyModuleEvent(c1, Event.STOPPED, State.RESOLVED),
				new DummyModuleEvent(c2, Event.STOPPING, State.STOPPING),
				new DummyModuleEvent(c2, Event.STOPPED, State.RESOLVED),
				new DummyModuleEvent(c3, Event.STOPPING, State.STOPPING),
				new DummyModuleEvent(c3, Event.STOPPED, State.RESOLVED),
				new DummyModuleEvent(c4, Event.STOPPING, State.STOPPING),
				new DummyModuleEvent(c4, Event.STOPPED, State.RESOLVED),
				new DummyModuleEvent(c5, Event.STOPPING, State.STOPPING),
				new DummyModuleEvent(c5, Event.STOPPED, State.RESOLVED),
				new DummyModuleEvent(c6, Event.STOPPING, State.STOPPING),
				new DummyModuleEvent(c6, Event.STOPPED, State.RESOLVED),
				new DummyModuleEvent(c7, Event.STOPPING, State.STOPPING),
				new DummyModuleEvent(c7, Event.STOPPED, State.RESOLVED),
				new DummyModuleEvent(systemBundle, Event.STOPPED, State.RESOLVED)));
		assertEvents(expectedModuleEvents, actualModuleEvents, false);

	}

	@Test
	public void testDynamicImport01() throws BundleException, IOException, ResolutionException {
		DummyModuleDataBase database = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(database);
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		Module c1 = installDummyModule("c1_v1.MF", "c1_v1", container);
		Module dynamic1 = installDummyModule("dynamic1_v1.MF", "dynamic1_v1", container);

		container.resolve(Arrays.asList(c1, dynamic1), true);

		ModuleWire dynamicWire = container.resolveDynamic("c1.b", dynamic1.getCurrentRevision());
		Assert.assertNotNull("No dynamic wire found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "c1.b", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
	}

	@Test
	public void testDynamicImport02() throws BundleException, IOException, ResolutionException {
		DummyModuleDataBase database = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(database);
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

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
	public void testDynamicImport03() throws BundleException, IOException, ResolutionException {
		DummyModuleDataBase database = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(database);
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);

		container.resolve(Arrays.asList(systemBundle), true);

		Module dynamic3 = installDummyModule("dynamic2_v1.MF", "dynamic2_v1", container);

		container.resolve(Arrays.asList(systemBundle, dynamic3), true);

		ModuleWire dynamicWire = container.resolveDynamic("c1.a", dynamic3.getCurrentRevision());
		Assert.assertNull("Dynamic wire found.", dynamicWire);

		Module c1 = installDummyModule("c1_v1.MF", "c1_v1", container);
		database.getModuleEvents();

		dynamicWire = container.resolveDynamic("c1.a", dynamic3.getCurrentRevision());
		Assert.assertNotNull("Dynamic wire not found.", dynamicWire);
		Assert.assertEquals("Wrong package found.", "c1.a", dynamicWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));

		ModuleWiring c1Wiring = c1.getCurrentRevision().getWiring();
		Assert.assertNotNull("c1 wiring is null.", c1Wiring);
	}

	@Test
	public void testDynamicImport04() throws BundleException, IOException, ResolutionException {
		DummyModuleDataBase database = new DummyModuleDataBase();
		ModuleContainer container = createDummyContainer(database);
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

	private ModuleContainer createDummyContainer(DummyModuleDataBase moduleDatabase) {
		DummyContainerAdaptor adaptor = new DummyContainerAdaptor(new DummyCollisionHook(false), new DummyResolverHookFactory(), moduleDatabase, Collections.<String, Object> emptyMap());
		return createDummyContainer(moduleDatabase, adaptor);

	}

	private ModuleContainer createDummyContainer(DummyModuleDataBase moduleDatabase, DummyContainerAdaptor adaptor) {
		ModuleContainer container = new ModuleContainer(adaptor, moduleDatabase);
		moduleDatabase.setContainer(container);
		return container;
	}

	private Module installDummyModule(String manifestFile, String location, ModuleContainer container) throws BundleException, IOException {
		return installDummyModule(manifestFile, location, null, null, null, container);
	}

	private Module installDummyModule(String manifestFile, String location, String alias, String extraExports, String extraCapabilities, ModuleContainer container) throws BundleException, IOException {
		Map<String, String> manifest = getManifest(manifestFile);
		ModuleRevisionBuilder builder = OSGiManifestBuilderFactory.createBuilder(manifest, alias, extraExports, extraCapabilities);
		Module system = container.getModule(0);
		return container.install(system, location, builder);
	}

	private Map<String, String> getManifest(String manifestFile) throws IOException, BundleException {
		URL manifest = ((BundleReference) getClass().getClassLoader()).getBundle().getEntry("/manifests/" + manifestFile);
		Assert.assertNotNull("Could not find manifest: " + manifestFile, manifest);
		return ManifestElement.parseBundleManifest(manifest.openStream(), null);
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
		List<DummyModuleEvent> result = new ArrayList<DummyModuleDataBase.DummyModuleEvent>();
		if (events.isEmpty()) {
			return result;
		}
		Event commonEvent = events.get(0).event;
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
