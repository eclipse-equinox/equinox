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
package test.bug306181a;

import java.util.Hashtable;
import org.osgi.framework.*;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		ServiceReference[] refs = context.getServiceReferences(Runnable.class.getName(), "(test=bug306181)");
		String error = null;
		if (refs == null)
			error = "Did not find expected service";
		else if (refs.length != 1)
			error = "Found wrong number of services: " + refs.length;
		if (error != null) {
			Hashtable props = new Hashtable();
			props.put("test", "bug306181");
			context.registerService(String.class.getName(), error, props);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
