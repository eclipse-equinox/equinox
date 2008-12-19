/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.link.c;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import test.link.c.service1.Service1;
import test.link.c.service2.Service2;
import test.link.c.service3.Service3;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		context.registerService(Service1.class.getName(), new Service1() {}, null);
		context.registerService(new String[] {Service1.class.getName(), Service2.class.getName()}, new Service2() {}, null);
		context.registerService(new String[] {Service1.class.getName(), Service2.class.getName(), Service3.class.getName()}, new Service3() {}, null);
	}

	public void stop(BundleContext context) throws Exception {
		// nothing
	}

}
