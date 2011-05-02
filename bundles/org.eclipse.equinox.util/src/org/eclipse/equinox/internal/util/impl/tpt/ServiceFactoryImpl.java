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

package org.eclipse.equinox.internal.util.impl.tpt;

import org.eclipse.equinox.internal.util.UtilActivator;
import org.eclipse.equinox.internal.util.ref.Log;
import org.osgi.framework.*;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public abstract class ServiceFactoryImpl implements ServiceFactory {

	public String bundleName;
	public static Log log;

	private static Bundle systemBundle = null;
	static {
		try {
			systemBundle = UtilActivator.bc.getBundle(0);
		} catch (Exception e) {
		}
	}
	static boolean emptyStorage;

	private static boolean security = Log.security();

	public static boolean privileged() {
		emptyStorage = UtilActivator.bc.getProperty("equinox.storage.empty") != null;
		return ((systemBundle.getState() != Bundle.STARTING) || emptyStorage) && security;
	}

	public static boolean useNames = true;
	static String suseNames;

	public ServiceFactoryImpl(String bundleName, Log log) {
		this.bundleName = bundleName;
		ServiceFactoryImpl.log = log;

		String tmp = UtilActivator.bc.getProperty("equinox.util.threadpool.useNames");
		if (suseNames != tmp)
			useNames = tmp == null || !tmp.equals("false");
	}

	public ServiceFactoryImpl(String bundleName) {
		this.bundleName = bundleName;
	}

	public Object getService(Bundle caller, ServiceRegistration sReg) {
		return getInstance(useNames ? getName(caller) : null);
	}

	public static String getName(Bundle caller) {
		StringBuffer bf = new StringBuffer(13);
		bf.append(" (Bundle ");
		bf.append(caller.getBundleId());
		bf.append(')');
		return bf.toString();
	}

	/**
	 * Nothing to be done here.
	 * 
	 * @param caller
	 *            caller bundle, which releases the factory instance
	 * @param sReg
	 * @param service
	 *            object that is released
	 */
	public void ungetService(Bundle caller, ServiceRegistration sReg, Object service) {
	}

	public abstract Object getInstance(String bundleName);
}
