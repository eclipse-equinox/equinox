/*******************************************************************************
 * Copyright (c) 2005, 2021 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 577432 - Speed up and improve file processing in Storage
 *******************************************************************************/

package org.eclipse.osgi.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.osgi.framework.internal.reliablefile.ReliableFile;
import org.eclipse.osgi.internal.debug.Debug;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * A utility class with some generally useful static methods for adaptor hook
 * implementations
 */
public class StorageUtil {

	/**
	 * Copies the content of the given path (file or directory) to the specified
	 * target. If the source is a directory all contained elements are copied
	 * recursively.
	 * 
	 * @param inFile  input directory to copy.
	 * @param outFile output directory to copy to.
	 * @throws IOException if any error occurs during the copy.
	 */
	public static void copy(File inFile, File outFile) throws IOException {
		Path source = inFile.toPath();
		Path target = outFile.toPath();
		if (Files.exists(source)) {
			Files.createDirectories(target.getParent());
			try (Stream<Path> walk = Files.walk(source)) {
				for (Path s : (Iterable<Path>) walk::iterator) {
					Path t = target.resolve(source.relativize(s));
					Files.copy(s, t, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	/**
	 * Read a file from an InputStream and write it to the file system.
	 *
	 * @param in   InputStream from which to read. This stream will be closed by
	 *             this method.
	 * @param file output file to create.
	 * @exception IOException
	 */
	public static void readFile(InputStream in, File file) throws IOException {
		try (InputStream stream = in) {
			Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * This function performs the equivalent of "rm -r" on a file or directory.
	 *
	 * @param file file or directory to delete
	 * @return false is the specified files still exists, true otherwise.
	 */
	public static boolean rm(File file, boolean DEBUG) {
		Path path = file.toPath();
		if (!Files.exists(path)) {
			return true;
		}
		try {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path f, BasicFileAttributes attrs) {
					return delete(f, DEBUG);
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
					return delete(dir, DEBUG);
				}

				private FileVisitResult delete(Path pathToDelete, boolean debug) {
					try {
						if (debug) {
							Debug.println("rm " + pathToDelete); //$NON-NLS-1$
						}
						Files.delete(pathToDelete);
					} catch (IOException e) {
						if (debug) {
							Debug.println("  rm failed:" + e.getMessage()); //$NON-NLS-1$
						}
					}
					return FileVisitResult.CONTINUE;
				}
			});
			return !Files.exists(path);
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Register a service object.
	 * 
	 * @param name    the service class name
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
			fileTest = ReliableFile.createTempFile("writableArea", ".dll", installDir); //$NON-NLS-1$ //$NON-NLS-2$
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

	private static final boolean IS_WINDOWS = File.separatorChar == '\\';

	// reserved names according to
	// https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file
	private static final Set<String> RESERVED_NAMES = new HashSet<>(Arrays.asList("aux", "com1", "com2", "com3", "com4", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			"com5", "com6", "com7", "com8", "com9", "con", "lpt1", "lpt2", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
			"lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "nul", "prn")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$

	/** Tests whether the filename can escape path into special device **/
	public static boolean isReservedFileName(File file) {
		// Directory names are not checked here because illegal directory names will be
		// handled by OS.
		if (!IS_WINDOWS) { // only windows has special file names which can escape any path
			return false;
		}
		String fileName = file.getName();
		// Illegal characters are not checked here because they are check by both JDK
		// and OS. This is only a check against technical allowed but unwanted device
		// names.
		int dot = fileName.indexOf('.');
		// on windows, filename suffixes are not relevant to name validity
		String basename = dot == -1 ? fileName : fileName.substring(0, dot);
		return RESERVED_NAMES.contains(basename.toLowerCase());
	}

}
