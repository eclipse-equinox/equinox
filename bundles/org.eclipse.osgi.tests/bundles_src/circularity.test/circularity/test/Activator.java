/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package circularity.test;

import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.osgi.framework.*;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		AbstractBundleTests.simpleResults.addEvent(new BundleEvent(BundleEvent.STARTED, context.getBundle()));
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		AbstractBundleTests.simpleResults.addEvent(new BundleEvent(BundleEvent.STOPPED, context.getBundle()));
	}

}
