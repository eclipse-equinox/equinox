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

package org.eclipse.osgi.framework.util;

import java.io.IOException;
import java.io.Serializable;
import java.security.*;
import org.eclipse.osgi.framework.adaptor.PermissionStorage;

/**
 * PermissionStorage privileged action class.
 */

public class SecurePermissionStorage implements PermissionStorage, PrivilegedExceptionAction {
	private PermissionStorage storage;
	private String location;
	private String[] data;
	private Serializable obj;
	private int action;
	private static final int GET = 1;
	private static final int SET = 2;
	private static final int LOCATION = 3;
	private static final int DESERIALIZE = 4;
	private static final int SERIALIZE = 5;

	public SecurePermissionStorage(PermissionStorage storage) {
		this.storage = storage;
	}

	public Object run() throws IOException {
		switch (action) {
			case GET :
				return storage.getPermissionData(location);
			case SET :
				storage.setPermissionData(location, data);
				return null;
			case LOCATION :
				return storage.getLocations();
			case SERIALIZE :
				storage.serializeConditionalPermissionInfos(obj);
				return null;
			case DESERIALIZE :
				return storage.deserializeConditionalPermissionInfos();
		}

		throw new UnsupportedOperationException();
	}

	public String[] getPermissionData(String location) throws IOException {
		this.location = location;
		this.action = GET;

		try {
			return (String[]) AccessController.doPrivileged(this);
		} catch (PrivilegedActionException e) {
			throw (IOException) e.getException();
		}
	}

	public String[] getLocations() throws IOException {
		this.action = LOCATION;

		try {
			return (String[]) AccessController.doPrivileged(this);
		} catch (PrivilegedActionException e) {
			throw (IOException) e.getException();
		}
	}

	public void setPermissionData(String location, String[] data) throws IOException {
		this.location = location;
		this.data = data;
		this.action = SET;

		try {
			AccessController.doPrivileged(this);
		} catch (PrivilegedActionException e) {
			throw (IOException) e.getException();
		}
	}

	public void serializeConditionalPermissionInfos(Serializable o) throws IOException {
		this.action = SERIALIZE;
		this.obj = o;
		try {
			AccessController.doPrivileged(this);
		} catch (PrivilegedActionException e) {
			throw (IOException) e.getException();
		}

	}

	public Object deserializeConditionalPermissionInfos() throws IOException {
		this.action = DESERIALIZE;
		try {
			return AccessController.doPrivileged(this);
		} catch (PrivilegedActionException e) {
			throw (IOException) e.getException();
		}
	}
}