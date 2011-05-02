/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.bug287750;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.startlevel.StartLevel;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		StartLevel sl = (StartLevel) context.getService(context.getServiceReference(StartLevel.class.getName()));
		sl.setStartLevel(10);
	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
