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
package org.eclipse.equinox.ds.tests.tb1.impl;

import java.util.Dictionary;
import java.util.Vector;

import org.eclipse.equinox.ds.tests.tbc.BoundTester;
import org.osgi.framework.ServiceReference;


public class BindUnbind implements BoundTester {

  private Vector boundObjects = new Vector();
  
  public void bindSAComp(ServiceReference sr) {
    if (boundObjects.contains(sr)) {
    } else {
      boundObjects.addElement(sr);
    }
  }
  
  public void unbindSAComp(ServiceReference sr) {
    if (boundObjects.contains(sr)) {
      boundObjects.removeElement(sr);
    }
  }
  
  public Dictionary getProperties() {
    return null;
  }

  public int getBoundObjectsCount() {
    return boundObjects.size();
  }

  public ServiceReference getBoundServiceRef(int index) {
    return (ServiceReference) boundObjects.elementAt(index);
  }

  public Object getBoundService(int index) {
    return null;
  }
}
