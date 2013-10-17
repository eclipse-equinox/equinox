/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.uninstall.start2;

import org.osgi.framework.*;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		for (Bundle b : context.getBundles()) {
			if ("test.uninstall.start1".equals(b.getSymbolicName())) {
				b.uninstall();
			}
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// nothing
	}

}
