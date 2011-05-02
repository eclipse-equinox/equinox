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
package org.eclipse.equinox.internal.ip.impl;

import java.util.*;
import org.eclipse.equinox.internal.ip.ProvisioningInfoProvider;
import org.osgi.service.provisioning.ProvisioningService;

/**
 * Provisioning data implementation. This Dictionary do not store a Strings with
 * length 0 (skips them). Also skips on put values "null".
 * 
 * @author Avgustin Marinov
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class ProvisioningData extends Hashtable {

	private static final long serialVersionUID = 1L;

	/** List of all providers used for data load. The highest ranking is last. */
	Vector providers = new Vector();

	public final static String PROVISIONING_ERROR = "provisioning.error";

	/**
	 * Constructs ProvisioningData.
	 * 
	 * @param bc
	 *            bundle context.
	 * @param agent
	 *            ProvisioningAgent.
	 */
	ProvisioningData() {
		super.put(ProvisioningService.PROVISIONING_UPDATE_COUNT, new Integer(0));
	}

	/**
	 * @see java.util.Hashtable#put(java.lang.Object, java.lang.Object)
	 */
	public Object put(Object key, Object value) {
		throw newUnsupportedOperation("put");
	}

	public Object remove(Object key) {
		throw newUnsupportedOperation("remove");
	}

	/**
	 * Gets value for key using all providers. The service with highest ranking.
	 * 
	 * @see java.util.Hashtable#get(java.lang.Object)
	 */
	public synchronized Object get(Object key) {
		Object value = super.get(key);
		Object emptyCash = null;
		// if somewhere have "" or byte[0] it will be overiden next ocurence of
		// the
		// value is found - for CM set "" when there is not value set
		boolean hasEmptyString = value != null && isEmpty(value);
		if (hasEmptyString)
			emptyCash = value;
		if (value == null || hasEmptyString) {
			synchronized (providers) {
				for (int i = providers.size(); i-- > 0;) {
					value = ((ProvisioningInfoProvider) providers.elementAt(i)).get(key);
					if (value != null && !isEmpty(value)) {
						break;
					}
					if (value != null && !hasEmptyString && isEmpty(value)) {
						if (!hasEmptyString)
							emptyCash = value;
						hasEmptyString = true;
					}
				}
			}
		}
		if (value == null && hasEmptyString) {
			value = emptyCash;
		}
		return value;
	}

	void set(Dictionary newData) {
		super.clear();
		add(newData);
	}

	void add(Dictionary toAdd) {
		if (toAdd == null) {
			return;
		}
		for (Enumeration e = toAdd.keys(); e.hasMoreElements();) {
			Object key = e.nextElement();
			if (key instanceof String) {
				Object value = toAdd.get(key);
				if (value instanceof String || value instanceof byte[] || (key.equals(ProvisioningService.PROVISIONING_UPDATE_COUNT) && value instanceof Integer)) {
					if (ProvisioningService.PROVISIONING_REFERENCE.equals(key)) {
						value = ((String) value).trim();
					}
					super.put(key, value);
				}
			}
		}
	}

	void putUC(Integer uc) {
		super.put(ProvisioningService.PROVISIONING_UPDATE_COUNT, uc);
	}

	void putPrivate(String key, String value) {
		if (value == null) {
			super.remove(key);
		} else {
			super.put(key, value);
		}
	}

	void setError(int code, String message) {
		String value = code + " " + ((message != null) ? message : " No details.");
		super.put(PROVISIONING_ERROR, value);
	}

	void clearError() {
		super.remove(PROVISIONING_ERROR);
	}

	private final boolean isEmpty(Object value) {
		return (value instanceof String && ((String) value).length() == 0) || (value instanceof byte[] && ((byte[]) value).length == 0);
	}

	private final RuntimeException newUnsupportedOperation(String methodName) {
		RuntimeException e;
		try {
			e = (RuntimeException) Class.forName("java.lang.UnsupportedOperationException").newInstance();
		} catch (Throwable t) {
			e = new IllegalStateException(methodName + " is unsupported method for provisioning dictionary!");
		}
		return e;
	}

}
