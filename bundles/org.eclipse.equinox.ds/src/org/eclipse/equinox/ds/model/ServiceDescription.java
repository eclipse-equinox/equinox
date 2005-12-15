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
package org.eclipse.equinox.ds.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * This class models the service element.
 * The service element is optional. It describes the service information to be
 * used when a component configuration is to be registered as a service. * 
 * 
 * @version $Revision: 1.2 $
 */
public class ServiceDescription implements Serializable {
	/**
	 * Eclipse-generated <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -4376202637155295067L;

	private boolean servicefactory;
	private List provides;

	public ServiceDescription() {
		servicefactory = false;
		provides = new ArrayList();
	}

	/**
	 * @param servicefactory indicates if servicefactory is set
	 */
	public void setServicefactory(boolean servicefactory) {
		this.servicefactory = servicefactory;
	}

	/**
	 * @return Returns true is servicefactory is set
	 */
	public boolean isServicefactory() {
		return servicefactory;
	}

	/**
	 * @param provide add this provide element to the array of provide elements.
	 */
	public void addProvide(ProvideDescription provide) {
		provides.add(provide);
	}

	/**
	 * @return Returns the array of provide elements.
	 */
	public ProvideDescription[] getProvides() {
		int size = provides.size();
		ProvideDescription[] result = new ProvideDescription[size];
		provides.toArray(result);
		return result;
	}
}
