/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.core.runtime.internal.adaptor.EclipseAdaptorMsg;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;

/**
 * A utility class with some generally useful static methods for adaptor hook implementations
 */
public class AdaptorUtil {
	/** The NULL tag used in bundle storage */
	public static final byte NULL = 0;
	/** The OBJECT tag used in bundle storage */
	public static final byte OBJECT = 1;

	/**
	 * Does a recursive copy of one directory to another.
	 * @param inDir input directory to copy.
	 * @param outDir output directory to copy to.
	 * @throws IOException if any error occurs during the copy.
	 */
	public static void copyDir(File inDir, File outDir) throws IOException {
		String[] files = inDir.list();
		if (files != null && files.length > 0) {
			outDir.mkdir();
			for (int i = 0; i < files.length; i++) {
				File inFile = new File(inDir, files[i]);
				File outFile = new File(outDir, files[i]);
				if (inFile.isDirectory()) {
					copyDir(inFile, outFile);
				} else {
					InputStream in = new FileInputStream(inFile);
					readFile(in, outFile);
				}
			}
		}
	}

	/**
	 * Read a file from an InputStream and write it to the file system.
	 *
	 * @param in InputStream from which to read.
	 * @param file output file to create.
	 * @exception IOException
	 */
	public static void readFile(InputStream in, File file) throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);

			byte buffer[] = new byte[1024];
			int count;
			while ((count = in.read(buffer, 0, buffer.length)) > 0) {
				fos.write(buffer, 0, count);
			}

			fos.close();
			fos = null;

			in.close();
			in = null;
		} catch (IOException e) {
			// close open streams
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException ee) {
					// nothing to do here
				}
			}

			if (in != null) {
				try {
					in.close();
				} catch (IOException ee) {
					// nothing to do here
				}
			}

			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Unable to read file"); //$NON-NLS-1$
				Debug.printStackTrace(e);
			}

			throw e;
		}
	}

	/**
	 * This function performs the equivalent of "rm -r" on a file or directory.
	 *
	 * @param   file file or directory to delete
	 * @return false is the specified files still exists, true otherwise.
	 */
	public static boolean rm(File file) {
		if (file.exists()) {
			if (file.isDirectory()) {
				String list[] = file.list();
				if (list != null) {
					int len = list.length;
					for (int i = 0; i < len; i++) {
						// we are doing a lot of garbage collecting here
						rm(new File(file, list[i]));
					}
				}
			}
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				if (file.isDirectory()) {
					Debug.println("rmdir " + file.getPath()); //$NON-NLS-1$
				} else {
					Debug.println("rm " + file.getPath()); //$NON-NLS-1$
				}
			}

			boolean success = file.delete();

			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				if (!success) {
					Debug.println("  rm failed!!"); //$NON-NLS-1$
				}
			}

			return (success);
		}
		return (true);
	}

	public static String readString(DataInputStream in, boolean intern) throws IOException {
		byte type = in.readByte();
		if (type == NULL)
			return null;
		return intern ? in.readUTF().intern() : in.readUTF();
	}

	public static void writeStringOrNull(DataOutputStream out, String string) throws IOException {
		if (string == null)
			out.writeByte(NULL);
		else {
			out.writeByte(OBJECT);
			out.writeUTF(string);
		}
	}

	public static Version loadVersion(DataInputStream in) throws IOException {
		String versionString = readString(in, false);
		try {
			return Version.parseVersion(versionString);
		} catch (IllegalArgumentException e) {
			return new InvalidVersion(versionString);
		}
	}

	/**
	 * Register a service object.
	 * @param name the service class name
	 * @param service the service object
	 * @param context the registering bundle context
	 * @return the service registration object
	 */
	public static ServiceRegistration register(String name, Object service, BundleContext context) {
		Hashtable properties = new Hashtable(7);
		Dictionary headers = context.getBundle().getHeaders();
		properties.put(Constants.SERVICE_VENDOR, headers.get(Constants.BUNDLE_VENDOR));
		properties.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
		properties.put(Constants.SERVICE_PID, context.getBundle().getBundleId() + "." + service.getClass().getName()); //$NON-NLS-1$
		return context.registerService(name, service, properties);
	}

	public static Dictionary loadManifestFrom(BaseData bundledata) throws BundleException {
		URL url = bundledata.getEntry(Constants.OSGI_BUNDLE_MANIFEST);
		if (url == null)
			return null;
		try {
			return Headers.parseManifest(url.openStream());
		} catch (IOException e) {
			throw new BundleException(NLS.bind(EclipseAdaptorMsg.ECLIPSE_DATA_ERROR_READING_MANIFEST, bundledata.getLocation()), e);
		}
	}

}
