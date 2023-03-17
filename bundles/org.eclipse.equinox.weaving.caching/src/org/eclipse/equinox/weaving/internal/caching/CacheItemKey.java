/*******************************************************************************
 * Copyright (c) 2023 Stefan Winkler and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0.
 *
 * Contributors:
 *     Stefan Winkler - initial implementation
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching;

import java.util.Objects;

/**
 * A key to find/access {@link CacheItem}s in the lookup map of classes queued
 * for writing.
 *
 * The key consists of the target cache directory and classname of the class
 * file to be written. We use this composed key instead of concatenating the
 * strings for performance reasons, as this saves us the otherwise necessary
 * String concatenation.
 */
public final class CacheItemKey {
	/**
	 * The String representation of the target cache directory
	 */
	public final String directory;
	/**
	 * The class name.
	 */
	public final String name;

	/**
	 * Create an instance of the {@link CacheItemKey} for the given directory and
	 * class name
	 *
	 * @param dir  The String representation of the target cache directory
	 * @param name The class name
	 */
	public CacheItemKey(final String dir, final String name) {
		this.directory = dir;
		this.name = name;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof CacheItemKey)) {
			return false;
		}
		final CacheItemKey other = (CacheItemKey) obj;
		return Objects.equals(other.name, name) && Objects.equals(other.directory, directory);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, directory);
	}
}
