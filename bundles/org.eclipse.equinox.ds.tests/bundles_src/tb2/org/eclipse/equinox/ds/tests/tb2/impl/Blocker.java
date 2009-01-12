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
package org.eclipse.equinox.ds.tests.tb2.impl;

import java.util.Dictionary;

import org.osgi.service.component.ComponentContext;

public class Blocker {
  
  public void activate(ComponentContext ctxt) {
    Dictionary props = ctxt.getProperties();
    int timeout = 40000;    // default value of 1 secs
    Object t = props.get("block.timeout");
    if (t != null) {
      if (t instanceof String) {
        timeout = Integer.parseInt((String) t);
      } else if (t instanceof Integer) {
        timeout = ((Integer)t).intValue();
      }
    }
    try {
      Thread.sleep(timeout);
    } catch (InterruptedException ignore) {
    }
  }

}
