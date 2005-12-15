/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.device;

import org.osgi.framework.ServiceReference;

/**
 * Match implementation class.
 *
 */
public class Match implements org.osgi.service.device.Match {

	private ServiceReference driver;
	private int matchValue;

	Match(ServiceReference driver, int matchValue) {
		this.driver = driver;
		this.matchValue = matchValue;
	}

	public ServiceReference getDriver() {
		return driver;
	}

	public int getMatchValue() {
		return matchValue;
	}
}