/*******************************************************************************
 * Copyright (c) 1997-2007 by ProSyst Software GmbH
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
import org.eclipse.equinox.internal.ds.Activator;
import org.eclipse.equinox.internal.ds.ComponentStorage;
import org.eclipse.equinox.internal.ds.model.ServiceComponent;
import org.eclipse.equinox.internal.util.io.ExternalizableDictionary;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;

/**
 * @author Pavlin Dobrev
 * @version 1.1
 */

public class FileStorage extends ComponentStorage {

	//TODO: this constant should be public and shared across other bundles that use the same property. 
	//Probably it should be in the supplement bundle?
	public static final String PROP_CHECK_CONFIG = "osgi.checkConfiguration";

	private BundleContext bc;

	private String[] dbBundlePath = new String[1];
	private String[] dbCompPath = new String[] {null, "COMPONENTS"};
	private static String CUSTOM_DB_NAME = "SCR";
	private File file;
	private ExternalizableDictionary data = new ExternalizableDictionary();
	private StringBuffer pathBuffer = new StringBuffer();
	private String separator;

	public FileStorage(BundleContext bc) {
		this.bc = bc;
		separator = bc.getProperty("path.separator");
		file = bc.getDataFile(CUSTOM_DB_NAME);
		try {
			if (file.exists()) {
				data.readObject(new BufferedInputStream(new FileInputStream(file)));
			}
		} catch (FileNotFoundException e) {
			// should be never thrown
			e.printStackTrace();
		} catch (IOException e) {
			Activator.log.error("[SCR] Error while loading components from data file " + file.getAbsolutePath(), e);
		} catch (Exception e) {
			Activator.log.error("[SCR] Error while loading components from data file " + file.getAbsolutePath(), e);
		}
	}

	public Vector loadComponentDefinitions(long bundleID) {
		Bundle bundle = null;
		try {
			bundle = bc.getBundle(bundleID);
			Vector components = null;
			if (!Activator.DBSTORE) {
				return parseXMLDeclaration(bundle);
			}

			long lastModified;
			// if not dev mode, we simply use the bundle's timestamp
			if (!Activator.getBoolean(PROP_CHECK_CONFIG)) {
				lastModified = bundle.getLastModified();
			} else {
				lastModified = getLastModifiedTimestamp(bundle);
			}

			dbBundlePath[0] = String.valueOf(bundleID);

			String lastModifiedValue = (String) data.get(getPath(dbBundlePath));
			if (lastModifiedValue == null) {
				components = parseXMLDeclaration(bundle);
				if (components != null && components.size() != 0) {
					data.put(getPath(dbBundlePath), "" + lastModified);
					saveComponentDefinitions(components, bundleID);
				}

			} else {
				long dbLastModified = Long.parseLong(lastModifiedValue);
				if (lastModified > dbLastModified) {
					components = parseXMLDeclaration(bundle);
					if (components != null && components.size() != 0) {
						data.put(getPath(dbBundlePath), "" + lastModified);
						saveComponentDefinitions(components, bundleID);
					}

				} else {
					components = loadComponentsFromDB(bundle);
				}
			}
			return components;
		} catch (Throwable e) {
			Activator.log.error("[SCR] Unexpected exception while processing bundle with id " + bundleID + " : " + bundle, e);
			return null;
		}
	}

	private Vector loadComponentsFromDB(Bundle bundle) throws Exception {
		try {
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
		} catch (Throwable t) {
			Activator.log.error("[SCR] Error while loading components from DB", t);
		}
		return null;
	}

	public void deleteComponentDefinitions(long bundleID) throws Exception {
		dbBundlePath[0] = String.valueOf(bundleID);
		data.remove(getPath(dbBundlePath));
	}

	private void saveComponentDefinitions(Vector components, long bundleID) throws Exception {
		try {
			if (components == null || components.size() == 0) {
				return;
			}
			dbCompPath[0] = String.valueOf(bundleID);

			DBObject tmpObj = new DBObject(components);
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			tmpObj.writeObject(buf);
			data.put(getPath(dbCompPath), buf.toByteArray());
			saveFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		//no body
	}

	private void saveFile() {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			try {
				data.writeObject(fos);
				fos.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private String getPath(String path[]) {
		pathBuffer.setLength(0);
		for (int i = 0; i < path.length; i++) {
			pathBuffer.append(path[i]).append(separator);
		}
		return pathBuffer.toString();
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
					return 0;
				}
			}
		}
		return result;
	}

	private ManifestElement[] parseManifestHeader(Bundle bundle) {
		Dictionary headers = bundle.getHeaders();
		String files = (String) headers.get(ComponentConstants.SERVICE_COMPONENT);
		if (files == null)
			return new ManifestElement[0];
		try {
			return ManifestElement.parseHeader(ComponentConstants.SERVICE_COMPONENT, files);
		} catch (BundleException e) {
			Activator.log(bundle.getBundleContext(), LogService.LOG_ERROR, "Error attempting parse manifest element header", e); //$NON-NLS-1$
			return new ManifestElement[0];
		}
	}
}
