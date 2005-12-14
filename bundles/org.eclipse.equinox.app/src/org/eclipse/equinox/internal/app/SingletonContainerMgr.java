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
package org.eclipse.equinox.internal.app;

import org.eclipse.equinox.app.*;

public class SingletonContainerMgr implements IContainer {
	private EclipseAppHandle singletonHandle;
	private String type;
	private IContainer singletonContainer;
	private ContainerManager containerManager;
	
	public SingletonContainerMgr(IContainer singletonContainer, String type, ContainerManager containerManager) {
		this.singletonContainer = singletonContainer;
		this.type = type;
		this.containerManager = containerManager;
	}
	public synchronized IApplication launch(IAppContext context) throws Exception {
		if (context != singletonHandle)
			throw new IllegalStateException("Only one application of type \"" + type + "\" is allowed to run at a time");
		return singletonContainer.launch(context);
	}

	public boolean isSingletonContainer() {
		return true;
	}

	synchronized boolean isLocked() {
		return singletonHandle != null;
	}

	synchronized void lock(EclipseAppHandle appHandle) {
		if (singletonHandle != null)
			throw new IllegalStateException("Only one application of type \"" + type + "\" is allowed to run at a time");
		singletonHandle = appHandle;
		refreshAppDescriptors();
	}
	synchronized void unlock() {
		singletonHandle = null;
		refreshAppDescriptors();
	}

	private void refreshAppDescriptors() {
		EclipseAppDescriptor[] singletonApps = containerManager.getAppDescriptorsByType(type);
		for (int i = 0; i < singletonApps.length; i++) {
			singletonApps[i].setSingletonMgr(this);
			singletonApps[i].refreshProperties();
		}
	}
}
