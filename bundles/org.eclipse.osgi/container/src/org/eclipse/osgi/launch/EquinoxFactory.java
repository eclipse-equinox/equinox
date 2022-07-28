/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.launch;

import java.util.Map;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.connect.ModuleConnector;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * The framework factory implementation for the Equinox framework.
 * @since 3.5
 */
public class EquinoxFactory implements FrameworkFactory, ConnectFrameworkFactory {

	@Override
	public Framework newFramework(Map<String, String> configuration) {
		return newFramework(configuration, null);
	}

	@Override
	public Framework newFramework(Map<String, String> configuration, ModuleConnector moduleConnector) {
		return new Equinox(configuration, moduleConnector);
	}
}
