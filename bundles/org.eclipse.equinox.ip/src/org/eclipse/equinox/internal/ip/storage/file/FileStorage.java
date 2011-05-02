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
package org.eclipse.equinox.internal.ip.storage.file;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.eclipse.equinox.internal.ip.ProvisioningStorage;
import org.eclipse.equinox.internal.ip.impl.Log;
import org.eclipse.equinox.internal.ip.impl.ProvisioningAgent;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.provisioning.ProvisioningService;

/**
 * File Storage.
 * 
 * @author Avgustin Marinov,
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class FileStorage implements ProvisioningStorage, BundleActivator {

	/** Name of data file used for storing configuration. */
	public static final String FILE_NAME = "_storage.zip";

	/** Bundle context. */
	private BundleContext bc;

	/**
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bc) throws Exception {
		Log.debug = ProvisioningAgent.getBoolean("equinox.provisioning.debug");
		this.bc = bc;
	}

	/**
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bc) throws Exception {
	}

	/**
	 * @see org.eclipse.equinox.internal.ip.ProvisioningStorage#getStoredInfo()
	 */
	public Dictionary getStoredInfo() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(bc.getDataFile(FILE_NAME));
		} catch (Exception e) {
			return null;
		}
		try {
			Dictionary toAdd = new Hashtable();
			getInfo(fis, toAdd);
			fis.close();
			return toAdd;
		} catch (Exception e) {
			Log.debug(e);
			return null;
		}
	}

	/**
	 * @see org.eclipse.equinox.internal.ip.ProvisioningStorage#store(java.util.Dictionary)
	 */
	public void store(Dictionary dictionary) {
		try {
			FileOutputStream fos = new FileOutputStream(bc.getDataFile(FILE_NAME));
			saveInfo(fos, dictionary);
			fos.close();
		} catch (Exception e) {
			Log.debug(e);
		}
	}

	/**
	 * Returns string name of storage.
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "File Storage";
	}

	/**
	 * Decodes data from input stream and loads it in passed dictionary.
	 * 
	 * @param is
	 *            input stream in specific format.
	 * @param info
	 *            dictionary.
	 */
	private void getInfo(InputStream is, Dictionary info) {
		if (is != null) {
			try {
				ZipInputStream zis = new ZipInputStream(is);
				ZipEntry ze;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int read;
				byte[] buff = new byte[256];
				while ((ze = zis.getNextEntry()) != null) {
					if ("props.txt".equals(ze.getName())) {
						BufferedReader reader = new BufferedReader(new InputStreamReader(zis));
						String line;
						int index;
						while ((line = reader.readLine()) != null) {
							if (!(line = line.trim()).startsWith("#")) {
								if ((index = line.indexOf("=")) != -1) {
									String key = line.substring(0, index);
									if (ProvisioningService.PROVISIONING_UPDATE_COUNT.equals(key)) {
										int uc = 0;
										try {
											uc = Integer.parseInt(line.substring(index + 1).trim());
										} catch (Exception _) {
										}
										info.put(key, new Integer(uc));
									} else {
										info.put(key, line.substring(index + 1));
									}
								}
							}
						}
					} else {
						while ((read = zis.read(buff)) != -1) {
							baos.write(buff, 0, read);
						}
						info.put(ze.getName(), baos.toByteArray());
						baos.reset();
					}
					zis.closeEntry();
				}
			} catch (Exception e) {
				Log.debug(e);
				// What is loaded is loaded.
			}
		}
	}

	/**
	 * Encodes "info" and saves it into an output stream.
	 * 
	 * @param os
	 *            output stream where to be saved data.
	 */
	private void saveInfo(OutputStream os, Dictionary info) {
		try {
			ZipOutputStream zos = new ZipOutputStream(os);
			ByteArrayOutputStream strings = new ByteArrayOutputStream();
			Object key;
			Object value;
			for (Enumeration e = info.keys(); e.hasMoreElements();) {
				key = e.nextElement();
				value = info.get(key);
				if (value instanceof String || ProvisioningService.PROVISIONING_UPDATE_COUNT.equals(key)) {
					strings.write((key + "=" + value + "\r\n").getBytes());
				} else if (value instanceof byte[]) {
					ZipEntry ze = new ZipEntry((String) key);
					zos.putNextEntry(ze);
					zos.write((byte[]) value);
					zos.closeEntry();
				}
			}
			ZipEntry ze = new ZipEntry("props.txt");
			zos.putNextEntry(ze);
			zos.write(strings.toByteArray());
			zos.closeEntry();
			zos.close();
		} catch (Exception e) {
			Log.debug(e);
			// What is load is load.
		}
	}
}
