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

public interface BoundMainProvider extends PropertiesProvider {

	public static final String DYNAMIC_SERVICE = "DynamicWorker";
	public static final String NAMED_SERVICE = "NamedService";
	public static final String STATIC_SERVICE = "StaticWorker";

	public Object getBoundService(String serviceName);
}
