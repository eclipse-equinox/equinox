/*******************************************************************************
 * Copyright (c) 2008, 2009 Heiko Seeberger and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0.
 *
 * Contributors:
 *     Heiko Seeberger - initial implementation
 *     Stefan Winkler - added capacity constant
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching;

/**
 * Constants for org.eclipse.equinox.weaving.caching.
 *
 * @author Heiko Seeberger
 */
public interface IBundleConstants {

	/**
	 * The symbolic name for this bundle: "org.eclipse.equinox.weaving.caching".
	 */
	public static final String BUNDLE_SYMBOLIC_NAME = "org.eclipse.equinox.weaving.caching"; //$NON-NLS-1$

	/**
	 * The capacity of the writer queue and lock map.
	 */
	public static final int QUEUE_CAPACITY = 5000;
}
