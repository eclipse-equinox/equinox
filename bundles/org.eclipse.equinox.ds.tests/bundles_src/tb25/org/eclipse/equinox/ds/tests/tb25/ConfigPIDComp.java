/*******************************************************************************
 * Copyright (c) 1997, 2012 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.tests.tb25;

import java.util.Dictionary;

import org.eclipse.equinox.ds.tests.tbc.PropertiesProvider;
import org.osgi.service.component.ComponentContext;

public class ConfigPIDComp implements PropertiesProvider {
  private ComponentContext ctxt;

  protected void activate(ComponentContext ctxt) {
    this.ctxt = ctxt;
  }

  protected void deactivate(ComponentContext ctxt) {

  }

  public Dictionary getProperties() {
    if (ctxt == null) {
      return null;
    }

    return ctxt.getProperties();
  }
}
