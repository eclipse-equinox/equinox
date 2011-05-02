/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.tests.tb1;

import java.util.Dictionary;

import org.eclipse.equinox.ds.tests.tb1.impl.BindUnbind;
import org.eclipse.equinox.ds.tests.tbc.ComponentContextProvider;
import org.eclipse.equinox.ds.tests.tbc.ComponentManager;
import org.osgi.service.component.ComponentContext;


public class BindUnbindSuccessor extends BindUnbind implements ComponentManager, ComponentContextProvider {

  private ComponentContext ctxt;
  
  public void activate(ComponentContext ctxt) {
   this.ctxt = ctxt;
  }
  
  public void deactivate(ComponentContext ctxt) {
    this.ctxt = null;
  }
  
  public void doNothing() {
    
  }
  
  public ComponentContext getContext() {
    return ctxt;
  }

  public void enableComponent(String name, boolean flag) {
    if (flag) ctxt.enableComponent(name);
    else      ctxt.disableComponent(name);
  }

  public Dictionary getProperties() {
    return ctxt.getProperties();
  }

  public ComponentContext getComponentContext() {
    return ctxt;
  }
}
