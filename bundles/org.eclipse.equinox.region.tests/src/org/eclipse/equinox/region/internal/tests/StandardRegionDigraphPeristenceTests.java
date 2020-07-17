/*******************************************************************************
 * Copyright (c) 2011, 2020 VMware Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SpringSource, a division of VMware - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.equinox.region.internal.tests;

import java.io.*;
import java.util.*;
import org.eclipse.equinox.region.*;
import org.eclipse.equinox.region.RegionDigraph.FilteredRegion;
import org.eclipse.virgo.teststubs.osgi.framework.StubBundle;
import org.eclipse.virgo.teststubs.osgi.framework.StubBundleContext;
import org.junit.*;
import org.osgi.framework.*;

public class StandardRegionDigraphPeristenceTests {

	private RegionDigraph digraph;

	private RegionDigraphPersistence persistence;

	private StubBundleContext systemBundleContext;

	private ThreadLocal<Region> threadLocal;

	private static final String BOOT_REGION = "boot";

	private static final Collection<String> regionNames = Arrays.asList("r0", "r1", "r2", "r3");

	@Before
	public void setUp() throws Exception {
		StubBundle stubSystemBundle = new StubBundle(0L, "osgi.framework", new Version("0"), "loc");
		systemBundleContext = (StubBundleContext) stubSystemBundle.getBundleContext();
		systemBundleContext.addInstalledBundle(stubSystemBundle);
		threadLocal = new ThreadLocal<Region>();
		this.digraph = RegionReflectionUtils.newStandardRegionDigraph(systemBundleContext, threadLocal);
		this.persistence = digraph.getRegionDigraphPersistence();
		Region boot = digraph.createRegion(BOOT_REGION);
		boot.addBundle(stubSystemBundle);

		for (String regionName : regionNames) {
			Region region = digraph.createRegion(regionName);
			for (int i = 0; i < 10; i++) {
				String bsn = region.getName() + "." + i;
				StubBundle b = (StubBundle) systemBundleContext.installBundle(bsn);
				systemBundleContext.addInstalledBundle(b);
				region.addBundle(b);
			}
		}
	}

	@Test
	public void testBasic() throws IOException {
		doTest();
	}

	@Test
	public void testSingleConnection() throws InvalidSyntaxException, BundleException, IOException {
		Region tail = null;
		// create a single connection between each region
		for (Region head : digraph) {
			if (tail != null) {
				String name = head.getName();
				tail.connectRegion(head, createFilter(name + "A", name + "B", name + "C"));
			}
			tail = head;
		}
		doTest();
	}

	@Test
	public void testMultiConnection() throws BundleException, InvalidSyntaxException, IOException {
		List<Region> tails = new ArrayList<Region>();
		// create multiple connections between each region
		for (Region head : digraph) {
			for (Region tail : tails) {
				String name = head.getName();
				tail.connectRegion(head, createFilter(name + "A", name + "B", name + "C"));
			}
			tails.add(head);
		}
		doTest();
	}

	@Test
	public void testMultiConnectionCycle() throws BundleException, InvalidSyntaxException, IOException {
		List<Region> tails = new ArrayList<Region>();
		for (Region region : digraph) {
			tails.add(region);
		}
		// create multiple connections between each region with cycles
		for (Region head : digraph) {
			for (Region tail : tails) {
				if (head == tail)
					continue;
				String name = head.getName();
				tail.connectRegion(head, createFilter(name + "A", name + "B", name + "C"));
			}
		}
		doTest();
	}

	@Test
	public void testInvalidOperations() throws IOException, BundleException {
		Region boot = digraph.getRegion(BOOT_REGION);
		Bundle b = boot.installBundle("dynamic.add.a.1", new ByteArrayInputStream(new byte[0]));
		// needed because we don't have a bundle hook to add it for us
		boot.addBundle(b);
		// needed because StubBundleContext.installBundle does not do this!
		systemBundleContext.addInstalledBundle((StubBundle) b);
		Bundle p = boot.getBundle(b.getSymbolicName(), b.getVersion());
		Assert.assertEquals(b, p);
		// TODO seems testing this will require a reference handler to be present
		// b = boot.installBundle("file:dynamic.add.a.2");
		// boot.addBundle(b); // needed because we don't have a bundle hook to add it for us

		RegionDigraph copy = copy(digraph);
		Region bootCopy = copy.getRegion(BOOT_REGION);
		p = bootCopy.getBundle(b.getSymbolicName(), b.getVersion());
		Assert.assertNull(p);
		try {
			bootCopy.installBundle("dynamic.add.b.1", new ByteArrayInputStream(new byte[0]));
		} catch (BundleException e) {
			// expected
		}
		try {
			bootCopy.installBundleAtLocation("dynamic.add.b.1", new ByteArrayInputStream(new byte[0]));
		} catch (BundleException e) {
			// expected
		}
		try {
			bootCopy.installBundle("dynamic.add.b.2");
		} catch (BundleException e) {
			// expected
		}

	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidPersistentName() throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(output);
		dataOut.writeUTF("test");
		dataOut.close();
		byte[] byteArray = output.toByteArray();
		readDigraph(byteArray);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidPersistentVersion() throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(output);
		dataOut.writeUTF("virgo region digraph");
		dataOut.writeInt(-1);
		dataOut.close();
		byte[] byteArray = output.toByteArray();
		readDigraph(byteArray);
	}

	private void readDigraph(byte[] byteArray) throws IOException {
		ByteArrayInputStream input = new ByteArrayInputStream(byteArray);
		try {
			persistence.load(input);
		} finally {
			input.close();
		}
	}

	private void doTest() throws IOException {
		// test a single write
		doTest(1);
		// test writing and reading the digraph multiple times to same stream
		doTest(10);
	}

	private RegionDigraph copy(RegionDigraph toCopy) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		persistence.save(digraph, output);
		output.close();

		InputStream input = new ByteArrayInputStream(output.toByteArray());
		RegionDigraph copy = persistence.load(input);
		input.close();
		return copy;
	}

	private void doTest(int iterations) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		for (int i = 0; i < iterations; i++) {
			persistence.save(digraph, output);
		}
		output.close();

		InputStream input = new ByteArrayInputStream(output.toByteArray());
		for (int i = 0; i < iterations; i++) {
			RegionDigraph copy = persistence.load(input);
			assertEquals(digraph, copy);
		}
		input.close();
	}

	private RegionFilter createFilter(String... input) throws InvalidSyntaxException {
		RegionFilterBuilder builder = digraph.createRegionFilterBuilder();
		for (String param : input) {
			builder.allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(bundle-symbolic-name=" + param + ")");
			builder.allow(RegionFilter.VISIBLE_HOST_NAMESPACE, "(" + RegionFilter.VISIBLE_HOST_NAMESPACE + "=" + param + ")");
			builder.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=" + param + ")");
			builder.allow(RegionFilter.VISIBLE_REQUIRE_NAMESPACE, "(" + RegionFilter.VISIBLE_REQUIRE_NAMESPACE + "=" + param + ")");
			builder.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(" + Constants.OBJECTCLASS + "=" + param + ")");
		}
		return builder.build();
	}

	static void assertEquals(RegionDigraph d1, RegionDigraph d2) {
		int rCnt1 = countRegions(d1);
		int rCnt2 = countRegions(d2);
		Assert.assertEquals(rCnt1, rCnt2);
		for (Region r1 : d1) {
			Region r2 = d2.getRegion(r1.getName());
			assertEquals(r1, r2);
		}
	}

	static int countRegions(RegionDigraph digraph) {
		return digraph.getRegions().size();
	}

	static void assertEquals(Region r1, Region r2) {
		Assert.assertNotNull(r1);
		Assert.assertNotNull(r2);
		Assert.assertEquals("Wrong name", r1.getName(), r2.getName());
		Set<Long> r1IDs = r1.getBundleIds();
		Set<Long> r2IDs = r2.getBundleIds();
		Assert.assertEquals(r1IDs.size(), r2IDs.size());
		for (Long id : r1IDs) {
			Assert.assertTrue("Missing id: " + id, r2IDs.contains(id));
		}
		assertEquals(r1.getEdges(), r2.getEdges());
	}

	static void assertEquals(Set<FilteredRegion> edges1, Set<FilteredRegion> edges2) {
		Assert.assertEquals(edges1.size(), edges2.size());
		Map<String, RegionFilter> edges2Map = new HashMap<String, RegionFilter>();
		for (FilteredRegion edge2 : edges2) {
			edges2Map.put(edge2.getRegion().getName(), edge2.getFilter());
		}
		for (FilteredRegion edge1 : edges1) {
			RegionFilter filter2 = edges2Map.get(edge1.getRegion().getName());
			Assert.assertNotNull("No filter found: " + edge1.getRegion().getName(), filter2);
			Assert.assertEquals(edge1.getFilter().getSharingPolicy(), filter2.getSharingPolicy());
		}
	}
}
