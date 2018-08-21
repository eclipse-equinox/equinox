/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.equinox.http.servlet.tests.bundle;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;

import org.eclipse.equinox.http.servlet.tests.util.TestServletPrototype;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/*
 * The Bundle-Activator for the bundle. Ideally this class is kept as small as
 * possible. 
 */
public class Activator extends Object implements BundleActivator {
	private static Activator INSTANCE;

	public static BundleContext getBundleContext() {
		return Activator.INSTANCE != null ? Activator.INSTANCE.bundleContext : null;
	}
	
	private BundleContext bundleContext;
	
	public Activator() {
		super();
		Activator.INSTANCE = this;
	}

	public void start(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;
		Dictionary<String, Object> serviceProps = new Hashtable<String, Object>();
		serviceProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/prototype/*");
		TestServletPrototype testDriver = new TestServletPrototype(bundleContext);
		bundleContext.registerService(Servlet.class, testDriver, serviceProps);
	}
	
	public void stop(BundleContext bundleContext) throws Exception {
		this.bundleContext = null;
	}
}
