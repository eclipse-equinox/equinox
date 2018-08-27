/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.container.dummys;

import java.util.EnumSet;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleRevision;
import org.osgi.framework.Bundle;

public class DummyModule extends Module {

	public DummyModule(Long id, String location, ModuleContainer container, EnumSet<Settings> settings, int startlevel) {
		super(id, location, container, settings, startlevel);
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
