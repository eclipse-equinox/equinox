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
package org.eclipse.equinox.ds.tests.tb15;

import java.util.Dictionary;

import org.eclipse.equinox.ds.tests.tbc.PropertiesProvider;
import org.eclipse.equinox.ds.tests.tbc.ComponentContextProvider;
import org.osgi.service.component.ComponentContext;


public class Component1 implements PropertiesProvider, ComponentContextProvider {
  protected static int deactCount = 0;
  private int deactPos = -1;
  private Dictionary properties;
  private ComponentContext ctxt;

  protected void activate(ComponentContext ctxt) {
    this.ctxt = ctxt;
    properties = ctxt.getProperties();
  }

  protected void deactivate(ComponentContext ctxt) {
    deactPos = deactCount++;
    properties.put("config.base.data", new Integer(deactPos));
  }

  public Dictionary getProperties() {
    return properties;
  }

  public int getDeactivationPos() {
    return deactPos;
  }

  public ComponentContext getComponentContext() {
    return ctxt;
  }
}
