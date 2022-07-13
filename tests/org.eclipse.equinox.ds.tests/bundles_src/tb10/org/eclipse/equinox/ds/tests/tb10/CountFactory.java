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
