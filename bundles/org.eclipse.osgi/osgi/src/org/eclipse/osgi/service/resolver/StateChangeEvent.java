/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

import java.util.EventObject;

public class StateChangeEvent extends EventObject {
	private static final long serialVersionUID = 3258131345132566067L;
	private StateDelta delta;

	public StateChangeEvent(StateDelta delta) {
		super(delta.getState());
		this.delta = delta;
	}

	/**
	 * Returns a delta detailing changes to a state object.
	 * 
	 * @return a state delta
	 */
	public StateDelta getDelta() {
		return delta;
	}
}