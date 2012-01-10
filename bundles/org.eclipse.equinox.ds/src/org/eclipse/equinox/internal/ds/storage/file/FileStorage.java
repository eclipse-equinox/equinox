/*******************************************************************************
 * Copyright (c) 1997, 2010 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ds.storage.file;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Vector;
import org.eclipse.equinox.internal.ds.*;
import org.eclipse.equinox.internal.ds.model.ServiceComponent;
import org.eclipse.equinox.internal.util.io.ExternalizableDictionary;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;

/**
 * This class implements a cache for the parsed component XML descriptions. 
 * 
 * @author Pavlin Dobrev
 * @author Stoyan Boshev
 */

public class FileStorage extends ComponentStorage {

	//TODO: this constant should be public and shared across other bundles that use the same property. 
	//Probably it should be in the supplement bundle?
	public static final String PROP_CHECK_CONFIG = "osgi.checkConfiguration"; //$NON-NLS-1$

	private static String CUSTOM_DB_NAME = "SCR"; //$NON-NLS-1$
	private BundleContext bc = null;
	private ExternalizableDictionary data = new ExternalizableDictionary();
	private StringBuffer pathBuffer = new StringBuffer();
	private String separator;
	private boolean isDirty = false;

	public FileStorage(BundleContext bc) {
		this.bc = bc;
		separator = bc.getProperty("path.separator"); //$NON-NLS-1$
		File file = bc.getDataFile(CUSTOM_DB_NAME);
		FileInputStream fis = null;
		try {
			if (file != null && file.exists()) {
				data.readObject(new BufferedInputStream(fis = new FileInputStream(file)));
			}
		} catch (IOException e) {
			Activator.log(null, LogService.LOG_ERROR, NLS.bind(Messages.ERROR_LOADING_DATA_FILE, file.getAbsolutePath()), e);
		} catch (Exception e) {
			Activator.log(null, LogService.LOG_ERROR, NLS.bind(Messages.ERROR_LOADING_DATA_FILE, file.getAbsolutePath()), e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

	public Vector loadComponentDefinitions(Bundle bundle, String dsHeader) {
		try {
			Vector components = null;
			if (!Activator.DBSTORE) {
				return parseXMLDeclaration(bundle, dsHeader);
			}

			long lastModified;
			// if not dev mode, we simply use the bundle's timestamp
			if (!Activator.getBoolean(PROP_CHECK_CONFIG)) {
				lastModified = bundle.getLastModified();
			} else {
				lastModified = getLastModifiedTimestamp(bundle);
			}

			String[] dbBundlePath = new String[1];
			dbBundlePath[0] = String.valueOf(bundle.getBundleId());

			String lastModifiedValue = (String) data.get(getPath(dbBundlePath));
			if (lastModifiedValue == null) {
				components = processXMLDeclarations(bundle, dsHeader, dbBundlePath, lastModified);
			} else {
				long dbLastModified = Long.parseLong(lastModifiedValue);
				if (lastModified != dbLastModified) {
					components = processXMLDeclarations(bundle, dsHeader, dbBundlePath, lastModified);
				} else {
					try {
						components = loadComponentsFromDB(bundle);
					} catch (Throwable t) {
						Activator.log(null, LogService.LOG_ERROR, Messages.ERROR_LOADING_COMPONENTS, t);
						//backup plan - parse the bundle's component XML declarations
						components = processXMLDeclarations(bundle, dsHeader, dbBundlePath, lastModified);
					}
				}
			}
			return components;
		} catch (Throwable e) {
			Activator.log(null, LogService.LOG_ERROR, NLS.bind(Messages.PROCESSING_BUNDLE_FAILED, Long.toString(bundle.getBundleId()), bundle), e);
			return null;
		}
	}

	private Vector processXMLDeclarations(Bundle bundle, String dsHeader, String[] dbBundlePath, long lastModified) throws Exception {
		Vector components = parseXMLDeclaration(bundle, dsHeader);
		if (components != null && components.size() != 0) {
			data.put(getPath(dbBundlePath), "" + lastModified); //$NON-NLS-1$
			saveComponentDefinitions(components, bundle.getBundleId());
		}
		return components;
	}

	private Vector loadComponentsFromDB(Bundle bundle) throws Exception {
		String[] dbCompPath = new String[] {null, "COMPONENTS"}; //$NON-NLS-1$
		ServiceComponent currentComponent = null;
		long bundleId = bundle.getBundleId();
		dbCompPath[0] = String.valueOf(bundleId);
		DBObject value = new DBObject();
		byte[] byteArr = (byte[]) data.get(getPath(dbCompPath));
		ByteArrayInputStream tmpIn = new ByteArrayInputStream(byteArr);
		value.readObject(tmpIn);
		Vector components = value.components;
		if (components == null) {
			return null;
		}
		for (int i = 0; i < components.size(); i++) {
			currentComponent = (ServiceComponent) components.elementAt(i);
			currentComponent.bundle = bundle;
			currentComponent.bc = bundle.getBundleContext();
		}
		return components;
	}

	public void deleteComponentDefinitions(long bundleID) {
		String[] dbBundlePath = new String[1];
		dbBundlePath[0] = String.valueOf(bundleID);
		data.remove(getPath(dbBundlePath));
		String[] dbCompPath = new String[] {null, "COMPONENTS"}; //$NON-NLS-1$
		dbCompPath[0] = String.valueOf(bundleID);
		data.remove(getPath(dbCompPath));
		File file = bc.getDataFile(CUSTOM_DB_NAME);
		if (file != null && file.exists()) {
			//delete the file to prevent leaving old information in it
			file.delete();
		}
		isDirty = true;
	}

	private void saveComponentDefinitions(Vector components, long bundleID) throws Exception {
		try {
			if (components == null || components.size() == 0) {
				return;
			}
			String[] dbCompPath = new String[] {null, "COMPONENTS"}; //$NON-NLS-1$
			dbCompPath[0] = String.valueOf(bundleID);

			DBObject tmpObj = new DBObject(components);
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			tmpObj.writeObject(buf);
			data.put(getPath(dbCompPath), buf.toByteArray());
			isDirty = true;
		} catch (Exception e) {
			Activator.log(null, LogService.LOG_ERROR, Messages.ERROR_SAVING_COMPONENT_DEFINITIONS, e);
		}
	}

	public void stop() {
		if (isDirty) {
			saveFile();
		}
	}

	private void saveFile() {
		FileOutputStream fos = null;
		try {
			File file = bc.getDataFile(CUSTOM_DB_NAME);
			if (file == null) {
				//save operation is not possible
				return;
			}
			fos = new FileOutputStream(file);
			try {
				data.writeObject(fos);
				isDirty = false;
			} catch (Exception e) {
				Activator.log(null, LogService.LOG_ERROR, Messages.ERROR_WRITING_OBJECT, e);
			}
		} catch (FileNotFoundException e) {
			Activator.log(null, LogService.LOG_ERROR, Messages.FILE_DOESNT_EXIST_OR_DIRECTORY, e);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					//ignore
				}
			}
		}
	}

	private String getPath(String path[]) {
		synchronized (pathBuffer) {
			pathBuffer.setLength(0);
			for (int i = 0; i < path.length; i++) {
				pathBuffer.append(path[i]).append(separator);
			}
			return pathBuffer.toString();
		}
	}

	/**
	 * The last modified timestamp of the bundle. Should only be called in development mode
	 * 
	 * @param bundle
	 * @return the last modified timestamp of the bundle
	 */
	protected long getLastModifiedTimestamp(Bundle bundle) {
		if (bundle == null)
			return 0;
		long result = 0;
		ManifestElement[] elements = parseManifestHeader(bundle);
		for (int i = 0; i < elements.length; i++) {
			URL componentURL = bundle.getEntry(elements[i].getValue());
			if (componentURL != null) {
				try {
					URLConnection connection = componentURL.openConnection();
					long lastModified = connection.getLastModified();
					if (lastModified > result)
						result = lastModified;
				} catch (IOException e) {
					//last modified cannot be calculated. should force reparse 
					return Long.MAX_VALUE;
				}
			}
		}
		return result;
	}

	private ManifestElement[] parseManifestHeader(Bundle bundle) {
		Dictionary headers = bundle.getHeaders(""); //$NON-NLS-1$
		String files = (String) headers.get(ComponentConstants.SERVICE_COMPONENT);
		if (files == null)
			return new ManifestElement[0];
		try {
			return ManifestElement.parseHeader(ComponentConstants.SERVICE_COMPONENT, files);
		} catch (BundleException e) {
			Activator.log(bundle.getBundleContext(), LogService.LOG_ERROR, Messages.ERROR_PARSING_MANIFEST_HEADER, e);
			return new ManifestElement[0];
		}
	}
}
