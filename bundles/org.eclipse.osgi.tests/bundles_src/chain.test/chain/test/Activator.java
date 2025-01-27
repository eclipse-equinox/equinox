/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package chain.test;

import chain.test.b.BMultiChain1;
import org.osgi.framework.*;

public class Activator implements BundleActivator, SynchronousBundleListener {

	@Override
	public void start(BundleContext context) throws Exception {
		if (context.getProperty("test.bug300692") == null)
			return;
		if (context.getProperty("test.bug300692.listener") != null) {
			context.addBundleListener(this);
		}

		new TestMultiChain();
		context.removeBundleListener(this);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// Nothing
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		if (event.getType() != BundleEvent.LAZY_ACTIVATION)
			return;
		Class clazz = BMultiChain1.class;
		System.out.println(clazz.getName());
	}

}
