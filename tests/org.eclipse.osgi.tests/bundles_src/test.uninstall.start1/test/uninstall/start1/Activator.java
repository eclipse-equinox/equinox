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
package test.uninstall.start1;

import org.osgi.framework.*;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		for (Bundle b : context.getBundles()) {
			if ("test.uninstall.start2".equals(b.getSymbolicName())) {
				b.uninstall();
			}
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// nothing
	}

}
