/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.configuration;

import java.io.File;
import java.net.URL;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.tests.OSGiTest;

public class ConfigurationSessionTest extends OSGiTest {
	public ConfigurationSessionTest(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	protected File getConfigurationDir() {
		Location configurationLocation = Platform.getConfigurationLocation();
		URL configurationURL = configurationLocation.getURL();
		assertEquals(configurationURL.toExternalForm(), "file", configurationURL.getProtocol());
		File configurationDir = new File(configurationURL.getFile());
		return configurationDir;
	}	
}
