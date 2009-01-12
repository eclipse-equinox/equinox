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
package org.eclipse.equinox.ds.tests.tb7;

import java.util.Dictionary;

import org.eclipse.equinox.ds.tests.tbc.BoundTester;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

public class DynamicCircuit2 implements BoundTester {

  private ServiceReference mateRef;
  private ComponentContext ctxt;
  
  public void activate(ComponentContext ctxt) {
    this.ctxt = ctxt;
  }
  
  public void deactivate(ComponentContext ctxt) {
    this.ctxt = null;
  }
  
  public int getBoundObjectsCount() {
    return (mateRef != null ? 1 : 0);
  }

  public Object getBoundService(int index) {
    return ctxt.locateService("referencedComponent", mateRef);
  }

  public ServiceReference getBoundServiceRef(int index) {
    return mateRef;
  }

  public Dictionary getProperties() {
    return null;
  }

  public void bind(ServiceReference mateRef) {
    this.mateRef = mateRef;
  }
  
  public void unbind(ServiceReference mateRef) {
    if (this.mateRef == mateRef) {
      this.mateRef = null;
    }
  }
  
}
