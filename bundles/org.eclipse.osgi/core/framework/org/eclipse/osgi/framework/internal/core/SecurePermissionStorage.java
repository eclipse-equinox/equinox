/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

import java.io.IOException;
import java.security.*;
import org.eclipse.osgi.framework.adaptor.PermissionStorage;

/**
 * PermissionStorage privileged action class.
 */

public class SecurePermissionStorage implements PermissionStorage, PrivilegedExceptionAction {
	private PermissionStorage storage;
	private String location;
	private String[] data;
	private String[] infos;
	private int action;
	private static final int GET = 1;
	private static final int SET = 2;
	private static final int LOCATION = 3;
	private static final int GET_INFOS = 4;
	private static final int SAVE_INFOS = 5;

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
			case SAVE_INFOS :
				storage.saveConditionalPermissionInfos(data);
				return null;
			case GET_INFOS :
				return storage.getConditionalPermissionInfos();
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

	public void saveConditionalPermissionInfos(String[] infos) throws IOException {
		this.action = SAVE_INFOS;
		this.infos = infos;
		try {
			AccessController.doPrivileged(this);
		} catch (PrivilegedActionException e) {
			throw (IOException) e.getException();
		}

	}

	public String[] getConditionalPermissionInfos() throws IOException {
		this.action = GET_INFOS;
		try {
			return (String[]) AccessController.doPrivileged(this);
		} catch (PrivilegedActionException e) {
			throw (IOException) e.getException();
		}
	}
}
