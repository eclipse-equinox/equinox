/*******************************************************************************
 * Copyright (c) 1997, 2018 by ProSyst Software GmbH
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
package org.eclipse.equinox.ds.tests.tb15;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

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
    properties = getProperties(ctxt.getProperties());
  }
  
  Properties getProperties(Dictionary dict) {
    Properties result = new Properties();
    Enumeration keys = dict.keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      result.put(key, dict.get(key));
    }
    return result;
  }


  protected void deactivate(ComponentContext ctxt) {
    deactPos = deactCount++;
    
    properties.put("config.base.data", Integer.valueOf(deactPos));
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
