/*
 * Copyright (c) The Open Services Gateway Initiative (2000, 2001).
 * All Rights Reserved.
 *
 * Implementation of certain elements of the Open Services Gateway Initiative (OSGI)
 * Specification may be subject to third party intellectual property rights,
 * including without limitation, patent rights (such a third party may or may
 * not be a member of OSGi). OSGi is not responsible and shall not be held
 * responsible in any manner for identifying or failing to identify any or
 * all such third party intellectual property rights.
 *
 * This document and the information contained herein are provided on an "AS IS"
 * basis and OSGI DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO ANY WARRANTY THAT THE USE OF THE INFORMATION HEREIN WILL NOT
 * INFRINGE ANY RIGHTS AND ANY IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. IN NO EVENT WILL OSGI BE LIABLE FOR ANY LOSS OF
 * PROFITS, LOSS OF BUSINESS, LOSS OF USE OF DATA, INTERRUPTION OF BUSINESS,
 * OR FOR DIRECT, INDIRECT, SPECIAL OR EXEMPLARY, INCIDENTIAL, PUNITIVE OR
 * CONSEQUENTIAL DAMAGES OF ANY KIND IN CONNECTION WITH THIS DOCUMENT OR THE
 * INFORMATION CONTAINED HEREIN, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH LOSS OR DAMAGE.
 *
 * All Company, brand and product names may be trademarks that are the sole
 * property of their respective owners. All rights reserved.
 */
package org.eclipse.osgi.framework.internal.core;

import org.osgi.framework.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;

/**
 * Service Factory class, providing StartLevel objects
 * to those requesting org.osgi.service.startlevel.StartLevel service. 
 */
public class StartLevelFactory implements ServiceFactory {

	/** need a reference to the framework */
	Framework framework;

	protected StartLevelFactory(Framework framework) {
		this.framework = framework;
	}

	/**
	 * Returns a StartLevel object, created for each requesting bundle.
	 * 
	 * @param   callerBundle  bundle, requested to get StartLevel service.
	 * @pre callerBundle!=null
	 * @param   sReg  ServiceRegistration of the StartLevel service
	 * @pre sReg!=null
	 * @return  StartLevel object
	 */
	public Object getService(Bundle callerBundle, ServiceRegistration sReg) {
		return new StartLevel(callerBundle, framework);
	}

	/**
	 * Does nothing, as the StartLevel bundle does not keep references to StartLevel objects.
	 *
	 * @param   callerBundle  bundle requesting to unget StartLevel service.
	 * @param   sReg  ServiceRegistration of StartLevel
	 * @param   obj  Service object, already been got by this bundle.
	 */
	public void ungetService(Bundle callerBundle, ServiceRegistration sReg, Object obj) {
	}
}