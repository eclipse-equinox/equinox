/*******************************************************************************
 * Copyright (c) 2005, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.storage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.osgi.internal.debug.Debug;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * A utility class with some generally useful static methods for adaptor hook implementations
 */
public class StorageUtil {
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
			for (String file : files) {
				File inFile = new File(inDir, file);
				File outFile = new File(outDir, file);
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
	 * @param in InputStream from which to read. This stream will be closed by this method.
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
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ee) {
					// nothing to do here
				}
			}

			if (fos != null) {
				try {
					fos.close();
				} catch (IOException ee) {
					// nothing to do here
				}
			}
		}
	}

	/**
	 * This function performs the equivalent of "rm -r" on a file or directory.
	 *
	 * @param   file file or directory to delete
	 * @return false is the specified files still exists, true otherwise.
	 */
	public static boolean rm(File file, boolean DEBUG) {
		if (file.exists()) {
			if (file.isDirectory()) {
				String list[] = file.list();
				if (list != null) {
					int len = list.length;
					for (int i = 0; i < len; i++) {
						// we are doing a lot of garbage collecting here
						rm(new File(file, list[i]), DEBUG);
					}
				}
			}
			if (DEBUG) {
				if (file.isDirectory()) {
					Debug.println("rmdir " + file.getPath()); //$NON-NLS-1$
				} else {
					Debug.println("rm " + file.getPath()); //$NON-NLS-1$
				}
			}

			boolean success = file.delete();

			if (DEBUG) {
				if (!success) {
					Debug.println("  rm failed!"); //$NON-NLS-1$
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

	/**
	 * Register a service object.
	 * @param name the service class name
	 * @param service the service object
	 * @param context the registering bundle context
	 * @return the service registration object
	 */
	public static ServiceRegistration<?> register(String name, Object service, BundleContext context) {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		properties.put(Constants.SERVICE_PID, context.getBundle().getBundleId() + "." + service.getClass().getName()); //$NON-NLS-1$
		return context.registerService(name, service, properties);
	}

	public static boolean canWrite(File installDir) {
		if (!installDir.isDirectory())
			return false;

		if (Files.isWritable(installDir.toPath()))
			return true;

		File fileTest = null;
		try {
			// we use the .dll suffix to properly test on Vista virtual directories
			// on Vista you are not allowed to write executable files on virtual directories
			// like "Program Files"
			fileTest = File.createTempFile("writableArea", ".dll", installDir); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IOException e) {
			// If an exception occured while trying to create the file, it means that it is
			// not writtable
			return false;
		} finally {
			if (fileTest != null)
				fileTest.delete();
		}
		return true;
	}

	public static URL encodeFileURL(File file) throws MalformedURLException {
		return file.toURI().toURL();
	}

	public static byte[] getBytes(InputStream in, int length, int BUF_SIZE) throws IOException {
		byte[] classbytes;
		int bytesread = 0;
		int readcount;
		try {
			if (length > 0) {
				classbytes = new byte[length];
				for (; bytesread < length; bytesread += readcount) {
					readcount = in.read(classbytes, bytesread, length - bytesread);
					if (readcount <= 0) /* if we didn't read anything */
						break; /* leave the loop */
				}
			} else /* does not know its own length! */ {
				length = BUF_SIZE;
				classbytes = new byte[length];
				readloop: while (true) {
					for (; bytesread < length; bytesread += readcount) {
						readcount = in.read(classbytes, bytesread, length - bytesread);
						if (readcount <= 0) /* if we didn't read anything */
							break readloop; /* leave the loop */
					}
					byte[] oldbytes = classbytes;
					length += BUF_SIZE;
					classbytes = new byte[length];
					System.arraycopy(oldbytes, 0, classbytes, 0, bytesread);
				}
			}
			if (classbytes.length > bytesread) {
				byte[] oldbytes = classbytes;
				classbytes = new byte[bytesread];
				System.arraycopy(oldbytes, 0, classbytes, 0, bytesread);
			}
		} finally {
			try {
				in.close();
			} catch (IOException ee) {
				// nothing to do here
			}
		}
		return classbytes;
	}

	public static void move(File from, File to, boolean DEBUG) throws IOException {
		try {
			Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			if (DEBUG) {
				Debug.println("Failed to move atomically: " + from + " to " + to); //$NON-NLS-1$ //$NON-NLS-2$
			}
			// remove in case it failed because the target to non-empty directory or
			// the target type does not match the from
			rm(to, DEBUG);
			// also, try without atomic operation
			Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		if (DEBUG) {
			Debug.println("Successfully moved file: " + from + " to " + to); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}
