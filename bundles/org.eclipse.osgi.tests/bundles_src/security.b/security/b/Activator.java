/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/

package security.b;

import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

  private BundleContext bc;
  
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		System.out.println("Starting bundle Test2!!");
		System.out.println("Security manager: "+System.getSecurityManager());
		this.bc = context;
		doTestAction();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		System.out.println("Stopping bundle Test2!!");
	}

	private void doTestAction()  {
	  Bundle[] bundles = bc.getBundles();
    Bundle thisBundle = bc.getBundle();
    for (int i = 0; i < bundles.length; i++) {
      if (thisBundle.getBundleId() != bundles[i].getBundleId()) {
        checkBundle(bundles[i]);
      }
    }
	}
	
	private void checkBundle(Bundle bundle) {
	  Dictionary headers = bundle.getHeaders();

	}
}
