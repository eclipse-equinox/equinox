/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.tests.tb4;

import org.eclipse.equinox.ds.tests.tbc.TestHelper;
import org.osgi.service.component.ComponentContext;


public class AnotherComponent {

  public void activate(ComponentContext ctxt) {
    TestHelper.setActivatedStandAlone(true);
  }
  
  public void deactivate(ComponentContext ctxt) {
    TestHelper.setActivatedStandAlone(false);
  }
}
