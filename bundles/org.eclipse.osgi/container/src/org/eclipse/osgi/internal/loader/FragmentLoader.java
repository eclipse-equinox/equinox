/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
