/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
package test.bug259903.c;

import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.osgi.framework.*;
import test.bug259903.b.Service3;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		// nothing
	}

	public void stop(BundleContext context) throws Exception {

		new Service3() {/* nothing*/};
		AbstractBundleTests.simpleResults.addEvent(new BundleEvent(BundleEvent.STOPPED, context.getBundle()));
	}

}
