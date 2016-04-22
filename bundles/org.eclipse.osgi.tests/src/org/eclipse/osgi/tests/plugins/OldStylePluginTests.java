/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.plugins;

import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.BundleInstaller;
import org.osgi.framework.*;
import org.osgi.framework.namespace.*;
import org.osgi.framework.wiring.*;
import org.osgi.resource.Namespace;

public class OldStylePluginTests extends CoreTest {
	public static int BUNDLE_LISTENER = 0x01;
	public static int SYNC_BUNDLE_LISTENER = 0x02;
	public static int SIMPLE_RESULTS = 0x04;
	public static final String BUNDLES_ROOT = "bundle_tests";
	private BundleInstaller installer;
	private boolean compatPluginInstalled = false;

	public static Test suite() {
		return new TestSuite(OldStylePluginTests.class);
	}

	protected void setUp() throws Exception {
		compatPluginInstalled = false;
		for (BundleWire hostWire : getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(BundleWiring.class).getProvidedWires(HostNamespace.HOST_NAMESPACE)) {
			if ("org.eclipse.osgi.compatibility.plugins".equals(hostWire.getRequirer().getSymbolicName())) {
				compatPluginInstalled = true;
				break;
			}
		}
		if (compatPluginInstalled) {
			assertTrue("Plugin compatibility fragment not configured.", getContext().getServiceReference(PluginConverter.class) != null);
		}
		installer = new BundleInstaller(BUNDLES_ROOT, OSGiTestsActivator.getContext());
		installer.refreshPackages(null);
	}

	protected void tearDown() throws Exception {
		installer.shutdown();
		installer = null;
	}

	public BundleContext getContext() {
		return OSGiTestsActivator.getContext();
	}

	public void testSimplePlugin() {
		if (!compatPluginInstalled) {
			return;
		}
		String pluginName = "test.plugins.a";
		Version pluginVersion = Version.parseVersion("1.0.0");
		Bundle bundle = installPlugin(pluginName, false);
		BundleRevision revision = bundle.adapt(BundleRevision.class);
		assertIdentity(revision, pluginName, pluginVersion, false);
		assertTrue("Did not expect any requirements.", revision.getDeclaredRequirements(null).isEmpty());
	}

	public void testImportPlugin() {
		if (!compatPluginInstalled) {
			return;
		}
		String pluginName = "test.plugins.b";
		Version pluginVersion = Version.parseVersion("1.0.0");
		Bundle bundle = installPlugin(pluginName, false);
		BundleRevision revision = bundle.adapt(BundleRevision.class);
		assertIdentity(revision, pluginName, pluginVersion, false);

		Collection<BundleRequirement> requirements = revision.getDeclaredRequirements(null);
		assertEquals("Wrong number of requirements: " + requirements, 7, requirements.size());
		int index = 0;
		for (BundleRequirement req : requirements) {
			index++;
			Map<String, Object> matchAttributes = new HashMap<String, Object>();
			Map<String, Object> noMatchAttributes = new HashMap<String, Object>();

			matchAttributes.put(BundleNamespace.BUNDLE_NAMESPACE, "x" + index);
			noMatchAttributes.put(BundleNamespace.BUNDLE_NAMESPACE, "x" + index);

			Version match = null;
			Version noMatch = null;
			switch (index) {
				case 1 :
					match = Version.parseVersion("1.0.0");
					noMatch = Version.parseVersion("1.0.0.x");
					break;
				case 2 :
					match = Version.parseVersion("1.0.100");
					noMatch = Version.parseVersion("1.1");
					break;
				case 4 :
					match = Version.parseVersion("90000");
					noMatch = Version.parseVersion("0.1");
					break;
				default :
					match = Version.parseVersion("1.1");
					noMatch = Version.parseVersion("2.0");
					break;
			}

			matchAttributes.put(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, match);
			noMatchAttributes.put(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, noMatch);
			Filter f = null;
			try {
				f = getContext().createFilter(req.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
			} catch (InvalidSyntaxException e) {
				fail("Failed to create filter.", e);
			}
			assertTrue("Did not match requirement: " + req + ": against: " + matchAttributes, f.matches(matchAttributes));
			assertFalse("Unexpected match requirement: " + req + ": against: " + matchAttributes, f.matches(noMatchAttributes));

			String resolution = req.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
			if (resolution == null) {
				resolution = Namespace.RESOLUTION_MANDATORY;
			}
			String visibility = req.getDirectives().get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE);
			if (visibility == null) {
				visibility = BundleNamespace.VISIBILITY_PRIVATE;
			}

			if (index == 6) {
				assertEquals("Wrong visibility: " + req, BundleNamespace.VISIBILITY_REEXPORT, visibility);
			} else {
				assertEquals("Wrong visibility: " + req, BundleNamespace.VISIBILITY_PRIVATE, visibility);
			}
			if (index == 7) {
				assertEquals("Wrong resolution: " + req, Namespace.RESOLUTION_OPTIONAL, resolution);
			} else {
				assertEquals("Wrong resolution: " + req, Namespace.RESOLUTION_MANDATORY, resolution);
			}
		}
	}

