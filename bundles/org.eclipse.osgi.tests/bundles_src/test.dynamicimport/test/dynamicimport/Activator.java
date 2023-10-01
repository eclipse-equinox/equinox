/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package test.dynamicimport;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		try {
			Class.forName("org.osgi.framework.hooks.resolver.ResolverHook");
			throw new RuntimeException("Should have failed to dynamically import the package.");
		} catch (ClassNotFoundException e) {
			// expected because dynamic imports should be filtered out
		}
	}

	public void stop(BundleContext context) throws Exception {
		// nothing
	}

}
