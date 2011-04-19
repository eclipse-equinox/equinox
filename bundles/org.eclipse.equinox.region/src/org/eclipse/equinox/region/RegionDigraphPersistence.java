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

package org.eclipse.equinox.region;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A region digraph persistence is used to persist the state of a {@link RegionDigraph}.
 * <p />
 * <strong>Concurrent Semantics</strong><br />
 * 
 * Implementations of this interface must be thread safe.
 */
public interface RegionDigraphPersistence {

	/**
	 * Creates a new digraph and reads the content of the digraph from the provided input. The provided input must have
	 * been persisted using the {@link #save(RegionDigraph, OutputStream)} method.
	 * <p />
	 * Note that the returned digraph is disconnected from the OSGi runtime. Any modifications made to the returned
	 * digraph will not affect the OSGi runtime behavior of the bundles installed in the running framework.
	 * <p />
	 * The specified stream remains open after this method returns.
	 * 
	 * @param input an input stream to read a digraph from.
	 * @return the new digraph
	 * @throws IOException if error occurs reading the digraph.
	 * @throws IllegalArgumentException if the input stream is not a digraph or has an incompatible persistent version
	 */
	RegionDigraph load(InputStream input) throws IOException;

	/**
	 * Writes the specified {@link RegionDigraph} to the provided output in a form suitable for using the
	 * {@link #load(InputStream)} method.
	 * <p />
	 * After the digraph has been written, the output stream is flushed. The output stream remains open after this
	 * method returns.
	 * 
	 * @param digraph a digraph to be written.
	 * @param output an output stream to write a digraph to.
	 * @throws IOException if error occurs writing the digraph.
	 */
	void save(RegionDigraph digraph, OutputStream output) throws IOException;
}
