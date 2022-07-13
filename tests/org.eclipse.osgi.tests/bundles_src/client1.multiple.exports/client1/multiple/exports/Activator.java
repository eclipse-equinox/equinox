/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
package client1.multiple.exports;

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
			AbstractBundleTests.simpleResults.addEvent("success"); //$NON-NLS-1$
		}
		try {
			AbstractBundleTests.simpleResults.addEvent(Class.forName("host.multiple.exports.PrivateClass2").getName()); //$NON-NLS-1$
		} catch (Throwable t) {
			AbstractBundleTests.simpleResults.addEvent("success"); //$NON-NLS-1$
		}
	}

	public void stop(BundleContext context) throws Exception {
		// nothing
	}

}
