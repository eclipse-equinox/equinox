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
package org.eclipse.equinox.internal.wireadmin;

import org.eclipse.equinox.internal.util.ref.Log;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.wireadmin.*;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class Activator implements BundleActivator, ServiceListener, ServiceFactory {

	static final String PREFIX = "[WireAdmin]: ";

	public static boolean LOG_DEBUG;
	static Log log;

	private WireAdminImpl wireAdmin;
	private ServiceReference cmRef;

	public static BundleContext bc;

	WireReDispatcher wireReDispatcher;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bc) throws Exception {
		Activator.bc = bc;
		log = new Log(bc, false);
		log.setPrintOnConsole(getBoolean("equinox.services.wireadmin.console"));

		LOG_DEBUG = getBoolean("equinox.services.wireadmin.debug");
		log.setDebug(LOG_DEBUG);

		cmRef = bc.getServiceReference(ConfigurationAdmin.class.getName());

		ConfigurationAdmin cm = null;
		if (cmRef != null) {
			cm = (ConfigurationAdmin) bc.getService(cmRef);
		}

		try {
			bc.addServiceListener(this, '(' + Constants.OBJECTCLASS + '=' + ConfigurationAdmin.class.getName() + ')');
		} catch (InvalidSyntaxException ise) {
			/* syntax is valid */
		}

		wireAdmin = new WireAdminImpl(bc, cm);

		try {
			bc.addServiceListener(wireAdmin, "(|(" + Constants.OBJECTCLASS + '=' + WireAdminListener.class.getName() + ')' + '(' + Constants.OBJECTCLASS + '=' + Consumer.class.getName() + ')' + '(' + Constants.OBJECTCLASS + '=' + Producer.class.getName() + "))");
		} catch (InvalidSyntaxException ise) {
			/* syntax is valid */
			// ise.printStackTrace();
		}
		wireReDispatcher = new WireReDispatcher();
		wireReDispatcher.start(bc);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bc) throws Exception {
		if (wireReDispatcher != null) {
			wireReDispatcher.stop();
			wireReDispatcher = null;
		}

		wireAdmin.unregister();

		bc.removeServiceListener(wireAdmin);
		bc.removeServiceListener(this);

		if (cmRef != null) {
			bc.ungetService(cmRef);
			cmRef = null;
		}

		log.close();
		log = null;

		wireAdmin = null;

		bc = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
	 */
	public void serviceChanged(ServiceEvent e) {
		switch (e.getType()) {
			case ServiceEvent.MODIFIED :
			case ServiceEvent.REGISTERED :
				this.cmRef = e.getServiceReference();
				wireAdmin.cm = (ConfigurationAdmin) bc.getService(cmRef);
				break;
			case ServiceEvent.UNREGISTERING :
				this.cmRef = null;
				wireAdmin.cm = null;
				break;
		}
	}

	public Object getService(Bundle bundle, ServiceRegistration registration) {
		return null;
	}

	public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
	}

	public static boolean getBoolean(String property) {
		String prop = (bc != null) ? bc.getProperty(property) : System.getProperty(property);
		return ((prop != null) && prop.equalsIgnoreCase("true"));
	}

	public static int getInteger(String property, int defaultValue) {
		String prop = (bc != null) ? bc.getProperty(property) : System.getProperty(property);
		if (prop != null) {
			try {
				return Integer.decode(prop).intValue();
			} catch (NumberFormatException e) {
				//do nothing
			}
		}
		return defaultValue;
	}
}
