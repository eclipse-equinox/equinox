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
package org.eclipse.equinox.ds.tests.tb10;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.eclipse.equinox.ds.tests.tbc.BoundCountProvider;
import org.osgi.framework.ServiceReference;


public class CountFactory implements BoundCountProvider {

  private List boundServices = new ArrayList();
  
  public int getBoundServiceCount(String service) {
    return boundServices.size();
  }
  
  public void bindService(ServiceReference ref) {
    boundServices.add(ref);
  }
  
  public void unbindService(ServiceReference ref) {
    boundServices.remove(ref);
  }
  
  public Dictionary getProperties() {
    return null;
  }

}
