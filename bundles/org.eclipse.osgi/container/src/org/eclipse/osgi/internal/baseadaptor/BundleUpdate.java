/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.baseadaptor;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.hooks.StorageHook;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.adaptor.BundleOperation;
import org.eclipse.osgi.framework.internal.core.ReferenceInputStream;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;

public class BundleUpdate implements BundleOperation {
	private BaseData data;
	private BaseData newData;
	private URLConnection source;
	private BaseStorage storage;

	public BundleUpdate(BaseData data, URLConnection source, BaseStorage storage) {
		this.data = data;
		this.source = source;
		this.storage = storage;
	}

	/**
	 * Perform the change to persistent storage.
	 *
	 * @return Bundle object for the target bundle.
	 * @throws BundleException if an error occurs
	 */
	public BundleData begin() throws BundleException {
		try {
			newData = storage.createBaseData(data.getBundleID(), data.getLocation());
			newData.setLastModified(System.currentTimeMillis());
			newData.setStartLevel(data.getStartLevel());
			newData.setStatus(data.getStatus());
			// load the storage hooks into the new data
			StorageHook[] storageHooks = data.getAdaptor().getHookRegistry().getStorageHooks();
			StorageHook[] instanceHooks = new StorageHook[storageHooks.length];
			for (int i = 0; i < storageHooks.length; i++) {
				instanceHooks[i] = storageHooks[i].create(newData);
				instanceHooks[i].copy(data.getStorageHook((String) instanceHooks[i].getKey()));
			}
			newData.setStorageHooks(instanceHooks);
			// get the new eclipse storage hooks
			BaseStorageHook newStorageHook = (BaseStorageHook) newData.getStorageHook(BaseStorageHook.KEY);
			InputStream in = source.getInputStream();
			URL sourceURL = source.getURL();
			String protocol = sourceURL == null ? null : sourceURL.getProtocol();
			try {
				if (in instanceof ReferenceInputStream) {
					URL reference = ((ReferenceInputStream) in).getReference();
					if (!"file".equals(reference.getProtocol())) //$NON-NLS-1$
						throw new BundleException(NLS.bind(AdaptorMsg.ADAPTOR_URL_CREATE_EXCEPTION, reference));
					// check to make sure we are not just trying to update to the same
					// directory reference.  This would be a no-op.
					String path = reference.getPath();
					newStorageHook.setReference(true);
					newStorageHook.setFileName(path);
				} else {
					File genDir = newStorageHook.createGenerationDir();
					if (!genDir.exists())
						throw new BundleException(NLS.bind(AdaptorMsg.ADAPTOR_DIRECTORY_CREATE_EXCEPTION, genDir.getPath()));
					newStorageHook.setReference(false);
					newStorageHook.setFileName(BaseStorage.BUNDLEFILE_NAME);
					File outFile = new File(genDir, newStorageHook.getFileName());
					if ("file".equals(protocol)) { //$NON-NLS-1$
						File inFile = new File(source.getURL().getPath());
						if (inFile.isDirectory()) {
							AdaptorUtil.copyDir(inFile, outFile);
						} else {
							AdaptorUtil.readFile(in, outFile);
						}
					} else {
						AdaptorUtil.readFile(in, outFile);
					}
				}
				Dictionary<String, String> manifest = storage.loadManifest(newData, true);
				for (int i = 0; i < instanceHooks.length; i++)
					instanceHooks[i].initialize(manifest);
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (IOException ee) {
					// nothing to do here
				}
			}
		} catch (IOException e) {
			throw new BundleException(AdaptorMsg.BUNDLE_READ_EXCEPTION, BundleException.READ_ERROR, e);
		}

		return (newData);
	}

	/**
	 * Commit the change to persistent storage.
	 *
	 * @param postpone If true, the bundle's persistent
	 * storage cannot be immediately reclaimed.
	 * @throws BundleException If a failure occured modifiying peristent storage.
	 */

	public void commit(boolean postpone) throws BundleException {
		storage.processExtension(data, BaseStorage.EXTENSION_UNINSTALLED); // remove the old extension
		storage.processExtension(newData, BaseStorage.EXTENSION_UPDATED); // update to the new one
		newData.setLastModified(System.currentTimeMillis()); // save the last modified
		storage.updateState(newData, BundleEvent.UPDATED);
		try {
			newData.save();
		} catch (IOException e) {
			throw new BundleException(AdaptorMsg.ADAPTOR_STORAGE_EXCEPTION, e);
		}
		BaseStorageHook oldStorageHook = (BaseStorageHook) data.getStorageHook(BaseStorageHook.KEY);
		try {
			oldStorageHook.delete(postpone, BaseStorageHook.DEL_GENERATION);
		} catch (IOException e) {
			data.getAdaptor().getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, data.getBundle(), e);
		}
	}

	/**
	 * Undo the change to persistent storage.
	 *
	 * @throws BundleException If a failure occured modifiying peristent storage.
	 */
	public void undo() throws BundleException {
		if (newData != null) {
			BaseStorageHook newStorageHook = (BaseStorageHook) newData.getStorageHook(BaseStorageHook.KEY);
			try {
				if (newStorageHook != null)
					newStorageHook.delete(false, BaseStorageHook.DEL_GENERATION);
			} catch (IOException e) {
				data.getAdaptor().getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, data.getBundle(), e);
			}
		}
	}
}
