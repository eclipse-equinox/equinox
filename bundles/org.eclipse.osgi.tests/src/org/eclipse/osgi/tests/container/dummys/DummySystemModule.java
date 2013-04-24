/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.container.dummys;

import org.eclipse.osgi.container.*;
import org.osgi.framework.Bundle;

public class DummySystemModule extends SystemModule {

	public DummySystemModule(ModuleContainer container) {
		super(container);
	}

	@Override
	public Bundle getBundle() {
		return null;
	}

	@Override
	protected void cleanup(ModuleRevision revision) {
		// Do nothing
	}

}
