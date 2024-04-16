
/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package test.bug471551;

import javax.xml.validation.SchemaFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		ClassLoader cl = context.getBundle().adapt(BundleWiring.class).getClassLoader();
		ClassLoader currentTCCL = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(cl);
		try {
			SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
			System.out.println("SchemaFactory with TCCL: " + sf.getClass().getName());
		} finally {
			Thread.currentThread().setContextClassLoader(currentTCCL);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// nothing
	}

}
