/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import org.eclipse.osgi.service.resolver.BaseDescription;
import org.osgi.framework.Version;

abstract class BaseDescriptionImpl implements BaseDescription {

	protected final Object monitor = new Object();

	private volatile String name;

	private volatile Version version;

	public String getName() {
		return name;
	}

	public Version getVersion() {
		synchronized (this.monitor) {
			if (version == null)
				return Version.emptyVersion;
			return version;
		}
	}

	protected void setName(String name) {
		this.name = name;
	}

	protected void setVersion(Version version) {
		this.version = version;
	}

}
