/*******************************************************************************
 * Copyright (c) 1997-2009 by ProSyst Software GmbH
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
package org.eclipse.equinox.ds.tests.tb14;

import java.util.Dictionary;

import org.eclipse.equinox.ds.tests.tbc.PropertiesProvider;
import org.eclipse.equinox.ds.tests.tbc.ComponentContextProvider;
import org.osgi.service.component.ComponentContext;


public class Optional implements PropertiesProvider, ComponentContextProvider {
  private Dictionary properties;
  private ComponentContext ctxt;

  protected void activate(ComponentContext ctxt) {
    this.ctxt = ctxt;
    properties = ctxt.getProperties();
  }

  protected void deactivate(ComponentContext ctxt) {

  }

  public Dictionary getProperties() {
    return properties;
  }

  public ComponentContext getComponentContext() {
    return ctxt;
  }
}
