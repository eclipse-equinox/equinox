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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.*;
import java.util.*;
import org.eclipse.equinox.region.*;
import org.eclipse.equinox.region.RegionDigraph.FilteredRegion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.*;

public class StandardRegionDigraphPeristenceTests {

	private RegionDigraph digraph;

	private RegionDigraphPersistence persistence;

	private BundleContext systemBundleContext;

	private Map<String, Bundle> installedBundles;

	private long bundleIdCounter;

	private ThreadLocal<Region> threadLocal;

	private static final String BOOT_REGION = "boot";

	private static final Collection<String> regionNames = Arrays.asList("r0", "r1", "r2", "r3");

	@BeforeEach
	public void setUp() throws Exception {
		installedBundles = new HashMap<>();
		bundleIdCounter = 1L;
		
		Bundle stubSystemBundle = MockBundleBuilder.createMockBundle(0L, "osgi.framework", new Version("0"), "loc");
		systemBundleContext = mock(BundleContext.class);
		when(systemBundleContext.getBundle(0L)).thenReturn(stubSystemBundle);
		installedBundles.put("osgi.framework", stubSystemBundle);
		
		// Mock bundle installation
		when(systemBundleContext.installBundle(anyString())).thenAnswer(invocation -> {
			String location = invocation.getArgument(0);
			Bundle bundle = MockBundleBuilder.createMockBundle(bundleIdCounter++, location, new Version("1.0.0"), location);
			installedBundles.put(location, bundle);
			return bundle;
		});
		
		threadLocal = new ThreadLocal<>();
		this.digraph = RegionReflectionUtils.newStandardRegionDigraph(systemBundleContext, threadLocal);
		this.persistence = digraph.getRegionDigraphPersistence();
		Region boot = digraph.createRegion(BOOT_REGION);
		boot.addBundle(stubSystemBundle);

		for (String regionName : regionNames) {
			Region region = digraph.createRegion(regionName);
			for (int i = 0; i < 10; i++) {
				String bsn = region.getName() + "." + i;
				Bundle b = systemBundleContext.installBundle(bsn);
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
		List<Region> tails = new ArrayList<>();
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
		List<Region> tails = new ArrayList<>();
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
		Bundle p = boot.getBundle(b.getSymbolicName(), b.getVersion());
		assertEquals(b, p);
		// TODO seems testing this will require a reference handler to be present
		// b = boot.installBundle("file:dynamic.add.a.2");
		// boot.addBundle(b); // needed because we don't have a bundle hook to add it
		// for us

		RegionDigraph copy = copy(digraph);
		Region bootCopy = copy.getRegion(BOOT_REGION);
		p = bootCopy.getBundle(b.getSymbolicName(), b.getVersion());
		assertNull(p);
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

	@Test
	public void testInvalidPersistentName() {
		assertThrows(IllegalArgumentException.class, () -> {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(output);
			dataOut.writeUTF("test");
			dataOut.close();
			byte[] byteArray = output.toByteArray();
			readDigraph(byteArray);
		});
	}

	@Test
	public void testInvalidPersistentVersion() {
		assertThrows(IllegalArgumentException.class, () -> {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(output);
			dataOut.writeUTF("virgo region digraph");
			dataOut.writeInt(-1);
			dataOut.close();
			byte[] byteArray = output.toByteArray();
			readDigraph(byteArray);
		});
	}

	private void readDigraph(byte[] byteArray) throws IOException {
		try (ByteArrayInputStream input = new ByteArrayInputStream(byteArray)) {
			persistence.load(input);
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
			assertDigraphEquals(digraph, copy);
		}
		input.close();
	}

	@SuppressWarnings("deprecation") // VISIBLE_SERVICE_NAMESPACE
	private RegionFilter createFilter(String... input) throws InvalidSyntaxException {
		RegionFilterBuilder builder = digraph.createRegionFilterBuilder();
		for (String param : input) {
			builder.allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(bundle-symbolic-name=" + param + ")");
			builder.allow(RegionFilter.VISIBLE_HOST_NAMESPACE,
					"(" + RegionFilter.VISIBLE_HOST_NAMESPACE + "=" + param + ")");
			builder.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE,
					"(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=" + param + ")");
			builder.allow(RegionFilter.VISIBLE_REQUIRE_NAMESPACE,
					"(" + RegionFilter.VISIBLE_REQUIRE_NAMESPACE + "=" + param + ")");
			builder.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(" + Constants.OBJECTCLASS + "=" + param + ")");
		}
		return builder.build();
	}

	static void assertDigraphEquals(RegionDigraph d1, RegionDigraph d2) {
		int rCnt1 = countRegions(d1);
		int rCnt2 = countRegions(d2);
		assertEquals(rCnt1, rCnt2);
		for (Region r1 : d1) {
			Region r2 = d2.getRegion(r1.getName());
			assertRegionEquals(r1, r2);
		}
	}

	static int countRegions(RegionDigraph digraph) {
		return digraph.getRegions().size();
	}

	static void assertRegionEquals(Region r1, Region r2) {
		assertNotNull(r1);
		assertNotNull(r2);
		assertEquals("Wrong name", r1.getName(), r2.getName());
		Set<Long> r1IDs = r1.getBundleIds();
		Set<Long> r2IDs = r2.getBundleIds();
		assertEquals(r1IDs.size(), r2IDs.size());
		for (Long id : r1IDs) {
			assertTrue("Missing id: " + id, r2IDs.contains(id));
		}
		assertEdgesEquals(r1.getEdges(), r2.getEdges());
	}

	static void assertEdgesEquals(Set<FilteredRegion> edges1, Set<FilteredRegion> edges2) {
		assertEquals(edges1.size(), edges2.size());
		Map<String, RegionFilter> edges2Map = new HashMap<>();
		for (FilteredRegion edge2 : edges2) {
			edges2Map.put(edge2.getRegion().getName(), edge2.getFilter());
		}
		for (FilteredRegion edge1 : edges1) {
			RegionFilter filter2 = edges2Map.get(edge1.getRegion().getName());
			assertNotNull("No filter found: " + edge1.getRegion().getName(), filter2);
			assertEquals(edge1.getFilter().getSharingPolicy(), filter2.getSharingPolicy());
		}
	}
}
