/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
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
package org.eclipse.osgi.internal.loader.buddy;

import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

public interface IBuddyPolicy {
	public Class<?> loadClass(String name);

	public URL loadResource(String name);

	public Enumeration<URL> loadResources(String name);

	default public void addListResources(Set<String> results, String path, String filePattern, int options) {
		// nothing by default
	}
}
