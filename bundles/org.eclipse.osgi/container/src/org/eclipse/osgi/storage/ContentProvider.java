/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.storage;

import java.io.File;
import org.osgi.framework.BundleException;

/**
 * A content provider is a marker interface that is used but the framework
 * internally to handle different kinds of bundles. For example, reference
 * installed bundles or connect bundles. The type of the provider indicates how
 * the framework will handle the install or update of the bundle content.
 */
public interface ContentProvider {

	/**
	 * The type of the provided content
	 */
	public enum Type {
		REFERENCE, CONNECT, DEFAULT;
	}

	/**
	 * A file of the content, may be {@code null}
	 * 
	 * @return the file, may be {@code null}
	 * @throws BundleException
	 */
	File getContent() throws BundleException;

	/**
	 * The type of content
	 * 
	 * @return the type of content
	 */
	Type getType();
}
