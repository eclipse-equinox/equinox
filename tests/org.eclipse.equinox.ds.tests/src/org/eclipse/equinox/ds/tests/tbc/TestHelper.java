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
