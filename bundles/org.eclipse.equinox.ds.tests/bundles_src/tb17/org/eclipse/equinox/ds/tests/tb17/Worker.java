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
package org.eclipse.equinox.ds.tests.tb17;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import org.eclipse.equinox.ds.tests.tbc.PropertiesProvider;
import org.eclipse.equinox.ds.tests.tbc.ComponentContextProvider;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;


public class Worker implements PropertiesProvider, ComponentContextProvider {
  private Dictionary properties;
  private ComponentContext ctxt;
  private ServiceRegistration sr;

  protected void activate(ComponentContext ctxt) {
    this.ctxt = ctxt;
    properties = ctxt.getProperties();

    Object prop = properties.get(ComponentConstants.COMPONENT_NAME);
    if (prop != null) {
      Dictionary<String, Object> serviceProps = new Hashtable<String, Object>();
      serviceProps.put(ComponentConstants.COMPONENT_NAME, prop);
      sr = ctxt.getBundleContext().registerService(PropertiesProvider.class.getName(), this, serviceProps);
    }
  }

  protected void deactivate(ComponentContext ctxt) {
    if (sr != null) {
      sr.unregister();
      sr = null;
    }
  }

  public Dictionary getProperties() {
    return properties;
  }

  public ComponentContext getComponentContext() {
    return ctxt;
  }
}
