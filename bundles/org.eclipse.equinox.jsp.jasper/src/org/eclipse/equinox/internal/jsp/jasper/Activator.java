/*******************************************************************************
 * Copyright (c) 2005, 2017 Cognos Incorporated, IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.equinox.internal.jsp.jasper;

import org.eclipse.osgi.framework.util.Wirings;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.namespace.PackageNamespace;

public class Activator implements BundleActivator {

	private volatile static Bundle thisBundle;

	@Override
	public void start(BundleContext context) throws Exception {
		// disable the JSR99 compiler that does not work in OSGi;
		// This will convince jasper to use the JDTCompiler that invokes ecj (see JSP-21
		// on the glassfish bug-tracker)
		System.setProperty("org.apache.jasper.compiler.disablejsr199", Boolean.TRUE.toString()); //$NON-NLS-1$
		thisBundle = context.getBundle();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		thisBundle = null;
	}

	public static Bundle getJasperBundle() {
		Bundle bundle = FrameworkUtil.getBundle(org.apache.jasper.servlet.JspServlet.class);
		if (bundle != null) {
			return bundle;
		}
		return Wirings.getRequiredWires(thisBundle, PackageNamespace.PACKAGE_NAMESPACE).stream().filter(p -> {
			Object packageName = p.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
			return "org.apache.jasper.servlet".equals(packageName); //$NON-NLS-1$
		}).map(p -> p.getProvider().getBundle()).findAny().orElse(null);
	}
}
