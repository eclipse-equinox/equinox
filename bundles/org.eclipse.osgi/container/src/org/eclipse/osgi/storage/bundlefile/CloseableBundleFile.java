/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
 *     Rob Harrop - SpringSource Inc. (bug 253942)
 *******************************************************************************/

package org.eclipse.osgi.storage.bundlefile;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ContainerEvent;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.storage.BundleInfo;
import org.eclipse.osgi.storage.Storage.StorageException;
import org.eclipse.osgi.util.NLS;

/**
 * A BundleFile that manages the number of open bundle files by using the MRUBundleFileList
 * @param <E> a type specified by extending classes to call {@link #getInputStream(Object)}
 */
public abstract class CloseableBundleFile<E> extends BundleFile {

	// A reentrant lock is used here (instead of intrinsic synchronization)
	// to allow the lock conditional held
	// see lockOpen() and open(boolean)
	private final ReentrantLock openLock = new ReentrantLock();
	private final Condition refCondition = openLock.newCondition();

	private final MRUBundleFileList mruList;

	protected final BundleInfo.Generation generation;

	protected final Debug debug;

	/**
	 * The closed flag
	 */
	private volatile boolean closed = true;

	private int referenceCount = 0;

	public CloseableBundleFile(File basefile, BundleInfo.Generation generation, MRUBundleFileList mruList, Debug debug) {
		super(basefile);
		this.debug = debug;
		this.generation = generation;
		this.closed = true;
		this.mruList = mruList;
	}

	/**
	 * Checks if the bundle file is open
	 * @return true if the bundle file is open and locked
	 */
	private boolean lockOpen() {
		try {
			open(true);
			return true;
		} catch (IOException e) {
			if (generation != null) {
				ModuleRevision r = generation.getRevision();
				if (r != null) {
					ContainerEvent eventType = ContainerEvent.ERROR;
					// If the revision has been removed from the list of revisions then it has been deleted
					// because the bundle has been uninstalled or updated
					if (!r.getRevisions().getModuleRevisions().contains(r)) {
						// instead of filling the log with errors about missing files from 
						// uninstalled/updated bundles just give it an info level
						eventType = ContainerEvent.INFO;
					}
					generation.getBundleInfo().getStorage().getAdaptor().publishContainerEvent(eventType, r.getRevisions().getModule(), e);
				}
			}
			// TODO not sure if throwing a runtime exception is better
			// throw new RuntimeException("Failed to open bundle file.", e);
			return false;
		}
	}

	/**
	 * Opens this bundle file.
	 * @param keepLock true if the open lock should be retained
	 * @throws IOException
	 */
	private void open(boolean keepLock) throws IOException {
		openLock.lock();
		try {
			if (closed) {
				boolean needBackPressure = mruListAdd();
				if (needBackPressure) {
					// release lock before applying back pressure
					openLock.unlock();
					try {
						mruListApplyBackPressure();
					} finally {
						// get lock back after back pressure
						openLock.lock();
					}
				}
				// check close again after getting open lock again
				if (closed) {
					// always add again if back pressure was applied in case
					// the bundle file got removed while releasing the open lock
					if (needBackPressure) {
						mruListAdd();
					}
					// This can throw an IO exception resulting in closed remaining true on exit
					doOpen();
					closed = false;
				}
			} else {
				mruListUse();
			}
		} finally {
			if (!keepLock || closed) {
				openLock.unlock();
			}
		}
	}

	/**
	 * Opens the bundle file
	 * @throws IOException if an error occurs
	 */
	protected abstract void doOpen() throws IOException;

	/**
	 * Extracts a directory and all sub content to disk
	 * @param dirName the directory name to extract
	 * @return the File used to extract the content to.  A value
	 * of <code>null</code> is returned if the directory to extract does 
	 * not exist or if content extraction is not supported.
	 */
	File extractDirectory(String dirName) {
		if (!lockOpen()) {
			return null;
		}
		try {
			for (String path : getPaths()) {
				if (path.startsWith(dirName) && !path.endsWith("/")) //$NON-NLS-1$
					getFile(path, false);
			}
			return getExtractFile(dirName);
		} finally {
			openLock.unlock();
		}
	}

	protected abstract Iterable<String> getPaths();

	private File getExtractFile(String entryName) {
		if (generation == null)
			return null;
		return generation.getExtractFile(".cp", entryName); //$NON-NLS-1$
	}

