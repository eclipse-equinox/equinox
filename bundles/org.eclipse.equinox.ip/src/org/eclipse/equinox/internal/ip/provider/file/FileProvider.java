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
package org.eclipse.equinox.internal.ip.provider.file;

import java.io.*;
import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.equinox.internal.ip.ProvisioningInfoProvider;
import org.eclipse.equinox.internal.ip.impl.Log;
import org.eclipse.equinox.internal.ip.impl.ProvisioningAgent;
import org.eclipse.equinox.internal.ip.provider.BaseProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.provisioning.ProvisioningService;

/**
 * Loads data from inner (in jar) file or from file on hard disk. Only string
 * "TEXT" data is loaded directly into provisioning dictionary. All binaries are
 * load into inner dictionary only and are delivered only to when get method is
 * invoked.
 * 
 * @author Avgustin Marinov
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class FileProvider extends BaseProvider implements ProvisioningInfoProvider {

	/** Name of configuration file. */
	public static final String PROPS_FILE = "/props.txt";

	/** This line points start of TEXT section in props.txt file */
	public static final String TEXT = "#[TEXT]";

	/** This line points start of BINARY section in props.txt file */
	public static final String BINARY = "#[BINARY]";

	/** This property is used to be determined if to be used this allowed. */
	public static final String FILE_SUPPORT = "equinox.provisioning.file.provider.allowed";

	/**
	 * This property is used to be determined if to be provider only if this
	 * provider is started for first.
	 */
	public static final String LOAD_ONCE = "equinox.provisioning.file.load.once";

	/**
	 * This dictionary keeps data that is provided by provider but is not put
	 * into provisioning data.
	 */
	private Dictionary innerDict = new Hashtable();

	/** File that is used as flag if this is first start of provider. */
	private File file;

	/**
	 * @see org.eclipse.equinox.internal.ip.impl.provider.BaseProvider#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bc) throws Exception {
		boolean filesupport = true;
		if (bc.getProperty(FILE_SUPPORT) != null)
			if (bc.getProperty(FILE_SUPPORT).equals("false"))
				filesupport = false;
		if (!filesupport) {
			Log.debug(this + " is not an allowed provider.");
			return;
		}

		file = bc.getDataFile("_mark123");
		boolean loadonce = true;
		if (bc.getProperty(LOAD_ONCE) != null)
			if (bc.getProperty(LOAD_ONCE).equals("false"))
				loadonce = false;
		if (file.exists() && loadonce) {
			Log.debug("File provider have loaded data already!");
			return;
		}
		super.start(bc);
	}

	/**
	 * Loads data from inner (in jar) file or from file on hard disk. Data from
	 * "props" is put into provisioning data dictionary but others entries are
	 * put into "innerDict" and are available if someone request them.
	 * 
	 * @see org.eclipse.equinox.internal.ip.ProvisioningInfoProvider#init(org.osgi.service.provisioning.ProvisioningService)
	 */
	public Dictionary init(ProvisioningService prvSrv) throws Exception {
		boolean loadonce = true;
		if (ProvisioningAgent.bc.getProperty(LOAD_ONCE) != null)
			if (ProvisioningAgent.bc.getProperty(LOAD_ONCE).equals("false"))
				loadonce = false;
		if (file.exists() && loadonce) {
			Log.debug("File provider have loaded data already!");
			return null;
		}
		String externalFileLocation = ProvisioningAgent.bc.getProperty("equinox.provisioning.file.provider.specfile.location");
		BufferedReader reader = new BufferedReader(new InputStreamReader(externalFileLocation == null ? FileProvider.class.getResourceAsStream(PROPS_FILE) : new FileInputStream(externalFileLocation)));
		Dictionary dictionary = new Hashtable(10, 1.0F);
		try {
			ByteArrayOutputStream baos = null;
			byte[] buffer = null;
			int i;

			String line;
			boolean stringMode = true;
			while ((line = reader.readLine()) != null) {
				if (!(line = line.trim()).startsWith("#")) {
					if ((i = line.indexOf('=')) != -1) {
						if (stringMode) {
							dictionary.put(line.substring(0, i), line.substring(i + 1));
						} else {
							if (baos == null) {
								baos = new ByteArrayOutputStream();
								buffer = new byte[256];
							}

							String key = line.substring(0, i);
							String entryName = line.substring(i + 1);
							InputStream is;
							if (externalFileLocation == null) {
								is = FileProvider.class.getResourceAsStream(entryName);
								if (is == null && entryName.length() != 0 && entryName.charAt(0) != '/') {
									is = FileProvider.class.getResourceAsStream('/' + entryName);
								}
							} else
								is = new FileInputStream(entryName);
							for (; //
							(i = is.read(buffer, 0, buffer.length)) != -1; //
							baos.write(buffer, 0, i)) {
							}

							innerDict.put(key, baos.toByteArray());

							baos.reset();
						}
					}
				} else {
					if (line.equalsIgnoreCase(TEXT)) {
						stringMode = true;
					} else if (line.equalsIgnoreCase(BINARY)) {
						stringMode = false;
					}
				}
			}
			if (!file.exists()) {
				FileOutputStream fos = new FileOutputStream(file);
				try {
					fos.write(ZERO_BYTE_ARRAY);
				} finally {
					fos.close();
				}
			}
		} finally {
			reader.close();
		}
		return dictionary;
	}

	private static final byte[] ZERO_BYTE_ARRAY = new byte[] {0};

	/**
	 * Provides value for this key form "innnerDict".
	 * 
	 * @param key
	 *            the key.
	 * @return the value.
	 */
	public Object get(Object key) {
		return innerDict.get(key);
	}

	/**
	 * Returns name of this provider.
	 * 
	 * @return name of provider.
	 */
	public String toString() {
		return "File";
	}
}
