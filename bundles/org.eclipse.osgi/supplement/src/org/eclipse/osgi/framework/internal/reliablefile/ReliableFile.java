/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others.
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

package org.eclipse.osgi.framework.internal.reliablefile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * ReliableFile class used by ReliableFileInputStream and ReliableOutputStream.
 * This class encapsulates all the logic for reliable file support.
 */
public class ReliableFile {
	/**
	 * Open mask. Obtain the best data stream available. If the primary data
	 * contents are invalid (corrupt, missing, etc.), the data for a prior
	 * version may be used.
	 * An IOException will be thrown if a valid data content can not be
	 * determined.
	 * This is mutually exclusive with <code>OPEN_FAIL_ON_PRIMARY</code>.
	 */
	public static final int OPEN_BEST_AVAILABLE = 0;
	/**
	 * Open mask. Obtain only the data stream for the primary file where any other
	 * version will not be valid. This should be used for data streams that are
	 * managed as a group as a prior contents may not match the other group data.
	 * If the primary data is not invalid, a IOException will be thrown.
	 * This is mutually exclusive with <code>OPEN_BEST_AVAILABLE</code>.
	 */
	public static final int OPEN_FAIL_ON_PRIMARY = 1;

	/**
	 * Use the last generation of the file
	 */
	public static final int GENERATION_LATEST = 0;
	/**
	 * Keep infinite backup files
	 */
	public static final int GENERATIONS_INFINITE = 0;

	/**
	 * Extension of tmp file used during writing.
	 * A reliable file with this extension should
	 * never be directly used.
	 */
	public static final String tmpExt = ".tmp"; //$NON-NLS-1$

	/**
	 * Property to set the maximum size of a file that will be buffered. When calculating a ReliableFile
	 * checksum, if the file is this size or small, ReliableFile will read the file contents into a
	 * <code>BufferedInputStream</code> and reset the buffer to avoid having to read the data from the
	 * media twice. Since this method require memory for storage, it is limited to this size. The default
	 * maximum is 128-KBytes.
	 */
	public static final String PROP_MAX_BUFFER = "osgi.reliableFile.maxInputStreamBuffer"; //$NON-NLS-1$
	/**
	 * The maximum number of generations to keep as backup files in case last generation
	 * file is determined to be invalid.
	 */
	public static final String PROP_MAX_GENERATIONS = "osgi.ReliableFile.maxGenerations"; //$NON-NLS-1$
	/**
	 * @see org.eclipse.osgi.internal.location.LocationHelper#PROP_OSGI_LOCKING
	 */
	public static final String PROP_OSGI_LOCKING = "osgi.locking"; //$NON-NLS-1$

	private static final int FILETYPE_VALID = 0;
	private static final int FILETYPE_CORRUPT = 1;
	private static final int FILETYPE_NOSIGNATURE = 2;

	private static final byte identifier1[] = {'.', 'c', 'r', 'c'};
	private static final byte identifier2[] = {'.', 'v', '1', '\n'};

	private static final int BUF_SIZE = 4096;
	private static final int maxInputStreamBuffer;
	private static final int defaultMaxGenerations;
	private static final boolean fileSharing;
	//our cache of the last looked up generations for a file
	private static File lastGenerationFile = null;
	private static int[] lastGenerations = null;
	private static final Object lastGenerationLock = new Object();
	private static final int MAX_TEMP_NUM = 100000;
	private static final AtomicInteger nextTemp = new AtomicInteger(1);

	static {
		String prop = System.getProperty(PROP_MAX_BUFFER);
		int tmpMaxInput = 128 * 1024; //128k
		if (prop != null) {
			try {
				tmpMaxInput = Integer.parseInt(prop);
			} catch (NumberFormatException e) {/*ignore*/
			}
		}
		maxInputStreamBuffer = tmpMaxInput;

		int tmpDefaultMax = 2;
		prop = System.getProperty(PROP_MAX_GENERATIONS);
		if (prop != null) {
			try {
				tmpDefaultMax = Integer.parseInt(prop);
			} catch (NumberFormatException e) {/*ignore*/
			}
		}
		defaultMaxGenerations = tmpDefaultMax;

		prop = System.getProperty(PROP_OSGI_LOCKING);
		boolean tmpFileSharing = true;
		if (prop != null) {
			if (prop.equals("none")) { //$NON-NLS-1$
				tmpFileSharing = false;
			}
		}
		fileSharing = tmpFileSharing;
	}

