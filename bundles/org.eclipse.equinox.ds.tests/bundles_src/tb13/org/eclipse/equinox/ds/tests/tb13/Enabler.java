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
package org.eclipse.equinox.ds.tests.tb13;

import java.util.Dictionary;

import org.eclipse.equinox.ds.tests.tbc.ComponentManager;
import org.osgi.service.component.ComponentContext;


public class Enabler implements ComponentManager {
  private ComponentContext ctxt;

  public void activate(ComponentContext ctxt) {
   this.ctxt = ctxt;
  }

  public void deactivate(ComponentContext ctxt) {
    this.ctxt = null;
  }

  public void enableComponent(String name, boolean flag) {
    if (flag) ctxt.enableComponent(name);
    else      ctxt.disableComponent(name);
  }

  public Dictionary getProperties() {
    return ctxt.getProperties();
  }

}
