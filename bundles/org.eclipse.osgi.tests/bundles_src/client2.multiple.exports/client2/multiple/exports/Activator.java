/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package client2.multiple.exports;

import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		try {
			AbstractBundleTests.simpleResults.addEvent(Class.forName("host.multiple.exports.PublicClass1").getName()); //$NON-NLS-1$
		} catch (Throwable t) {
			AbstractBundleTests.simpleResults.addEvent(t);
		}
		try {
			AbstractBundleTests.simpleResults.addEvent(Class.forName("host.multiple.exports.PublicClass2").getName()); //$NON-NLS-1$
		} catch (Throwable t) {
			AbstractBundleTests.simpleResults.addEvent(t);
		}

		try {
			AbstractBundleTests.simpleResults.addEvent(Class.forName("host.multiple.exports.PrivateClass1").getName()); //$NON-NLS-1$
		} catch (Throwable t) {
			AbstractBundleTests.simpleResults.addEvent(t);
		}
		try {
			AbstractBundleTests.simpleResults.addEvent(Class.forName("host.multiple.exports.PrivateClass2").getName()); //$NON-NLS-1$
		} catch (Throwable t) {
			AbstractBundleTests.simpleResults.addEvent(t);
		}
	}

	public void stop(BundleContext context) throws Exception {
		// nothing
	}

}