	/** File object for original reference file */
	private File referenceFile;

	/** List of checksum file objects: File => specific ReliableFile generation */
	private static Hashtable<File, CacheInfo> cacheFiles = new Hashtable<>(20);

	private File inputFile = null;
	private File outputFile = null;
	private Checksum appendChecksum = null;

	/**
	 * ReliableFile object factory. This method is called by ReliableFileInputStream
	 * and ReliableFileOutputStream to get a ReliableFile object for a target file.
	 * If the object is in the cache, the cached copy is returned.
	 * Otherwise a new ReliableFile object is created and returned.
	 * The use count of the returned ReliableFile object is incremented.
	 *
	 * @param name Name of the target file.
	 * @return A ReliableFile object for the target file.
	 * @throws IOException If the target file is a directory.
	 */
	static ReliableFile getReliableFile(String name) throws IOException {
		return getReliableFile(new File(name));
	}

	/**
	 * ReliableFile object factory. This method is called by ReliableFileInputStream
	 * and ReliableFileOutputStream to get a ReliableFile object for a target file.
	 * If the object is in the cache, the cached copy is returned.
	 * Otherwise a new ReliableFile object is created and returned.
	 * The use count of the returned ReliableFile object is incremented.
	 *
	 * @param file File object for the target file.
	 * @return A ReliableFile object for the target file.
	 * @throws IOException If the target file is a directory.
	 */
	static ReliableFile getReliableFile(File file) throws IOException {
		if (file.isDirectory()) {
			throw new FileNotFoundException("file is a directory"); //$NON-NLS-1$
		}
		return new ReliableFile(file);
	}

	/**
	 * Private constructor used by the static getReliableFile factory methods.
	 *
	 * @param file File object for the target file.
	 */
	private ReliableFile(File file) {
		referenceFile = file;
	}

	private static int[] getFileGenerations(File file) {
		if (!fileSharing) {
			synchronized (lastGenerationLock) {
				if (lastGenerationFile != null) {
					//shortcut maybe, only if filesharing is not supported
					if (file.equals(lastGenerationFile))
						return lastGenerations;
				}
			}
		}
		int[] generations = null;
		try {
			String name = file.getName();
			String prefix = name + '.';
			int prefixLen = prefix.length();
			File parent = new File(file.getParent());
			String[] files = parent.list();
			if (files == null)
				return null;
			List<Integer> list = new ArrayList<>(defaultMaxGenerations);
			if (file.exists())
				list.add(Integer.valueOf(0)); //base file exists
			for (String candidateFile : files) {
				if (candidateFile.startsWith(prefix)) {
					try {
						int id = Integer.parseInt(candidateFile.substring(prefixLen));
						list.add(Integer.valueOf(id));
					}catch (NumberFormatException e) {/*ignore*/
					}
				}
			}
			if (list.size() == 0)
				return null;
			Object[] array = list.toArray();
			Arrays.sort(array);
			generations = new int[array.length];
			for (int i = 0, j = array.length - 1; i < array.length; i++, j--) {
				generations[i] = ((Integer) array[j]).intValue();
			}
			return generations;
		} finally {
			if (!fileSharing) {
				synchronized (lastGenerationLock) {
					lastGenerationFile = file;
					lastGenerations = generations;
				}
			}
		}
	}

