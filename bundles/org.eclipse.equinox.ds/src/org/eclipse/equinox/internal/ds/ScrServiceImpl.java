/*******************************************************************************
 * Copyright (c) 1997-2010 by ProSyst Software GmbH
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

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.osgi.framework.Bundle;

public class ScrServiceImpl implements ScrService {

	boolean disposed = false;

	public Component getComponent(long componentId) {
		if (!disposed && InstanceProcess.resolver != null) {
			return InstanceProcess.resolver.getComponent(componentId);
		}
		return null;
	}

	public Component[] getComponents() {
		if (!disposed && InstanceProcess.resolver != null) {
			return InstanceProcess.resolver.mgr.getComponents();
		}
		return null;
	}

	public Component[] getComponents(Bundle bundle) {
		if (!disposed && InstanceProcess.resolver != null) {
			return InstanceProcess.resolver.mgr.getComponents(bundle);
		}
		return null;
	}

	public void dispose() {
		disposed = true;
	}

	public Component[] getComponents(String componentName) {
		if (!disposed && InstanceProcess.resolver != null) {
			return InstanceProcess.resolver.mgr.getComponents(componentName);
		}
		return null;
	}

}
