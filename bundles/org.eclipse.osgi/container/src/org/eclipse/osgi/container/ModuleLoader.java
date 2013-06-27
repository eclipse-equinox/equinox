/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container;

import java.net.URL;
import java.util.Collection;
import java.util.List;

/**
 * @since 3.10
 */
public interface ModuleLoader {
	/**
	 * 
	 * @param path
	 * @param filePattern
	 * @param options
	 * @return TODO
	 * @see ModuleWiring#findEntries(String, String, int)
	 */
	public List<URL> findEntries(String path, String filePattern, int options);

	/**
	 * 
	 * @param path
	 * @param filePattern
	 * @param options
	 * @return TODO
	 * @see ModuleWiring#listResources(String, String, int)
	 */
	public Collection<String> listResources(String path, String filePattern, int options);

	/**
	 * 
	 * @return TODO
	 */
	public ClassLoader getClassLoader();

	public boolean getAndSetTrigger();

	public boolean isTriggerSet();

	public void loadFragments(Collection<ModuleRevision> fragments);
}
