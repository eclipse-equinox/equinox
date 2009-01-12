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

public interface BoundMainProvider extends PropertiesProvider {

  public static final String DYNAMIC_SERVICE = "DynamicWorker";
  public static final String NAMED_SERVICE = "NamedService";
  public static final String STATIC_SERVICE = "StaticWorker";
  public Object getBoundService(String serviceName);
}
