/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fragment.test.attach.host.a;

import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.osgi.framework.*;

public class Activator2 implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		AbstractBundleTests.simpleResults.addEvent(new BundleEvent(BundleEvent.STARTED, context.getBundle()));
	}

	public void stop(BundleContext context) throws Exception {
		AbstractBundleTests.simpleResults.addEvent(new BundleEvent(BundleEvent.STOPPED, context.getBundle()));
	}

}
