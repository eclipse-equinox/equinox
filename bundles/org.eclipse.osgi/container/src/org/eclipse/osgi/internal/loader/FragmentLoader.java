/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
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
package org.eclipse.osgi.internal.loader;

import java.net.URL;
import java.util.*;
import org.eclipse.osgi.container.ModuleLoader;
import org.eclipse.osgi.container.ModuleRevision;

public class FragmentLoader extends ModuleLoader {

	@Override
	protected List<URL> findEntries(String path, String filePattern, int options) {
		return Collections.emptyList();
	}

	@Override
	protected Collection<String> listResources(String path, String filePattern, int options) {
		return Collections.emptyList();
	}

	@Override
	protected ClassLoader getClassLoader() {
		return null;
	}

	@Override
	protected boolean getAndSetTrigger() {
		// nothing to do here
		return false;
	}

	@Override
	public boolean isTriggerSet() {
		// nothing to do here
		return false;
	}

	@Override
	protected void loadFragments(Collection<ModuleRevision> fragments) {
		// do nothing
	}

}
