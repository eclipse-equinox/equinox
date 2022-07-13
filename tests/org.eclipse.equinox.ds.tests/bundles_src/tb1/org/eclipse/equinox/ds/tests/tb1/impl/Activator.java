/*******************************************************************************
 * Copyright (c) 1997-2009 by ProSyst Software GmbH
 * http://www.prosyst.com
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.tests.tb1.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

  private BundleContext ctx;
  private static Activator instance;
  
  public Activator() {
    Activator.instance = this;
  }
  
  public void start(BundleContext context) throws Exception {
    this.ctx = context;
  }

  public void stop(BundleContext context) throws Exception {
    this.ctx = null;
  }

  public static BundleContext getContext() {
    return instance.ctx; 
  }
}
