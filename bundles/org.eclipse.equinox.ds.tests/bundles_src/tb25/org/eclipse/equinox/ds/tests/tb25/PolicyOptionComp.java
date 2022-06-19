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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.ds.tests.tbc.PropertiesProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

public class PolicyOptionComp implements PropertiesProvider {
  private static final String RANK_MISSING_VALUE = "property.is.missing";

  private ComponentContext    ctxt;
  private Map                 newProps;

  protected void activate(ComponentContext ctxt) {
    this.ctxt = ctxt;
  }

  protected void deactivate(ComponentContext ctxt) {

  }

  public void bind01(PropertiesProvider service, Map properties) {
    registerMethodCall("bind01", properties);
  }

  public void bind11(PropertiesProvider service, Map properties) {
    registerMethodCall("bind11", properties);
  }

  public void bind0n(PropertiesProvider service, Map properties) {
    registerMethodCall("bind0n", properties);
  }

  public void bind1n(PropertiesProvider service, Map properties) {
    registerMethodCall("bind1n", properties);
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

  private synchronized void registerMethodCall(String name, Map properties) {
    if (newProps == null) {
      newProps = new Hashtable();
    }
    Object rankValue = null;
    if (properties != null) {
      newProps.putAll(properties);
      rankValue = properties.get(Constants.SERVICE_RANKING);
    }
    List list = (List) newProps.get(name);
    if (list == null) {
      list = new ArrayList();
      newProps.put(name, list);
    }
    list.add(rankValue != null ? rankValue : RANK_MISSING_VALUE);
  }
}
