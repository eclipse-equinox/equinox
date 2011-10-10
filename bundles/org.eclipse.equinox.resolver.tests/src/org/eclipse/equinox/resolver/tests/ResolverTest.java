/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.resolver.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.osgi.service.resolver.extras.DescriptionReference;
import org.eclipse.osgi.service.resolver.extras.SpecificationReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.Wire;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.resolver.Environment;
import org.osgi.service.resolver.Resolver;

public class ResolverTest extends TestCase {

	private Resolver resolver;
	private ServiceReference<Resolver> resolverRef;
	private PlatformAdmin platformAdmin;
	private ServiceReference<PlatformAdmin> platformAdminRef;

	protected void setUp() throws Exception {
		Activator.getBundle(Activator.BUNDLE_RESOLVER).start();
		resolverRef = Activator.getBundleContext().getServiceReference(Resolver.class);
		resolver = Activator.getBundleContext().getService(resolverRef);
		assertNotNull("No resolver service.", resolver); //$NON-NLS-1$
		platformAdminRef = Activator.getBundleContext().getServiceReference(PlatformAdmin.class);
		platformAdmin = Activator.getBundleContext().getService(platformAdminRef);
		assertNotNull("No platformadmin service.", resolver); //$NON-NLS-1$
	}

	protected void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(resolverRef);
		Activator.getBundleContext().ungetService(platformAdminRef);
		Activator.getBundle(Activator.BUNDLE_RESOLVER).stop();
	}

	private Map<String, Map<String, List<BaseDescription>>> getRepository(List<? extends BundleRevision> bundles) {
		Map<String, Map<String, List<BaseDescription>>> repository = new HashMap<String, Map<String,List<BaseDescription>>>();
		for (BundleRevision revision : bundles) {
			List<BundleCapability> capabilities = revision.getDeclaredCapabilities(null);
			for (BundleCapability capability : capabilities) {
				BaseDescription description = ((DescriptionReference) capability).getDescription();
				Map<String, List<BaseDescription>> namespace = repository.get(capability.getNamespace());
				if (namespace == null) {
					namespace = new HashMap<String, List<BaseDescription>>(2);
					repository.put(capability.getNamespace(), namespace);
				}
				List<BaseDescription> repoCapabilities = namespace.get(description.getName());
				if (repoCapabilities == null) {
					repoCapabilities = new ArrayList<BaseDescription>(2);
					namespace.put(description.getName(), repoCapabilities);
				}
				repoCapabilities.add(description);
				if (repoCapabilities.size() > 1) {
					Collections.sort(repoCapabilities, new Comparator<BaseDescription>() {
						public int compare(BaseDescription d1, BaseDescription d2) {
								String systemBundle = "org.eclipse.osgi";
								if (systemBundle.equals(d1.getSupplier().getSymbolicName()) && !systemBundle.equals(d2.getSupplier().getSymbolicName()))
									return -1;
								else if (!systemBundle.equals(d1.getSupplier().getSymbolicName()) && systemBundle.equals(d2.getSupplier().getSymbolicName()))
									return 1;
							if (d1.getSupplier().isResolved() != d2.getSupplier().isResolved())
								return d1.getSupplier().isResolved() ? -1 : 1;
							int versionCompare = -(d1.getVersion().compareTo(d2.getVersion()));
							if (versionCompare != 0)
								return versionCompare;
							return d1.getSupplier().getBundleId() <= d2.getSupplier().getBundleId() ? -1 : 1;
						}
					});
				}
			}
		}
		return repository;
	}

	@SuppressWarnings("unchecked")
	private void getRequirementsRevsionsWiring(List<BundleRevision> revisions, Map<Resource, List<Wire>> wiring) {
		Bundle[] bundles = Activator.getBundleContext().getBundles();
		bundlesLoop: for (Bundle bundle : bundles) {
			BundleRevision revision = bundle.adapt(BundleRevision.class);
			BundleDescription description = (BundleDescription) revision;
			if (!description.isResolved()) {
				// check the ee
				description.getContainingState().resolve(new BundleDescription[] {description}, false);
				ResolverError[] errors = description.getContainingState().getResolverErrors(description);
				for (ResolverError resolverError : errors) {
					if (resolverError.getType() == ResolverError.PLATFORM_FILTER)
						continue bundlesLoop;
				}
			}
			revisions.add(revision);
			BundleWiring bundleWiring = revision.getWiring();
			if (bundleWiring != null) {
				List<? extends Wire> wires = bundleWiring.getRequiredWires(null);
				wiring.put(revision, (List<Wire>) wires);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Map<Resource, List<Wire>> getWiring(List<BundleDescription> revisions) {
		State state = platformAdmin.getFactory().createState(true);
		for (BundleDescription bundle : revisions) {
			if (bundle.getContainingState() != null)
				throw new IllegalArgumentException("Bundle already in a state: " + bundle);
			state.addBundle(bundle);
		}
		state.resolve();
		Map<Resource, List<Wire>> result = new HashMap<Resource, List<Wire>>();
		for (BundleDescription bundle : revisions) {
			BundleWiring bundleWiring = bundle.getWiring();
			if (bundleWiring != null) {
				List<? extends Wire> wires = bundleWiring.getRequiredWires(null);
				result.put(bundle, (List<Wire>) wires);
			}
		}
		return result;
	}

	private void compareWirings(Map<Resource, List<Wire>> expected,
			Map<Resource, List<Wire>> actual) {
		assertEquals("Wrong number of bundles got resolved.", expected.size(), actual.size());
		for (Map.Entry<Resource, List<Wire>> resourceEntry : expected.entrySet()) {
			List<Wire> expectedWires = resourceEntry.getValue();
			List<Wire> actualWires = actual.get(resourceEntry.getKey());
			assertNotNull("No wires found for: " + resourceEntry.getKey(), actualWires);
			for (Wire expectedWire : expectedWires) {
				boolean found = false;
				for (Wire actualWire : actualWires) {
					boolean sameProvider = expectedWire.getProvider().equals(actualWire.getProvider());
					boolean sameCapability = expectedWire.getCapability().equals(actualWire.getCapability());
					boolean sameRequirer = expectedWire.getRequirer().equals(actualWire.getRequirer());
					boolean sameRequirement = expectedWire.getRequirement().equals(actualWire.getRequirement());
					found = sameProvider && sameCapability && sameRequirer && sameRequirement;
					if (found)
						break;
				}
				if (!found)
					fail("Failed to find expected wire: " + expectedWire);
			}
		}
	}

	public void testResolverService01() {
		List<BundleRevision> revisions = new ArrayList<BundleRevision>();
		Map<Resource, List<Wire>> wiring = new HashMap<Resource, List<Wire>>();
		getRequirementsRevsionsWiring(revisions, wiring);

		Map<String, Map<String, List<BaseDescription>>> repository = getRepository(revisions);
		Map<Resource, List<Wire>> result = resolver.resolve(new TestEnvironment(null, repository), null, revisions);

		System.out.println("Number of bundles currently resolved: " + wiring.size());
		System.out.println("Number of bundles to resolve: " + revisions.size());
		System.out.println("Number of bundles that got resolved: " + result.size());

		compareWirings(wiring, result);
	}

	public void testResolverService02() {
		List<BundleRevision> revisions = new ArrayList<BundleRevision>();
		Map<Resource, List<Wire>> wiring = new HashMap<Resource, List<Wire>>();
		getRequirementsRevsionsWiring(revisions, wiring);

		Map<String, Map<String, List<BaseDescription>>> repository = getRepository(revisions);
		Map<Resource, List<Wire>> result = resolver.resolve(new TestEnvironment(wiring, repository), null, revisions);

		System.out.println("Number of bundles currently resolved: " + wiring.size());
		System.out.println("Number of bundles to resolve: " + revisions.size());
		System.out.println("Number of bundles that got resolved: " + result.size());

		assertEquals("Wrong number of bundles got resolved.", 0, result.size());
	}

	public void testResolverServiceImportPackage() throws BundleException {
		StateObjectFactory factory = platformAdmin.getFactory();
		int id = 0;		
		List<BundleDescription> revisions = new ArrayList<BundleDescription>();
		Hashtable<String, String> manifest = new Hashtable<String, String>();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, "b, c");
		BundleDescription a = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(a);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "b");
		BundleDescription b = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(b);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "c");
		BundleDescription c = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(c);

		Map<String, Map<String, List<BaseDescription>>> repository = getRepository(revisions);
		Map<Resource, List<Wire>> result = resolver.resolve(new TestEnvironment(null, repository), Arrays.asList(a), null);

		Map<Resource, List<Wire>> expectedWiring = getWiring(revisions);
		compareWirings(expectedWiring, result);
		dumpResults(result);
	}

	public void testResolverServiceFragmentHost1() throws BundleException {
		StateObjectFactory factory = platformAdmin.getFactory();
		int id = 0;		
		List<BundleDescription> revisions = new ArrayList<BundleDescription>();
		Hashtable<String, String> manifest = new Hashtable<String, String>();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, "b, c");
		BundleDescription a = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(a);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		BundleDescription b = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(b);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "BFrag");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.FRAGMENT_HOST, "B");
		manifest.put(Constants.EXPORT_PACKAGE, "b");
		BundleDescription bFrag = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(bFrag);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		BundleDescription c = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(c);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "CFrag");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.FRAGMENT_HOST, "C");
		manifest.put(Constants.EXPORT_PACKAGE, "c");
		BundleDescription cFrag = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(cFrag);

		Map<String, Map<String, List<BaseDescription>>> repository = getRepository(revisions);
		Map<Resource, List<Wire>> result = resolver.resolve(new TestEnvironment(null, repository), Arrays.asList(a), null);

		Map<Resource, List<Wire>> expectedWiring = getWiring(revisions);
		compareWirings(expectedWiring, result);
		dumpResults(result);
	}

	public void testResolverServiceFragmentHost2() throws BundleException {
		StateObjectFactory factory = platformAdmin.getFactory();
		int id = 0;		
		List<BundleDescription> revisions = new ArrayList<BundleDescription>();
		Hashtable<String, String> manifest = new Hashtable<String, String>();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, "b.frag, b; v=1");
		BundleDescription a = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(a);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "b; version=1.0; v=1");
		BundleDescription b1 = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(b1);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "2.0");
		manifest.put(Constants.EXPORT_PACKAGE, "b; version=1.0; v=2");
		BundleDescription b2 = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(b2);


		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "BFrag");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.FRAGMENT_HOST, "B; multiple-hosts:=true");
		manifest.put(Constants.EXPORT_PACKAGE, "b.frag; uses:=b");
		BundleDescription bFrag = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(bFrag);


		Map<String, Map<String, List<BaseDescription>>> repository = getRepository(revisions);
		Map<Resource, List<Wire>> result = resolver.resolve(new TestEnvironment(null, repository), Arrays.asList(a), null);

		Map<Resource, List<Wire>> expectedWiring = getWiring(revisions);
		compareWirings(expectedWiring, result);
		dumpResults(result);
	}

	public void testResolverServiceRequireBundle() throws BundleException {
		StateObjectFactory factory = platformAdmin.getFactory();
		int id = 0;		
		List<BundleDescription> revisions = new ArrayList<BundleDescription>();
		Hashtable<String, String> manifest = new Hashtable<String, String>();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "B, C");
		BundleDescription a = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(a);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "b");
		BundleDescription b = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(b);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "c");
		BundleDescription c = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(c);

		Map<String, Map<String, List<BaseDescription>>> repository = getRepository(revisions);
		Map<Resource, List<Wire>> result = resolver.resolve(new TestEnvironment(null, repository), Arrays.asList(a), null);
		
		Map<Resource, List<Wire>> expectedWiring = getWiring(revisions);
		compareWirings(expectedWiring, result);
		dumpResults(result);
	}

	public void testResolverServiceRequireCapability() throws BundleException {
		StateObjectFactory factory = platformAdmin.getFactory();
		int id = 0;		
		List<BundleRevision> revisions = new ArrayList<BundleRevision>();
		Hashtable<String, String> manifest = new Hashtable<String, String>();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_CAPABILITY, "B; filter:=\"(B=b)\", C; filter:=\"(C=c)\"");
		BundleDescription a = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(a);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.PROVIDE_CAPABILITY, "B; B=b");
		BundleDescription b = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(b);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.PROVIDE_CAPABILITY, "C; C=c");
		BundleDescription c = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(c);

		Map<String, Map<String, List<BaseDescription>>> repository = getRepository(revisions);
		Map<Resource, List<Wire>> result = resolver.resolve(new TestEnvironment(null, repository), Arrays.asList(a), null);
		dumpResults(result);
	}


	private void dumpResults(Map<Resource, List<Wire>> result) {
		System.out.println("TestCase: " + getName());
		for (Map.Entry<Resource, List<Wire>> entry : result.entrySet()) {
			System.out.println(" " + entry.getKey());
			for (Wire wire : entry.getValue()) {
				System.out.println("  " + wire);
			}
		}
	}

	public void testResolverPreferenceOrder() throws BundleException {
		StateObjectFactory factory = platformAdmin.getFactory();
		int id = 0;		
		List<BundleDescription> revisions = new ArrayList<BundleDescription>();
		Hashtable<String, String> manifest = new Hashtable<String, String>();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, "b");
		manifest.put(Constants.REQUIRE_BUNDLE, "C");
		manifest.put(Constants.REQUIRE_CAPABILITY, "D; filter:=\"(D=d)\"");
		BundleDescription a = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(a);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "3.0");
		manifest.put(Constants.EXPORT_PACKAGE, "b; version=3.0");
		BundleDescription b3 = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), id++);
		revisions.add(b3);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "2.0");
		manifest.put(Constants.EXPORT_PACKAGE, "b; version=2.0");
		BundleDescription b2 = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), id++);
		revisions.add(b2);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "b; version=1.0");
		BundleDescription b1 = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), id++);
		revisions.add(b1);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "3.0");
		manifest.put(Constants.EXPORT_PACKAGE, "c");
		BundleDescription c3 = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), id++);
		revisions.add(c3);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "2.0");
		manifest.put(Constants.EXPORT_PACKAGE, "c");
		BundleDescription c2 = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), id++);
		revisions.add(c2);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "c");
		BundleDescription c1 = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), id++);
		revisions.add(c1);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D");
		manifest.put(Constants.BUNDLE_VERSION, "3.0");
		manifest.put(Constants.PROVIDE_CAPABILITY, "D; D=d; version:Version=\"3.0\"");
		BundleDescription d3 = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(d3);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D");
		manifest.put(Constants.BUNDLE_VERSION, "2.0");
		manifest.put(Constants.PROVIDE_CAPABILITY, "D; D=d; version:Version=\"2.0\"");
		BundleDescription d2 = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(d2);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.PROVIDE_CAPABILITY, "D; D=d; version:Version=\"1.0\"");
		BundleDescription d1 = factory.createBundleDescription(null, manifest, manifest.get(Constants.BUNDLE_SYMBOLICNAME), id++);
		revisions.add(d1);

		Map<String, Map<String, List<BaseDescription>>> repository = getRepository(revisions);
		Map<Resource, List<Wire>> result = resolver.resolve(new TestEnvironment(null, repository, true), Arrays.asList(a), null);

		Map<Resource, List<Wire>> expectedWiring = getWiring(revisions);
		dumpResults(result);
		compareWirings(expectedWiring, result);
	}

	

	class TestEnvironment implements Environment {
		private final Map<Resource, List<Wire>> wiring;
		private final Map<String, Map<String, List<BaseDescription>>> repository;
		private final boolean reverseCandidates;
		public TestEnvironment(Map<Resource, List<Wire>> wiring, Map<String, Map<String, List<BaseDescription>>> repository) {
			this(wiring, repository, false);
		}

		public TestEnvironment(Map<Resource, List<Wire>> wiring, Map<String, Map<String, List<BaseDescription>>> repository, boolean reverseCandidates) {
			this.wiring = wiring;
			this.repository = repository;
			this.reverseCandidates = reverseCandidates;
		}

		public boolean isEffective(Requirement requirement) throws NullPointerException {
			return true;
		}

		public Map<Resource, List<Wire>> getWiring() {
			if (wiring == null)
				return Collections.emptyMap();
			else
				return wiring;
		}

		public Collection<Capability> findProviders(Requirement requirement) throws NullPointerException {
			List<Capability> result = new ArrayList<Capability>();
			Map<String, List<BaseDescription>> namespace = repository.get(requirement.getNamespace());
			VersionConstraint specification = ((SpecificationReference) requirement).getSpecification();
			String name = specification.getName();
			List<BaseDescription> candidates;
			if (name == null || name.indexOf('*') > -1) {
				candidates = new ArrayList<BaseDescription>();
				for (List<BaseDescription> baseDescriptions : namespace.values()) {
					candidates.addAll(baseDescriptions);
				}
			} else {
				candidates = namespace.get(name);
			}
			if (candidates != null) {
				for (BaseDescription baseDescription : candidates) {
					if (BundleRevision.HOST_NAMESPACE.equals(requirement.getNamespace())) {
						result.addAll(baseDescription.getSupplier().getCapabilities(BundleRevision.HOST_NAMESPACE));
					} else {
						result.add(baseDescription.getCapability());
					}
				}
			}
			if (reverseCandidates)
				Collections.reverse(result);
			return result;
		}
	}
}