	@Override
	public File getFile(String entry, boolean nativeCode) {
		if (!lockOpen()) {
			return null;
		}
		try {
			BundleEntry bEntry = getEntry(entry);
			if (bEntry == null)
				return null;

			try {
				File nested = getExtractFile(bEntry.getName());
				if (nested != null) {
					if (nested.exists()) {
						/* the entry is already cached */
						if (debug.DEBUG_BUNDLE_FILE)
							Debug.println("File already present: " + nested.getPath()); //$NON-NLS-1$
						if (nested.isDirectory())
							// must ensure the complete directory is extracted (bug 182585)
							extractDirectory(bEntry.getName());
					} else {
						if (bEntry.getName().endsWith("/")) { //$NON-NLS-1$
							nested.mkdirs();
							if (!nested.isDirectory()) {
								if (debug.DEBUG_BUNDLE_FILE)
									Debug.println("Unable to create directory: " + nested.getPath()); //$NON-NLS-1$
								throw new IOException(NLS.bind(Msg.ADAPTOR_DIRECTORY_CREATE_EXCEPTION, nested.getAbsolutePath()));
							}
							extractDirectory(bEntry.getName());
						} else {
							InputStream in = bEntry.getInputStream();
							if (in == null)
								return null;
							generation.storeContent(nested, in, nativeCode);
						}
					}

					return nested;
				}
			} catch (IOException | StorageException e) {
				if (debug.DEBUG_BUNDLE_FILE)
					Debug.printStackTrace(e);
				generation.getBundleInfo().getStorage().getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, "Unable to extract content: " + generation.getRevision() + ": " + entry, e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} finally {
			openLock.unlock();
		}
		return null;
	}

	@Override
	public boolean containsDir(String dir) {
		if (!lockOpen()) {
			return false;
		}
		try {
			if (dir == null)
				return false;

			if (dir.length() == 0)
				return true;

			if (dir.charAt(0) == '/') {
				if (dir.length() == 1)
					return true;
				dir = dir.substring(1);
			}

			if (dir.length() > 0 && dir.charAt(dir.length() - 1) != '/')
				dir = dir + '/';

			for (String entry : getPaths()) {
				if (entry.startsWith(dir)) {
					return true;
				}
			}
		} finally {
			openLock.unlock();
		}
		return false;
	}

	@Override
	public BundleEntry getEntry(String path) {
		if (!lockOpen()) {
			return null;
		}
		try {
			return findEntry(path);
		} finally {
			openLock.unlock();
		}
	}

	/**
	 * Finds the bundle entry for the specified path
	 * @param path the path of the entry to find
	 * @return the entry or {@code null} if no entry exists
	 */
	protected abstract BundleEntry findEntry(String path);

	@Override
	public Enumeration<String> getEntryPaths(String path, boolean recurse) {
		if (!lockOpen()) {
			return null;
		}
		try {
			if (path == null)
				throw new NullPointerException();

			// Strip any leading '/' off of path.
			if (path.length() > 0 && path.charAt(0) == '/')
				path = path.substring(1);
			// Append a '/', if not already there, to path if not an empty string.
			if (path.length() > 0 && path.charAt(path.length() - 1) != '/')
				path = new StringBuilder(path).append("/").toString(); //$NON-NLS-1$

			LinkedHashSet<String> result = new LinkedHashSet<>();
			// Get all entries and add the ones of interest.
			for (String entryPath : getPaths()) {
				// Is the entry of possible interest? Note that 
				// string.startsWith("") == true.
				if (entryPath.startsWith(path)) {
					// If we get here, we know that the entry is either (1) equal to
					// path, (2) a file under path, or (3) a subdirectory of path.
					if (path.length() < entryPath.length()) {
						// If we get here, we know that entry is not equal to path.
						getEntryPaths(path, entryPath.substring(path.length()), recurse, result);
					}
				}
			}
			return result.size() == 0 ? null : Collections.enumeration(result);
		} finally {
			openLock.unlock();
		}
	}

	private void getEntryPaths(String path, String entry, boolean recurse, LinkedHashSet<String> entries) {
		if (entry.length() == 0)
			return;
		int slash = entry.indexOf('/');
		if (slash == -1)
			entries.add(path + entry);
		else {
			path = path + entry.substring(0, slash + 1);
			entries.add(path);
			if (recurse)
				getEntryPaths(path, entry.substring(slash + 1), true, entries);
		}
	}

	@Override
	public void close() throws IOException {
		openLock.lock();
		try {
			if (!closed) {
				if (referenceCount > 0 && isMruListClosing()) {
					// there are some opened streams to this BundleFile still;
					// wait for them all to close because this is being closed by the MRUBundleFileList
					try {
						refCondition.await(1000, TimeUnit.MICROSECONDS); // timeout after 1 second
					} catch (InterruptedException e) {
						// do nothing for now ...
					}
					if (referenceCount != 0 || closed)
						// either another thread closed the bundle file or we timed waiting for all the reference inputstreams to close
						// If the referenceCount did not reach zero then this bundle file will remain open until the
						// bundle file is closed explicitly (i.e. bundle is updated/uninstalled or framework is shutdown)
						return;

				}
				closed = true;
				doClose();
				mruListRemove();
				postClose();
			}
		} finally {
			openLock.unlock();
		}
	}

	/**
	 * Closes the bundle file
	 * @throws IOException if an error occurs closing
	 */
	protected abstract void doClose() throws IOException;

	/**
	 * Called after closing the bundle file.
	 */
	protected abstract void postClose();

	private boolean isMruListClosing() {
		return this.mruList != null && this.mruList.isClosing(this);
	}

	private boolean isMruEnabled() {
		return this.mruList != null && this.mruList.isEnabled();
	}

	private void mruListRemove() {
		if (this.mruList != null) {
			this.mruList.remove(this);
		}
	}

	private void mruListUse() {
		if (this.mruList != null) {
			mruList.use(this);
		}
	}

	private void mruListApplyBackPressure() {
		if (this.mruList != null) {
			this.mruList.applyBackpressure();
		}
	}

	private boolean mruListAdd() {
		if (this.mruList != null) {
			return mruList.add(this);
		}
		return false;
	}

	@Override
	public void open() throws IOException {
		open(false);
	}

	void incrementReference() {
		openLock.lock();
		try {
			referenceCount += 1;
		} finally {
			openLock.unlock();
		}
	}

	void decrementReference() {
		openLock.lock();
		try {
			referenceCount = Math.max(0, referenceCount - 1);
			// only notify if the referenceCount is zero.
			if (referenceCount == 0)
				refCondition.signal();
		} finally {
			openLock.unlock();
		}
	}

	/**
	 * Gets the input stream for the specified entry.
	 * This method will ensure the bundle file is open,
	 * call {@link #doGetInputStream(Object)} to get the
	 * actual input stream, then if the bundle file limit
	 * is enabled it will wrapper the input stream in a 
	 * special input stream that keeps track of active
	 * input streams to prevent the bundle file from being
	 * closed until the stream is closed (or a timeout happens).
	 * @param entry the entry to get the input stream for
	 * @return the input stream for the entry
	 * @throws IOException
	 */
	public InputStream getInputStream(E entry) throws IOException {
		if (!lockOpen()) {
			throw new IOException("Failed to lock bundle file."); //$NON-NLS-1$
		}
		try {
			InputStream in = doGetInputStream(entry);
			if (isMruEnabled()) {
				in = new BundleEntryInputStream(in);
			}
			return in;
		} finally {
			openLock.unlock();
		}
	}

	/**
	 * Gets the input stream for the specified entry.
	 * @param entry the entry to get the input stream for.  The type is specified by the
	 * extending class.
	 * @return the input steam for the entry
	 * @throws IOException if an error occurs
	 */
	protected abstract InputStream doGetInputStream(E entry) throws IOException;

	private class BundleEntryInputStream extends FilterInputStream {

		private boolean streamClosed = false;

		public BundleEntryInputStream(InputStream stream) {
			super(stream);
			incrementReference();
		}

		@Override
		public int available() throws IOException {
			try {
				return super.available();
			} catch (IOException e) {
				throw enrichExceptionWithBaseFile(e);
			}
		}

		@Override
		public void close() throws IOException {
			try {
				super.close();
			} catch (IOException e) {
				throw enrichExceptionWithBaseFile(e);
			} finally {
				synchronized (this) {
					if (streamClosed)
						return;
					streamClosed = true;
				}
				decrementReference();
			}
		}

		@Override
		public int read() throws IOException {
			try {
				return super.read();
			} catch (IOException e) {
				throw enrichExceptionWithBaseFile(e);
			}
		}

		@Override
		public int read(byte[] var0, int var1, int var2) throws IOException {
			try {
				return super.read(var0, var1, var2);
			} catch (IOException e) {
				throw enrichExceptionWithBaseFile(e);
			}
		}

		@Override
		public int read(byte[] var0) throws IOException {
			try {
				return super.read(var0);
			} catch (IOException e) {
				throw enrichExceptionWithBaseFile(e);
			}
		}

		@Override
		public void reset() throws IOException {
			try {
				super.reset();
			} catch (IOException e) {
				throw enrichExceptionWithBaseFile(e);
			}
		}

		@Override
		public long skip(long var0) throws IOException {
			try {
				return super.skip(var0);
			} catch (IOException e) {
				throw enrichExceptionWithBaseFile(e);
			}
		}

		private IOException enrichExceptionWithBaseFile(IOException e) {
			return new IOException(getBaseFile().toString(), e);
		}
	}
}