	public void testExportSinglePackage() {
		if (!compatPluginInstalled) {
			return;
		}
		String pluginName = "test.plugins.c";
		Version pluginVersion = Version.parseVersion("1.0.0");
		Bundle bundle = installPlugin(pluginName, false);
		BundleRevision revision = bundle.adapt(BundleRevision.class);
		assertIdentity(revision, pluginName, pluginVersion, false);
		assertTrue("Did not expect any requirements.", revision.getDeclaredRequirements(null).isEmpty());

		Collection<BundleCapability> packages = revision.getDeclaredCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of exports: " + packages, 1, packages.size());
		assertEquals("Wrong package name.", pluginName + ".exported", packages.iterator().next().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
	}

	public void testExportAllPackage() {
		if (!compatPluginInstalled) {
			return;
		}
		String pluginName = "test.plugins.d";
		Version pluginVersion = Version.parseVersion("1.0.0");
		Bundle bundle = installPlugin(pluginName, false);
		BundleRevision revision = bundle.adapt(BundleRevision.class);
		assertIdentity(revision, pluginName, pluginVersion, false);
		assertTrue("Did not expect any requirements.", revision.getDeclaredRequirements(null).isEmpty());

		Collection<BundleCapability> packages = revision.getDeclaredCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of exports: " + packages, 4, packages.size());
		Iterator<BundleCapability> iPackages = packages.iterator();
		// Note that '.' is exported because scaning for * found a resource (plugin.xml) in root of the bundle.
		assertEquals("Wrong package name.", ".", iPackages.next().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		assertEquals("Wrong package name.", pluginName, iPackages.next().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		assertEquals("Wrong package name.", pluginName + ".exported", iPackages.next().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		assertEquals("Wrong package name.", pluginName + ".exported.sub", iPackages.next().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
	}

	public void testActivator() {
		if (!compatPluginInstalled) {
			return;
		}
		String pluginName = "test.plugins.e";
		Version pluginVersion = Version.parseVersion("1.0.0");
		Bundle bundle = installPlugin(pluginName, false);
		BundleRevision revision = bundle.adapt(BundleRevision.class);
		assertIdentity(revision, pluginName, pluginVersion, false);

		try {
			bundle.start();
			Collection<ServiceReference<Object>> testServices = getContext().getServiceReferences(Object.class, "(" + Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE + "=" + bundle.getSymbolicName() + ")");
			assertFalse("Did not find registered service.", testServices.isEmpty());
		} catch (BundleException e) {
			fail("Failed to start bundle.", e);
		} catch (InvalidSyntaxException e) {
			fail("Failed to create filter.", e);
		}
	}

	public void testExtensionSingleton() {
		if (!compatPluginInstalled) {
			return;
		}
		String pluginName = "test.plugins.f";
		Version pluginVersion = Version.parseVersion("1.0.0");
		Bundle bundle = installPlugin(pluginName, false);
		BundleRevision revision = bundle.adapt(BundleRevision.class);
		assertIdentity(revision, pluginName, pluginVersion, true);
	}

	public void testFragment() {
		if (!compatPluginInstalled) {
			return;
		}
		String pluginName = "test.plugins.g";
		Version pluginVersion = Version.parseVersion("1.0.0");
		Bundle bundle = installPlugin(pluginName, false);
		BundleRevision revision = bundle.adapt(BundleRevision.class);
		assertIdentity(revision, pluginName, pluginVersion, false);
		assertTrue("revision is not a fragment.", (revision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0);
		Collection<BundleRequirement> reqs = revision.getDeclaredRequirements(HostNamespace.HOST_NAMESPACE);
		assertEquals("Wrong number of host requirements.", 1, reqs.size());
		BundleRequirement req = reqs.iterator().next();

		Map<String, Object> matchAttributes = new HashMap<String, Object>();
		Map<String, Object> noMatchAttributes = new HashMap<String, Object>();

		matchAttributes.put(HostNamespace.HOST_NAMESPACE, "x");
		noMatchAttributes.put(HostNamespace.HOST_NAMESPACE, "x");

		Version match = Version.parseVersion("1.1");
		Version noMatch = Version.parseVersion("2.0");

		matchAttributes.put(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, match);
		noMatchAttributes.put(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, noMatch);
		Filter f = null;
		try {
			f = getContext().createFilter(req.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
		} catch (InvalidSyntaxException e) {
			fail("Failed to create filter.", e);
		}
		assertTrue("Did not match requirement: " + req + ": against: " + matchAttributes, f.matches(matchAttributes));
		assertFalse("Unexpected match requirement: " + req + ": against: " + matchAttributes, f.matches(noMatchAttributes));

	}

	private void assertIdentity(BundleRevision revision, String pluginName, Version pluginVersion, boolean isSingleton) {
		assertEquals("Wrong symbolic name", pluginName, revision.getSymbolicName());
		assertEquals("Wrong version.", pluginVersion, revision.getVersion());
		Collection<BundleCapability> capabilities = new ArrayList<BundleCapability>();
		capabilities.addAll(revision.getDeclaredCapabilities(IdentityNamespace.IDENTITY_NAMESPACE));
		capabilities.addAll(revision.getDeclaredCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
		capabilities.addAll(revision.getDeclaredCapabilities(HostNamespace.HOST_NAMESPACE));
		int expectedNumCaps = (revision.getTypes() & BundleRevision.TYPE_FRAGMENT) == 0 ? 3 : 1;
		assertEquals("Wrong number of capabilities: " + capabilities, expectedNumCaps, capabilities.size());
		for (BundleCapability cap : capabilities) {
			String namespace = cap.getNamespace();
			String versionKey = IdentityNamespace.IDENTITY_NAMESPACE.equals(namespace) ? IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE : AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
			Map<String, Object> attrs = cap.getAttributes();
			assertEquals("Wrong name: " + namespace, pluginName, attrs.get(namespace));
			assertEquals("Wrong version: " + namespace, pluginVersion, attrs.get(versionKey));
			String singletonDirective = cap.getDirectives().get(IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE);
			singletonDirective = singletonDirective == null ? Boolean.FALSE.toString() : singletonDirective;
			assertEquals("Wrong singleton directive.", isSingleton, Boolean.TRUE.toString().equals(singletonDirective));
		}
	}

	private Bundle installPlugin(String plugin, boolean expectedFailure) {
		try {
			return installer.installBundle(plugin);
		} catch (BundleException e) {
			if (!expectedFailure) {
				fail("Failed to install plugin.", e);
			}
			return null;
		}
	}
}