/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/
package test;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.*;

public class ConsoleTestActivator implements BundleActivator {
	ServiceRegistration serviceRegistration;

	public void start(BundleContext context) throws Exception {
		serviceRegistration = context.registerService(CommandProvider.class.getName(), new ConsoleTestCommandProvider(), null);
	}

	public void stop(BundleContext context) throws Exception {
		serviceRegistration.unregister();
	}

}
