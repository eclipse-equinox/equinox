/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleException;

public class SystemState extends StateImpl {
	public boolean addBundle(BundleDescription description) {
		if (!super.addBundle(description))
			return false;
		updateTimeStamp();
		return true;
	}

	public boolean removeBundle(BundleDescription toRemove) {
		if (!super.removeBundle(toRemove))
			return false;
		updateTimeStamp();
		return true;
	}

	public boolean updateBundle(BundleDescription newDescription) {
		if (!super.removeBundle(newDescription))
			return false;
		updateTimeStamp();
		return true;
	}

	private void updateTimeStamp() {
		if (timeStamp == Long.MAX_VALUE)
			timeStamp = 0;
		timeStamp++;
	}

	public StateDelta compare(State state) throws BundleException {
		// we don't implement this (no big deal: the system state is private to the framework)
		throw new UnsupportedOperationException();
	}

}