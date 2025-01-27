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
package ext.framework.a.requires;

import ext.framework.a.FrameworkExtTest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.eclipse.osgi.tests.bundles.ExtensionBundleTests;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		AbstractBundleTests.simpleResults.addEvent(new FrameworkExtTest().testIt("success")); //$NON-NLS-1$
		AbstractBundleTests.simpleResults
				.addEvent(getURLContent(this.getClass().getResource("/ext/framework/a/fwkresource.txt"))); //$NON-NLS-1$
		ExtensionBundleTests.events.add(context.getBundle().getSymbolicName() + " STARTED");
	}

	private String getURLContent(URL resource) throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.openStream()))) {
			return br.readLine();
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		ExtensionBundleTests.events.add(context.getBundle().getSymbolicName() + " STOPPED");
	}

}
