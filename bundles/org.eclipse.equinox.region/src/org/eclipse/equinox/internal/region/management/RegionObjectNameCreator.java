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

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.eclipse.equinox.region.management.ManageableRegion;

/**
 * {@link RegionObjectNameCreator} is responsible for creating {@link ObjectName}s for {@link ManageableRegion}s.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Thread safe.
 */
final class RegionObjectNameCreator {

	private final String domain;
	private final String frameworkUUID;

	RegionObjectNameCreator(String domain, String frameworkUUID) {
		this.domain = domain;
		this.frameworkUUID = frameworkUUID;
	}

	ObjectName getRegionObjectName(String regionName) {
		try {
			String name = this.domain + ":type=Region,name=" + regionName; //$NON-NLS-1$
			if (frameworkUUID != null)
				name += ",frameworkUUID=" + frameworkUUID; //$NON-NLS-1$
			return new ObjectName(name);
		} catch (MalformedObjectNameException e) {
			e.printStackTrace();
			return null;
		}
	}

}
