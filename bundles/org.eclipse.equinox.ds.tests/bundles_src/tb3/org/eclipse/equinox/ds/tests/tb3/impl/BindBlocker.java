/*******************************************************************************
 * Copyright (c) 1997-2009 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.tests.tb3.impl;

import org.osgi.framework.ServiceReference;

public class BindBlocker {
  
  // the time the bind method will block
  private int timeout = 60000;
  
  
  public void setLogger(ServiceReference log) {
    try {
      Thread.sleep(timeout);
    } catch (InterruptedException ignore) { 
    }
  }

  public void unsetLogger(ServiceReference log) {
  }
}
