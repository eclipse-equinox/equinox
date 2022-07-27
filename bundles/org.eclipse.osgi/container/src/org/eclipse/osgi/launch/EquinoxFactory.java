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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.connect.ConnectFramework;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.connect.FrameworkUtilHelper;
import org.osgi.framework.connect.ModuleConnector;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * The framework factory implementation for the Equinox framework.
 * @since 3.5
 */
public class EquinoxFactory implements FrameworkFactory, ConnectFrameworkFactory, FrameworkUtilHelper {

	static final Set<FrameworkUtilHelper> connectHelpers = ConcurrentHashMap.newKeySet();

	@Override
	public Framework newFramework(Map<String, String> configuration) {
		return new Equinox(configuration);
	}

	@Override
	public ConnectFramework newFramework(Map<String, String> configuration, ModuleConnector moduleConnector) {
		return new EquinoxConnect(configuration, moduleConnector);
	}

	@Override
	public Optional<Bundle> getBundle(Class<?> classFromBundle) {
		return connectHelpers.stream().flatMap(helper -> helper.getBundle(classFromBundle).stream()).findFirst();
	}

	private static final class EquinoxConnect extends Equinox implements ConnectFramework {

		public EquinoxConnect(Map<String, String> configuration, ModuleConnector moduleConnector) {
			super(configuration, moduleConnector);
		}

		@Override
		public void addFrameworkUtilHelper(FrameworkUtilHelper helper) {
			connectHelpers.add(helper);
		}

		@Override
		public void removeFrameworkUtilHelper(FrameworkUtilHelper helper) {
			connectHelpers.remove(helper);
		}

	}
}
