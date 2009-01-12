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

import org.osgi.framework.ServiceReference;

public interface BoundTester extends PropertiesProvider {

  public int getBoundObjectsCount();
  public ServiceReference getBoundServiceRef(int index);
  public Object getBoundService(int index);
  
}
