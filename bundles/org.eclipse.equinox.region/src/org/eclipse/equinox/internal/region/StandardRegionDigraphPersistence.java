/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.region;

import java.io.*;
import java.util.*;
import org.eclipse.equinox.region.*;
import org.eclipse.equinox.region.RegionDigraph.FilteredRegion;
import org.osgi.framework.*;

/**
 * 
 * Class used for reading and writing a region digraph to persistent storage.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Thread safe.
 */
final class StandardRegionDigraphPersistence implements RegionDigraphPersistence {

	private static final String PERSISTENT_NAME = "equinox region digraph"; //$NON-NLS-1$

	private static final int PERSISTENT_VERSION = 1;

	static void writeRegionDigraph(DataOutputStream out, RegionDigraph digraph) throws IOException {
		if (!(digraph instanceof StandardRegionDigraph))
			throw new IllegalArgumentException("Only digraphs of type '" + StandardRegionDigraph.class.getName() + "' are allowed: " + digraph.getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
		Map<Region, Set<FilteredRegion>> filteredRegions = ((StandardRegionDigraph) digraph).getFilteredRegions();

		try {
			// write the persistent name and version
			out.writeUTF(PERSISTENT_NAME);
			out.writeInt(PERSISTENT_VERSION);
			// write the number of regions
			out.writeInt(filteredRegions.size());
			// write each region
			for (Region region : filteredRegions.keySet()) {
				writeRegion(out, region);
			}
			// write each edge
			// write number of tail regions
			out.writeInt(filteredRegions.size());
			for (Map.Entry<Region, Set<FilteredRegion>> edges : filteredRegions.entrySet()) {
				// write the number of edges for this tail
				out.writeInt(edges.getValue().size());
				for (FilteredRegion edge : edges.getValue()) {
					writeEdge(out, edges.getKey(), edge.getFilter(), edge.getRegion());
				}
			}
		} finally {
			// note that the output is flushed even on exception
			out.flush();
		}
	}

	private static void writeRegion(DataOutputStream out, Region region) throws IOException {
		// write region name
		out.writeUTF(region.getName());

		Set<Long> ids = region.getBundleIds();
		// write number of bundles
		out.writeInt(ids.size());
		for (Long id : ids) {
			// write each bundle id
			out.writeLong(id);
		}
	}

	private static void writeEdge(DataOutputStream out, Region tail, RegionFilter filter, Region head) throws IOException {
		// write tail region name
		out.writeUTF(tail.getName());
		// write head region name
		out.writeUTF(head.getName());
		// save the sharing policy
		Map<String, Collection<String>> policy = filter.getSharingPolicy();
		// write the number of name spaces
		out.writeInt(policy.size());
		// write each name space policy
		for (Map.Entry<String, Collection<String>> namespace : policy.entrySet()) {
			// write the name space name
			out.writeUTF(namespace.getKey());
			Collection<String> filters = namespace.getValue();
			// write the number of filters
			out.writeInt(filters.size());
			for (String filterSpec : filters) {
				// write each filter
				out.writeUTF(filterSpec);
			}
		}
	}

	static StandardRegionDigraph readRegionDigraph(DataInputStream in, BundleContext bundleContext, ThreadLocal<Region> threadLocal) throws IOException, InvalidSyntaxException, BundleException {
		StandardRegionDigraph digraph = new StandardRegionDigraph(bundleContext, threadLocal);

		// Read and check the persistent name and version
		String persistentName = in.readUTF();
		if (!PERSISTENT_NAME.equals(persistentName)) {
			throw new IllegalArgumentException("Input stream does not represent a digraph"); //$NON-NLS-1$
		}
		int persistentVersion = in.readInt();
		if (PERSISTENT_VERSION != persistentVersion) {
			throw new IllegalArgumentException("Input stream contains a digraph with an incompatible version '" + persistentVersion + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		// read the number of regions
		int numRegions = in.readInt();
		for (int i = 0; i < numRegions; i++) {
			readRegion(in, digraph);
		}
		// read each edge
		// read number of tail regions
		int numTails = in.readInt();
		for (int i = 0; i < numTails; i++) {
			// read the number of edges for this tail
			int numEdges = in.readInt();
			for (int j = 0; j < numEdges; j++) {
				readEdge(in, digraph);
			}
		}

		return digraph;
	}

	private static Region readRegion(DataInputStream in, RegionDigraph digraph) throws IOException, BundleException {
		// read region name
		String name = in.readUTF();
		Region region = digraph.createRegion(name);

		// read number of bundles
		int numIds = in.readInt();
		for (int i = 0; i < numIds; i++) {
			region.addBundle(in.readLong());
		}
		return region;
	}

	private static void readEdge(DataInputStream in, RegionDigraph digraph) throws IOException, InvalidSyntaxException, BundleException {
		// read tail region name
		String tailName = in.readUTF();
		Region tail = digraph.getRegion(tailName);
		if (tail == null)
			throw new IOException("Could not find tail region: " + tailName); //$NON-NLS-1$
		// read head region name
		String headName = in.readUTF();
		Region head = digraph.getRegion(headName);
		if (head == null)
			throw new IOException("Could not find head region: " + headName); //$NON-NLS-1$
		// read the sharing policy
		RegionFilterBuilder builder = digraph.createRegionFilterBuilder();
		// read the number of name spaces
		int numSpaces = in.readInt();
		// read each name space policy
		for (int i = 0; i < numSpaces; i++) {
			// read the name space name
			String namespace = in.readUTF();
			// read the number of filters
			int numFilters = in.readInt();
			for (int j = 0; j < numFilters; j++) {
				String filter = in.readUTF();
				builder.allow(namespace, filter);
			}
		}
		digraph.connect(tail, builder.build(), head);
	}

	/** 
	 * {@inheritDoc}
	 */
	public RegionDigraph load(InputStream input) throws IOException {
		try {
			return readRegionDigraph(new DataInputStream(input), null, null);
		} catch (InvalidSyntaxException e) {
			// This should never happen since the filters were valid on save
			// propagate as IllegalStateException
			throw new IllegalStateException("Internal error reading a filter", e); //$NON-NLS-1$
		} catch (BundleException e) {
			// This should never happen since the digraph was valid on save
			// propagate as IllegalStateException
			throw new IllegalStateException("Internal error creating the digraph", e); //$NON-NLS-1$
		}
	}

	/** 
	 * {@inheritDoc}
	 */
	public void save(RegionDigraph digraph, OutputStream output) throws IOException {
		writeRegionDigraph(new DataOutputStream(output), digraph);
	}
}
