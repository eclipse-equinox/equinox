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
package test.link.b.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import test.link.b.SomeAPI;
import test.link.b.params.AParam;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		SomeAPI api = new SomeAPI();
		api.getBundleID(context.getBundle());
		api.getString(new AParam());
		// test class that does not exist
		try {
			getClass().getClassLoader().loadClass("test.link.b.DoesNotExist"); //$NON-NLS-1$
			throw new RuntimeException("Unexpected class load success"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			// expected
		}
		// test resource
		URL resource = this.getClass().getResource("/test/link/b/resource.txt"); //$NON-NLS-1$
		if (resource == null)
			throw new RuntimeException("Did not find resource.txt"); //$NON-NLS-1$
		BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream()));
		try {
			String content = reader.readLine();
			if (!"Test content".equals(content)) //$NON-NLS-1$
				throw new RuntimeException("Unexpected content in resource.txt: " + content); //$NON-NLS-1$
		} finally {
			reader.close();
		}
		// test resource that does not exist
		URL notExist = this.getClass().getResource("/test/link/b/DoesNotExist.txt"); //$NON-NLS-1$
		if (notExist != null)
			throw new RuntimeException("Should not have found resource: " + notExist.getPath()); //$NON-NLS-1$

	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
