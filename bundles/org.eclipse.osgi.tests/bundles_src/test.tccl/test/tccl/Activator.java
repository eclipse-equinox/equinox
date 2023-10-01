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
package test.tccl;

import org.osgi.framework.*;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		System.out.println("test.tccl: start");
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		ClassLoader serviceTCCL = (ClassLoader) context.getService(context
				.getServiceReferences(ClassLoader.class.getName(), "(equinox.classloader.type=contextClassLoader)")[0]); //$NON-NLS-1$
		if (tccl != serviceTCCL) {
			BundleException e = new BundleException("Wrong thread context class loader found"); //$NON-NLS-1$
			e.printStackTrace();
			throw e;
		}
	}

	public void stop(BundleContext context) throws Exception {
		System.out.println("test.tccl: stop");
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		ClassLoader serviceTCCL = (ClassLoader) context.getService(context
				.getServiceReferences(ClassLoader.class.getName(), "(equinox.classloader.type=contextClassLoader)")[0]); //$NON-NLS-1$
		if (tccl != serviceTCCL) {
			BundleException e = new BundleException("Wrong thread context class loader found"); //$NON-NLS-1$
			e.printStackTrace();
			throw e;
		}
	}
}