	/**
	 * Returns an InputStream object for reading the target file.
	 *
	 * @param generation the maximum generation to evaluate
	 * @param openMask mask used to open data.
	 * are invalid (corrupt, missing, etc).
	 * @return An InputStream object which can be used to read the target file.
	 * @throws IOException If an error occurs preparing the file.
	 */
	InputStream getInputStream(int generation, int openMask) throws IOException {
		if (inputFile != null) {
			throw new IOException("Input stream already open"); //$NON-NLS-1$
		}
		int[] generations = getFileGenerations(referenceFile);
		if (generations == null) {
			throw new FileNotFoundException("File not found"); //$NON-NLS-1$
		}
		String name = referenceFile.getName();
		File parent = new File(referenceFile.getParent());

		boolean failOnPrimary = (openMask & OPEN_FAIL_ON_PRIMARY) != 0;
		if (failOnPrimary && generation == GENERATIONS_INFINITE)
			generation = generations[0];

		File textFile = null;
		InputStream textIS = null;
		for (int generation2 : generations) {
			if (generation != 0) {
				if (generation2 > generation || (failOnPrimary && generation2 != generation))
					continue;
			}
			File file;
			if (generation2 != 0)
				file = new File(parent, name + '.' + generation2);
			else
				file = referenceFile;
			InputStream is = null;
			CacheInfo info;
			synchronized (cacheFiles) {
				info = cacheFiles.get(file);
				long timeStamp = file.lastModified();
				if (info == null || timeStamp != info.timeStamp) {
					InputStream tempIS = new FileInputStream(file);
					try {
						long fileSize = file.length();
						if (fileSize > 0 && fileSize < maxInputStreamBuffer) {
							tempIS = new BufferedInputStream(tempIS, (int) fileSize);
							// reuse the tempIS since it supports mark/reset
							is = tempIS;
						}
						Checksum cksum = getChecksumCalculator();
						int filetype = getStreamType(tempIS, cksum, fileSize);
						info = new CacheInfo(filetype, cksum, timeStamp, fileSize);
						cacheFiles.put(file, info);
					} catch (IOException e) {/*ignore*/
					} finally {
						if (is == null) {
							// close the tempIS since it was simply used to get the check sum
							try {
								tempIS.close();
							} catch (IOException e) {/*ignore*/
							}
						}
					}
				}
			}

			// if looking for a specific generation only, only look at one
			//  and return the result.
			if (failOnPrimary) {
				if (info != null && info.filetype == FILETYPE_VALID) {
					inputFile = file;
					if (is != null)
						return is;
					return new FileInputStream(file);
				}
				throw new IOException("ReliableFile is corrupt"); //$NON-NLS-1$
			}

			// if error, ignore this file & try next
			if (info == null)
				continue;

			// we're  not looking for a specific version, so let's pick the best case
			switch (info.filetype) {
				case FILETYPE_VALID :
					inputFile = file;
					if (is != null)
						return is;
					return new FileInputStream(file);

				case FILETYPE_NOSIGNATURE :
					if (textFile == null) {
						textFile = file;
						textIS = is;
					}
					break;
			}
		}

		// didn't find any valid files, if there are any plain text files
		//  use it instead
		if (textFile != null) {
			inputFile = textFile;
			if (textIS != null)
				return textIS;
			return new FileInputStream(textFile);
		}
		throw new IOException("ReliableFile is corrupt"); //$NON-NLS-1$
	}

	/**
	 * Returns an OutputStream object for writing the target file.
	 *
	 * @param append append new data to an existing file.
	 * @param appendGeneration specific generation of file to append from.
	 * @return An OutputStream object which can be used to write the target file.
	 * @throws IOException IOException If an error occurs preparing the file.
	 */
	OutputStream getOutputStream(boolean append, int appendGeneration) throws IOException {
		if (outputFile != null)
			throw new IOException("Output stream is already open"); //$NON_NLS-1$ //$NON-NLS-1$
		String name = referenceFile.getName();
		File parent = new File(referenceFile.getParent());
		File tmpFile = ReliableFile.createTempFile(name, tmpExt, parent);

		if (!append) {
			OutputStream os = new FileOutputStream(tmpFile);
			outputFile = tmpFile;
			return os;
		}

		InputStream is;
		try {
			is = getInputStream(appendGeneration, OPEN_BEST_AVAILABLE);
		} catch (FileNotFoundException e) {
			OutputStream os = new FileOutputStream(tmpFile);
			outputFile = tmpFile;
			return os;
		}

		try {
			CacheInfo info = cacheFiles.get(inputFile);
			appendChecksum = info.checksum;
			OutputStream os = new FileOutputStream(tmpFile);
			if (info.filetype == FILETYPE_NOSIGNATURE) {
				cp(is, os, 0, info.length);
			} else {
				cp(is, os, 16, info.length); // don't copy checksum signature
			}
			outputFile = tmpFile;
			return os;
		} finally {
			closeInputFile();
		}
	}

