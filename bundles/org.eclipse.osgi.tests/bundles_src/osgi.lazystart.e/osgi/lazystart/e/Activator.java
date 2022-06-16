/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package osgi.lazystart.e;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import osgi.lazystart.d.DTest;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		System.out.println(new DTest());
	}

	public void stop(BundleContext context) throws Exception {
		// nothing
	}

}
