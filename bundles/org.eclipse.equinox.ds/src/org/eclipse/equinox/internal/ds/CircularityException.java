/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
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

import org.eclipse.equinox.internal.ds.model.ServiceComponentProp;

/**
 * Used to find circular dependencies
 *
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class CircularityException extends Exception {
	private static final long serialVersionUID = 1L;
	private ServiceComponentProp causingComponent;

	public CircularityException(ServiceComponentProp scp) {
		this.causingComponent = scp;
	}

	public ServiceComponentProp getCausingComponent() {
		return causingComponent;
	}

}