	/**
	 * Close the target file for reading.
	 *
	 * @param checksum Checksum of the file contents
	 * @throws IOException If an error occurs closing the file.
	 */
	void closeOutputFile(Checksum checksum) throws IOException {
		if (outputFile == null)
			throw new IOException("Output stream is not open"); //$NON-NLS-1$
		int[] generations = getFileGenerations(referenceFile);
		String name = referenceFile.getName();
		File parent = new File(referenceFile.getParent());
		File newFile;
		if (generations == null)
			newFile = new File(parent, name + ".1"); //$NON-NLS-1$
		else
			newFile = new File(parent, name + '.' + (generations[0] + 1));

		mv(outputFile, newFile); // throws IOException if problem
		outputFile = null;
		appendChecksum = null;
		CacheInfo info = new CacheInfo(FILETYPE_VALID, checksum, newFile.lastModified(), newFile.length());
		cacheFiles.put(newFile, info);
		cleanup(generations, true);
		if (!fileSharing) {
			synchronized (lastGenerationLock) {
				lastGenerationFile = null;
				lastGenerations = null;
			}
		}
	}

	/**
	 * Abort the current output stream and do not update the reliable file table.
	 */
	void abortOutputFile() {
		if (outputFile == null)
			return;
		outputFile.delete();
		outputFile = null;
		appendChecksum = null;
	}

	File getOutputFile() {
		return outputFile;
	}

	/**
	 * Close the target file for reading.
	 */
	void closeInputFile() {
		inputFile = null;
	}

	private void cleanup(int[] generations, boolean generationAdded) {
		if (generations == null)
			return;
		String name = referenceFile.getName();
		File parent = new File(referenceFile.getParent());
		int generationCount = generations.length;
		// if a base file is in the list (0 in generations[]), we will
		//  never delete these files, so don't count them in the old
		//  generation count.
		if (generations[generationCount - 1] == 0)
			generationCount--;
		// assume here that the int[] does not include a file just created
		int rmCount = generationCount - defaultMaxGenerations;
		if (generationAdded)
			rmCount++;
		if (rmCount < 1)
			return;
		synchronized (cacheFiles) {
			// first, see if any of the files not deleted are known to
			//  be corrupt. If so, be sure to keep not to delete good
			//  backup files.
			for (int idx = 0, count = generationCount - rmCount; idx < count; idx++) {
				File file = new File(parent, name + '.' + generations[idx]);
				CacheInfo info = cacheFiles.get(file);
				if (info != null) {
					if (info.filetype == FILETYPE_CORRUPT)
						rmCount--;
				}
			}
			for (int idx = generationCount - 1; rmCount > 0; idx--, rmCount--) {
				File rmFile = new File(parent, name + '.' + generations[idx]);
				rmFile.delete();
				cacheFiles.remove(rmFile);
			}
		}
	}

	/**
	 * Rename a file.
	 *
	 * @param from The original file.
	 * @param to The new file name.
	 * @throws IOException If the rename failed.
	 */
	private static void mv(File from, File to) throws IOException {
		if (!from.renameTo(to)) {
			throw new IOException("rename failed"); //$NON-NLS-1$
		}
	}

