/*******************************************************************************
 * Copyright (c) 2011 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.equinox.internal.region.management;

import org.eclipse.equinox.region.management.ManageableRegion;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * {@link RegionObjectNameCreator} is responsible for creating {@link ObjectName}s for {@link ManageableRegion}s.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Thread safe.
 */
final class RegionObjectNameCreator {

	private final String domain;

	RegionObjectNameCreator(String domain) {
		this.domain = domain;
	}

	ObjectName getRegionObjectName(String regionName) {
		try {
			return new ObjectName(this.domain + ":type=Region,name=" + regionName);
		} catch (MalformedObjectNameException e) {
			e.printStackTrace();
			return null;
		}
	}

}
