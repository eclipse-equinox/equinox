/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package chain.test;

import chain.test.b.BMultiChain1;
import org.osgi.framework.*;

public class Activator implements BundleActivator, SynchronousBundleListener {

	public void start(BundleContext context) throws Exception {
		if (context.getProperty("test.bug300692") == null)
			return;
		if (context.getProperty("test.bug300692.listener") != null) {
			context.addBundleListener(this);
		}

		new TestMultiChain();
		context.removeBundleListener(this);
	}

	public void stop(BundleContext context) throws Exception {
		// Nothing
	}

	public void bundleChanged(BundleEvent event) {
		if (event.getType() != BundleEvent.LAZY_ACTIVATION)
			return;
		Class clazz = BMultiChain1.class;
	}

}