	/**
	 * Copy a file.
	 *
	 * @throws IOException If the copy failed.
	 */
	private static void cp(InputStream in, OutputStream out, int truncateSize, long length) throws IOException {
		try {
			if (truncateSize > length)
				length = 0;
			else
				length -= truncateSize;
			if (length > 0) {
				int bufferSize;
				if (length > BUF_SIZE) {
					bufferSize = BUF_SIZE;
				} else {
					bufferSize = (int) length;
				}

				byte buffer[] = new byte[bufferSize];
				long size = 0;
				int count;
				while ((count = in.read(buffer, 0, bufferSize)) > 0) {
					if ((size + count) >= length)
						count = (int) (length - size);
					out.write(buffer, 0, count);
					size += count;
				}
			}
		} finally {
			try {
				in.close();
			} catch (IOException e) {/*ignore*/
			}
			out.close();
		}
	}

	/**
	 * Answers a boolean indicating whether or not the specified reliable file
	 * exists on the underlying file system. This call only returns if a file
	 * exists and not if the file contents are valid.
	 * @param file returns true if the specified reliable file exists; otherwise false is returned
	 *
	 * @return <code>true</code> if the specified reliable file exists,
	 * <code>false</code> otherwise.
	 */
	public static boolean exists(File file) {
		String prefix = file.getName() + '.';
		File parent = new File(file.getParent());
		int prefixLen = prefix.length();
		String[] files = parent.list();
		if (files == null)
			return false;
		for (String candidateFile : files) {
			if (candidateFile.startsWith(prefix)) {
				try {
					Integer.parseInt(candidateFile.substring(prefixLen));
					return true;
				}catch (NumberFormatException e) {/*ignore*/
				}
			}
		}
		return file.exists();
	}

	/**
	 * Returns the time that the reliable file was last modified. Only the time
	 * of the last file generation is returned.
	 * @param file the file to determine the time of.
	 * @return time the file was last modified (see java.io.File.lastModified()).
	 */
	public static long lastModified(File file) {
		int[] generations = getFileGenerations(file);
		if (generations == null)
			return 0L;
		if (generations[0] == 0)
			return file.lastModified();
		String name = file.getName();
		File parent = new File(file.getParent());
		File newFile = new File(parent, name + '.' + generations[0]);
		return newFile.lastModified();
	}

	/**
	 * Returns the time that this ReliableFile was last modified. This method is only valid
	 * after requesting an input stream and the time of the actual input file is returned.
	 *
	 * @return time the file was last modified (see java.io.File.lastModified()) or
	 * 0L if an input stream is not open.
	 */
	public long lastModified() {
		if (inputFile != null) {
			return inputFile.lastModified();
		}
		return 0L;
	}

	/**
	 * Returns the a version number of a reliable managed file. The version can be expected
	 * to be unique for each successful file update.
	 *
	 * @param file the file to determine the version of.
	 * @return a unique version of this current file. A value of -1 indicates the file does
	 * not exist or an error occurred.
	 */
	public static int lastModifiedVersion(File file) {
		int[] generations = getFileGenerations(file);
		if (generations == null)
			return -1;
		return generations[0];
	}

	/**
	 * Delete the specified reliable file on the underlying file system.
	 * @param deleteFile the reliable file to delete
	 *
	 * @return <code>true</code> if the specified reliable file was deleted,
	 * <code>false</code> otherwise.
	 */
	public static boolean delete(File deleteFile) {
		int[] generations = getFileGenerations(deleteFile);
		if (generations == null)
			return false;
		String name = deleteFile.getName();
		File parent = new File(deleteFile.getParent());
		synchronized (cacheFiles) {
			for (int generation : generations) {
				// base files (.0 in generations[]) will never be deleted
				if (generation == 0)
					continue;
				File file = new File(parent, name + '.' + generation);
				if (file.exists()) {
					file.delete();
				}
				cacheFiles.remove(file);
			}
		}
		return true;
	}

