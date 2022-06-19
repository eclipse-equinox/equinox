/*******************************************************************************
 * Copyright (c) 1997, 2012 by ProSyst Software GmbH
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
package org.eclipse.equinox.ds.tests.tb25;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.equinox.ds.tests.tbc.PropertiesProvider;
import org.osgi.service.component.ComponentContext;

public class ServicePropertiesComp implements PropertiesProvider {
  private ComponentContext ctxt;
  private Map              newProps;

  protected void activate(ComponentContext ctxt) {
    this.ctxt = ctxt;
  }

  protected void deactivate(ComponentContext ctxt) {

  }

  public void bindDynamicRef(PropertiesProvider service, Map properties) {
  }

  public void bindStaticRef(PropertiesProvider service, Map properties) {
  }

  public synchronized void serviceUpdatedStatic(PropertiesProvider service, Map properties) {
    if (newProps == null) {
      newProps = new Hashtable();
    }
    if (properties != null) {
      newProps.putAll(properties);
    }
    newProps.put("serviceUpdatedStatic", Boolean.TRUE);
  }

  public synchronized void serviceUpdatedDynamic(PropertiesProvider service, Map properties) {
    if (newProps == null) {
      newProps = new Hashtable();
    }
    if (properties != null) {
      newProps.putAll(properties);
    }
    newProps.put("serviceUpdatedDynamic", Boolean.TRUE);
  }

  public synchronized Dictionary getProperties() {
    if (ctxt == null) {
      return null;
    }

    Dictionary result = new Hashtable();

    Dictionary ctxtProps = ctxt.getProperties();
    if (ctxtProps != null) {
      for (Enumeration en = ctxtProps.keys(); en.hasMoreElements();) {
        Object key = en.nextElement();
        result.put(key, ctxtProps.get(key));
      }
    }

    if (newProps != null) {
      for (Object key : newProps.keySet()) {
        result.put(key, newProps.get(key));
      }
    }

    return result;
  }
}
