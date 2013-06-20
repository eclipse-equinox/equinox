/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.plugins.e;

import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.*;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, context.getBundle().getSymbolicName());
		context.registerService(Object.class, new Object(), props);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// nothing
	}

}
