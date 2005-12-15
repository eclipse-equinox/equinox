/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class SSLTracker extends ServiceTracker {
	protected static final String sslsvcClass = "com.ibm.osg.service.ssl.SSLService"; //ssl //$NON-NLS-1$

	protected SSLTracker(BundleContext context) {
		super(context, sslsvcClass, null);

		open();
	}

	//BUGBUG need to handle opening and closing of HTTPS port!
	// e.g. httpAcceptor.createSSLListener();
}
