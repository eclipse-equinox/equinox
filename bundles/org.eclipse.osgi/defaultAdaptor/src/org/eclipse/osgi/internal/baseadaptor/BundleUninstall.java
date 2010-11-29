/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.baseadaptor;

import java.io.IOException;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.adaptor.BundleOperation;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;

public class BundleUninstall implements BundleOperation {
	private BaseData data;
	private BaseStorage storage;

	public BundleUninstall(BaseData data, BaseStorage storage) {
		this.data = data;
		this.storage = storage;
	}

	/**
	 * Perform the change to persistent storage.
	 *
	 * @return Bundle object for the target bundle.
	 * @throws BundleException If a failure occured modifiying peristent storage.
	 */
	public BundleData begin() throws BundleException {
		return data;
	}

	/**
	 * Commit the change to persistent storage.
	 *
	 * @param postpone If true, the bundle's persistent
	 * storage cannot be immediately reclaimed.
	 * @throws BundleException If a failure occured modifiying peristent storage.
	 */
	public void commit(boolean postpone) throws BundleException {
		BaseStorageHook storageHook = (BaseStorageHook) data.getStorageHook(BaseStorageHook.KEY);
		try {
			storageHook.delete(postpone, BaseStorageHook.DEL_BUNDLE_STORE);
		} catch (IOException e) {
			// nothing we can do
		}
		storage.processExtension(data, BaseStorage.EXTENSION_UNINSTALLED);
		data.setLastModified(System.currentTimeMillis());
		storage.updateState(data, BundleEvent.UNINSTALLED);
		data.setDirty(true);
		try {
			data.save();
		} catch (IOException e) {
			throw new BundleException(AdaptorMsg.ADAPTOR_STORAGE_EXCEPTION, e);
		}
	}

	/**
	 * Undo the change to persistent storage.
	 *
	 * @throws BundleException If a failure occured modifiying peristent storage.
	 */
	public void undo() throws BundleException {
		// do nothing
	}

}
