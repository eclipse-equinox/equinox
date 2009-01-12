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
package org.eclipse.equinox.ds.tests.tb4.impl;

import java.util.Dictionary;

import org.eclipse.equinox.ds.tests.tb4.NamedService;
import org.eclipse.equinox.ds.tests.tbc.ComponentContextProvider;
import org.osgi.service.component.ComponentContext;

public class NamedFactory implements NamedService, ComponentContextProvider {
  private String name = "name not init";
  private ComponentContext ctxt;
  
  public void activate(ComponentContext componentContext) {
    this.ctxt = componentContext;
    name = (String) componentContext.getProperties().get("name");
    if( name == null ) {
      this.name = "name not set";
    }
  }
  
  // it is absolutely legal to have activate without having deactivate!
  //public void deactivate(ComponentContext cc) {}

  public String getName() {
    return name;
  }
  
  public String toString() {
    return name;
  }
  
  public ComponentContext getComponentContext() {
    return ctxt;
  }

  public Dictionary getProperties() {
    return ctxt.getProperties();
  }

}
