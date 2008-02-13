/*******************************************************************************
 * Copyright (c) 1997-2007 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ds;

import java.io.IOException;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * ConfigurationManager.java
 * 
 * @author Valentin Valchev
 * @author Pavlin Dobrev
 * @version 1.0
 */

class ConfigurationManager {

	static ServiceTracker cmTracker;

	static Configuration getConfiguration(String pid) throws IOException {
		ConfigurationAdmin cmAdmin = (ConfigurationAdmin) cmTracker.getService();
		if (cmAdmin != null) {
			return cmAdmin.getConfiguration(pid);
		}
		return null;
	}

	static Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
		ConfigurationAdmin cmAdmin = (ConfigurationAdmin) cmTracker.getService();
		if (cmAdmin != null) {
			return cmAdmin.listConfigurations(filter);
		}
		return null;
	}
}
