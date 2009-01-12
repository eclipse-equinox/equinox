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
package org.eclipse.equinox.ds.tests.tbc;

public class TestHelper {

  private static boolean activatedStandAlone = false;
  private static boolean activatedServiceProvider = false;
  
  public static void reset() {
    activatedStandAlone = false;
    activatedServiceProvider = false;
  }

  public static boolean isActivatedServiceProvider() {
    return activatedServiceProvider;
  }

  public static void setActivatedServiceProvider(boolean activatedServiceProvider) {
    TestHelper.activatedServiceProvider = activatedServiceProvider;
  }

  public static boolean isActivatedStandAlone() {
    return activatedStandAlone;
  }

  public static void setActivatedStandAlone(boolean activatedStandAlone) {
    TestHelper.activatedStandAlone = activatedStandAlone;
  }
  
}
