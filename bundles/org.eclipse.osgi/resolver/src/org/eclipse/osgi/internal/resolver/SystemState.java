/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleException;

// this class provides synchronous access to resolve and add/remove/update bundle for the framework
public class SystemState extends StateImpl {
	synchronized public boolean addBundle(BundleDescription description) {
		return super.addBundle(description);
	}

	synchronized public boolean removeBundle(BundleDescription toRemove) {
		return super.removeBundle(toRemove);
	}

	synchronized public boolean updateBundle(BundleDescription newDescription) {
		return super.updateBundle(newDescription);
	}

	public StateDelta compare(State state) throws BundleException {
		// we don't implement this (no big deal: the system state is private to the framework)
		throw new UnsupportedOperationException();
	}
}