	/**
	 * Get a list of ReliableFile base names in a given directory. Only files with a valid
	 * ReliableFile generation are included.
	 * @param directory the directory to inquire.
	 * @return an array of ReliableFile names in the directory.
	 * @throws IOException if an error occurs.
	 */
	public static String[] getBaseFiles(File directory) throws IOException {
		if (!directory.isDirectory())
			throw new IOException("Not a valid directory"); //$NON-NLS-1$
		String files[] = directory.list();
		Set<String> list = new HashSet<>(files.length / 2);
		for (String file : files) {
			int pos = file.lastIndexOf('.');
			if (pos == -1)
				continue;
			String ext = file.substring(pos + 1);
			int generation = 0;
			try {
				generation = Integer.parseInt(ext);
			} catch (NumberFormatException e) {/*skip*/
			}
			if (generation == 0)
				continue;
			String base = file.substring(0, pos);
			list.add(base);
		}
		files = new String[list.size()];
		int idx = 0;
		for (String string : list) {
			files[idx++] = string;
		}
		return files;
	}

	/**
	 * Delete any old excess generations of a given reliable file.
	 * @param base realible file.
	 */
	public static void cleanupGenerations(File base) {
		ReliableFile rf = new ReliableFile(base);
		int[] generations = getFileGenerations(base);
		rf.cleanup(generations, false);
		if (!fileSharing) {
			synchronized (lastGenerationLock) {
				lastGenerationFile = null;
				lastGenerations = null;
			}
		}
	}

	/*
	 * Implementation note: This implementation differs from File.createTempFile by
	 * avoiding usage of SecureRandom to generate unique file names.
	 * 
	 * Any usage of this must be used in a context that is not sensitive to outside
	 * guessing of the temporary file name.
	 */
	public static File createTempFile(String prefix, String suffix, File directory) throws IOException {
		if (directory == null) {
			throw new IllegalArgumentException("No directory specified."); //$NON-NLS-1$
		}
		if (prefix == null) {
			throw new IllegalArgumentException("No prefix specified."); //$NON-NLS-1$
		}
		if (suffix == null) {
			suffix = ".tmp"; //$NON-NLS-1$
		}
		for (int i = 0; i < MAX_TEMP_NUM; i++) {
			File f = new File(directory, prefix + nextTemp.getAndUpdate(n -> {
				int next = n + 1;
				return next > MAX_TEMP_NUM ? 1 : next;
			}) + suffix);
			if (f.createNewFile()) {
				return f;
			}
		}
		throw new IOException("Maximum number of attempts reached to create a temporary file."); //$NON-NLS-1$
	}

	/**
	 * Inform ReliableFile that a file has been updated outside of
	 * ReliableFile.
	 */
	public static void fileUpdated(File file) {
		if (!fileSharing) {
			synchronized (lastGenerationLock) {
				lastGenerationFile = null;
				lastGenerations = null;
			}
		}
	}

	/**
	 * Append a checksum value to the end of an output stream.
	 * @param out the output stream.
	 * @param checksum the checksum value to append to the file.
	 * @throws IOException if a write error occurs.
	 */
	void writeChecksumSignature(OutputStream out, Checksum checksum) throws IOException {
		// tag on our signature and checksum
		out.write(ReliableFile.identifier1);
		out.write(intToHex((int) checksum.getValue()));
		out.write(ReliableFile.identifier2);
	}

	/**
	 * Returns the size of the ReliableFile signature + CRC at the end of the file.
	 * This method should be called only after calling getInputStream() or
	 * getOutputStream() methods.
	 *
	 * @return <code>int</code> size of the ReliableFIle signature + CRC appended
	 * to the end of the file.
	 * @throws IOException if getInputStream() or getOutputStream has not been
	 * called.
	 */
	int getSignatureSize() throws IOException {
		if (inputFile != null) {
			CacheInfo info = cacheFiles.get(inputFile);
			if (info != null) {
				switch (info.filetype) {
					case FILETYPE_VALID :
					case FILETYPE_CORRUPT :
						return 16;
					case FILETYPE_NOSIGNATURE :
						return 0;
				}
			}
		}
		throw new IOException("ReliableFile signature size is unknown"); //$NON-NLS-1$
	}

