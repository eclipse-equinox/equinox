/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.resolver;

import org.eclipse.equinox.ds.model.ComponentDescriptionProp;

/**
 * 
 */
class CircularityException extends Exception {

	/**
	 * Eclipse-generated <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 8249461007142713618L;

	private ComponentDescriptionProp cdp;

	CircularityException(ComponentDescriptionProp cdp) {
		this.cdp = cdp;
	}

	ComponentDescriptionProp getCircularDependency() {
		return cdp;
	}

}
