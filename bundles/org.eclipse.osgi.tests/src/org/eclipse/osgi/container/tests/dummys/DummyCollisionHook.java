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
package org.eclipse.osgi.container.tests.dummys;

import java.util.Collection;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleCollisionHook;

public class DummyCollisionHook implements ModuleCollisionHook {

	private final boolean filterCollisions;

	public DummyCollisionHook() {
		this(false);
	}

	public DummyCollisionHook(boolean filterCollisions) {
		super();
		this.filterCollisions = filterCollisions;
	}

	@Override
	public void filterCollisions(int operationType, Module target, Collection<Module> collisionCandidates) {
		if (filterCollisions) {
			collisionCandidates.clear();
		}
	}

}
