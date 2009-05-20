/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.provisional.security.ui.AuthorizationManager;
import org.eclipse.osgi.internal.provisional.service.security.AuthorizationStatus;
import org.eclipse.ui.IWorkbenchWindow;

// 1. if there are disabled bundles, and if so, which ones, why, and whether they are fatal or warn 
// 2. when bundles are disabled, why and whether they are fatal or warn
// 3. when bundles are enabled
public class DefaultAuthorizationManager extends AuthorizationManager {

	boolean enabled = (null != Activator.getAuthorizationEngine());

	private int currentStatus = IStatus.OK;
	private boolean needsAttention = false;

	public DefaultAuthorizationManager() {
		currentStatus = enabled ? Activator.getAuthorizationEngine().getStatus() : IStatus.OK;
	}

	public boolean isEnabled() {
		return true;
	}

	public boolean needsAttention() {
		return needsAttention; //TODO: make it happen
	}

	public IStatus getStatus() {
		currentStatus = enabled ? Activator.getAuthorizationEngine().getStatus() : IStatus.OK;
		return transformStatus(currentStatus);
	}

	public void displayManager(IWorkbenchWindow workbenchWindow) {
		//TODO: manager UI
	}

	private IStatus transformStatus(int engineStatus) {
		Status status = null;
		switch (engineStatus) {
			case AuthorizationStatus.OK :
				status = new Status(IStatus.OK, Activator.getSymbolicName(), ""); //$NON-NLS-1$ //TODO: text
				break;

			case AuthorizationStatus.ERROR :
				status = new Status(IStatus.ERROR, Activator.getSymbolicName(), ""); //$NON-NLS-1$ //TODO: text
				break;

			default :
				status = null;
				break;
		}
		return status;
	}
}