	long getInputLength() throws IOException {
		if (inputFile != null) {
			CacheInfo info = cacheFiles.get(inputFile);
			if (info != null) {
				return info.length;
			}
		}
		throw new IOException("ReliableFile length is unknown"); //$NON-NLS-1$
	}

	/**
	 * Returns a Checksum object for the current file contents. This method
	 * should be called only after calling getInputStream() or
	 * getOutputStream() methods.
	 *
	 * @return Object implementing Checksum interface initialized to the
	 * current file contents.
	 * @throws IOException if getOutputStream for append has not been called.
	 */
	Checksum getFileChecksum() throws IOException {
		if (appendChecksum == null)
			throw new IOException("Checksum is invalid!"); //$NON-NLS-1$
		return appendChecksum;
	}

	/**
	 * Create a checksum implementation used by ReliableFile.
	 *
	 * @return Object implementing Checksum interface used to calculate
	 * a reliable file checksum
	 */
	Checksum getChecksumCalculator() {
		// Using CRC32 because Adler32 isn't in the eeMinimum library.
		return new CRC32();
	}

	/**
	 * Determine if a File is a valid ReliableFile
	 *
	 * @return <code>true</code> if the file is a valid ReliableFile
	 * @throws IOException If an error occurs verifying the file.
	 */
	private int getStreamType(InputStream is, Checksum crc, long len) throws IOException {
		boolean markSupported = len < Integer.MAX_VALUE && is.markSupported();
		if (markSupported)
			is.mark((int) len);
		try {
			if (len < 16) {
				if (crc != null) {
					byte data[] = new byte[16];
					int num = is.read(data);
					if (num > 0)
						crc.update(data, 0, num);
				}
				return FILETYPE_NOSIGNATURE;
			}
			len -= 16;

			int pos = 0;
			byte data[] = new byte[BUF_SIZE];

			while (pos < len) {
				int read = data.length;
				if (pos + read > len)
					read = (int) (len - pos);

				int num = is.read(data, 0, read);
				if (num == -1) {
					throw new IOException("Unable to read entire file."); //$NON-NLS-1$
				}

				crc.update(data, 0, num);
				pos += num;
			}

			int num = is.read(data); // read last 16-byte signature
			if (num != 16) {
				throw new IOException("Unable to read entire file."); //$NON-NLS-1$
			}

			int i, j;
			for (i = 0; i < 4; i++)
				if (identifier1[i] != data[i]) {
					crc.update(data, 0, 16); // update crc w/ sig bytes
					return FILETYPE_NOSIGNATURE;
				}
			for (i = 0, j = 12; i < 4; i++, j++)
				if (identifier2[i] != data[j]) {
					crc.update(data, 0, 16); // update crc w/ sig bytes
					return FILETYPE_NOSIGNATURE;
				}
			long crccmp = Long.valueOf(new String(data, 4, 8, StandardCharsets.UTF_8), 16).longValue();
			if (crccmp == crc.getValue()) {
				return FILETYPE_VALID;
			}
			// do not update CRC
			return FILETYPE_CORRUPT;
		} finally {
			if (markSupported)
				is.reset();
		}
	}

	private static byte[] intToHex(int l) {
		byte[] buffer = new byte[8];
		int count = 8;

		do {
			int ch = (l & 0xf);
			if (ch > 9)
				ch = ch - 10 + 'a';
			else
				ch += '0';
			buffer[--count] = (byte) ch;
			l >>= 4;
		} while (count > 0);
		return buffer;
	}

	private class CacheInfo {
		int filetype;
		Checksum checksum;
		long timeStamp;
		long length;

		CacheInfo(int filetype, Checksum checksum, long timeStamp, long length) {
			this.filetype = filetype;
			this.checksum = checksum;
			this.timeStamp = timeStamp;
			this.length = length;
		}
	}
}
